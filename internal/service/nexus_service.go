package service

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"time"

	"github.com/spf13/viper"
	"go.uber.org/zap"
)

// NexusService Nexus制品仓库集成服务
type NexusService struct {
	baseURL  string
	username string
	password string
	client   *http.Client
	logger   *zap.Logger
}

// NewNexusService 创建Nexus服务实例
func NewNexusService(logger *zap.Logger) (*NexusService, error) {
	baseURL := viper.GetString("nexus.url")
	username := viper.GetString("nexus.username")
	password := viper.GetString("nexus.password")

	if baseURL == "" {
		return nil, fmt.Errorf("nexus.url not configured")
	}

	if username == "" || password == "" {
		return nil, fmt.Errorf("nexus credentials not configured")
	}

	client := &http.Client{
		Timeout: 60 * time.Second,
	}

	logger.Info("Nexus service initialized",
		zap.String("url", baseURL),
		zap.String("username", username),
	)

	return &NexusService{
		baseURL:  baseURL,
		username: username,
		password: password,
		client:   client,
		logger:   logger,
	}, nil
}

// ============================================
// Repository相关操作
// ============================================

// Repository Nexus仓库信息
type Repository struct {
	Name   string                 `json:"name"`
	Format string                 `json:"format"` // maven2, npm, docker, raw等
	Type   string                 `json:"type"`   // hosted, proxy, group
	URL    string                 `json:"url"`
	Online bool                   `json:"online"`
	Storage map[string]interface{} `json:"storage"`
}

// ListRepositories 列出所有仓库
func (s *NexusService) ListRepositories() ([]Repository, error) {
	endpoint := fmt.Sprintf("%s/service/rest/v1/repositories", s.baseURL)

	req, err := http.NewRequest("GET", endpoint, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.username, s.password)
	req.Header.Set("Accept", "application/json")

	resp, err := s.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to list repositories: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("nexus API error: %s - %s", resp.Status, string(body))
	}

	var repos []Repository
	if err := json.NewDecoder(resp.Body).Decode(&repos); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return repos, nil
}

// ============================================
// Component（制品）相关操作
// ============================================

// Component Nexus制品信息
type Component struct {
	ID         string                   `json:"id"`
	Repository string                   `json:"repository"`
	Format     string                   `json:"format"`
	Group      string                   `json:"group"`
	Name       string                   `json:"name"`
	Version    string                   `json:"version"`
	Assets     []Asset                  `json:"assets"`
	Tags       map[string]interface{}   `json:"tags"`
}

// Asset 制品资产
type Asset struct {
	DownloadURL string                 `json:"downloadUrl"`
	Path        string                 `json:"path"`
	ID          string                 `json:"id"`
	Repository  string                 `json:"repository"`
	Format      string                 `json:"format"`
	Checksum    map[string]string      `json:"checksum"`
	ContentType string                 `json:"contentType"`
	LastModified time.Time             `json:"lastModified"`
	FileSize    int64                  `json:"fileSize"`
}

// SearchComponents 搜索制品
func (s *NexusService) SearchComponents(repository, group, name, version string) ([]Component, error) {
	endpoint := fmt.Sprintf("%s/service/rest/v1/search", s.baseURL)

	params := url.Values{}
	if repository != "" {
		params.Set("repository", repository)
	}
	if group != "" {
		params.Set("group", group)
	}
	if name != "" {
		params.Set("name", name)
	}
	if version != "" {
		params.Set("version", version)
	}

	fullURL := endpoint
	if len(params) > 0 {
		fullURL = endpoint + "?" + params.Encode()
	}

	req, err := http.NewRequest("GET", fullURL, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.username, s.password)
	req.Header.Set("Accept", "application/json")

	resp, err := s.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to search components: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("nexus API error: %s - %s", resp.Status, string(body))
	}

	var result struct {
		Items []Component `json:"items"`
	}

	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return result.Items, nil
}

// GetComponent 获取制品详情
func (s *NexusService) GetComponent(componentID string) (*Component, error) {
	endpoint := fmt.Sprintf("%s/service/rest/v1/components/%s", s.baseURL, componentID)

	req, err := http.NewRequest("GET", endpoint, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.username, s.password)
	req.Header.Set("Accept", "application/json")

	resp, err := s.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to get component: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("nexus API error: %s - %s", resp.Status, string(body))
	}

	var component Component
	if err := json.NewDecoder(resp.Body).Decode(&component); err != nil {
		return nil, fmt.Errorf("failed to decode response: %w", err)
	}

	return &component, nil
}

