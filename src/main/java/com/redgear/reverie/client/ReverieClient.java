package com.redgear.reverie.client;

import com.redgear.reverie.Reverie;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Vector3f;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

@EventBusSubscriber(modid = Reverie.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ReverieClient {
    private ReverieClient() {}

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID, "white"), new WhiteEffects());
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
            colors.set(1.0F, 1.0F, 1.0F);
        }
    }
}
