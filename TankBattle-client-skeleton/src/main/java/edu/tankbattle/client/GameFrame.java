package edu.tankbattle.client;

import javax.swing.*;
import java.awt.*;

/** 主窗口：负责布局，具体业务功能后续逐步实现。 */
public class GameFrame extends JFrame {
    private final GamePanel gamePanel;

    // 登录界面需要使用的输入框
    private JTextField ipField;
    private JTextField portField;
    private JTextField nameField;

    private JPanel mainPanel;   // 保存主游戏界面，后续用于登录成功后切换
    
    private NetworkClient networkClient;    // 管理客户端与服务器之间的 TCP 连接

    public GameFrame() {
        setTitle("Tank Battle - 客户端");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        gamePanel = new GamePanel();

        setLayout(new BorderLayout(8, 8));
        add(createHeader(), BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);
        add(createSidePanel(), BorderLayout.EAST);

        setContentPane(createLoginPanel());
    }

    private JComponent createHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JLabel title = new JLabel("坦克大战客户端");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        JLabel status = new JLabel("连接状态：尚未连接");
        panel.add(title, BorderLayout.WEST);
        panel.add(status, BorderLayout.EAST);
        // TODO：保存状态标签，并根据网络状态更新。
        return panel;
    }

    private JComponent createSidePanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(220, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("游戏信息"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        panel.add(new JLabel("玩家列表："));
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("（TODO：显示在线玩家）"));
        panel.add(Box.createVerticalStrut(20));
        panel.add(new JLabel("操作说明："));
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("（TODO：显示键盘操作）"));
        panel.add(Box.createVerticalGlue());
        // TODO：后续增加玩家列表、聊天区和房间信息。
        return panel;
    }

    public GamePanel getGamePanel() { return gamePanel; }

    /**
     * 创建登录界面
     */
    private JComponent createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("玩家登录"),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 服务器 IP
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("服务器 IP："), gbc);

        gbc.gridx = 1;
        ipField = new JTextField("127.0.0.1", 18);
        formPanel.add(ipField, gbc);

        // 服务器端口
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("服务器端口："), gbc);

        gbc.gridx = 1;
        portField = new JTextField("8888", 18);
        formPanel.add(portField, gbc);

        // 玩家昵称
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("玩家昵称："), gbc);

        gbc.gridx = 1;
        nameField = new JTextField(18);
        formPanel.add(nameField, gbc);

        // 登录按钮
        JButton loginButton = new JButton("连接服务器");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        formPanel.add(loginButton, gbc);

        // 显示校验结果
        JLabel messageLabel = new JLabel("请输入服务器信息和玩家昵称");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        gbc.gridy = 4;
        formPanel.add(messageLabel, gbc);

        // 监听按钮点击事件
        loginButton.addActionListener(e -> {
            String ip = ipField.getText().trim();
            String portText = portField.getText().trim();
            String playerName = nameField.getText().trim();

            // 1. 检查 IP
            if (ip.isEmpty()) {
                messageLabel.setText("请输入服务器 IP 地址");
                return;
            }

            // 2. 检查端口格式
            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException ex) {
                messageLabel.setText("端口必须是数字");
                return;
            }

            // 3. 检查端口范围
            if (port < 1 || port > 65535) {
                messageLabel.setText("端口范围必须是 1～65535");
                return;
            }

            // 4. 检查昵称
            if (playerName.isEmpty()) {
                messageLabel.setText("请输入玩家昵称");
                return;
            }

            if (playerName.length() > 20) {
                messageLabel.setText("玩家昵称不能超过 20 个字符");
                return;
            }

            // 5. 防止重复点击
            loginButton.setEnabled(false);
            messageLabel.setText("正在连接服务器……");

            // 6. 在后台线程执行网络连接，避免界面卡住
            SwingWorker<String, Void> worker = new SwingWorker<>() {

                @Override
                protected String doInBackground() {
                    NetworkClient client = new NetworkClient();

                    try {
                        client.connect(ip, port);
                        networkClient = client;
                        return null; // null 表示 TCP 连接成功

                    } catch (Exception ex) {
                        client.close();
                        return ex.getMessage();
                    }
                }

                @Override
                protected void done() {
                    loginButton.setEnabled(true);

                    try {
                        String error = get();

                        if (error == null) {
                            messageLabel.setText("TCP 连接成功！");
                            System.out.println("服务器 IP：" + ip);
                            System.out.println("服务器端口：" + port);
                            System.out.println("玩家昵称：" + playerName);
                        } else {
                            messageLabel.setText("连接失败：" + error);
                            networkClient = null;
                        }

                    } catch (Exception ex) {
                        messageLabel.setText("连接失败：" + ex.getMessage());
                        networkClient = null;
                    }
                }
            };

            worker.execute();
        });

        panel.add(formPanel);
        return panel;
    }
}
