package service

import (
	"fmt"

	"code.gitea.io/sdk/gitea"
	"github.com/spf13/viper"
	"go.uber.org/zap"
)

// GiteaService Gitea集成服务
type GiteaService struct {
	client *gitea.Client
	logger *zap.Logger
	url    string
	token  string
}

// NewGiteaService 创建Gitea服务实例
func NewGiteaService(logger *zap.Logger) (*GiteaService, error) {
	url := viper.GetString("gitea.url")
	token := viper.GetString("gitea.admin_token")

	if url == "" {
		return nil, fmt.Errorf("gitea.url not configured")
	}

	// 如果没有token，返回未认证的客户端（仅用于公开API）
	var client *gitea.Client
	var err error

	if token != "" {
		client, err = gitea.NewClient(url, gitea.SetToken(token))
	} else {
		client, err = gitea.NewClient(url)
	}

	if err != nil {
		return nil, fmt.Errorf("failed to create gitea client: %w", err)
	}

	logger.Info("Gitea service initialized",
		zap.String("url", url),
		zap.Bool("authenticated", token != ""),
	)

	return &GiteaService{
		client: client,
		logger: logger,
		url:    url,
		token:  token,
	}, nil
}

// ============================================
// 用户相关操作
// ============================================

// GetUser 获取用户信息
func (s *GiteaService) GetUser(username string) (*gitea.User, error) {
	user, _, err := s.client.GetUserInfo(username)
	if err != nil {
		return nil, fmt.Errorf("failed to get user: %w", err)
	}
	return user, nil
}

// CreateUser 创建用户
func (s *GiteaService) CreateUser(opts gitea.CreateUserOption) (*gitea.User, error) {
	user, _, err := s.client.AdminCreateUser(opts)
	if err != nil {
		return nil, fmt.Errorf("failed to create user: %w", err)
	}

	s.logger.Info("Created Gitea user",
		zap.String("username", opts.Username),
		zap.String("email", opts.Email),
	)

	return user, nil
}

// ============================================
// 组织相关操作
// ============================================

// CreateOrganization 创建组织
func (s *GiteaService) CreateOrganization(opts gitea.CreateOrgOption) (*gitea.Organization, error) {
	org, _, err := s.client.CreateOrg(opts)
	if err != nil {
		return nil, fmt.Errorf("failed to create organization: %w", err)
	}

	s.logger.Info("Created Gitea organization",
		zap.String("name", opts.Name),
	)

	return org, nil
}

// GetOrganization 获取组织信息
func (s *GiteaService) GetOrganization(orgName string) (*gitea.Organization, error) {
	org, _, err := s.client.GetOrg(orgName)
	if err != nil {
		return nil, fmt.Errorf("failed to get organization: %w", err)
	}
	return org, nil
}

// ListOrganizations 列出所有组织
func (s *GiteaService) ListOrganizations() ([]*gitea.Organization, error) {
	orgs, _, err := s.client.AdminListOrgs(gitea.AdminListOrgsOptions{})
	if err != nil {
		return nil, fmt.Errorf("failed to list organizations: %w", err)
	}
	return orgs, nil
}

// ============================================
// 仓库相关操作
// ============================================

// CreateRepository 创建仓库
func (s *GiteaService) CreateRepository(orgName string, opts gitea.CreateRepoOption) (*gitea.Repository, error) {
	repo, _, err := s.client.CreateOrgRepo(orgName, opts)
	if err != nil {
		return nil, fmt.Errorf("failed to create repository: %w", err)
	}

	s.logger.Info("Created Gitea repository",
		zap.String("org", orgName),
		zap.String("repo", opts.Name),
	)

	return repo, nil
}

// CreateRepositoryInOrg 在组织中创建仓库（简化版）
func (s *GiteaService) CreateRepositoryInOrg(orgName, name, description string, private, autoInit bool, gitignore, license string) (*gitea.Repository, error) {
	opts := gitea.CreateRepoOption{
		Name:          name,
		Description:   description,
		Private:       private,
		AutoInit:      autoInit,
		Gitignores:    gitignore,
		License:       license,
		DefaultBranch: "main",
	}

	return s.CreateRepository(orgName, opts)
}

