package core.game.system.command

import core.game.node.entity.player.Player
import core.ServerConstants
import core.api.sendChat
import core.game.node.entity.player.info.Rights
import kotlin.collections.ArrayList

/**
 * Base class for Commands in the new system. Can pass a lambda as part of the constructor or after the constructor.
 * @author Ceikry, Player Name
 */
class Command(val name: String, val privilege: Privilege, val usage: String = "UNDOCUMENTED", val description: String = "UNDOCUMENTED", val handle: (Player, Array<String>) -> Unit) {
    fun attemptHandling(player: Player, args: Array<String>?){
        args ?: return
        val hasRights = CommandAuthorization.canUse(name, privilege, player.rights, ServerConstants.PLAYER_COMMANDS)
        if (hasRights) {
            handle(player,args)
        } else if (ServerConstants.PLAYER_COMMANDS || args[0] == "bank") {
            // Authentically, if you attempt to use a command you shouldn't have access to, the game is supposed to say the below line.
            // We do this for all our custom commands if they are enabled (in which case this will only trigger for commands above your privilege level),
            // otherwise we only do it for the one command we have evidence existed authentically, "bank" (https://www.reddit.com/r/2007scape/comments/3n1c4d/hey_everyone_i_just_tried_to_do_something_very/)
            sendChat(player, "Hey, everyone, I just tried to do something very silly!")
        }
    }
}

object CommandAuthorization {
    fun canUse(commandName: String, privilege: Privilege, rights: Rights, playerCommandsEnabled: Boolean): Boolean {
        if (rights == Rights.ADMINISTRATOR || commandName == "bank") return true
        return when (privilege) {
            Privilege.STANDARD -> playerCommandsEnabled
            Privilege.MODERATOR -> rights == Rights.PLAYER_MODERATOR
            Privilege.ADMIN -> false
        }
    }
}

object CommandMapping {
    private val mapping = hashMapOf<String, Command>()

    fun get(name: String): Command?{
        return mapping[name]
    }

    fun register(command: Command){
        mapping[command.name] = command
    }

    fun getCommands(): Array<Command> {
        return mapping.values.toTypedArray()
    }

    fun getNames(): Array<String> {
        return mapping.keys.toTypedArray()
    }

    fun getPageIndices(rights: Int): IntArray {
        val list = ArrayList<Int>()
        list.add(0)

        var lineCounter = 0
        for ((index, command) in getCommands().filter { it.privilege.ordinal <= rights }.withIndex()) {

            lineCounter += 2
            if (command.usage.isNotEmpty()) lineCounter++
            if (command.description.isNotEmpty()) lineCounter++

            if (lineCounter > 306) {
                list.add(index)
                lineCounter = 0
            }
        }

        return list.toIntArray()
    }
}
