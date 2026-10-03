package com.vidhya.focuslock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (SessionManager(context).isSessionActive()) {
            val s = Intent(context, SessionTimerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(s) else context.startService(s)
        }
    }
}