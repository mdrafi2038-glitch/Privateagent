package com.personalagent.client

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging

class AgentService : Service() {
    companion object {
        private const val CHANNEL_ID = "agent"
        private const val DATABASE_URL = "https://private-agent-98752-default-rtdb.firebaseio.com"
    }

    private var deviceRef: DatabaseReference? = null
    private var commandRef: DatabaseReference? = null
    private var listener: ChildEventListener? = null
    private var connectedRef: DatabaseReference? = null
    private var connectedListener: ValueEventListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private var authInProgress = false
    private var retryDelayMs = 5_000L

    private val retryAuth = object : Runnable {
        override fun run() = authenticate()
    }

    private val heartbeat = object : Runnable {
        override fun run() {
            updatePresence()
            handler.postDelayed(this, 60_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Personal Agent", NotificationManager.IMPORTANCE_LOW)
        )
        startForeground(
            7,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Personal Agent")
                .setContentText("Device registration and connection service")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build()
        )
        watchDatabaseConnection()
        authenticate()
    }

    private fun database() = FirebaseDatabase.getInstance(DATABASE_URL)

    private fun authenticate() {
        if (authInProgress) return
        val auth = FirebaseAuth.getInstance()
        val current = auth.currentUser
        if (current != null) {
            retryDelayMs = 5_000L
            register(current.uid)
            return
        }
        authInProgress = true
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                authInProgress = false
                retryDelayMs = 5_000L
                val uid = result.user?.uid
                if (uid == null) scheduleRetry("Authentication returned no user")
                else register(uid)
            }
            .addOnFailureListener { error ->
                authInProgress = false
                scheduleRetry("Authentication failed: ${error.localizedMessage ?: error.javaClass.simpleName}")
            }
    }

    private fun scheduleRetry(reason: String) {
        getSharedPreferences("agent", 0).edit().putString("lastError", reason).apply()
        handler.removeCallbacks(retryAuth)
        handler.postDelayed(retryAuth, retryDelayMs)
        retryDelayMs = (retryDelayMs * 2).coerceAtMost(300_000L)
    }

    private fun register(uid: String) {
        getSharedPreferences("agent", 0).edit().putString("id", uid).apply()
        val db = database()
        val ref = db.getReference("devices").child(uid)
        deviceRef = ref

        listener?.let { commandRef?.removeEventListener(it) }
        commandRef = db.getReference("commands").child(uid)
        listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val status = snapshot.child("status").getValue(String::class.java)
                if (status == "processed") return
                val type = snapshot.child("type").getValue(String::class.java) ?: return
                val message = snapshot.child("message").getValue(String::class.java)
                AgentCommandHandler.handle(this@AgentService, type, message)
                snapshot.ref.child("status").setValue("processed")
                    .addOnFailureListener { error ->
                        getSharedPreferences("agent", 0).edit()
                            .putString("lastError", "Could not update command status: ${error.localizedMessage}")
                            .apply()
                    }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                getSharedPreferences("agent", 0).edit()
                    .putString("lastError", "Command listener failed: ${error.message}")
                    .apply()
            }
        }
        commandRef?.addChildEventListener(listener!!)

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                ref.child("fcmToken").setValue(token)
            }
            .addOnFailureListener { error ->
                getSharedPreferences("agent", 0).edit()
                    .putString("lastError", "FCM token failed: ${error.localizedMessage}")
                    .apply()
            }

        updatePresence()
        handler.removeCallbacks(heartbeat)
        handler.post(heartbeat)
    }

    private fun watchDatabaseConnection() {
        connectedListener?.let { connectedRef?.removeEventListener(it) }
        val ref = database().getReference(".info/connected")
        connectedRef = ref
        val valueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) == true
                getSharedPreferences("agent", 0).edit()
                    .putBoolean("databaseConnected", connected)
                    .apply()
                if (connected) updatePresence()
            }
            override fun onCancelled(error: DatabaseError) {
                getSharedPreferences("agent", 0).edit()
                    .putString("lastError", "Database connection check failed: ${error.message}")
                    .apply()
            }
        }
        connectedListener = valueListener
        ref.addValueEventListener(valueListener)
    }

    private fun updatePresence() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (deviceRef == null) deviceRef = database().getReference("devices").child(uid)
        val battery = getSystemService(BatteryManager::class.java)
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val network = if (getSystemService(android.net.ConnectivityManager::class.java).activeNetwork != null) "online" else "offline"
        val data = mapOf(
            "uid" to uid,
            "deviceId" to Build.ID,
            "deviceName" to Build.DEVICE,
            "manufacturer" to Build.MANUFACTURER,
            "model" to Build.MODEL,
            "androidVersion" to Build.VERSION.RELEASE,
            "colorOSVersion" to prop("ro.build.version.oplusrom"),
            "battery" to battery,
            "network" to network,
            "online" to true,
            "connectionStatus" to "connected",
            "lastSeen" to ServerValue.TIMESTAMP,
            "appVersion" to "1.0.2"
        )
        deviceRef?.updateChildren(data)
            ?.addOnSuccessListener {
                getSharedPreferences("agent", 0).edit().remove("lastError").apply()
            }
            ?.addOnFailureListener { error ->
                getSharedPreferences("agent", 0).edit()
                    .putString("lastError", "Device registration/write failed: ${error.localizedMessage ?: error.javaClass.simpleName}")
                    .apply()
            }
    }

    private fun prop(name: String) = try {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
            .invoke(null, name) as String
    } catch (_: Exception) {
        "unknown"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        authenticate()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(heartbeat)
        handler.removeCallbacks(retryAuth)
        listener?.let { commandRef?.removeEventListener(it) }
        connectedListener?.let { connectedRef?.removeEventListener(it) }
        deviceRef?.child("online")?.setValue(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
