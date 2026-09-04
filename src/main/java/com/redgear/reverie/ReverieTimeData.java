package com.redgear.reverie;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Keeps the shared Reverie clock frozen at the lighting chosen by an operator. */
public final class ReverieTimeData extends SavedData {
    private static final String FILE_NAME = "reverie_time";
    public static final long DEFAULT_TIME = 6000L;
    private long time = DEFAULT_TIME;

    public static ReverieTimeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieTimeData::new, ReverieTimeData::load, null), FILE_NAME);
    }

    public long time() { return time; }

    public void set(long value) {
        long normalized = Math.floorMod(value, 24000L);
        if (time != normalized) {
            time = normalized;
            setDirty();
        }
    }

    private static ReverieTimeData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieTimeData data = new ReverieTimeData();
        if (tag.contains("Time")) data.time = Math.floorMod(tag.getLong("Time"), 24000L);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", 1);
        tag.putLong("Time", time);
        return tag;
    }
}
