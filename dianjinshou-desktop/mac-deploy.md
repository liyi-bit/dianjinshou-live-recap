# macOS DMG 打包指南

把 `dianjinshou-desktop` 打成签名 + 公证后的 arm64 dmg。本次（v1.1.6）走通的全流程留底，下次照葫芦画瓢。

---

## 前置一次性准备

下面这些只需做一次，做完之后所有版本都共用。

### 1. 系统 / 工具

- macOS 14+ Apple Silicon（M 系列芯片）
- Xcode 命令行工具：`xcode-select --install`
- Node.js 20+ + npm（用 nvm 装更省心）
- Python 3.9+（给 `publish_desktop.py` 调用 paramiko 用）

### 2. 签名证书

钥匙串里需要 `Developer ID Application` 证书：

```bash
security find-identity -v -p codesigning
# 应该看到类似：
#   Developer ID Application: The XTWC Information Technology (Beijing) Co., Ltd. (PHG36AHL48)
```

如果没有，从公司 Apple Developer 后台下载 `.p12`，双击导入钥匙串。

### 3. 公证密钥（ASC API Key）

存放位置：`~/Documents/钥匙串访问-证书/AuthKey_2PK3TDR7UC.p8`

`.p8` 文件 + Key ID + Issuer ID 都来自 [App Store Connect → Users and Access → Keys](https://appstoreconnect.apple.com/access/api)，复用公司账号那把。

校验密钥可用：

```bash
xcrun notarytool history \
  --key "/Users/bkw/Documents/钥匙串访问-证书/AuthKey_2PK3TDR7UC.p8" \
  --key-id "2PK3TDR7UC" \
  --issuer "69a6de81-8b26-47e3-e053-5b8c7c11a4d1"
# 输出 "Successfully received submission history" = 通过
```

### 4. sherpa-onnx 离线模型（500 MB，必须）

放置位置：

```
dianjinshou-desktop/build/sherpa-models/
├── paraformer-zh/
│   ├── model.int8.onnx   ~217 MB
│   └── tokens.txt        ~74 KB
└── punct-zh-en/
    └── model.onnx        ~280 MB
```

来源：sherpa-onnx 官方 HuggingFace（国内带宽时好时坏，建议从同事/网盘拉同款副本）：

- Paraformer：`csukuangfj/sherpa-onnx-paraformer-zh-2024-03-09` 仓库下的 `model.int8.onnx` 和 `tokens.txt`
- 标点模型：`csukuangfj/sherpa-onnx-punct-ct-transformer-zh-en-vocab272727-2024-04-12` 仓库下的 `model.onnx`

镜像：`https://hf-mirror.com/<repo>/resolve/main/<file>`。

> `electron-builder.yml` 的 `mac.extraResources` 已配置把 `build/sherpa-models/` 整个目录打进 `Contents/Resources/sherpa-models/`，所以模型只要放对路径，打包时会自动内置。dmg 体积会从原本的 ~140 MB 涨到 ~600 MB。

### 5. 项目依赖

```bash
cd dianjinshou-desktop
ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/ \
ELECTRON_BUILDER_BINARIES_MIRROR=https://npmmirror.com/mirrors/electron-builder-binaries/ \
npm install --legacy-peer-deps --registry=https://registry.npmmirror.com
```

`--legacy-peer-deps` 是绕开 `electron-vite@2.3.0` 与 `vite@^6` 的 peer dep 冲突，仓库里就这么用。

首次安装会触发 `electron-builder install-app-deps`，对 sherpa-onnx native 模块按 arm64 重编。

### 6. Electron framework 缓存（避免 GitHub 直连慢）

国内直连 `github.com/electron/electron/releases` 容易卡住。预先把 framework 拉到缓存：

```bash
CACHE=~/Library/Caches/electron
VER=33.4.11   # 跟 package.json 里 electron 版本对齐
curl -fL --progress-bar -o "$CACHE/electron-v$VER-darwin-arm64.zip" \
  "https://npmmirror.com/mirrors/electron/v$VER/electron-v$VER-darwin-arm64.zip"
curl -fLs -o "$CACHE/SHASUMS256.txt-v$VER" \
  "https://npmmirror.com/mirrors/electron/v$VER/SHASUMS256.txt"
```

---

## 每次打包

```bash
cd dianjinshou-desktop

# (1) 可选：升版本
npm version patch --no-git-tag-version

# (2) 打包（含签名 + 公证 + staple）
APPLE_API_KEY="/Users/bkw/Documents/钥匙串访问-证书/AuthKey_2PK3TDR7UC.p8" \
APPLE_API_KEY_ID="2PK3TDR7UC" \
APPLE_API_ISSUER="69a6de81-8b26-47e3-e053-5b8c7c11a4d1" \
CSC_NAME="The XTWC Information Technology (Beijing) Co., Ltd. (PHG36AHL48)" \
ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/ \
ELECTRON_BUILDER_BINARIES_MIRROR=https://npmmirror.com/mirrors/electron-builder-binaries/ \
npx electron-vite build && npx electron-builder --mac --arm64
```

> 不要用 `npm run build:mac`：`package.json` 里的 `build:mac` 末尾带了一个空的 `--config`，会让 electron-builder 把后续参数当成 config 路径。直接调 `npx` 更稳。

总耗时 15–30 分钟（v1.1.6 实测约 22 分钟）。其中：

- `electron-vite build`：~5 秒
- `packaging`（含 sherpa-models 500 MB）：~30 秒
- `signing`（深签遍 .app 内所有可执行）：~30 秒
- **`notarization`**：上传 ~600 MB zip 到 Apple → 排队 → 验证，**这一步耗时最长**，国内网络下平均 10–20 分钟
- `staple` + `building DMG`：~2 分钟

如果中途要查进度，看 `dist/` 目录大小变化，或者：

```bash
xcrun notarytool history --key "$APPLE_API_KEY" --key-id "$APPLE_API_KEY_ID" --issuer "$APPLE_API_ISSUER" | head
```

---

## 产物

```
dist/
├── 点金手-X.Y.Z-arm64.dmg            ~600 MB     ← 给最终用户
├── 点金手-X.Y.Z-arm64.dmg.blockmap   ~600 KB     ← 增量更新
├── latest-mac.yml                    347 B      ← autoUpdater 清单
└── mac-arm64/点金手.app                          ← dmg 解压后的 .app（验证用）
```

---

## 验证（每次打完都跑一遍）

```bash
# .app 应该 accepted
spctl -a -vvv dist/mac-arm64/点金手.app
# 期望：source=Notarized Developer ID

# .app 应该有 stapled ticket
stapler validate dist/mac-arm64/点金手.app
# 期望：The validate action worked!

# .app 签名细节
codesign -dvv dist/mac-arm64/点金手.app 2>&1 | grep -E "Identifier|Authority|Notarization|Runtime"
# 期望看到：Notarization Ticket=stapled / Runtime Version=14.0.0

# dmg 完整性
hdiutil verify dist/点金手-X.Y.Z-arm64.dmg
# 期望：checksum is VALID
```

> dmg 容器本身**没有**单独的公证 ticket（electron-builder 25 默认不 staple dmg 外壳），所以 `spctl -t install dist/...dmg` 会显示 `rejected`。这是预期的：用户挂载 dmg → 把 .app 拖到 /Applications → 启动时 Gatekeeper 校验的是 .app 上 stapled 的 ticket，整个流程不会弹"未经认证"。

---

## 发布到服务器

mac 端 autoUpdater 通过 `http://localhost:18081/desktop/latest-mac.yml` 拉清单。每次发新版必须**同时**上传这 3 个文件，缺一个会出 404 或 sha 校验失败：

- `点金手-X.Y.Z-arm64.dmg`
- `点金手-X.Y.Z-arm64.dmg.blockmap`
- `latest-mac.yml`

目前 `publish_desktop.py` 是 Windows 专用（硬编码 `D:\` 路径 + 仅出 win 包）。在 mac 上发版可以临时手动：

```bash
sshpass -p "$DIANJINSHOU_DEPLOY_PASSWORD" scp \
  dist/点金手-*-arm64.dmg \
  dist/点金手-*-arm64.dmg.blockmap \
  dist/latest-mac.yml \
  "$DIANJINSHOU_DEPLOY_USER@$DIANJINSHOU_DEPLOY_HOST:/opt/dianjinshou/desktop-releases/"

# 验证
curl -sI http://localhost:18081/desktop/latest-mac.yml | head -1
# 期望：HTTP/1.1 200 OK
```

> 后续把 `publish_desktop.py` 跨平台化（Darwin 上跑就发 mac、Windows 上跑就发 win），可以把这一段自动化掉。该工作未做。

---

## 故障排查

| 现象 | 原因 / 解决 |
|---|---|
| `electron-builder` 卡在 `downloading url=https://github.com/electron/...` | GitHub 直连慢。按上面"Electron framework 缓存"预先把 zip 拉到 `~/Library/Caches/electron/`。 |
| `notarization failed` 报 `unauthorized` / `Invalid credentials` | `.p8` 文件路径不对、或 Key ID / Issuer ID 跟密钥不匹配。`xcrun notarytool history` 自测一下。 |
| `code object is not signed at all` 报错 | sherpa-onnx native 模块没被深签。检查 `electron-builder.yml` 的 `mac.hardenedRuntime: true` 和 `entitlements: build/entitlements.mac.plist` 是否都在。 |
| 用户启动 .app 弹"App is damaged" | dmg 内的 .app 没 staple，或 staple 时网络断了。`stapler validate` 检查；不行就 `xcrun stapler staple <.app>` 手动补一次。 |
| `spctl -t install dist/xxx.dmg: rejected` | 正常现象，dmg 外壳没 staple。看上面"验证"小节的说明。 |
| 公证排队几十分钟没回来 | Apple notary 服务偶尔会拥堵。打开 [系统状态](https://www.apple.com/support/systemstatus/) 看 "Developer ID Notary Service" 是否绿灯。 |
| ASR 在 packaged 模式下找不到模型 | 检查 `dist/mac-arm64/点金手.app/Contents/Resources/sherpa-models/` 是否有内容；没有则是打包前 `build/sherpa-models/` 是空的。 |

---

## 关键参数复用速查

| 用途 | 值 |
|---|---|
| Apple Team ID | `PHG36AHL48` |
| 签名身份名 | `Developer ID Application: The XTWC Information Technology (Beijing) Co., Ltd. (PHG36AHL48)` |
| ASC Key ID | `2PK3TDR7UC` |
| ASC Issuer ID | `69a6de81-8b26-47e3-e053-5b8c7c11a4d1` |
| `.p8` 路径 | `~/Documents/钥匙串访问-证书/AuthKey_2PK3TDR7UC.p8` |
| 远端发布目录 | `<DEPLOY_HOST>:/opt/dianjinshou/desktop-releases/` |
| autoUpdater 清单 URL | `http://localhost:18081/desktop/latest-mac.yml` |
