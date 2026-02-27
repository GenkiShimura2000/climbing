# Climbing Android App (MVP)

This repository contains the Android MVP for the climbing video uploader.

## What is included
- Android app skeleton (Kotlin)
- Folder selection using Android SAF
- YouTube sign-in button (Google Sign-In with `youtube.upload` scope)
- Manual sync button (WorkManager, Wi-Fi only)
- GitHub Actions CI to build debug APK
- GitHub Actions release workflow to publish APK in GitHub Releases

## Your operation flow
1. Open **Actions** tab in GitHub.
2. Run `Android Release APK` workflow.
3. Set `release_tag` (example: `v0.1.0`) and run.
4. Open **Releases** and download `climbing-debug.apk`.
5. Install the APK on your Android device.
6. In app:
   - Select target folder
   - Connect YouTube account
   - Tap sync

## Local development notes
- This project uses:
  - Android Gradle Plugin 8.5.2
  - Kotlin 1.9.24
  - compileSdk 34
  - Java 17

## Required Google Cloud setup
YouTube upload requires OAuth configuration in Google Cloud Console.

1. Enable **YouTube Data API v3**.
2. Create an **OAuth client for Android**.
3. Register package name: `com.genkishimura.climbing`.
4. Register SHA-1 fingerprint for the signing certificate used in your APK.

If OAuth settings do not match the installed APK signature, YouTube sign-in will fail.
