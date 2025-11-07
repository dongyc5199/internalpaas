# React Migration Quickstart Guide

**Feature Branch**: `007-frontend-react-migration`
**Target Audience**: Frontend developers joining the migration effort

## Prerequisites

- **Node.js**: v18.x or higher ([Download](https://nodejs.org/))
- **npm**: v9.x or higher (comes with Node.js)
- **Java**: JDK 17+ (for running Spring Boot backend)
- **Git**: For version control
- **IDE**: VS Code recommended (with ESLint, Prettier, TypeScript extensions)

## Project Setup

### 1. Clone and Checkout Branch

```bash
git clone <repository-url>
cd internalpaas
git checkout 007-frontend-react-migration
```

### 2. Install Dependencies

```bash
# Install backend dependencies
./mvnw clean install -DskipTests

# Install frontend dependencies
cd src/main/frontend
npm install
```

### 3. Start Development Servers

**Terminal 1 - Backend (Spring Boot)**:
```bash
# From project root
./mvnw spring-boot:run

# Wait for: "Started InternalpaasApplication in X seconds"
# Backend will run on http://localhost:8080
```

**Terminal 2 - Frontend (Vite)**:
```bash
cd src/main/frontend
npm run dev

# Frontend dev server will run on http://localhost:5173
# Proxies API calls to http://localhost:8080
```

### 4. Access Application

- **React Dev Server**: http://localhost:5173 (with HMR)
- **Spring Boot**: http://localhost:8080 (serves production build + APIs)
- **H2 Console**: http://localhost:8080/h2-console

**Default Credentials**:
- Username: `admin`
- Password: `admin`

---

## Project Structure

```
internalpaas/
├── src/main/
│   ├── java/                     # Spring Boot backend
│   ├── resources/
│   │   ├── static/dist/          # Production React build output
│   │   ├── templates/            # Legacy Thymeleaf templates
│   │   └── application.properties
│   └── frontend/                 # React SPA source
│       ├── src/
│       │   ├── features/         # Feature modules
│       │   │   ├── admin/        # Admin features
│       │   │   ├── monitoring/   # Monitoring features
│       │   │   ├── terminal/     # SSH terminal
│       │   │   └── profile/      # User profile
│       │   ├── shared/           # Shared code
│       │   │   ├── components/   # Reusable UI components
│       │   │   ├── hooks/        # Custom hooks
│       │   │   ├── stores/       # Zustand stores
│       │   │   └── utils/        # Utility functions
│       │   ├── layouts/          # Layout components
│       │   ├── types/            # TypeScript types
│       │   ├── App.tsx           # Root component
│       │   ├── main.tsx          # Entry point
│       │   └── index.css         # Global styles
│       ├── public/               # Static assets
│       ├── tests/                # Test files
│       ├── package.json
│       ├── tsconfig.json
│       ├── vite.config.ts
│       └── vitest.config.ts
└── specs/007-frontend-react-migration/  # Feature specs
    ├── spec.md
    ├── plan.md
    ├── research.md
    ├── data-model.md
    ├── contracts/
    └── quickstart.md (this file)
```

---

## Development Workflow

### Hot Module Replacement (HMR)

Vite provides instant feedback on code changes:

1. Edit any `.tsx`, `.ts`, or `.css` file
2. Save file (Ctrl+S / Cmd+S)
3. Browser updates instantly without full reload
4. React component state preserved

**Example**:
```typescript
// src/features/admin/servers/components/ServerCard.tsx
export const ServerCard: React.FC<Props> = ({ server }) => {
  return (
    <div className={styles.card}>
      <h3>{server.name}</h3>
      {/* Edit and save - see changes immediately */}
    </div>
  );
};
```

### API Proxy

Vite proxies API calls to Spring Boot:

```typescript
// vite.config.ts
export default defineConfig({
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

**Usage**:
```typescript
// Frontend calls /api/servers
// Vite forwards to http://localhost:8080/api/servers
const response = await fetch('/api/servers');
```

---

## Key Technologies

### Core Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18.x | UI framework |
| TypeScript | 5.x | Type safety |
| Vite | 5.x | Build tool & dev server |
| React Router | 6.x | Client-side routing |
| TanStack Query | 5.x | Data fetching & caching |
| Zustand | 4.x | State management |
| React Hook Form | 7.x | Form handling |
| Zod | 3.x | Schema validation |
| ECharts | 5.x | Data visualization |
| STOMP.js | 7.x | WebSocket communication |

### Development Tools

| Tool | Purpose |
|------|---------|
| ESLint | Code linting |
| Prettier | Code formatting |
| Vitest | Unit testing |
| React Testing Library | Component testing |
| MSW | API mocking |
| TypeScript | Type checking |

---

## Common Tasks

### 1. Create New Feature Component

```bash
# Feature-based structure
cd src/main/frontend/src/features/admin/servers/components
touch ServerForm.tsx ServerForm.module.css ServerForm.test.tsx
```

**ServerForm.tsx**:
```typescript
import styles from './ServerForm.module.css';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { serverSchema } from '../schemas';

export const ServerForm: React.FC<Props> = ({ onSubmit }) => {
  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(serverSchema),
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className={styles.form}>
      <input {...register('name')} placeholder="Server name" />
      {errors.name && <span className={styles.error}>{errors.name.message}</span>}
      {/* More fields */}
      <button type="submit">Create Server</button>
    </form>
  );
};
```

### 2. Add API Hook

```typescript
// features/admin/servers/hooks/useServers.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { serverApi } from '../api/serverApi';

export const useServers = () =>
  useQuery({
    queryKey: ['servers'],
    queryFn: serverApi.getAll,
  });

export const useCreateServer = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: serverApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['servers'] }),
  });
};
```

### 3. Add WebSocket Integration

```typescript
// features/monitoring/hooks/useServerMetrics.ts
import { useWebSocket } from '@/shared/hooks/useWebSocket';

