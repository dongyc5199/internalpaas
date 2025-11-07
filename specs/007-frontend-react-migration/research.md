# Research Document: Frontend React Migration

**Branch**: `007-frontend-react-migration`
**Date**: 2025-01-04
**Phase**: Phase 0 - Research & Technical Decisions

## Overview

This document consolidates research findings and technical decisions for migrating the Dev Debug Platform's hybrid frontend (Thymeleaf + vanilla JS) to a unified React SPA architecture.

## Research Areas

### 1. State Management Solution

**Decision**: **Zustand**

**Rationale**:
- Lightweight (< 1KB) compared to Redux (~6KB)
- Simpler mental model than Redux - no boilerplate (actions, reducers, dispatchers)
- Built-in TypeScript support with excellent type inference
- No Context Provider wrapper required - avoids React Context performance issues
- Supports middleware for persistence (localStorage), devtools
- Easier learning curve for team members new to React state management

**Alternatives Considered**:

| Solution | Pros | Cons | Why Rejected |
|----------|------|------|--------------|
| **Redux** | Industry standard, extensive ecosystem, DevTools | Heavy boilerplate, steep learning curve, overkill for medium apps | Too complex for current team size; maintenance overhead not justified |
| **React Context** | Built-in, no dependencies | Performance issues with frequent updates, re-render cascade | Unsuitable for high-frequency updates (WebSocket metrics); causes unnecessary re-renders |
| **Jotai** | Atomic state, minimal boilerplate | Less mature ecosystem, smaller community | Zustand provides similar benefits with larger community support |
| **MobX** | Automatic reactivity, simple API | Magic observables harder to debug, reactive paradigm unfamiliar | Team prefers explicit state updates over reactive magic |

**Implementation Pattern**:
```typescript
// stores/authStore.ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  login: (credentials: Credentials) => Promise<void>;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      isAuthenticated: false,
      login: async (credentials) => {
        const user = await authApi.login(credentials);
        set({ user, isAuthenticated: true });
      },
      logout: () => set({ user: null, isAuthenticated: false }),
    }),
    { name: 'auth-storage' }
  )
);
```

---

### 2. Routing Solution

**Decision**: **React Router v6**

**Rationale**:
- De facto standard for React SPAs (99% of React projects use it)
- Excellent TypeScript support with typed route params
- Data loading patterns align with modern React (Suspense boundaries)
- Nested routes support complex layout hierarchies (admin, monitoring sections)
- Browser history API integration for proper back/forward button behavior
- Built-in navigation guards for protected routes

**Alternatives Considered**:

| Solution | Pros | Cons | Why Rejected |
|----------|------|------|--------------|
| **TanStack Router** | Type-safe routes, better TypeScript DX | New, smaller ecosystem, learning curve | Too new for production (released 2023); limited team experience |
| **Wouter** | Minimal (< 2KB), fast | Limited features, no nested routes | Lacks nested route support needed for admin/monitoring hierarchy |
| **Custom routing** | Full control, tailored solution | High maintenance, reinventing wheel | Not worth development/maintenance cost vs proven solution |

**Implementation Pattern**:
```typescript
// App.tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './components/ProtectedRoute';

<BrowserRouter>
  <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route element={<ProtectedRoute />}>
      <Route path="/" element={<MainLayout />}>
        <Route path="admin" element={<AdminLayout />}>
          <Route path="servers" element={<ServersPage />} />
          <Route path="users" element={<UsersPage />} />
        </Route>
        <Route path="monitoring" element={<MonitoringLayout />}>
          <Route path="dashboard" element={<MonitoringDashboard />} />
          <Route path="history" element={<HistoryPage />} />
        </Route>
      </Route>
    </Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
</BrowserRouter>
```

---

### 3. Data Fetching & API Client

**Decision**: **TanStack Query (React Query) v5 + Axios**

**Rationale**:
- **React Query** handles caching, background refetching, stale-while-revalidate patterns automatically
- Built-in optimistic updates with automatic rollback on failure
- Request deduplication prevents redundant network calls
- Excellent TypeScript support with generics for type-safe API responses
- DevTools for debugging cache state and query timelines
- **Axios** provides interceptors for authentication headers, error handling, request/response transformation

**Alternatives Considered**:

