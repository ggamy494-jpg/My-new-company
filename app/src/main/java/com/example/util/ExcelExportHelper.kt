package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CustomerDebt
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExportHelper {

    /**
     * Generates a CSV file formatted with UTF-8 BOM so Microsoft Excel and Google Sheets
     * display Arabic text with 100% correct encoding and alignment.
     */
    fun exportDebtsToExcel(
        context: Context,
        debts: List<CustomerDebt>,
        title: String = "تقرير_أجل_عملاء_شركة_زينة"
    ): File? {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
            val file = File(exportDir, "${title}_$timeStamp.csv")

            val numberFormat = NumberFormat.getNumberInstance(Locale.ENGLISH).apply {
                isGroupingUsed = false
                maximumFractionDigits = 2
            }

            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))

            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                    // Write UTF-8 Byte Order Mark (BOM) for Excel Arabic support
                    writer.write("\uFEFF")

                    // Header Row
                    val headers = listOf(
                        "م",
                        "اسم العميل / التاجر",
                        "اسم المحل / الماركت",
                        "الشخص المسؤول",
                        "رقم الهاتف الأساسي",
                        "رقم الهاتف الإضافي",
                        "العنوان",
                        "إحداثيات GPS",
                        "رابط خرائط جوجل",
                        "إجمالي الآجل (ج.م)",
                        "المسدد (ج.م)",
                        "المتبقي (ج.م)",
                        "نسبة السداد (%)",
                        "تاريخ الاستحقاق",
                        "حالة المديونية",
                        "رقم الفاتورة",
                        "أصناف الفاتورة والكميات",
                        "ملاحظات البضاعة",
                        "ملاحظات العميل",
                        "عدد التذكيرات المرسلة"
                    )
                    writeCsvLine(writer, headers)

                    var totalAmountSum = 0.0
                    var totalPaidSum = 0.0
                    var totalRemainingSum = 0.0

                    // Rows
                    debts.forEachIndexed { index, debt ->
                        totalAmountSum += debt.totalAmount
                        totalPaidSum += debt.paidAmount
                        totalRemainingSum += debt.remainingAmount

                        val gpsCoords = if (debt.hasGpsLocation) {
                            "${debt.latitude}, ${debt.longitude}"
                        } else ""

                        val mapsUrl = if (debt.hasGpsLocation) {
                            "https://maps.google.com/?q=${debt.latitude},${debt.longitude}"
                        } else ""

                        val invoiceItemsSummary = if (debt.hasDetailedInvoice) {
                            debt.getInvoiceItems().joinToString(" | ") { item ->
                                "${item.name} (كمية: ${item.quantity} × ${item.unitPrice} ج.م = ${item.totalPrice} ج.م)"
                            }
                        } else ""

                        val progressPercent = (debt.paymentProgress * 100).toInt()

                        val row = listOf(
                            (index + 1).toString(),
                            debt.customerName,
                            debt.storeName,
                            debt.contactPerson,
                            debt.customerPhone,
                            debt.secondaryPhone,
                            debt.address,
                            gpsCoords,
                            mapsUrl,
                            numberFormat.format(debt.totalAmount),
                            numberFormat.format(debt.paidAmount),
                            numberFormat.format(debt.remainingAmount),
                            "$progressPercent%",
                            dateFormat.format(Date(debt.dueDate)),
                            debt.statusLabelArabic(),
                            debt.invoiceNumber,
                            invoiceItemsSummary,
                            debt.notes,
                            debt.customerNotes,
                            debt.reminderCount.toString()
                        )
                        writeCsvLine(writer, row)
                    }

                    // Empty row for separation
                    writeCsvLine(writer, listOf("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""))

                    // Totals summary row
                    val summaryRow = listOf(
                        "★ الإجمالي العام",
                        "عدد العملاء: ${debts.size}",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "المجاميع الكلية:",
                        numberFormat.format(totalAmountSum),
                        numberFormat.format(totalPaidSum),
                        numberFormat.format(totalRemainingSum),
                        if (totalAmountSum > 0) "${((totalPaidSum / totalAmountSum) * 100).toInt()}%" else "100%",
                        "تاريخ التقرير: ${dateFormat.format(Date())}",
                        "شركة زينة للتوريدات",
                        "",
                        "",
                        "",
                        "",
                        ""
                    )
                    writeCsvLine(writer, summaryRow)

                    writer.flush()
                }
            }

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Creates and shares the Excel CSV file via Android share sheet (WhatsApp, Drive, Email, etc.)
     */
    fun shareExcelFile(context: Context, debts: List<CustomerDebt>, customTitle: String? = null) {
        if (debts.isEmpty()) {
            Toast.makeText(context, "لا توجد أواجل لتصديرها", Toast.LENGTH_SHORT).show()
            return
        }

        val title = customTitle ?: "شيت_أواجل_عملاء_زينة"
        val file = exportDebtsToExcel(context, debts, title)

        if (file != null && file.exists()) {
            try {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "شيت إكسيل - أواجل عملاء شركة زينة")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "مرفق شيت إكسيل بأجل العملاء والتحصيلات - شركة زينة للتوريدات.\nإجمالي العملاء: ${debts.size}\nتاريخ الاستخراج: ${SimpleDateFormat("yyyy/MM/dd", Locale("ar")).format(Date())}"
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "مشاركة شيت الإكسيل عبر...")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "تعذر فتح نافذة المشاركة: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "حدث خطأ أثناء إنشاء ملف الإكسيل", Toast.LENGTH_SHORT).show()
        }
    }

    private fun writeCsvLine(writer: OutputStreamWriter, values: List<String>) {
        val line = values.joinToString(",") { escapeCsv(it) }
        writer.write(line)
        writer.write("\r\n")
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
        if (str.contains(",") || str.contains("\"") || str.contains(";")) {
            str = str.replace("\"", "\"\"")
            return "\"$str\""
        }
        return str
    }
}
