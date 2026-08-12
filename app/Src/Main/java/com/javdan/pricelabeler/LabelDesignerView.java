package com.javdan.pricelabeler;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import java.util.*;

public class LabelDesignerView extends View {
    public interface Listener { void onFieldSelected(int index); void onChanged(); }
    private ArrayList<LabelField> fields = new ArrayList<>();
    private Bitmap productBitmap;
    private int selected = -1;
    private float downX, downY;
    private RectF labelRect = new RectF();
    private Listener listener;
    public boolean appendMode = false;
    public int labelBg = Color.WHITE;
    public int labelBorder = 0xFFD8D8D8;
    public float labelWidthPct = .38f;

    public LabelDesignerView(Context c) { super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null); }

    public void setListener(Listener l){ listener=l; }
    public void setFields(ArrayList<LabelField> f){ fields=f; invalidate(); }
    public ArrayList<LabelField> getFields(){ return fields; }
    public void setProductBitmap(Bitmap b){ productBitmap=b; invalidate(); }
    public void select(int i){ selected=i; invalidate(); }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        c.drawColor(0xFFF0F0F0);
        float W=getWidth(), H=getHeight();
        if(productBitmap!=null){
            Rect src=new Rect(0,0,productBitmap.getWidth(),productBitmap.getHeight());
            RectF dst=new RectF(12,H*.08f,W*.57f,H*.92f);
            c.drawBitmap(productBitmap,src,dst,null);
        } else {
            Paint p=new Paint(); p.setColor(Color.WHITE);
            c.drawRect(12,H*.08f,W*.57f,H*.92f,p);
        }
        labelRect.set(W*.61f,H*.08f,W-12,H*.92f);
        Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG); bg.setColor(labelBg);
        c.drawRoundRect(labelRect,22,22,bg);
        Paint border=new Paint(Paint.ANTI_ALIAS_FLAG); border.setStyle(Paint.Style.STROKE); border.setStrokeWidth(3); border.setColor(labelBorder);
        c.drawRoundRect(labelRect,22,22,border);

        for(int i=0;i<fields.size();i++){
            LabelField f=fields.get(i);
            RectF r=fieldRect(f);
            Paint box=new Paint(); box.setStyle(Paint.Style.STROKE); box.setStrokeWidth(i==selected?4:2);
            box.setColor(i==selected?0xFF1976D2:0xFFBBBBBB);
            c.drawRect(r,box);

            Paint title=new Paint(Paint.ANTI_ALIAS_FLAG); title.setColor(f.titleColor); title.setTextSize(dp(f.titleSize));
            title.setTextAlign(Paint.Align.RIGHT); title.setTypeface(Typeface.DEFAULT);
            Paint price=new Paint(Paint.ANTI_ALIAS_FLAG); price.setColor(f.priceColor); price.setTextSize(dp(f.priceSize));
            price.setTextAlign(Paint.Align.RIGHT); price.setTypeface(Typeface.DEFAULT_BOLD);
            float tx=r.right-dp(6);
            float y=r.top+dp(f.titleSize+2);
            c.drawText(f.name,tx,y,title);
            String val=(f.value==null||f.value.isEmpty())?"123,456 تومان":f.value+" تومان";
            float py=y+dp(f.priceSize+4);
            c.drawText(val,tx,py,price);
            if(f.strike){
                float width=price.measureText(val);
                c.drawLine(tx-width,py-dp(f.priceSize*.32f),tx,py-dp(f.priceSize*.32f),price);
            }
        }
    }

    private RectF fieldRect(LabelField f){
        float lw=labelRect.width(), lh=labelRect.height();
        float l=labelRect.left+f.x*lw, t=labelRect.top+f.y*lh;
        return new RectF(l,t,l+f.w*lw,t+f.h*lh);
    }

    @Override public boolean onTouchEvent(android.view.MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN){
            selected=-1;
            for(int i=fields.size()-1;i>=0;i--){
                if(fieldRect(fields.get(i)).contains(e.getX(),e.getY())){ selected=i; break; }
            }
            downX=e.getX(); downY=e.getY();
            if(listener!=null && selected>=0) listener.onFieldSelected(selected);
            invalidate(); return true;
        }
        if(e.getAction()==MotionEvent.ACTION_MOVE && selected>=0){
            LabelField f=fields.get(selected);
            float dx=(e.getX()-downX)/labelRect.width(), dy=(e.getY()-downY)/labelRect.height();
            f.x=Math.max(0,Math.min(1-f.w,f.x+dx)); f.y=Math.max(0,Math.min(1-f.h,f.y+dy));
            downX=e.getX(); downY=e.getY(); invalidate(); return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP){
            if(listener!=null) listener.onChanged();
            return true;
        }
        return true;
    }

    private float dp(float v){ return v*getResources().getDisplayMetrics().scaledDensity; }

    public Bitmap renderFinal(Bitmap source, int bgColor, int borderColor, boolean append, float widthPct){
        int sw=source.getWidth(), sh=source.getHeight();
        int lw=Math.max(260,(int)(sw*widthPct));
        int margin=Math.max(12,sw/60);
        Bitmap out;
        RectF box;
        if(append){
            out=Bitmap.createBitmap(sw+lw,sh,Bitmap.Config.ARGB_8888);
            Canvas c=new Canvas(out); c.drawColor(bgColor); c.drawBitmap(source,0,0,null);
            box=new RectF(sw,0,sw+lw-1,sh-1);
        } else {
            out=source.copy(Bitmap.Config.ARGB_8888,true);
            box=new RectF(sw-lw-margin,margin,sw-margin,sh-margin);
        }
        Canvas c=new Canvas(out);
        Paint bg=new Paint(Paint.ANTI_ALIAS_FLAG); bg.setColor(bgColor);
        c.drawRoundRect(box,24,24,bg);
        Paint b=new Paint(Paint.ANTI_ALIAS_FLAG); b.setStyle(Paint.Style.STROKE); b.setStrokeWidth(3); b.setColor(borderColor);
        c.drawRoundRect(box,24,24,b);

        for(LabelField f:fields){
            float l=box.left+f.x*box.width(), t=box.top+f.y*box.height();
            float r=l+f.w*box.width();
            Paint title=new Paint(Paint.ANTI_ALIAS_FLAG); title.setColor(f.titleColor); title.setTextSize(spToPx(f.titleSize));
            title.setTextAlign(Paint.Align.RIGHT);
            Paint price=new Paint(Paint.ANTI_ALIAS_FLAG); price.setColor(f.priceColor); price.setTextSize(spToPx(f.priceSize));
            price.setTextAlign(Paint.Align.RIGHT); price.setTypeface(Typeface.DEFAULT_BOLD);
            float tx=r-10;
            float ty=t+spToPx(f.titleSize+2);
            c.drawText(f.name,tx,ty,title);
            String val=(f.value==null?"":f.value);
            if(!val.isEmpty()){
                String text=val+" تومان";
                float py=ty+spToPx(f.priceSize+4);
                c.drawText(text,tx,py,price);
                if(f.strike){
                    float ww=price.measureText(text);
                    c.drawLine(tx-ww,py-spToPx(f.priceSize*.32f),tx,py-spToPx(f.priceSize*.32f),price);
                }
            }
        }
        return out;
    }

    private float spToPx(float sp){ return sp*getResources().getDisplayMetrics().scaledDensity; }
}
