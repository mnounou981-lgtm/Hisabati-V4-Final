package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.DateUtils
import com.example.domain.MoneyFormat
import com.example.ui.screens.tools.toolsAccountsWarrantiesTab
import com.example.ui.screens.tools.toolsAnalyticsTab
import com.example.ui.screens.tools.toolsSettingsTab
import com.example.ui.screens.tools.toolsSimulatorTab
import com.example.ui.theme.ExpenseRed
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel

@Composable
fun ToolsScreen(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel
) {
    val context = LocalContext.current
    val privacy = state.settings.privacyMode
    var subTab by remember { mutableIntStateOf(1) } // 0=التحليلات, 1=المحاكى والسريع, 2=الإعدادات والأمان, 3=الحسابات والضمانات

    // Simulator & Quick buttons state
    var simItem by remember { mutableStateOf("") }
    var simPrice by remember { mutableStateOf("") }
    var simAccount by remember { mutableStateOf("family") }
    var quickIcon by remember { mutableStateOf("🛍️") }
    var quickName by remember { mutableStateOf("") }
    var quickAmount by remember { mutableStateOf("") }

    // Price Check Form
    var pcProduct by remember { mutableStateOf("") }
    var pcStore by remember { mutableStateOf("") }
    var pcPrice by remember { mutableStateOf("") }

    // Accounts & Warranty Form
    var personalOpening by remember(state.settings.personalOpeningCents) {
        mutableStateOf(MoneyFormat.formatNumberOnly(state.settings.personalOpeningCents))
    }
    var sewingOpening by remember(state.settings.sewingOpeningCents) {
        mutableStateOf(MoneyFormat.formatNumberOnly(state.settings.sewingOpeningCents))
    }
    var wDevice by remember { mutableStateOf("") }
    var wStore by remember { mutableStateOf("") }
    var wExpiry by remember { mutableStateOf(DateUtils.shiftYM(state.currentYM, 12) + "-01") }
    var wPrice by remember { mutableStateOf("") }
    var wPhotoUri by remember { mutableStateOf("") }

    // Settings
    var newMember by remember { mutableStateOf("") }
    var selectedFamilyModeMember by remember(state.settings.familyMembers) {
        mutableStateOf(state.settings.familyMembers.firstOrNull() ?: "إسلام")
    }
    var meterRate by remember(state.settings.meterPriceCents) {
        mutableStateOf(MoneyFormat.formatNumberOnly(state.settings.meterPriceCents))
    }
    var threadRate by remember(state.settings.threadPriceCents) {
        mutableStateOf(MoneyFormat.formatNumberOnly(state.settings.threadPriceCents))
    }
    var prodCatKey by remember { mutableStateOf("veg") }
    var prodName by remember { mutableStateOf("") }
    var prodEmoji by remember { mutableStateOf("🛍️") }
    var pinInput by remember { mutableStateOf("") }

    // Selective reset checkboxes
    var resetSalary by remember { mutableStateOf(false) }
    var resetFamilyMonth by remember { mutableStateOf(false) }
    var resetSewingAccount by remember { mutableStateOf(false) }
    var resetHomeDebts by remember { mutableStateOf(false) }
    var resetSewDebts by remember { mutableStateOf(false) }
    var showSelectiveResetConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val dbSizeBytes = remember(state.transactions.size, state.debts.size, state.orders.size) {
        runCatching { context.getDatabasePath("sandooqi_local.db").length() }.getOrDefault(0L)
    }
    val storageSummaryText = remember(dbSizeBytes) {
        if (dbSizeBytes >= 1024L * 1024L) {
            String.format("%.2f MB", dbSizeBytes / (1024.0 * 1024.0))
        } else {
            "${(dbSizeBytes / 1024L).coerceAtLeast(1L)} KB"
        }
    }

    val warrantyPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) wPhotoUri = uri.toString() }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = viewModel.exportJsonBackup()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                viewModel.showToast("تم حفظ النسخة الاحتياطية بنجاح")
            } catch (_: Exception) {
                viewModel.showToast("تعذر حفظ ملف النسخة الاحتياطية")
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                viewModel.importJsonBackup(raw)
            } catch (_: Exception) {
                viewModel.showToast("تعذر قراءة ملف النسخة الاحتياطية")
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = subTab == 0,
                        onClick = { subTab = 0 },
                        label = { Text("التحليلات", maxLines = 1) },
                        modifier = Modifier.weight(1f).testTag("tools_subtab_0")
                    )
                    FilterChip(
                        selected = subTab == 1,
                        onClick = { subTab = 1 },
                        label = { Text("المحاكى والسريع", maxLines = 1) },
                        modifier = Modifier.weight(1f).testTag("tools_subtab_1")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = subTab == 2,
                        onClick = { subTab = 2 },
                        label = { Text("الإعدادات والأمان", maxLines = 1) },
                        modifier = Modifier.weight(1f).testTag("tools_subtab_2")
                    )
                    FilterChip(
                        selected = subTab == 3,
                        onClick = { subTab = 3 },
                        label = { Text("الحسابات والضمانات", maxLines = 1) },
                        modifier = Modifier.weight(1f).testTag("tools_subtab_3")
                    )
                }
            }
        }

        when (subTab) {
            0 -> toolsAnalyticsTab(
                state = state,
                viewModel = viewModel,
                privacy = privacy,
                pcProduct = pcProduct,
                onPcProductChange = { pcProduct = it },
                pcStore = pcStore,
                onPcStoreChange = { pcStore = it },
                pcPrice = pcPrice,
                onPcPriceChange = { pcPrice = it }
            )

            1 -> toolsSimulatorTab(
                state = state,
                viewModel = viewModel,
                privacy = privacy,
                simItem = simItem,
                onSimItemChange = { simItem = it },
                simPrice = simPrice,
                onSimPriceChange = { simPrice = it },
                simAccount = simAccount,
                onSimAccountChange = { simAccount = it },
                quickIcon = quickIcon,
                onQuickIconChange = { quickIcon = it },
                quickName = quickName,
                onQuickNameChange = { quickName = it },
                quickAmount = quickAmount,
                onQuickAmountChange = { quickAmount = it }
            )

            2 -> toolsSettingsTab(
                state = state,
                viewModel = viewModel,
                storageSummaryText = storageSummaryText,
                newMember = newMember,
                onNewMemberChange = { newMember = it },
                selectedFamilyModeMember = selectedFamilyModeMember,
                onSelectedFamilyModeMemberChange = { selectedFamilyModeMember = it },
                meterRate = meterRate,
                onMeterRateChange = { meterRate = it },
                threadRate = threadRate,
                onThreadRateChange = { threadRate = it },
                prodCatKey = prodCatKey,
                onProdCatKeyChange = { prodCatKey = it },
                prodName = prodName,
                onProdNameChange = { prodName = it },
                prodEmoji = prodEmoji,
                onProdEmojiChange = { prodEmoji = it },
                pinInput = pinInput,
                onPinInputChange = { pinInput = it },
                resetSalary = resetSalary,
                onResetSalaryChange = { resetSalary = it },
                resetFamilyMonth = resetFamilyMonth,
                onResetFamilyMonthChange = { resetFamilyMonth = it },
                resetSewingAccount = resetSewingAccount,
                onResetSewingAccountChange = { resetSewingAccount = it },
                resetHomeDebts = resetHomeDebts,
                onResetHomeDebtsChange = { resetHomeDebts = it },
                resetSewDebts = resetSewDebts,
                onResetSewDebtsChange = { resetSewDebts = it },
                onExportBackup = { exportJsonLauncher.launch("Sandooqi_Backup_${DateUtils.todayISO()}.json") },
                onImportBackup = { importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                onRequestSelectiveReset = { showSelectiveResetConfirm = true },
                onRequestFullReset = { showResetConfirm = true }
            )

            else -> toolsAccountsWarrantiesTab(
                state = state,
                viewModel = viewModel,
                privacy = privacy,
                personalOpening = personalOpening,
                onPersonalOpeningChange = { personalOpening = it },
                sewingOpening = sewingOpening,
                onSewingOpeningChange = { sewingOpening = it },
                wDevice = wDevice,
                onWDeviceChange = { wDevice = it },
                wStore = wStore,
                onWStoreChange = { wStore = it },
                wExpiry = wExpiry,
                onWExpiryChange = { wExpiry = it },
                wPrice = wPrice,
                onWPriceChange = { wPrice = it },
                wPhotoUri = wPhotoUri,
                onPickWarrantyPhoto = {
                    warrantyPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClearWarrantyPhoto = { wPhotoUri = "" }
            )
        }
    }

    if (showSelectiveResetConfirm) {
        AlertDialog(
            onDismissRequest = { showSelectiveResetConfirm = false },
            title = { Text("تصفير الأقسام المحددة؟") },
            text = { Text("سيتم تصفير الأقسام التي اخترتها فقط مع الحفاظ على باقي بيانات التطبيق.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    onClick = {
                        viewModel.selectiveReset(
                            resetSalary = resetSalary,
                            resetFamilyMonth = resetFamilyMonth,
                            resetSewingAccount = resetSewingAccount,
                            resetHomeDebts = resetHomeDebts,
                            resetSewDebts = resetSewDebts
                        )
                        resetSalary = false
                        resetFamilyMonth = false
                        resetSewingAccount = false
                        resetHomeDebts = false
                        resetSewDebts = false
                        showSelectiveResetConfirm = false
                    }
                ) { Text("نعم، صفّر المحدد") }
            },
            dismissButton = { TextButton(onClick = { showSelectiveResetConfirm = false }) { Text("إلغاء") } }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("تصفير جميع البيانات؟") },
            text = { Text("سيتم حذف جميع المعاملات والديون والطلبيات. يُنصح بتصدير نسخة احتياطية أولاً.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    onClick = {
                        viewModel.resetAllData()
                        showResetConfirm = false
                    }
                ) { Text("نعم، صفّر البيانات") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("إلغاء") } }
        )
    }
}
