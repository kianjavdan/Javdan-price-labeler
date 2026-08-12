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

    private float downX;
    private float downY;

    private final RectF workingRect = new RectF();
    private final RectF productPreviewRect = new RectF();

    private Listener listener;

    public boolean appendMode = true;
    public int canvasBackground = 0xFFF2F2F2;
    public float labelWidthPct = 0.38f;

    // تصویر محصول
    public float productX = 0.02f;
    public float productY = 0.05f;
    public float productW = 0.58f;
    public float productH = 0.90f;

    private static final int MODE_NONE = 0;
    private static final int MODE_FIELD_DRAG = 1;
    private static final int MODE_FIELD_RESIZE = 2;
    private static final int MODE_PRODUCT_DRAG = 3;
    private static final int MODE_PRODUCT_RESIZE = 4;

    private int touchMode = MODE_NONE;

    private static final float MIN_FIELD_W = 0.16f;
    private static final float MIN_FIELD_H = 0.08f;

    private static final float MIN_PRODUCT_W = 0.15f;
    private static final float MIN_PRODUCT_H = 0.15f;

    private float handleRadiusPx = 18f;


    public LabelDesignerView(Context context) {
        super(context);

        setLayerType(
                View.LAYER_TYPE_SOFTWARE,
                null
        );

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        handleRadiusPx =
                14f * density;
    }


    public void setListener(Listener listener) {
        this.listener = listener;
    }


    public void setFields(ArrayList<LabelField> fields) {

        this.fields =
                fields == null
                        ? new ArrayList<>()
                        : fields;

        invalidate();
    }


    public ArrayList<LabelField> getFields() {
        return fields;
    }


    public void setProductBitmap(Bitmap bitmap) {

        this.productBitmap =
                bitmap;

        invalidate();
    }


    public void select(int index) {

        selected =
                index;

        invalidate();
    }


    public int getSelectedIndex() {
        return selected;
    }


    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        canvas.drawColor(
                canvasBackground
        );

        float width =
                getWidth();

        float height =
                getHeight();


        // محدوده محصول
        productPreviewRect.set(
                width * productX,
                height * productY,
                width * (productX + productW),
                height * (productY + productH)
        );


        drawProductPreview(
                canvas,
                productPreviewRect
        );


        // محدوده طراحی کادرهای قیمت
        workingRect.set(
                width * 0.61f,
                height * 0.06f,
                width * 0.98f,
                height * 0.94f
        );


        for (
                int i = 0;
                i < fields.size();
                i++
        ) {

            LabelField field =
                    fields.get(i);

            if (!field.visible) {
                continue;
            }


            RectF rect =
                    fieldRect(field);


            drawField(
                    canvas,
                    field,
                    rect
            );


            if (
                    i == selected
                            && touchMode != MODE_PRODUCT_DRAG
                            && touchMode != MODE_PRODUCT_RESIZE
            ) {

                drawFieldSelection(
                        canvas,
                        rect
                );
            }
        }


        // کادر انتخاب تصویر محصول
        drawProductSelection(
                canvas,
                productPreviewRect
        );
    }


    private void drawProductPreview(
            Canvas canvas,
            RectF area
    ) {

        Paint base =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        base.setColor(
                Color.WHITE
        );

        canvas.drawRoundRect(
                area,
                18,
                18,
                base
        );


        if (
                productBitmap == null
                        || productBitmap.isRecycled()
        ) {
            return;
        }


        drawBitmapFitCenter(
                canvas,
                productBitmap,
                area
        );
    }


    private void drawBitmapFitCenter(
            Canvas canvas,
            Bitmap bitmap,
            RectF target
    ) {

        if (
                bitmap == null
                        || bitmap.isRecycled()
        ) {
            return;
        }


        float srcW =
                bitmap.getWidth();

        float srcH =
                bitmap.getHeight();


        if (
                srcW <= 0
                        || srcH <= 0
        ) {
            return;
        }


        float scale =
                Math.min(
                        target.width() / srcW,
                        target.height() / srcH
                );


        float dw =
                srcW * scale;

        float dh =
                srcH * scale;


        float left =
                target.centerX()
                        - dw / 2f;

        float top =
                target.centerY()
                        - dh / 2f;


        RectF dst =
                new RectF(
                        left,
                        top,
                        left + dw,
                        top + dh
                );


        Paint p =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                                | Paint.FILTER_BITMAP_FLAG
                );


        canvas.drawBitmap(
                bitmap,
                null,
                dst,
                p
        );
    }


    private void drawProductSelection(
            Canvas canvas,
            RectF rect
    ) {

        if (
                productBitmap == null
        ) {
            return;
        }


        Paint selection =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        selection.setStyle(
                Paint.Style.STROKE
        );

        selection.setStrokeWidth(
                3f
        );

        selection.setColor(
                0xFF00897B
        );


        canvas.drawRoundRect(
                rect,
                18,
                18,
                selection
        );


        Paint handle =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        handle.setStyle(
                Paint.Style.FILL
        );

        handle.setColor(
                0xFF00897B
        );


        canvas.drawCircle(
                rect.right,
                rect.bottom,
                handleRadiusPx,
                handle
        );


        Paint inner =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        inner.setStyle(
                Paint.Style.STROKE
        );

        inner.setStrokeWidth(
                3f
        );

        inner.setColor(
                Color.WHITE
        );


        canvas.drawCircle(
                rect.right,
                rect.bottom,
                handleRadiusPx * 0.60f,
                inner
        );
    }


    private void drawFieldSelection(
            Canvas canvas,
            RectF rect
    ) {

        Paint selection =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        selection.setStyle(
                Paint.Style.STROKE
        );

        selection.setStrokeWidth(
                4f
        );

        selection.setColor(
                0xFF1976D2
        );


        canvas.drawRoundRect(
                rect,
                14,
                14,
                selection
        );


        Paint handleFill =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        handleFill.setStyle(
                Paint.Style.FILL
        );

        handleFill.setColor(
                0xFF1976D2
        );


        canvas.drawCircle(
                rect.right,
                rect.bottom,
                handleRadiusPx,
                handleFill
        );


        Paint handleBorder =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        handleBorder.setStyle(
                Paint.Style.STROKE
        );

        handleBorder.setStrokeWidth(
                3f
        );

        handleBorder.setColor(
                Color.WHITE
        );


        canvas.drawCircle(
                rect.right,
                rect.bottom,
                handleRadiusPx * 0.62f,
                handleBorder
        );
    }


    private void drawField(
            Canvas canvas,
            LabelField field,
            RectF rect
    ) {

        if (!field.visible) {
            return;
        }


        float radius =
                Math.max(
                        0,
                        field.cornerRadius
                );


        Paint bg =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        bg.setStyle(
                Paint.Style.FILL
        );

        bg.setColor(
                field.backgroundColor
        );


        canvas.drawRoundRect(
                rect,
                radius,
                radius,
                bg
        );


        if (
                field.borderWidth > 0
        ) {

            Paint border =
                    new Paint(
                            Paint.ANTI_ALIAS_FLAG
                    );

            border.setStyle(
                    Paint.Style.STROKE
            );

            border.setStrokeWidth(
                    field.borderWidth
            );

            border.setColor(
                    field.borderColor
            );


            canvas.drawRoundRect(
                    rect,
                    radius,
                    radius,
                    border
            );
        }


        float horizontalPadding =
                Math.max(
                        5f,
                        field.paddingHorizontal
                );

        float verticalPadding =
                Math.max(
                        4f,
                        field.paddingVertical
                );


        float left =
                rect.left
                        + horizontalPadding;

        float right =
                rect.right
                        - horizontalPadding;

        float top =
                rect.top
                        + verticalPadding;

        float bottom =
                rect.bottom
                        - verticalPadding;


        float usableW =
                Math.max(
                        20f,
                        right - left
                );


        Paint.Align align =
                getPaintAlign(
                        field.textAlign
                );


        float anchorX =
                getAnchorX(
                        field.textAlign,
                        left,
                        right
                );


        Paint titlePaint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        titlePaint.setColor(
                field.titleColor
        );

        titlePaint.setTypeface(
                field.getTitleTypeface()
        );

        titlePaint.setTextAlign(
                align
        );

        titlePaint.setTextSize(
                spToPx(
                        Math.max(
                                8,
                                field.titleSize
                        )
                )
        );


        String title =
                safe(
                        field.name
                ).trim();


        String priceValue =
                safe(
                        field.value
                ).trim();


        fitTextSize(
                titlePaint,
                title,
                usableW,
                10f
        );


        float cursorY =
                top;


        if (
                field.showTitle
                        && !title.isEmpty()
        ) {

            float titleBaseline =
                    cursorY
                            - titlePaint.ascent();


            canvas.drawText(
                    title,
                    anchorX,
                    titleBaseline,
                    titlePaint
            );


            cursorY =
                    titleBaseline
                            + titlePaint.descent()
                            + Math.max(
                            0,
                            field.titlePriceGap
                    );
        }


        if (
                !field.showPrice
                        || priceValue.isEmpty()
        ) {
            return;
        }


        Paint pricePaint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        pricePaint.setColor(
                field.priceColor
        );

        pricePaint.setTypeface(
                field.getPriceTypeface()
        );

        pricePaint.setTextAlign(
                align
        );

        pricePaint.setTextSize(
                spToPx(
                        Math.max(
                                10,
                                field.priceSize
                        )
                )
        );


        Paint unitPaint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        unitPaint.setColor(
                field.tomanColor
        );

        unitPaint.setTypeface(
                field.getPriceTypeface()
        );

        unitPaint.setTextSize(
                spToPx(
                        Math.max(
                                8,
                                field.tomanSize
                        )
                )
        );


        String unit =
                field.showToman
                        ? "تومان"
                        : "";


        float gap =
                field.showToman
                        ? Math.max(
                        4f,
                        pricePaint.getTextSize()
                                * 0.12f
                )
                        : 0f;


        if (
                field.textAlign == 0
        ) {

            pricePaint.setTextAlign(
                    Paint.Align.RIGHT
            );

            unitPaint.setTextAlign(
                    Paint.Align.RIGHT
            );


            float unitW =
                    unitPaint.measureText(
                            unit
                    );


            float maxNumberW =
                    usableW
                            - unitW
                            - gap;


            fitTextSize(
                    pricePaint,
                    priceValue,
                    Math.max(
                            30f,
                            maxNumberW
                    ),
                    14f
            );


            float priceBaseline =
                    cursorY
                            - pricePaint.ascent();


            if (
                    priceBaseline
                            + pricePaint.descent()
                            > bottom
            ) {

                priceBaseline =
                        bottom
                                - pricePaint.descent();
            }


            canvas.drawText(
                    priceValue,
                    right,
                    priceBaseline,
                    pricePaint
            );


            float numberW =
                    pricePaint.measureText(
                            priceValue
                    );


            float unitRight =
                    right
                            - numberW
                            - gap;


            if (
                    field.showToman
            ) {

                canvas.drawText(
                        unit,
                        unitRight,
                        priceBaseline,
                        unitPaint
                );
            }


            if (
                    field.strike
            ) {

                float strikeLeft =
                        field.showToman
                                ? unitRight
                                - unitW
                                : right
                                - numberW;


                drawStrike(
                        canvas,
                        field,
                        strikeLeft,
                        right,
                        priceBaseline,
                        pricePaint
                );
            }


        } else {

            String fullText =
                    field.showToman
                            ? priceValue
                            + "  "
                            + unit
                            : priceValue;


            fitTextSize(
                    pricePaint,
                    fullText,
                    usableW,
                    14f
            );


            float priceBaseline =
                    cursorY
                            - pricePaint.ascent();


            if (
                    priceBaseline
                            + pricePaint.descent()
                            > bottom
            ) {

                priceBaseline =
                        bottom
                                - pricePaint.descent();
            }


            canvas.drawText(
                    fullText,
                    anchorX,
                    priceBaseline,
                    pricePaint
            );


            if (
                    field.strike
            ) {

                float textW =
                        pricePaint.measureText(
                                fullText
                        );


                float strikeLeft;
                float strikeRight;


                if (
                        field.textAlign == 1
                ) {

                    strikeLeft =
                            anchorX
                                    - textW / 2f;

                    strikeRight =
                            anchorX
                                    + textW / 2f;

                } else {

                    strikeLeft =
                            anchorX;

                    strikeRight =
                            anchorX
                                    + textW;
                }


                drawStrike(
                        canvas,
                        field,
                        strikeLeft,
                        strikeRight,
                        priceBaseline,
                        pricePaint
                );
            }
        }
    }


    private void drawStrike(
            Canvas canvas,
            LabelField field,
            float left,
            float right,
            float baseline,
            Paint pricePaint
    ) {

        Paint strikePaint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        strikePaint.setColor(
                field.priceColor
        );

        strikePaint.setStrokeWidth(
                Math.max(
                        2f,
                        pricePaint.getTextSize()
                                * 0.055f
                )
        );


        float y =
                baseline
                        - pricePaint.getTextSize()
                        * 0.34f;


        canvas.drawLine(
                left,
                y,
                right,
                y,
                strikePaint
        );
    }


    private Paint.Align getPaintAlign(
            int align
    ) {

        if (align == 1) {
            return Paint.Align.CENTER;
        }

        if (align == 2) {
            return Paint.Align.LEFT;
        }

        return Paint.Align.RIGHT;
    }


    private float getAnchorX(
            int align,
            float left,
            float right
    ) {

        if (align == 1) {
            return (left + right) / 2f;
        }

        if (align == 2) {
            return left;
        }

        return right;
    }


    private RectF fieldRect(
            LabelField field
    ) {

        float lw =
                workingRect.width();

        float lh =
                workingRect.height();


        float left =
                workingRect.left
                        + field.x * lw;

        float top =
                workingRect.top
                        + field.y * lh;


        return new RectF(
                left,
                top,
                left + field.w * lw,
                top + field.h * lh
        );
    }


    private boolean pointInHandle(
            float handleX,
            float handleY,
            float touchX,
            float touchY
    ) {

        float dx =
                touchX - handleX;

        float dy =
                touchY - handleY;


        float hitRadius =
                handleRadiusPx
                        * 1.9f;


        return (
                dx * dx
                        + dy * dy
        )
                <= hitRadius
                * hitRadius;
    }


    @Override
    public boolean onTouchEvent(
            MotionEvent event
    ) {

        float touchX =
                event.getX();

        float touchY =
                event.getY();


        switch (
                event.getActionMasked()
        ) {

            case MotionEvent.ACTION_DOWN: {

                touchMode =
                        MODE_NONE;


                /*
                 * اول بررسی Resize تصویر
                 */
                if (
                        productBitmap != null
                                && pointInHandle(
                                productPreviewRect.right,
                                productPreviewRect.bottom,
                                touchX,
                                touchY
                        )
                ) {

                    touchMode =
                            MODE_PRODUCT_RESIZE;

                    selected =
                            -1;

                    downX =
                            touchX;

                    downY =
                            touchY;

                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    true
                            );

                    invalidate();

                    return true;
                }


                /*
                 * بعد بررسی Resize کادر انتخاب‌شده
                 */
                if (
                        selected >= 0
                                && selected < fields.size()
                ) {

                    LabelField selectedField =
                            fields.get(selected);

                    RectF selectedRect =
                            fieldRect(selectedField);


                    if (
                            pointInHandle(
                                    selectedRect.right,
                                    selectedRect.bottom,
                                    touchX,
                                    touchY
                            )
                    ) {

                        touchMode =
                                MODE_FIELD_RESIZE;

                        downX =
                                touchX;

                        downY =
                                touchY;

                        getParent()
                                .requestDisallowInterceptTouchEvent(
                                        true
                                );

                        return true;
                    }
                }


                /*
                 * لمس خود تصویر = Drag تصویر
                 */
                if (
                        productBitmap != null
                                && productPreviewRect.contains(
                                touchX,
                                touchY
                        )
                ) {

                    touchMode =
                            MODE_PRODUCT_DRAG;

                    selected =
                            -1;

                    downX =
                            touchX;

                    downY =
                            touchY;

                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    true
                            );

                    invalidate();

                    return true;
                }


                /*
                 * انتخاب کادر قیمت
                 */
                selected =
                        -1;


                for (
                        int i =
                                fields.size() - 1;
                        i >= 0;
                        i--
                ) {

                    LabelField f =
                            fields.get(i);

                    if (!f.visible) {
                        continue;
                    }

                    RectF rect =
                            fieldRect(f);


                    if (
                            rect.contains(
                                    touchX,
                                    touchY
                            )
                    ) {

                        selected =
                                i;

                        break;
                    }
                }


                downX =
                        touchX;

                downY =
                        touchY;


                if (
                        selected >= 0
                ) {

                    touchMode =
                            MODE_FIELD_DRAG;


                    getParent()
                            .requestDisallowInterceptTouchEvent(
                                    true
                            );


                    if (
                            listener != null
                    ) {

                        listener.onFieldSelected(
                                selected
                        );
                    }
                }


                invalidate();

                return true;
            }


            case MotionEvent.ACTION_MOVE: {


                /*
                 * جابه‌جایی تصویر محصول
                 */
                if (
                        touchMode
                                == MODE_PRODUCT_DRAG
                ) {

                    float dx =
                            (
                                    touchX
                                            - downX
                            )
                                    / getWidth();


                    float dy =
                            (
                                    touchY
                                            - downY
                            )
                                    / getHeight();


                    productX =
                            clamp(
                                    productX + dx,
                                    0f,
                                    1f - productW
                            );


                    productY =
                            clamp(
                                    productY + dy,
                                    0f,
                                    1f - productH
                            );


                    downX =
                            touchX;

                    downY =
                            touchY;


                    invalidate();

                    return true;
                }


                /*
                 * Resize تصویر محصول
                 */
                if (
                        touchMode
                                == MODE_PRODUCT_RESIZE
                ) {

                    float rightPct =
                            touchX
                                    / getWidth();


                    float bottomPct =
                            touchY
                                    / getHeight();


                    rightPct =
                            clamp(
                                    rightPct,
                                    productX
                                            + MIN_PRODUCT_W,
                                    1f
                            );


                    bottomPct =
                            clamp(
                                    bottomPct,
                                    productY
                                            + MIN_PRODUCT_H,
                                    1f
                            );


                    productW =
                            rightPct
                                    - productX;


                    productH =
                            bottomPct
                                    - productY;


                    invalidate();

                    return true;
                }


                if (
                        selected < 0
                                || selected >= fields.size()
                ) {

                    return true;
                }


                LabelField field =
                        fields.get(selected);


                /*
                 * Drag کادر قیمت
                 */
                if (
                        touchMode
                                == MODE_FIELD_DRAG
                ) {

                    float dx =
                            (
                                    touchX
                                            - downX
                            )
                                    / workingRect.width();


                    float dy =
                            (
                                    touchY
                                            - downY
                            )
                                    / workingRect.height();


                    field.x =
                            clamp(
                                    field.x + dx,
                                    0f,
                                    1f - field.w
                            );


                    field.y =
                            clamp(
                                    field.y + dy,
                                    0f,
                                    1f - field.h
                            );


                    downX =
                            touchX;

                    downY =
                            touchY;


                    invalidate();

                    return true;
                }


                /*
                 * Resize کادر قیمت
                 */
                if (
                        touchMode
                                == MODE_FIELD_RESIZE
                ) {

                    float newRightPct =
                            (
                                    touchX
                                            - workingRect.left
                            )
                                    / workingRect.width();


                    float newBottomPct =
                            (
                                    touchY
                                            - workingRect.top
                            )
                                    / workingRect.height();


                    newRightPct =
                            clamp(
                                    newRightPct,
                                    field.x
                                            + MIN_FIELD_W,
                                    1f
                            );


                    newBottomPct =
                            clamp(
                                    newBottomPct,
                                    field.y
                                            + MIN_FIELD_H,
                                    1f
                            );


                    field.w =
                            newRightPct
                                    - field.x;


                    field.h =
                            newBottomPct
                                    - field.y;


                    invalidate();

                    return true;
                }


                return true;
            }


            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL: {

                if (
                        listener != null
                ) {

                    listener.onChanged();
                }


                touchMode =
                        MODE_NONE;


                getParent()
                        .requestDisallowInterceptTouchEvent(
                                false
                        );


                invalidate();

                return true;
            }
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

        int sw =
                source.getWidth();

        int sh =
                source.getHeight();


        float safeWidthPercent =
                Math.max(
                        0.24f,
                        Math.min(
                                widthPercent,
                                0.55f
                        )
                );


        int labelAreaW =
                Math.max(
                        260,
                        (int) (
                                sw
                                        * safeWidthPercent
                        )
                );


        int gap =
                Math.max(
                        10,
                        sw / 80
                );


        Bitmap output;

        RectF productArea;
        RectF labelArea;


        if (append) {

            int outW =
                    sw
                            + labelAreaW
                            + gap;


            output =
                    Bitmap.createBitmap(
                            outW,
                            sh,
                            Bitmap.Config.ARGB_8888
                    );


            Canvas canvas =
                    new Canvas(output);


            canvas.drawColor(
                    backgroundColor
            );


            productArea =
                    new RectF(
                            productX * sw,
                            productY * sh,
                            (productX + productW) * sw,
                            (productY + productH) * sh
                    );


            drawBitmapFitCenter(
                    canvas,
                    source,
                    productArea
            );


            labelArea =
                    new RectF(
                            sw + gap,
                            0,
                            outW,
                            sh
                    );


        } else {

            output =
                    Bitmap.createBitmap(
                            sw,
                            sh,
                            Bitmap.Config.ARGB_8888
                    );


            Canvas canvas =
                    new Canvas(output);


            canvas.drawColor(
                    backgroundColor
            );


            productArea =
                    new RectF(
                            productX * sw,
                            productY * sh,
                            (productX + productW) * sw,
                            (productY + productH) * sh
                    );


            drawBitmapFitCenter(
                    canvas,
                    source,
                    productArea
            );


            int margin =
                    Math.max(
                            10,
                            sw / 80
                    );


            labelArea =
                    new RectF(
                            sw
                                    - labelAreaW
                                    - margin,
                            margin,
                            sw
                                    - margin,
                            sh
                                    - margin
                    );
        }


        Canvas canvas =
                new Canvas(output);


        for (
                LabelField field : fields
        ) {

            if (!field.visible) {
                continue;
            }


            RectF rect =
                    new RectF(
                            labelArea.left
                                    + field.x
                                    * labelArea.width(),

                            labelArea.top
                                    + field.y
                                    * labelArea.height(),

                            labelArea.left
                                    + (
                                    field.x
                                            + field.w
                            )
                                    * labelArea.width(),

                            labelArea.top
                                    + (
                                    field.y
                                            + field.h
                            )
                                    * labelArea.height()
                    );


            drawField(
                    canvas,
                    field,
                    rect
            );
        }


        return output;
    }


    private void fitTextSize(
            Paint paint,
            String text,
            float maxWidth,
            float minPx
    ) {

        if (
                text == null
                        || text.isEmpty()
        ) {
            return;
        }


        float size =
                paint.getTextSize();


        while (
                paint.measureText(text)
                        > maxWidth
                        && size > minPx
        ) {

            size -=
                    1f;

            paint.setTextSize(
                    size
            );
        }
    }


    private float clamp(
            float value,
            float min,
            float max
    ) {

        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }


    private String safe(
            String s
    ) {

        return s == null
                ? ""
                : s;
    }


    private float spToPx(
            float sp
    ) {

        return sp
                * getResources()
                .getDisplayMetrics()
                .scaledDensity;
    }
                        }
