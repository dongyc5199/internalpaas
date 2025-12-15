package model

import (
	"database/sql/driver"
	"encoding/json"
	"time"

	"gorm.io/gorm"
)

// BuildStatus 构建状态枚举
type BuildStatus string

const (
	BuildStatusPending  BuildStatus = "pending"
	BuildStatusRunning  BuildStatus = "running"
	BuildStatusSuccess  BuildStatus = "success"
	BuildStatusFailure  BuildStatus = "failure"
	BuildStatusKilled   BuildStatus = "killed"
	BuildStatusSkipped  BuildStatus = "skipped"
	BuildStatusError    BuildStatus = "error"
)

// BuildTrigger 构建触发器
type BuildTrigger string

const (
	TriggerPush   BuildTrigger = "push"
	TriggerPR     BuildTrigger = "pull_request"
	TriggerTag    BuildTrigger = "tag"
	TriggerManual BuildTrigger = "manual"
	TriggerCron   BuildTrigger = "cron"
)

// JSONMap 用于存储JSON类型字段
type JSONMap map[string]interface{}

// Scan 实现sql.Scanner接口
func (j *JSONMap) Scan(value interface{}) error {
	if value == nil {
		*j = make(JSONMap)
		return nil
	}
	bytes, ok := value.([]byte)
	if !ok {
		return nil
	}
	return json.Unmarshal(bytes, j)
}

// Value 实现driver.Valuer接口
func (j JSONMap) Value() (driver.Value, error) {
	if j == nil {
		return nil, nil
	}
	return json.Marshal(j)
}

// BuildRecord 构建记录模型
type BuildRecord struct {
	ID        uint           `gorm:"primarykey" json:"id"`
	CreatedAt time.Time      `json:"created_at"`
	UpdatedAt time.Time      `json:"updated_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`

	// 关联关系
	RepositoryID  uint `gorm:"index;not null" json:"repository_id"`
	TriggerUserID uint `gorm:"index" json:"trigger_user_id"`

	// Drone CI集成
	DroneBuildID     int64  `gorm:"uniqueIndex" json:"drone_build_id"`
	DroneBuildNumber int    `gorm:"index" json:"drone_build_number"`
	DroneLink        string `gorm:"size:255" json:"drone_link"`

	// 构建信息
	BuildNumber int          `gorm:"index" json:"build_number"` // 仓库内的构建编号
	Status      BuildStatus  `gorm:"size:20;index" json:"status"`
	Trigger     BuildTrigger `gorm:"size:20" json:"trigger"`

	// Git信息
	Branch string `gorm:"size:100;index" json:"branch"`
	Commit string `gorm:"size:40;index" json:"commit"`
	Ref    string `gorm:"size:200" json:"ref"`
	Tag    string `gorm:"size:100" json:"tag"`

	// 提交信息
	CommitMessage string `gorm:"type:text" json:"commit_message"`
	CommitAuthor  string `gorm:"size:100" json:"commit_author"`
	CommitEmail   string `gorm:"size:100" json:"commit_email"`

	// 时间信息
	StartedAt  *time.Time `json:"started_at"`
	FinishedAt *time.Time `json:"finished_at"`
	Duration   int        `json:"duration"` // 秒

	// 构建环境
	Runner   string  `gorm:"size:100" json:"runner"`
	Platform string  `gorm:"size:50" json:"platform"` // linux/amd64
	Config   JSONMap `gorm:"type:jsonb" json:"config"`

	// 构建结果
	ExitCode   int     `json:"exit_code"`
	Error      string  `gorm:"type:text" json:"error"`
	LogSize    int64   `json:"log_size"`
	LogPath    string  `gorm:"size:255" json:"log_path"`
	ArtifactURL string `gorm:"size:255" json:"artifact_url"`

	// 统计信息
	TestCount        int     `json:"test_count"`
	TestPassCount    int     `json:"test_pass_count"`
	TestFailCount    int     `json:"test_fail_count"`
	Coverage         float64 `json:"coverage"`

	// 质量关联
	QualityReportID *uint `gorm:"index" json:"quality_report_id"`
}

// TableName 指定表名
func (BuildRecord) TableName() string {
	return "build_records"
}

// IsFinished 判断构建是否完成
func (b *BuildRecord) IsFinished() bool {
	return b.Status == BuildStatusSuccess ||
		b.Status == BuildStatusFailure ||
		b.Status == BuildStatusKilled ||
		b.Status == BuildStatusError
}

// IsSuccess 判断构建是否成功
func (b *BuildRecord) IsSuccess() bool {
	return b.Status == BuildStatusSuccess
}
