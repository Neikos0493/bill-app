package com.neikos0493.jileme

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSmokeTest {
    @get:Rule val rule=createAndroidComposeRule<MainActivity>()
    @Test fun launchAndCreateExpense(){
        rule.onNodeWithText("记了么").assertIsDisplayed()
        rule.onAllNodesWithContentDescription("记一笔").onFirst().performClick()
        rule.onNodeWithText("金额").performTextInput("12.34")
        rule.onNodeWithText("保存").performScrollTo().performClick()
        rule.waitForIdle();rule.onRoot().printToLog("JILEME_AFTER_SAVE")
        rule.onNodeWithText("记了么").assertIsDisplayed()
    }

    @Test fun aaExpenseAcrossAccounts(){
        rule.onAllNodesWithContentDescription("记一笔").onFirst().performClick()
        rule.onNodeWithText("金额").performTextInput("300")
        rule.onNodeWithText("商户／交易对方").performTextInput("AA测试聚餐")
        rule.onNodeWithContentDescription("AA开关").performScrollTo().performClick()
        rule.onNodeWithText("添加回款").performScrollTo().performClick()
        rule.onNodeWithText("收款金额").performScrollTo().performTextInput("120")
        rule.onNodeWithText("收款去向").performScrollTo().performClick()
        rule.onNodeWithText("微信支付").performClick()
        rule.onNodeWithText("保存").performScrollTo().performClick()
        rule.waitForIdle()
        val db=com.neikos0493.jileme.data.JilemeDatabase.get(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        val expense=kotlinx.coroutines.runBlocking{db.dao.transactions()}.first{it.merchant=="AA测试聚餐"}
        val link=kotlinx.coroutines.runBlocking{db.dao.linksForExpense(expense.id)}.single()
        val receipt=kotlinx.coroutines.runBlocking{db.dao.transaction(link.offsetTransactionId)}!!
        val account=kotlinx.coroutines.runBlocking{db.dao.accounts()}.first{it.id==receipt.accountId}
        org.junit.Assert.assertEquals(12000,receipt.amountMinor)
        org.junit.Assert.assertTrue(account.name.contains("微信"))
        org.junit.Assert.assertEquals(com.neikos0493.jileme.data.LinkKind.AA,link.kind)
        }
}
