package com.redgear.reverie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Personal destinations grouped by arrival bed. Reads never create saved records. */
public final class ReverieBookmarksData extends SavedData {
    private static final String FILE_NAME = "reverie_bookmarks";
    public static final int MAX_BOOKMARKS = 32;
    public static final int MAX_NAME_LENGTH = 48;
    private final Map<UUID, Map<Long, Map<String, BlockPos>>> players = new HashMap<>();
    private final Map<UUID, Map<Long, String>> selections = new HashMap<>();

    public static ReverieBookmarksData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieBookmarksData::new, ReverieBookmarksData::load, null), FILE_NAME);
    }

    public boolean put(UUID player, BlockPos scope, String name, BlockPos pos) {
        if (scope == null || pos == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) return false;
        Map<String, BlockPos> existing = all(player, scope);
        if (!existing.containsKey(name) && existing.size() >= MAX_BOOKMARKS) return false;
        players.computeIfAbsent(player, k -> new HashMap<>())
                .computeIfAbsent(scope.asLong(), k -> new TreeMap<>()).put(name, pos.immutable());
        select(player, scope, name);
        return true;
    }

    public boolean remove(UUID player, BlockPos scope, String name) {
        if (scope == null) return false;
        var scopes = players.get(player);
        var marks = scopes == null ? null : scopes.get(scope.asLong());
        if (marks == null || marks.remove(name) == null) return false;
        if (name.equals(selected(player, scope))) select(player, scope, null);
        if (marks.isEmpty()) scopes.remove(scope.asLong());
        if (scopes.isEmpty()) players.remove(player);
        setDirty();
        return true;
    }

    public String nameAt(UUID player, BlockPos scope, BlockPos pos) {
        return all(player, scope).entrySet().stream().filter(entry -> entry.getValue().equals(pos))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public int removePosition(BlockPos pos) {
        int removed = 0;
        for (var playerEntry : new ArrayList<>(players.entrySet())) {
            UUID player = playerEntry.getKey();
            for (var scopeEntry : new ArrayList<>(playerEntry.getValue().entrySet())) {
                BlockPos scope = BlockPos.of(scopeEntry.getKey());
                for (String name : new ArrayList<>(scopeEntry.getValue().keySet())) {
                    if (pos.equals(scopeEntry.getValue().get(name)) && remove(player, scope, name)) removed++;
                }
            }
        }
        return removed;
    }

    public Map<String, BlockPos> all(UUID player, BlockPos scope) {
        if (scope == null) return Map.of();
        var scopes = players.get(player);
        return scopes == null ? Map.of() : Collections.unmodifiableMap(scopes.getOrDefault(scope.asLong(), Map.of()));
    }

    /** Saves a bed with a readable unique name, or removes it when already saved. */
    public String toggleBed(UUID player, BlockPos scope, BlockPos pos, String preferredName) {
        String existing = nameAt(player, scope, pos);
        if (existing != null) {
            remove(player, scope, existing);
            return null;
        }
        String base = preferredName == null || preferredName.isBlank()
                ? "Dreamweaver's Bed " + pos.getX() + ", " + pos.getZ() : preferredName;
        String name = base;
        int suffix = 2;
        while (all(player, scope).containsKey(name) && suffix < 100) name = base + " (" + suffix++ + ")";
        return put(player, scope, name, pos) ? name : "";
    }

    public String selected(UUID player, BlockPos scope) {
        if (scope == null) return null;
        return selections.getOrDefault(player, Map.of()).get(scope.asLong());
    }

    private void select(UUID player, BlockPos scope, String name) {
        if (name == null) {
            var scopes = selections.get(player);
            if (scopes != null) { scopes.remove(scope.asLong()); if (scopes.isEmpty()) selections.remove(player); }
        } else selections.computeIfAbsent(player, k -> new HashMap<>()).put(scope.asLong(), name);
        setDirty();
    }

    public BlockPos target(UUID player, BlockPos scope, BlockPos bed) {
        String name = selected(player, scope);
        return name == null ? bed : all(player, scope).getOrDefault(name, bed);
    }

    /** Null is the bed sentinel; a bookmark literally named Bed remains selectable. */
    public String cycle(UUID player, BlockPos scope) {
        if (scope == null) return "Bed";
        List<String> names = new ArrayList<>(all(player, scope).keySet());
        Collections.sort(names);
        String current = selected(player, scope);
        int next = current == null ? 0 : names.indexOf(current) + 1;
        String name = next >= names.size() ? null : names.get(next);
        select(player, scope, name);
        return name == null ? "Bed" : name;
    }

    static ReverieBookmarksData load(CompoundTag tag, HolderLookup.Provider registries) {
        var data = new ReverieBookmarksData();
        CompoundTag stored = tag.getCompound("Players");
        for (String id : stored.getAllKeys()) {
            UUID player;
            try { player = UUID.fromString(id); }
            catch (IllegalArgumentException error) { Reverie.LOGGER.warn("Ignoring invalid bookmark player {}", id); continue; }
            CompoundTag scopes = stored.getCompound(id);
            for (String key : scopes.getAllKeys()) {
                long scope;
                try { scope = Long.parseLong(key); }
                catch (NumberFormatException error) { Reverie.LOGGER.warn("Ignoring invalid bookmark scope {}", key); continue; }
                CompoundTag marks = scopes.getCompound(key);
                Map<String, BlockPos> entries = new TreeMap<>();
                for (String name : marks.getAllKeys()) {
                    if (marks.contains(name, 4)) {
                        String migratedName = name.startsWith("Dream Bed ")
                                ? "Dreamweaver's Bed " + name.substring("Dream Bed ".length()) : name;
                        entries.put(migratedName, BlockPos.of(marks.getLong(name)));
                    }
                }
                if (!entries.isEmpty()) data.players.computeIfAbsent(player, k -> new HashMap<>()).put(scope, entries);
                String selected = tag.getCompound("Selections").getCompound(id).getString(key);
                if (entries.containsKey(selected)) data.selections.computeIfAbsent(player, k -> new HashMap<>()).put(scope, selected);
            }
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag stored = new CompoundTag();
        players.forEach((id, scopes) -> {
            CompoundTag scopeTags = new CompoundTag();
            scopes.forEach((scope, marks) -> {
                CompoundTag entries = new CompoundTag();
                marks.forEach((name, pos) -> entries.putLong(name, pos.asLong()));
                scopeTags.put(Long.toString(scope), entries);
            });
            stored.put(id.toString(), scopeTags);
        });
        CompoundTag selected = new CompoundTag();
        selections.forEach((id, scopes) -> {
            CompoundTag values = new CompoundTag();
            scopes.forEach((scope, name) -> values.putString(Long.toString(scope), name));
            selected.put(id.toString(), values);
        });
        tag.putInt("DataVersion", 2);
        tag.put("Players", stored);
        tag.put("Selections", selected);
        return tag;
    }
}
