# Fix SecurityException when accessing Media URIs

The application was encountering a `java.lang.SecurityException` when trying to access media URIs (specifically `content://media/external/images/media/...`). This typically occurs on Android 11+ due to Scoped Storage restrictions when the app tries to access a URI without sufficient permissions or when temporary URI permissions are not correctly propagated.

## Proposed Changes

### [Component] Android Manifest
- Added `READ_EXTERNAL_STORAGE` (for API <= 32) and `READ_MEDIA_IMAGES` (for API >= 33) permissions to `AndroidManifest.xml`.
- Added `READ_MEDIA_VISUAL_USER_SELECTED` for Android 14+ support.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/USER/AndroidStudioProjects/AllDirection/app/src/main/AndroidManifest.xml)

### [Component] Account Page (UI)
- Migrated from `ActivityResultContracts.GetContent()` to `ActivityResultContracts.PickVisualMedia()`.
- The Photo Picker (`PickVisualMedia`) is the modern, recommended way to pick media on Android. It provides better security and often doesn't require runtime permissions for the specific file picked, reducing the likelihood of `SecurityException`.
- Updated the image picking logic to use `PickVisualMediaRequest`.

#### [MODIFY] [AccountPage.kt](file:///C:/Users/USER/AndroidStudioProjects/AllDirection/app/src/main/java/com/fourDirection/allDirection/page/main/AccountPage.kt)

## Verification Plan

### Automated Tests
- Build the project to ensure no syntax errors were introduced by the migration to `PickVisualMedia`.
- Run the app and attempt to change the profile picture.

### Manual Verification
1. Open the **Account** page.
2. Tap on the camera icon to change the profile picture.
3. Select an image from the gallery (using the new Photo Picker).
4. Crop the image.
5. Verify that the image is uploaded successfully to Firebase and the profile picture updates without a `SecurityException`.
