package v1

import (
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
	"gorm.io/gorm"

	"github.com/yourorg/codehub/internal/model"
	"github.com/yourorg/codehub/internal/service"
	"github.com/yourorg/codehub/internal/websocket"
)

// BuildHandler 构建管理处理器
type BuildHandler struct {
	db            *gorm.DB
	logger        *zap.Logger
	droneService  *service.DroneService
	giteaService  *service.GiteaService
	wsHub         *websocket.Hub
}

// NewBuildHandler 创建构建处理器
func NewBuildHandler(db *gorm.DB, logger *zap.Logger, droneService *service.DroneService, giteaService *service.GiteaService, wsHub *websocket.Hub) *BuildHandler {
	return &BuildHandler{
		db:           db,
		logger:       logger,
		droneService: droneService,
		giteaService: giteaService,
		wsHub:        wsHub,
	}
}

// TriggerBuildRequest 触发构建请求
type TriggerBuildRequest struct {
	Branch string            `json:"branch" binding:"required"`
	Commit string            `json:"commit"` // 可选，不指定则使用最新提交
	Params map[string]string `json:"params"` // 构建参数
}

// ListBuilds 列出构建记录
// GET /api/v1/repositories/:repo_id/builds
func (h *BuildHandler) ListBuilds(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	repoID, err := strconv.ParseUint(c.Param("repo_id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的仓库ID"})
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
	if !h.hasRepositoryAccess(userID.(uint), &repository) {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权访问该仓库"})
		return
	}

	// 查询参数
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))
	status := c.Query("status")
	branch := c.Query("branch")

	query := h.db.Model(&model.BuildRecord{}).Where("repository_id = ?", repoID)

	if status != "" {
		query = query.Where("status = ?", status)
	}

	if branch != "" {
		query = query.Where("branch = ?", branch)
	}

	// 计算总数
	var total int64
	query.Count(&total)

	// 分页查询
	var builds []model.BuildRecord
	offset := (page - 1) * pageSize
	if err := query.
		Preload("TriggerUser").
		Offset(offset).
		Limit(pageSize).
		Order("created_at DESC").
		Find(&builds).Error; err != nil {
		h.logger.Error("Failed to query builds", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "查询构建记录失败"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"builds":    builds,
		"total":     total,
		"page":      page,
		"page_size": pageSize,
	})
}

// GetBuild 获取构建详情
// GET /api/v1/builds/:id
func (h *BuildHandler) GetBuild(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的构建ID"})
		return
	}

	var build model.BuildRecord
	if err := h.db.
		Preload("Repository.Project.Members").
		Preload("TriggerUser").
		Preload("QualityReport").
		First(&build, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "构建记录不存在"})
		} else {
			h.logger.Error("Failed to query build", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询构建记录失败"})
		}
		return
	}

	// 检查用户权限
	if !h.hasRepositoryAccess(userID.(uint), &build.Repository) {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权访问该构建记录"})
		return
	}

	c.JSON(http.StatusOK, build)
}

// TriggerBuild 触发新构建
// POST /api/v1/repositories/:repo_id/builds
func (h *BuildHandler) TriggerBuild(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	repoID, err := strconv.ParseUint(c.Param("repo_id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的仓库ID"})
		return
	}

	var req TriggerBuildRequest
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
	if !h.hasRepositoryAccess(userID.(uint), &repository) {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权操作该仓库"})
		return
	}

	// 检查CI是否已启用
	if !repository.DroneActive {
		c.JSON(http.StatusBadRequest, gin.H{"error": "该仓库未启用CI"})
		return
	}

	// 如果没有指定commit，从Gitea获取最新提交
	commitSHA := req.Commit
	if commitSHA == "" && h.giteaService != nil {
		commits, err := h.giteaService.ListCommits(
			repository.Project.GiteaOrgName,
			repository.Name,
			req.Branch,
			1,
			1,
		)
		if err != nil || len(commits) == 0 {
			h.logger.Error("Failed to get latest commit", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "获取最新提交失败"})
			return
		}
		commitSHA = commits[0].SHA
	}

	// 触发Drone构建
	if h.droneService == nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "Drone服务不可用"})
		return
	}

	owner := repository.Project.GiteaOrgName
	repoName := repository.Name

	droneBuild, err := h.droneService.TriggerBuild(owner, repoName, req.Branch, commitSHA, req.Params)
	if err != nil {
		h.logger.Error("Failed to trigger build in Drone",
			zap.Error(err),
			zap.String("owner", owner),
			zap.String("repo", repoName),
			zap.String("branch", req.Branch),
			zap.String("commit", commitSHA),
		)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "触发构建失败: " + err.Error()})
		return
	}

	// 创建构建记录
	build := model.BuildRecord{
		RepositoryID:     uint(repoID),
		TriggerUserID:    userID.(uint),
		DroneBuildID:     droneBuild.ID,
		DroneBuildNumber: int(droneBuild.Number),
		DroneLink:        droneBuild.Link,
		Status:           model.BuildStatus(droneBuild.Status),
		Trigger:          model.TriggerManual,
		Branch:           req.Branch,
		Commit:           commitSHA,
		Event:            droneBuild.Event,
		CommitMessage:    droneBuild.Message,
	}

	if droneBuild.Started > 0 {
		startTime := time.Unix(droneBuild.Started, 0)
		build.StartedAt = &startTime
	}

	if err := h.db.Create(&build).Error; err != nil {
		h.logger.Error("Failed to create build record", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "创建构建记录失败"})
		return
	}

	h.logger.Info("Build triggered successfully",
		zap.Uint("build_id", build.ID),
		zap.Int64("drone_build_id", droneBuild.ID),
		zap.String("branch", req.Branch),
		zap.String("commit", commitSHA),
	)

	// 通过WebSocket推送构建状态
	if h.wsHub != nil {
		h.wsHub.BroadcastBuildStatus(build.ID, string(build.Status), map[string]interface{}{
			"build_id":     build.ID,
			"repository_id": build.RepositoryID,
			"branch":       build.Branch,
			"commit":       build.Commit,
			"trigger":      string(build.Trigger),
		})
	}

	c.JSON(http.StatusCreated, build)
}

