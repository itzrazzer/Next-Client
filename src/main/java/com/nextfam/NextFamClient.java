package com.nextfam;

import com.nextfam.config.NfConfig;
import com.nextfam.gui.MenuScreen;
import com.nextfam.pack.PackStudioScreen;
import com.nextfam.hud.CombatTracker;
import com.nextfam.hud.Features;
import com.nextfam.hud.HudManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.lwjgl.glfw.GLFW;

public class NextFamClient implements ClientModInitializer {
    private boolean wasDown = false, wasPack = false;

    @Override
    public void onInitializeClient() {
        HudManager.init();
        CombatTracker.init();
        Features.init();
        HudRenderCallback.EVENT.register((ctx, tick) -> HudManager.render(ctx, tick));

        // Right Shift membuka menu (kode tombol bisa diubah di config/nextfam/keybinds.json)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) { wasDown = false; wasPack = true; return; }
            boolean down = GLFW.glfwGetKey(client.getWindow().getHandle(), NfConfig.menuKey) == GLFW.GLFW_PRESS;
            if (down && !wasDown) client.setScreen(new MenuScreen());
            wasDown = down;
            boolean pk = NfConfig.packKey > 0 && GLFW.glfwGetKey(client.getWindow().getHandle(), NfConfig.packKey) == GLFW.GLFW_PRESS;
            if (pk && !wasPack) client.setScreen(new PackStudioScreen(null));
            wasPack = pk;
        });
    }
}
