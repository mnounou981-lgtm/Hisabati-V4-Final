package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.backup.BackupManager
import com.example.domain.DateUtils
import com.example.domain.LedgerCalculator
import com.example.domain.MoneyFormat
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MonthPickerRow
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.theme.EmeraldMain
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SewingViolet
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun ReportsScreen(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel
) {
    val context = LocalContext.current
    val privacy = state.settings.privacyMode
    val rep = state.selectedReport
    var txQuery by remember { mutableStateOf("") }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = BackupManager.buildMonthlyCsv(rep.ym, state.transactions, state.orders)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray(Charsets.UTF_8))
                }
                viewModel.showToast("تم تصدير تقرير ${DateUtils.formatYM(rep.ym)} بنجاح")
            } catch (_: Exception) {
                viewModel.showToast("تعذر حفظ ملف CSV")
            }
        }
    }

    val last6Months = remember(rep.ym, state.transactions, state.monthRecords, state.settings) {
        (5 downTo 0).map { offset ->
            val ym = DateUtils.shiftYM(rep.ym, -offset)
            LedgerCalculator.computeMonthlyReport(ym, state.transactions, state.monthRecords, state.settings)
        }
    }

    val monthTx = remember(state.transactions, rep.ym, txQuery) {
        val q = txQuery.trim().lowercase()
        state.transactions.filter {
            it.ym == rep.ym && (q.isEmpty() || it.name.lowercase().contains(q) || it.creditor.lowercase().contains(q))
        }
    }

    val categoryTotals = remember(state.transactions, rep.ym) {
        state.transactions.filter {
            it.ym == rep.ym && it.type == "out" && !LedgerCalculator.isInternalTransfer(it)
        }.groupBy { it.expenseCategory.ifBlank { it.category } }
            .mapValues { (_, list) -> list.sumOf { it.amountCents } }
            .entries.sortedByDescending { it.value }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            MonthPickerRow(
                ym = rep.ym,
                onPreviousMonth = { viewModel.selectReportMonth(DateUtils.shiftYM(rep.ym, -1)) },
                onNextMonth = { viewModel.selectReportMonth(DateUtils.shiftYM(rep.ym, 1)) }
            )
        }

        // Surplus / Deficit Banner + Export CSV
        item {
            val net = rep.combinedNetFlowCents
            SandooqiCard(
                containerColor = if (net >= 0L) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
            ) {
                SectionHeader(
                    title = if (net >= 0L) "فائض تدفق الشهر" else "عجز تدفق الشهر",
                    subtitle = "المداخيل مقابل الصرف النقدي الفعلي",
                    icon = Icons.Rounded.BarChart
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = MoneyFormat.formatDzd(net, privacy),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (net >= 0L) EmeraldMain else ExpenseRed,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("المداخيل", MoneyFormat.formatDzd(rep.combinedIncomeCents, privacy), IncomeGreen, MaterialTheme.colorScheme.surface, Modifier.weight(1f))
                    StatTile("المصروف المسجل", MoneyFormat.formatDzd(rep.combinedRecordedExpenseCents, privacy), ExpenseRed, MaterialTheme.colorScheme.surface, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { csvLauncher.launch("Sandooqi_Report_${rep.ym}.csv") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("export_csv_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تصدير التقرير الشهري (Excel CSV)")
                }
            }
        }

        // 6-Month Bar Chart via Compose Canvas
        item {
            SandooqiCard {
                SectionHeader(
                    title = "مقارنة آخر 6 أشهر",
                    subtitle = "الأخضر: المداخيل • الأحمر: المصاريف",
                    icon = Icons.Rounded.ShowChart
                )
                Spacer(Modifier.height(12.dp))
                val maxVal = max(
                    1L,
                    last6Months.maxOf { max(it.combinedIncomeCents, it.combinedOutCashCents) }
                ).toFloat()
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val slotW = size.width / last6Months.size
                    val barW = (slotW * 0.28f).coerceAtLeast(8f)
                    last6Months.forEachIndexed { index, m ->
                        val centerX = slotW * index + slotW / 2f
                        val inH = (m.combinedIncomeCents.toFloat() / maxVal) * (size.height - 16f)
                        val outH = (m.combinedOutCashCents.toFloat() / maxVal) * (size.height - 16f)
                        drawRoundRect(
                            color = IncomeGreen,
                            topLeft = Offset(centerX - barW - 3f, size.height - inH),
                            size = Size(barW, max(4f, inH)),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = ExpenseRed,
                            topLeft = Offset(centerX + 3f, size.height - outH),
                            size = Size(barW, max(4f, outH)),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    last6Months.forEach { m ->
                        Text(DateUtils.formatShortMonth(m.ym), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Category Breakdown
        if (categoryTotals.isNotEmpty()) {
            val totalOut = max(1L, categoryTotals.sumOf { it.value })
            item {
                SandooqiCard {
                    SectionHeader(title = "توزيع المصاريف حسب الفئة", icon = Icons.Rounded.DonutLarge)
                    Spacer(Modifier.height(8.dp))
                    categoryTotals.take(6).forEach { (cat, cents) ->
                        val ratio = min(1f, cents.toFloat() / totalOut.toFloat())
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cat, style = MaterialTheme.typography.bodySmall)
                            Text("${MoneyFormat.formatDzd(cents, privacy)} (${(ratio * 100).toInt()}%)", style = MaterialTheme.typography.labelMedium)
                        }
                        LinearProgressIndicator(
                            progress = { ratio },
                            color = SewingViolet,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        // Full Searchable Transaction Log
        item {
            OutlinedTextField(
                value = txQuery,
                onValueChange = { txQuery = it },
                label = { Text("تصفية حركات ${DateUtils.formatYM(rep.ym)}...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (monthTx.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "لا توجد حركات مطابقة في هذا الشهر",
                    description = "جرّب الانتقال إلى شهر آخر أو إزالة نص البحث.",
                    icon = Icons.Rounded.SearchOff
                )
            }
        } else {
            items(monthTx, key = { it.id }) { tx ->
                SandooqiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.name, style = MaterialTheme.typography.titleSmall)
                            Text("${tx.dateISO} • ${tx.expenseCategory.ifBlank { tx.category }}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            text = MoneyFormat.formatDzd(tx.amountCents, privacy),
                            color = if (tx.type == "in") IncomeGreen else ExpenseRed,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
