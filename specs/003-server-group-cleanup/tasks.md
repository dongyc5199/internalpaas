# Tasks: Frontend Code Quality Cleanup

**Input**: Design documents from `/specs/003-server-group-cleanup/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: This is a code quality cleanup initiative. Tests already exist and MUST continue passing. No new test creation required.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each code quality improvement area.

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1=Linting, US2=Types, US3=Line Endings)
- Include exact file paths in descriptions

## Path Conventions
- **Frontend code**: `src/main/frontend/`
- **Configuration files**: Project root (`.eslintrc.cjs`, `.prettierrc.json`, etc.)
- **Test files**: `src/main/frontend/tests/`

---

## Phase 1: Setup & Audit

**Purpose**: Configuration creation and baseline measurement

- [ ] T001 [P] Create `.editorconfig` file at project root with cross-editor formatting settings
- [ ] T002 [P] Create `.gitattributes` file at project root with LF line ending enforcement for text files
- [ ] T003 [P] Update `.prettierrc.json` to add `"endOfLine": "lf"` configuration
- [ ] T004 Update `.eslintrc.cjs` to add `"@typescript-eslint/no-explicit-any": "error"` rule in rules section
- [ ] T005 Run `npm run lint > specs/003-server-group-cleanup/audit-baseline.txt 2>&1` to capture current violations
- [ ] T006 Run `grep -rn ": any" src/main/frontend --include="*.ts" --include="*.tsx" > specs/003-server-group-cleanup/any-usage-audit.txt` to capture `any` type usage
- [ ] T007 Commit configuration changes with message: "chore(config): add EditorConfig, GitAttributes, and update linting rules"

**Checkpoint**: Configuration files in place, baseline violations documented

---

## Phase 2: User Story 1 - Developer Code Quality Experience (Priority: P1) 🎯 MVP

**Goal**: Achieve zero ESLint/Prettier errors and warnings (`npm run lint` passes with exit code 0)

**Independent Test**: Run `npm run lint` from project root and verify exit code 0 with zero errors/warnings

### Automated Fixes for User Story 1

- [ ] T008 [US1] Run `npm run lint:fix` to apply ESLint auto-fixes across all frontend TypeScript files
- [ ] T009 [US1] Run `npm run format` to apply Prettier formatting across all frontend TypeScript files
- [ ] T010 [US1] Review `git diff` output to verify changes are only formatting (no logic changes)
- [ ] T011 [US1] Run `npm run test:run` to verify all existing tests still pass after formatting changes
- [ ] T012 [US1] Commit automated fixes with message: "style: apply automated ESLint and Prettier fixes"

### Manual Linting Fixes for User Story 1

- [ ] T013 [US1] Run `npm run lint > specs/003-server-group-cleanup/remaining-violations.txt 2>&1` to identify remaining violations
- [ ] T014 [P] [US1] Fix remaining ESLint violations in `src/main/frontend/main.ts` (if any)
- [ ] T015 [P] [US1] Fix remaining ESLint violations in `src/main/frontend/modules/dashboard.ts` (if any)
- [ ] T016 [P] [US1] Fix remaining ESLint violations in `src/main/frontend/modules/server-modal.ts` (if any)
- [ ] T017 [P] [US1] Fix remaining ESLint violations in `src/main/frontend/modules/ServerDetailOverlay.ts` (if any)
- [ ] T018 [P] [US1] Fix remaining ESLint violations in `src/main/frontend/modules/ServerListManager.ts` (if any)
- [ ] T019 [P] [US1] Fix remaining ESLint violations in `src/main/frontend/modules/SSHConfigImportWizard.ts` (if any)
- [ ] T020 [P] [US1] Fix remaining ESLint violations in `src/main/frontend/modules/tab-version-methods.ts` (if any)
- [ ] T021 [P] [US1] Fix remaining ESLint violations in `src/main/frontend/modules/theme.ts` (if any)
- [ ] T022 [P] [US1] Fix remaining ESLint violations in test files at `src/main/frontend/tests/` (if any)
- [ ] T023 [US1] Run `npm run test:run` to verify tests pass after manual fixes
- [ ] T024 [US1] Commit manual linting fixes with message: "style: fix remaining ESLint violations"

### Final Validation for User Story 1

- [ ] T025 [US1] Run `npm run lint` and verify exit code 0 (zero errors, zero warnings)
- [ ] T026 [US1] Verify linting completes in under 60 seconds (SC-001)
- [ ] T027 [US1] Run `npm run test:run` to confirm all tests passing

**Checkpoint**: User Story 1 complete - `npm run lint` passes cleanly, all tests passing

---

## Phase 3: User Story 3 - Line Ending Consistency (Priority: P3)

**Goal**: Standardize all source files to LF line endings

**Independent Test**: Run `find src/main/frontend -type f \( -name "*.ts" -o -name "*.tsx" \) -exec file {} \; | grep -v "LF"` and verify no results

**Note**: US3 moved before US2 because line ending normalization is mechanical and doesn't risk breaking type-dependent code

### Line Ending Normalization

- [ ] T028 [US3] Run `git add --renormalize .` to apply `.gitattributes` line ending rules to existing files
- [ ] T029 [US3] Review `git status` and `git diff --stat` to verify only line ending changes (no logic changes)
- [ ] T030 [US3] Run `npm run test:run` to verify tests pass after line ending normalization
- [ ] T031 [US3] Commit line ending changes with message: "chore: normalize line endings to LF"

### Validation for User Story 3

- [ ] T032 [US3] Verify all source files use LF endings: `find src/main/frontend -type f \( -name "*.ts" -o -name "*.tsx" \) -exec file {} \; | grep -v "LF"`
- [ ] T033 [US3] Verify configuration files enforced: check `.editorconfig`, `.gitattributes`, `.prettierrc.json` all specify LF
- [ ] T034 [US3] Test cross-platform behavior: open and save a file on Windows, verify LF preserved

**Checkpoint**: User Story 3 complete - all files use LF endings, configuration enforced

---

## Phase 4: User Story 2 - Type Safety Improvements (Priority: P2)

**Goal**: Eliminate all `any` types and replace with explicit TypeScript types

**Independent Test**: Run `npm run type-check` and verify exit code 0, then grep for `any` usage and verify zero instances in production code

### Type Improvements in Frontend Modules

**Strategy**: Work file-by-file, running tests after each file to catch breakage early

- [ ] T035 [P] [US2] Analyze `any` usage in `src/main/frontend/modules/dashboard.ts` and create type replacement plan
- [ ] T036 [US2] Replace `any` types with explicit types in `src/main/frontend/modules/dashboard.ts`
- [ ] T037 [US2] Run `npm run test:run -- dashboard.test.ts` to verify dashboard tests pass
- [ ] T038 [US2] Commit dashboard type improvements with message: "refactor(dashboard): replace 'any' types with explicit types"

- [ ] T039 [P] [US2] Analyze `any` usage in `src/main/frontend/modules/server-modal.ts` and create type replacement plan
- [ ] T040 [US2] Replace `any` types with explicit types in `src/main/frontend/modules/server-modal.ts`
- [ ] T041 [US2] Run `npm run test:run -- server-modal.test.ts` to verify server modal tests pass
- [ ] T042 [US2] Commit server modal type improvements with message: "refactor(server-modal): replace 'any' types with explicit types"

- [ ] T043 [P] [US2] Analyze `any` usage in `src/main/frontend/modules/ServerDetailOverlay.ts` and create type replacement plan
- [ ] T044 [US2] Replace `any` types with explicit types in `src/main/frontend/modules/ServerDetailOverlay.ts`
- [ ] T045 [US2] Run `npm run test:run -- ServerDetailOverlay.test.ts` to verify tests pass
- [ ] T046 [US2] Commit type improvements with message: "refactor(ServerDetailOverlay): replace 'any' types with explicit types"

- [ ] T047 [P] [US2] Analyze `any` usage in `src/main/frontend/modules/ServerListManager.ts` and create type replacement plan
- [ ] T048 [US2] Replace `any` types with explicit types in `src/main/frontend/modules/ServerListManager.ts`
- [ ] T049 [US2] Run `npm run test:run -- ServerListManager.test.ts` to verify tests pass (if test exists)
- [ ] T050 [US2] Commit type improvements with message: "refactor(ServerListManager): replace 'any' types with explicit types"

- [ ] T051 [P] [US2] Analyze `any` usage in `src/main/frontend/modules/SSHConfigImportWizard.ts` and create type replacement plan
- [ ] T052 [US2] Replace `any` types with explicit types in `src/main/frontend/modules/SSHConfigImportWizard.ts`
- [ ] T053 [US2] Run `npm run test:run` to verify tests pass
- [ ] T054 [US2] Commit type improvements with message: "refactor(SSHConfigImportWizard): replace 'any' types with explicit types"

- [ ] T055 [P] [US2] Analyze `any` usage in `src/main/frontend/modules/tab-version-methods.ts` and create type replacement plan
- [ ] T056 [US2] Replace `any` types with explicit types in `src/main/frontend/modules/tab-version-methods.ts`
- [ ] T057 [US2] Run `npm run test:run` to verify tests pass
- [ ] T058 [US2] Commit type improvements with message: "refactor(tab-version-methods): replace 'any' types with explicit types"

- [ ] T059 [P] [US2] Analyze `any` usage in `src/main/frontend/modules/theme.ts` and create type replacement plan
- [ ] T060 [US2] Replace `any` types with explicit types in `src/main/frontend/modules/theme.ts`
- [ ] T061 [US2] Run `npm run test:run` to verify tests pass
- [ ] T062 [US2] Commit type improvements with message: "refactor(theme): replace 'any' types with explicit types"

- [ ] T063 [P] [US2] Analyze `any` usage in `src/main/frontend/main.ts` and create type replacement plan
- [ ] T064 [US2] Replace `any` types with explicit types in `src/main/frontend/main.ts`
- [ ] T065 [US2] Run `npm run test:run` to verify tests pass
- [ ] T066 [US2] Commit type improvements with message: "refactor(main): replace 'any' types with explicit types"

### Type Improvements in Test Files

- [ ] T067 [P] [US2] Analyze `any` usage in test files at `src/main/frontend/tests/`
- [ ] T068 [US2] Replace `any` types with explicit types in test files (if any violations exist)
- [ ] T069 [US2] Run `npm run test:run` to verify all tests pass
- [ ] T070 [US2] Commit test type improvements with message: "refactor(tests): replace 'any' types with explicit types"

### Final Type Safety Validation

- [ ] T071 [US2] Run `npm run type-check` and verify exit code 0 (no type errors)
- [ ] T072 [US2] Run `grep -rn ": any" src/main/frontend --include="*.ts" --include="*.tsx" | wc -l` and verify count is 0
- [ ] T073 [US2] Run `npm run test:run` to confirm all tests passing
- [ ] T074 [US2] Document any exceptions (if any `any` types remain in `.d.ts` files) in specs/003-server-group-cleanup/type-exceptions.md

**Checkpoint**: User Story 2 complete - zero `any` types in production code, all type checks passing

---

## Phase 5: Final Validation & Polish

**Purpose**: Comprehensive validation and documentation updates

### Comprehensive Quality Checks

- [ ] T075 Run `npm run ci:check` (lint + type-check + tests + build) and verify exit code 0
- [ ] T076 Verify SC-001: Linting completes in under 60 seconds
- [ ] T077 Verify SC-002: 100% of frontend TypeScript files pass ESLint validation
- [ ] T078 Verify SC-003: Zero `any` types remain (run grep check again)
- [ ] T079 Verify SC-004: 100% of source files use LF line endings (run file check again)
- [ ] T080 Run full test suite: `npm run test:run` and verify 100% pass rate

### Documentation Updates

- [ ] T081 [P] Update `specs/003-server-group-cleanup/COMPLETION_REPORT.md` with final metrics (violations fixed, files cleaned, time spent)
- [ ] T082 [P] Update project documentation if any linting exceptions were documented
- [ ] T083 [P] Create `docs/CODE_QUALITY_STANDARDS.md` documenting new quality standards for future development (optional)

### Git Workflow

- [ ] T084 Review all commits in branch, ensure commit messages are descriptive and follow convention
- [ ] T085 Verify branch is up to date with master/main (rebase if needed)
- [ ] T086 Push branch to remote: `git push -u origin 003-server-group-cleanup`

### Pull Request Creation

- [ ] T087 Create pull request with title "chore: Frontend Code Quality Cleanup" and comprehensive description (see quickstart.md for template)
- [ ] T088 Add PR labels: `code-quality`, `technical-debt`, `no-behavioral-changes`
- [ ] T089 Request code review from team leads
- [ ] T090 Monitor CI checks on PR and address any failures

**Checkpoint**: All user stories complete, comprehensive validation passed, ready for code review

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **User Story 1 (Phase 2)**: Depends on Setup (T001-T007) - MUST establish linting baseline first
- **User Story 3 (Phase 3)**: Depends on US1 completion - line ending normalization after linting is clean
- **User Story 2 (Phase 4)**: Depends on US1 and US3 completion - type improvements done last (most complex, highest risk)
- **Final Validation (Phase 5)**: Depends on all user stories complete

### User Story Dependencies

- **User Story 1 (P1) - Linting**: Can start after Setup complete - No dependencies on other stories
- **User Story 3 (P3) - Line Endings**: Can start after US1 complete - Clean linting baseline prevents conflicts
- **User Story 2 (P2) - Type Safety**: Can start after US1 and US3 complete - Benefits from clean formatting and line endings

### Within Each User Story

- **US1**: Setup → Automated fixes → Manual fixes → Validation (sequential)
- **US2**: Multiple files can be processed in parallel (T035, T039, T043, T047, T051, T055, T059, T063 marked [P])
  - But within each file: Analyze → Replace → Test → Commit (sequential)
- **US3**: Mechanical transformation, all files at once

### Parallel Opportunities

- **Setup tasks**: T001, T002, T003 can run in parallel (different files)
- **US1 manual fixes**: T014-T022 can run in parallel IF different developers work on different files
- **US2 analysis tasks**: T035, T039, T043, T047, T051, T055, T059, T063, T067 can run in parallel (read-only analysis)
- **Final documentation**: T081, T082, T083 can run in parallel (different files)

### Critical Path

Setup (T001-T007) → US1 Automated (T008-T012) → US1 Manual (T013-T024) → US1 Validation (T025-T027) → US3 (T028-T034) → US2 (T035-T074) → Final Validation (T075-T090)

**Estimated Critical Path Time**: 12-20 hours (depends on `any` type complexity)

---

## Parallel Example: User Story 2 (Type Improvements)

```bash
# Multiple developers can work on different modules simultaneously:

