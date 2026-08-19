package core.game.ge

import core.ServerConstants
import core.cache.def.impl.ItemDefinition
import core.integrations.sqlite.SQLiteProvider
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import java.sql.Connection

/**
 * Collection of methods for interacting with the grand exchange databases
 * @author Ceikry
 */
object GEDB {
    lateinit var db: SQLiteProvider

    fun init() {
        init (ServerConstants.GRAND_EXCHANGE_DATA_PATH + "grandexchange.db")
    }

    fun init (path: String) {
        db = SQLiteProvider(path, expectedTables)
        db.initTables { createdTable: String ->
            if (createdTable == "price_index")
                populateInitialPriceIndex()
        }
        migrateBotOffers()
    }

    @JvmStatic fun run(closure: (conn: Connection) -> Unit) {
        db.run(closure)
    }

    private fun convertJsonArray(arr: JSONArray): String
    {
        val sb = StringBuilder()
        for ((index, data) in arr.withIndex())
        {
            val item = data as JSONObject
            sb.append("$index")
            sb.append(",")
            sb.append(item["id"])
            sb.append(",")
            sb.append(item["amount"])
            if(index + 1 < arr.size)
                sb.append(":")
        }
        return sb.toString()
    }

    var expectedTables = hashMapOf(
        "player_offers" to "CREATE TABLE player_offers(" +
                "uid INTEGER PRIMARY KEY ASC," +
                "player_uid      INTEGER," +
                "item_id         INTEGER," +
                "amount_total    INTEGER," +
                "amount_complete INTEGER," +
                "offered_value   INTEGER," +
                "time_stamp      INTEGER," +
                "offer_state     INTEGER," +
                "is_sale         INTEGER," +
                "withdraw_items  STRING ," +
                "slot_index      INTEGER," +
                "total_coin_xc   INTEGER)",
        "bot_offers" to "CREATE TABLE bot_offers(" +
                "item_id         INTEGER UNIQUE," +
                "amount          INTEGER," +
                "offered_value   INTEGER)",
        "price_index" to "CREATE TABLE price_index(" +
                "item_id         INTEGER," +
                "value           INTEGER," +
                "total_value     INTEGER," +
                "unique_trades   INTEGER," +
                "last_update     INTEGER)"
    )

    private fun populateInitialPriceIndex()
    {
        //price index isn't worth transferring, so we're just going to make a new one.
        run {
            with (it.prepareStatement(PriceIndex.INSERT_QUERY)) {
                ItemDefinition.getDefinitions().values.forEach { def ->
                    if(def.isTradeable) {
                        setInt(1, def.id)
                        setInt(2, def.getAlchemyValue(true))
                        setInt(3, def.getAlchemyValue(true))
                        setInt(4, 0)
                        setInt(5, 0)
                        execute()
                    }
                }
            }
        }

    }

    /** Adds fixed prices to bot stock created before fixed-price GE liquidity. */
    private fun migrateBotOffers() {
        run { conn ->
            var hasOfferValue = false
            conn.createStatement().use { stmt ->
                stmt.executeQuery("PRAGMA table_info(bot_offers)").use { columns ->
                    while (columns.next()) {
                        if (columns.getString("name") == "offered_value") {
                            hasOfferValue = true
                            break
                        }
                    }
                }
                if (!hasOfferValue) {
                    stmt.executeUpdate("ALTER TABLE bot_offers ADD COLUMN offered_value INTEGER NOT NULL DEFAULT 0")
                }
            }

            conn.prepareStatement("SELECT rowid, item_id FROM bot_offers WHERE offered_value <= 0").use { select ->
                select.executeQuery().use { results ->
                    conn.prepareStatement("UPDATE bot_offers SET offered_value = ? WHERE rowid = ?").use { update ->
                        while (results.next()) {
                            update.setInt(1, GrandExchange.getFixedBotPrice(results.getInt("item_id")))
                            update.setLong(2, results.getLong("rowid"))
                            update.addBatch()
                        }
                        update.executeBatch()
                    }
                }
            }

            conn.createStatement().use { stmt ->
                stmt.executeUpdate(
                    "UPDATE bot_offers SET " +
                        "amount = (SELECT MAX(duplicates.amount) FROM bot_offers duplicates WHERE duplicates.item_id = bot_offers.item_id), " +
                        "offered_value = (SELECT MAX(duplicates.offered_value) FROM bot_offers duplicates WHERE duplicates.item_id = bot_offers.item_id) " +
                        "WHERE rowid IN (SELECT MIN(rowid) FROM bot_offers GROUP BY item_id)"
                )
                stmt.executeUpdate(
                    "DELETE FROM bot_offers WHERE rowid NOT IN " +
                        "(SELECT MIN(rowid) FROM bot_offers GROUP BY item_id)"
                )
                stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS bot_offers_item_id_unique ON bot_offers(item_id)")
            }
        }
    }
}
