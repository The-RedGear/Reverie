# Reverie 0.1.3 — player experience and reliability audit

Audit date: 12 September 2026. This reviews the local 0.1.3 working tree, including its uncommitted features. Source review, executable saved-data checks, and build verification were performed. A dedicated-server play session, rendering comparison, crash injection, and a full modpack compatibility run were not performed. Recommendations below are explicitly separated from implemented fixes.

## Verdict

The core idea is strong: a place to work out a Survival build without leaving the server or spending its resources. The bed gives entry a purpose, and keeping the two inventories separate makes the creative space relevant to a Survival pack.

The current implementation is not ready for an unconditional stable-release recommendation. Inventory transactions and ownership still have structural risks. Several new features were implemented faster than they were documented or tested. Previous successful builds only established that the Java compiled; they did not establish that inventory restoration, equipment effects, or multiplayer rendering worked.

The next milestone should be a trustworthy, understandable building workflow. More features would currently increase the amount players and maintainers must understand. Broad modpack adoption also requires pack authors to be able to tune progression and compatibility. No one configuration will suit every pack.

## Changes made during this audit

| Finding | Correction | Verification |
|---|---|---|
| A timed wake could restore Survival inventory and then continue into the Reverie item purge in the same player tick | Stop processing dream restrictions immediately after the session ends | Source review; compiled |
| An anchor moved vertically still teleported arrivals to Y=33 | Use the arrival bed's actual height | Source review; compiled; in-game clearance test still needed |
| Waking loaded the original player state, then erased its restored status effects | Removed the unconditional effect clearing on restoration | Source review; compiled; modded-effect testing still needed |
| Guests at anchored beds could receive a temporary Dream Imprint | Load imprints only for non-anchored destinations | Source review; compiled |
| Compass use without a valid Dreamweaver's Bed could dereference a null scope | Guard missing/inactive sessions | Source review; null-scope data regression |
| Bookmark reads created empty player/scope records | Reads are now non-mutating | Executable regression |
| Bookmark selection was global per player and was not serialized | Save selection per player and dream scope | Executable round-trip regression |
| A bookmark literally named Bed collided with the built-in destination | Use a separate internal sentinel | Executable regression |
| Shift-using an unknown bookmark name created a bookmark instead of deleting nothing | Report a missing bookmark | Source review; data deletion regression |
| Bookmark count and label length were unbounded | Limit new bookmarks to 32 per scope and names to 48 characters; preserve existing stored entries | Executable boundary regressions |
| Bookmark cycling order could change after reloading | Sort names consistently | Source review; serialized selection regression |
| Bookmark travel trusted a block coordinate, even if its support or headroom was gone | Check loaded destination, border, support, collision, liquid, height, and common block hazards; reject unsafe destinations | Compiled; live teleport scenarios remain required |
| A missing Dreamweaver's Bed could be treated as an ordinary bookmark surface | Recognize the bed destination and report its absence | Source review; compiled |
| An overlap confirmation could authorize a different bed | Bind confirmation to the exact bed and expiry; an existing anchor no longer prompts about itself | Source review; geometry regressions |
| Automatic bed checks could load remote Overworld chunks | Skip unloaded beds rather than treating them as missing | Source review; compiled |
| Automatic recovery could copy a session without an Inventory list | Require a recognizable inventory before recreating the active snapshot | Source review; this is not complete transaction validation |
| Personal previews and transient confirmations could survive disconnects/world changes | Clear transient player state on logout/restoration and static server state on shutdown | Source review; live integrated-server switching test remains |
| Purge tracking was static across server lifetimes | Reset tracked chunks, queues, and counters at shutdown | Source review; compiled |
| Saved mob rules reintroduced the Dragon/Wither defaults and erased Warden/Phantom bans on every load | Treat an existing saved deny list as authoritative | Executable round-trip regression |
| Jade counted mobs differently from the actual cage cap in overlapping areas | Reuse the enforcement population count and send maximum charges from the server | Source review; compiled |
| Tooltips omitted the new interactions and lighting feedback still appended “ticks” to names | Added contextual Book, Clock, Compass, and Name Tag help; added bed/JEI imprint guidance; corrected feedback | Resource/source review; rendering not checked |
| Doctor did not flag missing session inventory, bed positions, or recovery snapshots | Added those checks | Source review; compiled |

