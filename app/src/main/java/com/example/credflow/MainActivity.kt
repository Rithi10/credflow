package com.example.credflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { CredflowApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredflowApp() {
    val context = LocalContext.current
    val appContainer = remember { AppContainer(context.applicationContext) }
    val viewModel: CredflowViewModel = remember {
        ViewModelProvider(
            context as ComponentActivity,
            CredflowViewModel.provideFactory(
                repository = appContainer.repository,
                sessionManager = appContainer.sessionManager,
                apiService = appContainer.apiService
            )
        )[CredflowViewModel::class.java]
    }
    val uiState by viewModel.uiState.collectAsState()

    val backStack = remember {
        mutableStateListOf(if (appContainer.sessionManager.isLoggedIn) Screen.Dashboard else Screen.Splash)
    }
    val screen = backStack.lastOrNull() ?: Screen.Splash

    var chatQuestion by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("Daily") }
    var day by remember { mutableStateOf("Monday") }
    var note by remember { mutableStateOf("") }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }

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
    fun openAdd(which: String) {
        name = ""; phone = ""; amount = ""; note = ""
        cycle = which
        day = "Monday"
        go(when (which) {
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
            cycle = cycle,
            day = day,
            note = note,
            onSuccess = { savedCustomer ->
                val targetScreen = when (savedCustomer.cycle) {
                    "Daily" -> Screen.DailyCustomers
                    "Weekly" -> Screen.WeeklyCustomers
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
            onSuccess = {
                go(Screen.CollectionSaved)
            }
        )
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
                            IconButton(onClick = { viewModel.refreshData() }) {
                                Icon(Icons.Default.Refresh, "Sync from MySQL", tint = Navy)
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
                        onBack = { goBack() },
                        onLogin = { identity, password ->
                            viewModel.login(identity, password) { resetTo(Screen.Dashboard) }
                        },
                        onSignUp = { nameVal, bNameVal, identity, password ->
                            viewModel.signUp(nameVal, bNameVal, identity, password) { resetTo(Screen.Dashboard) }
                        },
                        onGoogleSignInToken = { idToken ->
                            viewModel.googleSignInWithToken(idToken) { resetTo(Screen.Dashboard) }
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
                        onRefresh = { viewModel.refreshData() }
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
                        CustomerListPage(
                            title = screen.title,
                            cycle = listCycle,
                            customers = uiState.customers.filter { it.cycle == listCycle },
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
                            onWeekday = { day = it; go(weekdayScreen(it)) }
                        )
                    }
                    Screen.AddDaily, Screen.AddWeekly, Screen.AddMonthly -> AddCustomerPage(
                        cycle = cycle,
                        name = name, onName = { name = it },
                        phone = phone, onPhone = { phone = it },
                        amount = amount, onAmount = { amount = it },
                        day = day, onDay = { day = it },
                        note = note, onNote = { note = it },
                        onSave = { saveCustomer() }
                    )
                    Screen.DailyDetails, Screen.MonthlyDetails, Screen.WeeklyOverview -> CustomerDetailsPage(
                        customer = uiState.selectedCustomer,
                        onCollect = {
                            if (uiState.selectedCustomer != null) {
                                paymentAmount = ""
                                go(Screen.CollectionEntry)
                            }
                        },
                        onHistory = { go(Screen.PaymentHistory) },
                        onBack = { goBack() }
                    )
                    Screen.Monday, Screen.Tuesday, Screen.Wednesday, Screen.Thursday, Screen.Friday, Screen.Saturday, Screen.Sunday -> {
                        val weekday = screen.title.removeSuffix(" Collection")
                        CustomerListPage(
                            title = screen.title, cycle = "Weekly",
                            customers = uiState.customers.filter { it.cycle == "Weekly" && it.day.equals(weekday, true) },
                            onAdd = { day = weekday; openAdd("Weekly") },
                            onSelect = {
                                viewModel.selectCustomer(it)
                                go(Screen.WeeklyOverview)
                            },
                            onCollect = {
                                viewModel.selectCustomer(it)
                                paymentAmount = ""
                                go(Screen.CollectionEntry)
                            },
                            onWeekday = { day = it; go(weekdayScreen(it)) }
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
                        onDone = { resetTo(Screen.Dashboard) },
                        onHistory = { go(Screen.PaymentHistory) }
                    )
                    Screen.PaymentHistory -> SimpleListPage(
                        "Payment History", "Collections recorded in MySQL database",
                        uiState.payments.map { it.toDisplayText() }.ifEmpty { listOf("No collections recorded yet.") },
                        onPrimary = { goBack() }, primaryLabel = "Back"
                    )
                    Screen.Pending -> CustomerListPage("Pending Collections", "All", uiState.customers.filter { it.due > 0 },
                        onAdd = { openAdd("Daily") }, onSelect = { viewModel.selectCustomer(it); go(Screen.DailyDetails) },
                        onCollect = { viewModel.selectCustomer(it); paymentAmount = ""; go(Screen.CollectionEntry) }, onWeekday = { go(weekdayScreen(it)) })
                    Screen.Overdue -> CustomerListPage("Overdue Customers", "All", uiState.customers.filter { it.due > 0 },
                        onAdd = { openAdd("Daily") }, onSelect = { viewModel.selectCustomer(it); go(Screen.DailyDetails) },
                        onCollect = { viewModel.selectCustomer(it); paymentAmount = ""; go(Screen.CollectionEntry) }, onWeekday = { go(weekdayScreen(it)) })
                    Screen.AIAssistant -> AIAssistantPage(
                        onChat = { go(Screen.AIChatbot) }, onPrediction = { go(Screen.AIPrediction) },
                        onForecast = { go(Screen.Forecast) }, onReports = { go(Screen.Reports) }
                    )
                    Screen.AIChatbot -> AIChatPage(
                        question = chatQuestion, onQuestion = { chatQuestion = it },
                        answer = uiState.aiAnswer,
                        onAsk = { viewModel.askAi(chatQuestion) }
                    )
                    Screen.AIPrediction -> PredictionPage(customers = uiState.customers, onDetails = { go(Screen.RiskDetails) })
                    Screen.RiskDetails -> SimpleListPage("Repayment Risk Details", "Demo insight — computed from your live records",
                        listOf("Customers with a remaining balance: ${uiState.customers.count { it.due > 0 }}",
                            "Total remaining amount: ${rupees(uiState.totalOutstanding)}",
                            "Total collected so far: ${rupees(uiState.totalCollected)}",
                            "This insight is computed dynamically from your live MySQL and cached customer records."),
                        onPrimary = { goBack() }, primaryLabel = "Back to prediction")
                    Screen.Forecast -> SimpleListPage("AI Collection Forecast", "Estimates based on live records currently available",
                        listOf("Total remaining amount: ${rupees(uiState.totalOutstanding)}",
                            "Daily remaining: ${rupees(uiState.customers.filter { it.cycle == "Daily" }.sumOf { it.due })}",
                            "Weekly remaining: ${rupees(uiState.customers.filter { it.cycle == "Weekly" }.sumOf { it.due })}",
                            "Monthly remaining: ${rupees(uiState.customers.filter { it.cycle == "Monthly" }.sumOf { it.due })}",
                            "Active customer count: ${uiState.customersCount}"),
                        onPrimary = { go(Screen.WeeklyForecast) }, primaryLabel = "Weekly forecast",
                        onSecondary = { go(Screen.MonthlyForecast) }, secondaryLabel = "Monthly forecast")
                    Screen.WeeklyForecast -> ForecastDetailPage("Weekly Forecast Details", uiState.customers, "Weekly", onBack = { goBack() })
                    Screen.MonthlyForecast -> ForecastDetailPage("Monthly Forecast Details", uiState.customers, "Monthly", onBack = { goBack() })
                    Screen.Reports -> ReportsPage(uiState.customers, uiState.payments.map { it.toDisplayText() }, onDaily = { go(Screen.DailyReport) }, onWeekly = { go(Screen.WeeklyReport) }, onMonthly = { go(Screen.MonthlyReport) })
                    Screen.DailyReport -> ReportDetailPage("Daily Report Details", uiState.customers.filter { it.cycle == "Daily" }, uiState.payments.map { it.toDisplayText() }, onBack = { goBack() })
                    Screen.WeeklyReport -> ReportDetailPage("Weekly Report Details", uiState.customers.filter { it.cycle == "Weekly" }, uiState.payments.map { it.toDisplayText() }, onBack = { goBack() })
                    Screen.MonthlyReport -> ReportDetailPage("Monthly Report Details", uiState.customers.filter { it.cycle == "Monthly" }, uiState.payments.map { it.toDisplayText() }, onBack = { goBack() })
                    Screen.Search -> SearchPage(
                        query = uiState.searchQuery, onQuery = { viewModel.updateSearchQuery(it) },
                        customers = uiState.filteredCustomers,
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
                    Screen.Notifications -> SimpleListPage("Notifications", "Collection reminders and updates",
                        listOf("${uiState.customers.count { it.due > 0 }} customers have a remaining balance.",
                            "${uiState.payments.size} collection entries recorded.",
                            if (uiState.pendingOperationsCount > 0) "${uiState.pendingOperationsCount} operations queued for offline sync." else "All records synced with MySQL."),
                        onPrimary = { goBack() }, primaryLabel = "Back")
                    Screen.Profile -> ProfilePage(
                        businessName = uiState.businessName, onBusinessNameChange = { bName -> viewModel.updateProfile(bName, uiState.ownerName, uiState.ownerPhone, uiState.ownerEmail, uiState.businessAddress, uiState.gstinNumber) },
                        ownerName = uiState.ownerName, onOwnerNameChange = { oName -> viewModel.updateProfile(uiState.businessName, oName, uiState.ownerPhone, uiState.ownerEmail, uiState.businessAddress, uiState.gstinNumber) },
                        ownerPhone = uiState.ownerPhone, onOwnerPhoneChange = { oPhone -> viewModel.updateProfile(uiState.businessName, uiState.ownerName, oPhone, uiState.ownerEmail, uiState.businessAddress, uiState.gstinNumber) },
                        ownerEmail = uiState.ownerEmail, onOwnerEmailChange = { oEmail -> viewModel.updateProfile(uiState.businessName, uiState.ownerName, uiState.ownerPhone, oEmail, uiState.businessAddress, uiState.gstinNumber) },
                        businessAddress = uiState.businessAddress, onAddressChange = { bAddr -> viewModel.updateProfile(uiState.businessName, uiState.ownerName, uiState.ownerPhone, uiState.ownerEmail, bAddr, uiState.gstinNumber) },
                        gstinNumber = uiState.gstinNumber, onGstinChange = { gst -> viewModel.updateProfile(uiState.businessName, uiState.ownerName, uiState.ownerPhone, uiState.ownerEmail, uiState.businessAddress, gst) },
                        onSave = { },
                        onOpenSettings = { go(Screen.Settings) }
                    )
                    Screen.Settings -> SettingsPage(
                        acceptCash = uiState.acceptCash, onAcceptCashChange = { viewModel.updateSettings(acceptCash = it, acceptUpi = uiState.acceptUpi, acceptBank = uiState.acceptBank, dailyReminders = uiState.dailyReminders, overdueAlerts = uiState.overdueAlerts, defaultCycle = uiState.defaultCycle, appLockEnabled = uiState.appLockEnabled, lateFeeRule = uiState.lateFeeRule) },
                        acceptUpi = uiState.acceptUpi, onAcceptUpiChange = { viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = it, acceptBank = uiState.acceptBank, dailyReminders = uiState.dailyReminders, overdueAlerts = uiState.overdueAlerts, defaultCycle = uiState.defaultCycle, appLockEnabled = uiState.appLockEnabled, lateFeeRule = uiState.lateFeeRule) },
                        acceptBank = uiState.acceptBank, onAcceptBankChange = { viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = uiState.acceptUpi, acceptBank = it, dailyReminders = uiState.dailyReminders, overdueAlerts = uiState.overdueAlerts, defaultCycle = uiState.defaultCycle, appLockEnabled = uiState.appLockEnabled, lateFeeRule = uiState.lateFeeRule) },
                        dailyReminders = uiState.dailyReminders, onDailyRemindersChange = { viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = uiState.acceptUpi, acceptBank = uiState.acceptBank, dailyReminders = it, overdueAlerts = uiState.overdueAlerts, defaultCycle = uiState.defaultCycle, appLockEnabled = uiState.appLockEnabled, lateFeeRule = uiState.lateFeeRule) },
                        overdueAlerts = uiState.overdueAlerts, onOverdueAlertsChange = { viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = uiState.acceptUpi, acceptBank = uiState.acceptBank, dailyReminders = uiState.dailyReminders, overdueAlerts = it, defaultCycle = uiState.defaultCycle, appLockEnabled = uiState.appLockEnabled, lateFeeRule = uiState.lateFeeRule) },
                        defaultCycle = uiState.defaultCycle, onDefaultCycleChange = { viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = uiState.acceptUpi, acceptBank = uiState.acceptBank, dailyReminders = uiState.dailyReminders, overdueAlerts = uiState.overdueAlerts, defaultCycle = it, appLockEnabled = uiState.appLockEnabled, lateFeeRule = uiState.lateFeeRule) },
                        appLockEnabled = uiState.appLockEnabled, onAppLockChange = { viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = uiState.acceptUpi, acceptBank = uiState.acceptBank, dailyReminders = uiState.dailyReminders, overdueAlerts = uiState.overdueAlerts, defaultCycle = uiState.defaultCycle, appLockEnabled = it, lateFeeRule = uiState.lateFeeRule) },
                        lateFeeRule = uiState.lateFeeRule, onLateFeeRuleChange = { viewModel.updateSettings(acceptCash = uiState.acceptCash, acceptUpi = uiState.acceptUpi, acceptBank = uiState.acceptBank, dailyReminders = uiState.dailyReminders, overdueAlerts = uiState.overdueAlerts, defaultCycle = uiState.defaultCycle, appLockEnabled = uiState.appLockEnabled, lateFeeRule = it) },
                        onSave = { },
                        onOpenProfile = { go(Screen.Profile) },
                        onExportData = { },
                        onLogOut = {
                            viewModel.logout {
                                resetTo(Screen.Welcome)
                            }
                        },
                        onResetData = { viewModel.clearData() }
                    )
                }
            }
        }
    }
}
