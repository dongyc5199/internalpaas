# Contracts: Frontend Code Quality Cleanup

**Feature**: Frontend Code Quality Cleanup
**Date**: 2025-10-23
**Contract Type**: CLI Scripts & Configuration Interfaces

## Overview

This feature does not introduce traditional API contracts (REST/GraphQL endpoints) since it is a development tooling and code quality improvement initiative. Instead, this directory documents the "contracts" between:

1. **CLI Scripts**: Commands developers run to perform cleanup operations
2. **Configuration Files**: Interfaces between different tooling systems (ESLint, Prettier, TypeScript, Git)
3. **Testing Contracts**: Expected behavior after cleanup

These contracts ensure consistent behavior across development environments and team members.

---

## 1. CLI Script Contracts

### 1.1 Linting Commands

#### `npm run lint`

**Purpose**: Check code for linting violations without modifying files

**Input**: N/A (operates on `src/main/frontend/**/*.{ts,tsx}`)

**Output**:
```
Exit Code: 0 (success) | non-zero (violations found)
Stdout: List of violations with file:line:column format
Stderr: Fatal errors (e.g., config issues)
```

**Success Criteria**:
- After cleanup: Exit code MUST be 0
- After cleanup: Stdout MUST be empty (no violations)

**Example Success Output**:
```bash
$ npm run lint
✨  Done in 2.34s.
```

**Example Failure Output** (before cleanup):
```bash
$ npm run lint

/path/to/file.ts
  12:5  error  Unexpected any. Specify a different type  @typescript-eslint/no-explicit-any
  34:10 warning Missing return type on function         @typescript-eslint/explicit-function-return-type

✖ 2 problems (1 error, 1 warning)
```

---

#### `npm run lint:fix`

**Purpose**: Automatically fix auto-fixable linting violations

**Input**: N/A (operates on source files)

**Output**:
```
Exit Code: 0 (success) | non-zero (unfixable violations remain)
Side Effect: Modifies source files in place
Stdout: Report of fixes applied
```

**Contract**:
- MUST NOT fix violations that change code semantics
- MUST only apply safe style/formatting fixes
- MAY leave unfixable violations (manual intervention required)

**Example Output**:
```bash
$ npm run lint:fix

/path/to/file.ts
  12:5  error  Unexpected any  @typescript-eslint/no-explicit-any  [manual fix required]

✖ 1 problem (1 error, 0 warnings)
✔ 15 problems fixed automatically
```

---

#### `npm run format`

**Purpose**: Apply Prettier formatting to all frontend code

**Input**: N/A (operates on `src/main/frontend/**/*.{ts,tsx,js,jsx,css,scss,md}`)

**Output**:
```
Exit Code: 0 (success)
Side Effect: Modifies source files to match .prettierrc.json
Stdout: List of formatted files
```

**Contract**:
- MUST respect `.prettierrc.json` configuration
- MUST NOT change code logic, only formatting
- MUST format all matched files (no partial formatting)

**Example Output**:
```bash
$ npm run format

src/main/frontend/main.ts 250ms
src/main/frontend/modules/dashboard.ts 180ms
src/main/frontend/modules/theme.ts 120ms
✨  Done in 1.2s.
```

---

#### `npm run type-check`

**Purpose**: Run TypeScript compiler in check-only mode (no emit)

**Input**: N/A (operates on files in `tsconfig.json` include paths)

**Output**:
```
Exit Code: 0 (success) | non-zero (type errors found)
Stdout: Type errors with file:line:column format
```

**Success Criteria**:
- After cleanup: Exit code MUST be 0
- After cleanup: No type errors involving `any` types

**Example Success Output**:
```bash
$ npm run type-check
✨  Done in 3.5s.
```

**Example Failure Output** (before cleanup):
```bash
$ npm run type-check

src/main/frontend/modules/dashboard.ts:45:12 - error TS7006: Parameter 'event' implicitly has an 'any' type.

45   function handleClick(event) {
              ~~~~~

Found 3 errors in 2 files.
```

---

#### `npm run ci:check`

**Purpose**: Run comprehensive quality checks (lint + type-check + test + build)

**Input**: N/A (runs all sub-commands)

**Output**:
```
Exit Code: 0 (all checks pass) | non-zero (any check fails)
Stdout: Combined output from all checks
```

**Contract**:
- MUST run in order: lint → lint:style → type-check → test:run → build
- MUST fail fast (stop at first failure)
- Exit code 0 = codebase is production-ready

**Success Criteria**:
- After cleanup: This command MUST pass with exit code 0
- This contract is enforced in CI/CD pipeline

---

### 1.2 Git Operations

#### Line Ending Normalization

**Command**: `git add --renormalize .`

**Purpose**: Apply `.gitattributes` line ending rules to existing files

**Input**: Files tracked by git

**Output**:
```
Side Effect: Re-writes file line endings to match .gitattributes
Git status: Shows modified files (line endings changed)
```

