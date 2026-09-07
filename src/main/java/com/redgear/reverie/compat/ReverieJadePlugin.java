package com.redgear.reverie.compat;

import com.redgear.reverie.*;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class ReverieJadePlugin implements IWailaPlugin {
    private static final ResourceLocation UID=ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID,"bed_status");
    private static final Provider PROVIDER=new Provider();
    @Override public void register(IWailaCommonRegistration registration){registration.registerBlockDataProvider(PROVIDER,DreamweaversBedBlock.class);}
    @Override public void registerClient(IWailaClientRegistration registration){registration.registerBlockComponent(PROVIDER,DreamweaversBedBlock.class);}

    private static final class Provider implements IServerDataProvider<BlockAccessor>,IBlockComponentProvider {
        @Override public ResourceLocation getUid(){return UID;}
        @Override public void appendServerData(CompoundTag data,BlockAccessor accessor){
            if(!(accessor.getLevel() instanceof ServerLevel level))return;
            var state=accessor.getBlockState(); var bed=state.getValue(BedBlock.PART)==BedPart.HEAD
                    ?accessor.getPosition().relative(state.getValue(BedBlock.FACING).getOpposite()):accessor.getPosition();
            var links=ReverieBedLinksData.get(level.getServer());
            var statusBed=bed;
            if(level.dimension().equals(Reverie.REVERIE_LEVEL)) {
                if(accessor.getPlayer() instanceof ServerPlayer player) {
                    var session=player.getData(ReverieSession.TYPE);
                    if(session.active() && bed.equals(session.dreamBed())) statusBed=session.wakingBed();
                }
                if(statusBed.equals(bed)) {
                    var linkedWakingBed=links.wakingBed(bed);
                    if(linkedWakingBed!=null) statusBed=linkedWakingBed;
                }
            }
            var owner=ReverieBedOwnersData.get(level.getServer()).owner(bed);
            data.putString("Owner",owner==null?"Unclaimed":level.getServer().getProfileCache().get(owner)
                    .map(com.mojang.authlib.GameProfile::getName).orElse("Unknown dreamer"));
            data.putInt("Occupants",links.occupantCount(statusBed));
            data.putInt("Capacity",ReverieConfig.MAX_DREAMERS_PER_BED.get());
            data.putString("Policy",ReverieBedAccessData.get(level.getServer()).policy(statusBed).name());
            var dreamBed=level.dimension().equals(Reverie.REVERIE_LEVEL)?bed:links.dreamBed(bed);
            data.putBoolean("Anchored",dreamBed!=null&&ReverieAnchorsData.get(level.getServer()).contains(dreamBed));
        }
        @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config){
            CompoundTag data=accessor.getServerData(); if(!data.contains("Owner"))return;
            tooltip.add(Component.translatable("jade.reverie.owner",data.getString("Owner")).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("jade.reverie.occupancy",data.getInt("Occupants"),data.getInt("Capacity")));
            String policy=data.getString("Policy");
            if(!"DEFAULT".equals(policy)) {
                tooltip.add(Component.translatable("jade.reverie.access",policy.toLowerCase(java.util.Locale.ROOT)));
            }
            if(data.getBoolean("Anchored")) tooltip.add(Component.translatable("jade.reverie.anchored"));
        }
    }
}
