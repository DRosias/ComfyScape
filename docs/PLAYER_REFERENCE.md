# Player reference

## Useful commands

- `::commands [page]` and `::commandsearch <term> [--chat]` - browse available commands.
- `::players` - online players; `::where <username>` - their general area; `::stats [player]` - statistics book.
- `::bank` - open your bank; `::shop` - open the credit shop; `::quests` - implementation list; `::rules` - server rules; `::reply` - reply to your last DM; `::xface` - close an interface/dialogue.
- `::teleport [destination]` or `::tp [destination]` - teleport-network menu or a destination by name.
- `::spellbook normal|ancients|lunar` - switch only to a spellbook you have already unlocked.
- `::resetpassword` - change your own password using masked prompts.

Player commands depend on `world.player_commands` (currently enabled in the tracked template). If a command is unavailable, use `::commands` or ask staff rather than trying admin commands.

## Hans at home: setup and adult-check options

Talk to Hans at home to choose XP and server options. Your selection persists.

- XP rates: 1x, 10x, or 25x; **50x requires Hans adult verification**.
- **Requires Hans adult verification:** enable access to `::autopickup` and `::autobankdrops`, then toggle either command as desired.
- **Requires Hans adult verification:** enable teleport-unlock bypass, which makes all travel-network destinations available.

Hans adult verification is a three-question in-game check; a failed attempt has a 30-second retry delay.

## Travel and drops

- The home travel portal and `::teleport`/`::tp` use the unlocked travel network. Reach a hub to discover it; current hubs are Al Kharid, Draynor, Edgeville, Falador, Grand Exchange, Lumbridge, Port Sarim, Varrock, and Wizards' Tower.
- Network travel has a 30-second cooldown. The bypass above removes discovery requirements, not that cooldown.
- **Requires Hans adult verification and Hans enabling auto-drop command access:** `::autopickup` routes eligible NPC drops to inventory when space exists; `::autobankdrops` sends eligible drops to bank (unnoted), but stackables already in inventory stay there. If banking fails, it falls back to pickup only when auto-pickup is also on.
- Auto-pickup/auto-bank are disabled in the Wilderness and when the world PvP setting is on; unrouteable drops still behave normally.

## Grand Exchange and community conveniences

- Use GE booths or clerks to exchange, collect, view history, and use item sets. The server can provide configured bot sell stock; prices are based on the server's bot-price logic. Ironman restrictions are enforced by the GE handler.
- New accounts receive 2,000 credits under the current production rules; `::shop` opens the credit shop.
- The server creates a default Global clan service account when that option is enabled. Its exact in-game availability/settings are operator controlled.
