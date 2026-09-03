package com.redgear.reverie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Inventories that exist only in a particular player's dream at a particular anchor. */
public final class ReverieDreamInventoryData extends SavedData {
    private static final String FILE_NAME = "reverie_dream_inventories";
    private final Map<String, CompoundTag> inventories = new HashMap<>();

    public static ReverieDreamInventoryData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieDreamInventoryData::new, ReverieDreamInventoryData::load, null), FILE_NAME);
    }

    private static String key(UUID player, BlockPos anchor) {
        return player + "@" + anchor.asLong();
    }

    public void loadInto(ServerPlayer player, BlockPos anchor) {
        CompoundTag stored = inventories.get(key(player.getUUID(), anchor));
        player.getInventory().clearContent();
        if (stored == null) return;
        player.getInventory().load(stored.getList("Inventory", 10));
        player.getInventory().selected = Math.max(0, Math.min(8, stored.getInt("Selected")));
        ModdedInventoryBridge.restoreAll(player, stored.getCompound("ModdedInventories"));
    }

    public void capture(ServerPlayer player, BlockPos anchor) {
        CompoundTag stored = new CompoundTag();
        ListTag inventory = new ListTag();
        player.getInventory().save(inventory);
        stored.put("Inventory", inventory);
        stored.putInt("Selected", player.getInventory().selected);
        stored.put("ModdedInventories", ModdedInventoryBridge.captureAll(player));
        inventories.put(key(player.getUUID(), anchor), stored);
        setDirty();
    }

    public boolean has(UUID player, BlockPos anchor) { return inventories.containsKey(key(player, anchor)); }
    public int size() { return inventories.size(); }
    public boolean clear(UUID player, BlockPos anchor) {
        boolean changed = inventories.remove(key(player, anchor)) != null;
        if (changed) setDirty();
        return changed;
    }

    private static ReverieDreamInventoryData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieDreamInventoryData data = new ReverieDreamInventoryData();
        CompoundTag stored = tag.getCompound("Inventories");
        for (String key : stored.getAllKeys()) data.inventories.put(key, stored.getCompound(key).copy());
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag stored = new CompoundTag();
        inventories.forEach((key, inventory) -> stored.put(key, inventory.copy()));
        tag.putInt("DataVersion", 1);
        tag.put("Inventories", stored);
        return tag;
    }
}
