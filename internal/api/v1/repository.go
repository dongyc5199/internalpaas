package v1

import (
	"net/http"
	"strconv"
	"strings"

	"code.gitea.io/sdk/gitea"
	"github.com/gin-gonic/gin"
	"github.com/spf13/viper"
	"go.uber.org/zap"
	"gorm.io/gorm"

	"github.com/yourorg/codehub/internal/model"
	"github.com/yourorg/codehub/internal/service"
)

// RepositoryHandler manages repository APIs.
type RepositoryHandler struct {
	db           *gorm.DB
	logger       *zap.Logger
	giteaService *service.GiteaService
	droneService *service.DroneService
}

// NewRepositoryHandler creates a new handler.
func NewRepositoryHandler(db *gorm.DB, logger *zap.Logger, giteaService *service.GiteaService, droneService *service.DroneService) *RepositoryHandler {
	return &RepositoryHandler{
		db:           db,
		logger:       logger,
		giteaService: giteaService,
		droneService: droneService,
	}
}

// CreateRepositoryRequest represents the payload for creating a repository.
type CreateRepositoryRequest struct {
	Name        string `json:"name" binding:"required,min=1,max=100"`
	Description string `json:"description"`
	IsPrivate   bool   `json:"is_private"`
	AutoInit    bool   `json:"auto_init"`
	GitIgnore   string `json:"gitignore"`
	License     string `json:"license"`
}

// UpdateCIRequest toggles CI.
type UpdateCIRequest struct {
	Enabled bool `json:"enabled" binding:"required"`
}

// ListRepositories returns repositories visible to the current user.
func (h *RepositoryHandler) ListRepositories(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	projectID := c.Query("project_id")
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))

	query := h.db.Model(&model.Repository{})

	if projectID != "" {
		pid, err := strconv.ParseUint(projectID, 10, 32)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "invalid project_id"})
			return
		}

		var project model.Project
		if err := h.db.Preload("Members").First(&project, pid).Error; err != nil {
			c.JSON(http.StatusNotFound, gin.H{"error": "project not found"})
			return
		}

		hasMember := false
		for _, member := range project.Members {
			if member.ID == userID.(uint) {
				hasMember = true
				break
			}
		}
		if !hasMember {
			c.JSON(http.StatusForbidden, gin.H{"error": "forbidden"})
			return
		}

		query = query.Where("project_id = ?", pid)
	} else {
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

	var total int64
	query.Count(&total)

	var repositories []model.Repository
	offset := (page - 1) * pageSize
	if err := query.Preload("Project").Offset(offset).Limit(pageSize).Order("updated_at DESC").Find(&repositories).Error; err != nil {
		h.logger.Error("Failed to query repositories", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "query repositories failed"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"repositories": repositories,
		"total":        total,
		"page":         page,
		"page_size":    pageSize,
	})
}

// GetRepository returns repository details.
func (h *RepositoryHandler) GetRepository(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid repository id"})
		return
	}

	var repository model.Repository
	if err := h.db.Preload("Project").Preload("Project.Members").First(&repository, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "repository not found"})
		} else {
			h.logger.Error("Failed to query repository", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "query repository failed"})
		}
		return
	}

	hasMember := false
	for _, member := range repository.Project.Members {
		if member.ID == userID.(uint) {
			hasMember = true
			break
		}
	}
	if !hasMember {
		c.JSON(http.StatusForbidden, gin.H{"error": "forbidden"})
		return
	}

	if repository.GiteaRepoID > 0 && h.giteaService != nil {
		if repo, err := h.giteaService.GetRepository(repository.Project.GiteaOrgName, repository.Name); err == nil {
			repository.StarCount = repo.Stars
			repository.ForkCount = repo.Forks
			repository.IssueCount = repo.OpenIssues
			repository.Size = int64(repo.Size)
		}
	}

	c.JSON(http.StatusOK, repository)
}

