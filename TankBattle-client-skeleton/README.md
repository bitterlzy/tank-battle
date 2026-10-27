# TankBattle Client（客户端骨架）

当前阶段仅建立 Java Swing 客户端结构，业务功能保留为 TODO，按教学步骤逐步实现。

## 环境
- JDK 17+
- Maven 3.8+
- Swing（JDK 自带）
- Gson（后续实现 JSON 协议时使用）

## 启动
在项目根目录执行：

```bash
mvn clean package
java -jar target/tankbattle-client-1.0.0.jar
```

当前只显示基础窗口，不会连接服务器，也没有登录、坦克移动或消息同步功能。

## 类职责
- `ClientMain`：程序入口。
- `GameFrame`：主窗口和布局。
- `GamePanel`：游戏绘制区域。
- `Tank`：坦克数据模型。
- `NetworkClient`：网络通信骨架。

## 建议实现顺序
1. 理解并运行当前空壳窗口。
2. 增加服务器 IP、端口和昵称登录界面。
3. 实现 Socket 连接与断开。
4. 实现 JSON Lines 协议和加入房间消息。
5. 接收 `state` 消息并转换为坦克数据。
6. 绘制本地坦克和其他玩家坦克。
7. 加入 WASD 键盘控制与位置同步。
8. 完善连接状态、错误提示和玩家列表。

客户端与服务端使用 UTF-8 JSON Lines，每条消息独占一行。
