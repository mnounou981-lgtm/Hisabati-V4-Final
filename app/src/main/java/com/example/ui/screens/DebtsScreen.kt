package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
    val context = LocalContext.current
    val privacy = state.settings.privacyMode
    val today = remember { DateUtils.todayISO() }
    var subTab by remember { mutableIntStateOf(0) } // 0=Shops/Rent, 1=Bills/Obligations, 2=Fac/Deductions, 3=Family
    var payingCreditor by remember { mutableStateOf<Pair<CreditorGroupSummary, Boolean>?>(null) }
    var payAmountText by remember { mutableStateOf("") }
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }

    // Manual Debt inputs (عليه / له)
    var debtIsReceivable by remember { mutableStateOf(false) }
    var debtCreditor by remember { mutableStateOf("") }
    var debtItem by remember { mutableStateOf("") }
    var debtAmount by remember { mutableStateOf("") }
    var debtDue by remember { mutableStateOf("") }
    var debtActivity by remember { mutableStateOf(CatalogDefaults.defaultDebtActivities.first()) }
    var debtPhotoUri by remember { mutableStateOf("") }
    var showAddActivityDialog by remember { mutableStateOf(false) }
    var newActivityLabel by remember { mutableStateOf("") }

    val allDebtActivities = remember(state.settings.customDebtActivities) {
        (CatalogDefaults.defaultDebtActivities + state.settings.customDebtActivities).distinct()
    }

    var debtSearch by remember { mutableStateOf("") }
    var debtDirFilter by remember { mutableStateOf("all") } // "all", "out", "in"

    val debtPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            debtPhotoUri = uri.toString()
        }
    }

    // Rent inputs
    var rentExpanded by remember { mutableStateOf(false) }
    var rentFilter by remember { mutableStateOf("all") } // "all", "in", "out"
    var rentUnit by remember { mutableStateOf(CatalogDefaults.rentUnits.first()) }
    var rentParty by remember { mutableStateOf("") }
    var rentAmount by remember { mutableStateOf("") }
    var rentDue by remember { mutableStateOf("") }
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

    val allRentRecords = remember(state.debts) {
        state.debts.filter { it.kind == "rent_in" || it.kind == "rent_out" }
    }
    val openRentPayablesCents = remember(allRentRecords) {
        allRentRecords.filter { it.kind == "rent_out" && !it.settled }
            .sumOf { (it.amountCents - it.paidCents).coerceAtLeast(0L) }
    }
    val recentCreditors = remember(state.debts) {
        state.debts.map { it.creditor.trim() }.filter { it.isNotEmpty() }.distinct().take(6)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp)
        ) {
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

        AnimatedContent(
            targetState = subTab,
            transitionSpec = {
                (fadeIn(tween(durationMillis = 400, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(durationMillis = 400, easing = FastOutSlowInEasing)) { it / 20 })
                    .togetherWith(fadeOut(tween(durationMillis = 240, easing = FastOutSlowInEasing)))
            },
            modifier = Modifier.weight(1f),
            label = "debts_subtab_transition"
        ) { targetSubTab ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (targetSubTab) {
            0 -> {
                // Top 3 Summary Tiles
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(
                            label = "ديون مفتوحة (عليه)",
                            value = MoneyFormat.formatDzd(state.openHomeAndBillDebtsCents, privacy),
                            accentColor = ExpenseRed,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "مستحقات وكراء (له)",
                            value = MoneyFormat.formatDzd(state.openRentReceivablesCents, privacy),
                            accentColor = IncomeGreen,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "كراء للدفع (عليه)",
                            value = MoneyFormat.formatDzd(openRentPayablesCents, privacy),
                            accentColor = GoldAccent,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Collapsible Rent section (له وعليه)
                item {
                    SandooqiCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { rentExpanded = !rentExpanded }
                                .testTag("toggle_rent_section"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.HomeWork, contentDescription = null, tint = EmeraldMain)
                                Column {
                                    Text(
                                        text = "قسم الكراء (سكن / محل) — له وعليه (${allRentRecords.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (rentExpanded) "اضغط للطي" else "اضغط لتسجيل أو متابعة الكراء",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (rentExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (rentExpanded) "طي" else "توسيع"
                            )
                        }

                        AnimatedVisibility(
                            visible = rentExpanded,
                            enter = expandVertically(tween(durationMillis = 380, easing = FastOutSlowInEasing)) +
                                fadeIn(tween(durationMillis = 320)),
                            exit = shrinkVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                                fadeOut(tween(durationMillis = 240))
                        ) {
                            Column {
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = rentIsReceivable,
                                        onClick = { rentIsReceivable = true },
                                        label = { Text("كراء له (إيجار لي)", maxLines = 1) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = !rentIsReceivable,
                                        onClick = { rentIsReceivable = false },
                                        label = { Text("كراء عليه (التزام)", maxLines = 1) },
                                        modifier = Modifier.weight(1f)
                                    )
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
                                    OutlinedTextField(
                                        value = rentParty,
                                        onValueChange = { rentParty = it },
                                        label = { Text("المستأجر / المؤجر") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
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
                                OutlinedTextField(
                                    value = rentDue,
                                    onValueChange = { rentDue = it },
                                    label = { Text("تاريخ الاستحقاق (اختياري YYYY-MM-DD)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { rentImmediate = !rentImmediate }
                                ) {
                                    Checkbox(checked = rentImmediate, onCheckedChange = { rentImmediate = it })
                                    Text("تحصيل / دفع فوري في الخزينة العائلية", style = MaterialTheme.typography.bodySmall)
                                }
                                Button(
                                    onClick = {
                                        val c = MoneyFormat.parseToCents(rentAmount) ?: 0L
                                        viewModel.addRentRecord(rentUnit, rentParty, c, rentIsReceivable, rentImmediate, rentDue)
                                        if (rentParty.isNotBlank() && c > 0L) {
                                            rentParty = ""; rentAmount = ""; rentDue = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("save_rent_button")
                                ) { Text("حفظ حركة الكراء") }

                                if (allRentRecords.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("all" to "الكل", "in" to "له", "out" to "عليه").forEach { (k, lbl) ->
                                            FilterChip(
                                                selected = rentFilter == k,
                                                onClick = { rentFilter = k },
                                                label = { Text(lbl, maxLines = 1) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    val visibleRents = when (rentFilter) {
                                        "in" -> allRentRecords.filter { it.kind == "rent_in" }
                                        "out" -> allRentRecords.filter { it.kind == "rent_out" }
                                        else -> allRentRecords
                                    }
                                    visibleRents.forEach { r ->
                                        val isRec = r.kind == "rent_in"
                                        val remRent = (r.amountCents - r.paidCents).coerceAtLeast(0L)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${if (isRec) "كراء له" else "كراء عليه"}: ${r.creditor} (${r.rentUnit.ifBlank { r.item }})",
                                                        style = MaterialTheme.typography.titleSmall
                                                    )
                                                    Text(
                                                        text = if (r.settled) {
                                                            if (isRec) "مُحصّل • ${MoneyFormat.formatDzd(r.amountCents, privacy)}"
                                                            else "مسدد • ${MoneyFormat.formatDzd(r.amountCents, privacy)}"
                                                        } else {
                                                            "قيد الانتظار • ${MoneyFormat.formatDzd(remRent, privacy)}"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (r.settled) MaterialTheme.colorScheme.onSurfaceVariant else if (isRec) IncomeGreen else ExpenseRed,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (!r.settled) {
                                                        OutlinedButton(
                                                            onClick = { viewModel.collectRentReceivable(r) },
                                                            modifier = Modifier.testTag("settle_rent_${r.id}")
                                                        ) {
                                                            Text(if (isRec) "تحصيل" else "تسديد")
                                                        }
                                                    }
                                                    IconButton(onClick = { viewModel.deleteDebt(r.id) }) {
                                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف حركة الكراء")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Enhanced Debt Entry Card (له / عليه + Photo + Recent Creditors + Activity)
                item {
                    SandooqiCard {
                        SectionHeader(
                            title = "تسجيل دين (له / عليه) — المحلات والأشخاص",
                            subtitle = "سجّل ما عليك للمحلات أو ما لك عند الأشخاص مع صورة الورقة",
                            icon = Icons.Rounded.Storefront
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !debtIsReceivable,
                                onClick = { debtIsReceivable = false },
                                label = { Text("عليه (دين لي دفعه)", maxLines = 1) },
                                modifier = Modifier.weight(1f).testTag("debt_dir_out_chip")
                            )
                            FilterChip(
                                selected = debtIsReceivable,
                                onClick = { debtIsReceivable = true },
                                label = { Text("له (مستحق لي)", maxLines = 1) },
                                modifier = Modifier.weight(1f).testTag("debt_dir_in_chip")
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = debtCreditor,
                            onValueChange = { debtCreditor = it },
                            label = { Text(if (debtIsReceivable) "اسم الشخص / الجهة المدينة لك" else "التاجر / المحل / الدائن") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("debt_creditor_input")
                        )
                        if (recentCreditors.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                recentCreditors.forEach { rc ->
                                    AssistChip(
                                        onClick = { debtCreditor = rc },
                                        label = { Text(rc) }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "قائمة اختيارات التصنيف (اختر أو أضف تسمية):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allDebtActivities.forEach { cat ->
                                FilterChip(
                                    selected = debtActivity == cat,
                                    onClick = { debtActivity = cat },
                                    label = { Text(cat) }
                                )
                            }
                            AssistChip(
                                onClick = {
                                    newActivityLabel = ""
                                    showAddActivityDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = "إضافة تسمية جديدة",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("تسمية جديدة") },
                                modifier = Modifier.testTag("add_debt_activity_chip")
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = debtItem,
                                onValueChange = { debtItem = it },
                                label = { Text("الغرض / البيان") },
                                singleLine = true,
                                modifier = Modifier.weight(1.1f).testTag("debt_item_input")
                            )
                            OutlinedTextField(
                                value = debtAmount,
                                onValueChange = { debtAmount = it },
                                label = { Text("المبلغ (دج)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(0.9f).testTag("debt_amount_input")
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    debtPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.heightIn(min = 52.dp).testTag("debt_photo_picker_button")
                            ) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("صورة ورقة الكريدي", maxLines = 1)
                            }
                            OutlinedTextField(
                                value = debtDue,
                                onValueChange = { debtDue = it },
                                label = { Text("تاريخ الاستحقاق (اختياري)") },
                                placeholder = { Text("YYYY-MM-DD") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("debt_due_input")
                            )
                        }
                        if (debtPhotoUri.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AsyncImage(
                                    model = debtPhotoUri,
                                    contentDescription = "صورة ورقة الكريدي",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                TextButton(onClick = { debtPhotoUri = "" }) {
                                    Text("إزالة الصورة")
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val cents = MoneyFormat.parseToCents(debtAmount) ?: 0L
                                viewModel.addManualHomeDebt(
                                    creditor = debtCreditor,
                                    item = debtItem,
                                    amountCents = cents,
                                    dueISO = debtDue,
                                    activity = debtActivity,
                                    photoUri = debtPhotoUri,
                                    isReceivable = debtIsReceivable
                                )
                                if (debtCreditor.isNotBlank() && cents > 0L) {
                                    debtCreditor = ""; debtItem = ""; debtAmount = ""; debtDue = ""; debtPhotoUri = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (debtIsReceivable) IncomeGreen else ExpenseRed
                            ),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add_manual_debt_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (debtIsReceivable) "إضافة مستحق (له)" else "إضافة دين (عليه)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Search & Filter Card
                item {
                    SandooqiCard {
                        OutlinedTextField(
                            value = debtSearch,
                            onValueChange = { debtSearch = it },
                            label = { Text("بحث باسم الجهة أو الغرض...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("debt_search_input")
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "all" to "الكل",
                                "out" to "عليه",
                                "in" to "له"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = debtDirFilter == key,
                                    onClick = { debtDirFilter = key },
                                    label = { Text(label, maxLines = 1) },
                                    modifier = Modifier.weight(1f).testTag("debt_filter_$key")
                                )
                            }
                        }
                    }
                }

                val combinedGroups = buildList {
                    if (debtDirFilter == "all" || debtDirFilter == "out") {
                        state.creditorGroups.forEach { add(it to false) }
                    }
                    if (debtDirFilter == "all" || debtDirFilter == "in") {
                        state.receivableGroups.forEach { add(it to true) }
                    }
                }.filter { (group, _) ->
                    val q = debtSearch.trim()
                    if (q.isEmpty()) true
                    else group.creditor.contains(q, ignoreCase = true) ||
                        group.debts.any { it.item.contains(q, ignoreCase = true) || it.activity.contains(q, ignoreCase = true) }
                }

                if (combinedGroups.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = "لا توجد ديون أو مستحقات مطابقة",
                            description = "جميع الديون المسجلة مسددة أو لا توجد نتائج مطابقة للبحث.",
                            icon = Icons.Rounded.CheckCircle
                        )
                    }
                } else {
                    items(
                        items = combinedGroups,
                        key = { (group, isRec) -> "${if (isRec) "in" else "out"}_${group.creditor}" }
                    ) { (group, isRec) ->
                        var expanded by remember { mutableStateOf(true) }
                        val accentColor = if (isRec) IncomeGreen else ExpenseRed
                        val activityTag = group.debts.firstOrNull()?.activity?.takeIf { it.isNotBlank() } ?: "عام"
                        SandooqiCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(group.creditor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Surface(
                                            color = accentColor.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (isRec) "له" else "عليه",
                                                color = accentColor,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = activityTag,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "(${group.debts.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "الأصلي: ${MoneyFormat.formatDzd(group.totalAmountCents, privacy)} • المدفوع: ${MoneyFormat.formatDzd(group.paidCents, privacy)} • المتبقي ${MoneyFormat.formatDzd(group.remainingCents, privacy)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (group.remainingCents > 0L) accentColor else IncomeGreen
                                    )
                                }
                            }
                            if (group.remainingCents > 0L) {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        payingCreditor = group to isRec
                                        payAmountText = MoneyFormat.formatNumberOnly(group.remainingCents)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRec) IncomeGreen else MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                                ) {
                                    Icon(Icons.Rounded.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isRec) "تحصيل دفعة" else "تسديد دفعة")
                                }
                            }
                            AnimatedVisibility(
                                visible = expanded,
                                enter = expandVertically(tween(durationMillis = 380, easing = FastOutSlowInEasing)) +
                                    fadeIn(tween(durationMillis = 320)),
                                exit = shrinkVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                                    fadeOut(tween(durationMillis = 240))
                            ) {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                    group.debts.forEach { d ->
                                        val remItem = (d.amountCents - d.paidCents).coerceAtLeast(0L)
                                        val overdueDays = if (remItem > 0L && d.dueDateISO.isNotBlank()) {
                                            (DateUtils.daysBetweenISO(d.dueDateISO, today) ?: 0).coerceAtLeast(0)
                                        } else 0
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${d.item} • ${d.activity} • ${d.dateISO}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "${MoneyFormat.formatDzd(d.amountCents, privacy)} (مدفوع ${MoneyFormat.formatDzd(d.paidCents, privacy)}) • متبقي ${MoneyFormat.formatDzd(remItem, privacy)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (remItem > 0L) accentColor else IncomeGreen
                                                    )
                                                    if (overdueDays > 0) {
                                                        Text(
                                                            text = "(متأخر ${overdueDays}ي)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = GoldAccent,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                if (d.photoUri.isNotBlank()) {
                                                    Spacer(Modifier.height(4.dp))
                                                    AsyncImage(
                                                        model = d.photoUri,
                                                        contentDescription = "صورة ورقة الكريدي",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { previewPhotoUri = d.photoUri }
                                                    )
                                                }
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (remItem > 0L) {
                                                    OutlinedButton(
                                                        onClick = { viewModel.paySingleDebt(d) },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.heightIn(min = 40.dp).testTag("pay_single_debt_${d.id}")
                                                    ) {
                                                        Text(if (isRec) "تحصيل البند" else "تسديد البند", maxLines = 1)
                                                    }
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
    }
    }

    if (showAddActivityDialog) {
        AlertDialog(
            onDismissRequest = { showAddActivityDialog = false },
            title = { Text("إضافة تسمية دين جديدة") },
            text = {
                OutlinedTextField(
                    value = newActivityLabel,
                    onValueChange = { newActivityLabel = it },
                    label = { Text("اسم التصنيف (مثل: مخبزة، حلاقة، كوزميتيك)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_debt_activity_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = newActivityLabel.trim()
                        if (cleaned.isNotEmpty()) {
                            viewModel.addCustomDebtActivity(cleaned)
                            debtActivity = cleaned
                            showAddActivityDialog = false
                            newActivityLabel = ""
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_debt_activity_button")
                ) { Text("إضافة") }
            },
            dismissButton = {
                TextButton(onClick = { showAddActivityDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (payingCreditor != null) {
        val (group, isRec) = payingCreditor!!
        AlertDialog(
            onDismissRequest = { payingCreditor = null },
            title = { Text(if (isRec) "تحصيل مستحق: ${group.creditor}" else "تسديد دين: ${group.creditor}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المتبقي: ${MoneyFormat.formatDzd(group.remainingCents, privacy)}")
                    OutlinedTextField(
                        value = payAmountText,
                        onValueChange = { payAmountText = it },
                        label = { Text(if (isRec) "المبلغ المحصّل (دج)" else "المبلغ المدفوع (دج)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val cents = MoneyFormat.parseToCents(payAmountText) ?: 0L
                    viewModel.payCreditorAmount(group.creditor, cents, isRec)
                    payingCreditor = null
                }) { Text(if (isRec) "إضافة للخزينة" else "تسديد من الخزينة") }
            },
            dismissButton = { TextButton(onClick = { payingCreditor = null }) { Text("إلغاء") } }
        )
    }

    if (!previewPhotoUri.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { previewPhotoUri = null },
            title = { Text("صورة ورقة الكريدي") },
            text = {
                AsyncImage(
                    model = previewPhotoUri,
                    contentDescription = "معاينة ورقة الكريدي",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            },
            confirmButton = {
                TextButton(onClick = { previewPhotoUri = null }) { Text("إغلاق") }
            }
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
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "plan_progress_${plan.id}"
    )

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
                progress = { animatedProgress },
                color = if (isCompleted) IncomeGreen else badgeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )
        }
    }
}

