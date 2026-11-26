package com.example.ui.screens.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
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
import com.example.ui.components.StatTile
import com.example.ui.theme.*
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel
import kotlin.math.roundToInt

fun LazyListScope.toolsAnalyticsTab(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel,
    privacy: Boolean,
    pcProduct: String,
    onPcProductChange: (String) -> Unit,
    pcStore: String,
    onPcStoreChange: (String) -> Unit,
    pcPrice: String,
    onPcPriceChange: (String) -> Unit
) {
    item {
        val rep = state.currentReport
        val combinedCash = rep.family.balanceCents + rep.personal.balanceCents + rep.sewing.balanceCents
        val savingRatePct = if (rep.combinedIncomeCents > 0L) {
            ((state.totalSavingsCents.coerceAtMost(rep.combinedIncomeCents) * 100.0) / rep.combinedIncomeCents).roundToInt()
        } else 0
        SandooqiCard {
            SectionHeader(
                title = "تحليلات الأداء المالي",
                subtitle = "نظرة مركزة على السيولة والالتزامات والادخار",
                icon = Icons.Rounded.Analytics
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    label = "إجمالي السيولة",
                    value = MoneyFormat.formatDzd(combinedCash, privacy),
                    accentColor = EmeraldMain,
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "التزامات مفتوحة",
                    value = MoneyFormat.formatDzd(state.openHomeAndBillDebtsCents + state.unpaidObligationsCents, privacy),
                    accentColor = ExpenseRed,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    label = "إجمالي المدخرات",
                    value = MoneyFormat.formatDzd(state.totalSavingsCents, privacy),
                    accentColor = IncomeGreen,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "مؤشر الادخار",
                    value = "$savingRatePct%",
                    accentColor = SkyBlue,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    item {
        SandooqiCard {
            SectionHeader(
                title = "دليل مقارنة الأسعار",
                subtitle = "سجّل وقارن أسعار المحلات للوصول لأفضل سعر",
                icon = Icons.Rounded.PriceCheck
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = pcProduct,
                    onValueChange = onPcProductChange,
                    label = { Text("السلعة") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("price_check_product_input")
                )
                OutlinedTextField(
                    value = pcStore,
                    onValueChange = onPcStoreChange,
                    label = { Text("المحل") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("price_check_store_input")
                )
                OutlinedTextField(
                    value = pcPrice,
                    onValueChange = onPcPriceChange,
                    label = { Text("السعر") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(0.8f).testTag("price_check_price_input")
                )
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = {
                    val c = MoneyFormat.parseToCents(pcPrice) ?: 0L
                    viewModel.addPriceCheck(pcProduct, pcStore, c)
                    if (pcProduct.isNotBlank() && pcStore.isNotBlank() && c > 0L) {
                        onPcProductChange("")
                        onPcStoreChange("")
                        onPcPriceChange("")
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add_price_check_button")
            ) { Text("تسجيل سعر للمقارنة") }
            if (state.priceChecks.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                state.priceChecks.take(10).forEach { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${p.product} (${p.store})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(p.dateISO, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(MoneyFormat.formatDzd(p.unitPriceCents, privacy), style = MaterialTheme.typography.labelLarge, color = EmeraldMain)
                        IconButton(onClick = { viewModel.deletePriceCheck(p.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف السعر")
                        }
                    }
                }
            }
        }
    }
}
