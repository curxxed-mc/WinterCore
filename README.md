# WinterCore

WinterCore is the core plugin behind a Minecraft server network. It keeps the everyday network features—staff tools, moderation, ranks, player commands, and cross-server communication—in one place instead of requiring multiple plugins.
My goal with WinterCore is to provide a good, customizable, free alternative to premium server cores like AquaCore.

The plugin is built around Spigot/Paper 1.8.8. It includes compatibility work for newer server versions, but that support is still experimental and has not been fully tested.

## Features

- Staff mode, vanish, freeze, invsee, reports, moderation history, alt checks, disguises, and maintenance controls
- Bans, mutes, warnings, and other moderation tools
- Private messages, replies, profiles, social links, tags, chat colours, ping, movement commands, item utilities, and more
- Redis-backed player presence, broadcasts, server switching, remote commands, configuration sync, and staff activity
- MongoDB-backed player and moderation data
- Ranks, permissions, tags, menus, chat filtering, scoreboards, tab lists, and name tags
- Optional PlaceholderAPI placeholders

## Requirements

- Java 8
- A Spigot-compatible server; 1.8.8 is the main target
- MongoDB for persistent player and moderation data
- Redis for network features
- PlaceholderAPI, if you want placeholder support

Before starting the server, configure MongoDB, Redis, and the server name in `plugins/WinterCore/config.yml`.

## Building

```powershell
.\gradlew.bat clean check shadowJar
```

```bash
./gradlew clean check shadowJar
```

The finished plugin JAR will be placed in `build/libs/`.
## Configuration

- `config.yml` — server, Redis, MongoDB, NameMC, and general settings
- `ranks.yml` and `permissions.yml` — ranks and permissions
- `tags.yml` and `menus.yml` — tag and menu content
- `messages.yml` and `chat-filter.yml` — messages and chat filtering

## License

WinterCore is available under the [Apache License 2.0](LICENSE).