The bookmark check deliberately declines travel into unloaded chunks. This prevents arbitrary chunk loading during this cleanup, but it also means distant bookmarks may require flying closer. A later travel implementation should load the destination through a bounded ticket/request and validate after loading; it should not silently generate arbitrary chunks.

## Release blockers still open

### 1. Ownership needs a dimension and a stable identity

`ReverieBedOwnersData` uses `BlockPos` alone, while both Overworld and Reverie code write to it. Two beds at the same X/Y/Z in different dimensions share one ownership key. Breaking or claiming one can affect the other's record. `BROKEN_BED_OWNERS` also uses coordinates without a dimension.

Anchor inventories and bookmarks are likewise identified by bed position. Moving a project anchor can strand its saved state; a replacement at an old position can inherit coordinate-associated state. Whether a project should follow a moved anchor is a design choice, but the accidental association is not an adequate identity model.

Recommended next change: use dimension-aware bed addresses, give permanent anchors stable IDs, and migrate old records with explicit reconciliation against live links and existing bed ownership. Preserve the old data as a backup. Existing ownership cannot always be inferred from an ambiguous coordinate record, so this must not be a blind rewrite. Add tests for identical coordinates in two dimensions and same-position replacement by another owner.

### 2. Inventory safety needs a transaction, not just a backup

The entry path captures a full player snapshot and asks SavedData to save, then clears inventories and later begins the session. Waking finishes the session before restoring all player data. Those operations are spread across multiple records and are not one atomic commit. A crash or compatibility failure between them can leave contradictory state.

`ModdedInventoryBridge` catches reflection failures and logs them; callers generally continue. A partial Accessories/Curios capture or restore is therefore not distinguishable from complete success to the transition code. “Backup available” is not proof that every equipment slot was captured and restored.

Recommended next change: explicit capture/enter/wake/complete stages with a transaction ID, per-integration capture results, retained pre-restore rollback, and success verification before closing recovery. Reject entry before changing inventory when an installed inventory adapter cannot be verified. On failed restoration retain the recovery state and show actionable feedback. Test interruption between each stage in a disposable dedicated-server world.

Self-recovery should remain an exceptional recovery action. Do not automatically overwrite a nonempty current Survival inventory merely because an old record says Active. Recovery history should distinguish player recovery, admin recovery, failed restore, and normal waking.

### 3. Recovery and saved kits need pre-load item validation

Dream templates currently restore items and modded equipment before the recurring purge removes restricted stacks. An item can apply an effect during that interval. Saved snapshots can also retain newly blacklisted items for future restores.

The proposed template-filtering patch was rejected by automatic approval review because it would permanently remove items from saved templates after blocklist changes. It was not applied. Decide whether to retain a reversible original and filter only the loaded copy, or authorize permanent cleanup of templates. Either path should report what was omitted and avoid modifying the Survival snapshot.

This check also needs a policy for nested storage. The current purge is not a universal recursive inspection of every shulker, bundle, modded backpack, custom component, networked storage system, or external inventory. Do not describe the dimension as impossible to exploit with arbitrary mods.

### 4. Live multiplayer evidence is still missing for this revision

The audit's saved-data suite does not instantiate a running server or render a client. The user has tested previous builds, but that does not establish that these changes work with two clients, all optional integrations, shaders, or world upgrades. The exact acceptance scenarios are in `RELEASE_TEST_MATRIX_0.1.3.md`.

## Player journey and feature review

