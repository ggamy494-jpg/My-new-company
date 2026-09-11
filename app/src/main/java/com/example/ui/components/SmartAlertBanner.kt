package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.FinancialSummary
import com.example.ui.theme.StatusDueToday
import com.example.ui.theme.StatusOverdue
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SmartAlertBanner(
    summary: FinancialSummary,
    onNotifyClick: () -> Unit,
    onViewOverdueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalUrgent = summary.overdueCount + summary.dueTodayCount
    val formatter = NumberFormat.getNumberInstance(Locale("ar"))
    val totalAmount = summary.totalOverdue + summary.dueTodayAmount

    AnimatedVisibility(
        visible = totalUrgent > 0,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("smart_alert_banner"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (summary.overdueCount > 0)
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (summary.overdueCount > 0) StatusOverdue.copy(alpha = 0.15f)
                                else StatusDueToday.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "تنبيه ذكي",
                            tint = if (summary.overdueCount > 0) StatusOverdue else StatusDueToday,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تنبيه ذكي بمواعيد التحصيل",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = buildString {
                                if (summary.overdueCount > 0) append("${summary.overdueCount} متأخرة ")
                                if (summary.overdueCount > 0 && summary.dueTodayCount > 0) append("• ")
                                if (summary.dueTodayCount > 0) append("${summary.dueTodayCount} مستحقة اليوم ")
                                append("(إجمالي ${formatter.format(totalAmount)} ج.م)")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNotifyClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_system_notification_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إشعار النظام",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedButton(
                        onClick = onViewOverdueClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_overdue_filter_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "عرض المستحقين",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
