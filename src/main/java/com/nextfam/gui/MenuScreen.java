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

/** Menu bergaya client premium: panel transparan, tab atas, profil di kiri, grid kartu modul. */
public class MenuScreen extends Screen {
    private static final String[] TABS = {"MODS", "THEMES", "SETTINGS"};
    private static final String[] CHIPS = {"ALL", "HUD", "PLAYER", "COMBAT", "VISUAL", "INV", "OPT", "TWEAKS"};
    private static final String[] CAT = {"ALL", "HUD", "Player", "Combat", "Visual", "Inventory", "Optimize", "Tweaks"};
    private static final int PURPLE = 0xFFA855F7, VIOLET = 0xFF7C3AED, GREEN = 0xFF2FA866, SW = 100;

    private record Btn(String label, boolean active, Runnable run) {}

    private int page = 0;
    private String filter = "ALL";
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
        search = new TextFieldWidget(textRenderer, 0, 0, 100, 14, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search..."));
        search.setText(old);
        addDrawableChild(search);
    }

    private Anim an(String k, float init) { return anims.computeIfAbsent(k, z -> new Anim(init)); }
    private int co(int c) { return Draw.a(c, o); }
    private int cp(int c) { return Draw.a(c, o * pg); }
    private boolean inR(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private boolean modsTab() { return page == 0; }
    private boolean cardView() { return modsTab() && cfg == null; }

    private void layout() {
        pw = Math.min(width - 20, 520); ph = Math.min(height - 20, 320);
        px = (width - pw) / 2; py = (height - ph) / 2;
        mainX = px + SW + 16; mainW = pw - SW - 24;
        cTop = modsTab() ? (cfg == null ? py + 76 : py + 58) : py + 38;
        cBot = py + ph - 16;
    }

    private void setPage(int p) { page = p; cfg = null; scroll = 0; pageAnim.set(0); status = ""; }
    private int tabW() { return Math.max(36, Math.min(58, (pw - 30 - 175) / 3)); }
    private int tabX(int i) { return px + pw - 30 - (3 * tabW() + 8) + i * (tabW() + 4); }
    private int chipX(int i) { int x = mainX; for (int k = 0; k < i; k++) x += textRenderer.getWidth(CHIPS[k]) + 12; return x; }
    private int chipW(int i) { return textRenderer.getWidth(CHIPS[i]) + 8; }

    // ---------------- render ----------------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        layout(); open.to(1f); o = open.get(); pageAnim.to(1f); pg = pageAnim.get();
        ctx.fill(0, 0, width, height, Draw.a(0x55000000, o));

        // panel transparan
        Draw.rrect(ctx, px, py, pw, ph, 6, co(0xB0121216));
        Draw.rrect(ctx, px, py, pw, 30, 6, co(0xC8000000));
        ctx.fill(px, py + 24, px + pw, py + 30, co(0xC8000000));
        ctx.fill(px, py + 30, px + pw, py + 31, co(0x99A855F7));

        // judul kiri atas
        com.nextfam.util.Logo.draw(ctx, px + 6, py + 3, 24, o);
        ctx.drawText(textRenderer, "NEXTFAM CLIENT - RAZZER", px + 34, py + 11, co(0xFFFFFFFF), true);

        // tab atas
        for (int i = 0; i < TABS.length; i++) {
            boolean sel = i == page, hov = inR(mx, my, tabX(i), py + 7, tabW(), 16);
            Anim h = an("tab" + i, 0); h.to(sel ? 1f : hov ? 0.5f : 0f);
            Draw.rrect(ctx, tabX(i), py + 7, tabW(), 16, 3, co(Draw.lerp(0x66222228, 0xCC3B2A66, h.get())));
            ctx.drawText(textRenderer, TABS[i], tabX(i) + (tabW() - textRenderer.getWidth(TABS[i])) / 2, py + 12, co(sel ? 0xFFFFFFFF : 0xFFB4B4C0), false);
        }
        boolean xh = inR(mx, my, px + pw - 24, py + 7, 16, 16);
        Draw.rrect(ctx, px + pw - 24, py + 7, 16, 16, 3, co(xh ? 0xFFB83A3A : 0x66222228));
        ctx.drawText(textRenderer, "X", px + pw - 19, py + 12, co(0xFFFFFFFF), false);

        sidebar(ctx, mx, my);

        // baris filter (chips + search) atau tombol back
        if (cardView()) {
            for (int i = 0; i < CHIPS.length; i++) {
                boolean sel = CAT[i].equals(filter), hov = inR(mx, my, chipX(i), py + 38, chipW(i), 14);
                Draw.rrect(ctx, chipX(i), py + 38, chipW(i), 14, 3, co(sel ? VIOLET : hov ? 0xFF3A4A6B : 0xFF2A3550));
                ctx.drawText(textRenderer, CHIPS[i], chipX(i) + 4, py + 42, co(0xFFFFFFFF), false);
            }
            search.visible = true;
            search.setX(mainX); search.setY(py + 56); search.setWidth(mainW);
        } else {
            search.visible = false;
            if (cfg != null) {
                boolean bh = inR(mx, my, mainX, py + 38, 60, 14);
                Draw.rrect(ctx, mainX, py + 38, 60, 14, 3, co(bh ? VIOLET : 0xFF2A3550));
                ctx.drawText(textRenderer, "< BACK", mainX + 8, py + 42, co(0xFFFFFFFF), false);
                ctx.drawText(textRenderer, cfg.name, mainX + 70, py + 42, co(0xFFE5E7EB), true);
            }
        }

        content(ctx, mx, my, -1, false);
        if (search.visible) search.render(ctx, mx, my, delta);
        if (!status.isEmpty()) ctx.drawText(textRenderer, status, mainX, py + ph - 12, cp(0xFFC084FC), false);
    }

