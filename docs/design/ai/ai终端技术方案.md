# MVP 方案（4 周可交付）

> 目标：在**安全可控**前提下，把“自然语言 → 可执行计划 → 受控执行 → 可审计报告”跑通，覆盖 70% 主流运维场景的首发 SOP，默认只读/预演，逐步开闸到半自动/自动。

---
## 一、范围（Scope）
**in-scope（必须交付）**
- Chat → Plan → Policy → Dry-run → Approve → Execute(单机/小批) → Verify → Report 全链路
- SSH 执行器（支持 JumpHost、多路复用）；命令预览与风险标注
- 策略引擎：黑/白名单 + OPA（Rego）基础规则；sudo 命令级白名单；金丝雀/并发上限
- 审计：对话、计划、渲染命令、stdout/stderr、exit code、diff、审批轨迹
- 模板化 SOP（首发 10 条中的 8 条）：
  1) Nginx 5xx 诊断  2) 磁盘 80% 自救  3) 端口/连通性排查  4) 应用崩溃/卡死处理  
  5) 虚机发布/回滚  6) 容器（K8s）灰度/回滚（仅 `--dry-run`/小规模）  
  7) 证书续期（Nginx）  8) 日志旋转与阈值治理
- OS 覆盖：Ubuntu 20.04/22.04、Debian 11、CentOS7/Stream8（以事实差异表驱动渲染）

**out-of-scope（MVP 不做）**
- 磁盘分区/LVM、RAID、内核大版本升级等高危重变更
- 跨 IDC 大规模编排/自愈、复杂审批流（先简化为单级/多人确认）
- 企业级多租户与细粒度审计报表（保留基础字段与导出）

---
## 二、成功标准（Acceptance）
- 高危命令**100%**被拦截或强制审批（含 `rm -rf /`、格式化、关机/重启、全量 `iptables -F` 等）
- 8 条 SOP 在**只读/预演**下零误报（不误报危险）并产出**可执行计划**；其中至少 5 条支持**半自动执行**（单机/小批 / 金丝雀）
- 每次执行都有**结构化报告**与**回滚脚本**，验证探针通过率 ≥ 95%
- 首批试点主机（≥20 台）执行 100+ 次，失败均可定位并回滚

---
## 三、系统与技术选型
- **后端**：Python（FastAPI）+ Runner（异步 Celery/Arq）
- **执行**：系统 OpenSSH（ControlMaster/ControlPersist）；Ansible（优先 `--check/--diff`）；K8s `kubectl --dry-run=server`
- **策略**：OPA（Rego）+ AST/正则静态审计；sudoers 精确到命令与参数
- **数据**：PostgreSQL（计划/审计/运行）、对象存储（日志与附件），WORM 策略可选
- **前端**：聊天 + 命令预览 + 审批弹窗 + 实时流日志；黑/白名单命令高亮
- **模型**：LLM 可插拔（内网推理优先，外部 API 需走网关与脱敏）；Prompt 模板与自检链已内置

---
## 四、关键安全基线
- 默认只读；`--dry-run` 与影子容器试跑优先；任何状态变更必须提供回滚
- 黑名单：`rm -rf /`、`mkfs* /dev/*`、`dd` 覆写、`shutdown|reboot`、无备份 `iptables -F`、`chown -R /` 等
- 风险模板：`rm -rf` 强制前缀与删除样本展示；禁用未绑定变量与隐式通配 `*`
- JIT 权限：短 TTL 证书或 agent forwarding；sudo 仅开放白名单子命令
- 并发与爆炸半径：批量执行强制金丝雀 + 速率限制 + 失败即停

---
## 五、里程碑（4 周冲刺）
**W1：规划与只读闭环**
- [ ] Chat → Plan（LLM）→ Policy（静态审计/OPA）→ 命令预览
- [ ] 本机/影子容器 Dry-run；生成结构化计划 JSON 与工作流 YAML
- 交付：3 条 SOP（Nginx 5xx、磁盘 80%、端口连通）在只读闭环跑通

**W2：连接真实主机与审计**
- [ ] SSH 执行器（跳板/多路复用）；事实采集 `facts()`；审批流（单级）
- [ ] 审计落库与导出；回滚脚本生成器（脚本+说明）
- 交付：再上 2 条 SOP（应用崩溃处理、日志旋转）支持半自动（单机/金丝雀）

