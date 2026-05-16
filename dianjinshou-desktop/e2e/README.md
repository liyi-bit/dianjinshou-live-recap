# Playwright E2E 脚手架

针对 Electron 主窗口的端到端测试。与 `src/**/__tests__/*` 下的 vitest 单测**并存**：
- **vitest**：store / util / 纯函数的单元测试（`npm test`）
- **playwright**：Electron 启动、preload IPC、登录、录制流程等真实跑的 E2E（`npm run test:e2e`）

## 目录

```
e2e/
├── fixtures/
│   └── electron-app.ts     # 启动 Electron、暴露 app/mainWindow 的 fixture
├── helpers/
│   └── login.ts            # 登录辅助函数
├── smoke.spec.ts           # 启动冒烟（无需登录）
└── login.spec.ts           # 登录流程（需要测试账号凭据）
```

## 首次运行

```bash
cd dianjinshou-desktop

# 1) 安装依赖（playwright 已作为 devDependency 声明）
npm i

# 2) 安装 Playwright 浏览器（Electron 测试不需要 chromium 下载，但 Playwright 需要其驱动）
npx playwright install --with-deps

# 3) 先构建 electron 产物（fixture 要读 out/main/index.js）
npm run build

# 4) 跑冒烟测试
npm run test:e2e -- --grep @smoke

# 5) 跑全部
npm run test:e2e

# 6) 调试模式（带浏览器 inspector）
npm run test:e2e:debug
```

## 环境变量

| 变量 | 作用 | 默认 |
|---|---|---|
| `E2E_API_BASE` | 被测应用连接的后端 API base | `http://localhost:18081/api/v1` |
| `E2E_TEST_PHONE` | 登录测试的手机号 | 未设置时 login.spec 会 skip |
| `E2E_TEST_PASSWORD` | 登录测试的密码 | 同上 |

示例：

```bash
E2E_API_BASE=http://127.0.0.1:8080/api/v1 \
E2E_TEST_PHONE=17300000000 \
E2E_TEST_PASSWORD=your-pw \
npm run test:e2e
```

## 写新测试的模板

```ts
// e2e/feature-x.spec.ts
import { test, expect } from './fixtures/electron-app'
import { loginWithPassword } from './helpers/login'

test('XX 功能', async ({ mainWindow }) => {
  await loginWithPassword(mainWindow, process.env.E2E_TEST_PHONE!, process.env.E2E_TEST_PASSWORD!)
  // 用 getByRole / getByText / locator 操作 UI
  await mainWindow.getByText('直播间总览').click()
  await expect(mainWindow.locator('table.djstbl')).toBeVisible()
})
```

## 注意事项

- Electron 主进程是独进程，测试必须**串行**（`playwright.config.ts` 里 `workers: 1`）
- Fixture 会把每个测试拉起一个新的 Electron 实例，Recovery 等启动逻辑会执行
- 失败会自动保存 screenshot / video / trace 到 `test-results/` 下
- **不要**在 CI 里跑 `login.spec` 除非提供真实测试账号
