package edu.tankbattle.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class Protocol {
    private Protocol() {}

    public static JsonObject parse(String line) {
        return JsonParser.parseString(line).getAsJsonObject();
    }
    public static String string(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }
    public static int integer(JsonObject o, String key, int fallback) {
        try { return o.has(key) ? o.get(key).getAsInt() : fallback; }
        catch (RuntimeException e) { return fallback; }
    }
    public static long longValue(JsonObject o, String key, long fallback) {
        try { return o.has(key) ? o.get(key).getAsLong() : fallback; }
        catch (RuntimeException e) { return fallback; }
    }
    public static JsonObject message(String type) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        return o;
    }
}
