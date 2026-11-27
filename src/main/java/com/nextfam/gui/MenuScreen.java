package com.nextfam.gui;

import com.nextfam.config.NfConfig;
import com.nextfam.config.Setting;
import com.nextfam.hud.HudElement;
import com.nextfam.hud.HudManager;
import com.nextfam.hud.Themes;
import com.nextfam.util.Anim;
import com.nextfam.util.Draw;
import com.nextfam.util.Logo;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.*;

/** Menu NextFam 4.0: panel kaca gelap, partikel, tab geser, kartu bergaya per-kategori, slider dan toggle animasi. */
public class MenuScreen extends Screen {
    private static final String[] TABS = {"MODS", "THEMES", "SETTINGS"};
    private static final String[] CHIPS = {"ALL", "HUD", "PLAYER", "COMBAT", "VISUAL", "INV", "CHAT", "OPT", "TWEAKS"};
    private static final String[] CAT = {"ALL", "HUD", "Player", "Combat", "Visual", "Inventory", "Chat", "Optimize", "Tweaks"};
    private static final int PURPLE = 0xFFA855F7, VIOLET = 0xFF7C3AED, LILAC = 0xFFC084FC, SW = 104;

    private record Btn(String label, boolean active, Runnable run) {}

    private int page = 0;
    private String filter = "ALL";
    private HudElement cfg;
    private float scroll = 0, total = 0;
    private TextFieldWidget search;
    private Setting drag; private int dragX1, dragX2;
    private String status = "";
    private final Anim open = new Anim(0), pageAnim = new Anim(1), tabAnim = new Anim(0);
    private final Map<String, Anim> anims = new HashMap<>();
    private float o = 1, pg = 1, t = 0;
    private int px, py, pw, ph, mainX, mainW, cTop, cBot;

    public MenuScreen() { super(Text.literal("NextFam Client")); }

