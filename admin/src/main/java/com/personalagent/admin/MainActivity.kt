package com.personalagent.admin

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    private fun showLogin() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        layout.addView(TextView(this).apply {
            text = "Personal Agent Admin"
            textSize = 28f
        })
        val email = EditText(this).apply { hint = "Admin email"; inputType = 33 }
        val password = EditText(this).apply { hint = "Password"; inputType = 129 }
        layout.addView(email)
        layout.addView(password)
        val loginButton = Button(this).apply { text = "Login" }
        layout.addView(loginButton)
        loginButton.setOnClickListener {
            val e = email.text.toString().trim()
            val p = password.text.toString()
            if (e.isBlank() || p.isBlank()) {
                Toast.makeText(this@MainActivity, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            loginButton.isEnabled = false
            loginButton.text = "Signing in..."
            auth.signInWithEmailAndPassword(e, p)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        loginButton.isEnabled = true
                        loginButton.text = "Login"
                        rejectAdmin("Sign-in completed, but Firebase returned no user. Please try again.")
                    } else {
                        db.child("admins").child(uid).get()
                            .addOnSuccessListener { role ->
                                if (role.getValue(Boolean::class.java) == true) {
                                    showDashboard()
                                } else {
                                    loginButton.isEnabled = true
                                    loginButton.text = "Login"
                                    rejectAdmin("Login succeeded, but this account is not an admin. In Realtime Database, a trusted project owner must authorize admins/$uid as boolean true. Do not enable public writes.")
                                }
                            }
                            .addOnFailureListener { error ->
                                loginButton.isEnabled = true
                                loginButton.text = "Login"
                                rejectAdmin("Could not read admin role. Check internet, Realtime Database URL and database rules. Details: ${error.localizedMessage ?: "unknown database error"}")
                            }
                    }
                }
                .addOnFailureListener { error ->
                    loginButton.isEnabled = true
                    loginButton.text = "Login"
                    Toast.makeText(this@MainActivity, "Firebase login failed: ${error.localizedMessage ?: error.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                }
        }
        setContentView(layout)
    }

    private fun rejectAdmin(message: String) {
        auth.signOut()
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showDashboard() {
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        list.addView(TextView(this).apply { text = "Personal Agent — Devices"; textSize = 24f })
        scroll.addView(list)
        setContentView(scroll)
        db.child("devices").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                list.removeViews(1, list.childCount - 1)
                for (device in snapshot.children) addDevice(device)
                if (snapshot.childrenCount == 0L) {
                    list.addView(TextView(this@MainActivity).apply {
                        text = "No registered devices yet. Install and open the Client app on a test phone."
                        textSize = 16f
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Could not load devices: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun addDevice(device: DataSnapshot) {
        val uid = device.key ?: return
        val manufacturer = device.child("manufacturer").getValue(String::class.java) ?: "Unknown"
        val model = device.child("model").getValue(String::class.java) ?: "Unknown"
        val battery = device.child("battery").getValue(Number::class.java)?.toInt() ?: -1
        val lastSeen = device.child("lastSeen").getValue(Long::class.java) ?: 0L
        val declaredOnline = device.child("online").getValue(Boolean::class.java) == true
        val fresh = lastSeen > 0 && System.currentTimeMillis() - lastSeen < 150_000L
        val online = declaredOnline && fresh

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 18, 0, 18)
        }
        box.addView(TextView(this).apply {
            text = "$manufacturer $model\nDevice ID: $uid\nStatus: ${if (online) "ONLINE" else "OFFLINE / STALE"}\nBattery: ${if (battery in 0..100) "$battery%" else "Unknown"}\nLast seen: ${if (lastSeen > 0) java.text.DateFormat.getDateTimeInstance().format(java.util.Date(lastSeen)) else "Not available"}"
            textSize = 16f
        })
        for (command in listOf("LOCK_DEVICE", "UNLOCK_DEVICE", "RESTRICT_MODE", "CLEAR_APPS")) {
            box.addView(Button(this).apply {
                text = when (command) {
                    "LOCK_DEVICE" -> "LOCK DEVICE"
                    "UNLOCK_DEVICE" -> "UNLOCK DEVICE"
                    "RESTRICT_MODE" -> "RESTRICT MODE"
                    else -> "CLEAR APPS (managed apps only)"
                }
                setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Confirm $command")
                        .setMessage("Send this command to $manufacturer $model?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Send") { _, _ -> send(uid, command, null) }
                        .show()
                }
            })
        }
        box.addView(Button(this).apply {
            text = "SEND MESSAGE"
            setOnClickListener {
                val input = EditText(this@MainActivity).apply { hint = "Message to display on device" }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Send message")
                    .setView(input)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Send") { _, _ ->
                        val message = input.text.toString().trim()
                        if (message.isNotBlank()) send(uid, "SEND_MESSAGE", message)
                    }
                    .show()
            }
        })
        list.addView(box)
    }

    private fun send(uid: String, type: String, message: String?) {
        val id = db.child("commands").child(uid).push().key
        if (id == null) {
            Toast.makeText(this, "Could not create command ID", Toast.LENGTH_SHORT).show()
            return
        }
        db.child("commands").child(uid).child(id)
            .setValue(mapOf(
                "type" to type,
                "message" to message,
                "createdAt" to ServerValue.TIMESTAMP,
                "status" to "pending"
            ))
            .addOnSuccessListener {
                Toast.makeText(this, "$type queued for device", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Command rejected: ${it.localizedMessage ?: "check admin authorization and Firebase rules"}", Toast.LENGTH_LONG).show()
            }
    }
}
