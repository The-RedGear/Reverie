# Changelog

## 0.1.3 - Dreams Worth Returning To

- Overlapping Dream Anchor regions now use the anchor nearest to the entered Dreamweaver's Bed for both arrival and saved Dream Inventory.
- Holding a Dreamweaver's Bed in an anchor region now identifies the Dream Anchor that will be used.

### Audit fixes

- Enforced restricted items across saved Dream Inventories and Dream Imprints, including Curios, Accessories, and cosmetic slots. Waking recovery snapshots are never altered by this cleanup.
- Migrated bed ownership records to dimension-aware keys so identical coordinates in the Overworld and Reverie cannot overwrite one another.
- Added a configurable grace period between the overstay warning and its wake-up effect.
- Added confirmation before replacing or erasing a Dream Imprint.
- Restricted Dream Anchor naming to the bed owner or an administrator.
- Made Figment Cage range guidance render at the player's height and limited one-hit mob cleanup to player attacks.
- Changed forced loaded-chunk purges to use bounded incremental work instead of blocking the server tick.
- Added clearer `block`/`allow` and `allow`/`deny` command aliases while preserving the existing commands.
- Prevented the Reverie inventory purge from running after a timed wake restores Survival inventory.
- Preserved restored waking status effects and corrected arrival height for vertically moved beds.
- Restricted Dream Imprints to non-anchored destinations.
- Made bookmark selection persist independently for each dream, stabilized cycling, and fixed bookmarks named Bed.
- Added bookmark count/name limits, missing-bookmark feedback, and destination safety checks.
- Bound anchor overlap confirmation to the intended bed.
- Stopped automatic bed checks from loading remote Overworld chunks.
- Cleared transient lighting, confirmation, and purge state when players disconnect or servers stop.
- Preserved administrator mob restrictions across restarts.
- Matched Jade cage population to the enforced limit and added contextual tool instructions.
- Expanded active-session diagnostics and added 58 saved-data regression checks to the build.

See `docs/RELEASE_AUDIT_0.1.3.md` for unresolved release blockers and the limits of this verification.

### Features

- Expanded the Reverie advancement path to teach lighting previews, Dream Imprints, saved beds, Compass travel, and anchor naming through their real in-game interactions.
- Added a Recovery Compass destination menu. Saved beds belong to the player and remain available after waking, replacing, or losing the Compass. Using the Compass on a bed toggles it, and destroying that bed removes it.
- Added personal Dream Bookmarks. Use a renamed Name Tag on a location in the Reverie to save it, then choose it from the Recovery Compass.
- Added personal Dream Imprints for temporary beds. Use a Book on a non-anchored Reverie bed to save the current inventory as the starting kit for future temporary dreams; the Book itself is excluded.
- New dream inventories now begin with a Recovery Compass. Existing Dream Imprints and saved anchor inventories continue loading exactly as saved.
- Anchored beds can now be named with renamed Name Tags. Anchor names appear in Jade and in overlap warnings.
- Overworld beds now show the name of the Dream Anchor serving their region even while nobody is inside the Reverie.
- Added an automatic warning when a dreamer's Overworld bed is missing or no longer has a safe place to wake.
- Guest countdowns now identify the bed host as well as the remaining time.
- Added Jade details for Figment Cages, including charge, chunk range, and current mob capacity.
- Adding a Dream Anchor in an overlapping region now warns the player and requires a second use to confirm.
- Clock feedback now uses clear lighting preset names instead of relying on tick values.
- Right-clicking a Clock cycles personal lighting presets. Sneak-right-clicking returns the player to the server's shared lighting; offhand use has no special behavior, and ordinary players cannot alter lighting for other dreamers.
- Added persistent recovery history and `/reverie recovery history <player>` for administrators.
- Added configurable automatic safety checks that repair a missing active inventory snapshot and warn about unsafe waking beds.
- Updated `/reverie doctor` and configuration output to report automatic safety-check status.
- Released this update as version `0.1.3`.

## 0.1.2 - Safer Dreams

- Added owner-only, one-time survival inventory recovery through a Dreamweaver's Bed when an interrupted transition leaves a recovery snapshot open. A second confirmation is required and the snapshot closes after restoration to prevent duplication.
- Added optional Jade bed inspection showing ownership, occupancy, capacity, access policy, and Dream Anchor status.
- Jade now resolves Reverie beds back to their linked Overworld beds so both sides display the same live occupancy and access information.
- Simplified Jade bed details by hiding default access and temporary-bed labels; only special access rules and anchored status are shown.
- Added optional JEI information pages for Dreamweaver's Beds and Figment Cages.
- Vanilla Recovery Compasses carried inside the Reverie now point toward the player's linked bed without replacing their normal last-death target outside the Reverie.
- Sneak-using a Recovery Compass inside the Reverie safely returns the player to their linked Dreamweaver's Bed.
- Reverie arrival beds now inherit the waking bed owner's claim. Anchoring an unclaimed bed claims it for the player creating the Dream Anchor, and ownership is retained when an anchored bed is broken.
- Added administrator-controlled default, private, and public bed access modes, per-player invitations, and temporary host assignment while retaining the existing owner-first rules by default.
- Added configurable notifications when guests enter or leave an owner's shared dream.
- Added configurable void recovery modes: awaken, return to the Reverie bed, or disabled. Existing awaken behavior remains the default.
- Added advancements for entering a shared dream, anchoring a bed, awakening safely, and overstaying.
- Added reduced-particle accessibility support and moved all new player-facing text into translations.
- Expanded `/reverie doctor` with session-state, safety-mode, optional-integration, access-override, notification, and audit status checks.
- Added concise audit records for entering, awakening, player recovery, forced awakening, administrative recovery, bed ownership, permissions, invitations, and host changes.
- Released this update as version `0.1.2`.

