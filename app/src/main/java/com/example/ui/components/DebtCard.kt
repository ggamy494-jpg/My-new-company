package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerDebt
import com.example.data.model.DebtStatus
import com.example.ui.theme.StatusDueSoon
import com.example.ui.theme.StatusDueSoonContainer
import com.example.ui.theme.StatusDueToday
import com.example.ui.theme.StatusDueTodayContainer
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusOverdueContainer
import com.example.ui.theme.StatusSettled
import com.example.ui.theme.StatusSettledContainer
import com.example.util.GpsLocationHelper
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DebtCard(
    debt: CustomerDebt,
    onReminderClick: (CustomerDebt) -> Unit,
    onRecordPaymentClick: (CustomerDebt) -> Unit,
    onHistoryClick: (CustomerDebt) -> Unit,
    onInvoiceItemsClick: (CustomerDebt) -> Unit,
    onEditClick: (CustomerDebt) -> Unit,
    onDeleteClick: (CustomerDebt) -> Unit,
    onCallClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formatter = NumberFormat.getNumberInstance(Locale("ar"))
    var showMenu by remember { mutableStateOf(false) }

    val (statusColor, statusBg) = when (debt.status) {
        DebtStatus.OVERDUE -> Pair(StatusOverdue, StatusOverdueContainer)
        DebtStatus.DUE_TODAY -> Pair(StatusDueToday, StatusDueTodayContainer)
        DebtStatus.DUE_SOON -> Pair(StatusDueSoon, StatusDueSoonContainer)
        DebtStatus.UPCOMING -> Pair(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
        DebtStatus.SETTLED -> Pair(StatusSettled, StatusSettledContainer)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("debt_card_${debt.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Avatar + Name & Phone + Status Badge + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle with first letter
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = (debt.storeName.ifBlank { debt.customerName }).trim().firstOrNull()?.toString() ?: "ز"
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (debt.storeName.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = debt.storeName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (debt.customerPhone.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = debt.customerPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { onCallClick(debt.customerPhone) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "اتصال بالعميل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = debt.statusLabelArabic(),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }

                // Options dropdown menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات إضافية",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (debt.hasDetailedInvoice) {
                            DropdownMenuItem(
                                text = { Text("عرض أصناف الفاتورة") },
                                leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onInvoiceItemsClick(debt)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("سجل الدفعات والإيصالات") },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onHistoryClick(debt)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تعديل البيانات") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEditClick(debt)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("حذف الحساب", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteClick(debt)
                            }
                        )
                    }
                }
            }

            // Responsible person if exists
            if (debt.contactPerson.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "المسؤول: ${debt.contactPerson}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Location & GPS row
            if (debt.hasGpsLocation || debt.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .clickable {
                            if (debt.hasGpsLocation) {
                                GpsLocationHelper.openInGoogleMaps(
                                    context,
                                    debt.latitude!!,
                                    debt.longitude!!,
                                    debt.customerName
                                )
                            } else {
                                GpsLocationHelper.openInGoogleMapsByAddress(context, debt.address)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "موقع العميل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (debt.hasGpsLocation) {
                            if (debt.address.isNotBlank()) "${debt.address} (GPS 📍 خريطة)"
                            else "موقع العميل محدد بـ GPS (اضغط للفتح بالخريطة)"
                        } else {
                            "${debt.address} (اضغط للبحث في الخرائط)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Overview Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "المبلغ المتبقي للتحصيل",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatter.format(debt.remainingAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (debt.isSettled) StatusSettled else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ج.م",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "إجمالي الآجل: ${formatter.format(debt.totalAmount)} ج.م",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "المسدد: ${formatter.format(debt.paidAmount)} ج.م",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusSettled,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Payment Progress Bar
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { debt.paymentProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (debt.isSettled) StatusSettled else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Invoice details chip or button
            if (debt.hasDetailedInvoice) {
                Spacer(modifier = Modifier.height(8.dp))
                AssistChip(
                    onClick = { onInvoiceItemsClick(debt) },
                    label = {
                        Text(
                            text = "📦 تفاصيل الفاتورة (${debt.getInvoiceItems().size} أصناف - اضغط للعرض)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            // Customer notes / info
            if (debt.customerNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "معلومات العميل: ${debt.customerNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // General notes
            if (debt.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📝 ${debt.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Reminders info if sent before
            if (debt.reminderCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔔 تم إرسال ${debt.reminderCount} تذكيرات للعميل",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons (Smart Reminder & Record Payment)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!debt.isSettled) {
                    FilledTonalButton(
                        onClick = { onReminderClick(debt) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("smart_reminder_btn_${debt.id}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تذكير ذكي",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = { onRecordPaymentClick(debt) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("record_payment_btn_${debt.id}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تسجيل دفعة",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { onHistoryClick(debt) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("view_history_btn_${debt.id}"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "عرض إيصالات وسجل السداد")
                    }
                }
            }
        }
    }
}
