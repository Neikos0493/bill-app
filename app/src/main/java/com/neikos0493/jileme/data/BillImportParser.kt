package com.neikos0493.jileme.data

import com.neikos0493.jileme.domain.Money
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

data class ImportPreview(val source: String, val rows: List<TransactionEntity>, val errors: List<String>, val fileName: String="") {
    val duplicates: List<TransactionEntity> get() = emptyList()
}

object BillImportParser {
    fun parse(fileName: String, bytes: ByteArray, accounts: List<AccountEntity>, categories: List<CategoryEntity>, rules: List<CategoryRuleEntity>): ImportPreview {
        val table=readTable(fileName,bytes)
        if(table.isEmpty()) return ImportPreview("",emptyList(),listOf("文件为空或无法识别"))
        val headerIndex=table.indexOfFirst { row -> row.any { it.trim()=="交易时间" } && row.any { it.contains("收/支") } }
        if(headerIndex<0) return ImportPreview("",emptyList(),listOf("未找到支付宝或微信账单的表头"))
        val headers=table[headerIndex].map { it.trim().replace(" ","") }
        fun index(vararg names: String)=headers.indexOfFirst { h -> names.any { h.equals(it,true) } }
        val timeI=index("交易时间","交易创建时间")
        val directionI=index("收/支")
        val amountI=index("金额","金额(元)","金额（元）")
        val merchantI=index("交易对方")
        val productI=index("商品说明","商品")
        val statusI=index("交易状态","当前状态")
        val orderI=index("交易订单号","交易单号")
        val noteI=index("备注")
        val source=if(headers.any { it=="交易订单号" }) TxnSource.ALIPAY_IMPORT else TxnSource.WECHAT_IMPORT
        val accountName=if(source==TxnSource.ALIPAY_IMPORT) "支付宝" else "微信"
        val account=accounts.firstOrNull { it.name.contains(accountName) && !it.archived } ?: return ImportPreview(source,emptyList(),listOf("请先在账户管理中添加$accountName"))
        val errors=mutableListOf<String>(); val rows=mutableListOf<TransactionEntity>()
        table.drop(headerIndex+1).forEachIndexed { offset,row ->
            fun value(i: Int)=if(i>=0 && i<row.size) row[i].trim() else ""
            if(row.all { it.isBlank() }) return@forEachIndexed
            val line=headerIndex+offset+2
            try {
                val direction=value(directionI)
                if(direction.contains("不计收支")) return@forEachIndexed
                val kind=when { direction.contains("支出") -> TxnKind.EXPENSE; direction.contains("收入") -> TxnKind.INCOME; else -> return@forEachIndexed }
                val amount=Money.parse(value(amountI).replace("¥","").replace("￥","")) ?: throw IllegalArgumentException("金额无效")
                val time=parseTime(value(timeI))
                val merchant=value(merchantI).ifBlank { value(productI) }.ifBlank { "未知商户" }
                val content=(merchant+" "+value(productI)+" "+value(noteI))
                val rule=rules.firstOrNull { content.contains(it.keyword,true) }
                val category=rule?.categoryId ?: categories.firstOrNull { it.kind==kind && it.name=="其他" }?.id
                val external=value(orderI).ifBlank { null }
                val status=value(statusI)
                val fingerprint=LedgerRepository.fingerprint(listOf(source,external.orEmpty(),time.toString(),amount.toString(),merchant,kind))
                rows += TransactionEntity(kind=kind,nature=if(status.contains("退款")) TxnNature.REFUND_UNLINKED else TxnNature.NORMAL,amountMinor=amount,accountId=rule?.accountId ?: account.id,categoryId=category,merchant=merchant,note=value(noteI),occurredAt=time,source=source,externalId=external,fingerprint=fingerprint,rawText=row.joinToString(","))
            } catch(e: Exception) { errors += "第 $line 行：${e.message ?: "解析失败"}" }
        }
        return ImportPreview(source,rows,errors,fileName)
    }

