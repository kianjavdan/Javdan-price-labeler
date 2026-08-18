# Javdan Price Labeler — WYSIWYG Designer v3

این بسته جایگزین مستقیم فایل‌های `MainActivity.java` و `LabelDesignerView.java` نسخه `Javdan_compact_label_designer_v2` است.

## تغییرات اصلی
- Render Engine مشترک: `LabelDesignerView.renderScene()` هم برای Preview و هم Export استفاده می‌شود.
- Slider/SeekBar زنده برای Product X/Y/Width/Height/Zoom، عرض و موقعیت پنل، Gap، Padding، Radius، Border و اندازه‌های متن.
- Background: Solid / Gradient / Pattern / Custom image.
- 11 Pattern آماده و انتخاب تصویر دلخواه JPG/PNG.
- Color Picker واقعی با Hue، ناحیه Saturation/Brightness، Preview، HEX و Palette.
- Crop Mode چهارگوشه Drag + Grid + Reset + Apply.
- Auto Height بر اساس metric واقعی عنوان، قیمت، تومان، gap و padding.
- قیمت و «تومان» با اندازه و رنگ مستقل render می‌شوند.
- تنظیم مستقل هر Price Card با Live Preview.
- Template persistence برای تنظیمات جدید Background/Product/Label/Crop.
- Excel / Manual Entry / Batch Export / Rial-to-Toman / Image matching دست نخورده باقی مانده‌اند.
- حالت قدیمی «لیبل بیرون تصویر» همچنان موجود است، ولی پیش‌فرض خاموش است تا خروجی عادی دقیقاً با Designer یکسان بماند.

## نصب در پروژه فعلی
هر دو فایل را در package زیر جایگزین کنید:
`app/src/main/java/com/javdan/pricelabeler/`

فایل‌های `LabelField.java` و `XlsxReader.java` و سایر فایل‌های پروژه فعلی حذف یا تغییر داده نشوند.

## نکته WYSIWYG
Preview و Export هر دو این مسیر را دارند:
`renderScene -> drawBackground -> computeProductRect -> sourceCropRect -> computeLabelLayout -> drawCard`
بنابراین Gap، Padding، Font size، Pattern، Gradient، Crop و Product transform در دو مسیر جدا محاسبه نمی‌شوند.
