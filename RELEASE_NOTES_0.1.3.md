# Reverie 0.1.3 | Dreams Worth Returning To

This update makes the Reverie easier to navigate, personalize, and understand while strengthening the systems that protect player inventories and shared dreams.

## Recovery Compass and Dream Navigation

- Added a destination menu to the Recovery Compass.
- Use a Recovery Compass on another Dreamweaver's Bed to save or remove it as a destination.
- Saved beds remain available after awakening or replacing the Recovery Compass.
- Destroying a saved bed removes it from the destination list.
- Added personal Dream Bookmarks. Use a renamed Name Tag on a location in the Reverie to save it as a Compass destination.
- Unsafe, missing, unloaded, and obstructed destinations cannot be used.
- Players entering without a saved Dream Inventory or Dream Imprint receive a Recovery Compass.
- Updated the destination menu and related messages to consistently use the Dreamweaver's Bed name.
- Existing destinations using the old Dream Bed label are updated automatically.

## Dream Imprints and Inventories

- Added Dream Imprints for temporary dreams.
- Use a Book on a temporary Dreamweaver's Bed to save the current inventory as the starting inventory for future temporary dreams.
- The Book used to create the Dream Imprint is not included in the saved inventory.
- Replacing or erasing a Dream Imprint requires confirmation.
- Dream Imprints are only used by non-anchored beds. Dream Anchors continue using their own saved Dream Inventories.
- Restricted items are removed from Dream Imprints and Dream Inventories, including supported Curios, Accessories, and cosmetic slots.
- Fixed timed awakening so the restored Survival inventory cannot be affected by the Reverie's item restrictions.
- Restored waking status effects are now preserved.

## Dream Anchors

- Dream Anchors can now be named with renamed Name Tags.
- Anchor names appear in Jade, overlap warnings, and Overworld bed information.
- Only the bed owner or an administrator can rename a Dream Anchor.
- Creating an overlapping Dream Anchor now requires confirmation.
- When anchor regions overlap, the closest Dream Anchor to the entered Overworld bed controls both the arrival location and saved Dream Inventory.
- Holding a Dreamweaver's Bed in the Overworld now identifies the Dream Anchor serving that location.
- Unnamed anchors are identified by their coordinates.
- Corrected arrival height for Dreamweaver's Beds moved vertically within the Reverie.
- Improved ownership records so beds at matching coordinates in different dimensions cannot interfere with each other.

## Personal Lighting

- Clock lighting previews are now personal and do not change the lighting for other players.
- Right-click a Clock to cycle through the Reverie's lighting presets.
- Sneak-right-click a Clock to return to the Reverie's normal lighting.
- Lighting feedback now shows concise preset names.
- Fixed Reverie lighting briefly changing when pausing or unpausing the game.
- Reverie lighting is now protected from unrelated world-time updates.

## Shared Dreams and Safety

- Guest countdowns now show whose shared dream is closing and how much time remains.
- Added an automatic warning when the Overworld bed is missing or no longer has a safe waking location.
- Added configurable automatic safety checks for active inventory backups and waking beds.
- Automatic bed checks no longer load distant Overworld chunks.
- Added persistent recovery history for administrators.
- Added `/reverie recovery history <player>`.
- Temporary session, lighting, confirmation, and cleanup data is now cleared when players disconnect or the server stops.

## Jade, Guidance, and Advancements

- Added Jade information for Figment Cages, including charge, range, and current mob capacity.
- Updated Jade's Figment count to match the cage's actual limit.
- Added contextual instructions for Books, Clocks, Recovery Compasses, and Name Tags while inside the Reverie.
- Added a Reverie creative tab containing the items used by the mod.
- Added advancements for creating a Dream Imprint, previewing lighting, saving a bed, traveling with the Recovery Compass, and naming a Dream Anchor.

## Administration and Reliability

- Expanded `/reverie doctor` with additional active-session and automatic safety checks.
- Updated configuration reporting for the new safety options.
- Added clearer command aliases for managing blocked items and permitted mobs while keeping the existing commands available.
- Preserved administrator mob restrictions across server restarts.
- Improved bookmark limits, naming, selection, and persistence between dreams.
- Improved destination checks to prevent travel to unsafe locations.
- Changed large loaded-area cleanups to process gradually instead of stalling a server tick.
- Improved Figment Cage guidance and limited instant mob cleanup to player attacks.

Back up your world before updating. Jade and JEI support remain optional.
