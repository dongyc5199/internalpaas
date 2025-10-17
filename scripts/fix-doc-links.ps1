# 批量修复文档链接脚本
# 将旧的 doc/ 路径更新为新的 docs/ 分类路径

Write-Host "开始修复文档链接..." -ForegroundColor Cyan

# 定义路径映射表 (旧路径 -> 新路径)
$pathMappings = @{
    # 架构文档
    'doc/project-overview.md' = 'docs/architecture/overview.md'
    'doc/frontend-architecture.md' = 'docs/architecture/frontend-architecture.md'
    'doc/Agent自动部署架构设计.md' = 'docs/architecture/agent-auto-deployment-architecture.md'
    
    # 管理后台设计
    'doc/admin-dashboard-redesign-plan.md' = 'docs/design/admin-dashboard/redesign-plan.md'
    'doc/admin-dashboard-api-design-specification.md' = 'docs/design/admin-dashboard/api-design-specification.md'
    'doc/admin-dashboard-stat-cards-design.md' = 'docs/design/admin-dashboard/stat-cards-design.md'
    
    # 服务器管理设计
    'doc/server-detail-page-design.md' = 'docs/design/server-management/detail-page-design.md'
    'doc/server-status-tags-design.md' = 'docs/design/server-management/status-tags-design.md'
    'doc/user-server-account-sync.md' = 'docs/design/server-management/user-server-account-sync.md'
    'doc/server-detail-integration-plan.md' = 'docs/design/server-management/detail-integration-plan.md'
    
    # Agent部署设计
    'doc/Agent部署测试指南.md' = 'docs/design/agent-deployment/deployment-testing-guide.md'
    'doc/workspace-ui-redesign-plan.md' = 'docs/design/agent-deployment/workspace-ui-redesign.md'
    
    # UI系统设计
    'doc/content-page-framework-design.md' = 'docs/design/ui-system/content-framework-design.md'
    'doc/login-page-design.md' = 'docs/design/ui-system/login-page-design.md'
    'doc/icon-resource-specification.md' = 'docs/design/ui-system/icon-resource-specification.md'
    'doc/drawer-integration.md' = 'docs/design/ui-system/drawer-integration.md'
    
    # 原型文件
    'doc/prototypes/' = 'docs/design/prototypes/'
    
    # 技术文档
    'doc/轻量级平台数据库策略优化方案.md' = 'docs/technical/database-strategy-optimization.md'
    'doc/H2数据库使用分析与优化建议.md' = 'docs/technical/h2-database-analysis-optimization.md'
    'doc/performance-optimization.md' = 'docs/technical/performance-optimization.md'
    'doc/n1-query-optimization-verification.md' = 'docs/technical/n1-query-optimization-verification.md'
    'doc/phase4-database-optimization-validation.md' = 'docs/technical/phase4-database-optimization-validation.md'
    'doc/数据库优化执行清单.md' = 'docs/technical/database-optimization-checklist.md'
    'doc/code-optimization-report.md' = 'docs/technical/code-optimization-report.md'
    'doc/code-review-optimization-recommendations.md' = 'docs/technical/code-review-optimization-recommendations.md'
    'doc/h2-database-clarification.md' = 'docs/technical/h2-database-clarification.md'
    'doc/数据库配置指南.md' = 'docs/technical/database-configuration-guide.md'
    'doc/postgresql-setup-guide.md' = 'docs/technical/postgresql-setup-guide.md'
    
    # 开发文档
    'doc/development-roadmap.md' = 'docs/development/roadmap.md'
    'doc/lombok-ide-setup.md' = 'docs/development/lombok-ide-setup.md'
    'doc/Dev_Debug_Platform_前端研发总结与指导文档.md' = 'docs/development/frontend-dev-summary-guide.md'
    
    # 用户指南
    'doc/deployment-guide.md' = 'docs/guides/deployment-guide.md'
    'doc/operations-manual.md' = 'docs/guides/operations-guide.md'
    'doc/offline-deployment-guide.md' = 'docs/guides/offline-deployment-guide.md'
    'doc/METRICS_HUB_INTEGRATION_GUIDE.md' = 'docs/guides/metrics-hub-integration-guide.md'
    'doc/QUICK_START_INTEGRATION.md' = 'docs/guides/hub-quick-start-integration.md'
    'doc/integration-visual-comparison.md' = 'docs/guides/hub-integration-visual-comparison.md'
    
    # 历史归档
    'doc/阶段4-数据存储迁移实施方案.md' = 'docs/archives/2024-phase4/数据存储迁移实施方案.md'
    'doc/阶段4-Step1完成报告.md' = 'docs/archives/2024-phase4/Step1完成报告.md'
    'doc/阶段4-Step3完成报告.md' = 'docs/archives/2024-phase4/Step3完成报告.md'
    'doc/阶段4-Step4进度报告.md' = 'docs/archives/2024-phase4/Step4进度报告.md'
    'doc/Step4-测试失败总结报告.md' = 'docs/archives/2024-phase4/Step4-测试失败总结报告.md'
    'doc/阶段4-功能测试指南.md' = 'docs/archives/2024-phase4/功能测试指南.md'
    'doc/阶段4-工作总结-2025-10-17.md' = 'docs/archives/2024-phase4/工作总结-2025-10-17.md'
    'doc/Hub集成剩余工作分析.md' = 'docs/archives/2024-phase4/Hub集成剩余工作分析.md'
    'doc/troubleshooting/' = 'docs/archives/troubleshooting/'
    'doc/fragment-restructure-report.md' = 'docs/archives/legacy-reports/fragment-restructure-report.md'
    'doc/DOCUMENTATION_MANAGEMENT_ANALYSIS.md' = 'docs/archives/legacy-reports/documentation-management-analysis.md'
    'doc/README.md' = 'docs/archives/legacy-reports/old-doc-readme.md'
}

