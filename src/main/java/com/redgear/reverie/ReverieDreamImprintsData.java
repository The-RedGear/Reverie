package com.redgear.reverie;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A player's reusable starting inventory for temporary dreams. */
public final class ReverieDreamImprintsData extends SavedData {
    private static final String FILE_NAME="reverie_dream_imprints";
    private final Map<UUID,CompoundTag> imprints=new HashMap<>();
    public static ReverieDreamImprintsData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(ReverieDreamImprintsData::new,ReverieDreamImprintsData::load,null),FILE_NAME);}
    public void capture(ServerPlayer player){CompoundTag stored=new CompoundTag();ListTag inventory=new ListTag();player.getInventory().save(inventory);inventory.removeIf(value->value instanceof CompoundTag item&&"minecraft:book".equals(item.getString("id")));stored.put("Inventory",inventory);stored.putInt("Selected",player.getInventory().selected);stored.put("ModdedInventories",ModdedInventoryBridge.captureAll(player));imprints.put(player.getUUID(),ReverieDreamInventoryData.sanitized(player,stored));setDirty();}
    public boolean has(UUID player){return imprints.containsKey(player);}
    public boolean loadInto(ServerPlayer player){CompoundTag stored=imprints.get(player.getUUID());player.getInventory().clearContent();ModdedInventoryBridge.clearAll(player);if(stored==null)return false;stored=ReverieDreamInventoryData.sanitized(player,stored);player.getInventory().load(stored.getList("Inventory",10));player.getInventory().selected=Math.max(0,Math.min(8,stored.getInt("Selected")));ModdedInventoryBridge.restoreAll(player,stored.getCompound("ModdedInventories"));return true;}
    public boolean clear(UUID player){boolean changed=imprints.remove(player)!=null;if(changed)setDirty();return changed;}
    private static ReverieDreamImprintsData load(CompoundTag tag,HolderLookup.Provider registries){var data=new ReverieDreamImprintsData();CompoundTag all=tag.getCompound("Players");for(String key:all.getAllKeys())try{data.imprints.put(UUID.fromString(key),all.getCompound(key).copy());}catch(IllegalArgumentException ignored){}return data;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){CompoundTag all=new CompoundTag();imprints.forEach((id,data)->all.put(id.toString(),data.copy()));tag.putInt("DataVersion",1);tag.put("Players",all);return tag;}
}