    @Override
    protected void init() {
        String old = search == null ? "" : search.getText();
        search = new TextFieldWidget(textRenderer, 0, 0, 100, 14, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search modules..."));
        search.setText(old);
        addDrawableChild(search);
    }

    // ---------- util ----------
    private Anim an(String k, float init) { return anims.computeIfAbsent(k, z -> new Anim(init)); }
    private int co(int c) { return Draw.a(c, o); }
    private int cp(int c) { return Draw.a(c, o * pg); }
    private boolean inR(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private boolean modsTab() { return page == 0; }
    private boolean cardView() { return modsTab() && cfg == null; }
    private void txt(DrawContext c, String s, int x, int y, int col) { c.drawText(textRenderer, s, x, y, col, false); }
    private void txtS(DrawContext c, String s, int x, int y, int col) { c.drawText(textRenderer, s, x, y, col, true); }
    private void ctxt(DrawContext c, String s, int cx, int y, int col) { c.drawText(textRenderer, s, cx - textRenderer.getWidth(s) / 2, y, col, false); }

    static int catColor(String c) {
        return switch (c) {
            case "HUD" -> 0xFFA855F7; case "Player" -> 0xFF60A5FA; case "Combat" -> 0xFFF87171; case "Visual" -> 0xFF22D3EE;
            case "Inventory" -> 0xFFFACC15; case "Chat" -> 0xFFFB923C; case "Optimize" -> 0xFF4ADE80; default -> 0xFFF472B6;
        };
    }

    private void layout() {
        pw = Math.min(width - 16, 600); ph = Math.min(height - 16, 360);
        px = (width - pw) / 2; py = (height - ph) / 2;
        mainX = px + 8 + SW + 10; mainW = pw - SW - 34;
        cTop = modsTab() ? (cfg == null ? py + 82 : py + 66) : py + 44;
        cBot = py + ph - 22;
    }

    private void setPage(int p) { page = p; cfg = null; scroll = 0; pageAnim.set(0); status = ""; }
    private int tabW() { return Math.max(40, Math.min(62, (pw - 40 - 210) / 3)); }
    private int tabX(int i) { return px + pw - 36 - (3 * tabW() + 8) + i * (tabW() + 4); }
    private int chipX(int i) { int x = mainX; for (int k = 0; k < i; k++) x += textRenderer.getWidth(CHIPS[k]) + 14; return x; }
    private int chipW(int i) { return textRenderer.getWidth(CHIPS[i]) + 10; }

    // ---------- render ----------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        layout();
        t = System.nanoTime() / 1_000_000_000.0f;
        open.to(1f); o = open.get(); pageAnim.to(1f); pg = pageAnim.get();

        // latar: gradasi + partikel
        ctx.fillGradient(0, 0, width, height, Draw.a(0x88000000, o), Draw.a(0xCC160A28, o));
        for (int i = 0; i < 32; i++) {
            float sp = 0.4f + ((i * 37) % 7) * 0.12f;
            float fx = ((i * 73) % 101) / 101f * width + (float) Math.sin(t * 0.4 + i) * 14f;
            float fy = height - ((t * 10f * sp + ((i * 53) % 97) / 97f * height) % height);
            float a = 0.10f + 0.28f * (0.5f + 0.5f * (float) Math.sin(t * 1.5 + i * 1.7));
            int s = i % 3 == 0 ? 2 : 1;
            ctx.fill((int) fx, (int) fy, (int) fx + s, (int) fy + s, Draw.a(LILAC, a * o));
        }

        boolean tf = o < 0.995f;
        var m = ctx.getMatrices();
        if (tf) {
            float s = 0.94f + 0.06f * o;
            m.pushMatrix(); m.translate(px + pw / 2f, py + ph / 2f); m.scale(s, s); m.translate(-(px + pw / 2f), -(py + ph / 2f));
        }

        // bayangan + panel
        for (int k = 4; k >= 1; k--) Draw.rrect(ctx, px - k * 2, py - k * 2, pw + k * 4, ph + k * 4, 10 + k, co(0x0C000000));
        Draw.rrect(ctx, px, py, pw, ph, 9, co(0xC0101018));
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 2f);
        Draw.rborder(ctx, px, py, pw, ph, 9, 1, co(Draw.a(PURPLE, 0.30f + 0.20f * pulse)));

        header(ctx, mx, my, pulse);
        sidebar(ctx, mx, my);

        // baris filter / search / back
        if (cardView()) {
            for (int i = 0; i < CHIPS.length; i++) {
                boolean sel = CAT[i].equals(filter), hov = inR(mx, my, chipX(i), py + 44, chipW(i), 14);
                Anim h = an("chip" + i, 0); h.to(sel ? 1f : hov ? 0.5f : 0f); float hv = h.get();
                int cc = i == 0 ? PURPLE : catColor(CAT[i]);
                Draw.rrect(ctx, chipX(i), py + 44, chipW(i), 14, 7, co(Draw.lerp(0x66000000, Draw.a(cc, 0.55f), hv)));
                if (sel) Draw.rborder(ctx, chipX(i), py + 44, chipW(i), 14, 7, 1, co(cc));
                txt(ctx, CHIPS[i], chipX(i) + 5, py + 48, co(sel ? 0xFFFFFFFF : 0xFFB4B4C0));
            }
            search.visible = true;
            search.setX(mainX); search.setY(py + 62); search.setWidth(mainW);
        } else {
            search.visible = false;
            if (cfg != null) {
                boolean bh = inR(mx, my, mainX, py + 44, 64, 16);
                Draw.rrect(ctx, mainX, py + 44, 64, 16, 8, co(bh ? Draw.a(VIOLET, 0.9f) : 0x66000000));
                txt(ctx, "< BACK", mainX + 12, py + 48, co(0xFFFFFFFF));
                txtS(ctx, cfg.name, mainX + 74, py + 48, co(0xFFE5E7EB));
            }
        }

        content(ctx, mx, my, -1, false);
        scrollbar(ctx);
        if (search.visible) search.render(ctx, mx, my, delta);

        // footer
        ctx.fill(px + 10, py + ph - 20, px + pw - 10, py + ph - 19, co(0x22FFFFFF));
        if (!status.isEmpty()) txt(ctx, status, mainX, py + ph - 14, cp(LILAC));
        String f = "NextFam Client 4.0  |  Razzer  |  Menu key: Right Shift";
        txt(ctx, f, px + pw - 12 - textRenderer.getWidth(f), py + ph - 14, co(0xFF6B7280));

        if (tf) m.popMatrix();
    }

