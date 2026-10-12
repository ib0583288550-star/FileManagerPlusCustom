# File Manager Plus Custom

Android file-manager prototype inspired by the category-based layout of File Manager Plus.

Features in this first build:
- Categories: Apps, Images, Videos, Audio, Documents, Downloads, New files
- Hebrew category editor
- Add and delete categories
- Move categories left/right (reorder)
- Order is saved locally

## Build APK
Open GitHub Actions and run **Build APK** manually. The workflow uploads `app-debug.apk` as an artifact.

This is an original implementation; it does not include proprietary File Manager Plus source code or assets.

## Version 1.10
- Quick Settings tile can open the app directly or show a Yes/No confirmation dialog.
- Includes a built-in storage-manager app icon.
- Adds refresh and clearer storage labels for internal, external, and OTG storage.

## Version 1.22
- Shows a system notification after mounting or ejecting storage from the Quick Settings tile.
- Requests notification permission on Android 13 and newer.
- Keeps the same app ID so it can be installed as an update.
