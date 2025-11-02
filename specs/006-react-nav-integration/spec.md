# Feature Specification: React Navigation Integration

**Feature Branch**: `006-react-nav-integration`
**Created**: 2025-11-02
**Status**: Draft
**Input**: User description: "完成React应用导航与主应用导航系统的集成，移除React应用的重复侧边栏,使用主应用的二级菜单进行导航"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Embedded Mode Navigation Without Duplication (Priority: P1)

When users access the deployment platform through the main application's navigation menu, they should see a seamless, unified interface without duplicate sidebars.

**Why this priority**: This is the core value proposition - eliminating the confusing "double sidebar" problem that currently exists. Without this, users experience a broken UI that undermines trust in the platform.

**Independent Test**: Can be fully tested by clicking "部署管理平台" in the main navigation and verifying that only one sidebar is visible. Delivers immediate UX value by providing a clean, professional interface.

**Acceptance Scenarios**:

1. **Given** user is logged into the main application, **When** they click "部署管理平台" menu item, **Then** the React application loads without its sidebar, showing only the main application's sidebar with expanded submenu
2. **Given** React deployment platform is loaded in embedded mode, **When** user inspects the page, **Then** no React sidebar component is rendered in the DOM
3. **Given** user navigates between deployment platform pages, **When** viewing different sections, **Then** the main sidebar remains visible and functional throughout

---

### User Story 2 - Synchronized Navigation State (Priority: P1)

When users click submenu items in the main application's navigation, the React application should respond by loading the corresponding page, and vice versa - when navigating within React, the main sidebar should highlight the active submenu item.

**Why this priority**: Without bidirectional sync, navigation becomes broken - users click menu items that don't respond, or the active state doesn't match what's displayed. This is essential for basic usability.

**Independent Test**: Can be tested by clicking each submenu item (概览总览, 发布管理, 策略配置) and verifying the React application loads the correct page. Delivers functional navigation that users expect from any web application.

**Acceptance Scenarios**:

1. **Given** user is on the deployment platform overview page, **When** they click "发布管理" submenu item, **Then** React Router navigates to `/deploy-platform/releases` and the releases page is displayed
2. **Given** user is viewing releases page in React, **When** the page loads, **Then** the main sidebar highlights "发布管理" as the active submenu item
3. **Given** user navigates using React's internal links (if any), **When** the route changes, **Then** the main sidebar updates to reflect the current active submenu item

---

### User Story 3 - Standalone Mode Preservation (Priority: P2)

Developers and testers should be able to access the React deployment platform directly at its standalone URL (`/admin/deploy-platform`) with full sidebar navigation preserved for development and testing purposes.

**Why this priority**: While not critical for end users, this maintains developer productivity and enables isolated testing of React components. It's a P2 because the production user experience (P1) must work first.

**Independent Test**: Can be tested by directly navigating to `/admin/deploy-platform` and verifying the React sidebar is visible and functional. Delivers value for development workflows.

**Acceptance Scenarios**:

1. **Given** developer accesses `/admin/deploy-platform` directly in browser, **When** the page loads, **Then** React application renders with its own sidebar visible
2. **Given** React application is in standalone mode, **When** user clicks sidebar navigation items, **Then** React Router handles navigation without affecting main application state
3. **Given** user is in standalone mode, **When** they navigate between pages, **Then** the React sidebar maintains its own active state independently

---

### User Story 4 - Seamless Style Integration (Priority: P2)

The React deployment platform should visually match the main application's design system when embedded, including colors, fonts, spacing, and component styles.

**Why this priority**: While functional integration (P1) is critical, visual consistency is important for professional appearance and user confidence. However, the application can function with minor style mismatches, making this P2.

**Independent Test**: Can be tested by visual inspection of embedded vs standalone modes, comparing colors, typography, and spacing against the main application. Delivers polished user experience.

**Acceptance Scenarios**:

