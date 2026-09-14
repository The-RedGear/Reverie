package com.redgear.reverie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** Player-owned bed destinations. These are world data, never data on a Compass item. */
public final class ReverieCompassDestinationsData extends SavedData {
    private static final String FILE_NAME = "reverie_compass_destinations";
    private final Map<UUID, Map<String, BlockPos>> players = new HashMap<>();

    public static ReverieCompassDestinationsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(
                ReverieCompassDestinationsData::new, ReverieCompassDestinationsData::load, null), FILE_NAME);
    }

    public Map<String, BlockPos> all(UUID player) {
        return Collections.unmodifiableMap(players.getOrDefault(player, Map.of()));
    }

    public String toggle(UUID player, BlockPos bed, String preferredName) {
        Map<String, BlockPos> destinations = players.getOrDefault(player, Map.of());
        String existing = destinations.entrySet().stream().filter(entry -> entry.getValue().equals(bed))
                .map(Map.Entry::getKey).findFirst().orElse(null);
        if (existing != null) {
            Map<String, BlockPos> mutable = players.get(player);
            mutable.remove(existing);
            if (mutable.isEmpty()) players.remove(player);
            setDirty();
            return null;
        }
        if (destinations.size() >= ReverieBookmarksData.MAX_BOOKMARKS) return "";
        String base = preferredName == null || preferredName.isBlank()
                ? "Dreamweaver's Bed " + bed.getX() + ", " + bed.getZ() : preferredName;
        String name = base;
        int suffix = 2;
        while (destinations.containsKey(name)) name = base + " (" + suffix++ + ")";
        players.computeIfAbsent(player, ignored -> new TreeMap<>()).put(name, bed.immutable());
        setDirty();
        return name;
    }

    public int removePosition(BlockPos bed) {
        int removed = 0;
        for (UUID player : new ArrayList<>(players.keySet())) {
            Map<String, BlockPos> destinations = players.get(player);
            int before = destinations.size();
            destinations.entrySet().removeIf(entry -> entry.getValue().equals(bed));
            removed += before - destinations.size();
            if (destinations.isEmpty()) players.remove(player);
        }
        if (removed > 0) setDirty();
        return removed;
    }

    static ReverieCompassDestinationsData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieCompassDestinationsData data = new ReverieCompassDestinationsData();
        CompoundTag stored = tag.getCompound("Players");
        for (String id : stored.getAllKeys()) {
            try {
                UUID player = UUID.fromString(id);
                CompoundTag entries = stored.getCompound(id);
                Map<String, BlockPos> destinations = new TreeMap<>();
                for (String name : entries.getAllKeys()) if (entries.contains(name, 4)) {
                    String migratedName = name.startsWith("Dream Bed ")
                            ? "Dreamweaver's Bed " + name.substring("Dream Bed ".length()) : name;
                    destinations.put(migratedName, BlockPos.of(entries.getLong(name)));
                }
                if (!destinations.isEmpty()) data.players.put(player, destinations);
            } catch (IllegalArgumentException error) {
                Reverie.LOGGER.warn("Ignoring invalid Compass destination owner {}", id);
            }
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag stored = new CompoundTag();
        players.forEach((player, destinations) -> {
            CompoundTag entries = new CompoundTag();
            destinations.forEach((name, bed) -> entries.putLong(name, bed.asLong()));
            stored.put(player.toString(), entries);
        });
        tag.putInt("DataVersion", 1);
        tag.put("Players", stored);
        return tag;
    }
}
