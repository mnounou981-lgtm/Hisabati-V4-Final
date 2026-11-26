package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.DateUtils
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainNavTab

@Composable
fun SandooqiTopHeader(
    currentYM: String,
    darkMode: Boolean,
    privacyMode: Boolean,
    themeStyle: String = "navy",
    onToggleDarkMode: () -> Unit,
    onTogglePrivacy: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headerBrush = SandooqiPalettes.headerBrush(themeStyle, darkMode)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(headerBrush)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Right (Start in RTL): "ص" emblem + Brand Title + Subtitle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                    ) {
                        Text(
                            text = "ص",
                            color = GoldBadgeBg,
                            fontFamily = CairoDisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "صندوقي",
                            color = Color.White,
                            fontFamily = CairoDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "إدارة مالية ذكية • ${DateUtils.formatYM(currentYM)}",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Left (End in RTL): Dark Mode, Privacy, Voice, Search
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HeaderActionButton(
                        icon = if (darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = if (darkMode) "الوضع النهاري" else "الوضع الليلي",
                        testTag = "header_dark_mode_button",
                        isActive = darkMode,
                        onClick = onToggleDarkMode
                    )
                    HeaderActionButton(
                        icon = if (privacyMode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = "إخفاء المبالغ",
                        testTag = "header_privacy_button",
                        isActive = privacyMode,
                        onClick = onTogglePrivacy
                    )
                    HeaderActionButton(
                        icon = Icons.Rounded.Mic,
                        contentDescription = "إدخال صوتي سريع",
                        testTag = "header_voice_button",
                        isActive = false,
                        onClick = onOpenVoice
                    )
                    HeaderActionButton(
                        icon = Icons.Rounded.Search,
                        contentDescription = "بحث شامل",
                        testTag = "header_search_button",
                        isActive = false,
                        onClick = onOpenSearch
                    )
                }
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 0.8.dp)
        }
    }
}

@Composable
private fun HeaderActionButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isActive) GoldBadgeBg.copy(alpha = 0.28f)
                else Color.White.copy(alpha = 0.14f)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) GoldBadgeBg else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SandooqiBottomNav(
    selectedTab: MainNavTab,
    onSelectTab: (MainNavTab) -> Unit,
    familyEntryOnlyMode: Boolean,
    modifier: Modifier = Modifier
) {
    val row1 = listOf(MainNavTab.HOME, MainNavTab.TREASURY, MainNavTab.EXPENSES, MainNavTab.DEBTS)
    val row2 = listOf(MainNavTab.BUDGET, MainNavTab.SEWING, MainNavTab.REPORTS, MainNavTab.TOOLS)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 10.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.7.dp)
            NavGridRow(
                tabs = row1,
                selectedTab = selectedTab,
                familyEntryOnlyMode = familyEntryOnlyMode,
                onSelectTab = onSelectTab
            )
            NavGridRow(
                tabs = row2,
                selectedTab = selectedTab,
                familyEntryOnlyMode = familyEntryOnlyMode,
                onSelectTab = onSelectTab
            )
        }
    }
}

@Composable
private fun NavGridRow(
    tabs: List<MainNavTab>,
    selectedTab: MainNavTab,
    familyEntryOnlyMode: Boolean,
    onSelectTab: (MainNavTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (tab in tabs) {
            val isSelected = tab == selectedTab
            val lockedOut = familyEntryOnlyMode && tab != MainNavTab.EXPENSES
            val activeBg = MaterialTheme.colorScheme.primaryContainer
            val activeTint = MaterialTheme.colorScheme.primary
            val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (lockedOut) 0.4f else 0.9f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) activeBg else Color.Transparent)
                    .clickable(enabled = !lockedOut) { onSelectTab(tab) }
                    .padding(vertical = 5.dp, horizontal = 2.dp)
                    .testTag("nav_tab_${tab.route}")
            ) {
                Icon(
                    imageVector = tabIcon(tab, isSelected),
                    contentDescription = tab.label,
                    tint = if (isSelected) activeTint else inactiveTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    color = if (isSelected) activeTint else inactiveTint,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun tabIcon(tab: MainNavTab, selected: Boolean): ImageVector = when (tab) {
    MainNavTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
    MainNavTab.TREASURY -> if (selected) Icons.Rounded.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
    MainNavTab.EXPENSES -> if (selected) Icons.Rounded.ShoppingBasket else Icons.Outlined.ShoppingBasket
    MainNavTab.DEBTS -> if (selected) Icons.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong
    MainNavTab.BUDGET -> if (selected) Icons.Rounded.Savings else Icons.Outlined.Savings
    MainNavTab.SEWING -> if (selected) Icons.Rounded.ContentCut else Icons.Outlined.ContentCut
    MainNavTab.REPORTS -> if (selected) Icons.Rounded.BarChart else Icons.Outlined.BarChart
    MainNavTab.TOOLS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
}