// CreateRepository creates a repository under a project.
func (h *RepositoryHandler) CreateRepository(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	projectID, err := strconv.ParseUint(c.Param("project_id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid project id"})
		return
	}

	var req CreateRepositoryRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request: " + err.Error()})
		return
	}

	var project model.Project
	if err := h.db.Preload("Members").First(&project, projectID).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "project not found"})
		} else {
			h.logger.Error("Failed to query project", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "query project failed"})
		}
		return
	}

	hasMember := false
	for _, member := range project.Members {
		if member.ID == userID.(uint) {
			hasMember = true
			break
		}
	}
	if !hasMember {
		c.JSON(http.StatusForbidden, gin.H{"error": "forbidden"})
		return
	}

	var existingRepo model.Repository
	if err := h.db.Where("project_id = ? AND name = ?", projectID, req.Name).First(&existingRepo).Error; err == nil {
		c.JSON(http.StatusConflict, gin.H{"error": "repository already exists"})
		return
	}

	var giteaRepoID int64
	var giteaCloneURL, giteaSshURL string

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
			h.logger.Error("Failed to create repository in Gitea", zap.Error(err), zap.String("org", project.GiteaOrgName), zap.String("repo", req.Name))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "create gitea repository failed: " + err.Error()})
			return
		}

		giteaRepoID = giteaRepo.ID
		giteaCloneURL = giteaRepo.CloneURL
		giteaSshURL = giteaRepo.SSHURL

		h.logger.Info("Repository created in Gitea", zap.Int64("gitea_repo_id", giteaRepoID), zap.String("clone_url", giteaCloneURL))
	}

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
		c.JSON(http.StatusInternalServerError, gin.H{"error": "create repository record failed"})
		return
	}

	h.ensureGiteaWebhook(project.GiteaOrgName, repository.Name)

	h.logger.Info("Repository created successfully", zap.Uint("repository_id", repository.ID), zap.String("name", repository.Name))

	c.JSON(http.StatusCreated, repository)
}

// UpdateCI enables or disables CI for a repository.
func (h *RepositoryHandler) UpdateCI(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid repository id"})
		return
	}

	var req UpdateCIRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid request: " + err.Error()})
		return
	}

	var repository model.Repository
	if err := h.db.Preload("Project").Preload("Project.Members").First(&repository, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "repository not found"})
		} else {
			h.logger.Error("Failed to query repository", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "query repository failed"})
		}
		return
	}

	hasMember := false
	for _, member := range repository.Project.Members {
		if member.ID == userID.(uint) {
			hasMember = true
			break
		}
	}
	if !hasMember {
		c.JSON(http.StatusForbidden, gin.H{"error": "forbidden"})
		return
	}

	if repository.DroneActive == req.Enabled {
		c.JSON(http.StatusOK, repository)
		return
	}

	owner := repository.Project.GiteaOrgName
	repoName := repository.Name

	if h.droneService != nil && repository.GiteaRepoID > 0 {
		if req.Enabled {
			droneRepo, err := h.droneService.EnableRepository(owner, repoName)
			if err != nil {
				h.logger.Error("Failed to enable CI in Drone", zap.Error(err), zap.String("owner", owner), zap.String("repo", repoName))
				c.JSON(http.StatusInternalServerError, gin.H{"error": "enable drone failed: " + err.Error()})
				return
			}
			repository.DroneRepoID = droneRepo.ID

			h.ensureGiteaWebhook(owner, repoName)

			h.logger.Info("CI enabled in Drone", zap.Int64("drone_repo_id", droneRepo.ID))
		} else {
			if err := h.droneService.DisableRepository(owner, repoName); err != nil {
				h.logger.Error("Failed to disable CI in Drone", zap.Error(err), zap.String("owner", owner), zap.String("repo", repoName))
				c.JSON(http.StatusInternalServerError, gin.H{"error": "disable drone failed: " + err.Error()})
				return
			}
			repository.DroneRepoID = 0
			h.logger.Info("CI disabled in Drone")
		}
	}

	repository.DroneActive = req.Enabled
	if err := h.db.Save(&repository).Error; err != nil {
		h.logger.Error("Failed to update repository", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "update repository failed"})
		return
	}

	c.JSON(http.StatusOK, repository)
}

