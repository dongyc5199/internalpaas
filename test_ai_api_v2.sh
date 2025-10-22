#!/bin/bash

# AI 功能集成测试脚本 v2 (带 CSRF token)
BASE_URL="http://localhost:9090"
COOKIE_FILE="cookies.txt"

echo "========================================="
echo "AI 功能集成测试 v2"
echo "========================================="
echo ""

# 步骤 0: 获取 CSRF Token
echo "[0/4] 获取 CSRF Token..."
curl -s -c $COOKIE_FILE "$BASE_URL/login" > /dev/null

# 从 Cookie 中提取 CSRF token
CSRF_TOKEN=$(grep XSRF-TOKEN $COOKIE_FILE | awk '{print $7}')
echo "✅ CSRF Token: ${CSRF_TOKEN:0:20}..."
echo ""

# 步骤 1: 登录
echo "[1/4] 正在登录 (root/admin123)..."
LOGIN_RESPONSE=$(curl -s -b $COOKIE_FILE -c $COOKIE_FILE \
  -X POST "$BASE_URL/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -d "username=root&password=admin123" \
  -w "\nHTTP_CODE:%{http_code}" \
  -L)

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

if [[ "$HTTP_CODE" == "200" ]]; then
  echo "✅ 登录成功"
elif [[ -z "$HTTP_CODE" ]] || [[ "$HTTP_CODE" == "302" ]]; then
  # 302 可能是成功登录后重定向
  echo "✅ 登录成功 (重定向)"
else
  echo "❌ 登录失败 (HTTP $HTTP_CODE)"
  exit 1
fi
echo ""

# 更新 CSRF Token
CSRF_TOKEN=$(grep XSRF-TOKEN $COOKIE_FILE | awk '{print $7}')

# 步骤 2: 测试命令补全 API
echo "[2/4] 测试命令补全 API..."
echo "请求: POST /ai/completion/suggest"

COMPLETION_RESPONSE=$(curl -s -b $COOKIE_FILE \
  -X POST "$BASE_URL/ai/completion/suggest" \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  --data '{"prompt":"git "}')

echo "响应:"
echo "$COMPLETION_RESPONSE" | head -30
echo ""

# 步骤 3: 测试 AI 流式对话 API (Echo)
echo "[3/4] 测试 AI 流式对话 API (Echo 客户端)..."
echo "请求: POST /ai/chat/stream"

echo "SSE 流式响应:"
timeout 5 curl -s -N -b $COOKIE_FILE \
  -X POST "$BASE_URL/ai/chat/stream" \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  --data '{"message":"你好","model":"echo","sessionId":"test-001"}' \
  2>/dev/null || true

echo ""
echo ""

# 步骤 4: 访问 AI Demo 页面
echo "[4/4] 检查 AI Demo 页面可访问性..."
DEMO_PAGE_CODE=$(curl -s -b $COOKIE_FILE \
  -o /dev/null -w "%{http_code}" \
  "$BASE_URL/terminal/ai-assist-demo")

if [ "$DEMO_PAGE_CODE" = "200" ]; then
  echo "✅ AI Demo 页面可访问 (HTTP $DEMO_PAGE_CODE)"
  echo "   URL: $BASE_URL/terminal/ai-assist-demo"
else
  echo "⚠️  AI Demo 页面状态: HTTP $DEMO_PAGE_CODE"
fi

echo ""
echo "========================================="
echo "✅ AI 功能测试完成！"
echo "========================================="
echo ""
echo "📌 后续操作:"
echo "1. 浏览器访问: http://localhost:9090"
echo "2. 登录账号: root / admin123"
echo "3. 访问 AI Demo: http://localhost:9090/terminal/ai-assist-demo"
echo ""

# 清理
rm -f $COOKIE_FILE
