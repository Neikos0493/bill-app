package com.neikos0493.jileme.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.neikos0493.jileme.data.*
import com.neikos0493.jileme.domain.Money
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun MoneyField(value: String, onValueChange: (String)->Unit, label: String="金额", modifier: Modifier=Modifier) {
    OutlinedTextField(value,onValueChange,label={Text(label)},prefix={Text("¥")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=modifier.fillMaxWidth())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun StringSelector(label: String, value: String, options: List<Pair<String,String>>, modifier: Modifier=Modifier, onSelect: (String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded,{expanded=!expanded},modifier=modifier) {
        OutlinedTextField(value=value,onValueChange={},readOnly=true,label={Text(label)},trailingIcon={ExposedDropdownMenuDefaults.TrailingIcon(expanded)},modifier=Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded,{expanded=false}) { options.forEach { DropdownMenuItem(text={Text(it.second)},onClick={onSelect(it.first);expanded=false}) } }
    }
}

@Composable fun SectionCard(title: String?=null, content: @Composable ColumnScope.()->Unit) {
    Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(title!=null) Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            content()
        }
    }
}

@Composable fun TransactionRow(t: TransactionEntity, account: String, category: String, net: Long, onClick: ()->Unit) {
    val income=t.kind==TxnKind.INCOME
    val title=t.merchant.ifBlank { when(t.kind){TxnKind.TRANSFER->"账户转账";else->category.ifBlank { "未分类" } } }
    val nature=when(t.nature){TxnNature.AA_RECEIPT->" · AA回款";TxnNature.AA_UNLINKED->" · 未关联AA";TxnNature.REFUND->" · 退款";TxnNature.REFUND_UNLINKED->" · 未关联退款";TxnNature.ADJUSTMENT->" · 余额校准";else->""}
    ListItem(
        headlineContent={Text(title+if(t.kind==TxnKind.EXPENSE && t.aaEnabled) " · AA" else "")},
        supportingContent={Text(SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(Date(t.occurredAt))+" · "+account+nature+if(t.status==TxnStatus.PENDING) " · 待确认" else "")},
        trailingContent={Column(horizontalAlignment=androidx.compose.ui.Alignment.End){Text((if(income) "+" else if(t.kind==TxnKind.EXPENSE) "-" else "")+Money.format(t.amountMinor),color=if(income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.SemiBold); if(t.kind==TxnKind.EXPENSE && net!=t.amountMinor) Text("净 ${Money.format(net)}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.secondary)}},
        modifier=Modifier.fillMaxWidth(),
        leadingContent={AssistChip(onClick=onClick,label={Text(when(t.kind){TxnKind.EXPENSE->"支";TxnKind.INCOME->"收";else->"转"})})},
    )
}

