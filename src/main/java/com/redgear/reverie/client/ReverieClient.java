package com.redgear.reverie.client;

import com.redgear.reverie.Reverie;
import com.redgear.reverie.ReverieNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.Items;


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
        event.enqueueWork(() -> {
            ReverieNetwork.CLIENT_DESTINATIONS = payload -> Minecraft.getInstance().setScreen(
                    new ReverieDestinationScreen(payload.destinations()));
            ReverieNetwork.CLIENT_LIGHTING = ReverieLightingState::set;
            NeoForge.EVENT_BUS.addListener(ReverieSkyEvents::computeFogColor);
            NeoForge.EVENT_BUS.addListener(ReverieLightingState::clientTick);
            NeoForge.EVENT_BUS.addListener(ReverieClient::dreamTooltips);
            ItemProperties.register(Items.RECOVERY_COMPASS, ResourceLocation.withDefaultNamespace("angle"),
                    new CompassItemPropertyFunction((level, stack, entity) -> {
                        if (entity.level().dimension().equals(Reverie.REVERIE_LEVEL)) {
                            var tracker = stack.get(DataComponents.LODESTONE_TRACKER);
                            if (tracker != null) return tracker.target().orElse(null);
                        }
                        return entity instanceof net.minecraft.world.entity.player.Player player
                                ? player.getLastDeathLocation().orElse(null) : null;
                    }));
        });
    }

    private static void dreamTooltips(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        var player = event.getEntity();
        if (player == null || !player.level().dimension().equals(Reverie.REVERIE_LEVEL)) return;
        var stack = event.getItemStack();
        String key = stack.is(Items.CLOCK) ? "tooltip.reverie.clock"
                : stack.is(Items.RECOVERY_COMPASS) ? "tooltip.reverie.compass"
                : stack.is(Items.NAME_TAG) ? "tooltip.reverie.bookmarks"
                : stack.is(Items.BOOK) ? "tooltip.reverie.imprint" : null;
        if (key != null) event.getToolTip().add(net.minecraft.network.chat.Component.translatable(key)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
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
            if (ReverieLightingState.time(level) == 6000L) {
                colors.set(1.0F, 1.0F, 1.0F);
            }
        }
    }
}
