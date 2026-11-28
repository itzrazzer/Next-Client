package com.nextfam.pack;

import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2f;

/** Preview 3D tanpa API model Minecraft: kubus isometrik untuk blok, sprite bertebal untuk item. Piksel digambar dengan fill + transformasi affine. */
public final class Preview3D {
    private Preview3D() {}

    private static final double PITCH = Math.toRadians(28);

    private static int shade(int argb, float f) {
        int r = Math.min(255, Math.round(((argb >> 16) & 255) * f)), g = Math.min(255, Math.round(((argb >> 8) & 255) * f)), b = Math.min(255, Math.round((argb & 255) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Titik dunia -> layar (sx, sy), yaw di sekeliling sumbu Y lalu pitch tetap. */
    private static double[] proj(double x, double y, double z, double yaw, double pitch) {
        double x1 = x * Math.cos(yaw) + z * Math.sin(yaw), z1 = -x * Math.sin(yaw) + z * Math.cos(yaw);
        double y2 = y * Math.cos(pitch) - z1 * Math.sin(pitch);
        return new double[]{x1, -y2};
    }

    private static double depthNormal(double nx, double ny, double nz, double yaw, double pitch) {
        double nz1 = -nx * Math.sin(yaw) + nz * Math.cos(yaw);
        return ny * Math.sin(pitch) + nz1 * Math.cos(pitch);
    }

    /** Satu sisi: tekstur dipetakan dengan matriks affine (P0 = pojok kiri-atas, U ke kanan, V ke bawah). */
    private static void face(DrawContext ctx, PackPreview.Img img, double[] p0, double[] u, double[] v, double yaw, double pitch,
                             float cx, float cy, float S, float shade) {
        if (img == null) return;
        double[] a = proj(p0[0], p0[1], p0[2], yaw, pitch);
        double[] b = proj(p0[0] + u[0], p0[1] + u[1], p0[2] + u[2], yaw, pitch);
        double[] c = proj(p0[0] + v[0], p0[1] + v[1], p0[2] + v[2], yaw, pitch);
        float K = 16f;                                        // 1 piksel tekstur = 16 unit lokal (untuk overlap anti-celah)
        float m00 = (float) ((b[0] - a[0]) * S / (img.w() * K)), m01 = (float) ((b[1] - a[1]) * S / (img.w() * K));
        float m10 = (float) ((c[0] - a[0]) * S / (img.h() * K)), m11 = (float) ((c[1] - a[1]) * S / (img.h() * K));
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.mul(new Matrix3x2f(m00, m01, m10, m11, cx + (float) a[0] * S, cy + (float) a[1] * S));
        for (int j = 0; j < img.h(); j++) {
            int i = 0;
            while (i < img.w()) {
                int col = img.argb()[j * img.w() + i];
                if ((col >>> 24) < 8) { i++; continue; }
                int k = i + 1;
                while (k < img.w() && img.argb()[j * img.w() + k] == col) k++;
                ctx.fill(Math.round(i * K), Math.round(j * K), Math.round(k * K) + 1, Math.round((j + 1) * K) + 1, shade(col, shade));
                i = k;
            }
        }
        m.popMatrix();
    }

    /** Kubus: top / sisi samping / depan (sisi +Z). bottom tidak terlihat. */
    public static void cube(DrawContext ctx, PackPreview.Img top, PackPreview.Img side, PackPreview.Img front, float cx, float cy, float size, double yaw) {
        float S = size * 0.62f;
        double[][][] faces = {
            // P0, U, V, normal
            {{-0.5, 0.5, 0.5}, {1, 0, 0}, {0, -1, 0}, {0, 0, 1}},     // +Z (depan)
            {{0.5, 0.5, 0.5}, {0, 0, -1}, {0, -1, 0}, {1, 0, 0}},     // +X
            {{0.5, 0.5, -0.5}, {-1, 0, 0}, {0, -1, 0}, {0, 0, -1}},   // -Z
            {{-0.5, 0.5, -0.5}, {0, 0, 1}, {0, -1, 0}, {-1, 0, 0}},   // -X
        };
        float[] shades = {0.85f, 0.65f, 0.85f, 0.65f};
        for (int i = 0; i < 4; i++) {
            double[][] f = faces[i];
            if (depthNormal(f[3][0], f[3][1], f[3][2], yaw, PITCH) <= 0.02) continue;
            // arahkan bayangan berdasarkan seberapa menghadap kamera
            double facing = depthNormal(f[3][0], f[3][1], f[3][2], yaw, PITCH);
            float sh = (float) (0.55 + 0.45 * Math.min(1.0, facing / Math.cos(PITCH)));
            face(ctx, i == 0 && front != null ? front : side, f[0], f[1], f[2], yaw, PITCH, cx, cy, S, Math.min(sh, shades[i] + 0.15f));
        }
        face(ctx, top, new double[]{-0.5, 0.5, -0.5}, new double[]{1, 0, 0}, new double[]{0, 0, 1}, yaw, PITCH, cx, cy, S, 1.0f);
    }

    /** Sprite item dengan ketebalan (lapisan) yang berputar di sumbu Y. */
    public static void extrudedSprite(DrawContext ctx, PackPreview.Img img, float cx, float cy, float size, double yaw) {
        if (img == null) return;
        float S = size * 0.85f;
        double pitch = Math.toRadians(14);
        int layers = 8; double thick = 1.0 / 14.0;
        boolean frontFirst = Math.cos(yaw) < 0;                  // urutan painter: belakang -> depan
        for (int n = 0; n < layers; n++) {
            int k = frontFirst ? layers - 1 - n : n;
            double z = -thick / 2 + thick * k / (layers - 1);
            boolean nearest = frontFirst ? k == 0 : k == layers - 1;
            float sh = nearest ? 1.0f : 0.55f;
            face(ctx, img, new double[]{-0.5, 0.5, z}, new double[]{1, 0, 0}, new double[]{0, -1, 0}, yaw, pitch, cx, cy, S, sh);
        }
    }

    /** Sprite datar sedikit miring. */
    public static void flat(DrawContext ctx, PackPreview.Img img, float cx, float cy, float size) {
        if (img == null) return;
        float S = size * 0.95f;
        face(ctx, img, new double[]{-0.5, 0.5, 0}, new double[]{1, 0, 0}, new double[]{0, -1, 0}, 0.0, 0.0, cx, cy, S, 1.0f);
    }
}
