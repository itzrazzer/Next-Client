package com.nextfam.hud;

import com.nextfam.config.Setting;
import com.nextfam.util.Anim;
import com.nextfam.util.Draw;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import java.util.*;

public abstract class HudElement {
    public final String id, name, category, desc;
    public final int defX, defY;
    public int x, y;
    public boolean enabled = true, locked = false;
    public float alphaMul = 1f;
    public final Anim shown = new Anim(0);
    public final List<Setting> settings = new ArrayList<>();

    private Setting add(Setting s) { settings.add(s); return s; }

    public final Setting scale = add(Setting.num("scale", "Scale", 1f, 0.5f, 3f));
    public final Setting opacity = add(Setting.num("opacity", "Opacity", 1f, 0.1f, 1f));
    public final Setting bg = add(Setting.bool("bg", "Background", true));
    public final Setting bgColor = add(Setting.color("bgColor", "Background Color", 0xAA16161C));
    public final Setting gradient = add(Setting.bool("gradient", "Gradient", false));
    public final Setting bgColor2 = add(Setting.color("bgColor2", "Gradient Color", 0xAA7C3AED));
    public final Setting textColor = add(Setting.color("textColor", "Text Color", 0xFFFFFFFF));
    public final Setting textShadow = add(Setting.bool("textShadow", "Text Shadow", true));
    public final Setting align = add(Setting.choice("align", "Alignment", 0, "Left", "Center", "Right"));
    public final Setting pad = add(Setting.num("pad", "Padding", 4f, 0f, 10f).step(1f));
    public final Setting border = add(Setting.bool("border", "Border", false));
    public final Setting borderColor = add(Setting.color("borderColor", "Border Color", 0xFFA855F7));
    public final Setting borderW = add(Setting.num("borderW", "Border Width", 1f, 1f, 4f).step(1f));
    public final Setting radius = add(Setting.num("radius", "Corner Radius", 3f, 0f, 8f).step(1f));
    public final Setting shadow = add(Setting.bool("shadow", "Shadow", false));
    public final Setting glow = add(Setting.bool("glow", "Glow", false));
    public final Setting animStyle = add(Setting.choice("animStyle", "Animation Style", 0, "Fade", "Slide", "Scale", "None"));

    protected HudElement(String id, String name, String category, String desc, int x, int y) {
        this.id = id; this.name = name; this.category = category; this.desc = desc;
        this.x = x; this.y = y; this.defX = x; this.defY = y;
    }

    protected Setting extra(Setting s) { return add(s); }
    protected static TextRenderer tr() { return MinecraftClient.getInstance().textRenderer; }

    /** Ukuran konten (tanpa padding). */
    protected abstract int cw();
    protected abstract int ch();
    protected abstract void content(DrawContext ctx, int x, int y, int w);

    public boolean visible() { return true; }
    public float extraAlpha() { return 1f; }

    public int padI() { return Math.round(pad.f); }
    public int width() { return cw() + 2 * padI(); }
    public int height() { return ch() + 2 * padI(); }
    public int sw() { return Math.round(width() * scale.f); }
    public int sh() { return Math.round(height() * scale.f); }

    protected int a(int c) { return Draw.a(c, opacity.f * alphaMul); }

    public void render(DrawContext ctx) {
        int w = width(), h = height(), r = Math.round(radius.f), p = padI();
        if (shadow.b) Draw.rrect(ctx, 2, 2, w, h, r, a(0x66000000));
        if (glow.b) for (int i = 3; i >= 1; i--) Draw.rrect(ctx, -i, -i, w + 2 * i, h + 2 * i, r + i, a(Draw.a(borderColor.c, 0.12f)));
        if (bg.b) {
            if (gradient.b) Draw.rrectGrad(ctx, 0, 0, w, h, r, a(bgColor.c), a(bgColor2.c));
            else Draw.rrect(ctx, 0, 0, w, h, r, a(bgColor.c));
        }
        if (border.b) Draw.rborder(ctx, 0, 0, w, h, r, Math.round(borderW.f), a(borderColor.c));
        content(ctx, p, p, cw());
    }

    protected void textC(DrawContext ctx, String s, int x, int y, int color) {
        ctx.drawText(tr(), s, x, y, a(color), textShadow.b);
    }

    /** Teks mengikuti pengaturan Alignment di dalam lebar konten w. */
    protected void textA(DrawContext ctx, String s, int x, int y, int w, int color) {
        int tw = tr().getWidth(s);
        int dx = align.idx == 0 ? 0 : align.idx == 1 ? (w - tw) / 2 : w - tw;
        textC(ctx, s, x + dx, y, color);
    }

    public Setting s(String key) {
        for (Setting s : settings) if (s.key.equals(key)) return s;
        return null;
    }
    public void setB(String k, boolean v) { Setting s = s(k); if (s != null) s.b = v; }
    public void setF(String k, float v) { Setting s = s(k); if (s != null) s.setF(v); }
    public void setC(String k, int v) { Setting s = s(k); if (s != null) s.c = v; }

    public void resetPos() { x = defX; y = defY; scale.reset(); opacity.reset(); locked = false; }
    public void resetAll() { resetPos(); for (Setting s : settings) s.reset(); enabled = true; }
}
