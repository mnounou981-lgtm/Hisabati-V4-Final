package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.CatalogDefaults
import com.example.domain.CatalogProduct
import com.example.domain.QuickExpensePreset
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sandooqiDataStore by preferencesDataStore(name = "sandooqi_prefs")

data class SandooqiSettings(
    val darkMode: Boolean = false,
    val privacyMode: Boolean = false,
    val soundEnabled: Boolean = true,
    val monthlyFamilyBudgetCents: Long = 40_000_00L,
    val weeklyFamilyBudgetCents: Long = 0L,
    val marketBudgetCents: Long = 15_000_00L,
    val meterPriceCents: Long = 300_00L,
    val threadPriceCents: Long = 100_00L,
    val personalOpeningCents: Long = 0L,
    val sewingOpeningCents: Long = 0L,
    val appPin: String = "1234",
    val appLockEnabled: Boolean = false,
    val familyEntryOnlyMode: Boolean = false,
    val activeFamilyMember: String = "إسلام",
    val familyMembers: List<String> = listOf("إسلام", "محمد", "يوسف", "نعيمة"),
    val quickExpenses: List<QuickExpensePreset> = CatalogDefaults.defaultQuickExpenses,
    val customProducts: List<CatalogProduct> = emptyList()
)

class SandooqiPreferences(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val MONTHLY_FAMILY_BUDGET = longPreferencesKey("monthly_family_budget_cents")
        val WEEKLY_FAMILY_BUDGET = longPreferencesKey("weekly_family_budget_cents")
        val MARKET_BUDGET = longPreferencesKey("market_budget_cents")
        val METER_PRICE = longPreferencesKey("meter_price_cents")
        val THREAD_PRICE = longPreferencesKey("thread_price_cents")
        val PERSONAL_OPENING = longPreferencesKey("personal_opening_cents")
        val SEWING_OPENING = longPreferencesKey("sewing_opening_cents")
        val APP_PIN = stringPreferencesKey("app_pin")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val FAMILY_ENTRY_ONLY = booleanPreferencesKey("family_entry_only")
        val ACTIVE_FAMILY_MEMBER = stringPreferencesKey("active_family_member")
        val FAMILY_MEMBERS = stringPreferencesKey("family_members_csv")
        val QUICK_EXPENSES_JSON = stringPreferencesKey("quick_expenses_json")
        val CUSTOM_PRODUCTS_JSON = stringPreferencesKey("custom_products_json")
    }

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val quickAdapter = moshi.adapter<List<QuickExpensePreset>>(
        Types.newParameterizedType(List::class.java, QuickExpensePreset::class.java)
    )
    private val productsAdapter = moshi.adapter<List<CatalogProduct>>(
        Types.newParameterizedType(List::class.java, CatalogProduct::class.java)
    )

    val settingsFlow: Flow<SandooqiSettings> = context.sandooqiDataStore.data.map { prefs ->
        val rawMembers = prefs[Keys.FAMILY_MEMBERS] ?: "إسلام,محمد,يوسف,نعيمة"
        val members = rawMembers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("إسلام", "محمد", "يوسف", "نعيمة") }

        val quickList = prefs[Keys.QUICK_EXPENSES_JSON]?.let { raw ->
            runCatching { quickAdapter.fromJson(raw) }.getOrNull()
        } ?: CatalogDefaults.defaultQuickExpenses

        val customList = prefs[Keys.CUSTOM_PRODUCTS_JSON]?.let { raw ->
            runCatching { productsAdapter.fromJson(raw) }.getOrNull()
        } ?: emptyList()

        SandooqiSettings(
            darkMode = prefs[Keys.DARK_MODE] ?: false,
            privacyMode = prefs[Keys.PRIVACY_MODE] ?: false,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            monthlyFamilyBudgetCents = prefs[Keys.MONTHLY_FAMILY_BUDGET] ?: 40_000_00L,
            weeklyFamilyBudgetCents = prefs[Keys.WEEKLY_FAMILY_BUDGET] ?: 0L,
            marketBudgetCents = prefs[Keys.MARKET_BUDGET] ?: 15_000_00L,
            meterPriceCents = prefs[Keys.METER_PRICE] ?: 300_00L,
            threadPriceCents = prefs[Keys.THREAD_PRICE] ?: 100_00L,
            personalOpeningCents = prefs[Keys.PERSONAL_OPENING] ?: 0L,
            sewingOpeningCents = prefs[Keys.SEWING_OPENING] ?: 0L,
            appPin = prefs[Keys.APP_PIN] ?: "1234",
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            familyEntryOnlyMode = prefs[Keys.FAMILY_ENTRY_ONLY] ?: false,
            activeFamilyMember = prefs[Keys.ACTIVE_FAMILY_MEMBER] ?: members.first(),
            familyMembers = members,
            quickExpenses = quickList,
            customProducts = customList
        )
    }

    suspend fun toggleDarkMode() {
        context.sandooqiDataStore.edit { it[Keys.DARK_MODE] = !(it[Keys.DARK_MODE] ?: false) }
    }

    suspend fun togglePrivacyMode() {
        context.sandooqiDataStore.edit { it[Keys.PRIVACY_MODE] = !(it[Keys.PRIVACY_MODE] ?: false) }
    }

    suspend fun toggleSound() {
        context.sandooqiDataStore.edit { it[Keys.SOUND_ENABLED] = !(it[Keys.SOUND_ENABLED] ?: true) }
    }

    suspend fun setMonthlyFamilyBudget(cents: Long) {
        context.sandooqiDataStore.edit { it[Keys.MONTHLY_FAMILY_BUDGET] = cents.coerceAtLeast(0L) }
    }

    suspend fun setWeeklyFamilyBudget(cents: Long) {
        context.sandooqiDataStore.edit { it[Keys.WEEKLY_FAMILY_BUDGET] = cents.coerceAtLeast(0L) }
    }

    suspend fun setMarketBudget(cents: Long) {
        context.sandooqiDataStore.edit { it[Keys.MARKET_BUDGET] = cents.coerceAtLeast(0L) }
    }

    suspend fun setSewingRates(meterCents: Long, threadCents: Long) {
        context.sandooqiDataStore.edit {
            it[Keys.METER_PRICE] = meterCents.coerceAtLeast(100L)
            it[Keys.THREAD_PRICE] = threadCents.coerceAtLeast(100L)
        }
    }

    suspend fun setAccountOpenings(personalCents: Long, sewingCents: Long) {
        context.sandooqiDataStore.edit {
            it[Keys.PERSONAL_OPENING] = personalCents.coerceAtLeast(0L)
            it[Keys.SEWING_OPENING] = sewingCents.coerceAtLeast(0L)
        }
    }

    suspend fun setPin(pin: String, lockEnabled: Boolean) {
        context.sandooqiDataStore.edit {
            it[Keys.APP_PIN] = pin
            it[Keys.APP_LOCK_ENABLED] = lockEnabled
        }
    }

    suspend fun setFamilyEntryOnlyMode(enabled: Boolean, activeMember: String? = null) {
        context.sandooqiDataStore.edit {
            it[Keys.FAMILY_ENTRY_ONLY] = enabled
            if (activeMember != null) it[Keys.ACTIVE_FAMILY_MEMBER] = activeMember
        }
    }

    suspend fun setActiveFamilyMember(name: String) {
        context.sandooqiDataStore.edit { it[Keys.ACTIVE_FAMILY_MEMBER] = name }
    }

    suspend fun updateFamilyMembers(members: List<String>) {
        val cleaned = members.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleaned.isNotEmpty()) {
            context.sandooqiDataStore.edit {
                it[Keys.FAMILY_MEMBERS] = cleaned.joinToString(",")
            }
        }
    }

    suspend fun updateQuickExpenses(items: List<QuickExpensePreset>) {
        context.sandooqiDataStore.edit {
            it[Keys.QUICK_EXPENSES_JSON] = quickAdapter.toJson(items)
        }
    }

    suspend fun updateCustomProducts(items: List<CatalogProduct>) {
        context.sandooqiDataStore.edit {
            it[Keys.CUSTOM_PRODUCTS_JSON] = productsAdapter.toJson(items)
        }
    }

    suspend fun applySettings(settings: SandooqiSettings) {
        context.sandooqiDataStore.edit {
            it[Keys.DARK_MODE] = settings.darkMode
            it[Keys.PRIVACY_MODE] = settings.privacyMode
            it[Keys.SOUND_ENABLED] = settings.soundEnabled
            it[Keys.MONTHLY_FAMILY_BUDGET] = settings.monthlyFamilyBudgetCents
            it[Keys.WEEKLY_FAMILY_BUDGET] = settings.weeklyFamilyBudgetCents
            it[Keys.MARKET_BUDGET] = settings.marketBudgetCents
            it[Keys.METER_PRICE] = settings.meterPriceCents
            it[Keys.THREAD_PRICE] = settings.threadPriceCents
            it[Keys.PERSONAL_OPENING] = settings.personalOpeningCents
            it[Keys.SEWING_OPENING] = settings.sewingOpeningCents
            it[Keys.APP_PIN] = settings.appPin
            it[Keys.APP_LOCK_ENABLED] = settings.appLockEnabled
            it[Keys.FAMILY_ENTRY_ONLY] = settings.familyEntryOnlyMode
            it[Keys.ACTIVE_FAMILY_MEMBER] = settings.activeFamilyMember
            it[Keys.FAMILY_MEMBERS] = settings.familyMembers.joinToString(",")
            it[Keys.QUICK_EXPENSES_JSON] = quickAdapter.toJson(settings.quickExpenses)
            it[Keys.CUSTOM_PRODUCTS_JSON] = productsAdapter.toJson(settings.customProducts)
        }
    }
}
