package com.example.credflow.data.remote.dto

import org.json.JSONObject

data class BusinessInfoDto(
    val ownerName: String = "",
    val businessName: String = "Credflow Finance",
    val phone: String = "",
    val email: String = "",
    val gstinNumber: String = "",
    val businessAddress: String = ""
) {
    companion object {
        fun fromJson(json: JSONObject?): BusinessInfoDto {
            if (json == null) return BusinessInfoDto()
            return BusinessInfoDto(
                ownerName = json.optString("owner_name", ""),
                businessName = json.optString("business_name", "Credflow Finance"),
                phone = json.optString("phone", ""),
                email = json.optString("email", ""),
                gstinNumber = json.optString("gstin_number", ""),
                businessAddress = json.optString("business_address", "")
            )
        }
    }
}

data class LedgerCustomerDto(
    val id: Int = 0,
    val userId: Int = 0,
    val name: String = "",
    val phone: String = "",
    val amount: Double = 0.0,
    val interest: Double = 0.0,
    val interestType: String = "Flat Interest",
    val totalInterest: Double = 0.0,
    val period: Int = 1,
    val periodUnit: String = "Months",
    val installmentAmount: Double = 0.0,
    val totalInstallments: Int = 1,
    val firstDueDate: String = "",
    val finalDueDate: String = "",
    val cycle: String = "Daily",
    val collectionDay: String = "Monday",
    val totalDue: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val loanStatus: String = "ACTIVE",
    val closedAt: String = "",
    val settlementAmount: Double = 0.0,
    val settlementType: String = "",
    val closureNote: String = "",
    val note: String = "",
    val createdAt: String = "",
    val status: String = "ACTIVE"
) {
    companion object {
        fun fromJson(json: JSONObject?): LedgerCustomerDto {
            if (json == null) return LedgerCustomerDto()
            return LedgerCustomerDto(
                id = json.optInt("id", 0),
                userId = json.optInt("user_id", 0),
                name = json.optString("name", ""),
                phone = json.optString("phone", ""),
                amount = json.optDouble("amount", 0.0),
                interest = json.optDouble("interest", 0.0),
                interestType = json.optString("interest_type", "Flat Interest"),
                totalInterest = json.optDouble("total_interest", 0.0),
                period = json.optInt("period", 1),
                periodUnit = json.optString("period_unit", "Months"),
                installmentAmount = json.optDouble("installment_amount", 0.0),
                totalInstallments = json.optInt("total_installments", 1),
                firstDueDate = json.optString("first_due_date", ""),
                finalDueDate = json.optString("final_due_date", ""),
                cycle = json.optString("cycle", "Daily"),
                collectionDay = json.optString("collection_day", "Monday"),
                totalDue = json.optDouble("total_due", 0.0),
                paidAmount = json.optDouble("paid_amount", 0.0),
                remainingBalance = json.optDouble("remaining_balance", 0.0),
                loanStatus = json.optString("loan_status", "ACTIVE"),
                closedAt = json.optString("closed_at", ""),
                settlementAmount = json.optDouble("settlement_amount", 0.0),
                settlementType = json.optString("settlement_type", ""),
                closureNote = json.optString("closure_note", ""),
                note = json.optString("note", ""),
                createdAt = json.optString("created_at", ""),
                status = json.optString("status", "ACTIVE")
            )
        }
    }
}

