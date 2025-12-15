package v1

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/spf13/viper"
	"go.uber.org/zap"
	"gorm.io/gorm"

	"github.com/yourorg/codehub/internal/model"
)

// WebhookHandler Webhook处理器
type WebhookHandler struct {
	db     *gorm.DB
	logger *zap.Logger
}

// NewWebhookHandler 创建Webhook处理器
func NewWebhookHandler(db *gorm.DB, logger *zap.Logger) *WebhookHandler {
	return &WebhookHandler{
		db:     db,
		logger: logger,
	}
}

// ===========================================
// Gitea Webhook处理
// ===========================================

// GiteaPushPayload Gitea推送事件载荷
type GiteaPushPayload struct {
	Ref        string `json:"ref"`
	Before     string `json:"before"`
	After      string `json:"after"`
	CompareURL string `json:"compare_url"`
	Repository struct {
		ID       int64  `json:"id"`
		Name     string `json:"name"`
		FullName string `json:"full_name"`
		Owner    struct {
			Username string `json:"username"`
		} `json:"owner"`
	} `json:"repository"`
	Pusher struct {
		Username string `json:"username"`
		Email    string `json:"email"`
	} `json:"pusher"`
	Sender struct {
		Username string `json:"username"`
	} `json:"sender"`
	Commits []struct {
		ID      string `json:"id"`
		Message string `json:"message"`
		Author  struct {
			Name     string `json:"name"`
			Email    string `json:"email"`
			Username string `json:"username"`
		} `json:"author"`
		Timestamp time.Time `json:"timestamp"`
	} `json:"commits"`
}

// GiteaWebhook 处理Gitea Webhook
// POST /api/v1/webhooks/gitea
func (h *WebhookHandler) GiteaWebhook(c *gin.Context) {
	// 验证签名
	if !h.verifyGiteaSignature(c) {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "无效的签名"})
		return
	}

	// 获取事件类型
	event := c.GetHeader("X-Gitea-Event")
	if event == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "缺少事件类型"})
		return
	}

	// 读取请求体
	body, err := io.ReadAll(c.Request.Body)
	if err != nil {
		h.logger.Error("Failed to read webhook body", zap.Error(err))
		c.JSON(http.StatusBadRequest, gin.H{"error": "读取请求体失败"})
		return
	}

	h.logger.Info("Received Gitea webhook",
		zap.String("event", event),
		zap.String("body", string(body)),
	)

	switch event {
	case "push":
		h.handleGiteaPush(c, body)
	case "pull_request":
		// TODO: 处理PR事件
		c.JSON(http.StatusOK, gin.H{"message": "PR事件待实现"})
	case "create":
		// TODO: 处理Tag创建事件
		c.JSON(http.StatusOK, gin.H{"message": "创建事件待实现"})
	default:
		h.logger.Info("Unhandled Gitea event", zap.String("event", event))
		c.JSON(http.StatusOK, gin.H{"message": "事件已接收但未处理"})
	}
}

// handleGiteaPush 处理Gitea推送事件
func (h *WebhookHandler) handleGiteaPush(c *gin.Context, body []byte) {
	var payload GiteaPushPayload
	if err := json.Unmarshal(body, &payload); err != nil {
		h.logger.Error("Failed to parse push payload", zap.Error(err))
		c.JSON(http.StatusBadRequest, gin.H{"error": "解析载荷失败"})
		return
	}

	// 查找对应的仓库
	var repository model.Repository
	if err := h.db.Where("gitea_repo_id = ?", payload.Repository.ID).First(&repository).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			h.logger.Warn("Repository not found for Gitea webhook",
				zap.Int64("gitea_repo_id", payload.Repository.ID),
				zap.String("repo_name", payload.Repository.FullName),
			)
			c.JSON(http.StatusNotFound, gin.H{"error": "仓库未找到"})
		} else {
			h.logger.Error("Failed to query repository", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询仓库失败"})
		}
		return
	}

	// 检查CI是否已启用
	if !repository.DroneActive {
		h.logger.Info("CI not enabled for repository, skipping",
			zap.Uint("repository_id", repository.ID),
			zap.String("repo_name", repository.Name),
		)
		c.JSON(http.StatusOK, gin.H{"message": "CI未启用，已忽略"})
		return
	}

	// 解析分支名称
	branch := strings.TrimPrefix(payload.Ref, "refs/heads/")

	h.logger.Info("Gitea push event processed",
		zap.Uint("repository_id", repository.ID),
		zap.String("branch", branch),
		zap.String("commit", payload.After),
	)

	// Drone会自动触发构建，这里只记录日志
	// 实际的构建记录会在Drone webhook中创建

	c.JSON(http.StatusOK, gin.H{
		"message":       "推送事件已处理",
		"repository_id": repository.ID,
		"branch":        branch,
		"commit":        payload.After,
	})
}

