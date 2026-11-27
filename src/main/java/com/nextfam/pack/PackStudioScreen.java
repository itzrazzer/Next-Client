package com.nextfam.pack;

import com.nextfam.util.Draw;
import com.nextfam.util.Fonts;
import com.nextfam.util.Logo;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.*;

/** NextFam Pack Studio: Import -> Browse -> Select -> Merge -> Export ZIP -> Apply. */
public class PackStudioScreen extends Screen {
    static final PackManager PM = new PackManager();
    static final TextureMerger MG = new TextureMerger();
    private static boolean scannedOnce;

    private static final int WHITE = 0xFFFFFFFF, DIM = 0xFF9A9A9A, PANEL = 0xF0121212, LINE = 0x33FFFFFF, WARN = 0xFFFB923C;

    private final Screen parent;
    private int cat = 0;
    private float scroll, packScroll;
    private String selected;
    private TextFieldWidget search, nameField;
    private String msg = "";
    private List<String> rows = List.of();
    private int rowsVer = -1; private int rowsCat = -1; private String rowsQ = "";
    private int conflicts; private int confVer = -1;
    private int pad = 8, leftW = 100, rightW, mainX, mainW, rightX, bodyTop, bodyBot;

    public PackStudioScreen(Screen parent) { super(Text.literal("NextFam Pack Studio")); this.parent = parent; }

    @Override
    protected void init() {
        String os = search == null ? "" : search.getText(), on = nameField == null ? "NextFam Custom Pack" : nameField.getText();
        search = new TextFieldWidget(textRenderer, 0, 0, 100, 14, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search textures..."));
        search.setText(os); addDrawableChild(search);
        nameField = new TextFieldWidget(textRenderer, 0, 0, 100, 14, Text.literal("Name"));
        nameField.setMaxLength(60); nameField.setText(on); addDrawableChild(nameField);
        if (!scannedOnce) { scannedOnce = true; PM.scanFolder(); }
    }

    private void layout() {
        rightW = Math.max(150, Math.min(190, width / 3));
        mainX = pad + leftW + 8; rightX = width - pad - rightW; mainW = rightX - 8 - mainX;
        bodyTop = pad + 30; bodyBot = height - pad;
    }

    private boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    private void t(DrawContext c, String s, int x, int y, int col) { c.drawText(textRenderer, Fonts.m(s), x, y, col, false); }
    private void tb(DrawContext c, String s, int x, int y, int col) { c.drawText(textRenderer, Fonts.b(s), x, y, col, false); }
    private void tc(DrawContext c, String s, int cx, int y, int col) { c.drawText(textRenderer, Fonts.m(s), cx - Fonts.w(textRenderer, s) / 2, y, col, false); }
    private void btn(DrawContext c, String s, int x, int y, int w, int h, boolean hov, boolean strong) {
        Draw.rrect(c, x, y, w, h, 4, strong ? (hov ? 0xFFFFFFFF : 0xFFE5E5E5) : (hov ? 0xFF3A3A3A : 0xFF262626));
        tc(c, s, x + w / 2, y + (h - 8) / 2 - 1, strong ? 0xFF000000 : WHITE);
    }
    private static char letter(PackSource p) { int i = PM.packs.indexOf(p); return (char) ('A' + Math.max(0, Math.min(25, i))); }

    private void refreshRows() {
        String q = search.getText();
        if (rowsVer != PM.indexVersion || rowsCat != cat || !rowsQ.equals(q)) {
            rows = TextureBrowser.filter(PM.index, TextureBrowser.CATS[cat], q);
            rowsVer = PM.indexVersion; rowsCat = cat; rowsQ = q;
        }
        if (confVer != PM.indexVersion) { conflicts = PackConflictManager.countConflicts(PM.index); confVer = PM.indexVersion; }
    }

    // ---------------- render ----------------
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        layout(); refreshRows();
        ctx.fill(0, 0, width, height, 0xF2060606);
        Draw.rrect(ctx, pad, pad, width - 2 * pad, 24, 6, PANEL);
        Logo.draw(ctx, pad + 4, pad - 1, 26, 1f);
        tb(ctx, "NEXTFAM PACK STUDIO", pad + 34, pad + 8, WHITE);
        // tombol header
        int bx = width - pad - 4;
        bx -= 52; btn(ctx, "CLOSE", bx, pad + 4, 52, 16, in(mx, my, bx, pad + 4, 52, 16), false);
        bx -= 58; btn(ctx, "RESCAN", bx, pad + 4, 54, 16, in(mx, my, bx, pad + 4, 54, 16), false);
        bx -= 84; btn(ctx, "IMPORT PACK", bx, pad + 4, 80, 16, in(mx, my, bx, pad + 4, 80, 16), true);

        left(ctx, mx, my);
        main(ctx, mx, my);
        right(ctx, mx, my);
        search.visible = true; search.setX(mainX); search.setY(bodyTop); search.setWidth(mainW);
        nameField.visible = true; nameField.setX(rightX + 6); nameField.setY(bodyBot - 70); nameField.setWidth(rightW - 12);
        search.render(ctx, mx, my, delta); nameField.render(ctx, mx, my, delta);
    }

