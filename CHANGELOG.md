# Changelog

## Unreleased

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
