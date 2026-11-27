package com.neikos0493.jileme.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neikos0493.jileme.data.*
import com.neikos0493.jileme.domain.LedgerCalculator
import com.neikos0493.jileme.domain.Money
import java.text.SimpleDateFormat
import java.util.*

private data class ReceiptDraft(val id: Long=0,val amount: String="",val accountId: Long=0,val payer: String="")

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EditorScreen(state: AppUiState, transactionId: Long, viewModel: AppViewModel, onBack: ()->Unit) {
    val existing=state.transactions.firstOrNull{it.id==transactionId}
    var loaded by remember(transactionId){mutableStateOf(false)}
    var kind by remember { mutableStateOf(if(existing?.kind==TxnKind.TRANSFER) TxnKind.TRANSFER else existing?.kind ?: TxnKind.EXPENSE) }
    var amount by remember { mutableStateOf("") }; var accountId by remember { mutableStateOf(0L) }; var toAccountId by remember { mutableStateOf(0L) }
    var categoryId by remember { mutableStateOf(0L) }; var merchant by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
    var occurredAt by remember { mutableStateOf(System.currentTimeMillis()) }; var aaEnabled by remember { mutableStateOf(false) }; var refund by remember { mutableStateOf(false) }; var aaReceipt by remember { mutableStateOf(false) }
    var refundExpenseId by remember { mutableStateOf(0L) }; var feeMode by remember { mutableStateOf(FeeMode.NONE) }; var feeRate by remember { mutableStateOf("") }; var feeFixed by remember { mutableStateOf("") }
    var receipts by remember { mutableStateOf(listOf<ReceiptDraft>()) }; var error by remember { mutableStateOf<String?>(null) }; var showDelete by remember { mutableStateOf(false) }
    val context=LocalContext.current
    LaunchedEffect(existing,state.accounts,state.categories) {
        if(accountId==0L) accountId=state.accounts.firstOrNull{!it.archived}?.id ?: 0
        if(toAccountId==0L) toAccountId=state.accounts.firstOrNull{!it.archived && it.id!=accountId}?.id ?: 0
        if(categoryId==0L) categoryId=state.categories.firstOrNull{it.kind==kind && !it.archived}?.id ?: 0
        if(existing!=null && !loaded) {
            amount=Money.input(existing.amountMinor);accountId=existing.accountId;toAccountId=existing.toAccountId ?: 0;categoryId=existing.categoryId ?: 0
            merchant=existing.merchant;note=existing.note;occurredAt=existing.occurredAt;kind=existing.kind;aaEnabled=existing.aaEnabled
            refund=existing.nature==TxnNature.REFUND;aaReceipt=existing.nature==TxnNature.AA_RECEIPT;feeMode=existing.feeMode;feeRate=existing.feeRate;feeFixed=if(existing.feeAmountMinor>0) Money.input(existing.feeAmountMinor) else ""
            refundExpenseId=state.links.firstOrNull{it.offsetTransactionId==existing.id}?.expenseId ?: 0
            receipts=state.links.filter{it.expenseId==existing.id && it.kind==LinkKind.AA}.mapNotNull{link->state.transactions.firstOrNull{it.id==link.offsetTransactionId}?.let{ReceiptDraft(it.id,Money.input(it.amountMinor),it.accountId,it.merchant)}}
            loaded=true
        } else if(transactionId==0L) loaded=true
    }
    val accountOptions=state.accounts.filter{!it.archived}.map{it.id.toString() to it.name}
    val categoryOptions=state.categories.filter{!it.archived && it.kind==kind}.map{it.id.toString() to it.name}
    val expenseOptions=state.transactions.filter{it.kind==TxnKind.EXPENSE && it.status==TxnStatus.CONFIRMED && it.id!=transactionId}.take(200).map{it.id.toString() to "${SimpleDateFormat("MM-dd",Locale.CHINA).format(Date(it.occurredAt))} ${it.merchant.ifBlank{"支出"}} ${Money.format(it.amountMinor)}"}
    val feePreview=LedgerCalculator.fee(Money.parse(amount)?:0,feeMode,Money.parse(feeFixed)?:0,feeRate)
    Scaffold(topBar={TopAppBar(title={Text(if(transactionId==0L)"记一笔" else if(existing?.status==TxnStatus.PENDING)"确认预添加" else "编辑账单")},navigationIcon={IconButton(onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回")}},actions={if(existing!=null)IconButton({showDelete=true}){Icon(Icons.Default.Delete,"删除")}})}) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){listOf(TxnKind.EXPENSE to "支出",TxnKind.INCOME to "收入",TxnKind.TRANSFER to "转账").forEachIndexed{i,item->SegmentedButton(selected=kind==item.first,onClick={kind=item.first;categoryId=state.categories.firstOrNull{it.kind==kind&&!it.archived}?.id?:0},shape=SegmentedButtonDefaults.itemShape(i,3)){Text(item.second)}}}
            MoneyField(amount,{amount=it})
            StringSelector(if(kind==TxnKind.TRANSFER)"转出账户" else if(kind==TxnKind.INCOME)"到账账户" else "支付账户",state.accounts.firstOrNull{it.id==accountId}?.name.orEmpty(),accountOptions){accountId=it.toLong()}
            if(kind==TxnKind.TRANSFER){StringSelector("转入账户",state.accounts.firstOrNull{it.id==toAccountId}?.name.orEmpty(),accountOptions){toAccountId=it.toLong()}
                StringSelector("手续费",when(feeMode){FeeMode.FIXED->"固定金额";FeeMode.PERCENT->"百分比";else->"无手续费"},listOf(FeeMode.NONE to "无手续费",FeeMode.FIXED to "固定金额",FeeMode.PERCENT to "百分比")){feeMode=it}
                if(feeMode==FeeMode.FIXED)MoneyField(feeFixed,{feeFixed=it},"手续费金额")
                if(feeMode==FeeMode.PERCENT)OutlinedTextField(feeRate,{feeRate=it},label={Text("费率（%）")},suffix={Text("%")},modifier=Modifier.fillMaxWidth())
                if(feeMode!=FeeMode.NONE)Text("手续费预览：${Money.format(feePreview)}；实际到账：${Money.format(Money.parse(amount)?:0)}",color=MaterialTheme.colorScheme.secondary)
            } else {
                StringSelector("分类",state.categories.firstOrNull{it.id==categoryId}?.name.orEmpty(),categoryOptions){categoryId=it.toLong()}
                OutlinedTextField(merchant,{merchant=it},label={Text(if(kind==TxnKind.EXPENSE)"商户／交易对方" else "付款人／来源")},modifier=Modifier.fillMaxWidth())
            }
            OutlinedTextField(note,{note=it},label={Text("备注")},modifier=Modifier.fillMaxWidth())
            val cal=Calendar.getInstance().apply{timeInMillis=occurredAt}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({DatePickerDialog(context,{_,y,m,d->val c=Calendar.getInstance().apply{timeInMillis=occurredAt;set(y,m,d)};occurredAt=c.timeInMillis},cal[Calendar.YEAR],cal[Calendar.MONTH],cal[Calendar.DAY_OF_MONTH]).show()}){Text(SimpleDateFormat("yyyy-MM-dd",Locale.CHINA).format(Date(occurredAt)))};OutlinedButton({TimePickerDialog(context,{_,h,m->val c=Calendar.getInstance().apply{timeInMillis=occurredAt;set(Calendar.HOUR_OF_DAY,h);set(Calendar.MINUTE,m)};occurredAt=c.timeInMillis},cal[Calendar.HOUR_OF_DAY],cal[Calendar.MINUTE],true).show()}){Text(SimpleDateFormat("HH:mm",Locale.CHINA).format(Date(occurredAt)))}}
            if(kind==TxnKind.EXPENSE){HorizontalDivider();Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("AA／分摊",fontWeight=FontWeight.Bold);Text("记录回款金额与去向，支持跨账户",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)};Switch(aaEnabled,{aaEnabled=it},modifier=Modifier.semantics{contentDescription="AA开关"})}
                if(aaEnabled){receipts.forEachIndexed{index,item->Card{Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("回款 ${index+1}");TextButton({receipts=receipts.filterIndexed{i,_->i!=index}}){Text("移除")}};MoneyField(item.amount,{v->receipts=receipts.mapIndexed{i,r->if(i==index)r.copy(amount=v)else r}},"收款金额");StringSelector("收款去向",state.accounts.firstOrNull{it.id==item.accountId}?.name.orEmpty(),accountOptions){id->receipts=receipts.mapIndexed{i,r->if(i==index)r.copy(accountId=id.toLong())else r}};OutlinedTextField(item.payer,{v->receipts=receipts.mapIndexed{i,r->if(i==index)r.copy(payer=v)else r}},label={Text("付款人（选填）")},modifier=Modifier.fillMaxWidth())}}}
                    TextButton({receipts=receipts+ReceiptDraft(accountId=state.accounts.firstOrNull{!it.archived}?.id?:0)}){Icon(Icons.Default.Add,null);Text("添加回款")}
                    val received=receipts.sumOf{Money.parse(it.amount)?:0};Text("已收 ${Money.format(received)} · 实际承担 ${Money.format((Money.parse(amount)?:0)-received)}",fontWeight=FontWeight.SemiBold)
                }
            }
            if(kind==TxnKind.INCOME){HorizontalDivider();Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("这是退款",fontWeight=FontWeight.Bold);Text("关联原支出，不计普通收入",style=MaterialTheme.typography.bodySmall)};Switch(refund,{refund=it;if(it)aaReceipt=false},modifier=Modifier.semantics{contentDescription="退款开关"})};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text("作为 AA 回款",fontWeight=FontWeight.Bold);Text("可关联其他账户产生的支出",style=MaterialTheme.typography.bodySmall)};Switch(aaReceipt,{aaReceipt=it;if(it)refund=false})};if(refund||aaReceipt){StringSelector("关联原支出",expenseOptions.firstOrNull{it.first==refundExpenseId.toString()}?.second.orEmpty(),expenseOptions){refundExpenseId=it.toLong()};if(refundExpenseId!=0L){val original=state.transactions.firstOrNull{it.id==refundExpenseId};if(original!=null)Text("原支出净额：${Money.format(LedgerCalculator.netExpense(original,state.transactions,state.links))}",color=MaterialTheme.colorScheme.secondary)}}}
            error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
            Button({
                val value=Money.parse(amount)
                when{value==null||value<=0->error="请输入有效金额";accountId==0L->error="请选择账户";kind==TxnKind.TRANSFER&&(toAccountId==0L||toAccountId==accountId)->error="转入与转出账户不能相同";kind==TxnKind.INCOME&&(refund||aaReceipt)&&refundExpenseId==0L->error="请选择关联原支出";else->{
                    val now=System.currentTimeMillis();val main=TransactionEntity(id=existing?.id?:0,kind=kind,nature=if(kind==TxnKind.INCOME&&refund)TxnNature.REFUND else if(kind==TxnKind.INCOME&&aaReceipt)TxnNature.AA_RECEIPT else existing?.nature?.takeIf{it==TxnNature.AA_UNLINKED||it==TxnNature.REFUND_UNLINKED||it==TxnNature.ADJUSTMENT}?:TxnNature.NORMAL,amountMinor=value,accountId=accountId,toAccountId=if(kind==TxnKind.TRANSFER)toAccountId else null,categoryId=if(kind==TxnKind.TRANSFER)null else categoryId,merchant=merchant,note=note,occurredAt=occurredAt,status=TxnStatus.CONFIRMED,source=existing?.source?:TxnSource.MANUAL,sourcePackage=existing?.sourcePackage.orEmpty(),externalId=existing?.externalId,fingerprint=existing?.fingerprint,rawText=existing?.rawText.orEmpty(),feeMode=if(kind==TxnKind.TRANSFER)feeMode else FeeMode.NONE,feeRate=feeRate,feeAmountMinor=if(kind==TxnKind.TRANSFER)feePreview else 0,aaEnabled=kind==TxnKind.EXPENSE&&aaEnabled,createdAt=existing?.createdAt?:now,updatedAt=now)
                    val receiptValues=if(main.aaEnabled)receipts.mapNotNull{r->val m=Money.parse(r.amount)?:return@mapNotNull null;if(m<=0)return@mapNotNull null;TransactionEntity(id=r.id,kind=TxnKind.INCOME,nature=TxnNature.AA_RECEIPT,amountMinor=m,accountId=r.accountId,merchant=r.payer,note="AA回款",occurredAt=occurredAt,status=TxnStatus.CONFIRMED,createdAt=now,updatedAt=now)}else emptyList()
                    viewModel.saveEntry(main,receiptValues,if(refund||aaReceipt)refundExpenseId else null);onBack()
                }}
            },Modifier.fillMaxWidth()){Text("保存")}
            if(existing?.status==TxnStatus.PENDING)OutlinedButton({viewModel.ignoreTransaction(existing.id);onBack()},Modifier.fillMaxWidth()){Text("忽略这条记录")}
            if(existing?.rawText?.isNotBlank()==true)SectionCard("原始内容"){Text(existing.rawText,style=MaterialTheme.typography.bodySmall)}
            Spacer(Modifier.height(40.dp))
        }
    }
    if(showDelete)AlertDialog(onDismissRequest={showDelete=false},title={Text("删除账单？")},text={Text("关联的 AA 或退款流水会保留并标记为未关联。")},confirmButton={TextButton({viewModel.deleteTransaction(transactionId);showDelete=false;onBack()}){Text("删除")}},dismissButton={TextButton({showDelete=false}){Text("取消")}})
}




