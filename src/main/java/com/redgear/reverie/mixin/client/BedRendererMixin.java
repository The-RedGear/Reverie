package com.redgear.reverie.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.redgear.reverie.Reverie;
import com.redgear.reverie.DreamweaversBedBlock;
import com.redgear.reverie.client.ReverieItemRenderContext;
import com.redgear.reverie.client.ReverieClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BedRenderer.class)
public abstract class BedRendererMixin {
    @Shadow @Final private ModelPart headRoot;
    @Shadow @Final private ModelPart footRoot;
    private static final Material REVERIE_BED = new Material(
            Sheets.BED_SHEET,
            ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID, "entity/bed/dreamweavers_bed"));

    @ModifyVariable(method = "render", at = @At(value = "STORE"), ordinal = 0)
    private Material reverie$useDreamweaversTexture(Material original, BedBlockEntity blockEntity) {
        return blockEntity.getBlockState().is(Reverie.DREAMWEAVERS_BED.get())
                || ReverieItemRenderContext.isDreamweaversBed() ? REVERIE_BED : original;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void reverie$renderPearlAnchorShimmer(BedBlockEntity blockEntity, float partialTick,
                                                  PoseStack poseStack, MultiBufferSource buffers,
                                                  int packedLight, int packedOverlay, CallbackInfo callback) {
        if (blockEntity.getLevel() == null
                || !blockEntity.getBlockState().is(Reverie.DREAMWEAVERS_BED.get())
                || !(blockEntity.getBlockState().getValue(DreamweaversBedBlock.ANCHORED)
                || blockEntity.getBlockState().getValue(DreamweaversBedBlock.DREAMING))) return;
        boolean foot = blockEntity.getBlockState().getValue(BedBlock.PART) == BedPart.FOOT;
        ModelPart model = foot ? footRoot : headRoot;
        Direction direction = blockEntity.getBlockState().getValue(BedBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5625F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + direction.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        VertexConsumer shimmer = buffers.getBuffer(RenderType.entityGlint());
        // A low-alpha warm-white tint turns the normal glint into a restrained pearl sheen.
        model.render(poseStack, shimmer, 0x00F000F0, packedOverlay, 0x55FFF8E8);
        poseStack.popPose();

        var occupancy = ReverieClient.hoveredBed();
        BlockPos renderedFoot = foot ? blockEntity.getBlockPos()
                : blockEntity.getBlockPos().relative(direction.getOpposite());
        // Vanilla renders beds from the head block entity. Compare its canonical
        // foot position with the server's occupancy key and draw only once.
        if (!occupancy.visible() || occupancy.bedPos() != renderedFoot.asLong() || foot) return;
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.35D, 0.5D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);
        Font font = Minecraft.getInstance().font;
        int line = 0;
        if (!occupancy.owner().isEmpty()) {
            drawName(font, occupancy.owner(), 0xFFF2D6, line++, poseStack, buffers, packedLight);
        }
        if (!occupancy.guests().isEmpty()) {
            for (String guest : occupancy.guests().split("\\u0000")) {
                if (!guest.isEmpty()) drawName(font, guest, 0xFFFFFF, line++, poseStack, buffers, packedLight);
            }
        }
        poseStack.popPose();
    }

    private static void drawName(Font font, String name, int color, int line, PoseStack poses,
                                 MultiBufferSource buffers, int packedLight) {
        float x = -font.width(name) / 2.0F;
        font.drawInBatch(name, x, line * 10.0F, color, false, poses.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0x55000000, packedLight);
    }
}
