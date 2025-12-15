import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuthStore } from './stores/authStore';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardLayout from './components/DashboardLayout';
import ProjectListPage from './pages/ProjectListPage';
import ProjectDetailPage from './pages/ProjectDetailPage';
import RepositoryListPage from './pages/RepositoryListPage';
import RepositoryDetailPage from './pages/RepositoryDetailPage';
import BuildListPage from './pages/BuildListPage';
import BuildDetailPage from './pages/BuildDetailPage';
import QualityReportListPage from './pages/QualityReportListPage';
import QualityReportDetailPage from './pages/QualityReportDetailPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route
            path="/"
            element={
              <PrivateRoute>
                <DashboardLayout />
              </PrivateRoute>
            }
          >
            <Route index element={<Navigate to="/projects" replace />} />
            <Route path="projects" element={<ProjectListPage />} />
            <Route path="projects/:id" element={<ProjectDetailPage />} />
            <Route path="repositories" element={<RepositoryListPage />} />
            <Route path="repositories/:id" element={<RepositoryDetailPage />} />
            <Route path="builds" element={<BuildListPage />} />
            <Route path="builds/:id" element={<BuildDetailPage />} />
            <Route path="quality" element={<QualityReportListPage />} />
            <Route path="quality-reports/:id" element={<QualityReportDetailPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