// verifyGiteaSignature 验证Gitea Webhook签名
func (h *WebhookHandler) verifyGiteaSignature(c *gin.Context) bool {
	secret := viper.GetString("gitea.webhook_secret")
	if secret == "" {
		// 如果未配置密钥，跳过验证（开发环境）
		h.logger.Warn("Gitea webhook secret not configured, skipping signature verification")
		return true
	}

	signature := c.GetHeader("X-Gitea-Signature")
	if signature == "" {
		h.logger.Warn("Missing Gitea signature header")
		return false
	}

	// 读取请求体
	body, err := io.ReadAll(c.Request.Body)
	if err != nil {
		h.logger.Error("Failed to read body for signature verification", zap.Error(err))
		return false
	}

	// 重置请求体，以便后续处理可以再次读取
	c.Request.Body = io.NopCloser(strings.NewReader(string(body)))

	// 计算HMAC-SHA256
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write(body)
	expectedSignature := hex.EncodeToString(mac.Sum(nil))

	return hmac.Equal([]byte(signature), []byte(expectedSignature))
}

// ===========================================
// Drone Webhook处理
// ===========================================

// DroneBuildPayload Drone构建事件载荷
type DroneBuildPayload struct {
	Action string `json:"action"` // created, updated, finished
	Repo   struct {
		ID       int64  `json:"id"`
		Name     string `json:"name"`
		Slug     string `json:"slug"`
		Owner    string `json:"namespace"`
	} `json:"repo"`
	Build struct {
		ID           int64     `json:"id"`
		Number       int       `json:"number"`
		Status       string    `json:"status"`
		Event        string    `json:"event"`
		Action       string    `json:"action"`
		Link         string    `json:"link"`
		Message      string    `json:"message"`
		Branch       string    `json:"source"`
		Ref          string    `json:"ref"`
		After        string    `json:"after"`
		Before       string    `json:"before"`
		Target       string    `json:"target"`
		Author       string    `json:"author_name"`
		AuthorEmail  string    `json:"author_email"`
		AuthorAvatar string    `json:"author_avatar"`
		Sender       string    `json:"sender"`
		Started      int64     `json:"started"`
		Finished     int64     `json:"finished"`
		Created      time.Time `json:"created"`
		Updated      time.Time `json:"updated"`
	} `json:"build"`
}

// DroneWebhook 处理Drone Webhook
// POST /api/v1/webhooks/drone
func (h *WebhookHandler) DroneWebhook(c *gin.Context) {
	// 验证签名
	if !h.verifyDroneSignature(c) {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "无效的签名"})
		return
	}

	// 读取请求体
	body, err := io.ReadAll(c.Request.Body)
	if err != nil {
		h.logger.Error("Failed to read webhook body", zap.Error(err))
		c.JSON(http.StatusBadRequest, gin.H{"error": "读取请求体失败"})
		return
	}

	var payload DroneBuildPayload
	if err := json.Unmarshal(body, &payload); err != nil {
		h.logger.Error("Failed to parse Drone payload", zap.Error(err))
		c.JSON(http.StatusBadRequest, gin.H{"error": "解析载荷失败"})
		return
	}

	h.logger.Info("Received Drone webhook",
		zap.String("action", payload.Action),
		zap.Int64("build_id", payload.Build.ID),
		zap.Int("build_number", payload.Build.Number),
		zap.String("status", payload.Build.Status),
		zap.String("repo", payload.Repo.Slug),
	)

	// 查找对应的仓库
	var repository model.Repository
	if err := h.db.Where("drone_repo_id = ?", payload.Repo.ID).First(&repository).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			h.logger.Warn("Repository not found for Drone webhook",
				zap.Int64("drone_repo_id", payload.Repo.ID),
				zap.String("repo_slug", payload.Repo.Slug),
			)
			c.JSON(http.StatusNotFound, gin.H{"error": "仓库未找到"})
		} else {
			h.logger.Error("Failed to query repository", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询仓库失败"})
		}
		return
	}

	// 处理构建状态更新
	switch payload.Action {
	case "created":
		h.handleDroneBuildCreated(c, &repository, &payload)
	case "updated", "finished":
		h.handleDroneBuildUpdated(c, &repository, &payload)
	default:
		h.logger.Info("Unhandled Drone action", zap.String("action", payload.Action))
		c.JSON(http.StatusOK, gin.H{"message": "操作已接收但未处理"})
	}
}

