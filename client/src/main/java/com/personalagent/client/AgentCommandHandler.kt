package com.personalagent.client

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object AgentCommandHandler {
    fun handle(c: Context, type: String?, message: String? = null) {
        val d = c.getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(c, AgentDeviceAdminReceiver::class.java)
        when (type) {
            "LOCK_DEVICE" -> {
                if (d.isAdminActive(admin)) d.lockNow()
            }
            "UNLOCK_DEVICE" -> {
                // Android does not allow an app/admin to bypass the user's secure
                // PIN/password/pattern. The user must unlock the device normally.
                c.getSharedPreferences("agent", 0).edit().putBoolean("unlockRequested", true).apply()
            }
            "RESTRICT_MODE" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && d.isDeviceOwnerApp(c.packageName)) {
                    d.addUserRestriction(admin, android.os.UserManager.DISALLOW_FACTORY_RESET)
                    d.addUserRestriction(admin, android.os.UserManager.DISALLOW_SAFE_BOOT)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) d.setStatusBarDisabled(admin, true)
                    c.getSharedPreferences("agent", 0).edit().putBoolean("restricted", true).apply()
                }
            }
            "CLEAR_APPS" -> {
                // Deliberately does not wipe every installed app. A package list must
                // be supplied by a future managed-apps policy before data is cleared.
                c.getSharedPreferences("agent", 0).edit().putBoolean("clearAppsRequested", true).apply()
            }
            "SEND_MESSAGE" -> {
                val n = c.getSystemService(android.app.NotificationManager::class.java)
                val ch = "agent_messages"
                if (Build.VERSION.SDK_INT >= 26) {
                    n.createNotificationChannel(
                        android.app.NotificationChannel(
                            ch, "Personal Agent messages",
                            android.app.NotificationManager.IMPORTANCE_DEFAULT
                        )
                    )
                }
                n.notify(
                    42,
                    NotificationCompat.Builder(c, ch)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Personal Agent")
                        .setContentText(message ?: "Message from administrator")
                        .setAutoCancel(true)
                        .build()
                )
            }
        }
    }
}