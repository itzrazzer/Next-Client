package com.nextfam.hud;

import java.util.ArrayDeque;
import org.lwjgl.glfw.GLFW;

public final class ClickTracker {
    private static final ArrayDeque<Long> LMB = new ArrayDeque<>(), RMB = new ArrayDeque<>();
    private static boolean lw, rw;
    public static boolean clicked;

    public static void poll(long window) {
        long now = System.currentTimeMillis();
        boolean l = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        clicked = (l && !lw) || (r && !rw);
        if (l && !lw) LMB.add(now);
        if (r && !rw) RMB.add(now);
        lw = l; rw = r;
        while (!LMB.isEmpty() && now - LMB.peekFirst() > 1000) LMB.pollFirst();
        while (!RMB.isEmpty() && now - RMB.peekFirst() > 1000) RMB.pollFirst();
    }
    public static int left() { return LMB.size(); }
    public static int right() { return RMB.size(); }
}
