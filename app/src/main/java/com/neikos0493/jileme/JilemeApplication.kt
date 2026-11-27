package com.neikos0493.jileme

import android.app.Application
import com.neikos0493.jileme.data.JilemeDatabase
import com.neikos0493.jileme.data.LedgerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JilemeApplication : Application() {
    val database by lazy { JilemeDatabase.get(this) }
    val ledger by lazy { LedgerRepository(database) }
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch { ledger.ensureDefaults() }
    }
}
