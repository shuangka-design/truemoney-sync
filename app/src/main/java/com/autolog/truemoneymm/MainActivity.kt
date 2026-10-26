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
            sendTestNotification()
        }

        btnExportXls.setOnClickListener {
            val file = TransactionStore.exportForMoneyManagerXls(this)
            Toast.makeText(
                this,
                "สร้างไฟล์สำเร็จ: ${file.absolutePath}\nนำไป Import ใน Money Manager ได้เลย!",
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
            tvHistory.text = records.joinToString("\n\n") {
                "• ${it.getFormattedDate()} | ${it.amount} บาท | ${it.merchant}"
            }
        }
    }

    private fun sendTestNotification() {
        val channelId = "test_tmn_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Test TrueMoney",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("TrueMoney")
            .setContentText("ชำระเงินสำเร็จ จำนวน 65.00 บาท ให้แก่ 7-Eleven")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(1001, notification)
        refreshHistory()
    }
}
