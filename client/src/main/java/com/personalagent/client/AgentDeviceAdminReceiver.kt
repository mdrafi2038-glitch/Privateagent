package com.personalagent.client
import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
class AgentDeviceAdminReceiver:DeviceAdminReceiver(){
 override fun onEnabled(c:Context,i:Intent){super.onEnabled(c,i)}
 override fun onDisabled(c:Context,i:Intent){super.onDisabled(c,i)}
}