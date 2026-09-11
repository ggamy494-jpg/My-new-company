package com.example.ui.components

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.model.CustomerDebt
import com.example.data.model.InvoiceItem
import com.example.util.GpsLocationHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditableInvoiceItem(
    var name: String = "",
    var quantityStr: String = "1",
    var unitPriceStr: String = ""
) {
    val totalPrice: Double
        get() {
            val q = quantityStr.toDoubleOrNull() ?: 0.0
            val p = unitPriceStr.toDoubleOrNull() ?: 0.0
            return q * p
        }
}

@Composable
fun AddEditDebtDialog(
    debtToEdit: CustomerDebt? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        storeName: String,
        contactPerson: String,
        phone: String,
        secondaryPhone: String,
        address: String,
        latitude: Double?,
        longitude: Double?,
        amount: Double,
        dueDate: Long,
        invoiceNumber: String,
        invoiceItemsJson: String,
        notes: String,
        customerNotes: String
    ) -> Unit
) {
    val context = LocalContext.current
    val formatter = NumberFormat.getNumberInstance(Locale("ar"))

    // Customer basic info
    var name by remember { mutableStateOf(debtToEdit?.customerName ?: "") }
    var storeName by remember { mutableStateOf(debtToEdit?.storeName ?: "") }
    var contactPerson by remember { mutableStateOf(debtToEdit?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(debtToEdit?.customerPhone ?: "") }
    var secondaryPhone by remember { mutableStateOf(debtToEdit?.secondaryPhone ?: "") }
    var address by remember { mutableStateOf(debtToEdit?.address ?: "") }
    var customerNotes by remember { mutableStateOf(debtToEdit?.customerNotes ?: "") }

    // GPS coordinates
    var latitude by remember { mutableStateOf(debtToEdit?.latitude) }
    var longitude by remember { mutableStateOf(debtToEdit?.longitude) }
    var isFetchingGps by remember { mutableStateOf(false) }

    // Input mode: 0 = Quick Amount (كتابة رقم الأجل مباشرة), 1 = Itemized Invoice (تفاصيل الفاتورة بالأصناف)
    val initialMode = if (debtToEdit?.hasDetailedInvoice == true) 1 else 0
    var selectedTab by remember { mutableIntStateOf(initialMode) }

    // Direct Amount
    var directAmountStr by remember {
        mutableStateOf(
            if (debtToEdit != null) {
                if (debtToEdit.totalAmount % 1.0 == 0.0) debtToEdit.totalAmount.toLong().toString()
                else debtToEdit.totalAmount.toString()
            } else ""
        )
    }

    // Invoice Items
    val invoiceItems = remember {
        mutableStateListOf<EditableInvoiceItem>().apply {
            if (debtToEdit != null && debtToEdit.hasDetailedInvoice) {
                debtToEdit.getInvoiceItems().forEach { item ->
                    add(
                        EditableInvoiceItem(
                            name = item.name,
                            quantityStr = if (item.quantity % 1.0 == 0.0) item.quantity.toLong().toString() else item.quantity.toString(),
                            unitPriceStr = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toLong().toString() else item.unitPrice.toString()
                        )
                    )
                }
            } else {
                add(EditableInvoiceItem())
            }
        }
    }

    val itemsTotal = invoiceItems.sumOf { it.totalPrice }

    var invoiceNumber by remember { mutableStateOf(debtToEdit?.invoiceNumber ?: "") }
    var notes by remember { mutableStateOf(debtToEdit?.notes ?: "") }

    // Due date
    var selectedDueDateMillis by remember {
        mutableLongStateOf(
            debtToEdit?.dueDate ?: run {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 7)
                cal.timeInMillis
            }
        )
    }

    var errorMessage by remember { mutableStateOf("") }
    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale("ar")) }

    // Location permission request
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isFetchingGps = true
            GpsLocationHelper.fetchCurrentGps(
                context = context,
                onSuccess = { lat, lng ->
                    isFetchingGps = false
                    latitude = lat
                    longitude = lng
                    Toast.makeText(context, "تم التقاط إحداثيات الموقع بنجاح عبر GPS", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    isFetchingGps = false
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        } else {
            Toast.makeText(context, "يرجى منح إذن الوصول للموقع لتحديد موقع العميل بالـ GPS", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestGps() {
        val fineStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineStatus == PackageManager.PERMISSION_GRANTED || coarseStatus == PackageManager.PERMISSION_GRANTED) {
            isFetchingGps = true
            GpsLocationHelper.fetchCurrentGps(
                context = context,
                onSuccess = { lat, lng ->
                    isFetchingGps = false
                    latitude = lat
                    longitude = lng
                    Toast.makeText(context, "تم التقاط إحداثيات الموقع بنجاح عبر GPS", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    isFetchingGps = false
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun showDatePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDueDateMillis
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val picker = DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedDueDateMillis = newCal.timeInMillis
            },
            year,
            month,
            day
        )
        picker.show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .testTag("add_edit_debt_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (debtToEdit == null) "تسجيل آجل جديد للعميل" else "تعديل بيانات الحساب",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Customer Info (اسم العميل، المحل، الشخص المسؤول)
                Text(
                    text = "بيانات العميل والمنشأة:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Customer Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input"),
                    label = { Text("اسم العميل / التاجر *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Store Name & Contact Person
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("store_name_input"),
                        label = { Text("اسم المحل / الماركت") },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("contact_person_input"),
                        label = { Text("المسؤول / المستلم") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Section 2: Phone Numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_phone_input"),
                        label = { Text("رقم الهاتف / واتساب *") },
                        placeholder = { Text("010xxxxxxxx") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = secondaryPhone,
                        onValueChange = { secondaryPhone = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("رقم إضافي (اختياري)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 3: GPS Location & Address
                Text(
                    text = "موقع العميل والـ GPS:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_address_input"),
                    label = { Text("العنوان بالتفصيل (المدينة، الشارع، علامة مميزة)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // GPS Fetch Button & Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (latitude != null && longitude != null)
                                    "📍 إحداثيات GPS: (${String.format(Locale.US, "%.4f", latitude)}, ${String.format(Locale.US, "%.4f", longitude)})"
                                else "لم يتم تحديد إحداثيات GPS بعد",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (latitude != null) FontWeight.Bold else FontWeight.Normal,
                                color = if (latitude != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (latitude != null) {
                                IconButton(
                                    onClick = {
                                        latitude = null
                                        longitude = null
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح إحداثيات GPS",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            Button(
                                onClick = { requestGps() },
                                enabled = !isFetchingGps,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("fetch_gps_btn")
                            ) {
                                if (isFetchingGps) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (latitude != null) "تحديث GPS" else "تحديد GPS", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 4: Flexible Debt Amount Mode (Direct Amount vs Detailed Invoice Items)
                Text(
                    text = "طريقة تسجيل الآجل والفاتورة:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "كتابة رقم الآجل مباشرة",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "تفاصيل الفاتورة بالأصناف",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab 0: Direct amount input
                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = directAmountStr,
                        onValueChange = {
                            directAmountStr = it
                            errorMessage = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("total_amount_input"),
                        label = { Text("إجمالي مبلغ الآجل (ج.م) *") },
                        placeholder = { Text("مثلاً: 5000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                } else {
                    // Tab 1: Detailed invoice items list
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "أصناف الفاتورة (${invoiceItems.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedButton(
                                onClick = { invoiceItems.add(EditableInvoiceItem()) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة صنف", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        invoiceItems.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = item.name,
                                            onValueChange = { item.name = it },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("اسم الصنف (مثلاً: زيت زينة)") },
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true
                                        )

                                        if (invoiceItems.size > 1) {
                                            IconButton(
                                                onClick = { invoiceItems.removeAt(index) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "حذف الصنف",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = item.quantityStr,
                                            onValueChange = { item.quantityStr = it },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("الكمية") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = item.unitPriceStr,
                                            onValueChange = { item.unitPriceStr = it },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("سعر الوحدة") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true
                                        )

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.width(90.dp)
                                        ) {
                                            Text(
                                                text = "الإجمالي",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${formatter.format(item.totalPrice)} ج.م",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Total calculation bar
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "إجمالي الفاتورة المحسوب:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${formatter.format(itemsTotal)} ج.م",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 5: Due Date / Collection Date
                Text(
                    text = "موعد التحصيل والاستحقاق *",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = { showDatePicker() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("select_due_date_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "تاريخ الاستحقاق: ${dateFormatter.format(Date(selectedDueDateMillis))}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 6: Additional details & notes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("رقم الفاتورة (اختياري)") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("ملاحظات البضاعة") },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Customer notes
                OutlinedTextField(
                    value = customerNotes,
                    onValueChange = { customerNotes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("معلومات عن العميل (شروط التعامل، مواعيد التواجد)") },
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

                // Save & Cancel Buttons
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
                            if (name.isBlank()) {
                                errorMessage = "يرجى كتابة اسم العميل"
                                return@Button
                            }

                            val calculatedAmount: Double
                            val invoiceItemsJson: String

                            if (selectedTab == 0) {
                                // Direct Amount
                                val amount = directAmountStr.toDoubleOrNull()
                                if (amount == null || amount <= 0) {
                                    errorMessage = "يرجى إدخال مبلغ آجل صحيح أكبر من الصفر"
                                    return@Button
                                }
                                calculatedAmount = amount
                                invoiceItemsJson = ""
                            } else {
                                // Detailed Invoice Items
                                val validItems = invoiceItems.filter { it.name.isNotBlank() }.map {
                                    InvoiceItem(
                                        name = it.name.trim(),
                                        quantity = it.quantityStr.toDoubleOrNull() ?: 1.0,
                                        unitPrice = it.unitPriceStr.toDoubleOrNull() ?: 0.0
                                    )
                                }
                                if (validItems.isEmpty()) {
                                    errorMessage = "يرجى إدخال صنف واحد على الأقل في الفاتورة أو اختيار 'كتابة رقم الآجل مباشرة'"
                                    return@Button
                                }
                                val sum = validItems.sumOf { it.totalPrice }
                                if (sum <= 0) {
                                    errorMessage = "يرجى إدخال أسعار وكميات صحيحة للأصناف"
                                    return@Button
                                }
                                calculatedAmount = sum
                                invoiceItemsJson = CustomerDebt.encodeInvoiceItems(validItems)
                            }

                            onSave(
                                name,
                                storeName,
                                contactPerson,
                                phone,
                                secondaryPhone,
                                address,
                                latitude,
                                longitude,
                                calculatedAmount,
                                selectedDueDateMillis,
                                invoiceNumber,
                                invoiceItemsJson,
                                notes,
                                customerNotes
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_debt_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ البيانات")
                    }
                }
            }
        }
    }
}