// GetBranches lists branches of a repository.
func (h *RepositoryHandler) GetBranches(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid repository id"})
		return
	}

	var repository model.Repository
	if err := h.db.Preload("Project").Preload("Project.Members").First(&repository, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "repository not found"})
		} else {
			h.logger.Error("Failed to query repository", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "query repository failed"})
		}
		return
	}

	hasMember := false
	for _, member := range repository.Project.Members {
		if member.ID == userID.(uint) {
			hasMember = true
			break
		}
	}
	if !hasMember {
		c.JSON(http.StatusForbidden, gin.H{"error": "forbidden"})
		return
	}

	if h.giteaService == nil || repository.GiteaRepoID == 0 {
		c.JSON(http.StatusOK, gin.H{"branches": []string{}})
		return
	}

	branches, err := h.giteaService.ListBranches(repository.Project.GiteaOrgName, repository.Name)
	if err != nil {
		h.logger.Error("Failed to list branches from Gitea", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "list branches failed"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"branches": branches})
}

// GetCommits returns commit history.
func (h *RepositoryHandler) GetCommits(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "unauthorized"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid repository id"})
		return
	}

	branch := c.DefaultQuery("branch", "")
	page, _ := strconv.Atoi(c.DefaultQuery("page", "1"))
	pageSize, _ := strconv.Atoi(c.DefaultQuery("page_size", "20"))

	var repository model.Repository
	if err := h.db.Preload("Project").Preload("Project.Members").First(&repository, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "repository not found"})
		} else {
			h.logger.Error("Failed to query repository", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "query repository failed"})
		}
		return
	}

	hasMember := false
	for _, member := range repository.Project.Members {
		if member.ID == userID.(uint) {
			hasMember = true
			break
		}
	}
	if !hasMember {
		c.JSON(http.StatusForbidden, gin.H{"error": "forbidden"})
		return
	}

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
		c.JSON(http.StatusInternalServerError, gin.H{"error": "list commits failed"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"commits":   commits,
		"page":      page,
		"page_size": pageSize,
	})
}

// ensureGiteaWebhook creates a default push webhook for a repository if missing.
func (h *RepositoryHandler) ensureGiteaWebhook(owner, repo string) {
	if h.giteaService == nil || owner == "" || repo == "" {
		return
	}

	webhookURL := h.buildWebhookURL()
	if webhookURL == "" {
		return
	}

	secret := viper.GetString("gitea.webhook_secret")

	if hooks, err := h.giteaService.ListWebhooks(owner, repo); err == nil {
		for _, hook := range hooks {
			if hook.Config["url"] == webhookURL {
				return
			}
		}
	}

	hookOption := gitea.CreateHookOption{
		Type: "gitea",
		Config: map[string]string{
			"url":          webhookURL,
			"content_type": "json",
		},
		Events: []string{"push"},
		Active: true,
	}

	if secret != "" {
		hookOption.Config["secret"] = secret
	}

	if _, err := h.giteaService.CreateWebhook(owner, repo, hookOption); err != nil {
		h.logger.Warn("Failed to create default webhook in Gitea", zap.Error(err), zap.String("repo", repo))
	}
}

// buildWebhookURL builds the webhook endpoint reachable by Gitea.
func (h *RepositoryHandler) buildWebhookURL() string {
	base := viper.GetString("server.webhook_base_url")
	if base == "" {
		base = "http://localhost:8880"
	}
	return strings.TrimSuffix(base, "/") + "/api/v1/webhooks/gitea"
}