// UploadComponent 上传制品（Maven格式）
func (s *NexusService) UploadComponent(repository, group, artifactID, version, filePath string) error {
	endpoint := fmt.Sprintf("%s/service/rest/v1/components?repository=%s",
		s.baseURL, url.QueryEscape(repository))

	// 打开文件
	file, err := os.Open(filePath)
	if err != nil {
		return fmt.Errorf("failed to open file: %w", err)
	}
	defer file.Close()

	// 创建multipart表单
	body := &bytes.Buffer{}
	writer := multipart.NewWriter(body)

	// 添加Maven坐标
	writer.WriteField("maven2.groupId", group)
	writer.WriteField("maven2.artifactId", artifactID)
	writer.WriteField("maven2.version", version)
	writer.WriteField("maven2.generate-pom", "true")

	// 添加文件
	part, err := writer.CreateFormFile("maven2.asset1", filepath.Base(filePath))
	if err != nil {
		return fmt.Errorf("failed to create form file: %w", err)
	}

	if _, err := io.Copy(part, file); err != nil {
		return fmt.Errorf("failed to copy file: %w", err)
	}

	writer.WriteField("maven2.asset1.extension", filepath.Ext(filePath)[1:])

	if err := writer.Close(); err != nil {
		return fmt.Errorf("failed to close writer: %w", err)
	}

	// 发送请求
	req, err := http.NewRequest("POST", endpoint, body)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.username, s.password)
	req.Header.Set("Content-Type", writer.FormDataContentType())

	resp, err := s.client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to upload component: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusNoContent && resp.StatusCode != http.StatusCreated {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("nexus API error: %s - %s", resp.Status, string(body))
	}

	s.logger.Info("Component uploaded",
		zap.String("repository", repository),
		zap.String("group", group),
		zap.String("artifact", artifactID),
		zap.String("version", version),
	)

	return nil
}

// DeleteComponent 删除制品
func (s *NexusService) DeleteComponent(componentID string) error {
	endpoint := fmt.Sprintf("%s/service/rest/v1/components/%s", s.baseURL, componentID)

	req, err := http.NewRequest("DELETE", endpoint, nil)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.username, s.password)

	resp, err := s.client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to delete component: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusNoContent {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("nexus API error: %s - %s", resp.Status, string(body))
	}

	s.logger.Info("Component deleted", zap.String("component_id", componentID))

	return nil
}

// ============================================
// Asset（资产）相关操作
// ============================================

// DownloadAsset 下载资产
func (s *NexusService) DownloadAsset(assetID, savePath string) error {
	// 先获取资产信息获得下载URL
	endpoint := fmt.Sprintf("%s/service/rest/v1/assets/%s", s.baseURL, assetID)

	req, err := http.NewRequest("GET", endpoint, nil)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.username, s.password)
	req.Header.Set("Accept", "application/json")

	resp, err := s.client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to get asset: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("nexus API error: %s - %s", resp.Status, string(body))
	}

	var asset Asset
	if err := json.NewDecoder(resp.Body).Decode(&asset); err != nil {
		return fmt.Errorf("failed to decode response: %w", err)
	}

	// 下载文件
	downloadReq, err := http.NewRequest("GET", asset.DownloadURL, nil)
	if err != nil {
		return fmt.Errorf("failed to create download request: %w", err)
	}

	downloadReq.SetBasicAuth(s.username, s.password)

	downloadResp, err := s.client.Do(downloadReq)
	if err != nil {
		return fmt.Errorf("failed to download asset: %w", err)
	}
	defer downloadResp.Body.Close()

	if downloadResp.StatusCode != http.StatusOK {
		return fmt.Errorf("download failed: %s", downloadResp.Status)
	}

	// 创建文件
	outFile, err := os.Create(savePath)
	if err != nil {
		return fmt.Errorf("failed to create file: %w", err)
	}
	defer outFile.Close()

	// 写入文件
	if _, err := io.Copy(outFile, downloadResp.Body); err != nil {
		return fmt.Errorf("failed to write file: %w", err)
	}

	s.logger.Info("Asset downloaded",
		zap.String("asset_id", assetID),
		zap.String("save_path", savePath),
	)

	return nil
}

// ============================================
// 辅助方法
// ============================================

// Ping 测试Nexus连接
func (s *NexusService) Ping() error {
	endpoint := fmt.Sprintf("%s/service/rest/v1/status", s.baseURL)

	req, err := http.NewRequest("GET", endpoint, nil)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.username, s.password)

	resp, err := s.client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to ping nexus: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("nexus not available: %s", resp.Status)
	}

	return nil
}

// GetServerVersion 获取Nexus服务器版本
func (s *NexusService) GetServerVersion() (string, error) {
	endpoint := fmt.Sprintf("%s/service/rest/v1/status", s.baseURL)

	req, err := http.NewRequest("GET", endpoint, nil)
	if err != nil {
		return "", fmt.Errorf("failed to create request: %w", err)
	}

	req.SetBasicAuth(s.username, s.password)
	req.Header.Set("Accept", "application/json")

	resp, err := s.client.Do(req)
	if err != nil {
		return "", fmt.Errorf("failed to get status: %w", err)
	}
	defer resp.Body.Close()

	var status map[string]interface{}
	if err := json.NewDecoder(resp.Body).Decode(&status); err != nil {
		return "", fmt.Errorf("failed to decode response: %w", err)
	}

	if version, ok := status["version"].(string); ok {
		return version, nil
	}

	return "unknown", nil
}
