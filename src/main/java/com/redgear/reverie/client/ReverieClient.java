package com.redgear.reverie.client;

import com.redgear.reverie.Reverie;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Vector3f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GrassColor;


@EventBusSubscriber(modid = Reverie.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ReverieClient {
    private static final int PLAINS_GRASS_COLOR = 0x91BD59;
    private ReverieClient() {}

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID, "white"), new WhiteEffects());
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Register explicitly on the gameplay bus. The subscriber bus selector is
        // deprecated in current NeoForge and was not reliably attaching this handler.
        event.enqueueWork(() -> NeoForge.EVENT_BUS.addListener(ReverieSkyEvents::computeFogColor));
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null && level instanceof ClientLevel client
                    && client.dimension().equals(Reverie.REVERIE_LEVEL)) {
                return PLAINS_GRASS_COLOR;
            }
            return level == null || pos == null ? GrassColor.getDefaultColor() : BiomeColors.getAverageGrassColor(level, pos);
        }, Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN);
    }

    private static final class WhiteEffects extends DimensionSpecialEffects {
        private WhiteEffects() {
            super(Float.NaN, true, SkyType.NONE, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 color, float daylight) {
            // The final, time-driven color is applied by ReverieSkyEvents after
            // Minecraft has completed its normal fog calculations.
            return new Vec3(0.96D, 0.97D, 0.99D);
        }

        @Nullable
        @Override
        public float[] getSunriseColor(float timeOfDay, float partialTicks) {
            return null;
        }

        @Override
        public boolean isFoggyAt(int x, int z) {
            return true;
        }

        @Override
        public void adjustLightmapColors(ClientLevel level, float partialTick, float skyDarken,
                                         float blockLight, float skyLight, int pixelX, int pixelY,
                                         Vector3f colors) {
            // Exact noon is Reverie's deliberately ambient-lit blank canvas. At every
            // other time leave Minecraft's lightmap untouched for accurate previews.
            if (Math.floorMod(level.getDayTime(), 24000L) == 6000L) {
                colors.set(1.0F, 1.0F, 1.0F);
            }
        }
    }
}
