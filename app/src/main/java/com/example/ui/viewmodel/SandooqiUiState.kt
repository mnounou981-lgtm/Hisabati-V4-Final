package com.example.ui.viewmodel

import com.example.data.local.*
import com.example.domain.*

enum class MainNavTab(val route: String, val label: String) {
    HOME("home", "الرئيسية"),
    TREASURY("treasury", "الخزينة"),
    EXPENSES("expenses", "المصاريف"),
    DEBTS("debts", "الديون"),
    BUDGET("budget", "الميزانية"),
    SEWING("sewing", "الخياطة"),
    REPORTS("reports", "التقارير"),
    TOOLS("tools", "الأدوات")
}

data class BillSpikeAlert(
    val name: String,
    val latestCents: Long,
    val averageCents: Long,
    val increasePct: Int
)

data class SearchHit(
    val kind: String,
    val title: String,
    val subtitle: String,
    val targetTab: MainNavTab
)

data class SandooqiUiState(
    val selectedTab: MainNavTab = MainNavTab.TREASURY,
    val currentYM: String = DateUtils.currentYM(),
    val reportYM: String = DateUtils.currentYM(),
    val settings: SandooqiSettings = SandooqiSettings(),
    val isAppLocked: Boolean = false,

    // Raw DB lists
    val transactions: List<TransactionEntity> = emptyList(),
    val debts: List<CreditorDebtEntity> = emptyList(),
    val obligations: List<ObligationEntity> = emptyList(),
    val facPlans: List<FacPlanEntity> = emptyList(),
    val goals: List<SavingsGoalEntity> = emptyList(),
    val seasonalBudgets: List<SeasonalBudgetEntity> = emptyList(),
    val orders: List<TailoringOrderEntity> = emptyList(),
    val sewServices: List<SewServiceEntity> = emptyList(),
    val sewProjects: List<SewProjectEntity> = emptyList(),
    val sewDebts: List<CreditorDebtEntity> = emptyList(),
    val sewMaterials: List<SewMaterialEntity> = emptyList(),
    val measurements: List<CustomerMeasurementEntity> = emptyList(),
    val marketItems: List<MarketItemEntity> = emptyList(),
    val priceChecks: List<PriceCheckEntity> = emptyList(),
    val warranties: List<WarrantyEntity> = emptyList(),
    val monthRecords: Map<String, MonthRecordEntity> = emptyMap(),

    // Computed summaries
    val currentReport: MonthlyCashReport = LedgerCalculator.computeMonthlyReport(
        DateUtils.currentYM(), emptyList(), emptyMap(), SandooqiSettings()
    ),
    val selectedReport: MonthlyCashReport = LedgerCalculator.computeMonthlyReport(
        DateUtils.currentYM(), emptyList(), emptyMap(), SandooqiSettings()
    ),
    val creditorGroups: List<CreditorGroupSummary> = emptyList(),
    val receivableGroups: List<CreditorGroupSummary> = emptyList(),
    val openHomeAndBillDebtsCents: Long = 0L,
    val unpaidObligationsCents: Long = 0L,
    val openRentReceivablesCents: Long = 0L,
    val expectedFamilyNetCents: Long = 0L,
    val totalSavingsCents: Long = 0L,
    val workshopSummary: WorkshopSummary = LedgerCalculator.computeWorkshopSummary(emptyList()),
    val billSpikes: List<BillSpikeAlert> = emptyList(),
    val lowStockMaterialsCount: Int = 0,
    val dueTodayOrdersCount: Int = 0,

    // UI transient dialogs & snackbars
    val toastMessage: String? = null,
    val showVoiceDialog: Boolean = false,
    val showSearchDialog: Boolean = false,
    val showMonthCloseDialog: Boolean = false,
    val showQuickAddSheet: Boolean = false
)
