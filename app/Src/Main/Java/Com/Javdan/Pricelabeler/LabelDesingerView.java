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
    public float labelWidthPct = 0.38f;

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
            Rect source = new Rect(
                    0,
                    0,
                    productBitmap.getWidth(),
                    productBitmap.getHeight()
            );

            RectF destination = new RectF(
                    12,
                    height * 0.08f,
                    width * 0.57f,
                    height * 0.92f
            );

            canvas.drawBitmap(productBitmap, source, destination, null);

        } else {
            Paint placeholder = new Paint();
            placeholder.setColor(Color.WHITE);

            canvas.drawRect(
                    12,
                    height * 0.08f,
                    width * 0.57f,
                    height * 0.92f,
                    placeholder
            );
        }

        // Label area
        labelRect.set(
                width * 0.61f,
                height * 0.08f,
                width - 12,
                height * 0.92f
        );

        Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
        background.setColor(labelBg);

        canvas.drawRoundRect(
                labelRect,
                22,
                22,
                background
        );

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(3);
        border.setColor(labelBorder);

        canvas.drawRoundRect(
                labelRect,
                22,
                22,
                border
        );

        // Draw price fields
        for (int i = 0; i < fields.size(); i++) {

            LabelField field = fields.get(i);

            RectF rect = fieldRect(field);

            Paint box = new Paint();
            box.setStyle(Paint.Style.STROKE);
            box.setStrokeWidth(i == selected ? 4 : 2);
            box.setColor(
                    i == selected
                            ? 0xFF1976D2
                            : 0xFFBBBBBB
            );

            canvas.drawRect(rect, box);

            Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            titlePaint.setColor(field.titleColor);
            titlePaint.setTextSize(dp(field.titleSize));
            titlePaint.setTextAlign(Paint.Align.RIGHT);
            titlePaint.setTypeface(Typeface.DEFAULT);

            Paint pricePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            pricePaint.setColor(field.priceColor);
            pricePaint.setTextSize(dp(field.priceSize));
            pricePaint.setTextAlign(Paint.Align.RIGHT);
            pricePaint.setTypeface(Typeface.DEFAULT_BOLD);

            float textX = rect.right - dp(6);

            float titleY =
                    rect.top + dp(field.titleSize + 2);

            canvas.drawText(
                    field.name,
                    textX,
                    titleY,
                    titlePaint
            );

            String value;

            if (field.value == null || field.value.isEmpty()) {
                value = "123,456 تومان";
            } else {
                value = field.value + " تومان";
            }

            float priceY =
                    titleY + dp(field.priceSize + 4);

            canvas.drawText(
                    value,
                    textX,
                    priceY,
                    pricePaint
            );

            // Strike-through
            if (field.strike) {

                float textWidth =
                        pricePaint.measureText(value);

                canvas.drawLine(
                        textX - textWidth,
                        priceY - dp(field.priceSize * 0.32f),
                        textX,
                        priceY - dp(field.priceSize * 0.32f),
                        pricePaint
                );
            }
        }
    }

    private RectF fieldRect(LabelField field) {

        float labelWidth = labelRect.width();
        float labelHeight = labelRect.height();

        float left =
                labelRect.left
                        + field.x * labelWidth;

        float top =
                labelRect.top
                        + field.y * labelHeight;

        return new RectF(
                left,
                top,
                left + field.w * labelWidth,
                top + field.h * labelHeight
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction()
                == MotionEvent.ACTION_DOWN) {

            selected = -1;

            for (int i = fields.size() - 1;
                 i >= 0;
                 i--) {

                if (fieldRect(fields.get(i))
                        .contains(
                                event.getX(),
                                event.getY()
                        )) {

                    selected = i;
                    break;
                }
            }

            downX = event.getX();
            downY = event.getY();

            if (listener != null
                    && selected >= 0) {

                listener.onFieldSelected(selected);
            }

            invalidate();

            return true;
        }

        if (event.getAction()
                == MotionEvent.ACTION_MOVE
                && selected >= 0) {

            LabelField field =
                    fields.get(selected);

            float dx =
                    (event.getX() - downX)
                            / labelRect.width();

            float dy =
                    (event.getY() - downY)
                            / labelRect.height();

            field.x =
                    Math.max(
                            0,
                            Math.min(
                                    1 - field.w,
                                    field.x + dx
                            )
                    );

            field.y =
                    Math.max(
                            0,
                            Math.min(
                                    1 - field.h,
                                    field.y + dy
                            )
                    );

            downX = event.getX();
            downY = event.getY();

            invalidate();

            return true;
        }

        if (event.getAction()
                == MotionEvent.ACTION_UP) {

            if (listener != null) {
                listener.onChanged();
            }

            return true;
        }

        return true;
    }

    private float dp(float value) {
        return value *
                getResources()
                        .getDisplayMetrics()
                        .scaledDensity;
    }

    public Bitmap renderFinal(
            Bitmap source,
            int backgroundColor,
            int borderColor,
            boolean append,
            float widthPercent
    ) {

        int sourceWidth =
                source.getWidth();

        int sourceHeight =
                source.getHeight();

        int labelWidth =
                Math.max(
                        260,
                        (int) (
                                sourceWidth
                                        * widthPercent
                        )
                );

        int margin =
                Math.max(
                        12,
                        sourceWidth / 60
                );

        Bitmap output;

        RectF box;

        if (append) {

            output =
                    Bitmap.createBitmap(
                            sourceWidth + labelWidth,
                            sourceHeight,
                            Bitmap.Config.ARGB_8888
                    );

            Canvas canvas =
                    new Canvas(output);

            canvas.drawColor(backgroundColor);

            canvas.drawBitmap(
                    source,
                    0,
                    0,
                    null
            );

            box =
                    new RectF(
                            sourceWidth,
                            0,
                            sourceWidth + labelWidth - 1,
                            sourceHeight - 1
                    );

        } else {

            output =
                    source.copy(
                            Bitmap.Config.ARGB_8888,
                            true
                    );

            box =
                    new RectF(
                            sourceWidth
                                    - labelWidth
                                    - margin,

                            margin,

                            sourceWidth
                                    - margin,

                            sourceHeight
                                    - margin
                    );
        }

        Canvas canvas =
                new Canvas(output);

        Paint background =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        background.setColor(
                backgroundColor
        );

        canvas.drawRoundRect(
                box,
                24,
                24,
                background
        );

        Paint border =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        border.setStyle(
                Paint.Style.STROKE
        );

        border.setStrokeWidth(3);
        border.setColor(borderColor);

        canvas.drawRoundRect(
                box,
                24,
                24,
                border
        );

        for (LabelField field : fields) {

            float left =
                    box.left
                            + field.x
                            * box.width();

            float top =
                    box.top
                            + field.y
                            * box.height();

            float right =
                    left
                            + field.w
                            * box.width();

            Paint titlePaint =
                    new Paint(
                            Paint.ANTI_ALIAS_FLAG
                    );

            titlePaint.setColor(
                    field.titleColor
            );

            titlePaint.setTextSize(
                    spToPx(
                            field.titleSize
                    )
            );

            titlePaint.setTextAlign(
                    Paint.Align.RIGHT
            );

            Paint pricePaint =
                    new Paint(
                            Paint.ANTI_ALIAS_FLAG
                    );

            pricePaint.setColor(
                    field.priceColor
            );

            pricePaint.setTextSize(
                    spToPx(
                            field.priceSize
                    )
            );

            pricePaint.setTextAlign(
                    Paint.Align.RIGHT
            );

            pricePaint.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            float textX =
                    right - 10;

            float titleY =
                    top
                            + spToPx(
                            field.titleSize + 2
                    );

            canvas.drawText(
                    field.name,
                    textX,
                    titleY,
                    titlePaint
            );

            String value =
                    field.value == null
                            ? ""
                            : field.value;

            if (!value.isEmpty()) {

                String priceText =
                        value + " تومان";

                float priceY =
                        titleY
                                + spToPx(
                                field.priceSize + 4
                        );

                canvas.drawText(
                        priceText,
                        textX,
                        priceY,
                        pricePaint
                );

                if (field.strike) {

                    float textWidth =
                            pricePaint
                                    .measureText(
                                            priceText
                                    );

                    canvas.drawLine(
                            textX - textWidth,

                            priceY
                                    - spToPx(
                                    field.priceSize
                                            * 0.32f
                            ),

                            textX,

                            priceY
                                    - spToPx(
                                    field.priceSize
                                            * 0.32f
                            ),

                            pricePaint
                    );
                }
            }
        }

        return output;
    }

    private float spToPx(float sp) {
        return sp *
                getResources()
                        .getDisplayMetrics()
                        .scaledDensity;
    }
                    }