// RestartBuild 重启构建
// POST /api/v1/builds/:id/restart
func (h *BuildHandler) RestartBuild(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的构建ID"})
		return
	}

	// 查询构建记录
	var build model.BuildRecord
	if err := h.db.
		Preload("Repository.Project.Members").
		First(&build, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "构建记录不存在"})
		} else {
			h.logger.Error("Failed to query build", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询构建记录失败"})
		}
		return
	}

	// 检查用户权限
	if !h.hasRepositoryAccess(userID.(uint), &build.Repository) {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权操作该构建"})
		return
	}

	// 重启Drone构建
	if h.droneService == nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "Drone服务不可用"})
		return
	}

	owner := build.Repository.Project.GiteaOrgName
	repoName := build.Repository.Name

	droneBuild, err := h.droneService.RestartBuild(owner, repoName, build.DroneBuildNumber)
	if err != nil {
		h.logger.Error("Failed to restart build in Drone",
			zap.Error(err),
			zap.String("owner", owner),
			zap.String("repo", repoName),
			zap.Int("build_number", build.DroneBuildNumber),
		)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "重启构建失败: " + err.Error()})
		return
	}

	// 创建新的构建记录
	newBuild := model.BuildRecord{
		RepositoryID:     build.RepositoryID,
		TriggerUserID:    userID.(uint),
		DroneBuildID:     droneBuild.ID,
		DroneBuildNumber: int(droneBuild.Number),
		DroneLink:        droneBuild.Link,
		Status:           model.BuildStatus(droneBuild.Status),
		Trigger:          model.TriggerRestart,
		Branch:           build.Branch,
		Commit:           build.Commit,
		Event:            droneBuild.Event,
		CommitMessage:    "Restarted from build #" + strconv.Itoa(build.DroneBuildNumber),
	}

	if droneBuild.Started > 0 {
		startTime := time.Unix(droneBuild.Started, 0)
		newBuild.StartedAt = &startTime
	}

	if err := h.db.Create(&newBuild).Error; err != nil {
		h.logger.Error("Failed to create build record", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "创建构建记录失败"})
		return
	}

	h.logger.Info("Build restarted successfully",
		zap.Uint("new_build_id", newBuild.ID),
		zap.Uint("original_build_id", build.ID),
	)

	c.JSON(http.StatusCreated, newBuild)
}

