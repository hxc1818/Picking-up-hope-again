# 《重拾希望 (Picking up hope again)》v3.0 全面重塑设计蓝图

> **版本**：v3.0  
> **实施策略**：分三期依次落地，每期均提交 GitHub Actions 云端编译并产包实机验证。  
> **文案红线**：全部界面、技能、道具、地图名称采用平实、自然、直白的表达，禁用中二或晦涩修饰词。

---

## 一、三期实施总览

| 期次 | 对应模块 | 核心目标 | 验收标志 |
| :--- | :--- | :--- | :--- |
| **第 1 期（已完成）** | **模块 C：单人启动页 + 账号云存档** | 替换原版开始界面与暂停菜单；构建本地 Node.js 测试云存档服；实现多账号隔离单人世界与轻量 RPG 进度云同步 | 登录后一键进单人游戏；多账号独立存档不串档；Node 测试服接口测试通过；离线可兜底游玩 |
| **第 2 期（已完成）** | **模块 A：原生 3D 实景战斗 + 关键帧运镜** | 淘汰 GUI 假 3D 舞台，在真实 Minecraft 世界生成战斗实体与挑高机库布景；隐形摄像机俯视/特写运镜；原生发光分色选中；极简图形头顶条与闪烁预计削韧预览；四属性图形图标；JSON 关键帧引擎 + `/battle_anim` 指令；6 角色 & 6 怪物全差异化招式 | 战斗全程 3D 实景渲染；切目标发光变化 + 削韧预览准确；大招特写运镜流畅；全角色与怪物均有独立招式关键帧 |
| **第 3 期（待开工）** | **模块 B：小体素装饰 + 大地图挑高重建** | 新增一批精致小巧 3D 体素装饰方块；结合原版楼梯/半砖/锁链/栏杆等方块将星舰上下层、冰原科考站与地下矿坑挑高至 8~12 格并做出景深与地形层次 | 彻底告别“盒子感”；25 阶段主线与全部交互点无缝连通；旧档升级自动刷新建筑不丢角色与剧情进度 |

---

## 二、模块 C 详细设计（第 1 期）

### 2.1 定制单人启动页与暂停菜单
- **定制标题界面（`HopeTitleScreen`）**：
  - 替换原版 `TitleScreen`，隐藏“多人游戏”“Minecraft Realms”“选世界列表”入口，营造独立单人 RPG 启动体验。
  - 支持 **登录账号 / 注册新账号 / 记住登录 / 切换账号 / 离线本地模式 / 服务器地址配置**。
  - 登录就绪后展示账号摘要（昵称、在线/离线状态、主线阶段、玩家等级、上次同步时间），并提供金色主按钮【进入游戏】一键创建或加载账号专属单人世界，以及【导入旧存档】辅助按钮。
  - 保留【游戏设置】与【退出游戏】。
- **定制暂停菜单（`HopePauseScreen`）**：
  - 替换原版 `PauseScreen`，实时展示当前登录账号与云同步状态。
  - 提供【继续游戏】、【立即同步云存档】、【游戏设置】、【保存并返回登录页】。

### 2.2 本地 Node.js 云存档测试服（`cloud-server/`）
- 纯 Node.js 22 内置模块实现（零第三方依赖），默认监听 `127.0.0.1:3847`，数据原子持久化于 `cloud-server/data/db.json`（保留最近 3 份历史快照防误损）。
- **REST API**：
  - `GET /api/health`
  - `POST /api/auth/register`（`scrypt` 随机盐哈希，重名返回 `409`）
  - `POST /api/auth/login`（签发 32 字节随机 hex Bearer Token）
  - `GET /api/auth/me`
  - `POST /api/auth/logout`
  - `GET /api/save/pull`
  - `POST /api/save/push`
- 模组内所有网络地址统一读取 `config/picking_hope/account_config.json` 的 `serverUrl`，后续部署生产环境只需修改这一地址。

### 2.3 多账号存档隔离与轻量进度同步协议
- **单人世界隔离**：每个账号对应独立单人存档目录 `puh_acc_<sanitizedAccountId>`，首次进图由模组内置的虚空超平坦模板 `assets/picking_hope_core/world_template/level.dat` 自动生成世界，再由 `StarshipSpawnHandler` 按主线进度重建地形与机关。
- **轻量 RPG 进度包（`CloudSaveSnapshot`）**：
  - `schemaVersion = 1`、`updatedAt`、`revision`、`reason`
  - `summary`：`playerLevel`、`worldLevel`、`questStage`、`questTitle`、`activePartyNames`、`starTicketsCount`
  - `payload`：`teamNbtBase64`（`TeamCapability`）、`inventoryNbtBase64`（完整背包与装备）、`questNbtBase64`（`PickingHopeQuest`）、`playerTags`（`picking_hope_*` / `hope_crate_*`）、安全大地图坐标与朝向。
