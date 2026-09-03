package com.redgear.reverie;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Independently persisted last-known waking states for disaster recovery. */
public final class ReverieRecoveryData extends SavedData {
    private static final String FILE_NAME = "reverie_recovery";
    private final Map<UUID, Entry> entries = new HashMap<>();

    public static ReverieRecoveryData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieRecoveryData::new, ReverieRecoveryData::load, null), FILE_NAME);
    }

    public void capture(UUID id, String name, CompoundTag player, long gameTime) {
        entries.put(id, new Entry(name, player.copy(), new CompoundTag(), true, gameTime));
        flushDirty();
    }
    public CompoundTag waking(UUID id) { Entry e = entries.get(id); return e == null ? null : e.waking.copy(); }
    public CompoundTag rollback(UUID id) { Entry e = entries.get(id); return e == null || e.rollback.isEmpty() ? null : e.rollback.copy(); }
    public Entry entry(UUID id) { return entries.get(id); }
    public int size() { return entries.size(); }
    public long activeCount() { return entries.values().stream().filter(Entry::active).count(); }
    public void prepareRestore(UUID id, CompoundTag current) { Entry e = entries.get(id); if (e != null) { e.rollback = current.copy(); flushDirty(); } }
    public void complete(UUID id) { Entry e = entries.get(id); if (e != null) { e.active = false; flushDirty(); } }
    public void flush(MinecraftServer server) { server.overworld().getDataStorage().save(); }
    private void flushDirty() { setDirty(); }

    private static ReverieRecoveryData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieRecoveryData data = new ReverieRecoveryData();
        CompoundTag all = tag.getCompound("Players");
        for (String key : all.getAllKeys()) try {
            CompoundTag e = all.getCompound(key);
            data.entries.put(UUID.fromString(key), new Entry(e.getString("Name"), e.getCompound("Waking"),
                    e.getCompound("Rollback"), e.getBoolean("Active"), e.getLong("GameTime")));
        } catch (IllegalArgumentException ignored) { Reverie.LOGGER.warn("Ignored malformed recovery entry {}", key); }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        entries.forEach((id, e) -> { CompoundTag out = new CompoundTag(); out.putString("Name", e.name);
            out.put("Waking", e.waking.copy()); if (!e.rollback.isEmpty()) out.put("Rollback", e.rollback.copy());
            out.putBoolean("Active", e.active); out.putLong("GameTime", e.gameTime); all.put(id.toString(), out); });
        tag.putInt("DataVersion", 1); tag.put("Players", all); return tag;
    }

    public static final class Entry {
        private final String name; private final CompoundTag waking; private CompoundTag rollback; private boolean active; private final long gameTime;
        private Entry(String name, CompoundTag waking, CompoundTag rollback, boolean active, long gameTime) {
            this.name=name; this.waking=waking.copy(); this.rollback=rollback.copy(); this.active=active; this.gameTime=gameTime;
        }
        public String name() { return name; } public boolean active() { return active; } public long gameTime() { return gameTime; }
        public boolean hasRollback() { return !rollback.isEmpty(); }
    }
}
