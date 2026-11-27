package com.neikos0493.jileme

import com.neikos0493.jileme.data.*
import com.neikos0493.jileme.domain.LedgerCalculator
import com.neikos0493.jileme.domain.Money
import com.neikos0493.jileme.domain.toYearMonth
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class LedgerCalculatorTest {
    private val time=System.currentTimeMillis()
    private val accounts=listOf(AccountEntity(1,"支付宝",1000_00),AccountEntity(2,"微信支付",500_00),AccountEntity(3,"现金",100_00))
    @Test fun `money uses exact decimal cents`(){assertEquals(12345L,Money.parse("123.45"));assertEquals(1L,Money.parse("0.009"));assertEquals("¥123.45",Money.format(12345))}
    @Test fun `fixed and percentage fees`(){assertEquals(100,LedgerCalculator.fee(1000_00,FeeMode.PERCENT,percent="0.1"));assertEquals(88,LedgerCalculator.fee(1000_00,FeeMode.FIXED,88));assertEquals(1,LedgerCalculator.fee(10000,FeeMode.PERCENT,percent="0.01"))}
    @Test fun `transfer changes two accounts and only fee is expense`(){
        val transfer=TransactionEntity(1,TxnKind.TRANSFER,amountMinor=1000_00,accountId=1,toAccountId=2,feeAmountMinor=1_00,occurredAt=time)
        val balances=LedgerCalculator.balances(accounts,listOf(transfer))
        assertEquals(-1_00L,balances[1]);assertEquals(1500_00L,balances[2]);assertEquals(1_00,LedgerCalculator.monthExpense(time.toYearMonth(),listOf(transfer),emptyList()))
    }
    @Test fun `AA receipts may arrive in different accounts`(){
        val expense=TransactionEntity(1,TxnKind.EXPENSE,amountMinor=300_00,accountId=1,aaEnabled=true,occurredAt=time)
        val wechat=TransactionEntity(2,TxnKind.INCOME,nature=TxnNature.AA_RECEIPT,amountMinor=120_00,accountId=2,occurredAt=time)
        val cash=TransactionEntity(3,TxnKind.INCOME,nature=TxnNature.AA_RECEIPT,amountMinor=80_00,accountId=3,occurredAt=time)
        val transactions=listOf(expense,wechat,cash);val links=listOf(TransactionLinkEntity(1,1,2,LinkKind.AA,120_00),TransactionLinkEntity(2,1,3,LinkKind.AA,80_00))
        val balances=LedgerCalculator.balances(accounts,transactions)
        assertEquals(700_00L,balances[1]);assertEquals(620_00L,balances[2]);assertEquals(180_00L,balances[3]);assertEquals(100_00,LedgerCalculator.netExpense(expense,transactions,links));assertEquals(100_00,LedgerCalculator.monthExpense(time.toYearMonth(),transactions,links));assertEquals(0,LedgerCalculator.monthIncome(time.toYearMonth(),transactions))
    }
    @Test fun `refund and AA combine without becoming ordinary income`(){
        val expense=TransactionEntity(1,TxnKind.EXPENSE,amountMinor=300_00,accountId=1,aaEnabled=true,occurredAt=time)
        val aa=TransactionEntity(2,TxnKind.INCOME,nature=TxnNature.AA_RECEIPT,amountMinor=100_00,accountId=2,occurredAt=time)
        val refund=TransactionEntity(3,TxnKind.INCOME,nature=TxnNature.REFUND,amountMinor=50_00,accountId=2,occurredAt=time)
        val transactions=listOf(expense,aa,refund);val links=listOf(TransactionLinkEntity(1,1,2,LinkKind.AA,100_00),TransactionLinkEntity(2,1,3,LinkKind.REFUND,50_00))
        assertEquals(150_00,LedgerCalculator.netExpense(expense,transactions,links));assertEquals(0,LedgerCalculator.monthIncome(time.toYearMonth(),transactions));assertEquals(650_00L,LedgerCalculator.balances(accounts,transactions)[2])
    }
    @Test fun `pending records do not affect any totals`(){
        val pending=TransactionEntity(1,TxnKind.EXPENSE,amountMinor=999_00,accountId=1,status=TxnStatus.PENDING,occurredAt=time)
        assertEquals(1000_00L,LedgerCalculator.balances(accounts,listOf(pending))[1]);assertEquals(0,LedgerCalculator.monthExpense(time.toYearMonth(),listOf(pending),emptyList()))
    }
}




