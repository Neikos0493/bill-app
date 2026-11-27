package com.neikos0493.jileme.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.combine
import java.security.MessageDigest

class LedgerRepository(private val db: JilemeDatabase) {
    val dao=db.dao
    val accounts=dao.observeAccounts()
    val categories=dao.observeCategories()
    val transactions=dao.observeTransactions()
    val links=dao.observeLinks()
    val rules=dao.observeRules()

    suspend fun ensureDefaults() = seedMutex.withLock {
        if(dao.accountCount()>0) return
        listOf("支付宝","微信支付","现金","银行卡").forEachIndexed { i,name ->
            dao.upsertAccount(AccountEntity(name=name,sortOrder=i,color=listOf(0xFF1677FF,0xFF07C160,0xFFB07B3E,0xFF5A65A0)[i]))
        }
        val expenses=listOf("餐饮","交通","购物","居家","医疗","教育","娱乐","通讯","旅行","人情","宠物","转账手续费","其他")
        expenses.forEachIndexed { i,name -> dao.upsertCategory(CategoryEntity(name=name,kind=TxnKind.EXPENSE,sortOrder=i)) }
        listOf("工资","奖金","经营收入","投资收益","红包","其他").forEachIndexed { i,name ->
            dao.upsertCategory(CategoryEntity(name=name,kind=TxnKind.INCOME,sortOrder=i))
        }
    }

    suspend fun saveEntry(main: TransactionEntity, aaReceipts: List<TransactionEntity> = emptyList(), refundExpenseId: Long? = null): Long = db.withTransaction {
        val id=if(main.id==0L) dao.insertTransaction(main) else { dao.updateTransaction(main); main.id }
        if(main.kind==TxnKind.EXPENSE && main.aaEnabled) {
            val existing=dao.linksForExpense(id)
            val keep=aaReceipts.map { it.id }.toSet()
            existing.filter { it.kind==LinkKind.AA && it.offsetTransactionId !in keep }.forEach {
                dao.deleteLink(it.id); dao.deleteTransaction(it.offsetTransactionId)
            }
            aaReceipts.forEach { receipt ->
                val value=receipt.copy(kind=TxnKind.INCOME,nature=TxnNature.AA_RECEIPT,status=main.status)
                val receiptId=if(value.id==0L) dao.insertTransaction(value) else { dao.updateTransaction(value); value.id }
                val old=dao.linkForOffset(receiptId)
                dao.upsertLink(TransactionLinkEntity(id=old?.id ?: 0,expenseId=id,offsetTransactionId=receiptId,kind=LinkKind.AA,amountMinor=value.amountMinor))
            }
        } else if(main.kind==TxnKind.EXPENSE) {
            dao.linksForExpense(id).filter { it.kind==LinkKind.AA }.forEach { link ->
                dao.transaction(link.offsetTransactionId)?.let { dao.updateTransaction(it.copy(nature=if(link.kind==LinkKind.AA) TxnNature.AA_UNLINKED else TxnNature.REFUND_UNLINKED,updatedAt=System.currentTimeMillis())) }
                dao.deleteLink(link.id)
            }
        }
        if((main.nature==TxnNature.REFUND || main.nature==TxnNature.AA_RECEIPT) && refundExpenseId!=null) {
            val old=dao.linkForOffset(id)
            dao.upsertLink(TransactionLinkEntity(id=old?.id ?: 0,expenseId=refundExpenseId,offsetTransactionId=id,kind=if(main.nature==TxnNature.AA_RECEIPT) LinkKind.AA else LinkKind.REFUND,amountMinor=main.amountMinor))
        } else if(main.kind==TxnKind.INCOME) {
            dao.linkForOffset(id)?.let { dao.deleteLink(it.id) }
        }
        id
    }

    suspend fun deleteTransaction(id: Long) = db.withTransaction {
        dao.transaction(id) ?: return@withTransaction
        dao.linksForExpense(id).forEach { link ->
            dao.transaction(link.offsetTransactionId)?.let { dao.updateTransaction(it.copy(nature=if(link.kind==LinkKind.AA) TxnNature.AA_UNLINKED else TxnNature.REFUND_UNLINKED,updatedAt=System.currentTimeMillis())) }
            dao.deleteLink(link.id)
        }
        dao.deleteLinkForOffset(id)
        dao.deleteTransaction(id)
    }

    suspend fun ignoreTransaction(id: Long) = db.withTransaction {
        dao.transaction(id)?.let { dao.updateTransaction(it.copy(status=TxnStatus.IGNORED,updatedAt=System.currentTimeMillis())) }
        dao.linksForExpense(id).forEach { link -> dao.transaction(link.offsetTransactionId)?.let { dao.updateTransaction(it.copy(status=TxnStatus.IGNORED)) } }
    }

    suspend fun saveNotification(value: TransactionEntity): Boolean = db.withTransaction {
        if(value.fingerprint!=null && dao.byFingerprint(value.fingerprint)!=null) return@withTransaction false
        dao.insertTransaction(value); true
    }

    suspend fun importTransactions(fileName: String, source: String, rows: List<TransactionEntity>, errorCount: Int=0): Triple<Int,Int,Int> = db.withTransaction {
        var added=0; var duplicate=0
        val existing=dao.transactions().toMutableList()
        rows.forEach { row ->
            val fingerprint=row.fingerprint ?: fingerprint(listOf(source,row.externalId.orEmpty(),row.occurredAt.toString(),row.amountMinor.toString(),row.merchant,row.kind))
            val externalMatch=row.externalId?.let { external -> existing.firstOrNull { it.externalId==external && it.source==source } }
            if(dao.byFingerprint(fingerprint)!=null || externalMatch!=null) duplicate++ else {
                val notificationMatch=existing.filter { old -> old.source.contains("NOTIFICATION") && old.kind==row.kind && old.amountMinor==row.amountMinor && kotlin.math.abs(old.occurredAt-row.occurredAt)<=120_000 && (old.merchant==row.merchant || old.merchant=="未知商户") }.singleOrNull()
                if(notificationMatch!=null) {
                    dao.updateTransaction(notificationMatch.copy(source=source,externalId=row.externalId,rawText=row.rawText,merchant=row.merchant,categoryId=row.categoryId,updatedAt=System.currentTimeMillis()))
                    existing.replaceAll { if(it.id==notificationMatch.id) notificationMatch.copy(source=source,externalId=row.externalId) else it }; duplicate++
                } else {
                    val id=dao.insertTransaction(row.copy(fingerprint=fingerprint,source=source)); existing += row.copy(id=id,fingerprint=fingerprint,source=source); added++
                }
            }
        }
        dao.insertBatch(ImportBatchEntity(fileName=fileName,source=source,addedCount=added,duplicateCount=duplicate,errorCount=errorCount))
        Triple(added,duplicate,0)
    }

    suspend fun restore(data: BackupData) = db.withTransaction {
        dao.clearLinks(); dao.clearTransactions(); dao.clearRules(); dao.clearCategories(); dao.clearAccounts(); dao.clearBatches()
        dao.insertAccounts(data.accounts); dao.insertCategories(data.categories); dao.insertTransactions(data.transactions); dao.insertLinks(data.links); dao.insertRules(data.rules)
    }

    companion object {
        private val seedMutex=Mutex()
        fun fingerprint(parts: List<String>): String = MessageDigest.getInstance("SHA-256").digest(parts.joinToString("|").toByteArray()).joinToString("") { "%02x".format(it) }
    }
}