- **同步时机**：
  - 进入世界前自动拉取云端并与本地 `local_profiles/<id>.json` 比对时间戳取最新份恢复。
  - 战斗胜利、任务推进、抽卡、角色/装备养成后防抖（2 秒）自动异步推送。
  - 退出世界前阻塞刷盘并同步云端（超时上限 2.5 秒，离线不卡顿）。

---

## 三、模块 A 详细实现（第 2 期，已完成）

1. **原生 3D 实景战斗实体与挑高星舰战术机库**：
   - 删除 `BattleStageRenderer`，改由服务端 `TurnBasedCombatManager` 在挑高星舰战术机库（`BattleArenaBuilder` + `BattleArenaLayout` + `ArenaTheme` 扩展枚举，地面 `Y=150`、穹顶 `Y=168`、含北侧太空舷窗与东西高架走廊）中真实生成参战 `BattleActorEntity`。
   - 客户端由 `BattleActorRenderer` 实时渲染 6 名可操纵角色（带 3D 武器与髋枢轴动作）及 6 类敌方机械原型（含 Boss 核心与左右独立机械臂），并通过 `BattleVfxManager` 在世界空间绘制斩击弧光、激光束、护盾罩与阵营底座光环。
2. **独立隐形摄像机实体与电影运镜**：
   - 客户端注册 `BattleCameraEntity` 与 `BattleCameraController`，进入战斗后强制切换 `FIRST_PERSON` 并 `setCameraEntity` 接管视角，彻底兼容 `ShoulderSurfing`，退战与登出自动归还相机。
   - 支持全景斜高俯视、切换目标微聚焦、大招瞄准肩后特写以及按 JSON 关键帧播放攻击冲刺推拉、环绕特写与震屏（`EntityViewRenderEvent.CameraSetup` / `FieldOfView` + `RenderLevelStageEvent` 矩阵投影/反投影）。
3. **多色原生发光、3D 点选与极简图形头顶条**：
   - 发光分色（`BattleActorEntity#isCurrentlyGlowing` / `getTeamColor`）：主目标金黄（`0xFFD700`）、扩散/群攻附带目标红橙（`0xFF5533`）、友方治疗/护盾/增益目标青绿（`0x38F2B0`）、当前行动角色天蓝（`0x55B8FF`）。
   - `TurnBasedBattleScreen` 支持直接 3D 射线点击敌方实体，敌方头顶仅显示 **像素风四属性弱点图标（`ElementIconRenderer`，`elem_*.png`）+ 白色韧性条（含 `BattleCombatPreview` 计算的闪烁预计削韧区段）+ 纯红血条**，不堆砌文字且不预告血量；顶部居中精简锁定条展示完整目标状态与行动意图。
   - 按 `1~4` 或点击头像卡先进入终结技瞄准预览态（展示大招预计削韧并切至肩后构图），再次确认/`Space`/`Enter` 施放，右键/`Esc`/`Q`/`E` 取消。
4. **数据驱动 56 份 JSON 关键帧引擎与 `/battle_anim` 指令**：
   - `BattleAnimClip` 与 `BattleAnimationRegistry` 从 `assets/picking_hope_core/battle_animations/*.json` 加载 56 份关键帧与运镜片段（支持 `config/picking_hope/battle_animations/*.json` 外部热重载覆盖），覆盖全部 6 名角色（普攻/战技/大招/瞄准/追击）与全部 6 类怪物共计 19 个差异化敌方招式。
   - 战斗中可输入 `/battle_anim reload`、`/battle_anim list`、`/battle_anim stop`、`/battle_anim play <clipId> [side actorSlot targetSlot]` 实时调试回放。

---

## 四、模块 B 详细设计（第 3 期预留架构规范）

1. 批量新增精致小巧 3D 体素装饰方块（电缆管道、全息监控屏、金属通风栅、警示护栏、科研台、医疗床/储物架、冰晶簇等）。
2. 结合原版楼梯、半砖、锁链、铁栏杆、灯笼、铁轨矿车、蓝冰、深板岩等方块，将星舰上下层、冰原科考站与地下矿坑统一挑高扩容至 8~12 格，做出拱梁吊顶、高低差地形、坠机坑与舷窗外残骸景深。
3. 旧存档保留角色与主线进度，登录时自动刷新新版建筑方块。
