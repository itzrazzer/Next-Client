package com.nextfam.hud;

import com.nextfam.config.Setting;
import com.nextfam.mixin.HandledScreenAccessor;
import com.nextfam.util.Anim;
import com.nextfam.util.Draw;
import com.nextfam.util.Proj;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import java.util.*;

public final class More {
    private More() {}

    static final Item[] IT = {null, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.ENDER_PEARL, Items.ARROW, Items.TOTEM_OF_UNDYING,
        Items.OBSIDIAN, Items.END_CRYSTAL, Items.TNT, Items.EXPERIENCE_BOTTLE, Items.COBBLESTONE, Items.OAK_PLANKS, Items.IRON_INGOT,
        Items.DIAMOND, Items.GOLD_INGOT, Items.SNOWBALL, Items.FIREWORK_ROCKET, Items.COOKED_BEEF, Items.WATER_BUCKET, Items.LAVA_BUCKET};
    static final String[] ITN = {"None", "Golden Apple", "Enchanted Golden Apple", "Ender Pearl", "Arrow", "Totem", "Obsidian", "End Crystal",
        "TNT", "XP Bottle", "Cobblestone", "Planks", "Iron Ingot", "Diamond", "Gold Ingot", "Snowball", "Firework", "Cooked Beef",
        "Water Bucket", "Lava Bucket"};

    static int count(Item item) {
        var p = Modules.mc().player; if (p == null || item == null) return 0;
        var inv = p.getInventory(); int c = 0;
        for (int i = 0; i < inv.size(); i++) { ItemStack s = inv.getStack(i); if (s.isOf(item)) c += s.getCount(); }
        return c;
    }

    public static void register(List<HudElement> l) {
        int n = l.size();
        l.add(new InventoryHud());
        l.add(new ItemCounter());
        l.add(new PotionCounter());
        l.add(new DurabilityHud());
        l.add(new EquipmentHud());
        l.add(new EnchantHud());
        l.add(new Modules.TextHud("reach", "Reach Display", "Combat", "Distance of your last hit", 6, 250,
                () -> CombatTracker.lastReach <= 0 ? "Reach: -" : String.format(Locale.ROOT, "Reach: %.2f", CombatTracker.lastReach)));
        l.add(new UhcHud());
        l.add(new Modules.TextHud("togglesprint", "Toggle Sprint", "Player", "Toggle sprint on and show status", 6, 270,
                () -> { var p = Modules.mc().player; return p != null && p.isSprinting() ? "[Sprinting (Toggled)]" : "[Sprint Toggled]"; }));
        l.add(new Modules.TextHud("togglesneak", "Toggle Sneak", "Player", "Toggle sneak on and show status", 6, 290,
                () -> { var p = Modules.mc().player; return p != null && p.isSneaking() ? "[Sneaking (Toggled)]" : "[Sneak Toggled]"; }));
        l.add(new Hitboxes());
        l.add(new ChunkBorder());
        l.add(new HeightLimit());
        l.add(new LightOverlay());
        l.add(new Trajectory());
        l.add(new DamageTint());
        l.add(new Zoom());
        l.add(new CustomFov());
        l.add(new ParticleCust());
        l.add(new WeatherChanger());
        l.add(new Freelook());
        l.add(new Snaplook());
        l.add(new NoPumpkin());
        l.add(new ShulkerPreview());

        Map<String, Item> m = new HashMap<>();
        m.put("invhud", Items.CHEST); m.put("itemcounter", Items.HOPPER); m.put("potioncounter", Items.SPLASH_POTION);
        m.put("durability", Items.ANVIL); m.put("equipment", Items.IRON_CHESTPLATE); m.put("enchants", Items.ENCHANTED_BOOK);
        m.put("reach", Items.STICK); m.put("uhc", Items.GOLDEN_APPLE); m.put("togglesprint", Items.LEATHER_BOOTS);
        m.put("togglesneak", Items.LEATHER_LEGGINGS); m.put("hitboxes", Items.ITEM_FRAME); m.put("chunkborder", Items.BARRIER);
        m.put("heightlimit", Items.LADDER); m.put("lightoverlay", Items.TORCH); m.put("trajectory", Items.BOW);
        m.put("damagetint", Items.REDSTONE_BLOCK); m.put("zoom", Items.SPYGLASS); m.put("customfov", Items.ENDER_EYE);
        m.put("particles", Items.FIREWORK_STAR); m.put("weather", Items.WATER_BUCKET); m.put("freelook", Items.COMPASS);
        m.put("snaplook", Items.ENDER_PEARL); m.put("nopumpkin", Items.CARVED_PUMPKIN); m.put("shulker", Items.SHULKER_BOX);
        for (int i = n; i < l.size(); i++) {
            HudElement e = l.get(i);
            e.enabled = false; e.defEnabled = false; e.shown.set(0f);
            Item it = m.get(e.id); if (it != null) e.icon = it;
        }
    }

