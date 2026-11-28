package com.nextfam.mixin;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Mengisi nilai opsi tanpa validator (dipakai Fullbright mode Gamma). */
@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor {
    @Mutable
    @Accessor("value")
    void nextfam$setValue(Object v);
}
