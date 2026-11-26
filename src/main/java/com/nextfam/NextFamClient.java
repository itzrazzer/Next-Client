package com.nextfam;

import com.nextfam.config.NfConfig;
import com.nextfam.gui.MenuScreen;
import com.nextfam.hud.CombatTracker;
import com.nextfam.hud.HudManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.lwjgl.glfw.GLFW;

public class NextFamClient implements ClientModInitializer {
    private boolean wasDown = false;

    @Override
    public void onInitializeClient() {
        HudManager.init();
        CombatTracker.init();
        HudRenderCallback.EVENT.register((ctx, tick) -> HudManager.render(ctx));

        // Shift + Klik Kanan membuka menu (bisa diubah di config/nextfam/keybinds.json)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) { wasDown = false; return; }
            long w = client.getWindow().getHandle();
            boolean shift = !NfConfig.menuShift
                    || GLFW.glfwGetKey(w, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(w, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            boolean btn = GLFW.glfwGetMouseButton(w, NfConfig.menuButton) == GLFW.GLFW_PRESS;
            boolean down = shift && btn;
            if (down && !wasDown) client.setScreen(new MenuScreen());
            wasDown = down;
        });
    }
}
