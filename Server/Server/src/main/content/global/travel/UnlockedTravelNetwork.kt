package content.global.travel

import core.ServerConstants
import core.api.Commands
import core.api.MapArea
import core.api.PersistPlayer
import core.api.StartupListener
import core.api.getAttribute
import core.api.openDialogue
import core.api.sendMessage
import core.api.setAttribute
import core.cache.def.impl.SceneryDefinition
import core.game.dialogue.DialogueFile
import core.game.interaction.IntType
import core.game.interaction.InteractionListener
import core.game.node.entity.Entity
import core.game.node.entity.player.Player
import core.game.node.entity.player.link.TeleportManager
import core.game.node.scenery.Scenery
import core.game.node.scenery.SceneryBuilder
import core.game.system.command.Privilege
import core.game.world.map.Location
import core.game.world.map.RegionManager
import core.game.world.map.zone.ZoneBorders
import org.json.simple.JSONObject

private const val DISCOVERY_RADIUS = 5
private const val TRAVEL_COOLDOWN_MILLIS = 30_000L
// The dialogue options interface permits at most five entries. A middle page
// needs Previous, Next, and Close controls, leaving room for two destinations.
private const val DESTINATIONS_PER_PAGE = 2

/**
 * The deliberately small initial set of player-discoverable travel hubs.
 * IDs are persisted, so they must remain stable if the enum is reordered.
 */
enum class UnlockedTravelDestination(
    val id: String,
    val displayName: String,
    val location: Location,
    vararg names: String
) {
    AL_KHARID("al-kharid", "Al Kharid", Location.create(3293, 3184, 0), "alkharid", "kharid"),
    DRAYNOR("draynor", "Draynor", Location.create(3083, 3249, 0), "draynor village"),
    EAST_ARDOUGNE("east-ardougne", "East Ardougne", Location.create(2663, 3305, 0), "ardougne", "ardy"),
    EDGEVILLE("edgeville", "Edgeville", Location.create(3088, 3491, 0), "edge"),
    FALADOR("falador", "Falador", Location.create(2965, 3380, 0), "fally"),
    GRAND_EXCHANGE("grand-exchange", "Grand Exchange", Location.create(3164, 3485, 0), "ge"),
    LUMBRIDGE("lumbridge", "Lumbridge", Location.create(3222, 3217, 0), "lumby"),
    PORT_SARIM("port-sarim", "Port Sarim", Location.create(3019, 3244, 0), "sarim"),
    VARROCK("varrock", "Varrock", Location.create(3213, 3428, 0)),
    WIZARDS_TOWER("wizards-tower", "Wizards' Tower", Location.create(3110, 3168, 0), "wizard tower", "wizards tower", "tower", "wizards");

    val unlockAttribute = "/save:travel:$id"
    val discoveryArea = ZoneBorders(
        location.x - DISCOVERY_RADIUS,
        location.y - DISCOVERY_RADIUS,
        location.x + DISCOVERY_RADIUS,
        location.y + DISCOVERY_RADIUS,
        location.z
    )
    private val aliases = (names.toList() + displayName + id).map(::normalizeTravelName).toSet()

    fun matches(name: String): Boolean = normalizeTravelName(name) in aliases
}

