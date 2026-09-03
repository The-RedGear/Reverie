# Contributing to Reverie

Thanks for helping improve Reverie.

## Reporting problems

Use the appropriate GitHub issue form and include:

- Minecraft and NeoForge versions
- Reverie version
- Relevant optional mods and their versions
- Whether the problem occurred on a dedicated server
- Steps that reproduce the problem
- The latest log or crash report when applicable

Inventory-loss reports should also state whether `/reverie recovery status <player>` finds a recovery snapshot. Never post a world download or player data publicly if it contains private server information.

## Code contributions

1. Open an issue before beginning a large behavioral change.
2. Build with Java 21 using `./gradlew build` or `gradlew.bat build`.
3. Keep optional integrations optional at runtime.
4. Preserve the waking-inventory escrow and recovery guarantees.
5. Test dimension transitions on both an integrated and dedicated server.

By submitting a contribution, you agree that the project owner may distribute it under Reverie's project license.
