# ========================================
# 多阶段构建 - 构建阶段
# ========================================
FROM golang:1.22.0-alpine3.19 AS builder

# 设置工作目录
WORKDIR /build

# 安装构建依赖
RUN apk add --no-cache git make

# 设置Go代理（加速依赖下载）
ENV GOPROXY=https://goproxy.cn,direct
ENV GO111MODULE=on
ENV CGO_ENABLED=0
ENV GOOS=linux
ENV GOARCH=amd64

# 复制go.mod和go.sum，利用Docker缓存
COPY go.mod go.sum ./
RUN go mod download && go mod verify

# 复制源代码
COPY . .

# 构建参数
ARG VERSION=dev
ARG BUILD_TIME

# 编译应用（静态链接）
RUN go build \
    -ldflags "-s -w -X main.Version=${VERSION} -X main.BuildTime=${BUILD_TIME}" \
    -o codehub-server \
    ./cmd/server

# 验证构建产物
RUN ls -lh codehub-server && \
    ./codehub-server --version || true

# ========================================
# 多阶段构建 - 运行阶段
# ========================================
FROM alpine:3.19

# 设置时区和CA证书
RUN apk add --no-cache \
    ca-certificates \
    tzdata \
    wget && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 创建非root用户
RUN addgroup -g 1000 codehub && \
    adduser -D -u 1000 -G codehub codehub

# 设置工作目录
WORKDIR /app

# 从构建阶段复制二进制文件
COPY --from=builder /build/codehub-server .

# 创建必要的目录
RUN mkdir -p /app/config /app/logs && \
    chown -R codehub:codehub /app

# 切换到非root用户
USER codehub

# 暴露端口
EXPOSE 8880

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8880/health || exit 1

# 启动命令
ENTRYPOINT ["./codehub-server"]
