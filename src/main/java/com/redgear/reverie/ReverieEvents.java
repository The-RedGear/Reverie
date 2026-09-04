package com.redgear.reverie;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

public final class ReverieEvents {
    private static final ResourceLocation ANCIENT_CITY_LOOT = ResourceLocation.withDefaultNamespace("chests/ancient_city");
    private static final java.util.Map<java.util.UUID, Long> ENTRY_CONFIRMATIONS = new java.util.HashMap<>();
    private static final java.util.Map<String, Long> FEEDBACK_COOLDOWNS = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, PendingDream> PENDING_DREAMS = new java.util.HashMap<>();
    private static long suppressSleepMessagesUntilTick;
    private static final long DREAM_TRANSITION_TICKS = 40L;
    private static long nextClockChangeTick;
    private ReverieEvents() {}

    public static boolean shouldSuppressSleepStatus(net.minecraft.server.MinecraftServer server) {
        return server.getTickCount() <= suppressSleepMessagesUntilTick;
    }

    @SubscribeEvent
    public static void addRareAncientCityBed(LootTableLoadEvent event) {
        if (!event.getName().equals(ANCIENT_CITY_LOOT)) return;
        event.getTable().addPool(LootPool.lootPool()
                .name("reverie_rare_dreamweavers_bed")
                .when(LootItemRandomChanceCondition.randomChance(0.005F))
                .add(LootItem.lootTableItem(Reverie.DREAMWEAVERS_BED_ITEM.get()))
                .build());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useBed(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().dimension().equals(Reverie.REVERIE_LEVEL)
                && (isBlocked(player, event.getLevel().getBlockState(event.getPos()))
                || isBlocked(player, event.getItemStack()))) {
            event.setCanceled(true);
            Component rejected = !event.getItemStack().isEmpty() && isBlocked(player, event.getItemStack())
                    ? event.getItemStack().getHoverName() : event.getLevel().getBlockState(event.getPos()).getBlock().getName();
            feedback(player, "rejected", "message.reverie.rejected_named", rejected);
            return;
        }
        if (player.level().dimension().equals(Reverie.REVERIE_LEVEL)
                && event.getLevel().getBlockState(event.getPos()).is(Reverie.FIGMENT_CAGE.get())) {
            if (event.getHand() != InteractionHand.MAIN_HAND) return;
            Item chargeItem = configuredItem(ReverieConfig.FIGMENT_CAGE_CHARGE_ITEM.get());
            boolean charging = chargeItem != null && event.getItemStack().is(chargeItem);
            boolean discharging = player.isShiftKeyDown() && event.getItemStack().isEmpty();
            if (!charging && !discharging) return;
            event.setCanceled(true);
            player.swing(InteractionHand.MAIN_HAND, true);
            int before = FigmentCagesData.get(player.server).radius(event.getPos());
            int change = charging ? 1 : -1;
            int radius = FigmentCagesData.get(player.server).resize(event.getPos(), change,
                    ReverieConfig.FIGMENT_CAGE_CHUNK_RADIUS.get());
            int width = radius * 2 + 1;
            if (radius == before) {
                player.displayClientMessage(Component.translatable(charging
                        ? "message.reverie.cage_max" : "message.reverie.cage_min", width, width), true);
            } else {
                if (charging && !player.isCreative()) event.getItemStack().shrink(1);
                player.displayClientMessage(Component.translatable("message.reverie.cage_size", width, width), true);
                player.level().playSound(null, event.getPos(), charging ? SoundEvents.RESPAWN_ANCHOR_CHARGE
                                : SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                        SoundSource.BLOCKS, 0.8F, 1.05F + radius * 0.08F);
                emitGust((ServerLevel) player.level(), event.getPos());
            }
            BlockState cageState = event.getLevel().getBlockState(event.getPos());
            if (cageState.is(Reverie.FIGMENT_CAGE.get())) event.getLevel().setBlock(event.getPos(),
                    cageState.setValue(FigmentCageBlock.CHARGES, Math.min(4, radius)), 3);
            if (charging && radius > before) awardReverieAdvancement(player, "charge_figment_cage");
            return;
        }
        if (!event.getLevel().getBlockState(event.getPos()).is(Reverie.DREAMWEAVERS_BED.get())) return;
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        player.swing(InteractionHand.MAIN_HAND, true);
        if (player.level().dimension().equals(Reverie.REVERIE_LEVEL)) {
            BlockPos dreamBed = bedFoot(event.getPos(), event.getLevel().getBlockState(event.getPos()));
            if (event.getItemStack().is(Items.RESPAWN_ANCHOR)) {
                updateAnchor(player, dreamBed);
            } else {
                awaken(player, event.getPos());
            }
        }
        else if (player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            BlockPos wakingBed = bedFoot(event.getPos(), event.getLevel().getBlockState(event.getPos()));
            if (player.isShiftKeyDown()) {
                showBedAccess(player, wakingBed);
                return;
            }
            if (player.isCreative()) {
                beginDreamTransition(player, wakingBed, event.getHand());
                return;
            }
            long now = player.level().getGameTime();
            Long expires = ENTRY_CONFIRMATIONS.remove(player.getUUID());
            if (expires == null || expires < now) {
                ENTRY_CONFIRMATIONS.put(player.getUUID(), now + 100L);
                player.displayClientMessage(Component.translatable("message.reverie.confirm_dream"), true);
                return;
            }
            beginDreamTransition(player, wakingBed, event.getHand());
        }
        else player.displayClientMessage(Component.translatable("message.reverie.overworld_only"), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useRestrictedItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.level().dimension().equals(Reverie.REVERIE_LEVEL)) return;
        if (isBlocked(player, event.getItemStack())) {
            event.setCanceled(true);
            feedback(player, "rejected", "message.reverie.rejected_named", event.getItemStack().getHoverName());
            return;
        }
        if (!event.getItemStack().is(Items.CLOCK) || !ReverieConfig.PLAYER_CLOCK_TIME_CONTROL.get()) return;
        event.setCanceled(true);
        if (player.getCooldowns().isOnCooldown(Items.CLOCK)) return;
        long now = player.server.getTickCount();
        if (now < nextClockChangeTick) {
            player.getCooldowns().addCooldown(Items.CLOCK, (int) (nextClockChangeTick - now));
            player.displayClientMessage(Component.translatable("message.reverie.time_settling"), true);
            return;
        }
        player.swing(event.getHand(), true);
        ReverieTimeData time = ReverieTimeData.get(player.server);
        if (player.isShiftKeyDown()) {
            time.set(ReverieTimeData.DEFAULT_TIME);
            ((ServerLevel) player.level()).setDayTime(ReverieTimeData.DEFAULT_TIME);
        } else {
            long next = Math.floorMod(time.time() + ReverieConfig.CLOCK_TIME_STEP.get(), 24000L);
            time.set(next);
            ((ServerLevel) player.level()).setDayTime(next);
        }
        nextClockChangeTick = now + ReverieConfig.GLOBAL_CLOCK_COOLDOWN_TICKS.get();
        player.getCooldowns().addCooldown(Items.CLOCK, ReverieConfig.CLOCK_COOLDOWN_TICKS.get());
        for (ServerPlayer dreamer : ((ServerLevel) player.level()).players()) {
            dreamer.displayClientMessage(Component.translatable(player.isShiftKeyDown()
                    ? "message.reverie.time_reset_by" : "message.reverie.time_advanced_by",
                    player.getDisplayName(), time.time()), true);
        }
    }

