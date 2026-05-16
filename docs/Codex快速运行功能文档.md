# 点金手源码交付给 Codex 的快速运行功能文档

> 目标读者：拿到源码后的用户，以及用户自己的 Codex + GPT-5.5。
> 目标：让 AI 先读这份文档，再按模块启动项目，快速判断哪些能力可以本地跑通，哪些能力依赖第三方服务。

---

## 1. 给 Codex 的第一条指令

把源码交给用户后，可以让用户在项目根目录对 Codex 说：

```text
请先阅读 docs/Codex快速运行功能文档.md，然后按文档检查我的本机环境，帮我启动后端、桌面端和管理后台。不要部署到服务器，不要运行 deploy_now.py / deploy_website.py / publish_desktop.py。
```

如果用户只是想先看界面，可以说：

```text
请先按 docs/Codex快速运行功能文档.md 启动管理后台和桌面端，后端优先连本地 8080；如果本地后端没起来，再临时使用已有远程 API。
```

---

## 2. 项目是什么

点金手是一个面向直播电商的 AI 直播复盘系统，核心能力包括：

- 直播间管理：添加抖音、快手、视频号等直播间，维护主播资料、账号类型、行业信息和监控状态。
- 桌面录制：Electron 客户端调用 FFmpeg 录制直播流，录制完成后转封装为 MP4。
- 本地 ASR：桌面端支持通过本地 sherpa-onnx WASM 工作进程做语音转文字。
- AI 复盘：后端接收 ASR 文本后生成整场复盘、切片复盘、关键词、诊断报告和优化建议。
- AI 对比：选择两场录制或切片，生成对比复盘。
- AI 助手：运营助手、违规助手、话术助手，支持会话历史和预设问题。
- 云空间：分片上传、文件管理、分享链接、云空间用量统计。
- 文件分析：上传本地视频、音频或文案，做 ASR、复盘、合规预审和切片。
- 短视频切片：从录制文件中截取片段并管理导出。
- 管理后台：管理员查看用户、任务、录制、AI 配置、云空间和系统日志。
- 官网静态页：`website/` 下的纯静态页面。

---

## 3. 代码目录地图

```text
dianjinshou/
├── dianjinshou-server/     Spring Boot 后端
├── dianjinshou-desktop/    Electron + Vue 3 桌面客户端
├── dianjinshou-admin/      Vite + Vue 3 管理后台
├── website/                官网静态页面
├── docs/                   功能、开发、数据库、测试和任务文档
├── docker-compose.prod.yml 本地/生产依赖服务与后端容器编排参考
├── deploy_now.py           后端远程部署脚本，交付用户本地运行时不要执行
├── deploy_website.py       官网远程部署脚本，交付用户本地运行时不要执行
└── publish_desktop.py      桌面端打包发布脚本，交付用户本地运行时不要执行
```

现有详细文档：

- `docs/功能文档_V1.md`：业务功能总览。
- `docs/AI开发文档_V1.md`：技术实现与模块说明。
- `docs/数据库文档_V1.md`：数据库结构说明。
- `docs/test-reports/`：历史测试报告。

---

## 4. 技术栈和端口

| 模块 | 目录 | 技术栈 | 默认端口 |
|---|---|---|---|
| 后端 | `dianjinshou-server/` | Java 8、Spring Boot 2.7.18、MyBatis-Plus、Flyway、MySQL、Redis、RabbitMQ、MinIO | `8080` |
| 桌面端 | `dianjinshou-desktop/` | Electron 33、electron-vite、Vue 3、TypeScript、Pinia、Arco Design、Vitest | Vite `5173` |
| 管理后台 | `dianjinshou-admin/` | Vite、Vue 3、TypeScript、Pinia、Arco Design | `5174` |
| MinIO 控制台 | 根 compose | MinIO | `9001` |
| RabbitMQ 控制台 | 根 compose | RabbitMQ Management | `15672` |

---

## 5. 本机依赖

建议环境：

- JDK 8。
- Maven 3.8+。
- Node.js 18+ 或 20+。
- npm，项目当前使用 `package-lock.json`，优先用 `npm install`。
- Docker Desktop，用来启动 MySQL、Redis、RabbitMQ、MinIO。
- Windows 用户建议使用 PowerShell。