    private void sidebar(DrawContext ctx, int mx, int my) {
        int sx = px + 8, sy = py + 38, sb = py + ph - 8;
        Draw.rrect(ctx, sx, sy, SW, sb - sy, 4, co(0x66000000));
        List<String> prof = HudManager.profiles();
        for (int i = 0; i < prof.size(); i++) {
            int y = sy + 4 + i * 16;
            if (y + 14 > sb - 44) break;
            String n = prof.get(i);
            boolean act = n.equals(NfConfig.profile), hov = inR(mx, my, sx + 3, y, SW - 6, 14);
            Draw.rrect(ctx, sx + 3, y, SW - 6, 14, 3, co(act ? 0xCC3B2A66 : hov ? 0x55FFFFFF : 0x44000000));
            ctx.drawText(textRenderer, textRenderer.trimToWidth(n, SW - 14), sx + 8, y + 3, co(act ? 0xFFFFFFFF : 0xFFB4B4C0), false);
        }
        boolean nh = inR(mx, my, sx + 4, sb - 40, SW - 8, 14);
        Draw.rrect(ctx, sx + 4, sb - 40, SW - 8, 14, 3, co(nh ? 0xFF33334A : 0xFF22222E));
        String nl = "NEW PROFILE";
        ctx.drawText(textRenderer, nl, sx + 4 + (SW - 8 - textRenderer.getWidth(nl)) / 2, sb - 36, co(0xFFB4B4C0), false);
        boolean eh = inR(mx, my, sx + 4, sb - 22, SW - 8, 16);
        Anim ea = an("edit", 0); ea.to(eh ? 1f : 0f);
        Draw.rrect(ctx, sx + 4, sb - 22, SW - 8, 16, 3, co(Draw.lerp(0xFF4F7BD8, PURPLE, ea.get())));
        String el = "EDIT HUD LAYOUT";
        ctx.drawText(textRenderer, el, sx + 4 + (SW - 8 - textRenderer.getWidth(el)) / 2, sb - 18, co(0xFFFFFFFF), false);
    }

    // ---------------- content (render + klik dalam satu jalur) ----------------
    private void content(DrawContext ctx, int mx, int my, int btn, boolean click) {
        if (click && !inR(mx, my, mainX, cTop, mainW, cBot - cTop)) return;
        if (!click) ctx.enableScissor(mainX, cTop, mainX + mainW, cBot);
        int y = Math.round(cTop - scroll + (1f - pg) * 10f);
        int start = y;
        if (cfg != null) y = rows(ctx, mx, my, btn, click, cfg.settings, y);
        else if (page == 0) y = cards(ctx, mx, my, btn, click, y);
        else if (page == 1) y = buttons(ctx, mx, my, btn, click, y);
        else { y = rows(ctx, mx, my, btn, click, List.of(NfConfig.ANIM, NfConfig.SPEED), y); y += 6; y = buttons(ctx, mx, my, btn, click, y); }
        if (!click) { ctx.disableScissor(); total = y - start; }
    }

