package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.FacPlanEntity
import com.example.domain.CatalogDefaults
import com.example.domain.CreditorGroupSummary
import com.example.domain.DateUtils
import com.example.domain.LedgerCalculator
import com.example.domain.MoneyFormat
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatTile
import com.example.ui.theme.EmeraldMain
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel

@Composable
fun DebtsScreen(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel
) {
    val privacy = state.settings.privacyMode
    var subTab by remember { mutableIntStateOf(0) } // 0=Shops/Rent, 1=Bills/Obligations, 2=Fac/Deductions, 3=Family
    var payingCreditor by remember { mutableStateOf<CreditorGroupSummary?>(null) }
    var payAmountText by remember { mutableStateOf("") }

    // Manual Debt inputs
    var debtCreditor by remember { mutableStateOf("") }
    var debtItem by remember { mutableStateOf("") }
    var debtAmount by remember { mutableStateOf("") }
    var debtDue by remember { mutableStateOf("") }

    // Rent inputs
    var rentUnit by remember { mutableStateOf(CatalogDefaults.rentUnits.first()) }
    var rentParty by remember { mutableStateOf("") }
    var rentAmount by remember { mutableStateOf("") }
    var rentIsReceivable by remember { mutableStateOf(true) }
    var rentImmediate by remember { mutableStateOf(false) }

    // Bills inputs
    var billName by remember { mutableStateOf(CatalogDefaults.standardBills.first()) }
    var billAmount by remember { mutableStateOf("") }
    var billCreditor by remember { mutableStateOf("") }
    var billPaid by remember { mutableStateOf(true) }
    var billRecurring by remember { mutableStateOf(true) }
    var billMonths by remember { mutableStateOf("1") }

    // Fac & Services monthly plan inputs
    var facPlanType by remember { mutableStateOf("fac") } // "fac" or "srv"
    var planListFilter by remember { mutableStateOf("all") } // "all", "fac", "srv"
    var facName by remember { mutableStateOf("") }
    var facInst by remember { mutableStateOf("") }
    var facMonths by remember { mutableStateOf("6") }
    var dedName by remember { mutableStateOf("") }
    var dedAmt by remember { mutableStateOf("") }
    var dedIsSrv by remember { mutableStateOf(false) }

    // Family member expense inputs
    var famPerson by remember(state.settings.familyMembers) {
        mutableStateOf(state.settings.familyMembers.firstOrNull() ?: "إسلام")
    }
    var famItem by remember { mutableStateOf("") }
    var famAmt by remember { mutableStateOf("") }

    val rentReceivables = remember(state.debts) {
        state.debts.filter { it.kind == "rent_in" && !it.settled }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    0 to "المحلات والكراء",
                    1 to "الفواتير",
                    2 to "الأقساط",
                    3 to "العائلة"
                ).forEach { (idx, label) ->
                    FilterChip(
                        selected = subTab == idx,
                        onClick = { subTab = idx },
                        label = { Text(label, maxLines = 1) },
                        modifier = Modifier.weight(1f).testTag("debts_subtab_$idx")
                    )
                }
            }
        }

        when (subTab) {
            0 -> {
                // Creditor groups
                item {
                    SandooqiCard {
                        SectionHeader(
                            title = "دفتر ديون المحلات (عليه)",
                            subtitle = "إجمالي الديون المفتوحة: ${MoneyFormat.formatDzd(state.openHomeAndBillDebtsCents, privacy)}",
                            icon = Icons.Rounded.Storefront
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = debtCreditor,
                                onValueChange = { debtCreditor = it },
                                label = { Text("التاجر / المحل") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = debtAmount,
                                onValueChange = { debtAmount = it },
                                label = { Text("المبلغ (دج)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = debtItem,
                                onValueChange = { debtItem = it },
                                label = { Text("الغرض") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                            OutlinedTextField(
                                value = debtDue,
                                onValueChange = { debtDue = it },
                                label = { Text("الاستحقاق (YYYY-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val cents = MoneyFormat.parseToCents(debtAmount) ?: 0L
                                viewModel.addManualHomeDebt(debtCreditor, debtItem, cents, debtDue, "أخرى")
                                if (debtCreditor.isNotBlank() && cents > 0L) {
                                    debtCreditor = ""; debtItem = ""; debtAmount = ""; debtDue = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add_manual_debt_button")
                        ) { Text("تسجيل دين جديد") }
                    }
                }

                if (state.creditorGroups.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "لا توجد ديون مسجلة حالياً",
                            description = "جميع ديون المحلات والفواتير مسددة.",
                            icon = Icons.Rounded.CheckCircle
                        )
                    }
                } else {
                    items(state.creditorGroups, key = { it.creditor }) { group ->
                        var expanded by remember { mutableStateOf(false) }
                        SandooqiCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(group.creditor, style = MaterialTheme.typography.titleMedium)
                                        if (group.hasOverdue) {
                                            Surface(color = ExpenseRed.copy(alpha = 0.14f), shape = RoundedCornerShape(8.dp)) {
                                                Text(
                                                    "متأخر",
                                                    color = ExpenseRed,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        "المتبقي: ${MoneyFormat.formatDzd(group.remainingCents, privacy)} من أصل ${MoneyFormat.formatDzd(group.totalAmountCents, privacy)} • اضغط للتفاصيل",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (group.remainingCents > 0L) ExpenseRed else IncomeGreen
                                    )
                                }
                                if (group.remainingCents > 0L) {
                                    Button(
                                        onClick = {
                                            payingCreditor = group
                                            payAmountText = MoneyFormat.formatNumberOnly(group.remainingCents)
                                        }
                                    ) { Text("تسديد") }
                                }
                            }
                            if (expanded) {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                group.debts.forEach { d ->
                                    val remItem = (d.amountCents - d.paidCents).coerceAtLeast(0L)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${d.item} (${d.dateISO})", style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                "متبقي: ${MoneyFormat.formatDzd(remItem, privacy)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (remItem > 0L) ExpenseRed else IncomeGreen
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteDebt(d.id) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف الدين")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Rent section (له وعليه)
                item {
                    SandooqiCard {
                        SectionHeader(
                            title = "حركة الكراء (له وعليه)",
                            subtitle = "كراء السكن أو المحلات التجارية",
                            icon = Icons.Rounded.HomeWork
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = rentIsReceivable, onClick = { rentIsReceivable = true }, label = { Text("كراء له (إيجار لي)") }, modifier = Modifier.weight(1f))
                            FilterChip(selected = !rentIsReceivable, onClick = { rentIsReceivable = false }, label = { Text("كراء عليه (التزام)") }, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CatalogDefaults.rentUnits.forEach { unit ->
                                FilterChip(
                                    selected = rentUnit == unit,
                                    onClick = { rentUnit = unit },
                                    label = { Text(unit) }
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = rentParty, onValueChange = { rentParty = it }, label = { Text("المستأجر / المؤجر") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(
                                value = rentAmount,
                                onValueChange = { rentAmount = it },
                                label = { Text("المبلغ (دج)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = rentImmediate, onCheckedChange = { rentImmediate = it })
                            Text("تحصيل / دفع فوري في الخزينة العائلية", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                val c = MoneyFormat.parseToCents(rentAmount) ?: 0L
                                viewModel.addRentRecord(rentUnit, rentParty, c, rentIsReceivable, rentImmediate, "")
                                if (rentParty.isNotBlank() && c > 0L) {
                                    rentParty = ""; rentAmount = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text("حفظ حركة الكراء") }
                    }
                }

                items(rentReceivables, key = { it.id }) { r ->
                    SandooqiCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("مستحق لك: ${r.creditor} (${r.rentUnit})", style = MaterialTheme.typography.titleSmall)
                                Text(MoneyFormat.formatDzd(r.amountCents - r.paidCents, privacy), color = IncomeGreen, fontWeight = FontWeight.Bold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { viewModel.collectRentReceivable(r) }) { Text("تحصيل") }
                                IconButton(onClick = { viewModel.deleteDebt(r.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Bill spikes & standard bills / obligations
                if (state.billSpikes.isNotEmpty()) {
                    item {
                        SandooqiCard(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)) {
                            SectionHeader(title = "تنبيه ارتفاع الفواتير", icon = Icons.Rounded.TrendingUp)
                            Spacer(Modifier.height(6.dp))
                            state.billSpikes.forEach { spike ->
                                Text(
                                    "• ${spike.name}: ${MoneyFormat.formatDzd(spike.latestCents, privacy)} (+${spike.increasePct}% عن المتوسط)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
                item {
                    SandooqiCard {
                        SectionHeader(title = "الفواتير والالتزامات الشهرية", icon = Icons.Rounded.ReceiptLong)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CatalogDefaults.standardBills.take(2).forEach { std ->
                                FilterChip(selected = billName == std, onClick = { billName = std }, label = { Text(std.removePrefix("فاتورة "), maxLines = 1) }, modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(value = billName, onValueChange = { billName = it }, label = { Text("اسم الفاتورة / الالتزام") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(value = billCreditor, onValueChange = { billCreditor = it }, label = { Text("الجهة / المستفيد (مثل: سونلغاز، اتصالات)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = billAmount,
                                onValueChange = { billAmount = it },
                                label = { Text("المبلغ (دج)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = billMonths,
                                onValueChange = { billMonths = it },
                                label = { Text("عدد الأشهر") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.7f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = billRecurring, onClick = { billRecurring = !billRecurring }, label = { Text("التزام شهري متكرر") }, modifier = Modifier.weight(1f))
                            FilterChip(selected = billPaid, onClick = { billPaid = !billPaid }, label = { Text("مدفوعة الآن") }, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val c = MoneyFormat.parseToCents(billAmount) ?: 0L
                                val m = billMonths.toIntOrNull() ?: 1
                                viewModel.addBillOrObligation(billName, c, billCreditor, DateUtils.todayISO(), billPaid, billRecurring, m)
                                if (c > 0L) billAmount = ""
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text("تسجيل الفاتورة / الالتزام") }
                    }
                }
                items(state.obligations, key = { it.id }) { obl ->
                    val unpaid = LedgerCalculator.obligationUnpaidMonths(obl, state.currentYM)
                    SandooqiCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(obl.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (unpaid.isEmpty()) "مسددة حتى ${DateUtils.formatYM(state.currentYM)}"
                                    else "أشهر غير مدفوعة: ${unpaid.size} (${MoneyFormat.formatDzd(obl.amountCents * unpaid.size, privacy)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (unpaid.isEmpty()) IncomeGreen else GoldAccent
                                )
                            }
                            Row {
                                if (unpaid.isNotEmpty()) {
                                    Button(onClick = { viewModel.payObligationCurrentMonth(obl) }) { Text("دفع شهر") }
                                }
                                IconButton(onClick = { viewModel.deleteObligation(obl.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Facility & Services Monthly Plans + Quick Deductions
                val facMonthlyDue = state.facPlans
                    .filter { it.planType != "srv" && it.paidMonths < it.totalMonths }
                    .sumOf { it.installmentCents }
                val srvMonthlyDue = state.facPlans
                    .filter { it.planType == "srv" && it.paidMonths < it.totalMonths }
                    .sumOf { it.installmentCents }
                val filteredPlans = when (planListFilter) {
                    "fac" -> state.facPlans.filter { it.planType != "srv" }
                    "srv" -> state.facPlans.filter { it.planType == "srv" }
                    else -> state.facPlans
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(
                            label = "أقساط فاسيليتي/شهر",
                            value = MoneyFormat.formatDzd(facMonthlyDue, privacy),
                            accentColor = EmeraldMain,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "أقساط خدمات/شهر",
                            value = MoneyFormat.formatDzd(srvMonthlyDue, privacy),
                            accentColor = GoldAccent,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    SandooqiCard {
                        SectionHeader(
                            title = "خطط الأقساط الشهرية (فاسيليتي وخدمات)",
                            subtitle = "متابعة أقساط الفاسيليتي والخدمات الاجتماعية شهرياً",
                            icon = Icons.Rounded.CreditScore
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = facPlanType == "fac",
                                onClick = { facPlanType = "fac" },
                                label = { Text("خطة فاسيليتي", maxLines = 1) },
                                modifier = Modifier.weight(1f).testTag("plan_type_fac_chip")
                            )
                            FilterChip(
                                selected = facPlanType == "srv",
                                onClick = { facPlanType = "srv" },
                                label = { Text("خطة خدمات شهرية", maxLines = 1) },
                                modifier = Modifier.weight(1f).testTag("plan_type_srv_chip")
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = facName,
                            onValueChange = { facName = it },
                            label = {
                                Text(
                                    if (facPlanType == "srv") "اسم الخدمة / الاشتراك الشهري"
                                    else "اسم الغرض / خطة الفاسيليتي"
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("plan_name_input")
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = facInst,
                                onValueChange = { facInst = it },
                                label = { Text("القسط الشهري (دج)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("plan_installment_input")
                            )
                            OutlinedTextField(
                                value = facMonths,
                                onValueChange = { facMonths = it },
                                label = { Text("عدد الأشهر") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f).testTag("plan_months_input")
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val c = MoneyFormat.parseToCents(facInst) ?: 0L
                                val m = facMonths.toIntOrNull() ?: 6
                                viewModel.addFacPlan(facName, c, m, facPlanType)
                                if (facName.isNotBlank() && c > 0L) {
                                    facName = ""
                                    facInst = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add_fac_plan_button")
                        ) {
                            Text(
                                if (facPlanType == "srv") "إضافة خطة خدمات شهرية"
                                else "إضافة خطة فاسيليتي"
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "all" to "الكل (${state.facPlans.size})",
                            "fac" to "فاسيليتي (${state.facPlans.count { it.planType != "srv" }})",
                            "srv" to "خدمات (${state.facPlans.count { it.planType == "srv" }})"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = planListFilter == key,
                                onClick = { planListFilter = key },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f).testTag("plan_filter_$key")
                            )
                        }
                    }
                }
                if (filteredPlans.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "لا توجد خطط مسجلة",
                            description = "أضف خطة فاسيليتي أو خدمات شهرية بالأعلى لمتابعة الأقساط والخصم الشهري.",
                            icon = Icons.Rounded.CreditScore
                        )
                    }
                } else {
                    items(filteredPlans, key = { it.id }) { plan ->
                        InstallmentPlanCard(
                            plan = plan,
                            currentYM = state.currentYM,
                            privacy = privacy,
                            onPay = { viewModel.payFacInstallment(plan) },
                            onDelete = { viewModel.deleteFacPlan(plan.id) }
                        )
                    }
                }
                item {
                    SandooqiCard {
                        SectionHeader(title = "اقتطاع مباشر (خدمات / فاسيليتي)", icon = Icons.Rounded.Payments)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !dedIsSrv, onClick = { dedIsSrv = false }, label = { Text("فاسيليتي") }, modifier = Modifier.weight(1f))
                            FilterChip(selected = dedIsSrv, onClick = { dedIsSrv = true }, label = { Text("خدمات اجتماعية") }, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = dedName, onValueChange = { dedName = it }, label = { Text("البيان") }, singleLine = true, modifier = Modifier.weight(1.2f))
                            OutlinedTextField(
                                value = dedAmt,
                                onValueChange = { dedAmt = it },
                                label = { Text("المبلغ (دج)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                val c = MoneyFormat.parseToCents(dedAmt) ?: 0L
                                viewModel.addDeduction(dedIsSrv, dedName, c, state.currentYM)
                                if (c > 0L) { dedName = ""; dedAmt = "" }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text("خصم من الحساب العائلي") }
                    }
                }
            }

            3 -> {
                // Family expenses & Family-Only Entry Mode
                item {
                    SandooqiCard {
                        SectionHeader(title = "مصروف أفراد العائلة", icon = Icons.Rounded.FamilyRestroom)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.settings.familyMembers.forEach { member ->
                                FilterChip(selected = famPerson == member, onClick = { famPerson = member }, label = { Text(member, maxLines = 1) })
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = famItem, onValueChange = { famItem = it }, label = { Text("الغرض") }, singleLine = true, modifier = Modifier.weight(1.2f))
                            OutlinedTextField(
                                value = famAmt,
                                onValueChange = { famAmt = it },
                                label = { Text("المبلغ (دج)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val c = MoneyFormat.parseToCents(famAmt) ?: 0L
                                viewModel.addFamilyExpense(famPerson, famItem, c)
                                if (famItem.isNotBlank() && c > 0L) { famItem = ""; famAmt = "" }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text("تسجيل مصروف لـ $famPerson") }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.enableFamilyEntryMode(famPerson) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.LockPerson, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("تفعيل وضع إدخال العائلة لـ $famPerson")
                        }
                    }
                }
            }
        }
    }

    if (payingCreditor != null) {
        val group = payingCreditor!!
        AlertDialog(
            onDismissRequest = { payingCreditor = null },
            title = { Text("تسديد دين: ${group.creditor}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المتبقي: ${MoneyFormat.formatDzd(group.remainingCents, privacy)}")
                    OutlinedTextField(
                        value = payAmountText,
                        onValueChange = { payAmountText = it },
                        label = { Text("المبلغ المدفوع (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val cents = MoneyFormat.parseToCents(payAmountText) ?: 0L
                    viewModel.payCreditorAmount(group.creditor, cents)
                    payingCreditor = null
                }) { Text("تسديد من الخزينة") }
            },
            dismissButton = { TextButton(onClick = { payingCreditor = null }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun InstallmentPlanCard(
    plan: FacPlanEntity,
    currentYM: String,
    privacy: Boolean,
    onPay: () -> Unit,
    onDelete: () -> Unit
) {
    val isService = plan.planType == "srv"
    val badgeColor = if (isService) GoldAccent else EmeraldMain
    val badgeLabel = if (isService) "خدمات" else "فاسيليتي"
    val totalMonthsSafe = plan.totalMonths.coerceAtLeast(1)
    val remainingMonths = (plan.totalMonths - plan.paidMonths).coerceAtLeast(0)
    val remainingTotalCents = remainingMonths.toLong() * plan.installmentCents
    val isCompleted = plan.paidMonths >= plan.totalMonths
    val isPaidThisMonth = plan.lastPaidYM == currentYM
    val progress = (plan.paidMonths.toFloat() / totalMonthsSafe.toFloat()).coerceIn(0f, 1f)

    SandooqiCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(plan.name, style = MaterialTheme.typography.titleSmall)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${plan.paidMonths}/${plan.totalMonths} شهر • ${MoneyFormat.formatDzd(plan.installmentCents, privacy)} شهرياً",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (isCompleted) "خطة مكتملة بالكامل"
                        else "المتبقي: $remainingMonths أشهر (${MoneyFormat.formatDzd(remainingTotalCents, privacy)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCompleted) IncomeGreen else badgeColor
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isCompleted && !isPaidThisMonth) {
                        Button(
                            onClick = onPay,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("pay_plan_${plan.id}")
                        ) { Text("خصم قسط") }
                    } else if (!isCompleted && isPaidThisMonth) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = IncomeGreen.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "مدفوع هذا الشهر",
                                style = MaterialTheme.typography.labelSmall,
                                color = IncomeGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_plan_${plan.id}")
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                color = if (isCompleted) IncomeGreen else badgeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
        }
    }
}

