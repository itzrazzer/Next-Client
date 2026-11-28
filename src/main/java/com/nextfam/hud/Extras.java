package com.nextfam.hud;

import com.nextfam.config.Setting;
import com.nextfam.util.Anim;
import com.nextfam.util.Draw;
import com.nextfam.util.Proj;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import com.nextfam.hud.HudManager;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public final class Extras {
    private Extras() {}

    public static void register(List<HudElement> l) {
        l.add(new TotemHud());
        l.add(new ShieldHud());
        l.add(new BlockOutline());
        l.add(new TntTimer());
        l.add(new CrystalOpt());
        l.add(new FpsOpt());
        l.add(new DeathAnim());
        l.add(new TotemTweaks());
        icons(l);
        More.register(l);
    }

    private static void icons(List<HudElement> l) {
        Map<String, Item> m = new HashMap<>();
        m.put("fps", Items.REPEATER); m.put("cps", Items.STONE_BUTTON); m.put("ping", Items.ENDER_PEARL);
        m.put("coords", Items.FILLED_MAP); m.put("direction", Items.COMPASS); m.put("biome", Items.OAK_SAPLING);
        m.put("clock", Items.CLOCK); m.put("server", Items.BEACON); m.put("memory", Items.REDSTONE);
        m.put("compass", Items.RECOVERY_COMPASS); m.put("keystrokes", Items.LEVER); m.put("armor", Items.DIAMOND_CHESTPLATE);
        m.put("potions", Items.POTION); m.put("health", Items.GOLDEN_APPLE); m.put("hunger", Items.COOKED_BEEF);
        m.put("saturation", Items.GOLDEN_CARROT); m.put("speed", Items.SUGAR); m.put("target", Items.TARGET);
        m.put("hit", Items.IRON_SWORD); m.put("combo", Items.DIAMOND_SWORD); m.put("cooldown", Items.NETHERITE_SWORD);
        m.put("damage", Items.FIRE_CHARGE); m.put("crosshair", Items.SPYGLASS);
        m.put("totem", Items.TOTEM_OF_UNDYING); m.put("shield", Items.SHIELD); m.put("blockoutline", Items.GLASS);
        m.put("tnttimer", Items.TNT); m.put("crystals", Items.END_CRYSTAL); m.put("fpsopt", Items.REDSTONE_TORCH);
        m.put("deathanim", Items.SKELETON_SKULL); m.put("totemtweaks", Items.GOLD_INGOT);
        for (HudElement e : l) { Item it = m.get(e.id); if (it != null) e.icon = it; }
    }

    // ---------- modul overlay (tanpa posisi) ----------
    abstract static class OverlayModule extends HudElement {
        OverlayModule(String id, String n, String cat, String desc) {
            super(id, n, cat, desc, 0, 0);
            overlay = true;
            settings.clear();
        }
        @Override protected int cw() { return 0; }
        @Override protected int ch() { return 0; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {}
        @Override public boolean visible() { return false; }
    }

    static class BlockOutline extends OverlayModule {
        final Setting color = extra(Setting.color("color", "Outline Color", 0xFFFFFFFF));
        final Setting thick = extra(Setting.num("thick", "Thickness", 1f, 1f, 4f).step(1f));
        final Setting rainbow = extra(Setting.bool("rainbow", "Rainbow", false));
        BlockOutline() { super("blockoutline", "Block Outlines", "Visual", "Colored outline on the block you look at (2D overlay)"); }
        private static final int[][] E = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        @Override public void worldRender(DrawContext ctx, int sw, int sh, float tp) {
            var mc = Modules.mc();
            if (!Proj.ready || mc.world == null || !(mc.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
            BlockPos pos = hit.getBlockPos();
            var shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
            if (shape.isEmpty()) return;
            Box b = shape.getBoundingBox().offset(pos).expand(0.002);
            double[][] c = new double[8][];
            double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
            int[][] corner = {{0,0,0},{1,0,0},{1,0,1},{0,0,1},{0,1,0},{1,1,0},{1,1,1},{0,1,1}};
            for (int i = 0; i < 8; i++) c[i] = Proj.cam(xs[corner[i][0]], ys[corner[i][1]], zs[corner[i][2]]);
            int col = rainbow.b ? (java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 3000) / 3000f, 0.7f, 1f) | 0xFF000000) : color.c;
            for (int[] e : E) Proj.line(ctx, c[e[0]], c[e[1]], col, Math.round(thick.f));
        }
    }

    static class TntTimer extends OverlayModule {
        final Setting dec = extra(Setting.bool("decimals", "Show Decimals", true));
        final Setting range = extra(Setting.num("range", "Max Distance", 48f, 8f, 128f).step(1f));
        TntTimer() { super("tnttimer", "TNT Timer", "Visual", "Fuse countdown above primed TNT"); }
        @Override public void worldRender(DrawContext ctx, int sw, int sh, float tp) {
            var mc = Modules.mc();
            if (!Proj.ready || mc.world == null) return;
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof TntEntity t)) continue;
                Vec3d p = t.getLerpedPos(tp);
                double[] c = Proj.cam(p.x, p.y + 1.3, p.z);
                if (c[2] < 0.2 || c[2] > range.f) continue;
                float secs = t.getFuse() / 20f;
                String s = dec.b ? String.format(Locale.ROOT, "%.1fs", secs) : Math.round(secs) + "s";
                int col = Modules.ramp(secs, 0f, 4f);
                int x = Proj.sx(c), y = Proj.sy(c);
                var tr = mc.textRenderer;
                Draw.rrect(ctx, x - tr.getWidth(s) / 2 - 3, y - 2, tr.getWidth(s) + 6, 12, 3, 0x99000000);
                ctx.drawText(tr, s, x - tr.getWidth(s) / 2, y, col, true);
            }
        }
    }

    static class CrystalOpt extends OverlayModule {
        CrystalOpt() { super("crystals", "Crystals Optimizer", "Optimize", "Removes an end crystal on your screen right after you hit it"); }
    }

    static class FpsOpt extends OverlayModule {
        final Setting uncap = extra(Setting.bool("uncap", "Uncap FPS (no frame limit)", true));
        final Setting vsync = extra(Setting.bool("vsync", "Turn VSync off", true));
        final Setting particles = extra(Setting.bool("particles", "Minimal Particles (changes look)", false));
        final Setting shadows = extra(Setting.bool("shadows", "Disable Entity Shadows (changes look)", false));
        final Setting dist = extra(Setting.num("dist", "Entity Distance", 1.0f, 0.5f, 5f));
        FpsOpt() { super("fpsopt", "FPS Optimizer", "Optimize", "Boosts FPS without changing how Minecraft looks (uncapped FPS, VSync off)"); }
    }

    static class DeathAnim extends OverlayModule {
        final Setting upright = extra(Setting.bool("upright", "Stand Upright (no falling over)", true));
        final Setting keep = extra(Setting.num("keep", "Keep Body (seconds)", 3f, 0f, 10f).step(1f));
        final Setting players = extra(Setting.bool("players", "Include Players", true));
        DeathAnim() { super("deathanim", "Death Animation", "Tweaks", "Dead players and mobs stay standing instead of falling over and vanishing (needs mixin)"); }
    }

    static class TotemTweaks extends OverlayModule {
        final Setting hidePop = extra(Setting.bool("hidePop", "Hide Totem Pop Animation", false));
        final Setting custom = extra(Setting.bool("custom", "Custom Pop Animation", true));
        final Setting size = extra(Setting.num("size", "Pop Size", 1.0f, 0.3f, 2.5f));
        final Setting handSize = extra(Setting.num("handSize", "Totem Size In Hand", 1.0f, 0.3f, 2.5f));
        final Setting dur = extra(Setting.num("dur", "Pop Duration (seconds)", 1.2f, 0.4f, 3f));
        final ItemStack st = new ItemStack(Items.TOTEM_OF_UNDYING);
        TotemTweaks() { super("totemtweaks", "Totem Tweaks", "Tweaks", "Resize the totem in your hand and the pop animation (needs mixin)"); }
        @Override public void worldRender(DrawContext ctx, int sw, int sh, float tp) {
            if (hidePop.b || !custom.b) return;
            long age = System.currentTimeMillis() - Features.totemPopStart;
            float p = age / (dur.f * 1000f);
            if (HudManager.editing) p = 0.4f;
            if (p < 0f || p >= 1f) return;
            float ease = p < 0.15f ? p / 0.15f : p < 0.7f ? 1f : (1f - p) / 0.3f;
            float s = 7f * size.f * ease;
            float wob = (float) Math.sin(p * 18f) * (1f - p) * 3f;
            var m = ctx.getMatrices();
            m.pushMatrix();
            m.translate(sw / 2f + wob, sh / 2f - 8f);
            m.scale(s, s);
            ctx.drawItem(st, -8, -8);
            m.popMatrix();
        }
    }

    // ---------- HUD baru ----------
    static class TotemHud extends HudElement {
        final Anim an = new Anim(0);
        final ItemStack st = new ItemStack(Items.TOTEM_OF_UNDYING);
        TotemHud() { super("totem", "Totem Counter", "Combat", "Totems of undying in your inventory", 6, 190); }
        static int count() {
            var p = Modules.mc().player; if (p == null) return 0;
            var inv = p.getInventory(); int c = 0;
            for (int i = 0; i < inv.size(); i++) { ItemStack s = inv.getStack(i); if (s.isOf(Items.TOTEM_OF_UNDYING)) c += s.getCount(); }
            return c;
        }
        @Override protected int cw() { return 20 + tr().getWidth("x00"); }
        @Override protected int ch() { return 16; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            int n = count(); an.to(n); int v = Math.round(an.get());
            ctx.drawItem(st, x, y);
            textC(ctx, "x" + v, x + 19, y + 4, n == 0 ? 0xFFF87171 : textColor.c);
        }
    }

    static class ShieldHud extends HudElement {
        final ItemStack st = new ItemStack(Items.SHIELD);
        ShieldHud() { super("shield", "Shield Status", "Combat", "Shield icon turns green > orange > red with durability, red flash when disabled", 6, 210); }
        @Override protected int cw() { return 20 + tr().getWidth("Cooldown 100%"); }
        @Override protected int ch() { return 16; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            var p = Modules.mc().player;
            float r = 1f; String s = HudManager.editing ? "Ready" : "No shield"; int col = 0xFF9CA3AF; boolean has = HudManager.editing, broken = false;
            if (p != null) {
                ItemStack sh = p.getOffHandStack().isOf(Items.SHIELD) ? p.getOffHandStack()
                        : p.getMainHandStack().isOf(Items.SHIELD) ? p.getMainHandStack() : ItemStack.EMPTY;
                if (!sh.isEmpty()) {
                    has = true;
                    r = sh.isDamageable() ? (sh.getMaxDamage() - sh.getDamage()) / (float) sh.getMaxDamage() : 1f;
                    if (p.getItemCooldownManager().isCoolingDown(sh)) { s = "Disabled"; broken = true; }
                    else s = p.isBlocking() ? "Blocking" : "Ready";
                }
            }
            if (has) col = r > 0.6f ? 0xFF4ADE80 : r > 0.3f ? 0xFFFB923C : 0xFFF87171;
            if (broken) { float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 120.0); col = Draw.lerp(0xFF7F1D1D, 0xFFFF4444, pulse); }
            Draw.rrect(ctx, x - 1, y - 1, 18, 18, 4, a(Draw.a(col, has ? 0.35f : 0.1f)));
            ctx.drawItem(st, x, y);
            if (has) Draw.rrect(ctx, x, y, 16, 16, 3, a(Draw.a(col, broken ? 0.55f : 0.32f)));
            textC(ctx, has ? s + " " + Math.round(r * 100) + "%" : s, x + 19, y + 4, col);
        }
    }
}
