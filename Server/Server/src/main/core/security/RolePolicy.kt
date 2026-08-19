package core.security

import core.ServerConstants

object RolePolicy {
    private val validUsername = Regex("[a-z0-9_]{1,12}")

    fun normalize(username: String): String = username.trim().lowercase().replace(' ', '_')

    fun reservedClanAccount(): String = ServerConstants.SERVER_NAME.lowercase()

    fun validateAllowlists(owners: Set<String>, moderators: Set<String>, reserved: String): List<String> {
        val errors = mutableListOf<String>()
        val invalid = (owners + moderators).filterNot(validUsername::matches)
        if (invalid.isNotEmpty()) errors += "Staff allowlists contain invalid usernames: ${invalid.sorted().joinToString()}"
        val overlap = owners.intersect(moderators)
        if (overlap.isNotEmpty()) errors += "Accounts cannot be both owner and moderator: ${overlap.sorted().joinToString()}"
        if (reserved in owners || reserved in moderators) errors += "Reserved clan service account '$reserved' cannot be staff"
        if (owners.isEmpty()) errors += "At least one owner account must be allowlisted"
        return errors
    }

    fun isAuthorized(username: String, rights: Int): Boolean {
        val normalized = normalize(username)
        return when (rights) {
            0 -> true
            1 -> normalized in ServerConstants.MODERATOR_ACCOUNTS
            2 -> normalized in ServerConstants.OWNER_ACCOUNTS
            else -> false
        }
    }

    fun canAssign(username: String, rights: Int): Boolean {
        val normalized = normalize(username)
        if (normalized in ServerConstants.OWNER_ACCOUNTS && rights != 2) return false

        return when (rights) {
            0 -> true
            1 -> !ServerConstants.PRODUCTION_MODE || normalized in ServerConstants.MODERATOR_ACCOUNTS
            2 -> !ServerConstants.PRODUCTION_MODE || normalized in ServerConstants.OWNER_ACCOUNTS
            else -> false
        }
    }

    fun isReserved(username: String): Boolean = normalize(username) == reservedClanAccount()
}
