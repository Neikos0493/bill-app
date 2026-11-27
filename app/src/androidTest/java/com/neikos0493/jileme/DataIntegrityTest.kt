package com.neikos0493.jileme

import android.content.Context
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neikos0493.jileme.data.*
import com.neikos0493.jileme.domain.GestureHasher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class DataIntegrityTest {
    @Test fun backupRestoreCsvAndGesture()=runTest{
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,JilemeDatabase::class.java).allowMainThreadQueries().build()
        val repository=LedgerRepository(db);repository.ensureDefaults()
        val account=db.dao.accounts().first();val category=db.dao.categories().first{it.kind==TxnKind.EXPENSE}
        val id=repository.saveEntry(TransactionEntity(kind=TxnKind.EXPENSE,amountMinor=12345,accountId=account.id,categoryId=category.id,merchant="备份测试",occurredAt=1000))
        val settings=SettingsRepository(context);val backup=BackupRepository(context,db,settings)
        val file=File(context.filesDir,"test-backup.json");file.writeText("")
        val uri=FileProvider.getUriForFile(context,context.packageName+".files",file)
        backup.export(uri);assertTrue(file.length()>100)
        repository.deleteTransaction(id);assertNull(db.dao.transaction(id))
        backup.restore(uri);assertEquals("备份测试",db.dao.transaction(id)?.merchant)
        val csv=File(context.filesDir,"test-export.csv");csv.writeText("");backup.exportCsv(FileProvider.getUriForFile(context,context.packageName+".files",csv));assertTrue(csv.readText().contains("备份测试"))
        val hash=GestureHasher.hash(listOf(0,1,2,4,8));assertTrue(GestureHasher.verify(listOf(0,1,2,4,8),hash));assertFalse(GestureHasher.verify(listOf(0,1,2,3),hash))
        file.delete();csv.delete();db.close()
    }

    @Test fun xlsxImport()=runTest{
        val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,JilemeDatabase::class.java).allowMainThreadQueries().build();val repository=LedgerRepository(db);repository.ensureDefaults()
        val headers=listOf("交易时间","交易对方","商品说明","收/支","金额","交易状态","交易订单号","备注")
        val values=listOf("2026-11-27 12:00:00","XLSX商户","午餐","支出","25.50","交易成功","XLSX1","")
        fun cell(ref:String,value:String)="<c r=\"$ref\" t=\"inlineStr\"><is><t>$value</t></is></c>"
        val xml="<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet><sheetData><row r=\"1\">"+headers.mapIndexed{i,v->cell(('A'+i).toString()+"1",v)}.joinToString("")+"</row><row r=\"2\">"+values.mapIndexed{i,v->cell(('A'+i).toString()+"2",v)}.joinToString("")+"</row></sheetData></worksheet>"
        val output=ByteArrayOutputStream();ZipOutputStream(output).use{zip->zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"));zip.write(xml.toByteArray());zip.closeEntry()}
        val result=BillImportParser.parse("test.xlsx",output.toByteArray(),db.dao.accounts(),db.dao.categories(),emptyList())
        assertEquals(1,result.rows.size);assertEquals(2550,result.rows[0].amountMinor);assertEquals("XLSX商户",result.rows[0].merchant)
        db.close()
    }
}
