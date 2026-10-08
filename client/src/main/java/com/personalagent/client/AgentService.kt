package com.personalagent.client
import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
class AgentService:Service(){
 private val channel="agent"
 override fun onCreate(){super.onCreate()
  getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channel,"Personal Agent",NotificationManager.IMPORTANCE_LOW))
  startForeground(7,NotificationCompat.Builder(this,channel).setContentTitle("Personal Agent").setContentText("Device management active").setSmallIcon(android.R.drawable.ic_lock_lock).build())
  register()
 }
 private fun register(){val a=FirebaseAuth.getInstance();val work={val id=getSharedPreferences("agent",0).getString("id",null)?:java.util.UUID.randomUUID().toString().also{getSharedPreferences("agent",0).edit().putString("id",it).apply()};FirebaseDatabase.getInstance().reference.child("devices").child(id).setValue(mapOf("id" to id,"manufacturer" to android.os.Build.MANUFACTURER,"model" to android.os.Build.MODEL,"android" to android.os.Build.VERSION.RELEASE,"lastSeen" to System.currentTimeMillis()))};if(a.currentUser!=null)work()else a.signInAnonymously().addOnSuccessListener{work()}}
 override fun onStartCommand(i:Intent?,f:Int,id:Int)=START_STICKY
 override fun onBind(i:Intent?):IBinder?=null
}