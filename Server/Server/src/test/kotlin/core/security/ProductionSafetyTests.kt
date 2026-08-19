package core.security

import core.ServerConstants
import core.auth.DevelopmentAuthenticator
import core.storage.InMemoryStorageProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProductionSafetyTests {
    @AfterEach
    fun reset() {
        ServerConstants.OWNER_ACCOUNTS = emptySet()
        ServerConstants.MODERATOR_ACCOUNTS = emptySet()
    }

    @Test
    fun `database endpoint must be private`() {
        assertTrue(ProductionSafety.isPrivateDatabaseHost("127.0.0.1"))
        assertTrue(ProductionSafety.isPrivateDatabaseHost("10.20.30.40"))
        assertFalse(ProductionSafety.isPrivateDatabaseHost("8.8.8.8"))
        assertFalse(ProductionSafety.isPrivateDatabaseHost(""))
    }

    @Test
    fun `bootstrap creates only the single missing allowlisted owner`() {
        ServerConstants.OWNER_ACCOUNTS = setOf("igneusowner")
        val storage = InMemoryStorageProvider()
        val authenticator = DevelopmentAuthenticator().also { it.configureFor(storage) }

        ProductionSafety.bootstrapConfiguredOwner(storage, authenticator, "changeme")

        assertTrue(storage.checkUsernameTaken("igneusowner"))
        assertEquals(2, storage.getAccountInfo("igneusowner").rights)
    }

    @Test
    fun `bootstrap is skipped without an explicit password`() {
        ServerConstants.OWNER_ACCOUNTS = setOf("igneusowner")
        val storage = InMemoryStorageProvider()
        val authenticator = DevelopmentAuthenticator().also { it.configureFor(storage) }

        ProductionSafety.bootstrapConfiguredOwner(storage, authenticator, null)

        assertFalse(storage.checkUsernameTaken("igneusowner"))
    }
}
