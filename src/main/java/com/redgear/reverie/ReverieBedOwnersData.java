package com.redgear.reverie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent ownership of Dreamweaver's Beds in the Overworld. */
public final class ReverieBedOwnersData extends SavedData {
    private static final String FILE_NAME = "reverie_bed_owners";
    private final Map<String, UUID> owners = new HashMap<>();

    public static ReverieBedOwnersData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieBedOwnersData::new, ReverieBedOwnersData::load, null), FILE_NAME);
    }

    private static String key(ResourceKey<Level> dimension, BlockPos bed) {
        return dimension.location() + "@" + bed.asLong();
    }

    public UUID owner(ResourceKey<Level> dimension, BlockPos bed) { return owners.get(key(dimension, bed)); }
    public UUID owner(BlockPos bed) { return owner(Level.OVERWORLD, bed); }

    public UUID claimIfUnowned(BlockPos bed, UUID player) {
        return claimIfUnowned(Level.OVERWORLD, bed, player);
    }

    public UUID claimIfUnowned(ResourceKey<Level> dimension, BlockPos bed, UUID player) {
        String key = key(dimension, bed);
        UUID owner = owners.get(key);
        if (owner != null) return owner;
        owners.put(key, player);
        setDirty();
        return player;
    }

    public void set(BlockPos bed, UUID player) {
        set(Level.OVERWORLD, bed, player);
    }

    public void set(ResourceKey<Level> dimension, BlockPos bed, UUID player) {
        if (!player.equals(owners.put(key(dimension, bed), player))) setDirty();
    }

    public void remove(BlockPos bed) {
        remove(Level.OVERWORLD, bed);
    }

    public void remove(ResourceKey<Level> dimension, BlockPos bed) {
        if (owners.remove(key(dimension, bed)) != null) setDirty();
    }

    private static ReverieBedOwnersData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieBedOwnersData data = new ReverieBedOwnersData();
        CompoundTag stored = tag.getCompound("Owners");
        for (String entryKey : stored.getAllKeys()) {
            try {
                String migrated = entryKey.contains("@") ? entryKey : key(Level.OVERWORLD, BlockPos.of(Long.parseLong(entryKey)));
                data.owners.put(migrated, stored.getUUID(entryKey));
            }
            catch (RuntimeException ignored) { Reverie.LOGGER.warn("Ignored malformed bed owner {}", entryKey); }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", 2);
        CompoundTag stored = new CompoundTag();
        owners.forEach(stored::putUUID);
        tag.put("Owners", stored);
        return tag;
    }
}
