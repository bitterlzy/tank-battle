package edu.tankbattle.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record ServerConfig(
        String host, int port, int maxClients, int maxLineBytes,
        int readTimeoutMs, int heartbeatTimeoutMs, int broadcastIntervalMs,
        int worldWidth, int worldHeight, int tankSize, int moveStep, int spawnMargin,
        int maxNicknameLength, int maxChatLength, int maxRooms, String defaultRoom,
        boolean friendlyFire, int respawnDelayMs, int maxHealth, int bulletSpeed,
        int bulletDamage, int bulletLifeMs, int maxBulletsPerPlayer,
        boolean enableObstacles, int obstacleCount) {

    public static ServerConfig load(String[] args) throws IOException {
        Properties p = new Properties();
        Path external = Path.of("config", "server.properties");
        if (Files.exists(external)) {
            try (InputStream in = Files.newInputStream(external)) { p.load(in); }
        } else {
            try (InputStream in = ServerConfig.class.getResourceAsStream("/server.properties")) {
                if (in != null) p.load(in);
            }
        }
        String host = p.getProperty("server.host", "0.0.0.0");
        int port = integer(p, "server.port", 5000);
        if (args.length > 0) port = Integer.parseInt(args[0]);
        return new ServerConfig(
                host, port, integer(p,"server.max-clients",100),
                integer(p,"server.max-line-bytes",16384), integer(p,"server.read-timeout-ms",45000),
                integer(p,"server.heartbeat-timeout-ms",90000), integer(p,"server.broadcast-interval-ms",100),
                integer(p,"game.width",800), integer(p,"game.height",600), integer(p,"game.tank-size",32),
                integer(p,"game.move-step",8), integer(p,"game.spawn-margin",48),
                integer(p,"game.max-nickname-length",16), integer(p,"game.max-chat-length",200),
                integer(p,"game.max-rooms",100), p.getProperty("game.default-room","大厅"),
                bool(p,"game.friendly-fire",false), integer(p,"game.respawn-delay-ms",3000),
                integer(p,"game.max-health",100), integer(p,"game.bullet-speed",12),
                integer(p,"game.bullet-damage",25), integer(p,"game.bullet-life-ms",2000),
                integer(p,"game.max-bullets-per-player",5), bool(p,"game.enable-obstacles",true),
                integer(p,"game.obstacle-count",8));
    }

    private static int integer(Properties p, String k, int d) {
        try { return Integer.parseInt(p.getProperty(k, String.valueOf(d)).trim()); }
        catch (RuntimeException e) { return d; }
    }
    private static boolean bool(Properties p, String k, boolean d) {
        return Boolean.parseBoolean(p.getProperty(k, String.valueOf(d)));
    }
}
