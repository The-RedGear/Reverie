package com.redgear.reverie.client;

import com.redgear.reverie.Reverie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class ReverieLightingState {
    private static long time = 6000L;
    private static boolean active;

    private ReverieLightingState() {}

    public static void set(long newTime) {
        time = Math.floorMod(newTime, 24000L);
        active = true;
        apply(Minecraft.getInstance().level);
    }

    public static long time(ClientLevel level) {
        return active && level.dimension().equals(Reverie.REVERIE_LEVEL)
                ? time : Math.floorMod(level.getDayTime(), 24000L);
    }

    public static boolean holdsReverieTime() {
        ClientLevel level = Minecraft.getInstance().level;
        return active && level != null && level.dimension().equals(Reverie.REVERIE_LEVEL);
    }

    public static void clientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().equals(Reverie.REVERIE_LEVEL)) {
            active = false;
            return;
        }
        if (active) apply(level);
    }

    private static void apply(ClientLevel level) {
        if (level != null && level.dimension().equals(Reverie.REVERIE_LEVEL)) level.setDayTime(time);
    }
}
