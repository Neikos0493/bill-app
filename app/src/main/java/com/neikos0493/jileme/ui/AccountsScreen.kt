package com.neikos0493.jileme.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neikos0493.jileme.data.*
import com.neikos0493.jileme.domain.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AccountsScreen(state: AppUiState, viewModel: AppViewModel, onBack: ()->Unit) {
    var editing by remember{mutableStateOf<AccountEntity?>(null)};var adding by remember{mutableStateOf(false)};var calibrate by remember{mutableStateOf<AccountEntity?>(null)}
    Scaffold(topBar={TopAppBar(title={Text("账户管理")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")}})},floatingActionButton={FloatingActionButton({adding=true}){Icon(Icons.Default.Add,"添加账户")}}){padding->
        LazyColumn(Modifier.padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) { items(state.accounts,key={it.id}){account->Card{Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(account.name,fontWeight=FontWeight.Bold);Text(if(account.archived)"已停用" else if(account.includeInTotal)"计入总余额" else "不计入总余额",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)};Text(Money.format(state.balances[account.id]?:0),style=MaterialTheme.typography.titleLarge)};Row{TextButton({editing=account}){Text("编辑")};TextButton({calibrate=account}){Text("余额校准")}}}}}}
    }
    if(adding||editing!=null)AccountDialog(editing,{adding=false;editing=null},{viewModel.saveAccount(it);adding=false;editing=null})
    calibrate?.let{account->var value by remember{mutableStateOf(Money.format(state.balances[account.id]?:0,false).replace(",",""))};AlertDialog(onDismissRequest={calibrate=null},title={Text("校准${account.name}")},text={MoneyField(value,{value=it},"实际余额")},confirmButton={TextButton({Money.parse(value)?.let{viewModel.calibrate(account,it)};calibrate=null}){Text("生成调整记录")}},dismissButton={TextButton({calibrate=null}){Text("取消")}})}
}
@Composable private fun AccountDialog(account: AccountEntity?,onDismiss:()->Unit,onSave:(AccountEntity)->Unit){
    var name by remember{mutableStateOf(account?.name.orEmpty())};var opening by remember{mutableStateOf(account?.openingBalanceMinor?.let{Money.input(it)}?:"0")};var include by remember{mutableStateOf(account?.includeInTotal?:true)};var archived by remember{mutableStateOf(account?.archived?:false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(account==null)"添加账户" else "编辑账户")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){OutlinedTextField(name,{name=it},label={Text("账户名称")});MoneyField(opening,{opening=it},"初始余额");Row{Text("计入总余额",Modifier.weight(1f));Switch(include,{include=it})};if(account!=null)Row{Text("停用账户",Modifier.weight(1f));Switch(archived,{archived=it})}}},confirmButton={TextButton({if(name.isNotBlank())onSave(AccountEntity(account?.id?:0,name.trim(),Money.parse(opening)?:0,include,archived,account?.color?:0xFF315C4C,account?.sortOrder?:0))}){Text("保存")}},dismissButton={TextButton(onDismiss){Text("取消")}})
}