Developer A:
Task T035: "Analyze `any` usage in src/main/frontend/modules/dashboard.ts"
Task T036-T038: "Replace types, test, commit dashboard.ts"

Developer B (parallel):
Task T039: "Analyze `any` usage in src/main/frontend/modules/server-modal.ts"
Task T040-T042: "Replace types, test, commit server-modal.ts"

Developer C (parallel):
Task T043: "Analyze `any` usage in src/main/frontend/modules/ServerDetailOverlay.ts"
Task T044-T046: "Replace types, test, commit ServerDetailOverlay.ts"

# All developers can analyze different files in parallel,
# then work sequentially within their assigned file
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T007)
2. Complete Phase 2: User Story 1 (T008-T027)
3. **STOP and VALIDATE**: Run `npm run lint`, verify exit code 0
4. Create intermediate PR for linting cleanup if desired (optional)

**MVP Delivered**: Clean codebase with zero linting violations

### Incremental Delivery

1. **Commit 1**: Setup & Configuration (T001-T007)
2. **Commit 2**: Automated Linting Fixes (T008-T012)
3. **Commit 3**: Manual Linting Fixes (T013-T024)
4. **Commit 4**: Line Ending Normalization (T028-T031)
5. **Commit 5+**: Type Improvements per file (T035-T074, one commit per file or per module)
6. **Final PR**: All commits together for comprehensive review

