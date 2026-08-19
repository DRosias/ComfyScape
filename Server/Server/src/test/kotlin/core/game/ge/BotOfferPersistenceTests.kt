package core.game.ge

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.sql.DriverManager

class BotOfferPersistenceTests {
    private val databaseFile = File("target/bot-offer-persistence-test.db").absoluteFile

    @BeforeEach
    fun createLegacyDatabaseWithDuplicates() {
        databaseFile.delete()
        DriverManager.getConnection("jdbc:sqlite:${databaseFile.path}").use { conn ->
            conn.createStatement().use { stmt ->
                GEDB.expectedTables.filterKeys { it != "bot_offers" }.values.forEach(stmt::execute)
                stmt.execute("CREATE TABLE bot_offers(item_id INTEGER, amount INTEGER, offered_value INTEGER)")
                stmt.execute("INSERT INTO bot_offers VALUES (100, 5, 50), (100, 9, 55)")
            }
        }
    }

    @AfterEach
    fun removeTestDatabase() {
        databaseFile.delete()
    }

    @Test
    fun `migration consolidates duplicates and all writes preserve one row per item`() {
        GEDB.init(databaseFile.path)
        assertBotOffer(amount = 9, price = 55, rows = 1)

        assertEquals(BotRestockResult(1, 3), GrandExchange.restockBotOffers(listOf(BotStockEntry(100, 12, 60))))
        assertEquals(BotRestockResult(0, 0), GrandExchange.restockBotOffers(listOf(BotStockEntry(100, 12, 60))))

        GrandExchangeOffer().apply {
            isBot = true
            itemID = 100
            amount = 4
            offeredValue = 60
        }.writeNew()

        assertBotOffer(amount = 16, price = 60, rows = 1)
    }

    private fun assertBotOffer(amount: Int, price: Int, rows: Int) {
        GEDB.run { conn ->
            conn.prepareStatement("SELECT amount, offered_value FROM bot_offers WHERE item_id = 100").use { select ->
                select.executeQuery().use { result ->
                    var count = 0
                    while (result.next()) {
                        count++
                        assertEquals(amount, result.getInt("amount"))
                        assertEquals(price, result.getInt("offered_value"))
                    }
                    assertEquals(rows, count)
                }
            }
        }
    }
}
