package model

import (
	"time"

	"gorm.io/gorm"
)

// Project 项目模型
type Project struct {
	ID        uint           `gorm:"primarykey" json:"id"`
	CreatedAt time.Time      `json:"created_at"`
	UpdatedAt time.Time      `json:"updated_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`

	// 基本信息
	Name        string `gorm:"uniqueIndex;size:100;not null" json:"name"`
	DisplayName string `gorm:"size:100" json:"display_name"`
	Description string `gorm:"type:text" json:"description"`
	Avatar      string `gorm:"size:255" json:"avatar"`

	// Gitea组织关联
	GiteaOrgID   int64  `gorm:"index" json:"gitea_org_id"`
	GiteaOrgName string `gorm:"size:100" json:"gitea_org_name"`

	// 状态
	IsActive  bool   `gorm:"default:true" json:"is_active"`
	IsPrivate bool   `gorm:"default:false" json:"is_private"`
	Status    string `gorm:"size:20;default:'active'" json:"status"` // active, archived, deleted

	// 统计信息
	RepositoryCount int `gorm:"default:0" json:"repository_count"`
	MemberCount     int `gorm:"default:0" json:"member_count"`
	BuildCount      int `gorm:"default:0" json:"build_count"`

	// 关联
	Members      []User       `gorm:"many2many:user_projects;" json:"-"`
	Repositories []Repository `gorm:"foreignKey:ProjectID" json:"-"`
}

// TableName 指定表名
func (Project) TableName() string {
	return "projects"
}
