# Feature Specification: Frontend React Migration

**Feature Branch**: `007-frontend-react-migration`
**Created**: 2025-01-04
**Status**: Draft
**Input**: User description: "现在系统中混合了react以及非react的前端内容，需要进行统一更新成react，进行分析研究如何进行重构替换。"

## Executive Summary

The Dev Debug Platform currently operates with a **hybrid frontend architecture** mixing React (deployed as microfrontend for Deploy Platform module) with traditional Thymeleaf templates, vanilla JavaScript (9,154 lines), and jQuery. This creates maintenance overhead, inconsistent user experience, and limits modern development practices.

This specification defines a phased migration to a **unified React-based single-page application (SPA)** while maintaining backward compatibility during transition. The migration will modernize the entire frontend stack, improve developer productivity, and establish a sustainable architecture for future feature development.

**Current Architecture Snapshot**:
- 44 Thymeleaf HTML templates (admin, monitoring, terminal modules)
- ~9,154 lines of vanilla JavaScript across 15 files
- 30+ CSS files totaling ~500KB
- 1 React microfrontend (Deploy Platform) with 49 TypeScript files
- 42 Spring Boot controllers serving mixed content
- 285 endpoint mappings (@GetMapping/@PostMapping)

## User Scenarios & Testing

### User Story 1 - Admin Dashboard Migration (Priority: P1)

Migrate the admin dashboard (servers, users, configuration) to React while maintaining all existing functionality including server status monitoring, user management, and real-time updates via WebSocket.

**Why this priority**: Admin dashboard is the most frequently used module by system administrators. It has complex state management (server status, metrics) and real-time updates, making it an ideal candidate to establish migration patterns. Success here validates the technical approach for other modules.

**Independent Test**: Administrator can view, add, edit, and delete servers through the new React dashboard. All CRUD operations complete successfully and real-time status updates display without page refresh. WebSocket connections maintain functionality identical to the legacy version.

**Acceptance Scenarios**:

1. **Given** admin user is logged in, **When** navigating to /admin/servers, **Then** the new React dashboard loads with all servers listed, status indicators accurate, and page renders in under 2 seconds
2. **Given** viewing server list in React dashboard, **When** a server status changes, **Then** the UI updates in real-time via WebSocket without requiring manual refresh
3. **Given** admin clicks "Add Server" button, **When** completing the server form with valid data, **Then** new server is created and immediately visible in the list
4. **Given** viewing a server detail page, **When** clicking "Edit" or "Delete", **Then** modal dialogs display properly and actions execute with immediate UI feedback
5. **Given** admin has been using legacy dashboard, **When** switching to React version, **Then** all previously available features (batch operations, filtering, sorting) remain accessible and functional

---

### User Story 2 - SSH Terminal React Component (Priority: P1)

Convert the WebSocket-based SSH terminal interface from vanilla JavaScript to a modern React component with improved user experience, better state management, and enhanced error handling.

**Why this priority**: SSH terminal is a core technical feature used daily by developers. It has complex real-time bidirectional communication via WebSocket and requires careful state management. Establishing this pattern early ensures all future real-time features follow consistent architecture.

**Independent Test**: Developer can connect to any server via SSH terminal, execute commands, see real-time output, and maintain stable connection for extended sessions. Terminal supports copy/paste, keyboard shortcuts, and handles network interruptions gracefully.

**Acceptance Scenarios**:

1. **Given** developer selects a server, **When** clicking "Open Terminal", **Then** SSH terminal React component initializes, WebSocket connection establishes within 2 seconds, and terminal is ready for input
2. **Given** terminal is connected, **When** typing commands and pressing Enter, **Then** commands execute on remote server and output streams back to terminal in real-time with no noticeable latency
3. **Given** active terminal session, **When** network connection temporarily drops, **Then** terminal displays "Connection Lost" indicator and automatically attempts reconnection
4. **Given** multiple terminal tabs open, **When** switching between tabs, **Then** each terminal maintains its independent WebSocket connection and session state without interference
5. **Given** terminal displaying long output, **When** scrolling through history, **Then** scroll performance remains smooth and previous output remains accessible

---

### User Story 3 - Monitoring Dashboard with Charts (Priority: P2)

Migrate monitoring dashboards (server metrics, application logs, history) to React with modern chart libraries replacing custom D3.js implementations. Improve data visualization, interactivity, and add export capabilities.

