package com.nextfam.gui;

import com.nextfam.config.NfConfig;
import com.nextfam.util.Draw;
import com.nextfam.util.Fonts;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Editor crosshair: gambar piksel demi piksel (kiri = isi, kanan = hapus). */
public class PixelCrosshairScreen extends Screen {
    private final Screen parent;
    private boolean paint = true;
    private int cell = 16, gx, gy;

    public PixelCrosshairScreen(Screen parent) { super(Text.literal("Crosshair Editor")); this.parent = parent; }

    private void geo() { cell = Math.max(8, Math.min(16, (height - 120) / 15)); gx = (width - 15 * cell) / 2; gy = 44; }
    private boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        geo();
        ctx.fill(0, 0, width, height, 0xE6000000);
        ctx.drawText(textRenderer, Fonts.b("CROSSHAIR EDITOR"), width / 2 - Fonts.wb(textRenderer, "CROSSHAIR EDITOR") / 2, 14, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, Fonts.m("Left click: draw   |   Right click: erase   |   drag to paint"), width / 2 - Fonts.w(textRenderer, "Left click: draw   |   Right click: erase   |   drag to paint") / 2, 28, 0xFF9A9A9A, false);
        Draw.rrect(ctx, gx - 6, gy - 6, 15 * cell + 12, 15 * cell + 12, 6, 0xFF141414);
        for (int r = 0; r < 15; r++) for (int c = 0; c < 15; c++) {
            int x = gx + c * cell, y = gy + r * cell;
            boolean on = NfConfig.CROSS[r][c];
            ctx.fill(x, y, x + cell - 1, y + cell - 1, on ? 0xFFFFFFFF : ((r + c) % 2 == 0 ? 0xFF1E1E1E : 0xFF262626));
        }
        ctx.fill(gx + 7 * cell, gy, gx + 7 * cell + 1, gy + 15 * cell, 0x22FFFFFF);
        ctx.fill(gx, gy + 7 * cell, gx + 15 * cell, gy + 7 * cell + 1, 0x22FFFFFF);
        String[] b = {"CLEAR", "PLUS", "DOT", "DONE"};
        for (int i = 0; i < 4; i++) {
            int bx = width / 2 - 2 * 62 + i * 62, by = gy + 15 * cell + 14;
            boolean h = in(mx, my, bx, by, 58, 18);
            Draw.rrect(ctx, bx, by, 58, 18, 5, i == 3 ? (h ? 0xFFFFFFFF : 0xFFE0E0E0) : (h ? 0xFF3A3A3A : 0xFF262626));
            ctx.drawText(textRenderer, Fonts.m(b[i]), bx + 29 - Fonts.w(textRenderer, b[i]) / 2, by + 5, i == 3 ? 0xFF000000 : 0xFFFFFFFF, false);
        }
    }

    private void setCell(double mx, double my, boolean v) {
        int c = (int) Math.floor((mx - gx) / cell), r = (int) Math.floor((my - gy) / cell);
        if (c >= 0 && c < 15 && r >= 0 && r < 15) NfConfig.CROSS[r][c] = v;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        geo(); double mx = click.x(), my = click.y();
        for (int i = 0; i < 4; i++) {
            int bx = width / 2 - 2 * 62 + i * 62, by = gy + 15 * cell + 14;
            if (!in(mx, my, bx, by, 58, 18)) continue;
            switch (i) {
                case 0 -> { for (boolean[] row : NfConfig.CROSS) java.util.Arrays.fill(row, false); }
                case 1 -> { for (boolean[] row : NfConfig.CROSS) java.util.Arrays.fill(row, false); for (int k = 2; k <= 6; k++) { NfConfig.CROSS[7][7 - k] = true; NfConfig.CROSS[7][7 + k] = true; NfConfig.CROSS[7 - k][7] = true; NfConfig.CROSS[7 + k][7] = true; } }
                case 2 -> { for (boolean[] row : NfConfig.CROSS) java.util.Arrays.fill(row, false); NfConfig.CROSS[7][7] = true; }
                default -> close();
            }
            return true;
        }
        paint = click.button() == 0;
        setCell(mx, my, paint);
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) { setCell(click.x(), click.y(), paint); return true; }

    @Override public void removed() { NfConfig.saveCross(); }
    @Override public void close() { NfConfig.saveCross(); client.setScreen(parent); }
}
