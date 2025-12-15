package database

import (
	"fmt"
	"time"

	"github.com/spf13/viper"
	"go.uber.org/zap"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"

	"github.com/yourorg/codehub/internal/model"
)

var DB *gorm.DB

// Config 数据库配置
type Config struct {
	Host         string
	Port         int
	Username     string
	Password     string
	Database     string
	SSLMode      string
	MaxOpenConns int
	MaxIdleConns int
}

// LoadConfig 从Viper加载数据库配置
func LoadConfig() *Config {
	return &Config{
		Host:         viper.GetString("database.host"),
		Port:         viper.GetInt("database.port"),
		Username:     viper.GetString("database.username"),
		Password:     viper.GetString("database.password"),
		Database:     viper.GetString("database.database"),
		SSLMode:      viper.GetString("database.sslmode"),
		MaxOpenConns: viper.GetInt("database.max_open_conns"),
		MaxIdleConns: viper.GetInt("database.max_idle_conns"),
	}
}

// Init 初始化数据库连接
func Init(log *zap.Logger) error {
	config := LoadConfig()

	// 构建DSN
	dsn := fmt.Sprintf(
		"host=%s port=%d user=%s password=%s dbname=%s sslmode=%s",
		config.Host,
		config.Port,
		config.Username,
		config.Password,
		config.Database,
		config.SSLMode,
	)

	// 配置GORM日志
	gormLogger := logger.Default.LogMode(logger.Info)
	if viper.GetString("server.mode") == "release" {
		gormLogger = logger.Default.LogMode(logger.Warn)
	}

	// 连接数据库
	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{
		Logger:                 gormLogger,
		SkipDefaultTransaction: true,
		PrepareStmt:            true,
	})
	if err != nil {
		return fmt.Errorf("failed to connect to database: %w", err)
	}

	// 获取底层sql.DB
	sqlDB, err := db.DB()
	if err != nil {
		return fmt.Errorf("failed to get sql.DB: %w", err)
	}

	// 设置连接池
	sqlDB.SetMaxOpenConns(config.MaxOpenConns)
	sqlDB.SetMaxIdleConns(config.MaxIdleConns)
	sqlDB.SetConnMaxLifetime(time.Hour)

	// 测试连接
	if err := sqlDB.Ping(); err != nil {
		return fmt.Errorf("failed to ping database: %w", err)
	}

	DB = db
	log.Info("Database connected successfully",
		zap.String("host", config.Host),
		zap.Int("port", config.Port),
		zap.String("database", config.Database),
	)

	return nil
}

// AutoMigrate 自动迁移数据库表结构
func AutoMigrate(log *zap.Logger) error {
	if DB == nil {
		return fmt.Errorf("database not initialized")
	}

	log.Info("Starting database migration...")

	// 按依赖顺序迁移表
	err := DB.AutoMigrate(
		&model.User{},
		&model.Project{},
		&model.Repository{},
		&model.BuildRecord{},
		&model.QualityReport{},
	)

	if err != nil {
		return fmt.Errorf("failed to migrate database: %w", err)
	}

	log.Info("Database migration completed successfully")
	return nil
}

// Close 关闭数据库连接
func Close() error {
	if DB == nil {
		return nil
	}

	sqlDB, err := DB.DB()
	if err != nil {
		return err
	}

	return sqlDB.Close()
}

// GetDB 获取数据库实例
func GetDB() *gorm.DB {
	return DB
}
