package edu.tankbattle.client;

import javax.swing.SwingUtilities;

/** 客户端入口：当前只启动基础窗口。 */
public final class ClientMain {
    private ClientMain() {}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true);
        });
    }
}
