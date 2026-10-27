package edu.tankbattle.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class GameRoom {
    private final String name;
    private final Map<Integer, Player> players = new ConcurrentHashMap<>();
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final Map<String, Bullet> bullets = new ConcurrentHashMap<>();
    private volatile String state = "waiting";
    private volatile long startedAt;

    public record Obstacle(int x, int y, int width, int height) {
        JsonObject toJson() {
            JsonObject o=new JsonObject(); o.addProperty("x",x); o.addProperty("y",y);
            o.addProperty("width",width); o.addProperty("height",height); return o;
        }
    }
    public GameRoom(String name, ServerConfig config, int seed) {
        this.name=name;
        if (config.enableObstacles()) {
            Random r=new Random(seed);
            for(int i=0;i<config.obstacleCount();i++) {
                int x=90+r.nextInt(Math.max(1,config.worldWidth()-180));
                int y=80+r.nextInt(Math.max(1,config.worldHeight()-160));
                obstacles.add(new Obstacle(x,y,40+r.nextInt(45),24+r.nextInt(35)));
            }
        }
    }
    public String name(){return name;} public String state(){return state;}
    public void state(String s){state=s;} public long startedAt(){return startedAt;}
    public void startedAt(long v){startedAt=v;}
    public Collection<Player> players(){return players.values();}
    public void add(Player p){players.put(p.id(),p);}
    public Player remove(int id){return players.remove(id);}
    public Player get(int id){return players.get(id);}
    public List<Obstacle> obstacles(){return Collections.unmodifiableList(obstacles);}
    public Collection<Bullet> bullets(){return bullets.values();}
    public void addBullet(Bullet bullet){bullets.put(bullet.id(),bullet);}
    public void removeBullet(String id){bullets.remove(id);}
    public boolean intersectsObstacle(int x,int y,int size) {
        for(Obstacle o:obstacles) if(x<o.x()+o.width() && x+size>o.x() && y<o.y()+o.height() && y+size>o.y()) return true;
        return false;
    }
    public JsonObject snapshot() {
        JsonObject out=Protocol.message("state");
        out.addProperty("roomName",name); out.addProperty("gameState",state);
        JsonArray list=new JsonArray();
        players.values().stream().sorted(Comparator.comparingInt(Player::id)).forEach(p->list.add(p.toJson(false)));
        out.add("players",list);
        JsonArray bulletList=new JsonArray();
        bullets.values().forEach(b -> bulletList.add(b.toJson()));
        out.add("bullets",bulletList);
        JsonArray obs=new JsonArray(); for(Obstacle o:obstacles) obs.add(o.toJson()); out.add("obstacles",obs);
        out.addProperty("serverTime",System.currentTimeMillis());
        return out;
    }
    public JsonObject roomInfo() {
        JsonObject o=new JsonObject(); o.addProperty("roomName",name); o.addProperty("playerCount",players.size());
        o.addProperty("gameState",state); return o;
    }
}
