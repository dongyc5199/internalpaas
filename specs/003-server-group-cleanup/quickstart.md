# Quick Start: Frontend Code Quality Cleanup

**Feature**: 003-server-group-cleanup
**Branch**: `003-server-group-cleanup`
**Estimated Time**: 8-16 hours (depends on violation count)

## Purpose

This guide walks developers through the systematic cleanup of ESLint/Prettier violations across the frontend TypeScript codebase. Follow these steps to achieve zero linting errors, eliminate `any` types, and standardize line endings.

---

## Prerequisites

### Required Tools

- **Node.js**: v20.x or higher
- **npm**: v10.x or higher
- **Git**: v2.40 or higher
- **IDE**: VS Code (recommended) or IntelliJ IDEA with ESLint + Prettier extensions

### Verify Setup

```bash
# Check Node/npm versions
node --version  # Should be v20.x+
npm --version   # Should be v10.x+

# Check you're on the cleanup branch
git branch --show-current  # Should show: 003-server-group-cleanup

# Install dependencies (if not already done)
npm install

# Verify linting tools work
npm run lint -- --version  # Should show ESLint version
```

---

## Phase 1: Audit Current State

### 1.1 Capture Baseline Violations

```bash
# Run lint and capture output
npm run lint > audit-baseline.txt 2>&1

# View the report
cat audit-baseline.txt

# Count violations by type
grep "error" audit-baseline.txt | wc -l
grep "warning" audit-baseline.txt | wc -l
```

**Expected Output**: Will show various violations including:
- Prettier formatting issues
- `any` type usage
- Missing type annotations
- Possible CRLF line ending issues (shown as prettier/prettier errors)

### 1.2 Count `any` Type Usage

```bash
# Find all explicit `any` usage
grep -rn ": any" src/main/frontend --include="*.ts" --include="*.tsx" > any-usage-audit.txt

# Count instances
wc -l any-usage-audit.txt
```

**Save these baseline reports** for tracking progress.

---

## Phase 2: Configuration Updates

### 2.1 Create `.editorconfig`

```bash
# Create .editorconfig file at project root
cat > .editorconfig << 'EOF'
# EditorConfig: https://editorconfig.org
root = true

# Default settings for all files
[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

# TypeScript/JavaScript files
[*.{ts,tsx,js,jsx,json}]
indent_style = space
indent_size = 4

# Markdown, YAML files (2-space indent)
[*.{md,yml,yaml}]
indent_style = space
indent_size = 2

# Java files (backend)
[*.java]
indent_style = space
indent_size = 4
EOF

# Verify file was created
cat .editorconfig
```

### 2.2 Create `.gitattributes`

```bash
# Create .gitattributes file at project root
cat > .gitattributes << 'EOF'
# Force LF line endings for text files
* text=auto eol=lf

# Binary files (no line ending conversion)
*.png binary
*.jpg binary
*.jpeg binary
*.gif binary
*.ico binary
*.jar binary
*.war binary
*.class binary
*.zip binary
*.pdf binary
EOF

# Verify file was created
cat .gitattributes
```

### 2.3 Update `.prettierrc.json`

```bash
# Backup current config
cp .prettierrc.json .prettierrc.json.backup

# Add endOfLine setting (use your preferred text editor)
# Or use this command to update in place:
cat > .prettierrc.json << 'EOF'
{
    "printWidth": 100,
    "tabWidth": 4,
    "useTabs": false,
    "semi": true,
    "singleQuote": false,
    "trailingComma": "none",
    "bracketSpacing": true,
    "arrowParens": "always",
    "endOfLine": "lf"
}
EOF

# Verify the change
cat .prettierrc.json
```

### 2.4 Update `.eslintrc.cjs`

```bash
# Backup current config
cp .eslintrc.cjs .eslintrc.cjs.backup

# Edit .eslintrc.cjs and add these rules to the "rules" section:
# (Use your IDE or text editor)
```

