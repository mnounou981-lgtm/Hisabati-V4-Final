package com.example.domain

import com.example.data.local.CreditorDebtEntity
import com.example.data.local.MonthRecordEntity
import com.example.data.local.ObligationEntity
import com.example.data.local.SandooqiSettings
import com.example.data.local.TailoringOrderEntity
import com.example.data.local.TransactionEntity
import kotlin.math.max

data class AccountCashSummary(
    val accountId: String,
    val title: String,
    val openingCents: Long,
    val externalIncomeCents: Long, // salary + other external income
    val cashOutCents: Long,        // cash outflows (non-transfer)
    val transferInCents: Long,
    val transferOutCents: Long,
    val recordedExpenseCents: Long // includes credit purchases & excludes double counting settlements
) {
    val totalInDisplayCents: Long get() = openingCents + externalIncomeCents + transferInCents
    val totalOutDisplayCents: Long get() = cashOutCents + transferOutCents
    val balanceCents: Long get() = openingCents + externalIncomeCents + transferInCents - transferOutCents - cashOutCents
}

data class MonthlyCashReport(
    val ym: String,
    val salary1Cents: Long,
    val salary2Cents: Long,
    val family: AccountCashSummary,
    val personal: AccountCashSummary,
    val sewing: AccountCashSummary,
    val isPreviousMonthClosed: Boolean,
    val hasOpenAutoCarryover: Boolean
) {
    val combinedBalanceCents: Long
        get() = family.balanceCents + personal.balanceCents + sewing.balanceCents

    val combinedIncomeCents: Long
        get() = family.externalIncomeCents + personal.externalIncomeCents + sewing.externalIncomeCents

    val combinedOutCashCents: Long
        get() = family.cashOutCents + personal.cashOutCents + sewing.cashOutCents

    val combinedRecordedExpenseCents: Long
        get() = family.recordedExpenseCents + personal.recordedExpenseCents + sewing.recordedExpenseCents

    val combinedNetFlowCents: Long
        get() = combinedIncomeCents - combinedOutCashCents
}

data class CreditorGroupSummary(
    val creditor: String,
    val debts: List<CreditorDebtEntity>,
    val totalAmountCents: Long,
    val paidCents: Long,
    val remainingCents: Long,
    val earliestDueISO: String,
    val hasOverdue: Boolean
)

data class WorkshopSummary(
    val totalOrdersCount: Int,
    val openOrdersCount: Int,
    val activeSalesCents: Long,
    val activeCostsCents: Long,
    val collectedNetCents: Long,
    val clientReceivablesCents: Long,
    val realizedProfitCents: Long,
    val projectedActiveProfitCents: Long
)

object LedgerCalculator {

    fun isInternalTransfer(tx: TransactionEntity): Boolean =
        tx.type == "transfer" || tx.category == "account_transfer" || tx.category == "transfer"

    fun transferDirectionFor(tx: TransactionEntity, accountId: String): String {
        if (!isInternalTransfer(tx)) return ""
        return when {
            tx.transferFromAccountId == accountId -> "out"
            tx.transferToAccountId == accountId -> "in"
            tx.accountId == accountId && tx.type == "out" -> "out"
            tx.accountId == accountId && tx.type == "in" -> "in"
            else -> ""
        }
    }