**W3：与现有工具整合**
- [ ] Ansible 渲染与 `--check/--diff` 预演；K8s `--dry-run=server`
- [ ] 速率限制/并发与失败即停；验证探针（HTTP、systemd、日志关键字）
- 交付：发布/回滚（虚机）+ K8s 灰度各 1 条 SOP 半自动

**W4：健壮性与看板**
- [ ] 健康检查/回滚闭环；影子环境试跑
- [ ] 观察性与看板：成功率、平均变更时长、爆炸半径、被拦截高危比例
- 交付：证书续期 SOP 半自动；总体演示+复盘报告

---
## 六、SOP 模板（MVP 交付版）
每条 SOP 均包含：`precheck`、`steps{run/retry/verify}`、`risk`、`rollback`、`post-verify`、`artifacts`、`metrics`。

**示例：Nginx 5xx 诊断（摘要）**
```yaml
apiVersion: ops.ai/v1
kind: Workflow
metadata: { name: nginx-5xx-diagnose, sev: P1 }
spec:
  targets: { selector: env=prod, role=web, maxParallel: 1 }
  approvals: { required: false }
  steps:
    - name: precheck
      run: |
        nginx -t || true
        systemctl is-active nginx || true
        journalctl -u nginx -n 200 --since "-10m" || true
    - name: upstream-probe
      run: |
        curl -sS -m 2 -o /dev/null -w "%{http_code}" http://127.0.0.1/health || true
      verify:
        - expectStdout: "200"
    - name: suggest-fix
      run: |
        # 生成修复建议（只读阶段不改动配置）
```

**示例：磁盘 80% 自救（摘要）**
```yaml
apiVersion: ops.ai/v1
kind: Workflow
metadata: { name: disk-autorecover, sev: P1 }
spec:
  steps:
    - name: locate
      run: |
        du -xhd1 / | sort -h | tail -n 20
    - name: rotate
      run: |
        logrotate -f /etc/logrotate.conf || true
        apt-get clean || yum clean all || true
      verify:
        - expect: "df -h / | awk 'NR==2{print $5+0<80}'"
```

> 余下 6 条 SOP 模板按相同结构产出，支持按主机事实（OS、包管理器、是否容器化）渲染差异命令。

---
## 七、API（MVP 子集）
- `POST /plan`：自然语言 → 计划 JSON（含风险/回滚/验证）
- `POST /policy/check`：命令/计划 → 允许/拒绝/需审批 + 解释
- `POST /execute`：工作流执行（流式日志）；`GET /runs/{id}`：状态+工件
- `GET /facts/{host}`：采集主机事实并缓存（TTL）

---
## 八、验证与演示脚本
- 场景 1：用户输入“网关 502，定位并恢复”→ 计划 → 预演 → 执行（重载/回滚）→ 报告
- 场景 2：磁盘报警 90% → 自救清理 → 验证空间回落 → 报告
- 场景 3：灰度发布失败即停 → 自动回滚 → 指标恢复

---
## 九、人员与投入
- 配置：后端 1、执行/安全 1、前端 0.5、SRE 顾问 0.5（可同岗兼任）
- 节奏：双周评审 + 每日站会；PoC 环境与影子环境各一套

---
## 十、主要风险与缓解
- **LLM 误判/幻觉**：计划→自检→策略三道闸；先只读，强依赖验证探针；模板优先
- **跨发行版差异**：事实采集+差异表渲染；关键命令用 Ansible 模块替代手写 shell
- **权限与密钥管理**：短 TTL 证书/agent forwarding；sudo 最小权限；审计全量
- **批量变更爆炸半径**：金丝雀 + 速率限制 + 失败即停 + 自动回滚

---
## 十一、Go/No-Go 准则
- Go：验收项通过、8 条 SOP 半自动稳定、无未闭环的 P0 风险
- No-Go：策略绕过、回滚失败率 > 5%、审计缺失或数据不一致

---
# 概述
目标：做一个“会说人话、敢做事、受约束”的 AI SSH 终端。它把自然语言运维需求拆成安全的命令工作流，在受控前提下自动登陆服务器执行，并产出可审计的结果与回滚方案。

非目标：
- 不是无限制的“万能 root”；默认最小权限与显式授权。
- 不替代 CM/编排（Ansible/Salt/Argo），而是贴合它们，优先复用而不是重造轮子。

