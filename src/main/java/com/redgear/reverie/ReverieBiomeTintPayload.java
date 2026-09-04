package com.redgear.reverie;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Sends the source bed biome's grass tint without replacing Reverie chunk biomes. */
public record ReverieBiomeTintPayload(int centerChunkX, int centerChunkZ, int radius, int grassColor, boolean reset)
        implements CustomPacketPayload {
    public static final Type<ReverieBiomeTintPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID, "biome_tint"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReverieBiomeTintPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ReverieBiomeTintPayload::centerChunkX,
            ByteBufCodecs.VAR_INT, ReverieBiomeTintPayload::centerChunkZ,
            ByteBufCodecs.VAR_INT, ReverieBiomeTintPayload::radius,
            ByteBufCodecs.VAR_INT, ReverieBiomeTintPayload::grassColor,
            ByteBufCodecs.BOOL, ReverieBiomeTintPayload::reset,
            ReverieBiomeTintPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