    /**
     * Exact single-source-of-truth monthly ledger across all 3 accounts.
     * Fixes:
     * 1) Family account chains prior month closing balance automatically if previous month wasn't manually closed,
     *    or respects explicit MonthRecord.carryoverInCents when closed.
     * 2) Credit purchases (isCreditPurchase = true) count toward recordedExpenseCents without draining cash until paid;
     *    debt payments (category = "debt_payment") drain cash without double-counting the original purchase expense.
     */
    fun computeMonthlyReport(
        ym: String,
        allTransactions: List<TransactionEntity>,
        monthRecords: Map<String, MonthRecordEntity>,
        settings: SandooqiSettings
    ): MonthlyCashReport {
        val currentRecord = monthRecords[ym]
        val salary1 = currentRecord?.salary1Cents ?: 0L
        val salary2 = currentRecord?.salary2Cents ?: 0L

        // Compute opening balances from prior history
        var personalOpening = settings.personalOpeningCents
        var sewingOpening = settings.sewingOpeningCents

        for (tx in allTransactions) {
            if (tx.ym >= ym || tx.isCreditPurchase) continue
            if (isInternalTransfer(tx)) {
                when (tx.accountId) {
                    "personal" -> if (transferDirectionFor(tx, "personal") == "in") personalOpening += tx.amountCents else personalOpening -= tx.amountCents
                    "sewing" -> if (transferDirectionFor(tx, "sewing") == "in") sewingOpening += tx.amountCents else sewingOpening -= tx.amountCents
                }
            } else {
                when (tx.accountId) {
                    "personal" -> if (tx.type == "in") personalOpening += tx.amountCents else if (tx.type == "out") personalOpening -= tx.amountCents
                    "sewing" -> if (tx.type == "in") sewingOpening += tx.amountCents else if (tx.type == "out") sewingOpening -= tx.amountCents
                }
            }
        }

        // Family opening balance: explicit closed carryover OR cumulative prior family cashflow
        val explicitCarryover = currentRecord?.carryoverInCents
        val prevYm = DateUtils.shiftYM(ym, -1)
        val prevClosed = monthRecords[prevYm]?.isClosed == true
        var autoFamilyCarryover = 0L
        var hasPriorFamilyActivity = false
        if (explicitCarryover == null) {
            val priorMonths = (allTransactions.filter { it.ym < ym && it.accountId == "family" }.map { it.ym } +
                monthRecords.keys.filter { it < ym }).distinct().sorted()
            var runningFamily = 0L
            for (pm in priorMonths) {
                hasPriorFamilyActivity = true
                val pmRec = monthRecords[pm]
                if (pmRec?.carryoverInCents != null) {
                    runningFamily = pmRec.carryoverInCents
                }
                runningFamily += (pmRec?.salary1Cents ?: 0L) + (pmRec?.salary2Cents ?: 0L)
                for (tx in allTransactions) {
                    if (tx.ym != pm || tx.accountId != "family" || tx.isCreditPurchase) continue
                    if (isInternalTransfer(tx)) {
                        if (transferDirectionFor(tx, "family") == "in") runningFamily += tx.amountCents
                        else if (transferDirectionFor(tx, "family") == "out") runningFamily -= tx.amountCents
                    } else {
                        if (tx.type == "in") runningFamily += tx.amountCents
                        else if (tx.type == "out") runningFamily -= tx.amountCents
                    }
                }
                if (pmRec?.isClosed == true) {
                    runningFamily = pmRec.carriedForwardCents
                }
            }
            autoFamilyCarryover = max(0L, runningFamily)
        }
        val familyOpening = explicitCarryover ?: autoFamilyCarryover

        // Aggregate current month
        val monthTx = allTransactions.filter { it.ym == ym }

        fun summarize(accountId: String, title: String, opening: Long, baseSalary: Long): AccountCashSummary {
            var extIn = baseSalary
            var cashOut = 0L
            var tIn = 0L
            var tOut = 0L
            var creditPurchases = 0L
            var debtPayments = 0L

            for (tx in monthTx) {
                if (tx.accountId != accountId) continue
                if (isInternalTransfer(tx)) {
                    when (transferDirectionFor(tx, accountId)) {
                        "in" -> tIn += tx.amountCents
                        "out" -> tOut += tx.amountCents
                    }
                    continue
                }
                if (tx.isCreditPurchase) {
                    creditPurchases += tx.amountCents
                    continue
                }
                if (tx.type == "in") {
                    extIn += tx.amountCents
                } else if (tx.type == "out") {
                    cashOut += tx.amountCents
                    if (tx.category == "debt_payment") {
                        debtPayments += tx.amountCents
                    }
                }
            }

            val recordedExpense = max(0L, cashOut - debtPayments + creditPurchases)
            return AccountCashSummary(
                accountId = accountId,
                title = title,
                openingCents = opening,
                externalIncomeCents = extIn,
                cashOutCents = cashOut,
                transferInCents = tIn,
                transferOutCents = tOut,
                recordedExpenseCents = recordedExpense
            )
        }

        return MonthlyCashReport(
            ym = ym,
            salary1Cents = salary1,
            salary2Cents = salary2,
            family = summarize("family", "الحساب العائلي", familyOpening, salary1 + salary2),
            personal = summarize("personal", "الحساب الشخصي", personalOpening, 0L),
            sewing = summarize("sewing", "حساب الخياطة", sewingOpening, 0L),
            isPreviousMonthClosed = prevClosed,
            hasOpenAutoCarryover = explicitCarryover == null && hasPriorFamilyActivity && !prevClosed && autoFamilyCarryover > 0L
        )
    }

