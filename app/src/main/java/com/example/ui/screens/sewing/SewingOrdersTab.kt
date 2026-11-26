package com.example.ui.screens.sewing

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.CustomerMeasurementEntity
import com.example.data.local.TailoringOrderEntity
import com.example.domain.CatalogDefaults
import com.example.domain.DateUtils
import com.example.domain.LedgerCalculator
import com.example.domain.MoneyFormat
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel
import kotlin.math.max
import kotlin.math.roundToLong

data class DraftFabricLine(
    val source: String, // "customer" or "workshop"
    val name: String,
    val meters: Double,
    val rateCents: Long,
    val laborRateCents: Long,
    val buyRateCents: Long
) {
    val lineLaborCents: Long get() = Math.round(meters * laborRateCents)
    val customerFabricPriceCents: Long get() = if (source == "workshop") Math.round(meters * rateCents) else 0L
    val workshopBuyCostCents: Long get() = if (source == "workshop") Math.round(meters * buyRateCents) else 0L
}

data class DraftSupplyLine(
    val name: String,
    val costCents: Long,
    val stockId: Long? = null,
    val stockQty: Double = 0.0
)

fun LazyListScope.sewingOrdersItems(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel,
    showOrderForm: Boolean,
    onToggleOrderForm: (Boolean) -> Unit,
    prefillClient: String,
    prefillPhone: String,
    prefillMeasSummary: String,
    onClearPrefill: () -> Unit,
    onPayOrder: (TailoringOrderEntity) -> Unit,
    onCancelOrder: (TailoringOrderEntity) -> Unit
) {
    item {
        SandooqiCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("دفتر الطلبيات", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${state.workshopSummary.openOrdersCount} جارية من أصل ${state.workshopSummary.totalOrdersCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onToggleOrderForm(!showOrderForm) },
                    modifier = Modifier.testTag("sew_new_order_toggle")
                ) {
                    Icon(if (showOrderForm) Icons.Rounded.Close else Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (showOrderForm) "عرض الدفتر" else "+ طلبية جديدة")
                }
            }
        }
    }

    if (showOrderForm) {
        item {
            NewOrderBuilderCard(
                state = state,
                viewModel = viewModel,
                prefillClient = prefillClient,
                prefillPhone = prefillPhone,
                prefillMeasSummary = prefillMeasSummary,
                onSaved = {
                    onClearPrefill()
                    onToggleOrderForm(false)
                }
            )
        }
        return
    }

    item {
        OrderListControls(state = state, viewModel = viewModel, onPayOrder = onPayOrder, onCancelOrder = onCancelOrder)
    }
}