检查命令：

```powershell
java -version
mvn -version
node -v
npm -v
docker --version
docker compose version
```

如果 `mvn` 不在 PATH，可以先查找本机已有 Maven：

```powershell
Get-ChildItem "$HOME\.m2\wrapper\dists" -Recurse -Filter mvn.cmd -ErrorAction SilentlyContinue
```

本机实测可用 Maven 路径示例：

```powershell
C:\Users\Admin\.m2\wrapper\dists\apache-maven-3.9.14\ed7edd442f634ac1c1ef5ba2b61b6d690b5221091f1a8e1123f5fadcc967520d\bin\mvn.cmd
```

如果 Docker 不可用，但机器安装了 phpStudy / Memurai，也可以用：

- phpStudy MySQL 5.7：`D:\360Downloads\phpstudy_pro\Extensions\MySQL5.7.26\bin\mysqld.exe`
- Memurai Redis：`C:\Program Files\Memurai\memurai.exe`

这种组合可以跑本地后端，但 MySQL 5.7 不能走 Flyway Community 自动迁移，见第 8 节的 MySQL 5.7 说明。

---

## 6. 启动顺序

推荐按这个顺序启动：

1. 启动基础设施：MySQL、Redis、RabbitMQ、MinIO。
2. 启动后端：`dianjinshou-server`。
3. 启动桌面端：`dianjinshou-desktop`。
4. 启动管理后台：`dianjinshou-admin`。

后端是桌面端和管理后台的数据来源。如果只想看前端界面，也可以先启动前端，但很多页面会因为接口不可用而报错。

---

## 7. 启动基础设施

项目根目录有 `docker-compose.prod.yml`，包含 MySQL、Redis、RabbitMQ、MinIO、后端和 Nginx。为了本地开发，建议只启动依赖服务：

```powershell
docker compose -f docker-compose.prod.yml up -d mysql redis rabbitmq minio
```

等待健康检查完成：

```powershell
docker compose -f docker-compose.prod.yml ps
```

默认账号：

| 服务 | 地址 | 账号 | 密码 |
|---|---|---|---|
| MySQL | `localhost:3306` | `root` | `root123456` |
| Redis | `localhost:6379` | 无 | 无 |
| RabbitMQ | `http://localhost:15672` | `guest` | `guest` |
| MinIO | `http://localhost:9001` | `minioadmin` | `minioadmin` |

如果要完全容器化运行后端，可以用：

```powershell
docker compose -f docker-compose.prod.yml up -d --build server
```

但本地开发更推荐用 Maven 直接启动后端，日志和调试更方便。

---

## 8. 启动后端

后端目录：

```powershell
cd dianjinshou-server
```

编译：

```powershell
mvn -DskipTests compile
```

启动开发环境：

```powershell
mvn -Dmaven.test.skip=true spring-boot:run -Dspring-boot.run.profiles=local
```

注意：当前仓库部分测试源码和业务构造器签名不同步，`spring-boot:run` 会触发 test-compile，所以本地快速启动要用 `-Dmaven.test.skip=true`，只用 `-DskipTests` 不够。

如果 8080 已被占用，可以换到 18080：

```powershell
mvn -Dmaven.test.skip=true spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments=--server.port=18080
```