---
# 关键用户故事
1. **命令提示**：工程师问“查 nginx 502 的原因”，终端生成诊断步骤与命令，展示风险和预期输出。
2. **一键修复**：描述“把 node14 升级到 18 并回滚可用”，终端给出计划、影响面、回滚点与执行确认。
3. **批量变更**：对一批主机执行 SOP（有审批、有速率限制、有故障回滚）。
4. **审计与取证**：所有对话、计划、命令、 stdout/stderr、退出码、文件差异统一沉淀为审计条目，可导出。

---
# 系统架构（文字）
**前端层**：聊天界面 + 命令预览 + 风险与回滚摘要 + 审批弹窗 + 实时流日志。
**编排层（Orchestrator）**：
- 意图解析（NLU）→ 任务规划（Planner）→ 工作流（Workflow）→ 审批（Approver）→ 执行（Executor）→ 验证（Verifier）→ 报告（Reporter）。
- 策略引擎（Policy）：RBAC、命令白/黑名单、敏感操作保护、环境准入。
**执行层**：
- SSH 执行器（优先复用系统 OpenSSH；支持 Bastion/JumpHost、ControlMaster、多路复用）。
- 工具适配：Ansible、kubectl、systemd、apt/yum/dnf、iptables/nft、journalctl 等。
**安全与合规**：
- 密钥与证书：短 TTL 的签名证书或 Agent Forwarding（不落盘）；支持 JIT 提权。
- 审计：语义计划、渲染后命令、返回流、文件补丁（diff）、宿主信息、审批轨迹。

---
# 核心流程（Chat → Plan → Approve → Execute → Verify → Report）
1. **Plan**：LLM 生成结构化计划（含前置条件、风险、回滚、幂等性说明）。
2. **Policy Check**：静态审计（正则/AST/特征）、策略匹配（OPA/Rego）、权限校验（RBAC）。
3. **Dry-run/影子环境**：支持 `--dry-run`、`--check`，或在影子容器/仿真环境试跑。
4. **Approve**：按策略自动/半自动审批（例如高危命令强制人工确认）。
5. **Execute**：分步执行，失败即停；支持并发与速率限制，采集结构化结果。
6. **Verify**：执行后健康检查（探针/指标/日志关键字）。
7. **Report**：生成摘要、详细日志、回滚脚本与下一步建议。

---
# 工作流 DSL（YAML）
```yaml
apiVersion: ops.ai/v1
kind: Workflow
metadata:
  name: upgrade-node18
  changeTicket: CHG-2025-1021
spec:
  targets:
    selector: env=prod, role=api
    maxParallel: 5
    rateLimitPerMin: 20
  approvals:
    required: true
    reviewers: ["sre-lead"]
  steps:
    - name: precheck
      run: |
        node -v || true
        df -h /
        systemctl is-active api || true
      verify:
        - expectExit: 0
    - name: enable-repo
      run: |
        case $(. /etc/os-release; echo $ID) in
          ubuntu)  apt-get update && apt-get install -y nodejs=18*;;
          centos|rhel) dnf module enable -y nodejs:18 && dnf install -y nodejs;;
        esac
      retry: {max: 2, backoff: 5s}
    - name: restart-service
      run: |
        systemctl restart api && systemctl is-active api
      verify:
        - expectStdout: "active"
    - name: rollback
      when: failed()
      run: |
        # 回滚逻辑（固定版本/快照）
```

**特性**：条件分支、并发与速率、重试、验证器、失败分支、审批关口。

---
# Agent 工具集（Toolbox）与接口
- `exec_shell(hosts, cmd, timeout, env)`：幂等执行（带 TTY/无 TTY），强制 `set -euo pipefail`。
- `fetch_file(host, path)` / `push_file(host, path, content, mode)`：上传下载，SHA256 校验。
- `edit_file(host, path, patch)`：统一用补丁（patch）而非整体覆盖，保存备份。
- `systemd(action, unit)`、`package(manager, action, name, version)`、`firewall(action, rule)`、`kube(kind, action, selector, manifest)`。
- `facts(host)`：采集 OS、包管理器、CPU/内存、路径、网卡，供计划渲染。

