package com.redgear.reverie.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.redgear.reverie.Reverie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class ReverieBedLabelRenderer {
    private ReverieBedLabelRenderer() {}

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        for (var occupancy : ReverieClient.occupiedBeds()) renderBed(event, occupancy);
    }

    private static void renderBed(RenderLevelStageEvent event,
                                  com.redgear.reverie.ReverieBedOccupancyPayload occupancy) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos foot = BlockPos.of(occupancy.bedPos());
        BlockState state = minecraft.level.getBlockState(foot);
        if (!state.is(Reverie.DREAMWEAVERS_BED.get())) return;

        Direction facing = state.getValue(BedBlock.FACING);
        var camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(foot.getX() + 0.5D + facing.getStepX() * 0.5D - camera.x,
                foot.getY() + 1.35D - camera.y,
                foot.getZ() + 0.5D + facing.getStepZ() * 0.5D - camera.z);
        poses.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        poses.scale(-0.025F, -0.025F, 0.025F);

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        int line = 0;
        if (!occupancy.owner().isEmpty()) draw(minecraft.font, occupancy.owner(), 0xFFF2D6, line++, poses, buffers);
        if (!occupancy.guests().isEmpty()) {
            for (String guest : occupancy.guests().split("\\u0000")) {
                if (!guest.isEmpty()) draw(minecraft.font, guest, 0xFFFFFF, line++, poses, buffers);
            }
        }
        buffers.endBatch();
        poses.popPose();
    }

    private static void draw(Font font, String name, int color, int line, PoseStack poses,
                             MultiBufferSource buffers) {
        font.drawInBatch(name, -font.width(name) / 2.0F, line * 10.0F, color, false,
                poses.last().pose(), buffers, Font.DisplayMode.NORMAL, 0x55000000, 0x00F000F0);
    }
}
