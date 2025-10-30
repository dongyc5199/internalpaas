/**
 * React Query Client Configuration
 *
 * Configured for optimal caching and retry behavior:
 * - staleTime: 5 minutes (data remains fresh)
 * - cacheTime: 10 minutes (cache persists after component unmount)
 * - retry: 1 attempt (avoid excessive retries on auth failures)
 */

import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Data is considered fresh for 5 minutes
      staleTime: 5 * 60 * 1000, // 5min

      // Cached data persists for 10 minutes after last usage
      gcTime: 10 * 60 * 1000, // 10min (renamed from cacheTime in v5)

      // Retry failed queries once before giving up
      retry: 1,

      // Don't refetch on window focus in development
      refetchOnWindowFocus: process.env.NODE_ENV === 'production',

      // Don't refetch on reconnect unless data is stale
      refetchOnReconnect: false,
    },
    mutations: {
      // Don't retry mutations automatically (user-initiated actions)
      retry: 0,
    },
  },
});
