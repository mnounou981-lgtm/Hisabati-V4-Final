package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.domain.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CreditorDebtEntity::class,
        ObligationEntity::class,
        FacPlanEntity::class,
        SavingsGoalEntity::class,
        SeasonalBudgetEntity::class,
        TailoringOrderEntity::class,
        SewServiceEntity::class,
        SewProjectEntity::class,
        SewMaterialEntity::class,
        CustomerMeasurementEntity::class,
        MarketItemEntity::class,
        PriceCheckEntity::class,
        WarrantyEntity::class,
        MonthRecordEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SandooqiDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun creditorDebtDao(): CreditorDebtDao
    abstract fun obligationDao(): ObligationDao
    abstract fun facPlanDao(): FacPlanDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun seasonalBudgetDao(): SeasonalBudgetDao
    abstract fun tailoringOrderDao(): TailoringOrderDao
    abstract fun sewServiceDao(): SewServiceDao
    abstract fun sewProjectDao(): SewProjectDao
    abstract fun sewMaterialDao(): SewMaterialDao
    abstract fun customerMeasurementDao(): CustomerMeasurementDao
    abstract fun marketItemDao(): MarketItemDao
    abstract fun priceCheckDao(): PriceCheckDao
    abstract fun warrantyDao(): WarrantyDao
    abstract fun monthRecordDao(): MonthRecordDao

    companion object {
        @Volatile
        private var INSTANCE: SandooqiDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fac_plans ADD COLUMN planType TEXT NOT NULL DEFAULT 'fac'")
            }
        }

        fun getInstance(context: Context): SandooqiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): SandooqiDatabase {
            return Room.databaseBuilder(
                appContext,
                SandooqiDatabase::class.java,
                "sandooqi_local.db"
            ).addMigrations(MIGRATION_2_3).fallbackToDestructiveMigration(true).addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        val instance = getInstance(appContext)
                        val ym = DateUtils.currentYM()
                        instance.monthRecordDao().upsert(
                            MonthRecordEntity(
                                ym = ym,
                                salary1Cents = 120_000_00L,
                                salary2Cents = 30_000_00L
                            )
                        )
                        instance.savingsGoalDao().insert(
                            SavingsGoalEntity(
                                name = "حصّالة الطوارئ والعيد",
                                targetCents = 50_000_00L,
                                savedCents = 0L
                            )
                        )
                    }
                }
            }).build()
        }
    }
}
