package content.global.quest

import content.data.Quests
import core.api.LoginListener
import core.api.StartupListener
import core.api.getVarp
import core.game.node.entity.player.Player
import core.game.node.entity.player.link.quest.Quest
import core.game.node.entity.player.link.quest.QuestRepository

/**
 * Registers completion-only quest entries for every quest listed by the client
 * which has no server-side quest implementation.
 *
 * Completion state is the common unlock contract used by content requirements,
 * so this deliberately grants that state without recreating quest rewards. No
 * experience, items, or quest-specific attributes are awarded here.
 */
class UnavailableQuestAutoComplete : StartupListener, LoginListener {
    private val completionOnlyQuests = mutableListOf<Quest>()

    override fun startup() {
        Quests.values()
            .filterNot { it == Quests.TEST_QUEST }
            .filterNot { QuestRepository.getQuests().containsKey(it) }
            .mapTo(completionOnlyQuests) { quest ->
                CompletionOnlyQuest(
                    quest = quest,
                    index = questTabButtonId(quest) + 1,
                    buttonId = questTabButtonId(quest),
                    questPoints = compatibilityQuestPoints(quest)
                ).also(QuestRepository::register)
            }
    }

    override fun login(player: Player) {
        val newlyCompleted = completionOnlyQuests.filter {
            player.questRepository.getStage(it) < 100
        }
        if (newlyCompleted.isEmpty()) return

        newlyCompleted.forEach { player.questRepository.setStage(it, 100) }
        player.questRepository.syncPoints()
        player.questRepository.syncronizeTab(player)
        player.sendMessage(
            "<col=CC6600>${newlyCompleted.size} unimplemented quest" +
                "${if (newlyCompleted.size == 1) " has" else "s have"} been marked complete.</col>"
        )
    }

    /**
     * Quest-tab headings do not correspond to quest enum entries. Myths has a
     * dedicated client button; all normal free/member quests follow the tab's
     * contiguous button layout.
     */
    private fun questTabButtonId(quest: Quests): Int = when (quest) {
        Quests.MYTHS_OF_THE_WHITE_LANDS -> 162
        else -> if (quest.ordinal < FIRST_MEMBERS_QUEST_ORDINAL) {
            quest.ordinal + FREE_QUEST_BUTTON_OFFSET
        } else {
            quest.ordinal + MEMBERS_QUEST_BUTTON_OFFSET
        }
    }

    /**
     * Keep the two explicitly requested quest-point awards. Other unavailable
     * quests have no reward data in this codebase, so do not fabricate awards.
     */
    private fun compatibilityQuestPoints(quest: Quests): Int = when (quest) {
        Quests.SHILO_VILLAGE -> 2
        Quests.PERILS_OF_ICE_MOUNTAIN -> 1
        else -> 0
    }

    private class CompletionOnlyQuest(
        quest: Quests,
        index: Int,
        buttonId: Int,
        questPoints: Int
    ) : Quest(quest, index, buttonId, questPoints, 0, 0, 0, 0) {
        override fun newInstance(`object`: Any?): Quest = this

        override fun getConfig(player: Player, stage: Int): IntArray {
            // No completion varp is known for these absent implementations.
            // Preserve varp 0 rather than changing an unrelated client setting.
            return intArrayOf(0, getVarp(player, 0))
        }
    }

    private companion object {
        const val FIRST_MEMBERS_QUEST_ORDINAL = 19
        const val FREE_QUEST_BUTTON_OFFSET = 12
        const val MEMBERS_QUEST_BUTTON_OFFSET = 13
    }
}