data class LedgerSummaryDto(
    val loanAmount: Double = 0.0,
    val interestRate: Double = 0.0,
    val interestType: String = "Flat Interest",
    val totalInterest: Double = 0.0,
    val totalRepayment: Double = 0.0,
    val totalPaid: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val outstandingPrincipal: Double = 0.0,
    val outstandingInterest: Double = 0.0,
    val installmentAmount: Double = 0.0,
    val totalInstallments: Int = 0,
    val paidInstallmentsCount: Int = 0,
    val pendingInstallmentsCount: Int = 0,
    val overdueInstallmentsCount: Int = 0,
    val pendingAmount: Double = 0.0,
    val overdueAmount: Double = 0.0,
    val nextDueDate: String = "",
    val overdueDays: Int = 0,
    val paymentsCount: Int = 0,
    val status: String = "ACTIVE"
) {
    companion object {
        fun fromJson(json: JSONObject?): LedgerSummaryDto {
            if (json == null) return LedgerSummaryDto()
            return LedgerSummaryDto(
                loanAmount = json.optDouble("loan_amount", 0.0),
                interestRate = json.optDouble("interest_rate", 0.0),
                interestType = json.optString("interest_type", "Flat Interest"),
                totalInterest = json.optDouble("total_interest", 0.0),
                totalRepayment = json.optDouble("total_repayment", 0.0),
                totalPaid = json.optDouble("total_paid", 0.0),
                remainingBalance = json.optDouble("remaining_balance", 0.0),
                outstandingPrincipal = json.optDouble("outstanding_principal", 0.0),
                outstandingInterest = json.optDouble("outstanding_interest", 0.0),
                installmentAmount = json.optDouble("installment_amount", 0.0),
                totalInstallments = json.optInt("total_installments", 0),
                paidInstallmentsCount = json.optInt("paid_installments_count", 0),
                pendingInstallmentsCount = json.optInt("pending_installments_count", 0),
                overdueInstallmentsCount = json.optInt("overdue_installments_count", 0),
                pendingAmount = json.optDouble("pending_amount", 0.0),
                overdueAmount = json.optDouble("overdue_amount", 0.0),
                nextDueDate = json.optString("next_due_date", ""),
                overdueDays = json.optInt("overdue_days", 0),
                paymentsCount = json.optInt("payments_count", 0),
                status = json.optString("status", "ACTIVE")
            )
        }
    }
}

data class LedgerInstallmentDto(
    val id: Int = 0,
    val customerId: Int = 0,
    val installmentNumber: Int = 0,
    val dueDate: String = "",
    val amount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val unpaidAmount: Double = 0.0,
    val status: String = "Pending",
    val isOverdue: Boolean = false,
    val isPending: Boolean = false,
    val daysOverdue: Int = 0
) {
    companion object {
        fun fromJson(json: JSONObject): LedgerInstallmentDto {
            return LedgerInstallmentDto(
                id = json.optInt("id", 0),
                customerId = json.optInt("customer_id", 0),
                installmentNumber = json.optInt("installment_number", 0),
                dueDate = json.optString("due_date", ""),
                amount = json.optDouble("amount", 0.0),
                paidAmount = json.optDouble("paid_amount", 0.0),
                unpaidAmount = json.optDouble("unpaid_amount", 0.0),
                status = json.optString("status", "Pending"),
                isOverdue = json.optBoolean("is_overdue", false),
                isPending = json.optBoolean("is_pending", false),
                daysOverdue = json.optInt("days_overdue", 0)
            )
        }
    }
}

data class LedgerPaymentDto(
    val id: Int = 0,
    val customerId: Int = 0,
    val customerName: String = "",
    val customerPhone: String = "",
    val amount: Double = 0.0,
    val paymentDate: String = "",
    val note: String = "Cash",
    val receiptNumber: String = "",
    val totalLoanAmount: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val createdAt: String = ""
) {
    companion object {
        fun fromJson(json: JSONObject): LedgerPaymentDto {
            return LedgerPaymentDto(
                id = json.optInt("id", 0),
                customerId = json.optInt("customer_id", 0),
                customerName = json.optString("customer_name", ""),
                customerPhone = json.optString("customer_phone", ""),
                amount = json.optDouble("amount", 0.0),
                paymentDate = json.optString("payment_date", ""),
                note = json.optString("note", "Cash"),
                receiptNumber = json.optString("receipt_number", ""),
                totalLoanAmount = json.optDouble("total_loan_amount", 0.0),
                totalPaidAmount = json.optDouble("total_paid_amount", 0.0),
                remainingBalance = json.optDouble("remaining_balance", 0.0),
                createdAt = json.optString("created_at", "")
            )
        }
    }
}