| Solution | Pros | Cons | Why Rejected |
|----------|------|------|--------------|
| **SWR** | Similar features, Vercel-backed | Less flexible cache control, smaller community | React Query has more granular cache control and larger ecosystem |
| **RTK Query** | Redux integration | Requires Redux, heavier bundle | Not using Redux; unnecessary complexity |
| **fetch + custom hooks** | No dependencies, full control | Must implement caching, retries, deduplication manually | Too much boilerplate; reinventing React Query functionality |

**Implementation Pattern**:
```typescript
// api/serverApi.ts
import axios from 'axios';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

const apiClient = axios.create({
  baseURL: '/api',
  withCredentials: true,
});

// Query hook
export const useServers = () =>
  useQuery({
    queryKey: ['servers'],
    queryFn: () => apiClient.get<Server[]>('/admin/servers').then(res => res.data),
    staleTime: 30000, // 30 seconds
    refetchInterval: 60000, // Background refetch every minute
  });

// Mutation hook with optimistic update
export const useDeleteServer = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => apiClient.delete(`/admin/servers/${id}`),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: ['servers'] });
      const previousServers = queryClient.getQueryData<Server[]>(['servers']);
      queryClient.setQueryData<Server[]>(['servers'], (old) =>
        old?.filter(s => s.id !== id)
      );
      return { previousServers };
    },
    onError: (err, id, context) => {
      queryClient.setQueryData(['servers'], context?.previousServers);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['servers'] });
    },
  });
};
```

---

### 4. Chart Library

**Decision**: **Apache ECharts (with echarts-for-react wrapper)**

**Rationale**:
- Most comprehensive feature set (50+ chart types including custom visualizations)
- Excellent performance with Canvas rendering (handles 1M+ data points)
- Built-in data zoom, pan, tooltip, legend interactions
- Export to PNG/SVG/PDF out-of-the-box
- Large community, active development (Apache Foundation project)
- Existing usage in system (already in dependencies for some features)
- Supports real-time streaming data updates efficiently

**Alternatives Considered**:

| Solution | Pros | Cons | Why Rejected |
|----------|------|------|--------------|
| **Recharts** | React-native API, composable components | Limited chart types, poor performance > 1000 points | Inadequate for large metric datasets (>10k points); SVG rendering too slow |
| **Chart.js** | Simple API, lightweight | Limited customization, basic interactivity | Lacks advanced features (data zoom, custom tooltips, complex legends) |
| **Victory** | React-first, accessible | Heavy bundle (>200KB), slow rendering | Bundle size too large; performance worse than ECharts |
| **D3.js (custom)** | Ultimate flexibility, fine-grained control | Steep learning curve, high maintenance | Would require rewriting existing charts; not worth cost vs pre-built solution |

**Implementation Pattern**:
```typescript
// components/MetricsChart.tsx
import ReactECharts from 'echarts-for-react';

export const MetricsChart: React.FC<Props> = ({ data, metric }) => {
  const option = {
    title: { text: `${metric.name} Over Time` },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'time' },
    yAxis: { type: 'value', name: metric.unit },
    dataZoom: [{ type: 'inside' }, { type: 'slider' }],
    series: [{
      name: metric.name,
      type: 'line',
      data: data.map(d => [d.timestamp, d.value]),
      smooth: true,
    }],
  };

  return <ReactECharts option={option} style={{ height: '400px' }} />;
};
```

---

### 5. Form Management

**Decision**: **React Hook Form v7**

**Rationale**:
- Best performance - uncontrolled inputs minimize re-renders
- Built-in validation with Zod/Yup schema support
- Smallest bundle size among form libraries (~9KB)
- Excellent TypeScript inference from schemas
- Easy integration with custom UI components
- Built-in field arrays for dynamic forms (e.g., server port configurations)

**Alternatives Considered**:

| Solution | Pros | Cons | Why Rejected |
|----------|------|------|--------------|
| **Formik** | Mature, popular, familiar API | Controlled inputs cause re-renders, heavier bundle (~15KB) | Performance issues with large forms; React Hook Form faster |
| **Final Form** | Subscription-based rendering, performant | Smaller ecosystem, less TypeScript support | Weaker TypeScript integration; React Hook Form more popular |
| **Uncontrolled forms** | Native, zero dependencies | Manual validation, state management complex | Too much boilerplate for complex multi-step forms |

