package com.nextfam.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

/** animations.json, themes.json, keybinds.json */
public final class NfConfig {
    private NfConfig() {}
    public static final Setting ANIM = Setting.bool("animations", "Enable Animations", true);
    public static final Setting SPEED = Setting.num("speed", "Animation Speed", 1f, 0.25f, 3f);
    public static String theme = "NextFam", profile = "default";
    public static int menuButton = 1;        // 0 = kiri, 1 = kanan
    public static boolean menuShift = true;

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
        JsonObject k = read("keybinds.json");
        if (k.has("menuMouseButton")) menuButton = k.get("menuMouseButton").getAsInt();
        if (k.has("menuNeedsShift")) menuShift = k.get("menuNeedsShift").getAsBoolean();
        save();
    }

    public static void save() {
        JsonObject a = new JsonObject(); a.add("animations", ANIM.toJson()); a.add("speed", SPEED.toJson());
        write("animations.json", a);
        JsonObject t = new JsonObject(); t.addProperty("theme", theme); t.addProperty("profile", profile);
        write("themes.json", t);
        JsonObject k = new JsonObject(); k.addProperty("menuMouseButton", menuButton); k.addProperty("menuNeedsShift", menuShift);
        write("keybinds.json", k);
    }
}
