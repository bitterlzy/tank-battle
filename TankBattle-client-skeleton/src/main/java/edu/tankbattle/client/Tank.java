package edu.tankbattle.client;

import java.awt.Color;

/** 坦克数据模型；当前只保存基础字段，不负责网络和绘制。 */
public class Tank {
    private int playerId;
    private String playerName;
    private int x;
    private int y;
    private String direction = "down";
    private Color color = Color.BLUE;
    private int health = 100;
    private boolean alive = true;

    public Tank() {
        // TODO：后续根据服务端 state 消息初始化坦克数据。
    }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    // TODO：后续增加位置更新、移动规则或状态转换方法。
}
