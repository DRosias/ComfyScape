package core.security

import core.ServerConstants
import core.cache.misc.buffer.ByteBufferUtils
import core.net.packet.`in`.Login
import java.nio.ByteBuffer
import java.util.Base64

object EncryptedPasswordInput {
    const val REQUIRED_ATTRIBUTE = "encrypted-password-input-required"
    private const val PREFIX = "rsa:"

    fun decrypt(response: Any): String? {
        val encoded = (response as? String)?.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX) ?: return null
        return try {
            val packet = Base64.getDecoder().decode(encoded)
            val modulusLength = (ServerConstants.MODULUS.bitLength() + 7) / 8
            if (packet.size != modulusLength + 1 || (packet[0].toInt() and 0xFF) != modulusLength) return null

            val plaintext = Login.decryptRSABuffer(
                ByteBuffer.wrap(packet),
                ServerConstants.EXPONENT,
                ServerConstants.MODULUS
            )
            if (!plaintext.hasRemaining() || (plaintext.get().toInt() and 0xFF) != 10) return null
            val password = ByteBufferUtils.getString(plaintext)
            if (plaintext.hasRemaining()) return null
            password
        } catch (_: Exception) {
            null
        }
    }
}
