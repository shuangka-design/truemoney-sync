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

        val isSupportedBankOrWallet = TransactionParser.supportedAppAccounts.containsKey(pkg)
        val isLineBankOfficial = (pkg == "jp.naver.line.android") && (
            title.contains("SCB Connect", ignoreCase = true) ||
            title.contains("KBank Live", ignoreCase = true) ||
            title.contains("Krungthai Connext", ignoreCase = true) ||
            title.contains("LINE BK", ignoreCase = true) ||
            title.contains("GSB NOW", ignoreCase = true) ||
            title.contains("ttb", ignoreCase = true)
        )

        if (!isSupportedBankOrWallet && !isLineBankOfficial) return

        val record = TransactionParser.parse(pkg, title, bigText) ?: return

        TransactionStore.saveRecord(applicationContext, record)
        TransactionStore.pendingAutoFillRecord = record

        Handler(Looper.getMainLooper()).post {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("amount", record.amount.toString()))
            Toast.makeText(
                applicationContext,
                "[${record.accountName}] จ่าย ${record.amount} บาท (${record.merchant})",
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
