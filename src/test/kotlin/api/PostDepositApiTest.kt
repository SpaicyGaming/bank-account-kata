package api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.netty.handler.codec.http.HttpResponseStatus.*
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import model.BankAccountOperation
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import repository.BankAccounts
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@ExtendWith(VertxExtension::class)
class PostDepositApiTest {

    private val port = 9999
    private val bankAccounts: BankAccounts = mockk()

    @BeforeEach
    internal fun setUp(vertx: Vertx, setup: VertxTestContext) {
        val router = Router.router(vertx).errorHandler(INTERNAL_SERVER_ERROR.code()) { it.failure().printStackTrace() }
        PostDepositApi(router, bankAccounts)
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port, setup.succeedingThenComplete())
    }

    @Test
    fun `should deposit money into account`() {
        val uuid = UUID.randomUUID()
        val amount = BigDecimal.valueOf(100).setScale(2)
        val operation = BankAccountOperation(
            uuid,
            BankAccountOperation.Operation.DEPOSIT,
            amount,
            LocalDateTime.now()
        )

        every { bankAccounts.deposit(any(), any(), any()) } returns Future.succeededFuture(operation)

        given()
            .port(port)
            .contentType(ContentType.JSON)
            .body(
                """{
                "uuid":"$uuid",
                "amount":$amount
                }
            """.trimIndent()
            )
            .post("/api/deposit")
            .then()
            .statusCode(OK.code())

        verify(exactly = 1) { bankAccounts.deposit(uuid, amount, any()) }
    }

    @Test
    internal fun `should return bad request if body is empty`() {
        every { bankAccounts.deposit(any(), any(), any()) } returns Future.failedFuture("error")

        given()
            .port(port)
            .contentType(ContentType.JSON)
            .post("/api/deposit")
            .then()
            .statusCode(BAD_REQUEST.code())
    }

    @Test
    internal fun `should return error if deposit fails`() {
        every { bankAccounts.deposit(any(), any(), any()) } returns Future.failedFuture("error")

        given()
            .port(port)
            .contentType(ContentType.JSON)
            .body(
                """{
                "uuid":"${UUID.randomUUID()}",
                "amount":100.00
                }
            """.trimIndent()
            )
            .post("/api/deposit")
            .then()
            .statusCode(INTERNAL_SERVER_ERROR.code())
    }

}