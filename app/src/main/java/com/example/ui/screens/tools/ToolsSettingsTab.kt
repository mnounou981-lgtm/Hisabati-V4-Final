package com.example.ui.screens.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
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
import com.example.domain.CatalogDefaults
import com.example.domain.MoneyFormat
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.EmeraldMain
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.SandooqiPalettes
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel

fun LazyListScope.toolsSettingsTab(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel,
    storageSummaryText: String,
    newMember: String,
    onNewMemberChange: (String) -> Unit,
    selectedFamilyModeMember: String,
    onSelectedFamilyModeMemberChange: (String) -> Unit,
    meterRate: String,
    onMeterRateChange: (String) -> Unit,
    threadRate: String,
    onThreadRateChange: (String) -> Unit,
    prodCatKey: String,
    onProdCatKeyChange: (String) -> Unit,
    prodName: String,
    onProdNameChange: (String) -> Unit,
    prodEmoji: String,
    onProdEmojiChange: (String) -> Unit,
    pinInput: String,
    onPinInputChange: (String) -> Unit,
    resetSalary: Boolean,
    onResetSalaryChange: (Boolean) -> Unit,
    resetFamilyMonth: Boolean,
    onResetFamilyMonthChange: (Boolean) -> Unit,
    resetSewingAccount: Boolean,
    onResetSewingAccountChange: (Boolean) -> Unit,
    resetHomeDebts: Boolean,
    onResetHomeDebtsChange: (Boolean) -> Unit,
    resetSewDebts: Boolean,
    onResetSewDebtsChange: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onRequestSelectiveReset: () -> Unit,
    onRequestFullReset: () -> Unit
) {
    // 1. Family Members & Restricted Entry Mode
    item {
        SandooqiCard {
            SectionHeader(
                title = "أفراد العائلة ووضع التسجيل المقيد",
                subtitle = "إدارة أفراد الأسرة وتفعيل وضع تسجيل المشتريات فقط",
                icon = Icons.Rounded.Groups
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMember,
                    onValueChange = onNewMemberChange,
                    label = { Text("إضافة اسم فرد جديد...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("new_family_member_input")
                )
                Button(
                    onClick = {
                        viewModel.addFamilyMember(newMember)
                        onNewMemberChange("")
                    },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("add_family_member_button")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة")
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.settings.familyMembers.forEach { member ->
                    InputChip(
                        selected = selectedFamilyModeMember == member,
                        onClick = { onSelectedFamilyModeMemberChange(member) },
                        label = { Text(member) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "إزالة $member",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.removeFamilyMember(member) }
                            )
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.enableFamilyEntryMode(selectedFamilyModeMember) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("enable_family_mode_settings_button")
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("تفعيل وضع أفراد العائلة لـ $selectedFamilyModeMember (تسجيل المشتريات فقط)")
            }
        }
    }

    // 2. Sewing Defaults & Custom Catalog Product Entry
    item {
        SandooqiCard {
            SectionHeader(
                title = "الأسعار الافتراضية للخياطة وقائمة المنتجات",
                icon = Icons.Rounded.ContentCut
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = meterRate,
                    onValueChange = onMeterRateChange,
                    label = { Text("تسعيرة المتر (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = threadRate,
                    onValueChange = onThreadRateChange,
                    label = { Text("تسعيرة الخيط (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.saveSewingRates(
                        MoneyFormat.parseToCents(meterRate) ?: 300_00L,
                        MoneyFormat.parseToCents(threadRate) ?: 100_00L
                    )
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("حفظ التسعيرة") }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("+ إضافة صنف جديد لقائمة المنتجات", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CatalogDefaults.categoryTabs.forEach { (key, label) ->
                    FilterChip(
                        selected = prodCatKey == key,
                        onClick = { onProdCatKeyChange(key) },
                        label = { Text(label, maxLines = 1) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = prodEmoji,
                    onValueChange = { if (it.length <= 4) onProdEmojiChange(it) },
                    label = { Text("أيقونة") },
                    singleLine = true,
                    modifier = Modifier.weight(0.55f)
                )
                OutlinedTextField(
                    value = prodName,
                    onValueChange = onProdNameChange,
                    label = { Text("اسم الصنف") },
                    singleLine = true,
                    modifier = Modifier.weight(1.45f).testTag("custom_product_name_input")
                )
                FilledTonalButton(
                    onClick = {
                        viewModel.addCustomProduct(prodName, prodEmoji, prodCatKey)
                        if (prodName.isNotBlank()) onProdNameChange("")
                    },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("add_custom_product_button")
                ) { Text("إضافة") }
            }
            if (state.settings.customProducts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.settings.customProducts.forEach { prod ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text("${prod.emoji} ${prod.name}") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "حذف ${prod.name}",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.deleteCustomProduct(prod.name, prod.categoryKey) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // 3. Appearance, Security, Storage & Backup
    item {
        SandooqiCard {
            SectionHeader(
                title = "المظهر والأمان والنسخ الاحتياطي",
                icon = Icons.Rounded.Storage
            )
            Spacer(Modifier.height(8.dp))
            Text("لون الواجهة والخلفية العلوية:", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SandooqiPalettes.all.forEach { palette ->
                    FilterChip(
                        selected = state.settings.themeStyle == palette.key,
                        onClick = { viewModel.setThemeStyle(palette.key) },
                        label = { Text(palette.label, maxLines = 1) },
                        modifier = Modifier.testTag("theme_palette_${palette.key}")
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.toggleSound() },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(if (state.settings.soundEnabled) "المؤثرات: مفعّلة" else "المؤثرات: معطّلة", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { viewModel.toggleDarkMode() },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = if (state.settings.darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.settings.darkMode) "الوضع النهاري" else "الوضع الليلي", maxLines = 1)
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("قفل التطبيق بالرمز السري عند الفتح", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("الرمز الحالي: ${state.settings.appPin}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = (if (state.settings.appLockEnabled) IncomeGreen else ExpenseRed).copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = if (state.settings.appLockEnabled) "مفعّل" else "معطّل",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (state.settings.appLockEnabled) IncomeGreen else ExpenseRed,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pinInput,
                onValueChange = { if (it.length <= 8) onPinInputChange(it) },
                label = { Text("تغيير الرمز السري للمدير (4+ أرقام)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.configureAppLock(pinInput.ifBlank { state.settings.appPin }, true) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("حفظ وتفعيل القفل") }
                OutlinedButton(
                    onClick = { viewModel.configureAppLock(pinInput, false) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text("تعطيل القفل") }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("التخزين المحلي (Room DB)", style = MaterialTheme.typography.bodySmall)
                    Text(storageSummaryText, style = MaterialTheme.typography.labelLarge, color = EmeraldMain)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExportBackup,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("export_json_backup_button")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("تنزيل نسخة JSON", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onImportBackup,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("import_json_backup_button")
                ) {
                    Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("استيراد نسخة", maxLines = 1)
                }
            }
        }
    }

    // 4. Selective & Full Reset Card
    item {
        val anyResetSelected = resetSalary || resetFamilyMonth || resetSewingAccount || resetHomeDebts || resetSewDebts
        SandooqiCard {
            SectionHeader(
                title = "التصفير الاختياري للأقسام",
                subtitle = "اختر القسم الذي تريد تصفيره فقط دون المساس ببقية البيانات",
                icon = Icons.Rounded.CleaningServices
            )
            Spacer(Modifier.height(8.dp))
            ResetCheckboxRow("الراتب الحالي (الشهر الحالي فقط)", resetSalary, onResetSalaryChange)
            ResetCheckboxRow("مصاريف ومدخول الصندوق المنزلي للشهر الحالي", resetFamilyMonth, onResetFamilyMonthChange)
            ResetCheckboxRow("حسابات الخياطة (المداخيل والمصاريف والتحويلات)", resetSewingAccount, onResetSewingAccountChange)
            ResetCheckboxRow("سجلات ديون المحلات والبيت", resetHomeDebts, onResetHomeDebtsChange)
            ResetCheckboxRow("سجلات ديون الورشة / الخياطة", resetSewDebts, onResetSewDebtsChange)
            Spacer(Modifier.height(8.dp))
            Button(
                enabled = anyResetSelected,
                onClick = onRequestSelectiveReset,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("selective_reset_button")
            ) { Text("تصفير الأقسام المحددة فقط") }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = onRequestFullReset,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("full_reset_button")
            ) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("تصفير جميع البيانات بالكامل")
            }
        }
    }
}

@Composable
private fun ResetCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
