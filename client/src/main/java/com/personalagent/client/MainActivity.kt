package com.personalagent.client

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var reportView: TextView
    private var connectionRef: DatabaseReference? = null
    private var connectionListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            text = "Personal Agent\n\nTest this phone without factory reset"
            textSize = 22f
        }
        content.addView(title)
        content.addView(button("Run device checks") { runDiagnostics() })
        content.addView(button("Start / restart Agent Service") {
            try {
                ContextCompat.startForegroundService(this, Intent(this, AgentService::class.java))
                reportView.text = "Service start requested. Run device checks again to review current status."
            } catch (e: Exception) {
                reportView.text = "Service start failed: ${e.javaClass.simpleName}: ${e.message}"
            }
        })
        content.addView(button("Enable Device Admin") {
            val component = ComponentName(this, AgentDeviceAdminReceiver::class.java)
            startActivity(
                Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                    .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Allows Personal Agent to use supported device-admin functions.")
            )
        })
        content.addView(button("Battery optimization settings") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })
        content.addView(button("Notification settings") {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        })
        content.addView(button("Share diagnostic report") { shareReport() })
        reportView = TextView(this).apply {
            textSize = 15f
            text = "Tap ‘Run device checks’ to test this phone. This test does not require factory reset."
            setPadding(0, 20, 0, 20)
        }
        content.addView(reportView)
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
        runDiagnostics()
        try {
            ContextCompat.startForegroundService(this, Intent(this, AgentService::class.java))
        } catch (_: Exception) {
            reportView.text = reportView.text.toString() + "\\nWARNING: service could not auto-start; tap the service button."
        }
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun runDiagnostics() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(this, AgentDeviceAdminReceiver::class.java)
        val power = getSystemService(PowerManager::class.java)
        val cm = getSystemService(ConnectivityManager::class.java)
        val lines = mutableListOf<String>()
        lines += "PERSONAL AGENT DEVICE TEST"
        lines += "Device: ${Build.MANUFACTURER} ${Build.MODEL}"
        lines += "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        lines += "Device Admin: ${if (dpm.isAdminActive(admin)) "PASS - enabled" else "WARNING - not enabled"}"
        lines += "Device Owner: ${if (dpm.isDeviceOwnerApp(packageName)) "PASS - enabled" else "NOT TESTED - requires managed-device provisioning"}"
        lines += "Network: ${if (cm.activeNetwork != null) "PASS - connected" else "FAIL - no active network"}"
        lines += "Battery optimization: ${if (power.isIgnoringBatteryOptimizations(packageName)) "PASS - exempt" else "WARNING - not exempt"}"
        lines += "Firebase Auth: ${if (FirebaseAuth.getInstance().currentUser != null) "PASS - signed in" else "WARNING - no signed-in user yet"}"
        val agentPrefs = getSharedPreferences("agent", 0)
        lines += "Firebase Database: ${if (agentPrefs.getBoolean("databaseConnected", false)) "PASS - connected" else "WARNING - not connected yet"}"
        lines += "Registered Device ID: ${agentPrefs.getString("id", "Not registered yet")}"
        agentPrefs.getString("lastError", null)?.let { lines += "Last Firebase error: $it" }
        if (Build.VERSION.SDK_INT >= 33) {
            lines += "Notifications: ${if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) "PASS - allowed" else "WARNING - permission not granted"}"
        } else {
            lines += "Notifications: API level does not require runtime notification permission"
        }
        lines += "Foreground service: use ‘Start / restart Agent Service’ if it is not running"
        lines += "Test mode: normal phone; no factory reset required"
        lines += "Note: Device Owner-only controls are not available from Device Admin alone."
        reportView.text = lines.joinToString("\n")
        watchFirebaseConnection()
    }

    private fun watchFirebaseConnection() {
        connectionListener?.let { listener -> connectionRef?.removeEventListener(listener) }
        val ref = FirebaseDatabase.getInstance("https://private-agent-98752-default-rtdb.firebaseio.com").getReference(".info/connected")
        connectionRef = ref
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) == true
                val status = if (connected) "PASS - connected to Realtime Database" else "WARNING - Realtime Database not connected"
                runOnUiThread {
                    reportView.text = reportView.text.toString()
                        .replace(Regex("Firebase Database:.*\\n?"), "")
                        .trimEnd() + "\nFirebase Database: " + status
                }
            }
            override fun onCancelled(error: DatabaseError) {
                runOnUiThread {
                    reportView.text = reportView.text.toString() + "\nFirebase Database: FAIL - ${error.message}"
                }
            }
        }
        connectionListener = listener
        ref.addValueEventListener(listener)
    }

    private fun shareReport() {
        val text = reportView.text.toString()
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Personal Agent diagnostic report")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, "Share diagnostic report"))
    }

    override fun onDestroy() {
        connectionListener?.let { listener -> connectionRef?.removeEventListener(listener) }
        super.onDestroy()
    }
}
