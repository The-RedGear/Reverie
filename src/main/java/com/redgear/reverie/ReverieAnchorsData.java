package com.redgear.reverie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

/** Permanent beds explicitly anchored from inside the Reverie. */
public final class ReverieAnchorsData extends SavedData {
    private static final String FILE_NAME = "reverie_anchors";
    private static final int CHUNK_RADIUS = 2;
    private final Set<BlockPos> anchors = new HashSet<>();
    private final Map<BlockPos, String> names = new HashMap<>();

    public static ReverieAnchorsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieAnchorsData::new, ReverieAnchorsData::load, null), FILE_NAME);
    }

    public boolean add(BlockPos pos) { boolean changed = anchors.add(pos.immutable()); if (changed) setDirty(); return changed; }
    public boolean remove(BlockPos pos) { boolean changed = anchors.remove(pos); names.remove(pos); if (changed) setDirty(); return changed; }
    public boolean contains(BlockPos pos) { return anchors.contains(pos); }
    public Set<BlockPos> all() { return Set.copyOf(anchors); }
    public String name(BlockPos pos) { return names.getOrDefault(pos, ""); }
    public void name(BlockPos pos, String value) { if (anchors.contains(pos)) { if (value == null || value.isBlank()) names.remove(pos); else names.put(pos.immutable(), value); setDirty(); } }
    public BlockPos overlapping(BlockPos candidate) {
        ChunkPos chunk = new ChunkPos(candidate);
        return anchors.stream().filter(pos -> !pos.equals(candidate))
                .filter(pos -> Math.abs(new ChunkPos(pos).x - chunk.x) <= CHUNK_RADIUS * 2
                        && Math.abs(new ChunkPos(pos).z - chunk.z) <= CHUNK_RADIUS * 2)
                .min(Comparator.comparingLong(BlockPos::asLong)).orElse(null);
    }

    public BlockPos findFor(BlockPos wakingBed) {
        ChunkPos source = new ChunkPos(wakingBed);
        return anchors.stream()
                .filter(pos -> Math.abs(new ChunkPos(pos).x - source.x) <= CHUNK_RADIUS
                        && Math.abs(new ChunkPos(pos).z - source.z) <= CHUNK_RADIUS)
                // Coverage is horizontal. Heights from two different dimensions
                // must not influence which nearby anchor is selected.
                .min(Comparator.comparingLong((BlockPos pos) -> horizontalDistanceSquared(pos, wakingBed))
                        .thenComparingLong(BlockPos::asLong))
                .orElse(null);
    }

    private static long horizontalDistanceSquared(BlockPos first, BlockPos second) {
        long x = (long) first.getX() - second.getX();
        long z = (long) first.getZ() - second.getZ();
        return x * x + z * z;
    }

    private static ReverieAnchorsData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieAnchorsData data = new ReverieAnchorsData();
        CompoundTag stored = tag.getCompound("Anchors");
        for (String key : stored.getAllKeys()) {
            try { BlockPos pos=BlockPos.of(Long.parseLong(key)); data.anchors.add(pos); String name=stored.getString(key); if(!name.isBlank())data.names.put(pos,name); }
            catch (NumberFormatException ignored) { Reverie.LOGGER.warn("Ignored malformed anchor {}", key); }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", 1);
        CompoundTag stored = new CompoundTag();
        anchors.forEach(pos -> stored.putString(Long.toString(pos.asLong()), names.getOrDefault(pos, "")));
        tag.put("Anchors", stored);
        return tag;
    }
}
