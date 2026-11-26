package com.nextfam.gui;

import com.nextfam.config.NfConfig;
import com.nextfam.config.Setting;
import com.nextfam.hud.HudElement;
import com.nextfam.hud.HudManager;
import com.nextfam.hud.Themes;
import com.nextfam.util.Anim;
import com.nextfam.util.Draw;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.*;

public class MenuScreen extends Screen {
    private static final String[] PAGES = {"Home", "HUD", "Player", "Combat", "Visual", "Animations", "Themes", "Settings"};
    private static final String[] DESC = {
        "Welcome to NextFam Client 2.0", "Information modules", "Player modules", "Combat modules", "Visual modules",
        "Global animation settings", "HUD appearance presets", "Profiles, import / export and reset"};
    private static final int PURPLE = 0xFFA855F7, VIOLET = 0xFF7C3AED, SIDE = 96;

    private record Btn(String label, boolean active, Runnable run) {}

    private int page = 0;
    private HudElement cfg;
    private float scroll = 0, total = 0;
    private TextFieldWidget search;
    private Setting drag; private int dragX1, dragX2;
    private String status = "";
    private final Anim open = new Anim(0), pageAnim = new Anim(1);
    private final Map<String, Anim> anims = new HashMap<>();
    private float o = 1, pg = 1;
    private int px, py, pw, ph, mainX, mainW, cTop, cBot;

    public MenuScreen() { super(Text.literal("NextFam Client")); }

