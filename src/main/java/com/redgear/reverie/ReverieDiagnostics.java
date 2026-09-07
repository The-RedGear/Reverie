package com.redgear.reverie;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.ModList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.ArrayList;
import java.util.List;

/** Shared validation for startup logging and /reverie doctor. */
public final class ReverieDiagnostics {
    private ReverieDiagnostics() {}

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        List<String> problems = problems(event.getServer());
        if (problems.isEmpty()) Reverie.LOGGER.info("Reverie configuration validation passed.");
        else problems.forEach(problem -> Reverie.LOGGER.warn("Reverie configuration: {}", problem));
    }

    public static List<String> problems(MinecraftServer server) {
        List<String> problems = new ArrayList<>();
        validateItem(ReverieConfig.SHARED_BED_COST_ITEM.get(), "sharedBedCostItem", problems);
        validateItem(ReverieConfig.FIGMENT_CAGE_CHARGE_ITEM.get(), "Figment Cage chargeItem", problems);
        String effect = ReverieConfig.OVERSTAY_EFFECT.get();
        ResourceLocation effectId = ResourceLocation.tryParse(effect);
        if (!effect.isBlank() && (effectId == null || !BuiltInRegistries.MOB_EFFECT.containsKey(effectId)))
            problems.add("wakeUpEffect '" + effect + "' is invalid; no wake-up effect will be applied");
        String voidMode=ReverieConfig.VOID_RECOVERY_MODE.get().trim().toLowerCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("awaken","bed","disabled").contains(voidMode))
            problems.add("voidRecoveryMode '"+voidMode+"' is invalid; use awaken, bed, or disabled");
        if (ReverieConfig.MAX_DREAM_MINUTES.get()>0 && ReverieConfig.OVERSTAY_WARNING_MINUTES.get()
                >=ReverieConfig.MAX_DREAM_MINUTES.get())
            problems.add("warningMinutes should be lower than maximumDreamMinutes so players receive a useful warning");
        if (server.getLevel(Reverie.REVERIE_LEVEL) == null) problems.add("the Reverie dimension did not load");
        for (var player:server.getPlayerList().getPlayers()) {
            var session=player.getData(ReverieSession.TYPE);
            if(session.active()&&!player.level().dimension().equals(Reverie.REVERIE_LEVEL))
                problems.add(player.getGameProfile().getName()+" has an active Reverie session outside the Reverie");
            if(!session.active()&&player.level().dimension().equals(Reverie.REVERIE_LEVEL))
                problems.add(player.getGameProfile().getName()+" is in the Reverie without an active session");
        }
        return problems;
    }

    private static void validateItem(String value, String name, List<String> problems) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (!value.isBlank() && (id == null || !BuiltInRegistries.ITEM.containsKey(id)))
            problems.add(name + " '" + value + "' is invalid; that mechanic will be disabled safely");
    }

    public static String compatibility() {
        return "Accessories=" + installed("accessories") + ", Curios=" + installed("curios")
                + ", Create=" + installed("create") + ", Jade=" + installed("jade") + ", JEI=" + installed("jei");
    }

    private static String installed(String id) { return ModList.get().isLoaded(id) ? "detected" : "absent"; }
}
