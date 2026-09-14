package com.redgear.reverie;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReverieDestinationSelectPayload(String name, boolean entryBed, boolean savedBed) implements CustomPacketPayload {
    public static final Type<ReverieDestinationSelectPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID, "select_destination"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReverieDestinationSelectPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeUtf(payload.name, ReverieBookmarksData.MAX_NAME_LENGTH);
                buffer.writeBoolean(payload.entryBed);
                buffer.writeBoolean(payload.savedBed);
            }, buffer -> new ReverieDestinationSelectPayload(
                    buffer.readUtf(ReverieBookmarksData.MAX_NAME_LENGTH), buffer.readBoolean(), buffer.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
