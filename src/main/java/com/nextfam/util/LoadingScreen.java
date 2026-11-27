package com.nextfam.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Loading screen NextFam: latar hitam, bintang berkedip, logo berdenyut dengan cahaya, titik mengorbit, progress bar bersinar. */
public final class LoadingScreen {
    private LoadingScreen() {}

    public static void draw(DrawContext ctx, float progress) {
        var w = MinecraftClient.getInstance().getWindow();
        int sw = w.getScaledWidth(), sh = w.getScaledHeight();
        float t = (System.nanoTime() / 1_000_000_000.0f);
        ctx.fill(0, 0, sw, sh, 0xFF000000);
        ctx.fillGradient(0, sh / 2, sw, sh, 0x00000000, 0x44202020);

        // bintang
        for (int i = 0; i < 70; i++) {
            int x = (i * 7919) % Math.max(1, sw), y = (i * 104729) % Math.max(1, sh);
            float a = 0.10f + 0.40f * (0.5f + 0.5f * (float) Math.sin(t * (0.8 + (i % 5) * 0.3) + i));
            ctx.fill(x, y, x + 1, y + 1, Draw.a(0xFFEDEDED, a));
        }

        int size = Math.max(72, Math.min(sw, sh) * 2 / 5);
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 2.0);
        int cx = sw / 2, cy = sh / 2 - 14;

        // cahaya di belakang logo
        for (int k = 6; k >= 1; k--) {
            int r = size / 2 + k * 7 + Math.round(pulse * 4);
            Draw.rrect(ctx, cx - r, cy - r, r * 2, r * 2, r, Draw.a(0xFFFFFFFF, 0.035f + 0.01f * (6 - k)));
        }
        Logo.draw(ctx, cx - size / 2, cy - size / 2, size, 0.85f + 0.15f * pulse);

        // titik mengorbit
        int R = size / 2 + 22;
        for (int i = 0; i < 16; i++) {
            double ang = t * 1.4 + i * Math.PI * 2 / 16;
            int dx = cx + (int) Math.round(Math.cos(ang) * R), dy = cy + (int) Math.round(Math.sin(ang) * R);
            float a = (i + 1) / 16f;
            int s = i > 11 ? 3 : 2;
            ctx.fill(dx - s / 2, dy - s / 2, dx - s / 2 + s, dy - s / 2 + s, Draw.a(0xFFE5E5E5, 0.15f + 0.85f * a));
        }

        // progress bar
        int bw = Math.min(240, sw * 2 / 3), bx = (sw - bw) / 2, by = cy + size / 2 + 34;
        Draw.rrect(ctx, bx - 1, by - 1, bw + 2, 7, 3, 0x33FFFFFF);
        Draw.rrect(ctx, bx, by, bw, 5, 2, 0xFF0B0B10);
        float p = Math.max(0.04f, Math.min(1f, progress));
        int fw = Math.round(bw * p);
        Draw.rrectGrad(ctx, bx, by, fw, 5, 2, 0xFFE5E5E5, 0xFF8C8C8C);
        int sx = bx + (int) (((t * 0.7f) % 1f) * fw);
        ctx.fill(Math.max(bx, sx - 8), by, Math.min(bx + fw, sx + 8), by + 5, 0x55FFFFFF);
    }
}
