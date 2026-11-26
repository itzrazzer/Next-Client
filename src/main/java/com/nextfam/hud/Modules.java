package com.nextfam.hud;

import com.nextfam.config.Setting;
import com.nextfam.util.Anim;
import com.nextfam.util.Draw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Modules {
    private Modules() {}
    static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    static int ramp(float v, float lo, float hi) {
        float t = Math.max(0f, Math.min(1f, (v - lo) / (hi - lo)));
        return t < 0.5f ? Draw.lerp(0xFFF87171, 0xFFFACC15, t * 2f) : Draw.lerp(0xFFFACC15, 0xFF4ADE80, (t - 0.5f) * 2f);
    }

    static String fmt(float v) { return v == Math.floor(v) ? String.valueOf((int) v) : String.format(Locale.ROOT, "%.1f", v); }

    public static void register(List<HudElement> l) {
        // ---- HUD (informasi) ----
        l.add(new NumberHud("fps", "FPS Counter", "HUD", "Frames per second with smooth color and graph", 6, 6,
                () -> (float) mc().getCurrentFps(), " FPS", 0, v -> Modules.ramp(v, 20, 120), false));
        l.add(new NumberHud("cps", "CPS Counter", "HUD", "Clicks per second with click flash", 6, 24,
                () -> (float) ClickTracker.left(), " CPS", 0, null, true));
        l.add(new NumberHud("ping", "Ping Display", "HUD", "Latency with connection color", 6, 42,
                () -> {
                    var p = mc().player; var nh = mc().getNetworkHandler();
                    if (p == null || nh == null) return 0f;
                    var e = nh.getPlayerListEntry(p.getUuid());
                    return e == null ? 0f : (float) e.getLatency();
                }, " ms", 0, v -> Modules.ramp(v, 300, 40), false));
        l.add(new TextHud("coords", "Coordinates", "HUD", "XYZ position", 6, 60, () -> {
            var p = mc().player; if (p == null) return "XYZ: -";
            return String.format(Locale.ROOT, "XYZ: %.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ());
        }));
        l.add(new TextHud("direction", "Direction", "HUD", "Facing direction", 6, 78, () -> {
            var p = mc().player; if (p == null) return "-";
            String n = p.getHorizontalFacing().name();
            return n.charAt(0) + n.substring(1).toLowerCase(Locale.ROOT);
        }));
        l.add(new TextHud("biome", "Biome", "HUD", "Current biome", 6, 96, () -> {
            var w = mc().world; var p = mc().player; if (w == null || p == null) return "-";
            return w.getBiome(p.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("unknown");
        }));
        l.add(new TextHud("clock", "Clock", "HUD", "Real-time clock", 6, 114,
                () -> java.time.LocalTime.now().withNano(0).toString()));
        l.add(new TextHud("server", "Server Information", "HUD", "Server address", 6, 132, () -> {
            ServerInfo si = mc().getCurrentServerEntry();
            return si == null ? "Singleplayer" : si.address;
        }));
        l.add(new TextHud("memory", "Memory Usage", "HUD", "Used / max memory", 6, 150, () -> {
            Runtime r = Runtime.getRuntime();
            return "MEM " + ((r.totalMemory() - r.freeMemory()) >> 20) + "/" + (r.maxMemory() >> 20) + " MB";
        }));
        l.add(new CompassHud());

        // ---- Player ----
        l.add(new KeystrokesHud());
        l.add(new ArmorHud());
        l.add(new PotionHud());
        l.add(new BarHud("health", "Health Bar", "Player", "Health with smooth bar", 9999, 190,
                () -> mc().player == null ? 20f : mc().player.getHealth(),
                () -> mc().player == null ? 20f : mc().player.getMaxHealth(), 0xFFF87171, false));
        l.add(new BarHud("hunger", "Hunger Bar", "Player", "Food level", 9999, 204,
                () -> mc().player == null ? 20f : (float) mc().player.getHungerManager().getFoodLevel(),
                () -> 20f, 0xFFFB923C, false));
        l.add(new BarHud("saturation", "Saturation", "Player", "Saturation level", 9999, 218,
                () -> mc().player == null ? 5f : mc().player.getHungerManager().getSaturationLevel(),
                () -> 20f, 0xFFFACC15, false));
        l.add(new NumberHud("speed", "Speed Display", "Player", "Horizontal speed in blocks per second", 6, 168,
                () -> mc().player == null ? 0f : (float) (mc().player.getVelocity().horizontalLength() * 20.0),
                " b/s", 1, null, false));

        // ---- Combat ----
        l.add(new TargetHud());
        l.add(new FlashText("hit", "Hit Indicator", "Flash when you hit a target", -1, 95, () -> "HIT!", () -> CombatTracker.lastHit));
        l.add(new TextHud("combo", "Combo Counter", "Combat", "Consecutive hits without being hit", -1, 160,
                () -> "Combo: " + CombatTracker.combo) {
            @Override public boolean visible() { return CombatTracker.combo > 0 || HudManager.editing; }
        });
        l.add(new BarHud("cooldown", "Attack Cooldown", "Combat", "Attack charge", -1, 135,
                () -> mc().player == null ? 1f : mc().player.getAttackCooldownProgress(0f), () -> 1f, 0xFFA855F7, true));
        l.add(new FlashText("damage", "Damage Indicator", "Last damage dealt", -1, 110,
                () -> "-" + fmt(CombatTracker.lastDamage), () -> CombatTracker.lastDamageTime));

        // ---- Visual ----
        l.add(new CrosshairHud());
        Extras.register(l);
    }

    // ================= elemen =================

    static class TextHud extends HudElement {
        final Supplier<String> sup; String t = "";
        TextHud(String id, String n, String cat, String desc, int x, int y, Supplier<String> s) { super(id, n, cat, desc, x, y); sup = s; }
        private void upd() { try { t = sup.get(); } catch (Exception e) { t = "-"; } }
        @Override protected int cw() { upd(); return Math.max(20, tr().getWidth(t)); }
        @Override protected int ch() { return 9; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) { upd(); textA(ctx, t, x, y, w, textColor.c); }
    }

    static class NumberHud extends HudElement {
        final Supplier<Float> val; final String suffix; final int dec; final Function<Float, Integer> cf; final boolean flash;
        final Anim num = new Anim(0), glowA = new Anim(0);
        int cur = 0xFFFFFFFF;
        final Setting graph = extra(Setting.bool("graph", "Show Graph", false));
        final float[] hist = new float[40]; long lastS;

        NumberHud(String id, String n, String cat, String desc, int x, int y, Supplier<Float> v, String suf, int dec,
                  Function<Float, Integer> cf, boolean flash) {
            super(id, n, cat, desc, x, y); val = v; suffix = suf; this.dec = dec; this.cf = cf; this.flash = flash;
        }
        @Override protected int cw() {
            int base = tr().getWidth("0000" + suffix + (dec > 0 ? ".0" : ""));
            return graph.b ? Math.max(base, 48) : base;
        }
        @Override protected int ch() { return 9 + (graph.b ? 16 : 0); }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            float t; try { t = val.get(); } catch (Exception e) { t = 0f; }
            num.to(t); float v = num.get();
            if (flash && ClickTracker.clicked) glowA.v = 1f;
            glowA.to(0f); float g = glowA.get();
            if (g > 0.02f) Draw.rrect(ctx, x - 2, y - 2, w + 4, 13, Math.round(radius.f), a(Draw.a(borderColor.c, 0.5f * g)));
            int target = cf == null ? textColor.c : cf.apply(t);
            cur = Draw.lerp(cur, target, 0.15f);
            String s = (dec > 0 ? String.format(Locale.ROOT, "%." + dec + "f", v) : String.valueOf(Math.round(v))) + suffix;
            textA(ctx, s, x, y, w, cur);
            if (graph.b) {
                long now = System.currentTimeMillis();
                if (now - lastS > 250) { System.arraycopy(hist, 1, hist, 0, 39); hist[39] = t; lastS = now; }
                float mx = 1f; for (float f : hist) mx = Math.max(mx, f);
                for (int i = 0; i < 40; i++) {
                    int h = Math.round(14f * hist[i] / mx);
                    ctx.fill(x + i * w / 40, y + 24 - h, Math.max(x + i * w / 40 + 1, x + (i + 1) * w / 40), y + 24, a(cur));
                }
            }
        }
    }

    static class BarHud extends HudElement {
        final Supplier<Float> v, m; final int color; final boolean pct; final Anim fill = new Anim(0);
        final Setting showText = extra(Setting.bool("showText", "Show Value", true));
        BarHud(String id, String n, String cat, String desc, int x, int y, Supplier<Float> v, Supplier<Float> m, int color, boolean pct) {
            super(id, n, cat, desc, x, y); this.v = v; this.m = m; this.color = color; this.pct = pct;
        }
        @Override protected int cw() { return 80; }
        @Override protected int ch() { return 10; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            float val = v.get(), max = m.get();
            fill.to(max <= 0 ? 0f : Math.min(1f, val / max));
            float f = fill.get();
            Draw.rrect(ctx, x, y, w, 10, 2, a(0x66000000));
            int fw = Math.round(w * f);
            if (fw > 0) Draw.rrect(ctx, x, y, fw, 10, 2, a(color));
            if (showText.b) {
                String s = pct ? Math.round(val / Math.max(0.001f, max) * 100f) + "%" : fmt(val) + "/" + fmt(max);
                textC(ctx, s, x + (w - tr().getWidth(s)) / 2, y + 1, textColor.c);
            }
        }
    }

    static class KeystrokesHud extends HudElement {
        final Anim[] an = new Anim[7];
        final Setting mouse = extra(Setting.bool("mouse", "Show Mouse Buttons", true));
        final Setting space = extra(Setting.bool("space", "Show Space Bar", true));
        final Setting pressed = extra(Setting.color("pressed", "Pressed Color", 0xFFA855F7));
        KeystrokesHud() {
            super("keystrokes", "Keystrokes", "Player", "WASD, mouse buttons and space with press animation", 6, 9999);
            for (int i = 0; i < an.length; i++) an[i] = new Anim(0);
            bg.def(false);
        }
        @Override protected int cw() { return 64; }
        @Override protected int ch() { return 42 + (mouse.b ? 24 : 0) + (space.b ? 14 : 0); }
        private void key(DrawContext c, int i, int kx, int ky, int kw, int kh, String label, boolean down, String sub) {
            an[i].to(down ? 1f : 0f); float p = an[i].get();
            int r = Math.round(radius.f);
            if (p > 0.05f) Draw.rrect(c, kx - 2, ky - 2, kw + 4, kh + 4, r + 2, a(Draw.a(pressed.c, 0.28f * p)));
            Draw.rrect(c, kx, ky + Math.round(p), kw, kh, r, a(Draw.lerp(bgColor.c, pressed.c, p * 0.85f)));
            int ty = ky + (sub == null ? (kh - 8) / 2 : 3) + Math.round(p);
            textC(c, label, kx + (kw - tr().getWidth(label)) / 2, ty, textColor.c);
            if (sub != null) textC(c, sub, kx + (kw - tr().getWidth(sub)) / 2, ty + 9, Draw.a(textColor.c, 0.7f));
        }
        @Override protected void content(DrawContext c, int x, int y, int w) {
            var o = mc().options;
            key(c, 0, x + 21, y, 22, 20, "W", o.forwardKey.isPressed(), null);
            key(c, 1, x, y + 22, 20, 20, "A", o.leftKey.isPressed(), null);
            key(c, 2, x + 22, y + 22, 20, 20, "S", o.backKey.isPressed(), null);
            key(c, 3, x + 44, y + 22, 20, 20, "D", o.rightKey.isPressed(), null);
            int yy = y + 44;
            if (mouse.b) {
                key(c, 4, x, yy, 31, 22, "LMB", o.attackKey.isPressed(), String.valueOf(ClickTracker.left()));
                key(c, 5, x + 33, yy, 31, 22, "RMB", o.useKey.isPressed(), String.valueOf(ClickTracker.right()));
                yy += 24;
            }
            if (space.b) key(c, 6, x, yy, 64, 12, "SPACE", o.jumpKey.isPressed(), null);
        }
    }

    static class ArmorHud extends HudElement {
        static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        final Anim[] an = {new Anim(1), new Anim(1), new Anim(1), new Anim(1)};
        final Setting pct = extra(Setting.bool("pct", "Show Percent", true));
        ArmorHud() { super("armor", "Armor Status", "Player", "Armor icons with durability", 9999, 60); }
        @Override protected int cw() { return 52; }
        @Override protected int ch() { return 72; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            var p = mc().player;
            for (int i = 0; i < 4; i++) {
                int yy = y + i * 18;
                ItemStack st = p == null ? ItemStack.EMPTY : p.getEquippedStack(SLOTS[i]);
                if (st.isEmpty()) { if (HudManager.editing) Draw.rborder(ctx, x, yy, 16, 16, 2, 1, a(0x44FFFFFF)); continue; }
                ctx.drawItem(st, x, yy);
                if (st.isDamageable()) {
                    float r = (st.getMaxDamage() - st.getDamage()) / (float) st.getMaxDamage();
                    an[i].to(r); float rv = an[i].get();
                    int col = ramp(rv, 0.1f, 0.7f);
                    ctx.fill(x + 18, yy + 11, x + 48, yy + 13, a(0x66000000));
                    ctx.fill(x + 18, yy + 11, x + 18 + Math.round(30 * rv), yy + 13, a(col));
                    if (pct.b) textC(ctx, Math.round(rv * 100) + "%", x + 18, yy + 1, col);
                }
            }
        }
    }

    static class PotionHud extends HudElement {
        static class PE { final Anim a = new Anim(0); String name = ""; int dur, max = 1; }
        final Map<String, PE> m = new LinkedHashMap<>();
        PotionHud() { super("potions", "Potion Effects", "Player", "Active effects with slide and fade animation", 9999, 130); }
        private void update() {
            var p = mc().player; Set<String> seen = new HashSet<>();
            if (p != null) for (StatusEffectInstance e : p.getStatusEffects()) {
                String k = e.getTranslationKey(); seen.add(k);
                PE pe = m.computeIfAbsent(k, z -> new PE());
                pe.name = Text.translatable(k).getString() + " " + (e.getAmplifier() + 1);
                pe.dur = e.getDuration(); pe.max = Math.max(pe.max, pe.dur); pe.a.to(1f);
            }
            if (HudManager.editing && seen.isEmpty()) {
                seen.add("sample"); PE pe = m.computeIfAbsent("sample", z -> new PE());
                pe.name = "Speed II"; pe.dur = 1200; pe.max = 1800; pe.a.to(1f);
            }
            for (var en : m.entrySet()) if (!seen.contains(en.getKey())) en.getValue().a.to(0f);
            m.values().removeIf(pe -> pe.a.t == 0f && pe.a.v < 0.02f);
        }
        @Override public boolean visible() { update(); return !m.isEmpty(); }
        @Override protected int cw() { return 96; }
        @Override protected int ch() { update(); return Math.max(18, m.size() * 20 - 2); }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            int i = 0;
            for (PE pe : m.values()) {
                float al = pe.a.get(); int ox = Math.round((1f - al) * -24f); int yy = y + i * 20;
                String time = pe.dur < 0 ? "INF" : (pe.dur / 1200) + ":" + String.format(Locale.ROOT, "%02d", (pe.dur / 20) % 60);
                textC(ctx, pe.name, x + ox, yy, Draw.a(textColor.c, al));
                textC(ctx, time, x + ox + w - tr().getWidth(time), yy, Draw.a(0xFFC084FC, al));
                float f = pe.dur < 0 ? 1f : Math.min(1f, pe.dur / (float) Math.max(1, pe.max));
                ctx.fill(x + ox, yy + 11, x + ox + w, yy + 14, a(Draw.a(0x66000000, al)));
                ctx.fill(x + ox, yy + 11, x + ox + Math.round(w * f), yy + 14, a(Draw.a(ramp(f, 0.05f, 0.6f), al)));
                i++;
            }
        }
    }

    static class TargetHud extends HudElement {
        final Anim hp = new Anim(0); LivingEntity last;
        TargetHud() { super("target", "Target HUD", "Combat", "Name and health of the last target you hit", -1, 30); }
        @Override public boolean visible() { return CombatTracker.targetRecent() || HudManager.editing; }
        @Override protected int cw() { return 110; }
        @Override protected int ch() { return 22; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            LivingEntity t = CombatTracker.targetRecent() ? CombatTracker.target : null;
            String nm = t == null ? "Target" : t.getName().getString();
            float h = t == null ? 14f : t.getHealth(), mx = t == null ? 20f : Math.max(1f, t.getMaxHealth());
            if (t != last) { hp.set(h / mx); last = t; }
            hp.to(h / mx); float f = hp.get();
            textA(ctx, nm, x, y, w, textColor.c);
            Draw.rrect(ctx, x, y + 11, w, 9, 2, a(0x66000000));
            int fw = Math.round(w * Math.max(0f, Math.min(1f, f)));
            if (fw > 0) Draw.rrect(ctx, x, y + 11, fw, 9, 2, a(ramp(f, 0.15f, 0.7f)));
            String s = fmt(h) + "/" + fmt(mx);
            textC(ctx, s, x + (w - tr().getWidth(s)) / 2, y + 12, textColor.c);
        }
    }

    static class FlashText extends HudElement {
        final Supplier<String> sup; final Supplier<Long> time;
        FlashText(String id, String n, String desc, int x, int y, Supplier<String> s, Supplier<Long> t) {
            super(id, n, "Combat", desc, x, y); sup = s; time = t;
        }
        private float age() { return Math.max(0f, 1f - (System.currentTimeMillis() - time.get()) / 900f); }
        @Override public boolean visible() { return HudManager.editing || age() > 0.01f; }
        @Override public float extraAlpha() { return HudManager.editing ? 1f : age(); }
        @Override protected int cw() { return Math.max(24, tr().getWidth(sup.get())); }
        @Override protected int ch() { return 9; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) { textA(ctx, sup.get(), x, y, w, textColor.c); }
    }

    static class CompassHud extends HudElement {
        CompassHud() { super("compass", "Compass", "HUD", "Compass strip following your yaw", -1, 4); }
        @Override protected int cw() { return 120; }
        @Override protected int ch() { return 10; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            var p = mc().player;
            float yaw = p == null ? 0f : MathHelper.wrapDegrees(p.getYaw());
            int from = (int) Math.floor(yaw - 60), to = (int) Math.ceil(yaw + 60);
            String[] letters = {"S", "W", "N", "E"};
            for (int k = from; k <= to; k++) {
                if (k % 15 != 0) continue;
                int px = x + Math.round(w / 2f + (k - yaw) * (w / 120f));
                if (px < x || px >= x + w) continue;
                int norm = ((k % 360) + 360) % 360;
                if (norm % 90 == 0) {
                    String l = letters[norm / 90];
                    textC(ctx, l, px - tr().getWidth(l) / 2, y, l.equals("N") ? 0xFFC084FC : textColor.c);
                } else ctx.fill(px, y + 4, px + 1, y + 8, a(Draw.a(textColor.c, 0.6f)));
            }
            ctx.fill(x + w / 2, y + 9, x + w / 2 + 1, y + 10, a(0xFFA855F7));
        }
    }

    static class CrosshairHud extends HudElement {
        final Setting size = extra(Setting.num("size", "Size", 5f, 1f, 15f).step(1f));
        final Setting gap = extra(Setting.num("gap", "Gap", 2f, 0f, 8f).step(1f));
        final Setting thick = extra(Setting.num("thick", "Thickness", 1f, 1f, 4f).step(1f));
        final Setting color = extra(Setting.color("color", "Crosshair Color", 0xFFFFFFFF));
        final Setting dot = extra(Setting.bool("dot", "Center Dot", false));
        CrosshairHud() {
            super("crosshair", "Custom Crosshair", "Visual", "Adjustable crosshair (the vanilla crosshair stays visible)", -1, -1);
            bg.def(false); pad.def(0f);
        }
        private int total() { return 2 * (Math.round(size.f) + Math.round(gap.f)) + Math.round(thick.f); }
        @Override protected int cw() { return total(); }
        @Override protected int ch() { return total(); }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            int s = Math.round(size.f), g = Math.round(gap.f), t = Math.round(thick.f), c = a(color.c);
            int cx = x + s + g, cy = y + s + g;
            ctx.fill(cx, y, cx + t, y + s, c);
            ctx.fill(cx, y + total() - s, cx + t, y + total(), c);
            ctx.fill(x, cy, x + s, cy + t, c);
            ctx.fill(x + total() - s, cy, x + total(), cy + t, c);
            if (dot.b) ctx.fill(cx, cy, cx + t, cy + t, c);
        }
    }
}