    private void left(DrawContext ctx, int mx, int my) {
        Draw.rrect(ctx, pad, bodyTop, leftW, bodyBot - bodyTop, 6, PANEL);
        tb(ctx, "CATEGORIES", pad + 8, bodyTop + 6, DIM);
        for (int i = 0; i < TextureBrowser.CATS.length; i++) {
            int y = bodyTop + 20 + i * 16;
            boolean sel = i == cat, hov = in(mx, my, pad + 4, y, leftW - 8, 15);
            Draw.rrect(ctx, pad + 4, y, leftW - 8, 15, 4, sel ? 0xFFEDEDED : hov ? 0xFF2A2A2A : 0x00000000);
            t(ctx, TextureBrowser.CATS[i], pad + 10, y + 3, sel ? 0xFF000000 : WHITE);
        }
        int by = bodyBot - 74;
        btn(ctx, "AUTO-FILL (priority)", pad + 4, by, leftW - 8, 16, in(mx, my, pad + 4, by, leftW - 8, 16), false);
        btn(ctx, "MERGE PACKS", pad + 4, by + 20, leftW - 8, 16, in(mx, my, pad + 4, by + 20, leftW - 8, 16), true);
        btn(ctx, "CLEAR PICKS", pad + 4, by + 40, leftW - 8, 16, in(mx, my, pad + 4, by + 40, leftW - 8, 16), false);
    }

    private void main(DrawContext ctx, int mx, int my) {
        Draw.rrect(ctx, mainX - 4, bodyTop - 4, mainW + 8, bodyBot - bodyTop + 8, 6, PANEL);
        int top = bodyTop + 20, bot = bodyBot - 14;
        float max = Math.max(0, rows.size() * 15 - (bot - top)); scroll = Math.max(0, Math.min(max, scroll));
        int first = (int) (scroll / 15), off = -(int) (scroll % 15);
        ctx.enableScissor(mainX, top, mainX + mainW, bot);
        if (rows.isEmpty()) tc(ctx, PM.packs.isEmpty() ? "No packs yet - press IMPORT PACK or put .zip files in resourcepacks" : "No textures (select packs on the right)", mainX + mainW / 2, top + 20, DIM);
        for (int i = first; i < rows.size() && top + off + (i - first) * 15 < bot; i++) {
            String p = rows.get(i); int y = top + off + (i - first) * 15;
            List<PackSource> l = PM.index.get(p); if (l == null) continue;
            PackSource chosen = MG.picks.get(p);
            boolean sel = p.equals(selected), hov = in(mx, my, mainX, y, mainW, 15) && my >= top && my < bot;
            if (sel || hov) Draw.rrect(ctx, mainX, y, mainW, 14, 3, sel ? 0xFF2E2E2E : 0xFF1E1E1E);
            Draw.rborder(ctx, mainX + 3, y + 2, 10, 10, 2, 1, chosen != null ? WHITE : 0xFF555555);
            if (chosen != null) ctx.fill(mainX + 5, y + 4, mainX + 11, y + 10, WHITE);
            t(ctx, Fonts.trim(textRenderer, TextureBrowser.shortName(p), mainW - 30 - l.size() * 14 - 10), mainX + 18, y + 3, chosen != null ? WHITE : 0xFFBBBBBB);
            if (PackConflictManager.isConflict(l)) tb(ctx, "!", mainX + mainW - 14 * l.size() - 12, y + 3, WARN);
            for (int k = 0; k < l.size(); k++) {
                PackSource s = l.get(k); int tx = mainX + mainW - 14 * (l.size() - k) - 2;
                boolean on = s == chosen;
                Draw.rrect(ctx, tx, y + 1, 12, 12, 3, on ? WHITE : 0xFF2A2A2A);
                tc(ctx, String.valueOf(letter(s)), tx + 6, y + 3, on ? 0xFF000000 : DIM);
            }
        }
        ctx.disableScissor();
        t(ctx, rows.size() + " textures  |  " + MG.count() + " picked  |  " + conflicts + " conflicts", mainX, bodyBot - 10, DIM);
    }

