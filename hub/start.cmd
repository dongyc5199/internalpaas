@echo off
REM ============================================================
REM Metrics Hub - Quick Start Script (Windows)
REM ============================================================

setlocal enabledelayedexpansion

set "COMMAND=%~1"

if "%COMMAND%"=="" (
    call :show_help
    exit /b 1
)

if "%COMMAND%"=="check" call :check_prerequisites
if "%COMMAND%"=="build" call :build
if "%COMMAND%"=="test" call :run_tests
if "%COMMAND%"=="package" call :package
if "%COMMAND%"=="run" call :run
if "%COMMAND%"=="dev" call :dev
if "%COMMAND%"=="docker-build" call :docker_build
if "%COMMAND%"=="docker-run" call :docker_run
if "%COMMAND%"=="health" call :health_check
if "%COMMAND%"=="help" call :show_help

if not defined COMMAND_FOUND (
    echo [ERROR] Unknown command: %COMMAND%
    call :show_help
    exit /b 1
)

exit /b 0

REM ============================================================
REM Functions
REM ============================================================

:check_prerequisites
echo [INFO] Checking prerequisites...
set COMMAND_FOUND=1

where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java is not installed.
    exit /b 1
)

where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven is not installed.
    exit /b 1
)

echo [SUCCESS] Prerequisites check passed
exit /b 0

:build
echo [INFO] Building Metrics Hub...
set COMMAND_FOUND=1
call mvn clean compile
if %ERRORLEVEL% neq 0 exit /b 1
echo [SUCCESS] Build completed
exit /b 0

:run_tests
echo [INFO] Running tests...
set COMMAND_FOUND=1
call mvn test
if %ERRORLEVEL% neq 0 exit /b 1
echo [SUCCESS] Tests completed
exit /b 0

:package
echo [INFO] Packaging application...
set COMMAND_FOUND=1
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 exit /b 1
echo [SUCCESS] Package created
exit /b 0

:run
echo [INFO] Starting Metrics Hub...
set COMMAND_FOUND=1
echo [INFO] Ports:
echo [INFO]   - 8080: Main HTTP API and Actuator
echo [INFO]   - 4317: OTLP gRPC endpoint
echo [INFO]   - 4318: OTLP HTTP endpoint
echo.

if not exist target\metrics-hub-*.jar (
    echo [WARNING] JAR file not found. Building...
    call :package
)

for %%f in (target\metrics-hub-*.jar) do (
    java -jar %%f
)
exit /b 0

:dev
echo [INFO] Starting Metrics Hub in development mode...
set COMMAND_FOUND=1
call mvn spring-boot:run -Dspring-boot.run.profiles=dev
exit /b 0

:docker_build
echo [INFO] Building Docker image...
set COMMAND_FOUND=1

where docker >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Docker is not installed.
    exit /b 1
)

docker build -t metrics-hub:latest .
if %ERRORLEVEL% neq 0 exit /b 1
echo [SUCCESS] Docker image built: metrics-hub:latest
exit /b 0

:docker_run
echo [INFO] Starting Metrics Hub in Docker...
set COMMAND_FOUND=1

docker run -d --name metrics-hub -p 8080:8080 -p 4317:4317 -p 4318:4318 metrics-hub:latest
if %ERRORLEVEL% neq 0 exit /b 1

echo [SUCCESS] Metrics Hub started in Docker
echo [INFO] View logs: docker logs -f metrics-hub
echo [INFO] Stop container: docker stop metrics-hub
echo [INFO] Remove container: docker rm metrics-hub
exit /b 0

:health_check
echo [INFO] Checking Metrics Hub health...
set COMMAND_FOUND=1

curl -f http://localhost:8080/actuator/health
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Health check failed. Is the service running?
    exit /b 1
)

echo [SUCCESS] Service is healthy
exit /b 0

:show_help
set COMMAND_FOUND=1
echo Metrics Hub - Quick Start Script (Windows)
echo.
echo Usage: start.cmd [COMMAND]
echo.
echo Commands:
echo     check           Check prerequisites (Java, Maven)
echo     build           Build the project
echo     test            Run tests
echo     package         Package the application (creates JAR)
echo     run             Run the application (build if needed)
echo     dev             Run in development mode
echo     docker-build    Build Docker image
echo     docker-run      Run in Docker container
echo     health          Check service health
echo     help            Show this help message
echo.
echo Examples:
echo     start.cmd dev
echo     start.cmd package
echo     start.cmd run
echo     start.cmd docker-build
echo     start.cmd health
echo.
exit /b 0
