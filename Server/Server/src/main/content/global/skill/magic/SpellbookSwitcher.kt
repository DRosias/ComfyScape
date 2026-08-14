package content.global.skill.magic

import core.game.event.SpellbookChangeEvent
import core.game.node.entity.combat.equipment.WeaponInterface
import core.game.node.entity.player.Player
import core.game.node.entity.player.link.SpellBookManager
import core.game.node.entity.player.link.SpellBookManager.SpellBook

/** Performs the common state change used when a player changes spellbooks. */
object SpellbookSwitcher {
    fun switch(player: Player, target: SpellBook, source: SpellBookManager.SpellbookChangeSource): Boolean {
        val current = SpellBook.forInterface(player.spellBookManager.spellBook) ?: return false
        if (current == target) return false

        val weaponInterface = player.getExtension<WeaponInterface>(WeaponInterface::class.java)
        if (weaponInterface != null && player.properties.autocastSpell != null) {
            weaponInterface.selectAutoSpell(-1, true)
        }

        player.dispatch(SpellbookChangeEvent(current, target, source))
        player.spellBookManager.setSpellBook(target)
        player.spellBookManager.update(player)
        return true
    }
}