    private void right(DrawContext ctx, int mx, int my) {
        Draw.rrect(ctx, rightX, bodyTop, rightW, bodyBot - bodyTop, 6, PANEL);
        tb(ctx, "PACKS (top = highest priority)", rightX + 8, bodyTop + 6, DIM);
        int ry = bodyTop + 20, vis = 5;
        List<PackSource> ps = new ArrayList<>(PM.packs);
        packScroll = Math.max(0, Math.min(Math.max(0, ps.size() - vis), packScroll));
        for (int i = (int) packScroll; i < ps.size() && i < (int) packScroll + vis; i++) {
            PackSource p = ps.get(i); int y = ry + (i - (int) packScroll) * 19;
            boolean on = PM.active.contains(p), hov = in(mx, my, rightX + 4, y, rightW - 8, 17);
            Draw.rrect(ctx, rightX + 4, y, rightW - 8, 17, 4, hov ? 0xFF222222 : 0xFF1A1A1A);
            Draw.rborder(ctx, rightX + 8, y + 3, 11, 11, 2, 1, on ? WHITE : 0xFF555555);
            if (on) ctx.fill(rightX + 10, y + 5, rightX + 17, y + 12, WHITE);
            tb(ctx, String.valueOf(letter(p)), rightX + 24, y + 4, WHITE);
            String info = p.error != null ? "invalid" : p.scanned ? p.entries().size() + " files" : "...";
            t(ctx, Fonts.trim(textRenderer, p.name, rightW - 110) , rightX + 36, y + 4, p.error != null ? WARN : WHITE);
            t(ctx, info, rightX + rightW - 70 - Fonts.w(textRenderer, info) + 20, y + 4, DIM);
            tc(ctx, "^", rightX + rightW - 28, y + 4, in(mx, my, rightX + rightW - 34, y, 12, 17) ? WHITE : DIM);
            tc(ctx, "v", rightX + rightW - 14, y + 4, in(mx, my, rightX + rightW - 20, y, 12, 17) ? WHITE : DIM);
        }
        if (ps.isEmpty()) t(ctx, "Copy a .zip path, then IMPORT PACK", rightX + 8, ry + 4, DIM);

        // preview
        int py = ry + vis * 19 + 6;
        tb(ctx, "PREVIEW", rightX + 8, py, DIM);
        int ps2 = Math.min(64, rightW - 100);
        if (selected != null && PM.index.get(selected) != null) {
            List<PackSource> l = PM.index.get(selected);
            PackSource chosen = MG.picks.get(selected); PackSource show = chosen != null ? chosen : l.get(0);
            Draw.rborder(ctx, rightX + 8, py + 12, ps2, ps2, 0, 1, 0x55FFFFFF);
            PackPreview.draw(ctx, PackPreview.load(show, selected), rightX + 8, py + 12, ps2);
            t(ctx, Fonts.trim(textRenderer, TextureBrowser.shortName(selected), rightW - ps2 - 26), rightX + ps2 + 16, py + 14, WHITE);
            t(ctx, "Using: " + (chosen == null ? "-" : chosen.name), rightX + ps2 + 16, py + 26, DIM);
            for (int k = 0; k < l.size() && k < 4; k++) {
                int tx = rightX + ps2 + 16 + (k % 2) * 30, ty = py + 40 + (k / 2) * 30;
                PackPreview.draw(ctx, PackPreview.load(l.get(k), selected), tx, ty, 24);
                tb(ctx, String.valueOf(letter(l.get(k))), tx + 26, ty + 2, WHITE);
            }
        } else t(ctx, "Select a texture to preview", rightX + 8, py + 16, DIM);

        // export
        int by = bodyBot - 50;
        t(ctx, "Pack name", rightX + 8, bodyBot - 82, DIM);
        btn(ctx, "EXPORT PACK", rightX + 6, by, (rightW - 16) / 2, 16, in(mx, my, rightX + 6, by, (rightW - 16) / 2, 16), false);
        btn(ctx, "EXPORT + APPLY", rightX + 10 + (rightW - 16) / 2, by, (rightW - 16) / 2, 16, in(mx, my, rightX + 10 + (rightW - 16) / 2, by, (rightW - 16) / 2, 16), true);
        String st = !PackExporter.status.isEmpty() ? PackExporter.status : !msg.isEmpty() ? msg : PM.status;
        t(ctx, Fonts.trim(textRenderer, st, rightW - 14), rightX + 8, bodyBot - 28, 0xFFD0D0D0);
        if (st.length() > 34) t(ctx, Fonts.trim(textRenderer, st.substring(Math.min(st.length(), 34)), rightW - 14), rightX + 8, bodyBot - 17, 0xFFD0D0D0);
    }

