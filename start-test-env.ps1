# 启动测试环境
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  阶段4 功能测试环境启动脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 1. 检查依赖服务
Write-Host "`n[1/4] 检查依赖服务..." -ForegroundColor Yellow
$postgresUp = Test-NetConnection localhost -Port 5432 -InformationLevel Quiet
$redisUp = Test-NetConnection localhost -Port 6379 -InformationLevel Quiet

if (-not $postgresUp) {
    Write-Host "  ❌ PostgreSQL未运行 (5432)" -ForegroundColor Red
    Write-Host "  提示: docker run -d --name metrics-postgres -e POSTGRES_DB=metrics_hub -e POSTGRES_PASSWORD=postgres -p 5432:5432 timescale/timescaledb:latest-pg14" -ForegroundColor Yellow
    exit 1
}
Write-Host "  ✅ PostgreSQL已运行" -ForegroundColor Green

if (-not $redisUp) {
    Write-Host "  ❌ Redis未运行 (6379)" -ForegroundColor Red
    Write-Host "  提示: docker run -d --name metrics-redis -p 6379:6379 redis:7-alpine" -ForegroundColor Yellow
    exit 1
}
Write-Host "  ✅ Redis已运行" -ForegroundColor Green

# 2. 启动Hub
Write-Host "`n[2/4] 启动Hub服务..." -ForegroundColor Yellow
Start-Job -Name "hub-service" -ScriptBlock {
    Set-Location $using:PWD\hub
    ..\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
} | Out-Null
Write-Host "  ⏳ Hub启动中,等待30秒..." -ForegroundColor Cyan
Start-Sleep -Seconds 30

# 3. 验证Hub
$hubHealth = $false
for ($i = 1; $i -le 5; $i++) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -ErrorAction SilentlyContinue
        if ($response.status -eq "UP") {
            $hubHealth = $true
            break
        }
    } catch {}
    Start-Sleep -Seconds 5
}

if (-not $hubHealth) {
    Write-Host "  ❌ Hub健康检查失败" -ForegroundColor Red
    Write-Host "  查看日志: Get-Job -Name hub-service | Receive-Job" -ForegroundColor Yellow
    exit 1
}
Write-Host "  ✅ Hub已就绪 (http://localhost:8080)" -ForegroundColor Green

# 4. 启动主应用
Write-Host "`n[3/4] 启动主应用..." -ForegroundColor Yellow
Start-Job -Name "main-app" -ScriptBlock {
    Set-Location $using:PWD
    .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
} | Out-Null
Write-Host "  ⏳ 主应用启动中,等待20秒..." -ForegroundColor Cyan
Start-Sleep -Seconds 20

# 5. 验证主应用
$appHealth = $false
for ($i = 1; $i -le 5; $i++) {
    try {
        Invoke-WebRequest -Uri "http://localhost:8081" -UseBasicParsing -ErrorAction SilentlyContinue | Out-Null
        $appHealth = $true
        break
    } catch {}
    Start-Sleep -Seconds 5
}

if (-not $appHealth) {
    Write-Host "  ❌ 主应用启动失败" -ForegroundColor Red
    Write-Host "  查看日志: Get-Job -Name main-app | Receive-Job" -ForegroundColor Yellow
    exit 1
}
Write-Host "  ✅ 主应用已就绪 (http://localhost:8081)" -ForegroundColor Green

# 6. 测试总结
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  ✅ 测试环境启动成功!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "服务地址:" -ForegroundColor Yellow
Write-Host "  - Hub服务:  http://localhost:8080"
Write-Host "  - 主应用:   http://localhost:8081"
Write-Host "  - 仪表板:   http://localhost:8081/dashboard"
Write-Host ""
Write-Host "健康检查:" -ForegroundColor Yellow
Write-Host "  - Hub:  curl http://localhost:8080/actuator/health"
Write-Host "  - 主应用: curl http://localhost:8081/actuator/health"
Write-Host ""
Write-Host "查看日志:" -ForegroundColor Yellow
Write-Host "  - Hub:  Get-Job -Name hub-service | Receive-Job"
Write-Host "  - 主应用: Get-Job -Name main-app | Receive-Job"
Write-Host ""
Write-Host "停止服务:" -ForegroundColor Yellow
Write-Host "  .\stop-test-env.ps1"
Write-Host ""
Write-Host "开始测试: 请参考 doc/阶段4-功能测试指南.md" -ForegroundColor Cyan
Write-Host ""
