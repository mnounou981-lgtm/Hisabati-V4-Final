package com.example.ui.screens.sewing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

fun LazyListScope.sewingWorkshopItems(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel
) {
    item {
        WorkshopStockAndProjectsSection(state = state, viewModel = viewModel)
    }
}

@Composable
private fun WorkshopStockAndProjectsSection(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel
) {
    val privacy = state.settings.privacyMode

    // Stock form
    var matName by remember { mutableStateOf("") }
    var matUnit by remember { mutableStateOf("قطعة") }
    var matQty by remember { mutableStateOf("10") }
    var matMin by remember { mutableStateOf("2") }
    var matUnitCost by remember { mutableStateOf("") }
    var deductTreasury by remember { mutableStateOf(true) }

    // Own Project form (مشاريعي الخاصة)
    var prjName by remember { mutableStateOf("") }
    var prjMeters by remember { mutableStateOf("") }
    var prjFabRate by remember { mutableStateOf("") }
    var prjSupplies by remember { mutableStateOf("") }
    var prjOther by remember { mutableStateOf("") }
    var prjSale by remember { mutableStateOf("") }
    var prjPaid by remember { mutableStateOf("") }
    var prjStatus by remember { mutableStateOf("work") }

    val mVal = MoneyFormat.normalizeDigits(prjMeters).replace(",", ".").toDoubleOrNull() ?: 0.0
    val rVal = MoneyFormat.parseToCents(prjFabRate) ?: 0L
    val fabTotalCents = Math.round(mVal * rVal)
    val totalPrjCost = fabTotalCents + (MoneyFormat.parseToCents(prjSupplies) ?: 0L) + (MoneyFormat.parseToCents(prjOther) ?: 0L)
    val saleCents = MoneyFormat.parseToCents(prjSale) ?: 0L
    val expectedPrjProfit = saleCents - totalPrjCost

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Materials Stock
        SandooqiCard {
            SectionHeader(
                title = "مخزون الخيوط واللوازم",
                subtitle = "${state.sewMaterials.size} مادة • ${state.lowStockMaterialsCount} منخفضة",
                icon = Icons.Rounded.Inventory2
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = matName, onValueChange = { matName = it }, label = { Text("اسم المادة (خيط، سحاب، أزرار...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("قطعة", "بكرة", "متر", "علبة").forEach { u ->
                    FilterChip(selected = matUnit == u, onClick = { matUnit = u }, label = { Text(u) }, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = matQty, onValueChange = { matQty = it }, label = { Text("الكمية") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = matUnitCost, onValueChange = { matUnitCost = it }, label = { Text("تكلفة الوحدة") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = matMin, onValueChange = { matMin = it }, label = { Text("حد التنبيه") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = deductTreasury, onCheckedChange = { deductTreasury = it })
                Text("خصم التكلفة من خزينة الخياطة", style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    val q = MoneyFormat.normalizeDigits(matQty).replace(",", ".").toDoubleOrNull() ?: 1.0
                    val minQ = MoneyFormat.normalizeDigits(matMin).replace(",", ".").toDoubleOrNull() ?: 2.0
                    val c = MoneyFormat.parseToCents(matUnitCost) ?: 0L
                    viewModel.addSewMaterial(matName, matUnit, q, minQ, c, deductTreasury)
                    if (matName.isNotBlank()) { matName = ""; matUnitCost = "" }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("+ إضافة للمخزون") }

            if (state.sewMaterials.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                state.sewMaterials.forEach { mat ->
                    val low = mat.qty <= mat.minQty
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mat.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${mat.qty} ${mat.unit}" + if (low) " • مخزون منخفض!" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (low) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = { viewModel.consumeSewMaterial(mat, 1.0) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("-1") }
                            Spacer(Modifier.width(4.dp))
                            OutlinedButton(onClick = { viewModel.restockSewMaterial(mat, 1.0) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("+1") }
                            IconButton(onClick = { viewModel.deleteSewMaterial(mat.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                            }
                        }
                    }
                }
            }
        }

        // Own Workshop Projects (مشاريعي الخاصة)
        SandooqiCard {
            SectionHeader(
                title = "مشاريعي الخاصة",
                subtitle = "قطع من تصميمك الخاص وتكلفة تصنيعها وبيعها",
                icon = Icons.Rounded.AutoAwesome
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = prjName, onValueChange = { prjName = it }, label = { Text("اسم المنتج / المشروع") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = prjMeters, onValueChange = { prjMeters = it }, label = { Text("كمية القماش (م)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = prjFabRate, onValueChange = { prjFabRate = it }, label = { Text("ثمن المتر (دج)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = prjSupplies, onValueChange = { prjSupplies = it }, label = { Text("الخيط واللوازم") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = prjOther, onValueChange = { prjOther = it }, label = { Text("مصاريف أخرى") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = prjSale, onValueChange = { prjSale = it }, label = { Text("سعر البيع (دج)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = prjPaid, onValueChange = { prjPaid = it }, label = { Text("المحصّل فعلياً") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("work" to "قيد التصنيع", "ready" to "جاهز للبيع", "sold" to "تم البيع").forEach { (st, lbl) ->
                    FilterChip(selected = prjStatus == st, onClick = { prjStatus = st }, label = { Text(lbl, maxLines = 1) }, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "التكلفة: ${MoneyFormat.formatDzd(totalPrjCost, privacy)} • الربح المتوقع: ${MoneyFormat.formatDzd(expectedPrjProfit, privacy)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (expectedPrjProfit >= 0L) IncomeGreen else ExpenseRed
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val sup = MoneyFormat.parseToCents(prjSupplies) ?: 0L
                    val oth = MoneyFormat.parseToCents(prjOther) ?: 0L
                    val paid = MoneyFormat.parseToCents(prjPaid) ?: 0L
                    viewModel.addSewProject(prjName, mVal, rVal, sup, oth, saleCents, paid, prjStatus)
                    if (prjName.isNotBlank()) {
                        prjName = ""; prjMeters = ""; prjFabRate = ""; prjSupplies = ""; prjOther = ""; prjSale = ""; prjPaid = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("+ تسجيل مشروعي") }

            if (state.sewProjects.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                state.sewProjects.forEach { p ->
                    val cost = p.fabricCostCents + p.suppliesCostCents + p.otherCostCents
                    val profit = p.salePriceCents - cost
                    val statusLbl = when (p.status) {
                        "ready" -> "جاهز للبيع"
                        "sold" -> "تم البيع"
                        else -> "قيد التصنيع"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${p.name} ($statusLbl)", style = MaterialTheme.typography.titleSmall)
                            Text("تكلفة: ${MoneyFormat.formatDzd(cost, privacy)} • بيع: ${MoneyFormat.formatDzd(p.salePriceCents, privacy)} • ربح: ${MoneyFormat.formatDzd(profit, privacy)}", style = MaterialTheme.typography.bodySmall, color = EmeraldMain)
                        }
                        Row {
                            if (p.status != "sold") {
                                TextButton(onClick = { viewModel.updateSewProjectStatus(p, if (p.status == "work") "ready" else "sold") }) {
                                    Text(if (p.status == "work") "جاهز" else "بيع")
                                }
                            }
                            IconButton(onClick = { viewModel.deleteSewProject(p.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                            }
                        }
                    }
                }
            }
        }
    }
}
