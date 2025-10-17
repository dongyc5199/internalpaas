# Dev Debug Platform 运维手册

## 📋 目录
- [日常运维](#日常运维)
- [备份与恢复](#备份与恢复)
- [安全管理](#安全管理)
- [故障处理](#故障处理)
- [性能调优](#性能调优)
- [版本升级](#版本升级)
- [监控告警](#监控告警)

## 🔧 日常运维

### 1. 应用状态检查

#### 1.1 快速状态检查
```bash
#!/bin/bash
# daily-check.sh - 每日状态检查脚本

echo "=== Dev Debug Platform 状态检查 ==="
echo "检查时间: $(date)"
echo

# 1. 检查应用进程
echo "1. 应用进程状态:"
if pgrep -f "internalpaas.*jar" > /dev/null; then
    PID=$(pgrep -f "internalpaas.*jar")
    echo "   ✅ 应用正在运行 (PID: $PID)"
    
    # 检查内存使用
    MEMORY=$(ps -p $PID -o %mem --no-headers | tr -d ' ')
    echo "   📊 内存使用率: ${MEMORY}%"
    
    # 检查CPU使用
    CPU=$(ps -p $PID -o %cpu --no-headers | tr -d ' ')
    echo "   ⚡ CPU使用率: ${CPU}%"
else
    echo "   ❌ 应用未运行"
fi

echo

# 2. 检查端口监听
echo "2. 端口监听状态:"
if netstat -tuln | grep :8080 > /dev/null; then
    echo "   ✅ 端口8080正在监听"
else
    echo "   ❌ 端口8080未监听"
fi

echo

# 3. 检查磁盘空间
echo "3. 磁盘空间检查:"
df -h | grep -E "(/$|/opt)" | while read line; do
    USAGE=$(echo $line | awk '{print $5}' | sed 's/%//')
    MOUNT=$(echo $line | awk '{print $6}')
    
    if [ $USAGE -gt 80 ]; then
        echo "   ⚠️  $MOUNT 磁盘使用率: ${USAGE}% (警告)"
    elif [ $USAGE -gt 90 ]; then
        echo "   ❌ $MOUNT 磁盘使用率: ${USAGE}% (危险)"
    else
        echo "   ✅ $MOUNT 磁盘使用率: ${USAGE}%"
    fi
done

echo

# 4. 检查应用健康状态
echo "4. 应用健康检查:"
if curl -s -f http://localhost:8080 > /dev/null; then
    echo "   ✅ HTTP服务正常"
else
    echo "   ❌ HTTP服务异常"
fi

echo

# 5. 检查日志文件大小
echo "5. 日志文件检查:"
LOG_DIR="/opt/devplatform/logs"
if [ -d "$LOG_DIR" ]; then
    find "$LOG_DIR" -name "*.log" -type f | while read logfile; do
        SIZE=$(du -h "$logfile" | cut -f1)
        echo "   📄 $(basename $logfile): $SIZE"
    done
else
    echo "   ⚠️  日志目录不存在: $LOG_DIR"
fi

echo
echo "=== 检查完成 ==="
```

#### 1.2 自动化检查任务
```bash
# 添加到crontab，每小时执行一次
0 * * * * /opt/devplatform/scripts/daily-check.sh >> /opt/devplatform/logs/health-check.log 2>&1
```

### 2. 日志管理

#### 2.1 日志轮换脚本
```bash
#!/bin/bash
# log-rotation.sh - 日志轮换脚本

LOG_DIR="/opt/devplatform/logs"
BACKUP_DIR="/opt/devplatform/backup/logs"
RETENTION_DAYS=30

# 创建备份目录
mkdir -p "$BACKUP_DIR"

# 压缩并移动旧日志
find "$LOG_DIR" -name "*.log" -mtime +1 -type f | while read logfile; do
    filename=$(basename "$logfile")
    datestamp=$(date +%Y%m%d_%H%M%S)
    
    # 压缩并移动
    gzip -c "$logfile" > "$BACKUP_DIR/${filename}_${datestamp}.gz"
    
    # 清空原文件（保持文件句柄）
    > "$logfile"
    
    echo "已轮换日志: $filename"
done

# 清理超过保留期的备份
find "$BACKUP_DIR" -name "*.gz" -mtime +$RETENTION_DAYS -delete

echo "日志轮换完成"
```

#### 2.2 日志分析脚本
```bash
#!/bin/bash
# log-analysis.sh - 日志分析脚本

LOG_FILE="/opt/devplatform/logs/application.log"
TODAY=$(date +%Y-%m-%d)

echo "=== $TODAY 日志分析 ==="

# 错误统计
echo "1. 错误统计:"
ERROR_COUNT=$(grep -c "ERROR" "$LOG_FILE" 2>/dev/null || echo "0")
WARN_COUNT=$(grep -c "WARN" "$LOG_FILE" 2>/dev/null || echo "0")
echo "   错误(ERROR): $ERROR_COUNT"
echo "   警告(WARN): $WARN_COUNT"

# 用户活动统计
echo
echo "2. 用户活动:"
LOGIN_COUNT=$(grep -c "用户.*登录" "$LOG_FILE" 2>/dev/null || echo "0")
echo "   登录次数: $LOGIN_COUNT"

# 应用操作统计
echo
echo "3. 应用操作:"
UPLOAD_COUNT=$(grep -c "上传应用" "$LOG_FILE" 2>/dev/null || echo "0")
START_COUNT=$(grep -c "启动应用" "$LOG_FILE" 2>/dev/null || echo "0")
STOP_COUNT=$(grep -c "停止应用" "$LOG_FILE" 2>/dev/null || echo "0")
echo "   应用上传: $UPLOAD_COUNT"
echo "   应用启动: $START_COUNT"
echo "   应用停止: $STOP_COUNT"

# 最近错误
echo
echo "4. 最近错误:"
tail -n 100 "$LOG_FILE" | grep "ERROR" | tail -n 5
```

## 💾 备份与恢复

### 1. 数据备份策略

#### 1.1 数据库备份脚本
```bash
#!/bin/bash
# backup-database.sh - 数据库备份脚本

BACKUP_DIR="/opt/devplatform/backup/database"
DB_PATH="/opt/devplatform/data/platformdb.mv.db"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p "$BACKUP_DIR"

# 停止应用（可选，用于一致性备份）
echo "开始数据库备份..."

# 复制数据库文件
if [ -f "$DB_PATH" ]; then
    cp "$DB_PATH" "$BACKUP_DIR/platformdb_${TIMESTAMP}.mv.db"
    
    # 压缩备份文件
    gzip "$BACKUP_DIR/platformdb_${TIMESTAMP}.mv.db"
    
    echo "数据库备份完成: platformdb_${TIMESTAMP}.mv.db.gz"
else
    echo "数据库文件不存在: $DB_PATH"
    exit 1
fi

# 清理超过7天的备份
find "$BACKUP_DIR" -name "*.gz" -mtime +7 -delete

echo "备份任务完成"
```

#### 1.2 应用配置备份
```bash
#!/bin/bash
# backup-config.sh - 配置文件备份脚本

CONFIG_DIR="/opt/devplatform"
BACKUP_DIR="/opt/devplatform/backup/config"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

# 创建配置文件归档
tar -czf "$BACKUP_DIR/config_${TIMESTAMP}.tar.gz" \
    --exclude="logs/*" \
    --exclude="data/*" \
    --exclude="backup/*" \
    -C "$CONFIG_DIR" .

echo "配置备份完成: config_${TIMESTAMP}.tar.gz"

# 清理超过30天的配置备份
find "$BACKUP_DIR" -name "*.tar.gz" -mtime +30 -delete
```

### 2. 数据恢复

#### 2.1 数据库恢复脚本
```bash
#!/bin/bash
# restore-database.sh - 数据库恢复脚本

BACKUP_FILE="$1"
DB_PATH="/opt/devplatform/data/platformdb.mv.db"

if [ -z "$BACKUP_FILE" ]; then
    echo "用法: $0 <备份文件路径>"
    echo "示例: $0 /opt/devplatform/backup/database/platformdb_20240824_120000.mv.db.gz"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "备份文件不存在: $BACKUP_FILE"
    exit 1
fi

echo "警告: 此操作将覆盖当前数据库！"
read -p "确认继续? (y/N): " confirm

if [ "$confirm" != "y" ]; then
    echo "操作已取消"
    exit 0
fi

# 停止应用
echo "停止应用..."
systemctl stop devplatform

# 备份当前数据库
if [ -f "$DB_PATH" ]; then
    mv "$DB_PATH" "${DB_PATH}.backup.$(date +%Y%m%d_%H%M%S)"
fi

# 恢复数据库
echo "恢复数据库..."
if [[ "$BACKUP_FILE" == *.gz ]]; then
    gunzip -c "$BACKUP_FILE" > "$DB_PATH"
else
    cp "$BACKUP_FILE" "$DB_PATH"
fi

# 启动应用
echo "启动应用..."
systemctl start devplatform

echo "数据库恢复完成"
```

## 🔐 安全管理

### 1. 安全检查清单

#### 1.1 定期安全检查
```bash
#!/bin/bash
# security-check.sh - 安全检查脚本

echo "=== 安全检查报告 ==="
echo "检查时间: $(date)"
echo

# 1. 检查文件权限
echo "1. 文件权限检查:"
CONFIG_FILES=(
    "/opt/devplatform/application-prod.yml"
    "/opt/devplatform/.env"
)

for file in "${CONFIG_FILES[@]}"; do
    if [ -f "$file" ]; then
        PERMS=$(stat -c "%a" "$file")
        if [ "$PERMS" = "600" ]; then
            echo "   ✅ $file: $PERMS (安全)"
        else
            echo "   ⚠️  $file: $PERMS (建议设置为600)"
        fi
    fi
done

# 2. 检查默认密码
echo
echo "2. 默认密码检查:"
if grep -q "admin123" /opt/devplatform/application*.yml 2>/dev/null; then
    echo "   ❌ 检测到默认密码，请立即修改"
else
    echo "   ✅ 未检测到默认密码"
fi

# 3. 检查开放端口
echo
echo "3. 开放端口检查:"
netstat -tuln | grep LISTEN | while read line; do
    PORT=$(echo $line | awk '{print $4}' | sed 's/.*://')
    echo "   🔍 监听端口: $PORT"
done

# 4. 检查SSL证书（如果配置了HTTPS）
echo
echo "4. SSL证书检查:"
if netstat -tuln | grep :443 > /dev/null; then
    echo "   🔍 HTTPS端口已开启"
    # 这里可以添加证书到期检查
else
    echo "   ℹ️  未配置HTTPS"
fi

echo
echo "=== 安全检查完成 ==="
```

#### 1.2 安全加固建议
```bash
# 1. 修改默认端口
sed -i 's/port: 8080/port: 8888/' application-prod.yml

# 2. 配置防火墙
ufw enable
ufw allow 8888/tcp
ufw allow ssh

# 3. 设置文件权限
chmod 700 /opt/devplatform
chmod 600 /opt/devplatform/application*.yml
chmod 600 /opt/devplatform/.env

# 4. 创建专用用户
useradd -r -s /bin/false devplatform
chown -R devplatform:devplatform /opt/devplatform
```

### 2. 访问控制

#### 2.1 IP白名单配置
```yaml
# application-prod.yml
server:
  address: 192.168.1.100  # 只绑定内网IP
  
management:
  endpoints:
    web:
      exposure:
        include: health,info
  server:
    address: 127.0.0.1  # 管理端点只允许本地访问
```

## 🚨 故障处理

### 1. 常见故障诊断

#### 1.1 应用无法启动
```bash
# 故障诊断步骤
echo "=== 应用启动故障诊断 ==="

# 1. 检查端口占用
echo "1. 检查端口占用:"
netstat -tuln | grep :8080

# 2. 检查内存使用
echo "2. 检查内存使用:"
free -h

# 3. 检查磁盘空间
echo "3. 检查磁盘空间:"
df -h

# 4. 检查Java版本
echo "4. 检查Java版本:"
java -version

# 5. 查看启动日志
echo "5. 查看启动日志:"
tail -n 50 /opt/devplatform/logs/application.log
```

#### 1.2 性能问题诊断
```bash
#!/bin/bash
# performance-diagnosis.sh - 性能诊断脚本

PID=$(pgrep -f "internalpaas.*jar")

if [ -z "$PID" ]; then
    echo "应用未运行"
    exit 1
fi

echo "=== 性能诊断报告 ==="
echo "应用PID: $PID"
echo "诊断时间: $(date)"
echo

# 1. CPU使用率
echo "1. CPU使用情况:"
top -p $PID -n 1 -b | tail -n +8

# 2. 内存使用
echo
echo "2. 内存使用情况:"
jmap -histo $PID | head -n 20

# 3. GC状态
echo
echo "3. GC状态:"
jstat -gc $PID

# 4. 线程状态
echo
echo "4. 线程状态:"
jstack $PID | grep "java.lang.Thread.State" | sort | uniq -c

# 5. 网络连接
echo
echo "5. 网络连接:"
netstat -an | grep :8080 | wc -l
echo "当前连接数"
```

### 2. 应急处理流程

#### 2.1 应急重启脚本
```bash
#!/bin/bash
# emergency-restart.sh - 应急重启脚本

LOG_FILE="/opt/devplatform/logs/emergency.log"
echo "$(date): 开始应急重启" >> "$LOG_FILE"

# 1. 获取当前状态
PID=$(pgrep -f "internalpaas.*jar")
if [ ! -z "$PID" ]; then
    echo "$(date): 发现应用进程 PID: $PID" >> "$LOG_FILE"
    
    # 2. 优雅停止
    echo "$(date): 尝试优雅停止应用" >> "$LOG_FILE"
    kill -TERM $PID
    
    # 等待30秒
    sleep 30
    
    # 3. 强制停止
    if pgrep -f "internalpaas.*jar" > /dev/null; then
        echo "$(date): 强制停止应用" >> "$LOG_FILE"
        kill -9 $PID
    fi
fi

# 4. 清理临时文件
rm -f /tmp/*.tmp
rm -f /opt/devplatform/data/*.lock

# 5. 启动应用
echo "$(date): 启动应用" >> "$LOG_FILE"
systemctl start devplatform

# 6. 验证启动
sleep 10
if pgrep -f "internalpaas.*jar" > /dev/null; then
    echo "$(date): 应用启动成功" >> "$LOG_FILE"
else
    echo "$(date): 应用启动失败" >> "$LOG_FILE"
fi
```

## ⚡ 性能调优

### 1. JVM调优参数

#### 1.1 生产环境JVM配置
```bash
# 高性能配置（8GB内存服务器）
JAVA_OPTS="
  -Xms2g -Xmx6g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=100
  -XX:+UseStringDeduplication
  -XX:+OptimizeStringConcat
  -XX:+UseCompressedOops
  -XX:+UseCompressedClassPointers
  -Djava.security.egd=file:/dev/./urandom
  -Dfile.encoding=UTF-8
  -Djava.awt.headless=true
"

# GC日志配置
GC_OPTS="
  -Xlog:gc:gc.log:time,tags
  -XX:+UseGCLogFileRotation
  -XX:NumberOfGCLogFiles=10
  -XX:GCLogFileSize=10M
"
```

#### 1.2 应用级优化配置
```yaml
# application-prod.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 25
          fetch_size: 100
        order_inserts: true
        order_updates: true
        cache:
          use_second_level_cache: true
          use_query_cache: true

server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
    accept-count: 100
    max-connections: 8192
    connection-timeout: 20000
```

### 2. 监控指标

#### 2.1 关键指标监控
```bash
#!/bin/bash
# metrics-monitor.sh - 指标监控脚本

PID=$(pgrep -f "internalpaas.*jar")
METRICS_FILE="/opt/devplatform/logs/metrics.log"
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")

if [ -z "$PID" ]; then
    echo "$TIMESTAMP,APP_DOWN" >> "$METRICS_FILE"
    exit 1
fi

# CPU使用率
CPU=$(ps -p $PID -o %cpu --no-headers | tr -d ' ')

# 内存使用率
MEMORY=$(ps -p $PID -o %mem --no-headers | tr -d ' ')

# 堆内存使用
HEAP_INFO=$(jstat -gc $PID | tail -n 1)
HEAP_USED=$(echo $HEAP_INFO | awk '{print $3+$4+$6+$8}')
HEAP_TOTAL=$(echo $HEAP_INFO | awk '{print $1+$2+$5+$7}')

# 线程数
THREAD_COUNT=$(jstack $PID | grep "java.lang.Thread.State" | wc -l)

# 网络连接数
CONN_COUNT=$(netstat -an | grep :8080 | grep ESTABLISHED | wc -l)

# 记录指标
echo "$TIMESTAMP,$CPU,$MEMORY,$HEAP_USED,$HEAP_TOTAL,$THREAD_COUNT,$CONN_COUNT" >> "$METRICS_FILE"
```

## 🔄 版本升级

### 1. 升级流程

#### 1.1 升级前准备
```bash
#!/bin/bash
# pre-upgrade.sh - 升级前准备脚本

echo "=== 升级前准备 ==="

# 1. 备份当前版本
echo "1. 备份当前应用..."
cp /opt/devplatform/internalpaas.jar /opt/devplatform/backup/internalpaas-$(date +%Y%m%d).jar

# 2. 备份数据库
echo "2. 备份数据库..."
/opt/devplatform/scripts/backup-database.sh

# 3. 备份配置文件
echo "3. 备份配置文件..."
/opt/devplatform/scripts/backup-config.sh

# 4. 检查磁盘空间
echo "4. 检查磁盘空间..."
df -h /opt/devplatform

# 5. 记录当前版本信息
echo "5. 记录版本信息..."
java -jar /opt/devplatform/internalpaas.jar --version > /opt/devplatform/logs/pre-upgrade-version.log 2>&1

echo "升级前准备完成"
```

#### 1.2 升级执行
```bash
#!/bin/bash
# upgrade.sh - 升级执行脚本

NEW_JAR="$1"

if [ -z "$NEW_JAR" ]; then
    echo "用法: $0 <新版本JAR文件路径>"
    exit 1
fi

if [ ! -f "$NEW_JAR" ]; then
    echo "文件不存在: $NEW_JAR"
    exit 1
fi

echo "=== 开始升级 ==="

# 1. 停止应用
echo "1. 停止应用..."
systemctl stop devplatform

# 2. 替换JAR文件
echo "2. 替换应用文件..."
cp "$NEW_JAR" /opt/devplatform/internalpaas.jar

# 3. 启动应用
echo "3. 启动应用..."
systemctl start devplatform

# 4. 等待启动完成
echo "4. 等待应用启动..."
sleep 30

# 5. 验证升级
echo "5. 验证升级..."
if curl -s -f http://localhost:8080 > /dev/null; then
    echo "✅ 升级成功"
else
    echo "❌ 升级失败，开始回滚..."
    /opt/devplatform/scripts/rollback.sh
fi
```

#### 1.3 回滚脚本
```bash
#!/bin/bash
# rollback.sh - 回滚脚本

echo "=== 开始回滚 ==="

# 1. 停止应用
systemctl stop devplatform

# 2. 恢复应用文件
BACKUP_JAR=$(ls -t /opt/devplatform/backup/internalpaas-*.jar | head -n 1)
if [ -f "$BACKUP_JAR" ]; then
    cp "$BACKUP_JAR" /opt/devplatform/internalpaas.jar
    echo "应用文件已回滚: $BACKUP_JAR"
else
    echo "未找到备份文件"
    exit 1
fi

# 3. 启动应用
systemctl start devplatform

echo "回滚完成"
```

## 📊 监控告警

### 1. 告警规则配置

#### 1.1 系统资源告警
```bash
#!/bin/bash
# alert-check.sh - 告警检查脚本

ALERT_LOG="/opt/devplatform/logs/alerts.log"
PID=$(pgrep -f "internalpaas.*jar")

# CPU使用率告警
if [ ! -z "$PID" ]; then
    CPU=$(ps -p $PID -o %cpu --no-headers | tr -d ' ')
    if (( $(echo "$CPU > 80" | bc -l) )); then
        echo "$(date): ALERT - CPU使用率过高: ${CPU}%" >> "$ALERT_LOG"
    fi
    
    # 内存使用率告警
    MEMORY=$(ps -p $PID -o %mem --no-headers | tr -d ' ')
    if (( $(echo "$MEMORY > 80" | bc -l) )); then
        echo "$(date): ALERT - 内存使用率过高: ${MEMORY}%" >> "$ALERT_LOG"
    fi
fi

# 磁盘空间告警
df -h | grep -E "(/$|/opt)" | while read line; do
    USAGE=$(echo $line | awk '{print $5}' | sed 's/%//')
    MOUNT=$(echo $line | awk '{print $6}')
    
    if [ $USAGE -gt 85 ]; then
        echo "$(date): ALERT - 磁盘空间不足: $MOUNT ${USAGE}%" >> "$ALERT_LOG"
    fi
done

# 应用状态告警
if [ -z "$PID" ]; then
    echo "$(date): ALERT - 应用进程不存在" >> "$ALERT_LOG"
elif ! curl -s -f http://localhost:8080 > /dev/null; then
    echo "$(date): ALERT - 应用HTTP服务异常" >> "$ALERT_LOG"
fi
```

#### 1.2 告警通知脚本
```bash
#!/bin/bash
# send-alert.sh - 发送告警通知

ALERT_TYPE="$1"
ALERT_MESSAGE="$2"
EMAIL="admin@company.com"

# 发送邮件告警
echo "$ALERT_MESSAGE" | mail -s "Dev Debug Platform Alert: $ALERT_TYPE" "$EMAIL"

# 记录告警日志
echo "$(date): SENT ALERT - $ALERT_TYPE: $ALERT_MESSAGE" >> /opt/devplatform/logs/alert-sent.log
```

### 2. 自动化监控

#### 2.1 监控脚本部署
```bash
# 将监控脚本添加到crontab
# 每5分钟检查一次系统状态
*/5 * * * * /opt/devplatform/scripts/alert-check.sh

# 每小时生成一次性能报告
0 * * * * /opt/devplatform/scripts/metrics-monitor.sh

# 每天凌晨执行备份
0 2 * * * /opt/devplatform/scripts/backup-database.sh
0 3 * * * /opt/devplatform/scripts/backup-config.sh

# 每周执行一次日志清理
0 4 * * 0 /opt/devplatform/scripts/log-rotation.sh
```

---

## 📞 紧急联系方式

**运维团队:**
- 24小时热线: 400-999-8888
- 邮箱: ops@devdebug.com
- 钉钉群: Dev Debug Platform运维群

**升级窗口:**
- 维护时间: 每周日 02:00-04:00
- 紧急升级: 需提前4小时通知

---
*最后更新: 2025-08-24*