data class LoanSettlementDto(
    val id: Int = 0,
    val customerId: Int = 0,
    val paymentId: Int? = null,
    val settlementType: String = "FULL",
    val settlementAmount: Double = 0.0,
    val outstandingBefore: Double = 0.0,
    val outstandingAfter: Double = 0.0,
    val paymentMethod: String = "Cash",
    val note: String = "",
    val loanClosed: Boolean = false,
    val createdAt: String = ""
) {
    companion object {
        fun fromJson(json: JSONObject): LoanSettlementDto {
            val pid = if (json.isNull("payment_id")) null else json.optInt("payment_id", 0).takeIf { it > 0 }
            return LoanSettlementDto(
                id = json.optInt("id", 0),
                customerId = json.optInt("customer_id", 0),
                paymentId = pid,
                settlementType = json.optString("settlement_type", "FULL"),
                settlementAmount = json.optDouble("settlement_amount", 0.0),
                outstandingBefore = json.optDouble("outstanding_before", 0.0),
                outstandingAfter = json.optDouble("outstanding_after", 0.0),
                paymentMethod = json.optString("payment_method", "Cash"),
                note = json.optString("note", ""),
                loanClosed = json.optBoolean("loan_closed", false),
                createdAt = json.optString("created_at", "")
            )
        }
    }
}

data class CustomerLedgerDto(
    val success: Boolean = false,
    val generatedAt: String = "",
    val business: BusinessInfoDto = BusinessInfoDto(),
    val customer: LedgerCustomerDto = LedgerCustomerDto(),
    val summary: LedgerSummaryDto = LedgerSummaryDto(),
    val installments: List<LedgerInstallmentDto> = emptyList(),
    val pendingInstallments: List<LedgerInstallmentDto> = emptyList(),
    val overdueInstallments: List<LedgerInstallmentDto> = emptyList(),
    val payments: List<LedgerPaymentDto> = emptyList(),
    val settlements: List<LoanSettlementDto> = emptyList(),
    val error: String? = null
) {
    companion object {
        private fun parseInstallmentArray(json: JSONObject, key: String): List<LedgerInstallmentDto> {
            val arr = json.optJSONArray(key) ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { add(LedgerInstallmentDto.fromJson(it)) }
                }
            }
        }

        fun fromJson(json: JSONObject): CustomerLedgerDto {
            val error = json.optString("error").takeIf { it.isNotBlank() }
            val payArr = json.optJSONArray("payments")
            val payments = buildList {
                if (payArr != null) {
                    for (i in 0 until payArr.length()) {
                        payArr.optJSONObject(i)?.let { add(LedgerPaymentDto.fromJson(it)) }
                    }
                }
            }
            val setArr = json.optJSONArray("settlements")
            val settlements = buildList {
                if (setArr != null) {
                    for (i in 0 until setArr.length()) {
                        setArr.optJSONObject(i)?.let { add(LoanSettlementDto.fromJson(it)) }
                    }
                }
            }

            return CustomerLedgerDto(
                success = json.optBoolean("success", error == null),
                generatedAt = json.optString("generated_at", ""),
                business = BusinessInfoDto.fromJson(json.optJSONObject("business")),
                customer = LedgerCustomerDto.fromJson(json.optJSONObject("customer")),
                summary = LedgerSummaryDto.fromJson(json.optJSONObject("summary")),
                installments = parseInstallmentArray(json, "installments"),
                pendingInstallments = parseInstallmentArray(json, "pending_installments"),
                overdueInstallments = parseInstallmentArray(json, "overdue_installments"),
                payments = payments,
                settlements = settlements,
                error = error
            )
        }
    }
}

