# Javdan Price Labeler v3.8 — Camera FileProvider Fix

این نسخه مسیر دوربین را از MediaStore به فایل موقت اختصاصی خود برنامه تغییر می‌دهد.

## فایل‌های لازم

سه فایل Java را در مسیر زیر قرار دهید:

`app/src/main/java/com/javdan/pricelabeler/`

- `MainActivity.java`
- `LabelDesignerView.java`
- `CameraFileProvider.java`

## تغییر Manifest — الزامی

داخل تگ `<application>` فایل `app/src/main/AndroidManifest.xml` این Provider را اضافه کنید:

```xml
<provider
    android:name=".CameraFileProvider"
    android:authorities="${applicationId}.camera.provider"
    android:exported="false"
    android:grantUriPermissions="true" />
```

فایل `AndroidManifest_PROVIDER_SNIPPET.xml` نیز همین قطعه را دارد.

## رفتار جدید

1. عکس دوربین در `getExternalFilesDir(Pictures)/camera_capture` ساخته می‌شود.
2. دوربین از طریق `content://<package>.camera.provider/...` روی همان فایل می‌نویسد.
3. بعد از برگشت، برنامه وجود و اندازه واقعی JPEG را بررسی می‌کند.
4. JPEG مستقیماً با `BitmapFactory.decodeFile` خوانده می‌شود.
5. EXIF Orientation اصلاح می‌شود.
6. Bitmap مستقیماً به `currentBitmap` و `LabelDesignerView` داده می‌شود.
7. نتیجه OEM-specific و `data` برگشتی دوربین دیگر معیار اصلی نیست.

## Build

```bash
gradle :app:assembleDebug --stacktrace
```
