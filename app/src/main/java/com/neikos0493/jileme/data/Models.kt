package com.neikos0493.jileme.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

object TxnKind { const val EXPENSE="EXPENSE"; const val INCOME="INCOME"; const val TRANSFER="TRANSFER" }
object TxnNature { const val NORMAL="NORMAL"; const val AA_RECEIPT="AA_RECEIPT"; const val AA_UNLINKED="AA_UNLINKED"; const val REFUND="REFUND"; const val REFUND_UNLINKED="REFUND_UNLINKED"; const val ADJUSTMENT="ADJUSTMENT" }
object TxnStatus { const val PENDING="PENDING"; const val CONFIRMED="CONFIRMED"; const val IGNORED="IGNORED" }
object TxnSource { const val MANUAL="MANUAL"; const val ALIPAY_NOTIFICATION="ALIPAY_NOTIFICATION"; const val WECHAT_NOTIFICATION="WECHAT_NOTIFICATION"; const val ALIPAY_IMPORT="ALIPAY_IMPORT"; const val WECHAT_IMPORT="WECHAT_IMPORT" }
object LinkKind { const val AA="AA"; const val REFUND="REFUND" }
object FeeMode { const val NONE="NONE"; const val FIXED="FIXED"; const val PERCENT="PERCENT" }

@Serializable
@Entity(tableName="accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val name: String,
    val openingBalanceMinor: Long = 0,
    val includeInTotal: Boolean = true,
    val archived: Boolean = false,
    val color: Long = 0xFF315C4C,
    val sortOrder: Int = 0,
)

@Serializable
@Entity(tableName="categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val name: String,
    val kind: String,
    val color: Long = 0xFF6B7F76,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

@Serializable
@Entity(
    tableName="transactions",
    indices=[Index(value=["fingerprint"], unique=true), Index(value=["occurredAt"]), Index(value=["accountId"]), Index(value=["externalId"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val kind: String,
    val nature: String = TxnNature.NORMAL,
    val amountMinor: Long,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val merchant: String = "",
    val note: String = "",
    val occurredAt: Long,
    val status: String = TxnStatus.CONFIRMED,
    val source: String = TxnSource.MANUAL,
    val sourcePackage: String = "",
    val externalId: String? = null,
    val fingerprint: String? = null,
    val rawText: String = "",
    val feeMode: String = FeeMode.NONE,
    val feeRate: String = "",
    val feeAmountMinor: Long = 0,
    val aaEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName="transaction_links",
    indices=[Index(value=["expenseId"]), Index(value=["offsetTransactionId"], unique=true)]
)
data class TransactionLinkEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val expenseId: Long,
    val offsetTransactionId: Long,
    val kind: String,
    val amountMinor: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName="category_rules", indices=[Index(value=["keyword"], unique=true)])
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val keyword: String,
    val categoryId: Long,
    val accountId: Long? = null,
)

@Serializable
@Entity(tableName="import_batches")
data class ImportBatchEntity(
    @PrimaryKey(autoGenerate=true) val id: Long = 0,
    val fileName: String,
    val source: String,
    val addedCount: Int,
    val duplicateCount: Int,
    val errorCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
)
