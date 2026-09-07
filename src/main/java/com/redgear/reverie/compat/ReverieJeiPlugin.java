package com.redgear.reverie.compat;

import com.redgear.reverie.Reverie;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class ReverieJeiPlugin implements IModPlugin {
    @Override public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID,"information");}
    @Override public void registerRecipes(IRecipeRegistration registration){
        registration.addIngredientInfo(Reverie.DREAMWEAVERS_BED_ITEM.get(),
                Component.translatable("jei.reverie.bed.1"),Component.translatable("jei.reverie.bed.2"),
                Component.translatable("jei.reverie.bed.3"),Component.translatable("jei.reverie.bed.4"));
        registration.addIngredientInfo(Reverie.FIGMENT_CAGE_ITEM.get(),
                Component.translatable("jei.reverie.cage.1"),Component.translatable("jei.reverie.cage.2"));
    }
}
