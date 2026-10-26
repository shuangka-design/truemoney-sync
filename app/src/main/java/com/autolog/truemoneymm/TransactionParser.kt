package com.autolog.truemoneymm

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ExpenseRecord(
    val amount: Double,
    val merchant: String,
    val accountName: String,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

object TransactionParser {

    val supportedAppAccounts = mapOf(
        "th.co.truemoney.wallet" to "TrueMoney",
        "com.kasikorn.retail.mbanking.wap" to "K PLUS",
        "com.scb.phone" to "SCB EASY",
        "ktbcs.netbank" to "Krungthai NEXT",
        "com.bbl.mobilebanking" to "Bangkok Bank",
        "com.krungsri.kma" to "KMA (Krungsri)",
        "com.TTB.touch" to "ttb touch",
        "com.TMB.touch.production" to "ttb touch",
        "com.gsb.mymo" to "MyMo (GSB)",
        "com.kbtg.make" to "MAKE by KBank",
        "com.kasikorn.linebk" to "LINE BK",
        "th.co.baac.amobileplus" to "BAAC A-Mobile",
        "com.uob.mightyth" to "UOB TMRW",
        "com.cimbthai.digitalbanking" to "CIMB Thai",
        "com.lhbank.mbanking" to "LHB You",
        "com.ghbank.ghball" to "GHB ALL GEN",
        "com.autolog.truemoneymm" to "Test Account"
    )

    private val expenseKeywords = listOf(
        "เงินออก", "ถอน/โอน", "โอนเงิน", "ชำระเงิน", "จ่ายเงิน", "หักเงิน",
        "หักบัญชี", "สแกนจ่าย", "จ่ายบิล", "ถอนเงิน", "รายการใช้จ่าย",
        "ชำระค่าสินค้า", "ยอดใช้จ่าย", "สำเร็จ", "บาท", "บ.", "THB"
    )

    private val ignoreKeywords = listOf(
        "เงินเข้า", "ได้รับเงิน", "รับเงินโอน", "เติมเงินสำเร็จ", "รับเงินคืน",
        "ฝากเงิน", "โปรโมชัน", "โปรโมชั่น", "คูปอง", "ส่วนลดพิเศษ", "สินเชื่อ",
        "รหัส OTP", "OTP ของคุณ"
    )

    private val amountPatterns = listOf(
        Pattern.compile("""(?:เงินออก|ถอน/โอน|โอนเงิน|ชำระเงิน|จ่ายเงิน|จำนวน|ยอดเงิน|ยอดชำระ)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s*(?:บาท|บ\.|THB)?"""),
        Pattern.compile("""([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s*(?:บาท|บ\.|THB)"""),
        Pattern.compile("""(?:฿|THB)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""")
    )

    private val merchantPatterns = listOf(
        Pattern.compile("""(?:ไปยัง|ให้แก่|ที่ร้าน|ร้านค้า|ผู้รับ|เข้าบัญชี|จ่ายบิล)\s*([^\n\r(จำนวน)(ยอด)(คงเหลือ)]+)"""),
        Pattern.compile("""ชำระเงินที่\s*([^\n\r]+)""")
    )

    fun resolveAccountName(packageName: String, title: String, text: String): String {
        val combined = "$title $text"
        if (combined.contains("K PLUS") || combined.contains("กสิกร") || combined.contains("KBank")) return "K PLUS"
        if (combined.contains("SCB") || combined.contains("ไทยพาณิชย์")) return "SCB EASY"
        if (combined.contains("Krungthai") || combined.contains("กรุงไทย")) return "Krungthai NEXT"
        if (combined.contains("BBL") || combined.contains("กรุงเทพ")) return "Bangkok Bank"
        if (combined.contains("KMA") || combined.contains("กรุงศรี")) return "KMA (Krungsri)"
        if (combined.contains("ttb") || combined.contains("ทีทีบี")) return "ttb touch"
        if (combined.contains("MyMo") || combined.contains("ออมสิน")) return "MyMo (GSB)"
        if (combined.contains("TrueMoney") || combined.contains("ทรูมันนี่")) return "TrueMoney"

        return supportedAppAccounts[packageName] ?: "Bank/Wallet"
    }

    fun parse(packageName: String, title: String, text: String): ExpenseRecord? {
        val combined = "$title $text".trim()

        if (ignoreKeywords.any { combined.contains(it, ignoreCase = true) }) {
            return null
        }

        if (!expenseKeywords.any { combined.contains(it, ignoreCase = true) }) {
            return null
        }

        val textBeforeBalance = combined.split("คงเหลือ", "ยอดเงินคงเหลือ", "ใช้ได้")[0]

        var extractedAmount: Double? = null
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(textBeforeBalance)
            if (matcher.find()) {
                val rawNumber = matcher.group(1)?.replace(",", "")
                extractedAmount = rawNumber?.toDoubleOrNull()
                if (extractedAmount != null && extractedAmount > 0) break
            }
        }

        if (extractedAmount == null) return null

        val accountName = resolveAccountName(packageName, title, text)

        var extractedMerchant = "จ่ายผ่าน $accountName"
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(textBeforeBalance)
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
            accountName = accountName,
            rawText = combined
        )
    }
}
