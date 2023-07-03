package repository

import io.vertx.core.Future
import io.vertx.junit5.VertxTestContext
import model.BankAccountOperation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

/**
 * This class is responsible for testing the [PostgresBankAccounts] class.
 */
open class PostgresBankAccountsTest : PostgresTest() {

    private lateinit var bankAccounts: BankAccounts

    override fun setup(): Future<Unit> {
        bankAccounts = BankAccounts.fromToPostgres(sqlClient)
        return Future.succeededFuture()
    }

    @Test
    fun `should deposit in account`(test: VertxTestContext) {
        val uuid = UUID.randomUUID()

        bankAccounts.deposit(uuid, BigDecimal.valueOf(100.00))
            .onSuccess {
                assertThat(it.id).isEqualTo(uuid)
                assertThat(it.amount).isEqualByComparingTo(BigDecimal.valueOf(100.00))
                assertThat(it.operation).isEqualTo(BankAccountOperation.Operation.DEPOSIT)
                test.completeNow()
            }
            .onFailure(test::failNow)
    }

    @Test
    fun `should not be able to withdraw`(test: VertxTestContext) {
        val uuid = UUID.randomUUID()

        bankAccounts.withdraw(uuid, BigDecimal.valueOf(100.00))
            .onSuccess {
                assertThat(it).isNull()
                test.completeNow()
            }.onFailure(test::failNow)
    }

    @Test
    fun `should not be able to withdraw more than what was deposited`(test: VertxTestContext) {
        val uuid = UUID.randomUUID()

        bankAccounts.deposit(uuid, BigDecimal.valueOf(100.00))
            .compose {
                bankAccounts.withdraw(uuid, BigDecimal.valueOf(101.00))
            }
            .onSuccess {
                assertThat(it).isNull()
                test.completeNow()
            }.onFailure(test::failNow)
    }


    @Test
    fun `should calculate balance based on operations`(test: VertxTestContext) {
        val uuid = UUID.randomUUID()

        bankAccounts.deposit(uuid, BigDecimal.valueOf(100.00))
            .compose {
                bankAccounts.withdraw(uuid, BigDecimal.valueOf(70.00))
            }.compose {
                bankAccounts.deposit(uuid, BigDecimal.valueOf(10.00))
            }.compose {
                bankAccounts.getBalance(uuid)
            }.onSuccess {
                assertThat(it).isEqualByComparingTo(BigDecimal.valueOf(40.00))
                test.completeNow()
            }.onFailure(test::failNow)
    }

    @Test
    fun `should see operation history`(test: VertxTestContext) {
        val uuid = UUID.randomUUID()

        bankAccounts.deposit(uuid, BigDecimal.valueOf(100.00))
            .compose {
                bankAccounts.withdraw(uuid, BigDecimal.valueOf(70.00))
            }.compose {
                bankAccounts.deposit(uuid, BigDecimal.valueOf(10.00))
            }.compose {
                bankAccounts.history(uuid)
            }.onSuccess { operations ->
                assertThat(operations).hasSize(3)
                operations.forEach { assertThat(it.id).isEqualTo(uuid) }

                val secondDeposit = operations[0]
                assertThat(secondDeposit.amount).isEqualByComparingTo(BigDecimal.valueOf(10.00))
                assertThat(secondDeposit.operation).isEqualTo(BankAccountOperation.Operation.DEPOSIT)

                val firstWithdrawal = operations[1]
                assertThat(firstWithdrawal.amount).isEqualByComparingTo(BigDecimal.valueOf(70.00))
                assertThat(firstWithdrawal.operation).isEqualTo(BankAccountOperation.Operation.WITHDRAWAL)

                val firstDeposit = operations[2]
                assertThat(firstDeposit.amount).isEqualByComparingTo(BigDecimal.valueOf(100.00))
                assertThat(firstDeposit.operation).isEqualTo(BankAccountOperation.Operation.DEPOSIT)

                test.completeNow()
            }.onFailure(test::failNow)
    }

}
