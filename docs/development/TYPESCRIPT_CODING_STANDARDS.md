# TypeScript 编码规范

**版本**: 1.0
**生效日期**: 2025-10-23
**状态**: 强制执行

---

## 📋 总则

本规范旨在确保项目 TypeScript 代码的类型安全性、可维护性和一致性。所有团队成员必须遵守本规范,所有代码提交必须通过ESLint类型检查。

---

## 🚫 禁止事项

### 1. 禁止使用 `any` 类型

**规则**: ❌ 严格禁止在生产代码中使用 `any` 类型

**ESLint配置**:
```javascript
"@typescript-eslint/no-explicit-any": "error"
```

**违规示例**:
```typescript
// ❌ 错误 - 禁止
function processData(data: any) {
    return data.value;
}

// ❌ 错误 - 禁止
const result: any = fetchData();

// ❌ 错误 - 禁止
(window as any).myProperty = value;
```

**正确做法**:
```typescript
// ✅ 正确 - 使用具体类型
interface DataStructure {
    value: string;
}
function processData(data: DataStructure) {
    return data.value;
}

// ✅ 正确 - 使用泛型
function fetchData<T>(): T {
    // ...
}

// ✅ 正确 - 扩展Window接口
interface WindowWithProperty extends Window {
    myProperty?: string;
}
const win = window as unknown as WindowWithProperty;
win.myProperty = value;
```

---

## ✅ 推荐模式

### 1. Window全局属性类型化

**场景**: 需要在window对象上添加自定义属性

**模式**:
```typescript
// 1. 定义接口扩展
interface WindowWithCustom extends Window {
    customProperty?: Type;
    customFunction?: (arg: ArgType) => ReturnType;
}

// 2. 类型安全访问
const win = window as unknown as WindowWithCustom;

// 3. 运行时检查
if (typeof win.customProperty !== 'undefined') {
    win.customProperty = value;
}

if (typeof win.customFunction === 'function') {
    win.customFunction(arg);
}
```

**实际案例**: 参考 `utils/notification.ts:10-18`, `utils/i18n.ts:8-10`, `modules/SSHConfigImportWizard.ts:22-27`

---

### 2. 第三方库类型集成

**场景**: 使用第三方库(如Chart.js)的类型定义

**模式**:
```typescript
// 1. 使用 import type 避免运行时开销
import type { LibraryType, LibraryConfig } from 'library-name';

// 2. 使用库提供的类型
function createConfig(options: Partial<LibraryConfig>): LibraryConfig {
    // ...
}

// 3. 实例类型定义
let instance: LibraryType | null = null;

// 4. 数组类型定义
const instances: Array<LibraryType | null> = [];
```

**实际案例**: 参考 `utils/chart.ts:5-9`

---

### 3. 动态属性访问类型化

**场景**: 需要动态访问对象属性

**模式**:
```typescript
// ❌ 错误 - 使用any
(obj as any)[field] = value;

// ✅ 正确 - 使用Record类型
interface MyObject {
    [key: string]: string | number;
}

// 或者
(obj as Record<string, string | number>)[field] = value;

// 更好的做法 - 使用类型守卫
function isValidField(field: string): field is keyof MyObject {
    return field in obj;
}

if (isValidField(field)) {
    obj[field] = value; // 类型安全
}
```

**实际案例**: 参考 `modules/SSHConfigImportWizard.ts:1466`

---

### 4. 联合类型 + 类型守卫

**场景**: 有限的字符串选项

**模式**:
```typescript
// 1. 定义联合类型
type TabName = "auto-scan" | "upload" | "manual";

// 2. 类型守卫函数
function isValidTabName(value: string): value is TabName {
    return value === "auto-scan" || value === "upload" || value === "manual";
}

// 3. 使用
const tabName = element.dataset.tab;
if (tabName && isValidTabName(tabName)) {
    switchTab(tabName); // 类型安全
}
```

**实际案例**: 参考 `modules/SSHConfigImportWizard.ts:241-253`

---

### 5. 泛型使用

**场景**: 函数需要支持多种类型

**模式**:
```typescript
// ✅ 推荐 - 使用泛型
function fetchData<T>(url: string): Promise<T> {
    return fetch(url).then(res => res.json());
}

// 使用
interface UserData {
    id: number;
    name: string;
}
const user = await fetchData<UserData>('/api/user');
// user 的类型是 UserData

// ✅ 泛型约束
function getProperty<T, K extends keyof T>(obj: T, key: K): T[K] {
    return obj[key];
}
```

---

### 6. Unknown类型的使用

**场景**: 真的不知道类型时使用 `unknown` 而非 `any`

**模式**:
```typescript
// ❌ 错误
function process(data: any) {
    return data.value; // 不安全
}

// ✅ 正确
function process(data: unknown) {
    // 必须先做类型检查
    if (typeof data === 'object' && data !== null && 'value' in data) {
        return (data as { value: string }).value;
    }
    throw new Error('Invalid data');
}

// 更好 - 使用类型守卫
interface DataWithValue {
    value: string;
}

function isDataWithValue(data: unknown): data is DataWithValue {
    return typeof data === 'object' &&
           data !== null &&
           'value' in data &&
           typeof (data as any).value === 'string';
}

function process(data: unknown) {
    if (isDataWithValue(data)) {
        return data.value; // 类型安全
    }
    throw new Error('Invalid data');
}
```

---

## 📝 函数返回类型

### 规则
所有导出的函数必须显式声明返回类型 (警告级别)

