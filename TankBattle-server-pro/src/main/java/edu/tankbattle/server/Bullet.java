package edu.tankbattle.server;

import com.google.gson.JsonObject;
import java.util.UUID;

public final class Bullet {
    private final String id = UUID.randomUUID().toString();
    private final int ownerId;
    private final String direction;
    private final int damage;
    private final long expiresAt;
    private int x, y;

    public Bullet(int ownerId, int x, int y, String direction, int damage, int speed, long expiresAt) {
        this.ownerId=ownerId; this.x=x; this.y=y; this.direction=direction;
        this.damage=damage; this.expiresAt=expiresAt;
    }
    public String id(){return id;} public int ownerId(){return ownerId;}
    public String direction(){return direction;} public int damage(){return damage;}
    public long expiresAt(){return expiresAt;} public int x(){return x;} public int y(){return y;}
    public void advance(int speed) {
        switch(direction) { case "up" -> y-=speed; case "down" -> y+=speed;
            case "left" -> x-=speed; case "right" -> x+=speed; }
    }
    public JsonObject toJson() {
        JsonObject o=Protocol.message("bullet");
        o.addProperty("bulletId",id); o.addProperty("ownerId",ownerId);
        o.addProperty("x",x); o.addProperty("y",y); o.addProperty("direction",direction);
        o.addProperty("damage",damage); o.addProperty("expiresAt",expiresAt); return o;
    }
}
