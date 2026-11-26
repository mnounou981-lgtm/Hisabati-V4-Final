package com.example.ui.screens.sewing

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CreditorDebtEntity
import com.example.data.local.TailoringOrderEntity
import com.example.domain.DateUtils
import com.example.domain.LedgerCalculator
import com.example.domain.MoneyFormat
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.theme.EmeraldMain
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel
import kotlin.math.max

fun LazyListScope.sewingFinanceItems(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel,
    onPayOrder: (TailoringOrderEntity) -> Unit,
    onPaySewDebt: (CreditorDebtEntity) -> Unit
) {
    item {
        SewingFinanceDashboardAndCashbox(
            state = state,
            viewModel = viewModel,
            onPayOrder = onPayOrder,
            onPaySewDebt = onPaySewDebt
        )
    }
}

@Composable
private fun SewingFinanceDashboardAndCashbox(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel,
    onPayOrder: (TailoringOrderEntity) -> Unit,
    onPaySewDebt: (CreditorDebtEntity) -> Unit
) {
    val context = LocalContext.current
    val privacy = state.settings.privacyMode
    val ws = state.workshopSummary
    val today = DateUtils.todayISO()

    // Receivables state
    var showReceivables by remember { mutableStateOf(false) }
    var recSearch by remember { mutableStateOf("") }
    var recFilter by remember { mutableStateOf("all") } // delivered, open, all

    // Monthly sewing report
    var reportYm by remember { mutableStateOf(state.currentYM) }

    // Cashbox forms
    var meterCount by remember { mutableStateOf("1") }
    var meterRate by remember(state.settings.meterPriceCents) { mutableStateOf(MoneyFormat.formatNumberOnly(state.settings.meterPriceCents)) }
    var meterNote by remember { mutableStateOf("") }

    var threadQty by remember { mutableStateOf("1") }
    var threadPrice by remember(state.settings.threadPriceCents) { mutableStateOf(MoneyFormat.formatNumberOnly(state.settings.threadPriceCents)) }

    var manualName by remember { mutableStateOf("") }
    var manualAmt by remember { mutableStateOf("") }

    var transferAmt by remember { mutableStateOf("") }

    // Sewing debts
    var dIsReceivable by remember { mutableStateOf(true) }
    var dName by remember { mutableStateOf("") }
    var dAmt by remember { mutableStateOf("") }
    var dDue by remember { mutableStateOf("") }
    var dSearch by remember { mutableStateOf("") }
    var dFilter by remember { mutableStateOf("all") } // all, open, paid, late

    val receivablesList = remember(state.orders, recSearch, recFilter) {
        val q = recSearch.trim().lowercase()
        state.orders.filter { o ->
            val rem = LedgerCalculator.orderRemainingCents(o)
            val matchFilter = when (recFilter) {
                "delivered" -> o.isDone
                "open" -> !o.isDone
                else -> true
            }
            val matchQ = q.isEmpty() || o.client.lowercase().contains(q) || o.item.lowercase().contains(q)
            !o.isCancelled && rem > 0L && matchFilter && matchQ
        }
    }

    val vipClients = remember(state.orders) {
        state.orders
            .filter { !it.isCancelled }
            .groupBy { it.client.trim() }
            .mapValues { (_, list) -> list.size to list.sumOf { it.totalCents } }
            .entries.sortedByDescending { it.value.second }
            .take(5)
    }

    val svcProfitTotal = remember(state.sewServices) {
        state.sewServices.sumOf { it.priceCents - it.threadCostCents - it.suppliesCostCents }
    }
    val prjProfitTotal = remember(state.sewProjects) {
        state.sewProjects.sumOf { it.salePriceCents - (it.fabricCostCents + it.suppliesCostCents + it.otherCostCents) }
    }
    val prjRealizedProfit = remember(state.sewProjects) {
        state.sewProjects.sumOf { it.paidCents - (it.fabricCostCents + it.suppliesCostCents + it.otherCostCents) }
    }

    val monthOrders = remember(state.orders, reportYm) {
        state.orders.filter { !it.isCancelled && it.dateISO.startsWith(reportYm) }
    }
    val monthSewTxs = remember(state.transactions, reportYm) {
        state.transactions.filter { it.ym == reportYm && it.accountId == "sewing" }
    }
    val monthSewIn = monthSewTxs.filter { it.type == "in" }.sumOf { it.amountCents }
    val monthSewOut = monthSewTxs.filter { it.type == "out" }.sumOf { it.amountCents }

    val filteredSewDebts = remember(state.sewDebts, dSearch, dFilter, today) {
        val q = dSearch.trim().lowercase()
        state.sewDebts.filter { d ->
            val rem = max(0L, d.amountCents - d.paidCents)
            val isLate = rem > 0L && d.dueDateISO.isNotBlank() && d.dueDateISO < today
            val matchFilter = when (dFilter) {
                "open" -> rem > 0L
                "paid" -> rem == 0L
                "late" -> isLate
                else -> true
            }
            val matchQ = q.isEmpty() || d.creditor.lowercase().contains(q)
            matchFilter && matchQ
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Receivables Entry & Panel
        SandooqiCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("مستحقات الزبائن", style = MaterialTheme.typography.titleMedium)
                    Text("إجمالي الذمم: ${MoneyFormat.formatDzd(ws.clientReceivablesCents, privacy)}", style = MaterialTheme.typography.bodySmall, color = GoldAccent)
                }
                Button(onClick = { showReceivables = !showReceivables }) {
                    Text(if (showReceivables) "إخفاء" else "عرض المستحقات")
                }
            }
            if (showReceivables) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = recSearch, onValueChange = { recSearch = it }, label = { Text("اسم الزبونة أو الموديل") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to "الكل", "delivered" to "المسلّمة", "open" to "الجارية").forEach { (k, lbl) ->
                        FilterChip(selected = recFilter == k, onClick = { recFilter = k }, label = { Text(lbl) }, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (receivablesList.isEmpty()) {
                    Text("لا توجد مستحقات مطابقة", style = MaterialTheme.typography.bodySmall)
                } else {
                    receivablesList.forEach { o ->
                        val rem = LedgerCalculator.orderRemainingCents(o)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${o.client} — ${o.item}", style = MaterialTheme.typography.titleSmall)
                                Text("المتبقي: ${MoneyFormat.formatDzd(rem, privacy)} (${o.stage})", style = MaterialTheme.typography.bodySmall, color = GoldAccent)
                            }
                            FilledTonalButton(onClick = { onPayOrder(o) }) { Text("تحصيل") }
                        }
                    }
                }
            }
        }

        // 6-KPI Profit Dashboard
        SandooqiCard {
            SectionHeader(
                title = "أرباح الخياطة (ملخص كل الطلبيات)",
                subtitle = "عدد الطلبيات الكلي: ${ws.totalOrdersCount} • الجارية: ${ws.openOrdersCount}",
                icon = Icons.Rounded.Analytics
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatTile("قيمة الطلبيات", MoneyFormat.formatDzd(ws.activeSalesCents, privacy), EmeraldMain, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), Modifier.weight(1f))
                StatTile("صافي المقبوض", MoneyFormat.formatDzd(ws.collectedNetCents, privacy), IncomeGreen, MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
                StatTile("المتبقي", MoneyFormat.formatDzd(ws.clientReceivablesCents, privacy), GoldAccent, MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatTile("التكلفة", MoneyFormat.formatDzd(ws.activeCostsCents, privacy), ExpenseRed, MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
                StatTile("الربح المتوقع", MoneyFormat.formatDzd(ws.projectedActiveProfitCents, privacy), EmeraldMain, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), Modifier.weight(1f))
                StatTile("الربح المحقق", MoneyFormat.formatDzd(ws.realizedProfitCents, privacy), if (ws.realizedProfitCents >= 0L) IncomeGreen else ExpenseRed, MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f), Modifier.weight(1f))
            }
        }

        // VIP Clients
        if (vipClients.isNotEmpty()) {
            SandooqiCard {
                SectionHeader(title = "أفضل زبونات الورشة (VIP)", icon = Icons.Rounded.Star)
                Spacer(Modifier.height(6.dp))
                vipClients.forEach { (cName, stats) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("$cName (${stats.first} طلبيات)", style = MaterialTheme.typography.bodySmall)
                        Text(MoneyFormat.formatDzd(stats.second, privacy), style = MaterialTheme.typography.labelMedium, color = EmeraldMain)
                    }
                }
            }
        }

        // Services & Projects Summary
        SandooqiCard {
            SectionHeader(title = "ملخص الخدمات والمشاريع", icon = Icons.Rounded.Assessment)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("ربح خدمات الزبائن", MoneyFormat.formatDzd(svcProfitTotal, privacy), EmeraldMain, MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
                StatTile("ربح مشاريعي المتوقع", MoneyFormat.formatDzd(prjProfitTotal, privacy), IncomeGreen, MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("الخدمات + المحقق", MoneyFormat.formatDzd(svcProfitTotal + prjRealizedProfit, privacy), EmeraldMain, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), Modifier.weight(1f))
                StatTile("المشاريع الخاصة", "${state.sewProjects.size}", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceVariant, Modifier.weight(1f))
            }
        }

        // Monthly Sewing Report
        SandooqiCard {
            SectionHeader(title = "تقرير الخياطة الشهري", subtitle = DateUtils.formatYM(reportYm), icon = Icons.Rounded.Summarize)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { reportYm = DateUtils.shiftYM(reportYm, -1) }) { Text("السابق") }
                Text(DateUtils.formatYM(reportYm), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                OutlinedButton(onClick = { reportYm = DateUtils.shiftYM(reportYm, 1) }) { Text("التالي") }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "طلبيات الشهر: ${monthOrders.size} • المداخيل: ${MoneyFormat.formatDzd(monthSewIn, privacy)} • المصاريف: ${MoneyFormat.formatDzd(monthSewOut, privacy)} • الصافي: ${MoneyFormat.formatDzd(monthSewIn - monthSewOut, privacy)}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = {
                    val text = "تقرير الخياطة — ${DateUtils.formatYM(reportYm)}\nالطلبيات: ${monthOrders.size}\nالمداخيل: ${MoneyFormat.formatDzd(monthSewIn)}\nالمصاريف: ${MoneyFormat.formatDzd(monthSewOut)}\nالصافي: ${MoneyFormat.formatDzd(monthSewIn - monthSewOut)}"
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                    }, "مشاركة تقرير الخياطة"))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("مشاركة / طباعة التقرير الشهري") }
        }

        // Sewing Cashbox Tools (حركات صندوق الخياطة)
        SandooqiCard {
            SectionHeader(
                title = "حركات صندوق الخياطة",
                subtitle = "رصيد صندوق الخياطة: ${MoneyFormat.formatDzd(state.currentReport.sewing.balanceCents, privacy)}",
                icon = Icons.Rounded.PointOfSale
            )
            Spacer(Modifier.height(10.dp))

            // 1) Meter Income
            Text("1. تسجيل مدخول بالمتر", style = MaterialTheme.typography.labelLarge, color = EmeraldMain)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = meterCount, onValueChange = { meterCount = it }, label = { Text("الأمتار") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.8f))
                OutlinedTextField(value = meterRate, onValueChange = { meterRate = it }, label = { Text("سعر المتر") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = meterNote, onValueChange = { meterNote = it }, label = { Text("الوصف") }, singleLine = true, modifier = Modifier.weight(1.2f))
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    val m = MoneyFormat.normalizeDigits(meterCount).replace(",", ".").toDoubleOrNull() ?: 0.0
                    val r = MoneyFormat.parseToCents(meterRate) ?: 0L
                    viewModel.addSewingMeterIncome(m, r, meterNote)
                    meterNote = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("إضافة لمدخول الخياطة") }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))

            // 2) Thread Consumption
            Text("2. تسجيل استهلاك الخيط", style = MaterialTheme.typography.labelLarge, color = ExpenseRed)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = threadQty, onValueChange = { threadQty = it }, label = { Text("العدد") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.8f))
                OutlinedTextField(value = threadPrice, onValueChange = { threadPrice = it }, label = { Text("السعر/بكرة") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = {
                    val q = MoneyFormat.normalizeDigits(threadQty).toIntOrNull() ?: 1
                    val p = MoneyFormat.parseToCents(threadPrice) ?: 0L
                    viewModel.addSewingThreadExpense(q, p)
                }) { Text("خصم الخيط") }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))

            // 3) Manual Sewing Expense
            Text("3. تسجيل مصروف يدوي للورشة", style = MaterialTheme.typography.labelLarge, color = ExpenseRed)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = manualName, onValueChange = { manualName = it }, label = { Text("اسم الاستهلاك") }, singleLine = true, modifier = Modifier.weight(1.2f))
                OutlinedTextField(value = manualAmt, onValueChange = { manualAmt = it }, label = { Text("المبلغ") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.9f))
                FilledTonalButton(onClick = {
                    val c = MoneyFormat.parseToCents(manualAmt) ?: 0L
                    viewModel.addManualTransaction("sewing", "out", "استهلاك خياطة: $manualName", c, "sew_out")
                    if (manualName.isNotBlank() && c > 0L) { manualName = ""; manualAmt = "" }
                }) { Text("خصم") }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))

            // 4) Transfer to Family
            Text("4. تحويل إلى الحساب العائلي", style = MaterialTheme.typography.labelLarge, color = EmeraldMain)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = transferAmt, onValueChange = { transferAmt = it }, label = { Text("المبلغ المحوّل للبيت (دج)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                Button(onClick = {
                    val c = MoneyFormat.parseToCents(transferAmt) ?: 0L
                    viewModel.transferBetweenAccounts("sewing", "family", c, "تحويل من صندوق الخياطة إلى العائلي")
                    if (c > 0L) transferAmt = ""
                }) { Text("تحويل للبيت") }
            }
        }

        // 5) Sewing Debts (ديون الخياطة)
        SandooqiCard {
            SectionHeader(title = "ديون الخياطة", subtitle = "مستحقات لكِ عند زبائن أو ديون لوازم عليكِ", icon = Icons.Rounded.ReceiptLong)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = dIsReceivable, onClick = { dIsReceivable = true }, label = { Text("دين لكِ عند زبون (+)") }, modifier = Modifier.weight(1f))
                FilterChip(selected = !dIsReceivable, onClick = { dIsReceivable = false }, label = { Text("دين عليكِ لمحل (-)") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = dName, onValueChange = { dName = it }, label = { Text("الاسم") }, singleLine = true, modifier = Modifier.weight(1.1f))
                OutlinedTextField(value = dAmt, onValueChange = { dAmt = it }, label = { Text("المبلغ") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.9f))
                OutlinedTextField(value = dDue, onValueChange = { dDue = it }, label = { Text("الاستحقاق") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    val c = MoneyFormat.parseToCents(dAmt) ?: 0L
                    viewModel.addSewDebt(dIsReceivable, dName, c, dDue)
                    if (dName.isNotBlank() && c > 0L) { dName = ""; dAmt = ""; dDue = "" }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("تسجيل دين الخياطة") }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = dSearch, onValueChange = { dSearch = it }, label = { Text("بحث في ديون الخياطة...") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("all" to "الكل", "open" to "غير مسددة", "paid" to "مسددة", "late" to "متأخرة").forEach { (k, lbl) ->
                    FilterChip(selected = dFilter == k, onClick = { dFilter = k }, label = { Text(lbl, maxLines = 1) }, modifier = Modifier.weight(1f))
                }
            }
            filteredSewDebts.forEach { d ->
                val rem = max(0L, d.amountCents - d.paidCents)
                val isIn = d.kind == "sew_debt_in"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${d.creditor} (${if (isIn) "لكِ" else "عليكِ"})", style = MaterialTheme.typography.titleSmall)
                            Text("المتبقي: ${MoneyFormat.formatDzd(rem, privacy)} من ${MoneyFormat.formatDzd(d.amountCents, privacy)}", style = MaterialTheme.typography.bodySmall, color = if (rem == 0L) IncomeGreen else if (isIn) EmeraldMain else ExpenseRed)
                        }
                        Row {
                            if (rem > 0L) {
                                TextButton(onClick = { onPaySewDebt(d) }) { Text(if (isIn) "تحصيل" else "تسديد") }
                            }
                            IconButton(onClick = { viewModel.deleteDebt(d.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                            }
                        }
                    }
                }
            }
        }
    }
}
