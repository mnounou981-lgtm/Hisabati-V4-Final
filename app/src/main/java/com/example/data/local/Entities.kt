package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ym: String,
    val dateISO: String,
    val accountId: String, // "family", "personal", "sewing"
    val type: String,      // "in", "out", "transfer"
    val category: String,  // "salary","shop","bill","fam","fac","srv","sav","extra","sew_in","sew_out","order_fabric_purchase","order_refund","account_transfer","debt_payment"
    val name: String,
    val amountCents: Long,
    val expenseCategory: String = "",
    val creditor: String = "",
    val transferId: Long? = null,
    val transferFromAccountId: String? = null,
    val transferToAccountId: String? = null,
    val orderId: Long? = null,
    val homeDebtId: Long? = null,
    val isCreditPurchase: Boolean = false,
    val personName: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "creditor_debts")
data class CreditorDebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creditor: String,
    val item: String,
    val activity: String = "أخرى",
    val amountCents: Long,
    val paidCents: Long = 0L,
    val dueDateISO: String = "",
    val dateISO: String,
    val ym: String,
    val expenseCategory: String = "",
    val photoUri: String = "",
    val kind: String = "home_debt", // "home_debt","bill_debt","rent_out","rent_in","sew_debt_in","sew_debt_out"
    val rentUnit: String = "",
    val settled: Boolean = false,
    val creditorCapCents: Long = 0L,
    val note: String = ""
)

@Entity(tableName = "obligations")
data class ObligationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountCents: Long,
    val creditor: String = "",
    val startYM: String,
    val paidMonthsCsv: String = "", // comma separated yyyy-MM
    val isRecurringBill: Boolean = false,
    val photoUri: String = ""
)

@Entity(tableName = "fac_plans")
data class FacPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val installmentCents: Long,
    val totalMonths: Int,
    val paidMonths: Int = 0,
    val lastPaidYM: String = "",
    val planType: String = "fac" // "fac" = فاسيليتي, "srv" = خدمات
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetCents: Long,
    val savedCents: Long = 0L,
    val fromHomeCents: Long = 0L,
    val fromSewingCents: Long = 0L,
    val fromExternalCents: Long = 0L,
    val deadlineYM: String = ""
)

@Entity(tableName = "seasonal_budgets")
data class SeasonalBudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "🌙",
    val targetCents: Long,
    val savedCents: Long = 0L,
    val targetYM: String
)

@Entity(tableName = "tailoring_orders")
data class TailoringOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val client: String,
    val phone: String = "",
    val item: String,
    val stage: String = "جديدة",
    val totalCents: Long,
    val paidCents: Long = 0L,
    val refundedCents: Long = 0L,
    val materialCostCents: Long = 0L,
    val laborCents: Long = 0L,
    val adjustmentCents: Long = 0L,
    val dueDateISO: String = "",
    val fittingDateISO: String = "",
    val dateISO: String,
    val photoUri: String = "",
    val fabricsSummary: String = "",
    val suppliesSummary: String = "",
    val measurementsSummary: String = "",
    val checklistMask: Int = 0, // bitmask for 4 prep steps: قص, سرفلة, خياطة, كيّ وتغليف
    val isDone: Boolean = false,
    val isCancelled: Boolean = false,
    val note: String = ""
)

@Entity(tableName = "sew_services")
data class SewServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val client: String,
    val item: String,
    val meters: Double = 0.0,
    val ratePerMeterCents: Long = 0L,
    val priceCents: Long = 0L,
    val threadCostCents: Long = 0L,
    val suppliesCostCents: Long = 0L,
    val paidCents: Long = 0L,
    val dateISO: String,
    val ym: String
)

@Entity(tableName = "sew_projects")
data class SewProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val meters: Double = 0.0,
    val fabricRateCents: Long = 0L,
    val fabricCostCents: Long = 0L,
    val suppliesCostCents: Long = 0L,
    val otherCostCents: Long = 0L,
    val salePriceCents: Long = 0L,
    val paidCents: Long = 0L,
    val status: String = "work", // "work", "ready", "sold"
    val dateISO: String,
    val ym: String
)

@Entity(tableName = "sew_materials")
data class SewMaterialEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String = "متر",
    val qty: Double = 0.0,
    val minQty: Double = 2.0,
    val costPerUnitCents: Long = 0L
)

@Entity(tableName = "customer_measurements")
data class CustomerMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val len: String = "",
    val shoulder: String = "",
    val chest: String = "",
    val waist: String = "",
    val hip: String = "",
    val sleeve: String = "",
    val note: String = ""
)

@Entity(tableName = "market_items")
data class MarketItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String = "حبة",
    val unitPriceCents: Long = 0L,
    val isDone: Boolean = false,
    val category: String = "مواد غذائية"
)

@Entity(tableName = "price_checks")
data class PriceCheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val product: String,
    val store: String,
    val unitPriceCents: Long,
    val dateISO: String
)

@Entity(tableName = "warranties")
data class WarrantyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceName: String,
    val store: String = "",
    val category: String = "أجهزة منزلية",
    val purchaseDateISO: String,
    val expiryDateISO: String,
    val priceCents: Long = 0L,
    val photoUri: String = "",
    val note: String = ""
)

@Entity(tableName = "month_records")
data class MonthRecordEntity(
    @PrimaryKey val ym: String,
    val salary1Cents: Long = 0L,
    val salary2Cents: Long = 0L,
    val carryoverInCents: Long? = null,
    val isClosed: Boolean = false,
    val closedAt: String = "",
    val savedToGoalCents: Long = 0L,
    val carriedForwardCents: Long = 0L
)