**Why this priority**: Monitoring dashboards provide critical operational visibility but currently use mixed chart implementations (some D3, some custom Canvas). Standardizing on React + modern charting library improves maintainability and enables advanced features like drill-down analysis.

**Independent Test**: User can view real-time server metrics (CPU, memory, disk) in interactive charts, select time ranges, compare multiple servers, and export data to CSV. All visualizations render smoothly even with 1000+ data points.

**Acceptance Scenarios**:

1. **Given** user navigates to monitoring dashboard, **When** page loads, **Then** all server metrics charts render within 3 seconds showing last 24 hours of data by default
2. **Given** viewing a metric chart, **When** hovering over data points, **Then** tooltip displays exact timestamp and value with appropriate formatting
3. **Given** monitoring dashboard is open, **When** new metrics arrive via WebSocket, **Then** charts update smoothly without full re-render, maintaining zoom level and selected time range
4. **Given** user selects custom time range (e.g., last 7 days), **When** clicking "Apply", **Then** charts reload with historical data for selected period within 2 seconds
5. **Given** viewing multiple server metrics, **When** clicking "Export" button, **Then** CSV file downloads with all visible data points properly formatted

---

### User Story 4 - User Profile & Settings SPA (Priority: P2)

Convert user profile, preferences, and settings pages to React, replacing Thymeleaf templates with form components that support real-time validation, optimistic updates, and improved accessibility.

**Why this priority**: User profile pages have simpler state requirements compared to dashboards, making them ideal for establishing form handling patterns. They also benefit significantly from React's form libraries and validation frameworks.

**Independent Test**: User can update profile information, change password, modify preferences (theme, language) and see changes reflected immediately across the application without full page reload.

**Acceptance Scenarios**:

1. **Given** user navigates to profile page, **When** page loads, **Then** current profile data populates form fields within 1 second and form is keyboard-navigable with proper ARIA labels
2. **Given** editing profile form, **When** typing in any field, **Then** real-time validation provides immediate feedback (e.g., "Email format invalid") without waiting for submit
3. **Given** user updates display name, **When** clicking "Save", **Then** UI shows optimistic update immediately, saves to backend, and reverts if save fails with clear error message
4. **Given** user changes theme preference from Light to Dark, **When** toggle is clicked, **Then** entire application theme switches instantly without page reload and preference persists across sessions
5. **Given** user changes language setting, **When** selecting new language, **Then** all UI text updates to new language within 1 second using i18n library

---

### User Story 5 - Unified Navigation & Layout Shell (Priority: P1)

Create a React-based application shell with unified navigation, authentication state management, and layout components that wrap all modules. Replace Thymeleaf main-layout.html with React Router-based navigation.

**Why this priority**: The application shell is foundational infrastructure that all other modules depend on. Establishing this early allows parallel migration of individual modules and ensures consistent navigation/layout patterns throughout migration.

**Independent Test**: User can navigate between all application sections (admin, monitoring, terminal, profile) using the new React navigation shell. Authentication state persists correctly, and layout remains consistent across all modules.

**Acceptance Scenarios**:

1. **Given** user logs in, **When** authentication succeeds, **Then** React application shell loads with personalized navigation menu based on user role (admin vs developer) within 2 seconds
2. **Given** user is on any page, **When** clicking navigation links, **Then** route changes without full page reload using client-side routing, and browser back/forward buttons work correctly
3. **Given** user's session expires, **When** attempting to access protected route, **Then** application redirects to login page and preserves intended destination for post-login redirect
4. **Given** user toggles sidebar collapse, **When** navigating between pages, **Then** sidebar state (collapsed/expanded) persists across navigation
5. **Given** multiple browser tabs open, **When** logging out in one tab, **Then** all tabs detect logout event and redirect to login page synchronously

---

### User Story 6 - Configuration & Application Management (Priority: P3)

Migrate application lifecycle management interfaces (upload, start, stop, restart) and configuration editors to React, improving file upload UX and adding drag-and-drop support.

**Why this priority**: While important, application management features are used less frequently than monitoring/admin tasks. They also have fewer real-time update requirements, making them suitable for later migration phases once patterns are established.

**Independent Test**: Developer can upload JAR files via drag-and-drop, manage application lifecycle (start/stop/restart), and edit configuration files with syntax highlighting. All operations complete within expected timeframes with clear progress feedback.

