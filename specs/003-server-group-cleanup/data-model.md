# Phase 1: Data Model

**Feature**: Frontend Code Quality Cleanup
**Date**: 2025-10-23
**Status**: Complete

## Overview

This code quality cleanup feature is **infrastructure-focused** and does not introduce new data entities or modify existing data models. The "data" in this context refers to code artifacts (TypeScript files, configuration files) rather than runtime application data.

## Entity Analysis

### From Feature Specification

The specification identified these "Key Entities":
- **ESLint Configuration**: Rules and settings for code quality standards
- **Prettier Configuration**: Formatting rules for consistent code style
- **TypeScript Files**: Source code files requiring type annotations and linting compliance
- **EditorConfig**: Cross-editor configuration file for consistent coding styles

These are **configuration artifacts**, not runtime data models. They do not require database schemas, API contracts, or state management.

## Configuration Artifacts (Not Data Models)

### 1. ESLint Configuration

**File**: `.eslintrc.cjs`
**Purpose**: Define code quality standards and linting rules
**Type**: CommonJS module (JavaScript)

**Schema** (conceptual):
```javascript
{
    root: boolean,
    env: {
        browser: boolean,
        es2021: boolean,
        node: boolean
    },
    parser: string,
    parserOptions: {
        ecmaVersion: string,
        sourceType: "module" | "script"
    },
    plugins: string[],
    extends: string[],
    ignorePatterns: string[],
    rules: {
        [ruleName: string]: "off" | "warn" | "error" | [severity, ...options]
    }
}
```

**Validation Rules**:
- Must extend at least `eslint:recommended`
- Must include `@typescript-eslint/parser` for TypeScript files
- Must not have conflicting rules with Prettier

**State Transitions**: N/A (static configuration)

---

### 2. Prettier Configuration

**File**: `.prettierrc.json`
**Purpose**: Define code formatting standards
**Type**: JSON

**Schema**:
```json
{
    "printWidth": number,        // Max line length (currently 100)
    "tabWidth": number,          // Spaces per indentation (currently 4)
    "useTabs": boolean,          // Use tabs vs spaces (currently false)
    "semi": boolean,             // Require semicolons (currently true)
    "singleQuote": boolean,      // Use single quotes (currently false)
    "trailingComma": "none" | "es5" | "all",
    "bracketSpacing": boolean,   // Space in object literals (currently true)
    "arrowParens": "avoid" | "always",
    "endOfLine": "lf" | "crlf" | "cr" | "auto"  // NEW: Will add "lf"
}
```

**Validation Rules**:
- Must be valid JSON
- Must be compatible with ESLint rules (no conflicts)
- `endOfLine` must match `.editorconfig` and `.gitattributes`

**State Transitions**: N/A (static configuration)

---

### 3. TypeScript Configuration

**File**: `tsconfig.json`
**Purpose**: TypeScript compiler settings
**Type**: JSON with comments (JSONC)

**Relevant Fields** (cleanup-related):
```json
{
    "compilerOptions": {
        "strict": boolean,              // Currently true (enables noImplicitAny)
        "noImplicitAny": boolean,       // Implied by strict:true
        "strictNullChecks": boolean,    // Implied by strict:true
        "target": string,               // ES2020
        "module": string,               // ESNext
        // ... other compiler options
    },
    "include": string[],
    "exclude": string[]
}
```

**Validation Rules**:
- `strict: true` must be maintained (enforces type safety)
- `include` must cover `src/main/frontend/**/*`

**State Transitions**: N/A (static configuration)

---

### 4. EditorConfig

**File**: `.editorconfig` (NEW - will be created)
**Purpose**: Cross-editor formatting hints
**Type**: INI format

**Schema**:
```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.{pattern}]
indent_style = space | tab
indent_size = number
```

