package edu.tankbattle.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ClientSession implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(ClientSession.class.getName());
    private static final Gson GSON = new Gson();
    private final GameServer server;
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final Object writeLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile Player player;

    public ClientSession(GameServer server, Socket socket) throws IOException {
        this.server=server; this.socket=socket;
        this.reader=new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }
    public Player player(){return player;} public void player(Player p){player=p;}
    public boolean isClosed(){return closed.get();}

    public void run() {
        try {
            String line;
            while (!closed.get() && (line=reader.readLine()) != null) {
                if (line.getBytes(StandardCharsets.UTF_8).length > server.config().maxLineBytes()) {
                    sendError("MESSAGE_TOO_LARGE", "消息超过长度限制"); break;
                }
                try {
                    JsonObject msg = Protocol.parse(line);
                    server.handle(this, msg);
                } catch (Exception ex) {
                    sendError("BAD_MESSAGE", "消息格式错误或参数无效");
                    LOG.log(Level.FINE, "Bad client message", ex);
                }
            }
        } catch (IOException e) {
            LOG.fine(() -> "客户端连接结束：" + e.getMessage());
        } finally {
            server.disconnect(this);
            close();
        }
    }

    public void send(JsonObject message) {
        if (closed.get()) return;
        String line=GSON.toJson(message);
        synchronized (writeLock) {
            try { writer.write(line); writer.write('\n'); writer.flush(); }
            catch (IOException e) { close(); }
        }
    }
    public void sendError(String code, String message) {
        JsonObject o=Protocol.message("error"); o.addProperty("code",code); o.addProperty("message",message); send(o);
    }
    @Override public void close() {
        if (!closed.compareAndSet(false,true)) return;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