**Acceptance Scenarios**:

1. **Given** developer navigates to applications page, **When** dragging JAR file onto upload zone, **Then** file upload begins immediately with progress bar showing percentage and estimated time remaining
2. **Given** application is stopped, **When** clicking "Start" button, **Then** button shows loading spinner, application starts within 10 seconds, and status indicator updates to "Running" with green color
3. **Given** viewing application configuration, **When** clicking "Edit Config", **Then** Monaco editor loads with syntax highlighting for properties/YAML format and supports find/replace
4. **Given** editing configuration file, **When** clicking "Save", **Then** file saves to server, application prompts "Restart required to apply changes?" with Yes/No options
5. **Given** multiple applications selected, **When** clicking "Batch Stop", **Then** confirmation modal displays, and upon confirmation, all applications stop concurrently with individual status feedback

---

### Edge Cases

- **What happens when user has both old and new frontend versions open in different browser tabs?**
  The system should detect version mismatch via API versioning and prompt users to refresh outdated tabs.

- **How does system handle users with deep-linked URLs to legacy Thymeleaf pages during migration?**
  Legacy URLs should redirect to equivalent React routes with appropriate URL parameter mapping.

- **What happens when WebSocket connection fails during real-time data updates?**
  React components should detect disconnection, display "Connection Lost" indicator, attempt automatic reconnection with exponential backoff, and queue updates for retry.

- **How does system handle browser back button during multi-step forms (e.g., SSH config import wizard)?**
  React Router should preserve form state in location state, allowing users to navigate back without losing progress.

- **What happens when user loses network connectivity mid-operation (e.g., during file upload)?**
  Upload component should detect connection loss, pause upload, and resume from last checkpoint when connectivity restores.

- **How does system handle concurrent edits by multiple users on shared resources (e.g., server configuration)?**
  Implement optimistic locking with conflict detection and prompt users to review changes before overwriting.

- **What happens when browser doesn't support required features (e.g., WebSocket, ES6)?**
  Display graceful degradation message with instructions to upgrade browser or enable compatibility mode.

## Requirements

### Functional Requirements

#### Architecture & Infrastructure

- **FR-001**: System MUST migrate all 44 Thymeleaf HTML templates to React components while maintaining feature parity with legacy implementation
- **FR-002**: System MUST establish React Router-based SPA architecture with client-side routing for all application sections (admin, monitoring, terminal, profile)
- **FR-003**: System MUST implement centralized state management using React Context API or Zustand for global application state (authentication, user preferences, theme)
- **FR-004**: System MUST create reusable component library covering common UI patterns (buttons, modals, forms, tables, cards) used across all modules
- **FR-005**: System MUST maintain backward compatibility by supporting both legacy Thymeleaf and new React routes during transition period (dual-mode operation)

#### WebSocket & Real-Time Communication

- **FR-006**: System MUST convert all WebSocket connections (server status updates, SSH terminal, log streaming) from vanilla JavaScript to React hooks with automatic reconnection logic
- **FR-007**: System MUST ensure React components subscribe to appropriate WebSocket topics and unsubscribe on unmount to prevent memory leaks
- **FR-008**: System MUST implement connection health monitoring with visual indicators (connected, reconnecting, disconnected) in React UI
- **FR-009**: System MUST buffer incoming WebSocket messages during component re-renders to prevent message loss

#### Data Fetching & API Integration

- **FR-010**: System MUST replace all jQuery $.ajax calls with modern fetch API or Axios wrapped in React hooks (useQuery/useMutation pattern)
- **FR-011**: System MUST implement loading states, error boundaries, and retry logic for all API requests in React components
- **FR-012**: System MUST support optimistic updates for mutations (create/update/delete) with automatic rollback on failure
- **FR-013**: System MUST cache API responses appropriately to minimize redundant network requests

#### Authentication & Authorization

- **FR-014**: System MUST migrate authentication flow to React with protected routes requiring valid session tokens
- **FR-015**: System MUST implement automatic token refresh mechanism to prevent session expiry during active usage
- **FR-016**: System MUST synchronize authentication state across multiple browser tabs using BroadcastChannel or localStorage events
- **FR-017**: System MUST display role-based navigation menus and hide unauthorized features based on user permissions