**Implementation Pattern**:
```typescript
// components/ServerForm.tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const serverSchema = z.object({
  name: z.string().min(3, 'Name must be at least 3 characters'),
  host: z.string().regex(/^[\w.-]+$/, 'Invalid hostname'),
  port: z.number().min(1).max(65535),
  username: z.string().min(1),
});

type ServerFormData = z.infer<typeof serverSchema>;

export const ServerForm: React.FC = () => {
  const { register, handleSubmit, formState: { errors } } = useForm<ServerFormData>({
    resolver: zodResolver(serverSchema),
  });

  const onSubmit = (data: ServerFormData) => {
    // Submit logic
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('name')} />
      {errors.name && <span>{errors.name.message}</span>}
      {/* Other fields */}
    </form>
  );
};
```

---

### 6. WebSocket Integration

**Decision**: **Custom React hooks wrapping STOMP.js client**

**Rationale**:
- STOMP protocol already used by backend (Spring WebSocket with STOMP)
- Custom hooks encapsulate connection lifecycle (connect, subscribe, unsubscribe, reconnect)
- useEffect cleanup ensures proper unsubscription on unmount
- Reconnection logic with exponential backoff built into hook
- Type-safe message handling with TypeScript generics

**Alternatives Considered**:

| Solution | Pros | Cons | Why Rejected |
|----------|------|------|--------------|
| **Socket.IO** | Automatic reconnection, fallback transports | Would require backend changes; different protocol | Backend uses STOMP; migration not justified |
| **Native WebSocket** | No dependencies, standard API | Manual STOMP frame parsing, no reconnection logic | STOMP protocol abstraction needed; custom implementation too complex |
| **SockJS** | Browser compatibility fallback | Overhead for modern browsers, less used today | Target browsers support native WebSocket; fallback unnecessary |

**Implementation Pattern**:
```typescript
// hooks/useWebSocket.ts
import { useEffect, useRef, useState } from 'zustand';
import { Client, StompSubscription } from '@stomp/stompjs';

export const useWebSocket = <T>(topic: string) => {
  const [message, setMessage] = useState<T | null>(null);
  const [status, setStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting');
  const clientRef = useRef<Client>();
  const subscriptionRef = useRef<StompSubscription>();

  useEffect(() => {
    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      reconnectDelay: 5000,
      onConnect: () => {
        setStatus('connected');
        subscriptionRef.current = client.subscribe(topic, (msg) => {
          setMessage(JSON.parse(msg.body));
        });
      },
      onDisconnect: () => setStatus('disconnected'),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      subscriptionRef.current?.unsubscribe();
      client.deactivate();
    };
  }, [topic]);

  return { message, status };
};
```

---

### 7. CSS Strategy

**Decision**: **CSS Modules**

**Rationale**:
- Scoped styles by default - prevents global namespace pollution
- No runtime overhead (unlike CSS-in-JS solutions)
- Works with existing CSS design system (--shell-* custom properties)
- Standard CSS syntax - no learning curve for team
- Vite has built-in support - no additional configuration
- Easy migration path from existing global CSS files

**Alternatives Considered**:

| Solution | Pros | Cons | Why Rejected |
|----------|------|------|--------------|
| **Styled Components** | Dynamic styles, props-based styling | Runtime cost, Flash of Unstyled Content (FOUC), heavier bundle | Performance overhead not worth benefits; prefer static CSS |
| **Tailwind CSS** | Utility-first, rapid development | Would require complete design system rewrite, large class names | Existing --shell-* design system already established; migration too costly |
| **Emotion** | Better performance than Styled Components | Still has runtime cost, CSS-in-JS complexity | Same issues as Styled Components; prefer zero-runtime solution |
| **Global CSS** | Simple, familiar | Naming collisions, specificity wars, hard to maintain at scale | Current pain point we're trying to solve |

