package com.neikos0493.jileme.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neikos0493.jileme.data.*
import com.neikos0493.jileme.domain.LedgerCalculator
import com.neikos0493.jileme.domain.Money

@Composable fun HomeScreen(state: AppUiState, onAdd: ()->Unit, onPending: ()->Unit, onTransaction: (Long)->Unit, onBudget: ()->Unit, onAccounts: ()->Unit) {
    val pending=state.transactions.count { it.status==TxnStatus.PENDING }
    val accounts=state.accounts.associateBy { it.id }; val categories=state.categories.associateBy { it.id }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text("记了么",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("本月收支概览",color=MaterialTheme.colorScheme.secondary)};IconButton(onAdd){Icon(Icons.Default.Add,contentDescription="记一笔")}} }
        item { Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primary)){Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("总余额",color=MaterialTheme.colorScheme.onPrimary.copy(alpha=.8f));Text(Money.format(state.totalBalance),style=MaterialTheme.typography.displaySmall,color=MaterialTheme.colorScheme.onPrimary,fontWeight=FontWeight.Bold);Text("账户 ${state.accounts.count{!it.archived}} 个",color=MaterialTheme.colorScheme.onPrimary.copy(alpha=.8f))}} }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)){SummaryCard("本月支出",state.monthExpense,Modifier.weight(1f));SummaryCard("本月收入",state.monthIncome,Modifier.weight(1f))} }
        item { val budget=state.settings.monthlyBudgetMinor; val used=state.monthExpense; SectionCard("月度预算"){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(budget>0) "已用 ${Money.format(used,false)}" else "尚未设置预算",fontWeight=FontWeight.SemiBold);TextButton(onBudget){Text(if(budget>0) "修改" else "设置")}};if(budget>0){LinearProgressIndicator(progress={ (used.toFloat()/budget).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth());Text("预算 ${Money.format(budget)} · 剩余 ${Money.format(budget-used)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)}} }
        if(pending>0) item { Card(Modifier.fillMaxWidth().clickable(onClick=onPending),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.tertiaryContainer)){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("$pending 条预添加记录等待确认");Text("去处理 →")}} }
        item { SectionCard("账户余额"){state.accounts.filter{!it.archived}.forEach{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(it.name);Text(Money.format(state.balances[it.id]?:0),fontWeight=FontWeight.Medium)}};TextButton(onAccounts){Text("管理账户")}} }
        item { Text("最近交易",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold) }
        items(state.transactions.filter{it.status!=TxnStatus.IGNORED}.take(8),key={it.id}){t->Card(Modifier.fillMaxWidth().clickable{onTransaction(t.id)}){TransactionRow(t,accounts[t.accountId]?.name.orEmpty(),categories[t.categoryId]?.name.orEmpty(),if(t.kind==TxnKind.EXPENSE) LedgerCalculator.netExpense(t,state.transactions,state.links) else t.amountMinor){onTransaction(t.id)}}}
        item { Spacer(Modifier.height(80.dp)) }
    }
}
@Composable private fun SummaryCard(label: String, amount: Long, modifier: Modifier) { Card(modifier){Column(Modifier.padding(16.dp)){Text(label,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.secondary);Text(Money.format(amount),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)}} }

