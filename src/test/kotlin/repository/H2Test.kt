package repository

import DBManager
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.jdbcclient.JDBCPool
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.SqlClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith

/**
 * This class is responsible for interacting with the H2 database for testing purposes.
 */
@ExtendWith(VertxExtension::class)
open class H2Test {

    internal lateinit var sqlClient: SqlClient

    companion object {

        private lateinit var sqlClientCompanion: SqlClient

        @JvmStatic
        @BeforeAll
        fun setupDB(vertx: Vertx, testContext: VertxTestContext) {
            sqlClientCompanion = JDBCPool.pool(vertx, DBManager.h2ConnectOptions())
            DBManager.createTables(sqlClientCompanion)
                ?.onSuccess { testContext.completeNow() }
                ?.onFailure { testContext.failNow(it) }
        }

        @JvmStatic
        @AfterAll
        internal fun resetDatabase(testContext: VertxTestContext) {
            sqlClientCompanion.query("drop all objects delete files")
                .execute()
                .onSuccess { testContext.completeNow() }
                .onFailure(Throwable::printStackTrace)
        }
    }

    @BeforeEach
    internal fun initializeJdbc(vertx: Vertx, testContext: VertxTestContext) {
        sqlClient = JDBCPool.pool(vertx, DBManager.h2ConnectOptions())
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

    private fun deleteAllFrom(table: String): Future<Unit> =
        sqlClient.query("delete from $table")
            .execute()
            .mapEmpty()

    open fun setup(): Future<Unit> = Future.succeededFuture()

}