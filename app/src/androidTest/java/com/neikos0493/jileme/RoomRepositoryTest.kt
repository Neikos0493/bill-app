package com.neikos0493.jileme

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neikos0493.jileme.data.*
import com.neikos0493.jileme.domain.LedgerCalculator
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomRepositoryTest {
    private lateinit var db: JilemeDatabase
    private lateinit var repository: LedgerRepository
    @Before fun setup(){db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),JilemeDatabase::class.java).allowMainThreadQueries().build();repository=LedgerRepository(db)}
    @After fun close(){db.close()}
    @Test fun aaRefundEditDeleteAndDuplicateImport()=runTest{
        val alipay=daoInsertAccount("支付宝");val wechat=daoInsertAccount("微信")
        val expenseId=repository.saveEntry(TransactionEntity(kind=TxnKind.EXPENSE,amountMinor=30000,accountId=alipay,aaEnabled=true,occurredAt=1000),listOf(TransactionEntity(kind=TxnKind.INCOME,amountMinor=12000,accountId=wechat,occurredAt=1000)))
        val links=db.dao.linksForExpense(expenseId);assertEquals(1,links.size);assertEquals(LinkKind.AA,links[0].kind)
        val aaId=links[0].offsetTransactionId
        val refundId=repository.saveEntry(TransactionEntity(kind=TxnKind.INCOME,nature=TxnNature.REFUND,amountMinor=5000,accountId=wechat,occurredAt=1001),refundExpenseId=expenseId)
        var all=db.dao.transactions();var allLinks=db.dao.links();val expense=db.dao.transaction(expenseId)!!
        assertEquals(13000,LedgerCalculator.netExpense(expense,all,allLinks));assertEquals(17000L,LedgerCalculator.balances(db.dao.accounts(),all)[wechat])
        repository.saveEntry(expense.copy(aaEnabled=false),emptyList())
        assertNull(db.dao.linkForOffset(aaId));assertNotNull(db.dao.linkForOffset(refundId));assertEquals(TxnNature.AA_UNLINKED,db.dao.transaction(aaId)?.nature)
        repository.deleteTransaction(expenseId)
        assertNotNull(db.dao.transaction(refundId));assertEquals(TxnNature.REFUND_UNLINKED,db.dao.transaction(refundId)?.nature)
        val row=TransactionEntity(kind=TxnKind.EXPENSE,amountMinor=100,accountId=alipay,occurredAt=2000,externalId="ORDER")
        assertEquals(Triple(1,0,0),repository.importTransactions("test.csv",TxnSource.ALIPAY_IMPORT,listOf(row)))
        assertEquals(Triple(0,1,0),repository.importTransactions("test.csv",TxnSource.ALIPAY_IMPORT,listOf(row)))
    }
    private suspend fun daoInsertAccount(name:String)=db.dao.upsertAccount(AccountEntity(name=name))
}

