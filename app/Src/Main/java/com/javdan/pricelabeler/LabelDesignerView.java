package com.javdan.pricelabeler;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class LabelDesignerView extends View {

    public interface Listener {
        void onFieldSelected(int index);
        void onChanged();
        // 0 = title, 1 = price, 2 = toman
        void onTextClicked(int fieldIndex, int part);
    }

    private ArrayList<LabelField> fields = new ArrayList<>();
    private Bitmap productBitmap;
    private Listener listener;

    private int selected = -1;
    private float downX, downY;

    private final RectF labelRect = new RectF();

    // Designer / product state
    public int canvasBackground = 0xFFF2F2F2;
    public float productX = 0.02f;
    public float productY = 0.06f;
    public float productW = 0.47f;
    public float productH = 0.88f;

    // Crop state (0..1 inside source image)
    public float cropLeft = 0f;
    public float cropTop = 0f;
    public float cropRight = 1f;
    public float cropBottom = 1f;

    private boolean cropMode = false;

    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_FIELD_MOVE = 1;
    private static final int TOUCH_FIELD_RESIZE = 2;
    private static final int TOUCH_PRODUCT_MOVE = 3;
    private static final int TOUCH_PRODUCT_RESIZE = 4;
    private static final int TOUCH_CROP_LEFT = 5;
    private static final int TOUCH_CROP_TOP = 6;
    private static final int TOUCH_CROP_RIGHT = 7;
    private static final int TOUCH_CROP_BOTTOM = 8;

    private int touchMode = TOUCH_NONE;

    private float fieldHandleRadius;
    private float productHandleRadius;
    private float cropTolerance;

    public LabelDesignerView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        float d = getResources().getDisplayMetrics().density;
        fieldHandleRadius = 11f * d;
        productHandleRadius = 13f * d;
        cropTolerance = 18f * d;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setFields(ArrayList<LabelField> fields) {
        this.fields = fields == null ? new ArrayList<>() : fields;
        invalidate();
    }

    public ArrayList<LabelField> getFields() {
        return fields;
    }

    public void setProductBitmap(Bitmap bitmap) {
        productBitmap = bitmap;
        invalidate();
    }

    public void select(int index) {
        selected = index;
        invalidate();
    }

    public int getSelected() {
        return selected;
    }

    public void setCropMode(boolean enabled) {
        cropMode = enabled;
        touchMode = TOUCH_NONE;
        invalidate();
    }

    public boolean isCropMode() {
        return cropMode;
    }

    public void setCrop(float left, float top, float right, float bottom) {
        cropLeft = clamp(left, 0f, .95f);
        cropTop = clamp(top, 0f, .95f);
        cropRight = clamp(right, cropLeft + .05f, 1f);
        cropBottom = clamp(bottom, cropTop + .05f, 1f);
        invalidate();
    }

    public void resetCrop() {
        cropLeft = 0f;
        cropTop = 0f;
        cropRight = 1f;
        cropBottom = 1f;
        invalidate();
        notifyChanged();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(canvasBackground);

        // Larger label workspace than previous version.
        float w = getWidth();
        float h = getHeight();
        labelRect.set(w * 0.515f, h * 0.035f, w * 0.985f, h * 0.965f);

        RectF productRect = getProductRect();
        drawProduct(canvas, productRect);

        // subtle label work area
        Paint workspace = new Paint(Paint.ANTI_ALIAS_FLAG);
        workspace.setStyle(Paint.Style.STROKE);
        workspace.setStrokeWidth(dp(1));
        workspace.setColor(0xFFCFCFCF);
        canvas.drawRoundRect(labelRect, dp(16), dp(16), workspace);

        for (int i = 0; i < fields.size(); i++) {
            LabelField f = fields.get(i);
            if (!f.visible) continue;

            RectF r = fieldRect(f);
            drawFieldBackground(canvas, f, r, i == selected);
            drawField(canvas, f, r);

            if (i == selected) {
                drawFieldResizeHandle(canvas, r);
            }
        }

        // Product resize handle is always visible, including crop mode.
        if (productBitmap != null) {
            drawProductResizeHandle(canvas, productRect);
        }

        if (cropMode && productBitmap != null) {
            drawCropOverlay(canvas, productRect);
        }
    }

    private RectF getProductRect() {
        float w = getWidth();
        float h = getHeight();
        return new RectF(
                productX * w,
                productY * h,
                (productX + productW) * w,
                (productY + productH) * h
        );
    }

    private void drawProduct(Canvas canvas, RectF dst) {
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.WHITE);
        canvas.drawRoundRect(dst, dp(12), dp(12), bg);

        if (productBitmap == null) return;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(productBitmap, cropSourceRect(productBitmap), dst, p);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dp(1));
        border.setColor(0xFFCCCCCC);
        canvas.drawRoundRect(dst, dp(12), dp(12), border);
    }

    private Rect cropSourceRect(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int l = Math.round(cropLeft * w);
        int t = Math.round(cropTop * h);
        int r = Math.round(cropRight * w);
        int b = Math.round(cropBottom * h);

        l = Math.max(0, Math.min(l, w - 1));
        t = Math.max(0, Math.min(t, h - 1));
        r = Math.max(l + 1, Math.min(r, w));
        b = Math.max(t + 1, Math.min(b, h));

        return new Rect(l, t, r, b);
    }

    private void drawFieldBackground(Canvas canvas, LabelField f, RectF r, boolean selected) {
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(f.backgroundColor);
        canvas.drawRoundRect(r, dp(f.cornerRadius), dp(f.cornerRadius), bg);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(Math.max(dp(1), dp(f.borderWidth)));
        border.setColor(selected ? 0xFF1976D2 : f.borderColor);
        canvas.drawRoundRect(r, dp(f.cornerRadius), dp(f.cornerRadius), border);
    }

    private void drawField(Canvas canvas, LabelField f, RectF r) {
        float padX = dp(Math.max(4, f.paddingHorizontal));
        float padY = dp(Math.max(3, f.paddingVertical));
        float usableW = Math.max(dp(20), r.width() - 2 * padX);

        Paint.Align align = alignment(f.textAlign);
        float anchorX = align == Paint.Align.RIGHT ? r.right - padX :
                align == Paint.Align.CENTER ? r.centerX() : r.left + padX;

        float cursorTop = r.top + padY;

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(f.titleColor);
        titlePaint.setTypeface(f.getTitleTypeface());
        titlePaint.setTextAlign(align);
        titlePaint.setTextSize(spToPx(f.titleSize));
        fitTextSize(titlePaint, safe(f.name), usableW, spToPx(8));

        if (f.showTitle && !safe(f.name).trim().isEmpty()) {
            float base = cursorTop - titlePaint.ascent();
            canvas.drawText(safe(f.name), anchorX, base, titlePaint);
            cursorTop = base + titlePaint.descent() + dp(f.titlePriceGap);
        }

        String value = safe(f.value).trim();
        if (!f.showPrice || value.isEmpty()) return;

        Paint pricePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pricePaint.setColor(f.priceColor);
        pricePaint.setTypeface(f.getPriceTypeface());
        pricePaint.setTextAlign(align);
        pricePaint.setTextSize(spToPx(f.priceSize));

        Paint tomanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tomanPaint.setColor(f.tomanColor);
        tomanPaint.setTypeface(f.getPriceTypeface());
        tomanPaint.setTextSize(spToPx(f.tomanSize));

        float base = cursorTop - pricePaint.ascent();

        if (align == Paint.Align.RIGHT) {
            // Price on right, toman immediately to its left.
            pricePaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(value, anchorX, base, pricePaint);

            float numberW = pricePaint.measureText(value);
            float gap = dp(5);

            if (f.showToman) {
                tomanPaint.setTextAlign(Paint.Align.RIGHT);
                float unitRight = anchorX - numberW - gap;
                canvas.drawText("تومان", unitRight, base, tomanPaint);

                if (f.strike) {
                    float unitW = tomanPaint.measureText("تومان");
                    drawStrike(canvas, f.priceColor, unitRight - unitW, anchorX, base, pricePaint);
                }
            } else if (f.strike) {
                drawStrike(canvas, f.priceColor, anchorX - numberW, anchorX, base, pricePaint);
            }
        } else {
            String text = f.showToman ? value + " تومان" : value;
            fitTextSize(pricePaint, text, usableW, spToPx(10));
            canvas.drawText(text, anchorX, base, pricePaint);

            if (f.strike) {
                float tw = pricePaint.measureText(text);
                float left = align == Paint.Align.CENTER ? anchorX - tw / 2f : anchorX;
                float right = align == Paint.Align.CENTER ? anchorX + tw / 2f : anchorX + tw;
                drawStrike(canvas, f.priceColor, left, right, base, pricePaint);
            }
        }
    }

    private void drawStrike(Canvas canvas, int color, float left, float right, float base, Paint pricePaint) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStrokeWidth(Math.max(dp(1.5f), pricePaint.getTextSize() * .05f));
        float y = base - pricePaint.getTextSize() * .32f;
        canvas.drawLine(left, y, right, y, p);
    }

    private RectF fieldRect(LabelField f) {
        float lw = labelRect.width();
        float lh = labelRect.height();

        float left = labelRect.left + f.x * lw;
        float top = labelRect.top + f.y * lh;

        return new RectF(
                left,
                top,
                left + f.w * lw,
                top + f.h * lh
        );
    }

    private void drawFieldResizeHandle(Canvas canvas, RectF r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF1976D2);
        canvas.drawCircle(r.right, r.bottom, fieldHandleRadius, p);

        p.setColor(Color.WHITE);
        canvas.drawCircle(r.right, r.bottom, fieldHandleRadius * .40f, p);
    }

    private void drawProductResizeHandle(Canvas canvas, RectF r) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF5E35B1);
        canvas.drawCircle(r.right, r.bottom, productHandleRadius, p);

        p.setColor(Color.WHITE);
        p.setStrokeWidth(dp(2));
        p.setStyle(Paint.Style.STROKE);
        float a = productHandleRadius * .45f;
        canvas.drawLine(r.right - a, r.bottom, r.right + a, r.bottom, p);
        canvas.drawLine(r.right, r.bottom - a, r.right, r.bottom + a, p);
    }

    private RectF cropScreenRect(RectF productRect) {
        return new RectF(
                productRect.left + cropLeft * productRect.width(),
                productRect.top + cropTop * productRect.height(),
                productRect.left + cropRight * productRect.width(),
                productRect.top + cropBottom * productRect.height()
        );
    }

    private void drawCropOverlay(Canvas canvas, RectF productRect) {
        RectF cr = cropScreenRect(productRect);

        Paint dim = new Paint();
        dim.setColor(0x88000000);
        canvas.drawRect(productRect.left, productRect.top, productRect.right, cr.top, dim);
        canvas.drawRect(productRect.left, cr.bottom, productRect.right, productRect.bottom, dim);
        canvas.drawRect(productRect.left, cr.top, cr.left, cr.bottom, dim);
        canvas.drawRect(cr.right, cr.top, productRect.right, cr.bottom, dim);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dp(2));
        border.setColor(Color.WHITE);
        canvas.drawRect(cr, border);

        Paint hp = new Paint(Paint.ANTI_ALIAS_FLAG);
        hp.setColor(0xFF00A3FF);
        float rr = dp(8);
        canvas.drawCircle(cr.left, cr.centerY(), rr, hp);
        canvas.drawCircle(cr.right, cr.centerY(), rr, hp);
        canvas.drawCircle(cr.centerX(), cr.top, rr, hp);
        canvas.drawCircle(cr.centerX(), cr.bottom, rr, hp);
    }

    private int textHitPart(LabelField f, RectF r, float x, float y) {
        float padY = dp(Math.max(3, f.paddingVertical));
        float titleTop = r.top + padY;
        float titleH = spToPx(f.titleSize) * 1.35f;

        if (f.showTitle && y >= titleTop && y <= titleTop + titleH) {
            return 0;
        }

        float priceTop = titleTop + (f.showTitle ? titleH + dp(f.titlePriceGap) : 0);
        float priceH = spToPx(f.priceSize) * 1.35f;

        if (f.showPrice && y >= priceTop && y <= priceTop + priceH) {
            if (f.showToman && f.textAlign == 0) {
                // Approximate the left 35% of price row as the Toman hit area.
                float split = r.right - r.width() * .42f;
                if (x < split) return 2;
            }
            return 1;
        }

        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        RectF productRect = getProductRect();

        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            touchMode = TOUCH_NONE;
            downX = x;
            downY = y;

            // Product resize takes priority even while cropping.
            if (productBitmap != null && distance(x, y, productRect.right, productRect.bottom) <= productHandleRadius * 1.8f) {
                touchMode = TOUCH_PRODUCT_RESIZE;
                return true;
            }

            if (cropMode && productBitmap != null) {
                RectF cr = cropScreenRect(productRect);

                if (Math.abs(x - cr.left) <= cropTolerance && y >= cr.top - cropTolerance && y <= cr.bottom + cropTolerance) {
                    touchMode = TOUCH_CROP_LEFT;
                    return true;
                }
                if (Math.abs(y - cr.top) <= cropTolerance && x >= cr.left - cropTolerance && x <= cr.right + cropTolerance) {
                    touchMode = TOUCH_CROP_TOP;
                    return true;
                }
                if (Math.abs(x - cr.right) <= cropTolerance && y >= cr.top - cropTolerance && y <= cr.bottom + cropTolerance) {
                    touchMode = TOUCH_CROP_RIGHT;
                    return true;
                }
                if (Math.abs(y - cr.bottom) <= cropTolerance && x >= cr.left - cropTolerance && x <= cr.right + cropTolerance) {
                    touchMode = TOUCH_CROP_BOTTOM;
                    return true;
                }

                // Inside product but not on crop edge => move the product.
                if (productRect.contains(x, y)) {
                    touchMode = TOUCH_PRODUCT_MOVE;
                    return true;
                }
            }

            // Product move in normal mode.
            if (productBitmap != null && productRect.contains(x, y)) {
                touchMode = TOUCH_PRODUCT_MOVE;
                return true;
            }

            // Label fields.
            for (int i = fields.size() - 1; i >= 0; i--) {
                LabelField f = fields.get(i);
                if (!f.visible) continue;
                RectF r = fieldRect(f);

                if (distance(x, y, r.right, r.bottom) <= fieldHandleRadius * 1.8f) {
                    selected = i;
                    touchMode = TOUCH_FIELD_RESIZE;
                    if (listener != null) listener.onFieldSelected(i);
                    invalidate();
                    return true;
                }

                if (r.contains(x, y)) {
                    selected = i;
                    if (listener != null) listener.onFieldSelected(i);

                    int part = textHitPart(f, r, x, y);
                    if (part >= 0 && listener != null) {
                        listener.onTextClicked(i, part);
                        invalidate();
                        return true;
                    }

                    touchMode = TOUCH_FIELD_MOVE;
                    invalidate();
                    return true;
                }
            }

            return true;
        }

        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            float dxNorm = (x - downX) / Math.max(1f, getWidth());
            float dyNorm = (y - downY) / Math.max(1f, getHeight());

            if (touchMode == TOUCH_PRODUCT_MOVE) {
                productX = clamp(productX + dxNorm, 0f, 1f - productW);
                productY = clamp(productY + dyNorm, 0f, 1f - productH);
            } else if (touchMode == TOUCH_PRODUCT_RESIZE) {
                productW = clamp(productW + dxNorm, .12f, 1f - productX);
                productH = clamp(productH + dyNorm, .12f, 1f - productY);
            } else if (touchMode == TOUCH_FIELD_MOVE && selected >= 0) {
                LabelField f = fields.get(selected);
                float dx = (x - downX) / Math.max(1f, labelRect.width());
                float dy = (y - downY) / Math.max(1f, labelRect.height());
                f.x = clamp(f.x + dx, 0f, 1f - f.w);
                f.y = clamp(f.y + dy, 0f, 1f - f.h);
            } else if (touchMode == TOUCH_FIELD_RESIZE && selected >= 0) {
                LabelField f = fields.get(selected);
                float dx = (x - downX) / Math.max(1f, labelRect.width());
                float dy = (y - downY) / Math.max(1f, labelRect.height());
                f.w = clamp(f.w + dx, .10f, 1f - f.x);
                f.h = clamp(f.h + dy, .06f, 1f - f.y);
            } else if (touchMode >= TOUCH_CROP_LEFT && touchMode <= TOUCH_CROP_BOTTOM) {
                float px = (x - productRect.left) / Math.max(1f, productRect.width());
                float py = (y - productRect.top) / Math.max(1f, productRect.height());

                if (touchMode == TOUCH_CROP_LEFT) {
                    cropLeft = clamp(px, 0f, cropRight - .05f);
                } else if (touchMode == TOUCH_CROP_TOP) {
                    cropTop = clamp(py, 0f, cropBottom - .05f);
                } else if (touchMode == TOUCH_CROP_RIGHT) {
                    cropRight = clamp(px, cropLeft + .05f, 1f);
                } else if (touchMode == TOUCH_CROP_BOTTOM) {
                    cropBottom = clamp(py, cropTop + .05f, 1f);
                }
            }

            downX = x;
            downY = y;
            invalidate();
            return true;
        }

        if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
            touchMode = TOUCH_NONE;
            notifyChanged();
            return true;
        }

        return true;
    }

    public Bitmap renderFinal(Bitmap source, int backgroundColor, int borderColor, boolean append, float widthPercent) {
        Bitmap cropped = makeCroppedBitmap(source);

        int sw = cropped.getWidth();
        int sh = cropped.getHeight();
        float safeWidth = clamp(widthPercent, .30f, .70f);
        int lw = Math.max(300, Math.round(sw * safeWidth));
        int margin = Math.max(10, sw / 70);

        Bitmap output;
        RectF box;

        if (append) {
            output = Bitmap.createBitmap(sw + lw, sh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(output);
            c.drawColor(backgroundColor);
            c.drawBitmap(cropped, 0, 0, null);
            box = new RectF(sw + margin, margin, sw + lw - margin, sh - margin);
        } else {
            output = cropped.copy(Bitmap.Config.ARGB_8888, true);
            box = new RectF(sw - lw - margin, margin, sw - margin, sh - margin);
        }

        Canvas canvas = new Canvas(output);

        RectF old = new RectF(labelRect);
        labelRect.set(box);

        for (LabelField f : fields) {
            if (!f.visible) continue;
            RectF r = fieldRect(f);
            drawFieldBackground(canvas, f, r, false);
            drawField(canvas, f, r);
        }

        labelRect.set(old);

        if (cropped != source && !cropped.isRecycled()) cropped.recycle();
        return output;
    }

    private Bitmap makeCroppedBitmap(Bitmap source) {
        Rect src = cropSourceRect(source);
        if (src.left == 0 && src.top == 0 && src.right == source.getWidth() && src.bottom == source.getHeight()) {
            return source;
        }
        return Bitmap.createBitmap(source, src.left, src.top, src.width(), src.height());
    }

    private void fitTextSize(Paint paint, String text, float maxWidth, float minPx) {
        if (text == null || text.isEmpty()) return;
        float s = paint.getTextSize();
        while (paint.measureText(text) > maxWidth && s > minPx) {
            s -= 1f;
            paint.setTextSize(s);
        }
    }

    private Paint.Align alignment(int a) {
        if (a == 1) return Paint.Align.CENTER;
        if (a == 2) return Paint.Align.LEFT;
        return Paint.Align.RIGHT;
    }

    private void notifyChanged() {
        if (listener != null) listener.onChanged();
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
