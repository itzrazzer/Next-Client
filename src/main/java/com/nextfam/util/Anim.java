package com.nextfam.util;

import com.nextfam.config.NfConfig;

/** Nilai animasi halus (exponential smoothing, berbasis waktu). */
public class Anim {
    public float v, t;
    private long last = 0;

    public Anim(float x) { v = t = x; }
    public void to(float x) { t = x; }
    public void set(float x) { v = t = x; }

    public float get() {
        long n = System.nanoTime();
        float dt = last == 0 ? 0.016f : Math.min(0.1f, (n - last) / 1e9f);
        last = n;
        if (!NfConfig.anim()) { v = t; return v; }
        v += (t - v) * (1f - (float) Math.exp(-dt * 12f * NfConfig.speed()));
        return v;
    }
}
