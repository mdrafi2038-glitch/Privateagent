package com.personalagent.client

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging

class AgentService : Service() {
    private val channel = "agent"
    private var deviceRef: DatabaseReference? = null
    private var commandRef: DatabaseReference? = null
    private var listener: ChildEventListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private val heartbeat = object : Runnable {
        override fun run() {
            updatePresence()
            handler.postDelayed(this, 60_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val n = getSystemService(NotificationManager::class.java)
        n.createNotificationChannel(
            NotificationChannel(channel, "Personal Agent", NotificationManager.IMPORTANCE_LOW)
        )
        startForeground(
            7,
            NotificationCompat.Builder(this, channel)
                .setContentTitle("Personal Agent")
                .setContentText("Device management active")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        )
        authenticate()
    }

    private fun authenticate() {
        val a = FirebaseAuth.getInstance()
        if (a.currentUser != null) register(a)
        else a.signInAnonymously().addOnSuccessListener { register(a) }
    }

    private fun register(a: FirebaseAuth) {
        val uid = a.currentUser?.uid ?: return
        getSharedPreferences("agent", 0).edit().putString("id", uid).apply()
        deviceRef = FirebaseDatabase.getInstance("https://private-agent-98752-default-rtdb.firebaseio.com").reference.child("devices").child(uid)

        updatePresence()

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            deviceRef?.child("fcmToken")?.setValue(token)
        }

        commandRef = FirebaseDatabase.getInstance("https://private-agent-98752-default-rtdb.firebaseio.com").reference.child("commands").child(uid)
        listener = commandRef!!.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(s: DataSnapshot, p: String?) {
                if (s.child("status").getValue(String::class.java) == "processed") return
                AgentCommandHandler.handle(
                    this@AgentService,
                    s.child("type").getValue(String::class.java),
                    s.child("message").getValue(String::class.java)
                )
                s.ref.child("status").setValue("processed")
            }
            override fun onChildChanged(s: DataSnapshot, p: String?) {}
            override fun onChildRemoved(s: DataSnapshot) {}
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(e: DatabaseError) {}
        })
        handler.removeCallbacks(heartbeat)
        handler.post(heartbeat)
    }

    private fun updatePresence() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (deviceRef == null) deviceRef = FirebaseDatabase.getInstance("https://private-agent-98752-default-rtdb.firebaseio.com").reference.child("devices").child(uid)
        deviceRef?.updateChildren(
            mapOf(
                "uid" to uid,
                "manufacturer" to Build.MANUFACTURER,
                "model" to Build.MODEL,
                "androidVersion" to Build.VERSION.RELEASE,
                "colorOSVersion" to prop("ro.build.version.oplusrom"),
                "battery" to battery(),
                "network" to network(),
                "online" to true,
                "lastSeen" to ServerValue.TIMESTAMP
            )
        )
    }

    private fun battery() =
        getSystemService(BatteryManager::class.java)
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

    private fun network() =
        if (getSystemService(android.net.ConnectivityManager::class.java).activeNetwork != null) "online" else "offline"

    private fun prop(n: String) = try {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
            .invoke(null, n) as String
    } catch (_: Exception) {
        "unknown"
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(heartbeat)
        listener?.let { commandRef?.removeEventListener(it) }
        deviceRef?.child("online")?.setValue(false)
        super.onDestroy()
    }

    override fun onBind(i: Intent?): IBinder? = null
}