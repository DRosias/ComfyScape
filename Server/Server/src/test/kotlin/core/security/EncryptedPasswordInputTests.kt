package core.security

import core.ServerConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.Base64

class EncryptedPasswordInputTests {
    @Test
    fun `decrypts a password encrypted with the account creation public key`() {
        assertEquals("Friend123!", EncryptedPasswordInput.decrypt(encrypt("Friend123!")))
    }

    @Test
    fun `rejects plaintext and malformed ciphertext`() {
        assertNull(EncryptedPasswordInput.decrypt("Friend123!"))
        assertNull(EncryptedPasswordInput.decrypt("rsa:not-base64"))
        assertNull(EncryptedPasswordInput.decrypt("rsa:" + Base64.getEncoder().encodeToString(byteArrayOf(1, 2))))
    }

    private fun encrypt(password: String): String {
        val plaintext = byteArrayOf(10) + password.toByteArray(Charsets.US_ASCII) + byteArrayOf(0)
        val ciphertext = BigInteger(plaintext).modPow(BigInteger("65537"), ServerConstants.MODULUS).toByteArray()
        val modulusLength = (ServerConstants.MODULUS.bitLength() + 7) / 8
        val padded = ByteArray(modulusLength)
        ciphertext.copyInto(
            padded,
            destinationOffset = (modulusLength - ciphertext.size).coerceAtLeast(0),
            startIndex = (ciphertext.size - modulusLength).coerceAtLeast(0)
        )
        val packet = byteArrayOf(modulusLength.toByte()) + padded
        return "rsa:" + Base64.getEncoder().encodeToString(packet)
    }
}
