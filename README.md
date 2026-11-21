# VYAPAR BILLING PRO

**Bill Smart. Grow Smart.**

A 100% free, zero-investment, offline-first native Android POS billing, GST invoicing, inventory, customer ledger, and PDF sharing application built with Kotlin and Jetpack Compose.

## Zero-Investment Architecture
- **No cloud backend or paid APIs**: Works 100% offline in Airplane Mode.
- **No SQL / Room database**: Pure atomic local JSON file storage in `filesDir/VyaparBilling/`.
- **Genuine PDF Generation**: Native Android `PdfDocument` supporting A4 multi-page invoices, 80mm thermal receipts, Hindi & English labels, and shop logo rendering.
- **WhatsApp PDF Sharing**: Real `application/pdf` document sharing via Android `FileProvider` and `Intent.ACTION_SEND`.
- **Backup & Restore**: Offline JSON export/import via Android Storage Access Framework (SAF).

## Build Real APK on GitHub Actions (Free)
1. Create a public GitHub repository.
2. Upload all project source files.
3. Push to the `main` branch.
4. Open the **Actions** tab in your GitHub repository.
5. Run the **Android Build (Vyapar Billing Pro)** workflow (or wait for the automatic push trigger).
6. Wait for the build job to turn green.
7. Open the completed workflow run.
8. Download **`VYAPAR-BILLING-PRO-APK`** from the **Artifacts** section.
9. Extract the downloaded ZIP file.
10. Install `app-debug.apk` on your Android 8.0+ device.

## Signed Release Build (Optional)
For Play Store `.aab` or signed `.apk` builds, set `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables and run:
```bash
./gradlew assembleRelease bundleRelease
```
