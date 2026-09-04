package com.redgear.reverie;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

/** Saved visual grass tints. Empty data from older worlds is populated lazily as beds are revisited. */
public final class ReverieBiomeTintsData extends SavedData {
    private static final String FILE_NAME = "reverie_biome_tints";
    private static final int REGION_RADIUS = 2;
    private final Map<Long, Integer> regions = new HashMap<>();

    public static ReverieBiomeTintsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieBiomeTintsData::new, ReverieBiomeTintsData::load, null), FILE_NAME);
    }

    public void remember(int chunkX, int chunkZ, int grassColor) {
        if (!Integer.valueOf(grassColor).equals(regions.put(ChunkPos.asLong(chunkX, chunkZ), grassColor))) setDirty();
    }

    public void sendTo(ServerPlayer player) {
        boolean first = true;
        for (Map.Entry<Long, Integer> entry : regions.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            PacketDistributor.sendToPlayer(player, new ReverieBiomeTintPayload(
                    ChunkPos.getX(entry.getKey()), ChunkPos.getZ(entry.getKey()), REGION_RADIUS, entry.getValue(), first));
            first = false;
        }
        if (first) PacketDistributor.sendToPlayer(player, new ReverieBiomeTintPayload(0, 0, 0, 0, true));
    }

    private static ReverieBiomeTintsData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieBiomeTintsData data = new ReverieBiomeTintsData();
        CompoundTag stored = tag.getCompound("Regions");
        for (String key : stored.getAllKeys()) {
            try { data.regions.put(Long.parseLong(key), stored.getInt(key)); }
            catch (NumberFormatException ignored) { Reverie.LOGGER.warn("Ignored malformed biome tint region {}", key); }
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", 1);
        CompoundTag stored = new CompoundTag();
        regions.forEach((region, color) -> stored.putInt(Long.toString(region), color));
        tag.put("Regions", stored);
        return tag;
    }
}
