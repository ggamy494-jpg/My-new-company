package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.CustomerDebt
import com.example.data.model.DebtStatus
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

enum class ReminderTone {
    FRIENDLY,   // ودي لطيف
    FORMAL,     // رسمي تجاري
    URGENT      // تذكير عاجل بالمتأخرات
}

object SmartReminderHelper {

    fun generateMessage(debt: CustomerDebt, tone: ReminderTone = ReminderTone.FRIENDLY): String {
        val formatter = NumberFormat.getNumberInstance(Locale("ar"))
        val remainingStr = formatter.format(debt.remainingAmount)
        val dueDateStr = debt.formattedDueDate()

        return when (tone) {
            ReminderTone.FRIENDLY -> {
                "السلام عليكم أخي الكريم ${debt.customerName}،\n" +
                "نود تذكيركم بموعد سداد الحساب الآجل المستحق بقيمة $remainingStr ج.م بتاريخ $dueDateStr.\n" +
                "شاكرين لكم حسن تعاونكم الدائم وتواصلكم الراقي معنا."
            }
            ReminderTone.FORMAL -> {
                "السادة / ${debt.customerName} المحترمون،\n" +
                "تحية طيبة وبعد، نود إحاطتكم علماً بأن موعد تسوية الفاتورة بمبلغ $remainingStr ج.م يستحق بتاريخ $dueDateStr.\n" +
                "نرجو التكرم بالترتيب للسداد في الموعد المحدد. وتفضلوا بقبول فائق الاحترام والتقدير."
            }
            ReminderTone.URGENT -> {
                "السلام عليكم ورحمة الله،\n" +
                "الأخ الفاضل ${debt.customerName}، نلفت عنايتكم الكريمة إلى أن الحساب الآجل المستحق بقيمة $remainingStr ج.م قد حل موعد سداده (${debt.statusLabelArabic()}).\n" +
                "يرجى التكرم بتسوية المبلغ في أقرب وقت لتفادي أي تأخير. شكراً لاهتمامكم."
            }
        }
    }

    private fun sanitizePhoneNumber(phone: String): String {
        var clean = phone.replace("[^0-9+]".toRegex(), "")
        if (clean.startsWith("00")) {
            clean = "+" + clean.substring(2)
        } else if (clean.startsWith("01") && clean.length == 11) {
            // Common Egyptian local mobile starting with 01X -> add +2
            clean = "+2$clean"
        }
        return clean
    }

    fun openWhatsApp(context: Context, debt: CustomerDebt, message: String) {
        try {
            val phone = sanitizePhoneNumber(debt.customerPhone)
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val url = if (phone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$phone&text=$encodedMessage"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMessage"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب. تأكد من تثبيته على الجهاز.", Toast.LENGTH_LONG).show()
        }
    }

    fun sendSms(context: Context, debt: CustomerDebt, message: String) {
        try {
            val phone = sanitizePhoneNumber(debt.customerPhone)
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", message)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الرسائل النصية.", Toast.LENGTH_SHORT).show()
        }
    }

    fun callCustomer(context: Context, phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${sanitizePhoneNumber(phone)}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح لوحة الاتصال.", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("نص التذكير بالتحصيل", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ نص التذكير بنجاح", Toast.LENGTH_SHORT).show()
    }
}