健康检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
```

如果后端跑在 18080：

```powershell
Invoke-RestMethod http://localhost:18080/api/v1/health
```

正常情况下会返回：

```json
{
  "status": "UP",
  "components": {
    "db": "UP",
    "redis": "UP",
    "rabbitmq": "UP"
  }
}
```

注意：

- `application.yml` 默认激活 `dev` profile。
- `application-dev.yml` 会连接本地 MySQL、Redis、RabbitMQ、MinIO。
- `application-local.yml` 会连接本地 MySQL / Redis，并排除 RabbitMQ 自动配置，更适合无 Docker 的快速本地启动。
- Flyway 迁移脚本在 `dianjinshou-server/src/main/resources/db/migration/`。
- 开发环境如果数据库已有旧结构，Flyway 可能因为版本基线或重复对象报错。交给 Codex 时，让它先看错误日志，再决定是否清空本地测试库或调整 profile，不能直接动生产库。
- 如果使用 phpStudy MySQL 5.7，Flyway Community 8.5 会报 `MySQL 5.7 is no longer supported`。这时用 `local` profile 关闭 Flyway，并手动导入迁移 SQL。
- AI、短信、COS 等第三方服务请用用户自己的密钥配置，交付运行文档不要依赖源码中的历史本地配置。

phpStudy MySQL 5.7 启动示例：

```powershell
$mysqld = "D:\360Downloads\phpstudy_pro\Extensions\MySQL5.7.26\bin\mysqld.exe"
$ini = "D:\360Downloads\phpstudy_pro\Extensions\MySQL5.7.26\my.ini"
Start-Process -FilePath $mysqld -ArgumentList @("--defaults-file=$ini","--console") -WindowStyle Hidden
```

phpStudy MySQL 默认账号常见为：

```text
root / root
```

创建本地库：

```powershell
$mysql = "D:\360Downloads\phpstudy_pro\Extensions\MySQL5.7.26\bin\mysql.exe"
& $mysql --user=root --password=$env:MYSQL_PWD --host=127.0.0.1 --port=3306 --execute="CREATE DATABASE IF NOT EXISTS dianjinshou CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

如果本地库已经是旧版点金手库，可手动补 V26-V36：

```powershell
$mysql = "D:\360Downloads\phpstudy_pro\Extensions\MySQL5.7.26\bin\mysql.exe"
$dir = "D:\360Downloads\IdeaProjects\dianjinshou\dianjinshou-server\src\main\resources\db\migration"
26..36 | ForEach-Object {
  $file = Get-ChildItem "$dir\V$($_)__*.sql" | Select-Object -First 1
  if ($file) {
    $path = $file.FullName.Replace('\','/')
    & $mysql --user=root --password=$env:MYSQL_PWD --host=127.0.0.1 --port=3306 --default-character-set=utf8mb4 --database=dianjinshou --execute="source $path"
  }
}
```

如果是全新空库且使用 MySQL 8，优先让 Flyway 自动执行全部迁移；如果是 MySQL 5.7，可以按同样方式从 V1-V36 手动导入。

---

## 9. 启动桌面端

桌面端目录：

```powershell
cd dianjinshou-desktop
```

安装依赖：

```powershell
npm install
```

如果使用 npm 11 遇到 `ERESOLVE`，并提示 `electron-vite@2.3.0` 与 `vite@6.x` peer dependency 冲突，本地快速运行可先使用：

```powershell
npm install --legacy-peer-deps
```

如果曾经用过 `--ignore-scripts`，或者 Electron 下载卡住，需要手动补装 Electron 二进制：

```powershell
$env:ELECTRON_MIRROR='https://npmmirror.com/mirrors/electron/'
node node_modules\electron\install.js
Test-Path node_modules\electron\dist\electron.exe
```

建议把开发环境 API 改成本地后端。编辑或让 Codex 编辑：

```text
dianjinshou-desktop/.env.development
```

内容：

```env
VITE_API_BASE=http://localhost:8080/api/v1
```

如果后端因为 8080 被占用而跑在 18080，则改为：

```env
VITE_API_BASE=http://localhost:18080/api/v1
```

启动：

```powershell
npm run dev
```

说明：

- `npm run dev` 会启动 Vite renderer，并拉起 Electron 窗口。
- renderer 默认端口是 `5173`。
- Electron 主进程中的后端 API 默认也会读取 `VITE_API_BASE`。
- 如果没有本地后端，桌面端可能连到 `.env.development` 指向的远程 API。
- 录制能力依赖 FFmpeg，项目通过 `ffmpeg-static` 提供二进制。
- 本地 ASR 相关模型和资源由 `electron/services/asr-model-manager.ts` 以及 `build/sherpa-models/` 约定管理，模型文件缺失时，录制后的转写能力可能不可用，但普通页面仍可运行。

安装依赖后必须确认 FFmpeg 是 Windows 可执行文件，否则直播识别正常但录制会失败：

```powershell
Test-Path node_modules\ffmpeg-static\ffmpeg.exe
& node_modules\ffmpeg-static\ffmpeg.exe -version
```

如果出现 `not a valid application for this OS platform`、`spawn UNKNOWN` 或无法输出版本号，说明 `node_modules\ffmpeg-static\ffmpeg.exe` 不可用。处理方式：

