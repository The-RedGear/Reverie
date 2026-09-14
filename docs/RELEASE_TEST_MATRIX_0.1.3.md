# Reverie 0.1.3 acceptance tests

Status: manual scenarios below have not been executed during the source audit. Use a disposable copy of an existing world and a fresh world. Record exact mod versions and retain logs. Do not use a production world for interruption or rollback tests.

Automated baseline: `gradlew regressionTest` runs 58 assertions covering bookmark persistence, empty reads, independent selections, limits, literal Bed names, deletion, anchor coverage boundaries, shared-bed release, and mob-rule persistence. `gradlew build` includes these checks. This does not test rendering or a real player inventory.

| Area | Scenario | Pass condition |
|---|---|---|
| Installation | Start a dedicated server with only required dependencies; connect two matching clients | No client-only class-loading or missing optional-mod error |
| Upgrade | Load a copied 0.1.2 world with anchors, existing inventories, and older config | Existing data preserved; names optional; usable exit; doctor explains any issue |
| Ownership | Put beds at identical X/Y/Z in both dimensions, with different owners; break/reclaim one | Other dimension's ownership is unchanged; currently an open blocker |
| Crafting | Craft normally and shift-craft; break head/foot by hand and axe; pick up with another player | Exactly the intended drop count; owner retained; guest cannot bypass access |
| Arrival | Move an anchor vertically and obstruct its surrounding area | Arrival is at anchor height with safe body clearance |
| Normal waking | Enter carrying named, enchanted, damaged, and container items; armor/offhand; XP; effects | Original items, counts, components, mode and intended effects restored |
| Timed waking | Carry a Survival item banned only in Reverie; trigger maximum stay on a purge tick | Restored Survival item is never purged |
| Interrupted entry | Stop/crash the disposable server at each inventory-transition stage | Recoverable, unambiguous snapshot; no duplicate Survival items |
| Interrupted waking | Repeat interruption before and after restoration and snapshot closure | Exactly one authoritative inventory; repeat recovery cannot duplicate |
| Optional equipment | Repeat with Curios, Accessories, both where supported, cosmetic slots and effect-bearing items | Items visible; effects correspond to equipped items; no ghost modifiers |
| Default inventory | No saved kit, saved empty kit, existing anchor inventory, guest at anchored bed | Compass only when no applicable save; no template leaks between inventory types |
| Imprint | Save with Book, modify kit, overwrite, restart, re-enter temporary and anchored beds | Saved template behavior matches instructions; Book exclusion clear |
| Restrictions | Save restricted equipment, blacklist after saving, nested storage, direct equip, import schematic | No escape of restricted functionality; unresolved template handling documented |
| Bookmarks | Two project scopes with identical names, name Bed, deletion, restart, 32-entry limit | Selection remains independent and predictable |
| Travel | Remove floor, obstruct ceiling, add lava/fire, unload target, target beyond border | Travel rejected with correct explanation; no void/suffocation teleport |
| Lighting | Two clients together: one cycles Clock and resets with Shift; logout and change worlds | Only that player's preview changes; reset and rejoin behave consistently |
| Lighting clarity | Try spawners under personal midnight while server lighting differs | Tooltip explains actual simulation; preview never promises to change spawning |
| Guests | Owner leaves/logs out; rejoins at 59 seconds; bed replaced; host override/public/invited access | Countdown correct and cannot restart indefinitely; access policy consistent |
| Missing bed | Break, obstruct, unload, replace Overworld bed and obstruct respawn fallback | No forced chunk loading from inspection; safe documented exit path |
| Anchors | Named active/idle bed viewed from both dimensions; corner overlap and expired confirmation | Correct name/destination; confirmation applies only to intended bed |
| Cage | Each charge 0–4; discharge; reload; overlap; remove via schematic/explosion/command | Visual state, stored range and capacity agree; no invisible stale cage |
| Mobs | Warden/Phantom, excluded bosses, cap, out-of-range, hand vs sword/axe, farm damage | Clear restrictions and consistent loot; documented simulation limits |
| Mob rules | Allow Dragon, deny Warden, restart copied test server | Admin choices retained |
| Schematics | Create cannon and direct paste with allowed and denied block entities | No crash; valid blocks survive; forbidden placements give bounded feedback |
| Purge | Large loaded region; unload mid-run; stop/restart; change list; force | No unexpected chunk loading; acceptable tick time; no stale next-world state |
| UI | No Jade/JEI, either alone, both; long names; large GUI scale | Essential controls discoverable; no raw keys, misleading defaults, clipped critical warnings |
| Overstay | Wake immediately on first warning and just after intended grace | Warning offers an actual opportunity to avoid penalty; currently unresolved |
| Recovery history | Admin restore/rollback, self-recovery, automatic repair, many normal wakes | Critical failures remain identifiable; outcome and actor clear |

Release gate: all inventory, ownership, travel and guest scenarios pass on the dedicated server; no unresolved duplication/loss reproduction; upgrade results documented; UI and performance checked with the advertised pack stack.
