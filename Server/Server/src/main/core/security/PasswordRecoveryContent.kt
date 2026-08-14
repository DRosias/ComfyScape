package core.security

import core.api.Commands
import core.api.LoginListener
import core.api.sendMessage
import core.game.node.entity.player.Player
import core.game.node.entity.player.info.Rights
import core.game.system.command.Privilege
import core.game.world.GameWorld
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PasswordRecoveryContent : Commands, LoginListener {
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
        .withZone(ZoneId.systemDefault())

    override fun defineCommands() {
        define(
            "recoveryrequests",
            Privilege.MODERATOR,
            "::recoveryrequests",
            "Lists pending password-recovery requests."
        ) { player, _ -> listRequests(player) }

        define(
            "recoverpassword",
            Privilege.MODERATOR,
            "::recoverpassword <lt>username<gt>",
            "Issues a temporary password after IRL identity verification."
        ) { player, args -> recoverPassword(player, args) }
    }

    override fun login(player: Player) {
        if (player.rights != Rights.REGULAR_PLAYER) {
            val count = PasswordRecoveryRequests.pending().size
            if (count > 0) {
                sendMessage(player, "Password recovery: $count pending request${if (count == 1) "" else "s"}. Use ::recoveryrequests.")
            }
        }
        if (PasswordRecoveryRequests.needsPersonalPassword(player.username)) {
            sendMessage(player, "You are using a temporary password. Use ::resetpassword now to choose your own.")
        }
    }

    private fun listRequests(player: Player) {
        val pending = PasswordRecoveryRequests.pending()
        if (pending.isEmpty()) {
            notify(player, "There are no pending password-recovery requests.")
            return
        }

        notify(player, "Pending password-recovery requests (${pending.size}):")
        pending.take(20).forEach {
            notify(player, "${it.username} - ${timestampFormat.format(Instant.ofEpochMilli(it.requestedAt))}")
        }
        if (pending.size > 20) notify(player, "${pending.size - 20} more request(s) are pending.")
        notify(player, "Verify the player IRL, then use ::recoverpassword username")
    }

    private fun recoverPassword(player: Player, args: Array<String>) {
        if (args.size != 2) {
            notify(player, "Usage: ::recoverpassword username")
            return
        }

        val username = RolePolicy.normalize(args[1])
        if (!PasswordRecoveryRequests.isValidUsername(username) || !PasswordRecoveryRequests.hasPending(username)) {
            notify(player, "No pending recovery request exists for that username.")
            return
        }
        if (!GameWorld.accountStorage.checkUsernameTaken(username)) {
            notify(player, "No pending recovery request exists for that username.")
            return
        }

        val target = GameWorld.accountStorage.getAccountInfo(username)
        if (!PasswordRecoveryAuthorization.canComplete(player.rights, target.rights)) {
            notify(player, "Moderators cannot reset another staff account.")
            return
        }

        val temporaryPassword = PasswordPolicy.generateTemporaryPassword()
        if (!PasswordRecoveryRequests.completeWithTemporaryPassword(username) {
                GameWorld.authenticator.updatePassword(username, temporaryPassword)
            }) {
            notify(player, "The request was already completed. Do not share the generated password.")
            return
        }

        notify(player, "Temporary password for $username: $temporaryPassword")
        notify(player, "This password is shown once. Share it privately, then have the player use ::resetpassword.")
    }
}