data class ExportReportSummaryDto(
    val totalCustomers: Int = 0,
    val totalPrincipal: Double = 0.0,
    val totalInterest: Double = 0.0,
    val totalRepayment: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val totalOverdueAmount: Double = 0.0,
    val overdueCustomersCount: Int = 0,
    val totalPaymentsCount: Int = 0,
    val filteredPaymentsTotal: Double = 0.0
) {
    companion object {
        fun fromJson(json: JSONObject?): ExportReportSummaryDto {
            if (json == null) return ExportReportSummaryDto()
            return ExportReportSummaryDto(
                totalCustomers = json.optInt("total_customers", 0),
                totalPrincipal = json.optDouble("total_principal", 0.0),
                totalInterest = json.optDouble("total_interest", 0.0),
                totalRepayment = json.optDouble("total_repayment", 0.0),
                totalCollected = json.optDouble("total_collected", 0.0),
                totalOutstanding = json.optDouble("total_outstanding", 0.0),
                totalOverdueAmount = json.optDouble("total_overdue_amount", 0.0),
                overdueCustomersCount = json.optInt("overdue_customers_count", 0),
                totalPaymentsCount = json.optInt("total_payments_count", 0),
                filteredPaymentsTotal = json.optDouble("filtered_payments_total", 0.0)
            )
        }
    }
}

data class ExportCustomerRowDto(
    val id: Int = 0,
    val name: String = "",
    val phone: String = "",
    val cycle: String = "",
    val collectionDay: String = "",
    val loanAmount: Double = 0.0,
    val interestRate: Double = 0.0,
    val interestType: String = "Flat Interest",
    val totalInterest: Double = 0.0,
    val totalDue: Double = 0.0,
    val totalPaid: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val installmentAmount: Double = 0.0,
    val totalInstallments: Int = 0,
    val paidInstallmentsCount: Int = 0,
    val pendingInstallmentsCount: Int = 0,
    val overdueInstallmentsCount: Int = 0,
    val overdueAmount: Double = 0.0,
    val overdueDays: Int = 0,
    val nextDueDate: String = "",
    val lastPaymentDate: String = "",
    val paymentsCount: Int = 0,
    val status: String = "ACTIVE"
) {
    companion object {
        fun fromJson(json: JSONObject): ExportCustomerRowDto {
            return ExportCustomerRowDto(
                id = json.optInt("id", 0),
                name = json.optString("name", ""),
                phone = json.optString("phone", ""),
                cycle = json.optString("cycle", ""),
                collectionDay = json.optString("collection_day", ""),
                loanAmount = json.optDouble("loan_amount", 0.0),
                interestRate = json.optDouble("interest_rate", 0.0),
                interestType = json.optString("interest_type", "Flat Interest"),
                totalInterest = json.optDouble("total_interest", 0.0),
                totalDue = json.optDouble("total_due", 0.0),
                totalPaid = json.optDouble("total_paid", 0.0),
                remainingBalance = json.optDouble("remaining_balance", 0.0),
                installmentAmount = json.optDouble("installment_amount", 0.0),
                totalInstallments = json.optInt("total_installments", 0),
                paidInstallmentsCount = json.optInt("paid_installments_count", 0),
                pendingInstallmentsCount = json.optInt("pending_installments_count", 0),
                overdueInstallmentsCount = json.optInt("overdue_installments_count", 0),
                overdueAmount = json.optDouble("overdue_amount", 0.0),
                overdueDays = json.optInt("overdue_days", 0),
                nextDueDate = json.optString("next_due_date", ""),
                lastPaymentDate = json.optString("last_payment_date", ""),
                paymentsCount = json.optInt("payments_count", 0),
                status = json.optString("status", "ACTIVE")
            )
        }
    }
}

