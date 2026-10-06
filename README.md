# SecureGuard

SecureGuard is a server-side Fabric administration and movement-monitoring mod. Players do not need the mod installed on their clients.

## Requirements and installation

- Minecraft **26.2** (the project platform target; not 26.1.2)
- Fabric Loader **0.19.3**
- Fabric API **0.158.0+26.2**
- Java **25**
- A dedicated Fabric server

Build the project with Kodari's **Compile** button, then install the resulting `secureguard` jar and Fabric API in the dedicated server's `mods` directory. Start the server once; SecureGuard creates its files under `config/secureguard/`. Back up that directory before upgrades.

## Initial access

The server console is always authorized. With a target online, run:

```text
sg admin add PlayerName
```

An offline target can be added by UUID instead:

```text
sg admin add 00000000-0000-0000-0000-000000000000
```

UUIDs, not names, are the stored authorization identifiers. Admin and whitelist add/remove actions are written to `audit.log`.

## Commands and visibility

Both `/sg` and `/secureguard` are registered.

| Command | Purpose |
|---|---|
| `help`, `info`, `reload` | Help, status, reload config |
| `whitelist add/remove <player-or-uuid>`, `whitelist list` | Manage full-access trusted UUIDs |
| `admin add/remove <player-or-uuid>`, `admin list` | Manage visible server admins |
| `anticheat on/off/status` | Toggle or inspect movement checks |
| `alert on/off/status` | Toggle the caller's alerts; console has an independent in-memory toggle |
| `ban <player> [reason]`, `ban-ip <player> [reason]` | Dispatch vanilla server ban commands |
| `unban <player>`, `unban-ip <ip>` | Dispatch vanilla pardon commands |
| `tp <player> [target]` | Teleport with the vanilla teleport command; one-argument form requires a player sender |
| `op <player>`, `deop <player>` | Dispatch vanilla operator commands |
| `restart` | Reports that restart is unsupported by this server environment |
| `stop` | Dispatches the vanilla graceful server stop command |
| `auth bypass <player>` | Reports authentication is disabled; SecureGuard has no password system |
| `debug <player>`, `debug off <player>` | Toggle per-player movement diagnostics |

Console, UUID admins, and whitelisted UUIDs have full command execution access. Admins see the complete command tree. Console and whitelist users have hidden management branches; a generic fallback accepts direct command entry without publishing those names. `/sg help` filters entries by the sender. Visibility is separate from authorization and is controlled by `commandVisibility` in `config.json`.

Normal players are not authorized. The root command is permission-filtered server-side; permissions do not depend on client state or operator status.

## Anti-cheat behavior

The server observes movement state and vanilla movement packets. Checks cover speed, prolonged hovering, high falls without observed damage, collision intersections, response to observed velocity changes, excessive movement-packet rates, and unusually large steps. Join, teleport, world-change, velocity, ping, and low-TPS grace/tolerance reduce false positives. Player state and packet windows are bounded and removed on disconnect.

These are conservative heuristic checks, not a deterministic vanilla-physics simulator or a replacement for a mature dedicated anti-cheat. Tune `checks` for the server's mechanics and review alerts before enabling punishments. Packet-rate checks are deliberately tolerant of ping and lag. `punishments.enabled` defaults to `false`; when enabled, per-check commands in `punishments.json` are dispatched only after the configured VL threshold and not below the configured low-TPS threshold. Setbacks are separately controlled in `config.json` and have a cooldown.

## Data files

- `config.json`: master enable, debug, visibility, alerts, per-check thresholds/decay/buffer, TPS tolerance, and setback settings.
- `admins.json`: `{"admins": ["UUID"]}`.
- `whitelist.json`: `{"whitelist": ["UUID"]}`.
- `alerts.json`: per-player UUID-to-boolean personal alert preferences.
- `punishments.json`: global punishment toggle and per-check vanilla command templates; `%player%` and `%uuid%` are substituted.
- `audit.log`: append-only JSON-lines records for administrative and anti-cheat punishment dispatches.

Missing or malformed fields use safe defaults. Punishments remain off by default. `reload` re-reads config and punishment settings without registering duplicate listeners or commands.

## Troubleshooting and updates

- Confirm the server is running Minecraft 26.2, Fabric Loader 0.19.3, Fabric API, and Java 25.
- If no one can run SecureGuard commands, use the server console to add a UUID with `sg admin add <uuid>`.
- If alerts are too frequent, raise alert thresholds or review ping/TPS tolerance before changing punishment thresholds.
- Back up `config/secureguard/` before replacing the jar. Keep it when updating; new missing fields receive defaults.
- Restart the server after installing or replacing the mod jar. `sg restart` intentionally does not kill or emulate a JVM restart.