**Add to the `rules` section**:
```javascript
rules: {
    "prettier/prettier": "error",
    "@typescript-eslint/no-explicit-any": "error",  // NEW
    "@typescript-eslint/explicit-function-return-type": "warn"  // NEW (optional)
}
```

### 2.5 Commit Configuration Changes

```bash
# Stage configuration files
git add .editorconfig .gitattributes .prettierrc.json .eslintrc.cjs

# Commit (separate from code changes)
git commit -m "chore(config): add EditorConfig, GitAttributes, and update linting rules

- Add .editorconfig for cross-editor consistency (LF endings, 4-space indent)
- Add .gitattributes to enforce LF line endings in git
- Update .prettierrc.json to explicitly set endOfLine: lf
- Update .eslintrc.cjs to prohibit explicit 'any' types

Related to: Frontend Code Quality Cleanup (003-server-group-cleanup)"
```

---

## Phase 3: Automated Fixes

### 3.1 Run Auto-Fix

```bash
# Run ESLint auto-fix
npm run lint:fix

# Run Prettier formatting
npm run format

# Check what changed
git status
git diff --stat
```

**Review the changes carefully**. Most should be formatting fixes (indentation, spacing, semicolons).

### 3.2 Verify Tests Still Pass

```bash
# Run tests after auto-fixes
npm run test:run

# Expected: All tests pass (same count as before)
```

**If tests fail**: The auto-fix broke something. Review the changes carefully and fix manually.

### 3.3 Commit Automated Fixes

```bash
# Stage all auto-fixed files
git add -u

# Commit with descriptive message
git commit -m "style: apply automated ESLint and Prettier fixes

- Fix indentation, spacing, and formatting issues
- Add missing semicolons and adjust quote styles
- Auto-fixable violations: [X errors, Y warnings]

All tests passing. No behavioral changes.

Applied via: npm run lint:fix && npm run format"
```

---

## Phase 4: Line Ending Normalization

### 4.1 Normalize Line Endings

```bash
# Apply .gitattributes rules to existing files
git add --renormalize .

# Check what changed (should only be line endings)
git status
git diff --stat
```

**Expected**: Many files show as modified, but changes are only line endings (CRLF → LF).

### 4.2 Verify Normalization

```bash
# Check that files are now LF
file src/main/frontend/main.ts
# Should show: ASCII text, with LF line terminators

# Or use git to verify
git diff --cached --word-diff=plain | head -50
# Should see ^M characters removed (CRLF → LF)
```

### 4.3 Commit Line Ending Changes

```bash
# Commit the normalized files
git commit -m "chore: normalize line endings to LF

Apply .gitattributes line ending policy to existing files.
All text files now use LF (Unix-style) line endings.

Applied via: git add --renormalize ."
```

---

## Phase 5: Manual Type Improvements

### 5.1 List Remaining Violations

```bash
# Run lint to see remaining issues
npm run lint > remaining-violations.txt 2>&1

# View violations
cat remaining-violations.txt

# Focus on `any` type violations
grep "no-explicit-any" remaining-violations.txt
```

### 5.2 Fix `any` Types File-by-File

**Strategy**: Work through files one at a time, running tests after each fix.

```bash
# Example: Fix dashboard.ts
# 1. Open the file in your IDE
code src/main/frontend/modules/dashboard.ts

# 2. Find `any` usage (IDE should highlight errors)
# 3. Replace with explicit types
# 4. Run tests for that module
npm run test:run -- dashboard.test.ts

# 5. If tests pass, commit the fix
git add src/main/frontend/modules/dashboard.ts
git commit -m "refactor(dashboard): replace 'any' types with explicit types

- Replace 'any' in event handlers with MouseEvent/KeyboardEvent
- Define ServerData interface for API responses
- Use Record<string, unknown> for dynamic config objects

Tests passing: dashboard.test.ts"
```

**Common Type Replacements**:

