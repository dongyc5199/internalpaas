package service

import (
	"context"
	"fmt"

	"github.com/drone/drone-go/drone"
	"github.com/spf13/viper"
	"go.uber.org/zap"
	"golang.org/x/oauth2"
)

// DroneService Drone CI集成服务
type DroneService struct {
	client drone.Client
	logger *zap.Logger
	url    string
	token  string
}

// NewDroneService 创建Drone服务实例
func NewDroneService(logger *zap.Logger) (*DroneService, error) {
	url := viper.GetString("drone.server_url")
	token := viper.GetString("drone.admin_token")

	if url == "" {
		return nil, fmt.Errorf("drone.server_url not configured")
	}

	if token == "" {
		return nil, fmt.Errorf("drone.admin_token not configured")
	}

	// 配置OAuth2 token
	config := new(oauth2.Config)
	auther := config.Client(
		context.Background(),
		&oauth2.Token{
			AccessToken: token,
		},
	)

	// 创建Drone客户端
	client := drone.NewClient(url, auther)

	logger.Info("Drone service initialized",
		zap.String("url", url),
	)

	return &DroneService{
		client: client,
		logger: logger,
		url:    url,
		token:  token,
	}, nil
}

// ============================================
// 仓库相关操作
// ============================================

// EnableRepository 启用仓库的CI
func (s *DroneService) EnableRepository(owner, repo string) (*drone.Repo, error) {
	repository, err := s.client.RepoEnable(owner, repo)
	if err != nil {
		return nil, fmt.Errorf("failed to enable repository: %w", err)
	}

	s.logger.Info("Enabled Drone CI for repository",
		zap.String("owner", owner),
		zap.String("repo", repo),
	)

	return repository, nil
}

// DisableRepository 禁用仓库的CI
func (s *DroneService) DisableRepository(owner, repo string) error {
	err := s.client.RepoDisable(owner, repo)
	if err != nil {
		return fmt.Errorf("failed to disable repository: %w", err)
	}

	s.logger.Info("Disabled Drone CI for repository",
		zap.String("owner", owner),
		zap.String("repo", repo),
	)

	return nil
}

// GetRepository 获取仓库信息
func (s *DroneService) GetRepository(owner, repo string) (*drone.Repo, error) {
	repository, err := s.client.Repo(owner, repo)
	if err != nil {
		return nil, fmt.Errorf("failed to get repository: %w", err)
	}
	return repository, nil
}

// ListRepositories 列出所有已启用CI的仓库
func (s *DroneService) ListRepositories() ([]*drone.Repo, error) {
	repos, err := s.client.RepoList()
	if err != nil {
		return nil, fmt.Errorf("failed to list repositories: %w", err)
	}
	return repos, nil
}

// SyncRepositories 同步Gitea仓库到Drone
func (s *DroneService) SyncRepositories() error {
	_, err := s.client.RepoListSync()
	if err != nil {
		return fmt.Errorf("failed to sync repositories: %w", err)
	}

	s.logger.Info("Synchronized repositories from Gitea")
	return nil
}

// ============================================
// 构建相关操作
// ============================================

// GetBuild 获取构建信息
func (s *DroneService) GetBuild(owner, repo string, buildNumber int) (*drone.Build, error) {
	build, err := s.client.Build(owner, repo, buildNumber)
	if err != nil {
		return nil, fmt.Errorf("failed to get build: %w", err)
	}
	return build, nil
}

// ListBuilds 列出仓库的构建记录
func (s *DroneService) ListBuilds(owner, repo string) ([]*drone.Build, error) {
	builds, err := s.client.BuildList(owner, repo, drone.ListOptions{})
	if err != nil {
		return nil, fmt.Errorf("failed to list builds: %w", err)
	}
	return builds, nil
}

// TriggerBuild 触发新构建
func (s *DroneService) TriggerBuild(owner, repo, branch, commit string, params map[string]string) (*drone.Build, error) {
	build, err := s.client.BuildCreate(owner, repo, branch, commit, params)
	if err != nil {
		return nil, fmt.Errorf("failed to trigger build: %w", err)
	}

	s.logger.Info("Triggered new build",
		zap.String("owner", owner),
		zap.String("repo", repo),
		zap.String("branch", branch),
		zap.String("commit", commit),
		zap.Int64("build_number", build.Number),
	)

	return build, nil
}

