package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CustomerDebt
import com.example.data.model.PaymentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AjelDao {

    @Query("SELECT * FROM customer_debts ORDER BY dueDate ASC")
    fun getAllDebts(): Flow<List<CustomerDebt>>

    @Query("SELECT * FROM customer_debts WHERE id = :id")
    suspend fun getDebtById(id: Long): CustomerDebt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: CustomerDebt): Long

    @Update
    suspend fun updateDebt(debt: CustomerDebt)

    @Delete
    suspend fun deleteDebt(debt: CustomerDebt)

    @Query("DELETE FROM customer_debts WHERE id = :id")
    suspend fun deleteDebtById(id: Long)

    @Query("UPDATE customer_debts SET paidAmount = :newPaidAmount WHERE id = :debtId")
    suspend fun updatePaidAmount(debtId: Long, newPaidAmount: Double)

    @Query("UPDATE customer_debts SET reminderCount = reminderCount + 1, lastReminderDate = :timestamp WHERE id = :debtId")
    suspend fun markReminderSent(debtId: Long, timestamp: Long)

    // Payments
    @Query("SELECT * FROM payment_records WHERE debtId = :debtId ORDER BY paymentDate DESC")
    fun getPaymentsForDebt(debtId: Long): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM customer_debts")
    suspend fun getAllDebtsSnapshot(): List<CustomerDebt>

    @Query("SELECT * FROM payment_records")
    suspend fun getAllPaymentsSnapshot(): List<PaymentRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<CustomerDebt>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<PaymentRecord>): List<Long>

    @Query("DELETE FROM payment_records WHERE id = :id")
    suspend fun deletePayment(id: Long)

    @Query("DELETE FROM payment_records")
    suspend fun deleteAllPayments()

    @Query("DELETE FROM customer_debts")
    suspend fun deleteAllDebts()
}
