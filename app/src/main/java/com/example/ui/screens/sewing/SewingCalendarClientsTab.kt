package com.example.ui.screens.sewing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomerMeasurementEntity
import com.example.domain.DateUtils
import com.example.domain.MoneyFormat
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.EmeraldMain
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel

fun LazyListScope.sewingCalendarItems(
    state: SandooqiUiState,
    weekOffset: Int,
    onChangeWeekOffset: (Int) -> Unit
) {
    val today = DateUtils.todayISO()
    val days = DateUtils.weekDatesISO(weekOffset)
    val rangeLabel = if (days.isNotEmpty()) "${days.first().first} — ${days.last().first}" else ""
    val dueAlerts = state.orders
        .filter { !it.isCancelled && !it.isDone && it.dueDateISO.isNotBlank() }
        .sortedBy { it.dueDateISO }
        .take(10)

    item {
        SandooqiCard {
            SectionHeader(title = "مواعيد الخياطة الأسبوعية", subtitle = rangeLabel, icon = Icons.Rounded.CalendarMonth)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onChangeWeekOffset(weekOffset - 1) }, modifier = Modifier.weight(1f)) { Text("السابق") }
                FilledTonalButton(onClick = { onChangeWeekOffset(0) }, modifier = Modifier.weight(1f)) { Text("هذا الأسبوع") }
                OutlinedButton(onClick = { onChangeWeekOffset(weekOffset + 1) }, modifier = Modifier.weight(1f)) { Text("التالي") }
            }
        }
    }

    items(days) { (iso, label) ->
        val dayOrders = state.orders.filter { !it.isCancelled && (it.dueDateISO == iso || it.fittingDateISO == iso) }
        SandooqiCard {
            Text(
                text = if (iso == today) "$label (اليوم)" else label,
                style = MaterialTheme.typography.titleSmall,
                color = if (iso == today) EmeraldMain else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            if (dayOrders.isEmpty()) {
                Text("لا توجد مواعيد تسليم أو قياس", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                dayOrders.forEach { o ->
                    val kind = if (o.dueDateISO == iso && o.fittingDateISO == iso) "قياس + تسليم"
                    else if (o.fittingDateISO == iso) "موعد تجربة" else "تسليم"
                    Text("• [$kind] ${o.client}: ${o.item} (${o.stage})", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    item {
        SandooqiCard {
            SectionHeader(
                title = "متابعة مواعيد التسليم",
                subtitle = "الطلبيات المتأخرة والقادمة",
                icon = Icons.Rounded.NotificationsActive
            )
            Spacer(Modifier.height(6.dp))
            if (dueAlerts.isEmpty()) {
                Text("لا توجد مواعيد تسليم معلقة حالياً.", style = MaterialTheme.typography.bodySmall)
            } else {
                dueAlerts.forEach { o ->
                    val isLate = o.dueDateISO < today
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${o.client} — ${o.item}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (isLate) "متأخر (${o.dueDateISO})" else o.dueDateISO,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLate) ExpenseRed else GoldAccent
                        )
                    }
                }
            }
        }
    }
}

fun LazyListScope.sewingClientsItems(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel,
    onStartOrderForClient: (name: String, phone: String, measSummary: String) -> Unit
) {
    item {
        ClientsBookAndServiceSection(
            state = state,
            viewModel = viewModel,
            onStartOrderForClient = onStartOrderForClient
        )
    }
}

@Composable
private fun ClientsBookAndServiceSection(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel,
    onStartOrderForClient: (name: String, phone: String, measSummary: String) -> Unit
) {
    val privacy = state.settings.privacyMode
    var mName by remember { mutableStateOf("") }
    var mPhone by remember { mutableStateOf("") }
    var mLen by remember { mutableStateOf("") }
    var mSh by remember { mutableStateOf("") }
    var mChest by remember { mutableStateOf("") }
    var mWaist by remember { mutableStateOf("") }
    var mHip by remember { mutableStateOf("") }
    var mSleeve by remember { mutableStateOf("") }
    var mNote by remember { mutableStateOf("") }
    var measSearch by remember { mutableStateOf("") }

    var svcClient by remember { mutableStateOf("") }
    var svcItem by remember { mutableStateOf("") }
    var svcMeters by remember { mutableStateOf("") }
    var svcRate by remember { mutableStateOf(MoneyFormat.formatNumberOnly(state.settings.meterPriceCents)) }
    var svcThread by remember { mutableStateOf("") }
    var svcSupplies by remember { mutableStateOf("") }
    var svcPaid by remember { mutableStateOf("") }

    val metersVal = MoneyFormat.normalizeDigits(svcMeters).replace(",", ".").toDoubleOrNull() ?: 0.0
    val rateCents = MoneyFormat.parseToCents(svcRate) ?: 0L
    val svcPriceCents = Math.round(metersVal * rateCents)
    val svcCostsCents = (MoneyFormat.parseToCents(svcThread) ?: 0L) + (MoneyFormat.parseToCents(svcSupplies) ?: 0L)
    val svcExpectedProfit = svcPriceCents - svcCostsCents

    val filteredMeas = remember(state.measurements, measSearch) {
        val q = measSearch.trim().lowercase()
        state.measurements.filter { q.isEmpty() || it.name.lowercase().contains(q) || it.phone.contains(q) }
    }

    val unifiedProfiles = remember(state.orders, state.measurements, state.sewServices) {
        val names = (state.orders.map { it.client.trim() } +
            state.measurements.map { it.name.trim() } +
            state.sewServices.map { it.client.trim() }).filter { it.isNotEmpty() }.distinct()
        names.map { name ->
            val ordCount = state.orders.count { !it.isCancelled && it.client.trim() == name }
            val svcCount = state.sewServices.count { it.client.trim() == name }
            val hasMeas = state.measurements.any { it.name.trim() == name }
            val totalSpent = state.orders.filter { !it.isCancelled && it.client.trim() == name }.sumOf { it.totalCents } +
                state.sewServices.filter { it.client.trim() == name }.sumOf { it.priceCents }
            Triple(name, "$ordCount طلبيات • $svcCount خدمات" + if (hasMeas) " • مقاسات محفوظة" else "", totalSpent)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Measurements Form
        SandooqiCard {
            SectionHeader(
                title = "دفتر مقاسات الزبونات",
                subtitle = "${state.measurements.size} زبونة محفوظة",
                icon = Icons.Rounded.Straighten
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = mName, onValueChange = { mName = it }, label = { Text("اسم الزبونة") }, singleLine = true, modifier = Modifier.weight(1.2f))
                OutlinedTextField(value = mPhone, onValueChange = { mPhone = it }, label = { Text("الهاتف") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = mLen, onValueChange = { mLen = it }, label = { Text("الطول") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = mSh, onValueChange = { mSh = it }, label = { Text("الكتف") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = mChest, onValueChange = { mChest = it }, label = { Text("الصدر") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = mWaist, onValueChange = { mWaist = it }, label = { Text("الخصر") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = mHip, onValueChange = { mHip = it }, label = { Text("الأرداف") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = mSleeve, onValueChange = { mSleeve = it }, label = { Text("الكم") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = mNote, onValueChange = { mNote = it }, label = { Text("ملاحظات (رقبة، قماش...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.addMeasurement(
                        CustomerMeasurementEntity(
                            name = mName, phone = mPhone, len = mLen, shoulder = mSh,
                            chest = mChest, waist = mWaist, hip = mHip, sleeve = mSleeve, note = mNote
                        )
                    )
                    if (mName.isNotBlank()) {
                        mName = ""; mPhone = ""; mLen = ""; mSh = ""; mChest = ""; mWaist = ""; mHip = ""; mSleeve = ""; mNote = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("حفظ مقاسات الزبونة") }

            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = measSearch,
                onValueChange = { measSearch = it },
                label = { Text("ابحثي عن اسم الزبونة...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            if (filteredMeas.isEmpty()) {
                Text("لا توجد مقاسات مسجلة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                filteredMeas.forEach { m ->
                    val summary = "طول:${m.len} كتف:${m.shoulder} صدر:${m.chest} خصر:${m.waist} أرداف:${m.hip} كم:${m.sleeve}"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${m.name} ${if (m.phone.isNotBlank()) "(${m.phone})" else ""}", style = MaterialTheme.typography.titleSmall)
                                Row {
                                    TextButton(onClick = { onStartOrderForClient(m.name, m.phone, summary) }) { Text("استخدام في طلبية") }
                                    IconButton(onClick = { viewModel.deleteMeasurement(m.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                                    }
                                }
                            }
                            Text(summary, style = MaterialTheme.typography.bodySmall)
                            if (m.note.isNotBlank()) Text("ملاحظة: ${m.note}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Unified Customer Profiles
        if (unifiedProfiles.isNotEmpty()) {
            SandooqiCard {
                SectionHeader(title = "سجل الزبونات الموحد", subtitle = "المقاسات والطلبيات والخدمات", icon = Icons.Rounded.Groups)
                Spacer(Modifier.height(6.dp))
                unifiedProfiles.take(10).forEach { (name, info, total) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleSmall)
                            Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(MoneyFormat.formatDzd(total, privacy), style = MaterialTheme.typography.labelMedium, color = EmeraldMain)
                    }
                }
            }
        }

        // Customer Sewing Service (قماش الزبونة)
        SandooqiCard {
            SectionHeader(
                title = "تسجيل خدمة خياطة للزبونة",
                subtitle = "خياطة قماش الزبونة بالمتر وحساب الربح الصافي",
                icon = Icons.Rounded.DesignServices
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = svcClient, onValueChange = { svcClient = it }, label = { Text("اسم الزبونة") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = svcItem, onValueChange = { svcItem = it }, label = { Text("نوع الخياطة") }, singleLine = true, modifier = Modifier.weight(1.2f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = svcMeters, onValueChange = { svcMeters = it }, label = { Text("الأمتار") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = svcRate, onValueChange = { svcRate = it }, label = { Text("سعر المتر (دج)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = svcThread, onValueChange = { svcThread = it }, label = { Text("تكلفة الخيط") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = svcSupplies, onValueChange = { svcSupplies = it }, label = { Text("لوازم أخرى") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(value = svcPaid, onValueChange = { svcPaid = it }, label = { Text("المقبوض (دج)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "ثمن الخياطة: ${MoneyFormat.formatDzd(svcPriceCents, privacy)} • الربح المتوقع: ${MoneyFormat.formatDzd(svcExpectedProfit, privacy)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (svcExpectedProfit >= 0L) IncomeGreen else ExpenseRed
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val paidCents = MoneyFormat.parseToCents(svcPaid) ?: svcPriceCents
                    val thr = MoneyFormat.parseToCents(svcThread) ?: 0L
                    val sup = MoneyFormat.parseToCents(svcSupplies) ?: 0L
                    viewModel.addSewService(svcClient, svcItem, metersVal, rateCents, thr, sup, paidCents)
                    if (svcClient.isNotBlank() && svcItem.isNotBlank() && metersVal > 0) {
                        svcClient = ""; svcItem = ""; svcMeters = ""; svcThread = ""; svcSupplies = ""; svcPaid = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("+ تسجيل خياطة الزبونة") }

            if (state.sewServices.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                state.sewServices.forEach { svc ->
                    val profit = svc.priceCents - svc.threadCostCents - svc.suppliesCostCents
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${svc.client} — ${svc.item} (${svc.meters}م)", style = MaterialTheme.typography.titleSmall)
                            Text("الثمن: ${MoneyFormat.formatDzd(svc.priceCents, privacy)} • الربح: ${MoneyFormat.formatDzd(profit, privacy)}", style = MaterialTheme.typography.bodySmall, color = IncomeGreen)
                        }
                        IconButton(onClick = { viewModel.deleteSewService(svc.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                        }
                    }
                }
            }
        }
    }
}
