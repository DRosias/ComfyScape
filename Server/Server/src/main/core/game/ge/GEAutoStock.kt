package core.game.ge

import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import core.ServerConstants
import java.io.File
import java.io.FileReader
import core.tools.Log
import core.api.log

data class BotStockEntry(val itemId: Int, val targetStock: Int, val sellPrice: Int)

data class BotRestockResult(val entriesRestocked: Int, val itemsAdded: Int)

object GEAutoStock {
    fun autostock() {
        restock()
    }

    fun restock(): BotRestockResult {
        if (!ServerConstants.GE_AUTOSTOCK_ENABLED) return BotRestockResult(0, 0)

        val catalog = loadCatalog()
        return GrandExchange.restockBotOffers(catalog)
    }

    private fun loadCatalog(): List<BotStockEntry> {
        val dataPath = ServerConstants.GRAND_EXCHANGE_DATA_PATH ?: "data${File.separator}eco"
        val stockFile = File(dataPath, "autostock.json")
        if (!stockFile.isFile) {
            log(GEAutoStock::class.java, Log.WARN, "GE auto-stock is enabled but ${stockFile.path} is missing.")
            return emptyList()
        }

        FileReader(stockFile).use { botReader ->
            val botSave = JSONParser().parse(botReader) as JSONObject
            val offers = botSave["offers"] as? JSONArray ?: return emptyList()
            return offers.map { offer ->
                val stock = offer as JSONObject
                val itemId = stock["item"].toString().toInt()
                val targetStock = stock["target"].toString().toInt()
                require(targetStock in 1..ServerConstants.BOTSTOCK_LIMIT) {
                    "GE stock target for item $itemId must be between 1 and ${ServerConstants.BOTSTOCK_LIMIT}."
                }
                BotStockEntry(itemId, targetStock, GrandExchange.getFixedBotPrice(itemId))
            }
        }
    }
}
