package com.neikos0493.jileme.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neikos0493.jileme.JilemeApplication
import com.neikos0493.jileme.data.*
import com.neikos0493.jileme.domain.*
import com.neikos0493.jileme.update.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth

private data class BaseData(val accounts: List<AccountEntity>,val categories: List<CategoryEntity>,val transactions: List<TransactionEntity>,val links: List<TransactionLinkEntity>,val rules: List<CategoryRuleEntity>)
private data class ExtraData(val preview: ImportPreview?,val duplicates: Int,val update: UpdateInfo?,val progress: Int?,val message: String?)

data class AppUiState(
    val loading: Boolean=true,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val links: List<TransactionLinkEntity> = emptyList(),
    val rules: List<CategoryRuleEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val balances: Map<Long,Long> = emptyMap(),
    val totalBalance: Long=0,
    val monthExpense: Long=0,
    val monthIncome: Long=0,
    val month: YearMonth = YearMonth.now(),
    val importPreview: ImportPreview?=null,
    val importDuplicates: Int=0,
    val updateInfo: UpdateInfo?=null,
    val downloadProgress: Int?=null,
    val message: String?=null,
)

class AppViewModel(application: Application): AndroidViewModel(application) {
    private val app=application as JilemeApplication
    private val repository=app.ledger
    private val settings=SettingsRepository(application)
    private val backup=BackupRepository(application,app.database,settings)
    private val updates=UpdateManager(application)
    private val mutableMessage=MutableStateFlow<String?>(null)
    private val preview=MutableStateFlow<ImportPreview?>(null)
    private val duplicateCount=MutableStateFlow(0)
    private val updateInfo=MutableStateFlow<UpdateInfo?>(null)
    private val progress=MutableStateFlow<Int?>(null)

    private val base=combine(repository.accounts,repository.categories,repository.transactions,repository.links,repository.rules) { a,c,t,l,r -> BaseData(a,c,t,l,r) }
    private val extras=combine(preview,duplicateCount,updateInfo,progress,mutableMessage) { p,d,u,pr,m -> ExtraData(p,d,u,pr,m) }
    val state: StateFlow<AppUiState> = combine(base,settings.settings,extras) { b,s,e ->
        AppUiState(false,b.accounts,b.categories,b.transactions,b.links,b.rules,s,LedgerCalculator.balances(b.accounts,b.transactions),LedgerCalculator.totalBalance(b.accounts,b.transactions),LedgerCalculator.monthExpense(YearMonth.now(),b.transactions,b.links),LedgerCalculator.monthIncome(YearMonth.now(),b.transactions),YearMonth.now(),e.preview,e.duplicates,e.update,e.progress,e.message)
    }.stateIn(viewModelScope,SharingStarted.Eagerly,AppUiState())
    init { viewModelScope.launch(Dispatchers.IO) { repository.ensureDefaults() } }
    private fun launch(action: suspend () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        try { action() } catch (t: Throwable) { mutableMessage.value=t.message ?: "操作失败" }
    }
    fun consumeMessage(){mutableMessage.value=null}
    fun saveEntry(main: TransactionEntity, receipts: List<TransactionEntity>, refundExpenseId: Long?)=launch { repository.saveEntry(main,receipts,refundExpenseId); mutableMessage.value="已保存" }
    fun deleteTransaction(id: Long)=launch { repository.deleteTransaction(id); mutableMessage.value="已删除；关联流水已保留" }
    fun ignoreTransaction(id: Long)=launch { repository.ignoreTransaction(id) }
    fun saveAccount(value: AccountEntity)=launch { repository.dao.upsertAccount(value) }
    fun saveCategory(value: CategoryEntity)=launch { repository.dao.upsertCategory(value) }
    fun saveRule(value: CategoryRuleEntity)=launch { repository.dao.upsertRule(value) }
    fun deleteRule(id: Long)=launch { repository.dao.deleteRule(id) }
    fun setBudget(value: Long)=launch { settings.setBudget(value) }
    fun setAutoAdd(value: Boolean)=launch { settings.setAutoAdd(value) }
    fun setTheme(value: String)=launch { settings.setTheme(value) }
    fun setLock(type: String,hash: String="")=launch { settings.setLock(type,hash) }
    fun calibrate(account: AccountEntity, actual: Long)=launch {
        val balances=LedgerCalculator.balances(repository.dao.accounts(),repository.dao.transactions())
        val current=balances[account.id] ?: account.openingBalanceMinor
        val difference=actual-current
        if(difference!=0L) repository.saveEntry(TransactionEntity(kind=if(difference>0) TxnKind.INCOME else TxnKind.EXPENSE,nature=TxnNature.ADJUSTMENT,amountMinor=kotlin.math.abs(difference),accountId=account.id,note="余额校准",occurredAt=System.currentTimeMillis()))
    }

    fun selectImport(name: String, uri: Uri)=launch {
        val bytes=getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("无法读取文件")
        val result=BillImportParser.parse(name,bytes,repository.dao.accounts(),repository.dao.categories(),repository.dao.rules())
        val existing=repository.dao.transactions().mapNotNull { it.fingerprint }.toSet()
        duplicateCount.value=result.rows.count { it.fingerprint in existing }
        preview.value=result
    }
    fun cancelImport(){preview.value=null; duplicateCount.value=0}
    fun confirmImport()=launch {
        val p=preview.value ?: return@launch
        val result=repository.importTransactions(p.fileName.ifBlank { "导入文件" },p.source,p.rows,p.errors.size)
        mutableMessage.value="导入完成：新增 ${result.first}，重复 ${result.second}，失败 ${p.errors.size}"
        preview.value=null; duplicateCount.value=0
    }
    fun exportBackup(uri: Uri)=launch { backup.export(uri); mutableMessage.value="完整备份已导出" }
    fun restoreBackup(uri: Uri)=launch { backup.restore(uri); mutableMessage.value="备份已恢复" }
    fun exportCsv(uri: Uri)=launch { backup.exportCsv(uri); mutableMessage.value="CSV 已导出" }

    fun checkUpdate(manual: Boolean=true)=launch {
        settings.setLastUpdateCheck(System.currentTimeMillis())
        val info=try { updates.check() } catch(t: Throwable) { if(manual) throw IllegalStateException("无法连接 GitHub，请稍后重试或前往 Releases 手动下载") else null }
        if(info!=null && updates.isNewer(info)) { updateInfo.value=info; if(!manual) mutableMessage.value="发现新版本 ${info.versionName}" }
        else if(manual) mutableMessage.value="当前已是最新版本"
    }
    fun downloadUpdate()=launch {
        val info=updateInfo.value ?: return@launch
        if(!updates.canInstall()) { mutableMessage.value="请允许安装未知应用后重试"; return@launch }
        progress.value=0
        val file=updates.download(info) { progress.value=it }
        progress.value=null
        getApplication<Application>().startActivity(updates.installIntent(file))
    }
    fun clearUpdate(){updateInfo.value=null; progress.value=null}
    fun updatePermissionIntent()=updates.permissionIntent()
}






