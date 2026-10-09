package com.personalagent.client

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AgentMessagingService : FirebaseMessagingService() {
    companion object {
        private const val DATABASE_URL = "https://private-agent-98752-default-rtdb.firebaseio.com"
    }

    override fun onNewToken(token: String) {
        val id = getSharedPreferences("agent", 0).getString("id", null) ?: return
        FirebaseDatabase.getInstance(DATABASE_URL)
            .getReference("devices").child(id).child("fcmToken").setValue(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        AgentCommandHandler.handle(this, message.data["type"], message.data["message"])
    }
}
