#!/bin/bash

# 内网环境离线部署脚本
# 用于在无外网连接的环境中部署应用

set -e  # 遇到错误立即退出

echo "=========================================="
echo "Dev Debug Platform - 内网离线部署脚本"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 步骤1: 检查环境
echo -e "${BLUE}[1/6] 检查部署环境...${NC}"

# 检查Java版本
if ! java -version 2>&1 | grep -q "11\|17\|21"; then
    echo -e "${RED}❌ 需要Java 11及以上版本${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java版本检查通过${NC}"

# 检查Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ 未找到Maven，请先安装Maven${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Maven环境检查通过${NC}"

# 步骤2: 创建vendor目录结构
echo -e "${BLUE}[2/6] 创建本地资源目录结构...${NC}"
mkdir -p src/main/resources/static/vendor/{bootstrap,chartjs,xterm,prism,flatpickr,sockjs,hammerjs}
echo -e "${GREEN}✅ 目录结构创建完成${NC}"

# 步骤3: 检查本地资源文件
echo -e "${BLUE}[3/6] 检查本地资源文件...${NC}"

MISSING_FILES=()

# 检查Bootstrap文件
if [ ! -f "src/main/resources/static/vendor/bootstrap/bootstrap.min.css" ]; then
    MISSING_FILES+=("Bootstrap CSS")
fi
if [ ! -f "src/main/resources/static/vendor/bootstrap/bootstrap.bundle.min.js" ]; then
    MISSING_FILES+=("Bootstrap JS")
fi

# 检查Chart.js文件
if [ ! -f "src/main/resources/static/vendor/chartjs/chart.umd.js" ]; then
    MISSING_FILES+=("Chart.js")
fi

# 检查XTerm.js文件
if [ ! -f "src/main/resources/static/vendor/xterm/xterm.js" ]; then
    MISSING_FILES+=("XTerm.js")
fi

if [ ${#MISSING_FILES[@]} -gt 0 ]; then
    echo -e "${YELLOW}⚠️  发现缺失的本地资源文件:${NC}"
    for file in "${MISSING_FILES[@]}"; do
        echo -e "   - $file"
    done
    echo -e "${YELLOW}请先运行 'scripts/download-vendor-libs.sh' 下载所需文件${NC}"
    
    # 询问是否继续
    read -p "是否继续部署？(y/N): " continue_deploy
    if [[ ! $continue_deploy =~ ^[Yy]$ ]]; then
        echo -e "${RED}部署已取消${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✅ 资源文件检查完成${NC}"

# 步骤4: 编译项目
echo -e "${BLUE}[4/6] 编译项目 (离线模式)...${NC}"
mvn clean package -Poffline -DskipTests=true

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 项目编译失败${NC}"
    exit 1
fi
echo -e "${GREEN}✅ 项目编译成功${NC}"

# 步骤5: 检查生成的JAR文件
echo -e "${BLUE}[5/6] 检查构建产物...${NC}"
JAR_FILE=$(find target -name "*.jar" ! -name "*-sources.jar" | head -1)

if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}❌ 未找到构建的JAR文件${NC}"
    exit 1
fi

JAR_SIZE=$(ls -lh "$JAR_FILE" | awk '{print $5}')
echo -e "${GREEN}✅ JAR文件: $JAR_FILE (大小: $JAR_SIZE)${NC}"

# 步骤6: 生成启动脚本
echo -e "${BLUE}[6/6] 生成启动脚本...${NC}"

# 生成Linux启动脚本
cat > start-offline.sh << 'EOF'
#!/bin/bash

echo "启动 Dev Debug Platform (离线模式)..."

# 检查端口占用
if netstat -tuln | grep -q ":8080 "; then
    echo "❌ 端口8080已被占用，请检查其他程序或修改配置文件中的端口"
    exit 1
fi

# 启动应用
java -jar \
    -Xms1g \
    -Xmx2g \
    -XX:+UseG1GC \
    -Dspring.profiles.active=offline \
    -Dserver.port=8080 \
    target/internalpaas-*.jar

EOF

# 生成Windows启动脚本
cat > start-offline.bat << 'EOF'
@echo off
echo 启动 Dev Debug Platform (离线模式)...

rem 检查Java环境
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ 未找到Java环境，请先安装Java 11或更高版本
    pause
    exit /b 1
)

rem 启动应用
java -jar ^
    -Xms1g ^
    -Xmx2g ^
    -XX:+UseG1GC ^
    -Dspring.profiles.active=offline ^
    -Dserver.port=8080 ^
    target\internalpaas-*.jar

pause
EOF

chmod +x start-offline.sh
chmod +x start-offline.bat

echo -e "${GREEN}✅ 启动脚本生成完成${NC}"

# 部署完成
echo ""
echo -e "${GREEN}=========================================="
echo -e "🎉 内网离线部署完成！"
echo -e "=========================================="
echo -e "📝 部署信息:"
echo -e "   - JAR文件: $JAR_FILE"
echo -e "   - 配置模式: offline (内网模式)"
echo -e "   - 默认端口: 8080"
echo -e "   - 资源模式: 本地资源文件"
echo ""
echo -e "🚀 启动命令:"
echo -e "   Linux/Mac: ./start-offline.sh"
echo -e "   Windows:   start-offline.bat"
echo ""
echo -e "🌐 访问地址:"
echo -e "   http://localhost:8080"
echo -e "   http://[服务器IP]:8080"
echo ""
echo -e "📋 重要说明:"
echo -e "   1. 请确保所有依赖库文件已下载到 vendor/ 目录"
echo -e "   2. 内网环境下所有资源将使用本地文件"
echo -e "   3. 如遇样式问题，请检查 vendor/ 目录下的文件完整性"
echo -e "   4. 数据库文件位于 data/ 目录，请定期备份"
echo -e "${NC}"