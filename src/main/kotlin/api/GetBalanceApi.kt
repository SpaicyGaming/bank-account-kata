package api

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import repository.BankAccounts
import java.util.*

class GetBalanceApi(router: Router, private val bankAccounts: BankAccounts) : Handler<RoutingContext> {

    init {
        router.get("/api/balance/:uuid").handler(this)
    }

    @Suppress("DuplicatedCode")
    override fun handle(context: RoutingContext) {
        val uuidStr = context.pathParam("uuid")

        val uuid: UUID
        try {
            uuid = UUID.fromString(uuidStr)
        } catch (e: IllegalArgumentException) {
            context.response().setStatusCode(BAD_REQUEST.code()).end()
            return
        }

        bankAccounts.getBalance(uuid)
            .onSuccess {
                println(JsonObject().put("balance", it).toBuffer()) // todo remove
                context.response().putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .end(JsonObject().put("balance", it).toBuffer())
            }
            .onFailure(context::fail)
    }

}