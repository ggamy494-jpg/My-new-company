package com.example.data.repository

import com.example.data.db.AjelDao
import com.example.data.model.CustomerDebt
import com.example.data.model.PaymentRecord
import kotlinx.coroutines.flow.Flow

class AjelRepository(private val ajelDao: AjelDao) {

    val allDebts: Flow<List<CustomerDebt>> = ajelDao.getAllDebts()

    suspend fun insertDebt(debt: CustomerDebt): Long {
        return ajelDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: CustomerDebt) {
        ajelDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: CustomerDebt) {
        ajelDao.deleteDebt(debt)
    }

    suspend fun deleteDebtById(id: Long) {
        ajelDao.deleteDebtById(id)
    }

    suspend fun recordPayment(
        debtId: Long,
        amount: Double,
        currentPaid: Double,
        note: String
    ): Long {
        val newPaid = currentPaid + amount
        ajelDao.updatePaidAmount(debtId, newPaid)
        val paymentRecord = PaymentRecord(
            debtId = debtId,
            amount = amount,
            paymentDate = System.currentTimeMillis(),
            note = note
        )
        return ajelDao.insertPayment(paymentRecord)
    }

    suspend fun markReminderSent(debtId: Long) {
        ajelDao.markReminderSent(debtId, System.currentTimeMillis())
    }

    fun getPaymentsForDebt(debtId: Long): Flow<List<PaymentRecord>> {
        return ajelDao.getPaymentsForDebt(debtId)
    }

    suspend fun getAllDebtsSnapshot(): List<CustomerDebt> {
        return ajelDao.getAllDebtsSnapshot()
    }

    suspend fun getAllPaymentsSnapshot(): List<PaymentRecord> {
        return ajelDao.getAllPaymentsSnapshot()
    }

    suspend fun clearAllData() {
        ajelDao.deleteAllPayments()
        ajelDao.deleteAllDebts()
    }

    suspend fun restoreFullDatabase(debts: List<CustomerDebt>, payments: List<PaymentRecord>) {
        clearAllData()
        ajelDao.insertDebts(debts)
        ajelDao.insertPayments(payments)
    }

    suspend fun mergeImportedData(newDebts: List<CustomerDebt>, newPayments: List<PaymentRecord>): Int {
        val existing = ajelDao.getAllDebtsSnapshot()
        var insertedOrUpdatedCount = 0

        for (newDebt in newDebts) {
            // Check if debt exists by name or phone or invoiceNumber
            val match = existing.find {
                (newDebt.customerPhone.isNotBlank() && it.customerPhone == newDebt.customerPhone) ||
                (newDebt.customerName.isNotBlank() && it.customerName.trim() == newDebt.customerName.trim()) ||
                (newDebt.invoiceNumber.isNotBlank() && it.invoiceNumber == newDebt.invoiceNumber)
            }

            val targetDebtId: Long
            if (match != null) {
                // Merge/update
                val updated = match.copy(
                    storeName = if (newDebt.storeName.isNotBlank()) newDebt.storeName else match.storeName,
                    contactPerson = if (newDebt.contactPerson.isNotBlank()) newDebt.contactPerson else match.contactPerson,
                    secondaryPhone = if (newDebt.secondaryPhone.isNotBlank()) newDebt.secondaryPhone else match.secondaryPhone,
                    address = if (newDebt.address.isNotBlank()) newDebt.address else match.address,
                    latitude = newDebt.latitude ?: match.latitude,
                    longitude = newDebt.longitude ?: match.longitude,
                    totalAmount = maxOf(match.totalAmount, newDebt.totalAmount),
                    paidAmount = maxOf(match.paidAmount, newDebt.paidAmount),
                    dueDate = newDebt.dueDate,
                    notes = if (newDebt.notes.isNotBlank()) newDebt.notes else match.notes,
                    customerNotes = if (newDebt.customerNotes.isNotBlank()) newDebt.customerNotes else match.customerNotes,
                    invoiceItemsJson = if (newDebt.invoiceItemsJson.isNotBlank()) newDebt.invoiceItemsJson else match.invoiceItemsJson
                )
                ajelDao.updateDebt(updated)
                targetDebtId = match.id
            } else {
                val toInsert = newDebt.copy(id = 0L)
                targetDebtId = ajelDao.insertDebt(toInsert)
                insertedOrUpdatedCount++
            }

            // Insert corresponding payments for this debt
            val relatedPayments = newPayments.filter { it.debtId == newDebt.id }
            for (p in relatedPayments) {
                ajelDao.insertPayment(p.copy(id = 0L, debtId = targetDebtId))
            }
        }
        return insertedOrUpdatedCount
    }
}
