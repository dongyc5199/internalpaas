import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { Suspense } from 'react';
import { queryClient } from './shared/config/queryClient';
import { MainLayout, ProtectedRoute, Loading, ToastContainer } from './shared/components';
import { LoginPage, RegisterPage } from './features/auth';
import { InitialConfigPage } from './features/config';
import { ROUTES } from './shared/constants';
import { protectedRoutes } from './routes';
import type { AppRoute } from './routes/types';

const renderProtectedRoute = (route: AppRoute): React.JSX.Element => {
  const Component = route.component;
  const content = (
    <Suspense fallback={<Loading text={route.suspenseText ?? '加载中...'} />}>
      <Component />
    </Suspense>
  );

  return (
    <Route
      key={route.path}
      path={route.path}
      element={
        <ProtectedRoute requiredRoles={route.requiredRoles}>
          <MainLayout>{content}</MainLayout>
        </ProtectedRoute>
      }
    />
  );
};

function App(): React.JSX.Element {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter basename="/app">
        <Routes>
          <Route path={ROUTES.LOGIN} element={<LoginPage />} />
          <Route path={ROUTES.REGISTER} element={<RegisterPage />} />
          <Route path={ROUTES.INITIAL_CONFIG} element={<InitialConfigPage />} />
          {protectedRoutes.map(renderProtectedRoute)}
          <Route path="*" element={<Navigate to={ROUTES.HOME} replace />} />
        </Routes>
      </BrowserRouter>

      {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
      <ToastContainer />
    </QueryClientProvider>
  );
}

export default App;