| Feature | Experience assessment | Recommended next refinement |
|---|---|---|
| Obtaining the bed | Ancient City materials give the feature an earned place in Survival. The rare loot reward is consistent with that theme. | Keep the recipe discoverable; ensure recipe unlocks do not require JEI. Clarify that this is a flat workspace at matching coordinates, not copied Overworld terrain. |
| Bed ownership | Binding the expensive item protects player investment and makes sharing understandable. | Resolve dimension-aware ownership first. Preserve ownership through all drop paths, explosions, creative copies, modded breaking, and moved beds. |
| Entering | The short sleep transition communicates travel well. The extra confirmation adds friction to a frequent action. | Keep confirmations for destructive operations; consider making routine entry confirmation configurable after the initial visit. Do not add another confirmation layer. |
| Guest entry | Owner-first entry and an Amethyst cost express the intended progression. | Explain the actual reason for denial before showing a generic confirmation. A configurable cost should display its real item name in gameplay; JEI can say Amethyst is the default. |
| Capacity and reserved owner place | The intent is sensible, but owner, active host, invitations, and public policy form several interacting rules. | Write one access resolver and use it for entry, countdowns, Jade, and commands. State whether a delegated host reserves capacity. |
| Host absence | A visible one-minute countdown is clearer than chat spam. | Current sessions capture a host ID; changing an admin host during a session may not transfer responsibility. Public/invited admission and later host enforcement also need to agree. |
| Waking and bed obstruction | A protected exit is essential. | Vanilla stand-up clearance does not guarantee protection against every trap. Validate nearby hazards and test fallback beds that are themselves unsafe. Avoid promising trap immunity. |
| Temporary vs anchored inventories | Separate project inventories are a useful reason to anchor. | Explain “temporary starts fresh” versus “anchor resumes this project” once at the right moment. Avoid filling the action bar with implementation details. |
| Dream Imprint | A Book is an intuitive way to record a kit, with no new item to learn. | Overwrite currently happens immediately. Add an overwrite confirmation and a deliberate clear/reset interaction. Keep the first save fast. A clear failure must never fall through into saving or waking. |
| Anchor naming | Names make destinations much easier to recognize. | Limit rename/remove rights according to ownership/admin policy. Currently the interaction does not check owner authorization. Use the name in command lists as well as Jade. |
| Anchor overlap | A warning is useful, but players need to know what overlap means. | Explain that the nearest eligible anchor wins, while active links may retain their existing destination. A stable anchor identity should come before elaborate management UI. |
| Recovery Compass | Providing one on a fresh dream is excellent onboarding. Its destination menu keeps the entry bed first and clearly disables unavailable beds. | Verify menu layout at every GUI scale and with long translated names. |
| Bookmarks | Useful for larger projects, but discovery and persistence were unfinished. | Names and destinations now survive consistently. Add scope-aware persistence rules for temporary projects and bounded distant travel. Do not make bookmarks an unrestricted substitute for bed progression. |
| Personal lighting | A strong building tool; private preview avoids multiplayer conflict. | Tell players it is visual only. It does not change the server's mob-spawn conditions. The old timeout/shared-cooldown settings need deprecation or a defined replacement. |
| Noon appearance | The white, ambient look gives the mod identity. | Keep it as default. Test under roofs, with gamma changes, and with shader/rendering mods. Full-bright noon is intentionally not a faithful Survival lighting preview. |
| Figment Cage | Four visible charges and chunk coverage are understandable. | Show range near the user's build height; current boundary particles are fixed at Y=33. Handle imported/removed cages so stored coverage cannot become stale. |
| Mob restrictions and loot | Restrained spawning protects servers; bare-hand cleanup versus weapon loot supports experimentation. | Communicate the loot rule. All incoming mob damage is currently made lethal, including environmental damage, which makes some farm tests misleading. Decide whether only player hits should be instant kills. |
| Spawners | Helpful for experiments, but the promise needs precision. | Spawner checks are explicitly overridden within cages; the current “still need darkness” tooltip and personal lighting do not define a reliable simulation model. Pick and test the intended server behavior. |
| Jade | Good optional discovery, especially for occupancy and named anchors. | Keep defaults hidden. Multiple Overworld beds sharing one anchor need clearly defined occupancy totals; a single linked-bed count is not an aggregate anchor count. |
| JEI | Appropriate for recipes and more detailed usage. | It should supplement tooltips, not be necessary to discover the Clock/Compass/Book controls. Describe configured defaults honestly. |
| Advancements | Entering and anchoring are good milestones. | “Five More Minutes” rewards ignoring the very warning intended to discourage long sessions. Consider a positive building milestone instead; changing/removing it needs a migration decision. |
| Overstay warning | The theme fits, but current timing is unfair. | The warning says “wake now,” yet the effect becomes eligible at the same tick. Give a real grace interval before applying the penalty, and ensure the warning names/configures the actual effect. |
| Blacklist and schematics | Dimension-level interception is a useful baseline. | A caller may assume a rejected placement succeeded. Retest Create's cannon and direct paste with block entities. Two optional Create mixin classes exist in source but are not listed in the active mixin configuration; do not assume those hooks are running. |
| Incremental purge | Loaded-chunk scope and a work budget are good choices. | `/purge force` still loops synchronously until done and can stall a large server. Prefer a bounded queued “run now” operation; profile capability calls and stop/restart behavior. |
| Recovery history | Useful to admins investigating a failure. | Normal wakes can evict interesting failures from a short history. Record transaction outcome and cause, with a readable timestamp; retain failures separately if needed. |
| Doctor | A good diagnostic entry point. | “Mod detected” is not “integration healthy.” Report adapter verification, snapshot stage, invalid live links, and which checks were skipped because chunks were unloaded. |
| Accessibility | Reduced particles and text feedback are good foundations. | Avoid relying on color alone. Check long names, large GUI scale, controller/alternate bindings, and action-bar priority during a host countdown. Use translated preset names. |

