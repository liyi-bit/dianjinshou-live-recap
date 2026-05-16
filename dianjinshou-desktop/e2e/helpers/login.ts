import type { Page } from '@playwright/test'

/**
 * 登录辅助：手机号 + 密码走 /auth/login 接口。
 * 需要被测账号已在后端注册。
 */
export async function loginWithPassword(page: Page, phone: string, password: string) {
  // 如果已经被路由守卫踹到登录页，就直接填表单
  if (!page.url().includes('#/login') && !(await page.locator('input[type="password"]').count())) {
    await page.goto('#/login')
  }
  await page.locator('input[type="tel"], input[inputmode="numeric"]').first().fill(phone)
  await page.locator('input[type="password"]').first().fill(password)
  await page.getByRole('button', { name: /^登录$/ }).click()
  // 登录成功后路由会跳到 '/'，等任意非登录路由元素就绪
  await page.waitForFunction(() => !location.hash.includes('/login'), { timeout: 15_000 })
}

/**
 * 从 localStorage 读取当前登录状态（用于断言/调试）。
 */
export async function getAuthSnapshot(page: Page): Promise<{ hasAccess: boolean; hasRefresh: boolean; phone: string }> {
  return page.evaluate(() => ({
    hasAccess: !!localStorage.getItem('accessToken'),
    hasRefresh: !!localStorage.getItem('refreshToken'),
    phone: localStorage.getItem('djs_phone') || '',
  }))
}

/**
 * 主动清登录状态（不跳路由，由调用方决定）。
 */
export async function clearAuth(page: Page) {
  await page.evaluate(() => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  })
}
