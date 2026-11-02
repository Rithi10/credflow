package com.example.credflow

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credflow.domain.model.LoanCalculator
import java.time.LocalDate
import java.util.Calendar

@Composable
fun AddCustomerPage(
    cycle: String,
    onCycleChange: (String) -> Unit = {},
    name: String, onName: (String) -> Unit,
    phone: String, onPhone: (String) -> Unit,
    amount: String, onAmount: (String) -> Unit,
    interest: String = "0", onInterest: (String) -> Unit = {},
    interestType: String = "Flat Interest", onInterestType: (String) -> Unit = {},
    period: String = "1", onPeriod: (String) -> Unit = {},
    periodUnit: String = "Months", onPeriodUnit: (String) -> Unit = {},
    day: String, onDay: (String) -> Unit,
    firstDueDate: String = "", onFirstDueDate: (String) -> Unit = {},
    note: String, onNote: (String) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current

    // Live automatic loan calculations
    val parsedPrincipal = amount.toDoubleOrNull() ?: 0.0
    val parsedInterest = interest.toDoubleOrNull() ?: 0.0
    val parsedPeriod = period.toIntOrNull() ?: 1

    val calcResult = remember(parsedPrincipal, parsedInterest, parsedPeriod, periodUnit, cycle, day, firstDueDate) {
        LoanCalculator.calculateLoan(
            principal = parsedPrincipal,
            interestRate = parsedInterest,
            period = parsedPeriod,
            periodUnit = periodUnit,
            frequency = cycle,
            collectionDay = day,
            firstDueDateStr = firstDueDate
        )
    }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Add $cycle Customer", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text("Configure borrower loan terms and collection schedule.", color = Muted, fontSize = 13.sp)
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = onName,
                label = { Text("Customer Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = phone,
                onValueChange = onPhone,
                label = { Text("Mobile Number *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Repayment Frequency
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Repayment Frequency *", fontWeight = FontWeight.SemiBold, color = Navy, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Daily", "Weekly", "Monthly").forEach { freq ->
                        FilterChip(
                            selected = cycle == freq,
                            onClick = {
                                onCycleChange(freq)
                                if (freq == "Daily" && periodUnit != "Days") onPeriodUnit("Days")
                                else if (freq == "Weekly" && periodUnit != "Weeks") onPeriodUnit("Weeks")
                                else if (freq == "Monthly" && periodUnit != "Months") onPeriodUnit("Months")
                            },
                            label = { Text(freq) }
                        )
                    }
                }
            }
        }

        // Principal Amount
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = onAmount,
                label = { Text("Loan Amount / Principal (₹) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Interest Rate & Interest Type
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = interest,
                    onValueChange = onInterest,
                    label = { Text("Interest Rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = interestType,
                    onValueChange = onInterestType,
                    label = { Text("Interest Type") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Repayment Period & Period Unit
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Repayment Duration *", fontWeight = FontWeight.SemiBold, color = Navy, fontSize = 14.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = period,
                        onValueChange = onPeriod,
                        label = { Text("Period") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Row(Modifier.weight(2f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Days", "Weeks", "Months").forEach { unit ->
                            FilterChip(
                                selected = periodUnit == unit,
                                onClick = { onPeriodUnit(unit) },
                                label = { Text(unit, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Collection Day (for Weekly)
        if (cycle == "Weekly") {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Collection Day *", fontWeight = FontWeight.SemiBold, color = Navy, fontSize = 14.sp)
                    listOf(
                        listOf("Monday", "Tuesday"),
                        listOf("Wednesday", "Thursday"),
                        listOf("Friday", "Saturday"),
                        listOf("Sunday")
                    ).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { weekday ->
                                FilterChip(
                                    selected = day == weekday,
                                    onClick = { onDay(weekday) },
                                    label = { Text(weekday) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // First Payment Due Date Picker
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("First Payment Due Date", fontWeight = FontWeight.SemiBold, color = Navy, fontSize = 14.sp)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (firstDueDate.isNotBlank()) firstDueDate else calcResult.firstDueDate,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Navy
                    )
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val dateStr = String.format("%04d-%02d-%02d", y, m + 1, d)
                                    onFirstDueDate(dateStr)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Date", fontSize = 12.sp)
                    }
                }
            }
        }

        // LIVE CALCULATION PREVIEW CARD
        item {
            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Blue)
                        Spacer(Modifier.width(8.dp))
                        Text("Live Loan Calculation Preview", fontWeight = FontWeight.Bold, color = Navy, fontSize = 15.sp)
                    }
                    HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 0.5.dp)

                    CalculationRow("Principal Amount", rupees(calcResult.principal))
                    CalculationRow("Interest (${calcResult.interestRate}%)", rupees(calcResult.interestAmount))
                    CalculationRow("Total Repayment", rupees(calcResult.totalRepayment), isBold = true, color = Navy)
                    CalculationRow("Total Installments", "${calcResult.numberOfInstallments} payments")
                    CalculationRow(
                        "$cycle Installment",
                        rupees(calcResult.installmentAmount),
                        isBold = true,
                        color = Blue
                    )
                    CalculationRow("First Due Date", calcResult.firstDueDate)
                    CalculationRow("Final Due Date", calcResult.finalDueDate)
                }
            }
        }

        item {
            OutlinedTextField(
                value = note,
                onValueChange = onNote,
                label = { Text("Notes (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = onSave,
                enabled = parsedPrincipal > 0,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text("Save Customer & Schedule")
            }
        }

        item {
            Text(
                "Credflow securely computes and stores the complete repayment schedule in MySQL.",
                color = Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CalculationRow(label: String, value: String, isBold: Boolean = false, color: Color = Color.Black) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Muted)
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}