---
# 风险控制（硬约束，不客气）
- **黑名单**：`rm -rf /`, `:(){ :|:& };:`, `dd if=/dev/zero of=...`, `mkfs* /dev/*`, `shutdown -h now`, `iptables -F`（无备份）、`chown -R root: /` 等。
- **高危模板**：任何 `rm -rf` 必须同时具备精确前缀保护与 `--preserve-root`，并展示删除列表样本。
- **AST/意图分析**：解析命令语法树，禁止隐式通配（`*`）、命令注入、变量未绑定。
- **读写限界**：白名单目录写入；系统目录改动要求审批与备份。
- **JIT 权限**：以角色签发短期令牌；`sudo` 仅对特定命令与参数放行。
- **速率与爆炸半径**：批量执行设并发上限和 `canary`（金丝雀）分批。
- **回滚必备**：任何改变系统状态的步骤必须给出回滚动作与验证。

---
# 与现有工具的整合策略
- **Ansible**：复杂/跨机编排优先生成 Playbook 并执行 `--check`/`--diff` 预演；小型一步命令直接 SSH。
- **Kubernetes**：把自然语言转换为 `kubectl`/`helm`/`kustomize`，默认 `--dry-run=server` 验证；对 Deploy 做逐步滚动并监控。
- **Systemd / 日志**：`systemctl` 与 `journalctl -u <unit> -n 200 --since "-5m"` 形成闭环验证。

---
# MVP（4 周冲刺）
**第 1 周**：聊天→计划→命令预览→策略校验→本机假执行（不登远程）。
**第 2 周**：引入 SSH 执行器（JumpHost/多路复用）、审计存储、审批流。
**第 3 周**：加入 Ansible 渲染与 `--check`、K8s `--dry-run`、批量目标与速率控制。
**第 4 周**：健康检查与回滚、影子容器试跑、看板（成功率、平均变更时长、爆炸半径）。

验收标准：
- 高危命令 100% 被拦截或转入审批。
- 执行完成有结构化报告与回滚脚本。
- 典型 SOP（Nginx 日志排错、系统包升级、证书更新）全自动闭环。

---
# 现实坑点清单（必须直面）
- `sed -i` 在 GNU/BSD 差异；
- `apt`/`yum`/`dnf` 行为差异和锁；
- `sudo` 需要 `requiretty`/无交互；
- `PATH`/locale/非登录 shell 环境差；
- `iptables` 与 `nft` 并存；
- `systemctl` 返回码与 `restart` 的竞态；
- 代理与私有仓库拉取失败；
- 长命令行/多行脚本的转义与注入风险。

---
# Prompt 模板（片段）
**计划生成**
```
你是资深 SRE。把用户意图转为“可执行运维计划 JSON”。包含：precheck、steps（每步：说明、命令、预期输出、失败处理）、风险、回滚、幂等性、验证。
目标主机事实：{facts}
用户请求：{request}
输出严格 JSON，不要代码块。
```

**自检/对齐**
```
审查下面计划是否：1) 有危险命令；2) 是否幂等；3) 是否提供回滚；4) 是否覆盖验证。输出问题列表与修订建议。
```

---
# Policy（示例 Rego 片段）
```rego
package ops.policy

deny[msg] {
  input.command.matches(/rm\s+-rf\s+\/(\s|$)/)
  msg := "禁止 rm -rf /"
}

deny[msg] {
  input.command.matches(/shutdown|reboot/)
  not input.approved
  msg := "关机/重启需要审批"
}

allow {
  not deny[_]
}
```

---
# API 草案
- `POST /plan`：输入自然语言与主机 facts → 返回计划 JSON。
- `POST /render`：计划 + 主机 facts → 渲染工作流（YAML）。
- `POST /policy/check`：命令/计划 → 返回允许/拒绝/需审批。
- `POST /execute`：工作流 → 执行句柄（可流式日志）。
- `GET /runs/{id}`：状态、日志、工件、回滚脚本。

---
# 代码骨架（Python 伪实现片段）
```python
@dataclass
class Step:
    name: str
    run: str
    verify: list[str] = field(default_factory=list)
    retry: dict | None = None

class SSHExecutor:
    def __init__(self, jump_host: str | None = None):
        ...
    def run(self, host: str, script: str, timeout: int = 120):
        # 用系统 ssh 调用，开启 ControlMaster，多路复用；强制 set -euo pipefail
        ...

class Policy:
    def check(self, cmd: str) -> tuple[bool, str]:
        if re.search(r"rm\s+-rf\s+/\b", cmd):
            return False, "禁止 rm -rf /"
        return True, "ok"

class Runner:
    def __init__(self, policy: Policy, exec: SSHExecutor):
        ...
    def execute(self, host: str, steps: list[Step]):
        for s in steps:
            ok, reason = self.policy.check(s.run)
            if not ok: raise Exception(f"policy deny: {reason}")
            rc, out = self.exec.run(host, f"set -euo pipefail\n{s.run}")
            # 解析 out 做 verify
```

