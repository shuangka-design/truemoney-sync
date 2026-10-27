package com.autolog.truemoneymm

import android.app.Notification
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.widget.Toast
import android.os.Handler
import android.os.Looper

class TrueMoneyNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        val fullMessage = "$bigText $subText".trim()

        if (!TransactionParser.isFinancialPackageOrTitle(pkg, title, fullMessage)) return

        val record = TransactionParser.parse(pkg, title, fullMessage) ?: return

        TransactionStore.saveRecord(applicationContext, record)
        TransactionStore.pendingAutoFillRecord = record

        Handler(Looper.getMainLooper()).post {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("amount", record.amount.toString()))
            Toast.makeText(
                applicationContext,
                "[${record.accountName}] จ่าย ${record.amount} บาท (คัดลอกยอดเงินแล้ว)",
                Toast.LENGTH_LONG
            ).show()
        }

        if (TransactionStore.isAutoLaunchEnabled(applicationContext)) {
            launchMoneyManager()
        }
    }

    private fun launchMoneyManager() {
        val pm = packageManager
        val freeIntent = pm.getLaunchIntentForPackage("com.realbyteapps.moneymanagerfree")
        val paidIntent = pm.getLaunchIntentForPackage("com.realbyteapps.moneymanager")
        val targetIntent = freeIntent ?: paidIntent

        if (targetIntent != null) {
            targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(targetIntent)
        }
    }
}
