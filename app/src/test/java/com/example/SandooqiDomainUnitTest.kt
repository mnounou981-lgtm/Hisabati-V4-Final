package com.example

import com.example.data.local.MonthRecordEntity
import com.example.data.local.SandooqiSettings
import com.example.data.local.TailoringOrderEntity
import com.example.data.local.TransactionEntity
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
        assertEquals("150 000 دج", MoneyFormat.formatDzd(150_000_00L))
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
