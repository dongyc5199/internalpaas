# Step 5 Hub代理端点功能测试脚本
# 测试日期: 2025年10月17日

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 5 Hub代理端点功能测试" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$baseUrl = "http://localhost:9999"
$testResults = @()

# 测试配置
$testServerId = 1
$from = [DateTimeOffset]::UtcNow.AddHours(-1).ToUnixTimeSeconds()
$to = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$step = 60

Write-Host "[INFO] 测试配置:" -ForegroundColor Yellow
Write-Host "  - Base URL: $baseUrl"
Write-Host "  - Server ID: $testServerId"
Write-Host "  - Time Range: $from - $to (1 hour)"
Write-Host "  - Step: $step seconds`n"

# ============================================
# 测试1: 单服务器Hub代理端点 - 正常查询
# ============================================
Write-Host "[TEST 1] 单服务器Hub代理端点 - 正常查询" -ForegroundColor Green
$url = "$baseUrl/monitoring/api/server/$testServerId/hub/query?from=$from&to=$to&step=$step"
Write-Host "  GET $url"

try {
    $response = Invoke-RestMethod -Uri $url -Method Get -ErrorAction Stop
    Write-Host "  ✅ 测试通过 - 状态码: 200" -ForegroundColor Green
    Write-Host "  响应数据: $($response | ConvertTo-Json -Depth 2 -Compress)"
    $testResults += @{Test = "单服务器Hub代理"; Status = "✅ Pass"; Code = 200}
} catch {
    Write-Host "  ❌ 测试失败 - $($_.Exception.Message)" -ForegroundColor Red
    $testResults += @{Test = "单服务器Hub代理"; Status = "❌ Fail"; Code = $_.Exception.Response.StatusCode.Value__}
}
Write-Host ""

# ============================================
# 测试2: 单服务器Hub代理端点 - 服务器不存在
# ============================================
Write-Host "[TEST 2] 单服务器Hub代理端点 - 服务器不存在(404)" -ForegroundColor Green
$url = "$baseUrl/monitoring/api/server/99999/hub/query?from=$from&to=$to&step=$step"
Write-Host "  GET $url"

try {
    $response = Invoke-RestMethod -Uri $url -Method Get -ErrorAction Stop
    Write-Host "  ❌ 应该返回404 - 实际: 200" -ForegroundColor Red
    $testResults += @{Test = "404错误处理"; Status = "❌ Fail"; Code = 200}
} catch {
    if ($_.Exception.Response.StatusCode.Value__ -eq 404) {
        Write-Host "  ✅ 测试通过 - 正确返回404" -ForegroundColor Green
        $testResults += @{Test = "404错误处理"; Status = "✅ Pass"; Code = 404}
    } else {
        Write-Host "  ❌ 错误状态码: $($_.Exception.Response.StatusCode.Value__)" -ForegroundColor Red
        $testResults += @{Test = "404错误处理"; Status = "❌ Fail"; Code = $_.Exception.Response.StatusCode.Value__}
    }
}
Write-Host ""

# ============================================
# 测试3: 批量服务器Hub代理端点
# ============================================
Write-Host "[TEST 3] 批量服务器Hub代理端点" -ForegroundColor Green
$url = "$baseUrl/monitoring/api/servers/hub/batch-query"
Write-Host "  POST $url"

$batchRequest = @{
    serverIds = @(1, 2, 3)
    from = $from
    to = $to
    step = $step
    fields = "cpu,memory,disk"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri $url -Method Post -Body $batchRequest -ContentType "application/json" -ErrorAction Stop
    Write-Host "  ✅ 测试通过 - 状态码: 200" -ForegroundColor Green
    Write-Host "  响应数据: $($response | ConvertTo-Json -Depth 2 -Compress)"
    $testResults += @{Test = "批量Hub代理"; Status = "✅ Pass"; Code = 200}
} catch {
    Write-Host "  ❌ 测试失败 - $($_.Exception.Message)" -ForegroundColor Red
    $testResults += @{Test = "批量Hub代理"; Status = "❌ Fail"; Code = $_.Exception.Response.StatusCode.Value__}
}
Write-Host ""

# ============================================
# 测试4: 废弃端点 - 仍可使用
# ============================================
Write-Host "[TEST 4] 废弃端点 - 向后兼容性" -ForegroundColor Green
$url = "$baseUrl/monitoring/history/api/server/$testServerId/realtime"
Write-Host "  GET $url"

try {
    $response = Invoke-RestMethod -Uri $url -Method Get -ErrorAction Stop
    Write-Host "  ✅ 测试通过 - 废弃端点仍可使用" -ForegroundColor Green
    Write-Host "  ⚠️  应在日志中看到废弃警告" -ForegroundColor Yellow
    $testResults += @{Test = "废弃端点兼容性"; Status = "✅ Pass"; Code = 200}
} catch {
    Write-Host "  ❌ 测试失败 - $($_.Exception.Message)" -ForegroundColor Red
    $testResults += @{Test = "废弃端点兼容性"; Status = "❌ Fail"; Code = $_.Exception.Response.StatusCode.Value__}
}
Write-Host ""

# ============================================
# 测试5: 参数验证
# ============================================
Write-Host "[TEST 5] 参数验证 - 缺少必填参数" -ForegroundColor Green
$url = "$baseUrl/monitoring/api/server/$testServerId/hub/query"
Write-Host "  GET $url (无参数)"

try {
    $response = Invoke-RestMethod -Uri $url -Method Get -ErrorAction Stop
    Write-Host "  ❌ 应该返回400 - 实际: 200" -ForegroundColor Red
    $testResults += @{Test = "参数验证"; Status = "❌ Fail"; Code = 200}
} catch {
    if ($_.Exception.Response.StatusCode.Value__ -eq 400 -or $_.Exception.Response.StatusCode.Value__ -eq 500) {
        Write-Host "  ✅ 测试通过 - 正确返回错误" -ForegroundColor Green
        $testResults += @{Test = "参数验证"; Status = "✅ Pass"; Code = $_.Exception.Response.StatusCode.Value__}
    } else {
        Write-Host "  ⚠️  状态码: $($_.Exception.Response.StatusCode.Value__)" -ForegroundColor Yellow
        $testResults += @{Test = "参数验证"; Status = "⚠️  Warn"; Code = $_.Exception.Response.StatusCode.Value__}
    }
}
Write-Host ""

# ============================================
# 测试总结
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试总结" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$passCount = ($testResults | Where-Object { $_.Status -like "*Pass*" }).Count
$failCount = ($testResults | Where-Object { $_.Status -like "*Fail*" }).Count
$warnCount = ($testResults | Where-Object { $_.Status -like "*Warn*" }).Count

Write-Host "总测试数: $($testResults.Count)"
Write-Host "通过: $passCount ✅" -ForegroundColor Green
Write-Host "失败: $failCount ❌" -ForegroundColor Red
Write-Host "警告: $warnCount ⚠️`n" -ForegroundColor Yellow

Write-Host "详细结果:" -ForegroundColor Yellow
foreach ($result in $testResults) {
    Write-Host "  $($result.Status) $($result.Test) (HTTP $($result.Code))"
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "测试完成!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# 返回退出码
if ($failCount -eq 0) {
    exit 0
} else {
    exit 1
}
