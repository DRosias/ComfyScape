package core.security

import core.ServerConstants
import core.auth.DevelopmentAuthenticator
import core.auth.UserAccountInfo
import core.storage.InMemoryStorageProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountDefaultTests {
    @Test
    fun `new account defaults and no-auth creation are normal players`() {
        ServerConstants.STARTING_CREDITS = 2000
        val storage = InMemoryStorageProvider()
        val authenticator = DevelopmentAuthenticator().also { it.configureFor(storage) }
        val account = UserAccountInfo.createDefault().also {
            it.username = "new_friend"
            it.password = "test-password"
            it.rights = 2
        }

        authenticator.createAccountWith(account)

        assertEquals(0, storage.getAccountInfo("new_friend").rights)
        assertEquals(2000, storage.getAccountInfo("new_friend").credits)
    }

    @Test
    fun `production credit policy requires 2000 starting credits`() {
        val originalCredits = ServerConstants.STARTING_CREDITS
        try {
            ServerConstants.STARTING_CREDITS = 2000
            assertEquals(true, ProductionSafety.hasRequiredStartingCredits())

            ServerConstants.STARTING_CREDITS = 1999
            assertEquals(false, ProductionSafety.hasRequiredStartingCredits())
        } finally {
            ServerConstants.STARTING_CREDITS = originalCredits
        }
    }
}
