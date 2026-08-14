# Private online production checklist

Run `run-server.bat` from this directory. It is the only normal launcher needed: it creates the ignored local configuration files when necessary, installs a native MariaDB Windows service on first use, starts it, waits for the local database, then builds and runs the game server.

## First launch

1. Run `run-server.bat`. Approve the Windows administrator prompt for the one-time MariaDB installation. MariaDB is installed as the automatic `ComfyScapeMariaDB` service and remains running after the game exits.
2. Wait for the game server to finish starting. Before forwarding TCP `43595` or allowing remote friends to join, log in locally as `igneusowner` with the initial password `changeme`.
3. Enter `::resetpassword`, supply `changeme`, then choose and confirm a unique new 5-20-character password. Passwords are case-sensitive and may use letters, digits, and the symbols `!@#$%^&*._+-`; spaces are not supported. The password is stored as a BCrypt hash.
4. Log out and back in to verify the new owner password. Use `::setrole modnemo mod` when you are ready to grant moderator tools to `modnemo`.
5. Forward only TCP `43595` for remote friends.

The bootstrap password is passed to the first Java process only and is not written to `mysql.env`, `config/default.conf`, source control, or logs. It remains unsafe until you rotate it, so do not expose the game port beforehand.

## Local files and database access

- `mysql.env` is created once with separate, generated application and root MariaDB passwords. Keep it private and back it up securely; the launcher never overwrites it.
- `config/default.conf` is created once from the tracked production template. It enables production mode, gives new accounts 2,000 credits, and authorizes only `igneusowner` as owner and `modnemo` as moderator.
- The values in `config/default.conf` intentionally remain placeholders. `run-server.bat` loads the actual ignored credentials from `mysql.env`, and the server gives those environment values precedence.
- MariaDB is installed as the automatic Windows service `ComfyScapeMariaDB`. The service persists its own data and stays running after the game process exits.
- MariaDB binds only to `127.0.0.1:3306`. It is reachable from this PC only, not from friends or the public internet.
- The application account has privileges only on `global.*`. Use the root credential only for backups, restores, migrations, or recovery. Everyday owner and moderator actions are performed in game.

To change the application or root database passwords later, stop the game server first. Update the password inside MariaDB using the MariaDB command-line client, then update the matching value in `mysql.env`. Changing only the file will lock the game server out. Do not put either database password in `config/default.conf`.

## Network and recovery

- Forward only the game port, TCP `43595`.
- Do not expose MariaDB `3306`, management `5555`, WebSocket `53595`, Remote Desktop, file sharing, or other host administration services publicly.
- Keep WebSocket and management networking disabled unless deliberately secured.
- If client/server RSA private material is ever shared outside the trusted group, rotate both client and server keys and rebuild the friend distribution.
- For database recovery, stop the game server and the `ComfyScapeMariaDB` service first. Do not remove the MariaDB data directory unless you intentionally want to discard all player and account data.

### Player password recovery

The maintained client redirects its legacy password-reset link to the game server itself. This uses the existing TCP `43595` listener and does not create a web server, admin panel, or additional public port.

1. The player enters their username on the login screen, clicks the password-reset link, and then contacts a mod or the owner IRL.
2. The server stores only the normalized username and request time. It never stores a submitted password, password hash, or requester IP in the recovery queue. The client always shows the same response so it does not reveal whether an account exists.
3. Only one request is kept per username. Repeated clicks during the five-minute cooldown do nothing; a click after five minutes replaces the existing request time instead of adding a duplicate.
4. On login, mods and the owner are told when requests are pending. Use `::recoveryrequests` to list them.
5. Verify the player's identity IRL, then use `::recoverpassword username`. The server generates a temporary password and shows it once to that staff member. Share it privately.
6. The player logs in with the temporary password and immediately runs `::resetpassword` to choose and confirm their own password. They receive a reminder on every login until they do so.

Mods can complete requests for normal players only. They cannot reset another mod or the owner. The owner may complete any pending request. The broad `::setpasswordother` command remains owner-only, and `::recoverpassword` refuses accounts without a pending request.

### Locked-out owner recovery

If `igneusowner` cannot log in, stop the game server and run `recover-owner-password.bat` from this directory. The script:

- refuses to proceed while TCP `43595` is active;
- starts the local `ComfyScapeMariaDB` service if needed;
- reads the new password twice using hidden console input;
- verifies that `igneusowner` is the configured owner; and
- updates only that account's BCrypt password.

The password is never accepted as a command-line argument or printed. After recovery, start the server normally with `run-server.bat` and verify the new owner login before exposing the game port.
