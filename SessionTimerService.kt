package com.vidhya.focuslock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SessionTimerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("focus_channel", "FocusLock Status", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        val notif = NotificationCompat.Builder(this, "focus_channel")
            .setContentTitle("FocusLock is Active")
            .setContentText("Focus session is enforcing your app blocklist")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1001, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, notif)
        }
        return START_STICKY
    }
}