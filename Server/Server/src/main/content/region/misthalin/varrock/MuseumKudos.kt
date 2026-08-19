package content.region.misthalin.varrock

import core.api.addItemOrDrop
import core.game.node.entity.player.Player
import core.game.node.entity.player.link.diary.DiaryType
import core.game.node.entity.skill.Skills

/**
 * Temporary full-completion replacement for the unimplemented Museum Kudos system.
 *
 * The reward amounts combine the three Information Clerk Kudos tiers and Orlando
 * Smith's Natural History completion reward. It also includes the four lamps
 * normally awarded for the Museum's quest-display reports.
 */
object MuseumKudos {
    private const val COMPLETION_REWARDED_ATTRIBUTE = "/save:museum:kudos:completion-rewarded"
    private const val HARD_DIARY_ORLANDO_TASK = 4

    fun hasFullKudos(player: Player): Boolean = player.getAttribute(COMPLETION_REWARDED_ATTRIBUTE, false)

    /** Awards each full-Kudos reward once and marks the player as having 153 Kudos. */
    fun claimCompletionRewards(player: Player): Boolean {
        if (hasFullKudos(player)) {
            return false
        }

        player.setAttribute(COMPLETION_REWARDED_ATTRIBUTE, true)
        player.skills.addExperience(Skills.MINING, 3_500.0)
        player.skills.addExperience(Skills.CRAFTING, 6_500.0)
        player.skills.addExperience(Skills.HUNTER, 5_000.0)
        player.skills.addExperience(Skills.PRAYER, 4_000.0)
        player.skills.addExperience(Skills.SLAYER, 5_000.0)
        player.skills.addExperience(Skills.SMITHING, 4_000.0)
        addItemOrDrop(player, 11_186)
        addItemOrDrop(player, 11_187)
        addItemOrDrop(player, 11_188)
        addItemOrDrop(player, 11_189)
        player.achievementDiaryManager.finishTask(
            player,
            DiaryType.VARROCK,
            2,
            HARD_DIARY_ORLANDO_TASK
        )
        return true
    }
}
