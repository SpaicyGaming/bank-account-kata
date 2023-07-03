package api

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST
import io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import repository.BankAccounts
import java.util.*


/**
 * This class is responsible for handling GET requests to the /api/history/:uuid endpoint.
 * It will return a JSON array of all the transactions for the given UUID.
 */
class GetHistoryApi(router: Router, private val bankAccounts: BankAccounts) : Handler<RoutingContext> {

    init {
        router.get("/api/history/:uuid").handler(this)
    }

    override fun handle(context: RoutingContext) {
        val uuidStr = context.pathParam("uuid")

        val uuid: UUID
        try {
            uuid = UUID.fromString(uuidStr)
        } catch (e: IllegalArgumentException) {
            context.response().setStatusCode(BAD_REQUEST.code()).end()
            return
        }

        bankAccounts.history(uuid)
            .onSuccess {
                if (it.isEmpty()) {
                    context.response().setStatusCode(NOT_FOUND.code()).end()
                } else {
                    context.response()
                        .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                        .end(JsonArray(it.map(JsonObject::mapFrom)).toBuffer())
                }
            }
            .onFailure(context::fail)
    }

}