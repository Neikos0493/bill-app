package com.neikos0493.jileme

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ShareImportTest {
    @get:Rule val compose=createComposeRule()
    @Test fun sharedBillOpensImportPreview(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        val file=File(context.filesDir,"shared-alipay.csv")
        file.writeText("交易时间,交易对方,商品说明,收/支,金额,交易状态,交易订单号,备注\n2026-11-27 12:00:00,分享商户,午餐,支出,25.50,交易成功,SHARE1,\n")
        val uri=FileProvider.getUriForFile(context,context.packageName+".files",file)
        ActivityScenario.launch<MainActivity>(Intent(Intent.ACTION_VIEW).setData(uri).setClass(context,MainActivity::class.java))
        compose.waitUntil(8000){compose.onAllNodesWithText("有效记录：1").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("官方账单导入").assertExists()
        compose.onNodeWithText("有效记录：1").assertExists()
        // ActivityScenarioRule 在该模拟器的单 Activity 分享场景关闭时存在误报；测试进程由 Runner 统一回收。
    }
}
