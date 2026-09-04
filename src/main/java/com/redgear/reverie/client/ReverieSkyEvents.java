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

        long ticks = Math.floorMod(level.getDayTime(), 24000L);
        float[] from;
        float[] to;
        float progress;
        if (ticks < 6000L) {
            from = DAWN; to = NOON; progress = ticks / 6000.0F;
        } else if (ticks < 12000L) {
            from = NOON; to = DUSK; progress = (ticks - 6000L) / 6000.0F;
        } else if (ticks < 18000L) {
            from = DUSK; to = MIDNIGHT; progress = (ticks - 12000L) / 6000.0F;
        } else {
            from = MIDNIGHT; to = DAWN; progress = (ticks - 18000L) / 6000.0F;
        }
        progress = progress * progress * (3.0F - 2.0F * progress);
        event.setRed(Mth.lerp(progress, from[0], to[0]));
        event.setGreen(Mth.lerp(progress, from[1], to[1]));
        event.setBlue(Mth.lerp(progress, from[2], to[2]));
    }

    private static final float[] DAWN = {0.92F, 0.94F, 0.98F};
    private static final float[] NOON = {NOON_RED, NOON_GREEN, NOON_BLUE};
    private static final float[] DUSK = {0.42F, 0.44F, 0.50F};
    private static final float[] MIDNIGHT = {MIDNIGHT_RED, MIDNIGHT_GREEN, MIDNIGHT_BLUE};
}
