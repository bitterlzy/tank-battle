package edu.tankbattle.server;

import com.google.gson.JsonObject;

import java.util.UUID;

public final class Player {
    private final int id;
    private final String sessionId = UUID.randomUUID().toString();
    private final String name;
    private final ClientSession session;
    private String roomName;
    private int x, y, health;
    private String direction = "down";
    private String color;
    private boolean ready;
    private boolean alive = true;
    private int kills, deaths;
    private long lastHeartbeat = System.currentTimeMillis();
    private long respawnAt;

    public Player(int id, String name, ClientSession session, String roomName,
                  int x, int y, int health, String color) {
        this.id=id; this.name=name; this.session=session; this.roomName=roomName;
        this.x=x; this.y=y; this.health=health; this.color=color;
    }
    public int id(){return id;} public String name(){return name;} public ClientSession session(){return session;}
    public String roomName(){return roomName;} public void roomName(String v){roomName=v;}
    public int x(){return x;} public int y(){return y;} public void position(int x,int y){this.x=x;this.y=y;}
    public int health(){return health;} public void health(int v){health=v;}
    public String direction(){return direction;} public void direction(String v){direction=v;}
    public String color(){return color;} public boolean ready(){return ready;} public void ready(boolean v){ready=v;}
    public boolean alive(){return alive;} public void alive(boolean v){alive=v;}
    public int kills(){return kills;} public void addKill(){kills++;}
    public int deaths(){return deaths;} public void addDeath(){deaths++;}
    public long lastHeartbeat(){return lastHeartbeat;} public void heartbeat(){lastHeartbeat=System.currentTimeMillis();}
    public long respawnAt(){return respawnAt;} public void respawnAt(long v){respawnAt=v;}
    public String sessionId(){return sessionId;}

    public JsonObject toJson(boolean includePrivate) {
        JsonObject o = Protocol.message("player");
        o.addProperty("playerId", id); o.addProperty("playerName", name);
        o.addProperty("x", x); o.addProperty("y", y); o.addProperty("direction", direction);
        o.addProperty("color", color); o.addProperty("health", health); o.addProperty("alive", alive);
        o.addProperty("ready", ready); o.addProperty("kills", kills); o.addProperty("deaths", deaths);
        o.addProperty("roomName", roomName);
        if (includePrivate) o.addProperty("sessionId", sessionId);
        return o;
    }
}
