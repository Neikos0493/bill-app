package com.neikos0493.jileme.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerDao {
    @Query("SELECT * FROM accounts ORDER BY sortOrder, id") fun observeAccounts(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM categories ORDER BY sortOrder, id") fun observeCategories(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id DESC") fun observeTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transaction_links") fun observeLinks(): Flow<List<TransactionLinkEntity>>
    @Query("SELECT * FROM category_rules ORDER BY keyword") fun observeRules(): Flow<List<CategoryRuleEntity>>

    @Query("SELECT * FROM accounts ORDER BY sortOrder, id") suspend fun accounts(): List<AccountEntity>
    @Query("SELECT * FROM categories ORDER BY sortOrder, id") suspend fun categories(): List<CategoryEntity>
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id DESC") suspend fun transactions(): List<TransactionEntity>
    @Query("SELECT * FROM transaction_links") suspend fun links(): List<TransactionLinkEntity>
    @Query("SELECT * FROM category_rules ORDER BY keyword") suspend fun rules(): List<CategoryRuleEntity>
    @Query("SELECT COUNT(*) FROM accounts") suspend fun accountCount(): Int
    @Query("SELECT * FROM transactions WHERE id=:id") suspend fun transaction(id: Long): TransactionEntity?
    @Query("SELECT * FROM transactions WHERE fingerprint=:fingerprint LIMIT 1") suspend fun byFingerprint(fingerprint: String): TransactionEntity?
    @Query("SELECT * FROM transaction_links WHERE expenseId=:expenseId") suspend fun linksForExpense(expenseId: Long): List<TransactionLinkEntity>
    @Query("SELECT * FROM transaction_links WHERE offsetTransactionId=:offsetId") suspend fun linkForOffset(offsetId: Long): TransactionLinkEntity?

    @Insert suspend fun insertTransaction(value: TransactionEntity): Long
    @Update suspend fun updateTransaction(value: TransactionEntity)
    @Upsert suspend fun upsertAccount(value: AccountEntity): Long
    @Upsert suspend fun upsertCategory(value: CategoryEntity): Long
    @Upsert suspend fun upsertTransaction(value: TransactionEntity): Long
    @Upsert suspend fun upsertLink(value: TransactionLinkEntity): Long
    @Upsert suspend fun upsertRule(value: CategoryRuleEntity): Long
    @Insert suspend fun insertBatch(value: ImportBatchEntity): Long
    @Insert suspend fun insertAccounts(values: List<AccountEntity>)
    @Insert suspend fun insertCategories(values: List<CategoryEntity>)
    @Insert suspend fun insertTransactions(values: List<TransactionEntity>)
    @Insert suspend fun insertLinks(values: List<TransactionLinkEntity>)
    @Insert suspend fun insertRules(values: List<CategoryRuleEntity>)

    @Query("DELETE FROM transactions WHERE id=:id") suspend fun deleteTransaction(id: Long)
    @Query("DELETE FROM transaction_links WHERE id=:id") suspend fun deleteLink(id: Long)
    @Query("DELETE FROM transaction_links WHERE offsetTransactionId=:id") suspend fun deleteLinkForOffset(id: Long)
    @Query("DELETE FROM accounts WHERE id=:id") suspend fun deleteAccount(id: Long)
    @Query("DELETE FROM categories WHERE id=:id") suspend fun deleteCategory(id: Long)
    @Query("DELETE FROM category_rules WHERE id=:id") suspend fun deleteRule(id: Long)

    @Query("DELETE FROM transaction_links") suspend fun clearLinks()
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM category_rules") suspend fun clearRules()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM accounts") suspend fun clearAccounts()
    @Query("DELETE FROM import_batches") suspend fun clearBatches()
}

@Database(
    entities=[AccountEntity::class, CategoryEntity::class, TransactionEntity::class, TransactionLinkEntity::class, CategoryRuleEntity::class, ImportBatchEntity::class],
    version=1,
    exportSchema=false,
)
abstract class JilemeDatabase : RoomDatabase() {
    abstract val dao: LedgerDao
    companion object {
        @Volatile private var instance: JilemeDatabase? = null
        fun get(context: android.content.Context): JilemeDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, JilemeDatabase::class.java, "jileme.db")
                .fallbackToDestructiveMigrationOnDowngrade().build().also { instance=it }
        }
    }
}

