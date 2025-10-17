# Task 3: T5 数据聚合与保留 - 进度报告

## 📋 任务概述

**任务名称**: T5 数据聚合与保留  
**任务编号**: Task 3  
**开始时间**: 2025-10-16  
**当前状态**: 🔄 进行中 (70%完成)

## 🎯 任务目标

实现数据聚合(Rollup)和保留策略(Retention),通过以下机制优化长期数据存储和查询性能:
1. **5分钟 Rollup** - 聚合原始数据,减少存储空间
2. **1小时 Rollup** - 进一步聚合,支持长期历史查询
3. **数据保留策略** - 自动清理过期数据
4. **智能查询路由** - 根据时间范围自动选择最优数据源

## ✅ 已完成工作

### 1. 数据库迁移脚本 ✅

创建了 `V2__create_rollup_tables.sql`:

#### 1.1 5分钟聚合表
```sql
CREATE TABLE server_metric_samples_5m (
    server_id VARCHAR(100),
    metric_name VARCHAR(255),
    time_bucket TIMESTAMP,
    avg_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    min_value DOUBLE PRECISION,
    sample_count INTEGER
);
```

#### 1.2 1小时聚合表
```sql
CREATE TABLE server_metric_samples_1h (
    server_id VARCHAR(100),
    metric_name VARCHAR(255),
    time_bucket TIMESTAMP,
    avg_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    min_value DOUBLE PRECISION,
    sample_count INTEGER
);
```

#### 1.3 索引优化
- 按服务器ID和时间查询: `idx_5m_server_time`, `idx_1h_server_time`
- 按指标名称和时间查询: `idx_5m_metric_time`, `idx_1h_metric_time`
- 唯一约束防止重复聚合: `idx_5m_unique`, `idx_1h_unique`

#### 1.4 监控视图
```sql
CREATE MATERIALIZED VIEW rollup_status AS
SELECT rollup_type, latest_bucket, total_buckets, total_samples
FROM [5m and 1h tables]
```

**文件路径**: `hub/src/main/resources/db/migration/V2__create_rollup_tables.sql`

---

### 2. RollupJobScheduler 服务 ✅

创建了 `RollupJobScheduler.java`:

#### 2.1 5分钟聚合任务
- **Cron**: `0 */5 * * * *` (每5分钟执行)
- **数据范围**: 6-11分钟前(留1分钟缓冲)
- **聚合策略**: AVG, MAX, MIN + 样本计数
- **去重**: ON CONFLICT DO UPDATE

#### 2.2 1小时聚合任务
- **Cron**: `0 0 * * * *` (每小时执行)
- **数据范围**: 1-3小时前
- **数据源**: 从5分钟rollup聚合(更高效)
- **聚合策略**: AVG(avg_value), MAX(max_value), MIN(min_value)

#### 2.3 监控功能
- `getRollupStats()` - 获取聚合统计信息
- `refreshRollupStatus()` - 刷新物化视图(每10分钟)
- 详细的日志记录(emoji + 执行时长)

#### 2.4 配置化
```yaml
metrics.hub.rollup.enabled: true/false
metrics.hub.rollup.5m.cron: "0 */5 * * * *"
metrics.hub.rollup.1h.cron: "0 0 * * * *"
```

**文件路径**: `hub/src/main/java/com/cmict/metricshub/service/RollupJobScheduler.java`

---

### 3. RetentionPolicyService 服务 ✅

创建了 `RetentionPolicyService.java`:

#### 3.1 数据保留策略
- **原始数据**: 30天 (可配置)
- **5分钟rollup**: 90天 (可配置)
- **1小时rollup**: 365天 (可配置)

#### 3.2 自动清理任务
- **Cron**: `0 0 2 * * *` (每天凌晨2点)
- **清理流程**:
  1. 删除超过30天的原始数据
  2. 删除超过90天的5分钟聚合数据
  3. 删除超过365天的1小时聚合数据
  4. 执行VACUUM回收磁盘空间

#### 3.3 监控功能
- `getRetentionStats()` - 获取保留统计信息
  - 各表数据量
  - 最旧数据时间戳
  - 保留期配置

