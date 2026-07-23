# WinterCore

WinterCore is a Minecraft server core for a network running around the Spigot/Paper 1.8.8 ecosystem. It brings common staff tools, player-facing quality-of-life commands, rank/tag handling, moderation, and cross-server features into one plugin.

## What it offers

- Staff tooling: vanish, staff mode, freeze, invsee, reports, moderation history, alts, disguises, warnings, mutes, bans, and server-wide maintenance controls.
- Player commands: private messages and replies, profiles/social links, chat colours, tags, ping, movement and utility commands, enchantments, item repair, feed/heal, and more.
- Network features: Redis-backed player presence, broadcasts, server switches, remote commands, configuration sync, staff activity, and moderation packets.
- Data and presentation: MongoDB-backed player/moderation data, ranks, permissions, tags, score/tab/name-tag display, menus, chat filtering, and optional PlaceholderAPI placeholders.

The exact command set and permissions are defined in the plugin's configuration and command classes; this is a broad core rather than a drop-in replacement for a single-purpose plugin.

## Requirements

- Java 8
- A Spigot-compatible server, primarily targeting 1.8.8
- MongoDB for persistent player and moderation data
- Redis for network features
- PlaceholderAPI is optional

Configure MongoDB, Redis, the server name, and any webhook endpoint in `plugins/WinterCore/config.yml` before enabling the plugin. Do not commit real connection strings, passwords, or webhook URLs.

## Build

```powershell
.\gradlew.bat clean check shadowJar
```

The shaded plugin JAR is written to `build/libs/`.

## Current state

WinterCore is in an active revamp/cleanup phase. The current source compiles and the Gradle `check` task passes, but there are no automated tests yet. It is best treated as a network-specific core: configure and test it on a non-production server before rolling it out.

Recent cleanup removed tracked IDE metadata, tightened lifecycle handling around Redis/disguises, fixed reload-state and scheduled-task issues, and removed unused client-brand/protocol code.

## Configuration files

- `config.yml` — server, Redis, MongoDB, NameMC, webhook, and general settings
- `ranks.yml` / `permissions.yml` — rank and permission definitions
- `tags.yml` / `menus.yml` — tag and menu content
- `messages.yml` / `chat-filter.yml` — messages and chat filtering rules

## License

Licensed under the [Apache License 2.0](LICENSE).
