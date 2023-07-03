package api

import JsonModules
import io.mockk.every
import io.mockk.mockk
import io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.hamcrest.CoreMatchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import repository.BankAccounts
import java.math.BigDecimal
import java.util.*

@ExtendWith(VertxExtension::class, JsonModules::class)
class GetBalanceApiTest {

    private val port = 9996
    private val bankAccounts: BankAccounts = mockk()

    @BeforeEach
    internal fun setUp(vertx: Vertx, setup: VertxTestContext) {
        val router = Router.router(vertx).errorHandler(INTERNAL_SERVER_ERROR.code()) { it.failure().printStackTrace() }
        GetBalanceApi(router, bankAccounts)
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port, setup.succeedingThenComplete())
    }

    @Test
    fun `should return the balance`() {
        val uuid = UUID.randomUUID()

        every { bankAccounts.getBalance(uuid) } returns Future.succeededFuture(BigDecimal.TEN)

        given()
            .port(port)
            .contentType(ContentType.JSON)
            .get("/api/balance/{uuid}", uuid)
            .then()
            .contentType("application/json")
            .body("balance", `is`(10))
            .statusCode(OK.code())
    }

}