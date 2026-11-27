package com.nextfam.mixin;

import com.nextfam.hud.Features;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "removeEntity", at = @At("HEAD"), cancellable = true)
    private void nextfam$hold(int id, Entity.RemovalReason reason, CallbackInfo ci) {
        if (Features.holdBody((ClientWorld) (Object) this, id, reason)) ci.cancel();
    }
}
