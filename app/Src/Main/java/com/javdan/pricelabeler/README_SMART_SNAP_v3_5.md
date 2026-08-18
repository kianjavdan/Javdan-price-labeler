# Javdan Price Labeler v3.5 — Smart Product Snap

Base: v3.4 TRUE WYSIWYG.

## Added
- Smart Product Snap section under Resize + Crop.
- Live checkbox: automatic product detection.
- Sensitivity slider (20–120), default 62.
- Product padding slider (0–20%), default 3.5%.
- Preview detection button with approximate confidence.
- Batch option: detect each product independently for every image.
- Fail-safe: if detection is uncertain, preserve the full original image instead of making a destructive crop.
- Smart Snap settings persist in SharedPreferences / template settings.
- Existing manual Crop remains available when Smart Snap is off.
- Rendering still uses the same TRUE-WYSIWYG engine.

## Detection model
This build uses a fast local image-processing detector optimized for product photos on plain or near-plain backgrounds. It estimates the border background color and detects foreground by color distance. It does not require internet or an AI model.

For complex scenes (shelves, hands, textured rooms, multiple objects), an ML segmentation model should be added as a separate future mode.
