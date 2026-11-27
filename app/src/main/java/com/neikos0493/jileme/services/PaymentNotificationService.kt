package com.neikos0493.jileme.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.neikos0493.jileme.data.JilemeDatabase
import com.neikos0493.jileme.data.LedgerRepository
import com.neikos0493.jileme.data.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class PaymentNotificationService : NotificationListenerService() {
    private val scope= CoroutineScope(SupervisorJob()+Dispatchers.IO)
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras=sbn.notification.extras
        val title=extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text=listOfNotNull(extras.getCharSequence(Notification.EXTRA_TEXT),extras.getCharSequence(Notification.EXTRA_BIG_TEXT)).joinToString(" ")
        scope.launch {
            val db=JilemeDatabase.get(applicationContext)
            val repository=LedgerRepository(db)
            repository.ensureDefaults()
            val settings=SettingsRepository(applicationContext).settings.first()
            val value=NotificationPaymentParser.parse(sbn.packageName,title,text,sbn.key,sbn.postTime,settings.autoAddNotifications,db.dao.accounts(),db.dao.categories(),db.dao.rules())
            if(value!=null) repository.saveNotification(value)
        }
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
