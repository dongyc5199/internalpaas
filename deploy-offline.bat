@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

echo ==========================================
echo Dev Debug Platform - 内网离线部署脚本
echo ==========================================
echo.

REM 步骤1: 检查环境
echo [1/6] 检查部署环境...

REM 检查Java版本
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 未找到Java环境，请先安装Java 11或更高版本
    pause
    exit /b 1
)
echo ✅ Java环境检查通过

REM 检查Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 未找到Maven，请先安装Maven
    pause
    exit /b 1
)
echo ✅ Maven环境检查通过

REM 步骤2: 创建vendor目录结构
echo [2/6] 创建本地资源目录结构...
mkdir "src\main\resources\static\vendor\bootstrap" 2>nul
mkdir "src\main\resources\static\vendor\chartjs" 2>nul
mkdir "src\main\resources\static\vendor\xterm" 2>nul
mkdir "src\main\resources\static\vendor\prism" 2>nul
mkdir "src\main\resources\static\vendor\flatpickr" 2>nul
mkdir "src\main\resources\static\vendor\sockjs" 2>nul
mkdir "src\main\resources\static\vendor\hammerjs" 2>nul
echo ✅ 目录结构创建完成

REM 步骤3: 检查本地资源文件
echo [3/6] 检查本地资源文件...

set MISSING_COUNT=0

if not exist "src\main\resources\static\vendor\bootstrap\bootstrap.min.css" (
    echo    - 缺失: Bootstrap CSS
    set /a MISSING_COUNT+=1
)
if not exist "src\main\resources\static\vendor\bootstrap\bootstrap.bundle.min.js" (
    echo    - 缺失: Bootstrap JS
    set /a MISSING_COUNT+=1
)
if not exist "src\main\resources\static\vendor\chartjs\chart.umd.js" (
    echo    - 缺失: Chart.js
    set /a MISSING_COUNT+=1
)
if not exist "src\main\resources\static\vendor\xterm\xterm.js" (
    echo    - 缺失: XTerm.js
    set /a MISSING_COUNT+=1
)

if !MISSING_COUNT! gtr 0 (
    echo ⚠️  发现 !MISSING_COUNT! 个缺失的本地资源文件
    echo 请先运行 'scripts\download-vendor-libs.bat' 下载所需文件
    echo.
    set /p continue_deploy=是否继续部署？(y/N): 
    if /i not "!continue_deploy!"=="y" (
        echo 部署已取消
        pause
        exit /b 1
    )
)

echo ✅ 资源文件检查完成

REM 步骤4: 编译项目
echo [4/6] 编译项目 (离线模式)...
call mvn clean package -Poffline -DskipTests=true

if errorlevel 1 (
    echo ❌ 项目编译失败
    pause
    exit /b 1
)
echo ✅ 项目编译成功

REM 步骤5: 检查生成的JAR文件
echo [5/6] 检查构建产物...

for %%f in (target\*.jar) do (
    if not "%%~nf"=="*-sources" (
        set JAR_FILE=%%f
        set JAR_SIZE=%%~zf
    )
)

if not defined JAR_FILE (
    echo ❌ 未找到构建的JAR文件
    pause
    exit /b 1
)

echo ✅ JAR文件: !JAR_FILE! (大小: !JAR_SIZE! 字节)

REM 步骤6: 生成启动脚本
echo [6/6] 生成启动脚本...

REM 生成Windows启动脚本
(
echo @echo off
echo echo 启动 Dev Debug Platform (离线模式^)...
echo.
echo rem 检查Java环境
echo java -version ^>nul 2^>^&1
echo if errorlevel 1 (
echo     echo ❌ 未找到Java环境，请先安装Java 11或更高版本
echo     pause
echo     exit /b 1
echo ^)
echo.
echo rem 启动应用
echo java -jar ^^
echo     -Xms1g ^^
echo     -Xmx2g ^^
echo     -XX:+UseG1GC ^^
echo     -Dspring.profiles.active=offline ^^
echo     -Dserver.port=8080 ^^
echo     target\internalpaas-*.jar
echo.
echo pause
) > start-offline.bat

REM 生成Linux启动脚本
(
echo #!/bin/bash
echo.
echo echo "启动 Dev Debug Platform (离线模式^)..."
echo.
echo # 检查端口占用
echo if netstat -tuln ^| grep -q ":8080 "; then
echo     echo "❌ 端口8080已被占用，请检查其他程序或修改配置文件中的端口"
echo     exit 1
echo fi
echo.
echo # 启动应用
echo java -jar \
echo     -Xms1g \
echo     -Xmx2g \
echo     -XX:+UseG1GC \
echo     -Dspring.profiles.active=offline \
echo     -Dserver.port=8080 \
echo     target/internalpaas-*.jar
) > start-offline.sh

echo ✅ 启动脚本生成完成

REM 部署完成
echo.
echo ==========================================
echo 🎉 内网离线部署完成！
echo ==========================================
echo 📝 部署信息:
echo    - JAR文件: !JAR_FILE!
echo    - 配置模式: offline (内网模式)
echo    - 默认端口: 8080
echo    - 资源模式: 本地资源文件
echo.
echo 🚀 启动命令:
echo    Windows:   start-offline.bat
echo    Linux/Mac: ./start-offline.sh
echo.
echo 🌐 访问地址:
echo    http://localhost:8080
echo    http://[服务器IP]:8080
echo.
echo 📋 重要说明:
echo    1. 请确保所有依赖库文件已下载到 vendor/ 目录
echo    2. 内网环境下所有资源将使用本地文件
echo    3. 如遇样式问题，请检查 vendor/ 目录下的文件完整性
echo    4. 数据库文件位于 data/ 目录，请定期备份
echo.

pause