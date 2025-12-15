package model

import (
	"time"

	"gorm.io/gorm"
)

// User 用户模型
type User struct {
	ID        uint           `gorm:"primarykey" json:"id"`
	CreatedAt time.Time      `json:"created_at"`
	UpdatedAt time.Time      `json:"updated_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`

	// 基本信息
	Username string `gorm:"uniqueIndex;size:50;not null" json:"username"`
	Email    string `gorm:"uniqueIndex;size:100;not null" json:"email"`
	Password string `gorm:"size:255;not null" json:"-"`
	FullName string `gorm:"size:100" json:"full_name"`
	Avatar   string `gorm:"size:255" json:"avatar"`

	// 状态
	IsActive bool `gorm:"default:true" json:"is_active"`
	IsAdmin  bool `gorm:"default:false" json:"is_admin"`

	// Gitea集成
	GiteaID       int64  `gorm:"index" json:"gitea_id"`
	GiteaUsername string `gorm:"size:50" json:"gitea_username"`
	GiteaToken    string `gorm:"size:255" json:"-"`

	// 最后登录
	LastLoginAt *time.Time `json:"last_login_at"`
	LastLoginIP string     `gorm:"size:45" json:"last_login_ip"`

	// 关联
	Projects     []Project     `gorm:"many2many:user_projects;" json:"-"`
	Repositories []Repository  `gorm:"foreignKey:OwnerID" json:"-"`
	BuildRecords []BuildRecord `gorm:"foreignKey:TriggerUserID" json:"-"`
}

// BeforeCreate GORM钩子：创建前
func (u *User) BeforeCreate(tx *gorm.DB) error {
	u.CreatedAt = time.Now()
	return nil
}

// TableName 指定表名
func (User) TableName() string {
	return "users"
}
