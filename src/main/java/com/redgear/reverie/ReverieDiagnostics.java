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
        if (server.getLevel(Reverie.REVERIE_LEVEL) == null) problems.add("the Reverie dimension did not load");
        return problems;
    }

    private static void validateItem(String value, String name, List<String> problems) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (!value.isBlank() && (id == null || !BuiltInRegistries.ITEM.containsKey(id)))
            problems.add(name + " '" + value + "' is invalid; that mechanic will be disabled safely");
    }

    public static String compatibility() {
        return "Accessories=" + installed("accessories") + ", Curios=" + installed("curios")
                + ", Create=" + installed("create");
    }

    private static String installed(String id) { return ModList.get().isLoaded(id) ? "detected" : "absent"; }
}
