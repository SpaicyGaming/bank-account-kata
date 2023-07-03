import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient

object DBManager {

    /**
     * This method creates the [JsonObject] configuration for the connection to the H2 in memory database.
     */
    fun h2ConnectOptions(): JsonObject = h2ConnectOptions(
        JsonObject()
            .put("url", "jdbc:h2:mem:bank;DB_CLOSE_DELAY=-1")
            .put("jdbcUrl", "jdbc:h2:mem:bank;DB_CLOSE_DELAY=-1")
            .put("username", "sa")
            .put("password", "")
            .put("max_pool_size", 16)
    )

    /**
     * This method creates the [JsonObject] configuration for the connection to the database.
     */
    fun h2ConnectOptions(config: JsonObject): JsonObject = JsonObject()
        .put("url", config.getString("url"))
        .put("jdbcUrl", config.getString("jdbcUrl"))
        .put("username", config.getString("username"))
        .put("password", config.getString("password"))
        .put("max_pool_size", config.getInteger("max_pool_size"))

    /**
     * This method creates the [PgConnectOptions] from the configuration.
     */
    fun pgConnectOptions(config: JsonObject): PgConnectOptions = PgConnectOptions()
        .setHost(config.getString("host"))
        .setPort(config.getInteger("port"))
        .setDatabase(config.getString("database"))
        .setUser(config.getString("username"))
        .setPassword(config.getString("password"))

    /**
     * This method creates the database tables.
     */
    @JvmStatic
    fun createTables(sqlClient: SqlClient): Future<RowSet<Row>>? {
        val accountsTableCreationQuery = """
                CREATE TABLE accounts
                (
                    id          UUID           NOT NULL,
                    amount      NUMERIC(20, 2) NOT NULL,
                    inserted_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
            """.trimIndent()

        return sqlClient.query(accountsTableCreationQuery)
            .execute()
    }

}