// RestartBuild 重启构建
func (s *DroneService) RestartBuild(owner, repo string, buildNumber int) (*drone.Build, error) {
	build, err := s.client.BuildRestart(owner, repo, buildNumber, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to restart build: %w", err)
	}

	s.logger.Info("Restarted build",
		zap.String("owner", owner),
		zap.String("repo", repo),
		zap.Int("build_number", buildNumber),
	)

	return build, nil
}

// CancelBuild 取消构建
func (s *DroneService) CancelBuild(owner, repo string, buildNumber int) error {
	err := s.client.BuildCancel(owner, repo, buildNumber)
	if err != nil {
		return fmt.Errorf("failed to cancel build: %w", err)
	}

	s.logger.Info("Cancelled build",
		zap.String("owner", owner),
		zap.String("repo", repo),
		zap.Int("build_number", buildNumber),
	)

	return nil
}

// GetBuildLogs 获取构建日志
func (s *DroneService) GetBuildLogs(owner, repo string, buildNumber, stage, step int) ([]*drone.Line, error) {
	logs, err := s.client.Logs(owner, repo, buildNumber, stage, step)
	if err != nil {
		return nil, fmt.Errorf("failed to get build logs: %w", err)
	}
	return logs, nil
}

// ============================================
// Secret相关操作
// ============================================

// CreateSecret 创建Secret
func (s *DroneService) CreateSecret(owner, repo string, secret *drone.Secret) (*drone.Secret, error) {
	createdSecret, err := s.client.SecretCreate(owner, repo, secret)
	if err != nil {
		return nil, fmt.Errorf("failed to create secret: %w", err)
	}

	s.logger.Info("Created secret",
		zap.String("owner", owner),
		zap.String("repo", repo),
		zap.String("name", secret.Name),
	)

	return createdSecret, nil
}

// ListSecrets 列出所有Secret
func (s *DroneService) ListSecrets(owner, repo string) ([]*drone.Secret, error) {
	secrets, err := s.client.SecretList(owner, repo)
	if err != nil {
		return nil, fmt.Errorf("failed to list secrets: %w", err)
	}
	return secrets, nil
}

// DeleteSecret 删除Secret
func (s *DroneService) DeleteSecret(owner, repo, name string) error {
	err := s.client.SecretDelete(owner, repo, name)
	if err != nil {
		return fmt.Errorf("failed to delete secret: %w", err)
	}

	s.logger.Info("Deleted secret",
		zap.String("owner", owner),
		zap.String("repo", repo),
		zap.String("name", name),
	)

	return nil
}

// ============================================
// Cron相关操作
// ============================================

// CreateCron 创建定时任务
func (s *DroneService) CreateCron(owner, repo string, cron *drone.Cron) (*drone.Cron, error) {
	createdCron, err := s.client.CronCreate(owner, repo, cron)
	if err != nil {
		return nil, fmt.Errorf("failed to create cron: %w", err)
	}

	s.logger.Info("Created cron job",
		zap.String("owner", owner),
		zap.String("repo", repo),
		zap.String("name", cron.Name),
		zap.String("expr", cron.Expr),
	)

	return createdCron, nil
}

// ListCrons 列出所有定时任务
func (s *DroneService) ListCrons(owner, repo string) ([]*drone.Cron, error) {
	crons, err := s.client.CronList(owner, repo)
	if err != nil {
		return nil, fmt.Errorf("failed to list crons: %w", err)
	}
	return crons, nil
}

// DeleteCron 删除定时任务
func (s *DroneService) DeleteCron(owner, repo, name string) error {
	err := s.client.CronDelete(owner, repo, name)
	if err != nil {
		return fmt.Errorf("failed to delete cron: %w", err)
	}

	s.logger.Info("Deleted cron job",
		zap.String("owner", owner),
		zap.String("repo", repo),
		zap.String("name", name),
	)

	return nil
}

// ============================================
// 用户相关操作
// ============================================

// GetCurrentUser 获取当前用户信息
func (s *DroneService) GetCurrentUser() (*drone.User, error) {
	user, err := s.client.Self()
	if err != nil {
		return nil, fmt.Errorf("failed to get current user: %w", err)
	}
	return user, nil
}

// ============================================
// 辅助方法
// ============================================

// Ping 测试Drone连接
func (s *DroneService) Ping() error {
	_, err := s.client.Self()
	if err != nil {
		return fmt.Errorf("failed to ping drone: %w", err)
	}
	return nil
}
