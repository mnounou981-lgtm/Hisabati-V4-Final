package com.example.data.backup

import com.example.data.local.*
import com.example.domain.LedgerCalculator
import com.example.domain.MoneyFormat
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = false)
data class SandooqiBackupBundle(
    val app: String = "Sandooqi",
    val version: Int = 2,
    val exportedAt: String,
    val settings: SandooqiSettings,
    val transactions: List<TransactionEntity>,
    val debts: List<CreditorDebtEntity>,
    val obligations: List<ObligationEntity>,
    val facPlans: List<FacPlanEntity>,
    val goals: List<SavingsGoalEntity>,
    val seasonalBudgets: List<SeasonalBudgetEntity>,
    val orders: List<TailoringOrderEntity>,
    val sewServices: List<SewServiceEntity> = emptyList(),
    val sewProjects: List<SewProjectEntity> = emptyList(),
    val sewMaterials: List<SewMaterialEntity>,
    val measurements: List<CustomerMeasurementEntity>,
    val marketItems: List<MarketItemEntity>,
    val priceChecks: List<PriceCheckEntity>,
    val warranties: List<WarrantyEntity>,
    val monthRecords: List<MonthRecordEntity>
)

object BackupManager {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(SandooqiBackupBundle::class.java).indent("  ")

    fun encodeJson(bundle: SandooqiBackupBundle): String = adapter.toJson(bundle)

    fun decodeJson(raw: String): SandooqiBackupBundle? {
        return try {
            val parsed = adapter.fromJson(raw) ?: return null
            if (parsed.app != "Sandooqi") return null
            parsed
        } catch (_: Exception) {
            null
        }
    }

    fun buildMonthlyCsv(
        ym: String,
        transactions: List<TransactionEntity>,
        orders: List<TailoringOrderEntity>
    ): String {
        val sb = StringBuilder("\uFEFF") // UTF-8 BOM for Arabic Excel
        sb.appendLine("النوع;التاريخ;البيان;الحساب;التصنيف;المبلغ (دج);ملاحظة")
        for (tx in transactions.filter { it.ym == ym }) {
            val accLabel = when (tx.accountId) {
                "personal" -> "الحساب الشخصي"
                "sewing" -> "حساب الخياطة"
                else -> "الحساب العائلي"
            }
            val typeLabel = when {
                LedgerCalculator.isInternalTransfer(tx) -> "تحويل داخلي"
                tx.isCreditPurchase -> "شراء بالدين"
                tx.type == "in" -> "وارد"
                else -> "صادر"
            }
            sb.appendLine(
                listOf(
                    typeLabel,
                    tx.dateISO,
                    tx.name,
                    accLabel,
                    tx.expenseCategory.ifBlank { tx.category },
                    MoneyFormat.formatNumberOnly(tx.amountCents),
                    tx.creditor
                ).joinToString(";") { csvEscape(it) }
            )
        }
        if (orders.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("طلبية خياطة;التاريخ;الزبونة;الموديل;الحالة;الإجمالي (دج);المدفوع (دج)")
            for (o in orders) {
                sb.appendLine(
                    listOf(
                        "طلبية",
                        o.dateISO,
                        o.client,
                        o.item,
                        if (o.isCancelled) "ملغاة" else o.stage,
                        MoneyFormat.formatNumberOnly(o.totalCents),
                        MoneyFormat.formatNumberOnly(o.paidCents - o.refundedCents)
                    ).joinToString(";") { csvEscape(it) }
                )
            }
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""
}
