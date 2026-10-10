package com.personalagent.client

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

class DiagnosticsActivity : AppCompatActivity() {
    private lateinit var results: LinearLayout
    private val rows = linkedMapOf<String, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
        }
        root.addView(TextView(this).apply {
            text = "Personal Agent — Diagnostics"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "Tests run on this phone. Device Owner-only capabilities are reported separately; this screen does not change device policy."
            textSize = 14f
        })
        root.addView(Button(this).apply {
            text = "Run tests again"
            setOnClickListener { runChecks() }
        })
        root.addView(Button(this).apply {
            text = "Open Battery Optimization Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        })
        root.addView(Button(this).apply {
            text = "Open App Settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                })
            }
        })
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(results)
        setContentView(ScrollView(this).apply { addView(root) })
        runChecks()
    }

    private fun addResult(name: String, status: String, detail: String) {
        runOnUiThread {
            val line = rows[name] ?: TextView(this).also {
                it.textSize = 15f
                it.setPadding(0, 10, 0, 10)
                rows[name] = it
                results.addView(it)
            }
            line.text = "$status  |  $name\n$detail"
        }
    }

    private fun runChecks() {
        rows.clear()
        results.removeAllViews()

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val hasInternetNetwork = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        addResult("Network", if (hasInternetNetwork) "PASS" else "FAIL",
            if (hasInternetNetwork) "An internet-capable network is connected; this does not guarantee external access." else "No internet-capable network detected. Check Wi-Fi/mobile data.")

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pm.isIgnoringBatteryOptimizations(packageName) else true
        addResult("Battery optimization", if (batteryOk) "PASS" else "WARNING",
            if (batteryOk) "Battery optimization exemption is active." else "Exemption is not active. Background work may be delayed; this is not always required.")

        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val receiver = ComponentName(this, AgentDeviceAdminReceiver::class.java)
        val adminActive = dpm.isAdminActive(receiver)
        addResult("Device Admin", if (adminActive) "PASS" else "WARNING",
            if (adminActive) "Device Admin receiver is active." else "Device Admin is not enabled. Some policy actions will not work.")

        val owner = dpm.isDeviceOwnerApp(packageName)
        addResult("Device Owner", if (owner) "PASS" else "NOT TESTED",
            if (owner) "This app is provisioned as Device Owner." else "Not provisioned as Device Owner. Factory reset is not required for these basic diagnostics; owner-only controls cannot be validated here.")

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        addResult("Firebase Authentication", if (user != null) "PASS" else "WARNING",
            if (user != null) "Firebase session exists. UID: ${user.uid.take(12)}… Anonymous: ${user.isAnonymous}" else "No Firebase session yet. Check authentication setup.")

        FirebaseDatabase.getInstance().getReference(".info/connected")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) == true
                    addResult("Realtime Database", if (connected) "PASS" else "FAIL",
                        if (connected) "Realtime Database reports an active connection." else "Database is not connected. Check internet, database URL, Firebase configuration and rules.")
                }
                override fun onCancelled(error: DatabaseError) {
                    addResult("Realtime Database", "FAIL", "Database check cancelled. Code: ${error.code}, Message: ${error.message}")
                }
            })

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                addResult("Firebase Cloud Messaging", if (token.isNotBlank()) "PASS" else "FAIL",
                    if (token.isNotBlank()) "FCM token retrieved (${token.take(8)}…). Token value is intentionally not fully displayed." else "FCM returned an empty token.")
            }
            .addOnFailureListener { error ->
                addResult("Firebase Cloud Messaging", "FAIL", "Could not retrieve token: ${error.localizedMessage ?: "unknown error"}")
            }

        addResult("Permission Center", "INFO", "Use the buttons above to review app settings and battery optimization. Device Admin and Device Owner are checked separately.")

        addResult("Notifications permission", if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) "PASS" else "WARNING",
            if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) "Notification permission is available or not runtime-gated on this Android version." else "Notifications permission has not been granted.")
        addResult("Summary", "INFO", "Checks are still running where results are asynchronous. Re-run after starting the Agent service if Firebase Authentication has no session.")
    }
}
