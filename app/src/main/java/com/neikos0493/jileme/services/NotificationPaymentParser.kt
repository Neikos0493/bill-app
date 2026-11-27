package com.neikos0493.jileme.services

import com.neikos0493.jileme.data.*
import com.neikos0493.jileme.domain.Money

object NotificationPaymentParser {
    private val amountPatterns=listOf(
        Regex("[¥￥]\\s*(\\d+(?:\\.\\d{1,2})?)"),
        Regex("(?:支付|付款|支出|收款|收入|到账|退款)(?:金额)?\\s*(?:人民币)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*元"),
        Regex("(\\d+(?:\\.\\d{1,2})?)\\s*元"),
    )
    fun parse(packageName: String, title: String, text: String, key: String, postedAt: Long, autoAdd: Boolean, accounts: List<AccountEntity>, categories: List<CategoryEntity>, rules: List<CategoryRuleEntity>): TransactionEntity? {
        val content=listOf(title,text).joinToString(" ").trim()
        if(content.isBlank()) return null
        val isAlipay=packageName=="com.eg.android.AlipayGphone"
        val isWechat=packageName=="com.tencent.mm"
        if(!isAlipay && !isWechat) return null
        val isRefund=content.contains("退款")
        val isIncome=isRefund || Regex("收款|收入|到账|转入").containsMatchIn(content)
        val isExpense=Regex("支付|付款|支出|消费|转出").containsMatchIn(content)
        if(!isIncome && !isExpense) return null
        val amount=amountPatterns.firstNotNullOfOrNull { p -> p.find(content)?.groupValues?.get(1)?.let(Money::parse) } ?: return null
        if(amount<=0) return null
        val kind=if(isIncome) TxnKind.INCOME else TxnKind.EXPENSE
        val account=accounts.firstOrNull { it.name.contains(if(isAlipay) "支付宝" else "微信") && !it.archived } ?: return null
        val rule=rules.firstOrNull { content.contains(it.keyword,ignoreCase=true) }
        val categoryId=rule?.categoryId ?: categories.firstOrNull { it.kind==kind && it.name=="其他" }?.id
        val merchant=Regex("向\\s*(.+?)\\s*付款").find(content)?.groupValues?.get(1)
            ?: title.replace(Regex("支付宝|微信支付|微信|支付消息"),"").trim().ifBlank { "未知商户" }
        val fingerprint=LedgerRepository.fingerprint(listOf(packageName,key,kind,amount.toString()))
        return TransactionEntity(
            kind=kind,
            nature=if(isRefund) TxnNature.REFUND_UNLINKED else TxnNature.NORMAL,
            amountMinor=amount,
            accountId=rule?.accountId ?: account.id,
            categoryId=categoryId,
            merchant=merchant,
            occurredAt=postedAt,
            status=if(autoAdd && !isRefund) TxnStatus.CONFIRMED else TxnStatus.PENDING,
            source=if(isAlipay) TxnSource.ALIPAY_NOTIFICATION else TxnSource.WECHAT_NOTIFICATION,
            sourcePackage=packageName,
            externalId=key,
            fingerprint=fingerprint,
            rawText=content.take(4000),
        )
    }
}
