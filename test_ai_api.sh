#!/bin/bash

# AI 功能测试脚本
BASE_URL="http://localhost:9090"
COOKIE_FILE="cookies.txt"

echo "========================================="
echo "AI 功能集成测试"
echo "========================================="
echo ""

# 步骤 1: 登录获取 Session
echo "[1/3] 正在登录..."
LOGIN_RESPONSE=$(curl -s -c $COOKIE_FILE -X POST \
  "$BASE_URL/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=root&password=admin123" \
  -L -w "\n%{http_code}")

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -1)
if [ "$HTTP_CODE" = "200" ]; then
  echo "✅ 登录成功 (HTTP $HTTP_CODE)"
else
  echo "❌ 登录失败 (HTTP $HTTP_CODE)"
  echo "响应: $LOGIN_RESPONSE"
  exit 1
fi
echo ""

# 步骤 2: 测试命令补全 API
echo "[2/3] 测试命令补全 API..."
echo "请求: POST /ai/completion/suggest"
echo '请求体: {"prompt":"git "}'

COMPLETION_RESPONSE=$(curl -s -b $COOKIE_FILE -X POST \
  "$BASE_URL/ai/completion/suggest" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"git "}')

echo "响应:"
echo "$COMPLETION_RESPONSE" | python -m json.tool 2>/dev/null || echo "$COMPLETION_RESPONSE"
echo ""

# 步骤 3: 测试 AI 流式对话 API
echo "[3/3] 测试 AI 流式对话 API (Echo 客户端)..."
echo "请求: POST /ai/chat/stream"
echo '请求体: {"message":"Hello AI","model":"echo"}'

echo "SSE 流式响应 (前 20 行):"
curl -s -b $COOKIE_FILE -N -X POST \
  "$BASE_URL/ai/chat/stream" \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello AI","model":"echo"}' \
  | head -20

echo ""
echo ""
echo "========================================="
echo "✅ AI 功能测试完成！"
echo "========================================="

# 清理
rm -f $COOKIE_FILE
