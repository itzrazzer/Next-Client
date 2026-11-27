package com.nextfam.pack;

import net.minecraft.client.gui.DrawContext;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/** Preview PNG tanpa API tekstur Minecraft: piksel digambar dengan fill (run-merge). */
public final class PackPreview {
    private PackPreview() {}
    public record Img(int w, int h, int[] argb) {}

    private static final Map<String, Img> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Img> e) { return size() > 48; }
    };
    private static final Img NONE = new Img(0, 0, new int[0]);

    public static synchronized Img load(PackSource ps, String path) {
        String key = ps.path + "|" + path;
        Img c = CACHE.get(key);
        if (c != null) return c == NONE ? null : c;
        Img r = NONE;
        if (path.toLowerCase().endsWith(".png")) {
            try {
                byte[] b = ps.read(path);
                if (b.length <= 8 * 1024 * 1024) {
                    BufferedImage im = ImageIO.read(new ByteArrayInputStream(b));
                    if (im != null) r = down(im);
                }
            } catch (Throwable ignored) {}
        }
        CACHE.put(key, r);
        return r == NONE ? null : r;
    }

    private static Img down(BufferedImage im) {
        int w = im.getWidth(), h = im.getHeight();
        if (h > w * 2) h = w;                         // strip animasi: ambil frame pertama
        int f = Math.max(1, (int) Math.ceil(Math.max(w, h) / 32.0));
        int ow = Math.max(1, w / f), oh = Math.max(1, h / f);
        int[] out = new int[ow * oh];
        for (int y = 0; y < oh; y++) for (int x = 0; x < ow; x++) {
            long a = 0, r = 0, g = 0, bl = 0; int n = 0;
            for (int dy = 0; dy < f; dy++) for (int dx = 0; dx < f; dx++) {
                int px = im.getRGB(Math.min(w - 1, x * f + dx), Math.min(h - 1, y * f + dy));
                int al = px >>> 24; a += al; r += ((px >> 16) & 255) * al; g += ((px >> 8) & 255) * al; bl += (px & 255) * al; n++;
            }
            if (a == 0) { out[y * ow + x] = 0; continue; }
            out[y * ow + x] = ((int) (a / n) << 24) | ((int) (r / a) << 16) | ((int) (g / a) << 8) | (int) (bl / a);
        }
        return new Img(ow, oh, out);
    }

    public static void draw(DrawContext ctx, Img img, int x, int y, int size) {
        ctx.fill(x, y, x + size, y + size, 0xFF1A1A1A);
        if (img == null) return;
        float sc = size / (float) Math.max(img.w(), img.h());
        var m = ctx.getMatrices();
        m.pushMatrix(); m.translate((float) x, (float) y); m.scale(sc, sc);
        for (int j = 0; j < img.h(); j++) {
            int i = 0;
            while (i < img.w()) {
                int c = img.argb()[j * img.w() + i];
                if ((c >>> 24) < 8) { i++; continue; }
                int k = i + 1;
                while (k < img.w() && img.argb()[j * img.w() + k] == c) k++;
                ctx.fill(i, j, k, j + 1, c | 0xFF000000);
                i = k;
            }
        }
        m.popMatrix();
    }
}
