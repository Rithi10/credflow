package com.example.credflow

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.credflow.di.AppContainer
import com.example.credflow.domain.model.Customer
import com.example.credflow.ui.viewmodel.CredflowViewModel
import com.example.credflow.util.CollectionBookExporter
import com.example.credflow.util.CustomerLedgerPdfExporter
import com.example.credflow.util.ExcelReportExporter
import com.example.credflow.util.ExportResult
import com.example.credflow.util.NotificationHelper
import com.example.credflow.util.PaymentReceiptPdfExporter
import com.example.credflow.util.ScheduleUtils
import com.example.credflow.worker.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { CredflowApp(intent) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredflowApp(intent: android.content.Intent? = null) {
    val context = LocalContext.current
    val appContainer = remember { AppContainer(context.applicationContext) }
    val viewModel: CredflowViewModel = remember {
        ViewModelProvider(
            context as ComponentActivity,
            CredflowViewModel.provideFactory(
                repository = appContainer.repository,
                sessionManager = appContainer.sessionManager,
                apiService = appContainer.apiService,
                appSettings = appContainer.appSettings
            )
        )[CredflowViewModel::class.java]
    }
    val uiState by viewModel.uiState.collectAsState()
    val installments by viewModel.installments.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannels(context)
        if (appContainer.appSettings.dailyReminders) {
            ReminderScheduler.scheduleDailyReminder(
                context,
                appContainer.appSettings.reminderHour,
                appContainer.appSettings.reminderMinute
            )
        }
        if (appContainer.appSettings.overdueAlerts) {
            ReminderScheduler.scheduleOverdueAlerts(context)
        }
    }

    val backStack = remember {
        mutableStateListOf(if (appContainer.sessionManager.isLoggedIn) Screen.Dashboard else Screen.Splash)
    }
    val screen = backStack.lastOrNull() ?: Screen.Splash

    fun go(target: Screen) {
        if (backStack.lastOrNull() != target) {
            backStack.add(target)
        }
    }
    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
        }
    }
    fun resetTo(target: Screen) {
        backStack.clear()
        backStack.add(target)
    }
    fun switchTab(target: Screen) {
        if (backStack.lastOrNull() != target) {
            if (target == Screen.Dashboard) {
                resetTo(Screen.Dashboard)
            } else {
                if (backStack.firstOrNull() == Screen.Dashboard) {
                    while (backStack.size > 1) {
                        backStack.removeAt(backStack.size - 1)
                    }
                    backStack.add(target)
                } else {
                    resetTo(Screen.Dashboard)
                    backStack.add(target)
                }
            }
        }
    }

    LaunchedEffect(intent, uiState.customers) {
        val targetScreen = intent?.getStringExtra(NotificationHelper.EXTRA_TARGET_SCREEN)
        val targetCustomerId = intent?.getIntExtra(NotificationHelper.EXTRA_CUSTOMER_ID, 0) ?: 0

        if (targetScreen == "CustomerDetails" && targetCustomerId > 0 && uiState.customers.isNotEmpty()) {
            val customer = uiState.customers.find { it.id == targetCustomerId }
            if (customer != null) {
                viewModel.selectCustomer(customer)
                go(when (customer.cycle) {
                    "Daily" -> Screen.DailyDetails
                    "Weekly" -> Screen.WeeklyOverview
                    else -> Screen.MonthlyDetails
                })
            }
        } else if (targetScreen == "DailyCustomers") {
            go(Screen.DailyCustomers)
        } else if (targetScreen == "Notifications") {
            go(Screen.Notifications)
        }
    }

    var chatQuestion by remember { mutableStateOf("") }
    var lastSavedReceiptFileName by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var interest by remember { mutableStateOf("10") }
    var interestType by remember { mutableStateOf("Flat Interest") }
    var period by remember { mutableStateOf("10") }
    var periodUnit by remember { mutableStateOf("Weeks") }
    var firstDueDate by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("Daily") }
    var day by remember { mutableStateOf("Monday") }
    var note by remember { mutableStateOf("") }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }

    fun openAdd(which: String, initialDay: String? = null) {
        name = ""; phone = ""; amount = ""; note = ""
        cycle = ScheduleUtils.normalizeCycle(which)
        day = when (cycle) {
            "Daily" -> "Daily"
            "Weekly" -> ScheduleUtils.normalizeWeeklyDay(initialDay)
            else -> ScheduleUtils.normalizeMonthlyDay(initialDay)
        }
        interest = "10"
        interestType = "Flat Interest"
        period = when (cycle) {
            "Daily" -> "30"
            "Weekly" -> "10"
            else -> "6"
        }
        periodUnit = when (cycle) {
            "Daily" -> "Days"
            "Weekly" -> "Weeks"
            else -> "Months"
        }
        firstDueDate = ""
        go(when (cycle) {
            "Daily" -> Screen.AddDaily
            "Weekly" -> Screen.AddWeekly
            else -> Screen.AddMonthly
        })
    }
    fun saveCustomer() {
        viewModel.addCustomer(
            name = name,
            phone = phone,
            amountStr = amount,
            interestStr = interest,
            interestType = interestType,
            periodStr = period,
            periodUnit = periodUnit,
            cycle = cycle,
            day = day,
            firstDueDate = firstDueDate,
            note = note,
            onSuccess = { savedCustomer ->
                val targetScreen = when {
                    savedCustomer.cycle.equals("Daily", true) -> Screen.DailyCustomers
                    savedCustomer.cycle.equals("Weekly", true) -> {
                        val wd = ScheduleUtils.normalizeWeeklyDay(savedCustomer.day, savedCustomer.firstDueDate)
                        if (wd.isNotBlank()) weekdayScreen(wd) else Screen.WeeklyCustomers
                    }
                    else -> Screen.MonthlyCustomers
                }
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.size - 1)
                }
                go(targetScreen)
            }
        )
    }
    fun recordPayment(c: Customer, value: Double, method: String) {
        viewModel.recordPayment(
            customer = c,
            amountStr = value.toString(),
            method = method,
            onSuccess = { updatedCustomer ->
                NotificationHelper.showPaymentNotification(
                    context = context,
                    customerName = updatedCustomer.name,
                    amount = value,
                    remainingBalance = updatedCustomer.due,
                    customerId = updatedCustomer.id
                )
                lastSavedReceiptFileName = null
                go(Screen.CollectionSaved)
            }
        )
    }
    fun launchExport(export: ExportResult, preferShare: Boolean) {
        val primary = if (preferShare) export.shareIntent else export.viewIntent
        val fallback = if (preferShare) export.viewIntent else export.shareIntent
        try {
            context.startActivity(primary)
        } catch (_: Exception) {
            try {
                context.startActivity(fallback)
            } catch (_: Exception) {
                viewModel.setExportError("Saved ${export.fileName}, but no app is available to open or share it.")
            }
        }
    }
    fun exportCustomerLedgerPdf(customerId: Int, preferShare: Boolean) {
        viewModel.prepareCustomerLedgerForExport(
            customerId = customerId,
            label = if (preferShare) "Preparing PDF to share..." else "Generating PDF ledger..."
        ) { result ->
            result.onSuccess { ledger ->
                val saved = CustomerLedgerPdfExporter.exportAndSave(context, ledger)
                saved.onSuccess { export ->
                    viewModel.setExportSuccess(
                        export.fileName,
                        "PDF saved to Downloads: ${export.fileName}"
                    )
                    launchExport(export, preferShare)
                }.onFailure { err ->
                    viewModel.setExportError("Failed to save PDF: ${err.message}")
                }
            }
        }
    }
    fun exportPaymentReceiptPdf(receipt: com.example.credflow.data.remote.dto.PaymentReceiptDto, launchMode: String?) {
        val saved = PaymentReceiptPdfExporter.exportAndSave(context, receipt)
        saved.onSuccess { export ->
            lastSavedReceiptFileName = export.fileName
            viewModel.setExportSuccess(export.fileName, "Receipt PDF saved: ${export.fileName}")
            if (launchMode == "share") {
                launchExport(export, preferShare = true)
            } else if (launchMode == "view") {
                launchExport(export, preferShare = false)
            }
        }.onFailure { err ->
            viewModel.setExportError("Failed to generate receipt PDF: ${err.message}")
        }
    }
    fun fetchAndExportPaymentReceipt(paymentId: Int, preferShare: Boolean) {
        viewModel.fetchPaymentReceipt(paymentId) { res ->
            res.onSuccess { receipt ->
                exportPaymentReceiptPdf(receipt, if (preferShare) "share" else "view")
            }.onFailure { err ->
                viewModel.setExportError(err.message ?: "Failed to load receipt from MySQL.")
            }
        }
    }
    fun exportExcelReport(cycleFilter: String = "all", startDate: String? = null, endDate: String? = null, preferShare: Boolean) {
        viewModel.prepareReportExportData(
            cycle = cycleFilter,
            startDate = startDate,
            endDate = endDate,
            label = if (preferShare) "Preparing Excel report to share..." else "Generating Excel report..."
        ) { result ->
            result.onSuccess { reportData ->
                val saved = ExcelReportExporter.exportAndSave(context, reportData)
                saved.onSuccess { export ->
                    viewModel.setExportSuccess(
                        export.fileName,
                        "Excel exported (${reportData.customers.size} customers, ${reportData.payments.size} payments): ${export.fileName}"
                    )
                    launchExport(export, preferShare)
                }.onFailure { err ->
                    viewModel.setExportError("Failed to save Excel file: ${err.message}")
                }
            }
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        goBack()
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue, onPrimary = Color.White, background = PageBg,
            surface = Color.White, onSurface = Navy, secondary = Green
        )
    ) {
        Scaffold(
            containerColor = if (screen == Screen.Splash) Navy else PageBg,
            topBar = {
                if (screen !in listOf(Screen.Splash, Screen.Welcome, Screen.Dashboard, Screen.Login)) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(if (screen == Screen.Settings) "Account" else "Credflow", fontSize = 11.sp, color = Muted)
                                Text(screen.title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Navy)
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                modifier = Modifier.padding(start = 8.dp),
                                onClick = { goBack() }
                            ) { Icon(Icons.Default.ArrowBack, "Back", tint = Navy) }
                        },
                        actions = {
                            val isAiScreen = screen in listOf(
                                Screen.AIAssistant,
                                Screen.AIChatbot,
                                Screen.AIPrediction,
                                Screen.Forecast
                            )
                            val isBusy = uiState.isSyncing || uiState.isRefreshingAi
                            IconButton(
                                onClick = {
                                    if (isAiScreen) viewModel.refreshAiData() else viewModel.refreshData()
                                },
                                enabled = !isBusy
                            ) {
                                if (isBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Navy
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = if (isAiScreen) "Refresh AI Data" else "Sync from MySQL",
                                        tint = Navy
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBg)
                    )
                }
            },
            bottomBar = {
                if (screen !in listOf(Screen.Splash, Screen.Welcome, Screen.Login, Screen.CollectionSaved)) {
                    NavigationBar(containerColor = Color.White) {
                        val nav = listOf(
                            Triple("Home", Icons.Default.Home, Screen.Dashboard),
                            Triple("AI", Icons.Default.AutoAwesome, Screen.AIAssistant),
                            Triple("Profile", Icons.Default.Person, Screen.Profile)
                        )
                        nav.forEach { (label, icon, target) ->
                            NavigationBarItem(
                                selected = screen == target,
                                onClick = { switchTab(target) },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Blue, selectedTextColor = Blue,
                                    indicatorColor = Color(0xFFEAF1FE)
                                )
                            )
                        }
                    }
                }
            },
            snackbarHost = {
                uiState.userMessage?.let { msg ->
                    SnackbarHost(hostState = remember { SnackbarHostState() }) {
                        Snackbar(
                            action = {
                                TextButton(onClick = { viewModel.clearUserMessage() }) {
                                    Text("Dismiss", color = Color.White)
                                }
                            }
                        ) { Text(msg) }
                    }
                }
            }
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (screen == Screen.Splash) Modifier else Modifier.padding(padding))
            ) {
                when (screen) {
                    Screen.Splash -> WelcomeSplash {
                        if (appContainer.sessionManager.isLoggedIn) {
                            resetTo(Screen.Dashboard)
                        } else {
                            go(Screen.Welcome)
                        }
                    }
                    Screen.Welcome -> WelcomePage(
                        onLogin = { go(Screen.Login) },
                        onExplore = { go(Screen.Login) }
                    )
                    Screen.Login -> LoginPage(
                        onBack = { if (backStack.size > 1) goBack() else resetTo(Screen.Welcome) },
                        onLogin = { identity, password ->
                            viewModel.login(identity, password) { resetTo(Screen.Dashboard) }
                        },
                        onSignUp = { nameVal, bNameVal, identity, password ->
                            viewModel.signUp(nameVal, bNameVal, identity, password) { resetTo(Screen.Dashboard) }
                        },
                        onGoogleSignInToken = { idToken ->
                            viewModel.googleSignInWithToken(idToken) { resetTo(Screen.Dashboard) }
                        },
                        onUpdateServerUrl = { newUrl ->
                            viewModel.updateServerUrl(newUrl)
                        },
                        onTestConnection = { onComplete ->
                            viewModel.testServerConnection(onComplete)
                        },
                        isLoading = uiState.isAuthenticating,
                        authError = uiState.authError,
                        onClearError = { viewModel.clearAuthError() }
                    )
                    Screen.Dashboard -> DashboardPage(
                        customers = uiState.customers,
                        payments = uiState.payments.map { it.toDisplayText() },
                        onCategories = { go(Screen.Categories) },
                        onSearch = { go(Screen.Search) },
                        onReports = { go(Screen.Reports) },
                        onNotifications = { go(Screen.Notifications) },
                        onSettings = { go(Screen.Settings) },
                        onDaily = { go(Screen.DailyCustomers) },
                        onWeekly = { go(Screen.WeeklyCustomers) },
                        onMonthly = { go(Screen.MonthlyCustomers) },
                        onRefresh = { viewModel.refreshData() },
                        isRefreshing = uiState.isSyncing,
                        refreshError = uiState.refreshError,
                        onDismissRefreshError = { viewModel.clearRefreshError() }
                    )
                    Screen.Categories -> CategoriesPage(
                        customers = uiState.customers,
                        onDaily = { go(Screen.DailyCustomers) },
                        onWeekly = { go(Screen.WeeklyCustomers) },
                        onMonthly = { go(Screen.MonthlyCustomers) },
                        onAdd = { openAdd(it) }
                    )
                    Screen.DailyCustomers, Screen.WeeklyCustomers, Screen.MonthlyCustomers -> {
                        val listCycle = when (screen) {
                            Screen.DailyCustomers -> "Daily"
                            Screen.WeeklyCustomers -> "Weekly"
                            else -> "Monthly"
                        }
                        val weeklyCounts = remember(uiState.customers) {
                            ScheduleUtils.WEEKDAYS.associateWith { wd ->
                                uiState.customers.count {
                                    it.cycle.equals("Weekly", ignoreCase = true) &&
                                        ScheduleUtils.normalizeWeeklyDay(it.day, it.firstDueDate).equals(wd, ignoreCase = true)
                                }
                            }
                        }
                        CustomerListPage(
                            title = screen.title,
                            cycle = listCycle,
                            customers = uiState.customers.filter { it.cycle.equals(listCycle, ignoreCase = true) },
                            onAdd = { openAdd(listCycle) },
                            onSelect = {
                                viewModel.selectCustomer(it)
                                go(when (listCycle) {
                                    "Daily" -> Screen.DailyDetails
                                    "Weekly" -> Screen.WeeklyOverview
                                    else -> Screen.MonthlyDetails
                                })
                            },
                            onCollect = {
                                viewModel.selectCustomer(it)
                                paymentAmount = ""
                                paymentMethod = "Cash"
                                go(Screen.CollectionEntry)
                            },
                            onWeekday = { day = it; go(weekdayScreen(it)) },
                            weekdayCounts = weeklyCounts
                        )
                    }
                    Screen.AddDaily, Screen.AddWeekly, Screen.AddMonthly -> AddCustomerPage(
                        cycle = cycle,
                        onCycleChange = { nextCycle ->
                            cycle = nextCycle
                            day = when (nextCycle) {
                                "Daily" -> "Daily"
                                "Weekly" -> ScheduleUtils.normalizeWeeklyDay(day, firstDueDate)
                                else -> ScheduleUtils.normalizeMonthlyDay(day, firstDueDate)
                            }
                        },
                        name = name, onName = { name = it },
                        phone = phone, onPhone = { phone = it },
                        amount = amount, onAmount = { amount = it },
                        interest = interest, onInterest = { interest = it },
                        interestType = interestType, onInterestType = { interestType = it },
                        period = period, onPeriod = { period = it },
                        periodUnit = periodUnit, onPeriodUnit = { periodUnit = it },
                        day = day, onDay = { day = it },
                        firstDueDate = firstDueDate, onFirstDueDate = { firstDueDate = it },
                        note = note, onNote = { note = it },
                        isSaving = uiState.isSavingCustomer,
                        onSave = { saveCustomer() }
                    )
                    Screen.DailyDetails, Screen.MonthlyDetails, Screen.WeeklyOverview -> CustomerDetailsPage(
                        customer = uiState.selectedCustomer,
                        installments = installments,
                        onCollect = {
                            if (uiState.selectedCustomer != null) {
                                paymentAmount = ""
                                go(Screen.CollectionEntry)
                            }
                        },
                        onHistory = { go(Screen.PaymentHistory) },
                        onLedger = {
                            viewModel.clearExportState()
                            uiState.selectedCustomer?.let { viewModel.loadCustomerLedger(it.id) }
                            go(Screen.CustomerLedger)
                        },
                        onCloseLoan = {
                            viewModel.clearLoanClosureError()
                            go(Screen.CloseLoan)
                        },
                        onBack = { goBack() }
                    )
                    Screen.CloseLoan -> CloseLoanPage(
                        customer = uiState.selectedCustomer,
                        isClosing = uiState.isClosingLoan,
                        closureError = uiState.loanClosureError,
                        onClearError = { viewModel.clearLoanClosureError() },
                        onSubmitClosure = { settlementType, settlementAmountStr, paymentMethod, note, closeLoanFlag ->
                            val cust = uiState.selectedCustomer ?: return@CloseLoanPage
                            viewModel.closeLoan(
                                customer = cust,
                                settlementType = settlementType,
                                settlementAmountStr = settlementAmountStr,
                                paymentMethod = paymentMethod,
                                note = note,
                                closeLoanFlag = closeLoanFlag
                            ) { resp ->
                                if (resp.receipt != null) {
                                    lastSavedReceiptFileName = null
                                    go(Screen.CollectionSaved)
                                } else {
                                    goBack()
                                }
                            }
                        },
                        onBack = { goBack() }
                    )
                    Screen.CustomerLedger -> {
                        val selectedCust = uiState.selectedCustomer
                        LaunchedEffect(selectedCust?.id) {
                            if (selectedCust != null && selectedCust.id > 0) {
                                viewModel.loadCustomerLedger(selectedCust.id)
                            }
                        }
                        CustomerLedgerPage(
                            ledgerState = uiState.customerLedgerState,
                            exportState = uiState.exportState,
                            selectedCustomer = selectedCust,
                            customers = uiState.customers,
                            onSelectCustomer = { cust ->
                                viewModel.clearExportState()
                                viewModel.selectCustomer(cust)
                                viewModel.loadCustomerLedger(cust.id)
                            },
                            onRefresh = { id -> viewModel.loadCustomerLedger(id) },
                            onSelectTab = { tab -> viewModel.selectLedgerTab(tab) },
                            onDownloadPdf = { id -> exportCustomerLedgerPdf(id, preferShare = false) },
                            onSharePdf = { id -> exportCustomerLedgerPdf(id, preferShare = true) },
                            onViewPaymentReceipt = { pid -> fetchAndExportPaymentReceipt(pid, preferShare = false) },
                            onSharePaymentReceipt = { pid -> fetchAndExportPaymentReceipt(pid, preferShare = true) },
                            onCloseLoan = { cust ->
                                viewModel.selectCustomer(cust)
                                viewModel.clearLoanClosureError()
                                go(Screen.CloseLoan)
                            },
                            onCollect = { cust ->
                                viewModel.selectCustomer(cust)
                                paymentAmount = ""
                                paymentMethod = "Cash"
                                go(Screen.CollectionEntry)
                            },
                            onBack = { goBack() }
                        )
                    }
                    Screen.Monday, Screen.Tuesday, Screen.Wednesday, Screen.Thursday, Screen.Friday, Screen.Saturday, Screen.Sunday -> {
                        val weekday = screen.title.removeSuffix(" Collection")
                        val weeklyCounts = remember(uiState.customers) {
                            ScheduleUtils.WEEKDAYS.associateWith { wd ->
                                uiState.customers.count {
                                    it.cycle.equals("Weekly", ignoreCase = true) &&
                                        ScheduleUtils.normalizeWeeklyDay(it.day, it.firstDueDate).equals(wd, ignoreCase = true)
                                }
                            }
                        }
                        CustomerListPage(
                            title = screen.title, cycle = "Weekly",
                            customers = uiState.customers.filter {
                                it.cycle.equals("Weekly", ignoreCase = true) &&
                                    ScheduleUtils.normalizeWeeklyDay(it.day, it.firstDueDate).equals(weekday, ignoreCase = true)
                            },
                            onAdd = { openAdd("Weekly", weekday) },
                            onSelect = {
                                viewModel.selectCustomer(it)
                                go(Screen.WeeklyOverview)
                            },
                            onCollect = {
                                viewModel.selectCustomer(it)
                                paymentAmount = ""
                                go(Screen.CollectionEntry)
                            },
                            onWeekday = { day = it; go(weekdayScreen(it)) },
                            weekdayCounts = weeklyCounts
                        )
                    }
                    Screen.CollectionEntry -> CollectionEntryPage(
                        customer = uiState.selectedCustomer,
                        amount = paymentAmount, onAmount = { paymentAmount = it },
                        method = paymentMethod, onMethod = { paymentMethod = it },
                        onSave = {
                            val c = uiState.selectedCustomer
                            val v = paymentAmount.toDoubleOrNull()
                            if (c != null && v != null && v > 0) recordPayment(c, v, paymentMethod)
                        }
                    )
                    Screen.CollectionSaved -> CollectionSavedPage(
                        customer = uiState.paidCustomer ?: uiState.selectedCustomer,
                        receipt = uiState.lastPaymentReceipt,
                        receiptSavedFileName = lastSavedReceiptFileName,
                        onAutoGenerateReceipt = { receipt ->
                            exportPaymentReceiptPdf(receipt, launchMode = null)
                        },
                        onShareReceipt = { receipt ->
                            exportPaymentReceiptPdf(receipt, launchMode = "share")
                        },
                        onViewReceiptPdf = { receipt ->
                            exportPaymentReceiptPdf(receipt, launchMode = "view")
                        },
                        onDone = { resetTo(Screen.Dashboard) },
                        onHistory = { go(Screen.PaymentHistory) }
                    )
                    Screen.PaymentHistory -> SimpleListPage(
                        "Payment History", "Collections recorded in MySQL database",
                        uiState.payments.distinctBy { it.id }.map { it.toDisplayText() }.ifEmpty { listOf("No collections recorded yet.") },
                        onPrimary = { goBack() }, primaryLabel = "Back"
                    )
                    Screen.Pending -> CustomerListPage("Pending Collections", "All", uiState.customers.filter { !it.isClosed && it.due > 0 },
                        onAdd = { openAdd("Daily") }, onSelect = { viewModel.selectCustomer(it); go(Screen.DailyDetails) },
                        onCollect = { viewModel.selectCustomer(it); paymentAmount = ""; go(Screen.CollectionEntry) }, onWeekday = { go(weekdayScreen(it)) })
                    Screen.Overdue -> CustomerListPage("Overdue Customers", "All", uiState.customers.filter { !it.isClosed && it.due > 0 },
                        onAdd = { openAdd("Daily") }, onSelect = { viewModel.selectCustomer(it); go(Screen.DailyDetails) },
                        onCollect = { viewModel.selectCustomer(it); paymentAmount = ""; go(Screen.CollectionEntry) }, onWeekday = { go(weekdayScreen(it)) })
                    Screen.AIAssistant -> AIAssistantPage(
                        onChat = { go(Screen.AIChatbot) }, onPrediction = { go(Screen.AIPrediction) },
                        onForecast = { go(Screen.Forecast) }, onReports = { go(Screen.Reports) }
                    )
                    Screen.AIChatbot -> AIChatPage(
                        question = chatQuestion,
                        onQuestion = { chatQuestion = it },
                        answer = uiState.aiAnswer,
                        chatState = uiState.chatState,
                        lastAskedQuestion = uiState.lastAskedQuestion,
                        onAsk = { viewModel.askAi(chatQuestion) },
                        onRetry = { viewModel.retryChat() },
                        onSelectSuggestion = { prompt ->
                            chatQuestion = prompt
                            viewModel.askAi(prompt)
                        }
                    )
                    Screen.AIPrediction -> {
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            viewModel.runRepaymentPrediction()
                        }
                        PredictionPage(
                            customers = uiState.customers,
                            predictionState = uiState.predictionState,
                            onRunPrediction = { viewModel.runRepaymentPrediction() },
                            onSelectCustomer = { customer ->
                                viewModel.selectCustomer(customer)
                                go(Screen.DailyDetails)
                            },
                            onDetails = { go(Screen.RiskDetails) }
                        )
                    }
                    Screen.RiskDetails -> SimpleListPage("Repayment Risk Details", "Demo insight — computed from your live records",
                        listOf("Customers with a remaining balance: ${uiState.customers.count { it.due > 0 }}",
                            "Total remaining amount: ${rupees(uiState.totalOutstanding)}",
                            "Total collected so far: ${rupees(uiState.totalCollected)}",
                            "This insight is computed dynamically from your live MySQL and cached customer records."),
                        onPrimary = { goBack() }, primaryLabel = "Back to prediction")
                    Screen.Forecast -> {
                        LaunchedEffect(screen) {
                            viewModel.loadCollectionForecast()
                        }
                        CollectionForecastPage(
                            forecastState = uiState.forecastState,
                            onRefresh = { viewModel.loadCollectionForecast() },
                            onPeriodSelect = { period -> viewModel.selectForecastPeriod(period) },
                            onCustomerClick = { customerId ->
                                val cust = uiState.customers.find { it.id == customerId }
                                if (cust != null) {
                                    viewModel.selectCustomer(cust)
                                    go(when (cust.cycle) {
                                        "Daily" -> Screen.DailyDetails
                                        "Weekly" -> Screen.WeeklyOverview
                                        else -> Screen.MonthlyDetails
                                    })
                                }
                            },
                            onBack = { goBack() }
                        )
                    }
                    Screen.WeeklyForecast -> ForecastDetailPage("Weekly Forecast Details", uiState.customers, "Weekly", onBack = { goBack() })
                    Screen.MonthlyForecast -> ForecastDetailPage("Monthly Forecast Details", uiState.customers, "Monthly", onBack = { goBack() })
                    Screen.Reports -> {
                        LaunchedEffect(Unit) {
                            if (uiState.reportsState is com.example.credflow.ui.viewmodel.ReportsUiState.Idle) {
                                viewModel.loadReports()
                            }
                        }
                        val activeReportState = uiState.reportsState as? com.example.credflow.ui.viewmodel.ReportsUiState.Success
                        val activeStart = activeReportState?.customStartDate
                        val activeEnd = activeReportState?.customEndDate
                        ReportsPage(
                            reportsState = uiState.reportsState,
                            exportState = uiState.exportState,
                            customers = uiState.customers,
                            payments = uiState.payments.map { it.toDisplayText() },
                            onDaily = { go(Screen.DailyReport) },
                            onWeekly = { go(Screen.WeeklyReport) },
                            onMonthly = { go(Screen.MonthlyReport) },
                            onRefresh = { viewModel.retryReports() },
                            onPresetSelect = { preset, start, end -> viewModel.selectReportsPreset(preset, start, end) },
                            onChartTabSelect = { tab -> viewModel.selectReportsChartTab(tab) },
                            onExportExcel = { exportExcelReport("all", activeStart, activeEnd, preferShare = false) },
                            onShareExcel = { exportExcelReport("all", activeStart, activeEnd, preferShare = true) },
                            onOpenCustomerLedgers = {
                                viewModel.clearExportState()
                                viewModel.selectCustomer(null)
                                go(Screen.CustomerLedger)
                            },
                            onBack = { goBack() }
                        )
                    }
                    Screen.DailyReport -> {
                        val cycleData = (uiState.reportsState as? com.example.credflow.ui.viewmodel.ReportsUiState.Success)?.reports?.cycleBreakdown?.get("daily")
                        ReportDetailPage("Daily Report Details", uiState.customers.filter { it.cycle == "Daily" }, uiState.payments.map { it.toDisplayText() }, cycleReport = cycleData, onBack = { goBack() })
                    }
                    Screen.WeeklyReport -> {
                        val cycleData = (uiState.reportsState as? com.example.credflow.ui.viewmodel.ReportsUiState.Success)?.reports?.cycleBreakdown?.get("weekly")
                        ReportDetailPage("Weekly Report Details", uiState.customers.filter { it.cycle == "Weekly" }, uiState.payments.map { it.toDisplayText() }, cycleReport = cycleData, onBack = { goBack() })
                    }
                    Screen.MonthlyReport -> {
                        val cycleData = (uiState.reportsState as? com.example.credflow.ui.viewmodel.ReportsUiState.Success)?.reports?.cycleBreakdown?.get("monthly")
                        ReportDetailPage("Monthly Report Details", uiState.customers.filter { it.cycle == "Monthly" }, uiState.payments.map { it.toDisplayText() }, cycleReport = cycleData, onBack = { goBack() })
                    }
                    Screen.Search -> {
                        LaunchedEffect(Unit) {
                            viewModel.executeCustomerSearch(0L)
                        }
                        SearchPage(
                            query = uiState.searchQuery,
                            onQuery = { viewModel.updateSearchQuery(it) },
                            customers = uiState.filteredCustomers,
                            cycleFilter = uiState.searchCycleFilter,
                            onCycleFilterChange = { viewModel.updateSearchCycleFilter(it) },
                            statusFilter = uiState.searchStatusFilter,
                            onStatusFilterChange = { viewModel.updateSearchStatusFilter(it) },
                            sortOption = uiState.searchSortOption,
                            onSortOptionChange = { viewModel.updateSearchSortOption(it) },
                            isSearching = uiState.isSearching,
                            searchError = uiState.searchError,
                            onClearFilters = { viewModel.clearSearchFilters() },
                            onRetry = { viewModel.executeCustomerSearch(0L) },
                            onSelect = {
                                viewModel.selectCustomer(it)
                                go(if (it.cycle == "Daily") Screen.DailyDetails else if (it.cycle == "Weekly") Screen.WeeklyOverview else Screen.MonthlyDetails)
                            },
                            onCollect = {
                                viewModel.selectCustomer(it)
                                paymentAmount = ""
                                go(Screen.CollectionEntry)
                            }
                        )
                    }
                    Screen.Notifications -> {
                        LaunchedEffect(Unit) {
                            if (uiState.notificationsState is com.example.credflow.ui.viewmodel.NotificationsUiState.Idle) {
                                viewModel.loadNotifications()
                            }
                        }
                        NotificationsPage(
                            notificationsState = uiState.notificationsState,
                            customers = uiState.customers,
                            payments = uiState.payments,
                            onSelectCustomer = { cust ->
                                viewModel.selectCustomer(cust)
                                go(when (cust.cycle) {
                                    "Daily" -> Screen.DailyDetails
                                    "Weekly" -> Screen.WeeklyOverview
                                    else -> Screen.MonthlyDetails
                                })
                            },
                            onSelectCustomerId = { customerId ->
                                val cust = uiState.customers.find { it.id == customerId }
                                if (cust != null) {
                                    viewModel.selectCustomer(cust)
                                    go(when (cust.cycle) {
                                        "Daily" -> Screen.DailyDetails
                                        "Weekly" -> Screen.WeeklyOverview
                                        else -> Screen.MonthlyDetails
                                    })
                                }
                            },
                            onMarkAsRead = { notificationId -> viewModel.markNotificationRead(notificationId) },
                            onMarkAllAsRead = { viewModel.markAllNotificationsRead() },
                            onFilterSelect = { filter -> viewModel.selectNotificationFilter(filter) },
                            onRefresh = { viewModel.retryNotifications() },
                            onOpenPaymentHistory = { go(Screen.PaymentHistory) },
                            onOpenOverdue = { go(Screen.Overdue) },
                            onOpenDailyReport = { go(Screen.DailyReport) },
                            onBack = { goBack() }
                        )
                    }
                    Screen.Profile -> ProfilePage(
                        businessName = uiState.businessName, onBusinessNameChange = {},
                        ownerName = uiState.ownerName, onOwnerNameChange = {},
                        ownerPhone = uiState.ownerPhone, onOwnerPhoneChange = {},
                        ownerEmail = uiState.ownerEmail, onOwnerEmailChange = {},
                        businessAddress = uiState.businessAddress, onAddressChange = {},
                        gstinNumber = uiState.gstinNumber, onGstinChange = {},
                        onSave = { },
                        onSaveProfile = { bName, oName, oPhone, oEmail, bAddr, gst, onComplete ->
                            viewModel.saveProfile(bName, oName, oPhone, oEmail, bAddr, gst, onComplete)
                        },
                        onOpenSettings = { go(Screen.Settings) }
                    )
                    Screen.Settings -> SettingsPage(
                        acceptCash = uiState.acceptCash,
                        onAcceptCashChange = { viewModel.updateSettings(acceptCash = it, acceptUpi = uiState.acceptUpi, acceptBank = uiState.acceptBank, dailyReminders = uiState.dailyReminders, overdueAlerts = uiState.overdueAlerts, defaultCycle = uiState.defaultCycle, lateFeeRule = uiState.lateFeeRule) },
                        acceptUpi = uiState.acceptUpi,
                        onAcceptUpiChange = { viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = it, acceptBank = uiState.acceptBank, dailyReminders = uiState.dailyReminders, overdueAlerts = uiState.overdueAlerts, defaultCycle = uiState.defaultCycle, lateFeeRule = uiState.lateFeeRule) },
                        acceptBank = uiState.acceptBank,
                        onAcceptBankChange = { viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = uiState.acceptUpi, acceptBank = it, dailyReminders = uiState.dailyReminders, overdueAlerts = uiState.overdueAlerts, defaultCycle = uiState.defaultCycle, lateFeeRule = uiState.lateFeeRule) },
                        dailyReminders = uiState.dailyReminders,
                        onDailyRemindersChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasNotificationPermission(context)) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                                ReminderScheduler.scheduleDailyReminder(context, uiState.reminderHour, uiState.reminderMinute)
                            } else {
                                ReminderScheduler.cancelDailyReminder(context)
                            }
                            viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = uiState.acceptUpi, acceptBank = uiState.acceptBank, dailyReminders = enabled, overdueAlerts = uiState.overdueAlerts, defaultCycle = uiState.defaultCycle, lateFeeRule = uiState.lateFeeRule)
                        },
                        reminderHour = uiState.reminderHour,
                        reminderMinute = uiState.reminderMinute,
                        onReminderTimeChange = { h, m ->
                            viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = uiState.acceptUpi, acceptBank = uiState.acceptBank, dailyReminders = uiState.dailyReminders, overdueAlerts = uiState.overdueAlerts, defaultCycle = uiState.defaultCycle, lateFeeRule = uiState.lateFeeRule, reminderHour = h, reminderMinute = m)
                            if (uiState.dailyReminders) {
                                ReminderScheduler.scheduleDailyReminder(context, h, m)
                            }
                        },
                        overdueAlerts = uiState.overdueAlerts,
                        onOverdueAlertsChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasNotificationPermission(context)) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                                ReminderScheduler.scheduleOverdueAlerts(context)
                            } else {
                                ReminderScheduler.cancelOverdueAlerts(context)
                            }
                            viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = uiState.acceptUpi, acceptBank = uiState.acceptBank, dailyReminders = uiState.dailyReminders, overdueAlerts = enabled, defaultCycle = uiState.defaultCycle, lateFeeRule = uiState.lateFeeRule)
                        },
                        defaultCycle = uiState.defaultCycle,
                        onDefaultCycleChange = { viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = uiState.acceptUpi, acceptBank = uiState.acceptBank, dailyReminders = uiState.dailyReminders, overdueAlerts = uiState.overdueAlerts, defaultCycle = it, lateFeeRule = uiState.lateFeeRule) },
                        lateFeeRule = uiState.lateFeeRule,
                        onLateFeeRuleChange = { viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = uiState.acceptUpi, acceptBank = uiState.acceptBank, dailyReminders = uiState.dailyReminders, overdueAlerts = uiState.overdueAlerts, defaultCycle = uiState.defaultCycle, lateFeeRule = it) },
                        onSave = { },
                        onOpenProfile = { go(Screen.Profile) },
                        onExportData = {
                            viewModel.prepareDataForExport { custs, pmts ->
                                val res = CollectionBookExporter.exportAndSave(context, custs, pmts)
                                res.onSuccess { export ->
                                    launchExport(export, preferShare = true)
                                }
                            }
                        },
                        onExportExcel = {
                            exportExcelReport("all", null, null, preferShare = true)
                        },
                        onLogOut = {
                            viewModel.logout {
                                resetTo(Screen.Login)
                            }
                        },
                        isLoggingOut = uiState.isLoggingOut,
                        onResetData = { viewModel.clearData() },
                        onDeleteServerData = {
                            viewModel.resetServerData { res ->
                                res.onSuccess {
                                    resetTo(Screen.Dashboard)
                                }
                            }
                        },
                        onChangePassword = { curr, new, conf, cb -> viewModel.changePassword(curr, new, conf, cb) },
                        isChangingPassword = uiState.isChangingPassword
                    )
                }
            }
        }
    }
}
