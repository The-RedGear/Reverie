package com.redgear.reverie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class FigmentCagesData extends SavedData {
    private static final String FILE_NAME = "reverie_figment_cages";
    private final Map<BlockPos, Integer> cages = new HashMap<>();
    public static FigmentCagesData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(FigmentCagesData::new, FigmentCagesData::load, null), FILE_NAME);
    }
    public void add(BlockPos pos) { if (cages.putIfAbsent(pos.immutable(), 0) == null) setDirty(); }
    public void remove(BlockPos pos) { if (cages.remove(pos) != null) setDirty(); }
    public Set<BlockPos> all() { return Set.copyOf(cages.keySet()); }
    public int radius(BlockPos cage) { return cages.getOrDefault(cage, 0); }
    public BlockPos nearest(BlockPos pos) { return cages.keySet().stream()
            .min(java.util.Comparator.comparingDouble(cage -> cage.distSqr(pos))).orElse(null); }
    public int resize(BlockPos cage, int change, int maximum) {
        int radius = Math.max(0, Math.min(maximum, radius(cage) + change));
        cages.put(cage.immutable(), radius); setDirty(); return radius;
    }
    public BlockPos cageFor(BlockPos pos) {
        net.minecraft.world.level.ChunkPos target = new net.minecraft.world.level.ChunkPos(pos);
        return cages.keySet().stream().filter(cage -> {
                    net.minecraft.world.level.ChunkPos cageChunk = new net.minecraft.world.level.ChunkPos(cage);
                    int radius = radius(cage);
                    return Math.abs(cageChunk.x - target.x) <= radius
                            && Math.abs(cageChunk.z - target.z) <= radius;
                })
                .min(java.util.Comparator.comparingDouble(cage -> cage.distSqr(pos))).orElse(null);
    }
    private static FigmentCagesData load(CompoundTag tag, HolderLookup.Provider registries) {
        FigmentCagesData data = new FigmentCagesData();
        for (String key : tag.getCompound("Cages").getAllKeys()) try { data.cages.put(BlockPos.of(Long.parseLong(key)), tag.getCompound("Cages").getInt(key)); }
        catch (NumberFormatException ignored) {}
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", 1);
        CompoundTag stored = new CompoundTag(); cages.forEach((pos, radius) -> stored.putInt(Long.toString(pos.asLong()), radius));
        tag.put("Cages", stored); return tag;
    }
}
