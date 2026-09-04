package com.redgear.reverie.mixin;

import com.redgear.reverie.Reverie;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void reverie$preventDaylightBurning(CallbackInfoReturnable<Boolean> callback) {
        Mob mob = (Mob) (Object) this;
        if (mob.level().dimension().equals(Reverie.REVERIE_LEVEL)) callback.setReturnValue(false);
    }
}
