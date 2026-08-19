package core.tools

import core.ServerConstants
import core.auth.ProductionAuthenticator
import core.game.system.config.ServerConfigParser
import core.security.PasswordPolicy
import core.security.ProductionSafety
import core.security.RolePolicy
import core.storage.SQLStorageProvider

object OwnerPasswordRecovery {
    private const val ownerUsername = "igneusowner"

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: OwnerPasswordRecovery <server-config>" }
        val console = System.console() ?: error("Run recover-owner-password.bat from a normal Command Prompt window.")

        ServerConfigParser.parse(args[0])
        ProductionSafety.validateConfiguration()
        require(ServerConstants.PRODUCTION_MODE) { "Owner recovery requires production mode." }
        require(ownerUsername in ServerConstants.OWNER_ACCOUNTS) { "$ownerUsername is not in the configured owner allowlist." }

        val storage = SQLStorageProvider()
        val authenticator = ProductionAuthenticator().also { it.configureFor(storage) }
        require(storage.checkUsernameTaken(ownerUsername)) { "The configured owner account does not exist." }
        val account = storage.getAccountInfo(ownerUsername)
        require(account.rights == 2 && RolePolicy.isAuthorized(account.username, account.rights)) {
            "The configured owner account does not have valid owner rights."
        }

        val firstCharacters = console.readPassword("New password for %s: ", ownerUsername)
            ?: error("Password entry was cancelled.")
        val first = String(firstCharacters)
        try {
            PasswordPolicy.validationError(ownerUsername, first)?.let { error(it) }
            val secondCharacters = console.readPassword("Confirm new password: ")
                ?: error("Password confirmation was cancelled.")
            val second = String(secondCharacters)
            try {
                require(first == second) { "Passwords did not match; no change was made." }
                authenticator.updatePassword(ownerUsername, first)
            } finally {
                secondCharacters.fill('\u0000')
            }
        } finally {
            firstCharacters.fill('\u0000')
        }

        println("Owner password updated successfully. The password was not printed or logged.")
    }
}
