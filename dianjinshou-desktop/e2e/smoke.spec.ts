import { test, expect } from './fixtures/electron-app'

/**
 * 冒烟测试：确认 Electron 能启动、主窗口加载、preload API 到位。
 * 不依赖后端登录，可以在任何环境下跑。
 */
test.describe('@smoke 启动冒烟', () => {
  test('主窗口打开并显示登录页或主界面', async ({ mainWindow }) => {
    await expect(mainWindow).toHaveTitle(/点金手|dianjinshou/i)

    // 基础 DOM：#app 根容器存在
    await expect(mainWindow.locator('#app')).toBeVisible({ timeout: 20_000 })
  })

  test('preload 暴露的 electronAPI 可用', async ({ mainWindow }) => {
    const apiKeys = await mainWindow.evaluate(() => {
      const api = (window as any).electronAPI
      return api ? Object.keys(api).sort() : null
    })
    expect(apiKeys).not.toBeNull()
    // 关键接口必须存在（如果改 preload 签名记得同步更新这个断言）
    expect(apiKeys).toEqual(
      expect.arrayContaining(['minimizeWindow', 'getAppVersion', 'startRecording', 'runAsr'])
    )
  })

  test('版本号从 main 进程正确拉取', async ({ mainWindow }) => {
    const version = await mainWindow.evaluate(async () => {
      const api = (window as any).electronAPI
      return await api?.getAppVersion?.()
    })
    expect(version).toMatch(/^\d+\.\d+\.\d+/)
  })
})
