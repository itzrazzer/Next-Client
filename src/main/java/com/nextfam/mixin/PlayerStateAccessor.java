package com.nextfam.mixin;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntityRenderState.class)
public interface PlayerStateAccessor {
    @Accessor("skinTextures")
    SkinTextures nextfam$skin();

    @Mutable
    @Accessor("skinTextures")
    void nextfam$setSkin(SkinTextures s);
}
