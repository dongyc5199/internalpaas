# Implementation Tasks: Frontend React Migration

**Branch**: `007-frontend-react-migration` | **Generated**: 2025-01-04

## Task Format

Each task follows this format:
```
- [ ] [TaskID] [Priority] [UserStory] Description with affected file paths
```

**Priority Levels**: P0 (blocking), P1 (high), P2 (medium), P3 (low)
**User Stories**: US1-US6 from spec.md, or INFRA for cross-cutting infrastructure

## Dependencies & Parallel Execution

Tasks can be executed in parallel unless marked with `[BLOCKS: TaskID]`. See [Dependency Graph](#dependency-graph) at end.

---

## Phase 1: Setup & Build Infrastructure (Week 1)

**Goal**: Establish build system, development environment, and project scaffolding

### Build System Setup

- [ ] [T001] [P0] [INFRA] Initialize Vite project in `src/main/frontend` with TypeScript template
  - Files: `src/main/frontend/package.json`, `vite.config.ts`, `tsconfig.json`
  - Install dependencies: react@18, react-dom@18, typescript@5, vite@5
  - Configure Vite dev server on port 5173 with proxy to :8080

- [ ] [T002] [P0] [INFRA] Configure TypeScript with strict mode and path aliases
  - Files: `src/main/frontend/tsconfig.json`, `tsconfig.node.json`
  - Enable `strict: true`, `noImplicitAny`, `strictNullChecks`
  - Set path aliases: `@/*` → `./src/*`, `@shared/*` → `./src/shared/*`

- [ ] [T003] [P0] [INFRA] Integrate Vite build with Maven lifecycle
  - Files: `pom.xml`, `.mvn/maven.config`
  - Add frontend-maven-plugin to execute `npm run build` during package phase
  - Configure output directory: `src/main/resources/static/dist`

- [ ] [T004] [P0] [INFRA] Configure ESLint and Prettier for code quality
  - Files: `src/main/frontend/.eslintrc.js`, `.prettierrc`, `.editorconfig`
  - Install @typescript-eslint/parser, eslint-plugin-react-hooks
  - Set rules: no-explicit-any (error), explicit-function-return-type (warn)

- [ ] [T005] [P0] [INFRA] Setup Vitest testing framework
  - Files: `src/main/frontend/vitest.config.ts`, `src/main/frontend/tests/setup.ts`
  - Install vitest, @testing-library/react, @testing-library/jest-dom
  - Configure jsdom environment and global test utilities

- [ ] [T006] [P1] [INFRA] Create environment configuration system
  - Files: `src/main/frontend/.env.development`, `.env.production`, `src/config/env.ts`
  - Define VITE_API_BASE_URL, VITE_WS_BASE_URL variables
  - Implement type-safe env variable access with validation

### Project Structure

- [ ] [T007] [P0] [INFRA] Create feature-based folder structure
  - Directories: `src/features/{admin,monitoring,terminal,profile,applications,config}`
  - Subdirs per feature: `components/`, `hooks/`, `api/`, `types/`, `stores/`
  - Create index.ts barrel exports for each feature

- [ ] [T008] [P0] [INFRA] Create shared infrastructure folders
  - Directories: `src/shared/{components,hooks,stores,utils,api}`
  - Files: `src/shared/types/index.ts`, `src/shared/constants.ts`
  - Setup central type definitions and utility functions

- [ ] [T009] [P1] [INFRA] Setup i18n infrastructure with react-i18next
  - Files: `src/i18n/config.ts`, `src/i18n/locales/en.json`, `zh.json`
  - Install react-i18next, i18next, i18next-browser-languagedetector
  - Migrate existing translations from backend to frontend JSON files

---

## Phase 2: Foundation - Core Infrastructure (Weeks 2-4)

**Goal**: Build application shell, authentication, and component library (enables US5)

### Application Shell (US5)

- [ ] [T010] [P0] [US5] Create main App component with router setup
  - Files: `src/main/frontend/src/App.tsx`, `src/main.tsx`
  - Install react-router-dom@6
  - Configure BrowserRouter with basename support for Spring Boot context path

- [ ] [T011] [P0] [US5] Implement MainLayout component replacing main-layout.html [BLOCKS: T012-T016]
  - Files: `src/layouts/MainLayout.tsx`, `MainLayout.module.css`
  - Migrate sidebar navigation from Thymeleaf template
  - Inherit --shell-* CSS variables from main application
  - Add responsive sidebar collapse/expand functionality

- [ ] [T012] [P0] [US5] Create Navigation component with role-based menu rendering
  - Files: `src/layouts/components/Navigation.tsx`, `Navigation.module.css`
  - Implement two-level menu structure (primary + secondary nav)
  - Filter menu items based on user role (admin vs developer)
  - Add active route highlighting with React Router NavLink

- [ ] [T013] [P0] [US5] Build Header component with user menu and theme toggle
  - Files: `src/layouts/components/Header.tsx`, `Header.module.css`
  - Migrate user dropdown from Thymeleaf (logout, profile link)
  - Add theme toggle switch (light/dark mode)
  - Display current user display name and avatar

- [ ] [T014] [P1] [US5] Create EmptyLayout for unauthenticated pages (login, register)
  - Files: `src/layouts/EmptyLayout.tsx`
  - Simple centered container layout without sidebar/header
  - Apply consistent branding and styling

- [ ] [T015] [P1] [US5] Implement AdminLayout with additional admin-specific UI elements
  - Files: `src/layouts/AdminLayout.tsx`
  - Extend MainLayout with admin tools sidebar section
  - Add breadcrumb navigation for nested admin pages

- [ ] [T016] [P1] [US5] Setup route configuration with nested routes
  - Files: `src/routes/index.tsx`, `src/routes/admin.tsx`, `src/routes/monitoring.tsx`
  - Define route hierarchy matching URL structure
  - Configure lazy loading for route components

### Authentication System (US5)

- [ ] [T017] [P0] [US5] Create authentication store with Zustand [BLOCKS: T018-T021]
  - Files: `src/shared/stores/authStore.ts`, `src/shared/types/user.ts`
  - Define AuthState interface (user, isAuthenticated, isLoading)
  - Implement persist middleware to save auth state to localStorage
  - Add actions: login, logout, refreshSession, updatePreferences

- [ ] [T018] [P0] [US5] Implement authentication API client
  - Files: `src/shared/api/authApi.ts`, `src/shared/api/client.ts`
  - Create Axios instance with interceptors for JSESSIONID cookie handling
  - Add methods: login(credentials), logout(), getCurrentUser(), refreshSession()
  - Configure baseURL from environment variables

- [ ] [T019] [P0] [US5] Build ProtectedRoute wrapper component
  - Files: `src/layouts/ProtectedRoute.tsx`
  - Check authentication state from authStore
  - Redirect to /login with returnUrl parameter if unauthenticated
  - Support role-based access control (adminOnly, roles prop)

- [ ] [T020] [P0] [US5] Create Login page component
  - Files: `src/features/auth/components/LoginPage.tsx`, `LoginForm.tsx`
  - Replace login.html Thymeleaf template
  - Use React Hook Form + Zod validation
  - Handle login errors with user-friendly messages

- [ ] [T021] [P1] [US5] Implement session synchronization across browser tabs
  - Files: `src/shared/hooks/useAuthSync.ts`
  - Use BroadcastChannel API or localStorage events
  - Sync login/logout actions across all tabs
  - Handle concurrent session invalidation

### State Management & Data Fetching

- [ ] [T022] [P0] [INFRA] Setup TanStack Query client [BLOCKS: T023-T025]
  - Files: `src/shared/api/queryClient.ts`
  - Install @tanstack/react-query@5, @tanstack/react-query-devtools
  - Configure QueryClient with default options (staleTime, cacheTime, retry logic)
  - Wrap App with QueryClientProvider

- [ ] [T023] [P0] [INFRA] Create base API client with interceptors
  - Files: `src/shared/api/client.ts`, `src/shared/api/interceptors.ts`
  - Configure Axios with baseURL, timeout, withCredentials for cookies
  - Add request interceptor for CSRF token headers
  - Add response interceptor for 401 handling (redirect to login)

- [ ] [T024] [P1] [INFRA] Implement global error boundary
  - Files: `src/shared/components/ErrorBoundary.tsx`
  - Catch React errors at route level
  - Display user-friendly error page with reload option
  - Log errors to console in development, send to backend in production

- [ ] [T025] [P1] [INFRA] Create UI state store for global UI concerns
  - Files: `src/shared/stores/uiStore.ts`
  - Manage theme (light/dark), sidebar collapsed state
  - Handle modal state (activeModal, modalProps)
  - Manage toast notifications queue

### Component Library

- [ ] [T026] [P0] [INFRA] Build Button component with variants [BLOCKS: All form components]
  - Files: `src/shared/components/Button/Button.tsx`, `Button.module.css`
  - Support variants: primary, secondary, danger, ghost
  - Add sizes: sm, md, lg
  - Include loading state with spinner

- [ ] [T027] [P0] [INFRA] Create Input component with validation display
  - Files: `src/shared/components/Input/Input.tsx`, `Input.module.css`
  - Support types: text, password, email, number
  - Display validation errors from React Hook Form
  - Add icons support (prefix, suffix)

- [ ] [T028] [P0] [INFRA] Implement Modal component with accessibility
  - Files: `src/shared/components/Modal/Modal.tsx`, `Modal.module.css`
  - Use React Portal for rendering outside root
  - Add ARIA attributes (role="dialog", aria-labelledby)
  - Support backdrop click to close, ESC key handler

- [ ] [T029] [P0] [INFRA] Build Table component with sorting and pagination
  - Files: `src/shared/components/Table/Table.tsx`, `Table.module.css`
  - Generic typed component for any data type
  - Built-in column sorting (client-side)
  - Pagination controls (page size, current page)

- [ ] [T030] [P1] [INFRA] Create Card component for consistent containers
  - Files: `src/shared/components/Card/Card.tsx`, `Card.module.css`
  - Support header, body, footer sections
  - Add hover effects and shadow variants

- [ ] [T031] [P1] [INFRA] Implement Select/Dropdown component
  - Files: `src/shared/components/Select/Select.tsx`
  - Single and multi-select support
  - Search/filter functionality for long lists
  - Keyboard navigation

- [ ] [T032] [P1] [INFRA] Build Loading/Spinner components
  - Files: `src/shared/components/Loading/Spinner.tsx`, `LoadingOverlay.tsx`
  - Multiple spinner variants (dots, circle, bars)
  - Full-page loading overlay with backdrop

- [ ] [T033] [P2] [INFRA] Create Toast notification system
  - Files: `src/shared/components/Toast/Toast.tsx`, `ToastContainer.tsx`
  - Auto-dismiss with configurable duration
  - Position variants (top-right, bottom-center, etc.)
  - Support action buttons in toast

---

## Phase 3: Admin Dashboard Migration (Weeks 5-8) - US1

**Goal**: Migrate server and user management to React with real-time updates

### Server Management (US1)

- [ ] [T034] [P1] [US1] Define Server entity types and Zod schemas [BLOCKS: T035-T041]
  - Files: `src/features/admin/servers/types/server.ts`, `schemas/serverSchema.ts`
  - Create Server, ServerStatus, ServerMetrics interfaces from data-model.md
  - Define Zod validation schemas for server forms
  - Create ServerFormData, ServerFilters types

- [ ] [T035] [P1] [US1] Implement server API client
  - Files: `src/features/admin/servers/api/serverApi.ts`
  - Methods: getAll(filters), getById(id), create(data), update(id, data), delete(id)
  - Add checkConnection(id), refreshStatus(id) endpoints
  - Type all responses with Server interface

- [ ] [T036] [P1] [US1] Create React Query hooks for server data [BLOCKS: T037-T040]
  - Files: `src/features/admin/servers/hooks/useServers.ts`, `useServerMutations.ts`
  - useServers(filters) - list query with caching
  - useServer(id) - single server query
  - useCreateServer(), useUpdateServer(), useDeleteServer() - mutations
  - Configure query keys for cache invalidation

- [ ] [T037] [P1] [US1] Build ServerListPage component replacing admin/servers.html
  - Files: `src/features/admin/servers/components/ServerListPage.tsx`
  - Display servers in Table component with columns: name, host, status, actions
  - Add filters for status, tags, search by name
  - Integrate useServers hook for data fetching

- [ ] [T038] [P1] [US1] Create ServerCard component for grid view option
  - Files: `src/features/admin/servers/components/ServerCard.tsx`, `ServerCard.module.css`
  - Display server details in card layout with status badge
  - Show key metrics (CPU, memory) if available
  - Add quick actions menu (edit, delete, open terminal)

- [ ] [T039] [P1] [US1] Implement ServerForm component for create/edit
  - Files: `src/features/admin/servers/components/ServerForm.tsx`
  - Use React Hook Form with zodResolver(serverSchema)
  - Fields: name, host, port, username, password, tags, groupId
  - Real-time validation with error display
  - Support both create and edit modes (pre-populate for edit)

- [ ] [T040] [P1] [US1] Build ServerDetailPage component replacing admin/server-detail.html
  - Files: `src/features/admin/servers/components/ServerDetailPage.tsx`
  - Display comprehensive server information with tabs (overview, metrics, logs)
  - Show applications deployed on server
  - Add connection test button with loading state

- [ ] [T041] [P1] [US1] Implement batch operations for servers
  - Files: `src/features/admin/servers/components/BatchActionsBar.tsx`
  - Multi-select checkbox in table rows
  - Batch actions: delete, tag, export to CSV
  - Confirmation modal before destructive actions

### WebSocket Integration for Real-Time Status (US1)

- [ ] [T042] [P1] [US1] Create WebSocket client wrapper using STOMP.js [BLOCKS: T043-T044]
  - Files: `src/shared/api/websocket.ts`
  - Install @stomp/stompjs
  - Initialize STOMP client with reconnection logic (exponential backoff)
  - Expose subscribe(topic, handler), unsubscribe(topic), sendMessage(destination, body) methods

- [ ] [T043] [P1] [US1] Build useWebSocket custom hook
  - Files: `src/shared/hooks/useWebSocket.ts`
  - Generic hook accepting topic and message handler
  - Manage subscription lifecycle (subscribe on mount, unsubscribe on unmount)
  - Return connection status (connecting, connected, disconnected) and latest message

- [ ] [T044] [P1] [US1] Implement useServerStatus hook for real-time status updates
  - Files: `src/features/admin/servers/hooks/useServerStatus.ts`
  - Subscribe to `/topic/server-status/{serverId}` WebSocket topic
  - Update React Query cache when status messages arrive
  - Parse ServerStatusUpdate message format from websocket-specification.md

- [ ] [T045] [P2] [US1] Add connection health indicator component
  - Files: `src/shared/components/WebSocketStatus.tsx`
  - Display connection status with icon (green dot = connected, yellow = reconnecting, red = disconnected)
  - Show tooltip with last connected timestamp
  - Include in Header component

### User Management (US1)

- [ ] [T046] [P1] [US1] Define User entity types and schemas
  - Files: `src/features/admin/users/types/user.ts`, `schemas/userSchema.ts`
  - Create User, UserRole, UserPreferences interfaces
  - Zod schemas for user registration, update, password change

- [ ] [T047] [P1] [US1] Implement user API client
  - Files: `src/features/admin/users/api/userApi.ts`
  - Methods: getAll(), getById(id), create(data), update(id, data), delete(id)
  - toggleAdmin(id), toggleSuperAdmin(id) role change methods

- [ ] [T048] [P1] [US1] Create useUsers React Query hooks
  - Files: `src/features/admin/users/hooks/useUsers.ts`
  - useUsers() - list all users
  - useUser(id) - single user detail
  - useCreateUser(), useUpdateUser(), useDeleteUser() mutations

- [ ] [T049] [P1] [US1] Build UserListPage component replacing admin/users.html
  - Files: `src/features/admin/users/components/UserListPage.tsx`
  - Display users in table with columns: username, email, role, created date, actions
  - Filter by role (all, admin, developer)
  - Search by username or email

- [ ] [T050] [P1] [US1] Create UserForm component for create/edit
  - Files: `src/features/admin/users/components/UserForm.tsx`
  - Use React Hook Form with zodResolver(userSchema)
  - Fields: username, email, password, role selector
  - Password confirmation field with match validation
  - Hide password field in edit mode (separate change password flow)

- [ ] [T051] [P2] [US1] Implement role badge component
  - Files: `src/features/admin/users/components/RoleBadge.tsx`
  - Color-coded badges: SUPER_ADMIN (red), ADMIN (orange), DEVELOPER (blue)
  - Tooltip with role permissions description

### Testing for US1

- [ ] [T052] [P2] [US1] Write unit tests for server components
  - Files: `src/features/admin/servers/__tests__/ServerListPage.test.tsx`, etc.
  - Test ServerListPage rendering with mock data
  - Test ServerForm validation errors
  - Test useServers hook with MSW mocked API

- [ ] [T053] [P2] [US1] Write integration tests for server CRUD flow
  - Files: `src/features/admin/servers/__tests__/integration/serverCrud.test.tsx`
  - Test complete create → list → edit → delete flow
  - Mock API responses with MSW
  - Verify React Query cache updates

- [ ] [T054] [P2] [US1] Write tests for WebSocket integration
  - Files: `src/shared/hooks/__tests__/useWebSocket.test.ts`
  - Mock STOMP client
  - Test subscription lifecycle
  - Test reconnection on disconnect

---

## Phase 4: SSH Terminal React Component (Weeks 9-12) - US2

**Goal**: Migrate SSH terminal to React with xterm.js and stable WebSocket connection

### Terminal Infrastructure (US2)

- [ ] [T055] [P1] [US2] Define SSH session types
  - Files: `src/features/terminal/types/session.ts`
  - Create SSHSession, SessionStatus, TerminalMessage interfaces
  - Define message types: input, output, resize, error, disconnect

- [ ] [T056] [P1] [US2] Implement SSH session API client [BLOCKS: T057-T060]
  - Files: `src/features/terminal/api/sshApi.ts`
  - Methods: createSession(serverId), getSessions(), getSession(id), closeSession(id)
  - testConnection(serverId) for pre-connection validation

- [ ] [T057] [P1] [US2] Create useSSHSessions React Query hook
  - Files: `src/features/terminal/hooks/useSSHSessions.ts`
  - useActiveSessions() - list active sessions
  - useCreateSession(serverId) mutation
  - useCloseSession(id) mutation

- [ ] [T058] [P1] [US2] Build useSSHWebSocket hook for terminal I/O [BLOCKS: T059-T060]
  - Files: `src/features/terminal/hooks/useSSHWebSocket.ts`
  - Subscribe to `/topic/ssh/{sessionId}/output` for receiving terminal output
  - Publish to `/app/ssh/{sessionId}/input` for sending commands
  - Handle resize messages for terminal window changes
  - Buffer messages during reconnection

### Terminal UI Components (US2)

- [ ] [T059] [P1] [US2] Integrate xterm.js in Terminal component
  - Files: `src/features/terminal/components/Terminal.tsx`, `Terminal.module.css`
  - Install xterm@5, xterm-addon-fit, xterm-addon-web-links
  - Initialize XTerm instance with proper theme colors
  - Connect xterm onData event to sendMessage via useSSHWebSocket
  - Write incoming messages to terminal with terminal.write()

- [ ] [T060] [P1] [US2] Build TerminalPage component replacing terminal/index.html
  - Files: `src/features/terminal/components/TerminalPage.tsx`
  - Display server selector dropdown
  - "Connect" button to create SSH session
  - Render Terminal component when session is active
  - Show connection status (connecting, connected, disconnected)

- [ ] [T061] [P1] [US2] Create TerminalControls component
  - Files: `src/features/terminal/components/TerminalControls.tsx`
  - Buttons: clear terminal, reconnect, close session
  - Display session info (server name, uptime)
  - Add copy/paste buttons for mobile support

- [ ] [T062] [P1] [US2] Implement multi-terminal tabs (TerminalManager)
  - Files: `src/features/terminal/components/TerminalManager.tsx`
  - Tab bar for switching between multiple SSH sessions
  - "New Terminal" button to open additional session
  - Close tab button with confirmation if session is active
  - Persist tab state in localStorage

- [ ] [T063] [P2] [US2] Add terminal customization settings
  - Files: `src/features/terminal/components/TerminalSettings.tsx`
  - Font size adjustment (12px - 20px)
  - Theme selection (dark, light, custom)
  - Cursor style (block, underline, bar)
  - Save settings to user preferences

### Error Handling & Reconnection (US2)

- [ ] [T064] [P1] [US2] Implement connection error handling
  - Files: `src/features/terminal/hooks/useSSHWebSocket.ts` (enhancement)
  - Detect WebSocket disconnection events
  - Display "Connection Lost" overlay on terminal
  - Automatic reconnection with exponential backoff (5s, 10s, 20s, 40s)
  - Manual reconnect button after max retries

- [ ] [T065] [P2] [US2] Add session timeout handling
  - Files: `src/features/terminal/hooks/useSessionTimeout.ts`
  - Detect idle sessions (no input/output for 30 minutes)
  - Show "Session Idle" warning after 25 minutes
  - Auto-close session after timeout with notification

### Testing for US2

- [ ] [T066] [P2] [US2] Write unit tests for terminal hooks
  - Files: `src/features/terminal/hooks/__tests__/useSSHWebSocket.test.ts`
  - Mock STOMP WebSocket client
  - Test message publishing and subscription
  - Test reconnection logic

- [ ] [T067] [P2] [US2] Write integration tests for terminal flow
  - Files: `src/features/terminal/__tests__/integration/terminalFlow.test.tsx`
  - Test session creation → connection → command execution → output display
  - Mock SSH API and WebSocket messages
  - Verify xterm.write called with correct output

---

## Phase 5: Monitoring Dashboard with Charts (Weeks 13-15) - US3

**Goal**: Migrate monitoring dashboards with interactive charts using Apache ECharts

### Data Visualization Infrastructure (US3)

- [ ] [T068] [P1] [US3] Define monitoring types and schemas
  - Files: `src/features/monitoring/types/metrics.ts`
  - Create ServerMetrics, MetricDataPoint, TimeRange, MetricType interfaces
  - Define AlertThreshold, ServerMetricHistory types

- [ ] [T069] [P1] [US3] Implement monitoring API client [BLOCKS: T070-T074]
  - Files: `src/features/monitoring/api/monitoringApi.ts`
  - Methods: getServerMetrics(serverId, timeRange), getMetricHistory(serverId, metricType, timeRange)
  - getAlertThresholds(serverId), getServerRanking(), exportMetrics(serverId, format)

- [ ] [T070] [P1] [US3] Create React Query hooks for monitoring data
  - Files: `src/features/monitoring/hooks/useMonitoring.ts`
  - useServerMetrics(serverId) - real-time metrics
  - useMetricHistory(serverId, metricType, timeRange) - historical data
  - Configure aggressive caching (staleTime: 30s) for metric queries

- [ ] [T071] [P1] [US3] Build useRealtimeMetrics hook with WebSocket
  - Files: `src/features/monitoring/hooks/useRealtimeMetrics.ts`
  - Subscribe to `/topic/server-status/{serverId}` WebSocket topic
  - Update React Query cache with incoming metric updates
  - Merge real-time data with historical chart data

### Chart Components (US3)

- [ ] [T072] [P1] [US3] Integrate Apache ECharts library [BLOCKS: T073-T076]
  - Files: `src/shared/components/Chart/EChartsWrapper.tsx`
  - Install echarts@5, echarts-for-react
  - Create base chart component with responsive sizing
  - Configure dark/light theme switching based on app theme

- [ ] [T073] [P1] [US3] Build MetricsChart component for time-series data
  - Files: `src/features/monitoring/components/MetricsChart.tsx`
  - Line chart for CPU/memory/disk usage over time
  - Support multiple series (compare multiple servers)
  - Zoom and pan controls using ECharts dataZoom
  - Tooltip with formatted timestamps and values

- [ ] [T074] [P1] [US3] Create TimeRangePicker component
  - Files: `src/features/monitoring/components/TimeRangePicker.tsx`
  - Preset ranges: Last hour, 6 hours, 24 hours, 7 days, 30 days
  - Custom range selector with date pickers
  - Update chart data when range changes

- [ ] [T075] [P2] [US3] Implement chart export functionality
  - Files: `src/features/monitoring/components/ChartExportButton.tsx`
  - Export chart as PNG image using ECharts.getDataURL()
  - Export data as CSV with headers and formatted values
  - Download file with descriptive filename (server-cpu-2025-01-04.csv)

- [ ] [T076] [P2] [US3] Build comparison chart for multiple servers
  - Files: `src/features/monitoring/components/ComparisonChart.tsx`
  - Multi-select server picker
  - Display metrics from multiple servers in one chart with different colors
  - Legend to show/hide individual server lines

### Dashboard Pages (US3)

- [ ] [T077] [P1] [US3] Create MonitoringDashboard page replacing monitoring/history-dashboard.html
  - Files: `src/features/monitoring/components/MonitoringDashboard.tsx`
  - Grid layout with multiple MetricsChart components (CPU, memory, disk, network)
  - Real-time update indicator showing data freshness
  - Auto-refresh toggle (on by default, 5-second interval)

- [ ] [T078] [P1] [US3] Build ServerDetailsMonitoring page replacing monitoring/server-details.html
  - Files: `src/features/monitoring/components/ServerDetailsMonitoring.tsx`
  - Detailed metrics for single server with multiple time ranges
  - Show current metrics summary cards at top (latest CPU%, memory%, etc.)
  - Historical charts below for trend analysis

- [ ] [T079] [P2] [US3] Implement ThresholdDashboard replacing monitoring/threshold-dashboard.html
  - Files: `src/features/monitoring/components/ThresholdDashboard.tsx`
  - Display alert thresholds for all servers
  - Edit threshold modal with form validation
  - Visual indicators for metrics exceeding thresholds (red/yellow alerts)

### Testing for US3

- [ ] [T080] [P2] [US3] Write unit tests for chart components
  - Files: `src/features/monitoring/components/__tests__/MetricsChart.test.tsx`
  - Test chart rendering with mock data
  - Test zoom/pan interactions
  - Test theme switching

- [ ] [T081] [P2] [US3] Write tests for real-time metric updates
  - Files: `src/features/monitoring/hooks/__tests__/useRealtimeMetrics.test.ts`
  - Mock WebSocket messages with metric updates
  - Verify React Query cache updates correctly
  - Test chart re-renders with new data

---

## Phase 6: User Profile & Settings SPA (Weeks 16-17) - US4

**Goal**: Migrate user profile and preferences pages with optimistic updates

### User Profile (US4)

- [ ] [T082] [P2] [US4] Define user profile types
  - Files: `src/features/profile/types/profile.ts`
  - UserProfile, UserPreferences, PasswordChangeRequest interfaces
  - Zod schemas for profile update, password change validation

- [ ] [T083] [P2] [US4] Implement profile API client
  - Files: `src/features/profile/api/profileApi.ts`
  - Methods: getProfile(), updateProfile(data), changePassword(data), updatePreferences(prefs)

- [ ] [T084] [P2] [US4] Create useProfile React Query hooks [BLOCKS: T085-T087]
  - Files: `src/features/profile/hooks/useProfile.ts`
  - useProfile() - fetch current user profile
  - useUpdateProfile() mutation with optimistic update
  - useChangePassword() mutation

### Profile UI Components (US4)

- [ ] [T085] [P2] [US4] Build ProfilePage component replacing user-profile.html
  - Files: `src/features/profile/components/ProfilePage.tsx`
  - Tab interface: Profile Info, Preferences, Security
  - Display user avatar, display name, email, role

- [ ] [T086] [P2] [US4] Create ProfileEditForm component
  - Files: `src/features/profile/components/ProfileEditForm.tsx`
  - Use React Hook Form with zodResolver
  - Fields: displayName, email, avatar upload
  - Real-time validation (email format, display name length)
  - Optimistic update: show changes immediately, revert on API error

- [ ] [T087] [P2] [US4] Build PasswordChangeForm component
  - Files: `src/features/profile/components/PasswordChangeForm.tsx`
  - Fields: current password, new password, confirm new password
  - Validation: password strength indicator, confirmation match
  - Success notification with auto-logout option

- [ ] [T088] [P2] [US4] Implement PreferencesPanel component
  - Files: `src/features/profile/components/PreferencesPanel.tsx`
  - Theme toggle (light, dark, auto) with immediate preview
  - Language selector (English, Chinese) with i18n integration
  - Sidebar preference (collapsed by default)
  - Save button with optimistic update

### Theme & i18n Integration (US4)

- [ ] [T089] [P2] [US4] Create useTheme hook for theme management
  - Files: `src/shared/hooks/useTheme.ts`
  - Read theme preference from uiStore
  - Apply theme by toggling data-theme attribute on <html>
  - Detect system theme preference (prefers-color-scheme media query)
  - Auto-switch theme when system preference changes (if theme = "auto")

- [ ] [T090] [P2] [US4] Build LanguageSwitcher component
  - Files: `src/shared/components/LanguageSwitcher.tsx`
  - Dropdown with language options (EN, ZH)
  - Use i18next.changeLanguage() on selection
  - Persist selection to user preferences API
  - Re-render all components with new translations

### Testing for US4

- [ ] [T091] [P2] [US4] Write unit tests for profile forms
  - Files: `src/features/profile/components/__tests__/ProfileEditForm.test.tsx`
  - Test form validation errors
  - Test optimistic update and revert on error
  - Test successful save flow

- [ ] [T092] [P2] [US4] Write tests for theme switching
  - Files: `src/shared/hooks/__tests__/useTheme.test.ts`
  - Test theme toggle updates <html> data-theme
  - Test auto theme follows system preference
  - Test theme persistence

---

## Phase 7: Configuration & Application Management (Weeks 18-19) - US6

**Goal**: Migrate application lifecycle and config editor with improved UX

### Application Management (US6)

- [ ] [T093] [P2] [US6] Define application types and schemas
  - Files: `src/features/applications/types/application.ts`
  - Application, ApplicationStatus, ApplicationConfig interfaces
  - Zod schemas for application upload, config update

- [ ] [T094] [P2] [US6] Implement application API client [BLOCKS: T095-T100]
  - Files: `src/features/applications/api/applicationApi.ts`
  - Methods: getAll(), getById(id), upload(file, serverId), delete(id)
  - Lifecycle: start(id), stop(id), restart(id)
  - Config: getActiveConfig(id), createConfig(id, content), applyConfig(id, configId)

- [ ] [T095] [P2] [US6] Create useApplications React Query hooks
  - Files: `src/features/applications/hooks/useApplications.ts`
  - useApplications(filters) - list applications with server filter
  - useApplication(id) - single app detail
  - useUploadApplication(), useStartApp(), useStopApp(), useRestartApp() mutations

### Application UI Components (US6)

- [ ] [T096] [P2] [US6] Build ApplicationsPage replacing applications.html
  - Files: `src/features/applications/components/ApplicationsPage.tsx`
  - Table view with columns: name, server, status, port, actions
  - Filter by status (all, running, stopped, error)
  - Search by application name

- [ ] [T097] [P2] [US6] Create ApplicationUploadModal with drag-and-drop
  - Files: `src/features/applications/components/ApplicationUploadModal.tsx`
  - Drag-and-drop zone for JAR files (react-dropzone library)
  - File validation (extension, size limit 100MB)
  - Upload progress bar with percentage and cancel button
  - Server selector dropdown

- [ ] [T098] [P2] [US6] Build ApplicationDetailPage replacing application-detail.html
  - Files: `src/features/applications/components/ApplicationDetailPage.tsx`
  - Application info card (name, status, port, debug port, PID)
  - Lifecycle control buttons (start, stop, restart) with loading states
  - Logs viewer with WebSocket streaming
  - Configuration tab

- [ ] [T099] [P2] [US6] Implement ApplicationStatusBadge component
  - Files: `src/features/applications/components/ApplicationStatusBadge.tsx`
  - Color-coded status: RUNNING (green), STOPPED (gray), ERROR (red), STARTING/STOPPING (yellow)
  - Animated pulse effect for transient states
  - Tooltip with status message if error

- [ ] [T100] [P2] [US6] Create BatchActionsBar for applications
  - Files: `src/features/applications/components/BatchActionsBar.tsx`
  - Multi-select checkboxes in table
  - Batch actions: stop, start, delete with confirmation
  - Progress indicator for batch operations

### Configuration Editor (US6)

- [ ] [T101] [P2] [US6] Integrate Monaco Editor for config editing
  - Files: `src/features/applications/components/ConfigEditor.tsx`
  - Install @monaco-editor/react
  - Syntax highlighting for .properties and .yaml/.yml files
  - Find/replace functionality
  - Validation for YAML syntax (parse errors)

- [ ] [T102] [P2] [US6] Build ConfigHistoryModal component
  - Files: `src/features/applications/components/ConfigHistoryModal.tsx`
  - Display list of previous config versions with timestamps
  - Diff viewer comparing two versions (monaco-editor diff mode)
  - Rollback button to apply previous version

- [ ] [T103] [P3] [US6] Create ConfigTemplateSelector
  - Files: `src/features/applications/components/ConfigTemplateSelector.tsx`
  - Dropdown with common config templates (Spring Boot, custom)
  - Apply template button to populate editor
  - Save custom template option

### Testing for US6

- [ ] [T104] [P2] [US6] Write unit tests for application components
  - Files: `src/features/applications/components/__tests__/ApplicationUploadModal.test.tsx`
  - Test drag-and-drop file handling
  - Test file validation errors
  - Test upload progress updates

- [ ] [T105] [P2] [US6] Write integration tests for lifecycle operations
  - Files: `src/features/applications/__tests__/integration/lifecycle.test.tsx`
  - Test start → running → stop → stopped flow
  - Mock API responses with MSW
  - Verify status badge updates

---

## Phase 8: Polish & Cross-Cutting Concerns (Week 20)

**Goal**: Finalize migration, optimize performance, and clean up legacy code

### Performance Optimization

- [ ] [T106] [P1] [INFRA] Implement code splitting for all route components
  - Files: `src/routes/index.tsx`, `src/routes/admin.tsx`, etc.
  - Use React.lazy() for all page components
  - Add Suspense with loading fallback
  - Verify separate chunks generated in build output

- [ ] [T107] [P1] [INFRA] Optimize bundle size with tree shaking
  - Files: `vite.config.ts`, `package.json`
  - Configure Vite rollupOptions for manual chunks (vendor, shared)
  - Analyze bundle with vite-plugin-visualizer
  - Ensure initial bundle < 500KB gzipped

- [ ] [T108] [P2] [INFRA] Add virtualization for long lists
  - Files: `src/shared/components/VirtualTable.tsx`
  - Install react-window or @tanstack/react-virtual
  - Replace Table component with VirtualTable for lists > 100 items
  - Test with 1000+ servers/applications

- [ ] [T109] [P2] [INFRA] Optimize chart rendering performance
  - Files: `src/features/monitoring/components/MetricsChart.tsx` (enhancement)
  - Use ECharts lazy update mode for real-time data
  - Debounce chart resize events
  - Limit data points rendered (aggregate if > 1000 points)

### Accessibility

- [ ] [T110] [P1] [INFRA] Run axe DevTools accessibility audit on all pages
  - Fix color contrast issues (WCAG AA minimum)
  - Add missing ARIA labels and roles
  - Ensure keyboard navigation works throughout app
  - Document: `docs/accessibility-audit-report.md`

- [ ] [T111] [P2] [INFRA] Add focus management for modals and page transitions
  - Files: `src/shared/components/Modal/Modal.tsx` (enhancement)
  - Focus trap within modals (focus-trap-react library)
  - Restore focus to trigger element on modal close
  - Announce page navigation to screen readers

- [ ] [T112] [P2] [INFRA] Implement skip navigation links
  - Files: `src/layouts/MainLayout.tsx` (enhancement)
  - "Skip to main content" link at top of page (visible on focus)
  - Keyboard shortcut hints in tooltips

### Legacy Code Cleanup

- [ ] [T113] [P1] [INFRA] Remove Thymeleaf templates and update Spring controllers
  - Delete: `src/main/resources/templates/admin/`, `monitoring/`, `terminal/`, etc.
  - Update controllers to return JSON or serve React SPA index.html
  - Remove Thymeleaf dependency from pom.xml

- [ ] [T114] [P1] [INFRA] Delete vanilla JavaScript files
  - Delete: `src/main/resources/static/js/dashboard.js`, `server-detail.js`, etc.
  - Remove jQuery dependency
  - Clean up unused CSS files

- [ ] [T115] [P2] [INFRA] Setup legacy URL redirects to React routes
  - Files: `src/main/java/...config/WebMvcConfig.java`
  - Redirect /admin/servers → /admin/servers (React route)
  - Redirect /terminal → /terminal (React route)
  - Add 301 redirects for deep-linked legacy URLs

### Documentation & Deployment

- [ ] [T116] [P1] [INFRA] Update CLAUDE.md with React architecture details
  - Add React component structure section
  - Document state management patterns
  - Update troubleshooting guide with React-specific issues

- [ ] [T117] [P2] [INFRA] Create React migration guide for developers
  - Document: `docs/development/react-migration-guide.md`
  - Component creation patterns
  - API hook usage examples
  - Testing best practices

- [ ] [T118] [P2] [INFRA] Write production deployment checklist
  - Document: `docs/deployment/react-production-checklist.md`
  - Environment variable configuration
  - Build verification steps
  - Rollback procedure

- [ ] [T119] [P1] [INFRA] Final end-to-end testing
  - Test all user stories acceptance criteria (US1-US6)
  - Cross-browser testing (Chrome, Firefox, Safari)
  - Performance testing with Lighthouse (target: TTI < 3s)

- [ ] [T120] [P1] [INFRA] Production deployment and monitoring
  - Deploy to staging environment
  - Monitor error logs and performance metrics for 48 hours
  - Deploy to production with rollback plan ready

---

## Dependency Graph

**Critical Path** (blocking tasks that must complete first):

```
Phase 1 Setup:
T001 → T002 → T003 (Build system)
T007 → T008 (Folder structure)
T009 (i18n)

Phase 2 Foundation:
T010 → T011 → T012, T013, T014 (Layout chain)
T017 → T018 → T019 → T020 (Auth chain)
T022 → T023 → T024, T025 (Data fetching)
T026 → T027, T028, T029 (Component library)

Phase 3 Admin (US1):
T034 → T035 → T036 → T037, T038, T039, T040 (Server management chain)
T042 → T043 → T044 (WebSocket chain)
T046 → T047 → T048 → T049, T050 (User management chain)

Phase 4 Terminal (US2):
T056 → T057, T058 → T059 → T060 (Terminal chain)

Phase 5 Monitoring (US3):
T069 → T070, T071 (Monitoring data)
T072 → T073, T074, T075, T076 (Charts)

Phase 6 Profile (US4):
T084 → T085, T086, T087 (Profile chain)

Phase 7 Applications (US6):
T094 → T095 → T096, T097, T098 (Applications chain)
```

**Parallel Execution Opportunities**:

```
Week 2-3: After T010-T017 complete, can work on:
- Layout components (T012-T016) - Developer A
- Auth components (T018-T021) - Developer B
- Component library (T026-T033) - Developer C

Week 5-7: After Phase 2 complete, can work on:
- Server management (T034-T041) - Developer A
- User management (T046-T051) - Developer B
- WebSocket integration (T042-T045) - Developer C

Week 9-11: After US1 complete:
- Terminal UI (T059-T063) - Developer A
- Terminal testing (T066-T067) - Developer B (can start earlier)

Week 13-15: After terminal complete:
- Chart components (T072-T076) - Developer A
- Dashboard pages (T077-T079) - Developer B
- Monitoring tests (T080-T081) - Developer C

Week 16-19: Can parallelize:
- User profile (US4: T082-T092) - Developer A
- Application management (US6: T093-T105) - Developer B
```

**Example Parallel Execution**:

```bash
# Week 2: Three developers work simultaneously
Developer A: T012 (MainLayout) → T013 (Navigation) → T014 (Header)
Developer B: T018 (Auth API) → T019 (ProtectedRoute) → T020 (LoginPage)
Developer C: T026 (Button) → T027 (Input) → T028 (Modal) → T029 (Table)

# Week 5: Parallel server and user management
Developer A: T034 → T035 → T036 → T037 (ServerListPage)
Developer B: T046 → T047 → T048 → T049 (UserListPage)
Developer C: T042 → T043 (WebSocket hooks, used by both A and B)

# Week 16-17: Two independent feature tracks
Developer A: T082 → T084 → T085 → T086 (Profile)
Developer B: T093 → T094 → T095 → T096 (Applications)
```

---

## Task Summary Statistics

**Total Tasks**: 120
**By Priority**:
- P0 (Blocking): 17 tasks
- P1 (High): 61 tasks
- P2 (Medium): 39 tasks
- P3 (Low): 3 tasks

**By User Story**:
- INFRA (Foundation): 38 tasks
- US1 (Admin Dashboard): 21 tasks
- US2 (SSH Terminal): 13 tasks
- US3 (Monitoring/Charts): 14 tasks
- US4 (Profile/Settings): 11 tasks
- US5 (Navigation/Layout): 11 tasks
- US6 (Applications/Config): 13 tasks

**By Phase**:
- Phase 1 (Setup): 9 tasks
- Phase 2 (Foundation): 23 tasks
- Phase 3 (US1 Admin): 21 tasks
- Phase 4 (US2 Terminal): 13 tasks
- Phase 5 (US3 Monitoring): 14 tasks
- Phase 6 (US4 Profile): 11 tasks
- Phase 7 (US6 Applications): 13 tasks
- Phase 8 (Polish): 15 tasks

**Estimated Effort**: 20 weeks with 2-3 developers

---

## Next Steps

1. Review and approve this task breakdown
2. Assign tasks to developers based on expertise
3. Create GitHub issues/tickets from these tasks
4. Begin Phase 1: Setup & Build Infrastructure (Week 1)
5. Hold weekly review meetings to track progress and adjust plan

**Branch**: All work on `007-frontend-react-migration` branch
**Merge Strategy**: Merge to `master` after each phase with comprehensive testing
