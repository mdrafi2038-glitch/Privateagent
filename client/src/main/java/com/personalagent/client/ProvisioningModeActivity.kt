package com.personalagent.client
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
class ProvisioningModeActivity:Activity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);setResult(RESULT_OK,Intent().apply{
  putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE,DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE)
  putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS,false)
  putExtra(DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED,true)
 });finish()}
}