    private void header(DrawContext ctx, int mx, int my, float pulse) {
        Draw.rrectGrad(ctx, px, py, pw, 36, 9, co(0xE02A1650), co(0xE0120A24));
        ctx.fill(px, py + 26, px + pw, py + 36, co(0xE0120A24));
        // garis aksen bergerak
        for (int x = 0; x < pw; x += 3) {
            float a = 0.18f + 0.65f * Math.max(0f, (float) Math.sin(((double) x / pw - t * 0.25) * Math.PI * 2));
            ctx.fill(px + x, py + 36, px + x + 3, py + 37, co(Draw.a(PURPLE, a)));
        }
        // logo + cahaya
        Draw.rrect(ctx, px + 6, py + 5, 26, 26, 13, co(Draw.a(PURPLE, 0.14f + 0.12f * pulse)));
        Logo.draw(ctx, px + 7, py + 6, 24, o);
        txtS(ctx, "NEXTFAM CLIENT", px + 38, py + 9, co(0xFFFFFFFF));
        txtS(ctx, "- RAZZER", px + 38 + textRenderer.getWidth("NEXTFAM CLIENT ") , py + 9, co(LILAC));
        if (pw >= 520) txt(ctx, "Your HUD. Your Style. Your NextFam.", px + 38, py + 21, co(0xFF8B8B9E));

        // tab: pil geser
        int tw = tabW();
        tabAnim.to(tabX(page)); float tx = tabAnim.get();
        Draw.rrect(ctx, tabX(0) - 3, py + 7, 3 * tw + 8 + 6, 22, 11, co(0x66000000));
        Draw.rrect(ctx, Math.round(tx), py + 9, tw, 18, 9, co(Draw.a(VIOLET, 0.9f)));
        for (int i = 0; i < TABS.length; i++) {
            boolean hov = inR(mx, my, tabX(i), py + 9, tw, 18);
            ctxt(ctx, TABS[i], tabX(i) + tw / 2, py + 14, co(i == page ? 0xFFFFFFFF : hov ? 0xFFE5E7EB : 0xFF9CA3AF));
        }
        // tombol tutup
        boolean xh = inR(mx, my, px + pw - 28, py + 9, 18, 18);
        Anim xa = an("x", 0); xa.to(xh ? 1f : 0f);
        Draw.rrect(ctx, px + pw - 28, py + 9, 18, 18, 9, co(Draw.lerp(0x66000000, 0xFFDC2626, xa.get())));
        ctxt(ctx, "x", px + pw - 19, py + 14, co(0xFFFFFFFF));
    }

    private void sidebar(DrawContext ctx, int mx, int my) {
        int sx = px + 8, sy = py + 44, sb = py + ph - 26;
        Draw.rrect(ctx, sx, sy, SW, sb - sy, 6, co(0x77000000));
        txt(ctx, "PROFILES", sx + 8, sy + 6, co(0xFF6B7280));
        List<String> prof = HudManager.profiles();
        for (int i = 0; i < prof.size(); i++) {
            int y = sy + 19 + i * 17;
            if (y + 15 > sb - 44) break;
            String n = prof.get(i);
            boolean act = n.equals(NfConfig.profile), hov = inR(mx, my, sx + 4, y, SW - 8, 15);
            Anim h = an("pf" + n, 0); h.to(act ? 1f : hov ? 0.5f : 0f); float hv = h.get();
            Draw.rrect(ctx, sx + 4, y, SW - 8, 15, 5, co(Draw.lerp(0x33000000, Draw.a(VIOLET, 0.75f), hv)));
            Draw.rrect(ctx, sx + 8, y + 4, 7, 7, 3, co(act ? 0xFF4ADE80 : 0xFF4B5563));
            txt(ctx, textRenderer.trimToWidth(n, SW - 30), sx + 20, y + 4, co(act ? 0xFFFFFFFF : 0xFFB4B4C0));
        }
        boolean nh = inR(mx, my, sx + 4, sb - 42, SW - 8, 15);
        Anim na = an("newp", 0); na.to(nh ? 1f : 0f);
        Draw.rrect(ctx, sx + 4, sb - 42, SW - 8, 15, 5, co(Draw.lerp(0x55000000, 0xFF33334A, na.get())));
        ctxt(ctx, "+ NEW PROFILE", sx + SW / 2, sb - 38, co(0xFFB4B4C0));
        boolean eh = inR(mx, my, sx + 4, sb - 22, SW - 8, 18);
        Anim ea = an("edit", 0); ea.to(eh ? 1f : 0f); float ev = ea.get();
        Draw.rrect(ctx, sx + 2, sb - 24, SW - 4, 22, 7, co(Draw.a(PURPLE, 0.12f + 0.22f * ev)));
        Draw.rrectGrad(ctx, sx + 4, sb - 22, SW - 8, 18, 5, co(Draw.lerp(VIOLET, PURPLE, ev)), co(Draw.lerp(0xFF5B21B6, VIOLET, ev)));
        ctxt(ctx, "EDIT HUD LAYOUT", sx + SW / 2, sb - 17, co(0xFFFFFFFF));
    }

