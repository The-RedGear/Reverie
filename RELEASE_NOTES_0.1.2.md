# Reverie 0.1.2 | Safer Dreams

This update focuses on protecting player progress, making shared beds easier to manage, and giving players clearer information both inside and outside the Reverie.

## Inventory Recovery

- Added owner-only inventory recovery through the Dreamweaver's Bed.
- If an interrupted transition leaves a valid recovery backup behind, the bed owner can sneak-use their bed with an empty hand to begin recovery.
- Recovery requires a second confirmation and closes the backup after restoration to prevent item duplication.
- Added configurable void recovery. Falling into the void can awaken the player, return them to their Reverie bed, or use the disabled behavior selected by the server.

## Bed Access and Shared Dreams

- Reverie arrival beds now inherit the waking bed owner's claim.
- Creating a Dream Anchor claims an unowned bed, and that ownership remains attached if the anchor is broken and picked up.
- Added administrator controls for default, private, and public bed access.
- Added commands for inviting players to beds and assigning temporary hosts.
- Existing owner-first entry rules remain the default.
- Added configurable notifications when guests enter or leave a shared dream.
- Added an advancement for entering the Reverie through another player's bed.
- Added an advancement for safely awakening with the waking inventory restored.
- Added an advancement for creating a Dream Anchor.

## Player Information and Mod Support

- Added optional Jade support for inspecting Dreamweaver's Beds.
- Jade displays bed ownership, occupancy, capacity, access rules, and Dream Anchor status.
- Reverie-side beds now show the same live Jade occupancy and access information as their linked Overworld beds.
- Jade hides ordinary default and temporary states, keeping special access rules and Anchored Bed status easy to spot.
- Added optional JEI information pages for Dreamweaver's Beds and Figment Cages.
- JEI explains that guests normally need the owner to enter first and must offer an Amethyst Shard.
- Recovery Compasses carried inside the Reverie point toward the player's linked bed.
- Sneak-using a Recovery Compass safely returns the player to that bed.
- Recovery Compasses return to their normal last-death behavior outside the Reverie.

## Accessibility and Feedback

- Added a reduced-particle option for players who prefer quieter visual effects.
- Added translated player-facing messages for the new recovery, access, notification, and safety features.
- Added an advancement for remaining in the Reverie beyond its configured warning.

## Administration and Diagnostics

- Expanded `/reverie doctor` with checks for active sessions, recovery safety, optional integrations, bed access overrides, notifications, and audit status.
- Added concise audit records for entering, awakening, player recovery, forced awakening, administrative recovery, bed ownership, permissions, invitations, and temporary host changes.

Back up your world before updating. Jade and JEI support are optional and do not need to be installed for Reverie to run.
