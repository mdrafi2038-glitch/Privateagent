package com.personalagent.client
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
class AgentMessagingService:FirebaseMessagingService(){
 override fun onNewToken(token:String){val id=getSharedPreferences("agent",0).getString("id",null)?:return;com.google.firebase.database.FirebaseDatabase.getInstance().reference.child("devices").child(id).child("fcmToken").setValue(token)}
 override fun onMessageReceived(m:RemoteMessage){AgentCommandHandler.handle(this,m.data["type"],m.data["message"])}
}