    @Override
    protected void init() {
        String old = search == null ? "" : search.getText();
        search = new TextFieldWidget(textRenderer, 0, 0, 110, 16, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search..."));
        search.setText(old);
        addDrawableChild(search);
    }

    private Anim an(String k, float init) { return anims.computeIfAbsent(k, z -> new Anim(init)); }
    private int co(int c) { return Draw.a(c, o); }
    private int cp(int c) { return Draw.a(c, o * pg); }
    private boolean inR(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private boolean cardPage() { return page >= 1 && page <= 4 && cfg == null; }

    private void layout() {
        pw = Math.min(width - 20, 560); ph = Math.min(height - 20, 340);
        px = (width - pw) / 2; py = (height - ph) / 2;
        mainX = px + SIDE + 10; mainW = pw - SIDE - 20;
        cTop = py + 44; cBot = py + ph - 8;
    }

    private void setPage(int p) { page = p; cfg = null; scroll = 0; pageAnim.set(0); pageAnim.to(1); status = ""; }

    // ---------------- render ----------------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        layout(); open.to(1f); o = open.get(); pageAnim.to(1f); pg = pageAnim.get();
        ctx.fill(0, 0, width, height, Draw.a(0x99000000, o));
        Draw.rrect(ctx, px, py, pw, ph, 6, co(0xF0141419));
        Draw.rrect(ctx, px, py, SIDE, ph, 6, co(0xFF0F0F14));
        ctx.fill(px + SIDE - 8, py, px + SIDE, py + ph, co(0xFF0F0F14));
        ctx.fill(px + SIDE, py + 6, px + SIDE + 1, py + ph - 6, co(0xFF26262F));

        ctx.drawText(textRenderer, "NEXTFAM", px + 12, py + 10, co(PURPLE), true);
        ctx.drawText(textRenderer, "CLIENT 2.0", px + 12, py + 21, co(0xFF9CA3AF), false);
        for (int i = 0; i < PAGES.length; i++) {
            int y = py + 36 + i * 20;
            boolean sel = i == page, hov = inR(mx, my, px + 6, y, SIDE - 12, 18);
            Anim h = an("p" + i, 0); h.to(sel ? 1f : hov ? 0.5f : 0f); float hv = h.get();
            if (hv > 0.01f) Draw.rrect(ctx, px + 6, y, SIDE - 12, 18, 4, co(Draw.a(0xFF7C3AED, 0.35f * hv)));
            if (sel) ctx.fill(px + 6, y + 3, px + 8, y + 15, co(PURPLE));
            ctx.drawText(textRenderer, PAGES[i], px + 14 + Math.round(hv * 2), y + 5, co(sel ? 0xFFFFFFFF : 0xFFB4B4C0), false);
        }
        boolean eh = inR(mx, my, px + 8, py + ph - 26, SIDE - 16, 18);
        Anim eha = an("edit", 0); eha.to(eh ? 1f : 0f);
        Draw.rrect(ctx, px + 8, py + ph - 26, SIDE - 16, 18, 4, co(Draw.lerp(VIOLET, PURPLE, eha.get())));
        String el = "EDIT HUD";
        ctx.drawText(textRenderer, el, px + 8 + (SIDE - 16 - textRenderer.getWidth(el)) / 2, py + ph - 21, co(0xFFFFFFFF), false);

        String title = cfg != null ? "<  " + cfg.name : PAGES[page];
        String desc = cfg != null ? "Settings for this HUD module" : DESC[page];
        ctx.drawText(textRenderer, title, mainX, py + 10, cp(0xFFFFFFFF), true);
        ctx.drawText(textRenderer, desc, mainX, py + 23, cp(0xFF9CA3AF), false);
        search.visible = cardPage();
        if (search.visible) {
            search.setX(px + pw - 122); search.setY(py + 8);
        }

        content(ctx, mx, my, -1, false);
        if (search.visible) search.render(ctx, mx, my, delta);
        if (!status.isEmpty()) ctx.drawText(textRenderer, status, mainX, py + ph - 18 + 8 - 8, cp(0xFFC084FC), false);
    }

    // ---------------- content (render + klik dalam satu jalur) ----------------
    private void content(DrawContext ctx, int mx, int my, int btn, boolean click) {
        if (click && !inR(mx, my, mainX, cTop, mainW, cBot - cTop)) return;
        if (!click) ctx.enableScissor(mainX, cTop, mainX + mainW, cBot);
        int y = Math.round(cTop - scroll + (1f - pg) * 10f);
        int start = y;
        if (cfg != null) y = rows(ctx, mx, my, btn, click, cfg.settings, y);
        else if (page == 5) y = rows(ctx, mx, my, btn, click, List.of(NfConfig.ANIM, NfConfig.SPEED), y);
        else if (cardPage()) y = cards(ctx, mx, my, btn, click, y);
        else y = buttons(ctx, mx, my, btn, click, y);
        if (!click) { ctx.disableScissor(); total = y - start; }
    }

    private List<HudElement> filtered() {
        String cat = PAGES[page], q = search == null ? "" : search.getText().toLowerCase(Locale.ROOT);
        List<HudElement> l = new ArrayList<>();
        for (HudElement e : HudManager.ELEMENTS)
            if (e.category.equals(cat) && (q.isEmpty() || e.name.toLowerCase(Locale.ROOT).contains(q) || e.desc.toLowerCase(Locale.ROOT).contains(q))) l.add(e);
        return l;
    }

    private int cards(DrawContext ctx, int mx, int my, int btn, boolean click, int y) {
        List<HudElement> l = filtered();
        int cols = Math.max(1, (mainW + 8) / 158), cw = (mainW - (cols - 1) * 8) / cols, ch = 60;
        for (int i = 0; i < l.size(); i++) {
            HudElement e = l.get(i);
            int x = mainX + (i % cols) * (cw + 8), yy = y + (i / cols) * (ch + 8);
            int tx = x + 8, ty = yy + ch - 20, gx = x + cw - 40, gy = yy + ch - 21;
            if (click) {
                if (btn != 0) continue;
                if (inR(mx, my, tx - 2, ty - 2, 34, 16)) { e.enabled = !e.enabled; HudManager.saveAll(); return y; }
                if (inR(mx, my, gx, gy, 32, 16)) { cfg = e; scroll = 0; pageAnim.set(0); return y; }
                continue;
            }
            boolean hov = inR(mx, my, x, yy, cw, ch) && my >= cTop && my < cBot;
            Anim h = an("h" + e.id, 0); h.to(hov ? 1f : 0f); float hv = h.get();
            Draw.rrect(ctx, x, yy, cw, ch, 5, cp(Draw.lerp(0xFF1B1B24, 0xFF282836, hv)));
            if (e.enabled) ctx.fill(x + 8, yy, x + cw - 8, yy + 1, cp(PURPLE));
            ctx.drawText(textRenderer, e.name, x + 8, yy + 8, cp(0xFFFFFFFF), true);
            ctx.drawText(textRenderer, textRenderer.trimToWidth(e.desc, cw - 16), x + 8, yy + 21, cp(0xFF9CA3AF), false);
            Anim t = an("t" + e.id, e.enabled ? 1f : 0f); t.to(e.enabled ? 1f : 0f); float tv = t.get();
            Draw.rrect(ctx, tx, ty, 26, 12, 6, cp(Draw.lerp(0xFF3A3A47, VIOLET, tv)));
            Draw.rrect(ctx, tx + 2 + Math.round(tv * 14), ty + 2, 8, 8, 4, cp(0xFFFFFFFF));
            ctx.drawText(textRenderer, e.enabled ? "ON" : "OFF", tx + 31, ty + 2, cp(e.enabled ? 0xFFC084FC : 0xFF9CA3AF), false);
            boolean gh = inR(mx, my, gx, gy, 32, 16);
            Draw.rrect(ctx, gx, gy, 32, 16, 4, cp(gh ? VIOLET : 0xFF2E2E3D));
            ctx.drawText(textRenderer, "CFG", gx + 16 - textRenderer.getWidth("CFG") / 2, gy + 4, cp(0xFFFFFFFF), false);
        }
        int rows = (l.size() + cols - 1) / cols;
        return y + rows * (ch + 8);
    }

    private int rows(DrawContext ctx, int mx, int my, int btn, boolean click, List<Setting> list, int y) {
        int rh = 22, right = mainX + mainW;
        for (Setting s : list) {
            if (click) {
                if (inR(mx, my, mainX, y, mainW, rh)) {
                    switch (s.type) {
                        case BOOL -> { if (btn == 0) { s.cycle(1); HudManager.saveAll(); } }
                        case ENUM, COLOR -> { if (btn == 0 || btn == 1) { s.cycle(btn == 1 ? -1 : 1); HudManager.saveAll(); } }
                        case NUM -> {
                            if (btn == 0) {
                                dragX1 = right - 150; dragX2 = right - 46; drag = s; slide(mx);
                            }
                        }
                    }
                    return y;
                }
                y += rh; continue;
            }
            boolean hov = inR(mx, my, mainX, y, mainW, rh) && my >= cTop && my < cBot;
            if (hov) Draw.rrect(ctx, mainX, y, mainW, rh - 2, 4, cp(0x22FFFFFF));
            ctx.drawText(textRenderer, s.name, mainX + 6, y + 7, cp(0xFFE5E7EB), false);
            switch (s.type) {
                case BOOL -> {
                    Anim t = an("s" + System.identityHashCode(s), s.b ? 1f : 0f); t.to(s.b ? 1f : 0f); float tv = t.get();
                    Draw.rrect(ctx, right - 36, y + 4, 26, 12, 6, cp(Draw.lerp(0xFF3A3A47, VIOLET, tv)));
                    Draw.rrect(ctx, right - 34 + Math.round(tv * 14), y + 6, 8, 8, 4, cp(0xFFFFFFFF));
                }
                case NUM -> {
                    int x1 = right - 150, x2 = right - 46;
                    float t = (s.f - s.min) / (s.max - s.min);
                    ctx.fill(x1, y + 9, x2, y + 12, cp(0xFF3A3A47));
                    ctx.fill(x1, y + 9, x1 + Math.round((x2 - x1) * t), y + 12, cp(VIOLET));
                    Draw.rrect(ctx, x1 + Math.round((x2 - x1) * t) - 3, y + 6, 6, 9, 2, cp(0xFFFFFFFF));
                    ctx.drawText(textRenderer, s.valueText(), x2 + 8, y + 7, cp(0xFF9CA3AF), false);
                }
                case COLOR -> {
                    Draw.rrect(ctx, right - 110, y + 4, 14, 14, 3, cp(0xFF555566));
                    Draw.rrect(ctx, right - 109, y + 5, 12, 12, 2, cp(s.c | 0xFF000000));
                    Draw.rrect(ctx, right - 109, y + 5, 12, 12, 2, cp(s.c));
                    ctx.drawText(textRenderer, s.valueText(), right - 92, y + 7, cp(0xFF9CA3AF), false);
                }
                case ENUM -> {
                    Draw.rrect(ctx, right - 130, y + 2, 120, 16, 4, cp(hov ? 0xFF33334A : 0xFF2A2A38));
                    String v = s.str();
                    ctx.drawText(textRenderer, v, right - 70 - textRenderer.getWidth(v) / 2, y + 6, cp(0xFFFFFFFF), false);
                }
            }
            y += rh;
        }
        return y;
    }

    private List<Btn> buttonsFor() {
        List<Btn> l = new ArrayList<>();
        if (page == 0) l.add(new Btn("EDIT HUD LAYOUT", true, () -> client.setScreen(new HudEditorScreen(this))));
        else if (page == 6) {
            for (String n : Themes.ALL.keySet())
                l.add(new Btn(n, n.equals(NfConfig.theme), () -> { Themes.apply(n); status = "Theme applied: " + n; }));
        } else if (page == 7) {
            for (String n : HudManager.profiles())
                l.add(new Btn("Profile: " + n, n.equals(NfConfig.profile), () -> { HudManager.loadProfile(n); status = "Loaded profile " + n; }));
            l.add(new Btn("+ New profile (copy of current)", false, () -> status = "Created " + HudManager.newProfile()));
            l.add(new Btn("Export layout to clipboard", false, () -> { client.keyboard.setClipboard(HudManager.exportString()); status = "Layout copied to clipboard"; }));
            l.add(new Btn("Import layout from clipboard", false, () ->
                status = HudManager.importString(client.keyboard.getClipboard()) ? "Layout imported" : "Clipboard has no valid layout"));
            l.add(new Btn("Reset all HUD settings", false, () -> { HudManager.resetAll(); status = "All HUD settings reset"; }));
        }
        return l;
    }

    private int buttons(DrawContext ctx, int mx, int my, int btn, boolean click, int y) {
        if (page == 0) {
            long on = HudManager.ELEMENTS.stream().filter(e -> e.enabled).count();
            String[] lines = {"Profile: " + NfConfig.profile, "Theme: " + NfConfig.theme,
                "Active modules: " + on + " / " + HudManager.ELEMENTS.size(), "Open this menu: Shift + Right Click", ""};
            for (String s : lines) {
                if (!click) ctx.drawText(textRenderer, s, mainX + 2, y, cp(0xFFB4B4C0), false);
                y += 13;
            }
        }
        for (Btn b : buttonsFor()) {
            if (click) {
                if (btn == 0 && inR(mx, my, mainX, y, mainW, 22)) { b.run().run(); HudManager.saveAll(); return y; }
            } else {
                boolean hov = inR(mx, my, mainX, y, mainW, 22) && my >= cTop && my < cBot;
                Anim h = an("b" + b.label(), 0); h.to(hov ? 1f : 0f);
                int base = b.active() ? 0xFF4C2A8C : 0xFF1F1F29;
                Draw.rrect(ctx, mainX, y, mainW, 22, 5, cp(Draw.lerp(base, 0xFF3B2A66, h.get())));
                if (b.active()) ctx.fill(mainX, y + 4, mainX + 2, y + 18, cp(PURPLE));
                ctx.drawText(textRenderer, b.label(), mainX + 10, y + 7, cp(0xFFFFFFFF), false);
            }
            y += 26;
        }
        return y;
    }

    private void slide(double mx) {
        if (drag == null) return;
        float t = (float) ((mx - dragX1) / (double) (dragX2 - dragX1));
        t = Math.max(0f, Math.min(1f, t));
        float v = drag.min + t * (drag.max - drag.min);
        drag.setF(Math.round(v / drag.step) * drag.step);
    }

    // ---------------- input ----------------
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        layout();
        double mx = click.x(), my = click.y(); int btn = click.button();
        if (search.visible && inR(mx, my, search.getX(), search.getY(), 110, 16)) {
            setFocused(search); search.setFocused(true);
            return search.mouseClicked(click, doubled);
        }
        search.setFocused(false); setFocused(null);
        for (int i = 0; i < PAGES.length; i++)
            if (btn == 0 && inR(mx, my, px + 6, py + 36 + i * 20, SIDE - 12, 18)) { setPage(i); return true; }
        if (btn == 0 && inR(mx, my, px + 8, py + ph - 26, SIDE - 16, 18)) { client.setScreen(new HudEditorScreen(this)); return true; }
        if (btn == 0 && cfg != null && inR(mx, my, mainX, py + 6, 120, 14)) { cfg = null; scroll = 0; pageAnim.set(0); return true; }
        content(null, (int) mx, (int) my, btn, true);
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (drag != null) { slide(click.x()); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (drag != null) { drag = null; HudManager.saveAll(); }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        float max = Math.max(0f, total - (cBot - cTop));
        scroll = Math.max(0f, Math.min(max, scroll - (float) v * 20f));
        return true;
    }

    @Override public void removed() { HudManager.saveAll(); }
    @Override public void close() { client.setScreen(null); }
}
