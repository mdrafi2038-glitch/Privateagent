package com.personalagent.client
import android.content.*
import androidx.core.content.ContextCompat
class BootReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){ContextCompat.startForegroundService(c,Intent(c,AgentService::class.java))}}