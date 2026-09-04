package com.redgear.reverie.client;

import com.redgear.reverie.Reverie;
import com.redgear.reverie.ReverieBiomeTintPayload;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Vector3f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Reverie.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ReverieClient {
    private static final Map<Long, Integer> GRASS_TINTS = new HashMap<>();
    private ReverieClient() {}

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID, "white"), new WhiteEffects());
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null && level instanceof ClientLevel client
                    && client.dimension().equals(Reverie.REVERIE_LEVEL)) {
                Integer tint = GRASS_TINTS.get(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
                if (tint != null) return tint;
            }
            return level == null || pos == null ? -1 : BiomeColors.getAverageGrassColor(level, pos);
        }, Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN);
    }

    public static void handleBiomeTint(ReverieBiomeTintPayload payload, IPayloadContext context) {
        GRASS_TINTS.clear();
        for (int dx = -payload.radius(); dx <= payload.radius(); dx++) {
            for (int dz = -payload.radius(); dz <= payload.radius(); dz++) {
                GRASS_TINTS.put(ChunkPos.asLong(payload.centerChunkX() + dx, payload.centerChunkZ() + dz),
                        payload.grassColor());
            }
        }
        net.minecraft.client.Minecraft.getInstance().levelRenderer.allChanged();
    }

    private static final class WhiteEffects extends DimensionSpecialEffects {
        private WhiteEffects() {
            super(Float.NaN, true, SkyType.NONE, true, true);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 color, float daylight) {
            return new Vec3(0.96D, 0.97D, 0.99D);
        }

        @Override
        public boolean isFoggyAt(int x, int z) {
            return true;
        }

        @Override
        public void adjustLightmapColors(ClientLevel level, float partialTick, float skyDarken,
                                         float blockLight, float skyLight, int pixelX, int pixelY,
                                         Vector3f colors) {
            // Preserve Reverie's original pearly, full-bright daytime canvas. Darker Clock
            // settings deliberately reveal real block and sky lighting for build tests.
            float darkness = Mth.clamp(skyDarken * 1.25F, 0.0F, 1.0F);
            if (darkness <= 0.05F) {
                colors.set(1.0F, 1.0F, 1.0F);
                return;
            }
            float localLight = Math.max(blockLight, skyLight * (1.0F - darkness));
            float brightness = Mth.clamp(0.12F + localLight * 0.88F, 0.12F, 1.0F);
            colors.set(brightness * 0.97F, brightness * 0.98F, brightness);
        }
    }
}