// CancelBuild 取消构建
// POST /api/v1/builds/:id/cancel
func (h *BuildHandler) CancelBuild(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的构建ID"})
		return
	}

	// 查询构建记录
	var build model.BuildRecord
	if err := h.db.
		Preload("Repository.Project.Members").
		First(&build, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "构建记录不存在"})
		} else {
			h.logger.Error("Failed to query build", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询构建记录失败"})
		}
		return
	}

	// 检查用户权限
	if !h.hasRepositoryAccess(userID.(uint), &build.Repository) {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权操作该构建"})
		return
	}

	// 检查构建状态
	if build.Status != model.BuildStatusPending && build.Status != model.BuildStatusRunning {
		c.JSON(http.StatusBadRequest, gin.H{"error": "只能取消进行中的构建"})
		return
	}

	// 取消Drone构建
	if h.droneService == nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "Drone服务不可用"})
		return
	}

	owner := build.Repository.Project.GiteaOrgName
	repoName := build.Repository.Name

	if err := h.droneService.CancelBuild(owner, repoName, build.DroneBuildNumber); err != nil {
		h.logger.Error("Failed to cancel build in Drone",
			zap.Error(err),
			zap.String("owner", owner),
			zap.String("repo", repoName),
			zap.Int("build_number", build.DroneBuildNumber),
		)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "取消构建失败: " + err.Error()})
		return
	}

	// 更新构建状态
	build.Status = model.BuildStatusKilled
	now := time.Now()
	build.FinishedAt = &now

	if err := h.db.Save(&build).Error; err != nil {
		h.logger.Error("Failed to update build record", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "更新构建记录失败"})
		return
	}

	h.logger.Info("Build cancelled successfully", zap.Uint("build_id", build.ID))

	c.JSON(http.StatusOK, build)
}

// GetBuildLogs 获取构建日志
// GET /api/v1/builds/:id/logs
func (h *BuildHandler) GetBuildLogs(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的构建ID"})
		return
	}

	// 查询构建记录
	var build model.BuildRecord
	if err := h.db.
		Preload("Repository.Project.Members").
		First(&build, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "构建记录不存在"})
		} else {
			h.logger.Error("Failed to query build", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询构建记录失败"})
		}
		return
	}

	// 检查用户权限
	if !h.hasRepositoryAccess(userID.(uint), &build.Repository) {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权访问该构建日志"})
		return
	}

	// 获取Drone日志
	if h.droneService == nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "Drone服务不可用"})
		return
	}

	owner := build.Repository.Project.GiteaOrgName
	repoName := build.Repository.Name

	// TODO: 需要知道具体的stage和step信息
	// 这里假设使用第一个stage和第一个step
	// 实际使用时可能需要从查询参数中获取
	stage := c.DefaultQuery("stage", "1")
	step := c.DefaultQuery("step", "1")

	stageNum, _ := strconv.Atoi(stage)
	stepNum, _ := strconv.Atoi(step)

	logs, err := h.droneService.GetBuildLogs(owner, repoName, build.DroneBuildNumber, stageNum, stepNum)
	if err != nil {
		h.logger.Error("Failed to get build logs from Drone",
			zap.Error(err),
			zap.String("owner", owner),
			zap.String("repo", repoName),
			zap.Int("build_number", build.DroneBuildNumber),
		)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "获取构建日志失败: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"logs":         logs,
		"build_id":     build.ID,
		"build_number": build.DroneBuildNumber,
		"stage":        stageNum,
		"step":         stepNum,
	})
}

// GetBuildStats 获取构建统计信息
// GET /api/v1/repositories/:repo_id/builds/stats
func (h *BuildHandler) GetBuildStats(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	repoID, err := strconv.ParseUint(c.Param("repo_id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的仓库ID"})
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
	if !h.hasRepositoryAccess(userID.(uint), &repository) {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权访问该仓库"})
		return
	}

	// 统计各状态的构建数量
	type StatusCount struct {
		Status string `json:"status"`
		Count  int64  `json:"count"`
	}

	var statusCounts []StatusCount
	h.db.Model(&model.BuildRecord{}).
		Where("repository_id = ?", repoID).
		Select("status, count(*) as count").
		Group("status").
		Scan(&statusCounts)

	// 计算总构建时间和平均构建时间
	var totalDuration int64
	var buildCount int64

	h.db.Model(&model.BuildRecord{}).
		Where("repository_id = ? AND duration > 0", repoID).
		Select("SUM(duration) as total_duration, COUNT(*) as build_count").
		Row().
		Scan(&totalDuration, &buildCount)

	avgDuration := int64(0)
	if buildCount > 0 {
		avgDuration = totalDuration / buildCount
	}

	// 获取最近的构建
	var recentBuilds []model.BuildRecord
	h.db.Model(&model.BuildRecord{}).
		Where("repository_id = ?", repoID).
		Order("created_at DESC").
		Limit(10).
		Find(&recentBuilds)

	c.JSON(http.StatusOK, gin.H{
		"status_counts":         statusCounts,
		"total_duration":        totalDuration,
		"average_duration":      avgDuration,
		"recent_builds":         recentBuilds,
		"total_builds":          buildCount,
	})
}

// hasRepositoryAccess 检查用户是否有仓库访问权限
func (h *BuildHandler) hasRepositoryAccess(userID uint, repository *model.Repository) bool {
	for _, member := range repository.Project.Members {
		if member.ID == userID {
			return true
		}
	}
	return false
}
