package core.security

import core.game.node.entity.player.info.Rights
import core.game.system.command.CommandAuthorization
import core.game.system.command.Privilege
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandAuthorizationTests {
    @Test
    fun `normal commands respect player command toggle except bank`() {
        assertFalse(CommandAuthorization.canUse("players", Privilege.STANDARD, Rights.REGULAR_PLAYER, false))
        assertTrue(CommandAuthorization.canUse("players", Privilege.STANDARD, Rights.REGULAR_PLAYER, true))
        assertTrue(CommandAuthorization.canUse("bank", Privilege.STANDARD, Rights.REGULAR_PLAYER, false))
    }

    @Test
    fun `moderators receive support commands but never admin commands`() {
        assertTrue(CommandAuthorization.canUse("unstuck", Privilege.MODERATOR, Rights.PLAYER_MODERATOR, false))
        assertTrue(CommandAuthorization.canUse("gerestock", Privilege.MODERATOR, Rights.PLAYER_MODERATOR, false))
        assertTrue(CommandAuthorization.canUse("recoverpassword", Privilege.MODERATOR, Rights.PLAYER_MODERATOR, false))
        assertFalse(CommandAuthorization.canUse("item", Privilege.ADMIN, Rights.PLAYER_MODERATOR, true))
        assertFalse(CommandAuthorization.canUse("setrole", Privilege.ADMIN, Rights.PLAYER_MODERATOR, true))
        assertFalse(CommandAuthorization.canUse("shutdown", Privilege.ADMIN, Rights.PLAYER_MODERATOR, true))
    }

    @Test
    fun `owner retains every command privilege`() {
        Privilege.values().forEach {
            assertTrue(CommandAuthorization.canUse("anything", it, Rights.ADMINISTRATOR, false))
        }
    }
}
