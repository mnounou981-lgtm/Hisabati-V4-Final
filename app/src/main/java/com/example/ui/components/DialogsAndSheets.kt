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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
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
