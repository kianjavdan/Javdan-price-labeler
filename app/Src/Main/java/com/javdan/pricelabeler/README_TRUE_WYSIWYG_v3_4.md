Javdan Price Labeler TRUE WYSIWYG v3.4

Fixes:
1) Batch Export no longer loses/clips the fourth price card when four cards exceed the available panel height.
2) If card heights do not fit, all visible cards are reduced equally so every card remains visible and aligned.
3) Excel price-header matching expanded for common variants such as عمده / پلن / تخفیف / فروش / همکار.
4) Batch fallback preserves the same Excel price-column order as Designer card order.
5) Existing WYSIWYG, Auto Height/manual height, Crop, Resize, Pattern, Gradient, Template and Rial->Toman behavior retained.

Replace:
app/src/main/java/com/javdan/pricelabeler/MainActivity.java
app/src/main/java/com/javdan/pricelabeler/LabelDesignerView.java
