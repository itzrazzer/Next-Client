package com.nextfam.mixin;

import com.nextfam.hud.Features;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true)
    private void nextfam$totem(ItemStack stack, CallbackInfo ci) {
        if (stack.isOf(Items.TOTEM_OF_UNDYING) && Features.hideTotemPop()) ci.cancel();
    }
}
