package service

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/spf13/viper"
	"go.uber.org/zap"
)

// SonarQubeService SonarQube集成服务
type SonarQubeService struct {
	baseURL string
	token   string
	client  *http.Client
	logger  *zap.Logger
}

// NewSonarQubeService 创建SonarQube服务实例
func NewSonarQubeService(logger *zap.Logger) (*SonarQubeService, error) {
	baseURL := viper.GetString("sonarqube.url")
	token := viper.GetString("sonarqube.token")

	if baseURL == "" {
		return nil, fmt.Errorf("sonarqube.url not configured")
	}

	if token == "" {
		return nil, fmt.Errorf("sonarqube.token not configured")
	}

	client := &http.Client{
		Timeout: 30 * time.Second,
	}

	logger.Info("SonarQube service initialized",
		zap.String("url", baseURL),
	)

	return &SonarQubeService{
		baseURL: baseURL,
		token:   token,
		client:  client,
		logger:  logger,
	}, nil
}

// ============================================
// 项目相关操作
// ============================================

// ProjectInfo SonarQube项目信息
type ProjectInfo struct {
	Key         string `json:"key"`
	Name        string `json:"name"`
	Qualifier   string `json:"qualifier"`
	Visibility  string `json:"visibility"`
	LastAnalysis string `json:"lastAnalysisDate,omitempty"`
}

// CreateProject 创建SonarQube项目
func (s *SonarQubeService) CreateProject(key, name string) (*ProjectInfo, error) {
	endpoint := fmt.Sprintf("%s/api/projects/create", s.baseURL)

	data := url.Values{}
	data.Set("project", key)
	data.Set("name", name)

	req, err := http.NewRequest("POST", endpoint, strings.NewReader(data.Encode()))
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.token, "")
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	resp, err := s.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to create project: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("sonarqube API error: %s - %s", resp.Status, string(body))
	}

	var result struct {
		Project ProjectInfo `json:"project"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	s.logger.Info("Created SonarQube project",
		zap.String("key", key),
		zap.String("name", name),
	)

	return &result.Project, nil
}

// GetProject 获取项目信息
func (s *SonarQubeService) GetProject(key string) (*ProjectInfo, error) {
	endpoint := fmt.Sprintf("%s/api/projects/search?projects=%s", s.baseURL, url.QueryEscape(key))

	req, err := http.NewRequest("GET", endpoint, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.token, "")

	resp, err := s.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to get project: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("sonarqube API error: %s - %s", resp.Status, string(body))
	}

	var result struct {
		Components []ProjectInfo `json:"components"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	if len(result.Components) == 0 {
		return nil, fmt.Errorf("project not found: %s", key)
	}

	return &result.Components[0], nil
}

// DeleteProject 删除项目
func (s *SonarQubeService) DeleteProject(key string) error {
	endpoint := fmt.Sprintf("%s/api/projects/delete", s.baseURL)

	data := url.Values{}
	data.Set("project", key)

	req, err := http.NewRequest("POST", endpoint, strings.NewReader(data.Encode()))
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.token, "")
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	resp, err := s.client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to delete project: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusNoContent && resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("sonarqube API error: %s - %s", resp.Status, string(body))
	}

	s.logger.Info("Deleted SonarQube project", zap.String("key", key))

	return nil
}

// ============================================
// 质量分析相关操作
// ============================================

// QualityGate 质量门禁状态
type QualityGate struct {
	Status     string            `json:"status"` // OK, WARN, ERROR
	Conditions []QualityCondition `json:"conditions"`
}

// QualityCondition 质量门禁条件
type QualityCondition struct {
	Status         string `json:"status"`
	MetricKey      string `json:"metricKey"`
	Comparator     string `json:"comparator"`
	ErrorThreshold string `json:"errorThreshold"`
	ActualValue    string `json:"actualValue"`
}

// Measure 度量指标
type Measure struct {
	Metric string `json:"metric"`
	Value  string `json:"value"`
}

// QualityAnalysis 质量分析结果
type QualityAnalysis struct {
	ProjectKey   string       `json:"projectKey"`
	AnalysisKey  string       `json:"analysisKey"`
	Date         time.Time    `json:"date"`
	QualityGate  QualityGate  `json:"qualityGate"`
	Measures     []Measure    `json:"measures"`
	BugCount     int          `json:"bugCount"`
	VulnerabilityCount int    `json:"vulnerabilityCount"`
	CodeSmellCount     int    `json:"codeSmellCount"`
	Coverage     float64      `json:"coverage"`
	Duplications float64      `json:"duplications"`
	LOC          int          `json:"linesOfCode"`
}

// GetQualityGate 获取项目的质量门禁状态
func (s *SonarQubeService) GetQualityGate(projectKey string) (*QualityGate, error) {
	endpoint := fmt.Sprintf("%s/api/qualitygates/project_status?projectKey=%s",
		s.baseURL, url.QueryEscape(projectKey))

	req, err := http.NewRequest("GET", endpoint, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.token, "")

	resp, err := s.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to get quality gate: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("sonarqube API error: %s - %s", resp.Status, string(body))
	}

	var result struct {
		ProjectStatus QualityGate `json:"projectStatus"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &result.ProjectStatus, nil
}

// GetMeasures 获取项目度量指标
func (s *SonarQubeService) GetMeasures(projectKey string, metrics []string) ([]Measure, error) {
	metricsParam := strings.Join(metrics, ",")
	endpoint := fmt.Sprintf("%s/api/measures/component?component=%s&metricKeys=%s",
		s.baseURL, url.QueryEscape(projectKey), url.QueryEscape(metricsParam))

	req, err := http.NewRequest("GET", endpoint, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.token, "")

	resp, err := s.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to get measures: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("sonarqube API error: %s - %s", resp.Status, string(body))
	}

	var result struct {
		Component struct {
			Measures []Measure `json:"measures"`
		} `json:"component"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return result.Component.Measures, nil
}

