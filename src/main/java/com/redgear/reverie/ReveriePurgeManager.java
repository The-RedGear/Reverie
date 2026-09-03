package com.redgear.reverie;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Incremental, loaded-chunks-only failsafe for content that bypassed placement interception. */
public final class ReveriePurgeManager {
    private static final Set<Long> LOADED = new HashSet<>();
    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();
    private static long current = Long.MIN_VALUE;
    private static int x, y, z;
    private static boolean active;
    private static long inspected, removed;
    private static long nextBatchTick, nextAutomaticTick;

    private ReveriePurgeManager() {}

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(Reverie.REVERIE_LEVEL))
            LOADED.add(event.getChunk().getPos().toLong());
    }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(Reverie.REVERIE_LEVEL))
            LOADED.remove(event.getChunk().getPos().toLong());
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(Reverie.REVERIE_LEVEL);
        if (level == null) return;
        long now = level.getGameTime();
        if (ReverieConfig.AUTOMATIC_BLOCK_PURGE.get() && !active && now >= nextAutomaticTick) {
            start(level);
            nextAutomaticTick = now + ReverieConfig.AUTOMATIC_PURGE_MINUTES.get() * 1200L;
        }
        if (active && now >= nextBatchTick) {
            process(level, ReverieConfig.PURGE_BLOCKS_PER_BATCH.get());
            nextBatchTick = now + ReverieConfig.PURGE_INTERVAL_TICKS.get();
        }
    }

    @SubscribeEvent
    public static void purgeDroppedItems(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !(item.level() instanceof ServerLevel level)
                || !level.dimension().equals(Reverie.REVERIE_LEVEL)) return;
        ItemStack stack = item.getItem();
        if (isBlockedItem(level, stack)) item.discard();
    }

    public static void start(ServerLevel level) {
        PENDING.clear();
        PENDING.addAll(LOADED);
        current = Long.MIN_VALUE;
        active = !PENDING.isEmpty();
        inspected = removed = 0;
        nextBatchTick = level.getGameTime();
    }

    public static void stop() {
        active = false;
        PENDING.clear();
        current = Long.MIN_VALUE;
    }

    public static void force(ServerLevel level) {
        start(level);
        while (active) process(level, 1_000_000);
    }

    public static Status status() { return new Status(active, LOADED.size(), PENDING.size() + (current == Long.MIN_VALUE ? 0 : 1), inspected, removed); }

    private static void process(ServerLevel level, int budget) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (budget-- > 0) {
            if (current == Long.MIN_VALUE) {
                Long next = PENDING.poll();
                if (next == null) { active = false; return; }
                current = next;
                x = z = 0;
                y = level.getMinBuildHeight();
            }
            ChunkPos chunkPos = new ChunkPos(current);
            if (!LOADED.contains(current) || !level.hasChunk(chunkPos.x, chunkPos.z)) {
                current = Long.MIN_VALUE;
                continue;
            }
            pos.set(chunkPos.getMinBlockX() + x, y, chunkPos.getMinBlockZ() + z);
            inspected++;
            if (ReverieEvents.isBlocked(level, level.getBlockState(pos))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                removed++;
            } else if (level.getBlockEntity(pos) instanceof Container container) {
                purgeContainer(level, container);
            }
            purgeCapability(level, pos);
            if (++x == 16) { x = 0; if (++z == 16) { z = 0; if (++y >= level.getMaxBuildHeight()) current = Long.MIN_VALUE; } }
        }
    }

    private static void purgeContainer(ServerLevel level, Container container) {
        boolean changed = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (isBlockedItem(level, container.getItem(slot))) {
                container.setItem(slot, ItemStack.EMPTY);
                removed++;
                changed = true;
            }
        }
        if (changed) container.setChanged();
    }

    /** Covers modded machines and storage that expose NeoForge item storage without implementing Container. */
    private static void purgeCapability(ServerLevel level, BlockPos pos) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler == null) return;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!isBlockedItem(level, stack)) continue;
            int remaining = stack.getCount();
            while (remaining > 0) {
                ItemStack extracted = handler.extractItem(slot, remaining, false);
                if (extracted.isEmpty()) break;
                remaining -= extracted.getCount();
                removed += extracted.getCount();
            }
        }
    }

    public static boolean isBlockedItem(ServerLevel level, ItemStack stack) {
        return !stack.isEmpty() && ReverieBlocklistData.get(level.getServer()).isItemBlocked(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.is(Reverie.UNUSABLE_ITEMS));
    }

    public record Status(boolean active, int loadedChunks, int remainingChunks, long inspected, long removed) {}
}
