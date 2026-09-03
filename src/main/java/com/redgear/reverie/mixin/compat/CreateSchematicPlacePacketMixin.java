package com.redgear.reverie.mixin.compat;

import com.redgear.reverie.ReverieEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Create hook: filter every block in a direct creative schematic paste. */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.schematics.packet.SchematicPlacePacket", remap = false)
public abstract class CreateSchematicPlacePacketMixin {
    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true, remap = false)
    private static void reverie$rejectBlockedDirectPaste(boolean includeAir, Level level, BlockPos pos,
                                                          BlockState state, BlockEntity blockEntity,
                                                          CallbackInfo callback) {
        if (ReverieEvents.rejectAutomatedPlacement(level, pos, state)) callback.cancel();
    }
}
