package com.nextfam.gui;

import com.nextfam.config.NfConfig;
import com.nextfam.config.Setting;
import com.nextfam.hud.HudElement;
import com.nextfam.hud.HudManager;
import com.nextfam.hud.Themes;
import com.nextfam.pack.PackStudioScreen;
import com.nextfam.util.Anim;
import com.nextfam.util.Draw;
import com.nextfam.util.Fonts;
import com.nextfam.util.Logo;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.*;

/** Menu NextFam 5.0: hitam-putih, font Poppins, kartu dengan culling, color picker HSV + favorit. */
public class MenuScreen extends Screen {
    private static final String[] TABS = {"MODS", "THEMES", "SETTINGS"};
    private static final String[] CHIPS = {"ALL", "HUD", "PLAYER", "COMBAT", "VISUAL", "INV", "CHAT", "OPT", "TWEAKS"};
    private static final String[] CAT = {"ALL", "HUD", "Player", "Combat", "Visual", "Inventory", "Chat", "Optimize", "Tweaks"};
    private static final int WHITE = 0xFFFFFFFF, GRAY = 0xFF9A9A9A, SOFT = 0xFFD0D0D0, SW = 104;

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
    // cache daftar kartu
    private List<HudElement> cached = List.of(); private String cKey = "";
    // color picker
    private Setting pick; private float hH, hS, hV, hA; private int pickDrag;

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
    private boolean cardView() { return modsTab() && cfg == null && pick == null; }
    private void txt(DrawContext c, String s, int x, int y, int col) { c.drawText(textRenderer, Fonts.m(s), x, y, col, false); }
    private void txtB(DrawContext c, String s, int x, int y, int col) { c.drawText(textRenderer, Fonts.b(s), x, y, col, false); }
    private void ctxt(DrawContext c, String s, int cx, int y, int col) { c.drawText(textRenderer, Fonts.m(s), cx - Fonts.w(textRenderer, s) / 2, y, col, false); }
    private void ctxtB(DrawContext c, String s, int cx, int y, int col) { c.drawText(textRenderer, Fonts.b(s), cx - Fonts.wb(textRenderer, s) / 2, y, col, false); }

    private void layout() {
        pw = Math.min(width - 16, 620); ph = Math.min(height - 16, 370);
        px = (width - pw) / 2; py = (height - ph) / 2;
        mainX = px + 8 + SW + 10; mainW = pw - SW - 34;
        cTop = modsTab() ? (cfg == null ? py + 82 : py + 66) : py + 44;
        cBot = py + ph - 22;
    }

    private void setPage(int p) { page = p; cfg = null; scroll = 0; pageAnim.set(0); status = ""; pick = null; }
    private int tabW() { return Math.max(40, Math.min(62, (pw - 40 - 210) / 3)); }
    private int tabX(int i) { return px + pw - 36 - (3 * tabW() + 8) + i * (tabW() + 4); }
    private int chipX(int i) { int x = mainX; for (int k = 0; k < i; k++) x += Fonts.w(textRenderer, CHIPS[k]) + 14; return x; }
    private int chipW(int i) { return Fonts.w(textRenderer, CHIPS[i]) + 10; }

    // ---------- render ----------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        layout();
        t = System.nanoTime() / 1_000_000_000.0f;
        open.to(1f); o = open.get(); pageAnim.to(1f); pg = pageAnim.get();

        ctx.fillGradient(0, 0, width, height, Draw.a(0x88000000, o), Draw.a(0xCC0C0C0C, o));
        for (int i = 0; i < 28; i++) {
            float sp = 0.4f + ((i * 37) % 7) * 0.12f;
            float fx = ((i * 73) % 101) / 101f * width + (float) Math.sin(t * 0.4 + i) * 14f;
            float fy = height - ((t * 10f * sp + ((i * 53) % 97) / 97f * height) % height);
            float a = 0.10f + 0.25f * (0.5f + 0.5f * (float) Math.sin(t * 1.5 + i * 1.7));
            int s = i % 3 == 0 ? 2 : 1;
            ctx.fill((int) fx, (int) fy, (int) fx + s, (int) fy + s, Draw.a(WHITE, a * o));
        }

