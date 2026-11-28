package com.nextfam.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nextfam.config.NfConfig;
import net.minecraft.client.MinecraftClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Deteksi pemain NextFam lain (logo di nametag / tab).
 * Mode "ntfy" (default): memakai topik publik ntfy.sh, tanpa server sendiri.
 * Mode "custom": server buatan sendiri (lihat server/presence_server.py), atur di config/nextfam/presence.json.
 * Hanya UUID yang dikirim, dan hanya jika "Share my NextFam status" aktif.
 */
public final class Presence {
    private Presence() {}
    private static final String VERSION = "5.2";
    private static final String TOPIC = "nextfam-presence-v1-7c1e9a4d";
    private static final long BEAT_MS = 6 * 60_000L, POLL_MS = 45_000L, TTL_S = 14 * 60L;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();

    private static volatile Set<UUID> users = Set.of();
    private static volatile long lastBeat, lastPoll;
    private static volatile boolean busy;
    private static String mode = "ntfy", url = "";
    private static boolean loaded;

    public static boolean isUser(UUID id) { return id != null && users.contains(id); }
    public static int count() { return users.size(); }

    private static void load() {
        if (loaded) return; loaded = true;
        JsonObject o = NfConfig.read("presence.json");
        if (o.has("mode")) mode = o.get("mode").getAsString();
        if (o.has("url")) url = o.get("url").getAsString();
        JsonObject w = new JsonObject(); w.addProperty("mode", mode); w.addProperty("url", url);
        w.addProperty("note", "mode: ntfy (public, no server needed) or custom (your own server, set url)");
        NfConfig.write("presence.json", w);
    }

    /** Dipanggil tiap tick client; pekerjaan jaringan berjalan di thread lain. */
    public static void tick(MinecraftClient c) {
        HudElement e = HudManager.get("nflogo");
        if (e == null || !e.enabled || c.player == null || busy) return;
        load();
        long now = System.currentTimeMillis();
        boolean share = e.s("share").b, fetch = e.s("others").b;
        final String uuid = c.player.getUuid().toString();
        if (share && now - lastBeat > BEAT_MS) {
            lastBeat = now; busy = true;
            CompletableFuture.runAsync(() -> { try { beat(uuid); } catch (Throwable ignored) {} busy = false; });
        } else if (fetch && now - lastPoll > POLL_MS) {
            lastPoll = now; busy = true;
            CompletableFuture.runAsync(() -> { try { poll(); } catch (Throwable ignored) {} busy = false; });
        }
    }

    private static boolean custom() { return mode.equals("custom") && url.startsWith("https://") || mode.equals("custom") && url.startsWith("http://"); }

    private static void beat(String uuid) throws Exception {
        if (custom()) {
            HttpRequest r = HttpRequest.newBuilder(URI.create(url.replaceAll("/+$", "") + "/hb")).timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{\"u\":\"" + uuid + "\",\"v\":\"" + VERSION + "\"}")).build();
            HTTP.send(r, HttpResponse.BodyHandlers.discarding());
        } else {
            HttpRequest r = HttpRequest.newBuilder(URI.create("https://ntfy.sh/" + TOPIC)).timeout(Duration.ofSeconds(8))
                    .header("Title", "nextfam").POST(HttpRequest.BodyPublishers.ofString("nf:" + uuid + ":" + VERSION)).build();
            HTTP.send(r, HttpResponse.BodyHandlers.discarding());
        }
    }

    private static void poll() throws Exception {
        Set<UUID> out = new HashSet<>();
        if (custom()) {
            HttpRequest r = HttpRequest.newBuilder(URI.create(url.replaceAll("/+$", "") + "/list")).timeout(Duration.ofSeconds(8)).GET().build();
            String body = HTTP.send(r, HttpResponse.BodyHandlers.ofString()).body();
            var arr = JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("users");
            for (var el : arr) { try { out.add(UUID.fromString(el.getAsString())); } catch (Exception ignored) {} if (out.size() > 2000) break; }
        } else {
            HttpRequest r = HttpRequest.newBuilder(URI.create("https://ntfy.sh/" + TOPIC + "/json?poll=1&since=15m")).timeout(Duration.ofSeconds(8)).GET().build();
            String body = HTTP.send(r, HttpResponse.BodyHandlers.ofString()).body();
            long nowS = System.currentTimeMillis() / 1000;
            for (String line : body.split("\n")) {
                if (line.isBlank()) continue;
                try {
                    JsonObject o = JsonParser.parseString(line).getAsJsonObject();
                    if (!o.has("message") || (o.has("event") && !o.get("event").getAsString().equals("message"))) continue;
                    if (o.has("time") && nowS - o.get("time").getAsLong() > TTL_S) continue;
                    String m = o.get("message").getAsString();
                    if (!m.startsWith("nf:")) continue;
                    int end = m.indexOf(':', 3);
                    out.add(UUID.fromString(end < 0 ? m.substring(3) : m.substring(3, end)));
                } catch (Exception ignored) {}
                if (out.size() > 2000) break;
            }
        }
        users = out;
    }
}
