package com.nextfam.hud;

import com.nextfam.config.Setting;
import com.nextfam.util.Freelook;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public final class Features {
    private Features() {}
    public static final String[] KEY_NAMES = {"Left Alt", "Left Ctrl", "Z", "X", "C", "V", "B", "R", "G", "H", "F"};
    private static final int[] KEY_CODES = {342, 341, 90, 88, 67, 86, 66, 82, 71, 72, 70};

    private static final List<Entity> PENDING = new ArrayList<>();
    // FPS optimizer
    private static boolean applied; private static ParticlesMode oldP; private static boolean oldS; private static double oldD;
    // particle customizer
    private static boolean pApplied; private static ParticlesMode pOld;
    // fov
    private static boolean fovApplied; private static int fovOrig; private static double fovCur, effOrig;
    // toggle sprint / sneak
    private static boolean spApplied, snApplied, spOld, snOld;
    // freelook
    private static boolean persp; private static Perspective oldPersp;

    public static boolean on(String id) { HudElement e = HudManager.get(id); return e != null && e.enabled; }

    public static boolean classicHand() {
        HudElement e = HudManager.get("anim17");
        return e != null && e.enabled && e.s("noDip") != null && e.s("noDip").b;
    }

    public static boolean hideVanillaCrosshair() {
        HudElement e = HudManager.get("crosshair");
        return e != null && e.enabled && e.s("hideVanilla") != null && e.s("hideVanilla").b;
    }

    public static boolean hideTotemPop() {
        HudElement e = HudManager.get("totemtweaks");
        return e != null && e.enabled && e.s("hidePop") != null && e.s("hidePop").b;
    }

    private static boolean held(MinecraftClient c, Setting key) {
        return key != null && c.currentScreen == null
                && GLFW.glfwGetKey(c.getWindow().getHandle(), KEY_CODES[Math.max(0, Math.min(KEY_CODES.length - 1, key.idx))]) == GLFW.GLFW_PRESS;
    }

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof EndCrystalEntity && on("crystals")) PENDING.add(entity);
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(Features::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(Features::restoreAll);
        More.initScreens();
        V4.init();
    }

    private static void tick(MinecraftClient c) {
        if (c.world == null || c.player == null) { Freelook.mode = 0; HOLD.clear(); return; }
        if (!PENDING.isEmpty()) { for (Entity e : PENDING) e.discard(); PENDING.clear(); }
        holdTick(c);
        fps(c);
        particles(c);
        view(c);
        toggles(c);
        look(c);
        HudElement w = HudManager.get("weather");
        if (w != null && w.enabled) {
            int m = w.s("mode").idx;
            c.world.setRainGradient(m >= 1 ? 1f : 0f);
            c.world.setThunderGradient(m == 2 ? 1f : 0f);
        }
    }

    // ---- Death animation: berdiri tegak + tahan badan beberapa detik ----
    public static volatile long totemPopStart = -100000;
    private static final Set<Integer> BYPASS = new HashSet<>();
    private static final List<Object[]> HOLD = new ArrayList<>();

    public static boolean cancelVanillaPop() {
        HudElement e = HudManager.get("totemtweaks");
        return e != null && e.enabled && (e.s("hidePop").b || e.s("custom").b);
    }

    public static boolean uprightFor(LivingEntity le) {
        HudElement d = HudManager.get("deathanim");
        if (d == null || !d.enabled || !d.s("upright").b) return false;
        if (le == MinecraftClient.getInstance().player) return false;
        return d.s("players").b || !(le instanceof PlayerEntity);
    }

    public static boolean holdBody(ClientWorld w, int id, Entity.RemovalReason r) {
        if (BYPASS.remove(id)) return false;
        HudElement d = HudManager.get("deathanim");
        if (d == null || !d.enabled) return false;
        float sec = d.s("keep").f;
        if (sec <= 0f) return false;
        Entity e = w.getEntityById(id);
        if (!(e instanceof LivingEntity le) || le == MinecraftClient.getInstance().player || le.deathTime <= 0) return false;
        if (le instanceof PlayerEntity && !d.s("players").b) return false;
        HOLD.add(new Object[]{id, r, Math.round(sec * 20f)});
        return true;
    }

    private static void holdTick(MinecraftClient c) {
        if (HOLD.isEmpty()) return;
        Iterator<Object[]> it = HOLD.iterator();
        List<Object[]> done = new ArrayList<>();
        while (it.hasNext()) {
            Object[] h = it.next();
            int t = (Integer) h[2] - 1; h[2] = t;
            if (t <= 0) { done.add(h); it.remove(); }
        }
        for (Object[] h : done) { BYPASS.add((Integer) h[0]); c.world.removeEntity((Integer) h[0], (Entity.RemovalReason) h[1]); }
    }

    private static boolean fpsApplied, oldVsync; private static int oldMax;

    private static void fps(MinecraftClient c) {
        var o = c.options; HudElement e = HudManager.get("fpsopt");
        if (e != null && e.enabled) {
            if (!applied) {
                oldP = o.getParticles().getValue(); oldS = o.getEntityShadows().getValue(); oldD = o.getEntityDistanceScaling().getValue();
                oldMax = o.getMaxFps().getValue(); oldVsync = o.getEnableVsync().getValue();
                applied = true;
            }
            if (e.s("uncap").b) o.getMaxFps().setValue(260);
            if (e.s("vsync").b) o.getEnableVsync().setValue(false);
            if (e.s("particles").b) o.getParticles().setValue(ParticlesMode.MINIMAL);
            if (e.s("shadows").b) o.getEntityShadows().setValue(false);
            o.getEntityDistanceScaling().setValue((double) e.s("dist").f);
        } else if (applied) restoreFps(o);
    }

    private static void restoreFps(net.minecraft.client.option.GameOptions o) {
        o.getParticles().setValue(oldP); o.getEntityShadows().setValue(oldS); o.getEntityDistanceScaling().setValue(oldD);
        o.getMaxFps().setValue(oldMax); o.getEnableVsync().setValue(oldVsync);
        applied = false;
    }

    private static void particles(MinecraftClient c) {
        var o = c.options; HudElement e = HudManager.get("particles");
        if (e != null && e.enabled) {
            if (!pApplied) { pOld = o.getParticles().getValue(); pApplied = true; }
            ParticlesMode[] m = {ParticlesMode.ALL, ParticlesMode.DECREASED, ParticlesMode.MINIMAL};
            o.getParticles().setValue(m[e.s("level").idx]);
        } else if (pApplied) { o.getParticles().setValue(pOld); pApplied = false; }
    }

    private static void view(MinecraftClient c) {
        var o = c.options;
        HudElement z = HudManager.get("zoom"), f = HudManager.get("customfov");
        boolean zoom = z != null && z.enabled && held(c, z.s("key")), custom = f != null && f.enabled;
        if (zoom || custom) {
            if (!fovApplied) { fovOrig = o.getFov().getValue(); effOrig = o.getFovEffectScale().getValue(); fovCur = fovOrig; fovApplied = true; }
            double target = zoom ? z.s("fov").f : f.s("fov").f;
            fovCur += (target - fovCur) * 0.45;
            o.getFov().setValue((int) Math.round(fovCur));
            if (custom && f.s("noDynamic").b) o.getFovEffectScale().setValue(0.0);
            else o.getFovEffectScale().setValue(effOrig);
        } else if (fovApplied) {
            o.getFov().setValue(fovOrig); o.getFovEffectScale().setValue(effOrig); fovApplied = false;
        }
    }

    private static void toggles(MinecraftClient c) {
        var o = c.options;
        if (on("togglesprint")) { if (!spApplied) { spOld = o.getSprintToggled().getValue(); spApplied = true; } o.getSprintToggled().setValue(true); }
        else if (spApplied) { o.getSprintToggled().setValue(spOld); spApplied = false; }
        if (on("togglesneak")) { if (!snApplied) { snOld = o.getSneakToggled().getValue(); snApplied = true; } o.getSneakToggled().setValue(true); }
        else if (snApplied) { o.getSneakToggled().setValue(snOld); snApplied = false; }
    }

    private static void look(MinecraftClient c) {
        HudElement f = HudManager.get("freelook"), s = HudManager.get("snaplook");
        int mode = 0;
        if (f != null && f.enabled && held(c, f.s("key"))) mode = 1;
        else if (s != null && s.enabled && held(c, s.s("key"))) mode = 2;
        if (mode == 1 && Freelook.mode != 1) {
            Freelook.yaw = c.player.getYaw(); Freelook.pitch = c.player.getPitch();
            oldPersp = c.options.getPerspective(); c.options.setPerspective(Perspective.THIRD_PERSON_BACK); persp = true;
        }
        if (mode != 1 && Freelook.mode == 1 && persp) { c.options.setPerspective(oldPersp); persp = false; }
        Freelook.mode = mode;
    }

    private static void restoreAll(MinecraftClient c) {
        var o = c.options;
        if (applied) restoreFps(o);
        if (pApplied) { o.getParticles().setValue(pOld); pApplied = false; }
        if (fovApplied) { o.getFov().setValue(fovOrig); o.getFovEffectScale().setValue(effOrig); fovApplied = false; }
        if (spApplied) { o.getSprintToggled().setValue(spOld); spApplied = false; }
        if (snApplied) { o.getSneakToggled().setValue(snOld); snApplied = false; }
    }
}