// handleDroneBuildCreated 处理Drone构建创建事件
func (h *WebhookHandler) handleDroneBuildCreated(c *gin.Context, repository *model.Repository, payload *DroneBuildPayload) {
	// 检查构建记录是否已存在（可能由API手动触发时已创建）
	var existingBuild model.BuildRecord
	err := h.db.Where("drone_build_id = ?", payload.Build.ID).First(&existingBuild).Error

	if err == nil {
		// 构建记录已存在，更新状态
		h.logger.Info("Build record already exists, updating",
			zap.Uint("build_id", existingBuild.ID),
			zap.Int64("drone_build_id", payload.Build.ID),
		)
		h.updateBuildRecord(&existingBuild, payload)
		c.JSON(http.StatusOK, gin.H{
			"message":  "构建记录已更新",
			"build_id": existingBuild.ID,
		})
		return
	}

	if err != gorm.ErrRecordNotFound {
		h.logger.Error("Failed to query build record", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "查询构建记录失败"})
		return
	}

	// 创建新的构建记录
	trigger := h.mapDroneEventToTrigger(payload.Build.Event)

	build := model.BuildRecord{
		RepositoryID:     repository.ID,
		DroneBuildID:     payload.Build.ID,
		DroneBuildNumber: payload.Build.Number,
		DroneLink:        payload.Build.Link,
		Status:           model.BuildStatus(payload.Build.Status),
		Trigger:          trigger,
		Event:            payload.Build.Event,
		Branch:           payload.Build.Branch,
		Commit:           payload.Build.After,
		Ref:              payload.Build.Ref,
		CommitMessage:    payload.Build.Message,
		CommitAuthor:     payload.Build.Author,
		CommitEmail:      payload.Build.AuthorEmail,
	}

	if payload.Build.Started > 0 {
		startTime := time.Unix(payload.Build.Started, 0)
		build.StartedAt = &startTime
	}

	if err := h.db.Create(&build).Error; err != nil {
		h.logger.Error("Failed to create build record", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "创建构建记录失败"})
		return
	}

	h.logger.Info("Build record created from webhook",
		zap.Uint("build_id", build.ID),
		zap.Int64("drone_build_id", payload.Build.ID),
		zap.String("status", payload.Build.Status),
	)

	c.JSON(http.StatusCreated, gin.H{
		"message":  "构建记录已创建",
		"build_id": build.ID,
	})
}

// handleDroneBuildUpdated 处理Drone构建更新事件
func (h *WebhookHandler) handleDroneBuildUpdated(c *gin.Context, repository *model.Repository, payload *DroneBuildPayload) {
	// 查找构建记录
	var build model.BuildRecord
	if err := h.db.Where("drone_build_id = ?", payload.Build.ID).First(&build).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			// 构建记录不存在，创建新记录（处理webhook丢失的情况）
			h.logger.Warn("Build record not found, creating new one",
				zap.Int64("drone_build_id", payload.Build.ID),
			)
			h.handleDroneBuildCreated(c, repository, payload)
			return
		}

		h.logger.Error("Failed to query build record", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "查询构建记录失败"})
		return
	}

	// 更新构建记录
	h.updateBuildRecord(&build, payload)

	h.logger.Info("Build record updated from webhook",
		zap.Uint("build_id", build.ID),
		zap.Int64("drone_build_id", payload.Build.ID),
		zap.String("status", payload.Build.Status),
	)

	c.JSON(http.StatusOK, gin.H{
		"message":  "构建记录已更新",
		"build_id": build.ID,
	})
}

