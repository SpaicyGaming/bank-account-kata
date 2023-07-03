package api

import io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.vertx.core.Handler
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import repository.BankAccounts
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * This class is responsible for handling POST requests to the /api/withdraw endpoint.
 * It will withdraw the given amount from the given UUID's account.
 * It expects a JSON body with the following format:
 * {
 * "amount": 100.00,
 * "uuid": "00000000-0000-0000-0000-000000000000"
 * }
 */
class PostWithdrawApi(router: Router, private val bankAccounts: BankAccounts) : Handler<RoutingContext> {

    init {
        router.post("/api/withdraw")
            .handler(BodyHandler.create())
            .handler(this)
    }

    @Suppress("DuplicatedCode")
    override fun handle(context: RoutingContext) {
        val body = context.body().asJsonObject()

        if (context.body().isEmpty || body.getDouble("amount") == null || body.getString("uuid") == null) {
            context.response().setStatusCode(BAD_REQUEST.code()).end()
            return
        }

        val amount: BigDecimal
        val uuid: UUID
        try {
            amount = BigDecimal.valueOf(body.getDouble("amount")).setScale(2, RoundingMode.FLOOR)
            uuid = UUID.fromString(body.getString("uuid"))
        } catch (e: Exception) {
            context.response().setStatusCode(BAD_REQUEST.code()).end()
            return
        }

        bankAccounts.withdraw(uuid, amount)
            .onSuccess {
                context.response().setStatusCode(OK.code()).end()
            }
            .onFailure(context::fail)
    }

}