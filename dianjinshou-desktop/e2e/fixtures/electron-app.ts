import { test as base, _electron as electron, type ElectronApplication, type Page } from '@playwright/test'
import path from 'path'
import { existsSync } from 'fs'

/**
 * Fixture：启动 Electron 应用，暴露 `app` 和 `mainWindow`。
 *
 * 用法：
 *   import { test, expect } from '../fixtures/electron-app'
 *   test('xxx', async ({ mainWindow }) => { ... })
 */

type ElectronFixtures = {
  app: ElectronApplication
  mainWindow: Page
}

const REPO_ROOT = path.resolve(__dirname, '../..')
const MAIN_ENTRY = path.join(REPO_ROOT, 'out/main/index.js')

export const test = base.extend<ElectronFixtures>({
  app: async ({}, use) => {
    if (!existsSync(MAIN_ENTRY)) {
      throw new Error(
        `找不到 ${MAIN_ENTRY}\n请先在 dianjinshou-desktop 下运行 \`npm run build\` 产出 out/`
      )
    }

    const app = await electron.launch({
      args: [MAIN_ENTRY],
      // 确保测试环境用可控的后端；如需本地后端，设置 E2E_API_BASE
      env: {
        ...process.env,
        VITE_API_BASE: process.env.E2E_API_BASE || 'http://localhost:18081/api/v1',
        NODE_ENV: 'test',
        ELECTRON_DISABLE_SECURITY_WARNINGS: '1',
      },
      timeout: 30_000,
    })

    await use(app)

    try {
      await app.close()
    } catch { /* 有些场景下窗口已关闭，忽略 */ }
  },

  mainWindow: async ({ app }, use) => {
    const win = await app.firstWindow({ timeout: 20_000 })
    await win.waitForLoadState('domcontentloaded')
    await use(win)
  },
})

export { expect } from '@playwright/test'
