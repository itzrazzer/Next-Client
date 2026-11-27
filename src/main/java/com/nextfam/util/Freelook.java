package com.nextfam.util;

/** Status Freelook / Snaplook (dipakai mixin Camera dan Entity). */
public final class Freelook {
    private Freelook() {}
    public static int mode = 0;          // 0 mati, 1 freelook, 2 snaplook (lihat ke belakang)
    public static float yaw, pitch;

    public static float yaw(float original) { return mode == 1 ? yaw : mode == 2 ? original + 180f : original; }
    public static float pitch(float original) { return mode == 1 ? pitch : original; }
}
