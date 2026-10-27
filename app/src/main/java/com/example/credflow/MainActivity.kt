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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val backStack = remember { mutableStateListOf(Screen.Splash) }
    val screen = backStack.lastOrNull() ?: Screen.Splash
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var searchText by remember { mutableStateOf("") }
    var chatQuestion by remember { mutableStateOf("") }
    var chatAnswer by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("Daily") }
    var day by remember { mutableStateOf("Monday") }
    var note by remember { mutableStateOf("") }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var paidCustomer by remember { mutableStateOf<Customer?>(null) }
    var toast by remember { mutableStateOf("") }
    val customers = remember { mutableStateListOf<Customer>() }
    val payments = remember { mutableStateListOf<String>() }

    var businessName by remember { mutableStateOf("Credflow Finance") }
    var ownerName by remember { mutableStateOf("Ramesh Kumar") }
    var ownerPhone by remember { mutableStateOf("9876543210") }
    var ownerEmail by remember { mutableStateOf("ramesh@credflow.com") }
    var businessAddress by remember { mutableStateOf("12 Brigade Road, Bengaluru") }
    var gstinNumber by remember { mutableStateOf("29ABCDE1234F1Z5") }

    var acceptCash by remember { mutableStateOf(true) }
    var acceptUpi by remember { mutableStateOf(true) }
    var acceptBank by remember { mutableStateOf(false) }
    var dailyReminders by remember { mutableStateOf(true) }
    var overdueAlerts by remember { mutableStateOf(true) }
    var defaultCycle by remember { mutableStateOf("Daily") }
    var appLockEnabled by remember { mutableStateOf(false) }
    var lateFeeRule by remember { mutableStateOf("No Charge") }

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
        val parsedAmount = amount.toDoubleOrNull()
        if (name.isBlank() || phone.isBlank() || parsedAmount == null || parsedAmount <= 0) {
            toast = "Enter customer name, phone and a valid amount."
        } else {
            val c = Customer(name.trim(), phone.trim(), parsedAmount, cycle, if (cycle == "Daily") "Daily" else if (cycle == "Monthly") "Monthly" else day, due = parsedAmount, note = note)
            customers.add(c)
            selectedCustomer = c
            toast = "Customer saved successfully."
            val targetScreen = when (cycle) {
                "Daily" -> Screen.DailyCustomers
                "Weekly" -> Screen.WeeklyCustomers
                else -> Screen.MonthlyCustomers
            }
            if (backStack.size > 1) {
                backStack.removeAt(backStack.size - 1)
            }
            go(targetScreen)
        }
    }
    fun recordPayment(c: Customer, value: Double, method: String) {
        val idx = customers.indexOfFirst { it.phone == c.phone && it.cycle == c.cycle }
        if (idx >= 0 && value > 0) {
            val old = customers[idx]
            val updated = old.copy(paid = old.paid + value, due = (old.due - value).coerceAtLeast(0.0))
            customers[idx] = updated
            paidCustomer = updated
            payments.add("${old.name} • ${rupees(value)} • $method")
            selectedCustomer = updated
            toast = "Collection updated."
            go(Screen.CollectionSaved)
        } else toast = "Enter a valid collection amount."
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
                if (toast.isNotBlank()) {
                    SnackbarHost(hostState = remember { SnackbarHostState() }) {
                        Snackbar { Text(toast) }
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
                    Screen.Splash -> WelcomeSplash { go(Screen.Welcome) }
                    Screen.Welcome -> WelcomePage(
                        onLogin = { go(Screen.Login) },
                        onExplore = { go(Screen.Login) }
                    )
                    Screen.Login -> LoginPage(
                        onBack = { goBack() },
                        onLogin = { identity, password ->
                            if (identity.isNotBlank() && password.isNotBlank()) {
                                if (identity.contains("@")) {
                                    ownerEmail = identity
                                } else if (identity.length >= 10) {
                                    ownerPhone = identity
                                }
                                toast = "Welcome back!"
                                resetTo(Screen.Dashboard)
                            } else toast = "Enter a valid email/phone and password."
                        },
                        onSignUp = { nameVal, bNameVal, identity, password ->
                            if (nameVal.isNotBlank()) ownerName = nameVal
                            if (bNameVal.isNotBlank()) businessName = bNameVal
                            if (identity.contains("@")) {
                                ownerEmail = identity
                            } else if (identity.length >= 10) {
                                ownerPhone = identity
                            }
                            toast = "Account created successfully!"
                            resetTo(Screen.Dashboard)
                        },
                        onGoogleSignIn = { chosenName, chosenEmail ->
                            ownerName = chosenName
                            ownerEmail = chosenEmail
                            businessName = "${chosenName.substringBefore(" ")}'s Finance Book"
                            toast = "Signed in as $chosenEmail"
                            resetTo(Screen.Dashboard)
                        }
                    )
                    Screen.Dashboard -> DashboardPage(
                        customers = customers,
                        payments = payments,
                        onCategories = { go(Screen.Categories) },
                        onSearch = { go(Screen.Search) },
                        onReports = { go(Screen.Reports) },
                        onNotifications = { go(Screen.Notifications) },
                        onSettings = { go(Screen.Settings) },
                        onDaily = { go(Screen.DailyCustomers) },
                        onWeekly = { go(Screen.WeeklyCustomers) },
                        onMonthly = { go(Screen.MonthlyCustomers) }
                    )
                    Screen.Categories -> CategoriesPage(
                        customers = customers,
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
                            customers = customers.filter { it.cycle == listCycle },
                            onAdd = { openAdd(listCycle) },
                            onSelect = { selectedCustomer = it; go(when (listCycle) {
                                "Daily" -> Screen.DailyDetails
                                "Weekly" -> Screen.WeeklyOverview
                                else -> Screen.MonthlyDetails
                            }) },
                            onCollect = { selectedCustomer = it; paymentAmount = ""; paymentMethod = "Cash"; go(Screen.CollectionEntry) },
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
                        customer = selectedCustomer,
                        onCollect = { selectedCustomer?.let { paymentAmount = ""; go(Screen.CollectionEntry) } },
                        onHistory = { go(Screen.PaymentHistory) },
                        onBack = { goBack() }
                    )
                    Screen.Monday, Screen.Tuesday, Screen.Wednesday, Screen.Thursday, Screen.Friday, Screen.Saturday, Screen.Sunday -> {
                        val weekday = screen.title.removeSuffix(" Collection")
                        CustomerListPage(
                            title = screen.title, cycle = "Weekly",
                            customers = customers.filter { it.cycle == "Weekly" && it.day.equals(weekday, true) },
                            onAdd = { day = weekday; openAdd("Weekly") },
                            onSelect = { selectedCustomer = it; go(Screen.WeeklyOverview) },
                            onCollect = { selectedCustomer = it; paymentAmount = ""; go(Screen.CollectionEntry) },
                            onWeekday = { day = it; go(weekdayScreen(it)) }
                        )
                    }
                    Screen.CollectionEntry -> CollectionEntryPage(
                        customer = selectedCustomer,
                        amount = paymentAmount, onAmount = { paymentAmount = it },
                        method = paymentMethod, onMethod = { paymentMethod = it },
                        onSave = {
                            val c = selectedCustomer
                            val v = paymentAmount.toDoubleOrNull()
                            if (c != null && v != null && v > 0) recordPayment(c, v, paymentMethod)
                            else toast = "Enter a valid amount."
                        }
                    )
                    Screen.CollectionSaved -> CollectionSavedPage(
                        customer = paidCustomer ?: selectedCustomer,
                        onDone = { resetTo(Screen.Dashboard) },
                        onHistory = { go(Screen.PaymentHistory) }
                    )
                    Screen.PaymentHistory -> SimpleListPage(
                        "Payment History", "Collections recorded in this app",
                        payments.toList().ifEmpty { listOf("No collections recorded yet.") },
                        onPrimary = { goBack() }, primaryLabel = "Back"
                    )
                    Screen.Pending -> CustomerListPage("Pending Collections", "All", customers.filter { it.due > 0 },
                        onAdd = { openAdd("Daily") }, onSelect = { selectedCustomer = it; go(Screen.DailyDetails) },
                        onCollect = { selectedCustomer = it; paymentAmount = ""; go(Screen.CollectionEntry) }, onWeekday = { go(weekdayScreen(it)) })
                    Screen.Overdue -> CustomerListPage("Overdue Customers", "All", customers.filter { it.due > 0 },
                        onAdd = { openAdd("Daily") }, onSelect = { selectedCustomer = it; go(Screen.DailyDetails) },
                        onCollect = { selectedCustomer = it; paymentAmount = ""; go(Screen.CollectionEntry) }, onWeekday = { go(weekdayScreen(it)) })
                    Screen.AIAssistant -> AIAssistantPage(
                        onChat = { go(Screen.AIChatbot) }, onPrediction = { go(Screen.AIPrediction) },
                        onForecast = { go(Screen.Forecast) }, onReports = { go(Screen.Reports) }
                    )
                    Screen.AIChatbot -> AIChatPage(
                        question = chatQuestion, onQuestion = { chatQuestion = it },
                        answer = chatAnswer,
                        onAsk = {
                            val q = chatQuestion.lowercase()
                            chatAnswer = when {
                                "overdue" in q || "pending" in q -> "There are ${customers.count { it.due > 0 }} customers with a remaining balance of ${rupees(customers.sumOf { it.due })}. This is calculated from the customer records currently saved on this device."
                                "today" in q || "daily" in q -> "Daily customers: ${customers.count { it.cycle == "Daily" }}. Remaining amount: ${rupees(customers.filter { it.cycle == "Daily" }.sumOf { it.due })}."
                                "week" in q || "weekly" in q -> "Weekly customers: ${customers.count { it.cycle == "Weekly" }}. Remaining amount: ${rupees(customers.filter { it.cycle == "Weekly" }.sumOf { it.due })}."
                                "month" in q || "monthly" in q -> "Monthly customers: ${customers.count { it.cycle == "Monthly" }}. Remaining amount: ${rupees(customers.filter { it.cycle == "Monthly" }.sumOf { it.due })}."
                                "collect" in q || "paid" in q -> "Collections recorded: ${payments.size}. Total remaining amount: ${rupees(customers.sumOf { it.due })}."
                                else -> "Try asking: “How much is pending?”, “Show weekly customers”, or “How much remains for daily customers?” This demo answers from records saved on this device."
                            }
                        }
                    )
                    Screen.AIPrediction -> PredictionPage(customers = customers, onDetails = { go(Screen.RiskDetails) })
                    Screen.RiskDetails -> SimpleListPage("Repayment Risk Details", "Demo insight — not a validated prediction model",
                        listOf("Customers with a remaining balance: ${customers.count { it.due > 0 }}",
                            "Total remaining amount: ${rupees(customers.sumOf { it.due })}",
                            "This version uses basic record summaries only. Connect a trained model and historical labelled payments to produce a real risk estimate."),
                        onPrimary = { goBack() }, primaryLabel = "Back to prediction")
                    Screen.Forecast -> SimpleListPage("AI Collection Forecast", "Estimates based on records currently available",
                        listOf("Total remaining amount: ${rupees(customers.sumOf { it.due })}",
                            "Daily remaining: ${rupees(customers.filter { it.cycle == "Daily" }.sumOf { it.due })}",
                            "Weekly remaining: ${rupees(customers.filter { it.cycle == "Weekly" }.sumOf { it.due })}",
                            "Monthly remaining: ${rupees(customers.filter { it.cycle == "Monthly" }.sumOf { it.due })}",
                            "This is a simple summary, not a trained forecasting model."),
                        onPrimary = { go(Screen.WeeklyForecast) }, primaryLabel = "Weekly forecast",
                        onSecondary = { go(Screen.MonthlyForecast) }, secondaryLabel = "Monthly forecast")
                    Screen.WeeklyForecast -> ForecastDetailPage("Weekly Forecast Details", customers, "Weekly", onBack = { goBack() })
                    Screen.MonthlyForecast -> ForecastDetailPage("Monthly Forecast Details", customers, "Monthly", onBack = { goBack() })
                    Screen.Reports -> ReportsPage(customers, payments, onDaily = { go(Screen.DailyReport) }, onWeekly = { go(Screen.WeeklyReport) }, onMonthly = { go(Screen.MonthlyReport) })
                    Screen.DailyReport -> ReportDetailPage("Daily Report Details", customers.filter { it.cycle == "Daily" }, payments, onBack = { goBack() })
                    Screen.WeeklyReport -> ReportDetailPage("Weekly Report Details", customers.filter { it.cycle == "Weekly" }, payments, onBack = { goBack() })
                    Screen.MonthlyReport -> ReportDetailPage("Monthly Report Details", customers.filter { it.cycle == "Monthly" }, payments, onBack = { goBack() })
                    Screen.Search -> SearchPage(
                        query = searchText, onQuery = { searchText = it },
                        customers = customers.filter { it.name.contains(searchText, true) || it.phone.contains(searchText, true) },
                        onSelect = { selectedCustomer = it; go(if (it.cycle == "Daily") Screen.DailyDetails else if (it.cycle == "Weekly") Screen.WeeklyOverview else Screen.MonthlyDetails) },
                        onCollect = { selectedCustomer = it; paymentAmount = ""; go(Screen.CollectionEntry) }
                    )
                    Screen.Notifications -> SimpleListPage("Notifications", "Collection reminders and updates",
                        listOf("${customers.count { it.due > 0 }} customers have a remaining balance.",
                            "${payments.size} collection entries recorded.",
                            "Reminder notifications are UI-only in this starter; device notifications need to be configured separately."),
                        onPrimary = { goBack() }, primaryLabel = "Back")
                    Screen.Profile -> ProfilePage(
                        businessName = businessName, onBusinessNameChange = { businessName = it },
                        ownerName = ownerName, onOwnerNameChange = { ownerName = it },
                        ownerPhone = ownerPhone, onOwnerPhoneChange = { ownerPhone = it },
                        ownerEmail = ownerEmail, onOwnerEmailChange = { ownerEmail = it },
                        businessAddress = businessAddress, onAddressChange = { businessAddress = it },
                        gstinNumber = gstinNumber, onGstinChange = { gstinNumber = it },
                        onSave = { toast = "Business profile saved successfully." },
                        onOpenSettings = { go(Screen.Settings) }
                    )
                    Screen.Settings -> SettingsPage(
                        acceptCash = acceptCash, onAcceptCashChange = { acceptCash = it },
                        acceptUpi = acceptUpi, onAcceptUpiChange = { acceptUpi = it },
                        acceptBank = acceptBank, onAcceptBankChange = { acceptBank = it },
                        dailyReminders = dailyReminders, onDailyRemindersChange = { dailyReminders = it },
                        overdueAlerts = overdueAlerts, onOverdueAlertsChange = { overdueAlerts = it },
                        defaultCycle = defaultCycle, onDefaultCycleChange = { defaultCycle = it },
                        appLockEnabled = appLockEnabled, onAppLockChange = { appLockEnabled = it },
                        lateFeeRule = lateFeeRule, onLateFeeRuleChange = { lateFeeRule = it },
                        onSave = { toast = "Settings saved successfully." },
                        onOpenProfile = { go(Screen.Profile) },
                        onExportData = { toast = "Collection data exported successfully." },
                        onLogOut = { resetTo(Screen.Welcome) },
                        onResetData = {
                            customers.clear()
                            payments.clear()
                            toast = "All customer and payment records cleared."
                        }
                    )
                }
            }
        }
    }
}
