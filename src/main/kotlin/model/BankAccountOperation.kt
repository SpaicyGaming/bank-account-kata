package model

import io.vertx.sqlclient.Row
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

/**
 * This class represents a bank account operation.
 */
data class BankAccountOperation(
    val id: UUID,
    val operation: Operation,
    val amount: BigDecimal,
    val time: LocalDateTime,
) {

    companion object {
        /**
         * This method creates a [BankAccountOperation] from a [Row].
         */
        @JvmStatic
        fun fromRow(row: Row): BankAccountOperation {
            val isUpperCase = row.getColumnIndex("ID") > -1

            return if (isUpperCase) {
                // H2
                BankAccountOperation(
                    row.getUUID("ID"),
                    Operation.fromAmount(row.getBigDecimal("AMOUNT")),
                    row.getBigDecimal("AMOUNT").abs(),
                    row.getLocalDateTime("INSERTED_AT")
                )
            } else {
                BankAccountOperation(
                    row.getUUID("id"),
                    Operation.fromAmount(row.getBigDecimal("amount")),
                    row.getBigDecimal("amount").abs(),
                    row.getLocalDateTime("inserted_at")
                )
            }
        }
    }

    /**
     * This enum represents the type of operation.
     */
    enum class Operation {
        DEPOSIT,
        WITHDRAWAL;

        companion object {
            @JvmStatic
            fun fromAmount(amount: BigDecimal): Operation {
                return if (amount > BigDecimal.ZERO) DEPOSIT else WITHDRAWAL
            }
        }
    }

}