# 需要处理的文件列表
$filesToFix = @(
    'AGENTS.md'
    'CI_README.md'
    'CLAUDE.md'
    'README.md'
    'docs/guides/quick-start.md'
    'docs/guides/hub-quick-start-integration.md'
    'docs/technical/database-optimization-checklist.md'
    'docs/technical/h2-database-analysis-optimization.md'
    'docs/design/admin-dashboard/stat-cards-design.md'
    'docs/design/agent-deployment/workspace-ui-redesign.md'
    'docs/design/prototypes/header-prototype.html'
    'docs/design/ui-system/content-framework-usage-example.md'
    'e2e/README.md'
    'hub/Hub模块与主应用集成完成情况报告.md'
    'src/main/java/com/cmict/internalpaas/controller/MonitoringController.java'
    'src/main/java/com/cmict/internalpaas/service/MonitoringSchedulerService.java'
    'upgrade/doc/demo-guide.md'
    'upgrade/doc/e2e-testing-completion.md'
)

$fixCount = 0

foreach ($file in $filesToFix) {
    $filePath = Join-Path $PSScriptRoot "..\$file"
    
    if (Test-Path $filePath) {
        Write-Host "处理: $file" -ForegroundColor Yellow
        $content = Get-Content $filePath -Raw -Encoding UTF8
        $originalContent = $content
        
        # 应用所有路径替换
        foreach ($oldPath in $pathMappings.Keys) {
            $newPath = $pathMappings[$oldPath]
            if ($content -match [regex]::Escape($oldPath)) {
                $content = $content -replace [regex]::Escape($oldPath), $newPath
                Write-Host "  ✓ $oldPath -> $newPath" -ForegroundColor Green
            }
        }
        
        # 如果内容有变化,保存文件
        if ($content -ne $originalContent) {
            Set-Content $filePath -Value $content -Encoding UTF8 -NoNewline
            $fixCount++
        }
    } else {
        Write-Host "跳过(文件不存在): $file" -ForegroundColor Gray
    }
}

Write-Host "`n修复完成! 共更新 $fixCount 个文件" -ForegroundColor Green
Write-Host "建议: 执行 'git diff' 检查更改" -ForegroundColor Cyan
