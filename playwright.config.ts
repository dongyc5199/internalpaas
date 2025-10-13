import { defineConfig, devices } from '@playwright/test';
import * as dotenv from 'dotenv';
import * as path from 'path';

// 加载.env.e2e环境变量
dotenv.config({ path: path.resolve(__dirname, '.env.e2e') });

/**
 * Playwright E2E测试配置
 *
 * 用于测试Dev Debug Platform的端到端功能
 */
export default defineConfig({
  // 测试目录
  testDir: './e2e',

  // 测试匹配模式
  testMatch: '**/*.spec.ts',

  // 最大失败数，超过后停止测试
  maxFailures: 10,

  // 全局超时时间（60秒）
  timeout: 60 * 1000,

  // 每个测试期望的超时时间（10秒）
  expect: {
    timeout: 10000,
  },

  // 失败重试次数
  retries: process.env.CI ? 2 : 0,

  // 并行worker数量
  workers: process.env.CI ? 1 : undefined,

  // 报告配置
  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['json', { outputFile: 'playwright-report/results.json' }],
    ['junit', { outputFile: 'playwright-report/junit.xml' }],
    ['list']
  ],

  // 全局使用配置
  use: {
    // 基础URL
    baseURL: process.env.BASE_URL || 'http://localhost:9090',

    // 操作超时时间
    actionTimeout: 10000,

    // 导航超时时间
    navigationTimeout: 30000,

    // 截图设置
    screenshot: 'only-on-failure',

    // 视频录制
    video: 'retain-on-failure',

    // 追踪
    trace: 'retain-on-failure',

    // 浏览器选项
    viewport: { width: 1280, height: 720 },

    // 忽略HTTPS错误
    ignoreHTTPSErrors: true,

    // 区域设置
    locale: 'zh-CN',

    // 时区
    timezoneId: 'Asia/Shanghai',
  },

  // 浏览器配置项目
  projects: [
    // Setup项目 - 用于登录等预备操作
    {
      name: 'setup',
      testMatch: /global\.setup\.ts/,
    },

    // Chromium浏览器测试
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // 使用setup项目创建的认证状态
        storageState: 'e2e/.auth/user.json',
      },
      dependencies: ['setup'],
    },

    // Firefox浏览器测试（可选）
    // {
    //   name: 'firefox',
    //   use: {
    //     ...devices['Desktop Firefox'],
    //     storageState: 'e2e/.auth/user.json',
    //   },
    //   dependencies: ['setup'],
    // },

    // WebKit浏览器测试（可选）
    // {
    //   name: 'webkit',
    //   use: {
    //     ...devices['Desktop Safari'],
    //     storageState: 'e2e/.auth/user.json',
    //   },
    //   dependencies: ['setup'],
    // },

    // 移动端Chrome模拟（可选）
    // {
    //   name: 'Mobile Chrome',
    //   use: {
    //     ...devices['Pixel 5'],
    //     storageState: 'e2e/.auth/user.json',
    //   },
    //   dependencies: ['setup'],
    // },
  ],

  // 本地开发服务器配置（可选）
  // 如果Spring Boot应用未运行，Playwright可以自动启动
  // webServer: {
  //   command: 'mvn spring-boot:run',
  //   url: 'http://localhost:9090',
  //   reuseExistingServer: !process.env.CI,
  //   timeout: 120 * 1000,
  // },
});
