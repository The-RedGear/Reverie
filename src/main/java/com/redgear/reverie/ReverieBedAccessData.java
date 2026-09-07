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

/** Optional administrator overrides layered over the normal owner-first bed rules. */
public final class ReverieBedAccessData extends SavedData {
    private static final String FILE_NAME = "reverie_bed_access";
    private final Map<BlockPos, Access> entries = new HashMap<>();

    public enum Policy { DEFAULT, PRIVATE, PUBLIC }

    public static ReverieBedAccessData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ReverieBedAccessData::new, ReverieBedAccessData::load, null), FILE_NAME);
    }

    public Policy policy(BlockPos bed) { return entries.getOrDefault(bed, Access.EMPTY).policy; }
    public boolean invited(BlockPos bed, UUID player) { return entries.getOrDefault(bed, Access.EMPTY).invited.contains(player); }
    public UUID host(BlockPos bed, UUID owner) {
        UUID override = entries.getOrDefault(bed, Access.EMPTY).host;
        return override == null ? owner : override;
    }
    public void setPolicy(BlockPos bed, Policy policy) { access(bed).policy = policy; changed(); }
    public void invite(BlockPos bed, UUID player) { if (access(bed).invited.add(player)) changed(); }
    public void uninvite(BlockPos bed, UUID player) { Access a=entries.get(bed); if (a!=null && a.invited.remove(player)) changed(); }
    public void setHost(BlockPos bed, UUID player) { access(bed).host=player; changed(); }
    public void resetHost(BlockPos bed) { Access a=entries.get(bed); if (a!=null && a.host!=null) { a.host=null; changed(); } }
    public void remove(BlockPos bed) { if (entries.remove(bed)!=null) changed(); }
    public int size() { return entries.size(); }

    private Access access(BlockPos bed) { return entries.computeIfAbsent(bed.immutable(), ignored -> new Access()); }
    private void changed() { setDirty(); }

    private static ReverieBedAccessData load(CompoundTag tag, HolderLookup.Provider registries) {
        ReverieBedAccessData data = new ReverieBedAccessData();
        CompoundTag all=tag.getCompound("Beds");
        for (String key:all.getAllKeys()) try {
            CompoundTag in=all.getCompound(key); Access access=new Access();
            try { access.policy=Policy.valueOf(in.getString("Policy")); } catch (IllegalArgumentException ignored) {}
            if (in.hasUUID("Host")) access.host=in.getUUID("Host");
            CompoundTag invites=in.getCompound("Invites");
            for (String id:invites.getAllKeys()) try { access.invited.add(UUID.fromString(id)); } catch (IllegalArgumentException ignored) {}
            data.entries.put(BlockPos.of(Long.parseLong(key)), access);
        } catch (RuntimeException ignored) { Reverie.LOGGER.warn("Ignored malformed bed access entry {}", key); }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all=new CompoundTag();
        entries.forEach((bed,access)->{ CompoundTag out=new CompoundTag(); out.putString("Policy",access.policy.name());
            if(access.host!=null) out.putUUID("Host",access.host); CompoundTag invites=new CompoundTag();
            access.invited.forEach(id->invites.putBoolean(id.toString(),true)); out.put("Invites",invites);
            all.put(Long.toString(bed.asLong()),out); });
        tag.putInt("DataVersion",1); tag.put("Beds",all); return tag;
    }

    private static final class Access {
        private static final Access EMPTY = new Access();
        private Policy policy=Policy.DEFAULT; private UUID host; private final Set<UUID> invited=new HashSet<>();
    }
}
