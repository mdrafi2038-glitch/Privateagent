package com.personalagent.client
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
class MainActivity:AppCompatActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b)
  val l=android.widget.LinearLayout(this);l.orientation=android.widget.LinearLayout.VERTICAL;l.setPadding(32,32,32,32)
  val t=android.widget.TextView(this);t.text="Personal Agent\\n\\nDevice: "+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+"\\nAndroid: "+android.os.Build.VERSION.RELEASE;t.textSize=20f;l.addView(t)
  val s=android.widget.Button(this);s.text="Start Agent Service";s.setOnClickListener{androidx.core.content.ContextCompat.startForegroundService(this,Intent(this,AgentService::class.java))};l.addView(s)
  val b=android.widget.Button(this);b.text="Battery optimization settings";b.setOnClickListener{startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))};l.addView(b)
  val d=android.widget.Button(this);d.text="Enable Device Admin";d.setOnClickListener{startActivity(Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,android.content.ComponentName(this,AgentDeviceAdminReceiver::class.java)))};l.addView(d)
  setContentView(l);androidx.core.content.ContextCompat.startForegroundService(this,Intent(this,AgentService::class.java))
 }
}