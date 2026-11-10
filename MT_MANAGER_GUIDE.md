# PC HUB CENTER — MT Manager Integration & Compatibility Guide
### Authorized APK Patching & DEX Injection Considerations

> [!CAUTION]
> **Important Security & Policy Notice**:
> This guide is intended solely for developers and authorized security researchers integrating licensing controls into APKs that they own or have explicit authorization to modify.

---

### 1. Why Simply Copying a DEX File Will Fail
Beginners often assume that copying a compiled `.dex` file into an arbitrary APK using tools like MT Manager or NP Manager will instantly protect the app. In modern Android applications, this causes crashes due to several technical bottlenecks:

1. **Missing R Resource IDs**:
   If the activation UI relies on Android compiled XML layout IDs (`R.id.*`), these IDs will not exist in the target APK's `resources.arsc`, resulting in `NoClassDefFoundError` or `ResourceNotFoundException`.
   *Solution*: The PC HUB CENTER module uses **Jetpack Compose and purely programmatic components**, meaning it does **NOT** require external XML resource IDs to render.

2. **Missing Firebase Dependencies**:
   If the target APK was not built with Firebase Firestore and Google Play Services, calling `FirebaseFirestore.getInstance()` will trigger `ClassNotFoundException: com.google.firebase.firestore.FirebaseFirestore`.
   *Solution*: The target APK must include the Firebase Firestore DEX classes, or you must bundle them in a secondary multidex (`classes2.dex` or `classes3.dex`).

3. **Application Signing Certificate Mismatch**:
   Firebase verifies the SHA-1 / SHA-256 certificate fingerprint of the APK making requests. If you re-sign the APK with an MT Manager test key (`testkey`), Firebase will reject authentication and API calls unless that test key is registered in the Firebase Console.
   *Solution*: Always register the signature of the key used to sign the final patched APK in your Firebase Console.

4. **Package Name Binding**:
   PC HUB CENTER verifies the application's package name and product ID. If injected into an APK with a different package name, update the `google-services.json` and registered Android app in Firebase.

---

### 2. Step-by-Step MT Manager Integration on Authorized APKs

#### Step 2.1: Prepare Multidex
1. Compile the PC HUB CENTER module into a standalone DEX (`classes_pchub.dex`) using `d8` or Android Studio.
2. Open the authorized APK in MT Manager.
3. If the APK already contains `classes.dex`, rename `classes_pchub.dex` to the next available number (e.g., `classes2.dex` or `classes3.dex`).
4. Copy `classes2.dex` into the root of the APK.

#### Step 2.2: Add Required Permissions
Open `AndroidManifest.xml` in MT Manager (using AXML Editor) and ensure the following permissions are present inside `<manifest>`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

#### Step 2.3: Hook the Application Entry Point
Locate the main Activity or custom Application class in MT Manager:
1. Decompile `classes.dex` using Dex Editor Plus.
2. Search for the main launcher activity:
   `android.intent.action.MAIN` in `AndroidManifest.xml`.
3. Open the corresponding smali class (e.g., `Lcom/target/app/MainActivity;`).
4. In `onCreate(Landroid/os/Bundle;)V`:
   Insert the activation gate check immediately after `super.onCreate(savedInstanceState)`:

```smali
# PC HUB CENTER Smali Hook Example:
# Check if license is active before executing target code:
invoke-static {p0}, Lcom/pchub/license/PcHubLicenseManager;->get()Lcom/pchub/license/PcHubLicenseManager;
move-result-object v0
invoke-virtual {v0}, Lcom/pchub/license/PcHubLicenseManager;->isAppUnlocked()Z
move-result v1
if-nez v1, :cond_unlocked

# If locked, launch PC HUB CENTER Activation Activity:
new-instance v2, Landroid/content/Intent;
const-class v3, Lcom/pchub/license/ui/PcHubActivationActivity;
invoke-direct {v2, p0, v3}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
invoke-virtual {p0, v2}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
invoke-virtual {p0}, Landroid/app/Activity;->finish()V
return-void

:cond_unlocked
# Original target app code continues here...
```

#### Step 2.4: Re-sign the APK
1. In MT Manager, save and exit Dex Editor Plus.
2. Sign the APK using your custom Keystore (do not use default public test keys in production).
3. Extract the SHA-256 fingerprint of your Keystore and ensure it is registered under your Firebase project's Android Apps.
