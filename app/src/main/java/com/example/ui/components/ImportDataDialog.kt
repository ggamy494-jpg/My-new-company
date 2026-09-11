package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ImportMode
import com.example.util.BackupPayload
import com.example.util.DataBackupHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImportDataDialog(
    onImportConfirmed: (BackupPayload, ImportMode) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedPayload by remember { mutableStateOf<BackupPayload?>(null) }
    var importMode by remember { mutableStateOf(ImportMode.MERGE) }
    var rawTextBackup by remember { mutableStateOf("") }
    var showTextInput by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val content = DataBackupHelper.readTextFromUri(context, uri)
            if (content != null) {
                val payload = DataBackupHelper.parseBackupJson(content)
                if (payload != null) {
                    selectedPayload = payload
                } else {
                    Toast.makeText(context, "الملف المحدد غير صالح أو تالف", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "تعذر قراءة محتوى الملف", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "استيراد ومزامنة بيانات",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedPayload == null) {
                    Text(
                        text = "يمكنك استيراد بيانات الأواجل المرسلة إليك من زميل أو مندوب آخر عنده البرنامج:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Pick file button
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_backup_file_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار ملف البيانات (.ajel / .json)", fontWeight = FontWeight.Bold)
                    }

                    // Toggle text input option
                    TextButton(
                        onClick = { showTextInput = !showTextInput },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(if (showTextInput) "إخفاء اللصق اليدوي" else "أو لصق نص النسخة الاحتياطية يدوياً", fontSize = 12.sp)
                    }

                    if (showTextInput) {
                        OutlinedTextField(
                            value = rawTextBackup,
                            onValueChange = { rawTextBackup = it },
                            label = { Text("الصق نص البيانات هنا") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        FilledTonalButton(
                            onClick = {
                                if (rawTextBackup.isNotBlank()) {
                                    val payload = DataBackupHelper.parseBackupJson(rawTextBackup.trim())
                                    if (payload != null) {
                                        selectedPayload = payload
                                    } else {
                                        Toast.makeText(context, "النص الملصق غير مطابق لصيغة النسخة الاحتياطية", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("فحص وتأكيد النص")
                        }
                    }
                } else {
                    // Preview of the loaded payload
                    val p = selectedPayload!!
                    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")).format(Date(p.exportDate))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "✓ تم العثور على بيانات صالحة:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "• المرسل: ${p.senderName}", fontSize = 12.sp, color = Color(0xFF047857))
                            Text(text = "• تاريخ التصدير: $dateStr", fontSize = 12.sp, color = Color(0xFF047857))
                            Text(text = "• عدد حسابات العملاء: ${p.debts.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                            Text(text = "• عدد إيصالات السداد: ${p.payments.size}", fontSize = 12.sp, color = Color(0xFF047857))
                        }
                    }

                    Text(
                        text = "كيف ترغب في معالجة هذه البيانات؟",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Mode 1: MERGE
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { importMode = ImportMode.MERGE }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                RadioButton(
                                    selected = importMode == ImportMode.MERGE,
                                    onClick = { importMode = ImportMode.MERGE }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "دمج مع البيانات الحالية (موصى به)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "إضافة العملاء الجدد وتحديث المبالغ دون حذف أي حسابات سابقة.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Mode 2: REPLACE
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { importMode = ImportMode.REPLACE }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                RadioButton(
                                    selected = importMode == ImportMode.REPLACE,
                                    onClick = { importMode = ImportMode.REPLACE }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "استبدال كامل لقاعدة البيانات",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "حذف البيانات الحالية واعتماد هذه النسخة فقط (مثالي للمدير).",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (importMode == ImportMode.REPLACE) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تنبيه: الاستبدال سيحذف أية سجلات غير موجودة في هذا الملف.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedPayload != null) {
                Button(
                    onClick = {
                        onImportConfirmed(selectedPayload!!, importMode)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = if (importMode == ImportMode.REPLACE) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) {
                    Text(
                        text = if (importMode == ImportMode.REPLACE) "استبدال وتحديث الآن" else "دمج البيانات الآن",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (selectedPayload != null) {
                        selectedPayload = null
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (selectedPayload != null) "اختيار ملف آخر" else "إلغاء")
            }
        }
    )
}
