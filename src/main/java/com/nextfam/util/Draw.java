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
        if (r == 0 && !grad) { c.fill(x, y, x + w, y + h, c1); return; }
        if (!grad) {
            for (int i = 0; i < r; i++) {
                int in = inset(r, i);
                c.fill(x + in, y + i, x + w - in, y + i + 1, c1);
                c.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, c1);
            }
            c.fill(x, y + r, x + w, y + h - r, c1);
            return;
        }
        for (int row = 0; row < h; row++) {
            int in = inset(r, Math.min(row, h - 1 - row));
            c.fill(x + in, y + row, x + w - in, y + row + 1, lerp(c1, c2, h <= 1 ? 0 : row / (float) (h - 1)));
        }
    }

    public static void rborder(DrawContext c, int x, int y, int w, int h, int r, int bw, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int in = inset(r, Math.min(row, h - 1 - row));
            int l = x + in, rt = x + w - in;
            if (row < bw || row >= h - bw) c.fill(l, y + row, rt, y + row + 1, color);
            else {
                c.fill(l, y + row, l + bw, y + row + 1, color);
                c.fill(rt - bw, y + row, rt, y + row + 1, color);
            }
        }
    }
}
