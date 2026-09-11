package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "payment_records",
    foreignKeys = [
        ForeignKey(
            entity = CustomerDebt::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["debtId"])]
)
data class PaymentRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val debtId: Long,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val note: String = ""
) {
    fun formattedDate(): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        return sdf.format(Date(paymentDate))
    }
}
