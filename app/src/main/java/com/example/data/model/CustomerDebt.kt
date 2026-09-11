package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class DebtStatus {
    OVERDUE,     // متأخر السداد
    DUE_TODAY,   // مستحق اليوم
    DUE_SOON,    // مستحق قريباً (خلال 3 أيام)
    UPCOMING,    // قادم
    SETTLED      // تم السداد بالكامل
}

data class InvoiceItem(
    val name: String,
    val quantity: Double,
    val unitPrice: Double
) {
    val totalPrice: Double
        get() = quantity * unitPrice
}

@Entity(tableName = "customer_debts")
data class CustomerDebt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val customerName: String,               // اسم العميل أو التاجر
    val storeName: String = "",             // اسم المحل / السوبرماركت
    val contactPerson: String = "",         // الشخص المسؤول
    val customerPhone: String,              // رقم الهاتف الأساسي / واتساب
    val secondaryPhone: String = "",        // رقم هاتف إضافي
    val address: String = "",               // العنوان ومقر العميل
    val latitude: Double? = null,           // خط العرض GPS
    val longitude: Double? = null,          // خط الطول GPS
    val totalAmount: Double,                // إجمالي مبلغ الآجل
    val paidAmount: Double = 0.0,           // المبلغ المسدد
    val dueDate: Long,                      // تاريخ الاستحقاق / موعد التحصيل
    val createdDate: Long = System.currentTimeMillis(),
    val invoiceNumber: String = "",         // رقم الفاتورة
    val invoiceItemsJson: String = "",      // تفاصيل بنود الفاتورة (أصناف وأسعار)
    val notes: String = "",                 // ملاحظات الفاتورة أو البضاعة
    val customerNotes: String = "",         // معلومات إضافية عن العميل وطريقة السداد
    val reminderCount: Int = 0,
    val lastReminderDate: Long? = null
) {
    val remainingAmount: Double
        get() = (totalAmount - paidAmount).coerceAtLeast(0.0)

    val isSettled: Boolean
        get() = remainingAmount <= 0.001

    val paymentProgress: Float
        get() = if (totalAmount > 0) ((paidAmount / totalAmount).coerceIn(0.0, 1.0)).toFloat() else 1f

    val hasGpsLocation: Boolean
        get() = latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0

    val hasDetailedInvoice: Boolean
        get() = invoiceItemsJson.isNotBlank() && getInvoiceItems().isNotEmpty()

    val status: DebtStatus
        get() {
            if (isSettled) return DebtStatus.SETTLED

            val todayCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = todayCalendar.timeInMillis

            val dueCalendar = Calendar.getInstance().apply {
                timeInMillis = dueDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dueStart = dueCalendar.timeInMillis

            val diffMillis = dueStart - todayStart
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

            return when {
                diffDays < 0 -> DebtStatus.OVERDUE
                diffDays == 0L -> DebtStatus.DUE_TODAY
                diffDays in 1..3 -> DebtStatus.DUE_SOON
                else -> DebtStatus.UPCOMING
            }
        }

    val daysDifferenceFromToday: Long
        get() {
            val todayCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = todayCalendar.timeInMillis

            val dueCalendar = Calendar.getInstance().apply {
                timeInMillis = dueDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dueStart = dueCalendar.timeInMillis
            return TimeUnit.MILLISECONDS.toDays(dueStart - todayStart)
        }

    fun formattedDueDate(): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
        return sdf.format(Date(dueDate))
    }

    fun statusLabelArabic(): String {
        return when (status) {
            DebtStatus.OVERDUE -> {
                val daysAgo = -daysDifferenceFromToday
                if (daysAgo == 1L) "متأخر منذ يوم" else "متأخر منذ $daysAgo أيام"
            }
            DebtStatus.DUE_TODAY -> "مستحق التحصيل اليوم"
            DebtStatus.DUE_SOON -> {
                val daysLeft = daysDifferenceFromToday
                if (daysLeft == 1L) "مستحق غداً" else "مستحق بعد $daysLeft أيام"
            }
            DebtStatus.UPCOMING -> "قادم (${formattedDueDate()})"
            DebtStatus.SETTLED -> "تم السداد بالكامل"
        }
    }

    fun getInvoiceItems(): List<InvoiceItem> {
        if (invoiceItemsJson.isBlank()) return emptyList()
        val items = mutableListOf<InvoiceItem>()
        try {
            val array = JSONArray(invoiceItemsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                items.add(
                    InvoiceItem(
                        name = obj.optString("name", ""),
                        quantity = obj.optDouble("quantity", 1.0),
                        unitPrice = obj.optDouble("unitPrice", 0.0)
                    )
                )
            }
        } catch (_: Exception) {
            // Ignored
        }
        return items
    }

    companion object {
        fun encodeInvoiceItems(items: List<InvoiceItem>): String {
            if (items.isEmpty()) return ""
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("name", item.name)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice)
                }
                array.put(obj)
            }
            return array.toString()
        }
    }
}
