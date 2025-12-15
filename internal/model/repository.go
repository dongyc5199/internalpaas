package model

import (
	"time"

	"gorm.io/gorm"
)

// Repository 代码仓库模型
type Repository struct {
	ID        uint           `gorm:"primarykey" json:"id"`
	CreatedAt time.Time      `json:"created_at"`
	UpdatedAt time.Time      `json:"updated_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`

	// 基本信息
	Name        string `gorm:"size:100;not null" json:"name"`
	FullName    string `gorm:"uniqueIndex;size:200;not null" json:"full_name"` // owner/repo
	Description string `gorm:"type:text" json:"description"`
	Language    string `gorm:"size:50" json:"language"`

	// 所属关系
	ProjectID uint `gorm:"index;not null" json:"project_id"`
	OwnerID   uint `gorm:"index;not null" json:"owner_id"`

	// Gitea集成
	GiteaRepoID int64  `gorm:"uniqueIndex" json:"gitea_repo_id"`
	CloneURL    string `gorm:"size:255" json:"clone_url"`
	SSHURL      string `gorm:"size:255" json:"ssh_url"`
	WebURL      string `gorm:"size:255" json:"web_url"`

	// Drone CI集成
	DroneRepoID int64  `gorm:"index" json:"drone_repo_id"`
	DroneActive bool   `gorm:"default:false" json:"drone_active"`
	DroneConfig string `gorm:"size:100" json:"drone_config"` // .drone.yml路径

	// 状态
	IsPrivate bool   `gorm:"default:false" json:"is_private"`
	IsFork    bool   `gorm:"default:false" json:"is_fork"`
	IsArchived bool  `gorm:"default:false" json:"is_archived"`
	Status    string `gorm:"size:20;default:'active'" json:"status"`

	// 代码统计
	Size         int64      `json:"size"`          // 字节
	StarCount    int        `gorm:"default:0" json:"star_count"`
	ForkCount    int        `gorm:"default:0" json:"fork_count"`
	IssueCount   int        `gorm:"default:0" json:"issue_count"`
	DefaultBranch string    `gorm:"size:100;default:'main'" json:"default_branch"`
	LastPushAt   *time.Time `json:"last_push_at"`

	// 关联
	BuildRecords   []BuildRecord   `gorm:"foreignKey:RepositoryID" json:"-"`
	QualityReports []QualityReport `gorm:"foreignKey:RepositoryID" json:"-"`
}

// TableName 指定表名
func (Repository) TableName() string {
	return "repositories"
}
