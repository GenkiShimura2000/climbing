# Climbing Android App (MVP)

This repository contains the Android MVP for the climbing video uploader.

## What is included
- Android app skeleton (Kotlin)
- GitHub Actions CI to build debug APK
- GitHub Actions release workflow to publish APK in GitHub Releases

## Your operation flow
1. Open **Actions** tab in GitHub.
2. Run `Android Release APK` workflow.
3. Set `release_tag` (example: `v0.1.0`) and run.
4. Open **Releases** and download `climbing-debug.apk`.
5. Install the APK on your Android device.

## Local development notes
- This project uses:
  - Android Gradle Plugin 8.5.2
  - Kotlin 1.9.24
  - compileSdk 34
  - Java 17

