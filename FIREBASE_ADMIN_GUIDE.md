# PC HUB CENTER — Firebase Backend & Future Admin Panel Guide

### 1. Firestore Database Schema

#### `/licenses/{licenseKey}`
Each document is identified by the uppercase license key (e.g., `PCHUB-DEMO-30DAYS-VALID`):

| Field | Type | Description |
|---|---|---|
| `licenseKey` | String | Unique license key string |
| `productId` | String | Target product identifier (e.g. `PCHUB-TOOL-001`) |
| `status` | String | `UNACTIVATED`, `ACTIVE`, `BLOCKED`, `BANNED`, `EXPIRED` |
| `type` | String | `7_DAYS`, `30_DAYS`, `90_DAYS`, `180_DAYS`, `365_DAYS`, `LIFETIME`, `CUSTOM` |
| `durationDays` | Number | Validity length in days (-1 for lifetime) |
| `boundDeviceId` | String | Hardware-bound device UUID (empty when unactivated) |
| `deviceModel` | String | Device model (e.g., "Google Pixel 8") |
| `createdAt` | Timestamp | Creation timestamp |
| `activationDate`| Timestamp | When the key was first activated |
| `expiresAt` | Timestamp | License expiration date |
| `lastVerificationDate` | Timestamp | Last online ping or verification check |
| `notes` | String | Optional administrator note |

#### `/activations/{logId}`
Audit and telemetry logs:

| Field | Type | Description |
|---|---|---|
| `licenseKey` | String | Associated license key |
| `productId` | String | Product ID |
| `deviceId` | String | Hardware UUID |
| `action` | String | `ACTIVATION_SUCCESS`, `REVERIFICATION`, `RESET_BINDING` |
| `timestamp` | Timestamp | Event timestamp |

---

### 2. Ready-to-Use Firebase Cloud Functions

The functions are located in `/functions/index.js` and can be deployed with:
```bash
cd functions
firebase deploy --only functions
```

#### Available Functions for Future Web Admin Panel:

1. **`generateLicenseKeys`**:
   - Parameters: `{ productId: "PCHUB-TOOL-001", type: "30_DAYS", durationDays: 30, count: 10, notes: "Batch #1" }`
   - Generates individual or bulk license keys in Firestore with `UNACTIVATED` status.

2. **`blockLicense`**:
   - Parameters: `{ licenseKey: "PCHUB-XXXX-XXXX" }`
   - Immediately changes status to `BLOCKED`. Connected Android apps will lock and show the "License Blocked" popup.

3. **`unblockLicense`**:
   - Parameters: `{ licenseKey: "PCHUB-XXXX-XXXX" }`
   - Restores status to `ACTIVE`.

4. **`banLicense`**:
   - Parameters: `{ licenseKey: "PCHUB-XXXX-XXXX", reason: "Fraud" }`
   - Permanently changes status to `BANNED`. Connected apps will lock and show the "License Banned" popup.

5. **`resetDeviceBinding`**:
   - Parameters: `{ licenseKey: "PCHUB-XXXX-XXXX" }`
   - Clears `boundDeviceId` and `deviceModel`, allowing the license to be activated on a new device.

---

### 3. Test License Keys Provisioned in Demo

The following test keys are supported for verification:
- `PCHUB-DEMO-30DAYS-VALID` (Unactivated 30-day key -> activates to Active)
- `PCHUB-DEMO-LIFETIME-VALID` (Unactivated Lifetime key -> activates to Permanent access)
- `PCHUB-DEMO-BLOCKED-KEY` (Blocked key -> shows "License Blocked" modal popup)
- `PCHUB-DEMO-BANNED-KEY` (Banned key -> shows "License Banned" modal popup)
- `PCHUB-DEMO-EXPIRED-KEY` (Expired key -> shows "License Expired" modal popup)
- `PCHUB-INVALID-XYZ` (Invalid key -> shows "Invalid License" modal popup)
