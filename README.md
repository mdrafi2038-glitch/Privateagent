# Personal Agent

Two Android apps:
- Personal Agent Client: com.personalagent.client
- Personal Agent Admin: com.personalagent.admin

Target: Android 15 / API 35, minimum Android 10 / API 29.

## Firebase setup
1. Enable Authentication > Anonymous for the Client.
2. Enable Authentication > Email/Password for the Admin.
3. Create Realtime Database.
4. Upload google-services.json to client/ and admin/.
5. After creating the Admin account, copy its Firebase UID and create:
   admins/<ADMIN_UID> = true
   in Realtime Database. The rules intentionally prevent the app from granting itself admin access.
6. Deploy database.rules.json as the Realtime Database rules.

## Build
GitHub Actions builds both debug APKs on pushes to main. Artifacts are named personal-agent-apks.

## OPPO / ColorOS
On the managed device, review Auto Launch, background activity, battery optimization, notifications, overlay permission, and Device Admin/Device Owner enrollment in Settings. Exact menu names vary by ColorOS release.

## MDM limitations
Android does not allow an ordinary app to silently become Device Owner after installation, and arbitrary app-data clearing requires device-owner/profile-owner privileges. Remote keyguard unlocking is also restricted by Android security; a LOCK_DEVICE operation can lock a managed device, but an app cannot generally bypass the user's secure credential to unlock it remotely.
