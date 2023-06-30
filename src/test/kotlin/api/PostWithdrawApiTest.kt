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
class PostWithdrawApiTest {

    private val port = 9998
    private val bankAccounts: BankAccounts = mockk()

    @BeforeEach
    internal fun setUp(vertx: Vertx, setup: VertxTestContext) {
        val router = Router.router(vertx).errorHandler(INTERNAL_SERVER_ERROR.code()) { it.failure().printStackTrace() }
        PostWithdrawApi(router, bankAccounts)
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(port, setup.succeedingThenComplete())
    }

    @Test
    internal fun `should withdraw money into account`() {
        val uuid = UUID.randomUUID()
        val amount = BigDecimal.valueOf(100).setScale(2)
        val operation = BankAccountOperation(
            uuid,
            BankAccountOperation.Operation.WITHDRAWAL,
            amount,
            LocalDateTime.now()
        )

        every { bankAccounts.withdraw(any(), any(), any()) } returns Future.succeededFuture(operation)

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
            .post("/api/withdraw")
            .then()
            .statusCode(OK.code())

        verify(exactly = 1) { bankAccounts.withdraw(uuid, amount, any()) }
    }

    @Test
    internal fun `should return bad request if body is empty`() {
        every { bankAccounts.withdraw(any(), any(), any()) } returns Future.succeededFuture()

        given()
            .port(port)
            .contentType(ContentType.JSON)
            .post("/api/withdraw")
            .then()
            .statusCode(BAD_REQUEST.code())
    }

    @Test
    internal fun `should return error if withdrawal fails`() {
        every { bankAccounts.withdraw(any(), any(), any()) } returns Future.failedFuture("error")

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
            .post("/api/withdraw")
            .then()
            .statusCode(INTERNAL_SERVER_ERROR.code())
    }

}