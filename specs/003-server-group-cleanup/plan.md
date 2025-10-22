# Implementation Plan: Frontend Code Quality Cleanup

**Branch**: `003-server-group-cleanup` | **Date**: 2025-10-23 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-server-group-cleanup/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

This implementation plan addresses systematic cleanup of ESLint/Prettier violations across the frontend TypeScript codebase. The primary requirement is to achieve zero linting errors and warnings (`npm run lint` passes with exit code 0), eliminate all `any` type usage, and standardize line endings to LF. The technical approach involves a phased cleanup strategy starting with auto-fixable issues, followed by manual type improvements and line ending normalization, concluding with CI/CD integration to prevent regression.

## Technical Context

**Language/Version**: TypeScript 5.9.3 (targeting ES2020)
**Primary Dependencies**:
- ESLint 8.57.1 with @typescript-eslint plugins (v7.18.0)
- Prettier 3.6.2 with eslint-plugin-prettier integration
- Stylelint 16.25.0 for CSS/SCSS linting
- Vite 5.4.20 for build tooling
- Vitest 1.6.1 for unit testing

**Storage**: N/A (code quality cleanup, no data storage)
**Testing**:
- Vitest for unit tests
- Playwright 1.56.0 for E2E tests
- Custom scripts: `npm run lint`, `npm run type-check`

**Target Platform**: Browser (frontend) + Node.js (development tooling)
**Project Type**: Web application (Spring Boot backend + TypeScript frontend)
**Performance Goals**:
- Linting execution completes in under 60 seconds
- No impact on runtime application performance (cleanup only affects development experience)

**Constraints**:
- Must maintain backward compatibility with existing functionality
- Cannot break existing features during cleanup
- Must coordinate with ongoing development to minimize merge conflicts
- Cleanup must be reviewable (staged commits, not massive single commit)

**Scale/Scope**:
- Approximately 20-30 TypeScript files in `src/main/frontend/`
- Target: ~2000-3000 lines of TypeScript code
- Current violations: Unknown exact count (needs audit)
- Expected effort: 2-5 days developer time

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Status**: ✅ **PASS** (Constitution file is currently a template with no active principles)

The project's constitution file (`.specify/memory/constitution.md`) contains only placeholder templates without ratified principles. Therefore, there are no constitutional gates to evaluate at this time.

**Recommendation**: Consider establishing project constitution principles for future features, especially around:
- Code quality standards (directly relevant to this cleanup effort)
- Testing requirements (mentioned in the pending document)
- Review and approval processes
- Technical debt management policies

**Re-evaluation after Phase 1**: Will verify that the design artifacts comply with any newly established principles if the constitution is updated during this feature development.

## Project Structure

### Documentation (this feature)

```
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```
src/main/frontend/              # TypeScript frontend code (cleanup target)
├── main.ts                     # Entry point
├── modules/                    # Feature modules
│   ├── dashboard.ts
│   ├── server-group-management.ts  # Previously cleaned file
│   ├── server-modal.ts
│   ├── ServerDetailOverlay.ts
│   ├── ServerListManager.ts
│   ├── SSHConfigImportWizard.ts
│   ├── tab-version-methods.ts
│   └── theme.ts
├── tests/                      # Unit tests
│   ├── bootstrap.test.ts
│   ├── chart.test.ts
│   ├── dashboard.test.ts
│   └── [other test files]
└── [configuration files]

# Configuration files (project root)
├── .eslintrc.cjs              # ESLint configuration
├── .prettierrc.json           # Prettier configuration
├── tsconfig.json              # TypeScript configuration
├── package.json               # npm scripts and dependencies
└── .editorconfig              # NOT PRESENT (needs creation)

# Linting/formatting tools
├── node_modules/              # Dependencies (excluded from linting)
└── target/                    # Build output (excluded from linting)
```

**Structure Decision**: This is a **web application** with a Spring Boot backend (Java) and a TypeScript frontend. This cleanup effort focuses **exclusively on the frontend** located in `src/main/frontend/`. The backend Java code is explicitly out of scope.

**Key Directories for This Feature**:
- **Cleanup target**: `src/main/frontend/**/*.{ts,tsx}` (approximately 20-30 files)
- **Configuration targets**: `.eslintrc.cjs`, `.prettierrc.json`, `.editorconfig` (create), `.gitattributes` (create/update)
- **Test files**: `src/main/frontend/tests/**/*.test.ts` (also subject to linting rules)

## Complexity Tracking

*Fill ONLY if Constitution Check has violations that must be justified*

**Status**: N/A - No constitutional violations (constitution not yet ratified)

