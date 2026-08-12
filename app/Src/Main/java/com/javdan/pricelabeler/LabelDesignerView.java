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

    private Listener listener;

    // تنظیمات کلی پیش‌نمایش
    public boolean appendMode = true;
    public int canvasBackground = 0xFFF2F2F2;
    public float labelWidthPct = 0.38f;

    // موقعیت و اندازه تصویر محصول در پیش‌نمایش و خروجی
    public float productX = 0.02f;
    public float productY = 0.05f;
    public float productW = 0.58f;
    public float productH = 0.90f;


    public LabelDesignerView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
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
        this.productBitmap = bitmap;
        invalidate();
    }


    public void select(int index) {
        selected = index;
        invalidate();
    }


    public int getSelectedIndex() {
        return selected;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(canvasBackground);

        float width = getWidth();
        float height = getHeight();

        drawProductPreview(
                canvas,
                width,
                height
        );

        // محدوده طراحی کادرهای قیمت
        workingRect.set(
                width * 0.61f,
                height * 0.06f,
                width * 0.98f,
                height * 0.94f
        );

        for (int i = 0; i < fields.size(); i++) {

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

            if (i == selected) {
                drawSelection(
                        canvas,
                        rect
                );
            }
        }
    }


    private void drawProductPreview(
            Canvas canvas,
            float width,
            float height
    ) {

        RectF area =
                new RectF(
                        width * productX,
                        height * productY,
                        width * (productX + productW),
                        height * (productY + productH)
                );

        if (productBitmap == null) {

            Paint empty =
                    new Paint(
                            Paint.ANTI_ALIAS_FLAG
                    );

            empty.setColor(
                    Color.WHITE
            );

            canvas.drawRoundRect(
                    area,
                    18,
                    18,
                    empty
            );

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


    private void drawSelection(
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

        Paint handle =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        handle.setColor(
                0xFF1976D2
        );

        canvas.drawCircle(
                rect.right,
                rect.bottom,
                9f,
                handle
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

        // پس‌زمینه مستقل
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


        // حاشیه مستقل
        if (field.borderWidth > 0) {

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


        // عنوان
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


        float cursorY = top;


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


        // قیمت
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


        if (field.textAlign == 0) {

            // راست‌چین
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


            if (field.showToman) {

                canvas.drawText(
                        unit,
                        unitRight,
                        priceBaseline,
                        unitPaint
                );
            }


            if (field.strike) {

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

            // وسط یا چپ
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


            if (field.strike) {

                float textW =
                        pricePaint.measureText(
                                fullText
                        );

                float strikeLeft;
                float strikeRight;


                if (field.textAlign == 1) {

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

        float right =
                left
                        + field.w * lw;

        float bottom =
                top
                        + field.h * lh;

        return new RectF(
                left,
                top,
                right,
                bottom
        );
    }


    @Override
    public boolean onTouchEvent(
            MotionEvent event
    ) {

        if (
                event.getAction()
                        == MotionEvent.ACTION_DOWN
        ) {

            selected = -1;


            for (
                    int i = fields.size() - 1;
                    i >= 0;
                    i--
            ) {

                LabelField f =
                        fields.get(i);

                if (!f.visible) {
                    continue;
                }

                if (
                        fieldRect(f)
                                .contains(
                                        event.getX(),
                                        event.getY()
                                )
                ) {

                    selected = i;
                    break;
                }
            }


            downX =
                    event.getX();

            downY =
                    event.getY();


            if (
                    listener != null
                            && selected >= 0
            ) {

                listener.onFieldSelected(
                        selected
                );
            }


            invalidate();

            return true;
        }


        if (
                event.getAction()
                        == MotionEvent.ACTION_MOVE
                        && selected >= 0
        ) {

            LabelField field =
                    fields.get(selected);


            float dx =
                    (
                            event.getX()
                                    - downX
                    )
                            / workingRect.width();


            float dy =
                    (
                            event.getY()
                                    - downY
                    )
                            / workingRect.height();


            field.x =
                    Math.max(
                            0f,
                            Math.min(
                                    1f
                                            - field.w,
                                    field.x + dx
                            )
                    );


            field.y =
                    Math.max(
                            0f,
                            Math.min(
                                    1f
                                            - field.h,
                                    field.y + dy
                            )
                    );


            downX =
                    event.getX();

            downY =
                    event.getY();


            invalidate();

            return true;
        }


        if (
                event.getAction()
                        == MotionEvent.ACTION_UP
        ) {

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


            // تصویر محصول با موقعیت و اندازه قابل تنظیم
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


            // محدوده کادرهای قیمت
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


        // فقط کادرهای مستقل رسم می‌شوند
        for (
                LabelField field
                        : fields
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

            size -= 1f;

            paint.setTextSize(
                    size
            );
        }
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
