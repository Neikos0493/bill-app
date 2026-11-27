package com.neikos0493.jileme

import com.neikos0493.jileme.data.*
import com.neikos0493.jileme.services.NotificationPaymentParser
import org.junit.Assert.*
import org.junit.Test

class ParserTest {
    private val accounts=listOf(AccountEntity(1,"支付宝"),AccountEntity(2,"微信支付"))
    private val categories=listOf(CategoryEntity(1,"其他",TxnKind.EXPENSE),CategoryEntity(2,"其他",TxnKind.INCOME),CategoryEntity(3,"餐饮",TxnKind.EXPENSE))
    @Test fun `CSV handles quoted comma newline and escaped quotes`(){
        val rows=CsvParser.parse("a,b\r\n\"x,y\",\"hello\"\"world\nline1\nline2\"")
        assertEquals("x,y",rows[1][0]);assertEquals("hello\"world\nline1\nline2",rows[1][1])
    }
    @Test fun `Alipay official style CSV`(){
        val text="交易时间,交易对方,商品说明,收/支,金额,交易状态,交易订单号,备注\n2026-11-27 12:00:00,测试商户,午餐,支出,25.50,交易成功,ABC123,\n"
        val result=BillImportParser.parse("alipay.csv",text.toByteArray(),accounts,categories,listOf(CategoryRuleEntity(1,"测试",3)))
        assertEquals(1,result.rows.size);assertEquals(2550,result.rows[0].amountMinor);assertEquals(TxnKind.EXPENSE,result.rows[0].kind);assertEquals(3L,result.rows[0].categoryId)
    }
    @Test fun `Wechat official style CSV`(){
        val text="交易时间,交易类型,交易对方,商品,收/支,金额(元),支付方式,当前状态,交易单号,备注\n2026-11-27 12:00:00,商户消费,测试商户,晚餐,支出,¥30.00,零钱,支付成功,WX123,\n"
        val result=BillImportParser.parse("wechat.csv",text.toByteArray(),accounts,categories,emptyList())
        assertEquals(TxnSource.WECHAT_IMPORT,result.source);assertEquals(3000,result.rows[0].amountMinor);assertEquals(2L,result.rows[0].accountId)
    }
    @Test fun `notification requires amount and direction`(){
        val t=NotificationPaymentParser.parse("com.tencent.mm","微信支付","你已成功付款¥12.30","key",1000,false,accounts,categories,emptyList())
        assertNotNull(t);assertEquals(1230,t!!.amountMinor);assertEquals(TxnStatus.PENDING,t.status)
        assertNull(NotificationPaymentParser.parse("com.tencent.mm","微信","你有一条新消息","key2",1000,false,accounts,categories,emptyList()))
    }
    @Test fun `refund notification stays pending even with auto add`(){
        val t=NotificationPaymentParser.parse("com.eg.android.AlipayGphone","支付宝","退款金额10.00元已到账","r",1000,true,accounts,categories,emptyList())
        assertEquals(TxnKind.INCOME,t!!.kind);assertEquals(TxnStatus.PENDING,t.status);assertEquals(TxnNature.REFUND_UNLINKED,t.nature)
    }
}

