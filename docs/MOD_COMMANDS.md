# Moderator reference

Use this account for support and moderation, not normal progression. Moderator rights do **not** include owner/economy/developer powers.

## Support and rescue

- `::teleto <username>` - go to a player.
- `::teletome <username>` - bring a player to you.
- `::sendhome <username>` or `::unstuck <username>` - send a stuck player to server home.
- `::kick <username>` - disconnect a player. A moderator cannot kick the owner.
- `::where <username>`, `::players`, `::stats [player]` - locate/check players.

## Password recovery

1. Confirm the player's identity outside the game.
2. Run `::recoveryrequests`.
3. Run `::recoverpassword <username>` only for an active request.
4. Share the one-time temporary password privately; tell the player to run `::resetpassword` immediately.

Mods cannot recover another moderator or owner, and cannot use the owner's `::resetpasswordother`.

## Grand Exchange stock

- `::gerestock` tops up only the configured GE bot offers in `data/eco/autostock.json`, never above each target. Use it when configured stock has been bought down.
- Do not use an alternate/player account to create stock, trade with yourself, or test the economy. Escalate price, blacklist, and custom-offer changes to the owner.

## Boundaries

- No `::ban`, `::mute`, `::jail`, `::ipban`, `::setrole`, item/credit grants, XP/quest edits, GE blacklisting, or server shutdown.
- Do not use mod teleports or kicks for ordinary gameplay, retaliation, convenience transport, or player coercion.
- Record the reason and affected player for every rescue, kick, recovery, and GE restock according to the community's external moderation process (the repository has no staff-log procedure).