data class ExportPaymentRowDto(
    val paymentId: Int = 0,
    val customerId: Int = 0,
    val customerName: String = "",
    val customerPhone: String = "",
    val cycle: String = "",
    val paymentDate: String = "",
    val amount: Double = 0.0,
    val method: String = "Cash",
    val createdAt: String = ""
) {
    companion object {
        fun fromJson(json: JSONObject): ExportPaymentRowDto {
            return ExportPaymentRowDto(
                paymentId = json.optInt("payment_id", 0),
                customerId = json.optInt("customer_id", 0),
                customerName = json.optString("customer_name", ""),
                customerPhone = json.optString("customer_phone", ""),
                cycle = json.optString("cycle", ""),
                paymentDate = json.optString("payment_date", ""),
                amount = json.optDouble("amount", 0.0),
                method = json.optString("method", "Cash"),
                createdAt = json.optString("created_at", "")
            )
        }
    }
}

data class ExportInstallmentRowDto(
    val customerId: Int = 0,
    val customerName: String = "",
    val customerPhone: String = "",
    val cycle: String = "",
    val installmentNumber: Int = 0,
    val dueDate: String = "",
    val amount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val unpaidAmount: Double = 0.0,
    val status: String = "",
    val daysOverdue: Int = 0
) {
    companion object {
        fun fromJson(json: JSONObject): ExportInstallmentRowDto {
            return ExportInstallmentRowDto(
                customerId = json.optInt("customer_id", 0),
                customerName = json.optString("customer_name", ""),
                customerPhone = json.optString("customer_phone", ""),
                cycle = json.optString("cycle", ""),
                installmentNumber = json.optInt("installment_number", 0),
                dueDate = json.optString("due_date", ""),
                amount = json.optDouble("amount", 0.0),
                paidAmount = json.optDouble("paid_amount", 0.0),
                unpaidAmount = json.optDouble("unpaid_amount", 0.0),
                status = json.optString("status", ""),
                daysOverdue = json.optInt("days_overdue", 0)
            )
        }
    }
}

data class ExportReportDataDto(
    val success: Boolean = false,
    val generatedAt: String = "",
    val cycleFilter: String = "all",
    val startDate: String? = null,
    val endDate: String? = null,
    val business: BusinessInfoDto = BusinessInfoDto(),
    val summary: ExportReportSummaryDto = ExportReportSummaryDto(),
    val customers: List<ExportCustomerRowDto> = emptyList(),
    val payments: List<ExportPaymentRowDto> = emptyList(),
    val pendingAndOverdueInstallments: List<ExportInstallmentRowDto> = emptyList(),
    val error: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): ExportReportDataDto {
            val error = json.optString("error").takeIf { it.isNotBlank() }
            val filterObj = json.optJSONObject("filter")

            val customersArr = json.optJSONArray("customers")
            val customers = buildList {
                if (customersArr != null) {
                    for (i in 0 until customersArr.length()) {
                        customersArr.optJSONObject(i)?.let { add(ExportCustomerRowDto.fromJson(it)) }
                    }
                }
            }

            val paymentsArr = json.optJSONArray("payments")
            val payments = buildList {
                if (paymentsArr != null) {
                    for (i in 0 until paymentsArr.length()) {
                        paymentsArr.optJSONObject(i)?.let { add(ExportPaymentRowDto.fromJson(it)) }
                    }
                }
            }

            val instArr = json.optJSONArray("pending_and_overdue_installments")
            val installments = buildList {
                if (instArr != null) {
                    for (i in 0 until instArr.length()) {
                        instArr.optJSONObject(i)?.let { add(ExportInstallmentRowDto.fromJson(it)) }
                    }
                }
            }

            return ExportReportDataDto(
                success = json.optBoolean("success", error == null),
                generatedAt = json.optString("generated_at", ""),
                cycleFilter = filterObj?.optString("cycle", "all") ?: "all",
                startDate = filterObj?.optString("start_date")?.takeIf { it.isNotBlank() && it != "null" },
                endDate = filterObj?.optString("end_date")?.takeIf { it.isNotBlank() && it != "null" },
                business = BusinessInfoDto.fromJson(json.optJSONObject("business")),
                summary = ExportReportSummaryDto.fromJson(json.optJSONObject("summary")),
                customers = customers,
                payments = payments,
                pendingAndOverdueInstallments = installments,
                error = error
            )
        }
    }
}

