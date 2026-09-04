package com.redgear.reverie;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashSet;
import java.util.Set;

public final class ReverieCommands {
    private ReverieCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("reverie").requires(source -> source.hasPermission(2))
                .then(Commands.literal("blocklist")
                        .then(editBranch("add", true))
                        .then(editBranch("remove", false))
                        .then(Commands.literal("list").executes(context -> list(context.getSource()))))
                .then(Commands.literal("awaken")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> awaken(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("recovery")
                        .then(Commands.literal("restore").then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> recover(context.getSource(), EntityArgument.getPlayer(context, "player"), false))))
                        .then(Commands.literal("rollback").then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> recover(context.getSource(), EntityArgument.getPlayer(context, "player"), true))))
                        .then(Commands.literal("status").then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> recoveryStatus(context.getSource(), EntityArgument.getPlayer(context, "player")))))));
        dispatcher.register(Commands.literal("reverie").requires(source -> source.hasPermission(2))
                .then(Commands.literal("sessions").then(Commands.literal("list")
                        .executes(context -> listSessions(context.getSource()))))
                .then(Commands.literal("anchors")
                        .then(Commands.literal("list").executes(context -> listAnchors(context.getSource())))
                        .then(Commands.literal("remove").then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> removeAnchor(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"))))))
                .then(Commands.literal("bed").then(Commands.literal("owner")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setBedOwner(context.getSource(), EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("inventory").then(Commands.literal("clear")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("anchor", BlockPosArgument.blockPos())
                                        .executes(context -> clearDreamInventory(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                BlockPosArgument.getLoadedBlockPos(context, "anchor")))))))
                .then(Commands.literal("config").then(Commands.literal("show")
                        .executes(context -> showConfig(context.getSource()))))
                .then(Commands.literal("audit").then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> audit(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("purge")
                        .then(Commands.literal("run").executes(context -> startPurge(context.getSource())))
                        .then(Commands.literal("force").executes(context -> forcePurge(context.getSource())))
                        .then(Commands.literal("status").executes(context -> purgeStatus(context.getSource())))
                        .then(Commands.literal("stop").executes(context -> stopPurge(context.getSource()))))
                .then(Commands.literal("doctor").executes(context -> doctor(context.getSource())))
                .then(Commands.literal("time")
                        .then(Commands.literal("query").executes(context -> queryTime(context.getSource())))
                        .then(Commands.literal("day").executes(context -> setTime(context.getSource(), 1000L, "day")))
                        .then(Commands.literal("noon").executes(context -> setTime(context.getSource(), 6000L, "noon")))
                        .then(Commands.literal("night").executes(context -> setTime(context.getSource(), 13000L, "night")))
                        .then(Commands.literal("midnight").executes(context -> setTime(context.getSource(), 18000L, "midnight")))
                        .then(Commands.literal("reset").executes(context -> setTime(context.getSource(), ReverieTimeData.DEFAULT_TIME, "noon")))
                        .then(Commands.literal("set")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 23999))
                                        .executes(context -> setTime(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "ticks"), "custom")))))
                .then(Commands.literal("cleanup").executes(context -> cleanup(context.getSource()))));
        Set<ResourceLocation> entitySuggestions = new HashSet<>(BuiltInRegistries.ENTITY_TYPE.keySet());
        dispatcher.register(Commands.literal("reverie").requires(source -> source.hasPermission(2))
                .then(Commands.literal("moblist")
                        .then(Commands.literal("add").then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(entitySuggestions, builder))
                                .executes(context -> editMobList(context.getSource(), ResourceLocationArgument.getId(context, "id"), true))))
                        .then(Commands.literal("remove").then(Commands.argument("id", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(entitySuggestions, builder))
                                .executes(context -> editMobList(context.getSource(), ResourceLocationArgument.getId(context, "id"), false))))
                        .then(Commands.literal("list").executes(context -> listMobs(context.getSource())))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> editBranch(String name, boolean add) {
        Set<ResourceLocation> suggestions = new HashSet<>(BuiltInRegistries.BLOCK.keySet());
        suggestions.addAll(BuiltInRegistries.ITEM.keySet());
        return Commands.literal(name)
                .then(Commands.literal("hand").executes(context -> editHand(context.getSource(), add)))
                .then(Commands.argument("id", ResourceLocationArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(suggestions, builder))
                        .executes(context -> edit(context.getSource(), ResourceLocationArgument.getId(context, "id"), add)));
    }

    private static int editHand(CommandSourceStack source, boolean add) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("Hold a block or item in your main hand first."));
            return 0;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        int result = edit(source, itemId, add);
        if (stack.getItem() instanceof BlockItem blockItem) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            if (!blockId.equals(itemId)) result |= edit(source, blockId, add);
        }
        return result;
    }

    private static int edit(CommandSourceStack source, ResourceLocation id, boolean add) {
        boolean isBlock = BuiltInRegistries.BLOCK.containsKey(id);
        boolean isItem = BuiltInRegistries.ITEM.containsKey(id);
        if (!isBlock && !isItem) {
            source.sendFailure(Component.literal("Unknown block or item: " + id));
            return 0;
        }
        ReverieBlocklistData data = ReverieBlocklistData.get(source.getServer());
        boolean changed = false;
        if (isBlock) changed |= add ? data.addBlock(id) : data.removeBlock(id);
        if (isItem) changed |= add ? data.addItem(id) : data.removeItem(id);
        String type = isBlock && isItem ? "block and item" : isBlock ? "block" : "item";
        String action = add ? "Blocked " : "Allowed ";
        boolean result = changed;
        source.sendSuccess(() -> Component.literal(action + type + " " + id + (result ? "" : " (already set)")), true);
        if (add && changed) {
            ServerLevel reverie = source.getServer().getLevel(Reverie.REVERIE_LEVEL);
            if (reverie != null) ReveriePurgeManager.start(reverie);
        }
        return changed ? 1 : 0;
    }

    private static int list(CommandSourceStack source) {
        ReverieBlocklistData data = ReverieBlocklistData.get(source.getServer());
        sendSet(source, "Added blocks", data.blockedBlocks());
        sendSet(source, "Removed blocks", data.allowedBlocks());
        sendSet(source, "Added items", data.blockedItems());
        sendSet(source, "Removed items", data.allowedItems());
        source.sendSuccess(() -> Component.literal("Datapack-tag entries remain active unless shown under Removed."), false);
        return 1;
    }

    private static void sendSet(CommandSourceStack source, String label, Set<ResourceLocation> values) {
        String text = values.stream().sorted().map(ResourceLocation::toString).reduce((a, b) -> a + ", " + b).orElse("(none)");
        source.sendSuccess(() -> Component.literal(label + ": " + text), false);
    }

    private static int awaken(CommandSourceStack source, ServerPlayer player) {
        if (!ReverieEvents.awaken(player, null)) {
            source.sendFailure(Component.literal(player.getGameProfile().getName() + " is not currently in an active Reverie session."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Awakened " + player.getGameProfile().getName() + " and restored their waking state."), true);
        return 1;
    }

    private static int recover(CommandSourceStack source, ServerPlayer player, boolean rollback) {
        if (!ReverieEvents.restoreRecovery(player, rollback)) {
            source.sendFailure(Component.literal("No " + (rollback ? "rollback" : "waking") + " recovery snapshot exists for " + player.getGameProfile().getName() + "."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Restored " + player.getGameProfile().getName() + " from the " + (rollback ? "pre-recovery rollback" : "latest waking backup") + "."), true);
        return 1;
    }

    private static int recoveryStatus(CommandSourceStack source, ServerPlayer player) {
        ReverieRecoveryData.Entry entry = ReverieRecoveryData.get(source.getServer()).entry(player.getUUID());
        if (entry == null) { source.sendFailure(Component.literal("No recovery snapshot exists for " + player.getGameProfile().getName() + ".")); return 0; }
        source.sendSuccess(() -> Component.literal("Recovery for " + entry.name() + ": "
                + (entry.active() ? "ACTIVE DREAM" : "retained backup") + ", rollback=" + (entry.hasRollback() ? "available" : "none")
                + ", captured at game time " + entry.gameTime()), false);
        return 1;
    }

    private static int listSessions(CommandSourceStack source) {
        int count = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            ReverieSession session = player.getData(ReverieSession.TYPE);
            if (!session.active()) continue;
            count++;
            long seconds = session.dreamElapsedTicks() / 20L;
            source.sendSuccess(() -> Component.literal(player.getGameProfile().getName() + ": " + seconds
                    + "s, waking bed " + session.wakingBed() + ", dream bed " + session.dreamBed()
                    + ", persistent inventory=" + session.anchorInventory()), false);
        }
        if (count == 0) source.sendSuccess(() -> Component.literal("No players are currently in the Reverie."), false);
        return count;
    }

    private static int listAnchors(CommandSourceStack source) {
        Set<BlockPos> anchors = ReverieAnchorsData.get(source.getServer()).all();
        if (anchors.isEmpty()) source.sendSuccess(() -> Component.literal("No Dream Anchors are registered."), false);
        anchors.stream().sorted(java.util.Comparator.comparingLong(BlockPos::asLong)).forEach(pos ->
                source.sendSuccess(() -> Component.literal("Dream Anchor: " + pos.toShortString()), false));
        return anchors.size();
    }

    private static int removeAnchor(CommandSourceStack source, BlockPos pos) {
        ReverieAnchorsData anchors = ReverieAnchorsData.get(source.getServer());
        if (!anchors.remove(pos)) { source.sendFailure(Component.literal("No Dream Anchor exists at " + pos.toShortString())); return 0; }
        ReverieBedLinksData.get(source.getServer()).setAnchored(pos, false);
        ServerLevel level = source.getServer().getLevel(Reverie.REVERIE_LEVEL);
        if (level != null && level.getBlockState(pos).is(Reverie.DREAMWEAVERS_BED.get())) {
            ReverieEvents.setAnchorVisual(level, pos, false);
        }
        source.sendSuccess(() -> Component.literal("Removed Dream Anchor at " + pos.toShortString()), true);
        return 1;
    }

    private static int setBedOwner(CommandSourceStack source, ServerPlayer owner) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer admin = source.getPlayerOrException();
        HitResult hit = admin.pick(6.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)) { source.sendFailure(Component.literal("Look directly at a Dreamweaver's Bed.")); return 0; }
        BlockPos pos = blockHit.getBlockPos();
        net.minecraft.world.level.block.state.BlockState state = admin.level().getBlockState(pos);
        if (!state.is(Reverie.DREAMWEAVERS_BED.get()) || !admin.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            source.sendFailure(Component.literal("Look at a Dreamweaver's Bed in the Overworld.")); return 0;
        }
        BlockPos foot = state.getValue(net.minecraft.world.level.block.BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD
                ? pos.relative(state.getValue(net.minecraft.world.level.block.BedBlock.FACING).getOpposite()) : pos;
        ReverieBedOwnersData.get(source.getServer()).set(foot, owner.getUUID());
        source.sendSuccess(() -> Component.literal("Transferred bed ownership to " + owner.getGameProfile().getName() + "."), true);
        return 1;
    }

    private static int clearDreamInventory(CommandSourceStack source, ServerPlayer player, BlockPos anchor) {
        boolean cleared = ReverieDreamInventoryData.get(source.getServer()).clear(player.getUUID(), anchor);
        if (!cleared) { source.sendFailure(Component.literal("No saved Dream Inventory was found for that player and anchor.")); return 0; }
        source.sendSuccess(() -> Component.literal("Cleared " + player.getGameProfile().getName() + "'s Dream Inventory at " + anchor.toShortString()), true);
        return 1;
    }

    private static int showConfig(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Bed capacity=" + ReverieConfig.MAX_DREAMERS_PER_BED.get()
                + ", guest cost=" + ReverieConfig.SHARED_BED_COST_ITEM.get()
                + ", consume cost=" + ReverieConfig.CONSUME_SHARED_BED_COST.get()), false);
        source.sendSuccess(() -> Component.literal("Anchor inventories=" + ReverieConfig.ANCHOR_DREAM_INVENTORIES.get()
                + ", warning=" + ReverieConfig.OVERSTAY_WARNING_MINUTES.get() + "m, maximum="
                + ReverieConfig.MAX_DREAM_MINUTES.get() + "m, effect=" + ReverieConfig.OVERSTAY_EFFECT.get()
                + " for " + ReverieConfig.OVERSTAY_EFFECT_SECONDS.get() + "s"), false);
        source.sendSuccess(() -> Component.literal("Player clock control=" + ReverieConfig.PLAYER_CLOCK_TIME_CONTROL.get()
                + ", step=" + ReverieConfig.CLOCK_TIME_STEP.get() + " ticks, cooldown="
                + ReverieConfig.CLOCK_COOLDOWN_TICKS.get() + " ticks"), false);
        source.sendSuccess(() -> Component.literal("Figment Cage maximum chunk radius=" + ReverieConfig.FIGMENT_CAGE_CHUNK_RADIUS.get()
                + " (maximum region " + (ReverieConfig.FIGMENT_CAGE_CHUNK_RADIUS.get() * 2 + 1) + "x"
                + (ReverieConfig.FIGMENT_CAGE_CHUNK_RADIUS.get() * 2 + 1) + "), maximum mobs="
                + ReverieConfig.FIGMENT_CAGE_MAX_MOBS.get()), false);
        source.sendSuccess(() -> Component.literal("Automatic purge=" + ReverieConfig.AUTOMATIC_BLOCK_PURGE.get()
                + ", batch interval=" + ReverieConfig.PURGE_INTERVAL_TICKS.get() + " ticks, blocks per batch="
                + ReverieConfig.PURGE_BLOCKS_PER_BATCH.get() + ", recurring interval="
                + ReverieConfig.AUTOMATIC_PURGE_MINUTES.get() + "m"), false);
        return 1;
    }

    private static int startPurge(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(Reverie.REVERIE_LEVEL);
        if (level == null) { source.sendFailure(Component.literal("The Reverie is not currently loaded.")); return 0; }
        ReveriePurgeManager.start(level);
        return purgeStatus(source);
    }

    private static int forcePurge(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(Reverie.REVERIE_LEVEL);
        if (level == null) { source.sendFailure(Component.literal("The Reverie is not currently loaded.")); return 0; }
        ReveriePurgeManager.force(level);
        source.sendSuccess(() -> Component.literal("Forced purge completed across all currently loaded Reverie chunks."), true);
        return purgeStatus(source);
    }

    private static int stopPurge(CommandSourceStack source) {
        ReveriePurgeManager.stop();
        source.sendSuccess(() -> Component.literal("Stopped the incremental Reverie purge."), true);
        return 1;
    }

    private static int purgeStatus(CommandSourceStack source) {
        ReveriePurgeManager.Status status = ReveriePurgeManager.status();
        source.sendSuccess(() -> Component.literal("Reverie purge: " + (status.active() ? "running" : "idle")
                + ", loaded chunks=" + status.loadedChunks() + ", remaining=" + status.remainingChunks()
                + ", inspected=" + status.inspected() + ", removed=" + status.removed()), false);
        return 1;
    }

    private static int doctor(CommandSourceStack source) {
        java.util.List<String> problems = ReverieDiagnostics.problems(source.getServer());
        long sessions = source.getServer().getPlayerList().getPlayers().stream()
                .filter(player -> player.getData(ReverieSession.TYPE).active()).count();
        ReverieBlocklistData blocklist = ReverieBlocklistData.get(source.getServer());
        ReverieRecoveryData recovery = ReverieRecoveryData.get(source.getServer());
        ReveriePurgeManager.Status purge = ReveriePurgeManager.status();
        source.sendSuccess(() -> Component.literal("Reverie Doctor — " + (problems.isEmpty() ? "no problems found" : problems.size() + " problem(s)")), false);
        source.sendSuccess(() -> Component.literal("Dimension=" + (source.getServer().getLevel(Reverie.REVERIE_LEVEL) == null ? "MISSING" : "loaded")
                + ", active sessions=" + sessions + ", anchors=" + ReverieAnchorsData.get(source.getServer()).all().size()
                + ", saved dream inventories=" + ReverieDreamInventoryData.get(source.getServer()).size()), false);
        source.sendSuccess(() -> Component.literal("Recovery snapshots=" + recovery.size() + " (active=" + recovery.activeCount()
                + "), blacklist additions=" + (blocklist.blockedBlocks().size() + blocklist.blockedItems().size())
                + ", purge=" + (purge.active() ? "running" : "idle") + " across " + purge.loadedChunks() + " loaded chunk(s)"), false);
        source.sendSuccess(() -> Component.literal("Compatibility: " + ReverieDiagnostics.compatibility()), false);
        for (String problem : problems) source.sendFailure(Component.literal("Problem: " + problem));
        return problems.isEmpty() ? 1 : 0;
    }

    private static int editMobList(CommandSourceStack source, ResourceLocation id, boolean add) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) { source.sendFailure(Component.literal("Unknown entity type: " + id)); return 0; }
        ReverieMobAllowlistData data = ReverieMobAllowlistData.get(source.getServer());
        boolean changed = add ? data.add(id) : data.remove(id);
        source.sendSuccess(() -> Component.literal((add ? "Allowed " : "Removed ") + id
                + (changed ? "" : " (already set)")), true);
        return changed ? 1 : 0;
    }

    private static int listMobs(CommandSourceStack source) {
        Set<ResourceLocation> mobs = ReverieMobAllowlistData.get(source.getServer()).all();
        String text = mobs.stream().sorted().map(ResourceLocation::toString).reduce((a, b) -> a + ", " + b).orElse("(none)");
        source.sendSuccess(() -> Component.literal("Denied Reverie mobs (all others allowed): " + text), false);
        return mobs.size();
    }

    private static int audit(CommandSourceStack source, ServerPlayer player) {
        ReverieSession session = player.getData(ReverieSession.TYPE);
        java.util.UUID owner = session.wakingBed() == null ? null : ReverieBedOwnersData.get(source.getServer()).owner(session.wakingBed());
        ReverieRecoveryData.Entry recovery = ReverieRecoveryData.get(source.getServer()).entry(player.getUUID());
        source.sendSuccess(() -> Component.literal("Audit " + player.getGameProfile().getName() + ": session=" + session.active()
                + ", dimension=" + player.level().dimension().location() + ", elapsed=" + session.dreamElapsedTicks() / 20L
                + "s, wakingBed=" + session.wakingBed() + ", dreamBed=" + session.dreamBed()
                + ", bedOwner=" + owner + ", persistentInventory=" + session.anchorInventory()
                + ", recovery=" + (recovery == null ? "none" : recovery.active() ? "active" : "backup")), false);
        return 1;
    }

    private static int cleanup(CommandSourceStack source) {
        int cleaned = 0;
        ServerLevel overworld = source.getServer().overworld();
        ServerLevel reverie = source.getServer().getLevel(Reverie.REVERIE_LEVEL);
        ReverieBedLinksData links = ReverieBedLinksData.get(source.getServer());
        for (BlockPos bed : links.wakingBeds()) {
            if (overworld.getBlockState(bed).is(Reverie.DREAMWEAVERS_BED.get())) continue;
            BlockPos temporary = links.removeLink(bed);
            if (temporary != null && reverie != null) ReverieEvents.removeDreamBed(reverie, temporary);
            ReverieBedOwnersData.get(source.getServer()).remove(bed);
            cleaned++;
        }
        if (reverie != null) for (BlockPos anchor : ReverieAnchorsData.get(source.getServer()).all()) {
            if (reverie.getBlockState(anchor).is(Reverie.DREAMWEAVERS_BED.get())) continue;
            ReverieAnchorsData.get(source.getServer()).remove(anchor);
            links.setAnchored(anchor, false);
            cleaned++;
        }
        int result = cleaned;
        source.sendSuccess(() -> Component.literal("Reverie cleanup removed " + result + " stale record(s)."), true);
        return cleaned;
    }

    private static int setTime(CommandSourceStack source, long time, String label) {
        ServerLevel level = source.getServer().getLevel(Reverie.REVERIE_LEVEL);
        if (level == null) {
            source.sendFailure(Component.literal("The Reverie is not currently loaded."));
            return 0;
        }
        ReverieTimeData.get(source.getServer()).set(time);
        level.setDayTime(Math.floorMod(time, 24000L));
        source.sendSuccess(() -> Component.literal("Reverie lighting set to " + label + " (" + Math.floorMod(time, 24000L) + ")."), true);
        return 1;
    }

    private static int queryTime(CommandSourceStack source) {
        long time = ReverieTimeData.get(source.getServer()).time();
        source.sendSuccess(() -> Component.literal("The Reverie clock is frozen at " + time + "."), false);
        return (int) time;
    }
}
