package com.redgear.reverie.client;

import com.redgear.reverie.Reverie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class ReverieSkyEvents {
    private static final float NOON_RED = 0.96F;
    private static final float NOON_GREEN = 0.97F;
    private static final float NOON_BLUE = 0.99F;
    private static final float MIDNIGHT_RED = 0.008F;
    private static final float MIDNIGHT_GREEN = 0.012F;
    private static final float MIDNIGHT_BLUE = 0.025F;

    private ReverieSkyEvents() {}

    public static void computeFogColor(ViewportEvent.ComputeFogColor event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().equals(Reverie.REVERIE_LEVEL)) return;

        float time = level.getTimeOfDay((float) event.getPartialTick());
        float daylight = Mth.clamp((Mth.cos((time - 0.25F) * Mth.TWO_PI) + 1.0F) * 0.5F,
                0.0F, 1.0F);
        // Smooth the endpoints so noon and midnight hold their intended appearance
        // instead of changing abruptly as the Clock crosses them.
        daylight = daylight * daylight * (3.0F - 2.0F * daylight);

        event.setRed(Mth.lerp(daylight, MIDNIGHT_RED, NOON_RED));
        event.setGreen(Mth.lerp(daylight, MIDNIGHT_GREEN, NOON_GREEN));
        event.setBlue(Mth.lerp(daylight, MIDNIGHT_BLUE, NOON_BLUE));
    }
}
