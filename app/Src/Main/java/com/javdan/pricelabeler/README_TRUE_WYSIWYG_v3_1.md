# Javdan Price Labeler — v3.1 TRUE WYSIWYG

This patch fixes the Preview/Export mismatch in v3.

## Fixed

- Preview canvas now has a canonical 1000:730 design aspect ratio.
- Export no longer uses the original product image width/height as the output canvas.
- Normal JPG/PNG export uses the same aspect ratio as Designer Preview (2000x1460 export resolution).
- Product rendering preserves the cropped source image aspect ratio (FIT_CENTER inside the product bounding box), so product images are not stretched.
- Background, label position, label width, card layout, gap, padding and typography stay proportional between Preview and Export.
- MainActivity no longer forces the Designer to an arbitrary 1020px height; the Designer measures itself from the canonical aspect ratio.
- Existing Excel, Manual Entry, Batch Export, crop, templates, colors, patterns and Rial-to-Toman logic are retained.

## Files to replace

Copy these two files into:

`app/src/main/java/com/javdan/pricelabeler/`

- `MainActivity.java`
- `LabelDesignerView.java`

Then run:

`gradle :app:assembleDebug --stacktrace`

## Important

The legacy "append label outside image" mode is retained intentionally. Normal Designer output is strict WYSIWYG. The legacy append mode extends the canonical canvas horizontally by design.