## Command and configuration assessment

The current admin surface covers awaken, recovery restore/rollback/status/history, sessions, anchors, ownership, permissions, invitations, host override, inventory clear, configuration display, audit, purge, doctor, time, cleanup, and block/mob lists. This is a useful operational set, but consistency is more valuable now than adding commands.

`moblist add` allows a mob while `blocklist add` blocks an item. The list output partly explains it, but the opposing meanings invite mistakes. Add explicit allow/deny aliases and retain existing commands for compatibility. Destructive inventory clear and recovery replacement should explain the affected player/scope and preserve rollback. Offline recovery/history lookup currently uses online-player arguments; that is limiting for incident response.

The configuration contains legacy clock step, global cooldown, and reset settings whose comments/outputs are no longer aligned with personal-only controls. Do not silently repurpose old values. Mark obsolete values clearly, document replacements, and test loading old serverconfig files. Keep player-facing tooltips short and show actual configured limits in server-provided information.

## Maintainability and packaging

`ReverieEvents` contains entry, persistence, travel, mobs, UI feedback, ownership, safety, and lighting. That makes it easy for one subsystem to keep running after another changes dimension—the timed-wake purge bug is an example. Extract transition handling first, then lighting/bookmarks and cage management, after behavioral tests are in place. A wholesale rewrite without tests would increase risk.

New saved-data classes previously compressed entire methods onto one line and swallowed malformed input broadly. Bookmarks are now readable and isolate bad scope records. Apply the same approach gradually to imprint/history code. Preserve previous data versions and keep migration fixtures.

The repository has a CI build workflow, issue templates, contribution guidance, and security reporting instructions. The new regression task is part of `check`, so the existing CI build will run it. The ordinary test task still has no game tests; the separate regression task tests saved data only.

Other cleanup candidates: unused biome-tint payload/data code after the fixed grass-color decision, dormant global Clock controller state, unused Create mixin source, and scaffold comments/placeholder homepage metadata. The README currently mentions `0.2.0-beta.1` while the build version is `0.1.3`; reconcile that before publishing. Avoid claiming “without changing your survival save”—the mod stores its data in that save.

Do not advertise guaranteed compatibility with arbitrary modpacks. Publish the exact tested Minecraft, NeoForge, Create, Curios, Accessories, Jade, and JEI versions and distinguish “optional” from “verified.” Preserve the user's artwork and use the existing project art for packaging.

## Recommended order

1. Fix dimension-aware ownership and transition transaction handling, with migration and interruption tests.
2. Resolve blocked-template behavior and verify equipment restoration using the actual supported integrations.
3. Complete the dedicated-server acceptance matrix and test an existing 0.1.2 world.
4. Resolve overstay grace, imprint overwrite/reset, and access/host consistency.
5. Reconcile release documentation and remove obsolete controls from descriptions.
6. Ship the tested building loop. Develop the destination UI for 0.1.4 afterward.

## Technical references

The review uses the project's actual implementation as its primary evidence. NeoForge's version-specific references support the testing and persistence recommendations: [Saved Data](https://docs.neoforged.net/docs/1.21.1/datastorage/saveddata/), [Events](https://docs.neoforged.net/docs/1.21.1/concepts/events/), and [Game Tests](https://docs.neoforged.net/docs/1.21.1/misc/gametest/). Marking SavedData dirty is a persistence mechanism, not an inventory transaction guarantee; world-level behavior should be exercised in a running game-test/server environment.
