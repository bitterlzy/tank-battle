
package edu.tankbattle.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 客户端网络管理类。
 * 当前只负责建立和关闭 TCP 连接。
 */
public class NetworkClient {

    // 保存 TCP Socket 对象
    private Socket socket;

    /**
     * 连接服务器
     *
     * @param host 服务器 IP 地址
     * @param port 服务器端口
     */
    public void connect(String host, int port) throws IOException {
        // 创建 Socket 对象
        Socket newSocket = new Socket();

        try {
            // 最多等待 3 秒建立连接
            newSocket.connect(
                    new InetSocketAddress(host, port),
                    3000
            );

            // 连接成功后保存 Socket
            socket = newSocket;

        } catch (IOException e) {
            // 连接失败时关闭临时 Socket，避免资源泄漏
            try {
                newSocket.close();
            } catch (IOException ignored) {
                // 忽略关闭时产生的异常
            }

            throw e;
        }
    }

    /**
     * 判断当前是否已经建立 TCP 连接
     */
    public boolean isConnected() {
        return socket != null
                && socket.isConnected()
                && !socket.isClosed();
    }

    /**
     * 关闭 TCP 连接
     */
    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("关闭连接失败：" + e.getMessage());
            }
        }
    }
}
