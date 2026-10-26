package com.autolog.truemoneymm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TransactionStore {
    private const val PREF_NAME = "tmn_mm_prefs"
    private const val KEY_HISTORY = "tx_history"
    private const val KEY_AUTO_LAUNCH = "auto_launch_mm"
    private const val KEY_ACCOUNT_NAME = "mm_account_name"
    private const val KEY_DEFAULT_CATEGORY = "mm_default_category"

    @Volatile
    var pendingAutoFillRecord: ExpenseRecord? = null

    fun saveRecord(context: Context, record: ExpenseRecord) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        val obj = JSONObject().apply {
            put("amount", record.amount)
            put("merchant", record.merchant)
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

    fun getAccountName(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCOUNT_NAME, "TrueMoney") ?: "TrueMoney"
    }

    fun getCategoryName(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DEFAULT_CATEGORY, "อาหาร") ?: "อาหาร"
    }

    /**
     * ส่งออกไฟล์ .xls (Tab-Separated) ตามโครงสร้างมาตรฐานของแอป Money Manager (Realbyte)
     * สามารถกด Import ในเมนู Backup/Restore -> Import Excel File ได้ทันที
     */
    fun exportForMoneyManagerXls(context: Context): File {
        val records = getHistory(context)
        val account = getAccountName(context)
        val category = getCategoryName(context)

        val sb = StringBuilder()
        // Header มาตรฐานของแอป Money Manager (Realbyte)
        sb.append("Date	Account	Category	Subcategory	Note	THB	Income/Expense	Description	Amount	Currency
")

        for (r in records) {
            val dateStr = r.getFormattedDate()
            val cleanNote = r.merchant.replace("	", " ").replace("
", " ")
            val cleanDesc = r.rawText.replace("	", " ").replace("
", " ")
            sb.append("$dateStr	$account	$category		$cleanNote	${r.amount}	Expense	$cleanDesc	${r.amount}	THB
")
        }

        val outFile = File(context.getExternalFilesDir(null), "TrueMoney_MoneyManager_Import.xls")
        outFile.writeText(sb.toString(), Charsets.UTF_8)
        return outFile
    }
}
