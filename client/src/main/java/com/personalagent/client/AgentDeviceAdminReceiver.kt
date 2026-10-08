package com.personalagent.client
import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AgentDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        ContextCompat.startForegroundService(context, Intent(context, AgentService::class.java))
    }
}