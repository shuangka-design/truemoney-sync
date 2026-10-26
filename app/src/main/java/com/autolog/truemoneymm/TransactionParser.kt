package com.autolog.truemoneymm

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ExpenseRecord(
    val amount: Double,
    val merchant: String,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

object TransactionParser {
    // ตรวจจับข้อความแจ้งเตือนการใช้จ่ายภาษาไทยของ TrueMoney
    private val expenseKeywords = listOf(
        "ชำระเงิน", "จ่ายเงิน", "โอนเงิน", "หักเงิน", "สำเร็จ", "รายการใช้จ่าย", "บาท", "THB"
    )

    // ยกเว้นแจ้งเตือนที่เป็นเงินเข้าหรือโปรโมชันโฆษณา
    private val ignoreKeywords = listOf(
        "ได้รับเงิน", "เงินเข้า", "เติมเงินสำเร็จ", "รับเงินคืน", "โปรโมชัน", "คูปอง", "ส่วนลดพิเศษ"
    )

    // Regex ดึงตัวเลขจำนวนเงิน เช่น 45.00 บาท, ฿1,200.50, จำนวน 150 บาท
    private val amountPatterns = listOf(
        Pattern.compile("([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s*(?:บาท|THB)"),
        Pattern.compile("(?:จำนวน|ยอดชำระ|ชำระเงิน|โอนเงิน|฿)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)")
    )

    // Regex ดึงชื่อร้านค้าหรือผู้รับ
    private val merchantPatterns = listOf(
        Pattern.compile("(?:ให้แก่|ไปยัง|ที่ร้าน|ร้านค้า|ผู้รับ)\s*([^\n\r(จำนวน)(ยอด)]+)"),
        Pattern.compile("ชำระเงินที่\s*([^\n\r]+)")
    )

    fun parse(title: String, text: String): ExpenseRecord? {
        val combined = "$title $text".trim()

        if (ignoreKeywords.any { combined.contains(it, ignoreCase = true) }) {
            return null
        }

        if (!expenseKeywords.any { combined.contains(it, ignoreCase = true) }) {
            return null
        }

        var extractedAmount: Double? = null
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(combined)
            if (matcher.find()) {
                val rawNumber = matcher.group(1)?.replace(",", "")
                extractedAmount = rawNumber?.toDoubleOrNull()
                if (extractedAmount != null && extractedAmount > 0) break
            }
        }

        if (extractedAmount == null) return null

        var extractedMerchant = "TrueMoney Expense"
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(combined)
            if (matcher.find()) {
                val found = matcher.group(1)?.trim()
                if (!found.isNullOrEmpty()) {
                    extractedMerchant = found.take(40)
                    break
                }
            }
        }

        return ExpenseRecord(
            amount = extractedAmount,
            merchant = extractedMerchant,
            rawText = combined
        )
    }
}
