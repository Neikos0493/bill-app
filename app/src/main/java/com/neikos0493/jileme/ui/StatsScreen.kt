package com.neikos0493.jileme.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neikos0493.jileme.data.*
import com.neikos0493.jileme.domain.*
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@Composable fun StatsScreen(state: AppUiState) {
    val month=YearMonth.now();val range=LedgerCalculator.monthRange(month)
    val monthTx=state.transactions.filter{it.status==TxnStatus.CONFIRMED&&it.occurredAt in range}
    val data=LedgerCalculator.categoryExpenses(month,state.transactions,state.links).entries.sortedByDescending{it.value};val total=data.sumOf{it.value}.coerceAtLeast(1)
    val names=state.categories.associateBy{it.id};val accounts=state.accounts.associateBy{it.id}
    val expenses=monthTx.filter{it.kind==TxnKind.EXPENSE}
    val merchants=expenses.groupBy{it.merchant.ifBlank{"未填写商户"}}.mapValues{entry->entry.value.sumOf{LedgerCalculator.netExpense(it,state.transactions,state.links)}}.entries.sortedByDescending{it.value}.take(5)
    val byAccount=expenses.groupBy{it.accountId}.mapValues{entry->entry.value.sumOf{LedgerCalculator.netExpense(it,state.transactions,state.links)}}.entries.sortedByDescending{it.value}
    val days=(1..month.lengthOfMonth()).map{day->val dayRange=month.atDay(day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()..month.atDay(day).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()-1;day to expenses.filter{it.occurredAt in dayRange}.sumOf{LedgerCalculator.netExpense(it,state.transactions,state.links)}}
    val maxDay=days.maxOf{it.second}.coerceAtLeast(1)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item{Text("统计",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("${month.year} 年 ${month.monthValue} 月",color=MaterialTheme.colorScheme.secondary)}
        item{SectionCard("本月净支出"){Text(Money.format(state.monthExpense),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold);Text("普通收入 ${Money.format(state.monthIncome)} · 结余 ${Money.format(state.monthIncome-state.monthExpense)}")}}
        item{SectionCard("每日支出趋势"){Canvas(Modifier.fillMaxWidth().height(150.dp)){val gap=4f;val bar=(size.width-gap*(days.size-1))/days.size;days.forEachIndexed{i,item->val h=size.height*(item.second.coerceAtLeast(0).toFloat()/maxDay);drawRect(if(item.second>0)Color(0xFF315C4C)else Color(0xFFEFF2EE),topLeft=Offset(i*(bar+gap),size.height-h),size=androidx.compose.ui.geometry.Size(bar,h))}};Text("1 日至 ${month.lengthOfMonth()} 日，最高单日 ${Money.format(maxDay)}",style=MaterialTheme.typography.bodySmall)}}
        item{SectionCard("分类占比"){if(data.isEmpty())Text("暂无支出") else Canvas(Modifier.fillMaxWidth().height(22.dp)){var x=0f;data.forEach{entry->val width=size.width*(entry.value.coerceAtLeast(0).toFloat()/total);drawRect(color=names[entry.key]?.color?.let{Color(it)}?:Color(0xFF9A6B38),topLeft=Offset(x,0f),size=androidx.compose.ui.geometry.Size(width,size.height));x+=width}}}}
        items(data.size){i->val entry=data[i];SectionCard{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(entry.key?.let{names[it]?.name}?:"转账手续费",fontWeight=FontWeight.SemiBold);Text(Money.format(entry.value))};LinearProgressIndicator(progress={(entry.value.coerceAtLeast(0).toFloat()/total).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())}}
        item{SectionCard("商户排行"){if(merchants.isEmpty())Text("暂无数据")else merchants.forEach{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(it.key);Text(Money.format(it.value))}}}}
        item{SectionCard("账户支出"){if(byAccount.isEmpty())Text("暂无数据")else byAccount.forEach{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(accounts[it.key]?.name.orEmpty());Text(Money.format(it.value))}}}}
        item{val aa=monthTx.filter{it.nature==TxnNature.AA_RECEIPT}.sumOf{it.amountMinor};val refunds=monthTx.filter{it.nature==TxnNature.REFUND}.sumOf{it.amountMinor};val transfers=monthTx.filter{it.kind==TxnKind.TRANSFER};SectionCard("AA、退款与手续费"){Text("本月 AA 回款 ${Money.format(aa)}");Text("本月关联退款 ${Money.format(refunds)}");Text("${transfers.size} 笔转账 · 手续费 ${Money.format(transfers.sumOf{it.feeAmountMinor})}")}}
        item{Spacer(Modifier.height(80.dp))}
    }
}
