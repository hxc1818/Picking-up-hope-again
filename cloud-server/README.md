# Picking Up Hope 本地云存档测试服务器

零第三方依赖的 Node.js 本地测试后端，用于模组账号登录、注册与单人 RPG 进度云存档同步验证。

## 启动方式

```powershell
cd cloud-server
node server.js
```

或直接双击 `start_cloud_server.bat`。

- 默认监听地址：`http://127.0.0.1:3847`（可通过环境变量 `HOST` / `PORT` 覆盖）
- 数据存储目录：`cloud-server/data/db.json`（已列入 `.gitignore`，不提交进仓库，每次推送进度自动保留最近 3 份历史快照）

## 自动化接口测试

```powershell
cd cloud-server
node test_api.mjs
```

## REST API 列表

- `GET /api/health`：服务健康检查
- `POST /api/auth/register`：注册账号（`{ username, password, displayName? }`）
- `POST /api/auth/login`：登录账号（`{ username, password }`）
- `GET /api/auth/me`：验证 `Authorization: Bearer <token>` 并返回账号与存档摘要
- `POST /api/auth/logout`：注销当前 Token
- `GET /api/save/pull`：拉取云端最新 RPG 进度快照
- `POST /api/save/push`：推送最新 RPG 进度快照（递增 `revision`）

## 迁移至生产服务器

模组客户端与服务端统一从游戏目录下的 `config/picking_hope/account_config.json` 读取 `serverUrl`（也可直接在游戏标题界面点开服务器配置修改）。后续迁移到正式云服务或 Cloudflare Workers 时，只需保持上述 REST API 协议一致并修改 `serverUrl` 即可。