**Validation Rules**:
- `end_of_line` must be `lf` (matching Prettier and gitattributes)
- `indent_size` must match Prettier's `tabWidth` (4)
- `indent_style` must be `space` (matching Prettier's `useTabs: false`)

**State Transitions**: N/A (static configuration)

---

### 5. Git Attributes

**File**: `.gitattributes` (NEW - will be created)
**Purpose**: Define line ending handling in version control
**Type**: Plain text (git-specific format)

**Schema**:
```
<pattern> text|binary [eol=lf|crlf]
```

**Example Content**:
```
* text=auto eol=lf
*.png binary
*.jpg binary
*.jar binary
```

**Validation Rules**:
- Must enforce `eol=lf` for text files
- Binary files must be marked as `binary`

**State Transitions**: N/A (static configuration)

---

## "Data" Flows (Code Quality Process)

While this feature doesn't involve traditional data models, it does have a **process flow** involving code artifacts:

### Cleanup Process Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ Input: Codebase with Linting Violations                        │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: Configuration Updates                                   │
│ - Create .editorconfig                                          │
│ - Create .gitattributes                                         │
│ - Update .eslintrc.cjs (add no-explicit-any rule)              │
│ - Update .prettierrc.json (add endOfLine: lf)                  │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 2: Automated Fixes                                         │
│ - Run: npm run lint:fix                                         │
│ - Run: npm run format                                           │
│ - Fixes: style violations, formatting issues                   │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 3: Line Ending Normalization                               │
│ - Run: git add --renormalize .                                  │
│ - Converts all text files to LF                                 │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│ Step 4: Manual Type Improvements                                │
│ - Replace `any` with explicit types                             │
│ - Add missing type annotations                                  │
│ - Run tests after each file modification                        │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│ Validation: Run CI Check                                        │
│ - npm run ci:check                                              │
│   ├── npm run lint (must pass with 0 errors)                   │
│   ├── npm run type-check (must pass)                            │
│   ├── npm run test:run (must pass)                              │
│   └── npm run build (must succeed)                              │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│ Output: Clean Codebase                                          │
│ - Zero linting errors/warnings                                  │
│ - No `any` types in production code                             │
│ - LF line endings everywhere                                    │
│ - All tests passing                                             │
└─────────────────────────────────────────────────────────────────┘
```

### Validation State Machine

Each TypeScript file transitions through these states:

```
┌──────────┐  lint:fix   ┌──────────┐  manual   ┌──────────┐  validation  ┌──────────┐
│ Dirty    ├────────────>│ Auto-    ├──────────>│ Type-    ├─────────────>│ Clean    │
│ (has     │             │ Fixed    │   review  │ Safe     │   (tests     │ (ready   │
│ viola-   │             │ (style   │           │ (no any) │    pass)     │  to      │
│ tions)   │             │  clean)  │           │          │              │  merge)  │
└──────────┘             └──────────┘           └──────────┘              └──────────┘
     │                        │                      │                         │
     │                        │ manual fixes fail    │ tests fail              │
     │                        ▼──────────────────────▼─────────────────────────┘
     │                   ┌──────────┐
     └───────────────────┤ Needs    │
        restart process  │ Fix      │
                         └──────────┘
```

## Relationships Between Artifacts

```
.gitattributes ────> enforces line endings in git
      │
      └──> must align with
                │
                ▼
           .editorconfig ────> guides editor behavior
                │
                └──> must align with
                          │
                          ▼
                     .prettierrc.json ────> formats code
                          │
                          └──> must be compatible with
                                    │
                                    ▼
                               .eslintrc.cjs ────> validates code quality
                                    │
                                    └──> validated by
                                              │
                                              ▼
                                         tsconfig.json ────> compiles TypeScript
                                              │
                                              └──> compiles
                                                        │
                                                        ▼
                                                   *.ts files
```

## No Database Schema Required

This feature does **not** require:
- Database tables or migrations
- API endpoints for CRUD operations
- State persistence beyond file system
- User-facing data models

All "data" is source code and configuration files managed by git.

## Summary

**Entity Count**: 0 runtime data entities
**Configuration Files**: 5 (ESLint, Prettier, TypeScript, EditorConfig, GitAttributes)
**State Management**: File system + git version control
**Persistence**: Git repository (no database)
**Validation**: Linting tools + TypeScript compiler + test suite

This feature is purely infrastructure/tooling focused with no traditional data modeling requirements.