    // ---------------- input ----------------
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        layout();
        double mx = click.x(), my = click.y(); int b = click.button();
        for (TextFieldWidget f : new TextFieldWidget[]{search, nameField}) {
            if (in(mx, my, f.getX(), f.getY(), f.getWidth(), f.getHeight())) { setFocused(f); f.setFocused(true); return f.mouseClicked(click, doubled); }
        }
        search.setFocused(false); nameField.setFocused(false); setFocused(null);

        int bx = width - pad - 4;
        bx -= 52; if (in(mx, my, bx, pad + 4, 52, 16)) { close(); return true; }
        bx -= 58; if (in(mx, my, bx, pad + 4, 54, 16)) { PM.scanFolder(); msg = "Rescanning..."; return true; }
        bx -= 84; if (in(mx, my, bx, pad + 4, 80, 16)) { msg = PM.importPath(client.keyboard.getClipboard()); PackExporter.status = ""; return true; }

        for (int i = 0; i < TextureBrowser.CATS.length; i++)
            if (in(mx, my, pad + 4, bodyTop + 20 + i * 16, leftW - 8, 15)) { cat = i; scroll = 0; return true; }
        int by = bodyBot - 74; String cn = TextureBrowser.CATS[cat];
        if (in(mx, my, pad + 4, by, leftW - 8, 16)) { int n = PackConflictManager.autoResolve(PM.index, MG, cn, false); msg = "Auto-filled " + n + " textures by priority"; PackExporter.status = ""; return true; }
        if (in(mx, my, pad + 4, by + 20, leftW - 8, 16)) { int n = PackConflictManager.autoResolve(PM.index, MG, "All", true); msg = "Merged: resolved " + n + " conflicts by priority"; PackExporter.status = ""; return true; }
        if (in(mx, my, pad + 4, by + 40, leftW - 8, 16)) { MG.clear(); msg = "Picks cleared"; PackExporter.status = ""; return true; }

        // daftar tekstur
        int top = bodyTop + 20, bot = bodyBot - 14;
        if (in(mx, my, mainX, top, mainW, bot - top)) {
            int idx = (int) ((my - top + scroll) / 15);
            if (idx >= 0 && idx < rows.size()) {
                String p = rows.get(idx); List<PackSource> l = PM.index.get(p);
                if (l == null) return true;
                if (b == 1) { MG.unpick(p); return true; }
                for (int k = 0; k < l.size(); k++) {
                    int tx = mainX + mainW - 14 * (l.size() - k) - 2;
                    if (mx >= tx && mx < tx + 12) { PackSource s = l.get(k); if (MG.picks.get(p) == s) MG.unpick(p); else MG.pick(p, s); selected = p; return true; }
                }
                if (mx < mainX + 16) { if (MG.picks.containsKey(p)) MG.unpick(p); else MG.pick(p, l.get(0)); }
                selected = p;
            }
            return true;
        }

        // daftar pack
        int ry = bodyTop + 20;
        List<PackSource> ps = new ArrayList<>(PM.packs);
        for (int i = (int) packScroll; i < ps.size() && i < (int) packScroll + 5; i++) {
            int y = ry + (i - (int) packScroll) * 19;
            if (in(mx, my, rightX + rightW - 34, y, 12, 17)) { PM.move(i, -1); return true; }
            if (in(mx, my, rightX + rightW - 20, y, 12, 17)) { PM.move(i, 1); return true; }
            if (in(mx, my, rightX + 4, y, rightW - 40, 17)) { PM.toggle(ps.get(i)); return true; }
        }

        int ey = bodyBot - 50, bw = (rightW - 16) / 2;
        if (in(mx, my, rightX + 6, ey, bw, 16)) { PackExporter.export(nameField.getText(), MG.picks, false); return true; }
        if (in(mx, my, rightX + 10 + bw, ey, bw, 16)) { PackExporter.export(nameField.getText(), MG.picks, true); return true; }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        layout();
        if (in(mx, my, rightX, bodyTop, rightW, 20 + 5 * 19)) packScroll -= (float) v;
        else scroll -= (float) v * 30f;
        return true;
    }

    @Override public void close() { client.setScreen(parent); }
}
