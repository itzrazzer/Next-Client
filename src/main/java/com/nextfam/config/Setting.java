package com.nextfam.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.Locale;

public class Setting {
    public enum T { BOOL, NUM, COLOR, ENUM }

    public static final int[] PALETTE = {
        0xFFFFFFFF, 0xFFA855F7, 0xFF7C3AED, 0xFFC084FC, 0xFF60A5FA, 0xFF22D3EE, 0xFF4ADE80, 0xFFFACC15,
        0xFFFB923C, 0xFFF87171, 0xFFF472B6, 0xFF9CA3AF, 0xFF000000, 0xAA16161C, 0x66000000, 0x44FFFFFF,
        0xCC0A0A12, 0xAA7C3AED
    };

    public final T type;
    public final String key, name;
    public boolean b;
    public float f, min, max, step = 0.05f;
    public int c, idx;
    public String[] opts;
    private boolean db; private float df; private int dc, di;

    private Setting(T t, String k, String n) { type = t; key = k; name = n; }

    public static Setting bool(String k, String n, boolean v) { Setting s = new Setting(T.BOOL, k, n); s.b = v; return s.snap(); }
    public static Setting num(String k, String n, float v, float min, float max) {
        Setting s = new Setting(T.NUM, k, n); s.f = v; s.min = min; s.max = max; return s.snap();
    }
    public static Setting color(String k, String n, int v) { Setting s = new Setting(T.COLOR, k, n); s.c = v; return s.snap(); }
    public static Setting choice(String k, String n, int idx, String... opts) {
        Setting s = new Setting(T.ENUM, k, n); s.idx = idx; s.opts = opts; return s.snap();
    }

    public Setting step(float s) { step = s; return this; }
    private Setting snap() { db = b; df = f; dc = c; di = idx; return this; }
    public Setting def(boolean v) { b = v; return snap(); }
    public Setting def(float v) { f = v; return snap(); }
    public void reset() { b = db; f = df; c = dc; idx = di; }
    public void setF(float v) { f = Math.max(min, Math.min(max, v)); }
    public String str() { return opts[idx]; }

    public void cycle(int dir) {
        switch (type) {
            case BOOL -> b = !b;
            case ENUM -> idx = (idx + dir + opts.length) % opts.length;
            case COLOR -> {
                int i = -1;
                for (int k = 0; k < PALETTE.length; k++) if (PALETTE[k] == c) { i = k; break; }
                i = (i + dir + PALETTE.length) % PALETTE.length;
                c = PALETTE[i];
            }
            default -> {}
        }
    }

    public JsonElement toJson() {
        return switch (type) {
            case BOOL -> new JsonPrimitive(b);
            case NUM -> new JsonPrimitive(f);
            case COLOR -> new JsonPrimitive(c);
            case ENUM -> new JsonPrimitive(idx);
        };
    }

    public void fromJson(JsonElement e) {
        try {
            switch (type) {
                case BOOL -> b = e.getAsBoolean();
                case NUM -> setF(e.getAsFloat());
                case COLOR -> c = e.getAsInt();
                case ENUM -> idx = Math.max(0, Math.min(opts.length - 1, e.getAsInt()));
            }
        } catch (Exception ignored) {}
    }

    public String valueText() {
        return switch (type) {
            case BOOL -> b ? "ON" : "OFF";
            case NUM -> step >= 1f ? String.valueOf(Math.round(f)) : String.format(Locale.ROOT, "%.2f", f);
            case COLOR -> String.format(Locale.ROOT, "#%08X", c);
            case ENUM -> str();
        };
    }
}
