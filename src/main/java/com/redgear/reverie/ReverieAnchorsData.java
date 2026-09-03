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

/** Permanent beds explicitly anchored from inside the Reverie. */
public final class ReverieAnchorsData extends SavedData {
    private static final String FILE_NAME = "reverie_anchors";
    private static final int CHUNK_RADIUS = 2;
    private final Set<BlockPos> anchors = new HashSet<>();

    public static ReverieAnchorsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieAnchorsData::new, ReverieAnchorsData::load, null), FILE_NAME);
    }

    public boolean add(BlockPos pos) { boolean changed = anchors.add(pos.immutable()); if (changed) setDirty(); return changed; }
    public boolean remove(BlockPos pos) { boolean changed = anchors.remove(pos); if (changed) setDirty(); return changed; }
    public boolean contains(BlockPos pos) { return anchors.contains(pos); }
    public Set<BlockPos> all() { return Set.copyOf(anchors); }

    public BlockPos findFor(BlockPos wakingBed) {
        ChunkPos source = new ChunkPos(wakingBed);
        return anchors.stream()
                .filter(pos -> Math.abs(new ChunkPos(pos).x - source.x) <= CHUNK_RADIUS
                        && Math.abs(new ChunkPos(pos).z - source.z) <= CHUNK_RADIUS)
                .min(Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(wakingBed))
                        .thenComparingLong(BlockPos::asLong))
                .orElse(null);
    }

    private static ReverieAnchorsData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieAnchorsData data = new ReverieAnchorsData();
        CompoundTag stored = tag.getCompound("Anchors");
        for (String key : stored.getAllKeys()) {
            try { data.anchors.add(BlockPos.of(Long.parseLong(key))); }
            catch (NumberFormatException ignored) { Reverie.LOGGER.warn("Ignored malformed anchor {}", key); }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", 1);
        CompoundTag stored = new CompoundTag();
        anchors.forEach(pos -> stored.putBoolean(Long.toString(pos.asLong()), true));
        tag.put("Anchors", stored);
        return tag;
    }
}
