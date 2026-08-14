package core.security

import core.ServerConstants
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RolePolicyTests {
    @AfterEach
    fun reset() {
        ServerConstants.OWNER_ACCOUNTS = emptySet()
        ServerConstants.MODERATOR_ACCOUNTS = emptySet()
        ServerConstants.PRODUCTION_MODE = false
    }

    @Test
    fun `allowlists reject overlap invalid names reserved account and missing owner`() {
        val errors = RolePolicy.validateAllowlists(
            setOf("owner", "bad-name", "comfyscape"),
            setOf("owner"),
            "comfyscape"
        )
        assertTrue(errors.any { it.contains("invalid usernames") })
        assertTrue(errors.any { it.contains("both owner and moderator") })
        assertTrue(errors.any { it.contains("Reserved clan service") })
        assertTrue(RolePolicy.validateAllowlists(emptySet(), emptySet(), "comfyscape").any { it.contains("At least one owner") })
    }

    @Test
    fun `database rights are capped by the configured role`() {
        ServerConstants.OWNER_ACCOUNTS = setOf("owner")
        ServerConstants.MODERATOR_ACCOUNTS = setOf("helper")
        assertTrue(RolePolicy.isAuthorized("Owner", 2))
        assertTrue(RolePolicy.isAuthorized("helper", 1))
        assertTrue(RolePolicy.isAuthorized("friend", 0))
        assertFalse(RolePolicy.isAuthorized("helper", 2))
        assertFalse(RolePolicy.isAuthorized("owner", 1))
        assertFalse(RolePolicy.isAuthorized("intruder", 2))
        assertFalse(RolePolicy.isAuthorized("owner", 99))
    }

    @Test
    fun `configured owner cannot be demoted in any server mode`() {
        ServerConstants.OWNER_ACCOUNTS = setOf("owner")

        ServerConstants.PRODUCTION_MODE = false
        assertFalse(RolePolicy.canAssign("Owner", 0))
        assertFalse(RolePolicy.canAssign("owner", 1))
        assertTrue(RolePolicy.canAssign("owner", 2))

        ServerConstants.PRODUCTION_MODE = true
        assertFalse(RolePolicy.canAssign("owner", 0))
        assertFalse(RolePolicy.canAssign("owner", 1))
        assertTrue(RolePolicy.canAssign("owner", 2))
    }
}
