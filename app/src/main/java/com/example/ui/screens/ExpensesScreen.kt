package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.CatalogDefaults
import com.example.domain.DateUtils
import com.example.domain.MoneyFormat
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.EmeraldMain
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel
import kotlin.math.min

@Composable
fun ExpensesScreen(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel
) {
    val privacy = state.settings.privacyMode
    var subTab by remember { mutableIntStateOf(0) } // 0 = Expenses, 1 = Market List
    var selectedCatKey by remember { mutableStateOf("veg") }
    var searchProd by remember { mutableStateOf("") }

    var itemName by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("1") }
    var selectedUnit by remember { mutableStateOf("كغ") }
    var unitPriceText by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("خضر وفواكه") }
    var isCredit by remember { mutableStateOf(false) }
    var shopName by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf("") }

    var marketName by remember { mutableStateOf("") }
    var marketQty by remember { mutableStateOf("1") }
    var marketUnit by remember { mutableStateOf("حبة") }
    var marketPrice by remember { mutableStateOf("") }
    var marketBudgetText by remember(state.settings.marketBudgetCents) {
        mutableStateOf(MoneyFormat.formatNumberOnly(state.settings.marketBudgetCents))
    }
    var recentSearch by remember { mutableStateOf("") }

    var pinExitText by remember { mutableStateOf("") }
    var showExitFamilyDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) receiptUri = uri.toString()
    }

    val allProducts = remember(state.settings.customProducts) {
        CatalogDefaults.products + state.settings.customProducts
    }

    val filteredProducts = remember(selectedCatKey, searchProd, allProducts) {
        val q = searchProd.trim()
        allProducts.filter {
            (q.isNotEmpty() && it.name.contains(q)) || (q.isEmpty() && it.categoryKey == selectedCatKey)
        }
    }

    val recentExpenses = remember(state.transactions, state.currentYM, recentSearch) {
        val q = recentSearch.trim().lowercase()
        state.transactions.filter {
            it.ym == state.currentYM &&
                (it.category == "shop" || it.category == "fam" || it.category == "bill") &&
                (q.isEmpty() || it.name.lowercase().contains(q) || it.creditor.lowercase().contains(q) || it.expenseCategory.lowercase().contains(q))
        }.take(40)
    }

    val monthSpent = state.currentReport.family.recordedExpenseCents
    val monthBudget = state.settings.monthlyFamilyBudgetCents
    val progress = if (monthBudget > 0L) min(1f, monthSpent.toFloat() / monthBudget.toFloat()) else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.settings.familyEntryOnlyMode) {
            item {
                SandooqiCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("وضع إدخال العائلة (${state.settings.activeFamilyMember})", style = MaterialTheme.typography.titleSmall)
                            Text("يمكنك تسجيل المشتريات اليومية فقط.", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { showExitFamilyDialog = true }) {
                            Text("خروج")
                        }
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    label = { Text("تسجيل المصاريف") },
                    modifier = Modifier.weight(1f).testTag("expenses_subtab_record")
                )
                FilterChip(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    label = { Text("قائمة السوق (${state.marketItems.count { !it.isDone }})") },
                    modifier = Modifier.weight(1f).testTag("expenses_subtab_market")
                )
            }
        }

        if (subTab == 0) {
            // Unified Monthly Budget Progress Bar
            item {
                SandooqiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ميزانية العائلة الموحدة", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${MoneyFormat.formatDzd(monthSpent, privacy)} / ${MoneyFormat.formatDzd(monthBudget, privacy)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (progress >= 0.9f) ExpenseRed else EmeraldMain
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = if (progress >= 0.9f) ExpenseRed else EmeraldMain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }
            }

            // Quick 1-Tap Expense Chips
            item {
                SectionHeader(title = "مصروف سريع بضغطة واحدة", icon = Icons.Rounded.FlashOn)
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
                            modifier = Modifier.testTag("quick_expense_${preset.id}")
                        )
                    }
                }
            }

            // 6-Category Catalog Grid
            item {
                SandooqiCard {
                    SectionHeader(title = "كتالوج المشتريات", icon = Icons.Rounded.Category)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchProd,
                        onValueChange = { searchProd = it },
                        label = { Text("بحث سريع في المنتجات...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CatalogDefaults.categoryTabs.forEach { (key, label) ->
                            FilterChip(
                                selected = selectedCatKey == key && searchProd.isBlank(),
                                onClick = {
                                    searchProd = ""
                                    selectedCatKey = key
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    filteredProducts.chunked(3).take(5).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowItems.forEach { prod ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (itemName == prod.name) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            itemName = prod.name
                                            selectedUnit = prod.defaultUnit
                                            expenseCategory = prod.categoryLabel
                                        }
                                ) {
                                    Text(
                                        text = "${prod.emoji} ${prod.name}",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                                    )
                                }
                            }
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            // Expense / Credit Entry Form
            item {
                SandooqiCard {
                    SectionHeader(title = "تفاصيل الشراء", icon = Icons.Rounded.ShoppingBag)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("اسم السلعة أو المصروف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("expense_item_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qtyText,
                            onValueChange = { qtyText = it },
                            label = { Text("الكمية") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.9f)
                        )
                        OutlinedTextField(
                            value = unitPriceText,
                            onValueChange = { unitPriceText = it },
                            label = { Text("سعر الوحدة (دج)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.3f).testTag("expense_price_input")
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CatalogDefaults.units.forEach { u ->
                            FilterChip(
                                selected = selectedUnit == u,
                                onClick = { selectedUnit = u },
                                label = { Text(u) }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("التصنيف:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CatalogDefaults.expenseCategories.forEach { cat ->
                            FilterChip(
                                selected = expenseCategory == cat,
                                onClick = { expenseCategory = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isCredit,
                            onClick = { isCredit = false },
                            label = { Text("نقداً (من الخزينة)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isCredit,
                            onClick = { isCredit = true },
                            label = { Text("بالدين (كريدي)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text(if (isCredit) "اسم المحل / البقال (مطلوب للدين)" else "المحل (اختياري لمقارنة الأسعار)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (receiptUri.isBlank()) "إرفاق صورة الوصل" else "تغيير الصورة")
                        }
                        if (receiptUri.isNotBlank()) {
                            AsyncImage(
                                model = receiptUri,
                                contentDescription = "صورة الوصل",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val qty = MoneyFormat.normalizeDigits(qtyText).replace(",", ".").toDoubleOrNull() ?: 1.0
                            val unitCents = MoneyFormat.parseToCents(unitPriceText) ?: 0L
                            if (itemName.isBlank() || unitCents <= 0L) {
                                viewModel.showToast("أدخل اسم السلعة والسعر")
                            } else {
                                viewModel.addGroceryExpense(
                                    name = itemName.trim(),
                                    qty = qty,
                                    unit = selectedUnit,
                                    unitPriceCents = unitCents,
                                    category = expenseCategory,
                                    dateISO = DateUtils.todayISO(),
                                    isCredit = isCredit,
                                    shopOrCreditor = shopName,
                                    photoUri = receiptUri
                                )
                                itemName = ""
                                qtyText = "1"
                                unitPriceText = ""
                                receiptUri = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("save_expense_button")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isCredit) "تسجيل في الديون والميزانية" else "خصم وتسجيل المصروف")
                    }
                }
            }

            // Recent Purchases grouped by day
            item {
                SectionHeader(title = "مشتريات ومصاريف الشهر", icon = Icons.Rounded.Receipt)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = recentSearch,
                    onValueChange = { recentSearch = it },
                    label = { Text("بحث في المشتريات أو المحل...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (recentExpenses.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "لم تسجل أي مصاريف هذا الشهر",
                        description = "استخدم الأزرار السريعة أو الكتالوج لتسجيل مشترياتك.",
                        icon = Icons.Rounded.ShoppingBasket
                    )
                }
            } else {
                val grouped = recentExpenses.groupBy { it.dateISO }
                grouped.forEach { (dateIso, dayList) ->
                    item(key = "day_$dateIso") {
                        Text(
                            text = DateUtils.formatDayHeader(dateIso),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(dayList, key = { it.id }) { tx ->
                        SandooqiCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tx.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = tx.expenseCategory.ifBlank { tx.category } +
                                            if (tx.isCreditPurchase) " • دين" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (tx.isCreditPurchase) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        MoneyFormat.formatDzd(tx.amountCents, privacy),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                    IconButton(onClick = { viewModel.deleteTransaction(tx) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Market Shopping List Sub-tab
            val estimatedTotal = state.marketItems.filter { !it.isDone }.sumOf {
                Math.round(it.quantity * it.unitPriceCents)
            }
            item {
                SandooqiCard {
                    SectionHeader(
                        title = "قائمة السوق والمقاضي",
                        subtitle = "التكلفة المقدرة للمتبقي: ${MoneyFormat.formatDzd(estimatedTotal, privacy)}",
                        icon = Icons.Rounded.Checklist
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = marketName,
                        onValueChange = { marketName = it },
                        label = { Text("غرض للشراء") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = marketQty,
                            onValueChange = { marketQty = it },
                            label = { Text("الكمية") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = marketPrice,
                            onValueChange = { marketPrice = it },
                            label = { Text("سعر تقديري (دج)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CatalogDefaults.units.forEach { u ->
                            FilterChip(selected = marketUnit == u, onClick = { marketUnit = u }, label = { Text(u) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val q = MoneyFormat.normalizeDigits(marketQty).replace(",", ".").toDoubleOrNull() ?: 1.0
                                val p = MoneyFormat.parseToCents(marketPrice) ?: 0L
                                viewModel.addMarketItem(marketName, q, marketUnit, p)
                                marketName = ""
                                marketPrice = ""
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("إضافة للقائمة") }
                        OutlinedButton(
                            onClick = { viewModel.clearDoneMarket() },
                            modifier = Modifier.weight(1f)
                        ) { Text("مسح المكتمل") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = marketBudgetText,
                            onValueChange = { marketBudgetText = it },
                            label = { Text("سقف السوق (دج)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(onClick = {
                            val b = MoneyFormat.parseToCents(marketBudgetText) ?: 0L
                            viewModel.setMarketBudget(b)
                            viewModel.showToast("تم تحديث سقف قائمة السوق")
                        }) { Text("حفظ السقف") }
                    }
                }
            }
            items(state.marketItems, key = { it.id }) { item ->
                SandooqiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.toggleMarketItem(item) }
                        ) {
                            Checkbox(checked = item.isDone, onCheckedChange = { viewModel.toggleMarketItem(item) })
                            Column {
                                Text(item.name, style = MaterialTheme.typography.titleSmall)
                                Text("${item.quantity} ${item.unit}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteMarketItem(item.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                        }
                    }
                }
            }
        }
    }

    if (showExitFamilyDialog) {
        AlertDialog(
            onDismissRequest = { showExitFamilyDialog = false },
            title = { Text("الخروج من وضع إدخال العائلة") },
            text = {
                OutlinedTextField(
                    value = pinExitText,
                    onValueChange = { pinExitText = it },
                    label = { Text("أدخل الرمز السري") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (viewModel.exitFamilyEntryMode(pinExitText)) {
                        showExitFamilyDialog = false
                        pinExitText = ""
                    }
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { showExitFamilyDialog = false }) { Text("إلغاء") } }
        )
    }
}
