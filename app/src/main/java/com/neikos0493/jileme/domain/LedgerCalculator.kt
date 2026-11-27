package com.neikos0493.jileme.domain

import com.neikos0493.jileme.data.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

object Money {
    fun parse(value: String): Long? = runCatching {
        BigDecimal(value.trim().replace(",", "")).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }.getOrNull()
    fun format(minor: Long, symbol: Boolean = true): String {
        val value=BigDecimal(minor).movePointLeft(2)
        val formatted=NumberFormat.getNumberInstance(Locale.CHINA).apply { minimumFractionDigits=2; maximumFractionDigits=2 }.format(value)
        return if(symbol) "¥$formatted" else formatted
    }
    fun input(minor: Long): String = BigDecimal(minor).movePointLeft(2).stripTrailingZeros().toPlainString()
}

object LedgerCalculator {
    fun fee(amountMinor: Long, mode: String, fixedMinor: Long = 0, percent: String = ""): Long = when(mode) {
        FeeMode.FIXED -> fixedMinor.coerceAtLeast(0)
        FeeMode.PERCENT -> runCatching {
            BigDecimal(amountMinor).multiply(BigDecimal(percent)).divide(BigDecimal(100), 0, RoundingMode.HALF_UP).longValueExact().coerceAtLeast(0)
        }.getOrDefault(0)
        else -> 0
    }

    fun balances(accounts: List<AccountEntity>, transactions: List<TransactionEntity>): Map<Long,Long> {
        val result=accounts.associate { it.id to it.openingBalanceMinor }.toMutableMap()
        transactions.filter { it.status==TxnStatus.CONFIRMED }.forEach { t ->
            when(t.kind) {
                TxnKind.EXPENSE -> result[t.accountId]=(result[t.accountId] ?: 0)-t.amountMinor
                TxnKind.INCOME -> result[t.accountId]=(result[t.accountId] ?: 0)+t.amountMinor
                TxnKind.TRANSFER -> {
                    result[t.accountId]=(result[t.accountId] ?: 0)-t.amountMinor-t.feeAmountMinor
                    t.toAccountId?.let { result[it]=(result[it] ?: 0)+t.amountMinor }
                }
            }
        }
        return result
    }

    fun totalBalance(accounts: List<AccountEntity>, transactions: List<TransactionEntity>): Long {
        val balances=balances(accounts,transactions)
        return accounts.filter { it.includeInTotal && !it.archived }.sumOf { balances[it.id] ?: it.openingBalanceMinor }
    }

    fun offsets(expenseId: Long, transactions: List<TransactionEntity>, links: List<TransactionLinkEntity>, kind: String?=null): Long {
        val confirmedIds=transactions.filter { it.status==TxnStatus.CONFIRMED }.associateBy { it.id }
        return links.filter { it.expenseId==expenseId && (kind==null || it.kind==kind) }
            .filter { confirmedIds.containsKey(it.offsetTransactionId) }.sumOf { it.amountMinor }
    }

    fun netExpense(expense: TransactionEntity, transactions: List<TransactionEntity>, links: List<TransactionLinkEntity>): Long =
        expense.amountMinor-offsets(expense.id,transactions,links)

    fun monthRange(month: YearMonth, zone: ZoneId=ZoneId.systemDefault()): LongRange {
        val start=month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end=month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()-1
        return start..end
    }

    fun monthExpense(month: YearMonth, transactions: List<TransactionEntity>, links: List<TransactionLinkEntity>): Long {
        val range=monthRange(month)
        val transactionMap=transactions.associateBy { it.id }
        val base=transactions.filter { it.status==TxnStatus.CONFIRMED && it.kind==TxnKind.EXPENSE && it.occurredAt in range }
            .sumOf { netExpense(it,transactions,links) }
        val fees=transactions.filter { it.status==TxnStatus.CONFIRMED && it.kind==TxnKind.TRANSFER && it.occurredAt in range }.sumOf { it.feeAmountMinor }
        return base+fees
    }

    fun monthIncome(month: YearMonth, transactions: List<TransactionEntity>): Long {
        val range=monthRange(month)
        return transactions.filter { it.status==TxnStatus.CONFIRMED && it.kind==TxnKind.INCOME && it.nature==TxnNature.NORMAL && it.occurredAt in range }.sumOf { it.amountMinor }
    }

    fun categoryExpenses(month: YearMonth, transactions: List<TransactionEntity>, links: List<TransactionLinkEntity>): Map<Long?,Long> {
        val range=monthRange(month)
        val result=mutableMapOf<Long?,Long>()
        transactions.filter { it.status==TxnStatus.CONFIRMED && it.kind==TxnKind.EXPENSE && it.occurredAt in range }.forEach { t ->
            result[t.categoryId]=(result[t.categoryId] ?: 0)+netExpense(t,transactions,links)
        }
        transactions.filter { it.status==TxnStatus.CONFIRMED && it.kind==TxnKind.TRANSFER && it.feeAmountMinor>0 && it.occurredAt in range }.forEach { t ->
            result[null]=(result[null] ?: 0)+t.feeAmountMinor
        }
        return result.filterValues { it!=0L }
    }
}

object GestureHasher {
    private const val ITERATIONS=12000
    fun hash(pattern: List<Int>, salt: String=java.util.UUID.randomUUID().toString()): String {
        val digest=javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec=javax.crypto.spec.PBEKeySpec(pattern.joinToString("-").toCharArray(),salt.toByteArray(),ITERATIONS,256)
        return salt+":"+android.util.Base64.encodeToString(digest.generateSecret(spec).encoded,android.util.Base64.NO_WRAP)
    }
    fun verify(pattern: List<Int>, stored: String): Boolean {
        val salt=stored.substringBefore(':',"")
        if(salt.isEmpty()) return false
        return hash(pattern,salt)==stored
    }
}

fun Long.toYearMonth(): YearMonth = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate().let { YearMonth.from(it) }
