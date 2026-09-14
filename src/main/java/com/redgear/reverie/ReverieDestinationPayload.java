package com.redgear.reverie;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ReverieDestinationPayload(List<Destination> destinations) implements CustomPacketPayload {
    public static final Type<ReverieDestinationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID, "destinations"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReverieDestinationPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> {
                buffer.writeVarInt(payload.destinations.size());
                for (Destination destination : payload.destinations) {
                    buffer.writeUtf(destination.name, ReverieBookmarksData.MAX_NAME_LENGTH);
                    buffer.writeBoolean(destination.entryBed);
                    buffer.writeBoolean(destination.savedBed);
                    buffer.writeBoolean(destination.available);
                }
            }, buffer -> {
                int size = Math.max(0, Math.min(buffer.readVarInt(), ReverieBookmarksData.MAX_BOOKMARKS + 1));
                List<Destination> destinations = new ArrayList<>(size);
                for (int index = 0; index < size; index++) destinations.add(new Destination(
                        buffer.readUtf(ReverieBookmarksData.MAX_NAME_LENGTH), buffer.readBoolean(),
                        buffer.readBoolean(), buffer.readBoolean()));
                return new ReverieDestinationPayload(List.copyOf(destinations));
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record Destination(String name, boolean entryBed, boolean savedBed, boolean available) {}
}