1. 删除后重新安装桌面端依赖，优先使用 `npm install --legacy-peer-deps`。
2. 仍失败时，从可信的 Windows FFmpeg 安装或同项目可用副本复制 `ffmpeg.exe` 覆盖该文件。
3. 覆盖后再次运行 `ffmpeg.exe -version`，确认成功后再启动桌面端录制。

常用验证：

```powershell
npm test
npx vue-tsc --noEmit
npm run build
```

如果 `5173` 被占用：

```powershell
Get-NetTCPConnection -LocalPort 5173 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

---

## 10. 启动管理后台

管理后台目录：

```powershell
cd dianjinshou-admin
```

安装依赖：

```powershell
npm install
```

开发环境推荐让 Vite 代理 `/api` 到本地后端。当前 `vite.config.ts` 里代理目标可能是远程服务器，接手方可以改成：

```ts
server: {
  port: 5174,
  proxy: {
      '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

如果后端跑在 18080，则代理目标改为：

```ts
target: 'http://localhost:18080'
```

同时保持：

```text
dianjinshou-admin/.env.development
```

内容为空值即可：

```env
VITE_API_BASE=
```

启动：

```powershell
npm run dev
```

访问：

```text
http://localhost:5174/admin/
```

后台初始管理员账号来自 Flyway 迁移 `V34__admin_accounts.sql`：

```text
用户名：admin
密码：test123456
```

构建验证：

```powershell
npm run build
```

---

## 11. 登录和基础业务验证

桌面端业务用户可以走注册流程：

1. 打开桌面端。
2. 进入注册或短信登录。
3. 开发环境短信验证码默认使用绕过码：`1234`。
4. 注册成功后可以添加直播间、查看复盘、使用云空间等页面。

建议最小验证路径：

1. 后端健康检查通过。
2. 桌面端能打开登录页。
3. 用验证码 `1234` 注册一个测试账号。
4. 登录后进入直播间列表。
5. 添加一个主播信息。
6. 管理后台用 `admin / test123456` 登录。
7. 管理后台能看到用户或任务列表。

录制、ASR、AI 分析属于更深链路，可能额外依赖：

- 可用直播间 URL。
- FFmpeg 可执行文件。
- 本地 ASR 模型文件。
- AI 服务 API Key。
- 对象存储配置。

---

## 12. 环境变量和密钥

交付给用户运行时，建议让用户自己创建本地配置或系统环境变量。

后端常见环境变量：

```env
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/dianjinshou?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=<YOUR_DB_PASSWORD>
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=<YOUR_RABBITMQ_PASSWORD>
JWT_SECRET=please-change-this-to-a-long-random-secret
STORAGE_TYPE=minio
STORAGE_ENDPOINT=http://localhost:9000
STORAGE_ACCESS_KEY=minioadmin
STORAGE_SECRET_KEY=minioadmin
AI_PROVIDER=yunwu
YUNWU_API_KEY=replace-with-user-key
YUNWU_ENDPOINT=https://api.yunwu.ai/v1
YUNWU_MODEL=gpt-4o-mini
```

桌面端：

```env
VITE_API_BASE=http://localhost:8080/api/v1
```

管理后台：

```env
VITE_API_BASE=
```

### 12.1 用户级第三方接入配置

后端 YAML / 环境变量里的 `dianjinshou.ai.yunwu-api-key` 只能作为基础配置。当前版本的任务分析会优先读取“当前登录用户”的第三方接入配置；如果用户级配置缺失，前端会报：

```text
云雾AI调用失败，已重试2次: 请先在设置→第三方接入完成配置
```

优先处理方式：

1. 在管理后台或桌面端登录对应账号。
2. 进入“设置 → 第三方接入”。
3. 填写云雾 AI API Key、模型和 Endpoint。
4. 保存后重新发起分析，或对失败任务调用重新分析接口。

本机调试时也可以直接检查数据库：

```sql
SELECT user_id, setting_key, setting_value
FROM user_third_party_settings
WHERE user_id = <USER_ID>
  AND setting_key LIKE 'ai.%';
```

必要时可在本地库写入占位配置，注意不要把真实密钥提交进仓库：

```sql
INSERT INTO user_third_party_settings (user_id, setting_key, setting_value, updated_at)
VALUES
  (<USER_ID>, 'ai.provider', 'yunwu', NOW(3)),
  (<USER_ID>, 'ai.yunwu.api_key', '<YUNWU_API_KEY>', NOW(3)),
  (<USER_ID>, 'ai.yunwu.endpoint', 'https://api.yunwu.ai/v1', NOW(3)),
  (<USER_ID>, 'ai.yunwu.model', 'gpt-4o-mini', NOW(3))
ON DUPLICATE KEY UPDATE
  setting_value = VALUES(setting_value),
  updated_at = NOW(3);
```

重新分析失败任务：

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:<BACKEND_PORT>/api/v1/analysis/<TASK_ID>/re-analyze" `
  -Headers @{ Authorization = "Bearer <TOKEN>" }
```

原则：

- 不要把真实生产密钥提交给用户或写进文档。
- 不要复用源码中历史遗留的第三方服务密钥。
- 如果源码里已经带有历史密钥，交付前应轮换对应密钥，并在用户环境中使用自己的新密钥。

---

## 13. 常用命令速查

项目根目录：

```powershell
docker compose -f docker-compose.prod.yml up -d mysql redis rabbitmq minio
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f mysql
```

后端：

```powershell
cd dianjinshou-server
mvn -DskipTests compile
mvn test
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

桌面端：

```powershell
cd dianjinshou-desktop
npm install
npm run dev
npm test
npm run build
npm run build:win
```

管理后台：

```powershell
cd dianjinshou-admin
npm install
npm run dev
npm run build
```

官网静态页：

```powershell
cd website
```

直接用浏览器打开 `index.html` 即可。

---

## 14. 不要让 Codex 做的事

源码交付给用户、本地运行调试时，默认不要执行：

```powershell
python deploy_now.py
python deploy_website.py
python publish_desktop.py
```

这些脚本用于远程部署、官网发布或桌面端发布，不属于本地快速启动流程。

也不要直接修改、清空或迁移生产数据库。所有本地调试都应该针对用户自己的本地 MySQL。

---

## 15. 常见问题

### 15.1 后端启动失败：MySQL 连接失败

先确认容器启动：

```powershell
docker compose -f docker-compose.prod.yml ps mysql
```

再确认 `application-dev.yml` 或环境变量中的账号密码是否匹配。

### 15.2 后端启动失败：Flyway 报错

这是数据库结构和迁移历史不一致导致的常见问题。处理顺序：

1. 确认当前连接的是本地测试库，不是生产库。
2. 如果是全新本地库，删除并重建 `dianjinshou` 数据库后重启。
3. 如果是已有数据，先备份，再让 Codex 根据具体 Flyway 错误判断。
4. 如果错误是 MySQL 5.7 不受 Flyway Community 支持，使用 `local` profile 关闭 Flyway，并手动 `source` 对应迁移 SQL。

### 15.3 健康检查里 RabbitMQ 是 DOWN

开发环境的 RabbitMQ listener 可能关闭自动消费，但健康检查仍会检查队列。先确认 RabbitMQ 容器运行，然后重启后端。若队列尚未创建，可以先访问一次会触发分析队列初始化的功能，或让 Codex 检查 `RabbitMqConfig.java`。

### 15.4 桌面端打不开或白屏

先看启动终端里的 Vite 和 Electron 报错。常见原因：

- `npm install` 未完成。
- 端口 `5173` 被占用。
- `VITE_API_BASE` 配错。
- Node 版本过低。

### 15.5 管理后台接口 404 或跨域

管理后台开发环境默认走 Vite 代理。确认：

- 访问地址是 `http://localhost:5174/admin/`。
- `VITE_API_BASE=` 为空，让请求走 `/api` 代理。
- `vite.config.ts` 的代理目标和后端端口一致，例如 `http://localhost:8080` 或 `http://localhost:18080`。

### 15.6 8080 被其他项目占用

不要直接停掉不属于本项目的 Java / Node 进程。先查占用者：

```powershell
Get-NetTCPConnection -LocalPort 8080 | Select-Object OwningProcess
Get-CimInstance Win32_Process -Filter "ProcessId=<PID>" | Select-Object ProcessId,Name,CommandLine
```

如果占用者是其他项目，点金手后端改跑 18080，并同步修改：

- `dianjinshou-desktop/.env.development`
- `dianjinshou-admin/vite.config.ts`

### 15.7 AI 分析不可用

AI 分析依赖后端 AI Provider 配置。没有有效 API Key 时，可以先验证登录、直播间、录制记录、管理后台等基础功能。AI 相关接口可能返回供应商认证错误。

### 15.8 录制或 ASR 不可用

录制依赖直播平台地址解析、FFmpeg 和本地网络。ASR 依赖本地模型或第三方语音服务配置。交付用户第一次跑项目时，建议先跑通登录和页面，再处理录制链路。

### 15.9 Docker 或 mvn 不在 PATH

症状：
- `docker` 命令不存在，但本机已经有 phpStudy MySQL、Redis/Memurai、MinIO 等服务。
- `mvn` 不在 PATH，但 Maven Wrapper 已经下载过 Maven。

处理方式：

1. 本机快速运行不强依赖 Docker；如果 MySQL、Redis、MinIO 已经可用，可以直接使用 `local` profile。
2. RabbitMQ 如果显示 `NOT_CONFIGURED`，通常不影响基础后台、桌面端和管理后台启动。
3. Maven 不在 PATH 时，优先查找：

```powershell
Get-ChildItem "$env:USERPROFILE\.m2\wrapper\dists" -Recurse -Filter mvn.cmd
```

找到后用完整路径启动后端，例如：

```powershell
& "C:\Users\Admin\.m2\wrapper\dists\apache-maven-3.9.14\<hash>\bin\mvn.cmd" spring-boot:run "-Dspring-boot.run.profiles=local"
```

### 15.10 npm install 依赖冲突或 Electron 下载卡住

症状：
- `npm install` 报 `ERESOLVE`。
- `electron-vite@2.3.0` 与 `vite@6.x` peer dependency 冲突。
- Electron 目录存在但 `node_modules\electron\dist\electron.exe` 不存在。

处理方式：

```powershell
cd dianjinshou-desktop
npm install --legacy-peer-deps
$env:ELECTRON_MIRROR='https://npmmirror.com/mirrors/electron/'
node node_modules\electron\install.js
Test-Path node_modules\electron\dist\electron.exe
```

如果返回 `True`，再启动桌面端。

### 15.11 直播识别正常但没有录制

症状：
- 桌面端显示老师正在直播。
- `live_streams` 状态正常。
- 但没有生成视频，或日志里出现 `spawn UNKNOWN`。

排查顺序：

1. 确认真的开启了录制监控，而不是只启动了状态轮询。
2. 查看桌面端错误日志：

```powershell
Get-Content run-logs\desktop-dev-current.err.log -Tail 200
```

3. 验证 FFmpeg：

```powershell
cd dianjinshou-desktop
& node_modules\ffmpeg-static\ffmpeg.exe -version
```

4. 如果 FFmpeg 不能输出版本号，替换为可用的 Windows `ffmpeg.exe` 后重启桌面端。
5. 再次开启录制监控，确认队列里出现 `recording` 状态和 `bytesReceived` 增长。

### 15.12 文件显示 0 字节但 bytesReceived 在增长

症状：
- 录制目录出现 `.flv` 文件。
- Windows 资源管理器或 `Get-Item` 显示 0 字节。
- Electron 录制状态里的 `bytesReceived` 持续增长。

说明：
- FFmpeg 录制中可能还没有刷新资源管理器展示的文件大小。
- 正常停止录制后会从临时 `.flv` 转封装为 `.mp4`。

验证方式：

```powershell
$fs = [System.IO.File]::Open("C:\path\to\recording.flv", 'Open', 'Read', 'ReadWrite')
$fs.Length
$fs.Close()
```

只要 `bytesReceived` 或 FileStream 长度增长，就说明录制进程正在写入。

### 15.13 云雾 AI 提示未配置

症状：

```text
请先在设置→第三方接入中配置 云雾 AI API Key
```

处理方式：

1. 不要只改 `application-local.yml` 或环境变量。
2. 检查当前登录用户的配置：

```sql
SELECT user_id, setting_key, setting_value
FROM user_third_party_settings
WHERE user_id = <USER_ID>
  AND setting_key LIKE 'ai.%';
```

3. 通过“设置 → 第三方接入”保存，或在本地库补齐 `ai.provider`、`ai.yunwu.api_key`、`ai.yunwu.endpoint`、`ai.yunwu.model` 等配置。
4. 重新触发分析：

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "http://localhost:<BACKEND_PORT>/api/v1/analysis/<TASK_ID>/re-analyze" `
  -Headers @{ Authorization = "Bearer <TOKEN>" }
```

5. 如果任务已经成功但旧 `error_msg` 还显示错误，只能在确认本地任务已完成后清理该字段；不要在生产库直接手工改。

### 15.14 多个项目端口或进程共存

症状：
- `8080`、`18080`、`5173`、`5174`、`5175` 已被其他项目占用。
- 前端连到了旧后端，导致配置、任务或录制状态看起来不一致。

处理方式：

1. 先查进程命令行，确认是不是当前项目，不要直接杀端口：

```powershell
Get-CimInstance Win32_Process | Where-Object {
  $_.CommandLine -match 'spring-boot|electron|vite|node'
} | Select-Object ProcessId, CommandLine
```

2. 如果已有其他项目占用后端端口，当前项目可改用 `18081` 等空闲端口。
3. 修改后端端口后，同时同步：
   - 桌面端 `.env.development` 的 `VITE_API_BASE`
   - 管理后台 `vite.config.ts` 的 proxy target
4. 启动后分别访问：
   - 后端健康检查：`http://localhost:<BACKEND_PORT>/api/v1/health`
   - 桌面端 Vite：`http://localhost:<DESKTOP_PORT>`
   - 管理后台：`http://localhost:<ADMIN_PORT>/admin/`

---

## 16. Codex 接手后的建议工作流

让 Codex 按这个顺序做：

1. 读取本文档和 `docs/功能文档_V1.md`。
2. 运行 `git status --short`，确认是否有用户改动。
3. 检查 Java、Maven、Node、FFmpeg 是否可用；Docker 不存在时先确认本机 MySQL、Redis、MinIO 是否已经可用。
4. 启动或确认 MySQL、Redis、RabbitMQ、MinIO；RabbitMQ 本地未配置时，先判断是否影响当前功能。
5. 启动后端并访问 `/api/v1/health`；如果默认端口被占用，先查进程命令行，再选择空闲端口。
6. 把桌面端 `.env.development` 指向本地后端；如果后端端口是 18080 或 18081，使用对应的 `http://localhost:<PORT>/api/v1`。
7. 如需管理后台，把 `vite.config.ts` 代理改成本地后端同一端口。
8. 启动桌面端和管理后台。
9. 验证 Electron 与 FFmpeg 二进制，尤其是 `node_modules\ffmpeg-static\ffmpeg.exe -version`。
10. 用最小验证路径测试登录、注册、列表页和后台登录。
11. 测试录制时，确认录制状态、`bytesReceived` 和最终文件，不要只看直播状态轮询。
12. 配置当前登录用户的第三方接入，再验证 AI 复盘任务。
13. 最后再测试 ASR、云空间等依赖外部能力的功能。

---

## 17. 当前交付状态速览

| 能力 | 本地快速运行难度 | 备注 |
|---|---:|---|
| 后端健康检查 | 低 | 需要 MySQL、Redis、RabbitMQ、MinIO |
| 桌面端界面 | 低 | `npm run dev` 拉起 Electron |
| 管理后台 | 低 | 注意代理目标和 `/admin/` base |
| 用户注册登录 | 中 | 开发验证码可用 `1234` |
| 管理员登录 | 低 | `admin / test123456` |
| 直播间管理 | 中 | 需要后端和数据库 |
| 录制 | 中高 | 需要可解析直播地址和 FFmpeg |
| 本地 ASR | 高 | 需要模型资源和音视频处理链路 |
| AI 复盘 | 高 | 需要当前用户第三方接入里的 AI API Key 和消息队列 |
| 云空间 | 中高 | 本地 MinIO 可跑，COS 需用户自己的密钥 |
| 远程部署 | 不建议默认执行 | 必须由用户明确要求 |
