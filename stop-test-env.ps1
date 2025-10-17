# 停止测试环境
Write-Host "停止测试环境..." -ForegroundColor Yellow

# 停止所有后台任务
Get-Job | Stop-Job
Get-Job | Remove-Job

Write-Host "✅ 已停止所有服务" -ForegroundColor Green
Write-Host ""
Write-Host "如需重新启动,请运行: .\start-test-env.ps1" -ForegroundColor Cyan
