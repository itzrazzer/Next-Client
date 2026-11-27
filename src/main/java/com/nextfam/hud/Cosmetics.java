package com.nextfam.hud;

import com.nextfam.config.NfConfig;
import com.nextfam.config.Setting;
import com.nextfam.mixin.EntityStateAccessor;
import com.nextfam.mixin.PlayerStateAccessor;
import com.nextfam.util.Anim;
import com.nextfam.util.Draw;
import com.nextfam.util.Proj;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

/** Ping Equalizer, Wings (overlay 2D), Cape custom, logo NextFam di nametag / tab. */
public final class Cosmetics {
    private Cosmetics() {}

    public static void register(List<HudElement> l) {
        l.add(new PingEq());
        l.add(new Wings());
        l.add(new CapeMod());
        l.add(new NfLogo());
    }

    // ---------------- logo di nametag + tab ----------------
    private static final Style LOGO = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("nextfam", "logo")));

    public static Text withLogo(Text t) {
        return Text.empty().append(Text.literal("\uE000").setStyle(LOGO)).append(Text.literal(" ")).append(t);
    }
    public static boolean logoTab() { HudElement e = HudManager.get("nflogo"); return e != null && e.enabled && e.s("tab").b; }
    public static boolean logoName() { HudElement e = HudManager.get("nflogo"); return e != null && e.enabled && e.s("nametag").b; }

    /** Dipanggil mixin untuk pemain lokal: ganti cape dan beri logo di nametag. */
    public static void applyToState(Object stateObj) {
        try {
            HudElement cm = HudManager.get("capemod");
            if (cm != null && cm.enabled && !NfConfig.cape.isEmpty()) {
                CapeManager.ensure();
                var a = CapeManager.asset();
                if (CapeManager.ok && a != null) {
                    PlayerStateAccessor ps = (PlayerStateAccessor) stateObj;
                    SkinTextures st = ps.nextfam$skin();
                    if (st != null) ps.nextfam$setSkin(new SkinTextures(st.body(), a, st.elytra(), st.model(), st.secure()));
                }
            }
            if (logoName()) {
                EntityStateAccessor es = (EntityStateAccessor) stateObj;
                Text dn = es.nextfam$name();
                if (dn != null) es.nextfam$setName(withLogo(dn));
            }
        } catch (Throwable ignored) {}
    }

    // ---------------- Cape ----------------
    public static final class CapeManager {
        static final Identifier TEX = Identifier.of("nextfam", "cape/custom");
        static String loaded = null; static boolean ok; static Object assetCache;
        private static List<String> filesCache = List.of(); private static long filesT;

        public static Path dir() { return NfConfig.dir().resolve("capes"); }

        public static List<String> files() {
            long now = System.currentTimeMillis();
            if (now - filesT < 2500) return filesCache;
            filesT = now;
            List<String> l = new ArrayList<>();
            try {
                Files.createDirectories(dir());
                Path readme = dir().resolve("README.txt");
                if (!Files.exists(readme)) Files.writeString(readme, "Put cape PNG files here (64x32 cape texture, or any picture - it will be fitted onto a cape).\nThen pick it in NextFam > Cape.\n");
                try (var st = Files.list(dir())) { st.forEach(p -> { String n = p.getFileName().toString(); if (n.toLowerCase(Locale.ROOT).endsWith(".png")) l.add(n); }); }
            } catch (Exception ignored) {}
            Collections.sort(l);
            filesCache = l;
            return l;
        }

        public static void select(String name) { NfConfig.cape = name; loaded = null; NfConfig.save(); }

        static void ensure() {
            String want = NfConfig.cape;
            if (want.equals(loaded)) return;
            loaded = want; ok = false; assetCache = null;
            if (want.isEmpty()) return;
            try {
                Path root = dir().normalize(), f = root.resolve(want).normalize();
                if (!f.startsWith(root)) return;                       // cegah path traversal
                byte[] png = convert(Files.readAllBytes(f));
                NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
                register(img);
                ok = true;
            } catch (Throwable t) { ok = false; }
        }

        private static byte[] convert(byte[] in) throws Exception {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(in));
            if (src == null) throw new IllegalArgumentException("bad image");
            boolean capeSheet = src.getWidth() == 2 * src.getHeight() && src.getWidth() >= 64;
            BufferedImage out;
            if (capeSheet) out = src;
            else {
                out = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = out.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(src, 2, 2, 20, 32, null);                       // sisi luar cape (1,1,10,16) x2
                g.drawImage(src, 24, 2, 20, 32, null);                      // sisi dalam
                g.setColor(new java.awt.Color(0, 0, 0, 90)); g.fillRect(24, 2, 20, 32);
                g.drawImage(src, 0, 2, 2, 32, null); g.drawImage(src, 22, 2, 2, 32, null);   // sisi kiri / kanan
                g.drawImage(src, 2, 0, 20, 2, null); g.drawImage(src, 22, 0, 20, 2, null);    // atas / bawah
                g.dispose();
            }
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ImageIO.write(out, "png", bo);
            return bo.toByteArray();
        }

        private static void register(NativeImage img) throws Exception {
            var tm = MinecraftClient.getInstance().getTextureManager();
            try { tm.destroyTexture(TEX); } catch (Throwable ignored) {}
            Object tex = null;
            for (Constructor<?> c : NativeImageBackedTexture.class.getConstructors()) {
                Class<?>[] pt = c.getParameterTypes(); Object[] args = new Object[pt.length]; boolean good = true, hasImg = false;
                for (int i = 0; i < pt.length; i++) {
                    if (pt[i] == NativeImage.class) { args[i] = img; hasImg = true; }
                    else if (pt[i] == Supplier.class) args[i] = (Supplier<String>) () -> "nextfam_cape";
                    else if (pt[i] == String.class) args[i] = "nextfam_cape";
                    else { good = false; break; }
                }
                if (good && hasImg) { tex = c.newInstance(args); break; }
            }
            if (tex == null) throw new IllegalStateException("no texture ctor");
            tm.registerTexture(TEX, (AbstractTexture) tex);
        }

        static AssetInfo.TextureAsset asset() {
            if (assetCache != null) return (AssetInfo.TextureAsset) assetCache;
            for (Class<?> c : AssetInfo.class.getDeclaredClasses()) {
                if (c.isInterface() || !AssetInfo.TextureAsset.class.isAssignableFrom(c)) continue;
                for (Constructor<?> k : c.getDeclaredConstructors()) {
                    Class<?>[] pt = k.getParameterTypes();
                    if (pt.length == 0) continue;
                    boolean all = true; for (Class<?> q : pt) if (q != Identifier.class) all = false;
                    if (!all) continue;
                    Object[] a = new Object[pt.length]; Arrays.fill(a, TEX);
                    try { k.setAccessible(true); assetCache = k.newInstance(a); return (AssetInfo.TextureAsset) assetCache; } catch (Throwable ignored) {}
                }
            }
            return null;
        }
    }

    // ---------------- modul ----------------
    static class CapeMod extends Extras.OverlayModule {
        CapeMod() { super("capemod", "Cape", "Visual", "Wear any cape image on your player (only you see it)"); }
        @Override public List<ExtraBtn> extraButtons() {
            List<ExtraBtn> l = new ArrayList<>();
            l.add(new ExtraBtn("NONE (my normal cape)", NfConfig.cape.isEmpty(), () -> CapeManager.select("")));
            for (String f : CapeManager.files()) l.add(new ExtraBtn(f, f.equals(NfConfig.cape), () -> CapeManager.select(f)));
            if (CapeManager.files().isEmpty()) l.add(new ExtraBtn("Put .png files in: config/nextfam/capes/", false, () -> {}));
            return l;
        }
    }

    static class NfLogo extends Extras.OverlayModule {
        final Setting nametag = extra(Setting.bool("nametag", "Logo on my nametag", true));
        final Setting tab = extra(Setting.bool("tab", "Logo in tab list", true));
        NfLogo() { super("nflogo", "NextFam Logo Tag", "Visual", "Shows the NextFam logo left of your name (nametag and tab). Other players need a NextFam server to be detected."); }
    }

    static class PingEq extends HudElement {
        final float[] hist = new float[10]; final Anim[] bars = new Anim[10]; long lastS; int cur;
        PingEq() {
            super("pingeq", "Ping Equalizer", "HUD", "Animated equalizer bars that follow your ping", 6, 420);
            for (int i = 0; i < bars.length; i++) bars[i] = new Anim(0);
            enableIcon();
        }
        private static int ping() {
            var mc = Modules.mc(); var p = mc.player; var nh = mc.getNetworkHandler();
            if (p == null || nh == null) return 0;
            var e = nh.getPlayerListEntry(p.getUuid());
            return e == null ? 0 : e.getLatency();
        }
        @Override protected int cw() { return 10 * 5 + 6 + tr().getWidth("000 ms"); }
        @Override protected int ch() { return 20; }
        @Override protected void content(DrawContext ctx, int x, int y, int w) {
            long now = System.currentTimeMillis();
            if (now - lastS > 400) { System.arraycopy(hist, 1, hist, 0, 9); cur = ping(); hist[9] = cur; lastS = now; }
            for (int i = 0; i < 10; i++) {
                bars[i].to(Math.min(1f, hist[i] / 250f)); float v = bars[i].get();
                int h = 2 + Math.round(v * 17f);
                int col = Modules.ramp(hist[i], 250f, 30f);
                Draw.rrect(ctx, x + i * 5, y + 20 - h, 4, h, 1, a(col));
            }
            textC(ctx, cur + " ms", x + 56, y + 6, Modules.ramp(cur, 250f, 30f));
        }
    }

    static class Wings extends Extras.OverlayModule {
        final Setting style = extra(Setting.choice("style", "Style", 0, "Angel", "Demon", "Neon", "Custom"));
        final Setting color = extra(Setting.color("color", "Custom Color", 0xFFFFFFFF));
        final Setting size = extra(Setting.num("size", "Size", 1.0f, 0.5f, 2f));
        final Setting flap = extra(Setting.num("flap", "Flap Speed", 1.6f, 0.4f, 4f));
        final Setting feathers = extra(Setting.num("feathers", "Feathers", 6f, 4f, 9f).step(1f));
        final Setting self = extra(Setting.bool("self", "Show On Me (third person)", true));
        final Setting others = extra(Setting.bool("others", "Show On Other Players", false));
        Wings() { super("wings", "Wings", "Visual", "Stylized wings on the back of players (2D overlay, visible from behind)"); }

        @Override public void worldRender(DrawContext ctx, int sw, int sh, float tp) {
            var mc = Modules.mc();
            if (!Proj.readyAny || mc.world == null || mc.player == null) return;
            double t = System.currentTimeMillis() / 1000.0;
            for (AbstractClientPlayerEntity pl : mc.world.getPlayers()) {
                boolean me = pl == mc.player;
                if (me && (!self.b || Proj.ready)) continue;
                if (!me && !others.b) continue;
                if (pl.isInvisible() || pl.isSpectator() || pl.squaredDistanceTo(mc.player) > 1600) continue;
                draw(ctx, pl, tp, t);
            }
        }

        private int base(double t) {
            return switch (style.idx) {
                case 0 -> 0xFFF4F4FF; case 1 -> 0xFFB91C1C;
                case 2 -> Draw.hsv((float) ((t * 0.25) % 1.0), 0.65f, 1f, 1f);
                default -> color.c;
            };
        }

        private void draw(DrawContext ctx, AbstractClientPlayerEntity pl, float tp, double t) {
            Vec3d pos = pl.getLerpedPos(tp);
            double yaw = Math.toRadians(MathHelper.lerpAngleDegrees(tp, pl.prevBodyYaw, pl.bodyYaw));
            double bx = Math.sin(yaw), bz = -Math.cos(yaw);           // arah belakang badan
            double rx = -Math.cos(yaw), rz = -Math.sin(yaw);          // kanan pemain
            double[] cp = Proj.camPos();
            if ((cp[0] - pos.x) * bx + (cp[2] - pos.z) * bz <= 0) return;   // kamera di depan: sayap tertutup badan
            double h = pl.isSneaking() ? 1.05 : 1.35;
            int n = Math.round(feathers.f);
            double fl = Math.sin(t * flap.f * Math.PI * 2 / 2.0) * 22.0;
            int col = base(t), mem = Draw.a(style.idx == 1 ? 0xFF1F1F1F : col, 0.55f);
            for (int side = -1; side <= 1; side += 2) {
                double ox = pos.x + bx * 0.18 + rx * side * 0.10, oy = pos.y + h, oz = pos.z + bz * 0.18 + rz * side * 0.10;
                double px = 0, py = 0, pz = 0;
                for (int i = 0; i < n; i++) {
                    double fr = n == 1 ? 0 : i / (double) (n - 1);
                    double ang = Math.toRadians(80 - 125 * fr + fl * (0.4 + fr));
                    double len = size.f * (0.9 + 0.55 * Math.sin(Math.PI * (0.15 + 0.7 * fr)));
                    double lx = rx * side * Math.cos(ang), lz = rz * side * Math.cos(ang), ly = Math.sin(ang);
                    double ex = ox + lx * len + bx * (0.12 + 0.30 * fr), ey = oy + ly * len, ez = oz + lz * len + bz * (0.12 + 0.30 * fr);
                    double[] c = Proj.cam(ox, oy, oz);
                    int th = (int) Math.max(1, Math.min(4, Math.round(9.0 / Math.max(1.0, c[2]))));
                    Proj.line3(ctx, ox, oy, oz, ex, ey, ez, col, th);
                    if (i > 0) Proj.line3(ctx, px, py, pz, ex, ey, ez, mem, Math.max(1, th - 1));
                    px = ex; py = ey; pz = ez;
                }
            }
        }
    }
}
