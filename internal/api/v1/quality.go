package v1

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"
	"gorm.io/gorm"

	"github.com/yourorg/codehub/internal/model"
	"github.com/yourorg/codehub/internal/service"
)

// QualityHandler 质量分析处理器
type QualityHandler struct {
	db               *gorm.DB
	logger           *zap.Logger
	sonarQubeService *service.SonarQubeService
}

// NewQualityHandler 创建质量分析处理器
func NewQualityHandler(db *gorm.DB, logger *zap.Logger, sonarQubeService *service.SonarQubeService) *QualityHandler {
	return &QualityHandler{
		db:               db,
		logger:           logger,
		sonarQubeService: sonarQubeService,
	}
}

// CreateQualityReportRequest 创建质量报告请求
type CreateQualityReportRequest struct {
	RepositoryID uint   `json:"repository_id" binding:"required"`
	BuildID      *uint  `json:"build_id"` // 可选，关联到具体构建
	Branch       string `json:"branch" binding:"required"`
	Commit       string `json:"commit" binding:"required"`
}

// GetQualityReport 获取质量报告详情
// GET /api/v1/quality-reports/:id
func (h *QualityHandler) GetQualityReport(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	id, err := strconv.ParseUint(c.Param("id"), 10, 32)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的报告ID"})
		return
	}

	var report model.QualityReport
	if err := h.db.
		Preload("Repository.Project.Members").
		Preload("BuildRecord").
		First(&report, id).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			c.JSON(http.StatusNotFound, gin.H{"error": "质量报告不存在"})
		} else {
			h.logger.Error("Failed to query quality report", zap.Error(err))
			c.JSON(http.StatusInternalServerError, gin.H{"error": "查询质量报告失败"})
		}
		return
	}

	// 检查用户权限
	if !h.hasRepositoryAccess(userID.(uint), &report.Repository) {
		c.JSON(http.StatusForbidden, gin.H{"error": "无权访问该质量报告"})
		return
	}

	c.JSON(http.StatusOK, report)
}

// ListQualityReports 列出质量报告
// GET /api/v1/repositories/:repo_id/quality-reports
func (h *QualityHandler) ListQualityReports(c *gin.Context) {
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
	branch := c.Query("branch")
	status := c.Query("quality_gate_status")

	query := h.db.Model(&model.QualityReport{}).Where("repository_id = ?", repoID)

	if branch != "" {
		query = query.Where("branch = ?", branch)
	}

	if status != "" {
		query = query.Where("quality_gate_status = ?", status)
	}

	// 计算总数
	var total int64
	query.Count(&total)

	// 分页查询
	var reports []model.QualityReport
	offset := (page - 1) * pageSize
	if err := query.
		Preload("BuildRecord").
		Offset(offset).
		Limit(pageSize).
		Order("created_at DESC").
		Find(&reports).Error; err != nil {
		h.logger.Error("Failed to query quality reports", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "查询质量报告失败"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"reports":   reports,
		"total":     total,
		"page":      page,
		"page_size": pageSize,
	})
}

// CreateQualityReport 创建质量报告
// POST /api/v1/quality-reports
func (h *QualityHandler) CreateQualityReport(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户未认证"})
		return
	}

	var req CreateQualityReportRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数错误: " + err.Error()})
		return
	}

	// 验证仓库访问权限
	var repository model.Repository
	if err := h.db.Preload("Project.Members").First(&repository, req.RepositoryID).Error; err != nil {
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

	// 从SonarQube获取分析结果
	if h.sonarQubeService == nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{"error": "SonarQube服务不可用"})
		return
	}

	// 构建SonarQube项目key: org/repo
	projectKey := fmt.Sprintf("%s/%s", repository.Project.GiteaOrgName, repository.Name)

	// 获取质量分析结果
	analysis, err := h.sonarQubeService.GetQualityAnalysis(projectKey)
	if err != nil {
		h.logger.Error("Failed to get quality analysis from SonarQube",
			zap.Error(err),
			zap.String("project_key", projectKey),
		)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "获取质量分析失败: " + err.Error()})
		return
	}

	// 序列化度量数据
	measuresJSON, err := json.Marshal(analysis.Measures)
	if err != nil {
		h.logger.Error("Failed to marshal measures", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "序列化度量数据失败"})
		return
	}

	conditionsJSON, err := json.Marshal(analysis.QualityGate.Conditions)
	if err != nil {
		h.logger.Error("Failed to marshal conditions", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "序列化质量门禁条件失败"})
		return
	}

	// 解析JSON到JSONMap
	var metricsMap model.JSONMap
	if err := json.Unmarshal(measuresJSON, &metricsMap); err != nil {
		h.logger.Error("Failed to unmarshal metrics", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "解析度量数据失败"})
		return
	}

	var conditionsMap model.JSONMap
	if err := json.Unmarshal(conditionsJSON, &conditionsMap); err != nil {
		h.logger.Error("Failed to unmarshal conditions", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "解析质量门禁条件失败"})
		return
	}

	// 创建质量报告
	report := model.QualityReport{
		RepositoryID:       req.RepositoryID,
		BuildID:            req.BuildID,
		Branch:             req.Branch,
		Commit:             req.Commit,
		SonarProjectKey:    projectKey,
		QualityGateStatus:  model.QualityGate(analysis.QualityGate.Status),
		QualityGateDetails: conditionsMap,
		Bugs:               analysis.BugCount,
		Vulnerabilities:    analysis.VulnerabilityCount,
		CodeSmells:         analysis.CodeSmellCount,
		Coverage:           analysis.Coverage,
		Duplications:       analysis.Duplications,
		Lines:              analysis.LOC,
		TechnicalDebt:      0, // TODO: 从sqale_index计算
		MaintainabilityRating: "A", // TODO: 从sqale_rating解析
		ReliabilityRating:    "A", // TODO: 从reliability_rating解析
		SecurityRating:       "A", // TODO: 从security_rating解析
		Metrics:             metricsMap,
	}

	if err := h.db.Create(&report).Error; err != nil {
		h.logger.Error("Failed to create quality report", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "创建质量报告失败"})
		return
	}

	// 如果关联了构建记录，更新构建记录
	if req.BuildID != nil {
		h.db.Model(&model.BuildRecord{}).
			Where("id = ?", *req.BuildID).
			Update("quality_report_id", report.ID)
	}

	h.logger.Info("Quality report created",
		zap.Uint("report_id", report.ID),
		zap.String("project_key", projectKey),
		zap.String("status", string(report.QualityGateStatus)),
	)

	c.JSON(http.StatusCreated, report)
}

