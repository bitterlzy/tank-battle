# TankBattle Server Pro（Java 17）

这是一个面向教学项目、便于继续扩展的坦克大战服务器。服务端独立运行，客户端通过 TCP + UTF-8 JSON Lines 通信。每条消息为一行 JSON，以 `\n` 结束。

## 1. 环境与启动

- JDK 17 或更高版本
- Maven 3.8+
- 默认监听 `0.0.0.0:5000`

在项目根目录执行：

```bash
mvn clean package
java -jar target/tankbattle-server-2.0.0.jar
```

指定端口：

```bash
java -jar target/tankbattle-server-2.0.0.jar 6000
```

要修改更多参数，把 `src/main/resources/server.properties` 复制到项目根目录下的 `config/server.properties`，修改复制后的文件再启动。外部配置优先于 jar 内置默认配置。

Windows 防火墙需要允许服务器 TCP 端口入站；同一台电脑测试可用 `127.0.0.1`，局域网客户端填写服务器电脑的局域网 IP。

## 2. 已实现功能

- 多客户端 TCP 接入、玩家 ID 分配、昵称校验、断线清理
- 心跳 `ping/pong` 与超时踢除
- 房间列表、创建房间、加入房间、默认大厅
- 服务端权威位置边界校验、移动速度限制、障碍物碰撞
- 玩家状态快照广播：位置、方向、颜色、生命值、存活状态、准备状态、击杀/死亡统计
- 房间聊天与系统消息
- 准备状态、开始游戏消息
- 服务端子弹模拟、障碍物/坦克命中检测、伤害结算、击杀/死亡统计、复活请求/倒计时校验
- 错误消息、配置文件、日志、资源上限和可扩展消息分发结构

**重要说明：**这是可扩展的教学服务器，不是生产级联机游戏引擎。服务端已经处理子弹移动、障碍物/坦克命中、伤害与击杀统计；客户端仍需根据 `state` / `bullet` / `tank_destroyed` 消息渲染效果。账号密码、数据库持久化、TLS、断线重连恢复、团队阵营与完整反作弊仍未实现。当前 `friendly-fire` 是预留配置，尚无队伍/阵营模型，因此不会改变命中规则。上线公网前还应补齐鉴权、速率限制和安全测试。

## 3. 消息协议

### 客户端 -> 服务端

加入（首次连接必须发送）：

```json
{"type":"join","playerName":"张三"}
```

指定房间加入：

```json
{"type":"join","playerName":"张三","roomName":"大厅"}
```

移动：

```json
{"type":"move","x":120,"y":160,"direction":"up"}
```

心跳：

```json
{"type":"ping","clientTime":1730000000000}
```

聊天：

```json
{"type":"chat","message":"大家好"}
```

房间列表：

```json
{"type":"room_list"}
```

创建房间（需先 join）：

```json
{"type":"create_room","roomName":"练习房"}
```

切换房间：

```json
{"type":"join_room","roomName":"练习房"}
```

准备/取消准备：

```json
{"type":"ready","ready":true}
```

开始游戏：

```json
{"type":"start_game"}
```

射击事件：

```json
{"type":"shoot","direction":"up"}
```

请求复活：

```json
{"type":"respawn"}
```

请求当前快照：

```json
{"type":"get_state"}
```

### 服务端 -> 客户端

- `join_result`: 加入结果、玩家 ID、地图宽高、坦克尺寸
- `state`: 房间内完整玩家快照、障碍物、游戏状态和服务器时间
- `pong`: 心跳应答
- `chat`: 聊天消息
- `system`: 系统消息
- `room_list`: 房间列表
- `room_created` / `room_joined` / `game_started`: 房间与游戏事件
- `bullet`: 射击事件（由客户端渲染）
- `error`: 错误码与提示

所有消息都是 JSON 对象并独占一行。客户端不能把多条 JSON 连在同一行发送。

## 4. 主要配置项

| 配置 | 作用 |
|---|---|
| `server.host` / `server.port` | 监听地址与端口 |
| `server.max-clients` | 最大 TCP 连接数 |
| `server.read-timeout-ms` | 预留的读取超时配置 |
| `server.heartbeat-timeout-ms` | 心跳超时 |
| `game.width` / `game.height` | 地图大小 |
| `game.tank-size` | 坦克尺寸 |
| `game.move-step` | 单次移动允许的基础步长 |
| `game.max-health` | 最大生命值 |
| `game.enable-obstacles` | 是否启用障碍物 |
| `game.obstacle-count` | 每个房间障碍物数量 |
| `game.friendly-fire` | 友伤策略预留（当前无队伍系统） |

## 5. 客户端实现约定

客户端建议使用独立 `NetworkClient` 线程读服务端消息，Swing UI 更新通过 `SwingUtilities.invokeLater`。移动时发送当前坐标与方向；收到 `state` 后以服务器快照为准刷新其他坦克。客户端启动时先发送 `join`，收到 `join_result.success=true` 后进入游戏界面。

## 6. 扩展建议

建议后续按职责拆分并逐步替换/增强：
- `MessageHandler`：按消息类型注册处理器
- `CombatService` + `BulletManager`：子弹模拟、命中检测、伤害与击杀统计
- `MapService`：地图加载、障碍物配置和碰撞策略
- `AccountService` / `PersistenceService`：账号与战绩持久化
- `ReconnectService`：会话令牌和断线恢复
- `RoomService`：房间权限、人数限制、观战与匹配
- 单元测试：协议解析、移动边界、碰撞、房间生命周期
