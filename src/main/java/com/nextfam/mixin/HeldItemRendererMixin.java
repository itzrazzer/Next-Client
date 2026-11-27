package com.nextfam.mixin;

import com.nextfam.hud.Features;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Shadow private float equipProgressMainHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private ItemStack mainHand;

    @Inject(method = "updateHeldItems", at = @At("TAIL"))
    private void nextfam$classic(CallbackInfo ci) {
        var p = MinecraftClient.getInstance().player;
        if (p != null && Features.classicHand() && ItemStack.areEqual(this.mainHand, p.getMainHandStack())) {
            this.equipProgressMainHand = 1.0f;
            this.prevEquipProgressMainHand = 1.0f;
        }
    }
}
