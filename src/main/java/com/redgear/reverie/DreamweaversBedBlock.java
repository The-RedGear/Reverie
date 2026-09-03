package com.redgear.reverie;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.Block;

/** A real vanilla-style, two-block bed with Reverie's travel behavior supplied by the event handler. */
public final class DreamweaversBedBlock extends BedBlock {
    public static final BooleanProperty ANCHORED = BooleanProperty.create("anchored");
    public static final BooleanProperty DREAMING = BooleanProperty.create("dreaming");

    public DreamweaversBedBlock(DyeColor color, BlockBehaviour.Properties properties) {
        super(color, properties);
        registerDefaultState(defaultBlockState().setValue(ANCHORED, false).setValue(DREAMING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, net.minecraft.world.level.block.state.BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ANCHORED, DREAMING);
    }
}