- Made the Reverie sky and actual build lighting follow its selected time, blending from the original pearly-white noon to a cool dark-gray midnight without orange sunrise or sunset tones.
- Corrected inherited grass tint regions to center on the linked Reverie anchor rather than the waking bed's original coordinates.
- Using a Clock cycles through sunrise, noon, sunset, and midnight; sneaking while using it makes fine 1,000-tick adjustments.
- Noon alone restores Reverie's original ambient-bright blank-canvas lighting, while every other time shows natural light and darkness.
- Temporary lighting returns to noon when its controlling player leaves the Reverie or after a configurable timeout (five minutes by default).
- Clock changes now synchronize immediately like vanilla time commands, midnight uses an Overworld-dark lightmap and near-black dream sky, and returning players receive their saved grass regions again on login.
- Fully decoupled Reverie lighting time from the Overworld's shared world time and forced clear Reverie weather regardless of Overworld rain or thunder.
- Replaced the daylight-derived sky curve with explicit dawn, noon, dusk, and midnight colors, added a bubble-pop adjustment cue, and standardized Reverie grass to the vanilla plains color.
- Defined the Lucid Expanse biome's grass color directly as `#91BD59` so biome-level and modded rendering paths use the same tint.
- Looking at an occupied Dreamweaver's Bed now shows its current dreamers above it, with the active owner accented and guests in white.
- Breaking an occupied Dreamweaver's Bed now requires a second deliberate break attempt after a warning.
- Non-owners must now pay the configured guest-entry cost when using another player's bed even while it is unoccupied; owners and creative players remain exempt.
- A bed's owner must now enter before guests can join. If the owner leaves, guests wake after a configurable one-minute grace period unless the owner returns first.
- Bed-host warnings now name the owner, the default guest grace period is one minute, its countdown persists through bed replacement, and occupied-bed labels correctly normalize the rendered head to the stored foot position.
- Guests now see their remaining host-absence grace time update once per second in the action bar.
- Capped the guest host-absence grace period at 60 seconds, including automatic correction of older 180-second configs, and removed the redundant chat alert.
- Moved occupied-bed floating names out of the bed block-entity renderer and into the reliable world render stage.
- Occupied-bed labels now remain visible within 32 blocks without crosshair targeting. Waking now validates vanilla bed stand-up space and falls back to the player's respawn point or world spawn when the bed area is obstructed.
- A bed owner who disconnects inside the Reverie now counts as absent: guests receive the configured countdown and awaken unless the owner reconnects to the same active dream session in time.
- Rebuilt occupied-bed names around Minecraft's native nametag drawing path, with direct client event registration, render-thread-safe occupancy updates, and the proper entity-effect render stage.
- Occupied beds now resolve disconnected players through Minecraft's profile cache instead of displaying a shortened UUID.
- Sun-sensitive mobs no longer ignite from daylight in the Reverie, regardless of its selected lighting time. Ordinary fire and lava remain effective.
- Removed the unreliable floating occupied-bed labels. Sneak-use now shows the bed owner, every current dreamer, and capacity together in the action bar.
- Crafted Dreamweaver's Beds now bind permanently to their crafter. Loot and Creative beds bind to their first placer, ownership survives breaking and pickup, and the owner is shown in the item tooltip.

## 0.1.0 - Initial alpha

- Added the Dreamweaver's Bed and coordinate-aligned Reverie dimension.
- Added transactional waking-inventory backups and administrative recovery tools.
- Added anchored, region-specific Dream Inventories.
- Added Curios and Accessories support, including cosmetic slots.
- Added shared-bed capacity, guest entry cost, occupancy visuals, transition particles, and sounds.
- Reserved a place for each bed's owner and exempted owners from guest-entry costs.
- Added the Figment Cage with five charge states, chunk-based ranges, mob caps, and boundary visualization.
- Added configurable dream-duration warnings and wake-up effects.
- Added advancement, XP, portal, damage, explosion, and survival-resource protections.
- Added a persistent block/item blacklist with tab completion and held-item targeting.
- Added dimension-level illegal placement rejection and incremental loaded-chunk purge controls.
- Added protection against restricted content introduced through automated building or schematic tools.
- Added a persistent, operator-controlled Reverie clock with day, noon, night, midnight, and custom lighting settings.
- Added player Clock controls for advancing Reverie time or returning it to noon, with a visible cooldown and server configuration.
- Added a shared Clock cooldown and dimension-wide feedback naming the player who changed the lighting.
- Added Overworld-biome grass tinting around each entry location without modifying or regenerating existing Reverie chunks.
- Persisted learned grass-tint regions across visits and fixed gray inventory grass rendering.
- Preserved Reverie's original pearly full-bright appearance at noon while allowing darker Clock settings to expose testable lighting.
- Removed vanilla sunrise and sunset colors from the Reverie and made darker times use a stable neutral-gray sky.
- Made Figment Cages authorize vanilla mob-spawner placement and light checks while retaining region, deny-list, collision, and population protections.
- Added sneak-use bed information for ownership, occupancy, capacity, and owner reservation.
- Clarified on the Figment Cage that hostile spawners still require darkness.
- Added three Reverie advancements for obtaining the bed, entering the dreamscape, and charging a Figment Cage.
- Added rate-limited, item-specific rejection feedback.
- Added startup configuration validation and `/reverie doctor` diagnostics.
- Added explicit saved-data schema versions while retaining tolerant loading of pre-versioned data.

This release is an alpha intended for testing and feedback. Back up worlds before installation.
