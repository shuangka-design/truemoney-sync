package com.autolog.truemoneymm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvHistory = findViewById(R.id.tvHistory)
        val btnGrantNotification = findViewById<Button>(R.id.btnGrantNotification)
        val btnGrantAccessibility = findViewById<Button>(R.id.btnGrantAccessibility)
        val switchAutoLaunch = findViewById<Switch>(R.id.switchAutoLaunch)
        val btnSimulate = findViewById<Button>(R.id.btnSimulate)
        val btnExportXls = findViewById<Button>(R.id.btnExportXls)
        val btnClearHistory = findViewById<Button>(R.id.btnClearHistory)

        switchAutoLaunch.isChecked = TransactionStore.isAutoLaunchEnabled(this)
        switchAutoLaunch.setOnCheckedChangeListener { _, isChecked ->
            TransactionStore.setAutoLaunchEnabled(this, isChecked)
        }

        btnGrantNotification.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnGrantAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnSimulate.setOnClickListener {
            sendTestNotification(
                title = "K PLUS",
                message = "รายการเงินออก บัญชี xxx-x-x1234-x จำนวน 120.00 บาท ไปยัง ร้านกาแฟอเมซอน คงเหลือ 5,400.00 บาท"
            )
        }

        btnExportXls.setOnClickListener {
            val file = TransactionStore.exportForMoneyManagerXls(this)
            Toast.makeText(
                this,
                "สร้างไฟล์สำเร็จ: ${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        }

        btnClearHistory.setOnClickListener {
            TransactionStore.clearHistory(this)
            refreshHistory()
        }

        refreshHistory()
    }

    override fun onResume() {
        super.onResume()
        refreshHistory()
    }

    private fun refreshHistory() {
        val records = TransactionStore.getHistory(this)
        if (records.isEmpty()) {
            tvHistory.text = "ยังไม่มีรายการ"
        } else {
            tvHistory.text = records.joinToString(separator = "\n\n") {
                "• [${it.accountName}] ${it.getFormattedDate()} | ${it.amount} บาท | ${it.merchant}"
            }
        }
    }

    private fun sendTestNotification(title: String, message: String) {
        val channelId = "test_tmn_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Test Financial Notification",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        refreshHistory()
    }
}
