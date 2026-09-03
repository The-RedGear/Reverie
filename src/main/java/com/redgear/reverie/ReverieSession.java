package com.redgear.reverie;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;

public final class ReverieSession implements INBTSerializable<CompoundTag> {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Reverie.MOD_ID);
    public static final Supplier<AttachmentType<ReverieSession>> TYPE = ATTACHMENTS.register(
            "session", () -> AttachmentType.serializable(ReverieSession::new).copyOnDeath().build());
    private boolean active;
    private CompoundTag wakingPlayer = new CompoundTag();
    private BlockPos wakingBed;
    private BlockPos dreamBed;
    private boolean anchorInventory;
    private long dreamElapsedTicks;
    private boolean overstayWarned;
    public boolean active() { return active; }
    public CompoundTag wakingPlayer() { return wakingPlayer.copy(); }
    public BlockPos wakingBed() { return wakingBed; }
    public BlockPos dreamBed() { return dreamBed; }
    public boolean anchorInventory() { return anchorInventory; }
    public long dreamElapsedTicks() { return dreamElapsedTicks; }
    public void tickDream() { if (active) dreamElapsedTicks++; }
    public boolean overstayWarned() { return overstayWarned; }
    public void markOverstayWarned() { overstayWarned = true; }
    public void begin(CompoundTag tag, BlockPos wakingBed, BlockPos dreamBed, boolean anchorInventory, long ignoredStartTime) {
        active = true;
        wakingPlayer = tag.copy();
        this.wakingBed = wakingBed.immutable();
        this.dreamBed = dreamBed.immutable();
        this.anchorInventory = anchorInventory;
        this.dreamElapsedTicks = 0L;
        this.overstayWarned = false;
    }
    public void finish() {
        active = false;
        wakingPlayer = new CompoundTag();
        wakingBed = null;
        dreamBed = null;
        anchorInventory = false;
        dreamElapsedTicks = 0L;
        overstayWarned = false;
    }

    @Override public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", 1);
        tag.putBoolean("Active", active);
        if (active) {
            tag.put("WakingPlayer", wakingPlayer.copy());
            if (wakingBed != null) tag.putLong("WakingBed", wakingBed.asLong());
            if (dreamBed != null) tag.putLong("DreamBed", dreamBed.asLong());
            tag.putBoolean("AnchorInventory", anchorInventory);
            tag.putLong("DreamElapsedTicks", dreamElapsedTicks);
            tag.putBoolean("OverstayWarned", overstayWarned);
        }
        return tag;
    }
    @Override public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        active = tag.getBoolean("Active");
        wakingPlayer = tag.contains("WakingPlayer") ? tag.getCompound("WakingPlayer").copy() : new CompoundTag();
        wakingBed = tag.contains("WakingBed") ? BlockPos.of(tag.getLong("WakingBed")) : null;
        dreamBed = tag.contains("DreamBed") ? BlockPos.of(tag.getLong("DreamBed")) : null;
        anchorInventory = tag.getBoolean("AnchorInventory");
        dreamElapsedTicks = tag.getLong("DreamElapsedTicks");
        overstayWarned = tag.getBoolean("OverstayWarned");
    }
}
