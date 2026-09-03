package com.redgear.reverie;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** World-persistent additions and removals layered over Reverie's datapack tags. */
public final class ReverieBlocklistData extends SavedData {
    private static final String FILE_NAME = "reverie_blocklist";
    private final Set<ResourceLocation> blockedBlocks = new HashSet<>();
    private final Set<ResourceLocation> allowedBlocks = new HashSet<>();
    private final Set<ResourceLocation> blockedItems = new HashSet<>();
    private final Set<ResourceLocation> allowedItems = new HashSet<>();

    public static ReverieBlocklistData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieBlocklistData::new, ReverieBlocklistData::load, null), FILE_NAME);
    }

    private static ReverieBlocklistData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieBlocklistData data = new ReverieBlocklistData();
        readSet(tag.getCompound("BlockedBlocks"), data.blockedBlocks);
        readSet(tag.getCompound("AllowedBlocks"), data.allowedBlocks);
        readSet(tag.getCompound("BlockedItems"), data.blockedItems);
        readSet(tag.getCompound("AllowedItems"), data.allowedItems);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", 1);
        tag.put("BlockedBlocks", writeSet(blockedBlocks));
        tag.put("AllowedBlocks", writeSet(allowedBlocks));
        tag.put("BlockedItems", writeSet(blockedItems));
        tag.put("AllowedItems", writeSet(allowedItems));
        return tag;
    }

    public boolean isBlockBlocked(ResourceLocation id, boolean tagged) {
        return !allowedBlocks.contains(id) && (tagged || blockedBlocks.contains(id));
    }

    public boolean isItemBlocked(ResourceLocation id, boolean tagged) {
        return !allowedItems.contains(id) && (tagged || blockedItems.contains(id));
    }

    public boolean addBlock(ResourceLocation id) { return update(id, blockedBlocks, allowedBlocks); }
    public boolean removeBlock(ResourceLocation id) { return update(id, allowedBlocks, blockedBlocks); }
    public boolean addItem(ResourceLocation id) { return update(id, blockedItems, allowedItems); }
    public boolean removeItem(ResourceLocation id) { return update(id, allowedItems, blockedItems); }

    public Set<ResourceLocation> blockedBlocks() { return Set.copyOf(blockedBlocks); }
    public Set<ResourceLocation> allowedBlocks() { return Set.copyOf(allowedBlocks); }
    public Set<ResourceLocation> blockedItems() { return Set.copyOf(blockedItems); }
    public Set<ResourceLocation> allowedItems() { return Set.copyOf(allowedItems); }

    private boolean update(ResourceLocation id, Set<ResourceLocation> addTo, Set<ResourceLocation> removeFrom) {
        boolean changed = addTo.add(id) | removeFrom.remove(id);
        if (changed) setDirty();
        return changed;
    }

    private static CompoundTag writeSet(Set<ResourceLocation> values) {
        CompoundTag tag = new CompoundTag();
        values.forEach(id -> tag.putBoolean(id.toString(), true));
        return tag;
    }

    private static void readSet(CompoundTag tag, Set<ResourceLocation> output) {
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) output.add(id);
        }
    }
}
