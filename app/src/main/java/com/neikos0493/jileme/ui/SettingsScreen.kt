package com.neikos0493.jileme.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.neikos0493.jileme.BuildConfig
import com.neikos0493.jileme.domain.Money

@Composable fun SettingsScreen(state:AppUiState,viewModel:AppViewModel,onAccounts:()->Unit,onCategories:()->Unit,onImport:()->Unit,onSecurity:()->Unit){
    val context=LocalContext.current;var showBudget by remember{mutableStateOf(false)}
    val backupCreate=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){it?.let(viewModel::exportBackup)}
    val backupOpen=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(viewModel::restoreBackup)}
    val csvCreate=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){it?.let(viewModel::exportCsv)}
    val installPermission=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){ }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Text("我的",style=MaterialTheme.typography.headlineMedium)}
        item{SectionCard("记账设置"){ListItem(headlineContent={Text("账户与余额")},modifier=Modifier.clickable(onClick=onAccounts));ListItem(headlineContent={Text("分类与商户规则")},modifier=Modifier.clickable(onClick=onCategories));ListItem(headlineContent={Text("月度预算")},supportingContent={Text(if(state.settings.monthlyBudgetMinor>0)Money.format(state.settings.monthlyBudgetMinor)else"未设置")},modifier=Modifier.clickable{showBudget=true})}}
        item{SectionCard("自动记账"){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text("自动添加进账单");Text("信息不完整时仍需确认",style=MaterialTheme.typography.bodySmall)};Switch(state.settings.autoAddNotifications,viewModel::setAutoAdd)};OutlinedButton({context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))},Modifier.fillMaxWidth()){Text("开启通知读取权限")};ListItem(headlineContent={Text("导入官方账单")},modifier=Modifier.clickable(onClick=onImport))}}
        item{SectionCard("数据与安全"){ListItem(headlineContent={Text("指纹与手势解锁")},supportingContent={Text(when(state.settings.lockType){"BIOMETRIC"->"指纹";"GESTURE"->"手势";else->"关闭"})},modifier=Modifier.clickable(onClick=onSecurity));OutlinedButton({backupCreate.launch("jileme-backup.json")},Modifier.fillMaxWidth()){Text("导出完整备份")};OutlinedButton({backupOpen.launch(arrayOf("application/json","*/*"))},Modifier.fillMaxWidth()){Text("恢复备份")};OutlinedButton({csvCreate.launch("jileme.csv")},Modifier.fillMaxWidth()){Text("导出 CSV")}}}
        item{SectionCard("外观"){StringSelector("主题",when(state.settings.themeMode){"LIGHT"->"浅色";"DARK"->"深色";else->"跟随系统"},listOf("SYSTEM" to "跟随系统","LIGHT" to "浅色","DARK" to "深色"),onSelect=viewModel::setTheme)}}
        item{SectionCard("应用更新"){Text("当前版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})");Button({viewModel.checkUpdate()},Modifier.fillMaxWidth()){Text("检查更新")};state.downloadProgress?.let{LinearProgressIndicator(progress={it/100f},modifier=Modifier.fillMaxWidth());Text("正在下载 $it%")};Text("账单数据仅保存在本地，检查更新时只访问 GitHub。",style=MaterialTheme.typography.bodySmall)}}
        item{Spacer(Modifier.height(80.dp))}
    }
    if(showBudget){var value by remember{mutableStateOf(Money.input(state.settings.monthlyBudgetMinor))};AlertDialog(onDismissRequest={showBudget=false},title={Text("月度预算")},text={MoneyField(value,{value=it})},confirmButton={TextButton({Money.parse(value)?.let(viewModel::setBudget);showBudget=false}){Text("保存")}},dismissButton={TextButton({showBudget=false}){Text("取消")}})}
    state.updateInfo?.let{info->AlertDialog(onDismissRequest=viewModel::clearUpdate,title={Text("新版本 ${info.versionName}")},text={Text(info.releaseNotes.ifBlank{"发现新版本，是否下载？"})},confirmButton={TextButton({if(viewModel.state.value.downloadProgress==null){val managerAllows=runCatching{context.packageManager.canRequestPackageInstalls()}.getOrDefault(true);if(managerAllows)viewModel.downloadUpdate()else installPermission.launch(viewModel.updatePermissionIntent())}}){Text("下载并安装")}},dismissButton={TextButton({context.startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/Neikos0493/bill-app/releases/latest")));viewModel.clearUpdate()}){Text("GitHub")}})}
}

