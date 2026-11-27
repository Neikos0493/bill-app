package com.neikos0493.jileme.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neikos0493.jileme.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun CategoriesScreen(state: AppUiState,viewModel:AppViewModel,onBack:()->Unit){
    var tab by remember{mutableStateOf(0)};var addCategory by remember{mutableStateOf(false)};var addRule by remember{mutableStateOf(false)}
    val kind=if(tab==0)TxnKind.EXPENSE else TxnKind.INCOME
    Scaffold(topBar={TopAppBar(title={Text("分类与规则")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")}})},floatingActionButton={FloatingActionButton({addCategory=true}){Icon(Icons.Default.Add,"添加分类")}}){padding->LazyColumn(Modifier.padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){SegmentedButton(tab==0,{tab=0},SegmentedButtonDefaults.itemShape(0,2)){Text("支出分类")};SegmentedButton(tab==1,{tab=1},SegmentedButtonDefaults.itemShape(1,2)){Text("收入分类")}}}
        items(state.categories.filter{it.kind==kind}.size){i->val c=state.categories.filter{it.kind==kind}[i];SectionCard{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(c.archived)"${c.name}（停用）" else c.name);TextButton({viewModel.saveCategory(c.copy(archived=!c.archived))}){Text(if(c.archived)"启用" else "停用")}}}}
        item{HorizontalDivider();Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("商户分类规则",style=MaterialTheme.typography.titleMedium);TextButton({addRule=true}){Text("添加规则")}};Text("通知和导入内容包含关键词时自动匹配",style=MaterialTheme.typography.bodySmall)}
        items(state.rules.size){i->val r=state.rules[i];SectionCard{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(r.keyword);Text("分类：${state.categories.firstOrNull{it.id==r.categoryId}?.name.orEmpty()}",style=MaterialTheme.typography.bodySmall)};TextButton({viewModel.deleteRule(r.id)}){Text("删除")}}}}
    }}
    if(addCategory){var name by remember{mutableStateOf("")};AlertDialog(onDismissRequest={addCategory=false},title={Text("添加分类")},text={OutlinedTextField(name,{name=it},label={Text("分类名称")})},confirmButton={TextButton({if(name.isNotBlank())viewModel.saveCategory(CategoryEntity(name=name.trim(),kind=kind));addCategory=false}){Text("保存")}},dismissButton={TextButton({addCategory=false}){Text("取消")}})}
    if(addRule){var keyword by remember{mutableStateOf("")};var categoryId by remember{mutableStateOf(state.categories.firstOrNull{it.kind==TxnKind.EXPENSE&&!it.archived}?.id?:0)};var accountId by remember{mutableStateOf(0L)};AlertDialog(onDismissRequest={addRule=false},title={Text("添加商户规则")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(keyword,{keyword=it},label={Text("关键词")});StringSelector("目标分类",state.categories.firstOrNull{it.id==categoryId}?.name.orEmpty(),state.categories.filter{!it.archived}.map{it.id.toString() to it.name}){categoryId=it.toLong()};StringSelector("指定账户（可选）",state.accounts.firstOrNull{it.id==accountId}?.name?:"不指定",listOf("0" to "不指定")+state.accounts.map{it.id.toString() to it.name}){accountId=it.toLong()}}},confirmButton={TextButton({if(keyword.isNotBlank()&&categoryId!=0L)viewModel.saveRule(CategoryRuleEntity(keyword=keyword.trim(),categoryId=categoryId,accountId=if(accountId==0L)null else accountId));addRule=false}){Text("保存")}},dismissButton={TextButton({addRule=false}){Text("取消")}})}
}

