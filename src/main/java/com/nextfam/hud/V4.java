package com.nextfam.hud;

import com.nextfam.config.Setting;
import com.nextfam.mixin.OverlayTextureAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Fitur 4.0: Auto GG / Auto GL, Quick Chat, Hit Color Customizer, 1.7 Visual Animations. */
public final class V4 {
    private V4() {}

    static final String[] GG = {"gg", "GG", "gg wp", "Good game", "gg!"};
    static final String[] GL = {"gl hf", "GL HF", "good luck", "gl", "glhf!"};
    static final String[] MSG = {"gg", "gl hf", "wp", "nt", "ty", "sorry", "lag", "brb", "rematch?", "gg wp"};
    static final String[] QK_NAMES = {"Numpad 1", "Numpad 2", "Numpad 3", "Numpad 4", "Numpad 5", "Numpad 6", "F6", "F7", "F8"};
    static final int[] QK_CODES = {321, 322, 323, 324, 325, 326, 295, 296, 297};
    static final String[] START = {"the game has started", "game started", "game starts in 1", "starting in 1 second", "fight!",
        "duel started", "match started", "match has begun", "round started", "the game begins"};
    static final String[] END = {"you won", "you win", "victory", "winner", "has won the game", "won the game", "game over",
        "you lost", "defeat", "1st place", "game ended", "duel ended", "match ended", "the game has ended"};

    private static String pending; private static int ticks; private static long last;
    private static final boolean[] was = new boolean[3];
    private static int hcLast = 0xB2FF0000; private static Object hcTex; private static boolean hcFail;

    public static void register(List<HudElement> l) {
        l.add(new AutoGG());
        l.add(new QuickChat());
        l.add(new HitColor());
        l.add(new Anim17());
    }

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((msg, overlay) -> { if (!overlay) onMessage(msg.getString().toLowerCase(Locale.ROOT)); });
        ClientTickEvents.END_CLIENT_TICK.register(V4::tick);
    }

    private static boolean hit(String s, String[] keys) { for (String k : keys) if (s.contains(k)) return true; return false; }

    private static void onMessage(String s) {
        HudElement e = HudManager.get("autogg");
        if (e == null || !e.enabled || s.startsWith("<")) return;
        long now = System.currentTimeMillis();
        if (now - last < e.s("cooldown").f * 1000) return;
        String m = null;
        if (e.s("gl").b && hit(s, START)) m = GL[e.s("glMsg").idx];
        else if (e.s("gg").b && hit(s, END)) m = GG[e.s("ggMsg").idx];
        if (m == null) return;
        pending = m; ticks = Math.round(e.s("delay").f * 20f); last = now;
    }

    private static void send(MinecraftClient c, String m) {
        var nh = c.getNetworkHandler();
        if (nh != null && m != null && !m.isEmpty()) nh.sendChatMessage(m);
    }

    private static void tick(MinecraftClient c) {
        if (c.player == null) { pending = null; return; }
        if (pending != null && --ticks <= 0) { send(c, pending); pending = null; }
        HudElement q = HudManager.get("quickchat");
        if (q != null && q.enabled && c.currentScreen == null) {
            long w = c.getWindow().getHandle();
            for (int i = 0; i < 3; i++) {
                boolean down = GLFW.glfwGetKey(w, QK_CODES[q.s("k" + i).idx]) == GLFW.GLFW_PRESS;
                if (down && !was[i]) send(c, MSG[q.s("m" + i).idx]);
                was[i] = down;
            }
        } else Arrays.fill(was, false);
        hitColor(c);
    }

    private static void hitColor(MinecraftClient c) {
        if (hcFail) return;
        HudElement h = HudManager.get("hitcolor");
        int want = (h != null && h.enabled) ? ((Math.round(h.s("alpha").f * 255f) << 24) | (h.s("color").c & 0xFFFFFF)) : 0xB2FF0000;
        try {
            var tex = ((OverlayTextureAccessor) (Object) c.gameRenderer.getOverlayTexture()).nextfam$tex();
            if (tex == hcTex && want == hcLast) return;
            var img = tex.getImage();
            if (img == null) return;
            for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setColorArgb(x, y, want);
            tex.upload();
            hcTex = tex; hcLast = want;
        } catch (Throwable t) { hcFail = true; }
    }

    // ---------- modul ----------
    static class AutoGG extends Extras.OverlayModule {
        final Setting gg = extra(Setting.bool("gg", "Auto GG (game end)", true));
        final Setting ggMsg = extra(Setting.choice("ggMsg", "GG Message", 0, GG));
        final Setting gl = extra(Setting.bool("gl", "Auto GL (game start)", true));
        final Setting glMsg = extra(Setting.choice("glMsg", "GL Message", 0, GL));
        final Setting delay = extra(Setting.num("delay", "Delay (seconds)", 1f, 0f, 5f).step(0.5f));
        final Setting cooldown = extra(Setting.num("cooldown", "Cooldown (seconds)", 15f, 5f, 60f).step(1f));
        AutoGG() { super("autogg", "Auto GG / Auto GL", "Chat", "Sends gg / gl automatically when a game starts or ends (check server rules)"); }
    }

    static class QuickChat extends Extras.OverlayModule {
        final Setting k0 = extra(Setting.choice("k0", "Key 1", 0, QK_NAMES));
        final Setting m0 = extra(Setting.choice("m0", "Message 1", 0, MSG));
        final Setting k1 = extra(Setting.choice("k1", "Key 2", 1, QK_NAMES));
        final Setting m1 = extra(Setting.choice("m1", "Message 2", 1, MSG));
        final Setting k2 = extra(Setting.choice("k2", "Key 3", 2, QK_NAMES));
        final Setting m2 = extra(Setting.choice("m2", "Message 3", 2, MSG));
        QuickChat() { super("quickchat", "Quick Chat", "Chat", "Press a key to send a preset chat message"); }
    }

    static class HitColor extends Extras.OverlayModule {
        final Setting color = extra(Setting.color("color", "Hit Color", 0xFFFF0000));
        final Setting alpha = extra(Setting.num("alpha", "Strength", 0.7f, 0.2f, 1f));
        HitColor() { super("hitcolor", "Hit Color Customizer", "Visual", "Changes the red flash on entities that get hit (needs mixin)"); }
    }

    static class Anim17 extends Extras.OverlayModule {
        final Setting noDip = extra(Setting.bool("noDip", "No Attack Dip (classic hand)", true));
        Anim17() { super("anim17", "1.7 Visual Animations", "Tweaks", "Classic look: the held item no longer dips when you attack (partial, needs mixin)"); }
    }
}
