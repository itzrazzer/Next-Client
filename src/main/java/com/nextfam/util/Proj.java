package com.nextfam.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Proyeksi world -> layar (kamera orang pertama) untuk overlay 2D: block outline, TNT timer. */
public final class Proj {
    private Proj() {}
    public static boolean ready;
    private static double cx, cy, cz, fx, fy, fz, rx, ry, rz, ux, uy, uz, k;
    private static int W, H;

    public static void setup(MinecraftClient mc, float tp, int w, int h) {
        ready = false;
        var p = mc.player;
        if (p == null || !mc.options.getPerspective().isFirstPerson()) return;
        Vec3d pos = p.getLerpedPos(tp);
        cx = pos.x; cy = pos.y + p.getEyeHeight(p.getPose()); cz = pos.z;
        double yaw = Math.toRadians(p.getYaw(tp)), pit = Math.toRadians(p.getPitch(tp));
        fx = -Math.sin(yaw) * Math.cos(pit); fy = -Math.sin(pit); fz = Math.cos(yaw) * Math.cos(pit);
        rx = -Math.cos(yaw); ry = 0; rz = -Math.sin(yaw);
        ux = ry * fz - rz * fy; uy = rz * fx - rx * fz; uz = rx * fy - ry * fx;
        double fov = Math.toRadians(mc.options.getFov().getValue());
        k = (h / 2.0) / Math.tan(fov / 2.0);
        W = w; H = h; ready = true;
    }

    /** koordinat kamera: {kanan, atas, depan} */
    public static double[] cam(double x, double y, double z) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        return new double[]{dx * rx + dy * ry + dz * rz, dx * ux + dy * uy + dz * uz, dx * fx + dy * fy + dz * fz};
    }
    public static int sx(double[] c) { return (int) Math.round(W / 2.0 + c[0] / c[2] * k); }
    public static int sy(double[] c) { return (int) Math.round(H / 2.0 - c[1] / c[2] * k); }

    public static void line(DrawContext ctx, double[] a, double[] b, int color, int th) {
        double near = 0.05;
        if (a[2] < near && b[2] < near) return;
        if (a[2] < near) { double t = (near - a[2]) / (b[2] - a[2]); a = new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, near}; }
        else if (b[2] < near) { double t = (near - b[2]) / (a[2] - b[2]); b = new double[]{b[0] + (a[0] - b[0]) * t, b[1] + (a[1] - b[1]) * t, near}; }
        int x0 = sx(a), y0 = sy(a), x1 = sx(b), y1 = sy(b);
        int dx = x1 - x0, dy = y1 - y0, n = Math.max(Math.abs(dx), Math.abs(dy));
        if (n > 5000) return;
        if (n == 0) { ctx.fill(x0, y0, x0 + th, y0 + th, color); return; }
        int step = Math.max(1, th);
        for (int i = 0; i <= n; i += step) {
            int x = x0 + (int) ((long) dx * i / n), y = y0 + (int) ((long) dy * i / n);
            ctx.fill(x, y, x + th, y + th, color);
        }
    }

    public static double[] camPos() { return new double[]{cx, cy, cz}; }
    public static double[] dir() { return new double[]{fx, fy, fz}; }

    private static final int[][] CORNERS = {{0,0,0},{1,0,0},{1,0,1},{0,0,1},{0,1,0},{1,1,0},{1,1,1},{0,1,1}};
    private static final int[][] EDGES = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};

    public static void box(DrawContext ctx, Box b, int color, int th) {
        double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
        double[][] c = new double[8][];
        for (int i = 0; i < 8; i++) c[i] = cam(xs[CORNERS[i][0]], ys[CORNERS[i][1]], zs[CORNERS[i][2]]);
        for (int[] e : EDGES) line(ctx, c[e[0]], c[e[1]], color, th);
    }

    public static void line3(DrawContext ctx, double x0, double y0, double z0, double x1, double y1, double z1, int color, int th) {
        line(ctx, cam(x0, y0, z0), cam(x1, y1, z1), color, th);
    }
}
