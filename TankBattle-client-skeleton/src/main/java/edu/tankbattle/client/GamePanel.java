package edu.tankbattle.client;

import javax.swing.*;
import java.awt.*;

/** 游戏绘制区域。 */
public class GamePanel extends JPanel {
    public GamePanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(new Color(35, 45, 42));
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        setFocusable(true);
        // TODO：绘制地图、障碍物、坦克、子弹和其他游戏对象。
        // TODO：后续处理键盘输入。
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        String message = "GamePanel：游戏绘制区域（TODO）";
        FontMetrics metrics = g.getFontMetrics();
        int x = (getWidth() - metrics.stringWidth(message)) / 2;
        g.drawString(message, Math.max(10, x), getHeight() / 2);
    }
}
