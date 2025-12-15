package v1

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
	"gorm.io/gorm"

	"github.com/yourorg/codehub/internal/service"
)

// ArtifactHandler 制品管理处理器
type ArtifactHandler struct {
	db           *gorm.DB
	logger       *zap.Logger
	nexusService *service.NexusService
}

// NewArtifactHandler 创建制品处理器
func NewArtifactHandler(db *gorm.DB, logger *zap.Logger, nexusService *service.NexusService) *ArtifactHandler {
	return &ArtifactHandler{
		db:           db,
		logger:       logger,
		nexusService: nexusService,
	}
}

// ListRepositories 列出Nexus仓库
// GET /api/v1/artifacts/repositories
func (h *ArtifactHandler) ListRepositories(c *gin.Context) {
	_, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	if h.nexusService == nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "Nexus服务不可用"})
		return
	}

	repos, err := h.nexusService.ListRepositories()
	if err != nil {
		h.logger.Error("Failed to list Nexus repositories", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "获取仓库列表失败: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"repositories": repos,
		"total":        len(repos),
	})
}

// SearchArtifacts 搜索制品
// GET /api/v1/artifacts/search
func (h *ArtifactHandler) SearchArtifacts(c *gin.Context) {
	_, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	if h.nexusService == nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "Nexus服务不可用"})
		return
	}

	repository := c.Query("repository")
	group := c.Query("group")
	name := c.Query("name")
	version := c.Query("version")

	components, err := h.nexusService.SearchComponents(repository, group, name, version)
	if err != nil {
		h.logger.Error("Failed to search components", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "搜索制品失败: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"components": components,
		"total":      len(components),
	})
}

// GetArtifact 获取制品详情
// GET /api/v1/artifacts/:id
func (h *ArtifactHandler) GetArtifact(c *gin.Context) {
	_, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	if h.nexusService == nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "Nexus服务不可用"})
		return
	}

	componentID := c.Param("id")

	component, err := h.nexusService.GetComponent(componentID)
	if err != nil {
		h.logger.Error("Failed to get component", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "获取制品详情失败: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, component)
}

// DeleteArtifact 删除制品
// DELETE /api/v1/artifacts/:id
func (h *ArtifactHandler) DeleteArtifact(c *gin.Context) {
	_, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	if h.nexusService == nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "Nexus服务不可用"})
		return
	}

	componentID := c.Param("id")

	if err := h.nexusService.DeleteComponent(componentID); err != nil {
		h.logger.Error("Failed to delete component", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "删除制品失败: " + err.Error()})
		return
	}

	h.logger.Info("Component deleted", zap.String("component_id", componentID))

	c.JSON(http.StatusOK, gin.H{"message": "制品删除成功"})
}

// DownloadArtifact 下载制品
// GET /api/v1/artifacts/assets/:id/download
func (h *ArtifactHandler) DownloadArtifact(c *gin.Context) {
	_, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	if h.nexusService == nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "Nexus服务不可用"})
		return
	}

	assetID := c.Param("id")
	savePath := c.Query("path")

	if savePath == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "缺少保存路径参数"})
		return
	}

	if err := h.nexusService.DownloadAsset(assetID, savePath); err != nil {
		h.logger.Error("Failed to download asset", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "下载制品失败: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "制品下载成功",
		"path":    savePath,
	})
}
