package com.nextfam.mixin;

import com.nextfam.util.LoadingScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {
    @Shadow private float progress;

    @Inject(method = "render", at = @At("TAIL"))
    private void nextfam$render(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        LoadingScreen.draw(ctx, this.progress);
    }
}