#### 3.4 配置化
```yaml
metrics.hub.retention.enabled: true/false
metrics.hub.retention.cron: "0 0 2 * * *"
metrics.hub.retention.raw-data-days: 30
metrics.hub.retention.5m-rollup-days: 90
metrics.hub.retention.1h-rollup-days: 365
```

**文件路径**: `hub/src/main/java/com/cmict/metricshub/service/RetentionPolicyService.java`

---

### 4. 配置文件更新 ✅

更新了 `application.yaml`:

#### 4.1 新增配置段
```yaml
metrics-hub:
  rollup:
    enabled: false  # Dev环境默认关闭
    5m:
      cron: "0 */5 * * * *"
    1h:
      cron: "0 0 * * * *"
    refresh-status:
      cron: "0 */10 * * * *"

  retention:
    enabled: false  # Dev环境默认关闭
    cron: "0 0 2 * * *"
    raw-data-days: 30
    5m-rollup-days: 90
    1h-rollup-days: 365
```

#### 4.2 生产环境配置
```yaml
spring:
  profiles: prod

metrics-hub:
  rollup:
    enabled: true  # 生产环境启用
  retention:
    enabled: true  # 生产环境启用
```

**文件路径**: `hub/src/main/resources/application.yaml`

---

## 🔧 待完成工作

### 5. 智能查询路由增强 ⏳ (30%剩余)

需要增强 `MetricQueryService` 和 `TsdbReader`:

#### 5.1 查询路由逻辑
```java
public List<MetricSample> query(String serverId, Instant from, Instant to) {
    Duration range = Duration.between(from, to);
    
    if (range.toMinutes() <= 5) {
        return redisReader.query(...);        // 热数据
    } else if (range.toHours() <= 24) {
        return tsdbReader.queryRaw(...);      // 原始数据
    } else if (range.toDays() <= 7) {
        return tsdbReader.query5mRollup(...); // 5分钟聚合
    } else {
        return tsdbReader.query1hRollup(...); // 1小时聚合
    }
}
```

#### 5.2 需要添加的方法
- `TsdbReader.query5mRollup()` - 查询5分钟聚合表
- `TsdbReader.query1hRollup()` - 查询1小时聚合表
- `MetricDataSourceSelector.selectRollupLevel()` - 选择聚合级别

---

## 📊 当前状态

### 完成度统计

| 组件 | 状态 | 完成度 |
|-----|------|--------|
| 数据库迁移脚本 | ✅ 完成 | 100% |
| RollupJobScheduler | ✅ 完成 | 100% |
| RetentionPolicyService | ✅ 完成 | 100% |
| 配置文件 | ✅ 完成 | 100% |
| 智能查询路由 | ⏳ 待完成 | 0% |
| **总体进度** | **🔄 进行中** | **70%** |

### 文件清单

✅ **已创建**:
1. `hub/src/main/resources/db/migration/V2__create_rollup_tables.sql` (93行)
2. `hub/src/main/java/com/cmict/metricshub/service/RollupJobScheduler.java` (220行)
3. `hub/src/main/java/com/cmict/metricshub/service/RetentionPolicyService.java` (260行)

✅ **已修改**:
4. `hub/src/main/resources/application.yaml` (添加rollup/retention配置)

⏳ **待修改**:
5. `hub/src/main/java/com/cmict/metricshub/storage/TsdbReader.java` (需添加rollup查询)
6. `hub/src/main/java/com/cmict/metricshub/service/MetricQueryService.java` (需增强路由)

---

## 🎓 技术亮点

### 1. 时间分桶 (Time Bucketing)
使用PostgreSQL的 `date_trunc` 和模运算实现5分钟/1小时分桶:
```sql
date_trunc('minute', timestamp) - 
    (EXTRACT(MINUTE FROM timestamp)::integer % 5 || ' minutes')::interval
```

### 2. ON CONFLICT 去重
使用PostgreSQL的 `ON CONFLICT DO UPDATE` 防止重复聚合:
```sql
ON CONFLICT (server_id, metric_name, time_bucket) DO UPDATE SET ...
```

### 3. 级联聚合
1小时rollup从5分钟rollup聚合,而非从原始数据,大幅提升效率:
```sql
AVG(avg_value), MAX(max_value), MIN(min_value)
FROM server_metric_samples_5m
```

