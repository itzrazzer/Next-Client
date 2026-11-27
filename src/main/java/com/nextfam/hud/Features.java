package com.nextfam.hud;

import com.nextfam.config.Setting;
import com.nextfam.util.Freelook;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
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
        if (c.world == null || c.player == null) { Freelook.mode = 0; return; }
        if (!PENDING.isEmpty()) { for (Entity e : PENDING) e.discard(); PENDING.clear(); }
        deathAnim(c);
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

    private static void deathAnim(MinecraftClient c) {
        HudElement d = HudManager.get("deathanim");
        if (d == null || !d.enabled) return;
        boolean pl = d.s("players") != null && d.s("players").b;
        List<Entity> dead = new ArrayList<>();
        for (Entity e : c.world.getEntities())
            if (e instanceof LivingEntity le && le != c.player && le.deathTime > 0 && (pl || !(e instanceof PlayerEntity))) dead.add(e);
        for (Entity e : dead) e.discard();
    }

    private static void fps(MinecraftClient c) {
        var o = c.options; HudElement e = HudManager.get("fpsopt");
        if (e != null && e.enabled) {
            if (!applied) {
                oldP = o.getParticles().getValue(); oldS = o.getEntityShadows().getValue(); oldD = o.getEntityDistanceScaling().getValue();
                applied = true;
            }
            if (e.s("particles").b) o.getParticles().setValue(ParticlesMode.MINIMAL);
            if (e.s("shadows").b) o.getEntityShadows().setValue(false);
            o.getEntityDistanceScaling().setValue((double) e.s("dist").f);
        } else if (applied) {
            o.getParticles().setValue(oldP); o.getEntityShadows().setValue(oldS); o.getEntityDistanceScaling().setValue(oldD);
            applied = false;
        }
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
        if (applied) { o.getParticles().setValue(oldP); o.getEntityShadows().setValue(oldS); o.getEntityDistanceScaling().setValue(oldD); applied = false; }
        if (pApplied) { o.getParticles().setValue(pOld); pApplied = false; }
        if (fovApplied) { o.getFov().setValue(fovOrig); o.getFovEffectScale().setValue(effOrig); fovApplied = false; }
        if (spApplied) { o.getSprintToggled().setValue(spOld); spApplied = false; }
        if (snApplied) { o.getSneakToggled().setValue(snOld); snApplied = false; }
    }
}
