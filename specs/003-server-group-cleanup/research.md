# Phase 0: Research & Technical Decisions

**Feature**: Frontend Code Quality Cleanup
**Date**: 2025-10-23
**Status**: Complete

## Overview

This document consolidates research findings and technical decisions for systematically cleaning up ESLint/Prettier violations across the frontend TypeScript codebase. The research focuses on best practices for code quality cleanup, tooling configuration, and workflow strategies.

## Research Tasks

### 1. Current State Analysis

**Task**: Audit existing linting violations to understand scope and effort required

**Findings**:
- Project uses ESLint 8.57.1 with @typescript-eslint/recommended preset
- Prettier 3.6.2 integrated via eslint-plugin-prettier
- TypeScript 5.9.3 with strict mode enabled (`strict: true` in tsconfig.json)
- Current configuration in `.eslintrc.cjs` is appropriate for the project
- `npm run lint` targets: `src/main/frontend/**/*.{ts,tsx}`
- No `.editorconfig` file exists (needs creation)
- Unknown: Exact count of current violations (needs audit command)

**Decision**: Run comprehensive audit as first implementation step
- Command: `npm run lint > audit-report.txt 2>&1` to capture all violations
- Categorize violations by type (auto-fixable vs manual, type errors vs style)
- Estimate effort based on violation counts

**Rationale**: Cannot plan effective cleanup strategy without knowing violation types and quantities

---

### 2. Auto-Fixable vs Manual Violations

**Task**: Determine which violations can be automatically fixed vs require manual intervention

**Findings**:
- **Auto-fixable** (via `npm run lint:fix` or `npm run format`):
  - Prettier formatting issues (indentation, spacing, quotes, semicolons)
  - Import sorting
  - Simple rule violations with clear fixes
  - Expected: 60-80% of style violations

- **Manual intervention required**:
  - `any` type replacements (requires understanding type contracts)
  - Complex type inference issues
  - Logic-dependent rule violations
  - Expected: Most type safety improvements

**Decision**: Implement two-phase approach
1. **Phase 1 (Automated)**: Run `npm run lint:fix && npm run format`
2. **Phase 2 (Manual)**: Address remaining violations file-by-file

**Rationale**: Maximize efficiency by automating what can be automated, then focus human effort on complex issues

**Alternatives Considered**:
- Manual-first approach: Rejected because it's time-inefficient for style issues
- Fully automated with risky transforms: Rejected due to break risk

---

### 3. Line Ending Standardization

**Task**: Research best practices for enforcing LF line endings across Windows/Mac/Linux

**Findings**:
- Git's `core.autocrlf` setting can auto-convert but leads to inconsistent behavior
- `.gitattributes` file provides declarative, version-controlled line ending policy
- `.editorconfig` ensures editors respect line ending settings
- Prettier's `endOfLine` setting (currently not configured) enforces at format time

**Decision**: Implement three-layer enforcement
1. **`.gitattributes`**: `* text=auto eol=lf` (force LF in repository)
2. **`.editorconfig`**: `end_of_line = lf` (guide editor behavior)
3. **`.prettierrc.json`**: Add `"endOfLine": "lf"` (enforce in formatting)

**Rationale**: Multiple enforcement layers prevent regression and ensure consistency across tools/environments

**Implementation Steps**:
```bash
# 1. Create .gitattributes
echo "* text=auto eol=lf" > .gitattributes
echo "*.png binary" >> .gitattributes
echo "*.jpg binary" >> .gitattributes

# 2. Create .editorconfig
# (see configuration below)

# 3. Update .prettierrc.json
# Add "endOfLine": "lf"

# 4. Normalize existing files (one-time operation)
git add --renormalize .
git commit -m "chore: normalize line endings to LF"
```

**Alternatives Considered**:
- Only `.gitattributes`: Insufficient, editors may still create CRLF files
- Only `.editorconfig`: Not enforced by git, inconsistent across clones
- Only Prettier: Doesn't affect non-formatted files

