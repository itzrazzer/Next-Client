package com.nextfam.mixin;

import com.nextfam.hud.Features;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void nextfam$upright(LivingEntity entity, LivingEntityRenderState state, float tp, CallbackInfo ci) {
        if (entity.deathTime > 0 && Features.uprightFor(entity)) {
            try { ((LivingStateAccessor) (Object) state).nextfam$setDeath(0f); } catch (Throwable ignored) {}
        }
    }
}
