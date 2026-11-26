package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AccountCashSummary
import com.example.domain.DateUtils
import com.example.domain.LedgerCalculator
import com.example.domain.MoneyFormat
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MoneyText
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel

@Composable
fun TreasuryScreen(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel
) {
    val report = state.currentReport
    val privacy = state.settings.privacyMode

    var s1Text by remember(report.ym, report.salary1Cents) {
        mutableStateOf(if (report.salary1Cents > 0L) MoneyFormat.formatNumberOnly(report.salary1Cents) else "")
    }
    var s2Text by remember(report.ym, report.salary2Cents) {
        mutableStateOf(if (report.salary2Cents > 0L) MoneyFormat.formatNumberOnly(report.salary2Cents) else "")
    }

    var fromAcc by remember { mutableStateOf("family") }
    var toAcc by remember { mutableStateOf("personal") }
    var transferAmt by remember { mutableStateOf("") }
    var transferNote by remember { mutableStateOf("") }

    var manualAcc by remember { mutableStateOf("family") }
    var manualType by remember { mutableStateOf("in") }
    var manualName by remember { mutableStateOf("") }
    var manualAmt by remember { mutableStateOf("") }

    var historyFilter by remember { mutableStateOf("all") }

    val filteredHistory = remember(state.transactions, historyFilter, state.currentYM) {
        state.transactions.filter { tx ->
            tx.ym == state.currentYM && (historyFilter == "all" || tx.accountId == historyFilter)
        }.take(30)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 3 Account Cards matching screenshot
        item {
            AccountBalanceCard(
                summary = report.family,
                privacyMode = privacy,
                testTag = "card_account_family"
            )
        }
        item {
            AccountBalanceCard(
                summary = report.personal,
                privacyMode = privacy,
                testTag = "card_account_personal"
            )
        }
        item {
            AccountBalanceCard(
                summary = report.sewing,
                privacyMode = privacy,
                testTag = "card_account_sewing"
            )
        }

        // Salaries & Close Month Card (matching screenshot Card 4)
        item {
            SandooqiCard(modifier = Modifier.testTag("card_salaries_close_month")) {
                Text(
                    text = "رواتب ${DateUtils.formatYM(state.currentYM)} وإغلاق الشهر",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "الرصيد المرحّل للشهر الحالي: ${MoneyFormat.formatDzd(report.family.openingCents, privacy)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (report.hasOpenAutoCarryover) {
                    Text(
                        text = "تم احتساب ترحيل تلقائي من الشهر السابق المفتوح.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldMain
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { viewModel.setMonthCloseDialog(true) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("open_close_month_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EventAvailable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "إغلاق الشهر وترحيل الفائض",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = s1Text,
                        onValueChange = { s1Text = it },
                        label = { Text("الراتب الأول (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("salary_1_input")
                    )
                    OutlinedTextField(
                        value = s2Text,
                        onValueChange = { s2Text = it },
                        label = { Text("الراتب الثاني (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("salary_2_input")
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val c1 = MoneyFormat.parseToCents(s1Text) ?: 0L
                        val c2 = MoneyFormat.parseToCents(s2Text) ?: 0L
                        viewModel.saveSalaries(state.currentYM, c1, c2)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("save_salaries_button")
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("حفظ رواتب الشهر")
                }
            }
        }

        // Smart Salary Allocator
        item {
            val totalSal = report.salary1Cents + report.salary2Cents
            SandooqiCard {
                SectionHeader(
                    title = "التوزيع الذكي للرواتب",
                    subtitle = if (totalSal > 0L) "إجمالي الرواتب: ${MoneyFormat.formatDzd(totalSal, privacy)}" else "أدخل الرواتب لتفعيل التوزيع السريع",
                    icon = Icons.Rounded.PieChart
                )
                Spacer(Modifier.height(10.dp))
                if (totalSal > 0L) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AllocationPresetButton(
                            label = "60/20/20",
                            sub = "عائلي/شخصي/ادخار",
                            modifier = Modifier.weight(1f)
                        ) {
                            val personalPart = (totalSal * 20L) / 100L
                            val goal = state.goals.firstOrNull()
                            viewModel.transferBetweenAccounts("family", "personal", personalPart, "توزيع راتب 60/20/20")
                            if (goal != null) {
                                viewModel.depositToGoal(goal, (totalSal * 20L) / 100L, "family")
                            }
                        }
                        AllocationPresetButton(
                            label = "50/30/20",
                            sub = "مرن ومتوازن",
                            modifier = Modifier.weight(1f)
                        ) {
                            val personalPart = (totalSal * 30L) / 100L
                            val goal = state.goals.firstOrNull()
                            viewModel.transferBetweenAccounts("family", "personal", personalPart, "توزيع راتب 50/30/20")
                            if (goal != null) {
                                viewModel.depositToGoal(goal, (totalSal * 20L) / 100L, "family")
                            }
                        }
                        AllocationPresetButton(
                            label = "70/15/15",
                            sub = "أولوية البيت",
                            modifier = Modifier.weight(1f)
                        ) {
                            val personalPart = (totalSal * 15L) / 100L
                            val goal = state.goals.firstOrNull()
                            viewModel.transferBetweenAccounts("family", "personal", personalPart, "توزيع راتب 70/15/15")
                            if (goal != null) {
                                viewModel.depositToGoal(goal, (totalSal * 15L) / 100L, "family")
                            }
                        }
                    }
                }
            }
        }

        // Internal Transfer Card
        item {
            SandooqiCard {
                SectionHeader(
                    title = "تحويل بين الحسابات",
                    subtitle = "تحويل متوازن لا يغيّر صافي الثروة",
                    icon = Icons.Rounded.SwapHoriz
                )
                Spacer(Modifier.height(10.dp))
                Text("من حساب:", style = MaterialTheme.typography.labelMedium)
                AccountChoiceRow(selected = fromAcc, onSelect = { fromAcc = it })
                Spacer(Modifier.height(6.dp))
                Text("إلى حساب:", style = MaterialTheme.typography.labelMedium)
                AccountChoiceRow(selected = toAcc, onSelect = { toAcc = it })
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = transferAmt,
                        onValueChange = { transferAmt = it },
                        label = { Text("المبلغ (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("transfer_amount_input")
                    )
                    OutlinedTextField(
                        value = transferNote,
                        onValueChange = { transferNote = it },
                        label = { Text("ملاحظة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val cents = MoneyFormat.parseToCents(transferAmt) ?: 0L
                        viewModel.transferBetweenAccounts(fromAcc, toAcc, cents, transferNote)
                        if (cents > 0L && fromAcc != toAcc) {
                            transferAmt = ""
                            transferNote = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("execute_transfer_button")
                ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تنفيذ التحويل")
                }
            }
        }

        // External Income / Expense Card
        item {
            SandooqiCard {
                SectionHeader(
                    title = "تسجيل مدخول أو مصروف مباشر",
                    subtitle = "إضافة حركة مباشرة لأي حساب",
                    icon = Icons.Rounded.AddCard
                )
                Spacer(Modifier.height(10.dp))
                AccountChoiceRow(selected = manualAcc, onSelect = { manualAcc = it })
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = manualType == "in",
                        onClick = { manualType = "in" },
                        label = { Text("مدخول (+)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = manualType == "out",
                        onClick = { manualType = "out" },
                        label = { Text("مصروف (-)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = manualName,
                        onValueChange = { manualName = it },
                        label = { Text("البيان") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = manualAmt,
                        onValueChange = { manualAmt = it },
                        label = { Text("المبلغ (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = {
                        val cents = MoneyFormat.parseToCents(manualAmt) ?: 0L
                        viewModel.addManualTransaction(manualAcc, manualType, manualName, cents)
                        if (manualName.isNotBlank() && cents > 0L) {
                            manualName = ""
                            manualAmt = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("add_manual_tx_button")
                ) {
                    Text("تسجيل الحركة في الخزينة")
                }
            }
        }

        // Treasury History Header + Filter Chips
        item {
            SectionHeader(
                title = "سجل حركات الخزينة",
                subtitle = DateUtils.formatYM(state.currentYM),
                icon = Icons.Rounded.History
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("all" to "الكل", "family" to "العائلي", "personal" to "الشخصي", "sewing" to "الخياطة").forEach { (id, label) ->
                    FilterChip(
                        selected = historyFilter == id,
                        onClick = { historyFilter = id },
                        label = { Text(label, maxLines = 1) }
                    )
                }
            }
        }

        if (filteredHistory.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "لا توجد حركات مسجلة في هذا الفلتر",
                    description = "يمكنك تسجيل مدخول، مصروف أو تحويل بين الحسابات من الأعلى.",
                    icon = Icons.Rounded.AccountBalanceWallet
                )
            }
        } else {
            items(filteredHistory, key = { it.id }) { tx ->
                val isTransfer = LedgerCalculator.isInternalTransfer(tx)
                val isCredit = tx.isCreditPurchase
                val isIn = tx.type == "in" || (isTransfer && LedgerCalculator.transferDirectionFor(tx, tx.accountId) == "in")
                val amtColor = when {
                    isTransfer -> MaterialTheme.colorScheme.primary
                    isCredit -> GoldAccent
                    isIn -> IncomeGreen
                    else -> ExpenseRed
                }
                val prefix = if (isIn) "+" else "-"
                SandooqiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${tx.dateISO} • ${accountLabel(tx.accountId)}" + if (isCredit) " • دين" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$prefix${MoneyFormat.formatDzd(tx.amountCents, privacy)}",
                                color = amtColor,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { viewModel.deleteTransaction(tx) }) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف العملية",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountBalanceCard(
    summary: AccountCashSummary,
    privacyMode: Boolean,
    testTag: String
) {
    SandooqiCard(modifier = Modifier.testTag(testTag)) {
        Text(
            text = summary.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        MoneyText(
            cents = summary.balanceCents,
            privacyMode = privacyMode,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp),
            color = if (summary.balanceCents < 0L) ExpenseRed else MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "دخل: ${MoneyFormat.formatDzd(summary.totalInDisplayCents, privacyMode)}",
                color = EmeraldMain,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "خروج: ${MoneyFormat.formatDzd(summary.totalOutDisplayCents, privacyMode)}",
                color = ExpenseRed,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AllocationPresetButton(
    label: String,
    sub: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 6.dp),
        modifier = modifier.heightIn(min = 56.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Text(sub, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun AccountChoiceRow(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("family" to "العائلي", "personal" to "الشخصي", "sewing" to "الخياطة").forEach { (id, label) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelect(id) },
                label = { Text(label, maxLines = 1) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun accountLabel(id: String): String = when (id) {
    "personal" -> "الشخصي"
    "sewing" -> "الخياطة"
    else -> "العائلي"
}