    @SubscribeEvent
    public static void allowDreamTransitionSleep(CanContinueSleepingEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PENDING_DREAMS.containsKey(player.getUUID())) {
            event.setContinueSleeping(true);
        }
    }

    private static void beginDreamTransition(ServerPlayer player, BlockPos wakingBed, InteractionHand hand) {
        if (PENDING_DREAMS.containsKey(player.getUUID())) return;
        BlockState bedState = player.level().getBlockState(wakingBed);
        if (!bedState.is(Reverie.DREAMWEAVERS_BED.get())) return;
        java.util.UUID bedOwner = ReverieBedOwnersData.get(player.server).claimIfUnowned(wakingBed, player.getUUID());
        boolean isBedOwner = player.getUUID().equals(bedOwner);
        int occupants = ReverieBedLinksData.get(player.server).occupantCount(wakingBed);
        if (!canEnterBed(player, wakingBed, isBedOwner)) {
            player.displayClientMessage(Component.translatable("message.reverie.bed_reserved"), true);
            return;
        }
        Item sharedCost = configuredItem(ReverieConfig.SHARED_BED_COST_ITEM.get());
        boolean guest = !isBedOwner && occupants > 0 && sharedCost != null && !player.isCreative();
        if (guest && !player.getItemInHand(hand).is(sharedCost)) {
            player.displayClientMessage(Component.translatable("message.reverie.guest_cost", sharedCost.getDescription()), true);
            return;
        }
        AABB dangerArea = new AABB(wakingBed).inflate(8.0D, 5.0D, 8.0D);
        if (!player.isCreative() && !player.level().getEntitiesOfClass(Monster.class, dangerArea,
                monster -> monster.isPreventingPlayerRest(player)).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.reverie.not_safe"), true);
            return;
        }
        PendingDream pending = new PendingDream(wakingBed.immutable(),
                player.level().getGameTime() + DREAM_TRANSITION_TICKS, guest, hand, sharedCost,
                isBedOwner);
        if (player.isCreative()) {
            dream(player, pending);
        } else {
            BlockPos sleepingPos = wakingBed.relative(bedState.getValue(BedBlock.FACING));
            suppressSleepMessagesUntilTick = Math.max(suppressSleepMessagesUntilTick, player.server.getTickCount() + 60L);
            player.startSleeping(sleepingPos);
            PENDING_DREAMS.put(player.getUUID(), pending);
        }
    }

    private static void dream(ServerPlayer player, PendingDream pending) {
        BlockPos wakingBed = pending.wakingBed();
        ReverieSession session = player.getData(ReverieSession.TYPE);
        if (session.active()) return;
        ServerLevel destination = player.server.getLevel(Reverie.REVERIE_LEVEL);
        if (destination == null) {
            player.displayClientMessage(Component.translatable("message.reverie.missing_dimension"), false);
            return;
        }
        if (!canEnterBed(player, wakingBed, pending.bedOwner())) {
            player.displayClientMessage(Component.translatable("message.reverie.bed_reserved"), true);
            return;
        }
        if (pending.guest()) {
            ItemStack payment = player.getItemInHand(pending.hand());
            if (pending.costItem() == null || !payment.is(pending.costItem())) {
                player.displayClientMessage(Component.translatable("message.reverie.guest_cost_missing"), true);
                return;
            }
            if (ReverieConfig.CONSUME_SHARED_BED_COST.get() && !player.isCreative()) payment.shrink(1);
        }
        CompoundTag wakingPlayer = new CompoundTag();
        player.saveWithoutId(wakingPlayer);
        ReverieRecoveryData recovery = ReverieRecoveryData.get(player.server);
        recovery.capture(player.getUUID(), player.getGameProfile().getName(), wakingPlayer, player.level().getGameTime());
        recovery.flush(player.server);
        player.getInventory().clearContent();
        ModdedInventoryBridge.clearAll(player);
        clearDreamTransientState(player);
        ReverieBedLinksData links = ReverieBedLinksData.get(player.server);
        BlockPos arrivalBed = links.dreamBed(wakingBed);
        boolean anchored = arrivalBed != null && ReverieAnchorsData.get(player.server).contains(arrivalBed);
        if (arrivalBed == null || !destination.getBlockState(arrivalBed).is(Reverie.DREAMWEAVERS_BED.get())) {
            arrivalBed = ReverieAnchorsData.get(player.server).findFor(wakingBed);
            anchored = arrivalBed != null && destination.getBlockState(arrivalBed).is(Reverie.DREAMWEAVERS_BED.get());
            if (!anchored) {
                if (arrivalBed != null) ReverieAnchorsData.get(player.server).remove(arrivalBed);
                arrivalBed = placeArrivalBed(destination, BlockPos.containing(player.getX(), 32.0D, player.getZ()), player.getDirection());
            }
        }
        if (anchored) setAnchorVisual(destination, arrivalBed, true);
        links.join(wakingBed, arrivalBed, player.getUUID(), anchored);
        setDreamingVisual(player.server.overworld(), wakingBed, true);
        boolean useAnchorInventory = anchored && ReverieConfig.ANCHOR_DREAM_INVENTORIES.get()
                && pending.bedOwner();
        session.begin(wakingPlayer, wakingBed, arrivalBed, useAnchorInventory, player.server.overworld().getGameTime());
        if (useAnchorInventory) ReverieDreamInventoryData.get(player.server).loadInto(player, arrivalBed);
        emitGust((ServerLevel) player.level(), wakingBed);
        player.teleportTo(destination, arrivalBed.getX() + 0.5D, 33.0D, arrivalBed.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        int grassColor = player.server.overworld().getBiome(wakingBed).value()
                .getGrassColor(wakingBed.getX(), wakingBed.getZ());
        PacketDistributor.sendToPlayer(player, new ReverieBiomeTintPayload(
                wakingBed.getX() >> 4, wakingBed.getZ() >> 4, 2, grassColor));
        emitGust(destination, arrivalBed);
        playTransitionSound(destination, arrivalBed, true);
        player.setGameMode(GameType.CREATIVE);
        player.onUpdateAbilities();
        awardReverieAdvancement(player, "enter_reverie");
        player.displayClientMessage(Component.translatable("message.reverie.enter"), true);
    }

    private static void awardReverieAdvancement(ServerPlayer player, String path) {
        net.minecraft.advancements.AdvancementHolder advancement = player.server.getAdvancements().get(
                ResourceLocation.fromNamespaceAndPath(Reverie.MOD_ID, path));
        if (advancement == null) return;
        for (String criterion : advancement.value().criteria().keySet()) player.getAdvancements().award(advancement, criterion);
    }

    static boolean awaken(ServerPlayer player, net.minecraft.core.BlockPos usedBed) {
        ReverieSession session = player.getData(ReverieSession.TYPE);
        if (!session.active()) return false;
        CompoundTag wakingPlayer = session.wakingPlayer();
        long dreamDuration = session.dreamElapsedTicks();
        if (session.anchorInventory() && session.dreamBed() != null
                && ReverieAnchorsData.get(player.server).contains(session.dreamBed())) {
            ReverieDreamInventoryData.get(player.server).capture(player, session.dreamBed());
        }
        ServerLevel reverie = player.server.getLevel(Reverie.REVERIE_LEVEL);
        if (reverie != null) emitGust(reverie, session.dreamBed());
        BlockPos bedToRemove = ReverieBedLinksData.get(player.server).leave(session.wakingBed(), player.getUUID());
        if (ReverieBedLinksData.get(player.server).occupantCount(session.wakingBed()) == 0) {
            setDreamingVisual(player.server.overworld(), session.wakingBed(), false);
        }
        if (bedToRemove != null && reverie != null) removeDreamBed(reverie, bedToRemove);
        player.getInventory().clearContent();
        ModdedInventoryBridge.clearAll(player);
        session.finish();
        restoreWakingPlayer(player, wakingPlayer);
        applyOverstayEffect(player, dreamDuration);
        emitGust((ServerLevel) player.level(), player.blockPosition());
        playTransitionSound((ServerLevel) player.level(), player.blockPosition(), false);
        player.displayClientMessage(Component.translatable("message.reverie.awaken"), true);
        ReverieRecoveryData recovery = ReverieRecoveryData.get(player.server);
        recovery.complete(player.getUUID());
        recovery.flush(player.server);
        return true;
    }

    @SubscribeEvent
    public static void recoverOnLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ReverieSession session = player.getData(ReverieSession.TYPE);
        if (session.active() && !player.level().dimension().equals(Reverie.REVERIE_LEVEL)) {
            CompoundTag wakingPlayer = session.wakingPlayer();
            BlockPos bedToRemove = ReverieBedLinksData.get(player.server).leave(session.wakingBed(), player.getUUID());
            if (ReverieBedLinksData.get(player.server).occupantCount(session.wakingBed()) == 0) {
                setDreamingVisual(player.server.overworld(), session.wakingBed(), false);
            }
            ServerLevel reverie = player.server.getLevel(Reverie.REVERIE_LEVEL);
            if (bedToRemove != null && reverie != null) removeDreamBed(reverie, bedToRemove);
            session.finish();
            restoreWakingPlayer(player, wakingPlayer);
            ReverieRecoveryData recovery = ReverieRecoveryData.get(player.server);
            recovery.complete(player.getUUID());
            recovery.flush(player.server);
            emitGust((ServerLevel) player.level(), player.blockPosition());
            playTransitionSound((ServerLevel) player.level(), player.blockPosition(), false);
        }
    }

    static void restoreWakingPlayer(ServerPlayer player, CompoundTag wakingPlayer) {
        GameType wakingGameMode = GameType.byId(wakingPlayer.getInt("playerGameType"));
        player.load(wakingPlayer);
        player.teleportTo(player.server.overworld(), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
        player.setGameMode(wakingGameMode);
        player.removeAllEffects();
        ModdedInventoryBridge.refreshAll(player);
        player.onUpdateAbilities();
    }

    static boolean restoreRecovery(ServerPlayer player, boolean rollback) {
        ReverieRecoveryData data = ReverieRecoveryData.get(player.server);
        CompoundTag saved = rollback ? data.rollback(player.getUUID()) : data.waking(player.getUUID());
        if (saved == null) return false;
        if (!rollback) {
            CompoundTag current = new CompoundTag();
            player.saveWithoutId(current);
            data.prepareRestore(player.getUUID(), current);
            data.flush(player.server);
        }
        player.getInventory().clearContent();
        ModdedInventoryBridge.clearAll(player);
        ReverieSession session = player.getData(ReverieSession.TYPE);
        if (session.active()) {
            BlockPos bedToRemove = ReverieBedLinksData.get(player.server).leave(session.wakingBed(), player.getUUID());
            if (ReverieBedLinksData.get(player.server).occupantCount(session.wakingBed()) == 0) {
                setDreamingVisual(player.server.overworld(), session.wakingBed(), false);
            }
            ServerLevel reverie = player.server.getLevel(Reverie.REVERIE_LEVEL);
            if (bedToRemove != null && reverie != null) removeDreamBed(reverie, bedToRemove);
            session.finish();
        }
        restoreWakingPlayer(player, saved);
        data.complete(player.getUUID());
        data.flush(player.server);
        return true;
    }

    @SubscribeEvent
    public static void blockExperienceGain(PlayerXpEvent.XpChange event) {
        if (event.getEntity().level().dimension().equals(Reverie.REVERIE_LEVEL)) event.setAmount(0);
    }

    @SubscribeEvent
    public static void blockExperienceLevels(PlayerXpEvent.LevelChange event) {
        if (event.getEntity().level().dimension().equals(Reverie.REVERIE_LEVEL)) event.setLevels(0);
    }

    @SubscribeEvent
    public static void enforceDreamVitals(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PendingDream pending = PENDING_DREAMS.get(player.getUUID());
            if (pending != null) {
                boolean valid = player.isAlive()
                        && player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                        && player.isSleeping()
                        && player.level().getBlockState(pending.wakingBed()).is(Reverie.DREAMWEAVERS_BED.get());
                if (!valid) {
                    PENDING_DREAMS.remove(player.getUUID());
                    if (player.isSleeping()) player.stopSleepInBed(true, false);
                } else if (player.level().getGameTime() >= pending.completeAt()) {
                    PENDING_DREAMS.remove(player.getUUID());
                    dream(player, pending);
                    return;
                }
            }
            showAnchorCoverage(player);
            showOccupiedBeds(player);
            showFigmentBoundary(player);
        }
        if (!event.getEntity().level().dimension().equals(Reverie.REVERIE_LEVEL)) return;
        if (event.getEntity().level() instanceof ServerLevel reverie) {
            long chosenTime = ReverieTimeData.get(reverie.getServer()).time();
            if (reverie.getDayTime() != chosenTime) reverie.setDayTime(chosenTime);
        }
        // Follow the dimension's configured floor, as Aether/Forgiving Void do,
        // instead of assuming a particular minimum Y for every world definition.
        if (event.getEntity() instanceof ServerPlayer player
                && player.getY() <= player.level().getMinBuildHeight()) {
            player.fallDistance = 0.0F;
            awaken(player, null);
            return;
        }
        event.getEntity().experienceLevel = 0;
        event.getEntity().totalExperience = 0;
        event.getEntity().experienceProgress = 0.0F;
        event.getEntity().getFoodData().setFoodLevel(20);
        event.getEntity().getFoodData().setSaturation(0.0F);
        event.getEntity().getFoodData().setExhaustion(0.0F);
        if (event.getEntity() instanceof ServerPlayer dreamer) {
            ReverieSession session = dreamer.getData(ReverieSession.TYPE);
            if (session.active()) {
                session.tickDream();
                enforceDreamTime(dreamer, session);
                if (dreamer.tickCount % 5 == 0) {
                    ModdedInventoryBridge.ejectBlocked(dreamer, stack -> isBlocked(dreamer, stack));
                    purgeBlockedVanillaInventory(dreamer);
                }
            }
        }
    }

    @SubscribeEvent
    public static void preventPlayerDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level().dimension().equals(Reverie.REVERIE_LEVEL)) {
            event.setCanceled(true);
        } else if (event.getEntity() instanceof Mob mob
                && mob.level().dimension().equals(Reverie.REVERIE_LEVEL)) {
            if (event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                ItemStack weapon = player.getMainHandItem();
                mob.getPersistentData().putBoolean("reverie.SuppressLoot",
                        !(weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.AXES)));
            }
            event.setAmount(Float.MAX_VALUE);
        }
    }

    @SubscribeEvent
    public static void enforceFigmentLootRule(LivingDropsEvent event) {
        if (event.getEntity().level().dimension().equals(Reverie.REVERIE_LEVEL)
                && event.getEntity().getPersistentData().getBoolean("reverie.SuppressLoot")) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void preventMobSpawns(EntityJoinLevelEvent event) {
        if (event.getLevel().dimension().equals(Reverie.REVERIE_LEVEL) && event.getEntity() instanceof Mob) {
            if (!(event.getLevel() instanceof ServerLevel level)) return;
            ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType());
            BlockPos cage = FigmentCagesData.get(level.getServer()).cageFor(event.getEntity().blockPosition());
            if (!ReverieMobAllowlistData.get(level.getServer()).contains(type) || cage == null
                    || cagePopulation(level, cage) >= ReverieConfig.FIGMENT_CAGE_MAX_MOBS.get()) {
                rejectFigment(level, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ());
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void containFigments(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)
                || !level.dimension().equals(Reverie.REVERIE_LEVEL) || mob.tickCount % 10 != 0) return;
        if (FigmentCagesData.get(level.getServer()).cageFor(mob.blockPosition()) != null) return;
        rejectFigment(level, mob.getX(), mob.getY() + 0.5D, mob.getZ());
        mob.discard();
    }

    @SubscribeEvent
    public static void preventPortalCreation(BlockEvent.PortalSpawnEvent event) {
        if (event.getLevel() instanceof net.minecraft.world.level.Level level
                && level.dimension().equals(Reverie.REVERIE_LEVEL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void preventRestrictedBlockPlacement(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(Reverie.REVERIE_LEVEL)
                && isBlocked(level, event.getPlacedBlock())) {
            event.setCanceled(true);
            rejectBlockPlacement(level, event.getPos());
            if (event.getEntity() instanceof ServerPlayer player) {
                feedback(player, "rejected", "message.reverie.rejected_named", event.getPlacedBlock().getBlock().getName());
            }
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().dimension().equals(Reverie.REVERIE_LEVEL)
                && event.getPlacedBlock().is(Reverie.FIGMENT_CAGE.get())) {
            FigmentCagesData.get(player.server).add(event.getPos());
        }
        if (player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                && event.getPlacedBlock().is(Reverie.DREAMWEAVERS_BED.get())) {
            ReverieBedOwnersData.get(player.server).set(
                    bedFoot(event.getPos(), event.getPlacedBlock()), player.getUUID());
        }
    }

    @SubscribeEvent
    public static void preventExplosionGriefing(ExplosionEvent.Detonate event) {
        if (event.getLevel().dimension().equals(Reverie.REVERIE_LEVEL)) {
            event.getAffectedBlocks().clear();
        }
    }

    @SubscribeEvent
    public static void mirrorDestroyedWakingBed(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                || !event.getState().is(Reverie.DREAMWEAVERS_BED.get())) return;
        BlockPos brokenBed = bedFoot(event.getPos(), event.getState());
        ReverieBedOwnersData.get(level.getServer()).remove(brokenBed);
        ServerLevel reverie = level.getServer().getLevel(Reverie.REVERIE_LEVEL);
        if (reverie == null) return;
        removeDreamBed(reverie, ReverieBedLinksData.get(level.getServer()).removeLink(brokenBed));
    }

    @SubscribeEvent
    public static void removeDestroyedAnchor(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(Reverie.REVERIE_LEVEL)
                || !event.getState().is(Reverie.DREAMWEAVERS_BED.get())) return;
        BlockPos bed = bedFoot(event.getPos(), event.getState());
        if (ReverieAnchorsData.get(level.getServer()).remove(bed)) {
            ReverieBedLinksData.get(level.getServer()).setAnchored(bed, false);
            level.playSound(null, bed, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @SubscribeEvent
    public static void removeDestroyedFigmentCage(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(Reverie.REVERIE_LEVEL)
                && event.getState().is(Reverie.FIGMENT_CAGE.get())) FigmentCagesData.get(level.getServer()).remove(event.getPos());
    }

    @SubscribeEvent
    public static void preventDimensionEscape(EntityTravelToDimensionEvent event) {
        if (!event.getEntity().level().dimension().equals(Reverie.REVERIE_LEVEL)) return;
        if (event.getEntity() instanceof ServerPlayer player
                && !player.getData(ReverieSession.TYPE).active()) return;
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.reverie.cannot_leave"), true);
        }
    }

    private static void clearDreamTransientState(ServerPlayer player) {
        player.removeAllEffects();
        player.experienceLevel = 0;
        player.totalExperience = 0;
        player.experienceProgress = 0.0F;
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(0.0F);
        player.getFoodData().setExhaustion(0.0F);
    }

    private static boolean isBlocked(ServerPlayer player, BlockState state) {
        return ReverieBlocklistData.get(player.server).isBlockBlocked(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()), state.is(Reverie.UNUSABLE_BLOCKS));
    }

    public static boolean isBlocked(ServerLevel level, BlockState state) {
        return ReverieBlocklistData.get(level.getServer()).isBlockBlocked(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()), state.is(Reverie.UNUSABLE_BLOCKS));
    }

    public static boolean rejectAutomatedPlacement(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || !level.dimension().equals(Reverie.REVERIE_LEVEL)
                || !isBlocked(serverLevel, state)) return false;
        rejectBlockPlacement(serverLevel, pos);
        return true;
    }

    private static void rejectBlockPlacement(ServerLevel level, BlockPos pos) {
        emitGust(level, pos);
        net.minecraft.sounds.SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(
                ResourceLocation.fromNamespaceAndPath("minecraft", "entity.breeze.wind_burst"));
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.65F, 1.25F);
    }

    private static boolean isBlocked(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        return ReverieBlocklistData.get(player.server).isItemBlocked(
                BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.is(Reverie.UNUSABLE_ITEMS));
    }

    private static BlockPos bedFoot(BlockPos pos, BlockState state) {
        return state.getValue(BedBlock.PART) == BedPart.HEAD
                ? pos.relative(state.getValue(BedBlock.FACING).getOpposite()) : pos;
    }

    static void removeDreamBed(ServerLevel level, BlockPos foot) {
        if (foot == null) return;
        BlockState state = level.getBlockState(foot);
        if (!state.is(Reverie.DREAMWEAVERS_BED.get())) return;
        BlockPos otherHalf = state.getValue(BedBlock.PART) == BedPart.FOOT
                ? foot.relative(state.getValue(BedBlock.FACING))
                : foot.relative(state.getValue(BedBlock.FACING).getOpposite());
        level.setBlock(otherHalf, Blocks.AIR.defaultBlockState(), 35);
        level.setBlock(foot, Blocks.AIR.defaultBlockState(), 35);
    }

    private static void updateAnchor(ServerPlayer player, BlockPos bed) {
        ReverieAnchorsData anchors = ReverieAnchorsData.get(player.server);
        if (player.isShiftKeyDown()) {
            if (anchors.remove(bed)) {
                ReverieBedLinksData.get(player.server).setAnchored(bed, false);
                setAnchorVisual((ServerLevel) player.level(), bed, false);
                ((ServerLevel) player.level()).playSound(null, bed, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
                player.displayClientMessage(Component.translatable("message.reverie.anchor_removed"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.reverie.not_anchor"), true);
            }
        } else if (anchors.add(bed)) {
            ReverieBedLinksData.get(player.server).setAnchored(bed, true);
            setAnchorVisual((ServerLevel) player.level(), bed, true);
            emitGust((ServerLevel) player.level(), bed);
            ((ServerLevel) player.level()).playSound(null, bed, SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.reverie.anchor_created"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.reverie.already_anchor"), true);
        }
    }

    static void setAnchorVisual(ServerLevel level, BlockPos foot, boolean anchored) {
        setBedVisual(level, foot, DreamweaversBedBlock.ANCHORED, anchored);
    }

    private static void setDreamingVisual(ServerLevel level, BlockPos foot, boolean dreaming) {
        setBedVisual(level, foot, DreamweaversBedBlock.DREAMING, dreaming);
    }

    private static void setBedVisual(ServerLevel level, BlockPos foot,
                                     net.minecraft.world.level.block.state.properties.BooleanProperty property,
                                     boolean value) {
        if (foot == null) return;
        BlockState footState = level.getBlockState(foot);
        if (!footState.is(Reverie.DREAMWEAVERS_BED.get())) return;
        BlockPos head = foot.relative(footState.getValue(BedBlock.FACING));
        level.setBlock(foot, footState.setValue(property, value), 3);
        BlockState headState = level.getBlockState(head);
        if (headState.is(Reverie.DREAMWEAVERS_BED.get())) {
            level.setBlock(head, headState.setValue(property, value), 3);
        }
    }

    private static void emitGust(ServerLevel level, BlockPos pos) {
        if (pos == null) return;
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL,
                pos.getX() + 0.5D, pos.getY() + 0.75D, pos.getZ() + 0.5D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void playTransitionSound(ServerLevel level, BlockPos pos, boolean entering) {
        level.playSound(null, pos,
                entering ? SoundEvents.BREEZE_IDLE_GROUND : SoundEvents.BREEZE_SLIDE,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static void showAnchorCoverage(ServerPlayer player) {
        if (!player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                || player.level().getGameTime() % 10L != 0L
                || !(player.getMainHandItem().is(Reverie.DREAMWEAVERS_BED_ITEM.get())
                || player.getOffhandItem().is(Reverie.DREAMWEAVERS_BED_ITEM.get()))
                || ReverieAnchorsData.get(player.server).findFor(player.blockPosition()) == null) return;
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(player, ParticleTypes.END_ROD, false,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                3, 1.25D, 0.65D, 1.25D, 0.015D);
        if (level.getGameTime() % 120L == 0L && player.getRandom().nextInt(3) == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WHIRL,
                    SoundSource.AMBIENT, 0.3F, 1.15F);
        }
    }

    private static void showOccupiedBeds(ServerPlayer player) {
        if (!player.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                || player.level().getGameTime() % 10L != 0L) return;
        ServerLevel level = (ServerLevel) player.level();
        for (BlockPos bed : ReverieBedLinksData.get(player.server).occupiedWakingBeds()) {
            if (bed.distSqr(player.blockPosition()) > 32.0D * 32.0D
                    || !level.hasChunkAt(bed)
                    || !level.getBlockState(bed).is(Reverie.DREAMWEAVERS_BED.get())) continue;
            level.sendParticles(player, ParticleTypes.END_ROD, false,
                    bed.getX() + 0.5D, bed.getY() + 0.8D, bed.getZ() + 0.5D,
                    2, 0.65D, 0.35D, 0.65D, 0.005D);
        }
    }

    private static int cagePopulation(ServerLevel level, BlockPos cage) {
        int radius = FigmentCagesData.get(level.getServer()).radius(cage);
        net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(cage);
        int minX = (chunk.x - radius) << 4, minZ = (chunk.z - radius) << 4;
        int maxX = (chunk.x + radius + 1) << 4, maxZ = (chunk.z + radius + 1) << 4;
        return level.getEntitiesOfClass(Mob.class,
                new AABB(minX, level.getMinBuildHeight(), minZ, maxX, level.getMaxBuildHeight(), maxZ)).size();
    }

    private static void showFigmentBoundary(ServerPlayer player) {
        if (!player.level().dimension().equals(Reverie.REVERIE_LEVEL) || player.tickCount % 10 != 0
                || !(showsCageRange(player.getMainHandItem()) || showsCageRange(player.getOffhandItem()))) return;
        BlockPos cage = FigmentCagesData.get(player.server).cageFor(player.blockPosition());
        if (cage == null) cage = FigmentCagesData.get(player.server).nearest(player.blockPosition());
        if (cage == null) return;
        int radius = FigmentCagesData.get(player.server).radius(cage);
        net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(cage);
        int minX = (chunk.x - radius) << 4, minZ = (chunk.z - radius) << 4;
        int maxX = (chunk.x + radius + 1) << 4, maxZ = (chunk.z + radius + 1) << 4;
        ServerLevel level = (ServerLevel) player.level();
        double y = 33.15D;
        for (int offset = 0; offset <= maxX - minX; offset += 4) {
            level.sendParticles(player, ParticleTypes.END_ROD, false, minX + offset, y, minZ, 1, 0, 0, 0, 0);
            level.sendParticles(player, ParticleTypes.END_ROD, false, minX + offset, y, maxZ, 1, 0, 0, 0, 0);
            level.sendParticles(player, ParticleTypes.END_ROD, false, minX, y, minZ + offset, 1, 0, 0, 0, 0);
            level.sendParticles(player, ParticleTypes.END_ROD, false, maxX, y, minZ + offset, 1, 0, 0, 0, 0);
        }
    }

    private static boolean showsCageRange(ItemStack stack) {
        return stack.is(Reverie.FIGMENT_CAGE_ITEM.get()) || stack.getItem() instanceof SpawnEggItem;
    }

    private static void rejectFigment(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y, z, 6, 0.25D, 0.35D, 0.25D, 0.02D);
        net.minecraft.sounds.SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(
                ResourceLocation.fromNamespaceAndPath("minecraft", "entity.breeze.wind_burst"));
        level.playSound(null, x, y, z, sound, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private static void enforceDreamTime(ServerPlayer player, ReverieSession session) {
        long elapsed = session.dreamElapsedTicks();
        long warningTicks = ReverieConfig.OVERSTAY_WARNING_MINUTES.get() * 1200L;
        if (warningTicks > 0L && elapsed >= warningTicks && !session.overstayWarned()) {
            session.markOverstayWarned();
            player.displayClientMessage(Component.translatable("message.reverie.overstay_warning"), false);
            player.playNotifySound(SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 0.65F, 0.75F);
        }
        long maximumTicks = ReverieConfig.MAX_DREAM_MINUTES.get() * 1200L;
        if (maximumTicks > 0L && elapsed >= maximumTicks) awaken(player, null);
    }

    private static void applyOverstayEffect(ServerPlayer player, long elapsedTicks) {
        long warningTicks = ReverieConfig.OVERSTAY_WARNING_MINUTES.get() * 1200L;
        if (warningTicks <= 0L || elapsedTicks < warningTicks) return;
        ResourceLocation id = ResourceLocation.tryParse(ReverieConfig.OVERSTAY_EFFECT.get());
        if (id == null) return;
        BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(effect -> player.addEffect(new MobEffectInstance(
                effect, ReverieConfig.OVERSTAY_EFFECT_SECONDS.get() * 20,
                ReverieConfig.OVERSTAY_EFFECT_AMPLIFIER.get())));
    }

    private static Item configuredItem(String id) {
        if (id == null || id.isBlank()) return null;
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) return null;
        Item item = BuiltInRegistries.ITEM.get(location);
        return item == Items.AIR ? null : item;
    }

    private static boolean canEnterBed(ServerPlayer player, BlockPos wakingBed, boolean owner) {
        if (owner) return true;
        ReverieBedLinksData links = ReverieBedLinksData.get(player.server);
        int occupants = links.occupantCount(wakingBed);
        int maximum = ReverieConfig.MAX_DREAMERS_PER_BED.get();
        java.util.UUID bedOwner = ReverieBedOwnersData.get(player.server).owner(wakingBed);
        boolean ownerPresent = bedOwner != null && links.containsDreamer(wakingBed, bedOwner);
        return occupants < maximum && (ownerPresent || occupants < maximum - 1);
    }

    private static void showBedAccess(ServerPlayer player, BlockPos wakingBed) {
        java.util.UUID owner = ReverieBedOwnersData.get(player.server).claimIfUnowned(wakingBed, player.getUUID());
        int occupants = ReverieBedLinksData.get(player.server).occupantCount(wakingBed);
        int maximum = ReverieConfig.MAX_DREAMERS_PER_BED.get();
        player.displayClientMessage(Component.translatable(player.getUUID().equals(owner)
                ? "message.reverie.bed_info_owner" : "message.reverie.bed_info_guest", occupants, maximum), false);
    }

    private static void feedback(ServerPlayer player, String category, String translation, Object... args) {
        long now = player.server.getTickCount();
        String key = player.getUUID() + ":" + category;
        Long next = FEEDBACK_COOLDOWNS.get(key);
        if (next != null && next > now) return;
        FEEDBACK_COOLDOWNS.put(key, now + 20L);
        player.displayClientMessage(Component.translatable(translation, args), true);
    }

    private static void purgeBlockedVanillaInventory(ServerPlayer player) {
        purgeBlockedList(player, player.getInventory().items);
        purgeBlockedList(player, player.getInventory().armor);
        purgeBlockedList(player, player.getInventory().offhand);
    }

    private static void purgeBlockedList(ServerPlayer player, net.minecraft.core.NonNullList<ItemStack> stacks) {
        for (int slot = 0; slot < stacks.size(); slot++)
            if (!stacks.get(slot).isEmpty() && isBlocked(player, stacks.get(slot))) stacks.set(slot, ItemStack.EMPTY);
    }

    private record PendingDream(BlockPos wakingBed, long completeAt, boolean guest,
                                InteractionHand hand, Item costItem, boolean bedOwner) {}

    private static BlockPos placeArrivalBed(ServerLevel level, BlockPos center, Direction preferredDirection) {
        Direction direction = preferredDirection.getAxis().isHorizontal() ? preferredDirection : Direction.NORTH;
        for (int radius = 0; radius <= 16; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    BlockPos foot = new BlockPos(center.getX() + dx, 32, center.getZ() + dz);
                    BlockPos head = foot.relative(direction);
                    if (!level.getBlockState(foot).canBeReplaced() || !level.getBlockState(head).canBeReplaced()) continue;
                    if (!level.getBlockState(foot.below()).is(Blocks.WHITE_CONCRETE)
                            || !level.getBlockState(head.below()).is(Blocks.WHITE_CONCRETE)) continue;
                    BlockState base = Reverie.DREAMWEAVERS_BED.get().defaultBlockState()
                            .setValue(BedBlock.FACING, direction).setValue(BedBlock.OCCUPIED, false);
                    level.setBlock(foot, base.setValue(BedBlock.PART, BedPart.FOOT), 3);
                    level.setBlock(head, base.setValue(BedBlock.PART, BedPart.HEAD), 3);
                    return foot;
                }
            }
        }
        return center;
    }
}