#### Forms & Validation

- **FR-018**: System MUST implement all forms (server creation, user registration, configuration editing) using React Hook Form or Formik with real-time validation
- **FR-019**: System MUST provide accessible form controls with proper ARIA labels, keyboard navigation, and error message announcements
- **FR-020**: System MUST support multi-step forms (e.g., SSH config import wizard) with progress indicators and ability to navigate back without losing data

#### File Operations

- **FR-021**: System MUST support drag-and-drop file uploads for JAR files and configuration files with progress tracking
- **FR-022**: System MUST implement resumable uploads for large files (>100MB) with ability to pause/resume
- **FR-023**: System MUST validate file types and sizes on client-side before upload to provide immediate feedback

#### Data Visualization

- **FR-024**: System MUST replace custom D3.js/Canvas chart implementations with modern React chart library (Recharts, Chart.js, or Apache ECharts)
- **FR-025**: System MUST support interactive charts with tooltips, zoom, pan, and data point selection
- **FR-026**: System MUST update charts in real-time as new metric data arrives via WebSocket without full chart re-render
- **FR-027**: System MUST support exporting chart data and screenshots in multiple formats (CSV, PNG, PDF)

#### Internationalization

- **FR-028**: System MUST implement i18n using react-i18next or similar library supporting dynamic language switching without page reload
- **FR-029**: System MUST migrate existing Chinese/English translations from backend to frontend JSON locale files
- **FR-030**: System MUST format dates, numbers, and currencies according to selected locale

#### Styling & Theming

- **FR-031**: System MUST migrate CSS from 30+ separate stylesheets to CSS Modules or styled-components scoped to React components
- **FR-032**: System MUST support theme switching (light/dark mode) with CSS custom properties inherited from main application shell
- **FR-033**: System MUST ensure consistent design tokens (colors, spacing, typography) across all migrated components

#### Testing & Quality

- **FR-034**: System MUST achieve minimum 70% unit test coverage for all new React components using Vitest and React Testing Library
- **FR-035**: System MUST implement integration tests for critical user flows (login, server creation, terminal connection)
- **FR-036**: System MUST pass accessibility audit (WCAG 2.1 Level AA) for all migrated components

#### Performance

- **FR-037**: System MUST implement code splitting to ensure initial bundle size remains under 500KB (gzipped)
- **FR-038**: System MUST lazy-load route components to improve initial page load time
- **FR-039**: System MUST virtualize long lists (>100 items) using react-window or similar library to maintain smooth scrolling performance

#### Build & Deployment

- **FR-040**: System MUST integrate React build output with Maven build process, generating production bundles during mvn package
- **FR-041**: System MUST support development mode with hot module replacement (HMR) for rapid iteration
- **FR-042**: System MUST generate source maps in development and exclude them from production builds for security

### Key Entities

- **React Component**: Reusable UI building block encapsulating markup, styles, and behavior; examples include ServerListTable, SSHTerminal, MetricsChart
- **Route Configuration**: Mapping between URL paths and React components defining application navigation structure; includes nested routes for modular sections
- **API Client**: Abstraction layer for HTTP communication with Spring Boot backend; wraps fetch/Axios with authentication headers, error handling, and retry logic
- **WebSocket Hook**: Custom React hook managing WebSocket connection lifecycle (connect, subscribe, unsubscribe, reconnect) for real-time features
- **State Store**: Centralized application state container holding authentication status, user preferences, cached API responses, and UI state
- **Theme Configuration**: Object defining design tokens (colors, fonts, spacing) supporting multiple themes (light, dark) with CSS custom properties
- **Form Schema**: Validation rules and field definitions for forms; integrates with React Hook Form/Formik for consistent error handling
- **Locale Bundle**: JSON files containing translated strings for each supported language; loaded dynamically based on user language preference

## Success Criteria

### Measurable Outcomes