---
# 指标与看板
- **安全**：被拦截高危比例、审批转化率、回滚成功率。
- **质量**：计划→执行一次通过率、验证失败率、漂移检测次数。
- **效率**：平均诊断时间、平均变更时长、并发利用率。

---
# 部署拓扑建议
- 客户端 → 网关（Bastion/审计）→ 业务主机；
- 令牌短 TTL（分钟级），密钥不落盘，启用 SSH CA；
- 审计与日志异步落库（WORM 存储），对接告警。

---
# 下一步建议
- 确认首批 SOP 清单（10 条左右），覆盖：Nginx/系统包/磁盘清理/K8s 滚动更新/证书续期。
- 选定策略强度（默认严，遇阻走审批）。
- 选型：开源/商用 LLM 与内网推理架构、日志与审计存储。
- 搭一个“只读模式”的 PoC，先把命令建议与计划生成打磨到可用，再逐步开闸执行。



---
## 十二、落地建议：拆分与论证（编排层与整体集成）

### 12.1 先线性后 DAG —— 为什么、何时切换
**论证**：
- 90% 的运维 SOP（诊断→修复→验证）天然线性；线性流程**更易审计、易回滚、易做金丝雀**。
- DAG 带来并行度，但也引入**菱形依赖、重试风暴、部分失败的回滚难**，放大会话复杂度与审计难度。
- 故障域控制：线性下**失败即停**天然成立；DAG 需额外编排“失败传播”和**局部补偿**逻辑。
**切换阈值（建议）**：满足以下至少两条再引入 DAG：
- 步骤数 > 8 且平均单步耗时 < 10s（并行能显著缩短总时长）；
- 步骤之间数据/状态互不依赖（幂等/可交换）；
- 验证探针已可细分到**子路径**（能精确判断哪条支路失败）。
**指标**：每次变更的平均时长（MTTC）、回滚成功率、审计重放一致性（随机抽样 5% Run 回放结果一致）。
**实现最小集**：默认线性执行器 + 简单**扇出/扇入**（同类主机批次并行），保留 DAG 拓扑接口但不暴露给 MVP 用户。

### 12.2 先只读/小批（金丝雀）——把爆炸半径写进系统
**论证**：
- 只读（dry-run/影子容器）让**差异集**可见；小批让**真实风险**可控。
- 运维指标通常**噪声较大**（突发流量/缓存冷启动），需要**观察窗口**与**阈值统计**避免误晋级。
**金丝雀模板**：批次 `[1,5,20,rest]`，每批之间**冷却窗口**≥ 2×p95 变更时长。
**晋级条件表达式（示例）**：
```
probe.http_2xx_rate >= 0.99 AND logs.error_per_min <= 1 AND p95_latency_delta <= 10%
```
**稳健判定（Wilson 区间）**：
- 当样本量 n 较小，用 95% 置信的 Wilson 区间下界 ≥ 阈值才晋级，减少“偶然 1 次 200 OK 就放行”的错判。
**指标**：Canary 停表率、晋级平均时长、回滚触发率、窗口内 SLO 漂移（相对基线）。
**实现最小集**：统一 `canary{batches, promoteWhen, cooldown}` 字段，内置 Wilson 判定与**失败即停**策略；所有写操作默认走金丝雀。

### 12.3 优先“类型化动作”，减少裸 Shell —— 为了审计与幂等
**论证**：
- 类型化动作（`service.restart`、`package.install`、`file.patch`、`k8s.apply`…）可做**静态审计**、**跨发行版渲染**与**幂等保障**（例如 `package.install(version)`）。
- 裸 Shell 难以分析意图、容易被通配/注入坑死，回滚与幂等性不透明。
**目标**：MVP 阶段**裸 Shell 占比 < 30%**；优先落 6 类动作：service / package / filepatch / command（受限）/ k8s / ansible。
**实现最小集**：
- 解析层只输出高层意图→编排层**降解为类型化动作**；
- Shell 动作强制 `set -euo pipefail`、禁未绑定变量/隐式通配、写操作需回滚定义；
- `file.patch` 一律**补丁+备份**，禁整文件覆盖。

