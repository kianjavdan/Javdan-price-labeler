# Javdan Price Labeler v4.0 — Samsung Camera / MediaStore Fix

فایل‌های زیر را در package اصلی برنامه جایگزین کنید:
- MainActivity.java
- LabelDesignerView.java
- CameraFileProvider.java

تغییر اصلی:
- گرفتن عکس دیگر برای مسیر اصلی به فایل خصوصی CameraFileProvider وابسته نیست.
- قبل از باز شدن دوربین یک MediaStore Uri استاندارد ساخته می‌شود.
- در Android 10+ عکس با IS_PENDING=1 ایجاد و بعد از دریافت موفق IS_PENDING=0 می‌شود.
- URI قبل از باز شدن دوربین ذخیره می‌شود و بعد از بازسازی Activity هم بازیابی می‌شود.
- پس از برگشت از دوربین تا حدود 3 ثانیه برای نوشته‌شدن JPEG retry انجام می‌شود.
- fallback به data.getData() و thumbnail دوربین همچنان حفظ شده است.
- در صورت شکست، رکورد خالی MediaStore حذف می‌شود.

AndroidManifest فعلی می‌تواند بدون تغییر باقی بماند.
CameraFileProvider نیز برای سازگاری با نسخه‌های قبلی نگه داشته شده، ولی مسیر اصلی دوربین v4 از MediaStore استفاده می‌کند.
