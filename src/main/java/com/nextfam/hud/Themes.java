package com.nextfam.hud;

import com.nextfam.config.NfConfig;
import java.util.*;
import java.util.function.Consumer;

public final class Themes {
    public static final LinkedHashMap<String, Consumer<HudElement>> ALL = new LinkedHashMap<>();

    private static void base(HudElement e) {
        e.setB("bg", true); e.setC("bgColor", 0xAA16161C); e.setB("gradient", false);
        e.setB("border", false); e.setF("borderW", 1f); e.setF("radius", 3f); e.setF("pad", 4f);
        e.setB("glow", false); e.setB("shadow", false); e.setC("textColor", 0xFFFFFFFF); e.setB("textShadow", true);
    }

    static {
        ALL.put("NextFam", Themes::base);
        ALL.put("Lunar Inspired", e -> { base(e); e.setC("bgColor", 0x80000000); e.setF("radius", 2f); });
        ALL.put("Minimal", e -> { base(e); e.setB("bg", false); });
        ALL.put("Modern", e -> { base(e); e.setC("bgColor", 0xCC111118); e.setB("border", true); e.setC("borderColor", 0xFFA855F7); e.setF("radius", 5f); e.setF("pad", 5f); });
        ALL.put("Compact", e -> { base(e); e.setC("bgColor", 0x99000000); e.setF("pad", 1f); e.setF("radius", 0f); });
        ALL.put("Glass", e -> { base(e); e.setC("bgColor", 0x44FFFFFF); e.setB("border", true); e.setC("borderColor", 0x88FFFFFF); e.setF("radius", 6f); });
        ALL.put("Neon", e -> { base(e); e.setC("bgColor", 0xCC0A0A12); e.setB("border", true); e.setC("borderColor", 0xFFC084FC); e.setB("glow", true); e.setC("textColor", 0xFFE9D5FF); e.setF("radius", 4f); });
        ALL.put("Classic", e -> { base(e); e.setC("bgColor", 0x80000000); e.setF("radius", 0f); e.setF("pad", 3f); });
    }

    public static void apply(String name) {
        Consumer<HudElement> c = ALL.get(name);
        if (c == null) return;
        for (HudElement e : HudManager.ELEMENTS) {
            if (e.id.equals("crosshair") || e.id.equals("keystrokes")) {
                if (e.id.equals("keystrokes")) { c.accept(e); e.setB("bg", false); } // tombol punya latar sendiri
                continue;
            }
            c.accept(e);
        }
        NfConfig.theme = name;
        HudManager.saveAll();
    }
}
