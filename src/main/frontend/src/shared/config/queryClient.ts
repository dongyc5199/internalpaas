/**
 * React Query Client Configuration
 * Based on react-app/config/queryClient.ts
 */

import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,
      gcTime: 10 * 60 * 1000,
      retry: 1,
      refetchOnWindowFocus: import.meta.env.PROD,
      refetchOnReconnect: false,
    },
    mutations: {
      retry: 0,
    },
  },
});
