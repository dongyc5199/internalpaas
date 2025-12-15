package v1

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
	"gorm.io/gorm"

	"github.com/yourorg/codehub/internal/model"
	"github.com/yourorg/codehub/internal/service"
)

// RepositoryHandler 仓库管理处理器
type RepositoryHandler struct {
	db            *gorm.DB
	logger        *zap.Logger
	giteaService  *service.GiteaService
	droneService  *service.DroneService
}

// NewRepositoryHandler 创建仓库处理器
func NewRepositoryHandler(db *gorm.DB, logger *zap.Logger, giteaService *service.GiteaService, droneService *service.DroneService) *RepositoryHandler {
	return &RepositoryHandler{
		db:           db,
		logger:       logger,
		giteaService: giteaService,
		droneService: droneService,
	}
}

// CreateRepositoryRequest 创建仓库请求
type CreateRepositoryRequest struct {
	Name        string `json:"name" binding:"required,min=1,max=100"`
	Description string `json:"description"`
	IsPrivate   bool   `json:"is_private"`
	AutoInit    bool   `json:"auto_init"` // 是否自动初始化README
	GitIgnore   string `json:"gitignore"` // .gitignore模板
	License     string `json:"license"`   // 许可证类型
}

// UpdateCIRequest 更新CI配置请求
type UpdateCIRequest struct {
	Enabled bool `json:"enabled" binding:"required"`
}

// ListRepositories 列出仓库
// GET /api/v1/repositories
func (h *RepositoryHandler) ListRepositories(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	// 获取查询参数
	projectID := c.Query("project_id")
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))

	query := h.db.Model(&model.Repository{})

	// 如果指定了项目ID，只查询该项目的仓库
	if projectID != "" {
		pid, err := strconv.ParseUint(projectID, 10, 32)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "无效的项目ID"})
			return
		}

		// 检查用户是否是项目成员
		var project model.Project
		if err := h.db.Preload("Members").First(&project, pid).Error; err != nil {
			c.JSON(http.StatusNotFound, gin.H{"error": "项目不存在"})
			return
		}

		isMember := false
		for _, member := range project.Members {
			if member.ID == userID.(uint) {
				isMember = true
				break
			}
		}

		if !isMember {
			c.JSON(http.StatusForbidden, gin.H{"error": "无权访问该项目"})
			return
		}

		query = query.Where("project_id = ?", pid)
	} else {
		// 查询用户所有项目的仓库
		var projectIDs []uint
		h.db.Table("project_members").
			Where("user_id = ?", userID).
			Pluck("project_id", &projectIDs)

		if len(projectIDs) == 0 {
			c.JSON(http.StatusOK, gin.H{
				"repositories": []model.Repository{},
				"total":        0,
				"page":         page,
				"page_size":    pageSize,
			})
			return
		}

		query = query.Where("project_id IN ?", projectIDs)
	}

	// 计算总数
	var total int64
	query.Count(&total)

	// 分页查询
	var repositories []model.Repository
	offset := (page - 1) * pageSize
	if err := query.
		Preload("Project").
		Offset(offset).
		Limit(pageSize).
		Order("updated_at DESC").
		Find(&repositories).Error; err != nil {
		h.logger.Error("Failed to query repositories", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "查询仓库失败"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"repositories": repositories,
		"total":        total,
		"page":         page,
		"page_size":    pageSize,
	})
}

// GetRepository 获取仓库详情
// GET /api/v1/repositories/:id
func (h *RepositoryHandler) GetRepository(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的仓库ID"})
		return
	}

	var repository model.Repository
	if err := h.db.
		Preload("Project").
		Preload("Project.Members").
		First(&repository, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "仓库不存在"})
		} else {
			h.logger.Error("Failed to query repository", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询仓库失败"})
		}
		return
	}

	// 检查用户是否是项目成员
	isMember := false
	for _, member := range repository.Project.Members {
		if member.ID == userID.(uint) {
			isMember = true
			break
		}
	}

	if !isMember {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权访问该仓库"})
		return
	}

	// 如果仓库已同步到Gitea，获取最新统计信息
	if repository.GiteaRepoID > 0 && h.giteaService != nil {
		if repo, err := h.giteaService.GetRepository(repository.Project.GiteaOrgName, repository.Name); err == nil {
			// 更新统计信息
			repository.StarCount = repo.Stars
			repository.ForkCount = repo.Forks
			repository.IssueCount = repo.OpenIssues
			repository.Size = int64(repo.Size)
		}
	}

	c.JSON(http.StatusOK, repository)
}

