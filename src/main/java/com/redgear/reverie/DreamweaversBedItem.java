package com.redgear.reverie;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.UUID;

public final class DreamweaversBedItem extends BlockItem {
    private static final String OWNER_ID = "ReverieBedOwner";
    private static final String OWNER_NAME = "ReverieBedOwnerName";

    public DreamweaversBedItem(Block block, Properties properties) { super(block, properties); }

    public static UUID owner(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.hasUUID(OWNER_ID) ? tag.getUUID(OWNER_ID) : null;
    }

    public static void bind(ItemStack stack, UUID owner, String name) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putUUID(OWNER_ID, owner);
            tag.putString(OWNER_NAME, name);
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var ownerData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tooltip.add(ownerData.hasUUID(OWNER_ID)
                ? Component.translatable("tooltip.reverie.bound_to", ownerData.getString(OWNER_NAME))
                        .withStyle(ChatFormatting.GOLD)
                : Component.translatable("tooltip.reverie.unbound").withStyle(ChatFormatting.GRAY));
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.reverie.hold_shift").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.reverie.anchor.1").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.reverie.anchor.2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.reverie.anchor.3").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.reverie.anchor.4").withStyle(ChatFormatting.GRAY));
    }
}