export const useServerMetrics = (serverId: number) => {
  const { message, status } = useWebSocket<ServerMetrics>(
    `/topic/server-status/${serverId}`
  );

  return {
    metrics: message,
    isConnected: status === 'connected',
  };
};
```

### 4. Run Tests

```bash
# Unit tests
npm run test

# Watch mode
npm run test:watch

# Coverage
npm run test:coverage

# Specific file
npm run test ServerCard.test.tsx
```

### 5. Type Checking

```bash
# Check types
npm run type-check

# Watch mode
npm run type-check:watch
```

### 6. Linting & Formatting

```bash
# Lint code
npm run lint

# Fix lint errors
npm run lint:fix

# Format code
npm run format
```

---

## Debugging

### VS Code Launch Configuration

Create `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "chrome",
      "request": "launch",
      "name": "Launch Chrome against localhost",
      "url": "http://localhost:5173",
      "webRoot": "${workspaceFolder}/src/main/frontend/src",
      "sourceMapPathOverrides": {
        "webpack:///src/*": "${webRoot}/*"
      }
    }
  ]
}
```

### React DevTools

Install React DevTools browser extension:
- [Chrome](https://chrome.google.com/webstore/detail/react-developer-tools)
- [Firefox](https://addons.mozilla.org/en-US/firefox/addon/react-devtools/)

### TanStack Query DevTools

Already included in development builds:

```typescript
// App.tsx
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';

<QueryClientProvider client={queryClient}>
  <App />
  <ReactQueryDevtools initialIsOpen={false} />
</QueryClientProvider>
```

Access at: http://localhost:5173 (floating button bottom-left)

### Zustand DevTools

```typescript
// stores/authStore.ts
import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

export const useAuthStore = create<AuthState>()(
  devtools(
    (set) => ({ /* state */ }),
    { name: 'AuthStore' }
  )
);
```

---

## Building for Production

### Frontend Only

```bash
cd src/main/frontend
npm run build

# Output: src/main/resources/static/dist/
```

### Full Application

```bash
# From project root
./mvnw clean package

# Output: target/internalpaas-*.jar
# Includes React production build
```

### Production JAR

```bash
java -jar target/internalpaas-*.jar

# Access at: http://localhost:8080
# Serves React SPA + backend APIs
```

---

## Troubleshooting

### Issue: Port Already in Use

**Backend (8080)**:
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

**Frontend (5173)**:
```bash
# Kill Vite dev server
pkill -f vite
```

### Issue: Dependencies Not Installed

```bash
# Clean and reinstall
cd src/main/frontend
rm -rf node_modules package-lock.json
npm install
```

### Issue: TypeScript Errors

```bash
# Restart TypeScript server in VS Code
Ctrl+Shift+P → "TypeScript: Restart TS Server"

# Check tsconfig.json is correct
npm run type-check
```

### Issue: WebSocket Connection Failed

1. Ensure Spring Boot backend is running
2. Check CORS configuration in SecurityConfig.java
3. Verify session cookie is present (check DevTools → Application → Cookies)
4. Check browser console for STOMP errors

### Issue: API 401 Unauthorized

1. Login first at http://localhost:5173/login
2. Check session cookie exists
3. Verify Spring Security config allows endpoint access
4. Check JSESSIONID cookie domain matches

---

## Code Style Guide

### Naming Conventions

- **Components**: PascalCase (`ServerCard.tsx`)
- **Hooks**: camelCase with `use` prefix (`useServers.ts`)
- **Utils**: camelCase (`formatDate.ts`)
- **Constants**: UPPER_SNAKE_CASE (`API_BASE_URL`)
- **CSS Modules**: camelCase (`styles.cardHeader`)

### File Structure

```typescript
// 1. Imports
import React from 'react';
import { useQuery } from '@tanstack/react-query';
import styles from './Component.module.css';
import type { Props } from './types';

// 2. Types
interface ComponentProps {
  title: string;
  onAction: () => void;
}

// 3. Component
export const Component: React.FC<ComponentProps> = ({ title, onAction }) => {
  // 4. Hooks
  const { data, isLoading } = useQuery({ /* ... */ });
  const [state, setState] = useState();

  // 5. Effects
  useEffect(() => {
    // side effects
  }, []);

  // 6. Handlers
  const handleClick = () => {
    onAction();
  };

  // 7. Render
  if (isLoading) return <Loading />;

  return (
    <div className={styles.container}>
      <h2>{title}</h2>
      <button onClick={handleClick}>Action</button>
    </div>
  );
};
```

---

## Resources

### Documentation

- [React Docs](https://react.dev/)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [Vite Guide](https://vitejs.dev/guide/)
- [TanStack Query](https://tanstack.com/query/latest/docs/react/overview)
- [React Router](https://reactrouter.com/en/main)
- [Zustand](https://docs.pmnd.rs/zustand)

### Internal Docs

- [Spec Document](./spec.md)
- [Research Decisions](./research.md)
- [Data Model](./data-model.md)
- [API Contracts](./contracts/)

### Team Communication

- **Questions**: Ask in team chat or create GitHub issue
- **Bug Reports**: Use issue template with reproduction steps
- **Feature Requests**: Discuss in planning meetings

---

## Next Steps

1. **Read the spec**: Understand user stories and requirements
2. **Review research decisions**: Know the tech stack rationale
3. **Explore codebase**: Navigate feature folders
4. **Run tests**: Ensure environment is working
5. **Pick a task**: Check tasks.md (once generated) for available work
6. **Create PR**: Follow PR template and get code review

---

**Happy Coding!** 🚀
