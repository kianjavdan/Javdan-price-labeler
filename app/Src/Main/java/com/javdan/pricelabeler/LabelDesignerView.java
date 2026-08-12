package com.javdan.pricelabeler;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class LabelDesignerView extends View {

    public static final int TEXT_TITLE = 0;
    public static final int TEXT_PRICE = 1;
    public static final int TEXT_TOMAN = 2;

    public interface Listener {
        void onFieldSelected(int index);
        void onChanged();
        void onTextClicked(int fieldIndex, int textType);
    }

    private ArrayList<LabelField> fields = new ArrayList<>();
    private Bitmap productBitmap;
    private int selected = -1;
    private Listener listener;

    private final RectF previewLabelRect = new RectF();
    private float downX, downY, startX, startY;
    private boolean draggingField = false;
    private boolean resizingField = false;
    private int activeTextType = -1;

    public float productX = 0.02f;
    public float productY = 0.08f;
    public float productW = 0.61f;
    public float productH = 0.84f;
    public int canvasBackground = 0xFFF2F2F2;

    public boolean cropEnabled = false;
    public float cropLeft = 0f;
    public float cropTop = 0f;
    public float cropRight = 1f;
    public float cropBottom = 1f;

    public int outerTagColor = 0xFF171717;
    public int outerTagBorderColor = 0xFF333333;
    public int outerTagBorderWidth = 2;
    public int outerTagRadius = 24;
    public float labelWidthPct = 0.31f;

    public LabelDesignerView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setListener(Listener l) { listener = l; }
    public void setFields(ArrayList<LabelField> f) { fields = f == null ? new ArrayList<>() : f; invalidate(); }
    public ArrayList<LabelField> getFields() { return fields; }
    public void setProductBitmap(Bitmap b) { productBitmap = b; invalidate(); }
    public Bitmap getProductBitmap() { return productBitmap; }
    public void select(int index) { selected = index; invalidate(); }
    public int getSelected() { return selected; }

    public void resetCrop() {
        cropLeft = 0f; cropTop = 0f; cropRight = 1f; cropBottom = 1f;
        cropEnabled = false;
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

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(canvasBackground);

        float w = getWidth(), h = getHeight();
        RectF productRect = new RectF(productX*w, productY*h, (productX+productW)*w, (productY+productH)*h);
        drawProduct(canvas, productBitmap, productRect);

        float labelW = Math.max(w*.26f, w*labelWidthPct);
        float tagH = computeTagHeight(h);
        float top = (h-tagH)/2f;
        previewLabelRect.set(w-labelW-w*.015f, top, w-w*.015f, top+tagH);
        drawOuterTag(canvas, previewLabelRect);

        RectF inner = getInnerTagRect(previewLabelRect);
        layoutFieldsInside(inner);
        for (int i=0;i<fields.size();i++) {
            LabelField f=fields.get(i);
            if(!f.visible) continue;
            RectF r=fieldRect(f,inner);
            drawFieldCard(canvas,f,r,i==selected);
        }
        if(selected>=0 && selected<fields.size()) drawResizeHandle(canvas, fieldRect(fields.get(selected),inner));
    }

    private float computeTagHeight(float h) {
        int count=0; for(LabelField f:fields) if(f.visible) count++;
        count=Math.max(1,count);
        float fieldH=h*.135f;
        float gap=h*.012f;
        float pad=h*.025f;
        float total=pad*2 + count*fieldH + Math.max(0,count-1)*gap;
        return Math.min(h*.78f, Math.max(h*.28f,total));
    }

    private RectF getInnerTagRect(RectF outer) {
        float pad=Math.max(8f,outer.width()*.055f);
        return new RectF(outer.left+pad,outer.top+pad,outer.right-pad,outer.bottom-pad);
    }

    private void layoutFieldsInside(RectF inner) {
        int n=Math.max(1,fields.size());
        float gap=Math.max(6f, inner.height()*.014f);
        float each=(inner.height()-gap*Math.max(0,n-1))/n;
        for(int i=0;i<fields.size();i++){
            LabelField f=fields.get(i);
            f.x=0f; f.w=1f;
            f.y=(i*(each+gap))/inner.height();
            f.h=each/inner.height();
        }
    }

    private void drawProduct(Canvas c, Bitmap bmp, RectF dst) {
        Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG); bg.setColor(Color.WHITE); c.drawRoundRect(dst,18,18,bg);
        if(bmp==null) return;
        c.drawBitmap(bmp, sourceCropRect(bmp), dst, null);
    }

    private Rect sourceCropRect(Bitmap bmp) {
        int bw=bmp.getWidth(), bh=bmp.getHeight();
        if(!cropEnabled) return new Rect(0,0,bw,bh);
        int l=Math.max(0,Math.min(bw-1,Math.round(cropLeft*bw)));
        int t=Math.max(0,Math.min(bh-1,Math.round(cropTop*bh)));
        int r=Math.max(l+1,Math.min(bw,Math.round(cropRight*bw)));
        int b=Math.max(t+1,Math.min(bh,Math.round(cropBottom*bh)));
        return new Rect(l,t,r,b);
    }

    private void drawOuterTag(Canvas c,RectF r){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(outerTagColor); c.drawRoundRect(r,outerTagRadius,outerTagRadius,p);
        if(outerTagBorderWidth>0){
            Paint b=new Paint(Paint.ANTI_ALIAS_FLAG); b.setStyle(Paint.Style.STROKE); b.setStrokeWidth(outerTagBorderWidth); b.setColor(outerTagBorderColor);
            c.drawRoundRect(r,outerTagRadius,outerTagRadius,b);
        }
    }

    private RectF fieldRect(LabelField f,RectF inner){
        return new RectF(inner.left+f.x*inner.width(), inner.top+f.y*inner.height(), inner.left+(f.x+f.w)*inner.width(), inner.top+(f.y+f.h)*inner.height());
    }

    private void drawFieldCard(Canvas canvas, LabelField f, RectF r, boolean selectedNow) {
        Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG); bg.setColor(f.backgroundColor); canvas.drawRoundRect(r,f.cornerRadius,f.cornerRadius,bg);
        if(f.borderWidth>0){
            Paint b=new Paint(Paint.ANTI_ALIAS_FLAG); b.setStyle(Paint.Style.STROKE); b.setStrokeWidth(selectedNow?Math.max(3,f.borderWidth):f.borderWidth); b.setColor(selectedNow?0xFF2196F3:f.borderColor);
            canvas.drawRoundRect(r,f.cornerRadius,f.cornerRadius,b);
        }

        float padH=Math.max(8f,f.paddingHorizontal), padV=Math.max(5f,f.paddingVertical);
        Paint title=new Paint(Paint.ANTI_ALIAS_FLAG); title.setColor(f.titleColor); title.setTypeface(f.getTitleTypeface()); title.setTextSize(spToPx(f.titleSize)); title.setTextAlign(alignFor(f.textAlign));
        Paint price=new Paint(Paint.ANTI_ALIAS_FLAG); price.setColor(f.priceColor); price.setTypeface(f.getPriceTypeface()); price.setTextSize(spToPx(f.priceSize)); price.setTextAlign(alignFor(f.textAlign));

        float x=xForAlign(r,padH,f.textAlign);
        String titleText=safe(f.name), value=safe(f.value);
        float titleY=r.top+padV-title.ascent();

        if(f.showTitle && !titleText.isEmpty()){
            fitTextSize(title,titleText,r.width()-padH*2,9f);
            canvas.drawText(titleText,x,titleY,title);
        }

        if(f.showPrice && !value.isEmpty()){
            String display=f.showToman ? value+" تومان" : value;
            float priceY=titleY + (f.showTitle?Math.max(2,f.titlePriceGap):0) + Math.max(price.getTextSize(),18f);
            fitTextSize(price,display,r.width()-padH*2,12f);
            canvas.drawText(display,x,priceY,price);

            if(f.strike){
                float tw=price.measureText(display), y=priceY-price.getTextSize()*.34f;
                Paint s=new Paint(Paint.ANTI_ALIAS_FLAG); s.setColor(f.priceColor); s.setStrokeWidth(Math.max(2f,price.getTextSize()*.05f));
                if(f.textAlign==0) canvas.drawLine(x-tw,y,x,y,s);
                else if(f.textAlign==1) canvas.drawLine(x-tw/2,y,x+tw/2,y,s);
                else canvas.drawLine(x,y,x+tw,y,s);
            }
        }
    }

    private void drawResizeHandle(Canvas c,RectF r){
        float rad=Math.max(13f,Math.min(r.width(),r.height())*.10f);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(0xFF1976D2); c.drawCircle(r.right,r.bottom,rad,p);
        Paint q=new Paint(Paint.ANTI_ALIAS_FLAG); q.setStyle(Paint.Style.STROKE); q.setStrokeWidth(3); q.setColor(Color.WHITE); c.drawCircle(r.right,r.bottom,rad*.70f,q);
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(previewLabelRect.width()<=0) return true;
        RectF inner=getInnerTagRect(previewLabelRect);

        if(e.getAction()==MotionEvent.ACTION_DOWN){
            downX=startX=e.getX(); downY=startY=e.getY(); selected=-1; draggingField=false; resizingField=false; activeTextType=-1;
            for(int i=fields.size()-1;i>=0;i--){
                RectF r=fieldRect(fields.get(i),inner);
                if(nearResizeHandle(r,e.getX(),e.getY())){selected=i; resizingField=true; break;}
                if(r.contains(e.getX(),e.getY())){selected=i; draggingField=true; activeTextType=detectTextType(r,e.getY()); break;}
            }
            if(selected>=0 && listener!=null) listener.onFieldSelected(selected);
            invalidate(); return true;
        }

        if(e.getAction()==MotionEvent.ACTION_MOVE && selected>=0){
            LabelField f=fields.get(selected);
            if(resizingField){
                float newW=(e.getX()-inner.left)/inner.width()-f.x;
                float newH=(e.getY()-inner.top)/inner.height()-f.y;
                f.w=clamp(newW,.20f,1f-f.x); f.h=clamp(newH,.08f,1f-f.y);
            }else if(draggingField){
                float dx=(e.getX()-downX)/inner.width(), dy=(e.getY()-downY)/inner.height();
                f.x=clamp(f.x+dx,0f,1f-f.w); f.y=clamp(f.y+dy,0f,1f-f.h); downX=e.getX(); downY=e.getY();
            }
            invalidate(); return true;
        }

        if(e.getAction()==MotionEvent.ACTION_UP){
            float move=Math.abs(e.getX()-startX)+Math.abs(e.getY()-startY);
            if(selected>=0 && draggingField && move<18f && activeTextType>=0 && listener!=null) listener.onTextClicked(selected,activeTextType);
            draggingField=false; resizingField=false; activeTextType=-1;
            if(listener!=null) listener.onChanged(); invalidate(); return true;
        }
        return true;
    }

    private int detectTextType(RectF r,float y){ return y < r.top+r.height()*.45f ? TEXT_TITLE : TEXT_PRICE; }
    private boolean nearResizeHandle(RectF r,float x,float y){ float d=Math.max(28f,Math.min(r.width(),r.height())*.18f); return Math.abs(x-r.right)<=d && Math.abs(y-r.bottom)<=d; }

    public Bitmap renderFinal(Bitmap source,int backgroundColor,int borderColor,boolean append,float widthPercent){
        if(source==null) return null;
        Bitmap cropped=cropBitmap(source);
        int sw=cropped.getWidth(), sh=cropped.getHeight();
        float safeWidth=Math.max(.22f,Math.min(widthPercent,.40f));
        int tagW=Math.max(250,Math.round(sw*safeWidth));
        int outW=append?sw+tagW:sw, outH=sh;
        Bitmap out=Bitmap.createBitmap(outW,outH,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(out); canvas.drawColor(backgroundColor);

        RectF productDst=append?new RectF(0,0,sw,sh):new RectF(productX*sw,productY*sh,(productX+productW)*sw,(productY+productH)*sh);
        canvas.drawBitmap(cropped,null,productDst,null);

        int count=0; for(LabelField f:fields) if(f.visible) count++; count=Math.max(1,count);
        float fieldH=Math.max(78f,sh*.115f), gap=Math.max(7f,sh*.010f), pad=Math.max(12f,tagW*.055f);
        float tagH=Math.min(sh*.78f,pad*2+count*fieldH+Math.max(0,count-1)*gap);
        float left=append?sw+Math.max(6f,tagW*.03f):sw-tagW-Math.max(12f,sw*.02f);
        float right=append?outW-Math.max(6f,tagW*.03f):sw-Math.max(12f,sw*.02f);
        float top=(sh-tagH)/2f;
        RectF tag=new RectF(left,top,right,top+tagH);
        drawOuterTag(canvas,tag);

        RectF inner=new RectF(tag.left+pad,tag.top+pad,tag.right-pad,tag.bottom-pad);
        float each=(inner.height()-gap*Math.max(0,count-1))/count;
        int idx=0;
        for(LabelField f:fields){
            if(!f.visible) continue;
            RectF r=new RectF(inner.left,inner.top+idx*(each+gap),inner.right,inner.top+idx*(each+gap)+each);
            drawFieldCard(canvas,f,r,false); idx++;
        }

        if(cropped!=source && !cropped.isRecycled()) cropped.recycle();
        return out;
    }

    private Bitmap cropBitmap(Bitmap source){
        if(!cropEnabled) return source;
        Rect r=sourceCropRect(source);
        return Bitmap.createBitmap(source,r.left,r.top,r.width(),r.height());
    }

    private Paint.Align alignFor(int a){ if(a==1)return Paint.Align.CENTER; if(a==2)return Paint.Align.LEFT; return Paint.Align.RIGHT; }
    private float xForAlign(RectF r,float pad,int a){ if(a==1)return r.centerX(); if(a==2)return r.left+pad; return r.right-pad; }
    private void fitTextSize(Paint p,String text,float max,float min){ if(text==null||text.isEmpty())return; float s=p.getTextSize(); while(p.measureText(text)>max&&s>min){s-=1;p.setTextSize(s);} }
    private float spToPx(float sp){ return sp*getResources().getDisplayMetrics().scaledDensity; }
    private String safe(String s){ return s==null?"":s; }
    private float clamp(float v,float min,float max){ return Math.max(min,Math.min(max,v)); }
}
