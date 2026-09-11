package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.model.CustomerDebt
import com.example.data.model.DebtStatus
import java.text.NumberFormat
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID = "collection_reminders_channel"
    private const val CHANNEL_NAME = "تنبيهات مواعيد التحصيل"
    private const val CHANNEL_DESCRIPTION = "إشعارات ذكية للتنبيه بمواعيد تحصيل المبالغ الآجلة والديون المستحقة"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showCollectionAlertNotification(
        context: Context,
        overdueCount: Int,
        dueTodayCount: Int,
        totalDueAmount: Double
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formatter = NumberFormat.getNumberInstance(Locale("ar"))
        val formattedAmount = formatter.format(totalDueAmount)

        val title = when {
            overdueCount > 0 && dueTodayCount > 0 -> "⚠️ تنبيه تحصيل: ديون متأخرة ومستحقة اليوم!"
            overdueCount > 0 -> "⚠️ تنبيه: لديك $overdueCount حسابات متأخرة السداد"
            dueTodayCount > 0 -> "🔔 تذكير: لديك $dueTodayCount حسابات مستحقة التحصيل اليوم"
            else -> "سجل الآجل: ملخص الحسابات"
        }

        val contentText = "إجمالي المبالغ المطلوب متابعتها: $formattedAmount ج.م ($overdueCount متأخر، $dueTodayCount اليوم)"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(1001, notification)
        } catch (_: SecurityException) {
            // Notifications permission not granted
        }
    }

    fun showCustomerDueNotification(context: Context, debt: CustomerDebt) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            debt.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formatter = NumberFormat.getNumberInstance(Locale("ar"))
        val amountStr = formatter.format(debt.remainingAmount)

        val statusText = when (debt.status) {
            DebtStatus.OVERDUE -> "متأخر السداد"
            DebtStatus.DUE_TODAY -> "مستحق التحصيل اليوم"
            DebtStatus.DUE_SOON -> "يستحق قريباً"
            else -> "مستحق"
        }

        val title = "تذكير تحصيل: ${debt.customerName} ($statusText)"
        val message = "المبلغ المتبقي: $amountStr ج.م - موعد السداد: ${debt.formattedDueDate()}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify((2000 + debt.id).toInt(), notification)
        } catch (_: SecurityException) {
            // Handled
        }
    }
}
