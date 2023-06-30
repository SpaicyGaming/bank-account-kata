package repository

import DBManager
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.templates.SqlTemplate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.PostgreSQLContainer
import java.time.ZoneId
import java.util.*

@ExtendWith(VertxExtension::class)
open class PostgresTest {

    internal lateinit var sqlClient: SqlClient

    companion object {
        private val postgresql = PostgreSQLContainer("postgres:14").withReuse(true)
        private lateinit var dbConfig: JsonObject

        @BeforeAll
        @JvmStatic
        fun startContainer(vertx: Vertx, testContext: VertxTestContext) {
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

            postgresql.start()

            dbConfig = JsonObject()
                .put("host", postgresql.host)
                .put("port", postgresql.firstMappedPort)
                .put("database", postgresql.databaseName)
                .put("username", postgresql.username)
                .put("password", postgresql.password)

            DBManager.createTables(sqlClient = PgPool.pool(vertx, DBManager.pgConnectOptions(dbConfig), PoolOptions()))
                ?.onSuccess { testContext.completeNow() }
                ?.onFailure { testContext.failNow(it) }
        }
    }

    @BeforeEach
    internal fun initializeJdbc(vertx: Vertx, testContext: VertxTestContext) {
        sqlClient = PgPool.pool(vertx, DBManager.pgConnectOptions(dbConfig), PoolOptions())
        setup()
            .onSuccess { testContext.completeNow() }
            .onFailure { testContext.failNow(it) }
    }

    @AfterEach
    internal fun tearDown(testContext: VertxTestContext) {
        deleteAllFrom("accounts")
            .onSuccess { testContext.completeNow() }
            .onFailure { testContext.failNow(it) }
    }

    private fun deleteAllFrom(table: String): Future<Unit> {
        return SqlTemplate.forUpdate(sqlClient, "delete from $table")
            .execute(emptyMap())
            .mapEmpty()
    }

    open fun setup(): Future<Unit> = Future.succeededFuture()

}