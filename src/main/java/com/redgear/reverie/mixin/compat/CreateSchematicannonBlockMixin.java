package com.redgear.reverie.mixin.compat;

import com.redgear.reverie.ReverieEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Create hook: reject a cannon projectile before its block or block entity is installed. */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.schematics.cannon.LaunchedItem$ForBlockState", remap = false)
public abstract class CreateSchematicannonBlockMixin {
    @Shadow public BlockState state;
    @Shadow public BlockPos target;

    @Inject(method = "place", at = @At("HEAD"), cancellable = true, remap = false)
    private void reverie$rejectBlockedSchematicBlock(Level level, CallbackInfo callback) {
        if (ReverieEvents.rejectAutomatedPlacement(level, target, state)) callback.cancel();
    }
}
