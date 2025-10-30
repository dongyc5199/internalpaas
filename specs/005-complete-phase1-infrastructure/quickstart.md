# Quick Start Guide: Phase 1 Infrastructure

**Project**: Dev Debug Platform - React Frontend Migration Phase 1
**Date**: 2025-10-29
**Branch**: `005-complete-phase1-infrastructure`
**Audience**: Frontend developers onboarding to the React micro-frontend

## Overview

This guide will help you set up your development environment, run tests, start Storybook, and use the Phase 1 infrastructure components (UI library, state management, i18n) in your React applications.

---

## Prerequisites

### Required Software

| Tool | Version | Check Command | Install Link |
|------|---------|---------------|--------------|
| **Node.js** | 18.x or higher | `node -v` | [nodejs.org](https://nodejs.org/) |
| **npm** | 9.x or higher | `npm -v` | (comes with Node.js) |
| **Git** | 2.x or higher | `git --version` | [git-scm.com](https://git-scm.com/) |

### Verify Your Environment

```bash
# Check Node.js version (should be 18+)
node -v
# Expected output: v18.19.0 or higher

# Check npm version (should be 9+)
npm -v
# Expected output: 9.8.1 or higher

# Navigate to project root
cd E:\work\code\internalpaas

# Switch to feature branch
git checkout 005-complete-phase1-infrastructure
```

---

## Installation

### 1. Install Dependencies

```bash
# Install all dependencies (may take 2-3 minutes)
npm install

# Verify installation succeeded
npm list --depth=0
```

**Key Dependencies Installed**:
- React 19.2.0
- Vite 5.4.20
- TypeScript 5.9.3
- Vitest 1.6.1
- @tanstack/react-query 5.90.5
- zustand 5.0.8
- react-i18next 16.2.1
- Storybook 8.x

### 2. Verify TypeScript Configuration

```bash
# Check for TypeScript errors
npx tsc --noEmit

# Expected output: No errors (or only warnings)
```

---

## Running Tests

### Unit Tests (Vitest)

```bash
# Run all tests once
npm run test:run

# Run tests in watch mode (re-runs on file changes)
npm run test

# Run tests with coverage report
npm run test:coverage

# Expected output:
# ✓ src/main/frontend/tests/websocket.test.ts (29 tests passed)
# ✓ src/main/frontend/react-app/tests/components/Button.test.tsx (5 tests passed)
# ✓ src/main/frontend/react-app/tests/hooks/useTokenRefresh.test.tsx (8 tests passed)
#
# Test Files: 3 passed (3)
# Tests: 42 passed (42)
# Coverage: 85.3% (target: 80%)
```

### E2E Tests (Playwright) - Optional

```bash
# Install Playwright browsers (first time only)
npx playwright install

# Run E2E tests
npm run test:e2e

# Run E2E tests in headed mode (see browser)
npm run test:e2e -- --headed
```

### Troubleshooting Tests

**Problem**: WebSocket tests fail with "Timeout waiting for connection"

**Solution**: Increase timeout in test file:
```typescript
// src/main/frontend/tests/websocket.test.ts
it('should connect', { timeout: 10000 }, async () => {
  // Test code
});
```

**Problem**: Tests pass locally but fail in CI

**Solution**: Check for race conditions, use `vi.runAllTimersAsync()` for nested timers.

---

## Starting Storybook

Storybook provides an interactive UI component gallery with live documentation.

```bash
# Start Storybook dev server (port 6006)
npm run storybook

# Expected output:
# ╭───────────────────────────────────────────────╮
# │                                               │
# │   Storybook 8.x for React started            │
# │   Local:   http://localhost:6006              │
# │                                               │
# ╰───────────────────────────────────────────────╯
```

**Browser Navigation**:
1. Open http://localhost:6006 in your browser
2. Explore components in the left sidebar:
   - Components/Button
   - Components/Input
   - Components/Select
   - Components/Modal
   - Components/Table
3. Interact with component controls in the "Canvas" tab
4. Read auto-generated documentation in the "Docs" tab

### Building Storybook (Static Site)

```bash
# Build static Storybook site for deployment
npm run build-storybook

# Output directory: storybook-static/
# Serve with: npx http-server storybook-static
```

---

## Development Workflow

### Project Structure Overview

```text
src/main/frontend/react-app/
├── components/         # UI component library (Button, Input, etc.)
├── hooks/              # Custom React hooks (useTokenRefresh, useReleases)
├── stores/             # Zustand state stores (authStore)
├── i18n/               # Internationalization config and translations
├── config/             # React Query, constants
├── App.tsx             # Root React component
└── main.tsx            # Application entry point
```

---

## Using UI Components

All components are exported from `@/components` and use TypeScript for type safety.

### Button Component

```tsx
import { Button } from '@/components';

function MyForm() {
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    setLoading(true);
    await saveData();
    setLoading(false);
  };

  return (
    <>
      <Button
        variant="primary"
        size="medium"
        type="submit"
        loading={loading}
        onClick={handleSubmit}
      >
        Save Changes
      </Button>

      <Button
        variant="danger"
        size="small"
        onClick={handleDelete}
      >
        Delete
      </Button>
    </>
  );
}
```

**Props Reference**:
- `variant`: 'primary' | 'secondary' | 'danger' | 'success' | 'warning'
- `size`: 'small' | 'medium' | 'large'
- `disabled`: boolean
- `loading`: boolean (shows spinner)
- `type`: 'button' | 'submit' | 'reset'

### Input Component

```tsx
import { Input } from '@/components';
import { useState } from 'react';

function LoginForm() {
  const [username, setUsername] = useState('');
  const [error, setError] = useState('');

  return (
    <form>
      <Input
        label="Username"
        value={username}
        onChange={setUsername}
        placeholder="Enter username"
        error={error}
        required
      />
      <Input
        label="Password"
        type="password"
        value={password}
        onChange={setPassword}
        required
      />
    </form>
  );
}
```

**Props Reference**:
- `value`: string (controlled)
- `onChange`: (value: string) => void
- `type`: 'text' | 'password' | 'email' | 'number'
- `label`: string
- `error`: string (validation message)
- `placeholder`: string

### Modal Component

```tsx
import { Modal, Button } from '@/components';
import { useState } from 'react';

function DeleteConfirmation() {
  const [isOpen, setIsOpen] = useState(false);

  const handleDelete = () => {
    // Delete logic
    setIsOpen(false);
  };

  return (
    <>
      <Button onClick={() => setIsOpen(true)}>Delete</Button>

      <Modal
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        title="Confirm Deletion"
        footer={
          <>
            <Button variant="secondary" onClick={() => setIsOpen(false)}>
              Cancel
            </Button>
            <Button variant="danger" onClick={handleDelete}>
              Delete
            </Button>
          </>
        }
      >
        Are you sure you want to delete this release?
      </Modal>
    </>
  );
}
```

### Table Component

```tsx
import { Table } from '@/components';

interface Release {
  id: string;
  version: string;
  createdAt: string;
  status: 'active' | 'inactive';
}

function ReleaseList() {
  const { data: releases, isLoading } = useReleases();

  return (
    <Table<Release>
      data={releases || []}
      columns={[
        { key: 'version', label: 'Version', sortable: true },
        {
          key: 'status',
          label: 'Status',
          render: (status) => (
            <span className={status === 'active' ? 'badge-success' : 'badge-secondary'}>
              {status}
            </span>
          )
        },
        { key: 'createdAt', label: 'Created At' },
      ]}
      loading={isLoading}
      emptyMessage="No releases found"
      onRowClick={(release) => console.log('Clicked:', release.id)}
    />
  );
}
```

---

## Using State Management

### React Query (Server State)

React Query manages server data fetching, caching, and synchronization.

#### Example: Fetching Releases

```tsx
// hooks/useReleases.ts
import { useQuery } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';

interface Release {
  id: string;
  version: string;
  createdAt: string;
}

async function fetchReleases(token: string): Promise<Release[]> {
  const response = await fetch('/api/deploy-platform/releases', {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error('Failed to fetch releases');
  return response.json();
}

export function useReleases() {
  const token = useAuthStore((state) => state.token?.accessToken);

  return useQuery({
    queryKey: ['releases', token],
    queryFn: () => fetchReleases(token!),
    enabled: !!token, // Only run if token exists
    staleTime: 2 * 60 * 1000, // 2 minutes
  });
}

// Component usage:
function ReleasesPage() {
  const { data: releases, isLoading, error } = useReleases();

  if (isLoading) return <div>Loading...</div>;
  if (error) return <div>Error: {error.message}</div>;

  return (
    <ul>
      {releases?.map((release) => (
        <li key={release.id}>{release.version}</li>
      ))}
    </ul>
  );
}
```

**Key Concepts**:
- `queryKey`: Unique cache key (array of dependencies)
- `queryFn`: Async function that fetches data
- `enabled`: Conditional fetching
- `staleTime`: How long data stays "fresh" (no refetch)
- `cacheTime`: How long cached data persists after unmount

### Zustand (Client State)

Zustand manages local UI state (auth, preferences, form state).

#### Example: Authentication Store

```tsx
// stores/authStore.ts (already provided)
import { useAuthStore } from '@/stores/authStore';

// Component: Check authentication status
function ProtectedRoute() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const user = useAuthStore((state) => state.user);

  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }

  return <div>Welcome, {user?.username}!</div>;
}

// Component: Manual logout
function LogoutButton() {
  const clearAuth = useAuthStore((state) => state.clearAuth);

  return (
    <Button variant="secondary" onClick={clearAuth}>
      Logout
    </Button>
  );
}

// Hook: Get current token for API calls
function useApiToken() {
  return useAuthStore((state) => state.token?.accessToken);
}
```

#### Example: Custom UI State Store

```tsx
// stores/uiStore.ts
import { create } from 'zustand';

interface UIState {
  sidebarOpen: boolean;
  toggleSidebar: () => void;
  language: 'zh-CN' | 'en-US';
  setLanguage: (lang: 'zh-CN' | 'en-US') => void;
}

export const useUIStore = create<UIState>((set) => ({
  sidebarOpen: false,
  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
  language: 'zh-CN',
  setLanguage: (language) => set({ language }),
}));

// Component usage:
function Sidebar() {
  const isOpen = useUIStore((state) => state.sidebarOpen);
  const toggle = useUIStore((state) => state.toggleSidebar);

  return (
    <div className={isOpen ? 'sidebar-open' : 'sidebar-closed'}>
      <button onClick={toggle}>Toggle</button>
    </div>
  );
}
```

---

## Using Internationalization (i18n)

React-i18next provides language switching and translation management.

### Language Switching

```tsx
import { useTranslation } from 'react-i18next';

function LanguageSwitcher() {
  const { i18n } = useTranslation();

  return (
    <select
      value={i18n.language}
      onChange={(e) => i18n.changeLanguage(e.target.value)}
    >
      <option value="zh-CN">中文</option>
      <option value="en-US">English</option>
    </select>
  );
}
```

### Using Translations

```tsx
import { useTranslation } from 'react-i18next';

function DeploymentForm() {
  const { t } = useTranslation();

  return (
    <form>
      <h1>{t('deploy.release.title')}</h1>
      <Input
        label={t('deploy.release.version')}
        required
      />
      <Button type="submit">
        {t('common.buttons.submit')}
      </Button>
    </form>
  );
}
```

### Translation Key Structure

Translations are stored in `src/main/frontend/react-app/i18n/locales/*.json`:

```json
// zh-CN.json
{
  "common": {
    "buttons": {
      "submit": "提交",
      "cancel": "取消"
    }
  },
  "deploy": {
    "release": {
      "title": "发布管理",
      "version": "版本号"
    }
  }
}

// en-US.json
{
  "common": {
    "buttons": {
      "submit": "Submit",
      "cancel": "Cancel"
    }
  },
  "deploy": {
    "release": {
      "title": "Release Management",
      "version": "Version"
    }
  }
}
```

### Adding New Translations

1. **Edit translation files**:
   ```json
   // zh-CN.json
   {
     "myFeature": {
       "title": "我的功能"
     }
   }
   ```

2. **Use in component**:
   ```tsx
   const { t } = useTranslation();
   return <h1>{t('myFeature.title')}</h1>;
   ```

3. **Handle missing keys** (fallback to key name):
   ```tsx
   // If key doesn't exist, shows "myFeature.unknownKey"
   {t('myFeature.unknownKey')}
   ```

---

## Common Issues & Troubleshooting

### Issue 1: "Module not found" errors

**Symptom**: Import fails with `Cannot find module '@/components'`

**Solution**: Verify TypeScript path aliases in `tsconfig.json`:
```json
{
  "compilerOptions": {
    "paths": {
      "@/*": ["./src/main/frontend/react-app/*"]
    }
  }
}
```

### Issue 2: React Query not caching data

**Symptom**: API called multiple times for same data

**Solution**: Check `staleTime` and `cacheTime` in QueryClient config:
```typescript
// config/queryClient.ts
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      cacheTime: 10 * 60 * 1000, // 10 minutes
    },
  },
});
```

### Issue 3: i18n translations not updating

**Symptom**: Language switch doesn't change UI text

**Solution**: Ensure `App.tsx` wraps components with `I18nextProvider`:
```tsx
import { I18nextProvider } from 'react-i18next';
import i18n from './i18n/i18n';

function App() {
  return (
    <I18nextProvider i18n={i18n}>
      {/* Your components */}
    </I18nextProvider>
  );
}
```

### Issue 4: Storybook not loading styles

**Symptom**: Components look unstyled in Storybook

**Solution**: Import global styles in `.storybook/preview.ts`:
```typescript
import 'bootstrap/dist/css/bootstrap.min.css';
import '../src/main/frontend/react-app/styles/global.css';
```

---

## Development Best Practices

### 1. Component Development

✅ **DO**:
- Use TypeScript for all components
- Add PropTypes or TypeScript interfaces
- Create Storybook stories for each component
- Write unit tests for component logic

❌ **DON'T**:
- Use `any` type (enable strict TypeScript)
- Forget to handle loading and error states
- Mix server state (React Query) with client state (Zustand)

### 2. State Management

✅ **DO**:
- Use React Query for server data (API calls)
- Use Zustand for client state (UI, preferences)
- Keep token in memory (not localStorage)

❌ **DON'T**:
- Sync React Query data to Zustand (anti-pattern)
- Store sensitive data in localStorage
- Use global state for component-local state

### 3. Testing

✅ **DO**:
- Write tests for custom hooks
- Mock API calls in tests
- Use `@testing-library/react` user events

❌ **DON'T**:
- Test implementation details (internal state)
- Forget to clean up timers in tests
- Write E2E tests for everything (unit tests are faster)

---

## Next Steps

1. ✅ **Environment Set Up**: You can now run tests and Storybook
2. → **Explore Components**: Open Storybook and interact with UI components
3. → **Build Your First Page**: Create a new page using Button, Input, Table
4. → **Add Data Fetching**: Use `useQuery` to fetch server data
5. → **Contribute**: Read [CONTRIBUTING.md](../../CONTRIBUTING.md) for git workflow

---

## Additional Resources

### Documentation
- [React Query Docs](https://tanstack.com/query/latest/docs/react/overview)
- [Zustand Docs](https://docs.pmnd.rs/zustand/getting-started/introduction)
- [react-i18next Docs](https://react.i18next.com/)
- [Storybook Docs](https://storybook.js.org/docs/react/get-started/introduction)

### Internal Project Docs
- [Frontend Architecture Guide](../../../docs/architecture/frontend-architecture.md)
- [TypeScript Coding Standards](../../../docs/development/TYPESCRIPT_CODING_STANDARDS.md)
- [Spec Document](./spec.md)
- [Research Findings](./research.md)

### Getting Help
- Create an issue in the project repository
- Ask in the team Slack channel: #frontend-migration
- Review existing code in `src/main/frontend/react-app/`

---

**Guide Version**: 1.0
**Last Updated**: 2025-10-29
**Status**: ✅ Ready for Developers
