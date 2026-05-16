@echo off
REM 启动第二个桌面实例：renderer 走主实例的 Vite dev server，main 直连本地后端
REM 前置条件：主实例（npm run dev）已经在跑，Vite dev server 监听 5173

set INSTANCE_DIR=%APPDATA%\dianjinshou-desktop-instance2
set ELECTRON_RENDERER_URL=http://localhost:5173
set NODE_ENV=development

"%~dp0node_modules\electron\dist\electron.exe" "%~dp0" ^
  --user-data-dir="%INSTANCE_DIR%" ^
  --api-base=http://localhost:8080/api/v1
