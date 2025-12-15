package v1

import (
	"net/http"

	"code.gitea.io/sdk/gitea"
	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
	"gorm.io/gorm"

	"github.com/yourorg/codehub/internal/model"
	"github.com/yourorg/codehub/internal/service"
)

// ProjectHandler 项目处理器
type ProjectHandler struct {
	db           *gorm.DB
	logger       *zap.Logger
	giteaService *service.GiteaService
}

// NewProjectHandler 创建项目处理器
func NewProjectHandler(db *gorm.DB, logger *zap.Logger, giteaService *service.GiteaService) *ProjectHandler {
	return &ProjectHandler{
		db:           db,
		logger:       logger,
		giteaService: giteaService,
	}
}

// CreateProjectRequest 创建项目请求
type CreateProjectRequest struct {
	Name        string `json:"name" binding:"required,min=3,max=100"`
	DisplayName string `json:"display_name"`
	Description string `json:"description"`
	IsPrivate   bool   `json:"is_private"`
}

// UpdateProjectRequest 更新项目请求
type UpdateProjectRequest struct {
	DisplayName string `json:"display_name"`
	Description string `json:"description"`
	IsPrivate   bool   `json:"is_private"`
}

// ListProjects 列出所有项目
// @Summary 列出项目
// @Tags project
// @Security BearerAuth
// @Produce json
// @Success 200 {array} model.Project
// @Router /api/v1/projects [get]
func (h *ProjectHandler) ListProjects(c *gin.Context) {
	var projects []model.Project

	// 管理员可以看到所有项目
	isAdmin, _ := c.Get("is_admin")
	if isAdmin.(bool) {
		if err := h.db.Find(&projects).Error; err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{
				"error": "查询项目失败",
			})
			return
		}
	} else {
		// 普通用户只能看到自己的项目
		userID, _ := c.Get("user_id")
		if err := h.db.Joins("JOIN user_projects ON user_projects.project_id = projects.id").
			Where("user_projects.user_id = ?", userID).
			Find(&projects).Error; err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{
				"error": "查询项目失败",
			})
			return
		}
	}

	c.JSON(http.StatusOK, projects)
}

// GetProject 获取项目详情
// @Summary 获取项目
// @Tags project
// @Security BearerAuth
// @Param id path int true "项目ID"
// @Produce json
// @Success 200 {object} model.Project
// @Router /api/v1/projects/{id} [get]
func (h *ProjectHandler) GetProject(c *gin.Context) {
	projectID := c.Param("id")

	var project model.Project
	if err := h.db.Preload("Members").Preload("Repositories").First(&project, projectID).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{
				"error": "项目不存在",
			})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "查询项目失败",
		})
		return
	}

	// 检查权限
	userID, _ := c.Get("user_id")
	isAdmin, _ := c.Get("is_admin")

	if !isAdmin.(bool) {
		// 检查用户是否是项目成员
		var count int64
		h.db.Model(&model.User{}).
			Joins("JOIN user_projects ON user_projects.user_id = users.id").
			Where("user_projects.project_id = ? AND users.id = ?", project.ID, userID).
			Count(&count)

		if count == 0 {
			c.JSON(http.StatusForbidden, gin.H{
				"error": "无权访问此项目",
			})
			return
		}
	}

	c.JSON(http.StatusOK, project)
}

// CreateProject 创建项目
// @Summary 创建项目
// @Tags project
// @Security BearerAuth
// @Accept json
// @Produce json
// @Param request body CreateProjectRequest true "项目信息"
// @Success 200 {object} model.Project
// @Router /api/v1/projects [post]
func (h *ProjectHandler) CreateProject(c *gin.Context) {
	var req CreateProjectRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "请求参数错误: " + err.Error(),
		})
		return
	}

	// 检查项目名是否已存在
	var existing model.Project
	if err := h.db.Where("name = ?", req.Name).First(&existing).Error; err == nil {
		c.JSON(http.StatusConflict, gin.H{
			"error": "项目名已存在",
		})
		return
	}

	// 在Gitea中创建组织
	displayName := req.DisplayName
	if displayName == "" {
		displayName = req.Name
	}

	giteaOrg, err := h.giteaService.CreateOrganization(gitea.CreateOrgOption{
		Name:        req.Name,
		FullName:    displayName,
		Description: req.Description,
		Visibility:  "private",
	})
	if err != nil {
		h.logger.Error("Failed to create Gitea organization", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "创建Gitea组织失败: " + err.Error(),
		})
		return
	}

	// 创建项目
	project := model.Project{
		Name:         req.Name,
		DisplayName:  req.DisplayName,
		Description:  req.Description,
		IsPrivate:    req.IsPrivate,
		GiteaOrgID:   giteaOrg.ID,
		GiteaOrgName: giteaOrg.UserName,
		IsActive:     true,
		Status:       "active",
	}

	// 开始事务
	tx := h.db.Begin()

	if err := tx.Create(&project).Error; err != nil {
		tx.Rollback()
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "创建项目失败",
		})
		return
	}

	// 将当前用户添加为项目成员
	userID, _ := c.Get("user_id")
	var user model.User
	if err := tx.First(&user, userID).Error; err != nil {
		tx.Rollback()
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "查询用户失败",
		})
		return
	}

	if err := tx.Model(&project).Association("Members").Append(&user); err != nil {
		tx.Rollback()
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "添加项目成员失败",
		})
		return
	}

	tx.Commit()

	h.logger.Info("Project created",
		zap.Uint("project_id", project.ID),
		zap.String("name", project.Name),
		zap.Uint("creator_id", user.ID),
	)

	c.JSON(http.StatusOK, gin.H{
		"message": "项目创建成功",
		"project": project,
	})
}

// UpdateProject 更新项目
// @Summary 更新项目
// @Tags project
// @Security BearerAuth
// @Accept json
// @Param id path int true "项目ID"
// @Param request body UpdateProjectRequest true "项目信息"
// @Produce json
// @Success 200 {object} model.Project
// @Router /api/v1/projects/{id} [put]
func (h *ProjectHandler) UpdateProject(c *gin.Context) {
	projectID := c.Param("id")

	var req UpdateProjectRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "请求参数错误: " + err.Error(),
		})
		return
	}

	var project model.Project
	if err := h.db.First(&project, projectID).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{
				"error": "项目不存在",
			})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "查询项目失败",
		})
		return
	}

	// 更新字段
	project.DisplayName = req.DisplayName
	project.Description = req.Description
	project.IsPrivate = req.IsPrivate

	if err := h.db.Save(&project).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "更新项目失败",
		})
		return
	}

	h.logger.Info("Project updated",
		zap.Uint("project_id", project.ID),
		zap.String("name", project.Name),
	)

	c.JSON(http.StatusOK, gin.H{
		"message": "项目更新成功",
		"project": project,
	})
}

// DeleteProject 删除项目
// @Summary 删除项目
// @Tags project
// @Security BearerAuth
// @Param id path int true "项目ID"
// @Produce json
// @Success 200 {object} map[string]string
// @Router /api/v1/projects/{id} [delete]
func (h *ProjectHandler) DeleteProject(c *gin.Context) {
	projectID := c.Param("id")

	var project model.Project
	if err := h.db.First(&project, projectID).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{
				"error": "项目不存在",
			})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "查询项目失败",
		})
		return
	}

	// 软删除
	if err := h.db.Delete(&project).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "删除项目失败",
		})
		return
	}

	h.logger.Info("Project deleted",
		zap.Uint("project_id", project.ID),
		zap.String("name", project.Name),
	)

	c.JSON(http.StatusOK, gin.H{
		"message": "项目删除成功",
	})
}
