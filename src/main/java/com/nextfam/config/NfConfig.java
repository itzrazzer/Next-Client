package com.nextfam.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;

/** animations.json, themes.json, keybinds.json */
public final class NfConfig {
    private NfConfig() {}
    public static final Setting ANIM = Setting.bool("animations", "Enable Animations", true);
    public static final Setting SPEED = Setting.num("speed", "Animation Speed", 1f, 0.25f, 3f);
    public static String theme = "NextFam", profile = "default";
    public static int menuKey = 344;         // GLFW_KEY_RIGHT_SHIFT
    public static int packKey = 345;         // GLFW_KEY_RIGHT_CONTROL -> Pack Studio
    public static int ver = 0;
    public static String cape = "";
    public static final List<Integer> FAVS = new ArrayList<>();
    public static final boolean[][] CROSS = new boolean[15][15];
    static {
        for (int i = 2; i <= 5; i++) { CROSS[7][7 - i] = true; CROSS[7][7 + i] = true; CROSS[7 - i][7] = true; CROSS[7 + i][7] = true; }
    }

    public static boolean anim() { return ANIM.b; }
    public static float speed() { return SPEED.f; }

    public static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("nextfam"); }

    public static JsonObject read(String name) {
        try {
            Path p = dir().resolve(name);
            if (Files.exists(p)) return JsonParser.parseString(Files.readString(p)).getAsJsonObject();
        } catch (Exception ignored) {}
        return new JsonObject();
    }

    public static void write(String name, JsonElement e) {
        try {
            Path target = dir().resolve(name);
            Files.createDirectories(target.getParent());
            Files.writeString(target, new GsonBuilder().setPrettyPrinting().create().toJson(e));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public static void load() {
        JsonObject a = read("animations.json");
        if (a.has("animations")) ANIM.fromJson(a.get("animations"));
        if (a.has("speed")) SPEED.fromJson(a.get("speed"));
        JsonObject t = read("themes.json");
        if (t.has("theme")) theme = t.get("theme").getAsString();
        if (t.has("profile")) profile = t.get("profile").getAsString();
        if (t.has("ver")) ver = t.get("ver").getAsInt();
        FAVS.clear();
        if (t.has("favs")) for (JsonElement e : t.getAsJsonArray("favs")) FAVS.add(e.getAsInt());
        JsonObject co = read("cosmetics.json");
        if (co.has("cape")) cape = co.get("cape").getAsString();
        JsonObject cr = read("crosshair.json");
        if (cr.has("rows")) {
            JsonArray rows = cr.getAsJsonArray("rows");
            for (int r = 0; r < 15 && r < rows.size(); r++) { int m = rows.get(r).getAsInt(); for (int c = 0; c < 15; c++) CROSS[r][c] = (m >> c & 1) == 1; }
        }
        JsonObject k = read("keybinds.json");
        if (k.has("menuKey")) menuKey = k.get("menuKey").getAsInt();
        if (k.has("packStudioKey")) packKey = k.get("packStudioKey").getAsInt();
        save();
    }

    public static void save() {
        JsonObject a = new JsonObject(); a.add("animations", ANIM.toJson()); a.add("speed", SPEED.toJson());
        write("animations.json", a);
        JsonObject t = new JsonObject(); t.addProperty("theme", theme); t.addProperty("profile", profile); t.addProperty("ver", ver);
        JsonArray fv = new JsonArray(); for (int c : FAVS) fv.add(c); t.add("favs", fv);
        write("themes.json", t);
        JsonObject k = new JsonObject(); k.addProperty("menuKey", menuKey); k.addProperty("menuKeyName", "RIGHT_SHIFT (GLFW code 344)");
        k.addProperty("packStudioKey", packKey); k.addProperty("packStudioKeyName", "RIGHT_CONTROL (GLFW code 345)");
        JsonObject co = new JsonObject(); co.addProperty("cape", cape); write("cosmetics.json", co);
        saveCross();
        write("keybinds.json", k);
    }

    public static void saveCross() {
        JsonObject cr = new JsonObject(); JsonArray rows = new JsonArray();
        for (int r = 0; r < 15; r++) { int m = 0; for (int c = 0; c < 15; c++) if (CROSS[r][c]) m |= 1 << c; rows.add(m); }
        cr.add("rows", rows);
        write("crosshair.json", cr);
    }
}
