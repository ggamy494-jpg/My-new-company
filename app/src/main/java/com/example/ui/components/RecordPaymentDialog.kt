package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CustomerDebt
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RecordPaymentDialog(
    debt: CustomerDebt,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Double, note: String) -> Unit
) {
    val formatter = NumberFormat.getNumberInstance(Locale("ar"))
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("نقداً") }
    var errorMessage by remember { mutableStateOf("") }

    val quickNotes = listOf("نقداً", "InstaPay", "فودافون كاش", "تحويل بنكي", "شيك")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("record_payment_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تسجيل دفعة سداد",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Info card for current debt status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "العميل: ${debt.customerName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "المتبقي حالياً: ${formatter.format(debt.remainingAmount)} ج.م",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "المسدد سابقاً: ${formatter.format(debt.paidAmount)} ج.م",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fast Pay Full Balance Button
                OutlinedButton(
                    onClick = {
                        val rem = debt.remainingAmount
                        amountStr = if (rem % 1.0 == 0.0) rem.toLong().toString() else rem.toString()
                        errorMessage = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pay_full_balance_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("سداد كامل المبلغ المتبقي (${formatter.format(debt.remainingAmount)} ج.م)")
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Amount input
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorMessage = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input"),
                    label = { Text("المبلغ المسدد (ج.م) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Method / Note selection
                Text(
                    text = "طريقة السداد / البيان:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickNotes.take(3).forEach { qNote ->
                        FilterChip(
                            selected = note == qNote,
                            onClick = { note = qNote },
                            label = { Text(qNote, fontSize = 11.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickNotes.drop(3).forEach { qNote ->
                        FilterChip(
                            selected = note == qNote,
                            onClick = { note = qNote },
                            label = { Text(qNote, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_note_input"),
                    label = { Text("بيان الدفعة") },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Confirm / Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull()
                            if (amount == null || amount <= 0) {
                                errorMessage = "يرجى إدخال مبلغ سداد صحيح"
                                return@Button
                            }
                            if (amount > debt.remainingAmount + 0.01) {
                                errorMessage = "المبلغ المدخل أكبر من الرصيد المتبقي (${formatter.format(debt.remainingAmount)} ج.م)"
                                return@Button
                            }
                            onConfirmPayment(amount, note)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_payment_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تأكيد السداد")
                    }
                }
            }
        }
    }
}
