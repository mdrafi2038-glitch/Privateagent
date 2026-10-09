# Personal Agent

Two Android apps:
- Personal Agent Client: `com.personalagent.client`
- Personal Agent Admin: `com.personalagent.admin`

Target: Android 15 / API 35, minimum Android 10 / API 29.

## One-time Firebase setup

Firebase project: `private-agent-98752`
Realtime Database URL: `https://private-agent-98752-default-rtdb.firebaseio.com`

1. In Firebase Console, enable **Authentication → Sign-in method → Anonymous** for Client devices.
2. Enable **Authentication → Sign-in method → Email/Password** for Admin users.
3. Create/confirm the Realtime Database in the project above.
4. Create the Admin account in Firebase Authentication. Copy that account's UID.
5. In Realtime Database → Data, add `admins/<ADMIN_UID>` with the Boolean value `true` (not the string `"true"`). Do this only for trusted administrators.
6. Open `database.rules.json` in this repository and publish its contents in Realtime Database → Rules. Do not enable public read/write rules.
7. The Client app signs in anonymously, uses its Firebase UID as its device ID, and registers itself at `devices/<UID>`. The Admin dashboard reads this list; no manual device creation is intended.

## Normal phone setup (no factory reset)

1. Install the latest Client APK and open it.
2. Tap **Run device checks** and allow notifications if Android asks.
3. Tap **Enable Device Admin** and approve the Android system prompt if you want the supported screen-lock function.
4. Tap **Start / restart Agent Service** if needed. On OPPO/ColorOS, also review Auto Launch and battery/background restrictions.
5. Keep internet on. The diagnostic report shows the registered device ID, database connection state, and the last Firebase error.
6. Sign in to the Admin APK with the trusted Firebase Email/Password account. The phone should appear after registration succeeds.

## Fully managed / Device Owner setup

- Fully managed enrollment is for a new or factory-reset device during Android's initial setup flow. It cannot be enabled as Device Owner after ordinary setup just by granting Device Admin.
- Use the Android Enterprise QR provisioning flow on the device's Welcome screen only after the provisioning package, download URL/checksum, device-admin component, and enrollment configuration have been prepared and tested for the target Android/ColorOS version.
- Do not factory-reset a phone just to experiment on your daily device. First test enrollment on a spare device.
- After enrollment, open Client diagnostics and verify it reports Device Owner enabled. Some commands (for example reversible restriction mode) require Device Owner; normal Device Admin does not grant those privileges.
- Android does not let an app bypass the user's secure PIN/password to unlock remotely. Arbitrary app-data clearing is not implemented; the Admin app intentionally does not offer a fake clear-apps action.

## Build

GitHub Actions builds debug APKs for Client and Admin when changes are pushed to `main`. Open Actions → latest successful run → artifact `personal-agent-apks`. Install the Client APK on the managed/test device and the Admin APK on the trusted administrator's phone.

## OPPO / ColorOS

Menu names vary by OS version. Review Auto Launch, background activity, battery optimization, and notifications. These settings improve reliability but cannot override every vendor power-management restriction.