**ESLint配置**:
```javascript
"@typescript-eslint/explicit-function-return-type": "warn"
```

**示例**:
```typescript
// ⚠️ 警告 - 缺少返回类型
export function calculateTotal(items) {
    return items.reduce((sum, item) => sum + item.price, 0);
}

// ✅ 正确
export function calculateTotal(items: Item[]): number {
    return items.reduce((sum, item) => sum + item.price, 0);
}

// ✅ 正确 - 箭头函数
export const calculateTotal = (items: Item[]): number => {
    return items.reduce((sum, item) => sum + item.price, 0);
};
```

---

## 🧪 测试代码例外

### 允许的情况

测试代码中Mock和Stub **可以** 使用 `any`,但必须添加注释说明:

```typescript
// ✅ 允许 - 测试Mock
const mockChart = {
    destroy: jest.fn()
} as any; // Mock Chart.js instance for testing

// ✅ 允许 - 测试Spy
const spy = jest.spyOn(window as any, 'fetch');
```

**注意**:
- 测试any类型已延期至Post-MVP处理
- 即使在测试中,也应尽量使用类型安全的Mock库(如 `jest.MockedFunction<T>`)

---

## 🔍 代码审查检查清单

在代码审查时,审查者应检查以下项目:

- [ ] 没有使用 `any` 类型 (生产代码)
- [ ] Window全局属性使用接口扩展模式
- [ ] 第三方库使用 `import type` 导入类型
- [ ] 动态属性访问使用 `Record<K, V>` 或类型守卫
- [ ] 联合类型配合类型守卫使用
- [ ] 导出函数有显式返回类型
- [ ] 使用 `unknown` 而非 `any` 处理未知类型
- [ ] 测试代码的 `any` 使用有注释说明

---

## 🚀 CI/CD集成

### Pre-commit Hook

已配置Git pre-commit hook自动运行ESLint:

```bash
# .husky/pre-commit
npm run lint
```

### CI Pipeline

CI管道会在以下阶段检查类型安全:

1. **Lint阶段**:
   ```bash
   npm run lint
   ```
   - 检查所有TypeScript文件
   - 失败则拒绝PR

2. **Type Check阶段**:
   ```bash
   npm run type-check
   ```
   - 运行 `tsc --noEmit`
   - 验证类型正确性

3. **Build阶段**:
   ```bash
   npm run build
   ```
   - 完整构建验证

---

## 📚 参考资料

### 内部文档
- [Phase 4完成报告](../../specs/003-server-group-cleanup/PHASE4_COMPLETION_REPORT.md) - 详细的类型安全最佳实践案例
- [Phase 4进度文档](../../specs/003-server-group-cleanup/phase4-progress.md) - 技术实现细节

### 外部资源
- [TypeScript Handbook - Type Guards](https://www.typescriptlang.org/docs/handbook/2/narrowing.html)
- [TypeScript Deep Dive - Never Use Any](https://basarat.gitbook.io/typescript/type-system/never-use-any)
- [@typescript-eslint/no-explicit-any](https://typescript-eslint.io/rules/no-explicit-any/)

---

## 🔧 常见问题

### Q1: 遇到确实无法确定类型的情况怎么办?

**A**: 使用 `unknown` 而非 `any`,然后添加运行时类型检查:

```typescript
function handleData(data: unknown) {
    if (isValidDataStructure(data)) {
        // 类型安全地使用data
    }
}
```

### Q2: 第三方库没有类型定义怎么办?

**A**:
1. 优先寻找 `@types/库名` 包
2. 如果没有,创建 `.d.ts` 文件声明模块:
   ```typescript
   // types/library-name.d.ts
   declare module 'library-name' {
       export interface Config {
           // ...
       }
       export function init(config: Config): void;
   }
   ```
3. 最后考虑为库贡献类型定义

### Q3: ESLint报错但我觉得应该允许怎么办?

**A**:
1. **不要** 使用 `// eslint-disable-next-line`
2. 先与团队讨论是否真的需要例外
3. 如果确需例外,需要:
   - 技术负责人批准
   - 添加详细注释说明原因
   - 创建Issue跟踪后续改进

### Q4: 如何处理从后端返回的动态JSON?

**A**: 定义接口并使用类型守卫:

```typescript
interface ApiResponse {
    status: string;
    data: unknown;
}

function isUserData(data: unknown): data is UserData {
    return typeof data === 'object' &&
           data !== null &&
           'id' in data &&
           'name' in data;
}

const response: ApiResponse = await fetch('/api/user').then(r => r.json());
if (isUserData(response.data)) {
    // response.data 类型安全
}
```

---

## 📊 度量指标

项目当前类型安全指标:

- **生产代码any类型**: 3个 (仅废弃文件)
- **活跃代码any类型**: 0个 ✅
- **ESLint any错误**: 149个 (主要在测试文件)
- **目标**: 0个any类型 (Post-MVP)

**每月审查**: 技术负责人每月审查类型安全指标,确保持续改进。

---

## ⚖️ 规范变更流程

1. 提出变更建议 (通过Issue)
2. 团队讨论 (至少3个工作日)
3. 技术负责人批准
4. 更新本文档
5. 更新ESLint配置
6. 通知全体开发人员

---

**最后更新**: 2025-10-23
**维护者**: 技术团队
**版本历史**:
- v1.0 (2025-10-23): 初始版本,基于Phase 4成果

🤖 *本规范基于 [Phase 4: TypeScript类型安全改进](../../specs/003-server-group-cleanup/PHASE4_COMPLETION_REPORT.md) 项目经验编写*
