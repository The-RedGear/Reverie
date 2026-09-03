package com.redgear.reverie;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import java.util.List;

public final class FigmentCageItem extends BlockItem {
    public FigmentCageItem(Block block, Properties properties) { super(block, properties); }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!Screen.hasShiftDown()) { tooltip.add(Component.translatable("tooltip.reverie.hold_shift").withStyle(ChatFormatting.GRAY)); return; }
        tooltip.add(Component.translatable("tooltip.reverie.cage.1").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.reverie.cage.2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.reverie.cage.3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.reverie.cage.4").withStyle(ChatFormatting.GRAY));
    }
}