1. **Given** React application is in embedded mode, **When** viewing any page, **Then** CSS custom properties (colors, fonts) match the main application's theme
2. **Given** main application is in dark mode, **When** React application loads, **Then** it automatically switches to dark mode to match
3. **Given** user views the content area, **When** comparing with other main app pages, **Then** padding, margins, and spacing are consistent

---

### Edge Cases

- What happens when user directly navigates to a deep link (e.g., `/admin/workspace#page=deploy-platform/releases`)? Does the main sidebar properly highlight the submenu item and expand the parent menu?
- How does the system handle navigation when JavaScript is disabled or fails to load? Does it gracefully degrade?
- What happens when user uses browser back/forward buttons after navigating through deployment platform pages? Does the main sidebar state stay synchronized?
- How does the system detect embedded vs standalone mode if the React app is loaded via iframe or different domain?
- What happens if React Router navigation is triggered before the main application's navigation event handlers are initialized?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: React application MUST detect whether it's running in embedded mode (within main-layout.html) or standalone mode (direct URL access)
- **FR-002**: React application MUST conditionally render ShellLayout (with sidebar) only in standalone mode
- **FR-003**: React application MUST render ContentOnlyLayout (without sidebar) when in embedded mode
- **FR-004**: Main application sidebar MUST emit navigation events when submenu items are clicked
- **FR-005**: React Router MUST listen for navigation events from main application and update routes accordingly
- **FR-006**: React Router MUST emit route change events when internal navigation occurs
- **FR-007**: Main application MUST listen for React route change events and update sidebar active state
- **FR-008**: System MUST handle deep linking by parsing hash/query parameters and initializing both React Router and main sidebar state correctly
- **FR-009**: Browser history navigation (back/forward) MUST keep main sidebar and React Router synchronized
- **FR-010**: React application MUST inherit CSS custom properties from main application for consistent theming

### Key Entities *(include if feature involves data)*

- **NavigationMode**: Represents whether React app is in "embedded" or "standalone" mode (detected at runtime)
- **NavigationEvent**: Message structure for communication between main app and React Router, containing route/page identifier
- **LayoutComponent**: Union type representing either ShellLayout (with sidebar) or ContentOnlyLayout (without sidebar)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When accessing deployment platform through main navigation, users see zero duplicate sidebars (100% elimination of double sidebar issue)
- **SC-002**: Main sidebar submenu clicks navigate to correct React pages within 200ms (measured from click to route change)
- **SC-003**: React Router navigation updates main sidebar active state within 100ms (measured from route change to DOM update)
- **SC-004**: Standalone mode URL (`/admin/deploy-platform`) renders with React sidebar functional and independent
- **SC-005**: CSS custom properties from main application are successfully inherited by React components (verified by computed styles matching)
- **SC-006**: Browser back/forward navigation maintains synchronized state 100% of the time across 20 navigation sequences
- **SC-007**: Developer can switch between embedded and standalone modes by changing URL without code changes

## Assumptions

- Main application sidebar uses Thymeleaf templating with client-side JavaScript event handlers
- React application uses React Router v6 for internal navigation
- Both applications are served from the same domain/origin (no CORS issues)
- JavaScript is enabled in user's browser
- Main application's navigation JavaScript loads before React application attempts to establish communication
- React application is bundled with Vite and loaded via script tag in main-layout.html

## Dependencies

- **Main Application**: Existing sidebar navigation structure with submenu support (already implemented in POC)
- **React Router**: v6 with BrowserRouter or HashRouter configured
- **React Components**: ShellLayout and ContentOnlyLayout components
- **Build System**: Vite build configured to expose React application as a standalone bundle
- **CSS Variables**: Main application CSS custom properties must be defined and accessible

## Out of Scope

- Complete redesign of React application UI components
- Migration of main application from Thymeleaf to React
- Server-side rendering (SSR) of React application
- Advanced routing features like lazy loading optimization or route-based code splitting
- Multi-language (i18n) navigation synchronization beyond existing implementations
- Mobile responsive navigation (assumes desktop-first design)
- Navigation analytics or telemetry tracking