**Contract**:
- MUST be run after `.gitattributes` is created/updated
- MUST be committed separately (don't mix with code changes)
- Only affects text files (binaries excluded via `.gitattributes`)

**Example**:
```bash
$ git add --renormalize .
$ git status

modified:   src/main/frontend/main.ts
modified:   src/main/frontend/modules/dashboard.ts
...

# All changes are line ending normalizations (LF)
```

---

## 2. Configuration File Contracts

### 2.1 ESLint ↔ Prettier Integration

**Contract**: No conflicting rules

**Requirement**:
- `.eslintrc.cjs` MUST extend `plugin:prettier/recommended`
- This automatically disables ESLint formatting rules that conflict with Prettier

**Validation**:
```bash
# Should NOT report formatting as both an ESLint and Prettier error
$ npm run lint
# Only Prettier errors shown (via prettier/prettier rule)
```

---

### 2.2 Prettier ↔ EditorConfig Alignment

**Contract**: Indentation and line ending settings MUST match

| Setting         | .prettierrc.json | .editorconfig     | Must Match |
|----------------|------------------|-------------------|------------|
| Indentation    | `tabWidth: 4`    | `indent_size = 4` | ✅ |
| Indent Style   | `useTabs: false` | `indent_style = space` | ✅ |
| Line Endings   | `endOfLine: "lf"` | `end_of_line = lf` | ✅ |
| Trailing Newline | N/A | `insert_final_newline = true` | ℹ️ Editor only |

**Validation**:
- Files formatted by Prettier MUST pass EditorConfig validation
- IDEs configured with EditorConfig MUST produce Prettier-compatible files

---

### 2.3 Git Attributes ↔ Prettier Line Endings

**Contract**: Line ending enforcement MUST be consistent

**`.gitattributes`**:
```
* text=auto eol=lf
```

**`.prettierrc.json`**:
```json
{
  "endOfLine": "lf"
}
```

**Contract Guarantee**:
- Git stores files with LF
- Prettier formats files with LF
- Checkout on Windows still LF (not CRLF)

---

### 2.4 TypeScript ↔ ESLint Type Rules

**Contract**: TypeScript strict mode aligns with ESLint type rules

**`tsconfig.json`**:
```json
{
  "compilerOptions": {
    "strict": true  // Implies noImplicitAny
  }
}
```

**`.eslintrc.cjs`**:
```javascript
{
  "rules": {
    "@typescript-eslint/no-explicit-any": "error"  // Catches explicit any
  }
}
```

**Combined Effect**:
- TypeScript catches implicit `any` (compile-time)
- ESLint catches explicit `any` (lint-time)
- Together = comprehensive `any` prevention

---

## 3. Testing Contracts

### 3.1 No Behavioral Changes

**Contract**: All existing tests MUST pass after cleanup

**Before Cleanup**:
```bash
$ npm run test:run
✓ 45 tests passed
```

**After Each Cleanup Phase**:
```bash
$ npm run test:run
✓ 45 tests passed  # Same count, same tests
```

**Guarantee**: Zero test modifications required (cleanup doesn't change logic)

---

### 3.2 Type Safety Improvements Don't Break Tests

**Contract**: Replacing `any` types MUST NOT require test changes (unless tests were incorrectly typed)

**Example**:

**Before**:
```typescript
function processData(data: any) { ... }  // Test: passes any value
```

**After**:
```typescript
function processData(data: ServerData) { ... }  // Test: still passes correct value
```

**Test Stays Unchanged**:
```typescript
test('processes data correctly', () => {
  const testData: ServerData = { id: 1, name: 'test' };
  expect(processData(testData)).toBe(...);  // Still passes
});
```

**Contract Violation** (test was relying on incorrect types):
```typescript
// BAD: Test was passing wrong type, exploiting `any`
test('processes data incorrectly', () => {
  const testData = "wrong type";  // ❌ TypeScript error after cleanup
  expect(processData(testData as any)).toBe(...);  // Test needs fix
});
```

---

## 4. CI/CD Contracts

### 4.1 Pre-Merge Checks

**Contract**: Pull requests MUST pass CI checks before merge

**GitHub Actions Workflow** (proposed):
```yaml
name: Code Quality
on: [pull_request]
jobs:
  quality-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: npm ci
      - run: npm run ci:check  # Contract enforcement point
```

**Contract**:
- Exit code 0 = PR approved for merge
- Non-zero = PR blocked until fixed

---

### 4.2 Branch Protection

**Contract**: Main/master branch MUST enforce passing checks

**Configuration** (GitHub branch protection):
- ✅ Require status checks to pass before merging
- ✅ Require branches to be up to date before merging
- ✅ Required check: `quality-check`

---

## 5. Developer Workflow Contracts

### 5.1 Pre-Commit Hook (Optional)

**Contract**: If installed, pre-commit hook runs format + lint:fix

**Implementation** (via Husky):
```json
// package.json
{
  "scripts": {
    "precommit": "npm run format && npm run lint:fix && npm run test:run"
  }
}
```

**Behavior**:
- Runs automatically before `git commit`
- Developers can bypass with `--no-verify` (for WIP commits)
- CI still enforces (bypass doesn't affect remote checks)

---

### 5.2 IDE Integration Contract

**Contract**: IDE auto-formatting MUST match npm scripts

**Expected Behavior**:
- VS Code with ESLint + Prettier extensions
- IntelliJ with ESLint + Prettier plugins
- Vim/Neovim with ALE or similar

**Configuration**:
- IDE detects `.eslintrc.cjs` and `.prettierrc.json`
- Format-on-save produces same result as `npm run format`
- No conflicts between IDE and CLI formatting

---

## Summary of Contracts

| Contract Type | Count | Enforcement |
|--------------|-------|-------------|
| CLI Scripts | 6 | Manual execution + CI |
| Configuration Alignment | 4 | Tooling validation |
| Testing | 2 | Test suite |
| CI/CD | 2 | GitHub Actions |
| Developer Workflow | 2 | Tooling + culture |

**Total Contracts**: 16

All contracts are **testable** and **enforceable** through automation.
