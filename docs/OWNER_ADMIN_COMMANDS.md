# Owner/admin commands

Admins have all standard and moderator commands. Use `::commands [page]` or `::commandsearch <term> [--chat]` in game for the registered command list.

## Daily operations

- `::update [seconds]` / `::cancelupdate` - start or cancel the update countdown.
- `::announce <text>` - send a news announcement.
- `::players`, `::where <username>`, `::stats [player]` - presence and player information.
- `::giveitem <username> <item-id> [amount]` - deliver an item (banks it if their inventory is full).
- `::removeitem <inv|bank|equip> <username> <item-id> [amount]` and `::removeitemall <username> <item-id>` - remove player items, including noted variants for the latter.
- `::modcr <username> <amount>` - add/remove credits; `::csvmodcr <filename>` processes `username,amount` rows from a file relative to `data/`.
- `::resetpasswordother <username>` - owner password reset through masked prompts.

## Staff and moderation

- `::setrole <username> <player|mod|owner>` - set account role. In production, the target must already be in the corresponding configured allowlist.
- `::ban <username> <Nd|Ns|Nm|Nh>`, `::ipban <IP> <duration>`, `::mute <username> <duration>`, `::jail <seconds> <username>`, `::kick <username>`.
- `::recoveryrequests` then `::recoverpassword <username>` - only after out-of-game identity verification; temporary password is shown once.
- `::teleto <username>`, `::teletome <username>`, `::sendhome <username>`/`::unstuck <username>` - player assistance.

## Economy, progression, and world controls

- `::gerestock` - top up configured bot offers to targets in `data/eco/autostock.json`; it does not exceed those targets. Prefer this over `::addbotoffer`.
- `::addbotoffer <item-id> <amount>`, `::bange <item-id>`, `::allowge <item-id>` - GE inventory/trading controls; owner-only because they affect the economy.
- `::max [player]`, `::noobme [player]`, `::setlevel <skill> <level> [player]`, `::addxp <skill|id> <xp> [player]`, `::setpestpoints <points> [player]`, `::setslayerpoints <amount>`, `::setslayertask <npc-id> [amount]`, `::setqueststage <quest-index> <stage>`, `::allquest` - testing/recovery tools that change progression.
- `::home`, `::to <destination>`, `::tele <x> <y> <z>`/`<jagcoord>` - owner teleport tools.

## Keep owner-only / use sparingly

- `::shutdown` terminates the process immediately. Prefer console `stop` after an announced update.
- `::permadeath [player]` wipes a player save.
- `::empty`, `::emptybank`, `::fillbank`, `::item`, `::items`, `::npc`, `::object`, `::objectgrid`, `::1hit`, `::god`, `::invis`, and `::charge` can alter saves, gameplay, or world state.
- `::setattribute`, `::setvarp`, `::setvarbit`, `::setvarc`, `::cs2`, config/interface/camera commands, and other developer commands can corrupt or desynchronize a player. Use only for deliberate diagnosis with a backup.