    private List<HudElement> filtered() {
        String q = search == null ? "" : search.getText().toLowerCase(Locale.ROOT);
        List<HudElement> l = new ArrayList<>();
        for (HudElement e : HudManager.ELEMENTS)
            if ((filter.equals("ALL") || e.category.equalsIgnoreCase(filter))
                    && (q.isEmpty() || e.name.toLowerCase(Locale.ROOT).contains(q) || e.desc.toLowerCase(Locale.ROOT).contains(q))) l.add(e);
        return l;
    }

    private static String abbr(HudElement e) { return e.id.length() <= 4 ? e.id.toUpperCase(Locale.ROOT) : e.id.substring(0, 2).toUpperCase(Locale.ROOT); }

    private int cards(DrawContext ctx, int mx, int my, int btn, boolean click, int y) {
        List<HudElement> l = filtered();
        int gap = 6, cols = Math.max(1, (mainW + gap) / 96), cw = (mainW - (cols - 1) * gap) / cols, ch = 92;
        for (int i = 0; i < l.size(); i++) {
            HudElement e = l.get(i);
            int x = mainX + (i % cols) * (cw + gap), yy = y + (i / cols) * (ch + gap);
            int ox = x + 4, oy = yy + ch - 36, ow = cw - 8;       // tombol OPTIONS
            int ex = x, ey = yy + ch - 18;                          // bar ENABLED
            if (click) {
                if (btn != 0) continue;
                if (inR(mx, my, ox, oy, ow, 14)) { cfg = e; scroll = 0; pageAnim.set(0); return y; }
                if (inR(mx, my, ex, ey, cw, 18)) { e.enabled = !e.enabled; HudManager.saveAll(); return y; }
                continue;
            }
            boolean hov = inR(mx, my, x, yy, cw, ch) && my >= cTop && my < cBot;
            Anim h = an("h" + e.id, 0); h.to(hov ? 1f : 0f); float hv = h.get();
            Draw.rrect(ctx, x, yy, cw, ch, 5, cp(Draw.lerp(0x55000000, 0x77000000, hv)));
            Draw.rborder(ctx, x, yy, cw, ch, 5, 1, cp(Draw.lerp(0x33FFFFFF, 0x66C084FC, hv)));
            // ikon item Minecraft (diperbesar 2x)
            var m = ctx.getMatrices();
            m.pushMatrix(); m.translate(x + cw / 2f - 16f, yy + 6f); m.scale(2f, 2f);
            ctx.drawItem(e.iconStack(), 0, 0);
            m.popMatrix();
            if (!e.enabled) ctx.fill(x + cw / 2 - 16, yy + 6, x + cw / 2 + 16, yy + 38, cp(0x88101014));
            String nm = textRenderer.trimToWidth(e.name, cw - 8);
            ctx.drawText(textRenderer, nm, x + (cw - textRenderer.getWidth(nm)) / 2, yy + 42, cp(0xFFB4B4C0), false);
            // OPTIONS
            boolean oh = inR(mx, my, ox, oy, ow, 14);
            ctx.fill(ox, oy, ox + ow, oy + 14, cp(oh ? 0xCC3A3A48 : 0xAA2A2A34));
            ctx.drawText(textRenderer, "OPTIONS", ox + (ow - textRenderer.getWidth("OPTIONS")) / 2, oy + 3, cp(0xFFFFFFFF), false);
            // ENABLED / DISABLED
            Anim t = an("t" + e.id, e.enabled ? 1f : 0f); t.to(e.enabled ? 1f : 0f); float tv = t.get();
            boolean eh = inR(mx, my, ex, ey, cw, 18);
            int col = Draw.lerp(0xFF7A3434, GREEN, tv);
            Draw.rrect(ctx, ex, ey, cw, 18, 4, cp(eh ? Draw.lerp(col, 0xFFFFFFFF, 0.12f) : col));
            ctx.fill(ex + 2, ey, ex + cw - 2, ey + 6, cp(Draw.a(0xFFFFFFFF, 0.0f)));
            String s = e.enabled ? "ENABLED" : "DISABLED";
            ctx.drawText(textRenderer, s, ex + (cw - textRenderer.getWidth(s)) / 2, ey + 6, cp(0xFFFFFFFF), false);
        }
        int rows = (l.size() + cols - 1) / cols;
        return y + rows * (ch + gap);
    }

