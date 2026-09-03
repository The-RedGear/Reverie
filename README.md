# Reverie

Reverie is a NeoForge 1.21.1 mod for location-aligned creative dreaming in survival worlds.

The jar targets Minecraft 1.21.1 and accepts NeoForge 21.1.0 or newer. It is not a cross-Minecraft-version jar; later Minecraft releases require separate builds because their game APIs and data formats differ.

## Current vertical slice

- Craft and place a true two-block, vanilla-rendered **Dreamweaver's Bed** in the Overworld.
- Breaking either half in Survival drops exactly one Dreamweaver's Bed even when mined by hand; axes are tagged as the faster preferred tool.
- Dreamweaver's Bed uses its own 64x64 entity texture in the world and inventory, and has an endgame recipe built around any vanilla bed color, a Recovery Compass, Echo Shard, Phantom Membranes, and Amethyst Shards.
- Use it to escrow the complete waking player NBT and enter `reverie:the_reverie` at matching X/Z coordinates.
- The player receives ordinary vanilla Creative mode and arrives standing on the white-concrete surface.
- Use the shared Dreamweaver's Bed inside the Reverie to awaken. It remains while any player linked to it is still dreaming and disappears when the final dreamer leaves.
- Awakening clears dream inventories and restores the waking state and exact Overworld position.
- Runtime bridges clear Curios and Accessories functional and cosmetic handlers when those mods are installed; Accessories 1.21.1 uses its vanilla-container API rather than Forge item-handler methods.
- `reverie:unusable_in_reverie` block and item tags deny dangerous interactions. Ender Chests are included by default; datapacks can append more entries later.
- A client-side `reverie:white` dimension effect replaces the black End sky with pale white fog and no celestial skybox.
- The Reverie is fixed at midday with maximum ambient light and a full-bright lightmap for terrain, entities, and held items.
- XP and level changes are suppressed in the Reverie; dream hunger is fixed at full with zero saturation and exhaustion.
- Advancement criteria are rejected before progress is recorded while a player is in the Reverie, preventing achievements, recipes, advancement rewards, and advancement-driven mod mechanics from being earned in a dream.
- Potion and other active mob effects are cleared on both entry and awakening instead of crossing the boundary.
- Ender Chests, Creatender Link ender vaults/tanks, and Camping enderpacks/enderbags are denied by the default interaction tags.
- Pocket Factory entrances and all listed Nexus portal cores are denied by default.
- Create Power Loader's andesite/brass chunk loaders and Create Ender Link's vault, tank, and scope filter are denied by default.
- Creative/operator infrastructure is denied by default, including command blocks and command-block minecarts, structure blocks, jigsaws, the debug stick, barriers, structure voids, light blocks, spawners, vaults, bedrock, and knowledge books. Restricted blocks are rejected at placement time as well as interaction time.
- Vanilla Nether portal creation is canceled, End portal activation is denied, and all unauthorized entity travel out of the Reverie is canceled as a final containment layer.
- Entering requires a second use of the waking bed within five seconds, providing an in-world confirmation prompt.
- After confirming, the player visibly lies in the waking bed for two seconds before the dream transition; this pose works at any time of day and does not change the player's vanilla respawn point.
- Hostile mobs within vanilla bed-safety range prevent the dream transition and show a danger message, including for Creative-mode dreamers.
- Arrival places a Dreamweaver's Bed on the untouched white-concrete plane near the matching coordinates without overwriting dream builds.
- Arrival beds are shared by waking bed: everyone entering through the same Dreamweaver's Bed reuses one Reverie bed. Persistent occupant tracking keeps it present until the final linked dreamer leaves; breaking the waking bed removes the shared Reverie bed immediately.
- Every transition emits a wind-charge gust at both the departure and arrival beds for the traveler and nearby players.
- Arrival in the Reverie plays one randomly selected `entity.breeze.idle_ground` sample; awakening plays one randomly selected `entity.breeze.slide` sample. Minecraft's sound event system chooses the variation, so samples never stack simultaneously.
- In the Reverie, using an Echo Shard on a Dreamweaver's Bed makes it a permanent Dream Anchor without consuming the shard. Overworld beds in the matching 5x5-chunk region use the nearest anchor; sneak-use with an Echo Shard unlocks it.
- Holding a Dreamweaver's Bed in an Overworld chunk covered by an anchor produces a sparse personal End Rod particle preview. A quiet `entity.breeze.whirl` occasionally plays while the holder remains in covered chunks.
- Falling into the void invokes normal awakening; all player damage and every non-player mob spawn are canceled inside the Reverie.
- Explosions may still appear and sound normally, but their affected-block list is cleared so they cannot damage or remove Reverie builds.
- Operators can maintain a persistent per-world override list with `/reverie blocklist add <id>`, `/reverie blocklist remove <id>`, and `/reverie blocklist list`. IDs tab-complete from both registries; placeable-block IDs automatically update both their block and item forms. Removing an entry also overrides a matching built-in datapack tag.
- Operators can safely recover an online dreamer with `/reverie awaken <player>`. The player name uses native tab completion and the normal waking-state restoration, including inventory and game mode.
- Before inventory clearing, Reverie writes a second complete waking-state snapshot to `data/reverie_recovery.dat` and forces that world data to disk. `/reverie recovery status <player>` inspects it, `/reverie recovery restore <player>` restores it, and `/reverie recovery rollback <player>` reverses an accidental recovery using the automatically captured pre-restore state. Online usernames tab-complete.

## Important status

This is an alpha release. The core transition, void awakening, inventory escrow, persistent recovery backup, anchored inventories, blacklist enforcement, and admin recovery systems are implemented, but the mod should still be tested against a server's exact mod list before production use. Back up worlds before installation.

## Build

Use Java 21 and run `gradlew build`. The mod jar is written to `build/libs`.
