package com.nextfam.hud;

import com.google.gson.*;
import com.nextfam.config.NfConfig;
import com.nextfam.config.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import com.nextfam.util.Proj;
import java.nio.file.*;
import java.util.*;

public final class HudManager {
    public static final List<HudElement> ELEMENTS = new ArrayList<>();
    public static boolean editing = false;

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static void init() {
        Modules.register(ELEMENTS);
        loadActive();
    }

    // ---------- render ----------
    public static void fit(HudElement e, int sw, int sh) {
        if (e.x == -1) e.x = (sw - e.sw()) / 2;
        if (e.y == -1) e.y = (sh - e.sh()) / 2;
        e.x = Math.max(0, Math.min(e.x, Math.max(0, sw - e.sw())));
        e.y = Math.max(0, Math.min(e.y, Math.max(0, sh - e.sh())));
    }

    public static void draw(DrawContext ctx, HudElement e) {
        e.shown.to(e.enabled ? 1f : 0f);
        float s = e.shown.get();
        boolean none = e.animStyle.idx == 3;
        if (!e.enabled && (none || s < 0.02f)) return;
        if (e.enabled && !editing && !e.visible()) return;
        e.alphaMul = (none ? 1f : s) * e.extraAlpha();
        float sc = e.scale.f * (e.animStyle.idx == 2 ? 0.7f + 0.3f * s : 1f);
        float ox = e.animStyle.idx == 1 ? -(1f - s) * 30f : 0f;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(e.x + ox, (float) e.y);
        m.scale(sc, sc);
        e.render(ctx);
        m.popMatrix();
    }

    public static HudElement get(String id) {
        for (HudElement e : ELEMENTS) if (e.id.equals(id)) return e;
        return null;
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        var c = mc();
        if (c.options.hudHidden || c.currentScreen != null) return;
        ClickTracker.poll(c.getWindow().getHandle());
        int sw = c.getWindow().getScaledWidth(), sh = c.getWindow().getScaledHeight();
        float tp = tick.getTickProgress(false);
        Proj.setup(c, tp, sw, sh);
        for (HudElement e : ELEMENTS) {
            if (e.overlay) { if (e.enabled && c.world != null) e.worldRender(ctx, sw, sh, tp); continue; }
            fit(e, sw, sh); draw(ctx, e);
        }
    }

    // ---------- JSON ----------
    private static JsonObject layoutJson() {
        JsonObject o = new JsonObject(), els = new JsonObject(); JsonArray order = new JsonArray();
        for (HudElement e : ELEMENTS) {
            order.add(e.id);
            JsonObject j = new JsonObject();
            j.addProperty("x", e.x); j.addProperty("y", e.y); j.addProperty("enabled", e.enabled); j.addProperty("locked", e.locked);
            els.add(e.id, j);
        }
        o.add("order", order); o.add("elements", els);
        return o;
    }

    private static JsonObject settingsJson() {
        JsonObject o = new JsonObject();
        for (HudElement e : ELEMENTS) {
            JsonObject j = new JsonObject();
            for (Setting s : e.settings) j.add(s.key, s.toJson());
            o.add(e.id, j);
        }
        return o;
    }

    private static void applyLayout(JsonObject o) {
        try {
            if (o.has("elements")) {
                JsonObject els = o.getAsJsonObject("elements");
                for (HudElement e : ELEMENTS) {
                    if (!els.has(e.id)) continue;
                    JsonObject j = els.getAsJsonObject(e.id);
                    if (j.has("x")) e.x = j.get("x").getAsInt();
                    if (j.has("y")) e.y = j.get("y").getAsInt();
                    if (j.has("enabled")) e.enabled = j.get("enabled").getAsBoolean();
                    if (j.has("locked")) e.locked = j.get("locked").getAsBoolean();
                    e.shown.set(e.enabled ? 1f : 0f);
                }
            }
            if (o.has("order")) {
                List<HudElement> n = new ArrayList<>();
                for (JsonElement id : o.getAsJsonArray("order"))
                    for (HudElement e : ELEMENTS) if (e.id.equals(id.getAsString()) && !n.contains(e)) n.add(e);
                for (HudElement e : ELEMENTS) if (!n.contains(e)) n.add(e);
                ELEMENTS.clear(); ELEMENTS.addAll(n);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private static void applySettings(JsonObject o) {
        try {
            for (HudElement e : ELEMENTS) {
                if (!o.has(e.id)) continue;
                JsonObject j = o.getAsJsonObject(e.id);
                for (Setting s : e.settings) if (j.has(s.key)) s.fromJson(j.get(s.key));
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ---------- simpan / muat ----------
    public static void saveAll() {
        NfConfig.write("hud-layout.json", layoutJson());
        NfConfig.write("hud-settings.json", settingsJson());
        JsonObject p = new JsonObject(); p.add("layout", layoutJson()); p.add("settings", settingsJson());
        NfConfig.write("profiles/" + NfConfig.profile + ".json", p);
        NfConfig.save();
    }

    private static void loadActive() {
        NfConfig.load();
        applySettings(NfConfig.read("hud-settings.json"));
        applyLayout(NfConfig.read("hud-layout.json"));
    }

    public static List<String> profiles() {
        List<String> l = new ArrayList<>();
        try (var st = Files.list(NfConfig.dir().resolve("profiles"))) {
            st.forEach(p -> { String n = p.getFileName().toString(); if (n.endsWith(".json")) l.add(n.substring(0, n.length() - 5)); });
        } catch (Exception ignored) {}
        if (!l.contains(NfConfig.profile)) l.add(NfConfig.profile);
        Collections.sort(l);
        return l;
    }

    public static void loadProfile(String name) {
        JsonObject p = NfConfig.read("profiles/" + name + ".json");
        if (p.has("settings")) applySettings(p.getAsJsonObject("settings"));
        if (p.has("layout")) applyLayout(p.getAsJsonObject("layout"));
        NfConfig.profile = name;
        saveAll();
    }

    public static String newProfile() {
        List<String> existing = profiles();
        int i = 1; while (existing.contains("profile-" + i)) i++;
        String n = "profile-" + i;
        NfConfig.profile = n;
        saveAll();
        return n;
    }

    public static String exportString() {
        JsonObject p = new JsonObject(); p.add("layout", layoutJson()); p.add("settings", settingsJson());
        return p.toString();
    }

    public static boolean importString(String s) {
        try {
            JsonObject p = JsonParser.parseString(s).getAsJsonObject();
            if (!p.has("layout") && !p.has("settings")) return false;
            if (p.has("settings")) applySettings(p.getAsJsonObject("settings"));
            if (p.has("layout")) applyLayout(p.getAsJsonObject("layout"));
            saveAll();
            return true;
        } catch (Exception e) { return false; }
    }

    public static void resetAll() {
        for (HudElement e : ELEMENTS) { e.resetAll(); e.shown.set(1f); }
        NfConfig.theme = "NextFam";
        saveAll();
    }
}