// CreateRepository 创建仓库
// POST /api/v1/projects/:project_id/repositories
func (h *RepositoryHandler) CreateRepository(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	projectID, err := strconv.ParseUint(c.Param("project_id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的项目ID"})
		return
	}

	// 验证请求参数
	var req CreateRepositoryRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数错误: " + err.Error()})
		return
	}

	// 获取项目信息
	var project model.Project
	if err := h.db.Preload("Members").First(&project, projectID).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "项目不存在"})
		} else {
			h.logger.Error("Failed to query project", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询项目失败"})
		}
		return
	}

	// 检查用户是否是项目成员
	isMember := false
	for _, member := range project.Members {
		if member.ID == userID.(uint) {
			isMember = true
			break
		}
	}

	if !isMember {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权在该项目中创建仓库"})
		return
	}

	// 检查仓库名是否已存在
	var existingRepo model.Repository
	if err := h.db.Where("project_id = ? AND name = ?", projectID, req.Name).First(&existingRepo).Error; err == nil {
		c.JSON(http.StatusConflict, gin.H{"error": "仓库名称已存在"})
		return
	}

	// 在Gitea中创建仓库
	var giteaRepoID int64
	var giteaCloneURL string
	var giteaSshURL string

	if h.giteaService != nil {
		giteaRepo, err := h.giteaService.CreateRepositoryInOrg(
			project.GiteaOrgName,
			req.Name,
			req.Description,
			req.IsPrivate,
			req.AutoInit,
			req.GitIgnore,
			req.License,
		)
		if err != nil {
			h.logger.Error("Failed to create repository in Gitea",
				zap.Error(err),
				zap.String("org", project.GiteaOrgName),
				zap.String("repo", req.Name),
			)
			c.JSON(http.StatusInternalServerError, gin.H{"error": "创建Gitea仓库失败: " + err.Error()})
			return
		}

		giteaRepoID = giteaRepo.ID
		giteaCloneURL = giteaRepo.CloneURL
		giteaSshURL = giteaRepo.SSHURL

		h.logger.Info("Repository created in Gitea",
			zap.Int64("gitea_repo_id", giteaRepoID),
			zap.String("clone_url", giteaCloneURL),
		)
	}

	// 创建仓库记录
	repository := model.Repository{
		ProjectID:     uint(projectID),
		OwnerID:       userID.(uint),
		Name:          req.Name,
		FullName:      project.GiteaOrgName + "/" + req.Name,
		Description:   req.Description,
		IsPrivate:     req.IsPrivate,
		GiteaRepoID:   giteaRepoID,
		CloneURL:      giteaCloneURL,
		SSHURL:        giteaSshURL,
		DefaultBranch: "main",
		DroneActive:   false,
	}

	if err := h.db.Create(&repository).Error; err != nil {
		h.logger.Error("Failed to create repository record", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "创建仓库记录失败"})
		return
	}

	h.logger.Info("Repository created successfully",
		zap.Uint("repository_id", repository.ID),
		zap.String("name", repository.Name),
	)

	c.JSON(http.StatusCreated, repository)
}

