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
    }

    private ArrayList<LabelField> fields = new ArrayList<>();
    private Bitmap productBitmap;
    private int selected = -1;
    private float downX, downY;
    private final RectF labelRect = new RectF();
    private Listener listener;

    public boolean appendMode = false;
    public int labelBg = Color.WHITE;
    public int labelBorder = 0xFFD8D8D8;
    public float labelWidthPct = 0.44f;

    public LabelDesignerView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setFields(ArrayList<LabelField> fields) {
        this.fields = fields;
        invalidate();
    }

    public ArrayList<LabelField> getFields() {
        return fields;
    }

    public void setProductBitmap(Bitmap bitmap) {
        this.productBitmap = bitmap;
        invalidate();
    }

    public void select(int index) {
        selected = index;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(0xFFF0F0F0);

        float width = getWidth();
        float height = getHeight();

        // Product preview
        if (productBitmap != null) {
            Rect src = new Rect(0, 0, productBitmap.getWidth(), productBitmap.getHeight());
            RectF dst = new RectF(12, height * 0.08f, width * 0.56f, height * 0.92f);
            canvas.drawBitmap(productBitmap, src, dst, null);
        } else {
            Paint p = new Paint();
            p.setColor(Color.WHITE);
            canvas.drawRect(12, height * 0.08f, width * 0.56f, height * 0.92f, p);
        }

        // Wider label preview on the right
        labelRect.set(width * 0.59f, height * 0.08f, width - 12, height * 0.92f);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(labelBg);
        canvas.drawRoundRect(labelRect, 22, 22, bg);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(2.5f);
        border.setColor(labelBorder);
        canvas.drawRoundRect(labelRect, 22, 22, border);

        for (int i = 0; i < fields.size(); i++) {
            LabelField field = fields.get(i);
            RectF rect = fieldRect(field);

            // Selection / row outline
            Paint box = new Paint(Paint.ANTI_ALIAS_FLAG);
            box.setStyle(Paint.Style.STROKE);
            box.setStrokeWidth(i == selected ? 3f : 1.2f);
            box.setColor(i == selected ? 0xFF1976D2 : 0xFFE4E4E4);
            canvas.drawRoundRect(rect, 10, 10, box);

            drawField(canvas, field, rect, true);
        }
    }

    private void drawField(Canvas canvas, LabelField field, RectF rect, boolean preview) {
        float pad = Math.max(8f, rect.width() * 0.035f);
        float usableW = Math.max(20f, rect.width() - pad * 2f);
        float usableH = Math.max(20f, rect.height() - pad * 2f);

        // Title: smaller and always above price
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(field.titleColor);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setTextAlign(Paint.Align.RIGHT);

        float titleMax = usableH * 0.28f;
        float titleSize = Math.min(spToPx(Math.min(field.titleSize, 20)), titleMax);
        titlePaint.setTextSize(Math.max(12f, titleSize));
        fitTextSize(titlePaint, safe(field.name), usableW, 11f);

        float titleBaseline = rect.top + pad - titlePaint.ascent();
        canvas.drawText(safe(field.name), rect.right - pad, titleBaseline, titlePaint);

        // Empty values stay empty — no fake 123,456 placeholders
        String priceValue = safe(field.value).trim();
        if (priceValue.isEmpty()) {
            return;
        }

        Paint pricePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pricePaint.setColor(field.priceColor);
        pricePaint.setTypeface(Typeface.DEFAULT_BOLD);
        pricePaint.setTextAlign(Paint.Align.RIGHT);

        Paint unitPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        unitPaint.setColor(field.priceColor);
        unitPaint.setTypeface(Typeface.DEFAULT_BOLD);
        unitPaint.setTextAlign(Paint.Align.RIGHT);

        float remainingH = Math.max(18f, usableH * 0.55f);
        float priceSize = Math.min(spToPx(Math.min(field.priceSize, 30)), remainingH * 0.72f);
        pricePaint.setTextSize(Math.max(16f, priceSize));

        float unitSize = pricePaint.getTextSize() * 0.48f;
        unitPaint.setTextSize(Math.max(10f, unitSize));

        String unit = "تومان";
        float gap = Math.max(6f, rect.width() * 0.02f);

        // Fit number + unit into one line
        float unitW = unitPaint.measureText(unit);
        float numberMaxW = Math.max(30f, usableW - unitW - gap);
        fitTextSize(pricePaint, priceValue, numberMaxW, 14f);

        // Recalculate unit size if price was reduced
        unitPaint.setTextSize(Math.max(10f, pricePaint.getTextSize() * 0.48f));
        unitW = unitPaint.measureText(unit);

        float priceBaseline = rect.bottom - pad - pricePaint.descent();

        // Number on the right, unit immediately to its left.
        float rightX = rect.right - pad;
        canvas.drawText(priceValue, rightX, priceBaseline, pricePaint);

        float numberW = pricePaint.measureText(priceValue);
        float unitRightX = rightX - numberW - gap;
        canvas.drawText(unit, unitRightX, priceBaseline, unitPaint);

        if (field.strike) {
            Paint strikePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            strikePaint.setColor(field.priceColor);
            strikePaint.setStrokeWidth(Math.max(2f, pricePaint.getTextSize() * 0.055f));

            float y = priceBaseline - pricePaint.getTextSize() * 0.34f;
            float left = unitRightX - unitW;
            canvas.drawLine(left, y, rightX, y, strikePaint);
        }
    }

    private RectF fieldRect(LabelField field) {
        float lw = labelRect.width();
        float lh = labelRect.height();

        float left = labelRect.left + field.x * lw;
        float top = labelRect.top + field.y * lh;

        return new RectF(
                left,
                top,
                left + field.w * lw,
                top + field.h * lh
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            selected = -1;

            for (int i = fields.size() - 1; i >= 0; i--) {
                if (fieldRect(fields.get(i)).contains(event.getX(), event.getY())) {
                    selected = i;
                    break;
                }
            }

            downX = event.getX();
            downY = event.getY();

            if (listener != null && selected >= 0) {
                listener.onFieldSelected(selected);
            }

            invalidate();
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE && selected >= 0) {
            LabelField field = fields.get(selected);

            float dx = (event.getX() - downX) / labelRect.width();
            float dy = (event.getY() - downY) / labelRect.height();

            field.x = Math.max(0, Math.min(1 - field.w, field.x + dx));
            field.y = Math.max(0, Math.min(1 - field.h, field.y + dy));

            downX = event.getX();
            downY = event.getY();

            invalidate();
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (listener != null) {
                listener.onChanged();
            }
            return true;
        }

        return true;
    }

    public Bitmap renderFinal(
            Bitmap source,
            int backgroundColor,
            int borderColor,
            boolean append,
            float widthPercent
    ) {
        int sw = source.getWidth();
        int sh = source.getHeight();

        // Keep the label comfortably wide for Persian titles + long prices.
        float safeWidthPercent = Math.max(0.42f, Math.min(widthPercent, 0.52f));
        int lw = Math.max(320, (int) (sw * safeWidthPercent));
        int margin = Math.max(12, sw / 60);

        Bitmap output;
        RectF box;

        if (append) {
            output = Bitmap.createBitmap(sw + lw, sh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(output);
            c.drawColor(backgroundColor);
            c.drawBitmap(source, 0, 0, null);
            box = new RectF(sw, 0, sw + lw - 1, sh - 1);
        } else {
            output = source.copy(Bitmap.Config.ARGB_8888, true);
            box = new RectF(sw - lw - margin, margin, sw - margin, sh - margin);
        }

        Canvas canvas = new Canvas(output);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(backgroundColor);
        canvas.drawRoundRect(box, 24, 24, bg);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(Math.max(2f, sw / 500f));
        border.setColor(borderColor);
        canvas.drawRoundRect(box, 24, 24, border);

        for (LabelField field : fields) {
            RectF rect = new RectF(
                    box.left + field.x * box.width(),
                    box.top + field.y * box.height(),
                    box.left + (field.x + field.w) * box.width(),
                    box.top + (field.y + field.h) * box.height()
            );

            drawField(canvas, field, rect, false);
        }

        return output;
    }

    private void fitTextSize(Paint paint, String text, float maxWidth, float minPx) {
        if (text == null || text.isEmpty()) return;

        float size = paint.getTextSize();
        while (paint.measureText(text) > maxWidth && size > minPx) {
            size -= 1f;
            paint.setTextSize(size);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
