package com.nextfam.gui;

import com.nextfam.hud.HudElement;
import com.nextfam.hud.HudManager;
import com.nextfam.util.Draw;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public class HudEditorScreen extends Screen {
    private static final int PURPLE = 0xFFA855F7, SNAP = 6, BW = 58;
    private static final String[] ROW1 = {"SAVE", "CANCEL", "RESET", "PREVIEW", "LOCK ALL"};
    private static final String[] ROW2 = {"LOCK", "HIDE", "FRONT", "BACK", "GRID"};

    private final Screen parent;
    private final Map<HudElement, float[]> backup = new LinkedHashMap<>();
    private final List<HudElement> orderBackup = new ArrayList<>(HudManager.ELEMENTS);
    private HudElement selected;
    private int grabX, grabY;
    private boolean preview, grid;
    private final List<Integer> gx = new ArrayList<>(), gy = new ArrayList<>();

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
        for (HudElement e : HudManager.ELEMENTS)
            backup.put(e, new float[]{e.x, e.y, e.scale.f, e.opacity.f, e.enabled ? 1 : 0, e.locked ? 1 : 0});
    }

    @Override protected void init() { HudManager.editing = true; }
    @Override public void removed() { HudManager.editing = false; }

    private int bx(int i) { return (width - 5 * (BW + 4)) / 2 + i * (BW + 4); }
    private int by(int row) { return height - 44 + row * 22; }
    private boolean in(int row, int i, double mx, double my) { return mx >= bx(i) && mx < bx(i) + BW && my >= by(row) && my < by(row) + 18; }
    private boolean shift() {
        long w = client.getWindow().getHandle();
        return GLFW.glfwGetKey(w, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(w, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, preview ? 0x00000000 : 0x77000000);
        if (grid && !preview) {
            for (int x = 0; x < width; x += 10) ctx.fill(x, 0, x + 1, height, 0x14FFFFFF);
            for (int y = 0; y < height; y += 10) ctx.fill(0, y, width, y + 1, 0x14FFFFFF);
        }
        for (HudElement e : HudManager.ELEMENTS) {
            if (!e.enabled || e.overlay) continue;
            HudManager.fit(e, width, height);
            HudManager.draw(ctx, e);
            if (preview) continue;
            int c = e == selected ? PURPLE : 0x66FFFFFF;
            Draw.rborder(ctx, e.x, e.y, e.sw(), e.sh(), 0, 1, c);
            if (e == selected) Draw.rborder(ctx, e.x - 1, e.y - 1, e.sw() + 2, e.sh() + 2, 0, 1, Draw.a(PURPLE, 0.4f));
            ctx.drawText(textRenderer, e.name + (e.locked ? " [LOCKED]" : ""), e.x, e.y - 10, c, true);
        }
        if (preview) { ctx.drawText(textRenderer, "Preview - click anywhere to go back", 6, 6, 0xFFFFFFFF, true); return; }
        if (selected != null)
            ctx.drawText(textRenderer, selected.sw() + "x" + selected.sh() + "   x:" + selected.x + " y:" + selected.y
                    + "   scale " + String.format(Locale.ROOT, "%.2f", selected.scale.f)
                    + "   opacity " + String.format(Locale.ROOT, "%.2f", selected.opacity.f),
                    selected.x, selected.y + selected.sh() + 3, 0xFFFFFFFF, true);
        for (int x : gx) ctx.fill(x, 0, x + 1, height, PURPLE);
        for (int y : gy) ctx.fill(0, y, width, y + 1, PURPLE);
        for (int i = 0; i < 5; i++) button(ctx, 0, i, ROW1[i], mx, my, false);
        for (int i = 0; i < 5; i++) {
            boolean act = (i == 0 && selected != null && selected.locked) || (i == 4 && grid);
            button(ctx, 1, i, ROW2[i], mx, my, act);
        }
        ctx.drawText(textRenderer, "Drag: move  |  Scroll: scale  |  Shift+Scroll: opacity", width / 2 - 140, by(0) - 12, 0xFFAAAAAA, false);
    }

    private void button(DrawContext ctx, int row, int i, String label, int mx, int my, boolean active) {
        boolean h = in(row, i, mx, my);
        Draw.rrect(ctx, bx(i), by(row), BW, 18, 3, active ? 0xFF7C3AED : h ? 0xFF2E2E3D : 0xFF1F1F29);
        ctx.drawText(textRenderer, label, bx(i) + (BW - textRenderer.getWidth(label)) / 2, by(row) + 5, 0xFFFFFFFF, false);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (preview) { preview = false; return true; }
        if (click.button() != 0) return false;
        if (in(0, 0, mx, my)) { HudManager.saveAll(); close(); return true; }
        if (in(0, 1, mx, my)) { cancel(); return true; }
        if (in(0, 2, mx, my)) { for (HudElement e : HudManager.ELEMENTS) { e.resetPos(); } return true; }
        if (in(0, 3, mx, my)) { preview = true; return true; }
        if (in(0, 4, mx, my)) {
            boolean all = HudManager.ELEMENTS.stream().allMatch(e -> e.locked);
            for (HudElement e : HudManager.ELEMENTS) e.locked = !all;
            return true;
        }
        if (in(1, 0, mx, my)) { if (selected != null) selected.locked = !selected.locked; return true; }
        if (in(1, 1, mx, my)) { if (selected != null) { selected.enabled = false; selected = null; } return true; }
        if (in(1, 2, mx, my)) { if (selected != null) { HudManager.ELEMENTS.remove(selected); HudManager.ELEMENTS.add(selected); } return true; }
        if (in(1, 3, mx, my)) { if (selected != null) { HudManager.ELEMENTS.remove(selected); HudManager.ELEMENTS.add(0, selected); } return true; }
        if (in(1, 4, mx, my)) { grid = !grid; return true; }

        selected = null;
        List<HudElement> l = HudManager.ELEMENTS;
        for (int i = l.size() - 1; i >= 0; i--) {
            HudElement e = l.get(i);
            if (e.enabled && !e.overlay && mx >= e.x && mx < e.x + e.sw() && my >= e.y && my < e.y + e.sh()) {
                selected = e; grabX = (int) mx - e.x; grabY = (int) my - e.y; break;
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (selected == null || selected.locked || click.button() != 0) return false;
        int nx = (int) click.x() - grabX, ny = (int) click.y() - grabY;
        int w = selected.sw(), h = selected.sh();
        gx.clear(); gy.clear();
        if (grid) {
            nx = Math.round(nx / 10f) * 10; ny = Math.round(ny / 10f) * 10;
        } else {
            int cx = (width - w) / 2, cy = (height - h) / 2;
            if (Math.abs(nx - cx) <= SNAP) { nx = cx; gx.add(width / 2); }
            else if (Math.abs(nx) <= SNAP) nx = 0;
            else if (Math.abs(nx - (width - w)) <= SNAP) nx = width - w;
            if (Math.abs(ny - cy) <= SNAP) { ny = cy; gy.add(height / 2); }
            else if (Math.abs(ny) <= SNAP) ny = 0;
            else if (Math.abs(ny - (height - h)) <= SNAP) ny = height - h;
            for (HudElement o : HudManager.ELEMENTS) {
                if (o == selected || !o.enabled || o.overlay) continue;
                if (Math.abs(nx - o.x) <= SNAP) { nx = o.x; gx.add(o.x); }
                else if (Math.abs(nx + w - (o.x + o.sw())) <= SNAP) { nx = o.x + o.sw() - w; gx.add(o.x + o.sw()); }
                if (Math.abs(ny - o.y) <= SNAP) { ny = o.y; gy.add(o.y); }
                else if (Math.abs(ny + h - (o.y + o.sh())) <= SNAP) { ny = o.y + o.sh() - h; gy.add(o.y + o.sh()); }
            }
        }
        selected.x = nx; selected.y = ny;
        HudManager.fit(selected, width, height);
        return true;
    }

    @Override public boolean mouseReleased(Click click) { gx.clear(); gy.clear(); return super.mouseReleased(click); }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (selected == null) return false;
        if (shift()) selected.opacity.setF(selected.opacity.f + (float) v * 0.05f);
        else selected.scale.setF(selected.scale.f + (float) v * 0.1f);
        HudManager.fit(selected, width, height);
        return true;
    }

    private void cancel() {
        backup.forEach((e, b) -> {
            e.x = (int) b[0]; e.y = (int) b[1]; e.scale.f = b[2]; e.opacity.f = b[3]; e.enabled = b[4] == 1; e.locked = b[5] == 1;
        });
        HudManager.ELEMENTS.clear(); HudManager.ELEMENTS.addAll(orderBackup);
        close();
    }

    @Override public void close() { client.setScreen(parent); }
}
