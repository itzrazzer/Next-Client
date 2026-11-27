package com.nextfam.mixin;

import com.nextfam.util.Freelook;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Camera.class)
public class CameraMixin {
    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float nextfam$yaw(float yaw) { return Freelook.mode == 0 ? yaw : Freelook.yaw(yaw); }

    @ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float nextfam$pitch(float pitch) { return Freelook.mode == 0 ? pitch : Freelook.pitch(pitch); }
}
