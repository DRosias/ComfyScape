package core.game.node.entity.player.link

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BankPinManagerTests {
    @Test
    fun `configured pin remains locked until entered`() {
        val manager = BankPinManager(null)
        assertTrue(manager.isUnlocked)

        manager.pin = "1234"
        assertFalse(manager.isUnlocked)

        manager.isUnlocked = true
        assertTrue(manager.isUnlocked)
    }
}
