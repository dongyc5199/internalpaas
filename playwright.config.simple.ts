import { defineConfig, devices } from '@playwright/test';

/**
 * 简化的 Playwright 配置 - 用于快速测试图表功能
 * 不需要认证,直接访问页面
 */
export default defineConfig({
  testDir: './e2e/tests',
  testMatch: '**/admin-dashboard-charts.spec.ts',
  timeout: 60 * 1000,
  
  expect: {
    timeout: 10000,
  },

  retries: 0,
  workers: 1,

  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],

  use: {
    baseURL: 'http://localhost:9091',
    actionTimeout: 10000,
    navigationTimeout: 30000,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
    viewport: { width: 1280, height: 720 },
    ignoreHTTPSErrors: true,
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
