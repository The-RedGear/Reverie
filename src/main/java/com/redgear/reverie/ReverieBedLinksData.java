package com.redgear.reverie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent shared arrival-bed links and the dreamers currently using them. */
public final class ReverieBedLinksData extends SavedData {
    private static final String FILE_NAME = "reverie_bed_links";
    private final Map<BlockPos, Link> links = new HashMap<>();

    public static ReverieBedLinksData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieBedLinksData::new, ReverieBedLinksData::load, null), FILE_NAME);
    }

    public BlockPos dreamBed(BlockPos wakingBed) {
        Link link = links.get(wakingBed);
        return link == null ? null : link.dreamBed;
    }

    public int occupantCount(BlockPos wakingBed) {
        Link link = links.get(wakingBed);
        return link == null ? 0 : link.players.size();
    }

    public boolean containsDreamer(BlockPos wakingBed, UUID player) {
        Link link = links.get(wakingBed);
        return link != null && link.players.contains(player);
    }

    public Set<BlockPos> occupiedWakingBeds() {
        Set<BlockPos> result = new HashSet<>();
        links.forEach((pos, link) -> { if (!link.players.isEmpty()) result.add(pos); });
        return result;
    }

    public Set<BlockPos> wakingBeds() { return Set.copyOf(links.keySet()); }

    public void join(BlockPos wakingBed, BlockPos dreamBed, UUID player, boolean anchored) {
        Link link = links.computeIfAbsent(wakingBed.immutable(), ignored -> new Link(dreamBed.immutable()));
        link.dreamBed = dreamBed.immutable();
        link.anchored = anchored;
        if (link.players.add(player)) setDirty();
    }

    /** Returns the bed to remove only when this was its final dreamer. */
    public BlockPos leave(BlockPos wakingBed, UUID player) {
        if (wakingBed == null) return null;
        Link link = links.get(wakingBed);
        if (link == null) return null;
        if (link.players.remove(player)) setDirty();
        if (!link.players.isEmpty()) return null;
        links.remove(wakingBed);
        setDirty();
        return link.anchored ? null : link.dreamBed;
    }

    public BlockPos removeLink(BlockPos wakingBed) {
        Link removed = links.remove(wakingBed);
        if (removed != null) setDirty();
        return removed == null || removed.anchored ? null : removed.dreamBed;
    }

    public void setAnchored(BlockPos dreamBed, boolean anchored) {
        boolean changed = false;
        for (Link link : links.values()) {
            if (link.dreamBed.equals(dreamBed) && link.anchored != anchored) {
                link.anchored = anchored;
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    private static ReverieBedLinksData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieBedLinksData data = new ReverieBedLinksData();
        CompoundTag stored = tag.getCompound("Links");
        for (String key : stored.getAllKeys()) {
            try {
                CompoundTag linkTag = stored.getCompound(key);
                Link link = new Link(BlockPos.of(linkTag.getLong("DreamBed")));
                link.anchored = linkTag.getBoolean("Anchored");
                CompoundTag players = linkTag.getCompound("Players");
                for (String uuid : players.getAllKeys()) link.players.add(UUID.fromString(uuid));
                data.links.put(BlockPos.of(Long.parseLong(key)), link);
            } catch (IllegalArgumentException ignored) {
                Reverie.LOGGER.warn("Ignored malformed shared bed link {}", key);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", 1);
        CompoundTag stored = new CompoundTag();
        links.forEach((wakingBed, link) -> {
            CompoundTag linkTag = new CompoundTag();
            linkTag.putLong("DreamBed", link.dreamBed.asLong());
            linkTag.putBoolean("Anchored", link.anchored);
            CompoundTag players = new CompoundTag();
            link.players.forEach(uuid -> players.putBoolean(uuid.toString(), true));
            linkTag.put("Players", players);
            stored.put(Long.toString(wakingBed.asLong()), linkTag);
        });
        tag.put("Links", stored);
        return tag;
    }

    private static final class Link {
        private BlockPos dreamBed;
        private boolean anchored;
        private final Set<UUID> players = new HashSet<>();
        private Link(BlockPos dreamBed) { this.dreamBed = dreamBed; }
    }
}