    /** Preview isi shulker box saat kursor di atas item (layar inventory). */
    public static void initScreens() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof HandledScreen<?>)
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, td) -> shulker(ctx, s, mx, my));
        });
    }

    private static void shulker(DrawContext ctx, Screen s, int mx, int my) {
        if (!Features.on("shulker")) return;
        try {
            Slot f = ((HandledScreenAccessor) (Object) s).nextfam$focused();
            if (f == null || !f.hasStack()) return;
            var comp = f.getStack().get(DataComponentTypes.CONTAINER);
            if (comp == null) return;
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack i : comp.iterateNonEmpty()) items.add(i);
            if (items.isEmpty()) return;
            int rows = (items.size() + 8) / 9, w = 9 * 18 + 8, h = rows * 18 + 8;
            int x = Math.min(mx + 12, s.width - w - 2), y = Math.min(my - h - 4 < 2 ? my + 14 : my - h - 4, s.height - h - 2);
            var tr = Modules.mc().textRenderer;
            Draw.rrect(ctx, x, y, w, h, 4, 0xEE101014);
            Draw.rborder(ctx, x, y, w, h, 4, 1, 0xFFA855F7);
            for (int i = 0; i < items.size(); i++) {
                int ix = x + 4 + (i % 9) * 18, iy = y + 4 + (i / 9) * 18;
                ItemStack st = items.get(i);
                ctx.drawItem(st, ix, iy);
                if (st.getCount() > 1) { String c = String.valueOf(st.getCount()); ctx.drawText(tr, c, ix + 17 - tr.getWidth(c), iy + 9, 0xFFFFFFFF, true); }
            }
        } catch (Throwable ignored) {}
    }

    // ================= HUD inventory =================
    static class InventoryHud extends HudElement {
        InventoryHud() { super("invhud", "Inventory HUD", "Inventory", "Your inventory on the screen", 6, 230); }
        @Override protected int cw() { return 162; }
        @Override protected int ch() { return 54; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            var p = Modules.mc().player; var tr = tr();
            for (int i = 0; i < 27; i++) {
                int sx = x + (i % 9) * 18, sy = y + (i / 9) * 18;
                ctx.fill(sx, sy, sx + 16, sy + 16, a(0x33000000));
                if (p == null) continue;
                ItemStack st = p.getInventory().getStack(9 + i);
                if (st.isEmpty()) continue;
                ctx.drawItem(st, sx, sy);
                if (st.getCount() > 1) { String c = String.valueOf(st.getCount()); ctx.drawText(tr, c, sx + 17 - tr.getWidth(c), sy + 9, a(0xFFFFFFFF), true); }
            }
        }
    }

    static class ItemCounter extends HudElement {
        final Setting s1 = extra(Setting.choice("item1", "Item 1", 1, ITN));
        final Setting s2 = extra(Setting.choice("item2", "Item 2", 3, ITN));
        final Setting s3 = extra(Setting.choice("item3", "Item 3", 0, ITN));
        final ItemStack[] st = new ItemStack[IT.length];
        ItemCounter() { super("itemcounter", "Item Counter", "Inventory", "Counts up to 3 chosen items (also works as item tracker)", 6, 300); }
        private List<Item> sel() {
            List<Item> l = new ArrayList<>();
            for (Setting s : new Setting[]{s1, s2, s3}) if (IT[s.idx] != null) l.add(IT[s.idx]);
            if (l.isEmpty() && HudManager.editing) l.add(Items.GOLDEN_APPLE);
            return l;
        }
        @Override protected int cw() { return 20 + tr().getWidth("x000"); }
        @Override protected int ch() { return Math.max(16, sel().size() * 18 - 2); }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            int i = 0;
            for (Item it : sel()) {
                int idx = Arrays.asList(IT).indexOf(it);
                if (st[idx] == null) st[idx] = new ItemStack(it);
                ctx.drawItem(st[idx], x, y + i * 18);
                int c = count(it);
                textC(ctx, "x" + c, x + 19, y + i * 18 + 4, c == 0 ? 0xFFF87171 : textColor.c);
                i++;
            }
        }
    }

    static class PotionCounter extends HudElement {
        final ItemStack st = new ItemStack(Items.SPLASH_POTION);
        final Setting splashOnly = extra(Setting.bool("splashOnly", "Splash Only", true));
        PotionCounter() { super("potioncounter", "Potion Counter", "Inventory", "Potions in your inventory", 6, 320); }
        @Override protected int cw() { return 20 + tr().getWidth("x000"); }
        @Override protected int ch() { return 16; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            int c = count(Items.SPLASH_POTION) + (splashOnly.b ? 0 : count(Items.POTION) + count(Items.LINGERING_POTION));
            ctx.drawItem(st, x, y);
            textC(ctx, "x" + c, x + 19, y + 4, c == 0 ? 0xFFF87171 : textColor.c);
        }
    }

    static class DurabilityHud extends HudElement {
        final Anim an = new Anim(1);
        DurabilityHud() { super("durability", "Durability Display", "Inventory", "Durability of the item in your hand", 6, 340); }
        @Override protected int cw() { return 20 + tr().getWidth("0000/0000"); }
        @Override protected int ch() { return 16; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            var p = Modules.mc().player;
            ItemStack s = p == null ? ItemStack.EMPTY : p.getMainHandStack();
            if (s.isEmpty() || !s.isDamageable()) {
                if (!s.isEmpty()) ctx.drawItem(s, x, y);
                textC(ctx, HudManager.editing ? "1561/1561" : "-", x + 19, y + 4, 0xFF9CA3AF); return;
            }
            int left = s.getMaxDamage() - s.getDamage();
            an.to(left / (float) s.getMaxDamage()); float r = an.get();
            ctx.drawItem(s, x, y);
            textC(ctx, left + "/" + s.getMaxDamage(), x + 19, y + 1, Modules.ramp(r, 0.1f, 0.7f));
            ctx.fill(x + 19, y + 12, x + w, y + 14, a(0x66000000));
            ctx.fill(x + 19, y + 12, x + 19 + Math.round((w - 19) * r), y + 14, a(Modules.ramp(r, 0.1f, 0.7f)));
        }
    }

    static class EquipmentHud extends HudElement {
        EquipmentHud() { super("equipment", "Equipment HUD", "Inventory", "Hands and armor in one row with durability", 6, 360); }
        @Override protected int cw() { return 6 * 18; }
        @Override protected int ch() { return 22; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            var p = Modules.mc().player; if (p == null) return;
            ItemStack[] s = {p.getMainHandStack(), p.getOffHandStack(), p.getEquippedStack(EquipmentSlot.HEAD),
                p.getEquippedStack(EquipmentSlot.CHEST), p.getEquippedStack(EquipmentSlot.LEGS), p.getEquippedStack(EquipmentSlot.FEET)};
            for (int i = 0; i < 6; i++) {
                int sx = x + i * 18;
                if (s[i].isEmpty()) { if (HudManager.editing) Draw.rborder(ctx, sx, y, 16, 16, 2, 1, a(0x44FFFFFF)); continue; }
                ctx.drawItem(s[i], sx, y);
                if (s[i].isDamageable()) {
                    float r = (s[i].getMaxDamage() - s[i].getDamage()) / (float) s[i].getMaxDamage();
                    ctx.fill(sx, y + 18, sx + 16, y + 20, a(0x66000000));
                    ctx.fill(sx, y + 18, sx + Math.round(16 * r), y + 20, a(Modules.ramp(r, 0.1f, 0.7f)));
                }
            }
        }
    }

    static class EnchantHud extends HudElement {
        EnchantHud() { super("enchants", "Enchantment Display", "Inventory", "Enchantments of the item in your hand", 6, 390); }
        private List<String> lines() {
            List<String> out = new ArrayList<>(); var p = Modules.mc().player;
            if (p != null) {
                ItemStack s = p.getMainHandStack();
                add(out, s.get(DataComponentTypes.ENCHANTMENTS));
                add(out, s.get(DataComponentTypes.STORED_ENCHANTMENTS));
            }
            if (out.isEmpty() && HudManager.editing) out.add("Sharpness V");
            return out;
        }
        private static void add(List<String> out, ItemEnchantmentsComponent c) {
            if (c == null) return;
            for (var e : c.getEnchantmentEntries()) out.add(Enchantment.getName(e.getKey(), e.getIntValue()).getString());
        }
        @Override public boolean visible() { return !lines().isEmpty(); }
        @Override protected int cw() { int w = 40; for (String s : lines()) w = Math.max(w, tr().getWidth(s)); return w; }
        @Override protected int ch() { return Math.max(9, lines().size() * 10 - 1); }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            int i = 0; for (String s : lines()) { textA(ctx, s, x, y + i * 10, w, textColor.c); i++; }
        }
    }

    static class UhcHud extends HudElement {
        UhcHud() { super("uhc", "UHC Overlay", "HUD", "Golden apples, heads, coordinates and FPS for UHC", 6, 410); }
        private String[] lines() {
            var p = Modules.mc().player;
            String xyz = p == null ? "XYZ: -" : String.format(Locale.ROOT, "XYZ: %d / %d / %d", p.getBlockX(), p.getBlockY(), p.getBlockZ());
            return new String[]{"Gapples: " + count(Items.GOLDEN_APPLE), "Heads: " + count(Items.PLAYER_HEAD), xyz, "FPS: " + Modules.mc().getCurrentFps()};
        }
        @Override protected int cw() { int w = 60; for (String s : lines()) w = Math.max(w, tr().getWidth(s)); return w; }
        @Override protected int ch() { return 4 * 10 - 1; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            int i = 0; for (String s : lines()) { textA(ctx, s, x, y + i * 10, w, i == 0 ? 0xFFFACC15 : textColor.c); i++; }
        }
    }

    // ================= overlay dunia =================
    static class Hitboxes extends Extras.OverlayModule {
        final Setting range = extra(Setting.num("range", "Max Distance", 24f, 4f, 64f).step(1f));
        final Setting thick = extra(Setting.num("thick", "Thickness", 1f, 1f, 3f).step(1f));
        final Setting playerCol = extra(Setting.color("playerCol", "Player Color", 0xFFF87171));
        final Setting mobCol = extra(Setting.color("mobCol", "Mob Color", 0xFFFACC15));
        Hitboxes() { super("hitboxes", "Hitbox Visualizer", "Visual", "Draws entity hitboxes (2D overlay)"); }
        @Override public void worldRender(DrawContext ctx, int sw, int sh, float tp) {
            var mc = Modules.mc();
            if (!Proj.ready || mc.world == null || mc.player == null) return;
            double r2 = range.f * range.f;
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player || !(e instanceof LivingEntity || e instanceof EndCrystalEntity)) continue;
                if (e.squaredDistanceTo(mc.player) > r2) continue;
                Vec3d lp = e.getLerpedPos(tp);
                Box b = e.getBoundingBox().offset(lp.x - e.getX(), lp.y - e.getY(), lp.z - e.getZ());
                Proj.box(ctx, b, e instanceof PlayerEntity ? playerCol.c : mobCol.c, Math.round(thick.f));
            }
        }
    }

    static class ChunkBorder extends Extras.OverlayModule {
        final Setting color = extra(Setting.color("color", "Color", 0xFF22D3EE));
        ChunkBorder() { super("chunkborder", "Chunk Border", "Visual", "Chunk borders around you (2D overlay)"); }
        @Override public void worldRender(DrawContext ctx, int sw, int sh, float tp) {
            var mc = Modules.mc(); var p = mc.player;
            if (!Proj.ready || p == null) return;
            double cx = (p.getBlockX() >> 4) << 4, cz = (p.getBlockZ() >> 4) << 4, y0 = p.getY() - 10, y1 = p.getY() + 10;
            int c = color.c, soft = Draw.a(c, 0.45f);
            for (int i = 0; i <= 16; i += 4) {
                int col = (i == 0 || i == 16) ? c : soft;
                Proj.line3(ctx, cx + i, y0, cz, cx + i, y1, cz, col, 1);
                Proj.line3(ctx, cx + i, y0, cz + 16, cx + i, y1, cz + 16, col, 1);
                Proj.line3(ctx, cx, y0, cz + i, cx, y1, cz + i, col, 1);
                Proj.line3(ctx, cx + 16, y0, cz + i, cx + 16, y1, cz + i, col, 1);
            }
            for (double y = Math.ceil(y0 / 16) * 16; y <= y1; y += 16) {
                Proj.line3(ctx, cx, y, cz, cx + 16, y, cz, c, 1); Proj.line3(ctx, cx + 16, y, cz, cx + 16, y, cz + 16, c, 1);
                Proj.line3(ctx, cx + 16, y, cz + 16, cx, y, cz + 16, c, 1); Proj.line3(ctx, cx, y, cz + 16, cx, y, cz, c, 1);
            }
        }
    }

    static class HeightLimit extends Extras.OverlayModule {
        HeightLimit() { super("heightlimit", "Height Limit Overlay", "Visual", "Grid at the build limits when you are near them"); }
        @Override public void worldRender(DrawContext ctx, int sw, int sh, float tp) {
            var mc = Modules.mc(); var p = mc.player;
            if (!Proj.ready || p == null || mc.world == null) return;
            double bottom = mc.world.getBottomY(), top = bottom + mc.world.getHeight();
            double[] ys = {top, bottom}; int[] cols = {0xFFF87171, 0xFF60A5FA};
            for (int k = 0; k < 2; k++) {
                if (Math.abs(p.getY() - ys[k]) > 20) continue;
                double px = Math.floor(p.getX() / 2) * 2, pz = Math.floor(p.getZ() / 2) * 2;
                for (int i = -8; i <= 8; i += 2) {
                    Proj.line3(ctx, px - 8, ys[k], pz + i, px + 8, ys[k], pz + i, cols[k], 1);
                    Proj.line3(ctx, px + i, ys[k], pz - 8, px + i, ys[k], pz + 8, cols[k], 1);
                }
            }
        }
    }

    static class LightOverlay extends Extras.OverlayModule {
        final Setting radius = extra(Setting.num("radius", "Radius", 6f, 3f, 10f).step(1f));
        private final List<int[]> cache = new ArrayList<>(); private int frame;
        LightOverlay() { super("lightoverlay", "Light Overlay", "Visual", "Red X where mobs can spawn (block light 0)"); }
        @Override public void worldRender(DrawContext ctx, int sw, int sh, float tp) {
            var mc = Modules.mc(); var p = mc.player; var w = mc.world;
            if (!Proj.ready || p == null || w == null) return;
            if (frame++ % 10 == 0) {
                cache.clear(); int r = Math.round(radius.f); BlockPos o = p.getBlockPos();
                for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) for (int dy = -3; dy <= 2; dy++) {
                    BlockPos pos = o.add(dx, dy, dz);
                    if (!w.getBlockState(pos).isAir()) continue;
                    BlockPos below = pos.down();
                    if (!w.getBlockState(below).isSideSolidFullSquare(w, below, Direction.UP)) continue;
                    int bl = w.getLightLevel(LightType.BLOCK, pos);
                    cache.add(new int[]{pos.getX(), pos.getY(), pos.getZ(), bl == 0 ? 0xFFF87171 : 0xFF4ADE80});
                    if (cache.size() > 700) return;
                }
            }
            for (int[] c : cache) {
                double x = c[0], y = c[1] + 0.02, z = c[2];
                Proj.line3(ctx, x + 0.2, y, z + 0.2, x + 0.8, y, z + 0.8, c[3], 1);
                Proj.line3(ctx, x + 0.8, y, z + 0.2, x + 0.2, y, z + 0.8, c[3], 1);
            }
        }
    }

    static class Trajectory extends Extras.OverlayModule {
        final Setting color = extra(Setting.color("color", "Line Color", 0xFFFACC15));
        Trajectory() { super("trajectory", "Projectile Trajectory", "Combat", "Predicted path for bow, crossbow, pearls, snowballs, potions"); }
        @Override public void worldRender(DrawContext ctx, int sw, int sh, float tp) {
            var mc = Modules.mc(); var p = mc.player; var w = mc.world;
            if (!Proj.ready || p == null || w == null) return;
            ItemStack s = p.getMainHandStack(); double speed = 0, g = 0.05;
            if (s.isOf(Items.BOW) && p.isUsingItem()) { float pull = BowItem.getPullProgress(p.getItemUseTime()); if (pull < 0.1f) return; speed = pull * 3.0; }
            else if (s.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(s)) speed = 3.15;
            else if (s.isOf(Items.TRIDENT) && p.isUsingItem()) speed = 2.5;
            else if (s.isOf(Items.ENDER_PEARL) || s.isOf(Items.SNOWBALL) || s.isOf(Items.EGG)) { speed = 1.5; g = 0.03; }
            else if (s.isOf(Items.SPLASH_POTION) || s.isOf(Items.LINGERING_POTION)) speed = 0.5;
            else if (s.isOf(Items.EXPERIENCE_BOTTLE)) { speed = 0.7; g = 0.07; }
            if (speed <= 0) return;
            double[] cp = Proj.camPos(), d = Proj.dir();
            double x = cp[0], y = cp[1] - 0.1, z = cp[2], vx = d[0] * speed, vy = d[1] * speed, vz = d[2] * speed;
            for (int i = 0; i < 90; i++) {
                double nx = x + vx, ny = y + vy, nz = z + vz;
                Proj.line3(ctx, x, y, z, nx, ny, nz, color.c, 1);
                BlockPos bp = BlockPos.ofFloored(nx, ny, nz);
                if (!w.getBlockState(bp).getCollisionShape(w, bp).isEmpty()) {
                    Proj.box(ctx, new Box(nx - 0.15, ny - 0.15, nz - 0.15, nx + 0.15, ny + 0.15, nz + 0.15), 0xFFF87171, 1);
                    return;
                }
                x = nx; y = ny; z = nz; vx *= 0.99; vy = vy * 0.99 - g; vz *= 0.99;
            }
        }
    }

    static class DamageTint extends Extras.OverlayModule {
        final Setting color = extra(Setting.color("color", "Tint Color", 0xFFFF2020));
        final Setting strength = extra(Setting.num("strength", "Strength", 0.5f, 0.1f, 1f));
        DamageTint() { super("damagetint", "Damage Tint", "Visual", "Colored screen edges when you take damage"); }
        @Override public void worldRender(DrawContext ctx, int sw, int sh, float tp) {
            var p = Modules.mc().player; if (p == null || p.hurtTime <= 0) return;
            float t = Math.min(1f, p.hurtTime / 10f) * strength.f;
            int c0 = Draw.a(color.c, t), c1 = Draw.a(color.c, 0f);
            ctx.fillGradient(0, 0, sw, sh / 4, c0, c1);
            ctx.fillGradient(0, sh - sh / 4, sw, sh, c1, c0);
        }
    }

    // ================= modul yang dikerjakan lewat Features / mixin =================
    static class Zoom extends Extras.OverlayModule {
        final Setting key = extra(Setting.choice("key", "Zoom Key", 4, Features.KEY_NAMES));
        final Setting fov = extra(Setting.num("fov", "Zoom FOV", 30f, 30f, 70f).step(1f));
        Zoom() { super("zoom", "Zoom", "Visual", "Hold the key to zoom in (minimum FOV is 30)"); }
    }
    static class CustomFov extends Extras.OverlayModule {
        final Setting fov = extra(Setting.num("fov", "FOV", 90f, 30f, 110f).step(1f));
        final Setting dyn = extra(Setting.bool("noDynamic", "Disable Dynamic FOV", true));
        CustomFov() { super("customfov", "Custom FOV", "Visual", "Set your FOV and disable sprint / speed FOV change"); }
    }
    static class ParticleCust extends Extras.OverlayModule {
        final Setting level = extra(Setting.choice("level", "Particles", 1, "All", "Decreased", "Minimal"));
        ParticleCust() { super("particles", "Particle Customizer", "Optimize", "Choose the particle level"); }
    }
    static class WeatherChanger extends Extras.OverlayModule {
        final Setting mode = extra(Setting.choice("mode", "Weather", 0, "Clear", "Rain", "Thunder"));
        WeatherChanger() { super("weather", "Weather Changer", "Visual", "Client-side weather (the server is not changed)"); }
    }
    static class Freelook extends Extras.OverlayModule {
        final Setting key = extra(Setting.choice("key", "Hold Key", 0, Features.KEY_NAMES));
        Freelook() { super("freelook", "Freelook", "Tweaks", "Hold the key to look around without turning your player (needs mixin)"); }
    }
    static class Snaplook extends Extras.OverlayModule {
        final Setting key = extra(Setting.choice("key", "Hold Key", 5, Features.KEY_NAMES));
        Snaplook() { super("snaplook", "Snaplook", "Tweaks", "Hold the key to look behind you (needs mixin)"); }
    }
    static class NoPumpkin extends Extras.OverlayModule {
        NoPumpkin() { super("nopumpkin", "No Pumpkin Overlay", "Tweaks", "Removes the pumpkin blur from your screen (needs mixin)"); }
    }
    static class ShulkerPreview extends Extras.OverlayModule {
        ShulkerPreview() { super("shulker", "Shulker Box Preview", "Inventory", "Shows shulker box contents when hovering it in an inventory"); }
    }
}
