#!/bin/bash

# AI 功能最终测试脚本 (改进的 Session 管理)
BASE_URL="http://localhost:9090"
COOKIE_JAR="test_cookies.txt"

echo "========================================="
echo "AI 功能最终测试"
echo "========================================="
echo ""

# 清理旧 Cookie
rm -f $COOKIE_JAR

# Step 1: 访问登录页获取初始 Cookie 和 CSRF Token
echo "[1/5] 访问登录页面..."
curl -s -c $COOKIE_JAR -b $COOKIE_JAR \
  "$BASE_URL/login" > /dev/null

CSRF_TOKEN=$(cat $COOKIE_JAR | grep XSRF-TOKEN | awk '{print $7}')
echo "✅ CSRF Token: ${CSRF_TOKEN:0:30}..."
echo ""

# Step 2: 执行登录
echo "[2/5] 登录 (root/admin123)..."
curl -s -L -c $COOKIE_JAR -b $COOKIE_JAR \
  -X POST "$BASE_URL/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -d "username=root&password=admin123" \
  > /dev/null

# 检查登录后的 Session Cookie
if grep -q "JSESSIONID" $COOKIE_JAR; then
  echo "✅ 登录成功，已获取 Session"
else
  echo "❌ 登录失败"
  cat $COOKIE_JAR
  exit 1
fi
echo ""

# 更新 CSRF Token (登录后可能会变)
CSRF_TOKEN=$(cat $COOKIE_JAR | grep XSRF-TOKEN | tail -1 | awk '{print $7}')

# Step 3: 测试命令补全 API
echo "[3/5] 测试命令补全 API..."
echo "请求: POST /ai/completion/suggest"

COMPLETION_RESULT=$(curl -s -b $COOKIE_JAR \
  -X POST "$BASE_URL/ai/completion/suggest" \
  -H "Content-Type: application/json" \
  --data '{"prompt":"git "}')

echo "响应:"
if echo "$COMPLETION_RESULT" | grep -q "error"; then
  echo "❌ 测试失败:"
  echo "$COMPLETION_RESULT"
else
  echo "✅ 测试成功:"
  echo "$COMPLETION_RESULT" | head -20
fi
echo ""

# Step 4: 测试 AI 流式对话 API
echo "[4/5] 测试 AI 流式对话 API (Echo 客户端)..."
echo "请求: POST /ai/chat/stream"

echo "SSE 流式响应:"
timeout 3 curl -s -N -b $COOKIE_JAR \
  -X POST "$BASE_URL/ai/chat/stream" \
  -H "Content-Type: application/json" \
  --data '{"message":"你好AI","model":"echo","chatId":"test-123"}' \
  2>/dev/null | head -15 || true

echo ""
echo ""

# Step 5: 检查 AI Demo 页面
echo "[5/5] 检查 AI Demo 页面..."
DEMO_CODE=$(curl -s -b $COOKIE_JAR \
  -o /dev/null -w "%{http_code}" \
  "$BASE_URL/terminal/ai-assist-demo")

if [ "$DEMO_CODE" = "200" ]; then
  echo "✅ AI Demo 页面可访问 (HTTP $DEMO_CODE)"
else
  echo "⚠️  AI Demo 页面状态: HTTP $DEMO_CODE"
fi

echo ""
echo "========================================="
echo "✅ 测试完成！"
echo "========================================="
echo ""
echo "📌 浏览器测试:"
echo "   URL: http://localhost:9090/terminal/ai-assist-demo"
echo "   登录: root / admin123"
echo ""

# 清理
rm -f $COOKIE_JAR
