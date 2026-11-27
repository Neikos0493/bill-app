package com.neikos0493.jileme.data

import android.content.Context
import android.net.Uri
import com.neikos0493.jileme.domain.toYearMonth
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.File
import java.math.BigDecimal
import java.time.Instant

class BackupRepository(private val context: Context, private val db: JilemeDatabase, private val settings: SettingsRepository) {
    private val json=Json { prettyPrint=true; ignoreUnknownKeys=true }
    suspend fun export(uri: Uri) {
        val data=BackupData(accounts=db.dao.accounts(),categories=db.dao.categories(),transactions=db.dao.transactions(),links=db.dao.links(),rules=db.dao.rules(),settings=settings.export())
        context.contentResolver.openOutputStream(uri,"wt")!!.bufferedWriter().use { it.write(json.encodeToString(BackupData.serializer(),data)) }
    }
    suspend fun restore(uri: Uri) {
        val text=context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        val data=json.decodeFromString(BackupData.serializer(),text)
        require(data.version==1) { "不支持的备份版本：${data.version}" }
        val current=BackupData(accounts=db.dao.accounts(),categories=db.dao.categories(),transactions=db.dao.transactions(),links=db.dao.links(),rules=db.dao.rules(),settings=settings.export())
        File(context.filesDir,"before-restore-${System.currentTimeMillis()}.json").writeText(json.encodeToString(BackupData.serializer(),current))
        LedgerRepository(db).restore(data)
        settings.import(data.settings)
    }
    suspend fun exportCsv(uri: Uri) {
        val accounts=db.dao.accounts().associateBy { it.id }; val categories=db.dao.categories().associateBy { it.id }
        val links=db.dao.links().groupBy { it.offsetTransactionId }
        val rows=mutableListOf(listOf("时间","类型","性质","金额","账户","目标账户","分类","商户","备注","状态","来源","关联支出"))
        db.dao.transactions().forEach { t ->
            val type=when(t.kind){TxnKind.EXPENSE->"支出";TxnKind.INCOME->"收入";else->"转账"}
            rows.add(listOf(Instant.ofEpochMilli(t.occurredAt).toString(),type,t.nature,BigDecimal(t.amountMinor).movePointLeft(2).toPlainString(),accounts[t.accountId]?.name.orEmpty(),accounts[t.toAccountId]?.name.orEmpty(),categories[t.categoryId]?.name.orEmpty(),t.merchant,t.note,t.status,t.source,links[t.id]?.firstOrNull()?.expenseId?.toString().orEmpty()))
        }
        context.contentResolver.openOutputStream(uri,"wt")!!.bufferedWriter().use { out ->
            rows.forEach { row -> out.write(row.joinToString(",") { "\"${it.replace("\"","\"\"")}\"" }); out.newLine() }
        }
    }
}

