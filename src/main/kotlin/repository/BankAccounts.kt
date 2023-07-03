package repository

import io.vertx.core.Future
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.templates.SqlTemplate
import model.BankAccountOperation
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * This class is responsible for managing bank accounts.
 */
sealed interface BankAccounts {

    companion object {
        @JvmStatic
        fun fromToH2(sqlClient: SqlClient): BankAccounts = H2BankAccounts(sqlClient)

        @JvmStatic
        fun fromToPostgres(sqlClient: SqlClient): BankAccounts = PostgresBankAccounts(sqlClient)
    }

    /**
     * Deposit [amount] in [bankAccountId] at [insertedAt].
     */
    fun deposit(
        bankAccountId: UUID,
        amount: BigDecimal,
        insertedAt: LocalDateTime = LocalDateTime.now()
    ): Future<BankAccountOperation>

    /**
     * Withdraw [amount] from [bankAccountId] at [insertedAt].
     * If the amount is greater than the balance, the operation will fail.
     */
    fun withdraw(
        bankAccountId: UUID,
        amount: BigDecimal,
        insertedAt: LocalDateTime = LocalDateTime.now()
    ): Future<BankAccountOperation?>

    /**
     * Get the history of operations for [bankAccountId].
     * The operations are ordered by [BankAccountOperation.time] in descending order.
     * The first operation is the most recent one.
     */
    fun history(bankAccountId: UUID): Future<List<BankAccountOperation>>

    /**
     * Get the balance of [bankAccountId].
     */
    fun getBalance(bankAccountId: UUID): Future<BigDecimal>

}

/**
 * This class is responsible for managing bank accounts in H2.
 */
private class H2BankAccounts(private val sqlClient: SqlClient) : BankAccounts {

    override fun deposit(
        bankAccountId: UUID,
        amount: BigDecimal,
        insertedAt: LocalDateTime
    ): Future<BankAccountOperation> {
        return insertRecord(bankAccountId, amount, BankAccountOperation.Operation.DEPOSIT, insertedAt)
            .map { record -> record ?: throw RuntimeException("Failed to deposit $amount") }
    }

    override fun withdraw(
        bankAccountId: UUID,
        amount: BigDecimal,
        insertedAt: LocalDateTime
    ): Future<BankAccountOperation?> {
        return getBalance(bankAccountId)
            .compose { balance ->
                if (balance >= amount) {
                    insertRecord(bankAccountId, amount, BankAccountOperation.Operation.WITHDRAWAL, insertedAt)
                } else {
                    Future.succeededFuture(null)
                }
            }
    }

    private fun insertRecord(
        bankAccountId: UUID,
        amount: BigDecimal,
        operation: BankAccountOperation.Operation,
        insertedAt: LocalDateTime
    ): Future<BankAccountOperation?> {
        val amountParam = if (operation == BankAccountOperation.Operation.WITHDRAWAL) amount.negate() else amount
        return sqlClient.preparedQuery("insert into accounts (id, amount, inserted_at) values (?, ?, ?)")
            .execute(Tuple.of(bankAccountId, amountParam, insertedAt))
            .map(
                BankAccountOperation(
                    bankAccountId,
                    operation,
                    amount,
                    insertedAt
                )
            ).onFailure(Throwable::printStackTrace)
    }

    override fun history(bankAccountId: UUID): Future<List<BankAccountOperation>> {
        return sqlClient.preparedQuery("select * from accounts where id = ? order by inserted_at desc")
            .execute(Tuple.of(bankAccountId))
            .map { rows -> rows.map { row -> BankAccountOperation.fromRow(row) } }
    }

    override fun getBalance(bankAccountId: UUID): Future<BigDecimal> {
        return sqlClient.preparedQuery("select sum(amount) as balance from accounts where id = ?")
            .execute(Tuple.of(bankAccountId))
            .map { rows ->
                rows.map { row -> row.getBigDecimal(0) }
                    .firstOrNull() ?: BigDecimal.ZERO
            }
    }
}

/**
 * This class is responsible for managing bank accounts in Postgres.
 */
private class PostgresBankAccounts(private val sqlClient: SqlClient) : BankAccounts {

    override fun deposit(
        bankAccountId: UUID,
        amount: BigDecimal,
        insertedAt: LocalDateTime
    ): Future<BankAccountOperation> {
        return insertRecord(bankAccountId, amount, BankAccountOperation.Operation.DEPOSIT, insertedAt)
            .map { record -> record ?: throw RuntimeException("Failed to deposit $amount") }
    }

    override fun withdraw(
        bankAccountId: UUID,
        amount: BigDecimal,
        insertedAt: LocalDateTime
    ): Future<BankAccountOperation?> {
        return getBalance(bankAccountId)
            .compose { balance ->
                if (balance >= amount) {
                    insertRecord(bankAccountId, amount, BankAccountOperation.Operation.WITHDRAWAL, insertedAt)
                } else {
                    Future.succeededFuture(null)
                }
            }
    }

    private fun insertRecord(
        bankAccountId: UUID,
        amount: BigDecimal,
        operation: BankAccountOperation.Operation,
        insertedAt: LocalDateTime
    ): Future<BankAccountOperation?> {
        val amountParam = if (operation == BankAccountOperation.Operation.WITHDRAWAL) amount.negate() else amount
        return SqlTemplate.forUpdate(
            sqlClient,
            "insert into accounts(id, amount, inserted_at) values (#{id}, #{amount}, #{inserted_at})"
        )
            .execute(
                mapOf(
                    "id" to bankAccountId,
                    "amount" to amountParam,
                    "inserted_at" to insertedAt
                )
            )
            .map(
                BankAccountOperation(
                    bankAccountId,
                    operation,
                    amount,
                    insertedAt
                )
            )
    }

    override fun history(bankAccountId: UUID): Future<List<BankAccountOperation>> {
        return SqlTemplate.forQuery(
            sqlClient,
            "select * from accounts where id = #{id} order by inserted_at desc"
        )
            .execute(mapOf("id" to bankAccountId))
            .map { rows -> rows.map { row -> BankAccountOperation.fromRow(row) } }
    }

    override fun getBalance(bankAccountId: UUID): Future<BigDecimal> {
        return SqlTemplate.forQuery(
            sqlClient,
            "select sum(amount) as balance from accounts where id = #{id}"
        )
            .execute(mapOf("id" to bankAccountId))
            .map { rows ->
                rows.map { row -> row.getBigDecimal(0) }
                    .firstOrNull() ?: BigDecimal.ZERO
            }
    }

}
