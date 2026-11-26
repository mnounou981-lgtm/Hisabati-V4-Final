package com.example.ui.screens.tools

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.WarrantyEntity
import com.example.domain.DateUtils
import com.example.domain.MoneyFormat
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SandooqiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldAccent
import com.example.ui.viewmodel.SandooqiUiState
import com.example.ui.viewmodel.SandooqiViewModel

fun LazyListScope.toolsAccountsWarrantiesTab(
    state: SandooqiUiState,
    viewModel: SandooqiViewModel,
    privacy: Boolean,
    personalOpening: String,
    onPersonalOpeningChange: (String) -> Unit,
    sewingOpening: String,
    onSewingOpeningChange: (String) -> Unit,
    wDevice: String,
    onWDeviceChange: (String) -> Unit,
    wStore: String,
    onWStoreChange: (String) -> Unit,
    wExpiry: String,
    onWExpiryChange: (String) -> Unit,
    wPrice: String,
    onWPriceChange: (String) -> Unit,
    wPhotoUri: String,
    onPickWarrantyPhoto: () -> Unit,
    onClearWarrantyPhoto: () -> Unit
) {
    item {
        SandooqiCard {
            SectionHeader(
                title = "الأرصدة الافتتاحية للحسابات المستقلة",
                subtitle = "ضبط رصيد البداية التراكمي للشخصي والخياطة",
                icon = Icons.Rounded.AccountBalanceWallet
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = personalOpening,
                    onValueChange = onPersonalOpeningChange,
                    label = { Text("افتتاحي الشخصي (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = sewingOpening,
                    onValueChange = onSewingOpeningChange,
                    label = { Text("افتتاحي الخياطة (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = {
                    val p = MoneyFormat.parseToCents(personalOpening) ?: 0L
                    val s = MoneyFormat.parseToCents(sewingOpening) ?: 0L
                    viewModel.saveAccountOpenings(p, s)
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("حفظ الأرصدة الافتتاحية") }
        }
    }

    item {
        SandooqiCard {
            SectionHeader(title = "أرشيف الضمانات والفواتير", icon = Icons.Rounded.VerifiedUser)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = wDevice,
                    onValueChange = onWDeviceChange,
                    label = { Text("اسم الجهاز") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = wStore,
                    onValueChange = onWStoreChange,
                    label = { Text("المحل") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = wExpiry,
                    onValueChange = onWExpiryChange,
                    label = { Text("انتهاء الضمان (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = wPrice,
                    onValueChange = onWPriceChange,
                    label = { Text("السعر (دج)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPickWarrantyPhoto,
                    modifier = Modifier.weight(1f)
                ) { Text(if (wPhotoUri.isBlank()) "صورة شهادة الضمان" else "تغيير الصورة") }
                if (wPhotoUri.isNotBlank()) {
                    AsyncImage(
                        model = wPhotoUri,
                        contentDescription = "صورة الضمان",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val c = MoneyFormat.parseToCents(wPrice) ?: 0L
                    viewModel.addWarranty(
                        WarrantyEntity(
                            deviceName = wDevice,
                            store = wStore,
                            purchaseDateISO = DateUtils.todayISO(),
                            expiryDateISO = wExpiry,
                            priceCents = c,
                            photoUri = wPhotoUri
                        )
                    )
                    if (wDevice.isNotBlank()) {
                        onWDeviceChange("")
                        onWStoreChange("")
                        onWPriceChange("")
                        onClearWarrantyPhoto()
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text("حفظ الضمان") }
        }
    }

    if (state.warranties.isEmpty()) {
        item {
            EmptyStateCard(
                title = "لا توجد ضمانات محفوظة",
                description = "احفظ فواتير الأجهزة الكهربائية والضمانات لتنبيهك قبل انتهائها.",
                icon = Icons.Rounded.VerifiedUser
            )
        }
    } else {
        items(state.warranties, key = { it.id }) { w ->
            val daysLeft = DateUtils.daysBetweenISO(DateUtils.todayISO(), w.expiryDateISO)
            SandooqiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(w.deviceName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (daysLeft != null) {
                                if (daysLeft >= 0) "متبقي $daysLeft يوم (ينتهي ${w.expiryDateISO})"
                                else "منتهي منذ ${-daysLeft} يوم"
                            } else "ينتهي ${w.expiryDateISO}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (daysLeft != null && daysLeft < 30) ExpenseRed else GoldAccent
                        )
                        if (w.priceCents > 0L) {
                            Text(MoneyFormat.formatDzd(w.priceCents, privacy), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = { viewModel.deleteWarranty(w.id) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف")
                    }
                }
            }
        }
    }
}