    private fun parseTime(value: String): Long {
        val formats=listOf("yyyy-MM-dd HH:mm:ss","yyyy/M/d H:mm:ss","yyyy-MM-dd H:mm:ss")
        formats.forEach { pattern -> runCatching { return LocalDateTime.parse(value.trim(),DateTimeFormatter.ofPattern(pattern)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() } }
        value.toBigDecimalOrNull()?.let { serial -> val epoch=java.time.LocalDate.of(1899,12,30).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(); return epoch+serial.multiply(java.math.BigDecimal(86400000)).setScale(0,java.math.RoundingMode.HALF_UP).longValueExact() }
        throw IllegalArgumentException("时间无效：$value")
    }

    private fun readTable(fileName: String, bytes: ByteArray): List<List<String>> {
        return if(fileName.endsWith(".xlsx",true)) XlsxReader.read(bytes)
        else if(fileName.endsWith(".zip",true)) {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry=zip.nextEntry
                while(entry!=null) {
                    if(!entry.isDirectory && entry.name.endsWith(".csv",true)) return CsvParser.parse(decode(zip.readBytes()))
                    entry=zip.nextEntry
                }
                emptyList()
            }
        } else CsvParser.parse(decode(bytes))
    }

    private fun decode(bytes: ByteArray): String {
        val raw=if(bytes.size>=3 && bytes[0]==0xEF.toByte() && bytes[1]==0xBB.toByte() && bytes[2]==0xBF.toByte()) bytes.copyOfRange(3,bytes.size) else bytes
        return try {
            Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(raw)).toString()
        } catch(_: CharacterCodingException) { String(raw,java.nio.charset.Charset.forName("GBK")) }
    }
}

object CsvParser {
    fun parse(text: String): List<List<String>> {
        val rows=mutableListOf<List<String>>(); var row=mutableListOf<String>(); val field=StringBuilder(); var quoted=false; var i=0
        while(i<text.length) {
            val c=text[i]
            if(quoted) {
                if(c=='"' && i+1<text.length && text[i+1]=='"') { field.append('"'); i++ }
                else if(c=='"') quoted=false else field.append(c)
            } else when(c) {
                '"' -> quoted=true
                ',' -> { row.add(field.toString()); field.setLength(0) }
                '\r','\n' -> {
                    if(c=='\r' && i+1<text.length && text[i+1]=='\n') i++
                    row.add(field.toString()); field.setLength(0)
                    if(row.any { it.isNotBlank() }) rows.add(row)
                    row=mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        row.add(field.toString()); if(row.any { it.isNotBlank() }) rows.add(row)
        return rows
    }
}

object XlsxReader {
    fun read(bytes: ByteArray): List<List<String>> {
        val files=mutableMapOf<String,ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry=zip.nextEntry
            while(entry!=null) { if(!entry.isDirectory) files[entry.name]=zip.readBytes(); entry=zip.nextEntry }
        }
        val shared=files["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val sheet=files["xl/worksheets/sheet1.xml"] ?: return emptyList()
        return parseSheet(sheet,shared)
    }
    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val result=mutableListOf<String>(); val current=StringBuilder(); var inside=false
        val parser=org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser().apply { setInput(ByteArrayInputStream(bytes),"UTF-8") }
        var event=parser.eventType
        while(event!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when(event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> if(parser.name=="si") { current.setLength(0); inside=true }
                org.xmlpull.v1.XmlPullParser.TEXT -> if(inside) current.append(parser.text)
                org.xmlpull.v1.XmlPullParser.END_TAG -> if(parser.name=="si") { result.add(current.toString()); inside=false }
            }
            event=parser.next()
        }
        return result
    }
    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val rows=mutableListOf<List<String>>(); var row=mutableListOf<String>(); var cellIndex=0; var type=""; var value=StringBuilder(); var inValue=false
        val parser=org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser().apply { setInput(ByteArrayInputStream(bytes),"UTF-8") }
        fun colIndex(ref: String): Int { var n=0; ref.takeWhile { it.isLetter() }.forEach { n=n*26+(it.uppercaseChar()-'A'+1) }; return n-1 }
        var event=parser.eventType
        while(event!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when(event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> when(parser.name) {
                    "row" -> row=mutableListOf()
                    "c" -> { cellIndex=colIndex(parser.getAttributeValue(null,"r") ?: ""); type=parser.getAttributeValue(null,"t") ?: ""; value=StringBuilder() }
                    "v","t" -> inValue=true
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> if(inValue) value.append(parser.text)
                org.xmlpull.v1.XmlPullParser.END_TAG -> when(parser.name) {
                    "v","t" -> inValue=false
                    "c" -> {
                        while(row.size<cellIndex) row.add("")
                        val raw=value.toString()
                        row.add(if(type=="s") raw.toIntOrNull()?.let { shared.getOrNull(it) }.orEmpty() else raw)
                    }
                    "row" -> rows.add(row)
                }
            }
            event=parser.next()
        }
        return rows
    }
}






