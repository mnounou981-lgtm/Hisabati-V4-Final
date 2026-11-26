package com.example

import com.example.data.local.CreditorDebtEntity
import com.example.data.local.MonthRecordEntity
import com.example.data.local.SandooqiSettings
import com.example.data.local.TailoringOrderEntity
import com.example.data.local.TransactionEntity
import com.example.domain.CatalogDefaults
import com.example.domain.LedgerCalculator
import com.example.domain.MoneyFormat
import com.example.domain.VoiceEntryMode
import com.example.domain.VoiceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SandooqiDomainUnitTest {

    @Test
    fun moneyFormat_parsesArabicAndLatinNumeralsAccurately() {
        assertEquals(150_000_00L, MoneyFormat.parseToCents("١٥٠٠٠٠"))
        assertEquals(125_50L, MoneyFormat.parseToCents("125,5"))
        assertEquals("\u2066150\u00A0000\u2069 دج", MoneyFormat.formatDzd(150_000_00L))
        assertEquals("\u2066-150\u00A0000\u2069 دج", MoneyFormat.formatDzd(-150_000_00L))
        assertEquals("•••• دج", MoneyFormat.formatDzd(150_000_00L, privacyMode = true))
    }

    @Test
    fun ledgerCalculator_autoChainsOpenFamilyCarryoverAndRespectsClosedMonth() {
        val settings = SandooqiSettings()
        val m1 = MonthRecordEntity(
            ym = "2026-10",
            salary1Cents = 120_000_00L,
            salary2Cents = 30_000_00L,
            isClosed = false
        )
        val txs = listOf(
            TransactionEntity(
                id = 1L, ym = "2026-10", dateISO = "2026-10-05",
                accountId = "family", type = "out", category = "shop",
                name = "مقاضي", amountCents = 20_000_00L
            )
        )
        val repNov = LedgerCalculator.computeMonthlyReport("2026-11", txs, mapOf("2026-10" to m1), settings)
        assertEquals(130_000_00L, repNov.family.openingCents)
        assertTrue(repNov.hasOpenAutoCarryover)
    }

    @Test
    fun ledgerCalculator_avoidsDoubleCountingCreditPurchasesOnSettlement() {
        val settings = SandooqiSettings()
        val m1 = MonthRecordEntity(ym = "2026-11", salary1Cents = 100_000_00L)
        val txs = listOf(
            TransactionEntity(
                id = 1L, ym = "2026-11", dateISO = "2026-11-02",
                accountId = "family", type = "out", category = "shop",
                name = "خبز بالدين", amountCents = 2_000_00L, isCreditPurchase = true
            ),
            TransactionEntity(
                id = 2L, ym = "2026-11", dateISO = "2026-11-10",
                accountId = "family", type = "out", category = "debt_payment",
                name = "تسديد البقال", amountCents = 2_000_00L
            )
        )
        val rep = LedgerCalculator.computeMonthlyReport("2026-11", txs, mapOf("2026-11" to m1), settings)
        assertEquals(98_000_00L, rep.family.balanceCents)
        assertEquals(2_000_00L, rep.family.recordedExpenseCents)
    }

    @Test
    fun ledgerCalculator_cashVsCreditVsDebtSettlementKeepsExpectedNetExact() {
        val settings = SandooqiSettings()
        val m = MonthRecordEntity(ym = "2026-11", salary1Cents = 50_000_00L)

        // 1) Cash purchase: 3,000 DZD + Credit purchase: 4,500 DZD
        val creditTx = TransactionEntity(
            id = 1L, ym = "2026-11", dateISO = "2026-11-03",
            accountId = "family", type = "out", category = "shop",
            name = "توابل بالدين", amountCents = 4_500_00L, isCreditPurchase = true, homeDebtId = 10L
        )
        val cashTx = TransactionEntity(
            id = 2L, ym = "2026-11", dateISO = "2026-11-04",
            accountId = "family", type = "out", category = "shop",
            name = "خضر نقدا", amountCents = 3_000_00L, isCreditPurchase = false
        )
        val openDebtBefore = CreditorDebtEntity(
            id = 10L, creditor = "العطار", item = "توابل",
            amountCents = 4_500_00L, paidCents = 0L, dateISO = "2026-11-03", ym = "2026-11"
        )
        val repBeforePay = LedgerCalculator.computeMonthlyReport("2026-11", listOf(creditTx, cashTx), mapOf("2026-11" to m), settings)
        val groupsBefore = LedgerCalculator.groupCreditorDebts(listOf(openDebtBefore), "2026-11-26")
        assertEquals(47_000_00L, repBeforePay.family.balanceCents)
        assertEquals(7_500_00L, repBeforePay.family.recordedExpenseCents)
        assertEquals(4_500_00L, groupsBefore.sumOf { it.remainingCents })
        assertEquals(42_500_00L, repBeforePay.family.balanceCents - groupsBefore.sumOf { it.remainingCents })

        // 2) Partial settlement of 2,000 DZD
        val partialPayTx = TransactionEntity(
            id = 3L, ym = "2026-11", dateISO = "2026-11-12",
            accountId = "family", type = "out", category = "debt_payment",
            name = "تسديد العطار", amountCents = 2_000_00L
        )
        val openDebtAfter = openDebtBefore.copy(paidCents = 2_000_00L)
        val repAfterPay = LedgerCalculator.computeMonthlyReport("2026-11", listOf(creditTx, cashTx, partialPayTx), mapOf("2026-11" to m), settings)
        val groupsAfter = LedgerCalculator.groupCreditorDebts(listOf(openDebtAfter), "2026-11-26")
        assertEquals(45_000_00L, repAfterPay.family.balanceCents)
        assertEquals(7_500_00L, repAfterPay.family.recordedExpenseCents)
        assertEquals(2_500_00L, groupsAfter.sumOf { it.remainingCents })
        assertEquals(42_500_00L, repAfterPay.family.balanceCents - groupsAfter.sumOf { it.remainingCents })
    }

    @Test
    fun ledgerCalculator_transfersAcrossFamilyPersonalAndSewingPreserveCombinedTotal() {
        val settings = SandooqiSettings(
            personalOpeningCents = 10_000_00L,
            sewingOpeningCents = 25_000_00L
        )
        val mOct = MonthRecordEntity(ym = "2026-10", salary1Cents = 80_000_00L)
        val mNov = MonthRecordEntity(ym = "2026-11", salary1Cents = 80_000_00L)
        val txs = listOf(
            // Oct transfer 15,000 from sewing -> family
            TransactionEntity(
                id = 1L, ym = "2026-10", dateISO = "2026-10-15",
                accountId = "sewing", type = "transfer", category = "account_transfer",
                name = "تحويل", amountCents = 15_000_00L,
                transferId = 100L, transferFromAccountId = "sewing", transferToAccountId = "family"
            ),
            TransactionEntity(
                id = 2L, ym = "2026-10", dateISO = "2026-10-15",
                accountId = "family", type = "transfer", category = "account_transfer",
                name = "تحويل", amountCents = 15_000_00L,
                transferId = 100L, transferFromAccountId = "sewing", transferToAccountId = "family"
            ),
            // Nov transfer 5,000 from family -> personal, plus 2,000 personal cash expense & 12,000 sewing income
            TransactionEntity(
                id = 3L, ym = "2026-11", dateISO = "2026-11-05",
                accountId = "family", type = "transfer", category = "account_transfer",
                name = "تحويل", amountCents = 5_000_00L,
                transferId = 200L, transferFromAccountId = "family", transferToAccountId = "personal"
            ),
            TransactionEntity(
                id = 4L, ym = "2026-11", dateISO = "2026-11-05",
                accountId = "personal", type = "transfer", category = "account_transfer",
                name = "تحويل", amountCents = 5_000_00L,
                transferId = 200L, transferFromAccountId = "family", transferToAccountId = "personal"
            ),
            TransactionEntity(
                id = 5L, ym = "2026-11", dateISO = "2026-11-08",
                accountId = "personal", type = "out", category = "extra",
                name = "مصروف شخصي", amountCents = 2_000_00L
            ),
            TransactionEntity(
                id = 6L, ym = "2026-11", dateISO = "2026-11-09",
                accountId = "sewing", type = "in", category = "sew_in",
                name = "مداخيل خياطة", amountCents = 12_000_00L
            )
        )
        val repNov = LedgerCalculator.computeMonthlyReport(
            "2026-11", txs, mapOf("2026-10" to mOct, "2026-11" to mNov), settings
        )
        // Family: Oct(80k + 15k = 95k opening) + Nov(80k - 5k transfer = 170k)
        assertEquals(170_000_00L, repNov.family.balanceCents)
        // Personal: 10k opening + 5k transfer - 2k out = 13k
        assertEquals(13_000_00L, repNov.personal.balanceCents)
        // Sewing: (25k - 15k = 10k opening) + 12k in = 22k
        assertEquals(22_000_00L, repNov.sewing.balanceCents)
        // Combined: 170k + 13k + 22k = 205k
        assertEquals(205_000_00L, repNov.combinedBalanceCents)
    }

    @Test
    fun catalogDefaults_containsSpicesCategoryAndReceivablesGrouping() {
        assertTrue(CatalogDefaults.categoryTabs.any { it.first == "spices" && it.second == "توابل" })
        assertTrue(CatalogDefaults.products.count { it.categoryKey == "spices" } >= 10)
        assertTrue(CatalogDefaults.expenseCategories.contains("توابل وبهارات"))
        listOf("مواد غذائية", "دواء", "كنكايري", "أدوات مدرسية", "دخان وتبغ", "خضر", "لحوم", "دجاج").forEach {
            assertTrue("Missing debt activity: $it", CatalogDefaults.defaultDebtActivities.contains(it))
        }

        val rec = CreditorDebtEntity(
            id = 1L, creditor = "سعيد", item = "سلفة",
            amountCents = 10_000_00L, paidCents = 3_000_00L,
            dateISO = "2026-11-01", ym = "2026-11", kind = "home_debt_in"
        )
        val recGroups = LedgerCalculator.groupReceivableDebts(listOf(rec), "2026-11-26")
        assertEquals(1, recGroups.size)
        assertEquals(7_000_00L, recGroups.first().remainingCents)
    }

    @Test
    fun workshopSummary_excludesCancelledOrderCostsFromActiveProfit() {
        val orders = listOf(
            TailoringOrderEntity(
                id = 1L, client = "سارة", item = "قفطان",
                totalCents = 12_000_00L, paidCents = 12_000_00L,
                materialCostCents = 4_000_00L, dateISO = "2026-11-01"
            ),
            TailoringOrderEntity(
                id = 2L, client = "ليلى", item = "فستان",
                totalCents = 10_000_00L, paidCents = 3_000_00L, refundedCents = 3_000_00L,
                materialCostCents = 5_000_00L, dateISO = "2026-11-02", isCancelled = true
            )
        )
        val summary = LedgerCalculator.computeWorkshopSummary(orders)
        assertEquals(12_000_00L, summary.activeSalesCents)
        assertEquals(4_000_00L, summary.activeCostsCents)
        assertEquals(8_000_00L, summary.realizedProfitCents)
    }

    @Test
    fun voiceParser_detectsModePartyAndAmount() {
        val debt = VoiceParser.parseArabicVoiceEntry("خبز وحليب 350 دين عند البقال")
        assertEquals(VoiceEntryMode.DEBT_OUT, debt.mode)
        assertEquals("350", debt.amountText)
        assertEquals("البقال", debt.party)

        val rec = VoiceParser.parseArabicVoiceEntry("كراء محل 15000 له من أحمد")
        assertEquals(VoiceEntryMode.RECEIVABLE, rec.mode)
        assertEquals("15000", rec.amountText)
        assertEquals("أحمد", rec.party)
    }
}
