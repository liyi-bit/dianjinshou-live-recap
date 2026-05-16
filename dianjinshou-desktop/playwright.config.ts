import { defineConfig } from '@playwright/test'

/**
 * Playwright E2E 配置 —— 针对 Electron 桌面端。
 *
 * 约定：
 *  - 测试代码放在 e2e/**​/*.spec.ts
 *  - 跑测试前请先 `npm run build` 产出 out/（fixture 会读取 out/main/index.js）
 *  - 浏览器自动化走 @playwright/test 的 _electron API，不启动常规浏览器
 *  - API_BASE 默认指向生产；本地调试可用 E2E_API_BASE=http://127.0.0.1:8080/api/v1 覆盖
 */
export default defineConfig({
  testDir: './e2e',
  testMatch: /.*\.spec\.ts$/,
  timeout: 90_000,
  expect: { timeout: 10_000 },
  fullyParallel: false, // Electron 主进程只有一个，测试必须串行
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
})