**Implementation Pattern**:
```typescript
// components/ServerCard/ServerCard.module.css
.card {
  background: var(--shell-surface);
  border: 1px solid var(--shell-border);
  border-radius: 0.5rem;
  padding: 1rem;
}

.card:hover {
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.title {
  color: var(--shell-text-primary);
  font-size: 1.125rem;
  font-weight: 600;
}

// components/ServerCard/ServerCard.tsx
import styles from './ServerCard.module.css';

export const ServerCard: React.FC<Props> = ({ server }) => (
  <div className={styles.card}>
    <h3 className={styles.title}>{server.name}</h3>
  </div>
);
```

---

### 8. Build Tool

**Decision**: **Vite 5**

**Rationale**:
- Lightning-fast dev server with native ES modules (no bundling in dev)
- Hot Module Replacement (HMR) faster than Webpack (~50ms vs ~500ms)
- Production builds use Rollup - excellent tree-shaking, small bundles
- Built-in TypeScript support - no additional configuration
- CSS Modules, JSON imports, static asset handling out-of-the-box
- Maven integration straightforward via exec plugin

**Alternatives Considered**:

| Solution | Pros | Cons | Why Rejected |
|----------|------|------|--------------|
| **Webpack 5** | Mature, extensive plugin ecosystem | Slow dev server (HMR ~500ms), complex configuration | Development experience significantly worse than Vite |
| **Rollup** | Excellent production bundles | Poor dev server experience, requires additional setup | Vite uses Rollup for production; no need for standalone Rollup |
| **esbuild** | Fastest builds, minimal config | Less mature plugin ecosystem, limited CSS Modules support | Vite uses esbuild for dep pre-bundling; gets benefits without drawbacks |
| **Parcel** | Zero-config, fast | Less control over output, smaller community | Vite provides similar simplicity with more flexibility |

**Implementation Pattern**:
```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    outDir: '../resources/static/dist',
    emptyOutDir: true,
    sourcemap: process.env.NODE_ENV === 'development',
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
    },
  },
});
```

**Maven Integration**:
```xml
<!-- pom.xml -->
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>frontend-build</id>
      <phase>prepare-package</phase>
      <goals><goal>exec</goal></goals>
      <configuration>
        <workingDirectory>${project.basedir}/src/main/frontend</workingDirectory>
        <executable>npm</executable>
        <arguments>
          <argument>run</argument>
          <argument>build</argument>
        </arguments>
      </configuration>
    </execution>
  </executions>
</plugin>
```

---

### 9. Internationalization (i18n)

**Decision**: **react-i18next v14**

**Rationale**:
- Most popular i18n library for React (7M+ weekly downloads)
- Supports dynamic language switching without page reload
- Nested translation keys, pluralization, interpolation
- Lazy loading of translation files (only load current language)
- TypeScript support with type-safe translation keys
- Backend integration via i18next-http-backend for loading translations from API

**Alternatives Considered**:

| Solution | Pros | Cons | Why Rejected |
|----------|------|------|--------------|
| **FormatJS (react-intl)** | ICU message format, Facebook-backed | More complex API, larger bundle | Overkill for current needs; simpler API preferred |
| **LinguiJS** | Compile-time optimization, lightweight | Less mature, smaller ecosystem | react-i18next more proven, better community support |
| **Custom solution** | Full control, minimal dependencies | Must implement pluralization, interpolation, loading logic | Too much functionality to reimplement; not worth effort |

**Implementation Pattern**:
```typescript
// i18n/config.ts
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: require('./locales/en.json') },
      zh: { translation: require('./locales/zh.json') },
    },
    fallbackLng: 'en',
    interpolation: { escapeValue: false },
  });

// Component usage
import { useTranslation } from 'react-i18next';

export const WelcomeMessage: React.FC = () => {
  const { t, i18n } = useTranslation();
  return (
    <div>
      <h1>{t('welcome.title')}</h1>
      <p>{t('welcome.description', { userName: 'Admin' })}</p>
      <button onClick={() => i18n.changeLanguage('zh')}>中文</button>
    </div>
  );
};
```

---

### 10. Testing Strategy

**Decision**: **Vitest + React Testing Library + MSW**

**Rationale**:
- **Vitest**: Vite-native test runner - same config, same transforms, ~10x faster than Jest
- **React Testing Library**: Tests user behavior, not implementation details; encourages accessible components
- **MSW (Mock Service Worker)**: Intercepts network requests in tests, same handlers for dev/test

**Alternatives Considered**:

