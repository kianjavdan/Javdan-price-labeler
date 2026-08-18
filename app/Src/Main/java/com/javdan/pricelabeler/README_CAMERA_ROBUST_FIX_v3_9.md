# Javdan Price Labeler v3.9 — Robust Camera Return Fix

فایل‌های زیر را جایگزین کنید:
- MainActivity.java
- LabelDesignerView.java
- CameraFileProvider.java

اصلاحات:
- Retry کوتاه پس از برگشت از Camera برای کامل شدن فایل JPEG
- fallback به data.getData()
- fallback به extras["data"] Bitmap
- پشتیبانی Provider از r / w / wt / wa / rw / rwt
- openAssetFile برای سازگاری بیشتر با دوربین‌های OEM

AndroidManifest نسخه v3.8 کافی است و تغییر جدیدی لازم ندارد.
