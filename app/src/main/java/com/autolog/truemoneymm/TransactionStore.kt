package com.autolog.truemoneymm

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TransactionStore {
    private const val PREF_NAME = "tmn_mm_prefs"
    private const val KEY_HISTORY = "tx_history_v2"
    private const val KEY_AUTO_LAUNCH = "auto_launch_mm"
    private const val KEY_DEFAULT_CATEGORY = "mm_default_category"

    @Volatile
    var pendingAutoFillRecord: ExpenseRecord? = null

    fun saveRecord(context: Context, record: ExpenseRecord) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        val obj = JSONObject().apply {
            put("amount", record.amount)
            put("merchant", record.merchant)
            put("accountName", record.accountName)
            put("rawText", record.rawText)
            put("timestamp", record.timestamp)
        }
        arr.put(obj)
        prefs.edit().putString(KEY_HISTORY, arr.toString()).apply()
    }

    fun getHistory(context: Context): List<ExpenseRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        val list = mutableListOf<ExpenseRecord>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                ExpenseRecord(
                    amount = o.getDouble("amount"),
                    merchant = o.getString("merchant"),
                    accountName = o.optString("accountName", "TrueMoney"),
                    rawText = o.getString("rawText"),
                    timestamp = o.getLong("timestamp")
                )
            )
        }
        return list.reversed()
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, "[]").apply()
    }

    fun isAutoLaunchEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_LAUNCH, true)
    }

    fun setAutoLaunchEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_LAUNCH, enabled).apply()
    }

    fun getCategoryName(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DEFAULT_CATEGORY, "อาหาร") ?: "อาหาร"
    }

    fun exportForMoneyManagerXls(context: Context): File {
        val records = getHistory(context)
        val category = getCategoryName(context)
        val tab = "\t"
        val nl = "\n"

        val sb = StringBuilder()
        sb.append("Date${tab}Account${tab}Category${tab}Subcategory${tab}Note${tab}THB${tab}Income/Expense${tab}Description${tab}Amount${tab}Currency${nl}")

        for (r in records) {
            val dateStr = r.getFormattedDate()
            val cleanAccount = r.accountName.replace('\t', ' ').replace('\n', ' ')
            val cleanNote = r.merchant.replace('\t', ' ').replace('\n', ' ')
            val cleanDesc = r.rawText.replace('\t', ' ').replace('\n', ' ')
            sb.append("${dateStr}${tab}${cleanAccount}${tab}${category}${tab}${tab}${cleanNote}${tab}${r.amount}${tab}Expense${tab}${cleanDesc}${tab}${r.amount}${tab}THB${nl}")
        }

        // บันทึกลงโฟลเดอร์ Downloads ของมือถือโดยตรง เพื่อให้หาไฟล์เจอง่ายที่สุด
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        val fileName = "MoneyManager_Import_${System.currentTimeMillis() / 1000}.xls"
        val outFile = File(downloadDir, fileName)
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        return outFile
    }
}
