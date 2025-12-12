import { z } from 'zod';

/**
 * 将环境变量解析为类型安全的配置对象。
 * - 开发模式允许使用本地默认值
 * - 生产模式下缺失必填变量会直接抛错
 */
const envSchema = z.object({
  VITE_API_BASE_URL: z.string().optional(),
  VITE_WS_BASE_URL: z.string().optional(),
  VITE_ENV: z.string().optional(),
});

const parsed = envSchema.safeParse(import.meta.env);

if (!parsed.success) {
  const prettyErrors = parsed.error.issues.map((issue) => issue.message).join('; ');
  throw new Error(`环境变量解析失败: ${prettyErrors}`);
}

const { VITE_API_BASE_URL, VITE_WS_BASE_URL, VITE_ENV } = parsed.data;

const isProd = import.meta.env.PROD;

const withFallback = <T>(value: T | undefined, fallback: T, label: string): T => {
  if (value && String(value).trim().length > 0) {
    return value;
  }

  if (isProd) {
    throw new Error(`缺少必填环境变量: ${label}`);
  }

  return fallback;
};

export const env = {
  apiBaseUrl: withFallback(VITE_API_BASE_URL, 'http://localhost:8080', 'VITE_API_BASE_URL')
    .replace(/\/$/, ''),
  wsBaseUrl: withFallback(VITE_WS_BASE_URL, 'ws://localhost:8080/ws', 'VITE_WS_BASE_URL')
    .replace(/\/$/, ''),
  mode: VITE_ENV ?? import.meta.env.MODE,
};

export type EnvConfig = typeof env;
