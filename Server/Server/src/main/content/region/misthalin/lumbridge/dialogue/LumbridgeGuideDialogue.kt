package content.region.misthalin.lumbridge.dialogue

import core.api.isQuestComplete
import core.game.dialogue.DialoguePlugin
import core.game.dialogue.FacialExpression
import core.game.node.entity.player.Player
import core.game.node.entity.player.link.diary.DiaryType
import core.game.world.map.zone.impl.ModeratorZone
import core.plugin.Initializable
import org.rs09.consts.NPCs
import core.game.dialogue.IfTopic
import core.game.dialogue.Topic
import core.tools.END_DIALOGUE
import content.data.Quests

@Initializable
class LumbridgeGuideDialogue(player: Player? = null) : DialoguePlugin(player) {
    override fun newInstance(player: Player?): DialoguePlugin {
        return LumbridgeGuideDialogue(player)
    }

    override fun handle(interfaceId: Int, buttonId: Int): Boolean {
        val staff = player.isStaff
        val sheepShearerComplete = isQuestComplete(player, Quests.SHEEP_SHEARER)
        val cooksAssistantComplete = isQuestComplete(player, Quests.COOKS_ASSISTANT)

        when (stage) {
            0 -> npcl(FacialExpression.FRIENDLY, "Greetings, adventurer. I am Phileas, the Lumbridge Guide. I am here to give information and directions to new players. Do you require any help?").also { stage++ }
            1 -> showTopics(
                Topic("Where can I find a quest to go on?", 10),
                Topic("What monsters should I fight?", 20),
                Topic("Where can I make money?", 30),
                Topic("I'd like to know more about security.", 40),
                Topic("Where can I find a bank?", 50),
                IfTopic("More Options...", 100, staff, skipPlayer = true),
            )

            //More Options...
            100 -> showTopics(
                IfTopic("I would like to access the P-Mod room.", 200, staff),
                Topic("Go back...", 1, skipPlayer = true)
            )

            //Where can I find a quest?
            10 -> {
                if (!cooksAssistantComplete)
                    npcl(FacialExpression.HALF_THINKING, "You can try talking to the Cook in the Lumbridge Castle. I hear he is always looking for some help.")
                else if (!sheepShearerComplete)
                    npcl(FacialExpression.HALF_THINKING, "You can try talking to Fred the Farmer north-west of here. I hear he is always looking for some help.")
                else
                    npcl(FacialExpression.FRIENDLY, "You are such an accomplished adventurer already; you should be telling me some good quests to go on.")
                stage = END_DIALOGUE
            }

            //What monsters should I fight?
            20 -> if (player.properties.currentCombatLevel >= 30) {
                npcl(FacialExpression.FRIENDLY, "You're strong enough to work out what monsters to fight for yourself now, but the tutors might help you with any questions you have about the skills; they're just south of the general store.")
                stage = END_DIALOGUE
            } else {
                npcl(FacialExpression.FRIENDLY, "There are things to kill all over the place! At your level, you might like to try wandering westwards to the Wizards' Tower or north-west to the Barbarian Village.")
                stage++
            }
            21 -> npcl(FacialExpression.FRIENDLY, "Non-player characters usually appear as yellow dots on your mini-map, although there are some that you won't be able to fight, such as myself. Watch out for monsters which are tougher").also { stage++ }
            22 -> npcl(FacialExpression.FRIENDLY, "than you. A monster's combat level is shown next to their 'Attack' option. If that level is coloured green it means the monster is weaker than you. If it is red, it means the monster is tougher than you.").also { stage++ }
            23 -> npcl(FacialExpression.FRIENDLY, "Remember, you will do better if you have better armour and weapons and it's always worth carrying a bit of food to heal yourself.").also { stage = 1 }

            //Where can I make money?
            30 -> npcl(FacialExpression.FRIENDLY, "There are many ways to make money in the game. I would suggest either killing monsters or doing a trade skill such as Smithing or Fishing.").also { stage++ }
            31 -> npcl(FacialExpression.FRIENDLY, "Please don't try to get money by begging off other players. It will make you unpopular. Nobody likes a beggar. It is very irritating to have other players asking for your hard-earned cash.").also { stage = 1 }

            //I'd like to know more about security
            40 -> npcl(FacialExpression.FRIENDLY, "I can tell you about password security, avoiding item scamming and in-game moderation. I can also tell you about a place called the Stronghold of Security, where you can learn more about account security and have a").also { stage++ }
            41 -> {
                player.achievementDiaryManager.finishTask(player, DiaryType.LUMBRIDGE, 0, 17)
                npcl(FacialExpression.FRIENDLY, "bit of an adventure at the same time. In fact, why don't you just head there instead? It's a lot more fun, I promise. You can find it down the hole in the middle of Barbarian Village to the north-west.")
                stage = 1
            }

            //Where can I find a bank?
            50 -> npcl(FacialExpression.FRIENDLY, "You'll find a bank upstairs in Lumbridge Castle - go right to the top!").also { stage = 1 }

            //visit pmod room
            200 -> npcl(FacialExpression.FRIENDLY, "Yes, of course.").also { stage++ }
            201 -> {
                end()
                if (player.isStaff)
                    ModeratorZone.teleport(player)
            }

        }
        return true
    }

    override fun getIds(): IntArray {
        return intArrayOf(NPCs.LUMBRIDGE_GUIDE_2244)
    }
}
