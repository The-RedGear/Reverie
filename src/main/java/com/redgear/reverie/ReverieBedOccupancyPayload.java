package com.redgear.reverie;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReverieBedOccupancyPayload(long bedPos, String owner, String guests, boolean visible)
        implements CustomPacketPayload {
    public static final Type<ReverieBedOccupancyPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID, "bed_occupancy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReverieBedOccupancyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, ReverieBedOccupancyPayload::bedPos,
            ByteBufCodecs.STRING_UTF8, ReverieBedOccupancyPayload::owner,
            ByteBufCodecs.STRING_UTF8, ReverieBedOccupancyPayload::guests,
            ByteBufCodecs.BOOL, ReverieBedOccupancyPayload::visible,
            ReverieBedOccupancyPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