// GetRepository 获取仓库信息
func (s *GiteaService) GetRepository(owner, repo string) (*gitea.Repository, error) {
	repository, _, err := s.client.GetRepo(owner, repo)
	if err != nil {
		return nil, fmt.Errorf("failed to get repository: %w", err)
	}
	return repository, nil
}

// ListOrgRepositories 列出组织的所有仓库
func (s *GiteaService) ListOrgRepositories(orgName string) ([]*gitea.Repository, error) {
	repos, _, err := s.client.ListOrgRepos(orgName, gitea.ListOrgReposOptions{})
	if err != nil {
		return nil, fmt.Errorf("failed to list repositories: %w", err)
	}
	return repos, nil
}

// DeleteRepository 删除仓库
func (s *GiteaService) DeleteRepository(owner, repo string) error {
	_, err := s.client.DeleteRepo(owner, repo)
	if err != nil {
		return fmt.Errorf("failed to delete repository: %w", err)
	}

	s.logger.Info("Deleted Gitea repository",
		zap.String("owner", owner),
		zap.String("repo", repo),
	)

	return nil
}

// ============================================
// Webhook相关操作
// ============================================

// CreateWebhook 创建webhook
func (s *GiteaService) CreateWebhook(owner, repo string, opts gitea.CreateHookOption) (*gitea.Hook, error) {
	hook, _, err := s.client.CreateRepoHook(owner, repo, opts)
	if err != nil {
		return nil, fmt.Errorf("failed to create webhook: %w", err)
	}

	s.logger.Info("Created webhook",
		zap.String("owner", owner),
		zap.String("repo", repo),
		zap.String("url", opts.Config["url"]),
	)

	return hook, nil
}

// ListWebhooks 列出仓库的所有webhook
func (s *GiteaService) ListWebhooks(owner, repo string) ([]*gitea.Hook, error) {
	hooks, _, err := s.client.ListRepoHooks(owner, repo, gitea.ListHooksOptions{})
	if err != nil {
		return nil, fmt.Errorf("failed to list webhooks: %w", err)
	}
	return hooks, nil
}

// DeleteWebhook 删除webhook
func (s *GiteaService) DeleteWebhook(owner, repo string, hookID int64) error {
	_, err := s.client.DeleteRepoHook(owner, repo, hookID)
	if err != nil {
		return fmt.Errorf("failed to delete webhook: %w", err)
	}

	s.logger.Info("Deleted webhook",
		zap.String("owner", owner),
		zap.String("repo", repo),
		zap.Int64("hook_id", hookID),
	)

	return nil
}

// ============================================
// 分支和提交相关操作
// ============================================

// ListBranches 列出仓库的所有分支
func (s *GiteaService) ListBranches(owner, repo string) ([]*gitea.Branch, error) {
	branches, _, err := s.client.ListRepoBranches(owner, repo, gitea.ListRepoBranchesOptions{})
	if err != nil {
		return nil, fmt.Errorf("failed to list branches: %w", err)
	}
	return branches, nil
}

// GetCommit 获取提交信息
func (s *GiteaService) GetCommit(owner, repo, sha string) (*gitea.Commit, error) {
	commit, _, err := s.client.GetSingleCommit(owner, repo, sha)
	if err != nil {
		return nil, fmt.Errorf("failed to get commit: %w", err)
	}
	return commit, nil
}

// ListCommits 列出仓库的提交历史
func (s *GiteaService) ListCommits(owner, repo, branch string, page, pageSize int) ([]*gitea.Commit, error) {
	opts := gitea.ListCommitOptions{
		ListOptions: gitea.ListOptions{
			Page:     page,
			PageSize: pageSize,
		},
		SHA: branch,
	}

	commits, _, err := s.client.ListRepoCommits(owner, repo, opts)
	if err != nil {
		return nil, fmt.Errorf("failed to list commits: %w", err)
	}
	return commits, nil
}

// ============================================
// 辅助方法
// ============================================

// Ping 测试Gitea连接
func (s *GiteaService) Ping() error {
	_, _, err := s.client.ServerVersion()
	if err != nil {
		return fmt.Errorf("failed to ping gitea: %w", err)
	}
	return nil
}

// GetVersion 获取Gitea版本
func (s *GiteaService) GetVersion() (string, error) {
	version, _, err := s.client.ServerVersion()
	if err != nil {
		return "", fmt.Errorf("failed to get version: %w", err)
	}
	return version, nil
}
