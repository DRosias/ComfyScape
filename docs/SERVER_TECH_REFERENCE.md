# Server technical reference

## Build and run

- Server Maven root: `Server/Server/` (Java 11; Maven Wrapper is included).
- Build from that directory: `./mvnw.cmd clean package -DskipTests` on Windows. The assembly artifact is `target/*-with-dependencies.jar`.
- Normal Windows operator launcher: `Server/run-server.bat`. It runs `setup-local.ps1`, ensures the local `ComfyScapeMariaDB` service is available, builds, copies the assembly to `Server/Server/server.jar`, and starts it with `config/default.conf`.
- Direct Java entry point: `core.Server`. With no argument it reads `Server/Server/worldprops/default.conf`; with an argument it uses that configuration file.
- Console commands discovered: `stop` (clean process exit/save), `update`, `restartworker`, and `help`/`commands`. Use `stop` for a normal shutdown. In-game `::shutdown` exits immediately and is not a normal restart procedure.
- The game TCP port is `43594 + world_id`; the current `world_id = "1"` makes it **43595**. WebSocket, when enabled, is `53594 + world_id` unless `server.websocket_port` overrides it.

## Important configuration and data

- Tracked template: `Server/Server/worldprops/default.conf`.
- Windows launcher's local runtime config: `Server/config/default.conf` (created by setup; do not commit credentials).
- Local secret environment: `Server/mysql.env`; environment database values override config (`COMFYSCAPE_DB_*`, with selected `MYSQL_*` fallbacks).
- Player saves: `paths.save_path` -> currently `Server/Server/data/players/`, one JSON file per player. Saves include inventory/banks/equipment, location, skills and XP multiplier, quests, settings, spellbook, attributes, and content-hook data.
- Account database: MariaDB/MySQL database `global`, table `members`; account records include username, BCrypt password hash, rights, credits, IP/last game IP, mute/ban times, social/clan data, play time, and online/join data. GE data uses SQLite under `paths.eco_data` (currently `data/eco`).
- GE stock catalog: `Server/Server/data/eco/autostock.json`; enabled by `world.autostock_ge` and topped up at startup.
- Logs and server store: `data/logs` and `data/serverstore` (paths configured in `[paths]`).

## Staff accounts and access

- New registrations are normalized and created as regular players; production authentication hashes passwords and checks account rights against allowlists.
- In production, staff requires **both** a rights value in `members` and a matching name in `[security] owner_accounts` or `moderator_accounts`. Startup rejects unauthorized elevated rows and configured owners that lack owner rights.
- First owner: configure exactly one valid owner name in `owner_accounts`; the launcher can bootstrap a missing owner through its process-only `COMFYSCAPE_BOOTSTRAP_OWNER_PASSWORD`. Log in locally, immediately use `::resetpassword`, then verify a fresh login before making the game port public. The bootstrap password itself should not be copied into documentation, config, or chat.
- Dedicated moderator: have the player account exist first; add its normalized name to `moderator_accounts` in the active config, restart, then an owner runs `::setrole <username> mod`. The current template authorizes `modnemo`; use the active local config as the authority.
- Owners use `::setrole <username> player|mod|owner`; it cannot grant a production role to a name absent from the allowlist. Do not alter `members.rights` directly as a routine workflow.
- Password handling: players use `::resetpassword`; owners can use masked `::resetpasswordother <username>`. Mods/owners can process an identity-verified pending recovery with `::recoveryrequests` and `::recoverpassword <username>`; a mod cannot recover another staff account.

## Operator security/manual checks

- Keep `server.use_auth`, `server.persist_accounts`, and `security.production_mode` true; production also requires `starting_credits = 2000`, non-debug/non-dev settings, and `world.i_want_to_cheat = false`.
- Keep MariaDB private. The launcher configures it for `127.0.0.1:3306`; forward only TCP **43595**. Do not expose database, management, Remote Desktop, file sharing, or WebSocket ports.
- Leave WebSocket disabled unless deliberately needed. Production requires TLS, a keystore path, and `COMFYSCAPE_WEBSOCKET_KEYSTORE_PASSWORD` when it is enabled.
- Back up both the MariaDB data and `data/players/` before upgrades or manual recovery. The repository does not establish a backup schedule; this is an operator step.
- If the owner is locked out, stop the game process and use `Server/recover-owner-password.bat`; it verifies the configured owner and updates only that BCrypt password.
