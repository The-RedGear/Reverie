package com.redgear.reverie.client;

public final class ReverieItemRenderContext {
    private static final ThreadLocal<Boolean> DREAMWEAVERS_BED = ThreadLocal.withInitial(() -> false);
    private ReverieItemRenderContext() {}
    public static boolean isDreamweaversBed() { return DREAMWEAVERS_BED.get(); }
    public static void setDreamweaversBed(boolean value) { DREAMWEAVERS_BED.set(value); }
}
