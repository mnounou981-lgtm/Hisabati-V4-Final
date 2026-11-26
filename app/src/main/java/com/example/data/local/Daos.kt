package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateISO DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE transferId = :transferId")
    suspend fun deleteByTransferId(transferId: Long)

    @Query("DELETE FROM transactions WHERE ym = :ym AND accountId = 'family'")
    suspend fun clearFamilyMonth(ym: String)

    @Query("DELETE FROM transactions WHERE accountId = 'sewing' OR transferFromAccountId = 'sewing' OR transferToAccountId = 'sewing'")
    suspend fun clearSewingAccount()

    @Query("DELETE FROM transactions")
    suspend fun clearAll()
}

@Dao
interface CreditorDebtDao {
    @Query("SELECT * FROM creditor_debts ORDER BY settled ASC, dateISO DESC, id DESC")
    fun observeAll(): Flow<List<CreditorDebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CreditorDebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CreditorDebtEntity>)

    @Update
    suspend fun update(item: CreditorDebtEntity)

    @Query("DELETE FROM creditor_debts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM creditor_debts WHERE creditor = :creditor AND kind IN ('home_debt','bill_debt')")
    suspend fun deleteCreditorDebts(creditor: String)

    @Query("DELETE FROM creditor_debts WHERE kind IN ('home_debt','bill_debt','rent_in','rent_out')")
    suspend fun clearHomeAndBillDebts()

    @Query("DELETE FROM creditor_debts WHERE kind IN ('sew_debt_in','sew_debt_out')")
    suspend fun clearSewingDebts()

    @Query("DELETE FROM creditor_debts")
    suspend fun clearAll()
}

@Dao
interface ObligationDao {
    @Query("SELECT * FROM obligations ORDER BY id DESC")
    fun observeAll(): Flow<List<ObligationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ObligationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ObligationEntity>)

    @Update
    suspend fun update(item: ObligationEntity)

    @Query("DELETE FROM obligations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM obligations")
    suspend fun clearAll()
}

@Dao
interface FacPlanDao {
    @Query("SELECT * FROM fac_plans ORDER BY id DESC")
    fun observeAll(): Flow<List<FacPlanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FacPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FacPlanEntity>)

    @Update
    suspend fun update(item: FacPlanEntity)

    @Query("DELETE FROM fac_plans WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM fac_plans")
    suspend fun clearAll()
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY id ASC")
    fun observeAll(): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SavingsGoalEntity>)

    @Update
    suspend fun update(item: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM savings_goals")
    suspend fun clearAll()
}

@Dao
interface SeasonalBudgetDao {
    @Query("SELECT * FROM seasonal_budgets ORDER BY targetYM ASC, id DESC")
    fun observeAll(): Flow<List<SeasonalBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SeasonalBudgetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SeasonalBudgetEntity>)

    @Update
    suspend fun update(item: SeasonalBudgetEntity)

    @Query("DELETE FROM seasonal_budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM seasonal_budgets")
    suspend fun clearAll()
}

@Dao
interface TailoringOrderDao {
    @Query("SELECT * FROM tailoring_orders ORDER BY isCancelled ASC, isDone ASC, id DESC")
    fun observeAll(): Flow<List<TailoringOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TailoringOrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TailoringOrderEntity>)

    @Update
    suspend fun update(item: TailoringOrderEntity)

    @Query("DELETE FROM tailoring_orders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tailoring_orders")
    suspend fun clearAll()
}

@Dao
interface SewServiceDao {
    @Query("SELECT * FROM sew_services ORDER BY dateISO DESC, id DESC")
    fun observeAll(): Flow<List<SewServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SewServiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SewServiceEntity>)

    @Update
    suspend fun update(item: SewServiceEntity)

    @Query("DELETE FROM sew_services WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sew_services")
    suspend fun clearAll()
}

@Dao
interface SewProjectDao {
    @Query("SELECT * FROM sew_projects ORDER BY dateISO DESC, id DESC")
    fun observeAll(): Flow<List<SewProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SewProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SewProjectEntity>)

    @Update
    suspend fun update(item: SewProjectEntity)

    @Query("DELETE FROM sew_projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sew_projects")
    suspend fun clearAll()
}

@Dao
interface SewMaterialDao {
    @Query("SELECT * FROM sew_materials ORDER BY id DESC")
    fun observeAll(): Flow<List<SewMaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SewMaterialEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SewMaterialEntity>)

    @Update
    suspend fun update(item: SewMaterialEntity)

    @Query("DELETE FROM sew_materials WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sew_materials")
    suspend fun clearAll()
}

@Dao
interface CustomerMeasurementDao {
    @Query("SELECT * FROM customer_measurements ORDER BY name ASC")
    fun observeAll(): Flow<List<CustomerMeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CustomerMeasurementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CustomerMeasurementEntity>)

    @Query("DELETE FROM customer_measurements WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM customer_measurements")
    suspend fun clearAll()
}

@Dao
interface MarketItemDao {
    @Query("SELECT * FROM market_items ORDER BY isDone ASC, id DESC")
    fun observeAll(): Flow<List<MarketItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MarketItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MarketItemEntity>)

    @Update
    suspend fun update(item: MarketItemEntity)

    @Query("DELETE FROM market_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM market_items WHERE isDone = 1")
    suspend fun clearDone()

    @Query("DELETE FROM market_items")
    suspend fun clearAll()
}

@Dao
interface PriceCheckDao {
    @Query("SELECT * FROM price_checks ORDER BY dateISO DESC, id DESC")
    fun observeAll(): Flow<List<PriceCheckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PriceCheckEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PriceCheckEntity>)

    @Query("DELETE FROM price_checks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM price_checks")
    suspend fun clearAll()
}

@Dao
interface WarrantyDao {
    @Query("SELECT * FROM warranties ORDER BY expiryDateISO ASC, id DESC")
    fun observeAll(): Flow<List<WarrantyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WarrantyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WarrantyEntity>)

    @Query("DELETE FROM warranties WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM warranties")
    suspend fun clearAll()
}

@Dao
interface MonthRecordDao {
    @Query("SELECT * FROM month_records ORDER BY ym DESC")
    fun observeAll(): Flow<List<MonthRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MonthRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MonthRecordEntity>)

    @Query("SELECT * FROM month_records WHERE ym = :ym LIMIT 1")
    suspend fun getByYm(ym: String): MonthRecordEntity?

    @Query("DELETE FROM month_records")
    suspend fun clearAll()
}
