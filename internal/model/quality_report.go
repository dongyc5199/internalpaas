package model

import (
	"time"

	"gorm.io/gorm"
)

// QualityGate 质量门禁状态
type QualityGate string

const (
	QualityGatePassed QualityGate = "PASSED"
	QualityGateFailed QualityGate = "FAILED"
	QualityGateWarn   QualityGate = "WARN"
	QualityGateNone   QualityGate = "NONE"
)

// QualityReport 代码质量报告模型
type QualityReport struct {
	ID        uint           `gorm:"primarykey" json:"id"`
	CreatedAt time.Time      `json:"created_at"`
	UpdatedAt time.Time      `json:"updated_at"`
	DeletedAt gorm.DeletedAt `gorm:"index" json:"-"`

	// 关联关系
	RepositoryID uint       `gorm:"index;not null" json:"repository_id"`
	Repository   Repository `gorm:"foreignKey:RepositoryID" json:"repository,omitempty"`
	BuildID      *uint      `gorm:"index" json:"build_id"`
	BuildRecord  *BuildRecord `gorm:"foreignKey:BuildID;constraint:-" json:"build_record,omitempty"`

	// SonarQube集成
	SonarProjectKey string `gorm:"size:200;index" json:"sonar_project_key"`
	SonarTaskID     string `gorm:"size:100" json:"sonar_task_id"`
	SonarAnalysisID string `gorm:"size:100" json:"sonar_analysis_id"`
	SonarLink       string `gorm:"size:255" json:"sonar_link"`

	// 代码信息
	Branch string `gorm:"size:100;index" json:"branch"`
	Commit string `gorm:"size:40;index" json:"commit"`

	// 质量门禁
	QualityGateStatus QualityGate `gorm:"size:20;index" json:"quality_gate_status"`
	QualityGateDetails JSONMap     `gorm:"type:jsonb" json:"quality_gate_details"`

	// 代码度量
	Lines           int     `json:"lines"`
	CodeSmells      int     `json:"code_smells"`
	Bugs            int     `json:"bugs"`
	Vulnerabilities int     `json:"vulnerabilities"`
	SecurityHotspots int    `json:"security_hotspots"`
	Duplications    float64 `json:"duplications"` // 百分比
	Coverage        float64 `json:"coverage"`     // 百分比
	TechnicalDebt   int     `json:"technical_debt"` // 分钟

	// 等级评分
	ReliabilityRating      string `gorm:"size:2" json:"reliability_rating"`       // A-E
	SecurityRating         string `gorm:"size:2" json:"security_rating"`          // A-E
	MaintainabilityRating  string `gorm:"size:2" json:"maintainability_rating"`   // A-E
	SecurityReviewRating   string `gorm:"size:2" json:"security_review_rating"`   // A-E

	// 分析信息
	AnalyzedAt    *time.Time `json:"analyzed_at"`
	AnalysisDuration int     `json:"analysis_duration"` // 秒
	Status        string     `gorm:"size:20" json:"status"` // SUCCESS, FAILED, PENDING

	// 详细指标（JSON存储）
	Metrics JSONMap `gorm:"type:jsonb" json:"metrics"`
}

// TableName 指定表名
func (QualityReport) TableName() string {
	return "quality_reports"
}

// IsQualityGatePassed 判断质量门禁是否通过
func (q *QualityReport) IsQualityGatePassed() bool {
	return q.QualityGateStatus == QualityGatePassed
}

// GetOverallRating 获取整体评级（取最低）
func (q *QualityReport) GetOverallRating() string {
	ratings := []string{
		q.ReliabilityRating,
		q.SecurityRating,
		q.MaintainabilityRating,
		q.SecurityReviewRating,
	}

	// 找出最低评级
	minRating := "A"
	for _, rating := range ratings {
		if rating > minRating {
			minRating = rating
		}
	}
	return minRating
}