---

### 4. TypeScript `any` Type Elimination Strategy

**Task**: Find best practices for eliminating `any` types without breaking code

**Findings**:
- TypeScript's `noImplicitAny` compiler option catches implicit `any` (already enabled via `strict: true`)
- Explicit `any` usage requires manual review and replacement
- Common replacement strategies:
  - **Generic constraints**: `<T extends SomeInterface>` instead of `<T = any>`
  - **Union types**: `string | number` instead of `any`
  - **Type guards**: `is` predicates for runtime type narrowing
  - **`unknown` type**: Safer alternative when type is truly unknown
  - **`Record<string, unknown>`**: For object maps with unknown structure

**Decision**: Systematic file-by-file replacement approach
1. Identify all `any` usage: `grep -rn "any" src/main/frontend --include="*.ts" --include="*.tsx"`
2. For each instance, apply replacement strategy:
   - Event handlers: Use proper DOM event types (`MouseEvent`, `KeyboardEvent`)
   - API responses: Define explicit interfaces
   - Third-party library gaps: Use `unknown` with type guards
   - Truly dynamic data: Use `Record<string, unknown>` or `Map`

**Rationale**: Type safety improvements require understanding code intent; no automated approach is safe

**Alternatives Considered**:
- Blanket replacement with `unknown`: Too strict, breaks valid dynamic code
- Automated type inference tools (e.g., `ts-migrate`): Too aggressive, creates maintenance burden
- Ignore `any` in tests: Rejected, tests should also be type-safe

---

### 5. CI/CD Integration for Regression Prevention

**Task**: Research best practices for enforcing linting in continuous integration

**Findings**:
- GitHub Actions, GitLab CI, Jenkins all support pre-merge checks
- package.json already has `ci:check` script combining lint + type-check + tests
- Pre-commit hooks (e.g., Husky) can enforce locally before push
- Branch protection rules can require passing checks

**Decision**: Multi-level enforcement strategy
1. **Local (optional but recommended)**: Husky pre-commit hooks
   - Runs `npm run precommit` (format + lint:fix + tests)
   - Developers can bypass with `--no-verify` if needed for WIP commits

2. **CI/CD (mandatory)**: Add GitHub Actions workflow
   ```yaml
   name: Code Quality
   on: [pull_request]
   jobs:
     lint:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v4
         - uses: actions/setup-node@v4
           with:
             node-version: '20'
         - run: npm ci
         - run: npm run ci:check
   ```

3. **Branch Protection**: Require passing CI checks before merge

**Rationale**: Local checks provide fast feedback; CI checks enforce policy; both layers ensure quality

**Alternatives Considered**:
- Only CI checks: Slower feedback loop, wastes CI resources on preventable failures
- Only pre-commit hooks: Not enforced for force-pushes or hook-disabled commits
- Blocking pre-commit hooks: Too rigid, prevents WIP commits

---

### 6. ESLint Rule Configuration Validation

**Task**: Verify current ESLint configuration is appropriate and not overly strict

**Findings**:
- Current extends: `eslint:recommended`, `@typescript-eslint/recommended`, `prettier/recommended`
- These are industry-standard, well-balanced presets
- Current custom rules: Only `"prettier/prettier": "error"` (appropriate)
- No overly strict rules that would force bad patterns

**Decision**: Maintain current ESLint configuration with minor additions
- Add `@typescript-eslint/no-explicit-any: "error"` to catch new `any` usage
- Consider adding `@typescript-eslint/explicit-function-return-type: "warn"` (warn only, not error)

**Configuration Updates**:
```javascript
// .eslintrc.cjs - additions to rules section
rules: {
    "prettier/prettier": "error",
    "@typescript-eslint/no-explicit-any": "error",  // NEW: Prevent new `any` usage
    "@typescript-eslint/explicit-function-return-type": "warn"  // NEW: Encourage type annotations
}
```

**Rationale**: Current config is solid; targeted additions prevent regression without burdening developers

