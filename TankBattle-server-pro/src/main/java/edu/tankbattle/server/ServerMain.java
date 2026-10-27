package edu.tankbattle.server;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServerMain {
    private static final Logger LOG = Logger.getLogger(ServerMain.class.getName());

    private ServerMain() {}

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load(args);
        GameServer server = new GameServer(config);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("正在关闭服务器...");
            server.close();
        }, "shutdown-hook"));
        server.start();
        LOG.info(() -> "坦克大战服务器已启动，监听 " + config.host() + ":" + config.port());
        new CountDownLatch(1).await();
    }
}
