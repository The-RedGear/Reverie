package com.redgear.reverie.mixin;

import com.redgear.reverie.ReverieEvents;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "announceSleepStatus", at = @At("HEAD"), cancellable = true)
    private void reverie$suppressDreamweaverSleepStatus(CallbackInfo callback) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (ReverieEvents.shouldSuppressSleepStatus(level.getServer())) callback.cancel();
    }
}
