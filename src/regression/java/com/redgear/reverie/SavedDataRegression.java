package com.redgear.reverie;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Executable regressions for persistence behavior independent of a running world. */
public final class SavedDataRegression {
    private static int checks;
    public static void main(String[] args) {
        UUID player = UUID.randomUUID();
        BlockPos first = new BlockPos(0, 32, 0), second = new BlockPos(400, 32, 400);
        ReverieBookmarksData marks = new ReverieBookmarksData();
        check(marks.target(player, null, null) == null, "Missing session is safe");
        marks.target(player, first, first);
        check(marks.save(new CompoundTag(), null).getCompound("Players").isEmpty(), "Reading does not create records");
        check(marks.put(player, first, "Bed", first.above()), "Bookmark named Bed is allowed");
        marks.cycle(player, first);
        check(marks.target(player, first, first).equals(first), "Cycle reaches the real bed");
        marks.cycle(player, first);
        check(marks.target(player, first, first).equals(first.above()), "Bed bookmark is distinguishable");
        marks.put(player, second, "Other", second.above());
        check(marks.target(player, first, first).equals(first.above()), "Selections are scoped by dream");
        ReverieBookmarksData loaded = ReverieBookmarksData.load(marks.save(new CompoundTag(), null), null);
        check(loaded.target(player, first, first).equals(first.above()), "Selection survives save/load");
        check(!loaded.remove(player, first, "Absent"), "Deleting an absent bookmark does not create one");
        check(loaded.remove(player, first, "Bed"), "Selected bookmark can be deleted");
        check(loaded.target(player, first, first).equals(first), "Deletion falls back to bed");
        for (int i = 0; i < ReverieBookmarksData.MAX_BOOKMARKS; i++) check(loaded.put(player, first, "Mark " + i, first), "Bookmark within limit");
        check(!loaded.put(player, first, "Overflow", first), "Bookmark limit is enforced");
        check(loaded.put(player, first, "Mark 0", second), "Existing bookmarks remain editable at limit");
        check(!loaded.put(player, first, "x".repeat(49), first), "Long names rejected");
        ReverieCompassDestinationsData compassDestinations = new ReverieCompassDestinationsData();
        String savedBed = compassDestinations.toggle(player, second, "Workshop");
        check("Workshop".equals(savedBed), "Compass bed can be saved globally");
        var savedDestinations = ReverieCompassDestinationsData.load(
                compassDestinations.save(new CompoundTag(), null), null);
        check(second.equals(savedDestinations.all(player).get("Workshop")),
                "Compass bed survives save and reload");
        check(savedDestinations.removePosition(second) == 1 && savedDestinations.all(player).isEmpty(),
                "Destroying a bed removes its Compass destination");
        ReverieBookmarksData bedDestinations = new ReverieBookmarksData();
        String scopedBed = bedDestinations.toggleBed(player, first, second, "Workshop");
        check("Workshop".equals(scopedBed) && second.equals(bedDestinations.all(player, first).get(scopedBed)),
                "Compass bed destination is saved");
        check(bedDestinations.toggleBed(player, first, second, "Workshop") == null,
                "Using the compass on a saved bed removes it");
        bedDestinations.toggleBed(player, first, second, "Workshop");
        check(bedDestinations.removePosition(second) == 1 && bedDestinations.all(player, first).isEmpty(),
                "Breaking a bed removes its destination");
        ReverieMobAllowlistData mobs = new ReverieMobAllowlistData();
        var dragon = ResourceLocation.withDefaultNamespace("ender_dragon");
        var warden = ResourceLocation.withDefaultNamespace("warden");
        check(!mobs.contains(dragon) && mobs.contains(warden), "Defaults allow Warden and deny Dragon");
        mobs.add(dragon); mobs.remove(warden);
        var restored = ReverieMobAllowlistData.load(mobs.save(new CompoundTag(), null), null);
        check(restored.contains(dragon) && !restored.contains(warden), "Admin choices survive restart");
        ReverieAnchorsData anchors = new ReverieAnchorsData();
        anchors.add(first);
        anchors.name(first, "Workshop");
        check(anchors.findFor(new BlockPos(47, 32, 47)).equals(first), "Anchor includes its positive corner chunk");
        check(anchors.findFor(new BlockPos(48, 32, 48)) == null, "Anchor excludes next positive chunk");
        check(anchors.findFor(new BlockPos(-32, 32, -32)).equals(first), "Anchor includes negative corner chunk");
        check(anchors.findFor(new BlockPos(-33, 32, -33)) == null, "Negative chunk boundary is correct");
        ReverieAnchorsData overlappingAnchors = new ReverieAnchorsData();
        BlockPos fartherAnchor = new BlockPos(31, 200, 31);
        BlockPos nearerAnchor = new BlockPos(17, -60, 17);
        overlappingAnchors.add(fartherAnchor);
        overlappingAnchors.add(nearerAnchor);
        check(overlappingAnchors.findFor(new BlockPos(16, 90, 16)).equals(nearerAnchor),
                "Overlapping anchors choose the horizontally nearest bed regardless of height");
        check(anchors.overlapping(new BlockPos(64, 32, 64)).equals(first), "Corner overlap detected");
        check(anchors.overlapping(new BlockPos(80, 32, 80)) == null, "Separated regions do not overlap");
        check(anchors.save(new CompoundTag(), null).getCompound("Anchors").getString(Long.toString(first.asLong())).equals("Workshop"), "Anchor name serialized");
        anchors.remove(first);
        check(anchors.name(first).isEmpty(), "Removed anchor leaves no name behind");
        ReverieBedLinksData links = new ReverieBedLinksData();
        UUID guest = UUID.randomUUID();
        links.join(first, second, player, false); links.join(first, second, guest, false);
        check(links.leave(first, player) == null, "Guest retains temporary bed after host leaves");
        check(links.leave(first, guest).equals(second), "Final guest releases temporary bed");
        links.join(first, second, player, true);
        check(links.leave(first, player) == null, "Final exit preserves anchored bed");
        System.out.println("Passed " + checks + " saved-data regression checks.");
    }
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
        checks++;
    }
}
