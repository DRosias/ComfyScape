package core.security

import java.security.SecureRandom

object PasswordPolicy {
    const val MIN_LENGTH = 5
    const val MAX_LENGTH = 20

    private const val allowedCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*._+-"
    private const val temporaryCharacters = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"

    fun validationError(username: String, password: String): String? {
        if (password.length !in MIN_LENGTH..MAX_LENGTH) {
            return "Password must be between $MIN_LENGTH and $MAX_LENGTH characters."
        }
        if (password.any { it !in allowedCharacters }) {
            return "Password contains an unsupported character. Do not use spaces."
        }
        if (password.equals(RolePolicy.normalize(username), ignoreCase = true)) {
            return "Password cannot be the same as the username."
        }
        return null
    }

    fun generateTemporaryPassword(random: SecureRandom = SecureRandom(), length: Int = 12): String {
        require(length in MIN_LENGTH..MAX_LENGTH)
        return buildString(length) {
            repeat(length) {
                append(temporaryCharacters[random.nextInt(temporaryCharacters.length)])
            }
        }
    }
}