| Old Type | New Type | Use Case |
|----------|----------|----------|
| `any` | `MouseEvent` / `KeyboardEvent` | DOM event handlers |
| `any` | `HTMLElement` / `HTMLButtonElement` | DOM element references |
| `any[]` | `ServerData[]` | API response arrays |
| `any` | `Record<string, unknown>` | Dynamic object maps |
| `any` | `unknown` | Truly unknown data (with type guards) |
| `Function` | `(param: Type) => ReturnType` | Callback functions |

### 5.3 Track Progress

```bash
# After each file fix, check remaining count
npm run lint 2>&1 | grep "no-explicit-any" | wc -l

# Compare to baseline
echo "Baseline: $(wc -l < any-usage-audit.txt) instances"
echo "Current: $(grep -rn ": any" src/main/frontend --include="*.ts" --include="*.tsx" | wc -l) instances"
```

### 5.4 Final Type Check

```bash
# Ensure TypeScript compilation passes
npm run type-check

# Expected: Exit code 0, no errors
```

---

## Phase 6: Final Validation

### 6.1 Run Full CI Check

```bash
# This runs: lint + lint:style + type-check + test:run + build
npm run ci:check

# Expected: All checks pass, exit code 0
```

**If any check fails**: Fix the issues before proceeding.

### 6.2 Verify Success Criteria

```bash
# ✅ SC-001: Linting completes in under 60 seconds
time npm run lint
# Should complete in < 60s with exit code 0

# ✅ SC-002: 100% of TypeScript files pass
npm run lint
# Should show: 0 errors, 0 warnings

# ✅ SC-003: Zero 'any' types remain
grep -rn ": any" src/main/frontend --include="*.ts" --include="*.tsx"
# Should return no results (or only documented exceptions in .d.ts files)

# ✅ SC-004: All files use LF line endings
find src/main/frontend -type f \( -name "*.ts" -o -name "*.tsx" \) -exec file {} \; | grep -v "LF"
# Should return no results (all files have LF)

# ✅ SC-006: Tests pass
npm run test:run
# Should show all tests passing
```

### 6.3 Final Commit (If Any Remaining Fixes)

```bash
# If you made any final tweaks, commit them
git add -u
git commit -m "chore: final code quality cleanup adjustments

- Final type annotations added
- Final linting violations resolved

All success criteria met:
✅ npm run lint: 0 errors, 0 warnings
✅ npm run type-check: passes
✅ npm run test:run: all tests passing
✅ Line endings: 100% LF
✅ 'any' types: zero instances"
```

---

## Phase 7: Push and Create Pull Request

### 7.1 Push Branch

```bash
# Push the cleanup branch to remote
git push -u origin 003-server-group-cleanup
```

### 7.2 Create Pull Request

```bash
# Using GitHub CLI (if installed)
gh pr create --base master --head 003-server-group-cleanup \
  --title "chore: Frontend Code Quality Cleanup" \
  --body "$(cat <<'EOF'
## Summary
Systematic cleanup of ESLint/Prettier violations across the frontend TypeScript codebase.

## Changes
- ✅ Configuration updates (EditorConfig, GitAttributes, Prettier, ESLint)
- ✅ Automated style fixes (formatting, indentation, spacing)
- ✅ Line ending normalization (all files now use LF)
- ✅ `any` type elimination (replaced with explicit types)

## Validation
- ✅ `npm run lint`: 0 errors, 0 warnings
- ✅ `npm run type-check`: passes
- ✅ `npm run test:run`: 100% tests passing
- ✅ `npm run build`: successful

## Commits
- Configuration updates (separate commit for easy review)
- Automated fixes (auto-generated changes)
- Line ending normalization (git renormalize)
- Manual type improvements (reviewed and tested)

## Testing
- All existing tests pass (zero test modifications required)
- No behavioral changes
- Only code quality improvements

## Review Notes
- Review configuration files first (commit 1)
- Automated fixes are mechanical (commit 2-3)
- Manual type changes have test coverage (commit 4+)

Resolves technical debt documented in: docs/server-group-management-pending.md
EOF
)"
```

**Or manually**: Create PR via GitHub web interface using the template above.

---

## Troubleshooting

