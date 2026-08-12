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

        // 0 = title
        // 1 = price
        // 2 = toman
        void onTextClicked(int fieldIndex, int part);
    }

    private ArrayList<LabelField> fields = new ArrayList<>();

    private Bitmap productBitmap;

    private int selected = -1;

    private float downX;
    private float downY;

    private final RectF labelRect = new RectF();

    private Listener listener;

    public int canvasBackground = 0xFFF0F0F0;

    /*
     * محل نمایش تصویر محصول در Designer
     */
    public float productX = 0.02f;
    public float productY = 0.08f;
    public float productW = 0.55f;
    public float productH = 0.84f;

    /*
     * Crop درصدی
     *
     * left = 0 → بدون برش از چپ
     * right = 1 → بدون برش از راست
     */
    public float cropLeft = 0f;
    public float cropTop = 0f;
    public float cropRight = 1f;
    public float cropBottom = 1f;

    /*
     * حالت Crop
     */
    private boolean cropMode = false;

    /*
     * 0 = none
     * 1 = left
     * 2 = top
     * 3 = right
     * 4 = bottom
     */
    private int cropHandle = 0;

    /*
     * حرکت/Resize کادر قیمت
     */
    private int touchMode = 0;

    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_MOVE = 1;
    private static final int TOUCH_RESIZE = 2;

    private float handleRadius = 22f;

    public LabelDesignerView(Context context) {

        super(context);

        setLayerType(
                View.LAYER_TYPE_SOFTWARE,
                null
        );
    }

    public void setListener(
            Listener listener
    ) {

        this.listener =
                listener;
    }

    public void setFields(
            ArrayList<LabelField> fields
    ) {

        this.fields =
                fields;

        invalidate();
    }

    public ArrayList<LabelField> getFields() {

        return fields;
    }

    public void setProductBitmap(
            Bitmap bitmap
    ) {

        this.productBitmap =
                bitmap;

        invalidate();
    }

    public void select(
            int index
    ) {

        selected =
                index;

        invalidate();
    }

    public int getSelected() {

        return selected;
    }

    public void setCropMode(
            boolean enabled
    ) {

        cropMode =
                enabled;

        cropHandle =
                0;

        invalidate();
    }

    public boolean isCropMode() {

        return cropMode;
    }

    public void resetCrop() {

        cropLeft =
                0f;

        cropTop =
                0f;

        cropRight =
                1f;

        cropBottom =
                1f;

        invalidate();

        if (
                listener != null
        ) {

            listener.onChanged();
        }
    }

    public void setCrop(
            float left,
            float top,
            float right,
            float bottom
    ) {

        cropLeft =
                clamp(
                        left,
                        0f,
                        0.95f
                );

        cropTop =
                clamp(
                        top,
                        0f,
                        0.95f
                );

        cropRight =
                clamp(
                        right,
                        cropLeft + 0.05f,
                        1f
                );

        cropBottom =
                clamp(
                        bottom,
                        cropTop + 0.05f,
                        1f
                );

        invalidate();
    }

    @Override
    protected void onDraw(
            Canvas canvas
    ) {

        super.onDraw(
                canvas
        );

        canvas.drawColor(
                canvasBackground
        );

        float width =
                getWidth();

        float height =
                getHeight();

        RectF productRect =
                getProductRect();

        drawProduct(
                canvas,
                productRect
        );

        /*
         * لیبل سمت راست
         */
        labelRect.set(
                width * 0.59f,
                height * 0.08f,
                width - 12,
                height * 0.92f
        );

        /*
         * کادر اصلی بسیار کم‌رنگ
         */
        Paint labelBorderPaint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        labelBorderPaint.setStyle(
                Paint.Style.STROKE
        );

        labelBorderPaint.setStrokeWidth(
                1.5f
        );

        labelBorderPaint.setColor(
                0xFFDDDDDD
        );

        canvas.drawRoundRect(
                labelRect,
                18,
                18,
                labelBorderPaint
        );

        /*
         * کادرهای قیمت
         */
        for (
                int i = 0;
                i < fields.size();
                i++
        ) {

            LabelField field =
                    fields.get(i);

            if (
                    !field.visible
            ) {

                continue;
            }

            RectF rect =
                    fieldRect(
                            field
                    );

            drawFieldBackground(
                    canvas,
                    field,
                    rect,
                    i == selected
            );

            drawField(
                    canvas,
                    field,
                    rect
            );

            if (
                    i == selected
                            && !cropMode
            ) {

                drawResizeHandle(
                        canvas,
                        rect
                );
            }
        }

        if (
                cropMode
                        && productBitmap != null
        ) {

            drawCropOverlay(
                    canvas,
                    productRect
            );
        }
    }

    private void drawProduct(
            Canvas canvas,
            RectF productRect
    ) {

        if (
                productBitmap == null
        ) {

            Paint p =
                    new Paint(
                            Paint.ANTI_ALIAS_FLAG
                    );

            p.setColor(
                    Color.WHITE
            );

            canvas.drawRoundRect(
                    productRect,
                    12,
                    12,
                    p
            );

            return;
        }

        Rect src =
                cropSourceRect(
                        productBitmap
                );

        Paint p =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                                | Paint.FILTER_BITMAP_FLAG
                );

        canvas.drawBitmap(
                productBitmap,
                src,
                productRect,
                p
        );
    }

    private Rect cropSourceRect(
            Bitmap bitmap
    ) {

        int w =
                bitmap.getWidth();

        int h =
                bitmap.getHeight();

        int left =
                Math.round(
                        cropLeft
                                * w
                );

        int top =
                Math.round(
                        cropTop
                                * h
                );

        int right =
                Math.round(
                        cropRight
                                * w
                );

        int bottom =
                Math.round(
                        cropBottom
                                * h
                );

        left =
                Math.max(
                        0,
                        Math.min(
                                left,
                                w - 1
                        )
                );

        top =
                Math.max(
                        0,
                        Math.min(
                                top,
                                h - 1
                        )
                );

        right =
                Math.max(
                        left + 1,
                        Math.min(
                                right,
                                w
                        )
                );

        bottom =
                Math.max(
                        top + 1,
                        Math.min(
                                bottom,
                                h
                        )
                );

        return new Rect(
                left,
                top,
                right,
                bottom
        );
    }

    private RectF getProductRect() {

        float width =
                getWidth();

        float height =
                getHeight();

        return new RectF(
                productX * width,
                productY * height,
                (
                        productX
                                + productW
                )
                        * width,
                (
                        productY
                                + productH
                )
                        * height
        );
    }

    private void drawFieldBackground(
            Canvas canvas,
            LabelField field,
            RectF rect,
            boolean selected
    ) {

        Paint bg =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        bg.setColor(
                field.backgroundColor
        );

        canvas.drawRoundRect(
                rect,
                field.cornerRadius,
                field.cornerRadius,
                bg
        );

        Paint border =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        border.setStyle(
                Paint.Style.STROKE
        );

        border.setStrokeWidth(
                Math.max(
                        1,
                        field.borderWidth
                )
        );

        border.setColor(
                selected
                        ? 0xFF1976D2
                        : field.borderColor
        );

        canvas.drawRoundRect(
                rect,
                field.cornerRadius,
                field.cornerRadius,
                border
        );
    }

    private void drawResizeHandle(
            Canvas canvas,
            RectF rect
    ) {

        Paint p =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        p.setColor(
                0xFF1976D2
        );

        canvas.drawCircle(
                rect.right,
                rect.bottom,
                handleRadius,
                p
        );

        Paint center =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        center.setColor(
                Color.WHITE
        );

        canvas.drawCircle(
                rect.right,
                rect.bottom,
                handleRadius * 0.42f,
                center
        );
    }

    private void drawField(
            Canvas canvas,
            LabelField field,
            RectF rect
    ) {

        float px =
                Math.max(
                        4f,
                        field.paddingHorizontal
                );

        float py =
                Math.max(
                        4f,
                        field.paddingVertical
                );

        float usableW =
                Math.max(
                        20f,
                        rect.width()
                                - px * 2
                );

        Paint.Align align =
                alignment(
                        field.textAlign
                );

        float x;

        if (
                align == Paint.Align.LEFT
        ) {

            x =
                    rect.left
                            + px;

        } else if (
                align == Paint.Align.CENTER
        ) {

            x =
                    rect.centerX();

        } else {

            x =
                    rect.right
                            - px;
        }

        float cursorY =
                rect.top
                        + py;

        /*
         * عنوان
         */
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
                        field.titleSize
                )
        );

        fitTextSize(
                titlePaint,
                safe(
                        field.name
                ),
                usableW,
                9
        );

        if (
                field.showTitle
                        && !safe(field.name).isEmpty()
        ) {

            float titleBase =
                    cursorY
                            - titlePaint.ascent();

            canvas.drawText(
                    safe(
                            field.name
                    ),
                    x,
                    titleBase,
                    titlePaint
            );

            cursorY =
                    titleBase
                            + titlePaint.descent()
                            + field.titlePriceGap;
        }

        /*
         * قیمت
         */
        String value =
                safe(
                        field.value
                )
                        .trim();

        if (
                !field.showPrice
                        || value.isEmpty()
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
                        field.priceSize
                )
        );

        Paint tomanPaint =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        tomanPaint.setColor(
                field.tomanColor
        );

        tomanPaint.setTypeface(
                field.getPriceTypeface()
        );

        tomanPaint.setTextSize(
                spToPx(
                        field.tomanSize
                )
        );

        tomanPaint.setTextAlign(
                align
        );

        String full =
                value;

        float priceBase =
                cursorY
                        - pricePaint.ascent();

        /*
         * برای تراز راست، تومان کنار قیمت
         */
        if (
                field.showToman
        ) {

            String unit =
                    "تومان";

            float gap =
                    8f;

            if (
                    align == Paint.Align.RIGHT
            ) {

                canvas.drawText(
                        value,
                        x,
                        priceBase,
                        pricePaint
                );

                float numberW =
                        pricePaint.measureText(
                                value
                        );

                tomanPaint.setTextAlign(
                        Paint.Align.RIGHT
                );

                float unitX =
                        x
                                - numberW
                                - gap;

                canvas.drawText(
                        unit,
                        unitX,
                        priceBase,
                        tomanPaint
                );

                if (
                        field.strike
                ) {

                    float unitW =
                            tomanPaint.measureText(
                                    unit
                            );

                    drawStrike(
                            canvas,
                            field,
                            unitX - unitW,
                            x,
                            priceBase,
                            pricePaint
                    );
                }

            } else {

                full =
                        value
                                + " تومان";

                canvas.drawText(
                        full,
                        x,
                        priceBase,
                        pricePaint
                );

                if (
                        field.strike
                ) {

                    float fullW =
                            pricePaint.measureText(
                                    full
                            );

                    float left;

                    float right;

                    if (
                            align == Paint.Align.CENTER
                    ) {

                        left =
                                x
                                        - fullW / 2f;

                        right =
                                x
                                        + fullW / 2f;

                    } else {

                        left =
                                x;

                        right =
                                x
                                        + fullW;
                    }

                    drawStrike(
                            canvas,
                            field,
                            left,
                            right,
                            priceBase,
                            pricePaint
                    );
                }
            }

        } else {

            canvas.drawText(
                    value,
                    x,
                    priceBase,
                    pricePaint
            );

            if (
                    field.strike
            ) {

                float w =
                        pricePaint.measureText(
                                value
                        );

                float left;

                float right;

                if (
                        align == Paint.Align.RIGHT
                ) {

                    left =
                            x - w;

                    right =
                            x;

                } else if (
                        align == Paint.Align.CENTER
                ) {

                    left =
                            x - w / 2f;

                    right =
                            x + w / 2f;

                } else {

                    left =
                            x;

                    right =
                            x + w;
                }

                drawStrike(
                        canvas,
                        field,
                        left,
                        right,
                        priceBase,
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

        Paint p =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        p.setColor(
                field.priceColor
        );

        p.setStrokeWidth(
                Math.max(
                        2,
                        pricePaint.getTextSize()
                                * 0.05f
                )
        );

        float y =
                baseline
                        - pricePaint.getTextSize()
                        * 0.33f;

        canvas.drawLine(
                left,
                y,
                right,
                y,
                p
        );
    }

    private Paint.Align alignment(
            int align
    ) {

        if (
                align == 1
        ) {

            return Paint.Align.CENTER;

        } else if (
                align == 2
        ) {

            return Paint.Align.LEFT;
        }

        return Paint.Align.RIGHT;
    }

    private RectF fieldRect(
            LabelField field
    ) {

        float lw =
                labelRect.width();

        float lh =
                labelRect.height();

        float left =
                labelRect.left
                        + field.x
                        * lw;

        float top =
                labelRect.top
                        + field.y
                        * lh;

        return new RectF(
                left,
                top,
                left
                        + field.w
                        * lw,
                top
                        + field.h
                        * lh
        );
    }

    private boolean near(
            float a,
            float b,
            float tolerance
    ) {

        return Math.abs(
                a - b
        )
                <= tolerance;
    }

    /*
     * تشخیص لمس عنوان، قیمت، تومان
     */
    private int textHitPart(
            LabelField field,
            RectF rect,
            float x,
            float y
    ) {

        float top =
                rect.top
                        + field.paddingVertical;

        float titleHeight =
                spToPx(
                        field.titleSize
                )
                        * 1.45f;

        float priceTop =
                top;

        if (
                field.showTitle
        ) {

            if (
                    y >= top
                            && y <= top + titleHeight
            ) {

                return 0;
            }

            priceTop =
                    top
                            + titleHeight
                            + field.titlePriceGap;
        }

        float priceHeight =
                spToPx(
                        field.priceSize
                )
                        * 1.5f;

        if (
                y >= priceTop
                        && y <= priceTop + priceHeight
        ) {

            /*
             * سمت چپ قیمت را برای تومان در نظر می‌گیریم.
             */
            if (
                    field.showToman
                            && field.textAlign == 0
            ) {

                float tomanArea =
                        rect.width()
                                * 0.30f;

                if (
                        x
                                < rect.right
                                - field.paddingHorizontal
                                - rect.width()
                                * 0.42f
                                && x
                                > rect.right
                                - tomanArea
                                - rect.width()
                                * 0.42f
                ) {

                    return 2;
                }
            }

            return 1;
        }

        return -1;
    }

    @Override
    public boolean onTouchEvent(
            MotionEvent event
    ) {

        if (
                cropMode
        ) {

            return handleCropTouch(
                    event
            );
        }

        if (
                event.getAction()
                        == MotionEvent.ACTION_DOWN
        ) {

            selected =
                    -1;

            touchMode =
                    TOUCH_NONE;

            /*
             * کادر را از بالا به پایین بررسی می‌کنیم.
             */
            for (
                    int i =
                    fields.size() - 1;
                    i >= 0;
                    i--
            ) {

                LabelField field =
                        fields.get(i);

                if (
                        !field.visible
                ) {

                    continue;
                }

                RectF rect =
                        fieldRect(
                                field
                        );

                /*
                 * Resize Handle
                 */
                float dx =
                        event.getX()
                                - rect.right;

                float dy =
                        event.getY()
                                - rect.bottom;

                if (
                        Math.sqrt(
                                dx * dx
                                        + dy * dy
                        )
                                <= handleRadius * 2
                ) {

                    selected =
                            i;

                    touchMode =
                            TOUCH_RESIZE;

                    break;
                }

                if (
                        rect.contains(
                                event.getX(),
                                event.getY()
                        )
                ) {

                    selected =
                            i;

                    int part =
                            textHitPart(
                                    field,
                                    rect,
                                    event.getX(),
                                    event.getY()
                            );

                    /*
                     * لمس متن:
                     * پنجره تنظیم سایز باز شود
                     */
                    if (
                            part >= 0
                    ) {

                        if (
                                listener != null
                        ) {

                            listener.onFieldSelected(
                                    i
                            );

                            listener.onTextClicked(
                                    i,
                                    part
                            );
                        }

                        invalidate();

                        return true;
                    }

                    touchMode =
                            TOUCH_MOVE;

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
                    fields.get(
                            selected
                    );

            float dx =
                    (
                            event.getX()
                                    - downX
                    )
                            / labelRect.width();

            float dy =
                    (
                            event.getY()
                                    - downY
                    )
                            / labelRect.height();

            if (
                    touchMode
                            == TOUCH_MOVE
            ) {

                field.x =
                        clamp(
                                field.x
                                        + dx,
                                0f,
                                1f
                                        - field.w
                        );

                field.y =
                        clamp(
                                field.y
                                        + dy,
                                0f,
                                1f
                                        - field.h
                        );

            } else if (
                    touchMode
                            == TOUCH_RESIZE
            ) {

                field.w =
                        clamp(
                                field.w
                                        + dx,
                                0.08f,
                                1f
                                        - field.x
                        );

                field.h =
                        clamp(
                                field.h
                                        + dy,
                                0.06f,
                                1f
                                        - field.y
                        );
            }

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

            touchMode =
                    TOUCH_NONE;

            if (
                    listener != null
            ) {

                listener.onChanged();
            }

            return true;
        }

        return true;
    }

    private boolean handleCropTouch(
            MotionEvent event
    ) {

        RectF productRect =
                getProductRect();

        float tolerance =
                35f;

        RectF cropScreen =
                cropScreenRect(
                        productRect
                );

        if (
                event.getAction()
                        == MotionEvent.ACTION_DOWN
        ) {

            cropHandle =
                    0;

            if (
                    near(
                            event.getX(),
                            cropScreen.left,
                            tolerance
                    )
            ) {

                cropHandle =
                        1;

            } else if (
                    near(
                            event.getY(),
                            cropScreen.top,
                            tolerance
                    )
            ) {

                cropHandle =
                        2;

            } else if (
                    near(
                            event.getX(),
                            cropScreen.right,
                            tolerance
                    )
            ) {

                cropHandle =
                        3;

            } else if (
                    near(
                            event.getY(),
                            cropScreen.bottom,
                            tolerance
                    )
            ) {

                cropHandle =
                        4;
            }

            return true;
        }

        if (
                event.getAction()
                        == MotionEvent.ACTION_MOVE
                        && cropHandle != 0
        ) {

            float px =
                    (
                            event.getX()
                                    - productRect.left
                    )
                            / productRect.width();

            float py =
                    (
                            event.getY()
                                    - productRect.top
                    )
                            / productRect.height();

            if (
                    cropHandle == 1
            ) {

                cropLeft =
                        clamp(
                                px,
                                0f,
                                cropRight
                                        - 0.05f
                        );

            } else if (
                    cropHandle == 2
            ) {

                cropTop =
                        clamp(
                                py,
                                0f,
                                cropBottom
                                        - 0.05f
                        );

            } else if (
                    cropHandle == 3
            ) {

                cropRight =
                        clamp(
                                px,
                                cropLeft
                                        + 0.05f,
                                1f
                        );

            } else if (
                    cropHandle == 4
            ) {

                cropBottom =
                        clamp(
                                py,
                                cropTop
                                        + 0.05f,
                                1f
                        );
            }

            invalidate();

            return true;
        }

        if (
                event.getAction()
                        == MotionEvent.ACTION_UP
        ) {

            cropHandle =
                    0;

            if (
                    listener != null
            ) {

                listener.onChanged();
            }

            return true;
        }

        return true;
    }

    private RectF cropScreenRect(
            RectF productRect
    ) {

        return new RectF(
                productRect.left
                        + cropLeft
                        * productRect.width(),

                productRect.top
                        + cropTop
                        * productRect.height(),

                productRect.left
                        + cropRight
                        * productRect.width(),

                productRect.top
                        + cropBottom
                        * productRect.height()
        );
    }

    private void drawCropOverlay(
            Canvas canvas,
            RectF productRect
    ) {

        RectF cropRect =
                cropScreenRect(
                        productRect
                );

        Paint dim =
                new Paint();

        dim.setColor(
                0x88000000
        );

        /*
         * چهار ناحیه تاریک اطراف Crop
         */
        canvas.drawRect(
                productRect.left,
                productRect.top,
                productRect.right,
                cropRect.top,
                dim
        );

        canvas.drawRect(
                productRect.left,
                cropRect.bottom,
                productRect.right,
                productRect.bottom,
                dim
        );

        canvas.drawRect(
                productRect.left,
                cropRect.top,
                cropRect.left,
                cropRect.bottom,
                dim
        );

        canvas.drawRect(
                cropRect.right,
                cropRect.top,
                productRect.right,
                cropRect.bottom,
                dim
        );

        Paint border =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        border.setStyle(
                Paint.Style.STROKE
        );

        border.setStrokeWidth(
                4f
        );

        border.setColor(
                Color.WHITE
        );

        canvas.drawRect(
                cropRect,
                border
        );

        Paint handle =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        handle.setColor(
                0xFF1976D2
        );

        float r =
                15f;

        canvas.drawCircle(
                cropRect.left,
                cropRect.centerY(),
                r,
                handle
        );

        canvas.drawCircle(
                cropRect.right,
                cropRect.centerY(),
                r,
                handle
        );

        canvas.drawCircle(
                cropRect.centerX(),
                cropRect.top,
                r,
                handle
        );

        canvas.drawCircle(
                cropRect.centerX(),
                cropRect.bottom,
                r,
                handle
        );
    }

    /*
     * خروجی نهایی
     */
    public Bitmap renderFinal(
            Bitmap source,
            int backgroundColor,
            int borderColor,
            boolean append,
            float widthPercent
    ) {

        Bitmap cropped =
                makeCroppedBitmap(
                        source
                );

        int sw =
                cropped.getWidth();

        int sh =
                cropped.getHeight();

        float safeWidthPercent =
                Math.max(
                        0.28f,
                        Math.min(
                                widthPercent,
                                0.60f
                        )
                );

        int lw =
                Math.max(
                        260,
                        (int) (
                                sw
                                        * safeWidthPercent
                        )
                );

        int margin =
                Math.max(
                        12,
                        sw / 60
                );

        Bitmap output;

        RectF box;

        if (
                append
        ) {

            output =
                    Bitmap.createBitmap(
                            sw + lw,
                            sh,
                            Bitmap.Config.ARGB_8888
                    );

            Canvas c =
                    new Canvas(
                            output
                    );

            c.drawColor(
                    backgroundColor
            );

            c.drawBitmap(
                    cropped,
                    0,
                    0,
                    null
            );

            box =
                    new RectF(
                            sw,
                            margin,
                            sw + lw - margin,
                            sh - margin
                    );

        } else {

            output =
                    cropped.copy(
                            Bitmap.Config.ARGB_8888,
                            true
                    );

            box =
                    new RectF(
                            sw - lw - margin,
                            margin,
                            sw - margin,
                            sh - margin
                    );
        }

        Canvas canvas =
                new Canvas(
                        output
                );

        /*
         * فقط محدوده کلی label
         * شفاف/ساده است.
         * رنگ اصلی هر کادر از خود LabelField می‌آید.
         */
        Paint outer =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        outer.setStyle(
                Paint.Style.STROKE
        );

        outer.setStrokeWidth(
                1.5f
        );

        outer.setColor(
                borderColor
        );

        canvas.drawRoundRect(
                box,
                20,
                20,
                outer
        );

        RectF oldLabel =
                new RectF(
                        labelRect
                );

        labelRect.set(
                box
        );

        for (
                LabelField field
                        : fields
        ) {

            if (
                    !field.visible
            ) {

                continue;
            }

            RectF rect =
                    fieldRect(
                            field
                    );

            drawFieldBackground(
                    canvas,
                    field,
                    rect,
                    false
            );

            drawField(
                    canvas,
                    field,
                    rect
            );
        }

        labelRect.set(
                oldLabel
        );

        if (
                cropped != source
                        && !cropped.isRecycled()
        ) {

            cropped.recycle();
        }

        return output;
    }

    private Bitmap makeCroppedBitmap(
            Bitmap source
    ) {

        Rect src =
                cropSourceRect(
                        source
                );

        if (
                src.left == 0
                        && src.top == 0
                        && src.right == source.getWidth()
                        && src.bottom == source.getHeight()
        ) {

            return source;
        }

        return Bitmap.createBitmap(
                source,
                src.left,
                src.top,
                src.width(),
                src.height()
        );
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
                paint.measureText(
                        text
                )
                        > maxWidth
                        && size
                        > minPx
        ) {

            size -=
                    1f;

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
}
