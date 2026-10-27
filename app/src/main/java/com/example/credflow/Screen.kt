package com.example.credflow

enum class Screen(val title: String) {
    Splash("Credflow"), Welcome("Welcome"), Login("Login"), Dashboard("Dashboard"),
    Categories("Customer Categories"), DailyCustomers("Daily Customers"), AddDaily("Add Daily Customer"),
    DailyDetails("Daily Customer Details"), WeeklyCustomers("Weekly Customers"), AddWeekly("Add Weekly Customer"),
    WeeklyOverview("Weekly Collection Overview"), Monday("Monday Collection"), Tuesday("Tuesday Collection"),
    Wednesday("Wednesday Collection"), Thursday("Thursday Collection"), Friday("Friday Collection"),
    Saturday("Saturday Collection"), Sunday("Sunday Collection"), MonthlyCustomers("Monthly Customers"),
    AddMonthly("Add Monthly Customer"), MonthlyDetails("Monthly Customer Details"),
    CollectionEntry("Collection Entry"), CollectionSaved("Collection Saved"), PaymentHistory("Payment History"),
    Pending("Pending Collections"), Overdue("Overdue Customers"), AIAssistant("AI Assistant"),
    AIChatbot("AI Chatbot"), AIPrediction("AI Repayment Prediction"), RiskDetails("Repayment Risk Details"),
    Forecast("AI Collection Forecast"), WeeklyForecast("Weekly Forecast Details"),
    MonthlyForecast("Monthly Forecast Details"), Reports("Reports"), DailyReport("Daily Report Details"),
    WeeklyReport("Weekly Report Details"), MonthlyReport("Monthly Report Details"),
    Search("Search Customers"), Notifications("Notifications"), Profile("Business Profile"), Settings("App Settings")
}
