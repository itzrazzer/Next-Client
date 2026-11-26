package com.nextfam.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Loading screen hitam dengan logo NextFam (tanpa teks, karena font belum tentu siap). */
public final class LoadingScreen {
    private LoadingScreen() {}

    public static void draw(DrawContext ctx, float progress) {
        var w = MinecraftClient.getInstance().getWindow();
        int sw = w.getScaledWidth(), sh = w.getScaledHeight();
        ctx.fill(0, 0, sw, sh, 0xFF000000);
        int size = Math.max(64, Math.min(sw, sh) / 3);
        Logo.draw(ctx, (sw - size) / 2, (sh - size) / 2 - 12, size, 1f);
        int bw = Math.min(200, sw / 2), bx = (sw - bw) / 2, by = (sh + size) / 2 + 4;
        ctx.fill(bx, by, bx + bw, by + 3, 0xFF1F1F29);
        float p = Math.max(0.03f, Math.min(1f, progress));
        ctx.fill(bx, by, bx + Math.round(bw * p), by + 3, 0xFFA855F7);
    }
}
