package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.DateUtils
import com.example.domain.MoneyFormat
import com.example.ui.components.MoneyText
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainNavTab
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel
import kotlin.math.max
import kotlin.math.min

@Composable
fun HomeScreen(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel
) {
    val privacy = state.settings.privacyMode
    val rep = state.currentReport
    val dayOfMonth = max(1, DateUtils.dayOfMonth())
    val daysInMonth = DateUtils.daysInMonth(state.currentYM)
    val remainingDays = max(1, daysInMonth - dayOfMonth + 1)
    val spentSoFar = rep.family.recordedExpenseCents
    val dailyAvg = spentSoFar / dayOfMonth
    val runwayDays = if (dailyAvg > 0L && rep.family.balanceCents > 0L) {
        (rep.family.balanceCents / dailyAvg).toInt()
    } else if (rep.family.balanceCents > 0L) 30 else 0

    val overdueCreditors = state.creditorGroups.count { it.hasOverdue }
    val memberExpenses = state.settings.familyMembers.associateWith { member ->
        state.transactions
            .filter { it.ym == state.currentYM && it.category == "fam" && it.personName == member }
            .sumOf { it.amountCents }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Expected Balance Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SandooqiPalettes.heroBrush(state.settings.themeStyle, state.settings.darkMode))
                    .testTag("home_hero_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الصافي المتوقع للخزينة العائلية",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GoldBadgeBg
                        ) {
                            Text(
                                text = DateUtils.formatYM(state.currentYM),
                                color = GoldInk,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    MoneyText(
                        cents = state.expectedFamilyNetCents,
                        privacyMode = privacy,
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroMiniPill("السيولة الحالية", MoneyFormat.formatDzd(rep.family.balanceCents, privacy), Modifier.weight(1f))
                        HeroMiniPill("التزامات وديون", MoneyFormat.formatDzd(state.openHomeAndBillDebtsCents + state.unpaidObligationsCents, privacy), Modifier.weight(1f))
                        HeroMiniPill("كراء مستحق لك", MoneyFormat.formatDzd(state.openRentReceivablesCents, privacy), Modifier.weight(1f))
                    }
                }
            }
        }

        // Runway Meter
        item {
            val budget = state.settings.monthlyFamilyBudgetCents
            val budgetProgress = if (budget > 0L) min(1f, spentSoFar.toFloat() / budget.toFloat()) else 0f
            SandooqiCard {
                SectionHeader(
                    title = "مؤشر الكفاية والميزانية الشهرية",
                    subtitle = "المتبقي من الشهر: $remainingDays يوم • تكفي السيولة قرابة $runwayDays يوم",
                    icon = Icons.Rounded.Speed
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { budgetProgress },
                    color = if (budgetProgress >= 0.9f) ExpenseRed else EmeraldMain,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "المصروف: ${MoneyFormat.formatDzd(spentSoFar, privacy)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "السقف الموحد: ${MoneyFormat.formatDzd(budget, privacy)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Smart Advisor & Alerts
        item {
            SandooqiCard {
                SectionHeader(
                    title = "المستشار والتنبيهات الذكية",
                    subtitle = "تحليل فوري لوضعك المالي والورشة",
                    icon = Icons.Rounded.Lightbulb
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (overdueCreditors > 0) {
                        AdvisorAlertRow(
                            text = "لديك $overdueCreditors ديون متأخرة السداد تستحق المراجعة.",
                            color = ExpenseRed,
                            onClick = { viewModel.selectTab(MainNavTab.DEBTS) }
                        )
                    }
                    state.billSpikes.take(2).forEach { spike ->
                        AdvisorAlertRow(
                            text = "ارتفاع غير معتاد في ${spike.name} بنسبة +${spike.increasePct}%.",
                            color = GoldAccent,
                            onClick = { viewModel.selectTab(MainNavTab.DEBTS) }
                        )
                    }
                    if (state.dueTodayOrdersCount > 0) {
                        AdvisorAlertRow(
                            text = "يوجد ${state.dueTodayOrdersCount} طلبيات خياطة موعد تسليمها اليوم.",
                            color = SewingViolet,
                            onClick = { viewModel.selectTab(MainNavTab.SEWING) }
                        )
                    }
                    if (state.lowStockMaterialsCount > 0) {
                        AdvisorAlertRow(
                            text = "${state.lowStockMaterialsCount} مواد في مخزون الخياطة وصلت للحد الأدنى.",
                            color = GoldAccent,
                            onClick = { viewModel.selectTab(MainNavTab.SEWING) }
                        )
                    }
                    if (overdueCreditors == 0 && state.billSpikes.isEmpty() && state.dueTodayOrdersCount == 0 && state.lowStockMaterialsCount == 0) {
                        AdvisorAlertRow(
                            text = "الوضع المالي مستقر! استمر في تخصيص نسبة من الفائض للادخار.",
                            color = IncomeGreen,
                            onClick = { viewModel.selectTab(MainNavTab.BUDGET) }
                        )
                    }
                }
            }
        }

        // 3 Accounts Quick Tiles
        item {
            SectionHeader(title = "أرصدة الحسابات الثلاثة", icon = Icons.Rounded.AccountBalanceWallet)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(
                    label = "العائلي",
                    value = MoneyFormat.formatDzd(rep.family.balanceCents, privacy),
                    accentColor = EmeraldMain,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.weight(1f).clickable { viewModel.selectTab(MainNavTab.TREASURY) }
                )
                StatTile(
                    label = "الشخصي",
                    value = MoneyFormat.formatDzd(rep.personal.balanceCents, privacy),
                    accentColor = SkyBlue,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f).clickable { viewModel.selectTab(MainNavTab.TREASURY) }
                )
                StatTile(
                    label = "الخياطة",
                    value = MoneyFormat.formatDzd(rep.sewing.balanceCents, privacy),
                    accentColor = SewingViolet,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f).clickable { viewModel.selectTab(MainNavTab.SEWING) }
                )
            }
        }

        // Quick Action Shortcuts + Customizable 1-Tap Quick Expenses
        item {
            SandooqiCard {
                SectionHeader(title = "اختصارات سريعة", icon = Icons.Rounded.Bolt)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShortcutChip("مصروف", Icons.Rounded.ShoppingCart, Modifier.weight(1f), "shortcut_expense") {
                        viewModel.selectTab(MainNavTab.EXPENSES)
                    }
                    ShortcutChip("دين / فاتورة", Icons.Rounded.ReceiptLong, Modifier.weight(1f), "shortcut_debt") {
                        viewModel.selectTab(MainNavTab.DEBTS)
                    }
                    ShortcutChip("خياطة", Icons.Rounded.ContentCut, Modifier.weight(1f), "shortcut_sewing") {
                        viewModel.selectTab(MainNavTab.SEWING)
                    }
                    ShortcutChip("صوت", Icons.Rounded.Mic, Modifier.weight(1f), "shortcut_voice") {
                        viewModel.setVoiceDialog(true)
                    }
                }
                if (state.settings.quickExpenses.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "المصروف السريع بضغطة واحدة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.settings.quickExpenses.forEach { preset ->
                            AssistChip(
                                onClick = { viewModel.runQuickExpense(preset) },
                                label = {
                                    Text("${preset.icon} ${preset.name} (${MoneyFormat.formatNumberOnly(preset.amountCents)} دج)")
                                },
                                modifier = Modifier.testTag("home_quick_expense_${preset.id}")
                            )
                        }
                    }
                }
            }
        }

        // Family Members Summary
        item {
            SandooqiCard {
                SectionHeader(
                    title = "مصاريف أفراد العائلة",
                    subtitle = "مجموع مصاريف الشهر لكل فرد",
                    icon = Icons.Rounded.Groups
                )
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    memberExpenses.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (name, total) ->
                                StatTile(
                                    label = name,
                                    value = MoneyFormat.formatDzd(total, privacy),
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f).clickable { viewModel.selectTab(MainNavTab.DEBTS) }
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMiniPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun AdvisorAlertRow(text: String, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ShortcutChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        }
    }
}