**Alternatives Considered**:
- Adopt stricter preset (e.g., `airbnb-typescript`): Rejected, too opinionated and requires major refactoring
- Disable `any` warnings: Rejected, defeats purpose of type safety improvements
- Make return types mandatory (error): Too strict, TypeScript inference often sufficient

---

### 7. EditorConfig Configuration

**Task**: Define appropriate `.editorconfig` settings for the project

**Findings**:
- `.editorconfig` supported by all major IDEs (VS Code, IntelliJ, Vim, etc.)
- Should align with Prettier and ESLint configurations
- Current Prettier config: 4 spaces, no tabs, LF endings

**Decision**: Create `.editorconfig` matching existing conventions

**Configuration**:
```ini
# .editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.{ts,tsx,js,jsx,json}]
indent_style = space
indent_size = 4

[*.{md,yml,yaml}]
indent_style = space
indent_size = 2

[*.java]
indent_style = space
indent_size = 4
```

**Rationale**: Provides editor guidance consistent with Prettier, covers both frontend and backend

---

### 8. Phased Rollout Strategy

**Task**: Determine safest approach to merge large-scale cleanup changes

**Findings**:
- Large single PR creates review bottleneck and merge conflict risk
- Too many small PRs creates overhead
- Phased approach balances reviewability and efficiency

**Decision**: 4-phase commit strategy within single PR
1. **Commit 1**: Configuration changes (`.editorconfig`, `.gitattributes`, ESLint rules)
2. **Commit 2**: Automated fixes (`npm run lint:fix && npm run format`)
3. **Commit 3**: Line ending normalization (`git add --renormalize .`)
4. **Commit 4**: Manual `any` type replacements (file-by-file with separate sub-commits if large)

**Rationale**: Separates mechanical changes from semantic changes; aids code review and potential rollback

**Alternatives Considered**:
- Single massive commit: Not reviewable, risky
- Separate PRs per phase: Too much overhead, merge conflicts between phases
- Per-file PRs: Excessive overhead for ~20-30 files

---

### 9. Testing Strategy

**Task**: Ensure cleanup doesn't break functionality

**Findings**:
- Project has comprehensive test suite: Vitest unit tests + Playwright E2E tests
- `npm run test:run` executes tests with coverage
- CI check script runs tests automatically

**Decision**: Multi-level testing approach
1. **After automated fixes**: Run `npm run test:run` to catch any breakage
2. **After each manual type change**: Run tests for affected module
3. **Before final commit**: Run full `npm run ci:check` (lint + type-check + tests + build)
4. **In CI**: Automated test execution on PR

**Rationale**: Catch breakage as early as possible; incremental testing more efficient than end-to-end only

---

## Summary of Technical Decisions

| Decision Area | Choice | Rationale |
|---------------|--------|-----------|
| **Cleanup Approach** | Phased: Auto → Manual → CI | Maximizes efficiency, minimizes risk |
| **Line Endings** | Three-layer enforcement (git, editor, prettier) | Comprehensive prevention of regression |
| **`any` Elimination** | Systematic file-by-file review | Requires semantic understanding, no safe automation |
| **ESLint Config** | Maintain current + add `no-explicit-any` | Balanced, prevents regression |
| **EditorConfig** | Create matching Prettier/ESLint | Cross-editor consistency |
| **CI Integration** | GitHub Actions + optional pre-commit hooks | Multi-level enforcement |
| **Rollout** | Single PR with 4 logical commits | Balances reviewability and efficiency |
| **Testing** | Incremental + final full test suite | Early detection, efficient feedback |

## Next Steps (Phase 1)

With research complete, proceed to Phase 1:
1. Generate `data-model.md` (minimal for this cleanup feature)
2. Generate `contracts/` (CLI script contracts if applicable)
3. Generate `quickstart.md` (developer guide for running cleanup)
4. Update agent context

**Estimated Research Time**: 2 hours
**Estimated Implementation Time**: 8-16 hours (depends on violation count and `any` usage complexity)
