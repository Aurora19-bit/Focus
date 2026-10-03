package com.vidhya.focuslock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityService : AccessibilityService() {
    private lateinit var sessionManager: SessionManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        sessionManager = SessionManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            return
        }

        val targetPackage = event.packageName?.toString() ?: return
        if (targetPackage == packageName) return
        if (targetPackage == "com.android.systemui" || targetPackage.contains("launcher")) return

        if (!::sessionManager.isInitialized) {
            sessionManager = SessionManager(this)
        }

        if (sessionManager.isSessionActive() && !sessionManager.isInBreak && !sessionManager.isEmergencyPassActive()) {
            if (sessionManager.blockedPackages.contains(targetPackage)) {
                performGlobalAction(GLOBAL_ACTION_HOME)

                val intent = Intent(this, BlockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                             Intent.FLAG_ACTIVITY_CLEAR_TOP or
                             Intent.FLAG_ACTIVITY_SINGLE_TOP or
                             Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    putExtra("BLOCKED_PACKAGE", targetPackage)
                }
                startActivity(intent)

                sessionManager.recordDisableAttempt()
            }
        }
    }

    override fun onInterrupt() {}
}