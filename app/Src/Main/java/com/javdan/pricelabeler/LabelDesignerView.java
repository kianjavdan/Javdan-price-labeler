package com.javdan.pricelabeler;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class LabelDesignerView extends View {

    public static final int TEXT_TITLE = 0;
    public static final int TEXT_PRICE = 1;

    public interface Listener {
        void onFieldSelected(int index);
        void onChanged();
        void onTextClicked(int fieldIndex, int textType);
    }

    private ArrayList<LabelField> fields = new ArrayList<>();
    private Bitmap productBitmap;
    private int selected = -1;
    private Listener listener;

    private final RectF previewProductRect = new RectF();
    private final RectF previewLabelRect = new RectF();

    private float touchDownX, touchDownY;
    private float lastX, lastY;
    private boolean moved = false;
    private boolean resizing = false;

    public float productX = 0.02f;
    public float productY = 0.08f;
    public float productW = 0.62f;
    public float productH = 0.84f;

    public int canvasBackground = 0xFFF2F2F2;

    public boolean cropEnabled = false;
    public float cropLeft = 0f;
    public float cropTop = 0f;
    public float cropRight = 1f;
    public float cropBottom = 1f;

    public int outerTagColor = 0xFF181818;
    public int outerTagBorderColor = 0xFF3A3A3A;
    public int outerTagBorderWidth = 3;
    public int outerTagRadius = 22;

    public float labelWidthPct = 0.28f;
    public float fieldGapPct = 0.014f;
    public boolean autoHeight = true;

    public LabelDesignerView(Context context){
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
    }

    public void setListener(Listener l){
        listener = l;
    }

    public void setFields(ArrayList<LabelField> f){
        fields = f == null ? new ArrayList<>() : f;
        invalidate();
    }

    public ArrayList<LabelField> getFields(){
        return fields;
    }

    public void setProductBitmap(Bitmap b){
        productBitmap = b;
        invalidate();
    }

    public void select(int index){
        selected = index;
        invalidate();
    }

    public void resetCrop(){
        cropEnabled = false;
        cropLeft = 0f;
        cropTop = 0f;
        cropRight = 1f;
        cropBottom = 1f;
        invalidate();
    }

    public void setCrop(float l, float t, float r, float b){
        cropLeft = clamp(l,0f,.95f);
        cropTop = clamp(t,0f,.95f);
        cropRight = clamp(r,cropLeft+.05f,1f);
        cropBottom = clamp(b,cropTop+.05f,1f);
        cropEnabled = true;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawColor(canvasBackground);

        float w = getWidth();
        float h = getHeight();

        previewProductRect.set(
                productX*w,
                productY*h,
                (productX+productW)*w,
                (productY+productH)*h
        );

        drawProduct(canvas,productBitmap,previewProductRect);

        float labelW = Math.max(w*.24f,w*labelWidthPct);
        float rightMargin = w*.014f;
        float labelLeft = w - labelW - rightMargin;

        float tagH = calculatePreviewTagHeight(h);
        float labelTop = Math.max(h*.04f,(h-tagH)/2f);

        previewLabelRect.set(
                labelLeft,
                labelTop,
                w-rightMargin,
                labelTop+tagH
        );

        drawOuterTag(canvas,previewLabelRect);

        RectF inner = innerTag(previewLabelRect);
        layoutCards(inner);

        for (int i=0;i<fields.size();i++){
            LabelField f = fields.get(i);
            if (!f.visible) continue;
            RectF r = fieldRect(f,inner);
            drawCard(canvas,f,r,i==selected);
        }

        if (selected >= 0 && selected < fields.size()){
            drawResizeHandle(canvas,fieldRect(fields.get(selected),inner));
        }
    }

    private float calculatePreviewTagHeight(float canvasH){
        int count = visibleCount();
        float gap = canvasH*fieldGapPct;

        if (!autoHeight){
            return canvasH*.72f;
        }

        float compactCardH = canvasH*.125f;
        float pad = canvasH*.018f;

        float total = pad*2f + count*compactCardH + Math.max(0,count-1)*gap;
        return Math.min(canvasH*.82f,Math.max(canvasH*.28f,total));
    }

    private int visibleCount(){
        int c=0;
        for (LabelField f:fields) if (f.visible) c++;
        return Math.max(1,c);
    }

    private RectF innerTag(RectF outer){
        float pad = Math.max(8f,outer.width()*.045f);

        return new RectF(
                outer.left+pad,
                outer.top+pad,
                outer.right-pad,
                outer.bottom-pad
        );
    }

    private void layoutCards(RectF inner){
        int count = visibleCount();
        float gap = Math.max(5f,inner.height()*fieldGapPct);

        float cardH;
        if (autoHeight){
            cardH = (inner.height() - gap*Math.max(0,count-1))/count;
        } else {
            cardH = Math.max(54f,inner.height()*.18f);
        }

        int visibleIndex=0;

        for (LabelField f:fields){
            if (!f.visible) continue;

            f.x = 0f;
            f.w = 1f;
            f.y = (visibleIndex*(cardH+gap))/inner.height();
            f.h = cardH/inner.height();

            visibleIndex++;
        }
    }

    private RectF fieldRect(LabelField f, RectF inner){
        return new RectF(
                inner.left + f.x*inner.width(),
                inner.top + f.y*inner.height(),
                inner.left + (f.x+f.w)*inner.width(),
                inner.top + (f.y+f.h)*inner.height()
        );
    }

    private void drawProduct(Canvas c, Bitmap bmp, RectF dst){
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(Color.BLACK);
        c.drawRect(dst,bg);

        if (bmp == null) return;

        Rect src = sourceCropRect(bmp);
        c.drawBitmap(bmp,src,dst,null);
    }

    private Rect sourceCropRect(Bitmap bmp){
        int bw = bmp.getWidth();
        int bh = bmp.getHeight();

        if (!cropEnabled){
            return new Rect(0,0,bw,bh);
        }

        int l = Math.max(0,Math.min(bw-1,Math.round(cropLeft*bw)));
        int t = Math.max(0,Math.min(bh-1,Math.round(cropTop*bh)));
        int r = Math.max(l+1,Math.min(bw,Math.round(cropRight*bw)));
        int b = Math.max(t+1,Math.min(bh,Math.round(cropBottom*bh)));

        return new Rect(l,t,r,b);
    }

    private void drawOuterTag(Canvas c, RectF r){
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(outerTagColor);
        c.drawRoundRect(r,outerTagRadius,outerTagRadius,p);

        if (outerTagBorderWidth > 0){
            Paint b = new Paint(Paint.ANTI_ALIAS_FLAG);
            b.setStyle(Paint.Style.STROKE);
            b.setStrokeWidth(outerTagBorderWidth);
            b.setColor(outerTagBorderColor);
            c.drawRoundRect(r,outerTagRadius,outerTagRadius,b);
        }
    }

    private void drawCard(Canvas canvas, LabelField f, RectF r, boolean selected){
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(f.backgroundColor);
        canvas.drawRoundRect(r,f.cornerRadius,f.cornerRadius,bg);

        if (f.borderWidth > 0){
            Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(selected ? Math.max(3,f.borderWidth) : f.borderWidth);
            border.setColor(selected ? 0xFF2196F3 : f.borderColor);
            canvas.drawRoundRect(r,f.cornerRadius,f.cornerRadius,border);
        }

        float padH = Math.max(7f,f.paddingHorizontal);
        float padV = Math.max(5f,f.paddingVertical);
        float gap = Math.max(1f,f.titlePriceGap);

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(f.titleColor);
        title.setTypeface(f.getTitleTypeface());
        title.setTextSize(spToPx(f.titleSize));
        title.setTextAlign(alignFor(f.textAlign));

        Paint price = new Paint(Paint.ANTI_ALIAS_FLAG);
        price.setColor(f.priceColor);
        price.setTypeface(f.getPriceTypeface());
        price.setTextSize(spToPx(f.priceSize));
        price.setTextAlign(alignFor(f.textAlign));

        float anchorX = xForAlign(r,padH,f.textAlign);

        String titleText = safe(f.name);
        String priceText = safe(f.value);

        float maxW = r.width()-padH*2f;

        fitTextSize(title,titleText,maxW,9f);

        String displayPrice = priceText;
        if (f.showToman && !priceText.isEmpty()) displayPrice = priceText+" تومان";

        fitTextSize(price,displayPrice,maxW,11f);

        float titleH = f.showTitle && !titleText.isEmpty() ? (title.descent()-title.ascent()) : 0f;
        float priceH = f.showPrice && !displayPrice.isEmpty() ? (price.descent()-price.ascent()) : 0f;

        float totalH = titleH + (titleH>0 && priceH>0 ? gap : 0f) + priceH;
        float startY = r.centerY() - totalH/2f;

        if (titleH > 0){
            float baseline = startY - title.ascent();
            canvas.drawText(titleText,anchorX,baseline,title);
            startY += titleH + (priceH>0 ? gap : 0f);
        }

        if (priceH > 0){
            float baseline = startY - price.ascent();
            canvas.drawText(displayPrice,anchorX,baseline,price);

            if (f.strike){
                float tw = price.measureText(displayPrice);
                float y = baseline - price.getTextSize()*.34f;

                Paint s = new Paint(Paint.ANTI_ALIAS_FLAG);
                s.setColor(f.priceColor);
                s.setStrokeWidth(Math.max(2f,price.getTextSize()*.05f));

                if (f.textAlign == 0){
                    canvas.drawLine(anchorX-tw,y,anchorX,y,s);
                } else if (f.textAlign == 1){
                    canvas.drawLine(anchorX-tw/2f,y,anchorX+tw/2f,y,s);
                } else {
                    canvas.drawLine(anchorX,y,anchorX+tw,y,s);
                }
            }
        }
    }

    private void drawResizeHandle(Canvas c, RectF r){
        float radius = Math.max(14f,Math.min(r.width(),r.height())*.10f);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF1976D2);
        c.drawCircle(r.right,r.bottom,radius,p);

        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(3f);
        ring.setColor(Color.WHITE);
        c.drawCircle(r.right,r.bottom,radius*.72f,ring);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){
        if (previewLabelRect.width() <= 0 || previewLabelRect.height() <= 0) return true;

        RectF inner = innerTag(previewLabelRect);

        if (e.getAction() == MotionEvent.ACTION_DOWN){
            touchDownX = lastX = e.getX();
            touchDownY = lastY = e.getY();
            moved = false;
            resizing = false;
            selected = -1;

            for (int i=fields.size()-1;i>=0;i--){
                LabelField f = fields.get(i);
                if (!f.visible) continue;

                RectF r = fieldRect(f,inner);

                if (nearResizeHandle(r,e.getX(),e.getY())){
                    selected = i;
                    resizing = true;
                    break;
                }

                if (r.contains(e.getX(),e.getY())){
                    selected = i;
                    break;
                }
            }

            if (selected >= 0 && listener != null){
                listener.onFieldSelected(selected);
            }

            invalidate();
            return true;
        }

        if (e.getAction() == MotionEvent.ACTION_MOVE && selected >= 0){
            float dx = e.getX()-lastX;
            float dy = e.getY()-lastY;

            if (Math.abs(e.getX()-touchDownX)+Math.abs(e.getY()-touchDownY) > 8f){
                moved = true;
            }

            LabelField f = fields.get(selected);

            if (resizing){
                float newW = (e.getX()-inner.left)/inner.width() - f.x;
                float newH = (e.getY()-inner.top)/inner.height() - f.y;

                f.w = clamp(newW,.20f,1f-f.x);
                f.h = clamp(newH,.08f,1f-f.y);
            } else if (!autoHeight){
                f.x = clamp(f.x + dx/inner.width(),0f,1f-f.w);
                f.y = clamp(f.y + dy/inner.height(),0f,1f-f.h);
            }

            lastX = e.getX();
            lastY = e.getY();

            invalidate();
            return true;
        }

        if (e.getAction() == MotionEvent.ACTION_UP){
            if (selected >= 0 && !moved && !resizing && listener != null){
                RectF r = fieldRect(fields.get(selected),inner);
                int type = e.getY() < r.centerY() ? TEXT_TITLE : TEXT_PRICE;
                listener.onTextClicked(selected,type);
            }

            if (listener != null) listener.onChanged();

            resizing = false;
            invalidate();
            return true;
        }

        return true;
    }

    private boolean nearResizeHandle(RectF r, float x, float y){
        float d = Math.max(28f,Math.min(r.width(),r.height())*.18f);
        return Math.abs(x-r.right) <= d && Math.abs(y-r.bottom) <= d;
    }

    public Bitmap renderFinal(Bitmap source, int backgroundColor, int borderColor, boolean append, float widthPercent){
        if (source == null) return null;

        Bitmap cropped = cropBitmap(source);

        int sw = cropped.getWidth();
        int sh = cropped.getHeight();

        float safeWidth = Math.max(.20f,Math.min(widthPercent,.45f));
        int tagW = Math.max(230,Math.round(sw*safeWidth));

        int outW = append ? sw+tagW : sw;
        int outH = sh;

        Bitmap out = Bitmap.createBitmap(outW,outH,Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(backgroundColor);

        RectF productDst;

        if (append){
            productDst = new RectF(0,0,sw,sh);
        } else {
            productDst = new RectF(
                    productX*sw,
                    productY*sh,
                    (productX+productW)*sw,
                    (productY+productH)*sh
            );
        }

        canvas.drawBitmap(cropped,null,productDst,null);

        int count = visibleCount();

        float gap = Math.max(6f,sh*fieldGapPct);
        float cardH = Math.max(70f,sh*.105f);
        float outerPad = Math.max(10f,tagW*.045f);

        float tagH;
        if (autoHeight){
            tagH = outerPad*2f + count*cardH + Math.max(0,count-1)*gap;
        } else {
            tagH = sh*.72f;
        }

        tagH = Math.min(sh*.90f,Math.max(sh*.26f,tagH));

        float left;
        float right;

        if (append){
            left = sw + Math.max(6f,tagW*.03f);
            right = outW - Math.max(6f,tagW*.03f);
        } else {
            right = sw - Math.max(10f,sw*.018f);
            left = right - tagW;
        }

        float top = (sh-tagH)/2f;

        RectF tag = new RectF(left,top,right,top+tagH);

        Paint tagBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        tagBg.setColor(outerTagColor);
        canvas.drawRoundRect(tag,outerTagRadius,outerTagRadius,tagBg);

        if (outerTagBorderWidth > 0){
            Paint tagBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
            tagBorder.setStyle(Paint.Style.STROKE);
            tagBorder.setStrokeWidth(outerTagBorderWidth);
            tagBorder.setColor(outerTagBorderColor);
            canvas.drawRoundRect(tag,outerTagRadius,outerTagRadius,tagBorder);
        }

        RectF inner = new RectF(
                tag.left+outerPad,
                tag.top+outerPad,
                tag.right-outerPad,
                tag.bottom-outerPad
        );

        float available = inner.height() - gap*Math.max(0,count-1);
        float each = available/count;

        int index=0;

        for (LabelField f:fields){
            if (!f.visible) continue;

            RectF r = new RectF(
                    inner.left,
                    inner.top + index*(each+gap),
                    inner.right,
                    inner.top + index*(each+gap) + each
            );

            drawCard(canvas,f,r,false);
            index++;
        }

        if (cropped != source && !cropped.isRecycled()) cropped.recycle();

        return out;
    }

    private Bitmap cropBitmap(Bitmap source){
        if (!cropEnabled) return source;

        Rect r = sourceCropRect(source);

        return Bitmap.createBitmap(
                source,
                r.left,
                r.top,
                r.width(),
                r.height()
        );
    }

    private Paint.Align alignFor(int align){
        if (align == 1) return Paint.Align.CENTER;
        if (align == 2) return Paint.Align.LEFT;
        return Paint.Align.RIGHT;
    }

    private float xForAlign(RectF r, float pad, int align){
        if (align == 1) return r.centerX();
        if (align == 2) return r.left+pad;
        return r.right-pad;
    }

    private void fitTextSize(Paint p, String text, float maxW, float minPx){
        if (text == null || text.isEmpty()) return;

        float size = p.getTextSize();

        while (p.measureText(text) > maxW && size > minPx){
            size -= 1f;
            p.setTextSize(size);
        }
    }

    private float spToPx(float sp){
        return sp*getResources().getDisplayMetrics().scaledDensity;
    }

    private String safe(String s){
        return s == null ? "" : s;
    }

    private float clamp(float v, float min, float max){
        return Math.max(min,Math.min(max,v));
    }
}
