package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.SeasonalBudgetEntity
import com.example.domain.CatalogDefaults
import com.example.domain.DateUtils
import com.example.domain.MoneyFormat
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.EmeraldMain
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel
import kotlin.math.min

@Composable
fun BudgetScreen(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel
) {
    val privacy = state.settings.privacyMode
    var monthlyText by remember(state.settings.monthlyFamilyBudgetCents) {
        mutableStateOf(MoneyFormat.formatNumberOnly(state.settings.monthlyFamilyBudgetCents))
    }
    var weeklyText by remember(state.settings.weeklyFamilyBudgetCents) {
        mutableStateOf(
            if (state.settings.weeklyFamilyBudgetCents > 0L) MoneyFormat.formatNumberOnly(state.settings.weeklyFamilyBudgetCents) else ""
        )
    }

    var newGoalName by remember { mutableStateOf("") }
    var newGoalTarget by remember { mutableStateOf("") }

    var actionGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var isWithdraw by remember { mutableStateOf(false) }
    var goalAmtText by remember { mutableStateOf("") }
    var goalAccount by remember { mutableStateOf("family") }

    var seasonName by remember { mutableStateOf(CatalogDefaults.seasonalOccasions.first().first) }
    var seasonEmoji by remember { mutableStateOf(CatalogDefaults.seasonalOccasions.first().second) }
    var seasonTarget by remember { mutableStateOf("") }
    var seasonYm by remember { mutableStateOf(DateUtils.shiftYM(state.currentYM, 2)) }

    var fundingSeason by remember { mutableStateOf<SeasonalBudgetEntity?>(null) }
    var seasonFundAmt by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Unified Budget Card
        item {
            SandooqiCard {
                SectionHeader(
                    title = "الميزانية الموحدة للعائلة",
                    subtitle = "سقف واحد مرتبط بالخزينة والمصاريف والتقارير",
                    icon = Icons.Rounded.Tune
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = monthlyText,
                        onValueChange = { monthlyText = it },
                        label = { Text("السقف الشهري (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("monthly_budget_input")
                    )
                    OutlinedTextField(
                        value = weeklyText,
                        onValueChange = { weeklyText = it },
                        label = { Text("السقف الأسبوعي (اختياري)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val m = MoneyFormat.parseToCents(monthlyText) ?: 0L
                        val w = MoneyFormat.parseToCents(weeklyText) ?: 0L
                        viewModel.saveBudgets(m, w)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save_budget_button")
                ) { Text("حفظ سقف الميزانية") }
            }
        }

        // Savings Goals
        item {
            SandooqiCard {
                SectionHeader(
                    title = "أهداف الادخار والحصّالة",
                    subtitle = "إجمالي المدخرات: ${MoneyFormat.formatDzd(state.totalSavingsCents, privacy)}",
                    icon = Icons.Rounded.Savings
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newGoalName, onValueChange = { newGoalName = it }, label = { Text("اسم الهدف") }, singleLine = true, modifier = Modifier.weight(1.2f))
                    OutlinedTextField(
                        value = newGoalTarget,
                        onValueChange = { newGoalTarget = it },
                        label = { Text("المطلوب (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = {
                        val t = MoneyFormat.parseToCents(newGoalTarget) ?: 0L
                        viewModel.addSavingsGoal(newGoalName, t)
                        if (newGoalName.isNotBlank() && t > 0L) { newGoalName = ""; newGoalTarget = "" }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                ) { Text("إنشاء هدف ادخار جديد") }
            }
        }

        items(state.goals, key = { it.id }) { goal ->
            val pct = if (goal.targetCents > 0L) min(1f, goal.savedCents.toFloat() / goal.targetCents.toFloat()) else 0f
            val remaining = (goal.targetCents - goal.savedCents).coerceAtLeast(0L)
            SandooqiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(goal.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${MoneyFormat.formatDzd(goal.savedCents, privacy)} / ${MoneyFormat.formatDzd(goal.targetCents, privacy)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldMain,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { pct },
                    color = IncomeGreen,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(6.dp))
                )
                if (remaining > 0L) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "المتبقي للهدف: ${MoneyFormat.formatDzd(remaining, privacy)} • مساهمة العائلي ${MoneyFormat.formatDzd(goal.fromHomeCents, privacy)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { actionGoal = goal; isWithdraw = false; goalAmtText = "" },
                        modifier = Modifier.weight(1f)
                    ) { Text("إيداع") }
                    OutlinedButton(
                        onClick = { actionGoal = goal; isWithdraw = true; goalAmtText = "" },
                        modifier = Modifier.weight(1f)
                    ) { Text("سحب") }
                    if (state.goals.size > 1 && goal.savedCents == 0L) {
                        IconButton(onClick = { viewModel.deleteGoal(goal) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                        }
                    }
                }
            }
        }

        // Seasonal Occasions Budgets
        item {
            SandooqiCard {
                SectionHeader(
                    title = "ميزانيات المواسم والأعياد",
                    subtitle = "خطط مسبقاً لرمضان، الأعياد والدخول المدرسي",
                    icon = Icons.Rounded.Celebration
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CatalogDefaults.seasonalOccasions.forEach { (name, emoji) ->
                        FilterChip(
                            selected = seasonName == name,
                            onClick = { seasonName = name; seasonEmoji = emoji },
                            label = { Text("$emoji $name") }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = seasonTarget,
                        onValueChange = { seasonTarget = it },
                        label = { Text("المبلغ المستهدف (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = seasonYm,
                        onValueChange = { seasonYm = it },
                        label = { Text("الشهر (YYYY-MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val c = MoneyFormat.parseToCents(seasonTarget) ?: 0L
                        viewModel.addSeasonalBudget(seasonName, seasonEmoji, c, seasonYm)
                        if (c > 0L) seasonTarget = ""
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("إضافة ميزانية موسمية") }
            }
        }

        items(state.seasonalBudgets, key = { it.id }) { s ->
            val pct = if (s.targetCents > 0L) min(1f, s.savedCents.toFloat() / s.targetCents.toFloat()) else 0f
            SandooqiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${s.emoji} ${s.name} (${DateUtils.formatYM(s.targetYM)})", style = MaterialTheme.typography.titleSmall)
                        Text("${MoneyFormat.formatDzd(s.savedCents, privacy)} / ${MoneyFormat.formatDzd(s.targetCents, privacy)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        FilledTonalButton(onClick = { fundingSeason = s; seasonFundAmt = "" }) { Text("تخصيص") }
                        IconButton(onClick = { viewModel.deleteSeasonalBudget(s.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { pct },
                    color = GoldAccent,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }

    if (actionGoal != null) {
        val g = actionGoal!!
        AlertDialog(
            onDismissRequest = { actionGoal = null },
            title = { Text(if (isWithdraw) "سحب من ${g.name}" else "إيداع في ${g.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = goalAmtText,
                        onValueChange = { goalAmtText = it },
                        label = { Text("المبلغ (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Text(if (isWithdraw) "وجهة السحب:" else "مصدر الإيداع:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("family" to "العائلي", "sewing" to "الخياطة", "external" to "خارجي").forEach { (id, lbl) ->
                            FilterChip(selected = goalAccount == id, onClick = { goalAccount = id }, label = { Text(lbl) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val cents = MoneyFormat.parseToCents(goalAmtText) ?: 0L
                    if (isWithdraw) viewModel.withdrawFromGoal(g, cents, goalAccount)
                    else viewModel.depositToGoal(g, cents, goalAccount)
                    actionGoal = null
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { actionGoal = null }) { Text("إلغاء") } }
        )
    }

    if (fundingSeason != null) {
        val s = fundingSeason!!
        AlertDialog(
            onDismissRequest = { fundingSeason = null },
            title = { Text("تخصيص مبلغ لـ ${s.name}") },
            text = {
                OutlinedTextField(
                    value = seasonFundAmt,
                    onValueChange = { seasonFundAmt = it },
                    label = { Text("المبلغ المضاف (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = {
                Button(onClick = {
                    val cents = MoneyFormat.parseToCents(seasonFundAmt) ?: 0L
                    viewModel.fundSeasonalBudget(s, cents)
                    fundingSeason = null
                }) { Text("تخصيص") }
            },
            dismissButton = { TextButton(onClick = { fundingSeason = null }) { Text("إلغاء") } }
        )
    }
}
