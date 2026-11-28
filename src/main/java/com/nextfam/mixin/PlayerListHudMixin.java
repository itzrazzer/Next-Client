package com.nextfam.mixin;

import com.nextfam.hud.Cosmetics;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void nextfam$logo(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (!Cosmetics.logoTab()) return;
        Text t = cir.getReturnValue();
        if (t == null) return;
        java.util.UUID id = Cosmetics.uuidOf(entry.getProfile());
        if (Cosmetics.showLogoFor(id)) cir.setReturnValue(Cosmetics.withLogo(t));
    }
}
