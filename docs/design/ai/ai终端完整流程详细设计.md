# AI终端完整流程详细设计

> 基于技术方案文档,对 **Chat → Plan → Policy → Dry-run → Approve → Execute → Verify → Report** 完整流程进行详细拆分和设计

---

## 目录
- [1. Chat阶段 - 意图解析与上下文管理](#1-chat阶段---意图解析与上下文管理)
- [2. Plan阶段 - 计划生成与结构化](#2-plan阶段---计划生成与结构化)
- [3. Policy阶段 - 策略检查与风险评估](#3-policy阶段---策略检查与风险评估)
- [4. Dry-run阶段 - 预演与影响分析](#4-dry-run阶段---预演与影响分析)
- [5. Approve阶段 - 审批流与权限控制](#5-approve阶段---审批流与权限控制)
- [6. Execute阶段 - 执行编排与监控](#6-execute阶段---执行编排与监控)
- [7. Verify阶段 - 验证与健康检查](#7-verify阶段---验证与健康检查)
- [8. Report阶段 - 报告生成与审计](#8-report阶段---报告生成与审计)
- [9. 整体数据流与接口设计](#9-整体数据流与接口设计)
- [10. 异常处理与回滚机制](#10-异常处理与回滚机制)

---

## 1. Chat阶段 - 意图解析与上下文管理

### 1.1 核心职责
- 接收用户自然语言输入
- 维护对话上下文与会话状态
- 提取运维意图与关键参数
- 识别目标主机/服务

### 1.2 数据结构

```python
@dataclass
class ChatMessage:
    """对话消息"""
    id: str
    session_id: str
    role: Literal["user", "assistant", "system"]
    content: str
    timestamp: datetime
    metadata: dict  # 额外信息:IP、User-Agent等

@dataclass
class ChatSession:
    """对话会话"""
    id: str
    user_id: str
    title: str  # 自动生成或手动命名
    created_at: datetime
    updated_at: datetime
    context: dict  # 上下文信息
    state: Literal["active", "planning", "executing", "completed", "failed"]

@dataclass
class Intent:
    """意图识别结果"""
    type: Literal["diagnose", "fix", "deploy", "rollback", "query", "batch_update"]
    confidence: float  # 0-1置信度
    targets: List[str]  # 目标主机/服务
    parameters: dict  # 提取的参数
    suggested_sop: Optional[str]  # 匹配的SOP模板
    ambiguities: List[str]  # 需要澄清的点
```

### 1.3 处理流程

```python
class ChatProcessor:
    def __init__(self, llm_service, context_manager, sop_matcher):
        self.llm = llm_service
        self.context = context_manager
        self.sop = sop_matcher

    async def process_message(self, session_id: str, user_input: str) -> ChatResponse:
        """处理用户输入"""
        # 1. 加载会话上下文
        session = await self.context.get_session(session_id)
        history = await self.context.get_history(session_id, limit=10)

        # 2. 构建Prompt(包含历史对话、系统信息)
        prompt = self._build_prompt(user_input, history, session.context)

        # 3. LLM意图识别
        intent = await self.llm.parse_intent(prompt)

        # 4. 验证与澄清
        if intent.confidence < 0.7 or intent.ambiguities:
            return self._request_clarification(intent)

        # 5. 匹配SOP模板
        sop_template = await self.sop.match(intent)

        # 6. 提取目标主机facts(异步预加载)
        if intent.targets:
            await self._prefetch_facts(intent.targets)

        # 7. 返回响应,准备进入Plan阶段
        return ChatResponse(
            message="我理解您想要{intent.type},目标是{targets},接下来为您生成执行计划...",
            intent=intent,
            sop_template=sop_template,
            next_stage="plan"
        )

    def _build_prompt(self, user_input: str, history: List[ChatMessage], context: dict) -> str:
        """构建LLM Prompt"""
        return f"""
你是资深SRE助手,负责理解用户运维需求并提取关键信息。

# 历史对话
{self._format_history(history)}

# 当前环境上下文
- 用户角色: {context.get('user_role')}
- 可访问主机: {context.get('accessible_hosts')}
- 当前时间: {datetime.now()}

# 用户输入
{user_input}

# 任务
1. 识别运维意图类型(diagnose/fix/deploy/rollback/query/batch_update)
2. 提取目标主机/服务(支持模糊匹配)
3. 提取关键参数(版本号、配置项、时间范围等)
4. 标注不明确的地方需要澄清
5. 推荐匹配的SOP模板

输出JSON格式:
{{
  "intent_type": "...",
  "confidence": 0.0-1.0,
  "targets": ["host1", "service-x"],
  "parameters": {{}},
  "suggested_sop": "nginx-5xx-diagnose",
  "ambiguities": ["请确认是否需要重启服务?"]
}}
"""
```

### 1.4 API接口

```python
# POST /api/chat/message
{
  "session_id": "sess_123",
  "content": "生产环境nginx返回大量502,帮我排查一下"
}

# Response
{
  "message_id": "msg_456",
  "assistant_reply": "我理解您想要诊断Nginx 502问题,目标环境是生产环境...",
  "intent": {
    "type": "diagnose",
    "confidence": 0.95,
    "targets": ["prod-nginx-*"],
    "suggested_sop": "nginx-5xx-diagnose"
  },
  "next_action": {
    "stage": "plan",
    "plan_preview_url": "/api/plan/preview/plan_789"
  }
}
```

---

## 2. Plan阶段 - 计划生成与结构化

### 2.1 核心职责
- 将意图转换为结构化执行计划
- 生成每个步骤的详细命令
- 定义前置检查、验证条件、回滚方案
- 识别并发机会与依赖关系

### 2.2 数据结构

```python
@dataclass
class ExecutionPlan:
    """执行计划"""
    id: str
    session_id: str
    intent: Intent
    sop_template: Optional[str]

    # 计划元信息
    severity: Literal["P0", "P1", "P2"]  # 影响级别
    estimated_duration: int  # 预估时长(秒)
    blast_radius: str  # 影响范围描述

    # 目标主机
    targets: List[TargetHost]

    # 执行步骤
    steps: List[ExecutionStep]

    # 前置条件
    prechecks: List[Precheck]

    # 回滚策略
    rollback: RollbackStrategy

    # 验证规则
    verification: VerificationRules

    # 并发控制
    concurrency: ConcurrencyConfig

    # 生成时间戳
    created_at: datetime
    created_by: str

@dataclass
class TargetHost:
    """目标主机"""
    hostname: str
    ip: str
    facts: dict  # OS、包管理器、已安装服务等
    tags: List[str]  # env=prod, role=web等

@dataclass
class ExecutionStep:
    """执行步骤"""
    name: str
    order: int
    action_type: Literal["shell", "service", "package", "file", "k8s", "ansible"]

    # 命令/动作定义
    action: Union[ShellAction, ServiceAction, PackageAction, FileAction, K8sAction]

    # 运行条件
    when: Optional[str]  # 条件表达式
    depends_on: List[str]  # 依赖步骤名称

    # 重试策略
    retry: Optional[RetryConfig]

    # 超时设置
    timeout: int  # 秒

    # 预期结果
    expected_output: Optional[str]
    expected_exit_code: int = 0

    # 风险标注
    risk_level: Literal["safe", "moderate", "high", "critical"]
    risk_description: str

    # 验证器
    validators: List[Validator]

@dataclass
class ShellAction:
    """Shell命令动作"""
    script: str  # 脚本内容
    shell: str = "/bin/bash"
    env: dict = field(default_factory=dict)
    working_dir: Optional[str] = None
    sudo: bool = False
    sudo_command_whitelist: Optional[List[str]] = None  # sudo命令白名单

@dataclass
class ServiceAction:
    """服务管理动作"""
    service_name: str
    action: Literal["start", "stop", "restart", "reload", "status"]

@dataclass
class PackageAction:
    """包管理动作"""
    package_name: str
    action: Literal["install", "upgrade", "remove"]
    version: Optional[str] = None

@dataclass
class FileAction:
    """文件操作动作"""
    path: str
    action: Literal["patch", "backup", "restore", "create", "delete"]
    content: Optional[str] = None  # 补丁内容或文件内容
    backup_path: Optional[str] = None
    mode: Optional[str] = None  # 文件权限

@dataclass
class Precheck:
    """前置检查"""
    name: str
    check_type: Literal["command", "file_exists", "service_status", "disk_space", "port_available"]
    command: Optional[str] = None
    expected_result: str
    fail_action: Literal["abort", "warn", "continue"]

@dataclass
class RollbackStrategy:
    """回滚策略"""
    auto_rollback: bool  # 失败时自动回滚
    rollback_steps: List[ExecutionStep]  # 回滚步骤
    rollback_script: Optional[str]  # 生成的回滚脚本
    preserve_state: bool = True  # 是否保存原始状态

@dataclass
class ConcurrencyConfig:
    """并发配置"""
    max_parallel: int = 1  # 最大并发数
    batches: List[int] = field(default_factory=lambda: [1, 5, 20, -1])  # 金丝雀批次
    cooldown_seconds: int = 60  # 批次间冷却时间
    fail_fast: bool = True  # 失败即停
```

### 2.3 计划生成流程

```python
class PlanGenerator:
    def __init__(self, llm_service, fact_collector, sop_repository):
        self.llm = llm_service
        self.facts = fact_collector
        self.sop_repo = sop_repository

    async def generate_plan(self, intent: Intent, session_id: str) -> ExecutionPlan:
        """生成执行计划"""
        # 1. 收集目标主机facts
        target_facts = await self._collect_facts(intent.targets)

        # 2. 加载SOP模板(如果匹配到)
        sop_template = None
        if intent.suggested_sop:
            sop_template = await self.sop_repo.get(intent.suggested_sop)

        # 3. 构建计划生成Prompt
        prompt = self._build_plan_prompt(intent, target_facts, sop_template)

        # 4. LLM生成初步计划
        raw_plan = await self.llm.generate_plan(prompt)

        # 5. 结构化解析
        parsed_plan = self._parse_plan(raw_plan)

        # 6. 命令渲染(根据facts差异化生成)
        rendered_plan = await self._render_commands(parsed_plan, target_facts)

        # 7. 生成回滚策略
        rollback = await self._generate_rollback(rendered_plan, target_facts)

        # 8. 自检与优化
        validated_plan = await self._self_check(rendered_plan, rollback)

        # 9. 风险评估与标注
        final_plan = await self._assess_risks(validated_plan)

        return final_plan

    async def _collect_facts(self, targets: List[str]) -> Dict[str, dict]:
        """收集主机facts"""
        facts = {}
        for target in targets:
            # 并发收集facts
            facts[target] = await self.facts.collect(target)
            # facts包含: OS类型、版本、包管理器、已安装服务、网络配置等
        return facts

    def _build_plan_prompt(self, intent: Intent, facts: dict, sop: Optional[dict]) -> str:
        """构建计划生成Prompt"""
        return f"""
你是资深SRE,负责将运维意图转换为安全、幂等的执行计划。

# 用户意图
类型: {intent.type}
目标: {intent.targets}
参数: {json.dumps(intent.parameters, ensure_ascii=False)}

# 目标主机Facts
{json.dumps(facts, ensure_ascii=False, indent=2)}

# SOP模板参考
{json.dumps(sop, ensure_ascii=False, indent=2) if sop else "无匹配模板,需从零生成"}

# 要求
1. 生成结构化执行计划(JSON格式)
2. 每个步骤必须包含: 说明、命令、预期输出、失败处理
3. 根据不同主机的facts差异化生成命令(如apt vs yum)
4. 标注每步的风险等级(safe/moderate/high/critical)
5. 提供完整的前置检查与回滚方案
6. 确保幂等性(可重复执行)
7. 识别可并行的步骤

# 安全约束
- 禁止: rm -rf /, mkfs*, shutdown, dd覆写等高危命令
- 需审批: 重启服务、批量变更>10台、修改防火墙
- 强制备份: 配置文件修改前必须备份

输出JSON Schema:
{{
  "severity": "P0/P1/P2",
  "estimated_duration": 300,
  "blast_radius": "影响10台生产web服务器",
  "prechecks": [...],
  "steps": [
    {{
      "name": "检查nginx状态",
      "action_type": "shell",
      "action": {{
        "script": "systemctl is-active nginx || true"
      }},
      "risk_level": "safe",
      "validators": [...]
    }}
  ],
  "rollback": {{...}},
  "concurrency": {{
    "max_parallel": 1,
    "batches": [1, 5, 20, -1]
  }}
}}
"""

    async def _render_commands(self, plan: dict, facts: dict) -> ExecutionPlan:
        """根据facts渲染差异化命令"""
        for step in plan['steps']:
            if step['action_type'] == 'shell':
                # 根据OS类型渲染不同命令
                script = step['action']['script']
                rendered = self._render_by_os(script, facts)
                step['action']['script'] = rendered
            elif step['action_type'] == 'package':
                # 根据包管理器渲染
                step['action'] = self._render_package_action(step['action'], facts)
        return plan

    def _render_by_os(self, template: str, facts: dict) -> str:
        """根据OS渲染命令"""
        # 示例: 根据OS类型选择包管理器
        for host, host_facts in facts.items():
            os_id = host_facts.get('os', {}).get('id', 'unknown')
            if os_id in ['ubuntu', 'debian']:
                template = template.replace('{{pkg_manager}}', 'apt-get')
            elif os_id in ['centos', 'rhel']:
                template = template.replace('{{pkg_manager}}', 'yum')
        return template

    async def _generate_rollback(self, plan: ExecutionPlan, facts: dict) -> RollbackStrategy:
        """生成回滚策略"""
        rollback_steps = []

        for step in reversed(plan.steps):
            # 为每个有状态变更的步骤生成回滚动作
            if step.action_type == 'service' and step.action.action == 'restart':
                # 服务重启的回滚: 恢复之前的状态
                rollback_steps.append(ExecutionStep(
                    name=f"rollback_{step.name}",
                    action_type="service",
                    action=ServiceAction(
                        service_name=step.action.service_name,
                        action="restart"  # 简化示例
                    )
                ))
            elif step.action_type == 'file' and step.action.action == 'patch':
                # 文件补丁的回滚: 恢复备份
                rollback_steps.append(ExecutionStep(
                    name=f"rollback_{step.name}",
                    action_type="file",
                    action=FileAction(
                        path=step.action.path,
                        action="restore",
                        backup_path=step.action.backup_path
                    )
                ))

        return RollbackStrategy(
            auto_rollback=True,
            rollback_steps=rollback_steps,
            rollback_script=self._generate_rollback_script(rollback_steps)
        )

    async def _self_check(self, plan: ExecutionPlan, rollback: RollbackStrategy) -> ExecutionPlan:
        """自检与对齐"""
        prompt = f"""
审查下面的执行计划,检查:
1. 是否存在危险命令
2. 是否幂等(可重复执行)
3. 回滚方案是否完整
4. 验证条件是否充分
5. 并发控制是否合理

计划内容:
{json.dumps(plan.dict(), ensure_ascii=False, indent=2)}

输出问题列表与修订建议(JSON格式):
{{
  "issues": ["问题1", "问题2"],
  "suggestions": ["建议1", "建议2"],
  "revised_plan": {{...}}  # 修订后的计划(如果需要)
}}
"""
        review = await self.llm.review_plan(prompt)

        if review.get('issues'):
            # 如果有问题,使用修订后的计划
            return review.get('revised_plan', plan)

        return plan
```

### 2.4 API接口

```python
# POST /api/plan/generate
{
  "intent_id": "intent_123",
  "session_id": "sess_123"
}

# Response
{
  "plan_id": "plan_789",
  "severity": "P1",
  "estimated_duration": 180,
  "blast_radius": "影响5台生产Nginx服务器",
  "steps_count": 6,
  "requires_approval": true,
  "preview_url": "/api/plan/789/preview",
  "steps_summary": [
    {
      "name": "前置检查-Nginx状态",
      "risk_level": "safe"
    },
    {
      "name": "重载Nginx配置",
      "risk_level": "moderate",
      "requires_approval": true
    }
  ]
}

# GET /api/plan/{plan_id}
# 返回完整的ExecutionPlan结构
```

---

## 3. Policy阶段 - 策略检查与风险评估

### 3.1 核心职责
- 静态分析命令安全性
- 执行RBAC权限检查
- 应用OPA策略规则
- 识别高危操作并强制审批
- 生成风险评估报告

### 3.2 数据结构

```python
@dataclass
class PolicyCheckResult:
    """策略检查结果"""
    plan_id: str
    decision: Literal["allow", "deny", "require_approval"]
    overall_risk: Literal["safe", "moderate", "high", "critical"]

    # 检查详情
    checks: List[PolicyCheck]
    violations: List[PolicyViolation]
    warnings: List[str]

    # 审批要求
    approval_required: bool
    approval_reason: str
    required_approvers: List[str]  # 角色或用户

    # 时间戳
    checked_at: datetime

@dataclass
class PolicyCheck:
    """单项策略检查"""
    check_name: str
    check_type: Literal["blacklist", "whitelist", "rbac", "opa", "ast_analysis"]
    passed: bool
    message: str
    severity: Literal["info", "warning", "error", "critical"]

@dataclass
class PolicyViolation:
    """策略违反"""
    step_name: str
    violation_type: str
    description: str
    recommendation: str
    can_override: bool  # 是否可通过审批覆盖

@dataclass
class PolicyRule:
    """策略规则定义"""
    id: str
    name: str
    type: Literal["blacklist", "whitelist", "opa"]
    priority: int  # 优先级,数字越小越高
    enabled: bool

    # 规则内容
    pattern: Optional[str]  # 正则表达式
    opa_policy: Optional[str]  # Rego代码

    # 应用范围
    applies_to: List[str]  # 步骤类型或标签

    # 违规处理
    on_violation: Literal["deny", "warn", "require_approval"]
    approval_roles: List[str]
```

### 3.3 策略检查流程

```python
class PolicyEngine:
    def __init__(self, opa_client, rbac_service, audit_logger):
        self.opa = opa_client
        self.rbac = rbac_service
        self.audit = audit_logger

        # 加载策略规则
        self.rules = self._load_rules()

    async def check_plan(self, plan: ExecutionPlan, user: User) -> PolicyCheckResult:
        """检查执行计划"""
        checks = []
        violations = []
        warnings = []

        # 1. 黑名单检查(硬拦截)
        blacklist_result = await self._check_blacklist(plan)
        checks.append(blacklist_result)
        if not blacklist_result.passed:
            violations.extend(blacklist_result.violations)

        # 2. RBAC权限检查
        rbac_result = await self._check_rbac(plan, user)
        checks.append(rbac_result)
        if not rbac_result.passed:
            violations.append(PolicyViolation(
                step_name="全局",
                violation_type="permission_denied",
                description=f"用户{user.username}无权执行此操作",
                recommendation="请联系管理员授权",
                can_override=False
            ))

        # 3. 命令AST静态分析
        ast_result = await self._analyze_ast(plan)
        checks.append(ast_result)
        warnings.extend(ast_result.warnings)

        # 4. OPA策略评估
        opa_result = await self._evaluate_opa(plan, user)
        checks.append(opa_result)

        # 5. 白名单检查(sudo命令)
        sudo_result = await self._check_sudo_whitelist(plan)
        checks.append(sudo_result)

        # 6. 风险评估
        risk_assessment = self._assess_overall_risk(plan, checks)

        # 7. 决定是否需要审批
        decision, approval_info = self._make_decision(
            plan, violations, risk_assessment, user
        )

        # 8. 记录审计日志
        await self.audit.log_policy_check(plan.id, user.id, decision, checks)

        return PolicyCheckResult(
            plan_id=plan.id,
            decision=decision,
            overall_risk=risk_assessment,
            checks=checks,
            violations=violations,
            warnings=warnings,
            approval_required=(decision == "require_approval"),
            approval_reason=approval_info.get('reason', ''),
            required_approvers=approval_info.get('approvers', []),
            checked_at=datetime.now()
        )

    async def _check_blacklist(self, plan: ExecutionPlan) -> PolicyCheck:
        """黑名单检查"""
        # 危险命令模式
        DANGEROUS_PATTERNS = [
            r'rm\s+-rf\s+/(\s|$)',  # rm -rf /
            r':()\s*{\s*:\|\:&\s*}\s*;\s*:',  # fork炸弹
            r'dd\s+if=/dev/zero\s+of=/dev/',  # dd覆写磁盘
            r'mkfs\.\w+\s+/dev/',  # 格式化磁盘
            r'shutdown|poweroff|halt',  # 关机
            r'reboot',  # 重启(需审批)
            r'iptables\s+-F',  # 清空防火墙(需备份)
            r'chown\s+-R\s+\w+:\w+\s+/',  # 递归改变根目录权限
            r'chmod\s+-R\s+777\s+/',  # 全开权限
        ]

        violations = []

        for step in plan.steps:
            if step.action_type != 'shell':
                continue

            script = step.action.script

            for pattern in DANGEROUS_PATTERNS:
                if re.search(pattern, script, re.IGNORECASE):
                    violations.append(PolicyViolation(
                        step_name=step.name,
                        violation_type="blacklist",
                        description=f"检测到危险命令模式: {pattern}",
                        recommendation="请移除此命令或提供安全的替代方案",
                        can_override=False
                    ))

        return PolicyCheck(
            check_name="黑名单检查",
            check_type="blacklist",
            passed=(len(violations) == 0),
            message=f"发现{len(violations)}个黑名单违规" if violations else "通过",
            severity="critical" if violations else "info"
        )

    async def _check_rbac(self, plan: ExecutionPlan, user: User) -> PolicyCheck:
        """RBAC权限检查"""
        # 检查用户角色与操作权限
        required_permission = self._determine_required_permission(plan)

        has_permission = await self.rbac.check_permission(
            user.id,
            required_permission,
            resource_tags=plan.targets[0].tags if plan.targets else []
        )

        return PolicyCheck(
            check_name="RBAC权限检查",
            check_type="rbac",
            passed=has_permission,
            message="用户有权限执行此操作" if has_permission else "权限不足",
            severity="error" if not has_permission else "info"
        )

    def _determine_required_permission(self, plan: ExecutionPlan) -> str:
        """确定所需权限"""
        # 根据计划严重级别和操作类型确定
        if plan.severity == "P0":
            return "ops:critical_change"
        elif any(step.risk_level == "critical" for step in plan.steps):
            return "ops:high_risk_execute"
        elif len(plan.targets) > 10:
            return "ops:batch_execute"
        else:
            return "ops:execute"

    async def _analyze_ast(self, plan: ExecutionPlan) -> PolicyCheck:
        """AST静态分析"""
        warnings = []

        for step in plan.steps:
            if step.action_type != 'shell':
                continue

            script = step.action.script

            # 1. 检测未绑定变量
            if re.search(r'\$\{?\w+\}?', script):
                if 'set -u' not in script and 'set -euo pipefail' not in script:
                    warnings.append(f"{step.name}: 存在变量但未设置'set -u'保护")

            # 2. 检测隐式通配符
            if re.search(r'\s+\*\s+', script) and 'find' not in script:
                warnings.append(f"{step.name}: 使用了隐式通配符*,可能导致意外扩展")

            # 3. 检测命令注入风险
            if re.search(r'\$\(.*\)', script) or re.search(r'`.*`', script):
                warnings.append(f"{step.name}: 存在命令替换,注意注入风险")

            # 4. 检测管道失败处理
            if '|' in script and 'set -o pipefail' not in script:
                warnings.append(f"{step.name}: 使用管道但未设置'pipefail'")

        return PolicyCheck(
            check_name="AST静态分析",
            check_type="ast_analysis",
            passed=True,  # 警告不阻止执行
            message=f"发现{len(warnings)}个潜在问题",
            severity="warning" if warnings else "info",
            warnings=warnings
        )

    async def _evaluate_opa(self, plan: ExecutionPlan, user: User) -> PolicyCheck:
        """OPA策略评估"""
        # 构建OPA输入
        opa_input = {
            "plan": {
                "id": plan.id,
                "severity": plan.severity,
                "targets": [{"hostname": t.hostname, "tags": t.tags} for t in plan.targets],
                "steps": [
                    {
                        "name": s.name,
                        "action_type": s.action_type,
                        "risk_level": s.risk_level,
                        "command": s.action.script if s.action_type == 'shell' else None
                    }
                    for s in plan.steps
                ]
            },
            "user": {
                "id": user.id,
                "username": user.username,
                "role": user.role
            },
            "context": {
                "timestamp": datetime.now().isoformat(),
                "environment": plan.targets[0].tags.get('env') if plan.targets else None
            }
        }

        # 调用OPA评估
        result = await self.opa.evaluate("ops/policy/allow", opa_input)

        return PolicyCheck(
            check_name="OPA策略评估",
            check_type="opa",
            passed=result.get('allow', False),
            message=result.get('message', 'OPA策略检查'),
            severity="error" if not result.get('allow') else "info"
        )

    async def _check_sudo_whitelist(self, plan: ExecutionPlan) -> PolicyCheck:
        """检查sudo命令白名单"""
        violations = []

        for step in plan.steps:
            if step.action_type == 'shell' and step.action.sudo:
                script = step.action.script

                # 提取sudo命令
                sudo_commands = re.findall(r'sudo\s+(\S+)', script)

                for cmd in sudo_commands:
                    # 检查是否在白名单中
                    if not self._is_sudo_whitelisted(cmd, step.action.sudo_command_whitelist):
                        violations.append(PolicyViolation(
                            step_name=step.name,
                            violation_type="sudo_not_whitelisted",
                            description=f"sudo命令'{cmd}'不在白名单中",
                            recommendation="请添加到白名单或移除sudo",
                            can_override=True
                        ))

        return PolicyCheck(
            check_name="Sudo白名单检查",
            check_type="whitelist",
            passed=(len(violations) == 0),
            message=f"发现{len(violations)}个sudo违规" if violations else "通过",
            severity="warning" if violations else "info"
        )

    def _is_sudo_whitelisted(self, command: str, whitelist: Optional[List[str]]) -> bool:
        """检查sudo命令是否在白名单"""
        # 默认白名单
        DEFAULT_WHITELIST = [
            'systemctl', 'service', 'nginx', 'apt-get', 'yum',
            'journalctl', 'logrotate', 'certbot'
        ]

        allowed = whitelist if whitelist else DEFAULT_WHITELIST
        return any(command.startswith(allowed_cmd) for allowed_cmd in allowed)

    def _assess_overall_risk(self, plan: ExecutionPlan, checks: List[PolicyCheck]) -> str:
        """评估整体风险等级"""
        # 如果有critical检查失败,整体为critical
        if any(c.severity == "critical" and not c.passed for c in checks):
            return "critical"

        # 计算步骤风险分数
        risk_scores = {
            "safe": 0,
            "moderate": 1,
            "high": 3,
            "critical": 10
        }

        total_score = sum(risk_scores.get(step.risk_level, 0) for step in plan.steps)
        avg_score = total_score / len(plan.steps) if plan.steps else 0

        if avg_score >= 5:
            return "critical"
        elif avg_score >= 2:
            return "high"
        elif avg_score >= 1:
            return "moderate"
        else:
            return "safe"

    def _make_decision(
        self,
        plan: ExecutionPlan,
        violations: List[PolicyViolation],
        risk: str,
        user: User
    ) -> Tuple[str, dict]:
        """做出决策"""
        # 有不可覆盖的违规 → deny
        if any(not v.can_override for v in violations):
            return "deny", {"reason": "存在严重安全违规"}

        # P0级别 → 需要审批
        if plan.severity == "P0":
            return "require_approval", {
                "reason": "P0级别变更需要多人审批",
                "approvers": ["sre-lead", "ops-manager"]
            }

        # critical风险 → 需要审批
        if risk == "critical":
            return "require_approval", {
                "reason": "高风险操作需要审批",
                "approvers": ["sre-lead"]
            }

        # 批量操作(>10台) → 需要审批
        if len(plan.targets) > 10:
            return "require_approval", {
                "reason": "批量变更超过10台主机",
                "approvers": ["sre-lead"]
            }

        # 重启/关机操作 → 需要审批
        restart_keywords = ['reboot', 'shutdown', 'restart']
        for step in plan.steps:
            if step.action_type == 'shell':
                if any(kw in step.action.script.lower() for kw in restart_keywords):
                    return "require_approval", {
                        "reason": "包含重启/关机操作",
                        "approvers": ["sre-lead"]
                    }

        # 其他情况 → 允许
        return "allow", {}
```

### 3.4 OPA策略示例(Rego)

```rego
package ops.policy

import future.keywords.if
import future.keywords.in

# 默认拒绝
default allow = false

# 允许条件
allow if {
    not deny_critical_commands
    not deny_unauthorized_user
    not deny_production_without_approval
}

# 拒绝危险命令
deny_critical_commands if {
    some step in input.plan.steps
    step.command
    regex.match(`rm\s+-rf\s+/`, step.command)
}

# 拒绝未授权用户
deny_unauthorized_user if {
    input.plan.severity == "P0"
    not input.user.role in ["admin", "sre-lead"]
}

# 生产环境需要审批
deny_production_without_approval if {
    some target in input.plan.targets
    "env=prod" in target.tags
    input.plan.severity in ["P0", "P1"]
    not input.approved
}

# 批量操作限制
deny_batch_limit if {
    count(input.plan.targets) > 50
    not input.user.role == "admin"
}
```

### 3.5 API接口

```python
# POST /api/policy/check
{
  "plan_id": "plan_789",
  "user_id": "user_123"
}

# Response
{
  "decision": "require_approval",
  "overall_risk": "high",
  "approval_required": true,
  "approval_reason": "P1级别变更需要审批",
  "required_approvers": ["sre-lead"],
  "checks": [
    {
      "check_name": "黑名单检查",
      "passed": true,
      "severity": "info"
    },
    {
      "check_name": "RBAC权限检查",
      "passed": true,
      "severity": "info"
    }
  ],
  "violations": [],
  "warnings": [
    "步骤2: 使用了隐式通配符*"
  ]
}
```

---

## 4. Dry-run阶段 - 预演与影响分析

### 4.1 核心职责
- 在不改变系统状态的情况下预演执行
- 分析变更的影响范围
- 生成预期变更清单(diff)
- 检测潜在冲突与风险
- 支持影子环境试跑

### 4.2 数据结构

```python
@dataclass
class DryRunResult:
    """Dry-run结果"""
    plan_id: str
    run_id: str
    mode: Literal["local", "shadow_container", "remote_check"]

    # 执行结果
    success: bool
    steps_executed: int
    steps_total: int

    # 变更预览
    changes: List[ChangePreview]

    # 影响分析
    impact_analysis: ImpactAnalysis

    # 检测到的问题
    issues: List[DryRunIssue]
    warnings: List[str]

    # 执行详情
    step_results: List[StepDryRunResult]

    # 时间信息
    started_at: datetime
    completed_at: datetime
    duration_seconds: float

@dataclass
class ChangePreview:
    """变更预览"""
    target_host: str
    change_type: Literal["file_modified", "file_created", "file_deleted",
                         "service_restarted", "package_installed", "config_changed"]
    resource: str  # 文件路径、服务名等

    # 变更内容
    before: Optional[str]  # 变更前内容
    after: Optional[str]   # 变更后内容
    diff: Optional[str]    # 差异

    # 影响评估
    impact_score: float  # 0-10
    reversible: bool

@dataclass
class ImpactAnalysis:
    """影响分析"""
    affected_hosts_count: int
    affected_services: List[str]
    estimated_downtime_seconds: int

    # 依赖分析
    downstream_services: List[str]  # 下游依赖服务
    upstream_services: List[str]    # 上游依赖服务

    # 风险评分
    risk_score: float  # 0-100
    confidence: float  # 置信度 0-1
```

### 4.3 API接口

```python
# POST /api/dry-run/execute
{
  "plan_id": "plan_789",
  "mode": "remote_check"  # local/shadow_container/remote_check
}

# Response
{
  "run_id": "dryrun_abc123",
  "success": true,
  "steps_executed": 6,
  "steps_total": 6,
  "duration_seconds": 45.2,
  "changes_count": 3,
  "impact_analysis": {
    "affected_hosts_count": 5,
    "affected_services": ["nginx", "api-service"],
    "estimated_downtime_seconds": 10,
    "risk_score": 35.5
  },
  "issues": [],
  "warnings": ["步骤3: 配置文件将被修改"],
  "details_url": "/api/dry-run/dryrun_abc123/details"
}
```

---

## 5. Approve阶段 - 审批流与权限控制

### 5.1 核心职责
- 管理多级审批流程
- 支持单人/多人审批
- 审批超时与自动降级
- 审批历史与轨迹追踪
- 紧急绕过机制

### 5.2 数据结构

```python
@dataclass
class ApprovalRequest:
    """审批请求"""
    id: str
    plan_id: str
    requester_id: str
    requester_name: str

    # 审批配置
    approval_type: Literal["single", "multi", "unanimous", "majority"]
    required_approvers: List[str]  # 角色或用户ID
    required_count: int  # 需要的审批数量

    # 审批内容
    title: str
    description: str
    risk_summary: dict  # 从Policy阶段获取
    change_summary: dict  # 从Dry-run阶段获取

    # 状态
    status: Literal["pending", "approved", "rejected", "expired", "bypassed"]

    # 审批记录
    approvals: List[ApprovalRecord]

    # 时间控制
    created_at: datetime
    expires_at: datetime
    timeout_action: Literal["reject", "auto_approve", "notify"]

@dataclass
class ApprovalRecord:
    """审批记录"""
    id: str
    approval_request_id: str
    approver_id: str
    approver_name: str
    approver_role: str

    # 决策
    decision: Literal["approve", "reject", "delegate"]
    comment: str

    # 时间戳
    decided_at: datetime

    # 审批凭证
    signature: Optional[str]  # 数字签名
    ip_address: str
```

### 5.3 API接口

```python
# POST /api/approval/request
{
  "plan_id": "plan_789",
  "requester_id": "user_123"
}

# Response
{
  "approval_id": "approval_abc",
  "status": "pending",
  "required_approvers": ["sre-lead", "ops-manager"],
  "required_count": 1,
  "expires_at": "2025-01-21T18:00:00Z",
  "approval_url": "/approval/approval_abc"
}

# POST /api/approval/{approval_id}/submit
{
  "decision": "approve",  # approve/reject
  "comment": "风险可控,同意执行"
}

# Response
{
  "approval_id": "approval_abc",
  "status": "approved",  # pending/approved/rejected/expired
  "approved_count": 1,
  "required_count": 1,
  "can_proceed": true
}
```

---

## 6. Execute阶段 - 执行编排与监控

### 6.1 核心职责
- 金丝雀批次执行
- 实时流式日志
- 失败即停与自动回滚
- 并发控制与速率限制
- 执行状态追踪

### 6.2 数据结构

```python
@dataclass
class ExecutionRun:
    """执行运行"""
    id: str
    plan_id: str
    approval_id: Optional[str]

    # 执行模式
    mode: Literal["real", "dry_run"]

    # 状态
    status: Literal["pending", "running", "paused", "completed", "failed", "rolled_back"]

    # 批次信息
    current_batch: int
    total_batches: int
    batches: List[ExecutionBatch]

    # 统计信息
    total_steps: int
    completed_steps: int
    failed_steps: int

    # 时间信息
    started_at: Optional[datetime]
    completed_at: Optional[datetime]
    duration_seconds: Optional[float]

@dataclass
class ExecutionBatch:
    """执行批次"""
    batch_number: int
    target_hosts: List[str]
    status: Literal["pending", "running", "completed", "failed"]

    # 晋级条件评估
    promotion_checks: List[PromotionCheck]
    can_promote: bool

@dataclass
class StepExecutionResult:
    """步骤执行结果"""
    step_name: str
    target_host: str
    batch_number: int

    # 执行状态
    status: Literal["pending", "running", "completed", "failed", "skipped"]

    # 输出
    stdout: str
    stderr: str
    exit_code: int

    # 资源变更
    changes: List[ResourceChange]

    # 时间信息
    started_at: datetime
    completed_at: Optional[datetime]
    duration_seconds: Optional[float]
```

### 6.3 API接口

```python
# POST /api/execute/start
{
  "plan_id": "plan_789",
  "approval_id": "approval_abc"
}

# Response
{
  "run_id": "run_xyz123",
  "status": "running",
  "current_batch": 1,
  "total_batches": 4,
  "logs_stream_url": "ws://api/execute/run_xyz123/logs"
}

# GET /api/execute/{run_id}/status
{
  "run_id": "run_xyz123",
  "status": "running",
  "current_batch": 2,
  "total_batches": 4,
  "completed_steps": 15,
  "total_steps": 30,
  "failed_steps": 0,
  "progress_percent": 50.0,
  "estimated_remaining_seconds": 120
}

# WebSocket: /api/execute/{run_id}/logs
# 实时推送执行日志
```

---

## 7. Verify阶段 - 验证与健康检查

### 7.1 核心职责
- 执行后健康检查
- 验证服务可用性
- 检查关键指标
- SLO合规性验证
- 触发告警或回滚

### 7.2 数据结构

```python
@dataclass
class VerificationResult:
    """验证结果"""
    run_id: str
    target_host: str

    # 验证状态
    passed: bool
    overall_health: Literal["healthy", "degraded", "unhealthy"]

    # 检查结果
    checks: List[HealthCheck]

    # SLO验证
    slo_compliance: SLOCompliance

    # 时间信息
    verified_at: datetime
    duration_seconds: float

@dataclass
class HealthCheck:
    """健康检查"""
    check_name: str
    check_type: Literal["http_probe", "tcp_probe", "process_check",
                        "log_check", "metric_check", "custom_script"]

    # 检查结果
    passed: bool
    actual_value: any
    expected_value: any

    # 详情
    message: str
    error: Optional[str]

    # 时间
    checked_at: datetime
    response_time_ms: Optional[float]

@dataclass
class SLOCompliance:
    """SLO合规性"""
    availability: float  # 可用性百分比
    latency_p50: float
    latency_p95: float
    latency_p99: float
    error_rate: float

    # 合规判断
    meets_slo: bool
    violations: List[str]

@dataclass
class VerificationRules:
    """验证规则"""
    health_probes: List[HealthProbe]
    metric_checks: List[MetricCheck]
    log_checks: List[LogCheck]
    slo_thresholds: SLOThresholds

    # 验证策略
    retry_count: int = 3
    retry_delay_seconds: int = 5
    timeout_seconds: int = 30
```

### 7.3 API接口

```python
# POST /api/verify/run/{run_id}
{
  "verification_rules_id": "rules_123"
}

# Response
{
  "verification_id": "verify_abc",
  "total_hosts": 5,
  "passed_hosts": 4,
  "failed_hosts": 1,
  "overall_health": "degraded",
  "results_url": "/api/verify/verify_abc/results"
}

# GET /api/verify/{verification_id}/results
[
  {
    "host": "prod-web-01",
    "passed": true,
    "overall_health": "healthy",
    "checks_passed": 5,
    "checks_total": 5,
    "slo_compliance": {
      "meets_slo": true,
      "availability": 0.9998
    }
  },
  {
    "host": "prod-web-02",
    "passed": false,
    "overall_health": "degraded",
    "checks_passed": 4,
    "checks_total": 5,
    "failed_checks": ["http_probe"],
    "slo_compliance": {
      "meets_slo": false,
      "violations": ["错误率0.05% > 0.01%"]
    }
  }
]
```

---

## 8. Report阶段 - 报告生成与审计

### 8.1 核心职责
- 生成结构化执行报告
- 记录完整审计轨迹
- 导出回滚脚本
- 生成变更摘要
- 合规性报告

### 8.2 数据结构

```python
@dataclass
class ExecutionReport:
    """执行报告"""
    id: str
    run_id: str
    plan_id: str

    # 报告元信息
    title: str
    generated_at: datetime
    generated_by: str

    # 执行摘要
    summary: ExecutionSummary

    # 详细内容
    sections: List[ReportSection]

    # 附件
    artifacts: List[ReportArtifact]

    # 审计信息
    audit_trail: AuditTrail

    # 导出格式
    formats: List[str]  # ["pdf", "html", "json", "markdown"]

@dataclass
class ExecutionSummary:
    """执行摘要"""
    # 基本信息
    requester: str
    approved_by: List[str]
    executed_at: datetime
    duration_seconds: float

    # 执行结果
    status: str
    success_rate: float

    # 影响范围
    total_hosts: int
    affected_hosts: int
    total_changes: int

    # 风险评估
    initial_risk: str
    actual_impact: str

    # SLO合规
    slo_met: bool

    # 回滚信息
    rollback_triggered: bool
    rollback_success: Optional[bool]

@dataclass
class ReportSection:
    """报告章节"""
    title: str
    order: int
    content_type: Literal["text", "table", "chart", "code", "timeline"]
    content: dict

@dataclass
class AuditTrail:
    """审计轨迹"""
    entries: List[AuditEntry]

@dataclass
class AuditEntry:
    """审计条目"""
    timestamp: datetime
    stage: str  # chat/plan/policy/approve/execute/verify
    action: str
    actor: str  # 用户或系统
    actor_ip: str
    details: dict

    # 变更记录
    before_state: Optional[dict]
    after_state: Optional[dict]
```

### 8.3 报告章节结构

报告包含以下标准章节:

1. **执行概览** - 基本信息、执行时间、状态、成功率
2. **风险评估** - 策略检查结果、影响分析
3. **审批信息** - 审批人、审批时间、审批意见
4. **执行详情** - 批次执行情况、步骤详情
5. **变更清单** - 所有资源变更记录、备份信息
6. **验证结果** - 健康检查、SLO合规性
7. **执行时间线** - 完整的时间序列事件

### 8.4 API接口

```python
# POST /api/report/generate
{
  "run_id": "run_xyz123"
}

# Response
{
  "report_id": "report_abc",
  "title": "执行报告 - deploy - 2025-01-21 15:30",
  "generated_at": "2025-01-21T15:35:00Z",
  "summary": {
    "status": "completed",
    "success_rate": 1.0,
    "total_changes": 15,
    "slo_met": true
  },
  "download_urls": {
    "html": "/api/report/report_abc/download?format=html",
    "pdf": "/api/report/report_abc/download?format=pdf",
    "json": "/api/report/report_abc/download?format=json"
  }
}

# GET /api/report/{report_id}
# 返回完整报告结构

# GET /api/report/{report_id}/download?format=pdf
# 下载指定格式的报告
```

---

## 9. 整体数据流与接口设计

### 9.1 完整流程数据流

```
用户输入(自然语言)
    ↓
[Chat阶段] → Intent
    ↓
[Plan阶段] → ExecutionPlan
    ↓
[Policy阶段] → PolicyCheckResult
    ↓
判断: 需要审批?
    ├─ 是 → [Approve阶段] → ApprovalRequest
    └─ 否 → 继续
    ↓
[Dry-run阶段] → DryRunResult
    ↓
判断: Dry-run成功?
    ├─ 否 → 返回修改建议
    └─ 是 → 继续
    ↓
[Execute阶段] → ExecutionRun
    ↓
[Verify阶段] → VerificationResult
    ↓
判断: 验证通过?
    ├─ 否 → 自动回滚
    └─ 是 → 继续
    ↓
[Report阶段] → ExecutionReport
    ↓
完成
```

### 9.2 工作流上下文

```python
@dataclass
class WorkflowContext:
    """工作流上下文 - 贯穿整个流程的数据容器"""
    workflow_id: str
    session_id: str
    user_id: str

    # 各阶段数据
    chat_message: Optional[ChatMessage] = None
    intent: Optional[Intent] = None
    plan: Optional[ExecutionPlan] = None
    policy_result: Optional[PolicyCheckResult] = None
    dry_run_result: Optional[DryRunResult] = None
    approval: Optional[ApprovalRequest] = None
    execution_run: Optional[ExecutionRun] = None
    verification_results: Optional[List[VerificationResult]] = None
    report: Optional[ExecutionReport] = None

    # 状态跟踪
    current_stage: str
    status: Literal["in_progress", "waiting_approval", "completed", "failed"]

    # 时间戳
    created_at: datetime
    updated_at: datetime

    # 元数据
    metadata: dict
```

### 9.3 统一工作流API

```python
# ========== 启动工作流 ==========
POST /api/workflow/start
{
  "session_id": "sess_123",
  "user_input": "生产环境nginx返回大量502,帮我排查修复"
}

Response:
{
  "workflow_id": "wf_abc123",
  "current_stage": "approve",
  "status": "waiting_approval",
  "intent": {
    "type": "diagnose_and_fix",
    "confidence": 0.95
  },
  "plan_summary": {
    "severity": "P1",
    "steps_count": 6,
    "affected_hosts": 5
  },
  "approval_required": true,
  "approval_url": "/approval/approval_xyz",
  "next_action": "waiting_for_approval"
}

# ========== 查询工作流状态 ==========
GET /api/workflow/{workflow_id}/status

Response:
{
  "workflow_id": "wf_abc123",
  "current_stage": "execute",
  "status": "in_progress",
  "progress": {
    "current_batch": 2,
    "total_batches": 4,
    "completed_steps": 12,
    "total_steps": 24,
    "progress_percent": 50
  },
  "stages": {
    "chat": "completed",
    "plan": "completed",
    "policy": "completed",
    "dry_run": "completed",
    "approve": "completed",
    "execute": "in_progress",
    "verify": "pending",
    "report": "pending"
  }
}

# ========== 审批后继续工作流 ==========
POST /api/workflow/{workflow_id}/continue
{
  "approval_id": "approval_xyz"
}

Response:
{
  "workflow_id": "wf_abc123",
  "status": "in_progress",
  "current_stage": "execute",
  "message": "审批通过,继续执行"
}

# ========== 取消工作流 ==========
POST /api/workflow/{workflow_id}/cancel
{
  "reason": "需求变更"
}

# ========== 获取工作流完整上下文 ==========
GET /api/workflow/{workflow_id}/context

Response: {完整的WorkflowContext对象}
```

### 9.4 阶段间数据依赖关系

| 阶段 | 输入依赖 | 输出产物 | 下游消费者 |
|------|---------|---------|-----------|
| Chat | 用户输入 | Intent | Plan |
| Plan | Intent | ExecutionPlan | Policy, Dry-run, Execute |
| Policy | ExecutionPlan, User | PolicyCheckResult | Approve, Report |
| Dry-run | ExecutionPlan | DryRunResult | Approve, Report |
| Approve | Plan, Policy, Dry-run | ApprovalRequest | Execute |
| Execute | Plan, Approval | ExecutionRun | Verify, Report |
| Verify | ExecutionRun, Plan | VerificationResult | Report |
| Report | 所有阶段数据 | ExecutionReport | 用户 |

---

## 10. 异常处理与回滚机制

### 10.1 异常分类体系

```python
class OpsException(Exception):
    """运维异常基类"""
    def __init__(self, message: str, stage: str, recoverable: bool = True):
        self.message = message
        self.stage = stage
        self.recoverable = recoverable
        super().__init__(message)

class PlanGenerationError(OpsException):
    """计划生成错误 - 不可恢复"""
    pass

class PolicyViolationError(OpsException):
    """策略违规错误 - 不可恢复"""
    def __init__(self, violations: List[PolicyViolation]):
        self.violations = violations
        super().__init__(
            f"策略检查失败: {len(violations)}个违规",
            stage="policy",
            recoverable=False
        )

class ExecutionError(OpsException):
    """执行错误 - 可恢复(回滚)"""
    def __init__(self, step_name: str, host: str, error: str):
        self.step_name = step_name
        self.host = host
        super().__init__(
            f"步骤'{step_name}'在主机'{host}'执行失败: {error}",
            stage="execute",
            recoverable=True
        )

class VerificationError(OpsException):
    """验证错误 - 可恢复(回滚)"""
    def __init__(self, failed_checks: List[HealthCheck]):
        self.failed_checks = failed_checks
        super().__init__(
            f"验证失败: {len(failed_checks)}个检查未通过",
            stage="verify",
            recoverable=True
        )

class RollbackError(OpsException):
    """回滚错误 - 严重告警"""
    def __init__(self, original_error: str, rollback_error: str):
        self.original_error = original_error
        self.rollback_error = rollback_error
        super().__init__(
            f"执行失败且回滚失败! 原始错误: {original_error}, 回滚错误: {rollback_error}",
            stage="rollback",
            recoverable=False
        )
```

### 10.2 回滚机制

#### 10.2.1 回滚触发条件

- 执行步骤失败且配置了自动回滚
- 验证检查未通过
- 手动触发回滚
- 超时触发回滚

#### 10.2.2 回滚执行流程

```python
class RollbackExecutor:
    """回滚执行器"""

    async def execute_rollback(
        self,
        run: ExecutionRun,
        rollback_strategy: RollbackStrategy
    ) -> RollbackResult:
        """执行回滚"""

        rollback_run = ExecutionRun(
            id=f"rollback_{run.id}",
            plan_id=run.plan_id,
            mode="rollback",
            status="running",
            started_at=datetime.now()
        )

        try:
            # 1. 按逆序执行回滚步骤
            for step in reversed(rollback_strategy.rollback_steps):
                await self._execute_rollback_step(step, rollback_run)

            # 2. 验证回滚结果
            await self._verify_rollback(rollback_run)

            rollback_run.status = "completed"

        except Exception as e:
            rollback_run.status = "failed"
            rollback_run.error = str(e)

            # 回滚失败,发送P0告警
            await self._alert_rollback_failure(run, rollback_run, e)

        finally:
            rollback_run.completed_at = datetime.now()
            await self._save_rollback_run(rollback_run)

        return RollbackResult(
            rollback_run_id=rollback_run.id,
            success=(rollback_run.status == "completed"),
            steps_completed=rollback_run.completed_steps,
            error=rollback_run.error
        )
```

#### 10.2.3 回滚类型

| 变更类型 | 回滚方式 | 实现 |
|---------|---------|------|
| 文件修改 | 恢复备份文件 | `cp backup_path original_path` |
| 服务重启 | 重启到之前状态 | `systemctl restart service` |
| 包安装 | 卸载或降级 | `apt remove / yum downgrade` |
| 配置变更 | 应用备份配置 | `restore config + reload` |
| K8s部署 | 回滚到上一版本 | `kubectl rollout undo` |

### 10.3 全局异常处理策略

```python
@app.exception_handler(OpsException)
async def ops_exception_handler(request: Request, exc: OpsException):
    """全局异常处理器"""

    # 1. 记录审计日志
    await audit_logger.log_exception(exc, request)

    # 2. 如果可恢复,尝试自动回滚
    if exc.recoverable and exc.stage in ["execute", "verify"]:
        workflow_id = request.path_params.get("workflow_id")
        if workflow_id:
            ctx = await load_context(workflow_id)
            if ctx.execution_run and ctx.plan.rollback.auto_rollback:
                await trigger_rollback(ctx.execution_run, ctx.plan.rollback)

    # 3. 发送告警通知
    if not exc.recoverable:
        await send_alert(
            level="critical",
            message=exc.message,
            stage=exc.stage
        )

    # 4. 返回结构化错误响应
    return JSONResponse(
        status_code=500 if not exc.recoverable else 200,
        content={
            "error": exc.message,
            "stage": exc.stage,
            "recoverable": exc.recoverable,
            "timestamp": datetime.now().isoformat(),
            "support_ticket_url": "/support/create"
        }
    )
```

### 10.4 重试机制

```python
@dataclass
class RetryConfig:
    """重试配置"""
    max_retries: int = 3
    backoff_seconds: int = 5
    backoff_multiplier: float = 2.0  # 指数退避
    retry_on_errors: List[str] = field(default_factory=lambda: [
        "ConnectionError",
        "TimeoutError",
        "TemporaryFailure"
    ])

async def execute_with_retry(
    func: Callable,
    retry_config: RetryConfig,
    *args,
    **kwargs
):
    """带重试的执行"""

    last_error = None
    wait_time = retry_config.backoff_seconds

    for attempt in range(retry_config.max_retries + 1):
        try:
            result = await func(*args, **kwargs)

            # 成功,记录重试次数
            if attempt > 0:
                await audit_logger.log_retry_success(
                    func.__name__,
                    attempt
                )

            return result

        except Exception as e:
            last_error = e

            # 检查是否应该重试
            if attempt < retry_config.max_retries:
                if any(err in str(e) for err in retry_config.retry_on_errors):
                    await asyncio.sleep(wait_time)
                    wait_time *= retry_config.backoff_multiplier
                    continue

            # 达到最大重试次数或不可重试的错误
            break

    # 重试失败
    raise RetryExhaustedError(
        f"重试{retry_config.max_retries}次后仍然失败: {last_error}"
    )
```

### 10.5 故障处理决策树

```
执行失败
    ├─ 是否可重试?
    │   ├─ 是 → 重试(最多3次)
    │   │   ├─ 重试成功 → 继续
    │   │   └─ 重试失败 → 判断是否回滚
    │   └─ 否 → 判断是否回滚
    │
    ├─ 是否配置自动回滚?
    │   ├─ 是 → 执行回滚
    │   │   ├─ 回滚成功 → 记录+通知
    │   │   └─ 回滚失败 → P0告警+人工介入
    │   └─ 否 → 记录失败状态
    │
    └─ 发送告警通知
        ├─ P0: 回滚失败、数据一致性问题
        ├─ P1: 执行失败、验证失败
        └─ P2: 部分主机失败
```

---

## 总结

### 完整流程特点

1. **安全第一**:
   - 多层策略检查(黑名单/RBAC/OPA)
   - Dry-run预演
   - 强制审批机制
   - 自动回滚保护

2. **可观测性**:
   - 实时流式日志
   - 完整审计轨迹
   - 结构化报告
   - 时间线可视化

3. **可靠性**:
   - 金丝雀批次执行
   - 失败即停
   - 自动重试
   - 验证探针

4. **合规性**:
   - 审批流程
   - 变更记录
   - SLO验证
   - 审计报告

### 技术栈总结

- **后端**: Python (FastAPI) + Celery
- **执行**: SSH/Ansible/kubectl
- **策略**: OPA (Rego)
- **存储**: PostgreSQL + 对象存储
- **通信**: WebSocket (实时日志)
- **LLM**: 可插拔接口

---

**文档状态**: ✅ 已完成
**最后更新**: 2025-01-21

**包含章节**:
1. ✅ Chat阶段 - 意图解析与上下文管理
2. ✅ Plan阶段 - 计划生成与结构化
3. ✅ Policy阶段 - 策略检查与风险评估
4. ✅ Dry-run阶段 - 预演与影响分析
5. ✅ Approve阶段 - 审批流与权限控制
6. ✅ Execute阶段 - 执行编排与监控
7. ✅ Verify阶段 - 验证与健康检查
8. ✅ Report阶段 - 报告生成与审计
9. ✅ 整体数据流与接口设计
10. ✅ 异常处理与回滚机制