// GetRepositoryQualityTrend 获取仓库质量趋势
// GET /api/v1/repositories/:repo_id/quality-trend
func (h *QualityHandler) GetRepositoryQualityTrend(c *gin.Context) {
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
	limit, _ := strconv.Atoi(c.DefaultQuery("limit", "30"))
	branch := c.Query("branch")

	query := h.db.Model(&model.QualityReport{}).
		Where("repository_id = ?", repoID).
		Order("created_at DESC").
		Limit(limit)

	if branch != "" {
		query = query.Where("branch = ?", branch)
	}

	var reports []model.QualityReport
	if err := query.Find(&reports).Error; err != nil {
		h.logger.Error("Failed to query quality trend", zap.Error(err))
		c.JSON(http.StatusInternalServerError, gin.H{"error": "查询质量趋势失败"})
		return
	}

	// 构建趋势数据
	type TrendPoint struct {
		Date              string  `json:"date"`
		QualityGateStatus string  `json:"quality_gate_status"`
		Bugs              int     `json:"bugs"`
		Vulnerabilities   int     `json:"vulnerabilities"`
		CodeSmells        int     `json:"code_smells"`
		Coverage          float64 `json:"coverage"`
		Duplications      float64 `json:"duplications"`
	}

	trend := make([]TrendPoint, 0, len(reports))
	for _, report := range reports {
		trend = append(trend, TrendPoint{
			Date:              report.CreatedAt.Format("2006-01-02"),
			QualityGateStatus: string(report.QualityGateStatus),
			Bugs:              report.Bugs,
			Vulnerabilities:   report.Vulnerabilities,
			CodeSmells:        report.CodeSmells,
			Coverage:          report.Coverage,
			Duplications:      report.Duplications,
		})
	}

	c.JSON(http.StatusOK, gin.H{
		"repository_id": repoID,
		"branch":        branch,
		"trend":         trend,
	})
}

// GetQualityStatistics 获取质量统计信息
// GET /api/v1/repositories/:repo_id/quality-statistics
func (h *QualityHandler) GetQualityStatistics(c *gin.Context) {
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

	// 统计各质量门禁状态的数量
	type StatusCount struct {
		Status string `json:"status"`
		Count  int64  `json:"count"`
	}

	var statusCounts []StatusCount
	h.db.Model(&model.QualityReport{}).
		Where("repository_id = ?", repoID).
		Select("quality_gate_status as status, count(*) as count").
		Group("quality_gate_status").
		Scan(&statusCounts)

	// 获取最新报告
	var latestReport model.QualityReport
	h.db.Where("repository_id = ?", repoID).
		Order("created_at DESC").
		First(&latestReport)

	// 计算平均指标
	type AverageMetrics struct {
		AvgBugs            float64
		AvgVulnerabilities float64
		AvgCodeSmells      float64
		AvgCoverage        float64
		AvgDuplications    float64
	}

	var avgMetrics AverageMetrics
	h.db.Model(&model.QualityReport{}).
		Where("repository_id = ?", repoID).
		Select(`
			AVG(bugs) as avg_bugs,
			AVG(vulnerabilities) as avg_vulnerabilities,
			AVG(code_smells) as avg_code_smells,
			AVG(coverage) as avg_coverage,
			AVG(duplications) as avg_duplications
		`).
		Scan(&avgMetrics)

	c.JSON(http.StatusOK, gin.H{
		"status_counts":   statusCounts,
		"latest_report":   latestReport,
		"average_metrics": avgMetrics,
	})
}

// hasRepositoryAccess 检查用户是否有仓库访问权限
func (h *QualityHandler) hasRepositoryAccess(userID uint, repository *model.Repository) bool {
	for _, member := range repository.Project.Members {
		if member.ID == userID {
			return true
		}
	}
	return false
}
