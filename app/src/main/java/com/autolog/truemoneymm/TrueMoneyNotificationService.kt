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

    private val targetPackages = setOf(
        "th.co.truemoney.wallet",
        "com.autolog.truemoneymm" // อนุญาตให้ทดสอบจากปุ่มจำลองในแอปตัวเองได้
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        if (!targetPackages.contains(pkg)) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text

        val record = TransactionParser.parse(title, bigText) ?: return

        // 1. บันทึกลงฐานข้อมูลในแอป (สำหรับดูย้อนหลังหรือ Export .xls)
        TransactionStore.saveRecord(applicationContext, record)

        // 2. คัดลอกยอดเงินลง Clipboard และตั้งค่าเตรียม Auto-fill
        TransactionStore.pendingAutoFillRecord = record

        Handler(Looper.getMainLooper()).post {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("amount", record.amount.toString()))
            Toast.makeText(
                applicationContext,
                "ตรวจพบการจ่าย TrueMoney: ${record.amount} บาท (${record.merchant})",
                Toast.LENGTH_LONG
            ).show()
        }

        // 3. เปิดแอป Money Manager อัตโนมัติ (ถ้าเปิดโหมด Auto Launch ไว้)
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
