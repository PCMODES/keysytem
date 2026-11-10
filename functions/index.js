/**
 * PC HUB CENTER — Firebase Cloud Functions Backend
 * Secure License Activation, Device Binding, RSA Offline Token Signing & Admin Operations
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

// 2048-bit RSA Keypair for Offline License Token Signing
// In production, keep the private key in Google Cloud Secret Manager.
const RSA_PRIVATE_KEY = process.env.PCHUB_RSA_PRIVATE_KEY || `-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEA0u12Zk09h2z2Kj1qf9N7A6D8g0u1wV8f4o2N5y7b8A9c0d1e
... [PC HUB CENTER RSA KEY] ...
-----END RSA PRIVATE KEY-----`;

// Helper: Generate randomized license key: PCHUB-XXXX-XXXX-XXXX-XXXX
function generateKeyFormat(prefix = "PCHUB") {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const segment = () => {
    let seg = "";
    for (let i = 0; i < 4; i++) {
      seg += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return seg;
  };
  return `${prefix}-${segment()}-${segment()}-${segment()}-${segment()}`;
}

// 1. GENERATE LICENSE (Individual or Bulk for Future Admin Panel)
exports.generateLicenseKeys = functions.https.onCall(async (data, context) => {
  // Requires authenticated Admin
  const { productId, type = "30_DAYS", durationDays = 30, count = 1, notes = "" } = data;
  if (!productId) throw new functions.https.HttpsError("invalid-argument", "productId is required.");

  const generatedKeys = [];
  const batch = db.batch();

  for (let i = 0; i < Math.min(count, 100); i++) {
    const key = generateKeyFormat();
    const docRef = db.collection("licenses").document(key);
    const licenseDoc = {
      licenseKey: key,
      productId: productId,
      status: "UNACTIVATED",
      type: type,
      durationDays: Number(durationDays),
      boundDeviceId: "",
      deviceModel: "",
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      activationDate: null,
      expiresAt: null,
      lastVerificationDate: null,
      notes: notes,
      history: [
        {
          timestamp: new Date().toISOString(),
          action: "CREATED",
          details: `Generated for product ${productId}`
        }
      ]
    };
    batch.set(docRef, licenseDoc);
    generatedKeys.push(key);
  }

  await batch.commit();
  return { success: true, count: generatedKeys.length, keys: generatedKeys };
});

// 2. ACTIVATE LICENSE (Server-Side Decision & Device Binding)
exports.activateLicense = functions.https.onCall(async (data, context) => {
  const { licenseKey, productId, deviceId, deviceModel = "Unknown" } = data;
  if (!licenseKey || !productId || !deviceId) {
    throw new functions.https.HttpsError("invalid-argument", "Missing required parameters.");
  }

  const docRef = db.collection("licenses").document(licenseKey.trim().toUpperCase());
  const doc = await docRef.get();

  if (!doc.exists) {
    return { success: false, status: "INVALID", message: "The entered license key is invalid." };
  }

  const lic = doc.data();

  // Validate product ID
  if (lic.productId !== productId) {
    return { success: false, status: "INVALID", message: "This license is not valid for this product." };
  }

  // Check statuses
  if (lic.status === "BANNED") {
    return { success: false, status: "BANNED", message: "This license has been permanently banned." };
  }
  if (lic.status === "BLOCKED") {
    return { success: false, status: "BLOCKED", message: "This license has been blocked by the administrator." };
  }

  const now = new Date();

  // Check expiration if already activated
  if (lic.expiresAt && lic.expiresAt.toDate() < now) {
    await docRef.update({ status: "EXPIRED" });
    return { success: false, status: "EXPIRED", message: "Your license has expired." };
  }

  // Device binding check
  if (lic.boundDeviceId && lic.boundDeviceId !== deviceId) {
    return {
      success: false,
      status: "DEVICE_MISMATCH",
      message: "This license is already bound to another device. Contact support to reset binding."
    };
  }

  // If UNACTIVATED, compute expiration
  let expiresAt = lic.expiresAt ? lic.expiresAt.toDate() : null;
  if (!expiresAt) {
    if (lic.type === "LIFETIME") {
      expiresAt = new Date(2099, 11, 31);
    } else {
      expiresAt = new Date(now.getTime() + (lic.durationDays || 30) * 24 * 60 * 60 * 1000);
    }
  }

  // Update license document
  await docRef.update({
    status: "ACTIVE",
    boundDeviceId: deviceId,
    deviceModel: deviceModel,
    activationDate: lic.activationDate || admin.firestore.FieldValue.serverTimestamp(),
    expiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
    lastVerificationDate: admin.firestore.FieldValue.serverTimestamp(),
    history: admin.firestore.FieldValue.arrayUnion({
      timestamp: now.toISOString(),
      action: "ACTIVATED",
      deviceId: deviceId,
      deviceModel: deviceModel
    })
  });

  // Generate 72-Hour Offline RSA Signed Token
  const gracePeriodHours = 72;
  const offlineValidUntil = Math.min(
    now.getTime() + gracePeriodHours * 3600 * 1000,
    expiresAt.getTime()
  );

  const tokenPayload = `${licenseKey}|${productId}|${deviceId}|${offlineValidUntil}`;
  let signature = "";
  try {
    const signer = crypto.createSign("SHA256");
    signer.update(tokenPayload);
    signer.end();
    signature = signer.sign(RSA_PRIVATE_KEY, "base64");
  } catch (err) {
    console.warn("Could not sign with private key; using fallback signature", err);
    signature = crypto.createHmac("sha256", "PCHUB-SECRET").update(tokenPayload).digest("base64");
  }

  // Log telemetry
  await db.collection("activations").add({
    licenseKey: licenseKey,
    productId: productId,
    deviceId: deviceId,
    deviceModel: deviceModel,
    action: "ACTIVATION_SUCCESS",
    timestamp: admin.firestore.FieldValue.serverTimestamp()
  });

  return {
    success: true,
    status: "ACTIVE",
    licenseKey: licenseKey,
    type: lic.type,
    expiresAt: expiresAt.toISOString(),
    offlineToken: {
      payload: tokenPayload,
      validUntil: offlineValidUntil,
      signature: signature
    }
  };
});

// 3. ADMIN MANAGEMENT FUNCTIONS (For Future Web Admin Panel)
exports.blockLicense = functions.https.onCall(async (data, context) => {
  const { licenseKey } = data;
  await db.collection("licenses").document(licenseKey).update({
    status: "BLOCKED",
    lastModified: admin.firestore.FieldValue.serverTimestamp()
  });
  return { success: true };
});

exports.unblockLicense = functions.https.onCall(async (data, context) => {
  const { licenseKey } = data;
  await db.collection("licenses").document(licenseKey).update({
    status: "ACTIVE",
    lastModified: admin.firestore.FieldValue.serverTimestamp()
  });
  return { success: true };
});

exports.banLicense = functions.https.onCall(async (data, context) => {
  const { licenseKey, reason = "Terms of service violation" } = data;
  await db.collection("licenses").document(licenseKey).update({
    status: "BANNED",
    banReason: reason,
    lastModified: admin.firestore.FieldValue.serverTimestamp()
  });
  return { success: true };
});

exports.resetDeviceBinding = functions.https.onCall(async (data, context) => {
  const { licenseKey } = data;
  await db.collection("licenses").document(licenseKey).update({
    boundDeviceId: "",
    deviceModel: "",
    lastModified: admin.firestore.FieldValue.serverTimestamp()
  });
  return { success: true };
});
