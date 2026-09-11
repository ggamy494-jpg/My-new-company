package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CustomerDebt
import com.example.data.model.DebtStatus
import com.example.data.model.PaymentRecord
import com.example.data.repository.AjelRepository
import com.example.util.BackupPayload
import com.example.util.DataBackupHelper
import com.example.util.ExcelExportHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

enum class AppUserRole(val title: String) {
    MANAGER("حساب المدير العام"),
    SALES_REP("حساب المندوب / المحصل")
}

enum class ImportMode {
    MERGE,   // دمج مع البيانات الحالية
    REPLACE  // استبدال كامل للبيانات
}

enum class FilterTab(val title: String) {
    ALL("الكل"),
    OVERDUE("المتأخرة"),
    DUE_TODAY("مستحق اليوم"),
    DUE_SOON("قريباً"),
    SETTLED("المسددة")
}

enum class SortOption(val title: String) {
    DUE_DATE("موعد التحصيل"),
    AMOUNT_DESC("المبلغ (الأكبر)"),
    NAME("اسم العميل")
}

data class FinancialSummary(
    val totalRemaining: Double = 0.0,
    val totalOverdue: Double = 0.0,
    val overdueCount: Int = 0,
    val dueTodayAmount: Double = 0.0,
    val dueTodayCount: Int = 0,
    val totalCollected: Double = 0.0,
    val totalDebtsCount: Int = 0
)

data class ManagerDashboardStats(
    val totalDebtsGiven: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val collectionRate: Float = 0f,
    val totalCustomersCount: Int = 0,
    val settledCustomersCount: Int = 0,
    val overdueCustomersCount: Int = 0,
    val topDebtors: List<CustomerDebt> = emptyList()
)

class AjelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AjelRepository
    private val prefs: SharedPreferences = application.getSharedPreferences("zina_ajel_prefs", Context.MODE_PRIVATE)

    // Current User Role (Manager vs Sales Rep)
    private val _userRole = MutableStateFlow(
        if (prefs.getString("user_role", "MANAGER") == "SALES_REP") AppUserRole.SALES_REP
        else AppUserRole.MANAGER
    )
    val userRole: StateFlow<AppUserRole> = _userRole

    // Manager Security PIN (Default 1234)
    private val _managerPin = MutableStateFlow(prefs.getString("manager_pin", "1234") ?: "1234")
    val managerPin: StateFlow<String> = _managerPin

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AjelRepository(database.ajelDao())
    }

    val allDebts: StateFlow<List<CustomerDebt>> = repository.allDebts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFilter = MutableStateFlow(FilterTab.ALL)
    val selectedFilter: StateFlow<FilterTab> = _selectedFilter

    private val _selectedSort = MutableStateFlow(SortOption.DUE_DATE)
    val selectedSort: StateFlow<SortOption> = _selectedSort

    // Financial summary stats
    val summary: StateFlow<FinancialSummary> = allDebts.combine(_searchQuery) { debts, _ ->
        var totalRem = 0.0
        var totalOver = 0.0
        var overCount = 0
        var dueTodayAmt = 0.0
        var todayCount = 0
        var totalColl = 0.0

        for (debt in debts) {
            totalRem += debt.remainingAmount
            totalColl += debt.paidAmount
            when (debt.status) {
                DebtStatus.OVERDUE -> {
                    totalOver += debt.remainingAmount
                    overCount++
                }
                DebtStatus.DUE_TODAY -> {
                    dueTodayAmt += debt.remainingAmount
                    todayCount++
                }
                else -> {}
            }
        }

        FinancialSummary(
            totalRemaining = totalRem,
            totalOverdue = totalOver,
            overdueCount = overCount,
            dueTodayAmount = dueTodayAmt,
            dueTodayCount = todayCount,
            totalCollected = totalColl,
            totalDebtsCount = debts.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FinancialSummary()
    )

    // Manager Dashboard Stats
    val managerStats: StateFlow<ManagerDashboardStats> = allDebts.combine(summary) { debts, sum ->
        val totalGiven = debts.sumOf { it.totalAmount }
        val rate = if (totalGiven > 0) ((sum.totalCollected / totalGiven).coerceIn(0.0, 1.0)).toFloat() else 1f
        val settledCount = debts.count { it.isSettled }
        val topDebtors = debts.filter { !it.isSettled }
            .sortedByDescending { it.remainingAmount }
            .take(5)

        ManagerDashboardStats(
            totalDebtsGiven = totalGiven,
            totalCollected = sum.totalCollected,
            totalRemaining = sum.totalRemaining,
            collectionRate = rate,
            totalCustomersCount = debts.size,
            settledCustomersCount = settledCount,
            overdueCustomersCount = sum.overdueCount,
            topDebtors = topDebtors
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManagerDashboardStats()
    )

    // Filtered and sorted debt list
    val filteredDebts: StateFlow<List<CustomerDebt>> = combine(
        allDebts,
        _searchQuery,
        _selectedFilter,
        _selectedSort
    ) { debts, query, filter, sort ->
        var list = debts

        // 1. Search query
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.customerName.lowercase().contains(q) ||
                it.storeName.lowercase().contains(q) ||
                it.contactPerson.lowercase().contains(q) ||
                it.customerPhone.contains(q) ||
                it.address.lowercase().contains(q) ||
                it.notes.lowercase().contains(q) ||
                it.invoiceNumber.lowercase().contains(q)
            }
        }

        // 2. Tab filter
        list = when (filter) {
            FilterTab.ALL -> list
            FilterTab.OVERDUE -> list.filter { it.status == DebtStatus.OVERDUE }
            FilterTab.DUE_TODAY -> list.filter { it.status == DebtStatus.DUE_TODAY }
            FilterTab.DUE_SOON -> list.filter { it.status == DebtStatus.DUE_SOON }
            FilterTab.SETTLED -> list.filter { it.status == DebtStatus.SETTLED }
        }

        // 3. Sort
        when (sort) {
            SortOption.DUE_DATE -> list.sortedWith(
                compareBy<CustomerDebt> { it.isSettled }
                    .thenBy { it.dueDate }
            )
            SortOption.AMOUNT_DESC -> list.sortedWith(
                compareBy<CustomerDebt> { it.isSettled }
                    .thenByDescending { it.remainingAmount }
            )
            SortOption.NAME -> list.sortedBy { it.customerName }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Seed initial realistic sample data on first run
        viewModelScope.launch {
            allDebts.collect { list ->
                if (list.isEmpty()) {
                    seedSampleDebts()
                }
            }
        }
    }

    private suspend fun seedSampleDebts() {
        val cal = Calendar.getInstance()

        val sampleInvoice1 = CustomerDebt.encodeInvoiceItems(
            listOf(
                com.example.data.model.InvoiceItem("زيت زينة خليط 800 مل (كرتونة)", 10.0, 650.0),
                com.example.data.model.InvoiceItem("سكر أبيض زينة 1 كجم (باكت 10ك)", 5.0, 360.0),
                com.example.data.model.InvoiceItem("أرز مصري عريض الحبة (شيكارة 25ك)", 2.0, 750.0)
            )
        )

        val sampleInvoice2 = CustomerDebt.encodeInvoiceItems(
            listOf(
                com.example.data.model.InvoiceItem("مشروبات غازية كانز مشكل (باكت)", 15.0, 320.0),
                com.example.data.model.InvoiceItem("عصير طبيعي زينة لتر (كرتونة)", 8.0, 240.0)
            )
        )

        // Overdue customer 1 (3 days ago)
        val cal1 = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -3) }
        val debt1 = CustomerDebt(
            customerName = "سوبرماركت البركة",
            storeName = "البركة ماركت",
            contactPerson = "الحاج عماد الدسوقي",
            customerPhone = "01012345678",
            secondaryPhone = "01122334455",
            address = "شارع الجيش، ميدان الساعة، طنطا",
            latitude = 30.7865,
            longitude = 31.0004,
            totalAmount = 9800.0,
            paidAmount = 2800.0,
            dueDate = cal1.timeInMillis,
            invoiceNumber = "INV-2026-041",
            invoiceItemsJson = sampleInvoice1,
            notes = "توريد زيوت وسكر وأرز - تم تسليم البضاعة بالمخزن",
            customerNotes = "عميل قديم ومميز، يسدد بدفعات نقدية كل أسبوعين",
            reminderCount = 1
        )

        // Overdue customer 2 (1 day ago)
        val cal2 = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val debt2 = CustomerDebt(
            customerName = "ماركت الفهد للمواد الغذائية",
            storeName = "الفهد ماركت",
            contactPerson = "محمود عادل",
            customerPhone = "01198765432",
            address = "شارع الجمهورية - أمام بنك مصر",
            latitude = 30.7910,
            longitude = 30.9950,
            totalAmount = 6720.0,
            paidAmount = 0.0,
            dueDate = cal2.timeInMillis,
            invoiceNumber = "INV-2026-055",
            invoiceItemsJson = sampleInvoice2,
            notes = "مشروبات وعصائر صيفية - طلب مهلة يومين للسداد"
        )

        // Due today customer
        val calToday = (cal.clone() as Calendar)
        val debt3 = CustomerDebt(
            customerName = "هايبر النور التجاري",
            storeName = "هايبر النور فرع الكورنيش",
            contactPerson = "أحمد رضوان",
            customerPhone = "01234567890",
            address = "طريق الكورنيش، بجوار مبنى المحافظة",
            latitude = 30.7820,
            longitude = 31.0100,
            totalAmount = 15000.0,
            paidAmount = 5000.0,
            dueDate = calToday.timeInMillis,
            invoiceNumber = "INV-2026-088",
            notes = "دفعة توريدات غذائية متنوعة لشهر سبتمبر"
        )

        // Due in 2 days
        val cal4 = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) }
        val debt4 = CustomerDebt(
            customerName = "ميني ماركت الصفا",
            storeName = "الصفا والمروة",
            contactPerson = "إبراهيم السيد",
            customerPhone = "01055544332",
            address = "شارع النحاس، متفرع من البحر",
            totalAmount = 4500.0,
            paidAmount = 1500.0,
            dueDate = cal4.timeInMillis,
            notes = "توريد 10 كراتين مياه ومشروبات زينة"
        )

        // Settled customer
        val cal5 = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -8) }
        val debt5 = CustomerDebt(
            customerName = "مؤسسة الوفاء الحديثة",
            storeName = "الوفاء للمواد التموينية",
            contactPerson = "سالم عبد العليم",
            customerPhone = "01511223344",
            address = "المنطقة الصناعية، مخازن الجملة",
            totalAmount = 12000.0,
            paidAmount = 12000.0,
            dueDate = cal5.timeInMillis,
            notes = "تم سداد كامل الفاتورة بحوالة فورية"
        )

        val id1 = repository.insertDebt(debt1)
        val id2 = repository.insertDebt(debt2)
        val id3 = repository.insertDebt(debt3)
        val id4 = repository.insertDebt(debt4)
        val id5 = repository.insertDebt(debt5)

        // Seed some payment records
        repository.recordPayment(id1, 2800.0, 0.0, "دفعة مقدمة نقداً مع الاستلام")
        repository.recordPayment(id3, 5000.0, 0.0, "شيك بنكي مقبول الدفع")
        repository.recordPayment(id5, 12000.0, 0.0, "سداد كامل القيمة نقداً")
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: FilterTab) {
        _selectedFilter.value = filter
    }

    fun onSortSelected(sort: SortOption) {
        _selectedSort.value = sort
    }

    fun addDebt(
        name: String,
        storeName: String = "",
        contactPerson: String = "",
        phone: String,
        secondaryPhone: String = "",
        address: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        amount: Double,
        dueDate: Long,
        invoiceNumber: String = "",
        invoiceItemsJson: String = "",
        notes: String = "",
        customerNotes: String = ""
    ) {
        viewModelScope.launch {
            val newDebt = CustomerDebt(
                customerName = name.trim(),
                storeName = storeName.trim(),
                contactPerson = contactPerson.trim(),
                customerPhone = phone.trim(),
                secondaryPhone = secondaryPhone.trim(),
                address = address.trim(),
                latitude = latitude,
                longitude = longitude,
                totalAmount = amount,
                paidAmount = 0.0,
                dueDate = dueDate,
                invoiceNumber = invoiceNumber.trim(),
                invoiceItemsJson = invoiceItemsJson.trim(),
                notes = notes.trim(),
                customerNotes = customerNotes.trim()
            )
            repository.insertDebt(newDebt)
        }
    }

    fun updateDebt(
        debt: CustomerDebt,
        name: String,
        storeName: String = "",
        contactPerson: String = "",
        phone: String,
        secondaryPhone: String = "",
        address: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        amount: Double,
        dueDate: Long,
        invoiceNumber: String = "",
        invoiceItemsJson: String = "",
        notes: String = "",
        customerNotes: String = ""
    ) {
        viewModelScope.launch {
            val updated = debt.copy(
                customerName = name.trim(),
                storeName = storeName.trim(),
                contactPerson = contactPerson.trim(),
                customerPhone = phone.trim(),
                secondaryPhone = secondaryPhone.trim(),
                address = address.trim(),
                latitude = latitude,
                longitude = longitude,
                totalAmount = amount,
                dueDate = dueDate,
                invoiceNumber = invoiceNumber.trim(),
                invoiceItemsJson = invoiceItemsJson.trim(),
                notes = notes.trim(),
                customerNotes = customerNotes.trim()
            )
            repository.updateDebt(updated)
        }
    }

    fun recordPayment(
        debt: CustomerDebt,
        amount: Double,
        note: String
    ) {
        viewModelScope.launch {
            repository.recordPayment(
                debtId = debt.id,
                amount = amount,
                currentPaid = debt.paidAmount,
                note = note.trim()
            )
        }
    }

    fun deleteDebt(debt: CustomerDebt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    fun markReminderSent(debtId: Long) {
        viewModelScope.launch {
            repository.markReminderSent(debtId)
        }
    }

    fun sendSmartNotificationCheck(context: Context) {
        val s = summary.value
        if (s.overdueCount > 0 || s.dueTodayCount > 0) {
            NotificationHelper.showCollectionAlertNotification(
                context = context,
                overdueCount = s.overdueCount,
                dueTodayCount = s.dueTodayCount,
                totalDueAmount = s.totalOverdue + s.dueTodayAmount
            )
        }
    }

    fun getPaymentsForDebt(debtId: Long): StateFlow<List<PaymentRecord>> {
        return repository.getPaymentsForDebt(debtId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Role & Profile Management
    fun setUserRole(role: AppUserRole) {
        _userRole.value = role
        prefs.edit().putString("user_role", role.name).apply()
    }

    fun verifyPin(enteredPin: String): Boolean {
        return enteredPin == _managerPin.value
    }

    fun checkManagerPin(enteredPin: String): Boolean = verifyPin(enteredPin)

    fun updateManagerPin(newPin: String) {
        _managerPin.value = newPin
        prefs.edit().putString("manager_pin", newPin).apply()
    }

    // Excel Export
    fun exportDebtsToExcel(
        context: Context,
        debtsToExport: List<CustomerDebt>? = null,
        customTitle: String? = null,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val debts = debtsToExport ?: repository.getAllDebtsSnapshot()
                withContext(Dispatchers.Main) {
                    val title = customTitle ?: "تقرير_أجل_عملاء_شركة_زينة"
                    ExcelExportHelper.exportDebtsToExcel(context, debts, title)
                    ExcelExportHelper.shareExcelFile(context, debts, title)
                    onComplete?.invoke(true, "تم تجهيز شيت الإكسيل بنجاح (${debts.size} عميل)")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "حدث خطأ أثناء تصدير شيت الإكسيل: ${e.message}")
                }
            }
        }
    }

    // Data Backup Share (Send data to another user who has the app)
    fun shareBackupData(
        context: Context,
        senderName: String,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val debts = repository.getAllDebtsSnapshot()
                val payments = repository.getAllPaymentsSnapshot()
                withContext(Dispatchers.Main) {
                    DataBackupHelper.shareBackupFile(context, senderName, debts, payments)
                    onComplete?.invoke(true, "تم تجهيز ملف البيانات للمشاركة (${debts.size} عميل)")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "حدث خطأ أثناء تجهيز ملف البيانات: ${e.message}")
                }
            }
        }
    }

    fun shareBackupWithColleague(context: Context, senderName: String) {
        shareBackupData(context, senderName)
    }

    // Data Import (Merge or Replace)
    fun importBackupData(
        payload: BackupPayload,
        mode: ImportMode,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (mode == ImportMode.REPLACE) {
                    repository.restoreFullDatabase(payload.debts, payload.payments)
                    withContext(Dispatchers.Main) {
                        onResult(true, "تم استبدال واسترجاع ${payload.debts.size} عميل و ${payload.payments.size} إيصال سداد بنجاح!")
                    }
                } else {
                    val count = repository.mergeImportedData(payload.debts, payload.payments)
                    withContext(Dispatchers.Main) {
                        onResult(true, "تم دمج وتحديث البيانات بنجاح (${count} حسابات عملاء جديدة/محدثة)!")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, "حدث خطأ أثناء استيراد البيانات: ${e.message}")
                }
            }
        }
    }

    // Manager clear all data
    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllData()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
