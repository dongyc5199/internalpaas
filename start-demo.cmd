@echo off
chcp 65001 >nul
echo ==========================================
echo 🚀 启动工具函数演示
echo ==========================================
echo.

echo [1/3] 构建前端资源...
call npm run build
if %errorlevel% neq 0 (
    echo ❌ 前端构建失败
    pause
    exit /b 1
)
echo ✅ 前端构建成功
echo.

echo [2/3] 启动Spring Boot应用...
echo 提示: 应用启动后，访问 http://localhost:9090/demo/utils
echo.
start /B mvn spring-boot:run

echo [3/3] 等待应用启动...
timeout /t 15 /nobreak >nul

echo.
echo ==========================================
echo ✅ 启动完成！
echo ==========================================
echo.
echo 📋 可访问的演示页面:
echo.
echo   🌟 工具函数演示 (推荐):
echo      http://localhost:9090/demo/utils
echo.
echo   📊 服务器管理:
echo      http://localhost:9090/admin/servers
echo.
echo   📈 管理员仪表板:
echo      http://localhost:9090/admin/dashboard
echo.
echo 💡 提示: 按 Ctrl+C 停止应用
echo ==========================================

start http://localhost:9090/demo/utils

pause
