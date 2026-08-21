package com.javdan.pricelabeler;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;

/**
 * WYSIWYG renderer for Javdan Price Labeler.
 * Preview and export call the SAME renderScene() method.
 */
public class LabelDesignerView extends View {
    public static final int TEXT_TITLE = 0;
    public static final int TEXT_PRICE = 1;

    public static final int BG_SOLID = 0;
    public static final int BG_GRADIENT = 1;
    public static final int BG_PATTERN = 2;
    public static final int BG_IMAGE = 3;

    /**
     * Canonical design canvas. Preview and export MUST keep this exact aspect ratio.
     * Preview is only a scaled view of this logical canvas; export is a higher-resolution
     * rendering of the same canvas.
     */
    public static final float DESIGN_WIDTH = 1000f;
    public static final float DESIGN_HEIGHT = 730f;
    public static final float DESIGN_ASPECT = DESIGN_WIDTH / DESIGN_HEIGHT;
    public static final int EXPORT_WIDTH = 2000;
    public static final int EXPORT_HEIGHT = Math.round(EXPORT_WIDTH / DESIGN_ASPECT);

    public interface Listener {
        void onFieldSelected(int index);
        void onChanged();
        void onTextClicked(int fieldIndex, int textType);
    }

    /**
     * Touch-transform targets. When one of these is active, single-finger drag moves the
     * target and a two-finger pinch resizes it — this replaces the old X/Y/Width/Zoom
     * SeekBars for the product image, the main label panel and the info panel.
     */
    public static final int TARGET_NONE = -1;
    public static final int TARGET_PRODUCT = 0;
    public static final int TARGET_PANEL = 1;
    public static final int TARGET_INFO = 2;
    private int transformTarget = TARGET_NONE;
    private final ScaleGestureDetector scaleDetector;
    private float xLast, yLast;

    private ArrayList<LabelField> fields = new ArrayList<>();
    private ArrayList<InfoField> infoFields = new ArrayList<>();
    private Bitmap productBitmap;
    private Bitmap customBackgroundBitmap;
    private int selected = -1;
    private Listener listener;

    private final RectF previewProductRect = new RectF();
    private final RectF previewProductBoxRect = new RectF();
    private final RectF previewLabelRect = new RectF();
    private final RectF previewInfoRect = new RectF();
    private final ArrayList<RectF> previewCardRects = new ArrayList<>();

    // Product transform (normalized canvas coordinates)
    public float productX = 0.02f;
    public float productY = 0.08f;
    public float productW = 0.62f;
    public float productH = 0.84f;
    public float productZoom = 1f;

    // Background
    public int backgroundMode = BG_SOLID;
    public int canvasBackground = 0xFFF2F2F2;
    public int gradientColor1 = 0xFFFFFFFF;
    public int gradientColor2 = 0xFFE8EEF8;
    public float gradientAngle = 0f;
    public int backgroundAlpha = 255;
    public int patternIndex = 0;

    // Crop is normalized to source image. Source bitmap is never destructively resized.
    public boolean cropEnabled = false;
    public float cropLeft = 0f;
    public float cropTop = 0f;
    public float cropRight = 1f;
    public float cropBottom = 1f;
    private boolean cropMode = false;
    private int cropHandle = -1;

    // Main label panel
    public int outerTagColor = 0xFF181818;
    public int outerTagBorderColor = 0xFF3A3A3A;
    public int outerTagBorderWidth = 3;
    public int outerTagRadius = 22;
    public float labelWidthPct = 0.28f;
    public float labelX = 0.70f;
    public float labelY = 0.12f;
    /** field gap in logical px relative to a 1000px design canvas. Default 6px. */
    public float fieldGapPx = 6f;
    /** outer panel inner padding in logical px relative to a 1000px design canvas. */
    public float panelPaddingPx = 10f;
    public boolean autoHeight = true;
    /** Manual common height for all price cards when Auto Height is disabled, in logical design px. */
    public float manualCardHeightPx = 130f;

    // Legacy compatibility. MainActivity v2 used this normalized field.
    public float fieldGapPct = 0.006f;

    // Product information panel
    public boolean infoPanelEnabled = true;
    // 0 = below product, 1 = above product, 2 = free/custom Y
    public int infoPositionMode = 0;
    // 0 = vertical, 1 = horizontal, 2 = wrap/grid
    public int infoLayoutMode = 2;
    public int infoColumns = 2;
    public float infoWidthPct = 0.48f;
    public float infoX = 0.10f;
    public float infoY = 0.76f;
    public float infoDistancePx = 12f;
    public float infoGapPx = 8f;
    public float infoPaddingPx = 10f;
    public int infoOuterColor = 0x00FFFFFF;
    public int infoOuterBorderColor = 0xFF6AAE72;
    public int infoOuterBorderWidth = 0;
    public int infoOuterRadius = 18;

    private float touchDownX, touchDownY, lastX, lastY;
    private boolean moved = false;

    public LabelDesignerView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setBackgroundColor(Color.TRANSPARENT);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    public void setListener(Listener l) { listener = l; }

    /** Activates touch move+pinch for one target (product/panel/info), or TARGET_NONE to
     *  go back to normal tap-to-select-field behavior. Cancels Crop mode if it was active. */
    public void setTransformTarget(int target) {
        transformTarget = target;
        if (target != TARGET_NONE) cropMode = false;
        invalidate();
    }
    public int getTransformTarget() { return transformTarget; }
    public void setFields(ArrayList<LabelField> f) { fields = f == null ? new ArrayList<>() : f; invalidate(); }
    public ArrayList<LabelField> getFields() { return fields; }
    public void setInfoFields(ArrayList<InfoField> f) { infoFields = f == null ? new ArrayList<>() : f; invalidate(); }
    public ArrayList<InfoField> getInfoFields() { return infoFields; }
    public void setProductBitmap(Bitmap b) { productBitmap = b; invalidate(); }
    public void setCustomBackgroundBitmap(Bitmap b) { customBackgroundBitmap = b; invalidate(); }
    public void select(int index) { selected = index; invalidate(); }
    public int getSelectedIndex() { return selected; }

