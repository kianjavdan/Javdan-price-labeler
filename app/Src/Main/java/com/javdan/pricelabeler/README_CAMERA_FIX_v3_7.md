# Javdan Price Labeler v3.7 — Camera Return Fix

رفع مشکل باز شدن دوربین ولی وارد نشدن عکس به برنامه.

تغییرات:
- ذخیره pending camera Uri قبل از باز شدن دوربین در SharedPreferences.
- بازیابی Uri بعد از Activity recreation / process pressure.
- ذخیره Uri در onSaveInstanceState.
- پذیرش عکس حتی در بعضی دوربین‌های OEM که پس از نوشتن JPEG، RESULT_CANCELED برمی‌گردانند.
- بررسی واقعی وجود/اندازه فایل دوربین قبل از حذف.
- خارج کردن IS_PENDING در Android 10+ قبل از Decode.
- اصلاح EXIF و ورود مستقیم Bitmap به currentBitmap و Designer.
- Refresh فوری Designer پس از بازگشت از دوربین.
- حفظ تمام قابلیت‌های v3.6 از جمله Smart Snap، Batch، Excel و WYSIWYG.

جایگزینی:
app/src/main/java/com/javdan/pricelabeler/MainActivity.java
app/src/main/java/com/javdan/pricelabeler/LabelDesignerView.java
