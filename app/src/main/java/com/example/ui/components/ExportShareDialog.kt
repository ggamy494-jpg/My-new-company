package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerDebt
import com.example.data.model.DebtStatus

enum class ExportScope {
    ALL,
    OVERDUE_ONLY,
    SETTLED_ONLY
}

@Composable
fun ExportShareDialog(
    allDebts: List<CustomerDebt>,
    onExportExcel: (List<CustomerDebt>, String) -> Unit,
    onShareDataFile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Excel Sheet, 1: App Data File
    var exportScope by remember { mutableStateOf(ExportScope.ALL) }
    var senderName by remember { mutableStateOf("مندوب شركة زينة") }

    val filteredDebts = remember(exportScope, allDebts) {
        when (exportScope) {
            ExportScope.ALL -> allDebts
            ExportScope.OVERDUE_ONLY -> allDebts.filter { it.status == DebtStatus.OVERDUE }
            ExportScope.SETTLED_ONLY -> allDebts.filter { it.isSettled }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (selectedTab == 0) "تصدير شيت إكسيل" else "إرسال البيانات لمستخدم آخر",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 0) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("شيت إكسيل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ملف للبرنامج", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                if (selectedTab == 0) {
                    // EXCEL TAB
                    Text(
                        text = "اختر العملاء المراد تصديرهم لشيت الإكسيل:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Option 1: All
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportScope = ExportScope.ALL }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = exportScope == ExportScope.ALL,
                                    onClick = { exportScope = ExportScope.ALL }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("جميع العملاء والأواجل (${allDebts.size} عميل)", fontSize = 13.sp)
                            }

                            // Option 2: Overdue only
                            val overdueCount = allDebts.count { it.status == DebtStatus.OVERDUE }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportScope = ExportScope.OVERDUE_ONLY }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = exportScope == ExportScope.OVERDUE_ONLY,
                                    onClick = { exportScope = ExportScope.OVERDUE_ONLY }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الأواجل المتأخرة فقط ($overdueCount عميل)", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                            }

                            // Option 3: Settled only
                            val settledCount = allDebts.count { it.isSettled }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { exportScope = ExportScope.SETTLED_ONLY }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = exportScope == ExportScope.SETTLED_ONLY,
                                    onClick = { exportScope = ExportScope.SETTLED_ONLY }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("العملاء المسددين بالكامل ($settledCount عميل)", fontSize = 13.sp)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECFDF5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 الشيت متوافق بالكامل مع Microsoft Excel و Google Sheets مع دعم اللغة العربية والأرقام وإحداثيات الخرائط وتفاصيل الفواتير.",
                                fontSize = 11.sp,
                                color = Color(0xFF065F46),
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    // APP DATA FILE TAB
                    Text(
                        text = "إرسال نسخة من بياناتك إلى شخص آخر عنده البرنامج (زميل أو المدير):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = senderName,
                        onValueChange = { senderName = it },
                        label = { Text("اسم المرسل أو المندوب") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "📦 سيتم تجهيز ملف (.ajel) يحتوي على:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• عدد ${allDebts.size} حساب عميل مع تفاصيل الفواتير والـ GPS\n• كامل إيصالات وسجل الدفعات المسددة\n• الطرف الآخر يستطيع الضغط على 'استيراد بيانات' لدمجها مباشرة في جهازه.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        val title = when (exportScope) {
                            ExportScope.ALL -> "شيت_أواجل_عملاء_زينة_شامل"
                            ExportScope.OVERDUE_ONLY -> "شيت_أواجل_متأخرات_زينة"
                            ExportScope.SETTLED_ONLY -> "شيت_عملاء_زينة_المسددين"
                        }
                        onExportExcel(filteredDebts, title)
                    } else {
                        onShareDataFile(senderName.ifBlank { "مندوب زينة" })
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Default.TableChart else Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedTab == 0) "تصدير ومشاركة الشيت" else "إرسال ملف البيانات الآن",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
