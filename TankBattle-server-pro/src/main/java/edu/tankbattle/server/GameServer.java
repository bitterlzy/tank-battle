package edu.tankbattle.server;

import com.google.gson.*;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class GameServer implements AutoCloseable {
    private static final Logger LOG=Logger.getLogger(GameServer.class.getName());
    private static final Set<String> DIRECTIONS=Set.of("up","down","left","right");
    private static final String[] COLORS={"#E74C3C","#3498DB","#2ECC71","#F1C40F","#9B59B6","#E67E22","#1ABC9C","#ECF0F1"};
    private final ServerConfig config;
    private final AtomicInteger ids=new AtomicInteger(1000);
    private final Set<ClientSession> sessions=ConcurrentHashMap.newKeySet();
    private final Map<String,GameRoom> rooms=new ConcurrentHashMap<>();
    private final ExecutorService clients=Executors.newCachedThreadPool(r->{Thread t=new Thread(r,"client-handler");t.setDaemon(true);return t;});
    private final ScheduledExecutorService scheduler=Executors.newScheduledThreadPool(2,r->{Thread t=new Thread(r,"game-tick");t.setDaemon(true);return t;});
    private volatile ServerSocket serverSocket;
    private volatile boolean running;

    public GameServer(ServerConfig config) {
        this.config=config;
        rooms.put(config.defaultRoom(),new GameRoom(config.defaultRoom(),config,1));
    }
    public ServerConfig config(){return config;}

    public void start() throws IOException {
        serverSocket=new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(config.host(),config.port()));
        running=true;
        scheduler.scheduleAtFixedRate(this::tick,100,100,TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::checkHeartbeats,5,5,TimeUnit.SECONDS);
        Thread acceptor=new Thread(this::acceptLoop,"accept-loop"); acceptor.setDaemon(true); acceptor.start();
    }
    private void acceptLoop() {
        while(running) {
            try {
                Socket socket=serverSocket.accept();
                if(sessions.size()>=config.maxClients()) {
                    socket.close(); continue;
                }
                socket.setTcpNoDelay(true); socket.setKeepAlive(true);
                ClientSession session=new ClientSession(this,socket); sessions.add(session);
                clients.submit(session::run);
            } catch(SocketException e) { if(running) LOG.warning("Accept socket error: "+e.getMessage()); }
            catch(IOException e) { if(running) LOG.warning("Accept failed: "+e.getMessage()); }
        }
    }

    public void handle(ClientSession s, JsonObject msg) {
        String type=Protocol.string(msg,"type","");
        switch(type) {
            case "join" -> join(s,msg);
            case "move" -> move(s,msg);
            case "ping" -> ping(s,msg);
            case "leave" -> disconnect(s);
            case "chat" -> chat(s,msg);
            case "room_list" -> roomList(s);
            case "create_room" -> createRoom(s,msg);
            case "join_room" -> joinRoom(s,msg);
            case "ready" -> ready(s,msg);
            case "start_game" -> startGame(s);
            case "shoot" -> shoot(s,msg);
            case "respawn" -> respawn(s);
            case "get_state" -> sendState(s);
            default -> s.sendError("UNKNOWN_TYPE","不支持的消息类型："+type);
        }
    }

    private void join(ClientSession s, JsonObject msg) {
        if(s.player()!=null) { s.sendError("ALREADY_JOINED","当前连接已经加入游戏"); return; }
        String name=Protocol.string(msg,"playerName","").trim();
        if(name.isEmpty() || name.length()>config.maxNicknameLength()) {
            s.sendError("BAD_NAME","昵称不能为空，且长度不能超过"+config.maxNicknameLength()); return;
        }
        String roomName=Protocol.string(msg,"roomName",config.defaultRoom()).trim();
        if(roomName.isEmpty()) roomName=config.defaultRoom();
        GameRoom room=rooms.get(roomName);
        if(room==null) { s.sendError("ROOM_NOT_FOUND","房间不存在，请先创建或选择其他房间"); return; }
        int id=ids.incrementAndGet();
        int spawnIndex=room.players().size();
        int x=config.spawnMargin()+(spawnIndex%8)*Math.max(config.tankSize()+12,44);
        int y=config.spawnMargin()+(spawnIndex/8)*Math.max(config.tankSize()+12,44);
        x=Math.min(x,config.worldWidth()-config.tankSize()); y=Math.min(y,config.worldHeight()-config.tankSize());
        Player p=new Player(id,name,s,roomName,x,y,config.maxHealth(),COLORS[Math.floorMod(id,COLORS.length)]);
        s.player(p); p.heartbeat(); room.add(p);
        JsonObject result=Protocol.message("join_result");
        result.addProperty("success",true); result.addProperty("playerId",id);
        result.addProperty("playerName",name); result.addProperty("roomName",roomName);
        result.addProperty("message","连接成功"); result.addProperty("worldWidth",config.worldWidth());
        result.addProperty("worldHeight",config.worldHeight()); result.addProperty("tankSize",config.tankSize());
        s.send(result);
        broadcastState(room);
        broadcastSystem(room,name+" 加入了房间");
        LOG.info("玩家加入：id="+id+", name="+name+", room="+roomName);
    }

    private void move(ClientSession s, JsonObject msg) {
        Player p=requirePlayer(s); if(p==null) return;
        if(!p.alive()) { s.sendError("PLAYER_DEAD","当前坦克已被击毁"); return; }
        int x=Protocol.integer(msg,"x",p.x()), y=Protocol.integer(msg,"y",p.y());
        String direction=Protocol.string(msg,"direction",p.direction()).toLowerCase(Locale.ROOT);
        if(!DIRECTIONS.contains(direction)) { s.sendError("BAD_DIRECTION","方向必须为 up/down/left/right"); return; }
        x=clamp(x,0,config.worldWidth()-config.tankSize());
        y=clamp(y,0,config.worldHeight()-config.tankSize());
        // Server validates maximum per-message displacement to reduce teleporting.
        int maxDelta=config.moveStep()*4;
        if(Math.abs(x-p.x())>maxDelta || Math.abs(y-p.y())>maxDelta) {
            s.sendError("MOVE_TOO_FAST","移动距离过大"); return;
        }
        GameRoom room=rooms.get(p.roomName());
        if(room!=null && room.intersectsObstacle(x,y,config.tankSize())) {
            // Keep old position; still update direction so the sprite can face the requested way.
            p.direction(direction); broadcastState(room); return;
        }
        p.position(x,y); p.direction(direction); p.heartbeat();
        if(room!=null) broadcastState(room);
    }

    private void ping(ClientSession s, JsonObject msg) {
        Player p=s.player();
        if(p!=null) p.heartbeat();
        JsonObject pong=Protocol.message("pong"); pong.addProperty("serverTime",System.currentTimeMillis());
        pong.addProperty("clientTime",Protocol.longValue(msg,"clientTime",0)); s.send(pong);
    }
    private void chat(ClientSession s, JsonObject msg) {
        Player p=requirePlayer(s); if(p==null)return;
        String content=Protocol.string(msg,"message","").trim();
        if(content.isEmpty() || content.length()>config.maxChatLength()) {
            s.sendError("BAD_CHAT","聊天内容不能为空或超过长度限制"); return;
        }
        JsonObject out=Protocol.message("chat"); out.addProperty("playerId",p.id());
        out.addProperty("playerName",p.name()); out.addProperty("message",content);
        out.addProperty("time",System.currentTimeMillis()); broadcast(rooms.get(p.roomName()),out);
    }
    private void roomList(ClientSession s) {
        JsonObject out=Protocol.message("room_list"); JsonArray list=new JsonArray();
        rooms.values().stream().sorted(Comparator.comparing(GameRoom::name)).forEach(r->list.add(r.roomInfo()));
        out.add("rooms",list); s.send(out);
    }
    private void createRoom(ClientSession s, JsonObject msg) {
        Player p=requirePlayer(s); if(p==null)return;
        String name=Protocol.string(msg,"roomName","").trim();
        if(name.isEmpty() || name.length()>24) { s.sendError("BAD_ROOM_NAME","房间名长度必须为1至24"); return; }
        if(rooms.size()>=config.maxRooms()) { s.sendError("ROOM_LIMIT","房间数量已达上限"); return; }
        GameRoom room=new GameRoom(name,config,name.hashCode());
        GameRoom previous=rooms.putIfAbsent(name,room);
        if(previous!=null) { s.sendError("ROOM_EXISTS","房间名已存在"); return; }
        s.send(Protocol.message("room_created")); roomList(s);
    }
    private void joinRoom(ClientSession s, JsonObject msg) {
        Player p=requirePlayer(s); if(p==null)return;
        String name=Protocol.string(msg,"roomName","").trim();
        GameRoom next=rooms.get(name); if(next==null) { s.sendError("ROOM_NOT_FOUND","房间不存在"); return; }
        if(next==rooms.get(p.roomName())) { sendState(s); return; }
        if(next.state().equals("playing")) { s.sendError("ROOM_IN_GAME","游戏进行中，不能加入"); return; }
        GameRoom old=rooms.get(p.roomName());
        if(old!=null) { old.remove(p.id()); broadcastState(old); broadcastSystem(old,p.name()+" 离开了房间"); }
        p.roomName(name); p.ready(false); p.position(config.spawnMargin(),config.spawnMargin()); next.add(p);
        JsonObject o=Protocol.message("room_joined"); o.addProperty("roomName",name); s.send(o);
        broadcastState(next); broadcastSystem(next,p.name()+" 加入了房间");
    }
    private void ready(ClientSession s, JsonObject msg) {
        Player p=requirePlayer(s); if(p==null)return;
        p.ready(msg.has("ready") ? msg.get("ready").getAsBoolean() : true);
        GameRoom r=rooms.get(p.roomName()); if(r!=null) broadcastState(r);
    }
    private void startGame(ClientSession s) {
        Player p=requirePlayer(s); if(p==null)return;
        GameRoom r=rooms.get(p.roomName()); if(r==null)return;
        if(r.state().equals("playing")) { s.sendError("GAME_ALREADY_STARTED","游戏已经开始"); return; }
        if(r.players().isEmpty()) { s.sendError("ROOM_EMPTY","房间没有玩家"); return; }
        r.state("playing"); r.startedAt(System.currentTimeMillis());
        for(Player member:r.players()) { member.health(config.maxHealth()); member.alive(true); member.ready(false); }
        JsonObject o=Protocol.message("game_started"); o.addProperty("roomName",r.name()); o.addProperty("startedAt",r.startedAt());
        broadcast(r,o); broadcastState(r);
    }
    private void shoot(ClientSession s, JsonObject msg) {
        Player shooter=requirePlayer(s); if(shooter==null)return;
        GameRoom room=rooms.get(shooter.roomName()); if(room==null)return;
        if(!room.state().equals("playing")) { s.sendError("GAME_NOT_STARTED","游戏尚未开始"); return; }
        if(!shooter.alive()) { s.sendError("PLAYER_DEAD","当前坦克已被击毁"); return; }
        String dir=Protocol.string(msg,"direction",shooter.direction()).toLowerCase(Locale.ROOT);
        if(!DIRECTIONS.contains(dir)) { s.sendError("BAD_DIRECTION","无效的射击方向"); return; }
        long now=System.currentTimeMillis();
        long activeBullets=room.bullets().stream().filter(b->b.ownerId()==shooter.id()).count();
        if(activeBullets>=config.maxBulletsPerPlayer()) {
            s.sendError("TOO_MANY_BULLETS","当前子弹数量已达上限"); return;
        }
        int bx=shooter.x()+config.tankSize()/2, by=shooter.y()+config.tankSize()/2;
        switch(dir) { case "up" -> by-=config.tankSize()/2; case "down" -> by+=config.tankSize()/2;
            case "left" -> bx-=config.tankSize()/2; case "right" -> bx+=config.tankSize()/2; }
        Bullet bullet=new Bullet(shooter.id(),bx,by,dir,config.bulletDamage(),config.bulletSpeed(),now+config.bulletLifeMs());
        room.addBullet(bullet);
        broadcast(room,bullet.toJson());
    }
    private void respawn(ClientSession s) {
        Player p=requirePlayer(s); if(p==null)return;
        if(p.alive())return;
        if(System.currentTimeMillis()<p.respawnAt()) { s.sendError("RESPAWN_TOO_EARLY","复活倒计时尚未结束"); return; }
        p.alive(true); p.health(config.maxHealth()); p.position(config.spawnMargin(),config.spawnMargin());
        GameRoom r=rooms.get(p.roomName()); if(r!=null) { broadcastSystem(r,p.name()+" 已复活"); broadcastState(r); }
    }
    private Player requirePlayer(ClientSession s) {
        Player p=s.player();
        if(p==null) { s.sendError("NOT_JOINED","请先发送 join 消息"); return null; }
        return p;
    }
    private void sendState(ClientSession s) {
        Player p=s.player(); if(p==null){s.sendError("NOT_JOINED","请先加入游戏");return;}
        GameRoom r=rooms.get(p.roomName()); if(r!=null)s.send(r.snapshot());
    }
    private void broadcastState(GameRoom r) { if(r!=null) broadcast(r,r.snapshot()); }
    private void broadcastSystem(GameRoom r,String message) {
        if(r==null)return; JsonObject o=Protocol.message("system"); o.addProperty("message",message);
        o.addProperty("time",System.currentTimeMillis()); broadcast(r,o);
    }
    private void broadcast(GameRoom room,JsonObject msg) {
        if(room==null)return;
        for(Player p:room.players()) p.session().send(msg);
    }
    private int clamp(int v,int min,int max){return Math.max(min,Math.max(0,Math.min(v,max)));}
    private void tick() {
        long now=System.currentTimeMillis();
        for(GameRoom room:rooms.values()) {
            if(!room.state().equals("playing")) continue;
            boolean changed=false;
            for(Bullet bullet:new ArrayList<>(room.bullets())) {
                if(now>=bullet.expiresAt()) { room.removeBullet(bullet.id()); changed=true; continue; }
                bullet.advance(config.bulletSpeed());
                if(bullet.x()<0 || bullet.y()<0 || bullet.x()>=config.worldWidth()
                        || bullet.y()>=config.worldHeight()
                        || room.intersectsObstacle(bullet.x(),bullet.y(),6)) {
                    room.removeBullet(bullet.id()); changed=true; continue;
                }
                Player hit=null;
                for(Player target:room.players()) {
                    if(!target.alive() || target.id()==bullet.ownerId()) continue;
                    int tx=target.x(), ty=target.y(), size=config.tankSize();
                    if(bullet.x()>=tx && bullet.x()<=tx+size && bullet.y()>=ty && bullet.y()<=ty+size) { hit=target; break; }
                }
                if(hit!=null) {
                    room.removeBullet(bullet.id()); changed=true;
                    hit.health(Math.max(0,hit.health()-bullet.damage()));
                    if(hit.health()==0) {
                        hit.alive(false); hit.addDeath(); hit.respawnAt(now+config.respawnDelayMs());
                        Player owner=room.get(bullet.ownerId());
                        if(owner!=null) owner.addKill();
                        JsonObject destroyed=Protocol.message("tank_destroyed");
                        destroyed.addProperty("playerId",hit.id()); destroyed.addProperty("playerName",hit.name());
                        destroyed.addProperty("respawnAt",hit.respawnAt());
                        if(owner!=null) { destroyed.addProperty("killerId",owner.id()); destroyed.addProperty("killerName",owner.name()); }
                        broadcast(room,destroyed);
                    }
                }
            }
            if(changed) broadcastState(room);
        }
    }
    private void checkHeartbeats() {
        long now=System.currentTimeMillis();
        for(ClientSession s:sessions) {
            Player p=s.player();
            if(p!=null && now-p.lastHeartbeat()>config.heartbeatTimeoutMs()) {
                s.close(); disconnect(s);
            }
        }
    }
    public void disconnect(ClientSession s) {
        sessions.remove(s);
        Player p=s.player();
        if(p==null)return;
        s.player(null);
        GameRoom r=rooms.get(p.roomName());
        if(r!=null) {
            r.remove(p.id()); broadcastState(r); broadcastSystem(r,p.name()+" 已断开连接");
            if(r.players().isEmpty() && !r.name().equals(config.defaultRoom())) rooms.remove(r.name(),r);
        }
        LOG.info("玩家断开：id="+p.id()+", name="+p.name());
    }
    @Override public void close() {
        running=false;
        try { if(serverSocket!=null)serverSocket.close(); } catch(IOException ignored) {}
        for(ClientSession s:sessions) s.close();
        scheduler.shutdownNow(); clients.shutdownNow();
    }
}