- **SC-001**: All 44 legacy Thymeleaf templates successfully migrated to React components with 100% feature parity validated through manual testing checklist
- **SC-002**: React application bundle size (initial + lazy-loaded chunks) remains under 2MB gzipped, achieving faster load times than legacy implementation
- **SC-003**: Initial page load (Time to Interactive) improves by at least 30% compared to Thymeleaf server-rendered pages, measured via Lighthouse
- **SC-004**: Unit test coverage for React components reaches 70% minimum, with critical paths (authentication, real-time updates) achieving 90%+ coverage
- **SC-005**: Zero WebSocket connection memory leaks detected during 4-hour stress test with 50 concurrent users opening/closing terminals
- **SC-006**: All forms support keyboard-only navigation and pass WCAG 2.1 Level AA accessibility audit using axe DevTools
- **SC-007**: Development team reports 50% reduction in time to implement new features post-migration due to component reusability and modern tooling
- **SC-008**: Application supports graceful degradation for IE11 (shows upgrade prompt) and full functionality on Chrome 90+, Firefox 88+, Safari 14+

## Assumptions

1. **Backend API Stability**: Existing Spring Boot REST APIs remain stable throughout migration; no breaking changes to endpoints or response formats
2. **Browser Requirements**: Target users have modern browsers (Chrome 90+, Firefox 88+, Safari 14+); IE11 support limited to upgrade prompt
3. **WebSocket Protocol**: STOMP over WebSocket protocol remains unchanged; existing backend WebSocket handlers compatible with React clients
4. **Authentication Mechanism**: Current session-based authentication (Spring Security) continues to work; no migration to JWT tokens required
5. **Deployment Model**: Application continues as Spring Boot monolith with React SPA served as static resources under /static; no separate Node.js server needed
6. **User Training**: End users receive documentation on new UI features but no formal training required due to maintaining familiar workflows
7. **Phased Rollout**: Feature flags or routing rules allow gradual rollout of React modules while legacy Thymeleaf routes remain accessible
8. **Internationalization Scope**: Only Chinese and English languages currently supported; no additional languages added during migration
9. **CSS Framework**: Existing CSS design system (--shell-* custom properties) remains the foundation; no complete design overhaul planned
10. **Performance Budget**: Target environments have minimum 2 CPU cores, 4GB RAM, and broadband internet (5Mbps+)

## Non-Goals

- **Complete Backend Rewrite**: This migration focuses solely on frontend technology stack; existing Spring Boot backend services, data models, and business logic remain unchanged
- **Mobile Native Applications**: React migration targets web browsers only; no React Native mobile apps or Progressive Web App (PWA) features planned
- **Microservices Architecture**: Application remains a Spring Boot monolith; no decomposition into separate microservices for frontend modules
- **GraphQL Adoption**: Existing REST APIs continue to serve data; no GraphQL layer introduced during migration
- **Real-Time Collaboration**: No addition of collaborative editing features (e.g., multiple users editing same configuration file simultaneously)
- **Advanced Analytics**: No migration of monitoring/metrics backend to specialized time-series databases (InfluxDB, Prometheus); existing database storage retained
- **CI/CD Pipeline Overhaul**: Build process integrates with existing Maven workflow; no migration to dedicated frontend build servers or container registries
- **CSS Framework Replacement**: No adoption of Tailwind CSS, Bootstrap, or other utility-first frameworks; existing custom CSS design system preserved

## Dependencies

### Technical Dependencies

- **Existing React Infrastructure**: Deploy Platform module already uses React 18 with TypeScript; migration extends this stack to remaining modules
- **Spring Boot Backend**: All React components depend on existing REST APIs and WebSocket endpoints; backend must remain operational during migration
- **Build Tooling**: Maven must successfully invoke frontend build process (Vite or similar) during package phase
- **Authentication System**: React routes depend on Spring Security session management; session validation APIs must remain stable

### Resource Dependencies

- **Frontend Developer Availability**: Requires 2-3 frontend developers with React expertise for 3-4 months full-time effort
- **QA Testing Resources**: Manual testing of all 44 migrated pages requires dedicated QA engineer for 4-6 weeks
- **Backend Developer Coordination**: Occasional backend changes (new API endpoints, response format adjustments) require backend developer time
- **DevOps Support**: Updates to build scripts, deployment procedures, and server configurations require DevOps assistance

### External Dependencies

- **React Ecosystem Libraries**: Availability and stability of npm packages (React Router, React Hook Form, chart libraries, i18n)
- **Browser Vendor Support**: Ongoing browser updates may require polyfills or workarounds for new React features
- **TypeScript Compatibility**: Continuous updates to TypeScript compiler and React type definitions must remain compatible

## Migration Phases

### Phase 1: Foundation (Weeks 1-4)

**Goal**: Establish core infrastructure and prove migration viability

