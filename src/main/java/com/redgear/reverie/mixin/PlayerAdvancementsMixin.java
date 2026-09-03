package com.redgear.reverie.mixin;

import com.redgear.reverie.Reverie;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents even partial advancement progress from being created during a dream. */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow private ServerPlayer player;

    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private void reverie$preventDreamAdvancements(AdvancementHolder advancement, String criterion,
                                                   CallbackInfoReturnable<Boolean> callback) {
        if (player.level().dimension().equals(Reverie.REVERIE_LEVEL)
                && !advancement.id().getNamespace().equals(Reverie.MOD_ID)) {
            callback.setReturnValue(false);
        }
    }
}
