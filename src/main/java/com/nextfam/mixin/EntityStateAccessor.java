package com.nextfam.mixin;

import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderState.class)
public interface EntityStateAccessor {
    @Accessor("displayName")
    Text nextfam$name();

    @Mutable
    @Accessor("displayName")
    void nextfam$setName(Text t);
}
