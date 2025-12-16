# ========================================
# Multi-stage build - builder
# ========================================
FROM golang:1.22.0-alpine3.19 AS builder

WORKDIR /build

RUN apk add --no-cache git make

ENV GOPROXY=https://goproxy.cn,direct
ENV GO111MODULE=on
ENV CGO_ENABLED=0
ENV GOOS=linux
ENV GOARCH=amd64

COPY go.mod go.sum ./
RUN go mod download && go mod verify

COPY . .

ARG VERSION=dev
ARG BUILD_TIME

RUN go build \
    -ldflags "-s -w -X main.Version=${VERSION} -X main.BuildTime=${BUILD_TIME}" \
    -o codehub-server \
    ./cmd/server

RUN ls -lh codehub-server && ./codehub-server --version || true

# ========================================
# Multi-stage build - runner
# ========================================
FROM alpine:3.19

RUN apk add --no-cache \
    ca-certificates \
    tzdata \
    wget && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

RUN addgroup -g 1000 codehub && \
    adduser -D -u 1000 -G codehub codehub

WORKDIR /app

COPY --from=builder /build/codehub-server .
COPY config/config.yaml /app/config/config.yaml

RUN mkdir -p /app/config /app/logs && \
    chown -R codehub:codehub /app

USER codehub

EXPOSE 8880

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --server-response --quiet --tries=1 --spider http://localhost:8880/health || exit 1

ENTRYPOINT ["./codehub-server"]