### Single Developer Strategy

Work sequentially through phases:
1. Setup (1 hour)
2. User Story 1: Linting (3-5 hours)
3. User Story 3: Line Endings (30 minutes)
4. User Story 2: Type Safety (8-12 hours, file by file)
5. Final Validation (1 hour)

**Total Time**: 14-20 hours

### Team Strategy (3+ Developers)

1. **Day 1**: All developers complete Setup together (1 hour)
2. **Day 1-2**: Developer A completes US1 (linting) while others review docs (4 hours)
3. **Day 2**: Developer A completes US3 (line endings), unblocks others (30 minutes)
4. **Day 2-3**: After US1+US3 complete:
   - Developer A: Type improvements in dashboard.ts, server-modal.ts
   - Developer B: Type improvements in ServerDetailOverlay.ts, ServerListManager.ts
   - Developer C: Type improvements in SSHConfigImportWizard.ts, theme.ts, main.ts, tests
5. **Day 3**: Final validation and PR creation (1 hour)

**Total Time**: ~3 days with team collaboration

---

## Notes

### Task Format Compliance
- ✅ All tasks follow format: `- [ ] [ID] [P?] [Story?] Description with file path`
- ✅ Story labels applied: [US1], [US2], [US3]
- ✅ Parallel markers [P] applied where tasks operate on different files with no dependencies

