package content.region.misthalin.varrock.dialogue

import content.region.misthalin.varrock.MuseumKudos
import core.game.dialogue.DialoguePlugin
import core.game.dialogue.FacialExpression
import core.game.node.entity.player.Player
import core.plugin.Initializable
import core.tools.END_DIALOGUE
import core.tools.START_DIALOGUE
import org.rs09.consts.NPCs

/** Dialogue for Orlando Smith, who runs the Natural History Quiz. */
@Initializable
class OrlandoSmithDialogue(player: Player? = null) : DialoguePlugin(player) {
    override fun handle(interfaceId: Int, buttonId: Int): Boolean {
        when (stage) {
            START_DIALOGUE -> {
                if (MuseumKudos.claimCompletionRewards(player)) {
                    npcl(
                        FacialExpression.FRIENDLY,
                        "You've done outstanding work for the museum. I've awarded you all the Kudos rewards and lamps!"
                    ).also { stage = 10 }
                } else {
                    npcl(
                        FacialExpression.FRIENDLY,
                        "Hello! Welcome to the Natural History Quiz!"
                    ).also { stage++ }
                }
            }

            1 -> options(
                "What is the Natural History Quiz?",
                "How do I take part?",
                "Goodbye."
            ).also { stage++ }

            2 -> when (buttonId) {
                1 -> playerl(
                    FacialExpression.NEUTRAL,
                    "What is the Natural History Quiz?"
                ).also { stage = 3 }

                2 -> playerl(
                    FacialExpression.NEUTRAL,
                    "How do I take part?"
                ).also { stage = 5 }

                3 -> playerl(
                    FacialExpression.FRIENDLY,
                    "Goodbye."
                ).also { stage = END_DIALOGUE }
            }

            3 -> npcl(
                FacialExpression.FRIENDLY,
                "It's a quiz about the creatures in the Natural History exhibit."
            ).also { stage++ }

            4 -> npcl(
                FacialExpression.FRIENDLY,
                "Answer the questions correctly and you'll earn Kudos for the museum."
            ).also { stage = 1 }

            5 -> npcl(
                FacialExpression.FRIENDLY,
                "Study one of the plaques in the exhibit, then choose an answer to its question."
            ).also { stage = 1 }

            10 -> npcl(
                FacialExpression.FRIENDLY,
                "The workman's gate at the Dig Site is now available to you as well."
            ).also { stage = END_DIALOGUE }
        }
        return true
    }

    override fun newInstance(player: Player): DialoguePlugin = OrlandoSmithDialogue(player)

    override fun getIds(): IntArray = intArrayOf(NPCs.ORLANDO_SMITH_5965)
}