### 4. 条件Bean加载
使用 `@ConditionalOnProperty` 实现功能开关:
```java
@ConditionalOnProperty(name = "metrics.hub.rollup.enabled", havingValue = "true")
```

### 5. 事务一致性
所有聚合和删除操作使用 `@Transactional` 保证原子性

---

## 🚀 下一步计划

### 短期 (今天内完成)
1. ✅ 完成 `TsdbReader.query5mRollup()` 方法
2. ✅ 完成 `TsdbReader.query1hRollup()` 方法
3. ✅ 增强 `MetricQueryService` 路由逻辑
4. ✅ 编写单元测试
5. ✅ 运行测试验证
6. ✅ 创建最终完成报告

### 中期 (1-2天)
- 性能测试 - 对比查询原始数据 vs rollup数据
- 容量规划 - 评估不同服务器数量下的存储需求
- 监控面板 - 展示rollup状态和retention统计

### 长期优化
- TimescaleDB集成 - 使用 `create_hypertable` 和 `time_bucket`
- 并行聚合 - 多线程处理不同服务器的聚合
- 压缩存储 - TimescaleDB的自动压缩特性

---

## 📝 使用示例

### 1. 启用Rollup (生产环境)

```yaml
# application-prod.yaml
metrics-hub:
  rollup:
    enabled: true
  retention:
    enabled: true
```

### 2. 自定义保留期

```yaml
metrics-hub:
  retention:
    raw-data-days: 15      # 原始数据保留15天
    5m-rollup-days: 60     # 5分钟聚合60天
    1h-rollup-days: 180    # 1小时聚合半年
```

### 3. 调整聚合频率

```yaml
metrics-hub:
  rollup:
    5m:
      cron: "0 */10 * * * *"  # 改为每10分钟聚合一次
    1h:
      cron: "0 30 * * * *"    # 改为每小时30分执行
```

### 4. 查询Rollup状态

```java
@Autowired
private RollupJobScheduler rollupScheduler;

RollupStats stats = rollupScheduler.getRollupStats();
// stats.getLatest5mBucket() - 最新5分钟聚合时间
// stats.getTotal5mBuckets() - 总共5分钟聚合数量
```

### 5. 查询Retention状态

```java
@Autowired
private RetentionPolicyService retentionService;

RetentionStats stats = retentionService.getRetentionStats();
// stats.getRawDataCount() - 原始数据行数
// stats.getOldestRawData() - 最旧原始数据时间
```

---

## ⚠️ 注意事项

### 1. 首次启用
- 首次启用rollup时,需要手动执行一次聚合来处理历史数据
- 或等待定时任务自动回填

### 2. 数据库性能
- 聚合任务会消耗CPU和I/O资源
- 建议在低峰期(凌晨)执行retention清理
- VACUUM操作可能需要较长时间

### 3. 配置调整
- 修改保留期配置不会影响已存在的数据
- 需要等待下次retention任务执行才会生效

### 4. 监控告警
建议设置以下告警:
- Rollup任务执行失败
- Rollup延迟超过阈值(如超过1小时)
- 磁盘空间不足(清理可能失败)

---

## 📈 预期收益

### 存储空间节省
- **原始数据** (15秒采样): ~5.76M 数据点/天/服务器
- **5分钟rollup** (1/20): ~0.29M 数据点/天/服务器 (**节省95%**)
- **1小时rollup** (1/240): ~0.024M 数据点/天/服务器 (**节省99.6%**)

### 查询性能提升
- 查询7天数据: 从扫描 **40M行** 减少到 **2M行** (5分钟rollup)
- 查询30天数据: 从扫描 **173M行** 减少到 **0.7M行** (1小时rollup)
- 查询速度提升 **10-100倍**

### 成本节约
- 100台服务器, 30天保留: 从 **~200GB** 降至 **~20GB** (**节省90%**)
- 1000台服务器, 90天保留: 从 **~5TB** 降至 **~500GB** (**节省90%**)

---

**报告生成时间**: 2025-10-16 23:20  
**报告作者**: GitHub Copilot  
**项目**: InternalPaaS - Metrics Hub (T5)
