package com.redgear.reverie.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.redgear.reverie.Reverie;
import com.redgear.reverie.client.ReverieItemRenderContext;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityWithoutLevelRenderer.class)
public abstract class BlockEntityWithoutLevelRendererMixin {
    @Inject(method = "renderByItem", at = @At("HEAD"))
    private void reverie$beginItemRender(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                                          MultiBufferSource buffer, int packedLight, int packedOverlay, CallbackInfo callback) {
        ReverieItemRenderContext.setDreamweaversBed(stack.is(Reverie.DREAMWEAVERS_BED_ITEM.get()));
    }

    @Inject(method = "renderByItem", at = @At("RETURN"))
    private void reverie$endItemRender(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                                        MultiBufferSource buffer, int packedLight, int packedOverlay, CallbackInfo callback) {
        ReverieItemRenderContext.setDreamweaversBed(false);
    }
}