    fun obligationUnpaidMonths(obligation: ObligationEntity, endYm: String): List<String> {
        val start = obligation.startYM.ifBlank { endYm }
        if (start > endYm) return emptyList()
        val paidSet = obligation.paidMonthsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val result = mutableListOf<String>()
        var cursor = start
        var guard = 0
        while (cursor <= endYm && guard < 36) {
            if (cursor !in paidSet) result.add(cursor)
            cursor = DateUtils.shiftYM(cursor, 1)
            guard++
        }
        return result
    }

    fun groupCreditorDebts(debts: List<CreditorDebtEntity>, todayISO: String): List<CreditorGroupSummary> {
        return debts.filter { it.kind in listOf("home_debt", "bill_debt", "rent_out") }
            .groupBy { it.creditor.trim().ifEmpty { "تاجر / محل" } }
            .map { (creditor, list) ->
                val total = list.sumOf { it.amountCents }
                val paid = list.sumOf { it.paidCents }
                val rem = list.sumOf { max(0L, it.amountCents - it.paidCents) }
                val open = list.filter { it.amountCents > it.paidCents }
                val earliest = open.mapNotNull { it.dueDateISO.takeIf { d -> d.isNotBlank() } }.minOrNull().orEmpty()
                val overdue = earliest.isNotBlank() && earliest < todayISO && rem > 0L
                CreditorGroupSummary(
                    creditor = creditor,
                    debts = list.sortedBy { it.dateISO },
                    totalAmountCents = total,
                    paidCents = paid,
                    remainingCents = rem,
                    earliestDueISO = earliest,
                    hasOverdue = overdue
                )
            }
            .sortedWith(compareByDescending<CreditorGroupSummary> { it.remainingCents > 0L }.thenByDescending { it.remainingCents })
    }

    fun computeWorkshopSummary(orders: List<TailoringOrderEntity>): WorkshopSummary {
        var activeSales = 0L
        var activeCosts = 0L
        var collectedNet = 0L
        var receivables = 0L
        var openCount = 0

        for (o in orders) {
            val netPaid = o.paidCents - o.refundedCents
            collectedNet += netPaid
            if (!o.isCancelled) {
                if (!o.isDone) openCount++
                activeSales += o.totalCents
                activeCosts += o.materialCostCents
                receivables += max(0L, o.totalCents - netPaid)
            }
        }
        return WorkshopSummary(
            totalOrdersCount = orders.size,
            openOrdersCount = openCount,
            activeSalesCents = activeSales,
            activeCostsCents = activeCosts,
            collectedNetCents = collectedNet,
            clientReceivablesCents = receivables,
            realizedProfitCents = collectedNet - activeCosts,
            projectedActiveProfitCents = activeSales - activeCosts
        )
    }

    fun orderRemainingCents(order: TailoringOrderEntity): Long {
        if (order.isCancelled) return 0L
        val netPaid = order.paidCents - order.refundedCents
        return max(0L, order.totalCents - netPaid)
    }
}
