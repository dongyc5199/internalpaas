# Technical Debt Status Check Script
# Run with: npm run debt:check

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "   Technical Debt Status Check" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

# Check if TECHNICAL_DEBT.md exists
if (-Not (Test-Path "TECHNICAL_DEBT.md")) {
    Write-Host "[ERROR] TECHNICAL_DEBT.md not found" -ForegroundColor Red
    exit 1
}

# Parse TECHNICAL_DEBT.md for debt items
$content = Get-Content "TECHNICAL_DEBT.md" -Raw -Encoding UTF8

# Count debt items by priority (simple pattern matching)
$p0Count = ([regex]::Matches($content, "P0 -")).Count
$p1Count = ([regex]::Matches($content, "P1 -")).Count
$p2Count = ([regex]::Matches($content, "P2 -")).Count
$p3Count = ([regex]::Matches($content, "P3 -")).Count
$totalDebt = $p0Count + $p1Count + $p2Count + $p3Count

Write-Host "[DEBT STATISTICS]" -ForegroundColor Yellow
Write-Host "  Total: $totalDebt items" -ForegroundColor White
if ($p0Count -gt 0) {
    Write-Host "  [P0] Critical: $p0Count items" -ForegroundColor Red
}
if ($p1Count -gt 0) {
    Write-Host "  [P1] Important: $p1Count items" -ForegroundColor DarkYellow
}
if ($p2Count -gt 0) {
    Write-Host "  [P2] General: $p2Count items" -ForegroundColor Yellow
}
if ($p3Count -gt 0) {
    Write-Host "  [P3] Low: $p3Count items" -ForegroundColor Green
}
Write-Host ""

# Run tests and capture results
Write-Host "[RUNNING TESTS]" -ForegroundColor Yellow
$testOutput = & npm test 2>&1 | Out-String

# Parse test results
if ($testOutput -match "Test Files\s+(\d+) passed.*?out of (\d+)") {
    $passed = $Matches[1]
    $total = $Matches[2]
    $failed = $total - $passed
    $passRate = [math]::Round(($passed / $total) * 100, 1)
    
    Write-Host "[TEST RESULTS]" -ForegroundColor Yellow
    Write-Host "  Total: $total tests" -ForegroundColor White
    Write-Host "  Passed: $passed ($passRate%)" -ForegroundColor Green
    if ($failed -gt 0) {
        Write-Host "  Failed: $failed" -ForegroundColor Red
    }
    Write-Host ""
}

# Check TypeScript errors
Write-Host "[CHECKING TYPESCRIPT]" -ForegroundColor Yellow
$tscOutput = & npx tsc --noEmit 2>&1 | Out-String
if ($tscOutput -match "Found (\d+) error") {
    $errors = $Matches[1]
    Write-Host "  [ERROR] Found $errors type errors" -ForegroundColor Red
} else {
    Write-Host "  [OK] No type errors" -ForegroundColor Green
}
Write-Host ""

# Summary and recommendations
Write-Host "[RECOMMENDATIONS]" -ForegroundColor Cyan
if ($p0Count -gt 0) {
    Write-Host "  [!] Address P0 critical debt immediately" -ForegroundColor Red
}
if ($p1Count -gt 0) {
    Write-Host "  [*] Plan P1 debt for next iteration" -ForegroundColor Yellow
}
if ($failed -gt 0) {
    Write-Host "  [*] Fix failing test cases" -ForegroundColor Yellow
}
Write-Host "  [i] View details: TECHNICAL_DEBT.md" -ForegroundColor White
Write-Host "  [i] Quick summary: DEBT_SUMMARY.md" -ForegroundColor White
Write-Host ""

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Check complete!" -ForegroundColor Green
