import api.GetHistoryApi
import api.PostDepositApi
import api.PostWithdrawApi
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.jdbcclient.JDBCPool
import io.vertx.sqlclient.SqlClient
import json.JsonModules
import repository.BankAccounts


/**
 * This class is responsible for starting the verticle.
 * It will create the database tables and deploy the HTTP server.
 */
class BankVerticle : AbstractVerticle() {

    override fun start(start: Promise<Void>) {
        JsonModules.registerAll()
        val sqlClient = JDBCPool.pool(vertx, DBManager.h2ConnectOptions())

        vertx.executeBlocking<Unit> { promise ->
            DBManager.createTables(sqlClient)
                ?.onSuccess { promise.complete() }
                ?.onFailure(promise::fail)
        }.compose { deploy(sqlClient) }
            .onSuccess { start.complete() }
            .onFailure(start::fail)
    }

    private fun deploy(sqlClient: SqlClient): Future<HttpServer> {
        val bankAccounts = BankAccounts.fromToH2(sqlClient)

        val router = Router.router(vertx)
        GetHistoryApi(router, bankAccounts)
        PostDepositApi(router, bankAccounts)
        PostWithdrawApi(router, bankAccounts)

        return vertx.createHttpServer().requestHandler(router).listen(8080).onSuccess { server ->
            println("Http server is listening on port ${server.actualPort()}")
        }.onFailure { println("Http server failed to start") }
    }

}