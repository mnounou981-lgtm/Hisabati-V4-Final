package com.example.ui.screens.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
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
import com.example.domain.MoneyFormat
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.EmeraldMain
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel

fun LazyListScope.toolsSimulatorTab(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel,
    privacy: Boolean,
    simItem: String,
    onSimItemChange: (String) -> Unit,
    simPrice: String,
    onSimPriceChange: (String) -> Unit,
    simAccount: String,
    onSimAccountChange: (String) -> Unit,
    quickIcon: String,
    onQuickIconChange: (String) -> Unit,
    quickName: String,
    onQuickNameChange: (String) -> Unit,
    quickAmount: String,
    onQuickAmountChange: (String) -> Unit
) {
    item {
        val availableCents = when (simAccount) {
            "personal" -> state.currentReport.personal.balanceCents
            "sewing" -> state.currentReport.sewing.balanceCents
            "all" -> state.expectedFamilyNetCents +
                state.currentReport.personal.balanceCents +
                state.currentReport.sewing.balanceCents
            else -> state.expectedFamilyNetCents
        }
        val simCents = MoneyFormat.parseToCents(simPrice) ?: 0L
        val afterCents = availableCents - simCents
        val canAfford = simCents > 0L && afterCents >= 0L

        SandooqiCard {
            SectionHeader(
                title = "محاكي: هل أقدر أشتريه الآن؟",
                subtitle = "اختبر الشراء حسب الصندوق الذي تريد الدفع منه",
                icon = Icons.Rounded.Calculate
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = simItem,
                onValueChange = onSimItemChange,
                label = { Text("الشيء الذي تفكر في شرائه...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("simulator_item_input")
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = simPrice,
                onValueChange = onSimPriceChange,
                label = { Text("السعر المتوقع (دج)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().testTag("simulator_price_input")
            )
            Spacer(Modifier.height(8.dp))
            Text("الصندوق المستهدف:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "family" to "صندوق البيت فقط",
                    "personal" to "الحساب الشخصي",
                    "sewing" to "حساب الخياطة",
                    "all" to "جميع الصناديق"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = simAccount == key,
                        onClick = { onSimAccountChange(key) },
                        label = { Text(label, maxLines = 1) },
                        modifier = Modifier.testTag("sim_account_$key")
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "الرصيد المتاح حالياً: ${MoneyFormat.formatDzd(availableCents, privacy)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (simCents > 0L) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = (if (canAfford) IncomeGreen else ExpenseRed).copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (canAfford) {
                            "نعم، يمكنك شراء ${simItem.ifBlank { "هذا الغرض" }}! سيبقى لديك: ${MoneyFormat.formatDzd(afterCents, privacy)}"
                        } else {
                            "لا يُنصح حالياً! ينقصك: ${MoneyFormat.formatDzd(-afterCents, privacy)}"
                        },
                        color = if (canAfford) EmeraldMain else ExpenseRed,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    item {
        SandooqiCard {
            SectionHeader(
                title = "تخصيص أزرار المصروف السريع (في الرئيسية)",
                subtitle = "أضف أو احذف أزرار المصاريف المتكررة بضغطة واحدة",
                icon = Icons.Rounded.Bolt
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("🛍️", "🥖", "🥛", "☕", "🚕", "🥦", "💊", "⛽").forEach { emoji ->
                    FilterChip(
                        selected = quickIcon == emoji,
                        onClick = { onQuickIconChange(emoji) },
                        label = { Text(emoji) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quickIcon,
                    onValueChange = { if (it.length <= 4) onQuickIconChange(it) },
                    label = { Text("الأيقونة") },
                    singleLine = true,
                    modifier = Modifier.weight(0.55f)
                )
                OutlinedTextField(
                    value = quickName,
                    onValueChange = onQuickNameChange,
                    label = { Text("اسم المصروف السريع") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f).testTag("quick_preset_name_input")
                )
                OutlinedTextField(
                    value = quickAmount,
                    onValueChange = onQuickAmountChange,
                    label = { Text("المبلغ (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(0.95f).testTag("quick_preset_amount_input")
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val amt = MoneyFormat.parseToCents(quickAmount) ?: 0L
                    viewModel.addQuickExpensePreset(quickIcon, quickName, amt)
                    if (quickName.isNotBlank() && amt > 0L) {
                        onQuickNameChange("")
                        onQuickAmountChange("")
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add_quick_preset_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("إضافة زر سريع")
            }
            if (state.settings.quickExpenses.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                state.settings.quickExpenses.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${preset.icon} ${preset.name} (${MoneyFormat.formatNumberOnly(preset.amountCents)} دج)",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "حذف ${preset.name}",
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { viewModel.deleteQuickExpensePreset(preset.id) }
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