### Issue: Tests Fail After Auto-Fix

**Symptoms**: `npm run test:run` fails after running `npm run lint:fix`

**Solution**:
```bash
# Revert auto-fixes
git reset --hard HEAD

# Run lint:fix with --debug to see what changed
npm run lint:fix -- --debug

# Apply fixes manually or in smaller batches
npm run lint:fix -- "src/main/frontend/modules/dashboard.ts"
npm run test:run -- dashboard.test.ts
```

---

### Issue: Line Endings Still CRLF on Windows

**Symptoms**: `git diff` shows line ending changes even after normalization

**Solution**:
```bash
# Configure git to not auto-convert
git config core.autocrlf false

# Re-normalize
rm .git/index
git reset
git add --renormalize .
git commit -m "chore: re-normalize line endings with autocrlf disabled"
```

---

### Issue: Too Many `any` Type Violations to Fix Manually

**Symptoms**: 50+ instances of `any`, would take days to fix

**Solution**:
- **Prioritize**: Fix `any` types in critical paths (API boundaries, event handlers)
- **Document exceptions**: Add ESLint disable comments with justification:
  ```typescript
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const config: any = JSON.parse(dynamicConfig);  // TODO: Define ConfigSchema
  ```
- **Create follow-up task**: Document remaining `any` types as separate cleanup task

---

### Issue: ESLint and Prettier Conflict

**Symptoms**: Running `npm run lint:fix` followed by `npm run format` causes changes to flip-flop

**Solution**:
```bash
# Verify plugin:prettier/recommended is in extends
grep "prettier/recommended" .eslintrc.cjs

# If missing, add it:
# extends: [..., "plugin:prettier/recommended"]

# This disables conflicting ESLint formatting rules
```

---

## Next Steps After Merge

### 1. Set Up Pre-Commit Hooks (Optional)

```bash
# Install Husky
npm install --save-dev husky
npx husky init

# Create pre-commit hook
cat > .husky/pre-commit << 'EOF'
#!/bin/sh
npm run precommit
EOF

chmod +x .husky/pre-commit
```

### 2. Configure GitHub Branch Protection

Enable these settings on `master`/`main` branch:
- ✅ Require status checks to pass before merging
- ✅ Require branches to be up to date before merging
- ✅ Required check: `quality-check` (from CI workflow)

### 3. Update Team Documentation

Document the new quality standards:
- All new TypeScript files must pass `npm run lint` with 0 errors
- No `any` types allowed (without justification)
- Pre-commit hooks recommended (but optional)
- CI checks are mandatory

---

## Summary Checklist

Before creating the pull request, verify:

- [ ] `.editorconfig` created and committed
- [ ] `.gitattributes` created and committed
- [ ] `.prettierrc.json` updated with `endOfLine: "lf"`
- [ ] `.eslintrc.cjs` updated with `no-explicit-any` rule
- [ ] `npm run lint:fix` executed (auto-fixes applied)
- [ ] `npm run format` executed (Prettier formatting applied)
- [ ] `git add --renormalize .` executed (line endings normalized)
- [ ] All `any` types replaced with explicit types (or documented exceptions)
- [ ] `npm run ci:check` passes (0 errors)
- [ ] All tests pass (`npm run test:run`)
- [ ] Commits are logical and well-described
- [ ] Branch pushed to remote
- [ ] Pull request created with description

---

## Resources

- **ESLint Rules**: https://eslint.org/docs/rules/
- **TypeScript Handbook**: https://www.typescriptlang.org/docs/handbook/
- **Prettier Options**: https://prettier.io/docs/en/options.html
- **EditorConfig**: https://editorconfig.org/
- **Git Attributes**: https://git-scm.com/docs/gitattributes

**Estimated Time Breakdown**:
- Configuration setup: 1 hour
- Automated fixes: 1 hour
- Line ending normalization: 30 minutes
- Manual type improvements: 4-12 hours (variable)
- Testing and validation: 1-2 hours

**Total: 8-16 hours** depending on codebase complexity.