    public void setCropMode(boolean enabled) {
        cropMode = enabled;
        if (enabled) transformTarget = TARGET_NONE;
        if (enabled && !cropEnabled) cropEnabled = true;
        cropHandle = -1;
        invalidate();
    }
    public boolean isCropMode() { return cropMode; }

    public void resetCrop() {
        cropEnabled = false;
        cropLeft = 0f; cropTop = 0f; cropRight = 1f; cropBottom = 1f;
        invalidate();
    }

    public void setCrop(float l, float t, float r, float b) {
        cropLeft = clamp(l, 0f, .95f);
        cropTop = clamp(t, 0f, .95f);
        cropRight = clamp(r, cropLeft + .05f, 1f);
        cropBottom = clamp(b, cropTop + .05f, 1f);
        cropEnabled = true;
        invalidate();
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        if (widthMode == MeasureSpec.UNSPECIFIED) width = Math.round(DESIGN_WIDTH);
        else width = Math.max(1, widthSize);

        int desiredHeight = Math.max(1, Math.round(width / DESIGN_ASPECT));
        int height = desiredHeight;
        if (heightMode == MeasureSpec.AT_MOST) height = Math.min(desiredHeight, heightSize);
        // Even if a legacy parent supplied an EXACT height, keep the canonical aspect ratio.
        // MainActivity v3.1 uses WRAP_CONTENT, so this branch is only for backward compatibility.

        setMeasuredDimension(width, Math.max(1, height));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0 || getHeight() <= 0) return;
        renderScene(canvas, getWidth(), getHeight(), productBitmap, true);
    }

    /** ONE renderer used by both preview and export. */
    private void renderScene(Canvas canvas, float w, float h, Bitmap source, boolean editorOverlay) {
        drawBackground(canvas, w, h);

        RectF productRect = computeProductRect(w, h, source);
        if (editorOverlay) {
            previewProductRect.set(productRect);
            previewProductBoxRect.set(computeProductBoxRect(w, h));
        }
        drawProduct(canvas, source, productRect);

        Layout layout = computeLabelLayout(w, h);
        if (editorOverlay) {
            previewLabelRect.set(layout.outer);
            previewCardRects.clear();
            for (RectF r : layout.cards) previewCardRects.add(new RectF(r));
        }

        drawOuterTag(canvas, layout.outer, w, h);
        int cardIndex = 0;
        for (int i = 0; i < fields.size(); i++) {
            LabelField f = fields.get(i);
            if (!f.visible) continue;
            if (cardIndex >= layout.cards.size()) break;
            drawCard(canvas, f, layout.cards.get(cardIndex), editorOverlay && i == selected, w, h);
            cardIndex++;
        }

        if (infoPanelEnabled && !infoFields.isEmpty()) {
            InfoLayout infoLayout = computeInfoLayout(w, h, productRect);
            if (editorOverlay) previewInfoRect.set(infoLayout.outer);
            drawProductInfoPanelFromLayout(canvas, w, h, infoLayout);
        } else if (editorOverlay) {
            previewInfoRect.set(0, 0, 0, 0);
        }

        if (editorOverlay && cropMode) drawCropOverlay(canvas, productRect, source);
        if (editorOverlay && transformTarget != TARGET_NONE) drawTransformOverlay(canvas);
    }

    /** Bounding box used for the product image (before FIT_CENTER letterboxing). This is the
     *  rect that touch-drag/pinch manipulates, matching what the old X/Y/W/H/Zoom sliders did. */
    private RectF computeProductBoxRect(float w, float h) {
        float boxW = clamp(productW * productZoom, .03f, 1.5f) * w;
        float boxH = clamp(productH * productZoom, .03f, 1.5f) * h;
        float cx = (productX + productW * .5f) * w;
        float cy = (productY + productH * .5f) * h;
        return new RectF(cx - boxW / 2f, cy - boxH / 2f, cx + boxW / 2f, cy + boxH / 2f);
    }

    /**
     * Computes the actual drawn product rectangle. productW/productH define a bounding box,
     * while the bitmap itself is FIT_CENTER inside that box so its aspect ratio is never
     * distorted. This is critical for Preview == Export.
     */
    private RectF computeProductRect(float w, float h, Bitmap source) {
        float boxW = clamp(productW * productZoom, .03f, 1.5f) * w;
        float boxH = clamp(productH * productZoom, .03f, 1.5f) * h;
        float cx = (productX + productW * .5f) * w;
        float cy = (productY + productH * .5f) * h;

        if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
            return new RectF(cx - boxW/2f, cy - boxH/2f, cx + boxW/2f, cy + boxH/2f);
        }

        Rect src = sourceCropRect(source);
        float srcW = Math.max(1f, src.width());
        float srcH = Math.max(1f, src.height());
        float srcAspect = srcW / srcH;
        float boxAspect = boxW / Math.max(1f, boxH);

        float drawW, drawH;
        if (srcAspect >= boxAspect) {
            drawW = boxW;
            drawH = drawW / srcAspect;
        } else {
            drawH = boxH;
            drawW = drawH * srcAspect;
        }
        return new RectF(cx - drawW/2f, cy - drawH/2f, cx + drawW/2f, cy + drawH/2f);
    }

    private static class Layout {
        RectF outer = new RectF();
        ArrayList<RectF> cards = new ArrayList<>();
    }

    private Layout computeLabelLayout(float w, float h) {
        Layout out = new Layout();
        float logicalScale = Math.min(w, h) / 1000f;
        float gap = Math.max(0f, fieldGapPx * logicalScale);
        // compatibility for old templates that stored gapPct
        if (fieldGapPx < 0f) gap = Math.max(0f, h * fieldGapPct);
        float outerPad = Math.max(2f, panelPaddingPx * logicalScale);
        float labelW = clamp(labelWidthPct, .12f, .70f) * w;
        float labelLeft = clamp(labelX, -.30f, 1f) * w;
        if (labelLeft + labelW > w) labelLeft = w - labelW - w*.012f;

        int count = visibleCount();
        float totalCardsH = 0f;
        ArrayList<Float> cardHeights = new ArrayList<>();

        // All visible cards always share ONE common height.
        // Auto Height chooses the tallest natural content height so no card clips,
        // while manual mode uses the user's explicit slider value.
        float commonCardH;
        if (autoHeight) {
            commonCardH = 22f * logicalScale;
            for (LabelField f : fields) {
                if (!f.visible) continue;
                commonCardH = Math.max(commonCardH,
                        measureCardHeight(f, labelW - 2f*outerPad, logicalScale));
            }
        } else {
            commonCardH = Math.max(35f, manualCardHeightPx) * logicalScale;
        }

        for (LabelField f : fields) {
            if (!f.visible) continue;
            cardHeights.add(commonCardH);
            totalCardsH += commonCardH;
        }
        // Never let the last card fall outside the panel/canvas.
        // Previous builds clamped only tagH to 96% of the canvas but kept the original
        // commonCardH. With four cards this could make card #4 receive a zero/negative
        // visible rect and disappear in Batch Export. If the natural/manual height does
        // not fit, shrink ALL visible cards equally so their count/order is preserved.
        float maxTagH = h * .96f;
        float chromeH = outerPad * 2f + gap * Math.max(0, count - 1);
        float maxCommonCardH = Math.max(1f, (maxTagH - chromeH) / Math.max(1, count));
        if (commonCardH > maxCommonCardH) {
            commonCardH = maxCommonCardH;
            totalCardsH = commonCardH * count;
            cardHeights.clear();
            for (int i = 0; i < count; i++) cardHeights.add(commonCardH);
        }

        float tagH = chromeH + totalCardsH;
        tagH = Math.min(maxTagH, Math.max(tagH, 40f*logicalScale));

        float top = clamp(labelY, 0f, 1f) * h;
        if (top + tagH > h) top = Math.max(h*.02f, h - tagH - h*.02f);
        out.outer.set(labelLeft, top, labelLeft + labelW, top + tagH);

        float y = out.outer.top + outerPad;
        int vi = 0;
        for (LabelField f : fields) {
            if (!f.visible) continue;
            float ch = cardHeights.get(vi++);
            // If tag was constrained, cards still remain compact and are clipped rather than silently re-laid out.
            RectF r = new RectF(out.outer.left + outerPad, y, out.outer.right - outerPad,
                    y + ch);
            out.cards.add(r);
            y += ch + gap;
        }
        return out;
    }

    private float measureCardHeight(LabelField f, float cardW, float scale) {
        float padV = Math.max(0f, f.paddingVertical) * scale;
        float titleGap = Math.max(0f, f.titlePriceGap) * scale;
        float titleH = 0f, priceH = 0f, tomanH = 0f;
        if (f.showTitle && !safe(f.name).isEmpty()) {
            Paint p = textPaint(f.titleColor, f.getTitleTypeface(), spToPx(f.titleSize, scale));
            titleH = p.descent() - p.ascent();
        }
        if (f.showPrice && !safe(f.value).isEmpty()) {
            Paint p = textPaint(f.priceColor, f.getPriceTypeface(), spToPx(f.priceSize, scale));
            priceH = p.descent() - p.ascent();
            if (f.showToman) {
                Paint tp = textPaint(f.tomanColor, f.getPriceTypeface(), spToPx(f.tomanSize, scale));
                tomanH = tp.descent() - tp.ascent();
            }
        }
        float priceLine = Math.max(priceH, tomanH);
        float content = titleH + ((titleH > 0 && priceLine > 0) ? titleGap : 0f) + priceLine;
        return Math.max(22f*scale, content + padV*2f);
    }


    private static class InfoLayout {
        RectF outer = new RectF();
        ArrayList<RectF> cards = new ArrayList<>();
        ArrayList<InfoField> ordered = new ArrayList<>();
    }

    private void drawProductInfoPanelFromLayout(Canvas canvas, float w, float h, InfoLayout layout) {
        if (layout.cards.isEmpty()) return;

        float scale = Math.min(w, h) / 1000f;
        float radius = Math.max(0, infoOuterRadius) * scale;

        if (Color.alpha(infoOuterColor) > 0) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(infoOuterColor);
            canvas.drawRoundRect(layout.outer, radius, radius, p);
        }
        if (infoOuterBorderWidth > 0) {
            Paint b = new Paint(Paint.ANTI_ALIAS_FLAG);
            b.setStyle(Paint.Style.STROKE);
            b.setStrokeWidth(infoOuterBorderWidth * scale);
            b.setColor(infoOuterBorderColor);
            canvas.drawRoundRect(layout.outer, radius, radius, b);
        }

        for (int i = 0; i < layout.cards.size() && i < layout.ordered.size(); i++) {
            drawInfoCard(canvas, layout.ordered.get(i), layout.cards.get(i), w, h);
        }
    }

    private InfoLayout computeInfoLayout(float w, float h, RectF productRect) {
        InfoLayout out = new InfoLayout();
        ArrayList<InfoField> visible = new ArrayList<>();
        for (InfoField f : infoFields) if (f != null && f.visible) visible.add(f);
        if (visible.isEmpty()) return out;

        float scale = Math.min(w, h) / 1000f;
        float gap = Math.max(0f, infoGapPx) * scale;
        float pad = Math.max(0f, infoPaddingPx) * scale;
        float panelW = clamp(infoWidthPct, .12f, .95f) * w;
        float left = clamp(infoX, -.20f, 1f) * w;
        if (left + panelW > w) left = Math.max(0f, w - panelW - w*.012f);

        class Row {
            ArrayList<InfoField> fs = new ArrayList<>();
        }
        ArrayList<Row> rows = new ArrayList<>();

        if (infoLayoutMode == 0) {
            for (InfoField f : visible) { Row r = new Row(); r.fs.add(f); rows.add(r); }
        } else if (infoLayoutMode == 1) {
            Row r = new Row(); r.fs.addAll(visible); rows.add(r);
        } else {
            int cols = Math.max(1, Math.min(4, infoColumns));
            Row current = new Row();
            for (InfoField f : visible) {
                if (f.fullRow) {
                    if (!current.fs.isEmpty()) { rows.add(current); current = new Row(); }
                    Row full = new Row(); full.fs.add(f); rows.add(full);
                } else {
                    current.fs.add(f);
                    if (current.fs.size() >= cols) { rows.add(current); current = new Row(); }
                }
            }
            if (!current.fs.isEmpty()) rows.add(current);
        }

        ArrayList<Float> rowHeights = new ArrayList<>();
        float contentH = 0f;
        for (Row row : rows) {
            int n = Math.max(1, row.fs.size());
            float cardW = (panelW - 2f*pad - gap*Math.max(0,n-1)) / n;
            float rh = 24f*scale;
            for (InfoField f : row.fs) rh = Math.max(rh, measureInfoHeight(f, cardW, scale));
            rowHeights.add(rh);
            contentH += rh;
        }
        contentH += gap * Math.max(0, rows.size()-1);
        float panelH = Math.max(30f*scale, contentH + 2f*pad);

        float top;
        float distance = Math.max(0f, infoDistancePx) * scale;
        if (infoPositionMode == 1) {
            top = productRect.top - distance - panelH;
        } else if (infoPositionMode == 0) {
            top = productRect.bottom + distance;
        } else {
            top = clamp(infoY, 0f, 1f) * h;
        }
        top = Math.max(h*.01f, Math.min(top, h - panelH - h*.01f));
        out.outer.set(left, top, left+panelW, top+panelH);

        float y = top + pad;
        for (int ri=0; ri<rows.size(); ri++) {
            Row row = rows.get(ri);
            float rh = rowHeights.get(ri);
            int n = Math.max(1,row.fs.size());
            float cardW = (panelW - 2f*pad - gap*Math.max(0,n-1)) / n;
            float x = left + pad;
            for (InfoField f : row.fs) {
                out.cards.add(new RectF(x,y,x+cardW,y+rh));
                out.ordered.add(f);
                x += cardW + gap;
            }
            y += rh + gap;
        }
        return out;
    }

    private float measureInfoHeight(InfoField f, float cardW, float scale) {
        float padV = Math.max(0f, f.paddingVertical) * scale;
        String value = f.showValue ? safe(f.formattedValue()) : "";
        String title = f.showTitle ? safe(f.title) : "";
        float titleH = 0f, valueH = 0f;
        if (!title.isEmpty()) {
            Paint p = textPaint(f.titleColor, f.getTitleTypeface(), spToPx(f.titleSize, scale));
            titleH = p.descent()-p.ascent();
        }
        if (!value.isEmpty()) {
            Paint p = textPaint(f.valueColor, f.getValueTypeface(), spToPx(f.valueSize, scale));
            valueH = p.descent()-p.ascent();
        }
        // For compact information cards use one line when title+value fit; otherwise two lines.
        float padH = Math.max(0f, f.paddingHorizontal) * scale;
        if (titleH > 0 && valueH > 0) {
            Paint tp = textPaint(f.titleColor, f.getTitleTypeface(), spToPx(f.titleSize, scale));
            Paint vp = textPaint(f.valueColor, f.getValueTypeface(), spToPx(f.valueSize, scale));
            String one = title + ": " + value;
            float oneW = tp.measureText(title + ": ") + vp.measureText(value);
            if (oneW <= Math.max(1f, cardW-2f*padH)) return Math.max(titleH,valueH)+2f*padV;
        }
        float gap = (titleH>0 && valueH>0) ? Math.max(0f,f.titleValueGap)*scale : 0f;
        return Math.max(24f*scale, titleH+gap+valueH+2f*padV);
    }

    private void drawInfoCard(Canvas canvas, InfoField f, RectF r, float w, float h) {
        float scale = Math.min(w,h)/1000f;
        float radius = Math.max(0,f.cornerRadius)*scale;

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(f.backgroundColor);
        canvas.drawRoundRect(r,radius,radius,bg);

        if (f.borderWidth > 0) {
            Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(f.borderWidth*scale);
            border.setColor(f.borderColor);
            canvas.drawRoundRect(r,radius,radius,border);
        }

        String titleText = f.showTitle ? safe(f.title).trim() : "";
        String valueText = f.showValue ? safe(f.formattedValue()).trim() : "";
        if (titleText.isEmpty() && valueText.isEmpty()) return;

        float pad = Math.max(0f,f.paddingHorizontal)*scale;
        float maxW = Math.max(1f,r.width()-2f*pad);

        Paint title = textPaint(f.titleColor,f.getTitleTypeface(),spToPx(f.titleSize,scale));
        Paint value = textPaint(f.valueColor,f.getValueTypeface(),spToPx(f.valueSize,scale));
        title.setTextAlign(alignFor(f.textAlign));
        value.setTextAlign(alignFor(f.textAlign));
        float anchor = xForAlign(r,pad,f.textAlign);

        // Prefer a compact single line for "title: value".
        if (!titleText.isEmpty() && !valueText.isEmpty()) {
            String titlePrefix = titleText + ": ";
            float combined = title.measureText(titlePrefix)+value.measureText(valueText);
            if (combined <= maxW) {
                float base = r.centerY() - (Math.max(title.ascent(),value.ascent()) + Math.min(title.descent(),value.descent()))/2f;
                float totalW = combined;
                float start;
                if (f.textAlign == 1) start = r.centerX()-totalW/2f;
                else if (f.textAlign == 2) start = r.left+pad;
                else start = r.right-pad-totalW;
                title.setTextAlign(Paint.Align.LEFT);
                value.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(titlePrefix,start,base,title);
                canvas.drawText(valueText,start+title.measureText(titlePrefix),base,value);
                return;
            }
        }

        fitTextSize(title,titleText,maxW,7f*scale);
        fitTextSize(value,valueText,maxW,7f*scale);
        float titleH = titleText.isEmpty()?0f:title.descent()-title.ascent();
        float valueH = valueText.isEmpty()?0f:value.descent()-value.ascent();
        float gap = (titleH>0 && valueH>0)?Math.max(0,f.titleValueGap)*scale:0f;
        float total = titleH+gap+valueH;
        float y = r.centerY()-total/2f;
        if (titleH>0) {
            float base = y-title.ascent();
            canvas.drawText(titleText,anchor,base,title);
            y += titleH+gap;
        }
        if (valueH>0) {
            float base = y-value.ascent();
            canvas.drawText(valueText,anchor,base,value);
        }
    }

    private void drawBackground(Canvas c, float w, float h) {
        int alpha = clampInt(backgroundAlpha, 0, 255);
        if (backgroundMode == BG_GRADIENT) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            int c1 = withAlpha(gradientColor1, alpha);
            int c2 = withAlpha(gradientColor2, alpha);
            double a = Math.toRadians(gradientAngle);
            float dx = (float)Math.cos(a), dy = (float)Math.sin(a);
            float cx = w/2f, cy = h/2f, len = (float)Math.hypot(w,h)/2f;
            p.setShader(new LinearGradient(cx-dx*len, cy-dy*len, cx+dx*len, cy+dy*len,
                    c1, c2, Shader.TileMode.CLAMP));
            c.drawRect(0,0,w,h,p);
        } else if (backgroundMode == BG_PATTERN) {
            Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
            base.setColor(withAlpha(canvasBackground, alpha));
            c.drawRect(0,0,w,h,base);
            drawPattern(c,w,h,patternIndex,alpha);
        } else if (backgroundMode == BG_IMAGE && customBackgroundBitmap != null) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            p.setAlpha(alpha);
            Rect src = centerCropSource(customBackgroundBitmap, w/h);
            c.drawBitmap(customBackgroundBitmap, src, new RectF(0,0,w,h), p);
        } else {
            Paint p = new Paint(); p.setColor(withAlpha(canvasBackground, alpha));
            c.drawRect(0,0,w,h,p);
        }
    }

    private void drawPattern(Canvas c, float w, float h, int pattern, int alpha) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStrokeWidth(Math.max(1f,Math.min(w,h)*.003f));
        int idx = Math.floorMod(pattern, 11);
        if (idx == 7) { // luxury black
            p.setColor(withAlpha(0xFFB78D38, Math.min(alpha,90)));
            for (float x=-h; x<w+h; x+=Math.max(26f,w*.045f)) c.drawLine(x,0,x+h,h,p);
            return;
        }
        if (idx == 8 || idx == 9) { // paisley / iranian inspired geometry
            p.setStyle(Paint.Style.STROKE);
            p.setColor(withAlpha(idx==9?0xFF155C8A:0xFF8A5B2E, Math.min(alpha,80)));
            float s=Math.max(34f,w*.055f);
            for(float y=s/2;y<h;y+=s) for(float x=s/2;x<w;x+=s){
                c.drawCircle(x,y,s*.19f,p); c.drawRect(x-s*.12f,y-s*.12f,x+s*.12f,y+s*.12f,p);
            }
            return;
        }
        if (idx == 5) { // dots
            p.setStyle(Paint.Style.FILL); p.setColor(withAlpha(0xFF5A6470,Math.min(alpha,55)));
            float s=Math.max(22f,w*.03f); float r=Math.max(1.5f,s*.06f);
            for(float y=s/2;y<h;y+=s) for(float x=s/2;x<w;x+=s) c.drawCircle(x,y,r,p);
            return;
        }
        if (idx == 6) { // geometric
            p.setStyle(Paint.Style.STROKE); p.setColor(withAlpha(0xFF6B7280,Math.min(alpha,55)));
            float s=Math.max(32f,w*.045f);
            for(float y=0;y<h+s;y+=s) for(float x=0;x<w+s;x+=s){
                Path path=new Path(); path.moveTo(x,y-s*.45f); path.lineTo(x+s*.4f,y); path.lineTo(x,y+s*.45f); path.lineTo(x-s*.4f,y); path.close(); c.drawPath(path,p);
            }
            return;
        }
        if (idx == 10) { // white minimal
            p.setColor(withAlpha(0xFFB7C0CA,Math.min(alpha,45)));
            float s=Math.max(45f,w*.065f); for(float y=s;y<h;y+=s) c.drawLine(0,y,w,y,p); return;
        }
        if (idx == 3 || idx == 4) { // diagonal / fine lines
            p.setColor(withAlpha(idx==3?0xFF30343B:0xFF7D8793,Math.min(alpha,55)));
            float s=Math.max(idx==3?24f:14f,w*(idx==3?.035f:.018f));
            for(float x=-h;x<w+h;x+=s) c.drawLine(x,0,x+h,h,p); return;
        }
        // waves: blue, gold, purple
        int wave = idx==1 ? 0xFFB58B2A : idx==2 ? 0xFF7041A6 : 0xFF2779BD;
        p.setStyle(Paint.Style.STROKE); p.setColor(withAlpha(wave,Math.min(alpha,90)));
        float step=Math.max(46f,h*.09f);
        for(float y=step/2;y<h;y+=step){
            Path path=new Path(); path.moveTo(0,y);
            for(float x=0;x<w;x+=w/4f) path.cubicTo(x+w/8f,y-step*.28f,x+w/8f,y+step*.28f,x+w/4f,y);
            c.drawPath(path,p);
        }
    }

    private void drawProduct(Canvas c, Bitmap bmp, RectF dst) {
        if (bmp == null || dst.width() <= 0f || dst.height() <= 0f) return;
        Rect src = sourceCropRect(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        c.drawBitmap(bmp, src, dst, p);
    }

    private Rect sourceCropRect(Bitmap bmp) {
        int bw=bmp.getWidth(), bh=bmp.getHeight();
        if (!cropEnabled) return new Rect(0,0,bw,bh);
        int l=Math.max(0,Math.min(bw-1,Math.round(cropLeft*bw)));
        int t=Math.max(0,Math.min(bh-1,Math.round(cropTop*bh)));
        int r=Math.max(l+1,Math.min(bw,Math.round(cropRight*bw)));
        int b=Math.max(t+1,Math.min(bh,Math.round(cropBottom*bh)));
        return new Rect(l,t,r,b);
    }

    private void drawOuterTag(Canvas c, RectF r, float w, float h) {
        float scale=Math.min(w,h)/1000f;
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(outerTagColor); c.drawRoundRect(r,outerTagRadius*scale,outerTagRadius*scale,p);
        if(outerTagBorderWidth>0){ Paint b=new Paint(Paint.ANTI_ALIAS_FLAG); b.setStyle(Paint.Style.STROKE); b.setStrokeWidth(outerTagBorderWidth*scale); b.setColor(outerTagBorderColor); c.drawRoundRect(r,outerTagRadius*scale,outerTagRadius*scale,b); }
    }

    private void drawCard(Canvas canvas, LabelField f, RectF r, boolean isSelected, float w, float h) {
        float scale=Math.min(w,h)/1000f;
        float radius=f.cornerRadius*scale;
        Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG); bg.setColor(f.backgroundColor); canvas.drawRoundRect(r,radius,radius,bg);
        if(f.borderWidth>0 || isSelected){ Paint b=new Paint(Paint.ANTI_ALIAS_FLAG); b.setStyle(Paint.Style.STROKE); b.setStrokeWidth((isSelected?Math.max(3,f.borderWidth):f.borderWidth)*scale); b.setColor(isSelected?0xFF2196F3:f.borderColor); canvas.drawRoundRect(r,radius,radius,b); }

        float pad=Math.max(0f,f.paddingHorizontal)*scale;
        float gap=Math.max(0f,f.titlePriceGap)*scale;
        Paint title=textPaint(f.titleColor,f.getTitleTypeface(),spToPx(f.titleSize,scale)); title.setTextAlign(alignFor(f.textAlign));
        Paint price=textPaint(f.priceColor,f.getPriceTypeface(),spToPx(f.priceSize,scale)); price.setTextAlign(alignFor(f.textAlign));
        Paint toman=textPaint(f.tomanColor,f.getPriceTypeface(),spToPx(f.tomanSize,scale)); toman.setTextAlign(Paint.Align.LEFT);
        float anchor=xForAlign(r,pad,f.textAlign);
        float maxW=Math.max(1f,r.width()-pad*2f);
        fitTextSize(title,safe(f.name),maxW,7f*scale);

        String priceText=safe(f.value), tomanText=f.showToman && f.showPrice && !priceText.isEmpty()?"تومان":"";
        float priceLineW=price.measureText(priceText)+(tomanText.isEmpty()?0:toman.measureText(tomanText)+6f*scale);
        if(priceLineW>maxW && priceLineW>0){ float ratio=maxW/priceLineW; price.setTextSize(Math.max(8f*scale,price.getTextSize()*ratio)); toman.setTextSize(Math.max(7f*scale,toman.getTextSize()*ratio)); }

        float titleH=(f.showTitle&&!safe(f.name).isEmpty())?title.descent()-title.ascent():0;
        float priceH=(f.showPrice&&!priceText.isEmpty())?Math.max(price.descent()-price.ascent(),toman.descent()-toman.ascent()):0;
        float total=titleH+((titleH>0&&priceH>0)?gap:0)+priceH;
        float y=r.centerY()-total/2f;
        if(titleH>0){ float base=y-title.ascent(); canvas.drawText(safe(f.name),anchor,base,title); y+=titleH+(priceH>0?gap:0); }
        if(priceH>0){
            float base=y-price.ascent();
            if(tomanText.isEmpty()) { canvas.drawText(priceText,anchor,base,price); drawStrike(canvas,f,priceText,price,anchor,base,scale); }
            else drawPriceAndToman(canvas,f,priceText,tomanText,price,toman,r,anchor,base,maxW,scale);
        }
    }

    private void drawPriceAndToman(Canvas c, LabelField f, String priceText, String tomanText, Paint price, Paint toman, RectF r, float anchor, float base, float maxW, float scale){
        float pw=price.measureText(priceText), tw=toman.measureText(tomanText), spacing=6f*scale, total=pw+spacing+tw;
        float start;
        if(f.textAlign==1) start=r.centerX()-total/2f;
        else if(f.textAlign==2) start=r.left+Math.max(0,f.paddingHorizontal)*scale;
        else start=r.right-Math.max(0,f.paddingHorizontal)*scale-total;
        price.setTextAlign(Paint.Align.LEFT); toman.setTextAlign(Paint.Align.LEFT);
        c.drawText(priceText,start,base,price);
        float tomanBase=base-(price.getTextSize()-toman.getTextSize())*.10f;
        c.drawText(tomanText,start+pw+spacing,tomanBase,toman);
        if(f.strike){ Paint s=new Paint(Paint.ANTI_ALIAS_FLAG); s.setColor(f.priceColor); s.setStrokeWidth(Math.max(1f,price.getTextSize()*.05f)); float yy=base-price.getTextSize()*.34f; c.drawLine(start,yy,start+pw,yy,s); }
    }

    private void drawStrike(Canvas c, LabelField f, String text, Paint price, float anchor, float base, float scale){
        if(!f.strike) return; float tw=price.measureText(text), y=base-price.getTextSize()*.34f; Paint s=new Paint(Paint.ANTI_ALIAS_FLAG); s.setColor(f.priceColor); s.setStrokeWidth(Math.max(1f,price.getTextSize()*.05f));
        if(f.textAlign==0)c.drawLine(anchor-tw,y,anchor,y,s); else if(f.textAlign==1)c.drawLine(anchor-tw/2,y,anchor+tw/2,y,s); else c.drawLine(anchor,y,anchor+tw,y,s);
    }

    private void drawCropOverlay(Canvas c, RectF productRect, Bitmap source){
        if(source==null) return;
        // Crop handles are shown over the product bounds, normalized to the original source.
        float l=productRect.left+cropLeft*productRect.width(), t=productRect.top+cropTop*productRect.height();
        float r=productRect.left+cropRight*productRect.width(), b=productRect.top+cropBottom*productRect.height();
        RectF cr=new RectF(l,t,r,b);
        Paint shade=new Paint(); shade.setColor(0x66000000);
        c.drawRect(productRect.left,productRect.top,productRect.right,cr.top,shade); c.drawRect(productRect.left,cr.bottom,productRect.right,productRect.bottom,shade); c.drawRect(productRect.left,cr.top,cr.left,cr.bottom,shade); c.drawRect(cr.right,cr.top,productRect.right,cr.bottom,shade);
        Paint grid=new Paint(Paint.ANTI_ALIAS_FLAG); grid.setColor(Color.WHITE); grid.setStyle(Paint.Style.STROKE); grid.setStrokeWidth(Math.max(2f,Math.min(getWidth(),getHeight())*.002f)); c.drawRect(cr,grid);
        for(int i=1;i<3;i++){ float x=cr.left+cr.width()*i/3f, y=cr.top+cr.height()*i/3f; c.drawLine(x,cr.top,x,cr.bottom,grid); c.drawLine(cr.left,y,cr.right,y,grid); }
        Paint handle=new Paint(Paint.ANTI_ALIAS_FLAG); handle.setColor(0xFF1976D2); float rr=Math.max(10f,Math.min(getWidth(),getHeight())*.012f); c.drawCircle(cr.left,cr.top,rr,handle); c.drawCircle(cr.right,cr.top,rr,handle); c.drawCircle(cr.right,cr.bottom,rr,handle); c.drawCircle(cr.left,cr.bottom,rr,handle);
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(transformTarget != TARGET_NONE){
            scaleDetector.onTouchEvent(e);
            return onTransformTouch(e);
        }
        if(cropMode) return onCropTouch(e);
        if(previewCardRects.isEmpty()) return true;
        if(e.getAction()==MotionEvent.ACTION_DOWN){ touchDownX=lastX=e.getX(); touchDownY=lastY=e.getY(); moved=false; selected=-1; int vi=0; for(int i=0;i<fields.size();i++){ if(!fields.get(i).visible)continue; if(vi<previewCardRects.size()&&previewCardRects.get(vi).contains(e.getX(),e.getY())){selected=i;break;} vi++; } if(selected>=0&&listener!=null)listener.onFieldSelected(selected); invalidate(); return true; }
        if(e.getAction()==MotionEvent.ACTION_MOVE){ if(Math.abs(e.getX()-touchDownX)+Math.abs(e.getY()-touchDownY)>8)moved=true; lastX=e.getX();lastY=e.getY();return true; }
        if(e.getAction()==MotionEvent.ACTION_UP){ if(selected>=0&&!moved&&listener!=null){ RectF r=rectForFieldIndex(selected); if(r!=null)listener.onTextClicked(selected,e.getY()<r.centerY()?TEXT_TITLE:TEXT_PRICE); } if(listener!=null)listener.onChanged(); return true; }
        return true;
    }

    private RectF rectForFieldIndex(int index){ int vi=0; for(int i=0;i<fields.size();i++){ if(!fields.get(i).visible)continue; if(i==index&&vi<previewCardRects.size())return previewCardRects.get(vi);vi++; } return null; }

    private boolean onCropTouch(MotionEvent e){
        if(previewProductRect.width()<=0)return true;
        float x=e.getX(), y=e.getY();
        if(e.getAction()==MotionEvent.ACTION_DOWN){ cropHandle=nearestCropHandle(x,y); return true; }
        if(e.getAction()==MotionEvent.ACTION_MOVE&&cropHandle>=0){ float nx=clamp((x-previewProductRect.left)/previewProductRect.width(),0f,1f), ny=clamp((y-previewProductRect.top)/previewProductRect.height(),0f,1f); float min=.05f; if(cropHandle==0){cropLeft=Math.min(nx,cropRight-min);cropTop=Math.min(ny,cropBottom-min);} if(cropHandle==1){cropRight=Math.max(nx,cropLeft+min);cropTop=Math.min(ny,cropBottom-min);} if(cropHandle==2){cropRight=Math.max(nx,cropLeft+min);cropBottom=Math.max(ny,cropTop+min);} if(cropHandle==3){cropLeft=Math.min(nx,cropRight-min);cropBottom=Math.max(ny,cropTop+min);} cropEnabled=true; invalidate(); if(listener!=null)listener.onChanged(); return true; }
        if(e.getAction()==MotionEvent.ACTION_UP){cropHandle=-1;if(listener!=null)listener.onChanged();return true;} return true;
    }

    private int nearestCropHandle(float x,float y){ float[] xs={cropLeft,cropRight,cropRight,cropLeft}, ys={cropTop,cropTop,cropBottom,cropBottom}; float best=Float.MAX_VALUE;int bi=-1;float max=Math.max(45f,Math.min(getWidth(),getHeight())*.06f); for(int i=0;i<4;i++){float px=previewProductRect.left+xs[i]*previewProductRect.width(), py=previewProductRect.top+ys[i]*previewProductRect.height();float d=(float)Math.hypot(x-px,y-py);if(d<best&&d<=max){best=d;bi=i;}}return bi; }

    /**
     * Touch-transform handling for product / panel / info targets: one-finger drag moves the
     * target, two-finger pinch (handled by scaleDetector, see ScaleListener) resizes it. This
     * replaces the position/width/zoom SeekBars for a direct, on-canvas editing experience.
     */
    private boolean onTransformTouch(MotionEvent e){
        int action = e.getActionMasked();
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return true;

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            xLast = e.getX(); yLast = e.getY();
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE && e.getPointerCount() == 1 && !scaleDetector.isInProgress()) {
            float dx = e.getX() - xLast, dy = e.getY() - yLast;
            xLast = e.getX(); yLast = e.getY();
            if (transformTarget == TARGET_PRODUCT) {
                productX = clamp(productX + dx / w, -.9f, .98f);
                productY = clamp(productY + dy / h, -.9f, .98f);
            } else if (transformTarget == TARGET_PANEL) {
                labelX = clamp(labelX + dx / w, -.3f, 1f);
                labelY = clamp(labelY + dy / h, 0f, 1f);
            } else if (transformTarget == TARGET_INFO) {
                infoX = clamp(infoX + dx / w, -.2f, 1f);
                infoY = clamp(infoY + dy / h, 0f, 1f);
            }
            invalidate();
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_UP) {
            // Keep tracking the remaining finger so the drag doesn't jump after a pinch ends.
            int idx = e.getActionIndex() == 0 ? 1 : 0;
            if (idx < e.getPointerCount()) { xLast = e.getX(idx); yLast = e.getY(idx); }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (listener != null) listener.onChanged();
            return true;
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override public boolean onScale(ScaleGestureDetector d) {
            float f = d.getScaleFactor();
            if (transformTarget == TARGET_PRODUCT) {
                productZoom = clamp(productZoom * f, .2f, 3f);
            } else if (transformTarget == TARGET_PANEL) {
                labelWidthPct = clamp(labelWidthPct * f, .12f, .70f);
            } else if (transformTarget == TARGET_INFO) {
                infoWidthPct = clamp(infoWidthPct * f, .12f, .95f);
            }
            invalidate();
            return true;
        }
    }

    /** Dashed selection box + a center "move" knob over whichever target is active. */
    private void drawTransformOverlay(Canvas c){
        RectF r = transformTarget == TARGET_PRODUCT ? previewProductBoxRect
                : transformTarget == TARGET_PANEL ? previewLabelRect
                : previewInfoRect;
        if (r == null || r.width() <= 0 || r.height() <= 0) return;

        float unit = Math.min(getWidth(), getHeight());
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG); fill.setColor(0x2600BCD4);
        c.drawRoundRect(r, 20f, 20f, fill);

        Paint dash = new Paint(Paint.ANTI_ALIAS_FLAG);
        dash.setStyle(Paint.Style.STROKE);
        dash.setColor(0xFF00BCD4);
        dash.setStrokeWidth(Math.max(3f, unit * .004f));
        dash.setPathEffect(new DashPathEffect(new float[]{18f, 10f}, 0));
        c.drawRoundRect(r, 20f, 20f, dash);

        float rr = Math.max(14f, unit * .016f);
        Paint knob = new Paint(Paint.ANTI_ALIAS_FLAG); knob.setColor(0xFF00BCD4);
        c.drawCircle(r.centerX(), r.centerY(), rr, knob);
        Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG); icon.setColor(Color.WHITE); icon.setStrokeWidth(Math.max(2f, rr * .18f));
        c.drawLine(r.centerX() - rr * .5f, r.centerY(), r.centerX() + rr * .5f, r.centerY(), icon);
        c.drawLine(r.centerX(), r.centerY() - rr * .5f, r.centerX(), r.centerY() + rr * .5f, icon);
    }

    /**
     * TRUE WYSIWYG export.
     * The source photo dimensions never determine the output canvas anymore. The editor and
     * export use the same DESIGN_ASPECT, so all normalized positions, card sizes, fonts, gaps
     * and backgrounds keep the exact same visual proportions.
     */
    public Bitmap renderFinal(Bitmap source, int ignoredBackgroundColor, int ignoredBorderColor, boolean append, float widthPercent){
        if(source==null)return null;

        int outW = EXPORT_WIDTH;
        int outH = EXPORT_HEIGHT;

        // Legacy append mode is intentionally preserved. When enabled it extends the canonical
        // scene to the right; normal Designer output remains strict WYSIWYG.
        if(append){
            outW = Math.max(EXPORT_WIDTH + 1, Math.round(EXPORT_WIDTH * (1f + clamp(widthPercent,.12f,.70f))));
        }

        Bitmap out=Bitmap.createBitmap(outW,outH,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(out);
        renderScene(canvas,outW,outH,source,false);
        return out;
    }

    private int visibleCount(){ int c=0;for(LabelField f:fields)if(f.visible)c++;return Math.max(1,c); }
    private Paint textPaint(int color,Typeface typeface,float size){ Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);p.setColor(color);p.setTypeface(typeface);p.setTextSize(size);return p; }
    private Paint.Align alignFor(int a){return a==1?Paint.Align.CENTER:a==2?Paint.Align.LEFT:Paint.Align.RIGHT;}
    private float xForAlign(RectF r,float pad,int a){return a==1?r.centerX():a==2?r.left+pad:r.right-pad;}
    private void fitTextSize(Paint p,String text,float maxW,float minPx){if(text==null||text.isEmpty())return;float s=p.getTextSize();while(p.measureText(text)>maxW&&s>minPx){s-=1f;p.setTextSize(s);}}
    private float spToPx(float sp,float sceneScale){ return sp*getResources().getDisplayMetrics().scaledDensity*sceneScale; }
    private String safe(String s){return s==null?"":s;}
    private float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
    private int clampInt(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private int withAlpha(int color,int alpha){return (color&0x00FFFFFF)|(clampInt(alpha,0,255)<<24);}
    private Rect centerCropSource(Bitmap b,float targetAspect){ int bw=b.getWidth(),bh=b.getHeight();float a=(float)bw/bh;if(a>targetAspect){int nw=Math.round(bh*targetAspect),l=(bw-nw)/2;return new Rect(l,0,l+nw,bh);}int nh=Math.round(bw/targetAspect),t=(bh-nh)/2;return new Rect(0,t,bw,t+nh);}
}