### 12.4 把“晋级条件”当一等公民 —— 写进 DSL，而不是写在 Wiki
**论证**：
- 变更失败大多源于“条件想当然”与“人工盯指标走神”。
- 把**晋级条件**与**验证探针**写入 DSL，执行器据此**自动停表/回滚/晋级**，人只负责审批与复核。
**实现最小集**：
- DSL 内置 `verify[]` 与 `promoteWhen`（表达式语法）；
- Observability 接口：HTTP 探针、systemd、日志关键字、Prometheus（可选）；
- 表达式库提供运算子：比较、百分比变更、窗口聚合（avg/percentile/rate）。
**指标**：手动干预次数/Run、误晋级率、探针误报/漏报率。

### 12.5 “开源拼装路径”——造轮子慎重、用积木快跑
**解析层**：借鉴 Copilot CLI 的**Agent+工具授权**模式，仅输出**计划+动作序列**，不直接远程执行。
**编排层**：优先对接 Rundeck/StackStorm/AWX 之一做节点库存、作业与 RBAC（MVP 自研轻量编排 + 未来可替换）。
**执行层**：起步 **SSH/Salt-SSH**（无 Agent），随后补 **Ansible**（`--check/--diff`）与 **SSM**（云内主机）。
**安全审计**：可选对接 **Teleport**（零信任/会话录制/证书），或先自研最小“会话+命令+diff”审计。
**K8s 场景**：与 `kubectl/helm` 原生对接，必要时引入 Robusta 做诊断建议。
**利弊权衡**：
- 复用：快、稳、踩坑少，但二次集成成本（数据/权限/UI）要预估；
- 自研：更灵活，但风控与审计很耗时。**MVP 先 80 分复用，后续再 20 分差异化自研**。

### 12.6 多 Runner 一致性与选择策略
**问题**：不同 Runner（SSH/Ansible/K8s/SSM）幂等与预演能力不同。
**策略**：
- 选择器：`if k8s → K8sAction; elif targets>10 or 跨发行版 → AnsibleAction; elif 云内且有代理 → SSMAction; else → SSHAction`。
- 统一接口：`Action.dry_run/execute/rollback`；
- 统一鉴权：JIT 证书 + sudoers 命令白名单；
- 统一审计：stdout/stderr、diff、metrics、approver、batches。
**指标**：Runner 切换失败率、回滚成功率、预演与实操差异率（差分必须 < 5%）。

### 12.7 审批与安全基线（执行前就把坑填了）
**等级**：P0（关机/内核/磁盘/L3 防火墙/批量>10%）、P1（发布/证书/安全补丁）、P2（日志旋转/密钥）。
**策略**：
- P0：**多人审批 + 维护窗口 + 演练记录**；
- P1：单人审批 + 自动回滚；
- P2：自动放行但全量审计。
**硬拦**：`rm -rf /`, `mkfs* /dev/*`, `shutdown|reboot`（无审批）、`iptables -F`（无备份），禁未绑定变量/隐式通配。

### 12.8 指标体系与验收门槛（MVP 必须打表）
- **安全**：被拦截高危比例 ≥ 99%；未授权命令通过率 0；审计完整性（随机抽查 100% 可回放）。
- **质量**：计划→执行一次通过率 ≥ 90%；验证失败率 ≤ 5%；预演/实操差异率 ≤ 5%。
- **效率**：平均变更时长下降 30%；可重复 SOP 单次执行交付 < 10 分钟（含审批）。
- **可用性**：裸 Shell 占比 < 30%；模板覆盖 ≥ 8 条 SOP。

### 12.9 迁移与演进路线图
- **阶段 1（0–1 月）**：只读 + 小批；线性流程；类型化动作占比 ≥ 70%。
- **阶段 2（1–3 月）**：引入 DAG（仅无依赖的并行批）；完善表达式探针与 Prometheus 接口；Runner 扩充到 Ansible/SSM。
- **阶段 3（3–6 月）**：多租户/细粒度审计报表；Teleport 集成；模板市场（SOP 包）。

### 12.10 实验与演练计划（别迷信直觉）
- **Golden Plan**：固定 20 条意图 × 4 种 facts，回归时计划差异 ≤ 5%。
- **Policy Fuzz**：危险命令变体 200 例，**拦截率 100%**。
- **Canary 仿真**：模拟错误率上升、日志飙升、SLO 退化，验证停表与回滚触发；
- **混沌演练**：网络抖动/SSH 断连/部分主机失联，验证分布式锁与失败即停。

