#!/bin/bash

# 离线模式测试脚本

echo "======================================="
echo "Dev Debug Platform - 离线模式测试"
echo "======================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 测试1: 检查vendor目录结构
echo -e "${BLUE}[测试1] 检查vendor目录结构...${NC}"
VENDOR_DIR="src/main/resources/static/vendor"

if [ ! -d "$VENDOR_DIR" ]; then
    echo -e "${RED}❌ vendor目录不存在${NC}"
    exit 1
fi

REQUIRED_DIRS=("bootstrap" "chartjs" "xterm" "prism" "flatpickr" "sockjs" "hammerjs")
for dir in "${REQUIRED_DIRS[@]}"; do
    if [ ! -d "$VENDOR_DIR/$dir" ]; then
        echo -e "${YELLOW}⚠️  缺失目录: $VENDOR_DIR/$dir${NC}"
    else
        echo -e "${GREEN}✅ 目录存在: $dir${NC}"
    fi
done

# 测试2: 检查关键文件
echo -e "\n${BLUE}[测试2] 检查关键资源文件...${NC}"
CRITICAL_FILES=(
    "bootstrap/bootstrap.min.css"
    "bootstrap/bootstrap.bundle.min.js"
    "chartjs/chart.umd.js"
    "xterm/xterm.css"
    "xterm/xterm.js"
)

MISSING_COUNT=0
for file in "${CRITICAL_FILES[@]}"; do
    if [ ! -f "$VENDOR_DIR/$file" ]; then
        echo -e "${RED}❌ 缺失文件: $file${NC}"
        ((MISSING_COUNT++))
    else
        SIZE=$(ls -lh "$VENDOR_DIR/$file" | awk '{print $5}')
        echo -e "${GREEN}✅ 文件存在: $file ($SIZE)${NC}"
    fi
done

# 测试3: 检查配置文件
echo -e "\n${BLUE}[测试3] 检查离线配置文件...${NC}"
if [ ! -f "src/main/resources/application-offline.properties" ]; then
    echo -e "${RED}❌ 离线配置文件不存在${NC}"
else
    echo -e "${GREEN}✅ 离线配置文件存在${NC}"
    # 检查关键配置项
    if grep -q "app.offline.enabled=true" src/main/resources/application-offline.properties; then
        echo -e "${GREEN}✅ 离线模式已启用${NC}"
    else
        echo -e "${YELLOW}⚠️  离线模式配置可能有问题${NC}"
    fi
fi

# 测试4: 检查Java类
echo -e "\n${BLUE}[测试4] 检查Java配置类...${NC}"
if [ ! -f "src/main/java/com/cmict/internalpaas/config/OfflineDeploymentConfig.java" ]; then
    echo -e "${RED}❌ OfflineDeploymentConfig.java 不存在${NC}"
else
    echo -e "${GREEN}✅ OfflineDeploymentConfig.java 存在${NC}"
fi

if [ ! -f "src/main/java/com/cmict/internalpaas/service/OfflineDeploymentService.java" ]; then
    echo -e "${RED}❌ OfflineDeploymentService.java 不存在${NC}"
else
    echo -e "${GREEN}✅ OfflineDeploymentService.java 存在${NC}"
fi

# 测试5: 检查部署脚本
echo -e "\n${BLUE}[测试5] 检查部署脚本...${NC}"
DEPLOY_SCRIPTS=("deploy-offline.sh" "deploy-offline.bat" "scripts/download-vendor-libs.sh" "scripts/download-vendor-libs.bat")
for script in "${DEPLOY_SCRIPTS[@]}"; do
    if [ ! -f "$script" ]; then
        echo -e "${YELLOW}⚠️  脚本缺失: $script${NC}"
    else
        echo -e "${GREEN}✅ 脚本存在: $script${NC}"
    fi
done

# 测试总结
echo -e "\n${BLUE}[测试总结]${NC}"
if [ $MISSING_COUNT -eq 0 ]; then
    echo -e "${GREEN}🎉 所有关键资源文件都已就绪！${NC}"
    echo -e "${GREEN}✅ 可以使用离线模式部署${NC}"
    echo ""
    echo -e "${BLUE}下一步操作：${NC}"
    echo "1. 如果缺少vendor文件，运行: scripts/download-vendor-libs.sh"
    echo "2. 编译离线版本: mvn clean package -Poffline -DskipTests"
    echo "3. 启动测试: java -jar -Dspring.profiles.active=offline target/internalpaas-*.jar"
else
    echo -e "${YELLOW}⚠️  发现 $MISSING_COUNT 个缺失的关键文件${NC}"
    echo -e "${YELLOW}请先运行资源下载脚本：${NC}"
    echo "   scripts/download-vendor-libs.sh (Linux/Mac)"
    echo "   scripts/download-vendor-libs.bat (Windows)"
fi

echo ""
echo -e "${BLUE}完整的离线部署流程：${NC}"
echo "1. 下载资源: ./scripts/download-vendor-libs.sh"
echo "2. 离线部署: ./deploy-offline.sh"
echo "3. 启动应用: ./start-offline.sh"