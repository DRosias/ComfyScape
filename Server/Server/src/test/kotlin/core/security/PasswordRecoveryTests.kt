package core.security

import core.ServerConstants
import core.game.node.entity.player.info.Rights
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PasswordRecoveryTests {
    @BeforeEach
    fun prepare() {
        ServerConstants.STORE_PATH = null
        PasswordRecoveryRequests.clearForTests()
    }

    @AfterEach
    fun cleanup() {
        PasswordRecoveryRequests.clearForTests()
        ServerConstants.STORE_PATH = null
    }

    @Test
    fun `one request per username respects the five minute cooldown`() {
        val first = 1_000_000L
        assertTrue(PasswordRecoveryRequests.submitKnownAccountAt("friend", first))
        assertFalse(PasswordRecoveryRequests.submitKnownAccountAt("friend", first + PasswordRecoveryRequests.COOLDOWN_MILLIS - 1))
        assertEquals(listOf(PasswordRecoveryRequests.PendingRequest("friend", first)), PasswordRecoveryRequests.pending())

        val replacement = first + PasswordRecoveryRequests.COOLDOWN_MILLIS
        assertTrue(PasswordRecoveryRequests.submitKnownAccountAt("friend", replacement))
        assertEquals(listOf(PasswordRecoveryRequests.PendingRequest("friend", replacement)), PasswordRecoveryRequests.pending())
    }

    @Test
    fun `completion clears the request and requires a personal password`() {
        PasswordRecoveryRequests.submitKnownAccountAt("friend", 1_000L)
        var updates = 0

        assertTrue(PasswordRecoveryRequests.completeWithTemporaryPassword("friend") { updates++ })
        assertEquals(1, updates)
        assertFalse(PasswordRecoveryRequests.hasPending("friend"))
        assertTrue(PasswordRecoveryRequests.needsPersonalPassword("friend"))

        PasswordRecoveryRequests.personalPasswordChanged("friend")
        assertFalse(PasswordRecoveryRequests.needsPersonalPassword("friend"))
        assertFalse(PasswordRecoveryRequests.completeWithTemporaryPassword("friend") { updates++ })
        assertEquals(1, updates)
    }

    @Test
    fun `password policy accepts at sign and rejects unsafe input`() {
        assertNull(PasswordPolicy.validationError("friend", "SecurePass@"))
        assertTrue(PasswordPolicy.validationError("friend", "has space")!!.contains("unsupported"))
        assertTrue(PasswordPolicy.validationError("friend", "friend")!!.contains("username"))

        val first = PasswordPolicy.generateTemporaryPassword()
        val second = PasswordPolicy.generateTemporaryPassword()
        assertEquals(12, first.length)
        assertNotEquals(first, second)
        assertNull(PasswordPolicy.validationError("friend", first))
    }

    @Test
    fun `mods reset normal players only while owner can reset staff`() {
        assertTrue(PasswordRecoveryAuthorization.canComplete(Rights.PLAYER_MODERATOR, 0))
        assertFalse(PasswordRecoveryAuthorization.canComplete(Rights.PLAYER_MODERATOR, 1))
        assertFalse(PasswordRecoveryAuthorization.canComplete(Rights.PLAYER_MODERATOR, 2))
        assertFalse(PasswordRecoveryAuthorization.canComplete(Rights.REGULAR_PLAYER, 0))
        assertTrue(PasswordRecoveryAuthorization.canComplete(Rights.ADMINISTRATOR, 0))
        assertTrue(PasswordRecoveryAuthorization.canComplete(Rights.ADMINISTRATOR, 1))
        assertTrue(PasswordRecoveryAuthorization.canComplete(Rights.ADMINISTRATOR, 2))
    }
}