**Deliverables**:
- React application shell with unified navigation (replaces main-layout.html)
- Authentication state management and protected routes
- Component library with base components (Button, Input, Modal, Table)
- Build integration with Maven
- Development environment with HMR

**Success Indicator**: Developer can navigate between React shell and one migrated module (Admin Dashboard skeleton)

### Phase 2: Admin Module (Weeks 5-8)

**Goal**: Complete migration of most complex module to validate patterns

**Deliverables**:
- Server management CRUD operations (list, create, edit, delete)
- User management interfaces
- Real-time server status updates via WebSocket
- Batch operations support
- Unit tests for all components

**Success Indicator**: Administrators can perform all server/user management tasks through React UI with feature parity to legacy version

### Phase 3: Real-Time Features (Weeks 9-12)

**Goal**: Migrate WebSocket-heavy modules

**Deliverables**:
- SSH Terminal React component with xterm.js integration
- Application log streaming interface
- Monitoring dashboard with real-time metrics
- WebSocket reconnection logic and error handling

**Success Indicator**: Developers can open SSH terminals, view streaming logs, and monitor metrics with stable WebSocket connections

### Phase 4: Data Visualization (Weeks 13-15)

**Goal**: Replace custom chart implementations

**Deliverables**:
- Monitoring history dashboard with interactive charts
- Server metrics visualization (CPU, memory, disk trends)
- Chart export functionality (CSV, PNG)
- Performance optimization for large datasets

**Success Indicator**: Users can analyze historical metrics with smooth chart interactions and export capabilities

### Phase 5: Forms & Configuration (Weeks 16-18)

**Goal**: Migrate remaining CRUD interfaces

**Deliverables**:
- Application upload with drag-and-drop
- Configuration editor with syntax highlighting
- User profile and preferences pages
- Multi-step wizards (SSH config import)

**Success Indicator**: Developers can manage applications and configurations through React forms with real-time validation

### Phase 6: Polish & Decommission (Weeks 19-20)

**Goal**: Finalize migration and remove legacy code

**Deliverables**:
- Legacy Thymeleaf template cleanup
- Final accessibility audit and fixes
- Performance optimization (code splitting, lazy loading)
- Production deployment and monitoring

**Success Indicator**: All features accessible via React SPA; legacy routes redirect to React equivalents; no functionality regressions reported

## Technical Considerations

### Architecture Decisions

- **SPA vs MPA**: Choosing Single-Page Application architecture requires careful planning of route structure, state management, and SEO implications (if applicable)
- **State Management**: Decision between React Context, Zustand, Redux, or Jotai impacts complexity, performance, and learning curve
- **Component Library**: Build custom components vs adopt existing library (Ant Design, Material-UI) affects development speed and customization flexibility
- **CSS Strategy**: CSS Modules vs Styled Components vs Tailwind impacts bundle size, runtime performance, and developer experience
- **Build Tool**: Vite vs Webpack vs Rollup affects build speed, HMR performance, and configuration complexity

### Risk Mitigation

- **Feature Parity Gaps**: Maintain comprehensive checklist of legacy features and validate each during migration; implement feature flags for rollback capability
- **Performance Regression**: Establish performance budgets using Lighthouse CI; monitor bundle size, Time to Interactive, and WebSocket message latency
- **User Disruption**: Deploy behind feature flags allowing gradual rollout; maintain legacy routes as fallback during transition
- **Knowledge Gaps**: Conduct React training sessions for team members unfamiliar with hooks, TypeScript, or modern build tools
- **Technical Debt**: Resist temptation to "fix everything" during migration; focus on feature parity first, enhancements later

### Success Factors

- **Executive Sponsorship**: Secure commitment from engineering leadership for dedicated resources and extended timeline
- **Incremental Approach**: Prioritize high-value, high-usage modules (admin dashboard, terminal) to demonstrate ROI early
- **Automated Testing**: Invest in comprehensive test suite to catch regressions quickly and enable confident refactoring
- **Documentation**: Maintain migration guide documenting patterns, decisions, and lessons learned for team reference
- **User Feedback Loop**: Involve key users (admins, developers) in early testing to catch usability issues before full rollout

---

**Next Steps**: Upon approval, proceed to `/speckit.plan` to generate detailed technical implementation plan with specific tasks, file changes, and effort estimates.
