package com.redgear.reverie;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReverieLightingPayload(long time) implements CustomPacketPayload {
    public static final Type<ReverieLightingPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID, "lighting"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReverieLightingPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeLong(payload.time),
                    buffer -> new ReverieLightingPayload(buffer.readLong()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
