package com.nextfam.util;

import net.minecraft.client.gui.DrawContext;

public final class Draw {
    private Draw() {}

    public static int a(int argb, float mul) {
        mul = Math.max(0f, Math.min(1f, mul));
        int al = Math.round(((argb >>> 24) & 0xFF) * mul);
        return (al << 24) | (argb & 0xFFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = 0;
        for (int s = 24; s >= 0; s -= 8) {
            int a = (c1 >>> s) & 0xFF, b = (c2 >>> s) & 0xFF;
            r |= (Math.round(a + (b - a) * t) & 0xFF) << s;
        }
        return r;
    }

    private static int inset(int r, int i) {
        if (r <= 0 || i >= r) return 0;
        double dy = r - i - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, (double) r * r - dy * dy)));
    }

    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        rrectGrad(c, x, y, w, h, r, color, color);
    }

    public static void rrectGrad(DrawContext c, int x, int y, int w, int h, int r, int c1, int c2) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        boolean grad = c1 != c2;
        if (!grad) {
            if (r == 0) { c.fill(x, y, x + w, y + h, c1); return; }
            for (int i = 0; i < r; i++) {
                int in = inset(r, i);
                c.fill(x + in, y + i, x + w - in, y + i + 1, c1);
                c.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, c1);
            }
            c.fill(x, y + r, x + w, y + h - r, c1);
            return;
        }
        float den = Math.max(1, h - 1);
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            c.fill(x + in, y + i, x + w - in, y + i + 1, lerp(c1, c2, i / den));
            c.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, lerp(c1, c2, (h - 1 - i) / den));
        }
        if (h - 2 * r > 0) c.fillGradient(x, y + r, x + w, y + h - r, lerp(c1, c2, r / den), lerp(c1, c2, (h - r) / den));
    }

    public static void rborder(DrawContext c, int x, int y, int w, int h, int r, int bw, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) {
            c.fill(x, y, x + w, y + bw, color); c.fill(x, y + h - bw, x + w, y + h, color);
            c.fill(x, y + bw, x + bw, y + h - bw, color); c.fill(x + w - bw, y + bw, x + w, y + h - bw, color);
            return;
        }
        if (bw > r) {
            for (int row = 0; row < h; row++) {
                int in = inset(r, Math.min(row, h - 1 - row));
                int l = x + in, rt = x + w - in;
                if (row < bw || row >= h - bw) c.fill(l, y + row, rt, y + row + 1, color);
                else { c.fill(l, y + row, l + bw, y + row + 1, color); c.fill(rt - bw, y + row, rt, y + row + 1, color); }
            }
            return;
        }
        for (int row = 0; row < r; row++) {
            int in = inset(r, row);
            int l = x + in, rt = x + w - in, by = y + h - 1 - row;
            if (row < bw) { c.fill(l, y + row, rt, y + row + 1, color); c.fill(l, by, rt, by + 1, color); }
            else {
                c.fill(l, y + row, l + bw, y + row + 1, color); c.fill(rt - bw, y + row, rt, y + row + 1, color);
                c.fill(l, by, l + bw, by + 1, color); c.fill(rt - bw, by, rt, by + 1, color);
            }
        }
        c.fill(x, y + r, x + bw, y + h - r, color);
        c.fill(x + w - bw, y + r, x + w, y + h - r, color);
    }

    // ---- HSV helper untuk color picker ----
    public static int hsv(float h, float s, float v, float a) {
        int rgb = java.awt.Color.HSBtoRGB(h, s, v) & 0xFFFFFF;
        return (Math.round(Math.max(0f, Math.min(1f, a)) * 255f) << 24) | rgb;
    }
    public static float[] toHsv(int argb) {
        float[] f = java.awt.Color.RGBtoHSB((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, null);
        return new float[]{f[0], f[1], f[2], ((argb >>> 24) & 255) / 255f};
    }
}
