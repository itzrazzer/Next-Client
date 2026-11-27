package com.nextfam.mixin;

import com.nextfam.util.Freelook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void nextfam$freelook(double dx, double dy, CallbackInfo ci) {
        if (Freelook.mode == 1 && (Object) this == MinecraftClient.getInstance().player) {
            Freelook.yaw += (float) dx * 0.15f;
            Freelook.pitch = Math.max(-90f, Math.min(90f, Freelook.pitch + (float) dy * 0.15f));
            ci.cancel();
        }
    }
}
