package com.nextfam.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Proyeksi world -> layar untuk overlay 2D. Garis digambar sebagai SATU quad yang diputar (hemat FPS). */
public final class Proj {
    private Proj() {}
    public static boolean ready;       // orang pertama (block outline, TNT timer, dll.)
    public static boolean readyAny;    // termasuk third person (wings)
    private static double cx, cy, cz, fx, fy, fz, rx, ry, rz, ux, uy, uz, k;
    private static int W, H;

    public static void setup(MinecraftClient mc, float tp, int w, int h) {
        ready = false; readyAny = false;
        var p = mc.player;
        if (p == null) return;
        Perspective per = mc.options.getPerspective();
        boolean fp = per.isFirstPerson();
        Vec3d pos = p.getLerpedPos(tp);
        double eyeY = pos.y + p.getEyeHeight(p.getPose());
        double yaw = Math.toRadians(p.getYaw(tp)), pit = Math.toRadians(p.getPitch(tp));
        cx = pos.x; cy = eyeY; cz = pos.z;
        double cyaw = yaw, cpit = pit;
        if (!fp) {
            double d = 4.0, s = per == Perspective.THIRD_PERSON_FRONT ? 1 : -1;
            double f0x = -Math.sin(yaw) * Math.cos(pit), f0y = -Math.sin(pit), f0z = Math.cos(yaw) * Math.cos(pit);
            cx += f0x * d * s; cy += f0y * d * s; cz += f0z * d * s;
            if (s > 0) { cyaw = yaw + Math.PI; cpit = -pit; }
        }
        fx = -Math.sin(cyaw) * Math.cos(cpit); fy = -Math.sin(cpit); fz = Math.cos(cyaw) * Math.cos(cpit);
        rx = -Math.cos(cyaw); ry = 0; rz = -Math.sin(cyaw);
        ux = ry * fz - rz * fy; uy = rz * fx - rx * fz; uz = rx * fy - ry * fx;
        double fov = Math.toRadians(mc.options.getFov().getValue());
        k = (h / 2.0) / Math.tan(fov / 2.0);
        W = w; H = h; ready = fp; readyAny = true;
    }

    /** koordinat kamera: {kanan, atas, depan} */
    public static double[] cam(double x, double y, double z) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        return new double[]{dx * rx + dy * ry + dz * rz, dx * ux + dy * uy + dz * uz, dx * fx + dy * fy + dz * fz};
    }
    public static int sx(double[] c) { return (int) Math.round(W / 2.0 + c[0] / c[2] * k); }
    public static int sy(double[] c) { return (int) Math.round(H / 2.0 - c[1] / c[2] * k); }
    public static double[] camPos() { return new double[]{cx, cy, cz}; }
    public static double[] dir() { return new double[]{fx, fy, fz}; }

    public static void line(DrawContext ctx, double[] a, double[] b, int color, int th) {
        double near = 0.05;
        if (a[2] < near && b[2] < near) return;
        if (a[2] < near) { double t = (near - a[2]) / (b[2] - a[2]); a = new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, near}; }
        else if (b[2] < near) { double t = (near - b[2]) / (a[2] - b[2]); b = new double[]{b[0] + (a[0] - b[0]) * t, b[1] + (a[1] - b[1]) * t, near}; }
        double x0 = W / 2.0 + a[0] / a[2] * k, y0 = H / 2.0 - a[1] / a[2] * k;
        double x1 = W / 2.0 + b[0] / b[2] * k, y1 = H / 2.0 - b[1] / b[2] * k;
        if (Math.abs(x0) > 1e5 || Math.abs(y0) > 1e5 || Math.abs(x1) > 1e5 || Math.abs(y1) > 1e5) return;
        if ((x0 < 0 && x1 < 0) || (x0 > W && x1 > W) || (y0 < 0 && y1 < 0) || (y0 > H && y1 > H)) return;
        float dx = (float) (x1 - x0), dy = (float) (y1 - y0);
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.5f) { int X = (int) Math.round(x0), Y = (int) Math.round(y0); ctx.fill(X, Y, X + th, Y + th, color); return; }
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) x0, (float) y0);
        m.rotate((float) Math.atan2(dy, dx));
        ctx.fill(0, -(th / 2), Math.round(len), th - th / 2, color);
        m.popMatrix();
    }

    private static final int[][] CORNERS = {{0,0,0},{1,0,0},{1,0,1},{0,0,1},{0,1,0},{1,1,0},{1,1,1},{0,1,1}};
    private static final int[][] EDGES = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};

    public static void box(DrawContext ctx, Box b, int color, int th) {
        double[] xs = {b.minX, b.maxX}, ys = {b.minY, b.maxY}, zs = {b.minZ, b.maxZ};
        double[][] c = new double[8][];
        boolean any = false;
        for (int i = 0; i < 8; i++) { c[i] = cam(xs[CORNERS[i][0]], ys[CORNERS[i][1]], zs[CORNERS[i][2]]); if (c[i][2] > 0.05) any = true; }
        if (!any) return;                     // seluruh kotak di belakang kamera
        for (int[] e : EDGES) line(ctx, c[e[0]], c[e[1]], color, th);
    }

    public static void line3(DrawContext ctx, double x0, double y0, double z0, double x1, double y1, double z1, int color, int th) {
        line(ctx, cam(x0, y0, z0), cam(x1, y1, z1), color, th);
    }
}
