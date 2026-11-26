package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.GlobalSearchDialog
import com.example.ui.components.MonthCloseDialog
import com.example.ui.components.PinLockOverlay
import com.example.ui.components.SandooqiBottomNav
import com.example.ui.components.SandooqiTopHeader
import com.example.ui.components.VoiceEntryDialog
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.DebtsScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SewingScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.TreasuryScreen
import com.example.ui.theme.SandooqiTheme
import com.example.ui.viewmodel.MainNavTab
import com.example.ui.viewmodel.SandooqiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SandooqiApp()
        }
    }
}

@Composable
fun SandooqiApp(viewModel: SandooqiViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.toastMessage) {
        val msg = state.toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    SandooqiTheme(darkTheme = state.settings.darkMode) {
        if (state.isAppLocked) {
            PinLockOverlay(onUnlock = { viewModel.unlockApp(it) })
            return@SandooqiTheme
        }

        BackHandler(enabled = state.selectedTab != MainNavTab.TREASURY && !state.settings.familyEntryOnlyMode) {
            viewModel.selectTab(MainNavTab.TREASURY)
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                SandooqiTopHeader(
                    currentYM = state.currentYM,
                    darkMode = state.settings.darkMode,
                    privacyMode = state.settings.privacyMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onTogglePrivacy = { viewModel.togglePrivacyMode() },
                    onOpenVoice = { viewModel.setVoiceDialog(true) },
                    onOpenSearch = { viewModel.setSearchDialog(true) }
                )
            },
            bottomBar = {
                SandooqiBottomNav(
                    selectedTab = state.selectedTab,
                    onSelectTab = { viewModel.selectTab(it) },
                    familyEntryOnlyMode = state.settings.familyEntryOnlyMode
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (state.selectedTab) {
                    MainNavTab.HOME -> HomeScreen(state = state, viewModel = viewModel)
                    MainNavTab.TREASURY -> TreasuryScreen(state = state, viewModel = viewModel)
                    MainNavTab.EXPENSES -> ExpensesScreen(state = state, viewModel = viewModel)
                    MainNavTab.DEBTS -> DebtsScreen(state = state, viewModel = viewModel)
                    MainNavTab.BUDGET -> BudgetScreen(state = state, viewModel = viewModel)
                    MainNavTab.SEWING -> SewingScreen(state = state, viewModel = viewModel)
                    MainNavTab.REPORTS -> ReportsScreen(state = state, viewModel = viewModel)
                    MainNavTab.TOOLS -> ToolsScreen(state = state, viewModel = viewModel)
                }
            }

            if (state.showVoiceDialog) {
                VoiceEntryDialog(
                    onDismiss = { viewModel.setVoiceDialog(false) },
                    onSubmit = { parsed, title, amtCents, mode, party, immediate ->
                        viewModel.applyVoiceEntry(parsed, title, amtCents, mode, party, immediate)
                    }
                )
            }

            if (state.showSearchDialog) {
                GlobalSearchDialog(
                    onSearch = { viewModel.searchAll(it) },
                    onNavigate = { viewModel.selectTab(it) },
                    onDismiss = { viewModel.setSearchDialog(false) }
                )
            }

            if (state.showMonthCloseDialog) {
                MonthCloseDialog(
                    currentYM = state.currentYM,
                    surplusCents = state.currentReport.family.balanceCents,
                    goals = state.goals,
                    privacyMode = state.settings.privacyMode,
                    onDismiss = { viewModel.setMonthCloseDialog(false) },
                    onConfirmClose = { goalId, pct -> viewModel.closeMonth(goalId, pct) }
                )
            }
        }
    }
}
