package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CustomerDebt
import com.example.data.model.PaymentRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupPayload(
    val exportDate: Long,
    val senderName: String,
    val debts: List<CustomerDebt>,
    val payments: List<PaymentRecord>
)

object DataBackupHelper {

    /**
     * Serializes all debts and payment records into a JSON string.
     */
    fun buildBackupJson(
        senderName: String,
        debts: List<CustomerDebt>,
        payments: List<PaymentRecord>
    ): String {
        val root = JSONObject()
        root.put("app", "ZinaAjelTracker")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("senderName", senderName)

        val debtsArray = JSONArray()
        for (d in debts) {
            val debtObj = JSONObject().apply {
                put("id", d.id)
                put("customerName", d.customerName)
                put("storeName", d.storeName)
                put("contactPerson", d.contactPerson)
                put("customerPhone", d.customerPhone)
                put("secondaryPhone", d.secondaryPhone)
                put("address", d.address)
                d.latitude?.let { put("latitude", it) }
                d.longitude?.let { put("longitude", it) }
                put("totalAmount", d.totalAmount)
                put("paidAmount", d.paidAmount)
                put("dueDate", d.dueDate)
                put("createdDate", d.createdDate)
                put("invoiceNumber", d.invoiceNumber)
                put("invoiceItemsJson", d.invoiceItemsJson)
                put("notes", d.notes)
                put("customerNotes", d.customerNotes)
                put("reminderCount", d.reminderCount)
                d.lastReminderDate?.let { put("lastReminderDate", it) }
            }
            debtsArray.put(debtObj)
        }
        root.put("debts", debtsArray)

        val paymentsArray = JSONArray()
        for (p in payments) {
            val paymentObj = JSONObject().apply {
                put("id", p.id)
                put("debtId", p.debtId)
                put("amount", p.amount)
                put("paymentDate", p.paymentDate)
                put("note", p.note)
            }
            paymentsArray.put(paymentObj)
        }
        root.put("payments", paymentsArray)

        return root.toString(2)
    }

    /**
     * Creates a .ajel backup file and opens Android share dialog to send to WhatsApp, Drive, Bluetooth, etc.
     */
    fun shareBackupFile(
        context: Context,
        senderName: String,
        debts: List<CustomerDebt>,
        payments: List<PaymentRecord>
    ) {
        if (debts.isEmpty()) {
            Toast.makeText(context, "لا توجد بيانات لمشاركتها", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val json = buildBackupJson(senderName, debts, payments)
            val dir = File(context.cacheDir, "backups")
            if (!dir.exists()) dir.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val file = File(dir, "بيانات_أواجل_زينة_$timeStamp.ajel")

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    writer.write(json)
                    writer.flush()
                }
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "ملف نقل بيانات أواجل شركة زينة ($senderName)")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "مرفق ملف نقل بيانات الأواجل والتحصيلات لبرنامج زينة.\nالمرسل: $senderName\nعدد العملاء: ${debts.size}\nتاريخ الإرسال: ${SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(Date())}\n\nيمكن للطرف الآخر فتح التطبيق والضغط على 'استيراد بيانات' لتحميلها مباشرة."
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "إرسال ملف البيانات إلى...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ في مشاركة ملف البيانات: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Parses a backup JSON string into strongly typed objects.
     */
    fun parseBackupJson(jsonString: String): BackupPayload? {
        return try {
            val root = JSONObject(jsonString)
            val exportDate = root.optLong("timestamp", System.currentTimeMillis())
            val senderName = root.optString("senderName", "مستخدم آخر")

            val debtsArray = root.getJSONArray("debts")
            val debtsList = mutableListOf<CustomerDebt>()

            for (i in 0 until debtsArray.length()) {
                val obj = debtsArray.getJSONObject(i)
                val debt = CustomerDebt(
                    id = obj.optLong("id", 0L),
                    customerName = obj.optString("customerName", ""),
                    storeName = obj.optString("storeName", ""),
                    contactPerson = obj.optString("contactPerson", ""),
                    customerPhone = obj.optString("customerPhone", ""),
                    secondaryPhone = obj.optString("secondaryPhone", ""),
                    address = obj.optString("address", ""),
                    latitude = if (obj.has("latitude")) obj.optDouble("latitude") else null,
                    longitude = if (obj.has("longitude")) obj.optDouble("longitude") else null,
                    totalAmount = obj.optDouble("totalAmount", 0.0),
                    paidAmount = obj.optDouble("paidAmount", 0.0),
                    dueDate = obj.optLong("dueDate", System.currentTimeMillis()),
                    createdDate = obj.optLong("createdDate", System.currentTimeMillis()),
                    invoiceNumber = obj.optString("invoiceNumber", ""),
                    invoiceItemsJson = obj.optString("invoiceItemsJson", ""),
                    notes = obj.optString("notes", ""),
                    customerNotes = obj.optString("customerNotes", ""),
                    reminderCount = obj.optInt("reminderCount", 0),
                    lastReminderDate = if (obj.has("lastReminderDate")) obj.optLong("lastReminderDate") else null
                )
                debtsList.add(debt)
            }

            val paymentsArray = root.optJSONArray("payments")
            val paymentsList = mutableListOf<PaymentRecord>()
            if (paymentsArray != null) {
                for (i in 0 until paymentsArray.length()) {
                    val pObj = paymentsArray.getJSONObject(i)
                    val p = PaymentRecord(
                        id = pObj.optLong("id", 0L),
                        debtId = pObj.optLong("debtId", 0L),
                        amount = pObj.optDouble("amount", 0.0),
                        paymentDate = pObj.optLong("paymentDate", System.currentTimeMillis()),
                        note = pObj.optString("note", "")
                    )
                    paymentsList.add(p)
                }
            }

            BackupPayload(
                exportDate = exportDate,
                senderName = senderName,
                debts = debtsList,
                payments = paymentsList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reads text content from a content Uri (e.g. from file picker).
     */
    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
