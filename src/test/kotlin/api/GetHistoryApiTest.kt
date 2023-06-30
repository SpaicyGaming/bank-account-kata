package api

import JsonModules
import io.mockk.every
import io.mockk.mockk
import io.netty.handler.codec.http.HttpResponseStatus.*
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import model.BankAccountOperation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import repository.BankAccounts
import java.time.LocalDateTime
import java.util.*

@ExtendWith(VertxExtension::class, JsonModules::class)
class GetHistoryApiTest {

    private val port = 9997
    private val bankAccounts: BankAccounts = mockk()

    @BeforeEach
    internal fun setUp(vertx: Vertx, setup: VertxTestContext) {
        val router = Router.router(vertx).errorHandler(INTERNAL_SERVER_ERROR.code()) { it.failure().printStackTrace() }
        GetHistoryApi(router, bankAccounts)
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port, setup.succeedingThenComplete())
    }

    @Test
    internal fun `should return bad request if the uuid param is invalid`() {
        RestAssured.given()
            .port(port)
            .contentType(ContentType.JSON)
            .get("/api/history/{uuid}", "not-a-uuid")
            .then()
            .statusCode(BAD_REQUEST.code())
    }

    @Test
    internal fun `should return transaction history`() {
        val uuid = UUID.randomUUID()

        val operation1 = BankAccountOperation(
            uuid,
            BankAccountOperation.Operation.DEPOSIT,
            100.toBigDecimal(),
            LocalDateTime.now().minusDays(2)
        )
        val operation2 = BankAccountOperation(
            uuid,
            BankAccountOperation.Operation.WITHDRAWAL,
            50.toBigDecimal(),
            LocalDateTime.now().minusDays(1)
        )
        val operation3 =
            BankAccountOperation(uuid, BankAccountOperation.Operation.DEPOSIT, 100.toBigDecimal(), LocalDateTime.now())
        val operations = listOf(operation1, operation2, operation3)

        every { bankAccounts.history(any()) } returns Future.succeededFuture(operations)

        RestAssured.given()
            .port(port)
            .contentType(ContentType.JSON)
            .get("/api/history/{uuid}", uuid)
            .then()
            .contentType("application/json")
            .statusCode(OK.code())
            .extract().body().asString().also {
                assertThat(it).isEqualTo(
                    JsonArray(operations.map(JsonObject::mapFrom)).toString()
                )
            }
    }

}