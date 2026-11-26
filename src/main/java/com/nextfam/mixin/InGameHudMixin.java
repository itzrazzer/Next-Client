package com.nextfam.mixin;

import com.nextfam.hud.Features;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void nextfam$noPumpkin(DrawContext ctx, Identifier texture, float opacity, CallbackInfo ci) {
        if (texture != null && texture.getPath().contains("pumpkinblur") && Features.on("nopumpkin")) ci.cancel();
    }
}
