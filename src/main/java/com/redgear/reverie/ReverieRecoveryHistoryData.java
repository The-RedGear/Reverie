package com.redgear.reverie;
import net.minecraft.core.HolderLookup;import net.minecraft.nbt.*;import net.minecraft.server.MinecraftServer;import net.minecraft.world.level.saveddata.SavedData;import java.util.*;
public final class ReverieRecoveryHistoryData extends SavedData{
 private static final String FILE_NAME="reverie_recovery_history";private final Map<UUID,ArrayDeque<Entry>> entries=new HashMap<>();
 public static ReverieRecoveryHistoryData get(MinecraftServer s){return s.overworld().getDataStorage().computeIfAbsent(new Factory<>(ReverieRecoveryHistoryData::new,ReverieRecoveryHistoryData::load,null),FILE_NAME);}
 public void add(UUID id,long time,String action,String detail){var q=entries.computeIfAbsent(id,k->new ArrayDeque<>());q.addFirst(new Entry(time,action,detail));while(q.size()>20)q.removeLast();setDirty();}
 public List<Entry> entries(UUID id){return List.copyOf(entries.getOrDefault(id,new ArrayDeque<>()));}
 private static ReverieRecoveryHistoryData load(CompoundTag t,HolderLookup.Provider r){var d=new ReverieRecoveryHistoryData();CompoundTag all=t.getCompound("Players");for(String k:all.getAllKeys())try{UUID id=UUID.fromString(k);ListTag list=all.getList(k,10);for(int i=0;i<list.size();i++){CompoundTag e=list.getCompound(i);d.entries.computeIfAbsent(id,x->new ArrayDeque<>()).addLast(new Entry(e.getLong("Time"),e.getString("Action"),e.getString("Detail")));}}catch(Exception ignored){}return d;}
 @Override public CompoundTag save(CompoundTag t,HolderLookup.Provider r){CompoundTag all=new CompoundTag();entries.forEach((id,q)->{ListTag list=new ListTag();q.forEach(e->{CompoundTag n=new CompoundTag();n.putLong("Time",e.time);n.putString("Action",e.action);n.putString("Detail",e.detail);list.add(n);});all.put(id.toString(),list);});t.put("Players",all);t.putInt("DataVersion",1);return t;}public record Entry(long time,String action,String detail){}
}
