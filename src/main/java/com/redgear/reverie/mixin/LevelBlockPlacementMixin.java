package com.redgear.reverie.mixin;

import com.redgear.reverie.ReverieEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Final dimension-level gate used even when a mod bypasses NeoForge placement events. */
@Mixin(Level.class)
public abstract class LevelBlockPlacementMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"), cancellable = true)
    private void reverie$rejectIllegalState(BlockPos pos, BlockState state, int flags, int recursionLeft,
                                             CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerLevel level && ReverieEvents.rejectAutomatedPlacement(level, pos, state)) {
            callback.setReturnValue(false);
        }
    }
}
