package com.personalagent.client
import android.app.admin.DevicePolicyManager
import android.content.*
object AgentCommandHandler{
 fun handle(c:Context,type:String?,message:String?=null){when(type){
 "LOCK_DEVICE"->{val d=c.getSystemService(DevicePolicyManager::class.java);val a=ComponentName(c,AgentDeviceAdminReceiver::class.java);if(d.isAdminActive(a))d.lockNow()}
 "UNLOCK_DEVICE"->{c.sendBroadcast(Intent("com.personalagent.client.UNLOCK"))}
 "RESTRICT_MODE"->{c.getSharedPreferences("agent",0).edit().putBoolean("restricted",true).apply()}
 "CLEAR_APPS"->{}
 "SEND_MESSAGE"->{val n=c.getSystemService(android.app.NotificationManager::class.java);val ch="agent_messages";if(android.os.Build.VERSION.SDK_INT>=26)n.createNotificationChannel(android.app.NotificationChannel(ch,"Personal Agent messages",android.app.NotificationManager.IMPORTANCE_DEFAULT));n.notify(42,androidx.core.app.NotificationCompat.Builder(c,ch).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Personal Agent").setContentText(message?:"Message from administrator").setAutoCancel(true).build())}
 }}
}