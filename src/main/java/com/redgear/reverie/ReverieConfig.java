package com.redgear.reverie;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative gameplay settings, stored in each world's serverconfig folder. */
public final class ReverieConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_DREAMERS_PER_BED;
    public static final ModConfigSpec.ConfigValue<String> SHARED_BED_COST_ITEM;
    public static final ModConfigSpec.BooleanValue CONSUME_SHARED_BED_COST;
    public static final ModConfigSpec.BooleanValue ANCHOR_DREAM_INVENTORIES;
    public static final ModConfigSpec.IntValue OVERSTAY_WARNING_MINUTES;
    public static final ModConfigSpec.IntValue MAX_DREAM_MINUTES;
    public static final ModConfigSpec.ConfigValue<String> OVERSTAY_EFFECT;
    public static final ModConfigSpec.IntValue OVERSTAY_EFFECT_SECONDS;
    public static final ModConfigSpec.IntValue OVERSTAY_EFFECT_AMPLIFIER;
    public static final ModConfigSpec.BooleanValue PLAYER_CLOCK_TIME_CONTROL;
    public static final ModConfigSpec.IntValue CLOCK_TIME_STEP;
    public static final ModConfigSpec.IntValue CLOCK_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue GLOBAL_CLOCK_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue CLOCK_RESET_MINUTES;
    public static final ModConfigSpec.IntValue FIGMENT_CAGE_CHUNK_RADIUS;
    public static final ModConfigSpec.IntValue FIGMENT_CAGE_MAX_MOBS;
    public static final ModConfigSpec.ConfigValue<String> FIGMENT_CAGE_CHARGE_ITEM;
    public static final ModConfigSpec.BooleanValue AUTOMATIC_BLOCK_PURGE;
    public static final ModConfigSpec.IntValue PURGE_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue PURGE_BLOCKS_PER_BATCH;
    public static final ModConfigSpec.IntValue AUTOMATIC_PURGE_MINUTES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("bed_access");
        MAX_DREAMERS_PER_BED = builder.comment("Maximum simultaneous dreamers linked through one Overworld bed.")
                .defineInRange("maxDreamersPerBed", 2, 1, 100);
        SHARED_BED_COST_ITEM = builder.comment("Item a non-owner must use to enter another player's bed. Empty disables the cost.")
                .define("sharedBedCostItem", "minecraft:amethyst_shard");
        CONSUME_SHARED_BED_COST = builder.comment("Whether the guest entry item is consumed after a successful transition.")
                .define("consumeSharedBedCost", true);
        builder.pop();

        builder.push("reverie_time");
        PLAYER_CLOCK_TIME_CONTROL = builder.comment("Allow dreamers to change the shared frozen Reverie time with a Clock.")
                .define("playerClockControl", true);
        CLOCK_TIME_STEP = builder.comment("Ticks advanced when a player uses a Clock in the Reverie.")
                .defineInRange("clockStepTicks", 1000, 1, 24000);
        CLOCK_COOLDOWN_TICKS = builder.comment("Cooldown after using a Clock. Twenty ticks are approximately one second.")
                .defineInRange("clockCooldownTicks", 40, 1, 1200);
        GLOBAL_CLOCK_COOLDOWN_TICKS = builder.comment("Shared cooldown before any dreamer can change time again.")
                .defineInRange("globalClockCooldownTicks", 40, 1, 1200);
        CLOCK_RESET_MINUTES = builder.comment("Minutes before player-selected lighting returns to noon. Zero disables the timer; the setting player leaving always resets it.")
                .defineInRange("returnToNoonMinutes", 5, 0, 1440);
        builder.pop();

        builder.push("dream_inventory");
        ANCHOR_DREAM_INVENTORIES = builder.comment("Save a separate creative inventory per player and permanent Dream Anchor.")
                .define("anchorScopedInventories", true);
        builder.pop();

        builder.push("overstaying");
        OVERSTAY_WARNING_MINUTES = builder.comment("Minutes before a dreamer is warned and becomes eligible for the wake-up effect. Zero disables.")
                .defineInRange("warningMinutes", 45, 0, 10080);
        MAX_DREAM_MINUTES = builder.comment("Minutes before the player is automatically awakened. Zero disables forced waking.")
                .defineInRange("maximumDreamMinutes", 60, 0, 10080);
        OVERSTAY_EFFECT = builder.comment("Effect applied after ignoring the warning, such as minecraft:slowness. Empty disables.")
                .define("wakeUpEffect", "minecraft:slowness");
        OVERSTAY_EFFECT_SECONDS = builder.comment("Duration of the wake-up effect in seconds.")
                .defineInRange("wakeUpEffectSeconds", 240, 1, 86400);
        OVERSTAY_EFFECT_AMPLIFIER = builder.comment("Effect strength, where 0 is level I.")
                .defineInRange("wakeUpEffectAmplifier", 0, 0, 255);
        builder.pop();
        builder.push("figment_cage");
        FIGMENT_CAGE_CHUNK_RADIUS = builder.comment("Maximum charges per Figment Cage. Four matches a Respawn Anchor and permits a 9x9-chunk region.")
                .defineInRange("maximumCharges", 4, 0, 4);
        FIGMENT_CAGE_MAX_MOBS = builder.comment("Maximum mobs sustained by one Figment Cage.")
                .defineInRange("maximumMobs", 16, 1, 128);
        FIGMENT_CAGE_CHARGE_ITEM = builder.comment("Item used to add one range charge to a Figment Cage.")
                .define("chargeItem", "minecraft:echo_shard");
        builder.pop();
        builder.push("illegal_content_purge");
        AUTOMATIC_BLOCK_PURGE = builder.comment("Run recurring loaded-chunk audits. Off by default; immediate placement and item enforcement still remain active.")
                .define("automaticEnabled", false);
        PURGE_INTERVAL_TICKS = builder.comment("Ticks between incremental purge batches while an audit is running.")
                .defineInRange("batchIntervalTicks", 5, 1, 1200);
        PURGE_BLOCKS_PER_BATCH = builder.comment("Maximum block positions inspected in each incremental batch.")
                .defineInRange("blocksPerBatch", 4096, 64, 65536);
        AUTOMATIC_PURGE_MINUTES = builder.comment("Minutes between recurring audits when automaticEnabled is true.")
                .defineInRange("automaticIntervalMinutes", 30, 1, 10080);
        builder.pop();
        SPEC = builder.build();
    }

    private ReverieConfig() {}
}
