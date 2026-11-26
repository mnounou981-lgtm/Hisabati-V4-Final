package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CreditorDebtEntity
import com.example.data.local.TailoringOrderEntity
import com.example.domain.DateUtils
import com.example.domain.LedgerCalculator
import com.example.domain.MoneyFormat
import com.example.ui.components.SandooqiCard
import com.example.ui.screens.sewing.sewingCalendarItems
import com.example.ui.screens.sewing.sewingClientsItems
import com.example.ui.screens.sewing.sewingFinanceItems
import com.example.ui.screens.sewing.sewingOrdersItems
import com.example.ui.screens.sewing.sewingWorkshopItems
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.SewingVioletSoft
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel
import kotlin.math.max

@Composable
fun SewingScreen(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel
) {
    val privacy = state.settings.privacyMode
    var subTab by remember { mutableIntStateOf(0) } // 0=الطلبيات, 1=المواعيد, 2=الزبائن, 3=الورشة, 4=المالية
    var weekOffset by remember { mutableIntStateOf(0) }
    var showOrderForm by remember { mutableStateOf(false) }

    var prefillClient by remember { mutableStateOf("") }
    var prefillPhone by remember { mutableStateOf("") }
    var prefillMeas by remember { mutableStateOf("") }

    var payOrder by remember { mutableStateOf<TailoringOrderEntity?>(null) }
    var payOrderAmt by remember { mutableStateOf("") }
    var cancelOrderTarget by remember { mutableStateOf<TailoringOrderEntity?>(null) }
    var refundAmtText by remember { mutableStateOf("") }

    var paySewDebtTarget by remember { mutableStateOf<CreditorDebtEntity?>(null) }
    var paySewDebtAmt by remember { mutableStateOf("") }

    val today = DateUtils.todayISO()
    val lateCount = remember(state.orders, today) {
        state.orders.count { !it.isCancelled && !it.isDone && it.dueDateISO.isNotBlank() && it.dueDateISO < today }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sandooqi Workshop Hero Summary Strip
        item {
            SandooqiCard(containerColor = SewingVioletSoft.copy(alpha = 0.5f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("صندوقي للورشة والخياطة", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "اليوم: ${state.dueTodayOrdersCount} تسليم • متأخرة: $lateCount • ذمم: ${MoneyFormat.formatDzd(state.workshopSummary.clientReceivablesCents, privacy)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = MoneyFormat.formatDzd(state.currentReport.sewing.balanceCents, privacy),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 5 Sewing Tabs matching Sandooqi_V12_Ready
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    0 to "الطلبيات",
                    1 to "المواعيد",
                    2 to "الزبائن",
                    3 to "الورشة",
                    4 to "المالية"
                ).forEach { (idx, label) ->
                    FilterChip(
                        selected = subTab == idx,
                        onClick = { subTab = idx },
                        label = { Text(label, maxLines = 1) },
                        modifier = Modifier.weight(1f).testTag("sewing_subtab_$idx")
                    )
                }
            }
        }

        when (subTab) {
            0 -> sewingOrdersItems(
                state = state,
                viewModel = viewModel,
                showOrderForm = showOrderForm,
                onToggleOrderForm = { showOrderForm = it },
                prefillClient = prefillClient,
                prefillPhone = prefillPhone,
                prefillMeasSummary = prefillMeas,
                onClearPrefill = {
                    prefillClient = ""; prefillPhone = ""; prefillMeas = ""
                },
                onPayOrder = { order ->
                    payOrder = order
                    payOrderAmt = MoneyFormat.formatNumberOnly(LedgerCalculator.orderRemainingCents(order))
                },
                onCancelOrder = { order ->
                    cancelOrderTarget = order
                    refundAmtText = MoneyFormat.formatNumberOnly((order.paidCents - order.refundedCents).coerceAtLeast(0L))
                }
            )

            1 -> sewingCalendarItems(
                state = state,
                weekOffset = weekOffset,
                onChangeWeekOffset = { weekOffset = it }
            )

            2 -> sewingClientsItems(
                state = state,
                viewModel = viewModel,
                onStartOrderForClient = { name, phone, meas ->
                    prefillClient = name
                    prefillPhone = phone
                    prefillMeas = meas
                    showOrderForm = true
                    subTab = 0
                }
            )

            3 -> sewingWorkshopItems(state = state, viewModel = viewModel)

            4 -> sewingFinanceItems(
                state = state,
                viewModel = viewModel,
                onPayOrder = { order ->
                    payOrder = order
                    payOrderAmt = MoneyFormat.formatNumberOnly(LedgerCalculator.orderRemainingCents(order))
                },
                onPaySewDebt = { debt ->
                    paySewDebtTarget = debt
                    paySewDebtAmt = MoneyFormat.formatNumberOnly(max(0L, debt.amountCents - debt.paidCents))
                }
            )
        }
    }

    if (payOrder != null) {
        val o = payOrder!!
        AlertDialog(
            onDismissRequest = { payOrder = null },
            title = { Text("تسديد دفعة لطلبية ${o.client}") },
            text = {
                OutlinedTextField(
                    value = payOrderAmt,
                    onValueChange = { payOrderAmt = it },
                    label = { Text("المبلغ (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            },
            confirmButton = {
                Button(onClick = {
                    val cents = MoneyFormat.parseToCents(payOrderAmt) ?: 0L
                    viewModel.recordOrderPayment(o, cents)
                    payOrder = null
                }) { Text("تسجيل الدفعة") }
            },
            dismissButton = { TextButton(onClick = { payOrder = null }) { Text("إلغاء") } }
        )
    }

    if (cancelOrderTarget != null) {
        val o = cancelOrderTarget!!
        AlertDialog(
            onDismissRequest = { cancelOrderTarget = null },
            title = { Text("إلغاء طلبية ${o.client}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المدفوع الصافي: ${MoneyFormat.formatDzd(o.paidCents - o.refundedCents, privacy)}")
                    OutlinedTextField(
                        value = refundAmtText,
                        onValueChange = { refundAmtText = it },
                        label = { Text("المبلغ المسترد للزبونة (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    onClick = {
                        val ref = MoneyFormat.parseToCents(refundAmtText) ?: 0L
                        viewModel.cancelOrder(o, ref)
                        cancelOrderTarget = null
                    }
                ) { Text("تأكيد الإلغاء") }
            },
            dismissButton = { TextButton(onClick = { cancelOrderTarget = null }) { Text("تراجع") } }
        )
    }

    if (paySewDebtTarget != null) {
        val d = paySewDebtTarget!!
        val rem = max(0L, d.amountCents - d.paidCents)
        AlertDialog(
            onDismissRequest = { paySewDebtTarget = null },
            title = { Text(if (d.kind == "sew_debt_in") "تحصيل دين من ${d.creditor}" else "تسديد دين إلى ${d.creditor}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المتبقي: ${MoneyFormat.formatDzd(rem, privacy)}")
                    OutlinedTextField(
                        value = paySewDebtAmt,
                        onValueChange = { paySewDebtAmt = it },
                        label = { Text("المبلغ (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val c = MoneyFormat.parseToCents(paySewDebtAmt) ?: 0L
                    viewModel.paySewDebt(d, c)
                    paySewDebtTarget = null
                }) { Text("تأكيد الدفعة") }
            },
            dismissButton = { TextButton(onClick = { paySewDebtTarget = null }) { Text("إلغاء") } }
        )
    }
}