| Solution | Pros | Cons | Why Rejected |
|----------|------|------|--------------|
| **Jest + Enzyme** | Industry standard, mature | Slow with Vite setup, Enzyme tests implementation details | Vitest faster; Enzyme encourages brittle tests |
| **Cypress Component Testing** | Real browser, great DX | Slower than Vitest, heavier setup | Unit tests don't need real browser; use Cypress for E2E only |
| **uvu + happy-dom** | Minimal, fast | Less React-specific tooling, smaller ecosystem | Vitest provides better React integration |

**Implementation Pattern**:
```typescript
// components/ServerCard.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ServerCard } from './ServerCard';

describe('ServerCard', () => {
  it('renders server name and status', () => {
    const server = { id: 1, name: 'prod-server', status: 'online' };
    render(<ServerCard server={server} />);

    expect(screen.getByText('prod-server')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('online');
  });
});

// api/serverApi.test.ts (using MSW)
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

const server = setupServer(
  http.get('/api/admin/servers', () => {
    return HttpResponse.json([{ id: 1, name: 'test-server' }]);
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

---

## Technology Stack Summary

| Category | Technology | Version | Rationale Summary |
|----------|-----------|---------|-------------------|
| **UI Framework** | React | 18.x | Already used in Deploy Platform; team familiarity |
| **Language** | TypeScript | 5.x | Type safety, better tooling, team standard |
| **State Management** | Zustand | 4.x | Lightweight, simple API, great TypeScript support |
| **Routing** | React Router | 6.x | De facto standard, nested routes, type-safe params |
| **Data Fetching** | TanStack Query + Axios | 5.x / 1.x | Best-in-class caching, optimistic updates, interceptors |
| **Charts** | Apache ECharts | 5.x | Best performance, rich features, already in use |
| **Forms** | React Hook Form + Zod | 7.x / 3.x | Performant uncontrolled forms, type-safe validation |
| **WebSocket** | Custom hooks + STOMP.js | Custom / 7.x | Backend compatibility, lifecycle management |
| **Styling** | CSS Modules | N/A | Zero-runtime, scoped styles, existing design system |
| **Build Tool** | Vite | 5.x | Fast HMR, excellent DX, Rollup production builds |
| **i18n** | react-i18next | 14.x | Most popular, dynamic switching, TypeScript support |
| **Testing** | Vitest + RTL + MSW | 1.x / 14.x / 2.x | Fast, Vite-native, behavior-focused, network mocking |
| **Component Lib** | Custom (w/ existing deploy-platform components) | N/A | Reuse existing Button, Input, Modal, Table components |

---

## Architecture Patterns

### 1. Feature-Based Folder Structure

**Decision**: Organize by feature, not by technical layer

**Structure**:
```
src/
├── features/
│   ├── admin/
│   │   ├── servers/
│   │   │   ├── components/
│   │   │   │   ├── ServerCard.tsx
│   │   │   │   ├── ServerForm.tsx
│   │   │   │   └── ServerList.tsx
│   │   │   ├── hooks/
│   │   │   │   └── useServers.ts
│   │   │   ├── api/
│   │   │   │   └── serverApi.ts
│   │   │   └── types/
│   │   │       └── server.ts
│   │   └── users/
│   ├── monitoring/
│   ├── terminal/
│   └── profile/
├── shared/
│   ├── components/  # Reusable UI components
│   ├── hooks/       # useAuth, useTheme, etc.
│   ├── stores/      # Zustand stores
│   └── utils/
├── layouts/
│   ├── MainLayout.tsx
│   ├── AdminLayout.tsx
│   └── EmptyLayout.tsx
└── App.tsx
```

**Rationale**:
- Co-locates related files (components, hooks, API calls, types)
- Easier to find and modify feature code
- Better modularity - can extract feature as package if needed
- Reduces merge conflicts (different features in different folders)

---

### 2. Custom Hook Pattern for API Calls

**Pattern**: Every API endpoint gets a custom hook

```typescript
// features/admin/servers/hooks/useServers.ts
export const useServers = () => useQuery({
  queryKey: ['servers'],
  queryFn: serverApi.getAll,
});

export const useServer = (id: number) => useQuery({
  queryKey: ['servers', id],
  queryFn: () => serverApi.getById(id),
});