    private void scrollbar(DrawContext ctx) {
        int vh = cBot - cTop;
        if (total <= vh) return;
        int x = mainX + mainW + 3;
        Draw.rrect(ctx, x, cTop, 3, vh, 1, cp(0x22FFFFFF));
        float ratio = vh / total, max = total - vh;
        int th = Math.max(16, Math.round(vh * ratio)), ty = cTop + Math.round((vh - th) * (max <= 0 ? 0 : scroll / max));
        Draw.rrect(ctx, x, ty, 3, th, 1, cp(Draw.a(PURPLE, 0.8f)));
    }

    // ---------- content (render + klik dalam satu jalur) ----------
    private void content(DrawContext ctx, int mx, int my, int btn, boolean click) {
        if (click && !inR(mx, my, mainX, cTop, mainW, cBot - cTop)) return;
        if (!click) ctx.enableScissor(mainX, cTop, mainX + mainW, cBot);
        int y = Math.round(cTop - scroll + (1f - pg) * 12f);
        int start = y;
        if (cfg != null) y = rows(ctx, mx, my, btn, click, cfg.settings, y);
        else if (page == 0) y = cards(ctx, mx, my, btn, click, y);
        else if (page == 1) y = buttons(ctx, mx, my, btn, click, y);
        else { y = rows(ctx, mx, my, btn, click, List.of(NfConfig.ANIM, NfConfig.SPEED), y); y += 8; y = buttons(ctx, mx, my, btn, click, y); }
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

    private int cards(DrawContext ctx, int mx, int my, int btn, boolean click, int y) {
        List<HudElement> l = filtered();
        int gap = 8, cols = Math.max(1, (mainW + gap) / 108), cw = (mainW - (cols - 1) * gap) / cols, ch = 104;
        for (int i = 0; i < l.size(); i++) {
            HudElement e = l.get(i);
            int x = mainX + (i % cols) * (cw + gap), yy = y + (i / cols) * (ch + gap);
            int ox = x + 6, oy = yy + ch - 40, ow = cw - 12;   // OPTIONS
            int ex = x + 6, ey = yy + ch - 22, ew = cw - 12;   // ENABLED
            if (click) {
                if (btn != 0) continue;
                if (inR(mx, my, ox, oy, ow, 15)) { cfg = e; scroll = 0; pageAnim.set(0); return y; }
                if (inR(mx, my, ex, ey, ew, 17)) { e.enabled = !e.enabled; HudManager.saveAll(); return y; }
                continue;
            }
            int cc = catColor(e.category);
            boolean hov = inR(mx, my, x, yy, cw, ch) && my >= cTop && my < cBot;
            Anim h = an("h" + e.id, 0); h.to(hov ? 1f : 0f); float hv = h.get();
            int cy = yy - Math.round(hv * 2f);
            // kartu
            Draw.rrect(ctx, x, cy, cw, ch, 7, cp(Draw.lerp(0x66000000, 0x88181824, hv)));
            Draw.rborder(ctx, x, cy, cw, ch, 7, 1, cp(Draw.lerp(0x22FFFFFF, Draw.a(cc, 0.85f), hv)));
            Draw.rrectGrad(ctx, x + 1, cy + 1, cw - 2, 14, 6, cp(Draw.a(cc, 0.22f + 0.1f * hv)), cp(Draw.a(cc, 0f)));
            txt(ctx, e.category.toUpperCase(Locale.ROOT), x + 7, cy + 5, cp(Draw.a(cc, 0.95f)));
            Draw.rrect(ctx, x + cw - 12, cy + 6, 6, 6, 3, cp(e.enabled ? 0xFF4ADE80 : 0xFF6B7280));
            // ikon item dengan cahaya
            Draw.rrect(ctx, x + cw / 2 - 20, cy + 15, 40, 40, 20, cp(Draw.a(cc, 0.10f + 0.16f * hv)));
            var m = ctx.getMatrices();
            m.pushMatrix(); m.translate(x + cw / 2f - 16f, cy + 19f); m.scale(2f, 2f);
            ctx.drawItem(e.iconStack(), 0, 0);
            m.popMatrix();
            if (!e.enabled) Draw.rrect(ctx, x + cw / 2 - 20, cy + 15, 40, 40, 20, cp(0x88101018));
            String nm = textRenderer.trimToWidth(e.name, cw - 8);
            ctxt(ctx, nm, x + cw / 2, cy + 58, cp(0xFFE5E7EB));
            // OPTIONS
            boolean oh = inR(mx, my, ox, oy, ow, 15);
            Anim oa = an("o" + e.id, 0); oa.to(oh ? 1f : 0f);
            Draw.rrect(ctx, ox, oy - Math.round(hv * 2f), ow, 15, 5, cp(Draw.lerp(0x99262634, Draw.a(cc, 0.7f), oa.get())));
            ctxt(ctx, "OPTIONS", x + cw / 2, oy + 4 - Math.round(hv * 2f), cp(0xFFFFFFFF));
            // ENABLED / DISABLED
            Anim tg = an("t" + e.id, e.enabled ? 1f : 0f); tg.to(e.enabled ? 1f : 0f); float tv = tg.get();
            boolean eh = inR(mx, my, ex, ey, ew, 17);
            int top = Draw.lerp(0xFF7F2D2D, 0xFF35C27A, tv), bot = Draw.lerp(0xFF5E1F1F, 0xFF1F8F58, tv);
            if (eh) { top = Draw.lerp(top, 0xFFFFFFFF, 0.12f); bot = Draw.lerp(bot, 0xFFFFFFFF, 0.12f); }
            Draw.rrectGrad(ctx, ex, ey - Math.round(hv * 2f), ew, 17, 5, cp(top), cp(bot));
            ctxt(ctx, e.enabled ? "ENABLED" : "DISABLED", x + cw / 2, ey + 5 - Math.round(hv * 2f), cp(0xFFFFFFFF));
        }
        int rows = (l.size() + cols - 1) / cols;
        return y + rows * (ch + gap);
    }

    private int rows(DrawContext ctx, int mx, int my, int btn, boolean click, List<Setting> list, int y) {
        int rh = 24, right = mainX + mainW;
        int idx = 0;
        for (Setting s : list) {
            if (click) {
                if (inR(mx, my, mainX, y, mainW, rh)) {
                    switch (s.type) {
                        case BOOL -> { if (btn == 0) { s.cycle(1); HudManager.saveAll(); } }
                        case ENUM, COLOR -> { if (btn == 0 || btn == 1) { s.cycle(btn == 1 ? -1 : 1); HudManager.saveAll(); } }
                        case NUM -> { if (btn == 0) { dragX1 = right - 156; dragX2 = right - 52; drag = s; slide(mx); } }
                    }
                    return y;
                }
                y += rh; continue;
            }
            boolean hov = inR(mx, my, mainX, y, mainW, rh) && my >= cTop && my < cBot;
            Anim ha = an("r" + System.identityHashCode(s), 0); ha.to(hov ? 1f : 0f);
            Draw.rrect(ctx, mainX, y, mainW, rh - 3, 6, cp(Draw.lerp((idx & 1) == 0 ? 0x33000000 : 0x1F000000, 0x44FFFFFF, ha.get() * 0.5f)));
            txt(ctx, s.name, mainX + 8, y + 8, cp(0xFFE5E7EB));
            switch (s.type) {
                case BOOL -> {
                    Anim tg = an("s" + System.identityHashCode(s), s.b ? 1f : 0f); tg.to(s.b ? 1f : 0f); float tv = tg.get();
                    if (tv > 0.05f) Draw.rrect(ctx, right - 41, y + 2, 34, 18, 9, cp(Draw.a(VIOLET, 0.25f * tv)));
                    Draw.rrect(ctx, right - 38, y + 4, 28, 14, 7, cp(Draw.lerp(0xFF3A3A47, VIOLET, tv)));
                    Draw.rrect(ctx, right - 36 + Math.round(tv * 14), y + 6, 10, 10, 5, cp(0xFFFFFFFF));
                }
                case NUM -> {
                    int x1 = right - 156, x2 = right - 52;
                    float tt = (s.f - s.min) / (s.max - s.min);
                    int kx = x1 + Math.round((x2 - x1) * tt);
                    Draw.rrect(ctx, x1, y + 8, x2 - x1, 5, 2, cp(0xFF2A2A38));
                    Draw.rrectGrad(ctx, x1, y + 8, Math.max(3, kx - x1), 5, 2, cp(LILAC), cp(VIOLET));
                    Draw.rrect(ctx, kx - 6, y + 4, 12, 12, 6, cp(Draw.a(PURPLE, 0.30f)));
                    Draw.rrect(ctx, kx - 4, y + 6, 8, 8, 4, cp(0xFFFFFFFF));
                    Draw.rrect(ctx, x2 + 8, y + 4, right - x2 - 14, 14, 5, cp(0x55000000));
                    ctxt(ctx, s.valueText(), x2 + 8 + (right - x2 - 14) / 2, y + 8, cp(0xFFD1D5DB));
                }
                case COLOR -> {
                    Draw.rrect(ctx, right - 118, y + 3, 16, 16, 5, cp(0xFF4B5563));
                    Draw.rrect(ctx, right - 117, y + 4, 14, 14, 4, cp(0xFF202028));
                    Draw.rrect(ctx, right - 117, y + 4, 14, 14, 4, cp(s.c));
                    txt(ctx, s.valueText(), right - 98, y + 8, cp(0xFF9CA3AF));
                }
                case ENUM -> {
                    Draw.rrect(ctx, right - 140, y + 2, 132, 17, 6, cp(hov ? 0xFF33334A : 0xFF262634));
                    String v = textRenderer.trimToWidth(s.str(), 118);
                    ctxt(ctx, v, right - 74, y + 7, cp(0xFFFFFFFF));
                }
            }
            y += rh; idx++;
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
                if (btn == 0 && inR(mx, my, mainX, y, mainW, 24)) { b.run().run(); HudManager.saveAll(); return y; }
            } else {
                boolean hov = inR(mx, my, mainX, y, mainW, 24) && my >= cTop && my < cBot;
                Anim h = an("b" + b.label(), 0); h.to(hov ? 1f : 0f); float hv = h.get();
                int base = b.active() ? Draw.a(VIOLET, 0.55f) : 0x55000000;
                Draw.rrect(ctx, mainX, y, mainW, 24, 7, cp(Draw.lerp(base, Draw.a(VIOLET, 0.75f), hv)));
                if (b.active()) { Draw.rrect(ctx, mainX + 4, y + 5, 3, 14, 1, cp(LILAC)); txt(ctx, "ACTIVE", mainX + mainW - 46, y + 8, cp(0xFF4ADE80)); }
                txt(ctx, b.label(), mainX + 14, y + 8, cp(0xFFFFFFFF));
            }
            y += 29;
        }
        return y;
    }

