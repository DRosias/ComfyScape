package core.security

import core.ServerConstants
import core.ServerStore
import core.game.node.entity.player.info.Rights

object PasswordRecoveryAuthorization {
    fun canComplete(callerRights: Rights, targetRights: Int): Boolean = when (callerRights) {
        Rights.ADMINISTRATOR -> targetRights in 0..2
        Rights.PLAYER_MODERATOR -> targetRights == Rights.REGULAR_PLAYER.toInteger()
        Rights.REGULAR_PLAYER -> false
    }
}

object PasswordRecoveryRequests {
    const val HANDSHAKE_OPCODE = 187
    const val MAX_USERNAME_LENGTH = 12
    const val COOLDOWN_MILLIS = 5 * 60 * 1000L

    private const val requestsArchiveName = "password-recovery-requests"
    private const val passwordChangeArchiveName = "password-recovery-password-change"

    data class PendingRequest(val username: String, val requestedAt: Long)

    @JvmStatic
    @Synchronized
    fun submitKnownAccount(username: String): Boolean = submitKnownAccountAt(username, System.currentTimeMillis())

    @Synchronized
    internal fun submitKnownAccountAt(username: String, now: Long): Boolean {
        val normalized = RolePolicy.normalize(username)
        if (!isValidUsername(normalized)) return false

        val requests = ServerStore.getArchive(requestsArchiveName)
        val previous = (requests[normalized] as? Number)?.toLong()
        if (previous != null && now - previous < COOLDOWN_MILLIS) return false

        requests[normalized] = now
        persist(requestsArchiveName)
        return true
    }

    @Synchronized
    fun hasPending(username: String): Boolean =
        ServerStore.getArchive(requestsArchiveName).containsKey(RolePolicy.normalize(username))

    @Synchronized
    fun pending(): List<PendingRequest> = ServerStore.getArchive(requestsArchiveName)
        .mapNotNull { (key, value) ->
            val username = key as? String ?: return@mapNotNull null
            val timestamp = (value as? Number)?.toLong() ?: return@mapNotNull null
            if (isValidUsername(username)) PendingRequest(username, timestamp) else null
        }
        .sortedByDescending(PendingRequest::requestedAt)

    @Synchronized
    fun completeWithTemporaryPassword(username: String, updatePassword: () -> Unit): Boolean {
        val normalized = RolePolicy.normalize(username)
        val requests = ServerStore.getArchive(requestsArchiveName)
        if (!requests.containsKey(normalized)) return false

        updatePassword()
        requests.remove(normalized)
        ServerStore.getArchive(passwordChangeArchiveName)[normalized] = System.currentTimeMillis()
        persist(requestsArchiveName)
        persist(passwordChangeArchiveName)
        return true
    }

    @Synchronized
    fun needsPersonalPassword(username: String): Boolean =
        ServerStore.getArchive(passwordChangeArchiveName).containsKey(RolePolicy.normalize(username))

    @Synchronized
    fun personalPasswordChanged(username: String) {
        val normalized = RolePolicy.normalize(username)
        if (ServerStore.getArchive(passwordChangeArchiveName).remove(normalized) != null) {
            persist(passwordChangeArchiveName)
        }
    }

    internal fun isValidUsername(username: String): Boolean =
        username.length in 1..MAX_USERNAME_LENGTH && username.all { it in 'a'..'z' || it in '0'..'9' || it == '_' }

    @Synchronized
    internal fun clearForTests() {
        ServerStore.getArchive(requestsArchiveName).clear()
        ServerStore.getArchive(passwordChangeArchiveName).clear()
    }

    private fun persist(name: String) {
        if (ServerConstants.STORE_PATH != null) ServerStore.saveArchive(name)
    }
}
