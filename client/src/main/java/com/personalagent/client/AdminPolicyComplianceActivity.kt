package com.personalagent.client
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
class AdminPolicyComplianceActivity:Activity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);ContextCompat.startForegroundService(this,Intent(this,AgentService::class.java));setResult(RESULT_OK);finish()}
}