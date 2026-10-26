package com.autolog.truemoneymm

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MoneyManagerAutoFillService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pending = TransactionStore.pendingAutoFillRecord ?: return
        val rootNode = rootInActiveWindow ?: return

        // ค้นหาปุ่ม + (Floating Action Button) ในหน้าแรกของแอป Money Manager เพื่อกดเพิ่มรายการ
        val addButtons = rootNode.findAccessibilityNodeInfosByViewId(
            "com.realbyteapps.moneymanagerfree:id/fab_add"
        )
        if (addButtons.isNotEmpty()) {
            addButtons[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return
        }

        // เมื่อเข้ามาในหน้าเพิ่มรายการธุรกรรม ลองหาช่อง Note / Memo เพื่อกรอกชื่อร้านค้าและจำนวนเงิน
        fillTextIfFound(rootNode, pending.merchant)

        // เคลียร์สถานะเมื่อส่งค่าเสร็จสิ้น เพื่อไม่ให้กรอกซ้ำ
        TransactionStore.pendingAutoFillRecord = null
    }

    private fun fillTextIfFound(root: AccessibilityNodeInfo, noteText: String) {
        val editTexts = mutableListOf<AccessibilityNodeInfo>()
        findEditTexts(root, editTexts)
        for (node in editTexts) {
            val args = Bundle()
            args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                noteText
            )
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
    }

    private fun findEditTexts(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.className?.toString()?.contains("EditText") == true) {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            findEditTexts(node.getChild(i), list)
        }
    }

    override fun onInterrupt() {}
}