export const useCreateServer = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: serverApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['servers'] }),
  });
};
```

**Benefits**:
- Centralized query key management
- Easy to add loading/error states
- Automatic caching and refetching
- Testable in isolation

---

### 3. Protected Route Pattern

**Pattern**: Higher-order component wrapping authenticated routes

```typescript
// components/ProtectedRoute.tsx
export const ProtectedRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuthStore();
  const location = useLocation();

  if (isLoading) return <LoadingSpinner />;

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
};

// App.tsx
<Routes>
  <Route path="/login" element={<LoginPage />} />
  <Route element={<ProtectedRoute />}>
    <Route path="/" element={<MainLayout />}>
      {/* All protected routes */}
    </Route>
  </Route>
</Routes>
```

---

### 4. Error Boundary Pattern

**Pattern**: React error boundaries at route level

```typescript
// components/ErrorBoundary.tsx
export class ErrorBoundary extends React.Component<Props, State> {
  state = { hasError: false, error: null };

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
    // Optional: Send to error tracking service
  }

  render() {
    if (this.state.hasError) {
      return <ErrorFallback error={this.state.error} />;
    }
    return this.props.children;
  }
}

// App.tsx
<Route path="/admin" element={
  <ErrorBoundary><AdminLayout /></ErrorBoundary>
} />
```

---

## Performance Optimizations

### 1. Code Splitting Strategy

- **Route-level splitting**: Lazy load each feature module
  ```typescript
  const AdminServers = lazy(() => import('./features/admin/servers'));
  const Monitoring = lazy(() => import('./features/monitoring'));
  ```

- **Component-level splitting**: Lazy load heavy components (e.g., Monaco editor, chart library)
  ```typescript
  const MonacoEditor = lazy(() => import('./components/MonacoEditor'));
  ```

### 2. List Virtualization

- Use `react-window` for long lists (> 100 items)
- Render only visible items + buffer
- Reduces DOM nodes, improves scroll performance

### 3. Memoization Strategy

- `React.memo` for pure presentational components
- `useMemo` for expensive calculations
- `useCallback` for callbacks passed to memoized children

---

## Migration Approach

### Phase-by-Phase Strategy

1. **Phase 1 (Foundation)**:
   - Setup Vite build in `src/main/frontend`
   - Create React shell with navigation
   - Establish routing structure
   - Build component library

2. **Phase 2 (Admin Module)**:
   - Migrate server management (most complex)
   - Establish API patterns with React Query
   - Implement real-time updates with WebSocket hooks
   - Validate migration patterns

3. **Phase 3-6**: Sequential migration of remaining modules following established patterns

### Backward Compatibility Strategy

- **Dual-mode routing**: Both Thymeleaf and React routes active during migration
- **Feature flags**: Toggle between legacy/new UI per feature
- **Gradual rollout**: Users opt-in to new UI (beta program)
- **Fallback mechanism**: Link to legacy version if issues found

---

## Open Questions / Future Decisions

1. **Component Library**:
   - **Option A**: Extend existing deploy-platform components
   - **Option B**: Adopt Ant Design or Material-UI
   - **Recommendation**: Option A (consistency, smaller bundle, no learning curve)

2. **E2E Testing**:
   - **Option A**: Playwright (faster, better API)
   - **Option B**: Cypress (more popular, better docs)
   - **Recommendation**: Playwright (better TypeScript support, faster execution)

3. **Deployment Strategy**:
   - **Option A**: Big bang (deploy all at once after completion)
   - **Option B**: Incremental (deploy each phase separately)
   - **Recommendation**: Option B (lower risk, faster feedback)

---

## References

- [React Query Documentation](https://tanstack.com/query/latest)
- [Zustand Documentation](https://docs.pmnd.rs/zustand)
- [ECharts Documentation](https://echarts.apache.org/en/index.html)
- [Vite Guide](https://vitejs.dev/guide/)
- [React Router v6 Migration Guide](https://reactrouter.com/en/main/upgrading/v5)
- [TypeScript React Cheatsheet](https://react-typescript-cheatsheet.netlify.app/)

---

**Phase 0 Complete**: All technology decisions documented. Ready for Phase 1 (Data Model & Contracts).
