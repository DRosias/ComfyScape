package core.security

import core.ServerConstants
import core.auth.AuthProvider
import core.auth.UserAccountInfo
import core.game.world.GameWorld
import core.storage.AccountStorageProvider
import java.net.InetAddress
import java.security.SecureRandom
import java.util.Base64

object ProductionSafety {
    private val placeholderTokens = listOf("change_me", "changeme", "example", "password", "development")
    private const val bootstrapOwnerPasswordEnvironment = "COMFYSCAPE_BOOTSTRAP_OWNER_PASSWORD"
    internal const val REQUIRED_STARTING_CREDITS = 2000

    fun validateConfiguration() {
        if (!ServerConstants.PRODUCTION_MODE) return

        val errors = mutableListOf<String>()
        if (!ServerConstants.USE_AUTH) errors += "server.use_auth must be true"
        if (!ServerConstants.PERSIST_ACCOUNTS) errors += "server.persist_accounts must be true"
        if (!hasRequiredStartingCredits()) errors += "server.starting_credits must be $REQUIRED_STARTING_CREDITS"
        if (GameWorld.settings?.isBeta == true) errors += "world.debug must be false"
        if (GameWorld.settings?.isDevMode == true) errors += "world.dev must be false"
        if (ServerConstants.I_AM_A_CHEATER) errors += "world.i_want_to_cheat must be false"
        if (GameWorld.settings?.enabled_botting == true) errors += "world.enable_botting must be false"

        val user = ServerConstants.DATABASE_USER.orEmpty()
        val password = ServerConstants.DATABASE_PASS.orEmpty()
        if (user.isBlank() || user.equals("root", true) || placeholderTokens.any { user.lowercase().contains(it) }) {
            errors += "Use a dedicated non-root, non-placeholder database account"
        }
        if (password.isBlank() || placeholderTokens.any { password.lowercase().contains(it) }) {
            errors += "Set a strong, non-placeholder database password through the environment"
        }
        if (!isPrivateDatabaseHost(ServerConstants.DATABASE_ADDRESS.orEmpty())) {
            errors += "Database address must resolve only to loopback, link-local, or private addresses"
        }

        if (ServerConstants.WEBSOCKET_ENABLED) {
            if (!ServerConstants.WEBSOCKET_TLS_ENABLED) errors += "Production WebSocket service requires TLS"
            if (ServerConstants.WEBSOCKET_TLS_KEYSTORE_PATH.isBlank()) errors += "Production WebSocket service requires a keystore path"
            if (ServerConstants.WEBSOCKET_TLS_KEYSTORE_PASSWORD.isBlank()) errors += "Production WebSocket keystore password must come from the environment"
        }

        errors += RolePolicy.validateAllowlists(
            ServerConstants.OWNER_ACCOUNTS,
            ServerConstants.MODERATOR_ACCOUNTS,
            RolePolicy.reservedClanAccount()
        )
        require(errors.isEmpty()) { "Production security validation failed:\n - ${errors.joinToString("\n - ")}" }
    }

    internal fun hasRequiredStartingCredits(): Boolean =
        ServerConstants.STARTING_CREDITS == REQUIRED_STARTING_CREDITS

    fun prepareAndAudit(storage: AccountStorageProvider, authenticator: AuthProvider<*>) {
        if (!ServerConstants.PRODUCTION_MODE) return
        prepareDefaultClanServiceAccount(storage, authenticator)
        bootstrapConfiguredOwner(storage, authenticator, System.getenv(bootstrapOwnerPasswordEnvironment))

        val elevated = storage.getElevatedAccounts()
        val unauthorized = elevated.filterNot { RolePolicy.isAuthorized(it.username, it.rights) }
        val configuredOwners = elevated.filter { it.rights == 2 }.map { RolePolicy.normalize(it.username) }.toSet()
        val missingOwners = ServerConstants.OWNER_ACCOUNTS - configuredOwners
        val errors = mutableListOf<String>()
        if (unauthorized.isNotEmpty()) {
            errors += "Unauthorized or invalid elevated accounts: ${unauthorized.map { it.username }.sorted().joinToString()}"
        }
        if (missingOwners.isNotEmpty()) errors += "Allowlisted owners missing rights 2 in the database: ${missingOwners.sorted().joinToString()}"
        require(errors.isEmpty()) { "Staff-role audit failed:\n - ${errors.joinToString("\n - ")}" }
    }

    /**
     * Creates exactly one missing allowlisted owner for a brand-new local database.
     * The launcher supplies this password only through its child process environment.
     */
    internal fun bootstrapConfiguredOwner(
        storage: AccountStorageProvider,
        authenticator: AuthProvider<*>,
        bootstrapPassword: String?
    ) {
        val missingOwners = ServerConstants.OWNER_ACCOUNTS.filterNot(storage::checkUsernameTaken)
        if (missingOwners.isEmpty()) return
        if (bootstrapPassword.isNullOrBlank()) return
        require(missingOwners.size == 1) { "Bootstrap requires exactly one missing allowlisted owner" }
        require(bootstrapPassword.length in 5..20) { "Bootstrap owner password must be between 5 and 20 characters" }

        val owner = UserAccountInfo.createDefault().also {
            it.username = missingOwners.single()
            it.password = bootstrapPassword
            it.rights = 0
        }
        check(authenticator.createAccountWith(owner)) { "Unable to create the bootstrap owner account" }
        owner.rights = 2
        storage.update(owner)
    }

    private fun prepareDefaultClanServiceAccount(storage: AccountStorageProvider, authenticator: AuthProvider<*>) {
        if (GameWorld.settings?.enable_default_clan != true) return
        val username = RolePolicy.reservedClanAccount()
        val password = ByteArray(32).also(SecureRandom()::nextBytes).let(Base64.getUrlEncoder().withoutPadding()::encodeToString)
        val existing = storage.checkUsernameTaken(username)
        val account = if (existing) storage.getAccountInfo(username) else UserAccountInfo.createDefault().also { it.username = username }

        account.rights = 0
        account.credits = 0
        account.banEndTime = Long.MAX_VALUE
        account.online = false
        account.clanName = "Global"
        account.clanReqs = "-1,-1,7,7"
        if (existing) {
            authenticator.updatePassword(username, password)
            storage.update(account)
        } else {
            account.password = password
            check(authenticator.createAccountWith(account)) { "Unable to create the reserved default-clan service account" }
        }
    }

    internal fun isPrivateDatabaseHost(host: String): Boolean {
        if (host.isBlank()) return false
        return try {
            val addresses = InetAddress.getAllByName(host)
            addresses.isNotEmpty() && addresses.all {
                it.isLoopbackAddress || it.isLinkLocalAddress || it.isSiteLocalAddress
            }
        } catch (_: Exception) {
            false
        }
    }
}
