# PC HUB CENTER — Android License Activation Module

**PC HUB CENTER** is a lightweight, secure, and reusable Android license activation module and backend system.

---

## Key Highlights & Features

1. **Strict PC HUB CENTER Branding**:
   - Zero references to other names in the UI.
   - Clean, professional dark navy & cyber cyan theme (`#080C15`, `#0E172A`, `#00E5FF`, `#2979FF`).

2. **Clean Minimalist Activation Screen**:
   - Clean input field with uppercase auto-formatting and clipboard paste button.
   - **ACTIVATE** button with live verification loader.
   - **BUY KEY (WHATSAPP)** button linking directly to administrator (`+91 9118260403`) with pre-filled device ID and product details.

3. **Required Popups & States Implemented**:
   - **Invalid License**: "The entered license key is invalid." (Button: `TRY AGAIN`)
   - **License Blocked**: "This license has been blocked by the administrator." (Button: `OK`, app remains locked)
   - **License Banned**: "This license has been permanently banned." (Button: `OK`, app remains locked)
   - **License Expired**: "Your license has expired." (Button: `OK`, app remains locked)
   - **Active License**: Auto-unlocks the application into the protected PC HUB Dashboard / main home page.

4. **Multi-Product Reusable Architecture**:
   - Configurable `productId` per integrated application (e.g. `PCHUB-TOOL-001`).
   - Hardware-bound installation identity via `DeviceIdentityManager` (SHA-256 salted hardware properties + Android Keystore).
   - One license key per device binding enforcement.

5. **72-Hour Offline Grace Period**:
   - Uses asymmetric RSA-2048 digital signatures.
   - Allows users to run offline for up to 72 hours before internet re-verification is required.

6. **Cloud Firestore & Cloud Functions Ready**:
   - Zero-trust `firestore.rules` deployed directly to Firestore.
   - Ready-to-deploy Cloud Functions in `/functions/index.js` for future Web Admin Panel operations (key generation, bulk creation, remote block, ban, device reset).

---

## Testing Keys (Type or Paste in Activation Box)

You can type or paste any of these keys into the input box:

| Key | Expected Action / Popup |
|---|---|
| `PCHUB-DEMO-30DAYS-VALID` | Activates 30-Day License -> Unlocks PC HUB Dashboard / Home |
| `PCHUB-DEMO-LIFETIME-VALID` | Activates Lifetime License -> Unlocks PC HUB Dashboard / Home |
| `PCHUB-DEMO-BLOCKED-KEY` | Opens **License Blocked** Popup (Locked) |
| `PCHUB-DEMO-BANNED-KEY` | Opens **License Banned** Popup (Locked) |
| `PCHUB-DEMO-EXPIRED-KEY` | Opens **License Expired** Popup (Locked) |
| `PCHUB-INVALID-XYZ` | Opens **Invalid License** Popup (`TRY AGAIN`) |

---

## Included Documentation

- `INTEGRATION_GUIDE.md`: Step-by-step instructions for embedding the module into any Android application.
- `MT_MANAGER_GUIDE.md`: Technical guide for authorized APK patching, DEX injection, smali hooks, and avoiding common pitfalls.
- `FIREBASE_ADMIN_GUIDE.md`: Database schema and instructions for building a future Web Admin Panel.
