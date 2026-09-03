package com.redgear.reverie;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;

public final class ReverieMobAllowlistData extends SavedData {
    private static final String FILE_NAME = "reverie_mob_allowlist";
    private final Set<ResourceLocation> denied = new HashSet<>(Set.of(
            ResourceLocation.withDefaultNamespace("ender_dragon"), ResourceLocation.withDefaultNamespace("wither")));
    public static ReverieMobAllowlistData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieMobAllowlistData::new, ReverieMobAllowlistData::load, null), FILE_NAME);
    }
    public boolean contains(ResourceLocation id) { return !denied.contains(id); }
    public boolean add(ResourceLocation id) { boolean changed = denied.remove(id); if (changed) setDirty(); return changed; }
    public boolean remove(ResourceLocation id) { boolean changed = denied.add(id); if (changed) setDirty(); return changed; }
    public Set<ResourceLocation> all() { return Set.copyOf(denied); }
    private static ReverieMobAllowlistData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieMobAllowlistData data = new ReverieMobAllowlistData();
        if (tag.contains("Denied")) for (String key : tag.getCompound("Denied").getAllKeys()) { ResourceLocation id = ResourceLocation.tryParse(key); if (id != null) data.denied.add(id); }
        data.denied.remove(ResourceLocation.withDefaultNamespace("warden"));
        data.denied.remove(ResourceLocation.withDefaultNamespace("phantom"));
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", 1);
        CompoundTag stored = new CompoundTag(); denied.forEach(id -> stored.putBoolean(id.toString(), true));
        tag.put("Denied", stored); return tag;
    }
}