@Composable
private fun OrderListControls(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel,
    onPayOrder: (TailoringOrderEntity) -> Unit,
    onCancelOrder: (TailoringOrderEntity) -> Unit
) {
    val context = LocalContext.current
    val privacy = state.settings.privacyMode
    val today = DateUtils.todayISO()
    val prepSteps = listOf("قص", "سرفلة", "خياطة", "كيّ وتغليف")

    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") } // all, open, today, done, cancelled, late
    var sortBy by remember { mutableStateOf("due") } // due, new, old

    val filteredOrders = remember(state.orders, search, statusFilter, sortBy, today) {
        val q = search.trim().lowercase()
        state.orders.filter { o ->
            val matchQ = q.isEmpty() ||
                o.client.lowercase().contains(q) ||
                o.item.lowercase().contains(q) ||
                o.phone.contains(q) ||
                o.id.toString() == q
            val matchStatus = when (statusFilter) {
                "open" -> !o.isCancelled && !o.isDone
                "today" -> !o.isCancelled && !o.isDone && o.dueDateISO == today
                "done" -> !o.isCancelled && o.isDone
                "cancelled" -> o.isCancelled
                "late" -> !o.isCancelled && !o.isDone && o.dueDateISO.isNotBlank() && o.dueDateISO < today
                else -> true
            }
            matchQ && matchStatus
        }.let { list ->
            when (sortBy) {
                "new" -> list.sortedByDescending { it.id }
                "old" -> list.sortedBy { it.id }
                else -> list.sortedWith(compareBy<TailoringOrderEntity> { it.isCancelled }.thenBy { it.isDone }.thenBy { it.dueDateISO.ifBlank { "9999-99-99" } })
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SandooqiCard {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("البحث: الاسم، الهاتف، الموديل أو رقم الطلبية") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("sew_order_search_input")
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "all" to "الكل",
                    "open" to "الجارية",
                    "today" to "تسليم اليوم",
                    "late" to "المتأخرة",
                    "done" to "المسلّمة",
                    "cancelled" to "الملغاة"
                ).forEach { (k, lbl) ->
                    FilterChip(selected = statusFilter == k, onClick = { statusFilter = k }, label = { Text(lbl) })
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("due" to "الأقرب تسليماً", "new" to "الأحدث", "old" to "الأقدم").forEach { (k, lbl) ->
                    FilterChip(
                        selected = sortBy == k,
                        onClick = { sortBy = k },
                        label = { Text(lbl, maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (filteredOrders.isEmpty()) {
            EmptyStateCard(
                title = "لا توجد طلبيات مطابقة",
                description = "اضغط على (+ طلبية جديدة) لتسجيل أول طلبية بكل تفاصيل القماش والمقاسات.",
                icon = Icons.Rounded.ContentCut
            )
        } else {
            filteredOrders.forEach { order ->
                val rem = LedgerCalculator.orderRemainingCents(order)
                val isLate = !order.isCancelled && !order.isDone && order.dueDateISO.isNotBlank() && order.dueDateISO < today
                SandooqiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("#${order.id} • ${order.client} — ${order.item}", style = MaterialTheme.typography.titleMedium)
                            if (order.phone.isNotBlank()) {
                                Text("هاتف: ${order.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "التسليم: ${order.dueDateISO.ifBlank { "غير محدد" }}" +
                                    (if (order.fittingDateISO.isNotBlank()) " • القياس: ${order.fittingDateISO}" else "") +
                                    (if (isLate) " (متأخرة!)" else ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLate) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (order.photoUri.isNotBlank()) {
                            AsyncImage(
                                model = order.photoUri,
                                contentDescription = "صورة الموديل",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (order.isCancelled) RedSoft else SewingVioletSoft
                        ) {
                            Text(
                                text = if (order.isCancelled) "ملغاة" else order.stage,
                                color = if (order.isCancelled) ExpenseRed else SewingVioletDark,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "الإجمالي: ${MoneyFormat.formatDzd(order.totalCents, privacy)} • المدفوع: ${MoneyFormat.formatDzd(order.paidCents - order.refundedCents, privacy)} • المتبقي: ${MoneyFormat.formatDzd(rem, privacy)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (rem > 0L && !order.isCancelled) GoldAccent else IncomeGreen
                    )
                    if (order.fabricsSummary.isNotBlank()) {
                        Text("الأقمشة: ${order.fabricsSummary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (order.suppliesSummary.isNotBlank()) {
                        Text("اللوازم: ${order.suppliesSummary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (order.measurementsSummary.isNotBlank()) {
                        Text("المقاسات: ${order.measurementsSummary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (!order.isCancelled) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CatalogDefaults.sewingStages.forEach { st ->
                                FilterChip(
                                    selected = order.stage == st,
                                    onClick = { viewModel.updateOrderStage(order, st) },
                                    label = { Text(st, maxLines = 1) }
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            prepSteps.forEachIndexed { idx, stepLabel ->
                                val checked = (order.checklistMask and (1 shl idx)) != 0
                                FilterChip(
                                    selected = checked,
                                    onClick = { viewModel.toggleOrderChecklist(order, idx) },
                                    label = { Text(stepLabel, maxLines = 1) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (rem > 0L) {
                                Button(
                                    onClick = { onPayOrder(order) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("دفعة") }
                            }
                            OutlinedButton(
                                onClick = {
                                    val msg = "السلام عليكم ${order.client}، طلبيتك (${order.item}) جاهزة للتسليم. المتبقي: ${MoneyFormat.formatDzd(rem)}."
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, msg)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "إشعار جاهزية الطلبية"))
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("جاهزة") }
                            OutlinedButton(
                                onClick = {
                                    val receipt = buildString {
                                        appendLine("وصل طلبية خياطة #${order.id} — صندوقي")
                                        appendLine("الزبونة: ${order.client}")
                                        appendLine("الموديل: ${order.item}")
                                        appendLine("الحالة: ${order.stage}")
                                        appendLine("الإجمالي: ${MoneyFormat.formatDzd(order.totalCents)}")
                                        appendLine("المدفوع: ${MoneyFormat.formatDzd(order.paidCents - order.refundedCents)}")
                                        appendLine("المتبقي: ${MoneyFormat.formatDzd(rem)}")
                                        if (order.dueDateISO.isNotBlank()) appendLine("موعد التسليم: ${order.dueDateISO}")
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, receipt)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة وصل الطلبية"))
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("وصل") }
                            IconButton(onClick = { onCancelOrder(order) }) {
                                Icon(Icons.Rounded.Cancel, contentDescription = "إلغاء الطلبية", tint = ExpenseRed)
                            }
                            IconButton(onClick = { viewModel.deleteTailoringOrder(order) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف الطلبية")
                            }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.deleteTailoringOrder(order) }) { Text("حذف من الدفتر") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewOrderBuilderCard(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel,
    prefillClient: String,
    prefillPhone: String,
    prefillMeasSummary: String,
    onSaved: () -> Unit
) {
    var client by remember(prefillClient) { mutableStateOf(prefillClient) }
    var phone by remember(prefillPhone) { mutableStateOf(prefillPhone) }
    var item by remember { mutableStateOf("") }
    var fittingISO by remember { mutableStateOf("") }
    var dueISO by remember { mutableStateOf(DateUtils.todayISO()) }
    var photoUri by remember { mutableStateOf("") }

    var orderMeterRateText by remember { mutableStateOf(MoneyFormat.formatNumberOnly(state.settings.meterPriceCents)) }
    var flatLaborText by remember { mutableStateOf("0") }
    var adjustmentText by remember { mutableStateOf("0") }
    var depositText by remember { mutableStateOf("0") }

    // Fabrics editor
    var showFabrics by remember { mutableStateOf(false) }
    var fabSource by remember { mutableStateOf("customer") }
    var fabName by remember { mutableStateOf("") }
    var fabMetersText by remember { mutableStateOf("") }
    var fabRateText by remember { mutableStateOf("") }
    var fabLaborRateText by remember { mutableStateOf("") }
    var fabBuyRateText by remember { mutableStateOf("") }
    val fabricLines = remember { mutableStateListOf<DraftFabricLine>() }

    // Supplies & stock
    var showSupplies by remember { mutableStateOf(false) }
    var supName by remember { mutableStateOf("خيط") }
    var supCostText by remember { mutableStateOf("") }
    val supplyLines = remember { mutableStateListOf<DraftSupplyLine>() }

    // Measurements
    var showMeasurements by remember { mutableStateOf(prefillMeasSummary.isNotBlank()) }
    var mLen by remember { mutableStateOf("") }
    var mSh by remember { mutableStateOf("") }
    var mChest by remember { mutableStateOf("") }
    var mWaist by remember { mutableStateOf("") }
    var mHip by remember { mutableStateOf("") }
    var mSleeve by remember { mutableStateOf("") }
    var mNote by remember { mutableStateOf(prefillMeasSummary) }

    // Pricing Estimator
    var showEstimator by remember { mutableStateOf(false) }
    var estFabText by remember { mutableStateOf("0") }
    var estHoursText by remember { mutableStateOf("0") }
    var estLaborHourText by remember { mutableStateOf("0") }
    var estExtraText by remember { mutableStateOf("0") }
    var estProfitPctText by remember { mutableStateOf("0") }

    val orderPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) photoUri = uri.toString() }

    val totalMeters = fabricLines.sumOf { it.meters }
    val fabricLaborCents = fabricLines.sumOf { it.lineLaborCents }
    val workshopFabricsPriceCents = fabricLines.sumOf { it.customerFabricPriceCents }
    val workshopFabricsBuyCostCents = fabricLines.sumOf { it.workshopBuyCostCents }
    val suppliesCostCents = supplyLines.sumOf { it.costCents }
    val flatLaborCents = MoneyFormat.parseToCents(flatLaborText) ?: 0L
    val adjustmentCents = MoneyFormat.parseToCents(adjustmentText) ?: 0L
    val grandTotalCents = max(
        0L,
        fabricLaborCents + flatLaborCents + workshopFabricsPriceCents + suppliesCostCents + adjustmentCents
    )

    val estMatCents = if (suppliesCostCents + workshopFabricsBuyCostCents > 0L) {
        suppliesCostCents + workshopFabricsBuyCostCents
    } else {
        MoneyFormat.parseToCents(estFabText) ?: 0L
    }
    val estHours = MoneyFormat.normalizeDigits(estHoursText).replace(",", ".").toDoubleOrNull() ?: 0.0
    val estHourRate = MoneyFormat.parseToCents(estLaborHourText) ?: 0L
    val estExtra = MoneyFormat.parseToCents(estExtraText) ?: 0L
    val estProfitPct = MoneyFormat.normalizeDigits(estProfitPctText).replace(",", ".").toDoubleOrNull() ?: 0.0
    val estBaseCost = estMatCents + Math.round(estHours * estHourRate) + estExtra
    val estSuggestedCents = Math.round(estBaseCost * (1.0 + estProfitPct / 100.0))

    SandooqiCard {
        SectionHeader(title = "تسجيل طلبية جديدة", subtitle = "حساب الأقمشة والأمتار واللوازم والمقاسات", icon = Icons.Rounded.ContentCut)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = client, onValueChange = { client = it }, label = { Text("اسم الزبونة") }, singleLine = true, modifier = Modifier.weight(1.2f).testTag("sewing_client_input"))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("الهاتف") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(value = item, onValueChange = { item = it }, label = { Text("نوع اللباس / الموديل (قندورة، قفطان...)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("sewing_item_input"))
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = fittingISO, onValueChange = { fittingISO = it }, label = { Text("موعد التجربة") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = dueISO, onValueChange = { dueISO = it }, label = { Text("موعد التسليم") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { orderPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (photoUri.isBlank()) "صورة الموديل" else "تغيير الصورة")
            }
            if (photoUri.isNotBlank()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "صورة الموديل",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                )
            }
        }

        // Collapsible Section 1: Fabrics
        Spacer(Modifier.height(8.dp))
        FilledTonalButton(onClick = { showFabrics = !showFabrics }, modifier = Modifier.fillMaxWidth()) {
            Text("أقمشة الطلبية (${fabricLines.size}) — مجموع الأمتار: $totalMeters م")
        }
        if (showFabrics) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = fabSource == "customer", onClick = { fabSource = "customer" }, label = { Text("من الزبونة (معلومة)") }, modifier = Modifier.weight(1f))
                FilterChip(selected = fabSource == "workshop", onClick = { fabSource = "workshop" }, label = { Text("من الخياط (يُحتسب)") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = fabName, onValueChange = { fabName = it }, label = { Text("نوع القماش") }, singleLine = true, modifier = Modifier.weight(1.2f))
                OutlinedTextField(value = fabMetersText, onValueChange = { fabMetersText = it }, label = { Text("الأمتار") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.8f))
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = fabRateText, onValueChange = { fabRateText = it }, label = { Text(if (fabSource == "workshop") "سعر البيع/م" else "قيمة/م (معلومة)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = fabLaborRateText, onValueChange = { fabLaborRateText = it }, label = { Text("أجرة الخياطة/م") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                if (fabSource == "workshop") {
                    OutlinedTextField(value = fabBuyRateText, onValueChange = { fabBuyRateText = it }, label = { Text("سعر الشراء/م") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = {
                    val m = MoneyFormat.normalizeDigits(fabMetersText).replace(",", ".").toDoubleOrNull() ?: 0.0
                    val defaultLabor = MoneyFormat.parseToCents(orderMeterRateText) ?: state.settings.meterPriceCents
                    val labor = MoneyFormat.parseToCents(fabLaborRateText) ?: defaultLabor
                    val rate = MoneyFormat.parseToCents(fabRateText) ?: 0L
                    val buy = MoneyFormat.parseToCents(fabBuyRateText) ?: 0L
                    if (fabName.isNotBlank() && m > 0.0) {
                        fabricLines.add(DraftFabricLine(fabSource, fabName.trim(), m, rate, labor, buy))
                        fabName = ""; fabMetersText = ""; fabRateText = ""; fabLaborRateText = ""; fabBuyRateText = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("+ إضافة القماش للطلبية") }
            fabricLines.forEachIndexed { idx, f ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${f.name} (${f.meters} م) — ${if (f.source == "workshop") "الورشة" else "الزبونة"}", style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { fabricLines.removeAt(idx) }) { Icon(Icons.Rounded.Close, contentDescription = "إزالة") }
                }
            }
        }

        // Collapsible Section 2: Supplies & Stock
        Spacer(Modifier.height(6.dp))
        FilledTonalButton(onClick = { showSupplies = !showSupplies }, modifier = Modifier.fillMaxWidth()) {
            Text("اللوازم ومخزون الورشة (${supplyLines.size})")
        }
        if (showSupplies) {
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("خيط", "سحاب", "أزرار", "دانتيل", "طرز", "بطانة", "مطاط").forEach { s ->
                    FilterChip(selected = supName == s, onClick = { supName = s }, label = { Text(s) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = supName, onValueChange = { supName = it }, label = { Text("المادة") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = supCostText, onValueChange = { supCostText = it }, label = { Text("التكلفة (دج)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.9f))
                FilledTonalButton(onClick = {
                    val c = MoneyFormat.parseToCents(supCostText) ?: 0L
                    if (supName.isNotBlank() && c >= 0L) {
                        supplyLines.add(DraftSupplyLine(supName.trim(), c))
                        supCostText = ""
                    }
                }) { Text("إضافة") }
            }
            if (state.sewMaterials.isNotEmpty()) {
                Text("سحب سريع من مخزون الورشة (-1):", style = MaterialTheme.typography.labelSmall)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.sewMaterials.filter { it.qty >= 1.0 }.forEach { mat ->
                        AssistChip(
                            onClick = {
                                supplyLines.add(DraftSupplyLine("${mat.name} (مخزون)", mat.costPerUnitCents, mat.id, 1.0))
                            },
                            label = { Text("${mat.name} (${mat.qty} ${mat.unit})") }
                        )
                    }
                }
            }
            supplyLines.forEachIndexed { idx, s ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${s.name} — ${MoneyFormat.formatDzd(s.costCents)}", style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { supplyLines.removeAt(idx) }) { Icon(Icons.Rounded.Close, contentDescription = "إزالة") }
                }
            }
        }

        // Collapsible Section 3: Measurements
        Spacer(Modifier.height(6.dp))
        FilledTonalButton(onClick = { showMeasurements = !showMeasurements }, modifier = Modifier.fillMaxWidth()) {
            Text("مقاسات الطلبية (اختياري)")
        }
        if (showMeasurements) {
            Spacer(Modifier.height(6.dp))
            if (state.measurements.isNotEmpty()) {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.measurements.forEach { m ->
                        AssistChip(
                            onClick = {
                                mLen = m.len; mSh = m.shoulder; mChest = m.chest
                                mWaist = m.waist; mHip = m.hip; mSleeve = m.sleeve; mNote = m.note
                            },
                            label = { Text("نسخ: ${m.name}") }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = mLen, onValueChange = { mLen = it }, label = { Text("الطول") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = mSh, onValueChange = { mSh = it }, label = { Text("الكتف") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = mChest, onValueChange = { mChest = it }, label = { Text("الصدر") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = mWaist, onValueChange = { mWaist = it }, label = { Text("الخصر") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = mHip, onValueChange = { mHip = it }, label = { Text("الأرداف") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = mSleeve, onValueChange = { mSleeve = it }, label = { Text("الكم") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = mNote, onValueChange = { mNote = it }, label = { Text("ملاحظات المقاسات") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }

        // Collapsible Section 4: Pricing Estimator
        Spacer(Modifier.height(6.dp))
        FilledTonalButton(onClick = { showEstimator = !showEstimator }, modifier = Modifier.fillMaxWidth()) {
            Text("حاسبة التسعير المقترح")
        }
        if (showEstimator) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = estFabText, onValueChange = { estFabText = it }, label = { Text("تقدير المواد") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = estHoursText, onValueChange = { estHoursText = it }, label = { Text("الساعات") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.8f))
                OutlinedTextField(value = estLaborHourText, onValueChange = { estLaborHourText = it }, label = { Text("أجرة الساعة") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = estExtraText, onValueChange = { estExtraText = it }, label = { Text("طرز/إضافات") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = estProfitPctText, onValueChange = { estProfitPctText = it }, label = { Text("ربح %") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(0.8f))
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("السعر المقترح: ${MoneyFormat.formatDzd(estSuggestedCents)}", style = MaterialTheme.typography.titleSmall, color = EmeraldMain)
                TextButton(onClick = {
                    val diff = estSuggestedCents - (fabricLaborCents + workshopFabricsPriceCents + suppliesCostCents + adjustmentCents)
                    flatLaborText = MoneyFormat.formatNumberOnly(max(0L, diff))
                }) { Text("اعتماد السعر") }
            }
        }

        // Grand Total, Flat Labor, Adjustment & Deposit
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = orderMeterRateText, onValueChange = { orderMeterRateText = it }, label = { Text("أجرة المتر (دج)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            OutlinedTextField(value = flatLaborText, onValueChange = { flatLaborText = it }, label = { Text("أتعاب بدون أمتار") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f).testTag("sewing_total_input"))
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = adjustmentText, onValueChange = { adjustmentText = it }, label = { Text("تعديل السعر (+/-)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            OutlinedTextField(value = depositText, onValueChange = { depositText = it }, label = { Text("العربون (دج)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("المجموع النهائي للطلبية: ${MoneyFormat.formatDzd(grandTotalCents)}", style = MaterialTheme.typography.titleMedium, color = EmeraldMain)
                Text(
                    "أتعاب الخياطة: ${MoneyFormat.formatDzd(fabricLaborCents + flatLaborCents)} • قماش الورشة: ${MoneyFormat.formatDzd(workshopFabricsPriceCents)} • لوازم: ${MoneyFormat.formatDzd(suppliesCostCents)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                val dep = MoneyFormat.parseToCents(depositText) ?: 0L
                val fabText = fabricLines.joinToString(" | ") { "${it.name} (${it.meters}م)" }
                val supText = supplyLines.joinToString(" | ") { "${it.name} (${MoneyFormat.formatNumberOnly(sOrZero(it.costCents))} دج)" }
                val measText = listOfNotNull(
                    mLen.takeIf { it.isNotBlank() }?.let { "طول:$it" },
                    mSh.takeIf { it.isNotBlank() }?.let { "كتف:$it" },
                    mChest.takeIf { it.isNotBlank() }?.let { "صدر:$it" },
                    mWaist.takeIf { it.isNotBlank() }?.let { "خصر:$it" },
                    mHip.takeIf { it.isNotBlank() }?.let { "أرداف:$it" },
                    mSleeve.takeIf { it.isNotBlank() }?.let { "كم:$it" },
                    mNote.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                val stockDeductions = supplyLines.mapNotNull { line ->
                    line.stockId?.let { id -> id to line.stockQty }
                }
                viewModel.addTailoringOrder(
                    client = client,
                    phone = phone,
                    item = item,
                    totalCents = grandTotalCents,
                    depositCents = dep,
                    materialCostCents = workshopFabricsBuyCostCents + suppliesCostCents,
                    laborCents = fabricLaborCents + flatLaborCents,
                    dueISO = dueISO,
                    fittingISO = fittingISO,
                    fabricsSummary = fabText,
                    measurementsSummary = measText,
                    photoUri = photoUri,
                    adjustmentCents = adjustmentCents,
                    suppliesSummary = supText,
                    workshopFabricBuyCents = workshopFabricsBuyCostCents,
                    stockDeductions = stockDeductions
                )
                if (client.isNotBlank() && item.isNotBlank() && grandTotalCents > 0L) {
                    onSaved()
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("save_sewing_order_button")
        ) {
            Text("تسجيل الطلبية وعرض الحساب")
        }
    }
}

private fun sOrZero(cents: Long): Long = max(0L, cents)
