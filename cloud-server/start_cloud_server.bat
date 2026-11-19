@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ======================================================
echo   Picking Up Hope - 本地云存档测试服务器 (Node.js)
echo   默认地址: http://127.0.0.1:3847
echo ======================================================
node server.js
pause
