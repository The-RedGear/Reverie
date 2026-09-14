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
    private static final CageProvider CAGE_PROVIDER=new CageProvider();
    @Override public void register(IWailaCommonRegistration registration){registration.registerBlockDataProvider(PROVIDER,DreamweaversBedBlock.class);registration.registerBlockDataProvider(CAGE_PROVIDER,FigmentCageBlock.class);}
    @Override public void registerClient(IWailaClientRegistration registration){registration.registerBlockComponent(PROVIDER,DreamweaversBedBlock.class);registration.registerBlockComponent(CAGE_PROVIDER,FigmentCageBlock.class);}

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
            var owner=ReverieBedOwnersData.get(level.getServer()).owner(level.dimension(),bed);
            if(owner==null&&!statusBed.equals(bed)) owner=ReverieBedOwnersData.get(level.getServer())
                    .owner(net.minecraft.world.level.Level.OVERWORLD,statusBed);
            data.putString("Owner",owner==null?"Unclaimed":level.getServer().getProfileCache().get(owner)
                    .map(com.mojang.authlib.GameProfile::getName).orElse("Unknown dreamer"));
            data.putInt("Occupants",links.occupantCount(statusBed));
            data.putInt("Capacity",ReverieConfig.MAX_DREAMERS_PER_BED.get());
            data.putString("Policy",ReverieBedAccessData.get(level.getServer()).policy(statusBed).name());
            var dreamBed=level.dimension().equals(Reverie.REVERIE_LEVEL)?bed:links.dreamBed(bed);
            // Idle anchored links are intentionally removed when the last dreamer leaves,
            // so Overworld inspection must also resolve the anchor by its chunk region.
            if(dreamBed==null&&level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD))
                dreamBed=ReverieAnchorsData.get(level.getServer()).findFor(bed);
            data.putBoolean("Anchored",dreamBed!=null&&ReverieAnchorsData.get(level.getServer()).contains(dreamBed));
            data.putString("AnchorName",dreamBed==null?"":ReverieAnchorsData.get(level.getServer()).name(dreamBed));
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
            if(data.getBoolean("Anchored")&&!data.getString("AnchorName").isBlank())tooltip.add(Component.translatable("jade.reverie.anchor_name",data.getString("AnchorName")));
        }
    }
    private static final class CageProvider implements IServerDataProvider<BlockAccessor>,IBlockComponentProvider{
        private static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID,"cage_status");
        @Override public ResourceLocation getUid(){return ID;}
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getLevel() instanceof ServerLevel level)) return;
            int charges = FigmentCagesData.get(level.getServer()).radius(accessor.getPosition());
            data.putInt("Charges", charges);
            data.putInt("MaxCharges", ReverieConfig.FIGMENT_CAGE_CHUNK_RADIUS.get());
            data.putInt("Width", charges * 2 + 1);
            data.putInt("Mobs", ReverieEvents.cagePopulation(level, accessor.getPosition()));
            data.putInt("MaxMobs", ReverieConfig.FIGMENT_CAGE_MAX_MOBS.get());
        }
        @Override public void appendTooltip(ITooltip tooltip,BlockAccessor accessor,IPluginConfig config){CompoundTag d=accessor.getServerData();if(!d.contains("Charges"))return;tooltip.add(Component.translatable("jade.reverie.cage_charge",d.getInt("Charges"),d.getInt("MaxCharges")));tooltip.add(Component.translatable("jade.reverie.cage_region",d.getInt("Width"),d.getInt("Width")));tooltip.add(Component.translatable("jade.reverie.cage_mobs",d.getInt("Mobs"),d.getInt("MaxMobs")));}
    }
}
