package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CustomerDebt
import com.example.data.model.PaymentRecord
import com.example.ui.AjelViewModel
import com.example.ui.AppUserRole
import com.example.ui.FilterTab
import com.example.ui.SortOption
import com.example.ui.components.AddEditDebtDialog
import com.example.ui.components.DebtCard
import com.example.ui.components.ExportShareDialog
import com.example.ui.components.FinancialSummaryHeader
import com.example.ui.components.ImportDataDialog
import com.example.ui.components.InvoiceDetailsDialog
import com.example.ui.components.ManagerDashboardCard
import com.example.ui.components.ManagerPinDialog
import com.example.ui.components.PaymentHistoryDialog
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.components.SalesRepBanner
import com.example.ui.components.SmartAlertBanner
import com.example.ui.components.SmartReminderDialog
import com.example.util.SmartReminderHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AjelViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val debts by viewModel.filteredDebts.collectAsStateWithLifecycle()
    val allDebts by viewModel.allDebts.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val managerStats by viewModel.managerStats.collectAsStateWithLifecycle()

    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var debtToEdit by remember { mutableStateOf<CustomerDebt?>(null) }
    var debtForReminder by remember { mutableStateOf<CustomerDebt?>(null) }
    var debtForPayment by remember { mutableStateOf<CustomerDebt?>(null) }
    var debtForHistory by remember { mutableStateOf<CustomerDebt?>(null) }
    var debtForInvoiceItems by remember { mutableStateOf<CustomerDebt?>(null) }
    var debtToDelete by remember { mutableStateOf<CustomerDebt?>(null) }

    var showExportShareDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showManagerPinDialog by remember { mutableStateOf(false) }
    var isChangingPinMode by remember { mutableStateOf(false) }
    var showDataSyncMenu by remember { mutableStateOf(false) }

    var showSortMenu by remember { mutableStateOf(false) }

    // Notification permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.sendSmartNotificationCheck(context)
            Toast.makeText(context, "تم تفعيل إشعارات مواعيد التحصيل بنجاح", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "يرجى منح إذن الإشعارات لتصلك تنبيهات التحصيل", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerSmartNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                viewModel.sendSmartNotificationCheck(context)
                Toast.makeText(context, "تم فحص مواعيد التحصيل وإرسال التنبيه", Toast.LENGTH_SHORT).show()
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.sendSmartNotificationCheck(context)
            Toast.makeText(context, "تم فحص مواعيد التحصيل وإرسال التنبيه", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-prompt permission once on first launch if overdue debts exist
    LaunchedEffect(summary.overdueCount) {
        if (summary.overdueCount > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.zina_logo),
                            contentDescription = "شعار زينة للتوريدات",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "زينة للتوريدات",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "متابعة ديون العملاء والتحصيل الذكي",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Role Switcher Badge (Manager vs Sales Rep)
                    Surface(
                        onClick = {
                            if (userRole == AppUserRole.SALES_REP) {
                                isChangingPinMode = false
                                showManagerPinDialog = true
                            } else {
                                viewModel.setUserRole(AppUserRole.SALES_REP)
                                Toast.makeText(context, "تم التحويل إلى وضع المندوب الميداني", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = if (userRole == AppUserRole.MANAGER) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("role_switcher_top_bar_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (userRole == AppUserRole.MANAGER) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (userRole == AppUserRole.MANAGER) Color(0xFFB45309) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (userRole == AppUserRole.MANAGER) "المدير" else "المندوب",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (userRole == AppUserRole.MANAGER) Color(0xFFB45309) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Direct Excel Export Button
                    IconButton(
                        onClick = { showExportShareDialog = true },
                        modifier = Modifier.testTag("top_bar_excel_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "تصدير شيت إكسيل",
                            tint = Color(0xFF047857)
                        )
                    }

                    // Data Sync & Transfer Menu Button
                    Box {
                        IconButton(
                            onClick = { showDataSyncMenu = true },
                            modifier = Modifier.testTag("top_bar_sync_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "مزامنة ونقل البيانات",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = showDataSyncMenu,
                            onDismissRequest = { showDataSyncMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("تصدير شيت إكسيل (Excel)", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF047857))
                                },
                                onClick = {
                                    showDataSyncMenu = false
                                    showExportShareDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("إرسال البيانات لمستخدم آخر", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                onClick = {
                                    showDataSyncMenu = false
                                    showExportShareDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("استيراد ومزامنة كشف بيانات", fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                },
                                onClick = {
                                    showDataSyncMenu = false
                                    showImportDialog = true
                                }
                            )
                        }
                    }

                    // Smart notifications bell with badge
                    val urgentCount = summary.overdueCount + summary.dueTodayCount
                    IconButton(
                        onClick = { triggerSmartNotification() },
                        modifier = Modifier.testTag("top_app_bar_notification_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (urgentCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text("$urgentCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "تنبيهات التحصيل الذكية"
                            )
                        }
                    }

                    // Sort menu
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("sort_menu_button")
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "ترتيب القائمة")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sort.title,
                                            fontWeight = if (selectedSort == sort) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        if (selectedSort == sort) {
                                            Icon(Icons.Default.FilterList, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        viewModel.onSortSelected(sort)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة آجل جديد", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_debt_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_bar_input"),
                    placeholder = { Text("بحث باسم العميل أو رقم الهاتف أو الملاحظات...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            // Role Banner / Manager Dashboard
            if (userRole == AppUserRole.MANAGER) {
                item {
                    ManagerDashboardCard(
                        stats = managerStats,
                        onExportExcel = { showExportShareDialog = true },
                        onImportData = { showImportDialog = true },
                        onShareBackup = {
                            viewModel.shareBackupData(context, "المدير العام") { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onChangePin = {
                            isChangingPinMode = true
                            showManagerPinDialog = true
                        },
                        onSwitchToSalesRep = {
                            viewModel.setUserRole(AppUserRole.SALES_REP)
                            Toast.makeText(context, "تم التحويل إلى وضع المندوب الميداني", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                item {
                    SalesRepBanner(
                        onSendToManager = {
                            showExportShareDialog = true
                        },
                        onExportExcel = {
                            showExportShareDialog = true
                        },
                        onSwitchToManager = {
                            isChangingPinMode = false
                            showManagerPinDialog = true
                        }
                    )
                }
            }

            // 2. Financial Summary Card
            item {
                FinancialSummaryHeader(summary = summary)
            }

            // 3. Smart Alert Banner (shows if overdue or due today)
            item {
                SmartAlertBanner(
                    summary = summary,
                    onNotifyClick = { triggerSmartNotification() },
                    onViewOverdueClick = { viewModel.onFilterSelected(FilterTab.OVERDUE) }
                )
            }

            // 4. Filter Chips Row
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سجل العملاء والحسابات:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${debts.size} من إجمالي ${allDebts.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(FilterTab.entries) { tab ->
                            val count = when (tab) {
                                FilterTab.ALL -> allDebts.size
                                FilterTab.OVERDUE -> summary.overdueCount
                                FilterTab.DUE_TODAY -> summary.dueTodayCount
                                FilterTab.DUE_SOON -> allDebts.count { it.status == com.example.data.model.DebtStatus.DUE_SOON }
                                FilterTab.SETTLED -> allDebts.count { it.status == com.example.data.model.DebtStatus.SETTLED }
                            }

                            FilterChip(
                                selected = selectedFilter == tab,
                                onClick = { viewModel.onFilterSelected(tab) },
                                label = {
                                    Text(
                                        text = "${tab.title} ($count)",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedFilter == tab) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = if (tab == FilterTab.OVERDUE && summary.overdueCount > 0) {
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    FilterChipDefaults.filterChipColors()
                                }
                            )
                        }
                    }
                }
            }

            // 5. Debt List Items or Empty State
            if (debts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .testTag("empty_state_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة للبحث"
                                else "لا توجد حسابات مسجلة في هذا القسم",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "جرب البحث باسم آخر أو مسح كلمة البحث"
                                else "اضغط على زر '+ إضافة آجل جديد' لإضافة حساب عميل وموعد تحصيله",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = debts,
                    key = { it.id }
                ) { debt ->
                    DebtCard(
                        debt = debt,
                        onReminderClick = { debtForReminder = it },
                        onRecordPaymentClick = { debtForPayment = it },
                        onHistoryClick = { debtForHistory = it },
                        onInvoiceItemsClick = { debtForInvoiceItems = it },
                        onEditClick = { debtToEdit = it },
                        onDeleteClick = { debtToDelete = it },
                        onCallClick = { phone -> SmartReminderHelper.callCustomer(context, phone) }
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog || debtToEdit != null) {
        AddEditDebtDialog(
            debtToEdit = debtToEdit,
            onDismiss = {
                showAddDialog = false
                debtToEdit = null
            },
            onSave = { name, storeName, contactPerson, phone, secondaryPhone, address, latitude, longitude, amount, dueDate, invoiceNumber, invoiceItemsJson, notes, customerNotes ->
                if (debtToEdit == null) {
                    viewModel.addDebt(
                        name = name,
                        storeName = storeName,
                        contactPerson = contactPerson,
                        phone = phone,
                        secondaryPhone = secondaryPhone,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        amount = amount,
                        dueDate = dueDate,
                        invoiceNumber = invoiceNumber,
                        invoiceItemsJson = invoiceItemsJson,
                        notes = notes,
                        customerNotes = customerNotes
                    )
                    Toast.makeText(context, "تم تسجيل الحساب الآجل بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateDebt(
                        debt = debtToEdit!!,
                        name = name,
                        storeName = storeName,
                        contactPerson = contactPerson,
                        phone = phone,
                        secondaryPhone = secondaryPhone,
                        address = address,
                        latitude = latitude,
                        longitude = longitude,
                        amount = amount,
                        dueDate = dueDate,
                        invoiceNumber = invoiceNumber,
                        invoiceItemsJson = invoiceItemsJson,
                        notes = notes,
                        customerNotes = customerNotes
                    )
                    Toast.makeText(context, "تم تحديث بيانات الحساب بنجاح", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
                debtToEdit = null
            }
        )
    }

    // Invoice Details Dialog
    debtForInvoiceItems?.let { debt ->
        InvoiceDetailsDialog(
            debt = debt,
            onDismiss = { debtForInvoiceItems = null }
        )
    }

    // Smart Reminder Dialog
    debtForReminder?.let { debt ->
        SmartReminderDialog(
            debt = debt,
            onDismiss = { debtForReminder = null },
            onReminderSent = {
                viewModel.markReminderSent(it.id)
                debtForReminder = null
            }
        )
    }

    // Record Payment Dialog
    debtForPayment?.let { debt ->
        RecordPaymentDialog(
            debt = debt,
            onDismiss = { debtForPayment = null },
            onConfirmPayment = { amount, note ->
                viewModel.recordPayment(debt, amount, note)
                Toast.makeText(context, "تم تسجيل سداد الدفعة بنجاح", Toast.LENGTH_SHORT).show()
                debtForPayment = null
            }
        )
    }

    // Payment History Dialog
    debtForHistory?.let { debt ->
        val payments by viewModel.getPaymentsForDebt(debt.id).collectAsStateWithLifecycle()
        PaymentHistoryDialog(
            debt = debt,
            payments = payments,
            onDismiss = { debtForHistory = null }
        )
    }

    // Delete Confirmation Dialog
    debtToDelete?.let { debt ->
        AlertDialog(
            onDismissRequest = { debtToDelete = null },
            title = { Text("حذف حساب العميل") },
            text = {
                Text("هل أنت متأكد من رغبتك في حذف حساب '${debt.customerName}'؟ سيتم حذف جميع إيصالات السداد المرتبطة به نهائياً.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDebt(debt)
                        Toast.makeText(context, "تم حذف الحساب بنجاح", Toast.LENGTH_SHORT).show()
                        debtToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { debtToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Export & Share Dialog (Excel Sheet / App Data File)
    if (showExportShareDialog) {
        ExportShareDialog(
            allDebts = allDebts,
            onExportExcel = { debtsToExport, title ->
                viewModel.exportDebtsToExcel(context, debtsToExport, title) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onShareDataFile = { senderName ->
                viewModel.shareBackupData(context, senderName) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showExportShareDialog = false }
        )
    }

    // Import & Sync Dialog
    if (showImportDialog) {
        ImportDataDialog(
            onImportConfirmed = { payload, mode ->
                viewModel.importBackupData(payload, mode) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { showImportDialog = false }
        )
    }

    // Manager PIN Dialog (Unlock or Change PIN)
    if (showManagerPinDialog) {
        ManagerPinDialog(
            isChangingPin = isChangingPinMode,
            onPinSuccess = {
                viewModel.setUserRole(AppUserRole.MANAGER)
                Toast.makeText(context, "مرحباً بك في لوحة تحكم المدير العام", Toast.LENGTH_SHORT).show()
                showManagerPinDialog = false
            },
            onSaveNewPin = { newPin ->
                viewModel.updateManagerPin(newPin)
                Toast.makeText(context, "تم تغيير الرمز السري للمدير بنجاح", Toast.LENGTH_SHORT).show()
                showManagerPinDialog = false
            },
            verifyPin = { pin -> viewModel.checkManagerPin(pin) },
            onDismiss = { showManagerPinDialog = false }
        )
    }
}
