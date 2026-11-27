package com.neikos0493.jileme.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neikos0493.jileme.data.*
import com.neikos0493.jileme.domain.LedgerCalculator
import com.neikos0493.jileme.domain.toYearMonth
import java.time.YearMonth

@Composable fun TransactionsScreen(state: AppUiState, pendingOnly: Boolean=false, onAdd: ()->Unit, onTransaction: (Long)->Unit) {
    var month by remember { mutableStateOf(YearMonth.now()) }; var query by remember { mutableStateOf("") }; var kind by remember { mutableStateOf("ALL") };var accountFilter by remember{mutableStateOf("0")};var categoryFilter by remember{mutableStateOf("0")};var sourceFilter by remember{mutableStateOf("ALL")}
    val accounts=state.accounts.associateBy { it.id }; val categories=state.categories.associateBy { it.id }
    val filtered=state.transactions.filter { t ->
        t.status!=TxnStatus.IGNORED && (!pendingOnly || t.status==TxnStatus.PENDING) && (pendingOnly || t.occurredAt.toYearMonth()==month) && (kind=="ALL" || t.kind==kind) && (accountFilter=="0" || t.accountId==accountFilter.toLong()) && (categoryFilter=="0" || t.categoryId==categoryFilter.toLong()) && (sourceFilter=="ALL" || (if(sourceFilter=="NOTIFICATION")t.source.contains("NOTIFICATION")else if(sourceFilter=="IMPORT")t.source.contains("IMPORT")else t.source==TxnSource.MANUAL)) && (query.isBlank() || (t.merchant+t.note).contains(query,true))
    }
    Column(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(pendingOnly) "预添加记录" else "账单",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);IconButton(onAdd){Icon(Icons.Default.Add,"记一笔")}}
        if(!pendingOnly){Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){TextButton({month=month.minusMonths(1)}){Text("←")};Text("${month.year} 年 ${month.monthValue} 月",fontWeight=FontWeight.Bold);TextButton({month=month.plusMonths(1)}){Text("→")}}
            OutlinedTextField(query,{query=it},leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("搜索商户或备注")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("ALL" to "全部",TxnKind.EXPENSE to "支出",TxnKind.INCOME to "收入",TxnKind.TRANSFER to "转账").forEach{FilterChip(selected=kind==it.first,onClick={kind=it.first},label={Text(it.second)})}}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){StringSelector("账户",accounts[accountFilter.toLong()]?.name?:"全部账户",listOf("0" to "全部账户")+state.accounts.map{it.id.toString() to it.name},onSelect={accountFilter=it},modifier=Modifier.weight(1f));StringSelector("分类",categories[categoryFilter.toLong()]?.name?:"全部分类",listOf("0" to "全部分类")+state.categories.map{it.id.toString() to it.name},onSelect={categoryFilter=it},modifier=Modifier.weight(1f))}
            StringSelector("来源",when(sourceFilter){"NOTIFICATION"->"支付通知";"IMPORT"->"官方导入";"MANUAL"->"手动记录";else->"全部来源"},listOf("ALL" to "全部来源","MANUAL" to "手动记录","NOTIFICATION" to "支付通知","IMPORT" to "官方导入"),onSelect={sourceFilter=it})
        } else Text("确认后才会影响余额、预算和统计",color=MaterialTheme.colorScheme.secondary)
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(bottom=90.dp)) {
            items(filtered,key={it.id}){t->Card(Modifier.fillMaxWidth().clickable{onTransaction(t.id)}){TransactionRow(t,accounts[t.accountId]?.name.orEmpty(),categories[t.categoryId]?.name.orEmpty(),if(t.kind==TxnKind.EXPENSE) LedgerCalculator.netExpense(t,state.transactions,state.links) else t.amountMinor){onTransaction(t.id)}}}
            if(filtered.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(60.dp),contentAlignment=androidx.compose.ui.Alignment.Center){Text(if(pendingOnly) "没有待确认记录" else "本月暂无记录",color=MaterialTheme.colorScheme.secondary)} }
        }
    }
}




