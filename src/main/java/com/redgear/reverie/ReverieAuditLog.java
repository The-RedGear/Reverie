package com.redgear.reverie;

import net.minecraft.server.level.ServerPlayer;

/** Concise, grep-friendly records in the normal server log. */
public final class ReverieAuditLog {
    private ReverieAuditLog() {}
    public static void record(ServerPlayer player, String action, Object detail) {
        if (!ReverieConfig.AUDIT_LOG_ENABLED.get()) return;
        Reverie.LOGGER.info("[AUDIT] player={} uuid={} action={} detail={}",
                player.getGameProfile().getName(), player.getUUID(), action, detail);
    }
    public static void admin(String name, String action, Object detail) {
        if (ReverieConfig.AUDIT_LOG_ENABLED.get()) Reverie.LOGGER.info("[AUDIT] admin={} action={} detail={}", name, action, detail);
    }
}