    private int rows(DrawContext ctx, int mx, int my, int btn, boolean click, List<Setting> list, int y) {
        int rh = 22, right = mainX + mainW;
        for (Setting s : list) {
            if (click) {
                if (inR(mx, my, mainX, y, mainW, rh)) {
                    switch (s.type) {
                        case BOOL -> { if (btn == 0) { s.cycle(1); HudManager.saveAll(); } }
                        case ENUM, COLOR -> { if (btn == 0 || btn == 1) { s.cycle(btn == 1 ? -1 : 1); HudManager.saveAll(); } }
                        case NUM -> { if (btn == 0) { dragX1 = right - 150; dragX2 = right - 46; drag = s; slide(mx); } }
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
        if (page == 1) {
            for (String n : Themes.ALL.keySet())
                l.add(new Btn(n, n.equals(NfConfig.theme), () -> { Themes.apply(n); status = "Theme applied: " + n; }));
        } else if (page == 2) {
            l.add(new Btn("Export layout to clipboard", false, () -> { client.keyboard.setClipboard(HudManager.exportString()); status = "Layout copied to clipboard"; }));
            l.add(new Btn("Import layout from clipboard", false, () ->
                status = HudManager.importString(client.keyboard.getClipboard()) ? "Layout imported" : "Clipboard has no valid layout"));
            l.add(new Btn("Reset all HUD settings", false, () -> { HudManager.resetAll(); status = "All HUD settings reset"; }));
        }
        return l;
    }

    private int buttons(DrawContext ctx, int mx, int my, int btn, boolean click, int y) {
        for (Btn b : buttonsFor()) {
            if (click) {
                if (btn == 0 && inR(mx, my, mainX, y, mainW, 22)) { b.run().run(); HudManager.saveAll(); return y; }
            } else {
                boolean hov = inR(mx, my, mainX, y, mainW, 22) && my >= cTop && my < cBot;
                Anim h = an("b" + b.label(), 0); h.to(hov ? 1f : 0f);
                int base = b.active() ? 0xAA4C2A8C : 0x66000000;
                Draw.rrect(ctx, mainX, y, mainW, 22, 5, cp(Draw.lerp(base, 0xAA3B2A66, h.get())));
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
        if (search.visible && inR(mx, my, search.getX(), search.getY(), search.getWidth(), search.getHeight())) {
            setFocused(search); search.setFocused(true);
            return search.mouseClicked(click, doubled);
        }
        search.setFocused(false); setFocused(null);
        if (btn != 0) { content(null, (int) mx, (int) my, btn, true); return true; }

        if (inR(mx, my, px + pw - 24, py + 7, 16, 16)) { close(); return true; }
        for (int i = 0; i < TABS.length; i++) if (inR(mx, my, tabX(i), py + 7, tabW(), 16)) { setPage(i); return true; }

        int sx = px + 8, sb = py + ph - 8;
        List<String> prof = HudManager.profiles();
        for (int i = 0; i < prof.size(); i++) {
            int y = py + 38 + 4 + i * 16;
            if (y + 14 > sb - 44) break;
            if (inR(mx, my, sx + 3, y, SW - 6, 14)) { HudManager.loadProfile(prof.get(i)); status = "Loaded profile " + prof.get(i); return true; }
        }
        if (inR(mx, my, sx + 4, sb - 40, SW - 8, 14)) { status = "Created " + HudManager.newProfile(); return true; }
        if (inR(mx, my, sx + 4, sb - 22, SW - 8, 16)) { client.setScreen(new HudEditorScreen(this)); return true; }

        if (cardView()) {
            for (int i = 0; i < CHIPS.length; i++)
                if (inR(mx, my, chipX(i), py + 38, chipW(i), 14)) { filter = CAT[i]; scroll = 0; return true; }
        } else if (cfg != null && inR(mx, my, mainX, py + 38, 60, 14)) { cfg = null; scroll = 0; pageAnim.set(0); return true; }

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
