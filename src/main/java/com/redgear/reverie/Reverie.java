package com.redgear.reverie;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;
import org.slf4j.Logger;

@Mod(Reverie.MOD_ID)
public final class Reverie {
    public static final String MOD_ID = "reverie";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceKey<Level> REVERIE_LEVEL = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MOD_ID, "the_reverie"));
    public static final TagKey<Block> UNUSABLE_BLOCKS = TagKey.create(
            Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(MOD_ID, "unusable_in_reverie"));
    public static final TagKey<Item> UNUSABLE_ITEMS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MOD_ID, "unusable_in_reverie"));
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredBlock<DreamweaversBedBlock> DREAMWEAVERS_BED = BLOCKS.register("dreamweavers_bed",
            () -> new DreamweaversBedBlock(DyeColor.WHITE,
                    BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredItem<DreamweaversBedItem> DREAMWEAVERS_BED_ITEM = ITEMS.register("dreamweavers_bed",
            () -> new DreamweaversBedItem(DREAMWEAVERS_BED.get(), new Item.Properties()));
    public static final DeferredBlock<FigmentCageBlock> FIGMENT_CAGE = BLOCKS.register("figment_cage",
            () -> new FigmentCageBlock(BlockBehaviour.Properties.of().strength(3.0F, 6.0F).sound(SoundType.AMETHYST)
                    .lightLevel(state -> state.getValue(FigmentCageBlock.CHARGES) * 2)));
    public static final DeferredItem<FigmentCageItem> FIGMENT_CAGE_ITEM = ITEMS.register("figment_cage",
            () -> new FigmentCageItem(FIGMENT_CAGE.get(), new Item.Properties()));

    public Reverie(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ReverieSession.ATTACHMENTS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, ReverieConfig.SPEC);
        modBus.addListener(Reverie::addVanillaBedBlockEntitySupport);
        modBus.addListener(Reverie::addCreativeTabContents);
        NeoForge.EVENT_BUS.register(ReverieEvents.class);
        NeoForge.EVENT_BUS.register(ReveriePurgeManager.class);
        NeoForge.EVENT_BUS.register(ReverieDiagnostics.class);
        NeoForge.EVENT_BUS.addListener(ReverieCommands::register);
    }

    private static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(DREAMWEAVERS_BED_ITEM.get());
            event.accept(FIGMENT_CAGE_ITEM.get());
        }
    }

    private static void addVanillaBedBlockEntitySupport(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.BED, DREAMWEAVERS_BED.get());
    }
}
