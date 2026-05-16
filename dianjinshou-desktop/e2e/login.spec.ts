import { test, expect } from './fixtures/electron-app'
import { loginWithPassword, clearAuth, getAuthSnapshot } from './helpers/login'

/**
 * 登录流程测试。
 *
 * 需要环境变量：
 *   E2E_TEST_PHONE     被测账号手机号
 *   E2E_TEST_PASSWORD  被测账号密码
 * 未设置时该 describe 会被 skip，避免在未配置凭据的环境误跑。
 */
const PHONE = process.env.E2E_TEST_PHONE
const PASSWORD = process.env.E2E_TEST_PASSWORD

test.describe('登录 @auth', () => {
  test.skip(!PHONE || !PASSWORD, '需要 E2E_TEST_PHONE / E2E_TEST_PASSWORD 环境变量')

  test('密码正确可以登录并进入主界面', async ({ mainWindow }) => {
    await clearAuth(mainWindow)
    await loginWithPassword(mainWindow, PHONE!, PASSWORD!)

    const snap = await getAuthSnapshot(mainWindow)
    expect(snap.hasAccess).toBe(true)
    expect(snap.hasRefresh).toBe(true)
    expect(snap.phone).toBe(PHONE)
  })

  test('密码错误显示"手机号或密码错误"提示', async ({ mainWindow }) => {
    await clearAuth(mainWindow)
    await mainWindow.reload()
    await mainWindow.locator('input[type="tel"]').first().fill(PHONE!)
    await mainWindow.locator('input[type="password"]').first().fill('wrong-password-' + Date.now())
    await mainWindow.getByRole('button', { name: /^登录$/ }).click()

    // Arco Message 会插入 .arco-message 节点，里面含错误文案
    await expect(mainWindow.locator('.arco-message, .arco-notification').first()).toContainText(
      /密码错误|手机号或密码错误/,
      { timeout: 10_000 }
    )
  })
})