// UpdateCI 启用/禁用CI
// PUT /api/v1/repositories/:id/ci
func (h *RepositoryHandler) UpdateCI(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的仓库ID"})
		return
	}

	var req UpdateCIRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数错误: " + err.Error()})
		return
	}

	// 获取仓库信息
	var repository model.Repository
	if err := h.db.
		Preload("Project").
		Preload("Project.Members").
		First(&repository, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "仓库不存在"})
		} else {
			h.logger.Error("Failed to query repository", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询仓库失败"})
		}
		return
	}

	// 检查用户是否是项目成员
	isMember := false
	for _, member := range repository.Project.Members {
		if member.ID == userID.(uint) {
			isMember = true
			break
		}
	}

	if !isMember {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权修改该仓库"})
		return
	}

	// 如果CI状态没有变化，直接返回
	if repository.DroneActive == req.Enabled {
		c.JSON(http.StatusOK, repository)
		return
	}

	// 同步到Drone CI
	if h.droneService != nil && repository.GiteaRepoID > 0 {
		owner := repository.Project.GiteaOrgName
		repoName := repository.Name

		if req.Enabled {
			// 启用CI
			droneRepo, err := h.droneService.EnableRepository(owner, repoName)
			if err != nil {
				h.logger.Error("Failed to enable CI in Drone",
					zap.Error(err),
					zap.String("owner", owner),
					zap.String("repo", repoName),
				)
				c.JSON(http.StatusInternalServerError, gin.H{"error": "启用Drone CI失败: " + err.Error()})
				return
			}

			repository.DroneRepoID = droneRepo.ID

			h.logger.Info("CI enabled in Drone",
				zap.Int64("drone_repo_id", droneRepo.ID),
			)
		} else {
			// 禁用CI
			if err := h.droneService.DisableRepository(owner, repoName); err != nil {
				h.logger.Error("Failed to disable CI in Drone",
					zap.Error(err),
					zap.String("owner", owner),
					zap.String("repo", repoName),
				)
				c.JSON(http.StatusInternalServerError, gin.H{"error": "禁用Drone CI失败: " + err.Error()})
				return
			}

			repository.DroneRepoID = 0

			h.logger.Info("CI disabled in Drone")
		}
	}

	// 更新仓库CI状态
	repository.DroneActive = req.Enabled
	if err := h.db.Save(&repository).Error; err != nil {
		h.logger.Error("Failed to update repository", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "更新仓库失败"})
		return
	}

	c.JSON(http.StatusOK, repository)
}

// GetBranches 获取仓库分支列表
// GET /api/v1/repositories/:id/branches
func (h *RepositoryHandler) GetBranches(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的仓库ID"})
		return
	}

	// 获取仓库信息
	var repository model.Repository
	if err := h.db.
		Preload("Project").
		Preload("Project.Members").
		First(&repository, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "仓库不存在"})
		} else {
			h.logger.Error("Failed to query repository", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询仓库失败"})
		}
		return
	}

	// 检查用户是否是项目成员
	isMember := false
	for _, member := range repository.Project.Members {
		if member.ID == userID.(uint) {
			isMember = true
			break
		}
	}

	if !isMember {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权访问该仓库"})
		return
	}

	// 从Gitea获取分支列表
	if h.giteaService == nil || repository.GiteaRepoID == 0 {
		c.JSON(http.StatusOK, gin.H{"branches": []string{}})
		return
	}

	branches, err := h.giteaService.ListBranches(repository.Project.GiteaOrgName, repository.Name)
	if err != nil {
		h.logger.Error("Failed to list branches from Gitea", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "获取分支列表失败"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"branches": branches})
}

// GetCommits 获取仓库提交历史
// GET /api/v1/repositories/:id/commits
func (h *RepositoryHandler) GetCommits(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的仓库ID"})
		return
	}

	branch := c.DefaultQuery("branch", "")
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))

	// 获取仓库信息
	var repository model.Repository
	if err := h.db.
		Preload("Project").
		Preload("Project.Members").
		First(&repository, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "仓库不存在"})
		} else {
			h.logger.Error("Failed to query repository", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询仓库失败"})
		}
		return
	}

	// 检查用户是否是项目成员
	isMember := false
	for _, member := range repository.Project.Members {
		if member.ID == userID.(uint) {
			isMember = true
			break
		}
	}

	if !isMember {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权访问该仓库"})
		return
	}

	// 从Gitea获取提交历史
	if h.giteaService == nil || repository.GiteaRepoID == 0 {
		c.JSON(http.StatusOK, gin.H{"commits": []interface{}{}})
		return
	}

	if branch == "" {
		branch = repository.DefaultBranch
	}

	commits, err := h.giteaService.ListCommits(
		repository.Project.GiteaOrgName,
		repository.Name,
		branch,
		page,
		pageSize,
	)
	if err != nil {
		h.logger.Error("Failed to list commits from Gitea", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "获取提交历史失败"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"commits":   commits,
		"page":      page,
		"page_size": pageSize,
	})
}
