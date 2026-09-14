package com.redgear.reverie.mixin.client;

import com.redgear.reverie.client.ReverieLightingState;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleSetTime", at = @At("HEAD"), cancellable = true)
    private void reverie$holdPersonalLighting(ClientboundSetTimePacket packet, CallbackInfo callback) {
        if (ReverieLightingState.holdsReverieTime()) callback.cancel();
    }
}
