package com.nextfam.hud;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;

public final class CombatTracker {
    public static LivingEntity target;
    public static long lastHit, lastDamageTime;
    public static float lastDamage, lastHp;
    public static int combo;
    public static double lastReach;
    private static int lastHurt;

    public static boolean targetRecent() { return target != null && System.currentTimeMillis() - lastHit < 5000; }

    public static void init() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity le) {
                target = le; lastHit = System.currentTimeMillis(); lastHp = le.getHealth(); combo++;
                var eye = player.getEyePos(); var bb = entity.getBoundingBox();
                lastReach = eye.distanceTo(new net.minecraft.util.math.Vec3d(
                        Math.max(bb.minX, Math.min(eye.x, bb.maxX)), Math.max(bb.minY, Math.min(eye.y, bb.maxY)), Math.max(bb.minZ, Math.min(eye.z, bb.maxZ))));
            }
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(CombatTracker::tick);
    }

    private static void tick(MinecraftClient c) {
        var p = c.player;
        if (p == null) return;
        if (p.hurtTime > 0 && lastHurt == 0) combo = 0;
        lastHurt = p.hurtTime;
        long now = System.currentTimeMillis();
        if (now - lastHit > 3000) combo = 0;
        if (target != null) {
            float hp = target.getHealth();
            if (hp < lastHp) { lastDamage = lastHp - hp; lastDamageTime = now; }
            lastHp = hp;
            if (now - lastHit > 5000) target = null;
        }
    }
}