// updateBuildRecord 更新构建记录
func (h *WebhookHandler) updateBuildRecord(build *model.BuildRecord, payload *DroneBuildPayload) {
	build.Status = model.BuildStatus(payload.Build.Status)

	if payload.Build.Started > 0 {
		startTime := time.Unix(payload.Build.Started, 0)
		build.StartedAt = &startTime
	}

	if payload.Build.Finished > 0 {
		finishTime := time.Unix(payload.Build.Finished, 0)
		build.FinishedAt = &finishTime

		// 计算构建时长
		if build.StartedAt != nil {
			build.Duration = int(finishTime.Sub(*build.StartedAt).Seconds())
		}
	}

	if err := h.db.Save(build).Error; err != nil {
		h.logger.Error("Failed to update build record", zap.Error(err))
	}
}

// mapDroneEventToTrigger 映射Drone事件类型到触发器类型
func (h *WebhookHandler) mapDroneEventToTrigger(event string) model.BuildTrigger {
	switch event {
	case "push":
		return model.TriggerPush
	case "pull_request":
		return model.TriggerPR
	case "tag":
		return model.TriggerTag
	case "cron":
		return model.TriggerCron
	default:
		return model.TriggerManual
	}
}

// verifyDroneSignature 验证Drone Webhook签名
func (h *WebhookHandler) verifyDroneSignature(c *gin.Context) bool {
	secret := viper.GetString("drone.webhook_secret")
	if secret == "" {
		// 如果未配置密钥，跳过验证（开发环境）
		h.logger.Warn("Drone webhook secret not configured, skipping signature verification")
		return true
	}

	signature := c.GetHeader("X-Drone-Signature")
	if signature == "" {
		h.logger.Warn("Missing Drone signature header")
		return false
	}

	// 读取请求体
	body, err := io.ReadAll(c.Request.Body)
	if err != nil {
		h.logger.Error("Failed to read body for signature verification", zap.Error(err))
		return false
	}

	// 重置请求体
	c.Request.Body = io.NopCloser(strings.NewReader(string(body)))

	// 计算HMAC-SHA256
	mac := hmac.New(sha256.New, []byte(secret))
	mac.Write(body)
	expectedSignature := hex.EncodeToString(mac.Sum(nil))

	return hmac.Equal([]byte(signature), []byte(expectedSignature))
}

// ===========================================
// Webhook配置管理
// ===========================================

// ConfigureWebhookRequest 配置Webhook请求
type ConfigureWebhookRequest struct {
	Events []string `json:"events" binding:"required"` // 要监听的事件
}

// ConfigureRepositoryWebhook 为仓库配置Webhook
// POST /api/v1/repositories/:id/webhooks
func (h *WebhookHandler) ConfigureRepositoryWebhook(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	repoID, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的仓库ID"})
		return
	}

	var req ConfigureWebhookRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数错误: " + err.Error()})
		return
	}

	// 验证仓库访问权限
	var repository model.Repository
	if err := h.db.Preload("Project.Members").First(&repository, repoID).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "仓库不存在"})
		} else {
			h.logger.Error("Failed to query repository", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询仓库失败"})
		}
		return
	}

	// 检查用户权限
	hasAccess := false
	for _, member := range repository.Project.Members {
		if member.ID == userID.(uint) {
			hasAccess = true
			break
		}
	}

	if !hasAccess {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权操作该仓库"})
		return
	}

	// TODO: 调用GiteaService创建webhook
	// 这需要在GiteaService中实现CreateWebhook方法

	h.logger.Info("Webhook configuration requested",
		zap.Uint("repository_id", repository.ID),
		zap.Strings("events", req.Events),
	)

	c.JSON(http.StatusOK, gin.H{
		"message": "Webhook配置功能待实现",
		"events":  req.Events,
	})
}