        boolean tf = o < 0.995f;
        var m = ctx.getMatrices();
        if (tf) {
            float s = 0.94f + 0.06f * o;
            m.pushMatrix(); m.translate(px + pw / 2f, py + ph / 2f); m.scale(s, s); m.translate(-(px + pw / 2f), -(py + ph / 2f));
        }

        for (int k = 3; k >= 1; k--) Draw.rrect(ctx, px - k * 2, py - k * 2, pw + k * 4, ph + k * 4, 10 + k, co(0x0E000000));
        Draw.rrect(ctx, px, py, pw, ph, 9, co(0xD00A0A0A));
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 2f);
        Draw.rborder(ctx, px, py, pw, ph, 9, 1, co(Draw.a(WHITE, 0.22f + 0.14f * pulse)));

        header(ctx, mx, my, pulse);
        sidebar(ctx, mx, my);

        if (cardView()) {
            for (int i = 0; i < CHIPS.length; i++) {
                boolean sel = CAT[i].equals(filter), hov = inR(mx, my, chipX(i), py + 44, chipW(i), 14);
                Anim h = an("chip" + i, 0); h.to(sel ? 1f : hov ? 0.5f : 0f); float hv = h.get();
                Draw.rrect(ctx, chipX(i), py + 44, chipW(i), 14, 7, co(Draw.lerp(0x66000000, 0xFFEDEDED, hv)));
                txt(ctx, CHIPS[i], chipX(i) + 5, py + 47, co(hv > 0.6f ? 0xFF000000 : 0xFFC8C8C8));
            }
            search.visible = true;
            search.setX(mainX); search.setY(py + 62); search.setWidth(mainW);
        } else {
            search.visible = false;
            if (cfg != null) {
                boolean bh = inR(mx, my, mainX, py + 44, 64, 16);
                Draw.rrect(ctx, mainX, py + 44, 64, 16, 8, co(bh ? 0xFFEDEDED : 0x66000000));
                txt(ctx, "< BACK", mainX + 12, py + 47, co(bh ? 0xFF000000 : WHITE));
                txtB(ctx, cfg.name, mainX + 74, py + 47, co(WHITE));
            }
        }

        content(ctx, mx, my, -1, false);
        scrollbar(ctx);
        if (search.visible) search.render(ctx, mx, my, delta);

        ctx.fill(px + 10, py + ph - 20, px + pw - 10, py + ph - 19, co(0x22FFFFFF));
        if (!status.isEmpty()) txt(ctx, Fonts.trim(textRenderer, status, pw / 2 - 40), mainX, py + ph - 15, cp(SOFT));
        String f = "NextFam Client 5.0  |  Razzer  |  Menu: Right Shift  |  Pack Studio: Right Ctrl";
        txt(ctx, f, px + pw - 12 - Fonts.w(textRenderer, f), py + ph - 15, co(0xFF6B6B6B));

        if (pick != null) pickerRender(ctx, mx, my);
        if (tf) m.popMatrix();
    }

    private void header(DrawContext ctx, int mx, int my, float pulse) {
        Draw.rrectGrad(ctx, px, py, pw, 36, 9, co(0xE02B2B2B), co(0xE0101010));
        ctx.fill(px, py + 26, px + pw, py + 36, co(0xE0101010));
        for (int x = 0; x < pw; x += 3) {
            float a = 0.15f + 0.6f * Math.max(0f, (float) Math.sin(((double) x / pw - t * 0.25) * Math.PI * 2));
            ctx.fill(px + x, py + 36, px + x + 3, py + 37, co(Draw.a(WHITE, a)));
        }
        Draw.rrect(ctx, px + 6, py + 5, 26, 26, 13, co(Draw.a(WHITE, 0.10f + 0.10f * pulse)));
        Logo.draw(ctx, px + 7, py + 6, 24, o);
        txtB(ctx, "NEXTFAM CLIENT", px + 38, py + 8, co(WHITE));
        txt(ctx, "- RAZZER", px + 38 + Fonts.wb(textRenderer, "NEXTFAM CLIENT") + 4, py + 8, co(GRAY));
        if (pw >= 520) txt(ctx, "Your HUD. Your Style. Your NextFam.", px + 38, py + 20, co(0xFF777777));

        int tw = tabW();
        tabAnim.to(tabX(page)); float tx = tabAnim.get();
        Draw.rrect(ctx, tabX(0) - 3, py + 7, 3 * tw + 14, 22, 11, co(0x66000000));
        Draw.rrect(ctx, Math.round(tx), py + 9, tw, 18, 9, co(0xFFEDEDED));
        for (int i = 0; i < TABS.length; i++) {
            boolean hov = inR(mx, my, tabX(i), py + 9, tw, 18);
            ctxtB(ctx, TABS[i], tabX(i) + tw / 2, py + 13, co(i == page ? 0xFF000000 : hov ? WHITE : GRAY));
        }
        boolean xh = inR(mx, my, px + pw - 28, py + 9, 18, 18);
        Anim xa = an("x", 0); xa.to(xh ? 1f : 0f);
        Draw.rrect(ctx, px + pw - 28, py + 9, 18, 18, 9, co(Draw.lerp(0x66000000, 0xFFDC2626, xa.get())));
        ctxt(ctx, "x", px + pw - 19, py + 12, co(WHITE));
    }

    private void sidebar(DrawContext ctx, int mx, int my) {
        int sx = px + 8, sy = py + 44, sb = py + ph - 26;
        Draw.rrect(ctx, sx, sy, SW, sb - sy, 6, co(0x77000000));
        txtB(ctx, "PROFILES", sx + 8, sy + 5, co(0xFF777777));
        List<String> prof = HudManager.profiles();
        for (int i = 0; i < prof.size(); i++) {
            int y = sy + 19 + i * 17;
            if (y + 15 > sb - 68) break;
            String n = prof.get(i);
            boolean act = n.equals(NfConfig.profile), hov = inR(mx, my, sx + 4, y, SW - 8, 15);
            Anim h = an("pf" + n, 0); h.to(act ? 1f : hov ? 0.5f : 0f); float hv = h.get();
            Draw.rrect(ctx, sx + 4, y, SW - 8, 15, 5, co(Draw.lerp(0x33000000, 0xFFEDEDED, hv)));
            Draw.rrect(ctx, sx + 8, y + 4, 7, 7, 3, co(act ? 0xFF4ADE80 : 0xFF4B5563));
            txt(ctx, Fonts.trim(textRenderer, n, SW - 32), sx + 20, y + 3, co(hv > 0.6f ? 0xFF000000 : 0xFFC8C8C8));
        }
        sideBtn(ctx, mx, my, "newp", "+ NEW PROFILE", sx + 4, sb - 62, SW - 8, 15, false);
        sideBtn(ctx, mx, my, "packs", "PACK STUDIO", sx + 4, sb - 42, SW - 8, 16, false);
        sideBtn(ctx, mx, my, "edit", "EDIT HUD LAYOUT", sx + 4, sb - 22, SW - 8, 18, true);
    }

    private void sideBtn(DrawContext ctx, int mx, int my, String id, String label, int x, int y, int w, int h, boolean strong) {
        boolean hov = inR(mx, my, x, y, w, h);
        Anim a = an(id, 0); a.to(hov ? 1f : 0f); float v = a.get();
        if (strong) Draw.rrect(ctx, x - 2, y - 2, w + 4, h + 4, 7, co(Draw.a(WHITE, 0.08f + 0.18f * v)));
        Draw.rrectGrad(ctx, x, y, w, h, 5, co(strong ? Draw.lerp(0xFFE0E0E0, WHITE, v) : Draw.lerp(0xFF262626, 0xFF3C3C3C, v)),
                co(strong ? Draw.lerp(0xFFB5B5B5, 0xFFE0E0E0, v) : Draw.lerp(0xFF1C1C1C, 0xFF2E2E2E, v)));
        ctxt(ctx, label, x + w / 2, y + (h - 9) / 2, co(strong ? 0xFF000000 : WHITE));
    }

    private void scrollbar(DrawContext ctx) {
        int vh = cBot - cTop;
        if (total <= vh) return;
        int x = mainX + mainW + 3;
        Draw.rrect(ctx, x, cTop, 3, vh, 1, cp(0x22FFFFFF));
        float ratio = vh / total, max = total - vh;
        int th = Math.max(16, Math.round(vh * ratio)), ty = cTop + Math.round((vh - th) * (max <= 0 ? 0 : scroll / max));
        Draw.rrect(ctx, x, ty, 3, th, 1, cp(Draw.a(WHITE, 0.8f)));
    }

    // ---------- content (render + klik dalam satu jalur) ----------
    private void content(DrawContext ctx, int mx, int my, int btn, boolean click) {
        if (click && !inR(mx, my, mainX, cTop, mainW, cBot - cTop)) return;
        if (!click) ctx.enableScissor(mainX, cTop, mainX + mainW, cBot);
        int y = Math.round(cTop - scroll + (1f - pg) * 12f);
        int start = y;
        if (cfg != null) {
            y = rows(ctx, mx, my, btn, click, cfg.settings, y);
            y = extraBtns(ctx, mx, my, btn, click, y + 6, cfg.extraButtons());
        }
        else if (page == 0) y = cards(ctx, mx, my, btn, click, y);
        else if (page == 1) y = buttons(ctx, mx, my, btn, click, y);
        else { y = rows(ctx, mx, my, btn, click, List.of(NfConfig.ANIM, NfConfig.SPEED), y); y += 8; y = buttons(ctx, mx, my, btn, click, y); }
        if (!click) { ctx.disableScissor(); total = y - start; }
    }

    private List<HudElement> filtered() {
        String q = search == null ? "" : search.getText();
        String key = filter + "|" + q + "|" + HudManager.ELEMENTS.size();
        if (key.equals(cKey)) return cached;
        String ql = q.toLowerCase(Locale.ROOT);
        List<HudElement> l = new ArrayList<>();
        for (HudElement e : HudManager.ELEMENTS)
            if ((filter.equals("ALL") || e.category.equalsIgnoreCase(filter))
                    && (ql.isEmpty() || e.name.toLowerCase(Locale.ROOT).contains(ql) || e.desc.toLowerCase(Locale.ROOT).contains(ql))) l.add(e);
        cached = l; cKey = key;
        return l;
    }

    private int cards(DrawContext ctx, int mx, int my, int btn, boolean click, int y) {
        List<HudElement> l = filtered();
        int gap = 8, cols = Math.max(1, (mainW + gap) / 108), cw = (mainW - (cols - 1) * gap) / cols, ch = 106;
        for (int i = 0; i < l.size(); i++) {
            HudElement e = l.get(i);
            int x = mainX + (i % cols) * (cw + gap), yy = y + (i / cols) * (ch + gap);
            int ox = x + 6, oy = yy + ch - 40, ow = cw - 12;
            int ex = x + 6, ey = yy + ch - 22, ew = cw - 12;
            if (click) {
                if (btn != 0) continue;
                if (inR(mx, my, ox, oy, ow, 15)) { cfg = e; scroll = 0; pageAnim.set(0); return y; }
                if (inR(mx, my, ex, ey, ew, 17)) { e.enabled = !e.enabled; HudManager.saveAll(); return y; }
                continue;
            }
            if (yy + ch + 4 < cTop || yy - 4 > cBot) continue;          // culling: jangan gambar kartu di luar layar
            boolean hov = inR(mx, my, x, yy, cw, ch) && my >= cTop && my < cBot;
            Anim h = an("h" + e.id, 0); h.to(hov ? 1f : 0f); float hv = h.get();
            int lift = Math.round(hv * 2f), cy = yy - lift;
            Draw.rrect(ctx, x, cy, cw, ch, 7, cp(Draw.lerp(0x66000000, 0x88202020, hv)));
            Draw.rborder(ctx, x, cy, cw, ch, 7, 1, cp(Draw.lerp(0x22FFFFFF, 0xCCFFFFFF, hv)));
            Draw.rrectGrad(ctx, x + 1, cy + 1, cw - 2, 14, 6, cp(Draw.a(WHITE, 0.10f + 0.06f * hv)), cp(Draw.a(WHITE, 0f)));
            txtB(ctx, e.category.toUpperCase(Locale.ROOT), x + 7, cy + 4, cp(0xFF8A8A8A));
            Draw.rrect(ctx, x + cw - 12, cy + 6, 6, 6, 3, cp(e.enabled ? 0xFF4ADE80 : 0xFF6B7280));
            Draw.rrect(ctx, x + cw / 2 - 20, cy + 15, 40, 40, 20, cp(Draw.a(WHITE, 0.06f + 0.10f * hv)));
            var m = ctx.getMatrices();
            m.pushMatrix(); m.translate(x + cw / 2f - 16f, cy + 19f); m.scale(2f, 2f);
            ctx.drawItem(e.iconStack(), 0, 0);
            m.popMatrix();
            if (!e.enabled) Draw.rrect(ctx, x + cw / 2 - 20, cy + 15, 40, 40, 20, cp(0x88101010));
            ctxt(ctx, Fonts.trim(textRenderer, e.name, cw - 8), x + cw / 2, cy + 56, cp(0xFFEDEDED));   // nama fitur dinaikkan
            boolean oh = inR(mx, my, ox, oy, ow, 15);
            Anim oa = an("o" + e.id, 0); oa.to(oh ? 1f : 0f); float ov = oa.get();
            Draw.rrect(ctx, ox, oy - lift, ow, 15, 5, cp(Draw.lerp(0x99262626, 0xFFEDEDED, ov)));
            ctxt(ctx, "OPTIONS", x + cw / 2, oy + 3 - lift, cp(ov > 0.6f ? 0xFF000000 : WHITE));
            Anim tg = an("t" + e.id, e.enabled ? 1f : 0f); tg.to(e.enabled ? 1f : 0f); float tv = tg.get();
            boolean eh = inR(mx, my, ex, ey, ew, 17);
            int top = Draw.lerp(0xFF7F2D2D, 0xFF35C27A, tv), bot = Draw.lerp(0xFF5E1F1F, 0xFF1F8F58, tv);
            if (eh) { top = Draw.lerp(top, WHITE, 0.12f); bot = Draw.lerp(bot, WHITE, 0.12f); }
            Draw.rrectGrad(ctx, ex, ey - lift, ew, 17, 5, cp(top), cp(bot));
            ctxtB(ctx, e.enabled ? "ENABLED" : "DISABLED", x + cw / 2, ey + 4 - lift, cp(WHITE));
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
                        case ENUM -> { if (btn == 0 || btn == 1) { s.cycle(btn == 1 ? -1 : 1); HudManager.saveAll(); } }
                        case COLOR -> { if (btn == 0) openPicker(s); else if (btn == 1) { s.cycle(-1); HudManager.saveAll(); } }
                        case NUM -> { if (btn == 0) { dragX1 = right - 156; dragX2 = right - 52; drag = s; slide(mx); } }
                    }
                    return y;
                }
                y += rh; continue;
            }
            if (y + rh < cTop || y > cBot) { y += rh; idx++; continue; }
            boolean hov = inR(mx, my, mainX, y, mainW, rh) && my >= cTop && my < cBot;
            Anim ha = an("r" + System.identityHashCode(s), 0); ha.to(hov ? 1f : 0f);
            Draw.rrect(ctx, mainX, y, mainW, rh - 3, 6, cp(Draw.lerp((idx & 1) == 0 ? 0x33000000 : 0x1F000000, 0x44FFFFFF, ha.get() * 0.5f)));
            txt(ctx, s.name, mainX + 8, y + 6, cp(0xFFE5E5E5));
            switch (s.type) {
                case BOOL -> {
                    Anim tg = an("s" + System.identityHashCode(s), s.b ? 1f : 0f); tg.to(s.b ? 1f : 0f); float tv = tg.get();
                    Draw.rrect(ctx, right - 38, y + 4, 28, 14, 7, cp(Draw.lerp(0xFF3A3A3A, 0xFFEDEDED, tv)));
                    Draw.rrect(ctx, right - 36 + Math.round(tv * 14), y + 6, 10, 10, 5, cp(Draw.lerp(WHITE, 0xFF111111, tv)));
                }
                case NUM -> {
                    int x1 = right - 156, x2 = right - 52;
                    float tt = (s.f - s.min) / (s.max - s.min);
                    int kx = x1 + Math.round((x2 - x1) * tt);
                    Draw.rrect(ctx, x1, y + 8, x2 - x1, 5, 2, cp(0xFF2A2A2A));
                    Draw.rrectGrad(ctx, x1, y + 8, Math.max(3, kx - x1), 5, 2, cp(WHITE), cp(0xFF9A9A9A));
                    Draw.rrect(ctx, kx - 6, y + 4, 12, 12, 6, cp(Draw.a(WHITE, 0.28f)));
                    Draw.rrect(ctx, kx - 4, y + 6, 8, 8, 4, cp(WHITE));
                    Draw.rrect(ctx, x2 + 8, y + 4, right - x2 - 14, 14, 5, cp(0x55000000));
                    ctxt(ctx, s.valueText(), x2 + 8 + (right - x2 - 14) / 2, y + 6, cp(0xFFD1D5DB));
                }
                case COLOR -> {
                    Draw.rrect(ctx, right - 130, y + 3, 120, 17, 6, cp(hov ? 0xFF303030 : 0xFF262626));
                    Draw.rrect(ctx, right - 126, y + 5, 14, 13, 4, cp(0xFF505050));
                    Draw.rrect(ctx, right - 125, y + 6, 12, 11, 3, cp(0xFF202020));
                    Draw.rrect(ctx, right - 125, y + 6, 12, 11, 3, cp(s.c));
                    txt(ctx, s.valueText(), right - 106, y + 6, cp(0xFFBDBDBD));
                }
                case ENUM -> {
                    Draw.rrect(ctx, right - 140, y + 2, 132, 17, 6, cp(hov ? 0xFF343434 : 0xFF262626));
                    ctxt(ctx, Fonts.trim(textRenderer, s.str(), 118), right - 74, y + 5, cp(WHITE));
                }
            }
            y += rh; idx++;
        }
        return y;
    }

    private int extraBtns(DrawContext ctx, int mx, int my, int btn, boolean click, int y, List<HudElement.ExtraBtn> list) {
        for (HudElement.ExtraBtn b : list) {
            if (click) {
                if (btn == 0 && inR(mx, my, mainX, y, mainW, 24)) { b.run().run(); HudManager.saveAll(); return y; }
            } else {
                boolean hov = inR(mx, my, mainX, y, mainW, 24) && my >= cTop && my < cBot;
                Anim h = an("x" + b.label(), 0); h.to(hov ? 1f : 0f); float hv = h.get();
                Draw.rrect(ctx, mainX, y, mainW, 24, 7, cp(Draw.lerp(b.active() ? 0xAA3A3A3A : 0x55000000, 0xAA555555, hv)));
                if (b.active()) Draw.rrect(ctx, mainX + 4, y + 5, 3, 14, 1, cp(WHITE));
                txt(ctx, Fonts.trim(textRenderer, b.label(), mainW - 24), mainX + 14, y + 6, cp(WHITE));
            }
            y += 28;
        }
        return y;
    }

    private List<Btn> buttonsFor() {
        List<Btn> l = new ArrayList<>();
        if (page == 1) {
            for (String n : Themes.ALL.keySet())
                l.add(new Btn(n, n.equals(NfConfig.theme), () -> { Themes.apply(n); status = "Theme applied: " + n; }));
        } else if (page == 2) {
            l.add(new Btn("Open NextFam Pack Studio", false, () -> client.setScreen(new PackStudioScreen(this))));
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
                int base = b.active() ? 0xAA3A3A3A : 0x55000000;
                Draw.rrect(ctx, mainX, y, mainW, 24, 7, cp(Draw.lerp(base, 0xAA555555, hv)));
                if (b.active()) { Draw.rrect(ctx, mainX + 4, y + 5, 3, 14, 1, cp(WHITE)); txtB(ctx, "ACTIVE", mainX + mainW - 46, y + 7, cp(0xFF4ADE80)); }
                txt(ctx, b.label(), mainX + 14, y + 6, cp(WHITE));
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

    // ---------- color picker (palet bebas + favorit) ----------
    private static final int PKW = 244, PKH = 196, GC = 24, GR = 12, CELL = 7;
    private int pkX() { return px + (pw - PKW) / 2; }
    private int pkY() { return py + (ph - PKH) / 2; }

    private void openPicker(Setting s) {
        pick = s; float[] f = Draw.toHsv(s.c); hH = f[0]; hS = f[1]; hV = f[2]; hA = f[3]; pickDrag = 0;
    }
    private void applyPick() { pick.c = Draw.hsv(hH, hS, hV, hA); }
    private void closePicker() { pick = null; pickDrag = 0; HudManager.saveAll(); }

    private void pickerRender(DrawContext ctx, int mx, int my) {
        ctx.fill(px, py, px + pw, py + ph, 0xAA000000);
        int x0 = pkX(), y0 = pkY(), gx = x0 + 10, gy = y0 + 26;
        Draw.rrect(ctx, x0, y0, PKW, PKH, 8, 0xFF121212);
        Draw.rborder(ctx, x0, y0, PKW, PKH, 8, 1, 0x55FFFFFF);
        txtB(ctx, pick.name, x0 + 10, y0 + 8, WHITE);
        // saturation / brightness
        for (int j = 0; j < GR; j++) for (int i = 0; i < GC; i++)
            ctx.fill(gx + i * CELL, gy + j * CELL, gx + (i + 1) * CELL, gy + (j + 1) * CELL, Draw.hsv(hH, (i + 0.5f) / GC, 1f - (j + 0.5f) / GR, 1f));
        int mxk = gx + Math.round(hS * GC * CELL), myk = gy + Math.round((1f - hV) * GR * CELL);
        Draw.rborder(ctx, mxk - 3, myk - 3, 7, 7, 3, 1, 0xFFFFFFFF); Draw.rborder(ctx, mxk - 2, myk - 2, 5, 5, 2, 1, 0xFF000000);
        // hue
        int hy = gy + GR * CELL + 6;
        for (int i = 0; i < 42; i++) ctx.fill(gx + i * 4, hy, gx + i * 4 + 4, hy + 8, Draw.hsv(i / 42f, 1f, 1f, 1f));
        ctx.fill(gx + Math.round(hH * 168) - 1, hy - 2, gx + Math.round(hH * 168) + 1, hy + 10, WHITE);
        // alpha
        int ay = hy + 14;
        ctx.fill(gx, ay, gx + 168, ay + 8, 0xFF2A2A2A);
        for (int i = 0; i < 42; i++) ctx.fill(gx + i * 4, ay, gx + i * 4 + 4, ay + 8, Draw.hsv(hH, hS, hV, i / 41f));
        ctx.fill(gx + Math.round(hA * 168) - 1, ay - 2, gx + Math.round(hA * 168) + 1, ay + 10, WHITE);
        // palet
        txt(ctx, "PALETTE", gx, ay + 12, GRAY);
        int py0 = ay + 24;
        for (int i = 0; i < Setting.PALETTE.length; i++) {
            int sx = gx + (i % 14) * 13, sy = py0 + (i / 14) * 13;
            Draw.rrect(ctx, sx, sy, 11, 11, 3, 0xFF444444); Draw.rrect(ctx, sx + 1, sy + 1, 9, 9, 2, 0xFF202020); Draw.rrect(ctx, sx + 1, sy + 1, 9, 9, 2, Setting.PALETTE[i]);
        }
        // favorit
        int fy = py0 + 30;
        txt(ctx, "MY COLORS (right-click removes)", gx, fy, GRAY);
        for (int i = 0; i < 14; i++) {
            int sx = gx + i * 13, sy = fy + 11;
            boolean has = i < NfConfig.FAVS.size();
            Draw.rrect(ctx, sx, sy, 11, 11, 3, has ? 0xFF444444 : 0xFF222222);
            if (has) { Draw.rrect(ctx, sx + 1, sy + 1, 9, 9, 2, 0xFF202020); Draw.rrect(ctx, sx + 1, sy + 1, 9, 9, 2, NfConfig.FAVS.get(i)); }
        }
        // kolom kanan
        int rx = gx + 178;
        Draw.rrect(ctx, rx, gy, 46, 36, 5, 0xFF444444); Draw.rrect(ctx, rx + 1, gy + 1, 44, 34, 4, 0xFF202020); Draw.rrect(ctx, rx + 1, gy + 1, 44, 34, 4, pick.c);
        String hex = String.format("#%08X", pick.c);
        txt(ctx, hex.substring(0, 3), rx, gy + 40, GRAY); txt(ctx, hex.substring(3), rx, gy + 50, WHITE);
        btnSmall(ctx, mx, my, "SAVE", rx, gy + 66, 46, 15, false);
        btnSmall(ctx, mx, my, "DONE", rx, gy + 86, 46, 15, true);
    }

    private void btnSmall(DrawContext ctx, int mx, int my, String s, int x, int y, int w, int h, boolean strong) {
        boolean hov = inR(mx, my, x, y, w, h);
        Draw.rrect(ctx, x, y, w, h, 4, strong ? (hov ? WHITE : 0xFFE0E0E0) : (hov ? 0xFF3C3C3C : 0xFF2A2A2A));
        ctxt(ctx, s, x + w / 2, y + 3, strong ? 0xFF000000 : WHITE);
    }

    private boolean pickerClick(double mx, double my, int btn) {
        int x0 = pkX(), y0 = pkY(), gx = x0 + 10, gy = y0 + 26, hy = gy + GR * CELL + 6, ay = hy + 14, py0 = ay + 24, fy = py0 + 30, rx = gx + 178;
        if (!inR(mx, my, x0, y0, PKW, PKH)) { closePicker(); return true; }
        if (inR(mx, my, gx, gy, GC * CELL, GR * CELL)) { pickDrag = 1; pickerDrag(mx, my); return true; }
        if (inR(mx, my, gx, hy - 2, 168, 12)) { pickDrag = 2; pickerDrag(mx, my); return true; }
        if (inR(mx, my, gx, ay - 2, 168, 12)) { pickDrag = 3; pickerDrag(mx, my); return true; }
        for (int i = 0; i < Setting.PALETTE.length; i++) {
            if (inR(mx, my, gx + (i % 14) * 13, py0 + (i / 14) * 13, 11, 11)) {
                float[] f = Draw.toHsv(Setting.PALETTE[i]); hH = f[0]; hS = f[1]; hV = f[2]; hA = f[3]; applyPick(); return true;
            }
        }
        for (int i = 0; i < NfConfig.FAVS.size() && i < 14; i++) {
            if (inR(mx, my, gx + i * 13, fy + 11, 11, 11)) {
                if (btn == 1) { NfConfig.FAVS.remove(i); NfConfig.save(); }
                else { float[] f = Draw.toHsv(NfConfig.FAVS.get(i)); hH = f[0]; hS = f[1]; hV = f[2]; hA = f[3]; applyPick(); }
                return true;
            }
        }
        if (inR(mx, my, rx, gy + 66, 46, 15)) {
            Integer c = pick.c;
            if (!NfConfig.FAVS.contains(c) && NfConfig.FAVS.size() < 14) { NfConfig.FAVS.add(c); NfConfig.save(); status = "Color saved to My Colors"; }
            return true;
        }
        if (inR(mx, my, rx, gy + 86, 46, 15)) { closePicker(); return true; }
        return true;
    }

    private void pickerDrag(double mx, double my) {
        int x0 = pkX(), y0 = pkY(), gx = x0 + 10, gy = y0 + 26;
        if (pickDrag == 1) { hS = (float) Math.max(0, Math.min(1, (mx - gx) / (GC * CELL))); hV = (float) (1 - Math.max(0, Math.min(1, (my - gy) / (GR * CELL)))); }
        else if (pickDrag == 2) hH = (float) Math.max(0, Math.min(0.999, (mx - gx) / 168.0));
        else if (pickDrag == 3) hA = (float) Math.max(0, Math.min(1, (mx - gx) / 168.0));
        applyPick();
    }

    // ---------- input ----------
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        layout();
        double mx = click.x(), my = click.y(); int btn = click.button();
        if (pick != null) return pickerClick(mx, my, btn);
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
            if (y + 15 > sb - 68) break;
            if (inR(mx, my, sx + 4, y, SW - 8, 15)) { HudManager.loadProfile(prof.get(i)); status = "Loaded profile " + prof.get(i); return true; }
        }
        if (inR(mx, my, sx + 4, sb - 62, SW - 8, 15)) { status = "Created " + HudManager.newProfile(); return true; }
        if (inR(mx, my, sx + 4, sb - 42, SW - 8, 16)) { client.setScreen(new PackStudioScreen(this)); return true; }
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
        if (pick != null && pickDrag != 0) { pickerDrag(click.x(), click.y()); return true; }
        if (drag != null) { slide(click.x()); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        pickDrag = 0;
        if (drag != null) { drag = null; HudManager.saveAll(); }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (pick != null) return true;
        float max = Math.max(0f, total - (cBot - cTop));
        scroll = Math.max(0f, Math.min(max, scroll - (float) v * 22f));
        return true;
    }

    @Override public void removed() { HudManager.saveAll(); }
    @Override public void close() { if (pick != null) { closePicker(); return; } client.setScreen(null); }
}
