package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.backup.SandooqiBackupBundle
import com.example.data.local.*
import com.example.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class SandooqiViewModel(application: Application) : AndroidViewModel(application) {
    private val db = SandooqiDatabase.getInstance(application)
    private val prefs = SandooqiPreferences(application)

    private val txDao = db.transactionDao()
    private val debtDao = db.creditorDebtDao()
    private val oblDao = db.obligationDao()
    private val facDao = db.facPlanDao()
    private val goalDao = db.savingsGoalDao()
    private val seasonDao = db.seasonalBudgetDao()
    private val orderDao = db.tailoringOrderDao()
    private val sewServiceDao = db.sewServiceDao()
    private val sewProjectDao = db.sewProjectDao()
    private val materialDao = db.sewMaterialDao()
    private val measDao = db.customerMeasurementDao()
    private val marketDao = db.marketItemDao()
    private val priceDao = db.priceCheckDao()
    private val warrantyDao = db.warrantyDao()
    private val monthDao = db.monthRecordDao()

    private val _selectedTab = MutableStateFlow(MainNavTab.TREASURY)
    private val _reportYM = MutableStateFlow(DateUtils.currentYM())
    private val _isUnlocked = MutableStateFlow(false)
    private val _toast = MutableStateFlow<String?>(null)
    private val _showVoice = MutableStateFlow(false)
    private val _showSearch = MutableStateFlow(false)
    private val _showMonthClose = MutableStateFlow(false)

    private val coreBundleFlow = combine(
        prefs.settingsFlow,
        txDao.observeAll(),
        debtDao.observeAll(),
        oblDao.observeAll(),
        facDao.observeAll()
    ) { settings, txs, debts, obls, facs ->
        CorePart(settings, txs, debts, obls, facs)
    }

    private val savingsAndSewFlow = combine(
        goalDao.observeAll(),
        seasonDao.observeAll(),
        orderDao.observeAll(),
        materialDao.observeAll(),
        measDao.observeAll()
    ) { goals, seasons, orders, mats, meas ->
        SavingsSewPart(goals, seasons, orders, mats, meas)
    }

    private val sewWorkFlow = combine(
        sewServiceDao.observeAll(),
        sewProjectDao.observeAll()
    ) { services, projects ->
        services to projects
    }

    private val extrasFlow = combine(
        marketDao.observeAll(),
        priceDao.observeAll(),
        warrantyDao.observeAll(),
        monthDao.observeAll(),
        sewWorkFlow
    ) { market, prices, warranties, months, sewWork ->
        ExtrasPart(market, prices, warranties, months.associateBy { it.ym }, sewWork.first, sewWork.second)
    }

    private val uiControlsFlow = combine(
        _selectedTab,
        _reportYM,
        _isUnlocked,
        _toast,
        combine(_showVoice, _showSearch, _showMonthClose) { v, s, m -> Triple(v, s, m) }
    ) { tab, repYm, unlocked, toast, dialogs ->
        UiControlsPart(tab, repYm, unlocked, toast, dialogs.first, dialogs.second, dialogs.third)
    }

    val uiState: StateFlow<SandooqiUiState> = combine(
        coreBundleFlow,
        savingsAndSewFlow,
        extrasFlow,
        uiControlsFlow
    ) { core, sew, extras, ui ->
        val curYm = DateUtils.currentYM()
        val today = DateUtils.todayISO()
        val curRep = LedgerCalculator.computeMonthlyReport(curYm, core.txs, extras.months, core.settings)
        val selRep = LedgerCalculator.computeMonthlyReport(ui.reportYm, core.txs, extras.months, core.settings)
        val groups = LedgerCalculator.groupCreditorDebts(core.debts, today)
        val openDebtsCents = groups.sumOf { it.remainingCents }
        val unpaidOblCents = core.obls.sumOf {
            LedgerCalculator.obligationUnpaidMonths(it, curYm).size * max(0L, it.amountCents)
        }
        val rentReceivables = core.debts
            .filter { it.kind == "rent_in" && !it.settled }
            .sumOf { max(0L, it.amountCents - it.paidCents) }
        val expectedNet = curRep.family.balanceCents - openDebtsCents - unpaidOblCents + rentReceivables
        val totalSav = sew.goals.sumOf { max(0L, it.savedCents) }
        val workshop = LedgerCalculator.computeWorkshopSummary(sew.orders)
        val spikes = computeBillSpikes(core.txs)
        val lowMats = sew.mats.count { it.qty <= it.minQty }
        val dueToday = sew.orders.count { !it.isCancelled && !it.isDone && it.dueDateISO == today }
        val locked = core.settings.appLockEnabled && !ui.unlocked

        SandooqiUiState(
            selectedTab = ui.tab,
            currentYM = curYm,
            reportYM = ui.reportYm,
            settings = core.settings,
            isAppLocked = locked,
            transactions = core.txs,
            debts = core.debts,
            obligations = core.obls,
            facPlans = core.facs,
            goals = sew.goals,
            seasonalBudgets = sew.seasons,
            orders = sew.orders,
            sewServices = extras.services,
            sewProjects = extras.projects,
            sewDebts = core.debts.filter { it.kind == "sew_debt_in" || it.kind == "sew_debt_out" },
            sewMaterials = sew.mats,
            measurements = sew.meas,
            marketItems = extras.market,
            priceChecks = extras.prices,
            warranties = extras.warranties,
            monthRecords = extras.months,
            currentReport = curRep,
            selectedReport = selRep,
            creditorGroups = groups,
            openHomeAndBillDebtsCents = openDebtsCents,
            unpaidObligationsCents = unpaidOblCents,
            openRentReceivablesCents = rentReceivables,
            expectedFamilyNetCents = expectedNet,
            totalSavingsCents = totalSav,
            workshopSummary = workshop,
            billSpikes = spikes,
            lowStockMaterialsCount = lowMats,
            dueTodayOrdersCount = dueToday,
            toastMessage = ui.toast,
            showVoiceDialog = ui.showVoice,
            showSearchDialog = ui.showSearch,
            showMonthCloseDialog = ui.showMonthClose
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SandooqiUiState())

    private fun computeBillSpikes(txs: List<TransactionEntity>): List<BillSpikeAlert> {
        val bills = txs.filter { it.category == "bill" && it.type == "out" && it.amountCents > 0 }
        return bills.groupBy { it.name.substringBefore(" (").trim() }
            .mapNotNull { (billName, list) ->
                if (list.size < 2) return@mapNotNull null
                val latest = list.first().amountCents
                val history = list.drop(1).map { it.amountCents }
                val avg = history.sum() / history.size
                if (avg > 0L && latest >= (avg * 120L) / 100L) {
                    val pct = (((latest - avg) * 100.0) / avg).roundToInt()
                    BillSpikeAlert(billName, latest, avg, pct)
                } else null
            }
    }

    fun selectTab(tab: MainNavTab) {
        if (uiState.value.settings.familyEntryOnlyMode && tab != MainNavTab.EXPENSES) {
            showToast("وضع إدخال العائلة مفعّل — أدخل الرمز السري من صفحة المصاريف للخروج")
            return
        }
        _selectedTab.value = tab
    }

    fun selectReportMonth(ym: String) { _reportYM.value = ym }
    fun clearToast() { _toast.value = null }
    fun showToast(msg: String) { _toast.value = msg }
    fun setVoiceDialog(show: Boolean) { _showVoice.value = show }
    fun setSearchDialog(show: Boolean) { _showSearch.value = show }
    fun setMonthCloseDialog(show: Boolean) { _showMonthClose.value = show }

    fun toggleDarkMode() = viewModelScope.launch { prefs.toggleDarkMode() }
    fun togglePrivacyMode() = viewModelScope.launch { prefs.togglePrivacyMode() }
    fun toggleSound() = viewModelScope.launch { prefs.toggleSound() }

    fun unlockApp(pin: String): Boolean {
        return if (pin == uiState.value.settings.appPin) {
            _isUnlocked.value = true
            true
        } else {
            showToast("الرمز السري غير صحيح")
            false
        }
    }

    fun configureAppLock(pin: String, enabled: Boolean) = viewModelScope.launch {
        if (enabled && pin.length < 4) {
            showToast("الرمز يجب أن يكون 4 أرقام على الأقل")
            return@launch
        }
        val finalPin = if (pin.isNotBlank()) pin else uiState.value.settings.appPin
        prefs.setPin(finalPin, enabled)
        if (!enabled) _isUnlocked.value = true
        showToast(if (enabled) "تم تفعيل قفل التطبيق" else "تم إيقاف قفل التطبيق")
    }

    fun enableFamilyEntryMode(member: String) = viewModelScope.launch {
        prefs.setFamilyEntryOnlyMode(true, member)
        _selectedTab.value = MainNavTab.EXPENSES
        showToast("تم تفعيل وضع إدخال العائلة لـ $member")
    }

    fun exitFamilyEntryMode(pin: String): Boolean {
        return if (pin == uiState.value.settings.appPin) {
            viewModelScope.launch { prefs.setFamilyEntryOnlyMode(false) }
            showToast("تم الخروج من وضع العائلة")
            true
        } else {
            showToast("الرمز السري غير صحيح")
            false
        }
    }

    // --- Treasury & Salaries ---
    fun saveSalaries(ym: String, s1Cents: Long, s2Cents: Long) = viewModelScope.launch {
        val existing = monthDao.getByYm(ym)
        monthDao.upsert(
            (existing ?: MonthRecordEntity(ym = ym)).copy(
                salary1Cents = max(0L, s1Cents),
                salary2Cents = max(0L, s2Cents)
            )
        )
        showToast("تم حفظ رواتب ${DateUtils.formatYM(ym)}")
    }

    fun closeMonth(targetGoalId: Long?, savePercent: Int) = viewModelScope.launch {
        val ym = DateUtils.currentYM()
        val existing = monthDao.getByYm(ym) ?: MonthRecordEntity(ym = ym)
        if (existing.isClosed) {
            _showMonthClose.value = false
            showToast("تم إغلاق ${DateUtils.formatYM(ym)} مسبقاً")
            return@launch
        }
        val report = uiState.value.currentReport
        val surplus = max(0L, report.family.balanceCents)
        val pct = savePercent.coerceIn(0, 100)
        val saveCents = (surplus * pct) / 100L
        val carryForward = max(0L, surplus - saveCents)

        if (saveCents > 0L && targetGoalId != null) {
            val goal = uiState.value.goals.find { it.id == targetGoalId }
            if (goal != null) {
                goalDao.update(
                    goal.copy(
                        savedCents = goal.savedCents + saveCents,
                        fromHomeCents = goal.fromHomeCents + saveCents
                    )
                )
                txDao.insert(
                    TransactionEntity(
                        ym = ym,
                        dateISO = DateUtils.todayISO(),
                        accountId = "family",
                        type = "out",
                        category = "sav",
                        name = "ادخار من فائض إغلاق الشهر: ${goal.name}",
                        amountCents = saveCents
                    )
                )
            }
        }
        monthDao.upsert(
            existing.copy(
                isClosed = true,
                closedAt = DateUtils.todayISO(),
                savedToGoalCents = saveCents,
                carriedForwardCents = carryForward
            )
        )
        val nextYm = DateUtils.shiftYM(ym, 1)
        val nextRec = monthDao.getByYm(nextYm) ?: MonthRecordEntity(ym = nextYm)
        monthDao.upsert(nextRec.copy(carryoverInCents = carryForward))
        _showMonthClose.value = false
        showToast("تم إغلاق ${DateUtils.formatYM(ym)} وترحيل ${MoneyFormat.formatDzd(carryForward)}")
    }

    fun transferBetweenAccounts(fromAccount: String, toAccount: String, amountCents: Long, note: String) = viewModelScope.launch {
        if (fromAccount == toAccount || amountCents <= 0L) {
            showToast("اختر حسابين مختلفين ومبلغاً صحيحاً")
            return@launch
        }
        val tid = System.currentTimeMillis()
        val label = note.ifBlank { "تحويل من ${accountTitle(fromAccount)} إلى ${accountTitle(toAccount)}" }
        val ym = DateUtils.currentYM()
        val date = DateUtils.todayISO()
        txDao.insert(
            TransactionEntity(
                ym = ym, dateISO = date, accountId = fromAccount, type = "transfer",
                category = "account_transfer", name = label, amountCents = amountCents,
                transferId = tid, transferFromAccountId = fromAccount, transferToAccountId = toAccount
            )
        )
        txDao.insert(
            TransactionEntity(
                ym = ym, dateISO = date, accountId = toAccount, type = "transfer",
                category = "account_transfer", name = label, amountCents = amountCents,
                transferId = tid, transferFromAccountId = fromAccount, transferToAccountId = toAccount
            )
        )
        showToast("تم التحويل (${MoneyFormat.formatDzd(amountCents)}) بنجاح")
    }

    fun addManualTransaction(accountId: String, type: String, name: String, amountCents: Long, category: String = "extra") = viewModelScope.launch {
        if (name.isBlank() || amountCents <= 0L) {
            showToast("أدخل البيان والمبلغ الصحيح")
            return@launch
        }
        txDao.insert(
            TransactionEntity(
                ym = DateUtils.currentYM(),
                dateISO = DateUtils.todayISO(),
                accountId = accountId,
                type = type,
                category = category,
                name = name.trim(),
                amountCents = amountCents
            )
        )
        showToast("تم تسجيل الحركة")
    }

    fun deleteTransaction(tx: TransactionEntity) = viewModelScope.launch {
        if (tx.transferId != null) {
            txDao.deleteByTransferId(tx.transferId)
        } else {
            txDao.deleteById(tx.id)
        }
        showToast("تم حذف العملية")
    }

    // --- Expenses & Market ---
    fun addGroceryExpense(
        name: String,
        qty: Double,
        unit: String,
        unitPriceCents: Long,
        category: String,
        dateISO: String,
        isCredit: Boolean,
        shopOrCreditor: String,
        photoUri: String = ""
    ) = viewModelScope.launch {
        val totalCents = max(1L, Math.round(qty * unitPriceCents))
        val ym = if (dateISO.length >= 7) dateISO.substring(0, 7) else DateUtils.currentYM()
        val desc = if (qty == 1.0 && unit == "حبة") name else "$name ($qty $unit)"
        var debtId: Long? = null
        if (isCredit) {
            val cred = shopOrCreditor.trim().ifBlank { "محل مواد غذائية" }
            debtId = debtDao.insert(
                CreditorDebtEntity(
                    creditor = cred,
                    item = name,
                    activity = "مشتريات",
                    amountCents = totalCents,
                    paidCents = 0L,
                    dateISO = dateISO,
                    ym = ym,
                    expenseCategory = category,
                    photoUri = photoUri,
                    kind = "home_debt"
                )
            )
        }
        if (shopOrCreditor.isNotBlank() && unitPriceCents > 0L) {
            priceDao.insert(
                PriceCheckEntity(
                    product = name,
                    store = shopOrCreditor.trim(),
                    unitPriceCents = unitPriceCents,
                    dateISO = dateISO
                )
            )
        }
        txDao.insert(
            TransactionEntity(
                ym = ym,
                dateISO = dateISO,
                accountId = "family",
                type = "out",
                category = "shop",
                name = if (isCredit) "شراء بالدين: $desc (${shopOrCreditor.trim()})" else "مصروف: $desc",
                amountCents = totalCents,
                expenseCategory = category,
                creditor = shopOrCreditor.trim(),
                homeDebtId = debtId,
                isCreditPurchase = isCredit
            )
        )
        showToast(if (isCredit) "سُجّل ${MoneyFormat.formatDzd(totalCents)} دين عند ${shopOrCreditor.trim()}" else "تم تسجيل مصروف ${MoneyFormat.formatDzd(totalCents)}")
    }

    fun runQuickExpense(preset: QuickExpensePreset) = viewModelScope.launch {
        txDao.insert(
            TransactionEntity(
                ym = DateUtils.currentYM(),
                dateISO = DateUtils.todayISO(),
                accountId = "family",
                type = "out",
                category = "shop",
                name = "مصروف سريع: ${preset.name}",
                amountCents = preset.amountCents,
                expenseCategory = preset.category
            )
        )
        showToast("تم خصم ${MoneyFormat.formatDzd(preset.amountCents)} (${preset.name})")
    }

    fun addMarketItem(name: String, qty: Double, unit: String, unitPriceCents: Long) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        marketDao.insert(
            MarketItemEntity(
                name = name.trim(),
                quantity = max(0.1, qty),
                unit = unit,
                unitPriceCents = max(0L, unitPriceCents)
            )
        )
        showToast("أُضيف $name لقائمة السوق")
    }

    fun toggleMarketItem(item: MarketItemEntity) = viewModelScope.launch {
        marketDao.update(item.copy(isDone = !item.isDone))
    }

    fun deleteMarketItem(id: Long) = viewModelScope.launch { marketDao.deleteById(id) }
    fun clearDoneMarket() = viewModelScope.launch { marketDao.clearDone() }
    fun setMarketBudget(cents: Long) = viewModelScope.launch { prefs.setMarketBudget(cents) }

    // --- Debts, Bills, Obligations, Rent & Family ---
    fun addManualHomeDebt(creditor: String, item: String, amountCents: Long, dueISO: String, activity: String, photoUri: String = "") = viewModelScope.launch {
        if (creditor.isBlank() || amountCents <= 0L) return@launch
        debtDao.insert(
            CreditorDebtEntity(
                creditor = creditor.trim(),
                item = item.ifBlank { "دين $activity" },
                activity = activity,
                amountCents = amountCents,
                dueDateISO = dueISO,
                dateISO = DateUtils.todayISO(),
                ym = DateUtils.currentYM(),
                photoUri = photoUri,
                kind = "home_debt"
            )
        )
        showToast("تم تسجيل دين عند ${creditor.trim()}")
    }

    fun payCreditorAmount(creditor: String, paymentCents: Long) = viewModelScope.launch {
        if (paymentCents <= 0L) return@launch
        val open = uiState.value.debts
            .filter { it.creditor.trim() == creditor.trim() && it.kind in listOf("home_debt", "bill_debt", "rent_out") && it.amountCents > it.paidCents }
            .sortedWith(compareBy<CreditorDebtEntity> { it.ym }.thenBy { it.id })
        var left = paymentCents
        for (debt in open) {
            if (left <= 0L) break
            val rem = debt.amountCents - debt.paidCents
            val part = min(rem, left)
            val newPaid = debt.paidCents + part
            debtDao.update(debt.copy(paidCents = newPaid, settled = newPaid >= debt.amountCents))
            left -= part
        }
        val applied = paymentCents - left
        if (applied > 0L) {
            txDao.insert(
                TransactionEntity(
                    ym = DateUtils.currentYM(),
                    dateISO = DateUtils.todayISO(),
                    accountId = "family",
                    type = "out",
                    category = "debt_payment",
                    name = "تسديد دين للمحل: $creditor",
                    amountCents = applied,
                    creditor = creditor
                )
            )
            showToast("تم تسديد ${MoneyFormat.formatDzd(applied)} لـ $creditor")
        }
    }

    fun deleteDebt(id: Long) = viewModelScope.launch { debtDao.deleteById(id) }

    fun addRentRecord(unit: String, party: String, amountCents: Long, isReceivable: Boolean, immediateTreasury: Boolean, dueISO: String) = viewModelScope.launch {
        if (party.isBlank() || amountCents <= 0L) return@launch
        val ym = DateUtils.currentYM()
        val today = DateUtils.todayISO()
        if (isReceivable) {
            if (immediateTreasury) {
                txDao.insert(
                    TransactionEntity(
                        ym = ym, dateISO = today, accountId = "family", type = "in",
                        category = "extra", name = "كراء له ($unit) من: $party",
                        amountCents = amountCents, creditor = party
                    )
                )
            }
            debtDao.insert(
                CreditorDebtEntity(
                    creditor = party.trim(), item = unit, activity = "كراء له",
                    amountCents = amountCents, paidCents = if (immediateTreasury) amountCents else 0L,
                    dueDateISO = dueISO, dateISO = today, ym = ym,
                    kind = "rent_in", rentUnit = unit, settled = immediateTreasury
                )
            )
        } else {
            if (immediateTreasury) {
                txDao.insert(
                    TransactionEntity(
                        ym = ym, dateISO = today, accountId = "family", type = "out",
                        category = "debt_payment", name = "تسديد كراء ($unit) إلى: $party",
                        amountCents = amountCents, creditor = party
                    )
                )
            }
            debtDao.insert(
                CreditorDebtEntity(
                    creditor = party.trim(), item = unit, activity = "التزامات وكراء",
                    amountCents = amountCents, paidCents = if (immediateTreasury) amountCents else 0L,
                    dueDateISO = dueISO, dateISO = today, ym = ym,
                    kind = "rent_out", rentUnit = unit, settled = immediateTreasury
                )
            )
        }
        showToast("تم تسجيل حركة الكراء ($unit)")
    }

    fun collectRentReceivable(debt: CreditorDebtEntity) = viewModelScope.launch {
        val rem = max(0L, debt.amountCents - debt.paidCents)
        if (rem <= 0L) return@launch
        txDao.insert(
            TransactionEntity(
                ym = DateUtils.currentYM(), dateISO = DateUtils.todayISO(), accountId = "family",
                type = "in", category = "extra",
                name = "تحصيل كراء له (${debt.rentUnit.ifBlank { debt.item }}) من: ${debt.creditor}",
                amountCents = rem, creditor = debt.creditor
            )
        )
        debtDao.update(debt.copy(paidCents = debt.amountCents, settled = true))
        showToast("تم تحصيل ${MoneyFormat.formatDzd(rem)} وإضافتها للخزينة")
    }

    fun addBillOrObligation(name: String, amountCents: Long, creditor: String, dateISO: String, isPaid: Boolean, isRecurring: Boolean, unpaidMonths: Int, photoUri: String = "") = viewModelScope.launch {
        if (name.isBlank() || amountCents <= 0L) return@launch
        val baseYm = if (dateISO.length >= 7) dateISO.substring(0, 7) else DateUtils.currentYM()
        if (isRecurring) {
            val startYm = DateUtils.shiftYM(baseYm, -(max(1, unpaidMonths) - 1))
            oblDao.insert(
                ObligationEntity(
                    name = name.trim(),
                    amountCents = amountCents,
                    creditor = creditor.trim(),
                    startYM = startYm,
                    paidMonthsCsv = if (isPaid) baseYm else "",
                    isRecurringBill = true,
                    photoUri = photoUri
                )
            )
            if (isPaid) {
                txDao.insert(
                    TransactionEntity(
                        ym = baseYm, dateISO = dateISO, accountId = "family", type = "out",
                        category = "bill", name = "التزام شهري: ${name.trim()} (${DateUtils.formatYM(baseYm)})",
                        amountCents = amountCents, creditor = creditor.trim()
                    )
                )
            }
        } else if (isPaid) {
            txDao.insert(
                TransactionEntity(
                    ym = baseYm, dateISO = dateISO, accountId = "family", type = "out",
                    category = "bill", name = "فاتورة: ${name.trim()}",
                    amountCents = amountCents, creditor = creditor.trim()
                )
            )
        } else {
            val cred = creditor.trim().ifBlank { name.trim() }
            for (i in (max(1, unpaidMonths) - 1) downTo 0) {
                val ym = DateUtils.shiftYM(baseYm, -i)
                val label = if (unpaidMonths > 1) "${name.trim()} (${DateUtils.formatYM(ym)})" else name.trim()
                debtDao.insert(
                    CreditorDebtEntity(
                        creditor = cred, item = label, activity = "فواتير",
                        amountCents = amountCents, dueDateISO = dateISO, dateISO = dateISO,
                        ym = ym, photoUri = photoUri, kind = "bill_debt"
                    )
                )
            }
        }
        showToast(if (isRecurring) "تم تسجيل الالتزام الشهري" else if (isPaid) "سُجّلت الفاتورة كمدفوعة" else "سُجّلت الفاتورة ضمن الديون")
    }

    fun payObligationCurrentMonth(obligation: ObligationEntity) = viewModelScope.launch {
        val unpaid = LedgerCalculator.obligationUnpaidMonths(obligation, DateUtils.currentYM())
        val targetYm = unpaid.firstOrNull() ?: return@launch
        val paidSet = obligation.paidMonthsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        paidSet.add(targetYm)
        oblDao.update(obligation.copy(paidMonthsCsv = paidSet.joinToString(",")))
        txDao.insert(
            TransactionEntity(
                ym = DateUtils.currentYM(), dateISO = DateUtils.todayISO(), accountId = "family",
                type = "out", category = "bill",
                name = "التزام شهري: ${obligation.name} (${DateUtils.formatYM(targetYm)})",
                amountCents = obligation.amountCents, creditor = obligation.creditor
            )
        )
        showToast("تم دفع ${obligation.name} لشهر ${DateUtils.formatYM(targetYm)}")
    }

    fun deleteObligation(id: Long) = viewModelScope.launch { oblDao.deleteById(id) }

    fun addFacPlan(
        name: String,
        installmentCents: Long,
        totalMonths: Int,
        planType: String = "fac"
    ) = viewModelScope.launch {
        if (name.isBlank() || installmentCents <= 0L || totalMonths <= 0) return@launch
        val normalizedType = if (planType == "srv") "srv" else "fac"
        facDao.insert(
            FacPlanEntity(
                name = name.trim(),
                installmentCents = installmentCents,
                totalMonths = totalMonths,
                planType = normalizedType
            )
        )
        showToast(if (normalizedType == "srv") "تمت إضافة خطة الخدمات الشهرية" else "تمت إضافة خطة الفاسيليتي")
    }

    fun payFacInstallment(plan: FacPlanEntity) = viewModelScope.launch {
        val ym = DateUtils.currentYM()
        if (plan.paidMonths >= plan.totalMonths || plan.lastPaidYM == ym) {
            showToast("قسط هذا الشهر مسجل بالفعل")
            return@launch
        }
        val nextPaid = plan.paidMonths + 1
        val isService = plan.planType == "srv"
        val category = if (isService) "srv" else "fac"
        val labelPrefix = if (isService) "قسط خدمات" else "قسط فاسيليتي"
        facDao.update(plan.copy(paidMonths = nextPaid, lastPaidYM = ym))
        txDao.insert(
            TransactionEntity(
                ym = ym, dateISO = DateUtils.todayISO(), accountId = "family",
                type = "out", category = category,
                name = "$labelPrefix: ${plan.name} ($nextPaid/${plan.totalMonths})",
                amountCents = plan.installmentCents
            )
        )
        showToast("تم خصم ${if (isService) "قسط الخدمات" else "قسط الفاسيليتي"} $nextPaid/${plan.totalMonths}")
    }

    fun deleteFacPlan(id: Long) = viewModelScope.launch { facDao.deleteById(id) }

    fun addDeduction(isService: Boolean, name: String, amountCents: Long, targetYM: String) = viewModelScope.launch {
        if (amountCents <= 0L) return@launch
        val cat = if (isService) "srv" else "fac"
        val label = if (isService) "اقتطاع خدمات: ${name.ifBlank { "خدمات" }} (${DateUtils.formatYM(targetYM)})"
        else "اقتطاع فاسيليتي: ${name.ifBlank { "فاسيليتي" }}"
        txDao.insert(
            TransactionEntity(
                ym = targetYM.ifBlank { DateUtils.currentYM() },
                dateISO = DateUtils.todayISO(),
                accountId = "family",
                type = "out",
                category = cat,
                name = label,
                amountCents = amountCents
            )
        )
        showToast("تم تسجيل الاقتطاع")
    }

    fun addFamilyExpense(person: String, item: String, amountCents: Long) = viewModelScope.launch {
        if (person.isBlank() || item.isBlank() || amountCents <= 0L) return@launch
        txDao.insert(
            TransactionEntity(
                ym = DateUtils.currentYM(),
                dateISO = DateUtils.todayISO(),
                accountId = "family",
                type = "out",
                category = "fam",
                name = "مصروف $person: $item",
                amountCents = amountCents,
                expenseCategory = item,
                personName = person
            )
        )
        showToast("سُجّل مصروف $person")
    }

    // --- Budget & Savings ---
    fun saveBudgets(monthlyCents: Long, weeklyCents: Long) = viewModelScope.launch {
        prefs.setMonthlyFamilyBudget(monthlyCents)
        prefs.setWeeklyFamilyBudget(weeklyCents)
        showToast("تم حفظ سقف الميزانية الموحد")
    }

    fun addSavingsGoal(name: String, targetCents: Long) = viewModelScope.launch {
        if (name.isBlank() || targetCents <= 0L) return@launch
        goalDao.insert(SavingsGoalEntity(name = name.trim(), targetCents = targetCents))
        showToast("تم إنشاء هدف الادخار")
    }

    fun depositToGoal(goal: SavingsGoalEntity, amountCents: Long, source: String) = viewModelScope.launch {
        if (amountCents <= 0L) return@launch
        val report = uiState.value.currentReport
        if (source == "family" && amountCents > report.family.balanceCents) {
            showToast("الرصيد المتاح في الحساب العائلي غير كافٍ")
            return@launch
        }
        if (source == "sewing" && amountCents > report.sewing.balanceCents) {
            showToast("الرصيد المتاح في حساب الخياطة غير كافٍ")
            return@launch
        }
        val updated = when (source) {
            "family" -> goal.copy(savedCents = goal.savedCents + amountCents, fromHomeCents = goal.fromHomeCents + amountCents)
            "sewing" -> goal.copy(savedCents = goal.savedCents + amountCents, fromSewingCents = goal.fromSewingCents + amountCents)
            else -> goal.copy(savedCents = goal.savedCents + amountCents, fromExternalCents = goal.fromExternalCents + amountCents)
        }
        goalDao.update(updated)
        if (source == "family" || source == "sewing") {
            txDao.insert(
                TransactionEntity(
                    ym = DateUtils.currentYM(), dateISO = DateUtils.todayISO(),
                    accountId = source, type = "out",
                    category = if (source == "family") "sav" else "sav_sew",
                    name = "إيداع في هدف الادخار: ${goal.name}", amountCents = amountCents
                )
            )
        }
        showToast("تم إيداع ${MoneyFormat.formatDzd(amountCents)} في ${goal.name}")
    }

    fun withdrawFromGoal(goal: SavingsGoalEntity, amountCents: Long, dest: String) = viewModelScope.launch {
        if (amountCents <= 0L || amountCents > goal.savedCents) {
            showToast("المبلغ أكبر من الرصيد المدخر")
            return@launch
        }
        val updated = goal.copy(savedCents = max(0L, goal.savedCents - amountCents))
        goalDao.update(updated)
        if (dest == "family" || dest == "sewing") {
            txDao.insert(
                TransactionEntity(
                    ym = DateUtils.currentYM(), dateISO = DateUtils.todayISO(),
                    accountId = dest, type = "in",
                    category = if (dest == "family") "extra" else "sew_in",
                    name = "سحب من هدف الادخار: ${goal.name}", amountCents = amountCents
                )
            )
        }
        showToast("تم سحب ${MoneyFormat.formatDzd(amountCents)} من ${goal.name}")
    }

    fun deleteGoal(goal: SavingsGoalEntity) = viewModelScope.launch {
        if (uiState.value.goals.size <= 1 || goal.savedCents > 0L) {
            showToast("اسحب الرصيد أولاً وتأكد من بقاء هدف واحد على الأقل")
            return@launch
        }
        goalDao.deleteById(goal.id)
    }

    fun addSeasonalBudget(name: String, emoji: String, targetCents: Long, targetYM: String) = viewModelScope.launch {
        if (name.isBlank() || targetCents <= 0L) return@launch
        seasonDao.insert(SeasonalBudgetEntity(name = name.trim(), emoji = emoji, targetCents = targetCents, targetYM = targetYM))
        showToast("تمت إضافة ميزانية $name")
    }

    fun fundSeasonalBudget(season: SeasonalBudgetEntity, addCents: Long) = viewModelScope.launch {
        if (addCents <= 0L) return@launch
        seasonDao.update(season.copy(savedCents = season.savedCents + addCents))
        showToast("تم تخصيص ${MoneyFormat.formatDzd(addCents)} لـ ${season.name}")
    }

    fun deleteSeasonalBudget(id: Long) = viewModelScope.launch { seasonDao.deleteById(id) }

    // --- Tailoring Workshop ---
    fun addTailoringOrder(
        client: String,
        phone: String,
        item: String,
        totalCents: Long,
        depositCents: Long,
        materialCostCents: Long,
        laborCents: Long,
        dueISO: String,
        fittingISO: String,
        fabricsSummary: String,
        measurementsSummary: String,
        photoUri: String = "",
        adjustmentCents: Long = 0L,
        suppliesSummary: String = "",
        workshopFabricBuyCents: Long = 0L,
        stockDeductions: List<Pair<Long, Double>> = emptyList()
    ) = viewModelScope.launch {
        if (client.isBlank() || item.isBlank() || totalCents <= 0L) {
            showToast("أدخل اسم الزبونة والموديل والسعر الإجمالي")
            return@launch
        }
        val safeDeposit = min(totalCents, max(0L, depositCents))
        val today = DateUtils.todayISO()
        val ym = DateUtils.currentYM()
        val orderId = orderDao.insert(
            TailoringOrderEntity(
                client = client.trim(),
                phone = phone.trim(),
                item = item.trim(),
                totalCents = totalCents,
                paidCents = safeDeposit,
                materialCostCents = max(0L, materialCostCents),
                laborCents = max(0L, laborCents),
                adjustmentCents = adjustmentCents,
                dueDateISO = dueISO,
                fittingDateISO = fittingISO,
                dateISO = today,
                photoUri = photoUri,
                fabricsSummary = fabricsSummary,
                suppliesSummary = suppliesSummary,
                measurementsSummary = measurementsSummary
            )
        )
        if (safeDeposit > 0L) {
            txDao.insert(
                TransactionEntity(
                    ym = ym, dateISO = today, accountId = "sewing", type = "in",
                    category = "sew_in", name = "عربون طلبية (${client.trim()}): ${item.trim()}",
                    amountCents = safeDeposit, orderId = orderId
                )
            )
        }
        if (workshopFabricBuyCents > 0L) {
            txDao.insert(
                TransactionEntity(
                    ym = ym, dateISO = today, accountId = "sewing", type = "out",
                    category = "order_fabric_purchase", name = "شراء قماش للطلبية (${client.trim()}): ${item.trim()}",
                    amountCents = workshopFabricBuyCents, orderId = orderId
                )
            )
        }
        for ((matId, usedQty) in stockDeductions) {
            val mat = uiState.value.sewMaterials.find { it.id == matId } ?: continue
            materialDao.update(mat.copy(qty = max(0.0, mat.qty - usedQty)))
        }
        showToast("تم تسجيل طلبية ${client.trim()}")
    }

    fun updateTailoringOrder(order: TailoringOrderEntity) = viewModelScope.launch {
        orderDao.update(order)
        showToast("تم تحديث بيانات الطلبية")
    }

    fun deleteTailoringOrder(order: TailoringOrderEntity) = viewModelScope.launch {
        orderDao.deleteById(order.id)
        showToast("تم حذف الطلبية من الدفتر")
    }

    fun recordOrderPayment(order: TailoringOrderEntity, amountCents: Long) = viewModelScope.launch {
        val rem = LedgerCalculator.orderRemainingCents(order)
        if (amountCents <= 0L || amountCents > rem) {
            showToast("المبلغ غير صالح أو يتجاوز المتبقي")
            return@launch
        }
        orderDao.update(order.copy(paidCents = order.paidCents + amountCents))
        txDao.insert(
            TransactionEntity(
                ym = DateUtils.currentYM(), dateISO = DateUtils.todayISO(), accountId = "sewing",
                type = "in", category = "sew_in",
                name = "دفعة طلبية (${order.client}): ${order.item}",
                amountCents = amountCents, orderId = order.id
            )
        )
        showToast("تم تسجيل دفعة ${MoneyFormat.formatDzd(amountCents)}")
    }

    fun updateOrderStage(order: TailoringOrderEntity, newStage: String) = viewModelScope.launch {
        val done = newStage == "تم التسليم"
        orderDao.update(order.copy(stage = newStage, isDone = done))
    }

    fun toggleOrderChecklist(order: TailoringOrderEntity, bitIndex: Int) = viewModelScope.launch {
        val nextMask = order.checklistMask xor (1 shl bitIndex)
        orderDao.update(order.copy(checklistMask = nextMask))
    }

    fun cancelOrder(order: TailoringOrderEntity, refundCents: Long) = viewModelScope.launch {
        val netPaid = max(0L, order.paidCents - order.refundedCents)
        val safeRefund = min(netPaid, max(0L, refundCents))
        orderDao.update(
            order.copy(
                isCancelled = true,
                stage = "ملغاة",
                refundedCents = order.refundedCents + safeRefund
            )
        )
        if (safeRefund > 0L) {
            txDao.insert(
                TransactionEntity(
                    ym = DateUtils.currentYM(), dateISO = DateUtils.todayISO(), accountId = "sewing",
                    type = "out", category = "order_refund",
                    name = "ردّ دفعة طلبية (${order.client}): ${order.item}",
                    amountCents = safeRefund, orderId = order.id
                )
            )
        }
        showToast("تم إلغاء الطلبية")
    }

    fun addSewMaterial(name: String, unit: String, qty: Double, minQty: Double, costPerUnitCents: Long, deductFromTreasury: Boolean) = viewModelScope.launch {
        if (name.isBlank() || qty <= 0) return@launch
        materialDao.insert(SewMaterialEntity(name = name.trim(), unit = unit, qty = qty, minQty = minQty, costPerUnitCents = costPerUnitCents))
        if (deductFromTreasury && costPerUnitCents > 0L) {
            val total = Math.round(qty * costPerUnitCents)
            txDao.insert(
                TransactionEntity(
                    ym = DateUtils.currentYM(), dateISO = DateUtils.todayISO(), accountId = "sewing",
                    type = "out", category = "sew_out", name = "شراء مخزون خياطة: ${name.trim()}",
                    amountCents = total
                )
            )
        }
        showToast("تمت إضافة ${name.trim()} للمخزون")
    }

    fun consumeSewMaterial(mat: SewMaterialEntity, usedQty: Double) = viewModelScope.launch {
        if (usedQty <= 0 || usedQty > mat.qty) {
            showToast("الكمية غير متوفرة")
            return@launch
        }
        materialDao.update(mat.copy(qty = mat.qty - usedQty))
    }

    fun restockSewMaterial(mat: SewMaterialEntity, addQty: Double) = viewModelScope.launch {
        if (addQty <= 0) return@launch
        materialDao.update(mat.copy(qty = mat.qty + addQty))
    }

    fun deleteSewMaterial(id: Long) = viewModelScope.launch { materialDao.deleteById(id) }

    fun addMeasurement(m: CustomerMeasurementEntity) = viewModelScope.launch {
        if (m.name.isBlank()) return@launch
        measDao.insert(m)
        showToast("تم حفظ مقاسات ${m.name}")
    }

    fun deleteMeasurement(id: Long) = viewModelScope.launch { measDao.deleteById(id) }

    fun addSewService(
        client: String,
        item: String,
        meters: Double,
        ratePerMeterCents: Long,
        threadCostCents: Long,
        suppliesCostCents: Long,
        paidCents: Long
    ) = viewModelScope.launch {
        if (client.isBlank() || item.isBlank() || meters <= 0 || ratePerMeterCents <= 0L) {
            showToast("أدخل الزبونة والنوع والأمتار وسعر المتر")
            return@launch
        }
        val priceCents = Math.round(meters * ratePerMeterCents)
        val today = DateUtils.todayISO()
        val ym = DateUtils.currentYM()
        val safePaid = min(priceCents, max(0L, paidCents))
        sewServiceDao.insert(
            SewServiceEntity(
                client = client.trim(),
                item = item.trim(),
                meters = meters,
                ratePerMeterCents = ratePerMeterCents,
                priceCents = priceCents,
                threadCostCents = max(0L, threadCostCents),
                suppliesCostCents = max(0L, suppliesCostCents),
                paidCents = safePaid,
                dateISO = today,
                ym = ym
            )
        )
        if (safePaid > 0L) {
            txDao.insert(
                TransactionEntity(
                    ym = ym, dateISO = today, accountId = "sewing", type = "in",
                    category = "sew_in", name = "خدمة خياطة (${client.trim()}): ${item.trim()}",
                    amountCents = safePaid
                )
            )
        }
        val costs = max(0L, threadCostCents) + max(0L, suppliesCostCents)
        if (costs > 0L) {
            txDao.insert(
                TransactionEntity(
                    ym = ym, dateISO = today, accountId = "sewing", type = "out",
                    category = "sew_out", name = "لوازم خدمة خياطة (${client.trim()})",
                    amountCents = costs
                )
            )
        }
        showToast("تم تسجيل خدمة خياطة ${client.trim()}")
    }

    fun deleteSewService(id: Long) = viewModelScope.launch { sewServiceDao.deleteById(id) }

    fun addSewProject(
        name: String,
        meters: Double,
        fabricRateCents: Long,
        suppliesCostCents: Long,
        otherCostCents: Long,
        salePriceCents: Long,
        paidCents: Long,
        status: String
    ) = viewModelScope.launch {
        if (name.isBlank()) {
            showToast("أدخل اسم المشروع")
            return@launch
        }
        val fabricCostCents = Math.round(max(0.0, meters) * max(0L, fabricRateCents))
        val totalCost = fabricCostCents + max(0L, suppliesCostCents) + max(0L, otherCostCents)
        val today = DateUtils.todayISO()
        val ym = DateUtils.currentYM()
        sewProjectDao.insert(
            SewProjectEntity(
                name = name.trim(),
                meters = max(0.0, meters),
                fabricRateCents = max(0L, fabricRateCents),
                fabricCostCents = fabricCostCents,
                suppliesCostCents = max(0L, suppliesCostCents),
                otherCostCents = max(0L, otherCostCents),
                salePriceCents = max(0L, salePriceCents),
                paidCents = max(0L, paidCents),
                status = status,
                dateISO = today,
                ym = ym
            )
        )
        if (totalCost > 0L) {
            txDao.insert(
                TransactionEntity(
                    ym = ym, dateISO = today, accountId = "sewing", type = "out",
                    category = "sew_out", name = "تكلفة مشروع خاص: ${name.trim()}",
                    amountCents = totalCost
                )
            )
        }
        if (paidCents > 0L) {
            txDao.insert(
                TransactionEntity(
                    ym = ym, dateISO = today, accountId = "sewing", type = "in",
                    category = "sew_in", name = "مبيعات مشروع خاص: ${name.trim()}",
                    amountCents = paidCents
                )
            )
        }
        showToast("تم تسجيل المشروع الخاص ${name.trim()}")
    }

    fun updateSewProjectStatus(project: SewProjectEntity, status: String) = viewModelScope.launch {
        sewProjectDao.update(project.copy(status = status))
    }

    fun deleteSewProject(id: Long) = viewModelScope.launch { sewProjectDao.deleteById(id) }

    fun addSewingMeterIncome(meters: Double, rateCents: Long, note: String) = viewModelScope.launch {
        val amt = Math.round(meters * rateCents)
        if (meters <= 0 || amt <= 0L) {
            showToast("أدخل عدد الأمتار والسعر")
            return@launch
        }
        txDao.insert(
            TransactionEntity(
                ym = DateUtils.currentYM(), dateISO = DateUtils.todayISO(),
                accountId = "sewing", type = "in", category = "sew_in",
                name = if (note.isNotBlank()) "خياطة ($meters م): ${note.trim()}" else "مدخول خياطة ($meters متر)",
                amountCents = amt
            )
        )
        showToast("تمت إضافة ${MoneyFormat.formatDzd(amt)} لصندوق الخياطة")
    }

    fun addSewingThreadExpense(qty: Int, priceCents: Long) = viewModelScope.launch {
        val amt = qty * priceCents
        if (qty <= 0 || amt <= 0L) return@launch
        txDao.insert(
            TransactionEntity(
                ym = DateUtils.currentYM(), dateISO = DateUtils.todayISO(),
                accountId = "sewing", type = "out", category = "sew_out",
                name = "استهلاك خيط ($qty بكرة)", amountCents = amt
            )
        )
        showToast("تم خصم ${MoneyFormat.formatDzd(amt)} من صندوق الخياطة")
    }

    fun addSewDebt(isReceivable: Boolean, name: String, amountCents: Long, dueISO: String) = viewModelScope.launch {
        if (name.isBlank() || amountCents <= 0L) {
            showToast("أدخل الاسم والمبلغ")
            return@launch
        }
        debtDao.insert(
            CreditorDebtEntity(
                creditor = name.trim(),
                item = if (isReceivable) "مستحق خياطة" else "دين لوازم خياطة",
                activity = "خياطة",
                amountCents = amountCents,
                dueDateISO = dueISO,
                dateISO = DateUtils.todayISO(),
                ym = DateUtils.currentYM(),
                kind = if (isReceivable) "sew_debt_in" else "sew_debt_out"
            )
        )
        showToast("تم تسجيل دين الخياطة")
    }

    fun paySewDebt(debt: CreditorDebtEntity, amountCents: Long) = viewModelScope.launch {
        val rem = max(0L, debt.amountCents - debt.paidCents)
        if (amountCents <= 0L || amountCents > rem) {
            showToast("المبلغ غير صالح أو يتجاوز المتبقي")
            return@launch
        }
        val newPaid = debt.paidCents + amountCents
        debtDao.update(debt.copy(paidCents = newPaid, settled = newPaid >= debt.amountCents))
        val isIn = debt.kind == "sew_debt_in"
        txDao.insert(
            TransactionEntity(
                ym = DateUtils.currentYM(), dateISO = DateUtils.todayISO(),
                accountId = "sewing",
                type = if (isIn) "in" else "out",
                category = if (isIn) "sew_in" else "sew_out",
                name = (if (isIn) "تحصيل دفعة دين من " else "تسديد دفعة دين إلى ") + debt.creditor,
                amountCents = amountCents,
                creditor = debt.creditor
            )
        )
        showToast("تم تسجيل الدفعة")
    }

    // --- Tools & Settings ---
    fun addWarranty(w: WarrantyEntity) = viewModelScope.launch {
        if (w.deviceName.isBlank()) return@launch
        warrantyDao.insert(w)
        showToast("تم حفظ ضمان ${w.deviceName}")
    }

    fun deleteWarranty(id: Long) = viewModelScope.launch { warrantyDao.deleteById(id) }

    fun addPriceCheck(product: String, store: String, unitPriceCents: Long) = viewModelScope.launch {
        if (product.isBlank() || store.isBlank() || unitPriceCents <= 0L) return@launch
        priceDao.insert(
            PriceCheckEntity(
                product = product.trim(),
                store = store.trim(),
                unitPriceCents = unitPriceCents,
                dateISO = DateUtils.todayISO()
            )
        )
        showToast("تم تسجيل سعر ${product.trim()}")
    }

    fun deletePriceCheck(id: Long) = viewModelScope.launch { priceDao.deleteById(id) }

    fun addFamilyMember(name: String) = viewModelScope.launch {
        val current = uiState.value.settings.familyMembers
        if (name.isNotBlank() && name.trim() !in current) {
            prefs.updateFamilyMembers(current + name.trim())
            showToast("تمت إضافة ${name.trim()}")
        }
    }

    fun removeFamilyMember(name: String) = viewModelScope.launch {
        val current = uiState.value.settings.familyMembers
        if (current.size <= 1) {
            showToast("يجب الإبقاء على فرد واحد على الأقل")
            return@launch
        }
        val updated = current.filterNot { it == name }
        prefs.updateFamilyMembers(updated)
        if (uiState.value.settings.activeFamilyMember == name && updated.isNotEmpty()) {
            prefs.setActiveFamilyMember(updated.first())
        }
        showToast("تمت إزالة $name")
    }

    fun addQuickExpensePreset(
        icon: String,
        name: String,
        amountCents: Long,
        category: String = "مواد غذائية"
    ) = viewModelScope.launch {
        if (name.isBlank() || amountCents <= 0L) {
            showToast("أدخل اسم المصروف والمبلغ")
            return@launch
        }
        val current = uiState.value.settings.quickExpenses
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        val finalIcon = icon.trim().ifBlank { "🛍️" }
        prefs.updateQuickExpenses(
            current + QuickExpensePreset(
                id = nextId,
                icon = finalIcon,
                name = name.trim(),
                amountCents = amountCents,
                category = category
            )
        )
        showToast("تمت إضافة زر ${name.trim()} السريع")
    }

    fun deleteQuickExpensePreset(id: Long) = viewModelScope.launch {
        val updated = uiState.value.settings.quickExpenses.filterNot { it.id == id }
        prefs.updateQuickExpenses(updated)
        showToast("تم حذف الزر السريع")
    }

    fun addCustomProduct(
        name: String,
        emoji: String,
        categoryKey: String,
        defaultUnit: String = "حبة"
    ) = viewModelScope.launch {
        if (name.isBlank()) {
            showToast("أدخل اسم الصنف")
            return@launch
        }
        val catLabel = CatalogDefaults.categoryTabs.firstOrNull { it.first == categoryKey }?.second ?: "مواد غذائية"
        val current = uiState.value.settings.customProducts
        val finalEmoji = emoji.trim().ifBlank { "🛍️" }
        val newProd = CatalogProduct(
            name = name.trim(),
            emoji = finalEmoji,
            categoryKey = categoryKey,
            categoryLabel = catLabel,
            defaultUnit = defaultUnit
        )
        prefs.updateCustomProducts(current.filterNot { it.name == newProd.name && it.categoryKey == categoryKey } + newProd)
        showToast("تمت إضافة ${name.trim()} إلى قائمة المنتجات")
    }

    fun deleteCustomProduct(name: String, categoryKey: String) = viewModelScope.launch {
        val updated = uiState.value.settings.customProducts.filterNot { it.name == name && it.categoryKey == categoryKey }
        prefs.updateCustomProducts(updated)
    }

    fun saveSewingRates(meterCents: Long, threadCents: Long) = viewModelScope.launch {
        prefs.setSewingRates(meterCents, threadCents)
        showToast("تم تحديث تسعيرة الورشة")
    }

    fun saveAccountOpenings(personalCents: Long, sewingCents: Long) = viewModelScope.launch {
        prefs.setAccountOpenings(personalCents, sewingCents)
        showToast("تم حفظ الأرصدة الافتتاحية للحسابات")
    }

    fun applyVoiceEntry(parsed: ParsedVoiceEntry, finalTitle: String, finalAmountCents: Long, finalMode: VoiceEntryMode, finalParty: String, immediateTreasury: Boolean) = viewModelScope.launch {
        val title = finalTitle.ifBlank { parsed.title }.ifBlank { "حركة سريعة" }
        if (finalAmountCents <= 0L) {
            showToast("أدخل مبلغاً صحيحاً")
            return@launch
        }
        when (finalMode) {
            VoiceEntryMode.CASH -> addGroceryExpense(title, 1.0, "حبة", finalAmountCents, "أخرى", DateUtils.todayISO(), false, "")
            VoiceEntryMode.DEBT_OUT -> addGroceryExpense(title, 1.0, "حبة", finalAmountCents, "أخرى", DateUtils.todayISO(), true, finalParty.ifBlank { "محل / تاجر" })
            VoiceEntryMode.RECEIVABLE -> addRentRecord(title, finalParty.ifBlank { "شخص" }, finalAmountCents, true, immediateTreasury, "")
        }
        _showVoice.value = false
    }

    fun searchAll(query: String): List<SearchHit> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val state = uiState.value
        val hits = mutableListOf<SearchHit>()
        state.transactions.filter { it.name.lowercase().contains(q) || it.creditor.lowercase().contains(q) }.take(15).forEach {
            hits.add(SearchHit("عملية", it.name, "${it.dateISO} • ${MoneyFormat.formatDzd(it.amountCents)}", MainNavTab.REPORTS))
        }
        state.creditorGroups.filter { it.creditor.lowercase().contains(q) }.take(10).forEach {
            hits.add(SearchHit("دين محل", it.creditor, "المتبقي ${MoneyFormat.formatDzd(it.remainingCents)}", MainNavTab.DEBTS))
        }
        state.orders.filter { it.client.lowercase().contains(q) || it.item.lowercase().contains(q) }.take(10).forEach {
            hits.add(SearchHit("طلبية خياطة", "${it.client} — ${it.item}", "${it.stage} • ${MoneyFormat.formatDzd(it.totalCents)}", MainNavTab.SEWING))
        }
        state.warranties.filter { it.deviceName.lowercase().contains(q) }.take(5).forEach {
            hits.add(SearchHit("ضمان", it.deviceName, "ينتهي ${it.expiryDateISO}", MainNavTab.TOOLS))
        }
        return hits
    }

    fun exportJsonBackup(): String {
        val s = uiState.value
        return BackupManager.encodeJson(
            SandooqiBackupBundle(
                exportedAt = DateUtils.todayISO(),
                settings = s.settings,
                transactions = s.transactions,
                debts = s.debts,
                obligations = s.obligations,
                facPlans = s.facPlans,
                goals = s.goals,
                seasonalBudgets = s.seasonalBudgets,
                orders = s.orders,
                sewServices = s.sewServices,
                sewProjects = s.sewProjects,
                sewMaterials = s.sewMaterials,
                measurements = s.measurements,
                marketItems = s.marketItems,
                priceChecks = s.priceChecks,
                warranties = s.warranties,
                monthRecords = s.monthRecords.values.toList()
            )
        )
    }

    fun importJsonBackup(raw: String) = viewModelScope.launch {
        val bundle = BackupManager.decodeJson(raw)
        if (bundle == null) {
            showToast("ملف النسخة الاحتياطية غير صالح")
            return@launch
        }
        txDao.clearAll(); txDao.insertAll(bundle.transactions)
        debtDao.clearAll(); debtDao.insertAll(bundle.debts)
        oblDao.clearAll(); oblDao.insertAll(bundle.obligations)
        facDao.clearAll(); facDao.insertAll(bundle.facPlans)
        goalDao.clearAll(); goalDao.insertAll(bundle.goals)
        seasonDao.clearAll(); seasonDao.insertAll(bundle.seasonalBudgets)
        orderDao.clearAll(); orderDao.insertAll(bundle.orders)
        sewServiceDao.clearAll(); sewServiceDao.insertAll(bundle.sewServices)
        sewProjectDao.clearAll(); sewProjectDao.insertAll(bundle.sewProjects)
        materialDao.clearAll(); materialDao.insertAll(bundle.sewMaterials)
        measDao.clearAll(); measDao.insertAll(bundle.measurements)
        marketDao.clearAll(); marketDao.insertAll(bundle.marketItems)
        priceDao.clearAll(); priceDao.insertAll(bundle.priceChecks)
        warrantyDao.clearAll(); warrantyDao.insertAll(bundle.warranties)
        monthDao.clearAll(); monthDao.insertAll(bundle.monthRecords)
        prefs.applySettings(bundle.settings)
        showToast("تمت استعادة النسخة الاحتياطية بنجاح")
    }

    fun selectiveReset(
        resetSalary: Boolean,
        resetFamilyMonth: Boolean,
        resetSewingAccount: Boolean,
        resetHomeDebts: Boolean,
        resetSewDebts: Boolean
    ) = viewModelScope.launch {
        val ym = DateUtils.currentYM()
        if (resetSalary) {
            val existing = monthDao.getByYm(ym) ?: MonthRecordEntity(ym = ym)
            monthDao.upsert(existing.copy(salary1Cents = 0L, salary2Cents = 0L))
        }
        if (resetFamilyMonth) {
            txDao.clearFamilyMonth(ym)
        }
        if (resetSewingAccount) {
            txDao.clearSewingAccount()
        }
        if (resetHomeDebts) {
            debtDao.clearHomeAndBillDebts()
        }
        if (resetSewDebts) {
            debtDao.clearSewingDebts()
        }
        showToast("تم تصفير الأقسام المحددة بنجاح")
    }

    fun resetAllData() = viewModelScope.launch {
        txDao.clearAll()
        debtDao.clearAll()
        oblDao.clearAll()
        facDao.clearAll()
        goalDao.clearAll()
        seasonDao.clearAll()
        orderDao.clearAll()
        sewServiceDao.clearAll()
        sewProjectDao.clearAll()
        materialDao.clearAll()
        measDao.clearAll()
        marketDao.clearAll()
        priceDao.clearAll()
        warrantyDao.clearAll()
        monthDao.clearAll()
        monthDao.upsert(MonthRecordEntity(ym = DateUtils.currentYM(), salary1Cents = 0L, salary2Cents = 0L))
        goalDao.insert(SavingsGoalEntity(name = "حصّالة الطوارئ والعيد", targetCents = 50_000_00L))
        showToast("تم تصفير جميع البيانات")
    }

    private fun accountTitle(id: String) = when (id) {
        "personal" -> "الحساب الشخصي"
        "sewing" -> "حساب الخياطة"
        else -> "الحساب العائلي"
    }

    private data class CorePart(
        val settings: SandooqiSettings,
        val txs: List<TransactionEntity>,
        val debts: List<CreditorDebtEntity>,
        val obls: List<ObligationEntity>,
        val facs: List<FacPlanEntity>
    )

    private data class SavingsSewPart(
        val goals: List<SavingsGoalEntity>,
        val seasons: List<SeasonalBudgetEntity>,
        val orders: List<TailoringOrderEntity>,
        val mats: List<SewMaterialEntity>,
        val meas: List<CustomerMeasurementEntity>
    )

    private data class ExtrasPart(
        val market: List<MarketItemEntity>,
        val prices: List<PriceCheckEntity>,
        val warranties: List<WarrantyEntity>,
        val months: Map<String, MonthRecordEntity>,
        val services: List<SewServiceEntity>,
        val projects: List<SewProjectEntity>
    )

    private data class UiControlsPart(
        val tab: MainNavTab,
        val reportYm: String,
        val unlocked: Boolean,
        val toast: String?,
        val showVoice: Boolean,
        val showSearch: Boolean,
        val showMonthClose: Boolean
    )
}