### Code Quality Principles
- **No behavioral changes**: All changes are formatting/typing only, no logic modifications
- **Test-driven validation**: Run tests after every significant change
- **Incremental commits**: One logical change per commit for easier review
- **Reviewable changes**: Separate mechanical fixes from semantic changes

### Common Type Replacement Patterns
Reference `research.md` Section 4 for detailed strategies:
- Event handlers: `any` → `MouseEvent`, `KeyboardEvent`, `Event`
- DOM elements: `any` → `HTMLElement`, `HTMLButtonElement`, etc.
- API responses: `any` → Define explicit interfaces
- Dynamic objects: `any` → `Record<string, unknown>`
- Unknown data: `any` → `unknown` with type guards
- Callbacks: `any` → Explicit function signatures

### Risk Mitigation
- **Run tests frequently**: After each file in US2, after each phase
- **Commit frequently**: Enable easy rollback if issues arise
- **Review diffs carefully**: Ensure no logic changes in automated fixes
- **Coordinate with team**: Avoid merge conflicts during cleanup work

### Success Metrics (from spec.md)
- ✅ SC-001: Linting in under 60 seconds (Task T026, T076)
- ✅ SC-002: 100% TypeScript files pass (Task T025, T077)
- ✅ SC-003: Zero `any` types (Task T072, T078)
- ✅ SC-004: 100% LF line endings (Task T032, T079)
- ⏱️ SC-005: 50% reduction in formatting comments (measured post-merge)
- ⏱️ SC-006: Zero new violations in future PRs (enforced via CI after merge)

---

**Total Task Count**: 90 tasks
**Tasks per User Story**:
- Setup (Phase 1): 7 tasks
- User Story 1 - Linting (P1): 20 tasks (T008-T027)
- User Story 3 - Line Endings (P3): 7 tasks (T028-T034)
- User Story 2 - Type Safety (P2): 40 tasks (T035-T074)
- Final Validation: 16 tasks (T075-T090)

**Parallel Opportunities**: 15 tasks marked [P] can run in parallel with other tasks
**Independent Test Criteria**: Each user story has clear validation checkpoints
**Suggested MVP**: User Story 1 only (T001-T027) = 27 tasks = 4-6 hours

Format validation: ✅ ALL tasks follow required checklist format with ID, optional [P], optional [Story], description with file paths.
