# ========================================
# CodeHub Makefile
# ========================================

.PHONY: help
help: ## 显示帮助信息
	@echo "CodeHub - 局域网CI/CD平台"
	@echo ""
	@echo "可用命令:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'

# ========================================
# 环境检查
# ========================================
REQUIRED_GO_VERSION = 1.22.0
CURRENT_GO_VERSION = $(shell go version | awk '{print $$3}' | sed 's/go//')

.PHONY: check-go-version
check-go-version: ## 检查Go版本
	@echo "Required Go version: $(REQUIRED_GO_VERSION)"
	@echo "Current Go version:  $(CURRENT_GO_VERSION)"
	@if [ "$(CURRENT_GO_VERSION)" != "$(REQUIRED_GO_VERSION)" ]; then \
		echo "❌ Go version mismatch!"; \
		echo "Please install Go $(REQUIRED_GO_VERSION)"; \
		exit 1; \
	fi
	@echo "✅ Go version OK"

# ========================================
# 依赖管理
# ========================================
.PHONY: deps
deps: check-go-version ## 下载依赖
	@echo "Downloading dependencies..."
	@go mod download
	@go mod verify
	@echo "✅ Dependencies downloaded"

.PHONY: tidy
tidy: ## 整理依赖
	@go mod tidy

# ========================================
# 构建
# ========================================
VERSION ?= $(shell git describe --tags --always --dirty 2>/dev/null || echo "dev")
BUILD_TIME = $(shell date -u '+%Y-%m-%d_%H:%M:%S')
LDFLAGS = -ldflags "-X main.Version=$(VERSION) -X main.BuildTime=$(BUILD_TIME)"

.PHONY: build
build: check-go-version ## 编译项目
	@echo "Building codehub-server..."
	@CGO_ENABLED=0 go build $(LDFLAGS) -o bin/codehub-server ./cmd/server
	@echo "✅ Build complete: bin/codehub-server"

.PHONY: build-linux
build-linux: check-go-version ## 编译Linux版本
	@echo "Building for Linux..."
	@CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build $(LDFLAGS) -o bin/codehub-server-linux ./cmd/server
	@echo "✅ Build complete: bin/codehub-server-linux"

# ========================================
# 运行
# ========================================
.PHONY: run
run: check-go-version ## 运行开发服务器
	@echo "Starting development server..."
	@go run ./cmd/server

.PHONY: start
start: build ## 编译并运行
	@echo "Starting server..."
	@./bin/codehub-server

# ========================================
# 测试
# ========================================
.PHONY: test
test: check-go-version ## 运行测试
	@echo "Running tests..."
	@go test -v -race -coverprofile=coverage.txt ./...
	@echo "✅ Tests passed"

.PHONY: test-coverage
test-coverage: test ## 生成测试覆盖率报告
	@echo "Generating coverage report..."
	@go tool cover -html=coverage.txt -o coverage.html
	@echo "✅ Coverage report: coverage.html"

# ========================================
# 代码质量
# ========================================
.PHONY: lint
lint: ## 运行代码检查
	@echo "Running linter..."
	@golangci-lint run ./... || echo "⚠️  golangci-lint not installed. Run: go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest"

.PHONY: fmt
fmt: ## 格式化代码
	@echo "Formatting code..."
	@go fmt ./...
	@echo "✅ Code formatted"

.PHONY: vet
vet: ## 运行go vet
	@echo "Running go vet..."
	@go vet ./...
	@echo "✅ Vet passed"

# ========================================
# 安全扫描
# ========================================
.PHONY: vuln-check
vuln-check: ## 扫描安全漏洞
	@echo "Scanning for vulnerabilities..."
	@govulncheck ./... || echo "⚠️  govulncheck not installed. Run: go install golang.org/x/vuln/cmd/govulncheck@latest"

# ========================================
# Docker
# ========================================
.PHONY: docker-build
docker-build: ## 构建Docker镜像
	@echo "Building Docker image..."
	@docker build -t codehub-server:$(VERSION) .
	@echo "✅ Docker image built: codehub-server:$(VERSION)"

.PHONY: docker-run
docker-run: ## 运行Docker容器
	@docker run -p 8880:8880 codehub-server:$(VERSION)

# ========================================
# Docker Compose
# ========================================
.PHONY: up
up: ## 启动所有服务（Docker Compose）
	@echo "Starting all services..."
	@docker-compose up -d
	@echo "✅ All services started"

.PHONY: down
down: ## 停止所有服务
	@echo "Stopping all services..."
	@docker-compose down
	@echo "✅ All services stopped"

.PHONY: logs
logs: ## 查看服务日志
	@docker-compose logs -f

.PHONY: ps
ps: ## 查看服务状态
	@docker-compose ps

# ========================================
# 清理
# ========================================
.PHONY: clean
clean: ## 清理构建产物
	@echo "Cleaning..."
	@rm -rf bin/
	@rm -f coverage.txt coverage.html
	@echo "✅ Cleaned"

.PHONY: clean-all
clean-all: clean ## 清理所有（包括依赖）
	@echo "Cleaning all..."
	@rm -rf vendor/
	@go clean -cache -modcache -testcache
	@echo "✅ All cleaned"

# ========================================
# 开发工具
# ========================================
.PHONY: install-tools
install-tools: ## 安装开发工具
	@echo "Installing development tools..."
	@go install github.com/golangci/golangci-lint/cmd/golangci-lint@latest
	@go install golang.org/x/vuln/cmd/govulncheck@latest
	@echo "✅ Tools installed"

# ========================================
# Git
# ========================================
.PHONY: git-clean
git-clean: ## 清理Git工作区
	@git clean -fdx -e .env -e config/local.yaml

# ========================================
# 默认目标
# ========================================
.DEFAULT_GOAL := help