private fun normalizeTravelName(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")

object UnlockedTravelManager {
    const val TELEPORT_UNLOCK_BYPASS_ENABLED = "/save:teleportUnlockBypassEnabled"
    private const val PORTAL_OBJECT_ID = 13638 // Marble portal; unused by global travel handlers.
    private const val PORTAL_OPTION = "travel"

    private var portalLocation: Location? = null

    fun discoverAt(player: Player, announce: Boolean = true) {
        UnlockedTravelDestination.values()
            .filter { it.discoveryArea.insideBorder(player.location) }
            .forEach { unlock(player, it, announce) }
    }

    fun unlock(player: Player, destination: UnlockedTravelDestination, announce: Boolean = true) {
        if (isUnlocked(player, destination)) return

        setAttribute(player, destination.unlockAttribute, true)
        if (announce) {
            sendMessage(player, "You have discovered ${destination.displayName}.")
        }
    }

    fun isUnlocked(player: Player, destination: UnlockedTravelDestination): Boolean =
        getAttribute(player, destination.unlockAttribute, false)

    fun isTeleportUnlockBypassEnabled(player: Player): Boolean =
        getAttribute(player, TELEPORT_UNLOCK_BYPASS_ENABLED, false)

    internal fun availableDestinations(player: Player): List<UnlockedTravelDestination> =
        if (isTeleportUnlockBypassEnabled(player)) {
            UnlockedTravelDestination.values().sortedBy { it.displayName }
        } else {
            unlockedDestinations(player)
        }

    fun openMenu(player: Player) {
        discoverAt(player)
        if (availableDestinations(player).isEmpty()) {
            sendMessage(player, "You have not discovered any travel destinations yet.")
            return
        }
        openDialogue(player, UnlockedTravelDialogue())
    }

    fun travelByName(player: Player, name: String) {
        discoverAt(player)
        val destination = UnlockedTravelDestination.values().firstOrNull { it.matches(name) }
        if (destination == null) {
            sendMessage(player, "Unknown travel destination: $name.")
            return
        }
        travel(player, destination)
    }

    fun travel(player: Player, destination: UnlockedTravelDestination): Boolean {
        if (!isUnlocked(player, destination) && !isTeleportUnlockBypassEnabled(player)) {
            sendMessage(player, "You have not discovered ${destination.displayName} yet.")
            return false
        }

        val now = System.currentTimeMillis()
        val availableAt = player.savedData.globalData.globalTeleporterDelay
        if (availableAt > now) {
            val remainingSeconds = ((availableAt - now) + 999L) / 1000L
            sendMessage(player, "You can use the travel network again in $remainingSeconds seconds.")
            return false
        }

        if (!player.teleporter.send(destination.location, TeleportManager.TeleportType.NORMAL)) {
            return false
        }

        player.savedData.globalData.globalTeleporterDelay = now + TRAVEL_COOLDOWN_MILLIS
        return true
    }

    fun isTravelPortal(location: Location): Boolean = location == portalLocation

    fun spawnPortal() {
        val home = ServerConstants.HOME_LOCATION ?: return
        val candidates = listOf(
            home.transform(2, 0, 0),
            home.transform(0, 2, 0),
            home.transform(-2, 0, 0),
            home.transform(0, -2, 0)
        )
        val location = candidates.firstOrNull {
            RegionManager.getObject(it) == null && !RegionManager.isClipped(it.z, it.x, it.y)
        } ?: return

        SceneryDefinition.forId(PORTAL_OBJECT_ID).options[0] = PORTAL_OPTION.replaceFirstChar(Char::uppercase)
        SceneryBuilder.add(Scenery(PORTAL_OBJECT_ID, location))
        portalLocation = location
    }

    internal fun unlockedDestinations(player: Player): List<UnlockedTravelDestination> =
        UnlockedTravelDestination.values().filter { isUnlocked(player, it) }.sortedBy { it.displayName }

    internal const val portalObjectId = PORTAL_OBJECT_ID
    internal const val portalOption = PORTAL_OPTION
}

class UnlockedTravelNetwork : Commands, InteractionListener, MapArea, PersistPlayer, StartupListener {
    override fun defineCommands() {
        val handleTravelCommand: (Player, Array<String>) -> Unit = { player, args ->
            val destinationName = args.drop(1).joinToString(" ").trim()
            if (destinationName.isEmpty()) {
                UnlockedTravelManager.openMenu(player)
            } else {
                UnlockedTravelManager.travelByName(player, destinationName)
            }
        }

        define(
            "teleport",
            Privilege.STANDARD,
            "::teleport [destination]",
            "Opens the teleport menu or teleports to an unlocked destination.",
            handleTravelCommand
        )
        define(
            "tp",
            Privilege.STANDARD,
            "::tp [destination]",
            "Opens the teleport menu or teleports to an unlocked destination.",
            handleTravelCommand
        )
    }

    override fun defineListeners() {
        on(UnlockedTravelManager.portalObjectId, IntType.SCENERY, UnlockedTravelManager.portalOption) { player, node ->
            if (UnlockedTravelManager.isTravelPortal(node.location)) {
                UnlockedTravelManager.openMenu(player)
            }
            return@on true
        }
    }

    override fun defineAreaBorders(): Array<ZoneBorders> =
        UnlockedTravelDestination.values().map { it.discoveryArea }.toTypedArray()

    override fun areaEnter(entity: Entity) {
        if (entity is Player) {
            UnlockedTravelManager.discoverAt(entity)
        }
    }

    override fun parsePlayer(player: Player, data: JSONObject) {
        UnlockedTravelManager.discoverAt(player, announce = false)
    }

    override fun savePlayer(player: Player, save: JSONObject) {
        // Destination discovery is stored in normal saved attributes.
    }

    override fun startup() {
        UnlockedTravelManager.spawnPortal()
    }
}

private class UnlockedTravelDialogue : DialogueFile() {
    private var page = 0

    override fun handle(componentID: Int, buttonID: Int) {
        val player = player ?: return
        val destinations = UnlockedTravelManager.availableDestinations(player)
        if (destinations.isEmpty()) {
            end()
            return
        }

        if (stage == 0) {
            showPage(destinations)
            return
        }

        val pageCount = (destinations.size + DESTINATIONS_PER_PAGE - 1) / DESTINATIONS_PER_PAGE
        page = page.coerceIn(0, pageCount - 1)
        val pageDestinations = destinations.drop(page * DESTINATIONS_PER_PAGE).take(DESTINATIONS_PER_PAGE)
        val selectedIndex = buttonID - 1

        if (selectedIndex in pageDestinations.indices) {
            UnlockedTravelManager.travel(player, pageDestinations[selectedIndex])
            return
        }

        var navigationIndex = pageDestinations.size
        if (page > 0) {
            if (selectedIndex == navigationIndex) {
                page--
                showPage(destinations)
                return
            }
            navigationIndex++
        }
        if (page < pageCount - 1 && selectedIndex == navigationIndex) {
            page++
            showPage(destinations)
            return
        }

        end()
    }

    private fun showPage(destinations: List<UnlockedTravelDestination>) {
        val pageCount = (destinations.size + DESTINATIONS_PER_PAGE - 1) / DESTINATIONS_PER_PAGE
        page = page.coerceIn(0, pageCount - 1)
        val pageDestinations = destinations.drop(page * DESTINATIONS_PER_PAGE).take(DESTINATIONS_PER_PAGE)
        val options = pageDestinations.map { it.displayName }.toMutableList()
        if (page > 0) options.add("Previous page")
        if (page < pageCount - 1) options.add("Next page")
        options.add("Close")

        interpreter!!.sendOptions("Travel destinations", *options.toTypedArray())
        stage = 1
    }
}
