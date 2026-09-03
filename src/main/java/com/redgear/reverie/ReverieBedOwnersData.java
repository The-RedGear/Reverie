package com.redgear.reverie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent ownership of Dreamweaver's Beds in the Overworld. */
public final class ReverieBedOwnersData extends SavedData {
    private static final String FILE_NAME = "reverie_bed_owners";
    private final Map<BlockPos, UUID> owners = new HashMap<>();

    public static ReverieBedOwnersData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieBedOwnersData::new, ReverieBedOwnersData::load, null), FILE_NAME);
    }

    public UUID owner(BlockPos bed) { return owners.get(bed); }

    public UUID claimIfUnowned(BlockPos bed, UUID player) {
        UUID owner = owners.get(bed);
        if (owner != null) return owner;
        owners.put(bed.immutable(), player);
        setDirty();
        return player;
    }

    public void set(BlockPos bed, UUID player) {
        if (!player.equals(owners.put(bed.immutable(), player))) setDirty();
    }

    public void remove(BlockPos bed) {
        if (owners.remove(bed) != null) setDirty();
    }

    private static ReverieBedOwnersData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieBedOwnersData data = new ReverieBedOwnersData();
        CompoundTag stored = tag.getCompound("Owners");
        for (String key : stored.getAllKeys()) {
            try { data.owners.put(BlockPos.of(Long.parseLong(key)), stored.getUUID(key)); }
            catch (RuntimeException ignored) { Reverie.LOGGER.warn("Ignored malformed bed owner {}", key); }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", 1);
        CompoundTag stored = new CompoundTag();
        owners.forEach((bed, owner) -> stored.putUUID(Long.toString(bed.asLong()), owner));
        tag.put("Owners", stored);
        return tag;
    }
}
