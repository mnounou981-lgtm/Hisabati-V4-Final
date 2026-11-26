package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.SavingsGoalEntity
import com.example.domain.DateUtils
import com.example.domain.MoneyFormat
import com.example.domain.ParsedVoiceEntry
import com.example.domain.QuickExpensePreset
import com.example.domain.VoiceEntryMode
import com.example.domain.VoiceParser
import com.example.ui.theme.EmeraldMain
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.MainNavTab
import com.example.ui.viewmodel.SearchHit

@Composable
fun VoiceEntryDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        parsed: ParsedVoiceEntry,
        finalTitle: String,
        finalAmountCents: Long,
        finalMode: VoiceEntryMode,
        finalParty: String,
        immediateTreasury: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(VoiceEntryMode.CASH) }
    var party by remember { mutableStateOf("") }
    var immediateTreasury by remember { mutableStateOf(false) }
    var speechUnavailable by remember { mutableStateOf(false) }

    fun applyParsed(text: String) {
        rawText = text
        val parsed = VoiceParser.parseArabicVoiceEntry(text)
        if (parsed.title.isNotBlank()) title = parsed.title
        if (parsed.amountText.isNotBlank()) amountText = parsed.amountText
        mode = parsed.mode
        if (parsed.party.isNotBlank()) party = parsed.party
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull().orEmpty()
            if (spoken.isNotBlank()) {
                applyParsed(spoken)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("الإدخال الصوتي والذكي")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "مثال: \"خبز وحليب 350 دين عند البقال\" أو \"كراء 15000 له من أحمد\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilledTonalButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-DZ")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث لتسجيل المصروف أو الدين")
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            speechUnavailable = false
                            speechLauncher.launch(intent)
                        } else {
                            speechUnavailable = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("voice_start_mic_button")
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تحدث الآن بالعربية")
                }

                if (speechUnavailable) {
                    Text(
                        text = "خدمة التعرف الصوتي غير متوفرة على هذا الجهاز — اكتب الجملة أدناه ليتم تحليلها تلقائياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseRed
                    )
                }

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { applyParsed(it) },
                    label = { Text("نص الجملة (تحليل تلقائي)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_raw_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VoiceModeChip("نقدي", mode == VoiceEntryMode.CASH) { mode = VoiceEntryMode.CASH }
                    VoiceModeChip("دين عليّ", mode == VoiceEntryMode.DEBT_OUT) { mode = VoiceEntryMode.DEBT_OUT }
                    VoiceModeChip("مستحق لي", mode == VoiceEntryMode.RECEIVABLE) { mode = VoiceEntryMode.RECEIVABLE }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("البيان / الغرض") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (mode != VoiceEntryMode.CASH) {
                    OutlinedTextField(
                        value = party,
                        onValueChange = { party = it },
                        label = { Text(if (mode == VoiceEntryMode.DEBT_OUT) "المحل / الدائن" else "الطرف المدين") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (mode == VoiceEntryMode.RECEIVABLE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { immediateTreasury = !immediateTreasury }
                    ) {
                        Checkbox(checked = immediateTreasury, onCheckedChange = { immediateTreasury = it })
                        Text("إضافة المبلغ للخزينة فوراً (تم التحصيل)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cents = MoneyFormat.parseToCents(amountText) ?: 0L
                    onSubmit(
                        VoiceParser.parseArabicVoiceEntry(rawText),
                        title,
                        cents,
                        mode,
                        party,
                        immediateTreasury
                    )
                },
                modifier = Modifier.testTag("voice_save_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
private fun RowScope.VoiceModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        modifier = Modifier.weight(1f)
    )
}

@Composable
fun GlobalSearchDialog(
    onSearch: (String) -> List<SearchHit>,
    onNavigate: (MainNavTab) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val hits = remember(query) { onSearch(query) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("البحث الشامل في صندوقي")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("ابحث عن مصروف، تاجر، طلبية، ضمان...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input")
                )
                if (query.isBlank()) {
                    Text(
                        text = "اكتب كلمة للبحث في الحركات، ديون المحلات، طلبيات الخياطة والضمانات.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (hits.isEmpty()) {
                    Text(
                        text = "لا توجد نتائج مطابقة لـ \"$query\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(hits) { hit ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onNavigate(hit.targetTab)
                                        onDismiss()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "${hit.kind} • ${hit.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = hit.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}

@Composable
fun MonthCloseDialog(
    currentYM: String,
    surplusCents: Long,
    goals: List<SavingsGoalEntity>,
    privacyMode: Boolean,
    onDismiss: () -> Unit,
    onConfirmClose: (targetGoalId: Long?, savePercent: Int) -> Unit
) {
    var savePct by remember { mutableIntStateOf(20) }
    var selectedGoalId by remember(goals) { mutableStateOf(goals.firstOrNull()?.id) }
    val positiveSurplus = surplusCents.coerceAtLeast(0L)
    val saveCents = (positiveSurplus * savePct) / 100L
    val carryCents = (positiveSurplus - saveCents).coerceAtLeast(0L)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = EmeraldMain)
                Text("إغلاق ${DateUtils.formatYM(currentYM)}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "رصيد الحساب العائلي الحالي: ${MoneyFormat.formatDzd(surplusCents, privacyMode)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (surplusCents >= 0L) IncomeGreen else ExpenseRed
                )
                if (positiveSurplus > 0L && goals.isNotEmpty()) {
                    Text(
                        text = "نسبة الادخار من الفائض: $savePct%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 10, 20, 30, 50).forEach { pct ->
                            FilterChip(
                                selected = savePct == pct,
                                onClick = { savePct = pct },
                                label = { Text("$pct%") }
                            )
                        }
                    }
                    if (savePct > 0) {
                        Text("الهدف المستفيد:", style = MaterialTheme.typography.bodySmall)
                        goals.take(3).forEach { goal ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedGoalId = goal.id }
                            ) {
                                RadioButton(
                                    selected = selectedGoalId == goal.id,
                                    onClick = { selectedGoalId = goal.id }
                                )
                                Text(goal.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Text(
                        text = "• سيُحوّل للادخار: ${MoneyFormat.formatDzd(saveCents, privacyMode)}\n• سيُرحّل للشهر القادم: ${MoneyFormat.formatDzd(carryCents, privacyMode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "سيتم تثبيت أرصدة الشهر الحالي وإغلاقه رسمياً.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmClose(selectedGoalId, savePct) },
                modifier = Modifier.testTag("confirm_close_month_button")
            ) {
                Text("تأكيد الإغلاق والترحيل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    quickExpenses: List<QuickExpensePreset>,
    familyMembers: List<String>,
    privacyMode: Boolean,
    onDismiss: () -> Unit,
    onQuickPreset: (QuickExpensePreset) -> Unit,
    onAddExpense: (person: String?, item: String, amountCents: Long) -> Unit,
    onAddDebt: (creditor: String, item: String, amountCents: Long) -> Unit,
    onAddIncome: (accountId: String, name: String, amountCents: Long) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0=مصروف, 1=دين محل, 2=دخل/خياطة
    var expItem by remember { mutableStateOf("") }
    var expAmount by remember { mutableStateOf("") }
    var expPerson by remember(familyMembers) { mutableStateOf(familyMembers.firstOrNull() ?: "عام") }
    var memberDropdownOpen by remember { mutableStateOf(false) }

    var debtCreditor by remember { mutableStateOf("") }
    var debtItem by remember { mutableStateOf("") }
    var debtAmount by remember { mutableStateOf("") }

    var incAccount by remember { mutableStateOf("family") }
    var incName by remember { mutableStateOf("") }
    var incAmount by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Add,
                        contentDescription = null,
                        tint = IncomeGreen
                    )
                    Text(
                        text = "إضافة سريعة من أي مكان",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("quick_add_close_button")
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.Close,
                        contentDescription = "إغلاق"
                    )
                }
            }

            // 3 Tabs: مصروف | دين محل | دخل/خياطة
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    leadingIcon = {
                        Icon(
                            androidx.compose.material.icons.Icons.Rounded.ShoppingBasket,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("مصروف", maxLines = 1) },
                    modifier = Modifier.weight(1f).testTag("quick_tab_expense")
                )
                FilterChip(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    leadingIcon = {
                        Icon(
                            androidx.compose.material.icons.Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("دين محل", maxLines = 1) },
                    modifier = Modifier.weight(1f).testTag("quick_tab_debt")
                )
                FilterChip(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    leadingIcon = {
                        Icon(
                            androidx.compose.material.icons.Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("دخل/خياطة", maxLines = 1) },
                    modifier = Modifier.weight(1f).testTag("quick_tab_income")
                )
            }

            when (activeTab) {
                0 -> {
                    // 2-column grid of quick expense buttons
                    quickExpenses.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { preset ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 68.dp)
                                        .clickable {
                                            onQuickPreset(preset)
                                            onDismiss()
                                        }
                                        .testTag("quick_sheet_preset_${preset.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(preset.icon, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = MoneyFormat.formatDzd(preset.amountCents, privacyMode),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = IncomeGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }

                    OutlinedTextField(
                        value = expItem,
                        onValueChange = { expItem = it },
                        label = { Text("الغرض (خبز، خضر، مواصلات...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("quick_expense_item_input")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(0.9f)) {
                            OutlinedButton(
                                onClick = { memberDropdownOpen = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 54.dp)
                                    .testTag("quick_expense_member_button")
                            ) {
                                Text(expPerson, maxLines = 1)
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = memberDropdownOpen,
                                onDismissRequest = { memberDropdownOpen = false }
                            ) {
                                (listOf("عام (الخزينة)") + familyMembers).forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            expPerson = opt
                                            memberDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = expAmount,
                            onValueChange = { expAmount = it },
                            label = { Text("المبلغ (دج)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.1f).testTag("quick_expense_amount_input")
                        )
                    }

                    Button(
                        onClick = {
                            val cents = MoneyFormat.parseToCents(expAmount) ?: 0L
                            if (expItem.isNotBlank() && cents > 0L) {
                                val personArg = if (expPerson == "عام (الخزينة)") null else expPerson
                                onAddExpense(personArg, expItem, cents)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("quick_expense_submit_button")
                    ) {
                        Text("خصم المصروف من الخزينة", fontWeight = FontWeight.Bold)
                    }
                }

                1 -> {
                    OutlinedTextField(
                        value = debtCreditor,
                        onValueChange = { debtCreditor = it },
                        label = { Text("التاجر / المحل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("quick_debt_creditor_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = debtItem,
                            onValueChange = { debtItem = it },
                            label = { Text("الغرض") },
                            singleLine = true,
                            modifier = Modifier.weight(1.1f)
                        )
                        OutlinedTextField(
                            value = debtAmount,
                            onValueChange = { debtAmount = it },
                            label = { Text("المبلغ (دج)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(0.9f).testTag("quick_debt_amount_input")
                        )
                    }
                    Button(
                        onClick = {
                            val cents = MoneyFormat.parseToCents(debtAmount) ?: 0L
                            if (debtCreditor.isNotBlank() && cents > 0L) {
                                onAddDebt(debtCreditor, debtItem, cents)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("quick_debt_submit_button")
                    ) {
                        Text("تسجيل دين محل (عليه)", fontWeight = FontWeight.Bold)
                    }
                }

                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "family" to "العائلي",
                            "personal" to "الشخصي",
                            "sewing" to "الخياطة"
                        ).forEach { (id, label) ->
                            FilterChip(
                                selected = incAccount == id,
                                onClick = { incAccount = id },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = incName,
                        onValueChange = { incName = it },
                        label = { Text("البيان / مصدر الدخل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("quick_income_name_input")
                    )
                    OutlinedTextField(
                        value = incAmount,
                        onValueChange = { incAmount = it },
                        label = { Text("المبلغ (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("quick_income_amount_input")
                    )
                    Button(
                        onClick = {
                            val cents = MoneyFormat.parseToCents(incAmount) ?: 0L
                            if (incName.isNotBlank() && cents > 0L) {
                                onAddIncome(incAccount, incName, cents)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("quick_income_submit_button")
                    ) {
                        Text("تسجيل الدخل", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