    private void slide(double mx) {
        if (drag == null) return;
        float tt = (float) ((mx - dragX1) / (double) (dragX2 - dragX1));
        tt = Math.max(0f, Math.min(1f, tt));
        float v = drag.min + tt * (drag.max - drag.min);
        drag.setF(Math.round(v / drag.step) * drag.step);
    }

    // ---------- input ----------
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

        if (inR(mx, my, px + pw - 28, py + 9, 18, 18)) { close(); return true; }
        for (int i = 0; i < TABS.length; i++) if (inR(mx, my, tabX(i), py + 9, tabW(), 18)) { setPage(i); return true; }

        int sx = px + 8, sb = py + ph - 26;
        List<String> prof = HudManager.profiles();
        for (int i = 0; i < prof.size(); i++) {
            int y = py + 44 + 19 + i * 17;
            if (y + 15 > sb - 44) break;
            if (inR(mx, my, sx + 4, y, SW - 8, 15)) { HudManager.loadProfile(prof.get(i)); status = "Loaded profile " + prof.get(i); return true; }
        }
        if (inR(mx, my, sx + 4, sb - 42, SW - 8, 15)) { status = "Created " + HudManager.newProfile(); return true; }
        if (inR(mx, my, sx + 4, sb - 22, SW - 8, 18)) { client.setScreen(new HudEditorScreen(this)); return true; }

        if (cardView()) {
            for (int i = 0; i < CHIPS.length; i++)
                if (inR(mx, my, chipX(i), py + 44, chipW(i), 14)) { filter = CAT[i]; scroll = 0; return true; }
        } else if (cfg != null && inR(mx, my, mainX, py + 44, 64, 16)) { cfg = null; scroll = 0; pageAnim.set(0); return true; }

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
        scroll = Math.max(0f, Math.min(max, scroll - (float) v * 22f));
        return true;
    }

    @Override public void removed() { HudManager.saveAll(); }
    @Override public void close() { client.setScreen(null); }
}