// GetQualityAnalysis 获取完整的质量分析结果
func (s *SonarQubeService) GetQualityAnalysis(projectKey string) (*QualityAnalysis, error) {
	// 获取质量门禁
	qualityGate, err := s.GetQualityGate(projectKey)
	if err != nil {
		return nil, fmt.Errorf("failed to get quality gate: %w", err)
	}

	// 获取关键度量指标
	metrics := []string{
		"bugs",
		"vulnerabilities",
		"code_smells",
		"coverage",
		"duplicated_lines_density",
		"ncloc",
		"sqale_index",
		"reliability_rating",
		"security_rating",
		"sqale_rating",
	}

	measures, err := s.GetMeasures(projectKey, metrics)
	if err != nil {
		return nil, fmt.Errorf("failed to get measures: %w", err)
	}

	// 构建分析结果
	analysis := &QualityAnalysis{
		ProjectKey:  projectKey,
		Date:        time.Now(),
		QualityGate: *qualityGate,
		Measures:    measures,
	}

	// 解析度量值
	for _, measure := range measures {
		switch measure.Metric {
		case "bugs":
			fmt.Sscanf(measure.Value, "%d", &analysis.BugCount)
		case "vulnerabilities":
			fmt.Sscanf(measure.Value, "%d", &analysis.VulnerabilityCount)
		case "code_smells":
			fmt.Sscanf(measure.Value, "%d", &analysis.CodeSmellCount)
		case "coverage":
			fmt.Sscanf(measure.Value, "%f", &analysis.Coverage)
		case "duplicated_lines_density":
			fmt.Sscanf(measure.Value, "%f", &analysis.Duplications)
		case "ncloc":
			fmt.Sscanf(measure.Value, "%d", &analysis.LOC)
		}
	}

	return analysis, nil
}

// ============================================
// 分析任务相关操作
// ============================================

// AnalysisTask 分析任务
type AnalysisTask struct {
	ID              string    `json:"id"`
	Type            string    `json:"type"`
	ComponentKey    string    `json:"componentKey"`
	ComponentName   string    `json:"componentName"`
	Status          string    `json:"status"` // SUCCESS, FAILED, CANCELED, PENDING, IN_PROGRESS
	SubmittedAt     time.Time `json:"submittedAt"`
	StartedAt       time.Time `json:"startedAt,omitempty"`
	ExecutedAt      time.Time `json:"executedAt,omitempty"`
	ExecutionTimeMs int64     `json:"executionTimeMs,omitempty"`
}

// GetAnalysisTask 获取分析任务状态
func (s *SonarQubeService) GetAnalysisTask(taskID string) (*AnalysisTask, error) {
	endpoint := fmt.Sprintf("%s/api/ce/task?id=%s", s.baseURL, url.QueryEscape(taskID))

	req, err := http.NewRequest("GET", endpoint, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.token, "")

	resp, err := s.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to get analysis task: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("sonarqube API error: %s - %s", resp.Status, string(body))
	}

	var result struct {
		Task AnalysisTask `json:"task"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &result.Task, nil
}

// GetLatestAnalysis 获取项目最新的分析任务
func (s *SonarQubeService) GetLatestAnalysis(projectKey string) (*AnalysisTask, error) {
	endpoint := fmt.Sprintf("%s/api/ce/component?component=%s&status=SUCCESS",
		s.baseURL, url.QueryEscape(projectKey))

	req, err := http.NewRequest("GET", endpoint, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.token, "")

	resp, err := s.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to get latest analysis: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("sonarqube API error: %s - %s", resp.Status, string(body))
	}

	var result struct {
		Queue   []AnalysisTask `json:"queue"`
		Current AnalysisTask   `json:"current"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	// 返回当前分析任务（如果存在）
	if result.Current.ID != "" {
		return &result.Current, nil
	}

	return nil, fmt.Errorf("no analysis found for project: %s", projectKey)
}

// ============================================
// 辅助方法
// ============================================

// Ping 测试SonarQube连接
func (s *SonarQubeService) Ping() error {
	endpoint := fmt.Sprintf("%s/api/system/status", s.baseURL)

	req, err := http.NewRequest("GET", endpoint, nil)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.token, "")

	resp, err := s.client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to ping sonarqube: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("sonarqube not available: %s", resp.Status)
	}

	return nil
}

// GetServerVersion 获取SonarQube服务器版本
func (s *SonarQubeService) GetServerVersion() (string, error) {
	endpoint := fmt.Sprintf("%s/api/server/version", s.baseURL)

	req, err := http.NewRequest("GET", endpoint, nil)
	if err != nil {
		return "", fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.token, "")

	resp, err := s.client.Do(req)
	if err != nil {
		return "", fmt.Errorf("failed to get version: %w", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("failed to read response: %w", err)
	}

	return strings.TrimSpace(string(body)), nil
}
