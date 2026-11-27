package com.neikos0493.jileme.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.neikos0493.jileme.data.ImportPreview
import com.neikos0493.jileme.domain.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ImportScreen(state: AppUiState,viewModel:AppViewModel,onBack:()->Unit){
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){val context=viewModel.getApplication<android.app.Application>();val name=runCatching{context.contentResolver.query(uri,null,null,null,null)?.use{c->val i=c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);if(c.moveToFirst()&&i>=0)c.getString(i)else null}}.getOrNull()?:"bill.csv";viewModel.selectImport(name,uri)}}
    val p=state.importPreview
    Scaffold(topBar={TopAppBar(title={Text("官方账单导入")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")}})}){padding->LazyColumn(Modifier.padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{SectionCard("选择账单文件"){Text("支持支付宝、微信官方 CSV、ZIP 和 XLSX。解析在本地完成，不上传文件。");Button({picker.launch(arrayOf("*/*"))},Modifier.fillMaxWidth()){Text("选择文件")}}}
        p?.let{preview->item{SectionCard("导入预检"){Text("来源：${if(preview.source.contains("ALIPAY"))"支付宝" else if(preview.source.contains("WECHAT"))"微信" else "未知"}");Text("有效记录：${preview.rows.size}");Text("预计重复：${state.importDuplicates}");Text("解析异常：${preview.errors.size}");if(preview.errors.isNotEmpty())preview.errors.take(8).forEach{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(viewModel::confirmImport,enabled=preview.rows.isNotEmpty()){Text("确认导入")};OutlinedButton(viewModel::cancelImport){Text("取消")}}}}}
        item{SectionCard("去重与关联"){Text("导入时会优先使用交易单号生成指纹；重复记录不会再次入账。通知记录和导入记录的自动合并仅使用可靠匹配。")}}
    }}
}
