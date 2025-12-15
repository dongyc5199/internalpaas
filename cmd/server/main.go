package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/spf13/viper"
	"go.uber.org/zap"

	v1 "github.com/yourorg/codehub/internal/api/v1"
	"github.com/yourorg/codehub/internal/middleware"
	"github.com/yourorg/codehub/internal/pkg/database"
	"github.com/yourorg/codehub/internal/service"
)

var (
	// Version 版本号，编译时注入
	Version = "dev"
	// BuildTime 构建时间，编译时注入
	BuildTime = "unknown"
)

func main() {
	// 初始化日志
	logger, err := zap.NewProduction()
	if err != nil {
		log.Fatalf("Failed to initialize logger: %v", err)
	}
	defer logger.Sync()

	// 加载配置
	if err := loadConfig(); err != nil {
		logger.Fatal("Failed to load config", zap.Error(err))
	}

	// 打印版本信息
	logger.Info("Starting CodeHub Server",
		zap.String("version", Version),
		zap.String("build_time", BuildTime),
	)

	// 初始化数据库
	if err := database.Init(logger); err != nil {
		logger.Fatal("Failed to initialize database", zap.Error(err))
	}
	defer database.Close()

	// 运行数据库迁移
	if err := database.AutoMigrate(logger); err != nil {
		logger.Fatal("Failed to migrate database", zap.Error(err))
	}

	// 初始化服务
	giteaService, err := service.NewGiteaService(logger)
	if err != nil {
		logger.Warn("Failed to initialize Gitea service", zap.Error(err))
	}

	droneService, err := service.NewDroneService(logger)
	if err != nil {
		logger.Warn("Failed to initialize Drone service", zap.Error(err))
	}

	// 设置Gin模式
	mode := viper.GetString("server.mode")
	if mode == "" {
		mode = "debug"
	}
	gin.SetMode(mode)

	// 初始化Gin引擎
	router := gin.New()
	router.Use(gin.Logger())
	router.Use(gin.Recovery())
	router.Use(middleware.CORSMiddleware())

	// 健康检查端点
	router.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"status":     "ok",
			"version":    Version,
			"build_time": BuildTime,
		})
	})

	// 初始化API处理器
	authHandler := v1.NewAuthHandler(database.DB, logger)
	projectHandler := v1.NewProjectHandler(database.DB, logger, giteaService)
	repositoryHandler := v1.NewRepositoryHandler(database.DB, logger, giteaService, droneService)
	buildHandler := v1.NewBuildHandler(database.DB, logger, droneService, giteaService)
	webhookHandler := v1.NewWebhookHandler(database.DB, logger)

	// API v1路由组
	apiV1 := router.Group("/api/v1")
	{
		// 公开路由
		apiV1.GET("/ping", func(c *gin.Context) {
			c.JSON(200, gin.H{
				"message": "pong",
				"version": Version,
			})
		})

		// Webhook路由（公开，由外部服务调用）
		webhooks := apiV1.Group("/webhooks")
		{
			webhooks.POST("/gitea", webhookHandler.GiteaWebhook)
			webhooks.POST("/drone", webhookHandler.DroneWebhook)
		}

		// 认证路由
		auth := apiV1.Group("/auth")
		{
			auth.POST("/register", authHandler.Register)
			auth.POST("/login", authHandler.Login)
			auth.POST("/refresh", authHandler.RefreshToken)
			auth.GET("/me", middleware.AuthMiddleware(logger), authHandler.GetCurrentUser)
		}

		// 需要认证的路由
		authenticated := apiV1.Group("")
		authenticated.Use(middleware.AuthMiddleware(logger))
		{
			// 项目管理
			projects := authenticated.Group("/projects")
			{
				projects.GET("", projectHandler.ListProjects)
				projects.GET("/:id", projectHandler.GetProject)
				projects.POST("", projectHandler.CreateProject)
				projects.PUT("/:id", projectHandler.UpdateProject)
				projects.DELETE("/:id", projectHandler.DeleteProject)

				// 在项目下创建仓库
				projects.POST("/:project_id/repositories", repositoryHandler.CreateRepository)
			}

			// 仓库管理
			repositories := authenticated.Group("/repositories")
			{
				repositories.GET("", repositoryHandler.ListRepositories)
				repositories.GET("/:id", repositoryHandler.GetRepository)
				repositories.PUT("/:id/ci", repositoryHandler.UpdateCI)
				repositories.GET("/:id/branches", repositoryHandler.GetBranches)
				repositories.GET("/:id/commits", repositoryHandler.GetCommits)
				repositories.POST("/:id/webhooks", webhookHandler.ConfigureRepositoryWebhook)

				// 仓库下的构建管理
				repositories.GET("/:repo_id/builds", buildHandler.ListBuilds)
				repositories.POST("/:repo_id/builds", buildHandler.TriggerBuild)
				repositories.GET("/:repo_id/builds/stats", buildHandler.GetBuildStats)
			}

			// 构建管理
			builds := authenticated.Group("/builds")
			{
				builds.GET("/:id", buildHandler.GetBuild)
				builds.POST("/:id/restart", buildHandler.RestartBuild)
				builds.POST("/:id/cancel", buildHandler.CancelBuild)
				builds.GET("/:id/logs", buildHandler.GetBuildLogs)
			}
		}
	}

	// 配置HTTP服务器
	port := viper.GetString("server.port")
	if port == "" {
		port = "8880"
	}

	srv := &http.Server{
		Addr:           ":" + port,
		Handler:        router,
		ReadTimeout:    10 * time.Second,
		WriteTimeout:   10 * time.Second,
		MaxHeaderBytes: 1 << 20,
	}

	// 在goroutine中启动服务器
	go func() {
		logger.Info("Server listening", zap.String("port", port))
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			logger.Fatal("Failed to start server", zap.Error(err))
		}
	}()

	// 等待中断信号以优雅关闭服务器
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	logger.Info("Shutting down server...")

	// 5秒超时的优雅关闭
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		logger.Error("Server forced to shutdown", zap.Error(err))
	}

	logger.Info("Server exited")
}

// loadConfig 加载配置文件
func loadConfig() error {
	viper.SetConfigName("config")
	viper.SetConfigType("yaml")
	viper.AddConfigPath("./config")
	viper.AddConfigPath(".")

	// 设置默认值
	viper.SetDefault("server.port", "8880")
	viper.SetDefault("log.level", "info")

	// 读取环境变量
	viper.AutomaticEnv()

	// 读取配置文件（如果不存在则使用默认值）
	if err := viper.ReadInConfig(); err != nil {
		if _, ok := err.(viper.ConfigFileNotFoundError); ok {
			// 配置文件不存在，使用默认值
			return nil
		}
		return err
	}

	return nil
}
