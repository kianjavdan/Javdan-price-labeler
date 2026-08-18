package com.javdan.pricelabeler;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import java.util.ArrayDeque;
import java.util.Arrays;

/** Shared Smart-Snap detector used by both Designer and ImagePreparationActivity. */
public final class ProductDetectionEngine {
    private ProductDetectionEngine() {}

    public static class Result {
        public final Rect rect;
        public final float confidence;
        public final boolean fallback;
        public Result(Rect rect, float confidence, boolean fallback) {
            this.rect = rect; this.confidence = confidence; this.fallback = fallback;
        }
    }

    public static Result detect(Bitmap bmp, int sensitivity, float paddingPct) {
        if (bmp == null || bmp.getWidth() < 8 || bmp.getHeight() < 8)
            return new Result(null, 0f, false);

        Result primary = borderColorDetector(bmp, sensitivity, paddingPct);
        if (primary.rect != null && primary.confidence >= 0.52f) return primary;

        Result component = componentDetector(bmp, sensitivity, paddingPct);
        if (component.rect != null) {
            if (primary.rect == null || component.confidence >= primary.confidence)
                return component;
        }
        return primary;
    }

    private static Result borderColorDetector(Bitmap bmp, int sensitivity, float paddingPct) {
        final int bw=bmp.getWidth(), bh=bmp.getHeight();
        int maxSide=Math.max(bw,bh);
        int step=Math.max(1,maxSide/420);
        long rs=0,gs=0,bs=0; int bgCount=0;
        int band=Math.max(2,Math.min(bw,bh)/50);

        for(int x=0;x<bw;x+=step){
            for(int y=0;y<band;y+=Math.max(1,step/2)){int c=bmp.getPixel(x,y);rs+=Color.red(c);gs+=Color.green(c);bs+=Color.blue(c);bgCount++;}
            for(int y=Math.max(0,bh-band);y<bh;y+=Math.max(1,step/2)){int c=bmp.getPixel(x,y);rs+=Color.red(c);gs+=Color.green(c);bs+=Color.blue(c);bgCount++;}
        }
        for(int y=band;y<bh-band;y+=step){
            for(int x=0;x<band;x+=Math.max(1,step/2)){int c=bmp.getPixel(x,y);rs+=Color.red(c);gs+=Color.green(c);bs+=Color.blue(c);bgCount++;}
            for(int x=Math.max(0,bw-band);x<bw;x+=Math.max(1,step/2)){int c=bmp.getPixel(x,y);rs+=Color.red(c);gs+=Color.green(c);bs+=Color.blue(c);bgCount++;}
        }
        if(bgCount==0) return new Result(null,0f,false);
        int br=(int)(rs/bgCount), bg=(int)(gs/bgCount), bb=(int)(bs/bgCount);

        // UI semantics: larger sensitivity => easier/stronger detection.
        int s=clamp(sensitivity,0,100);
        int threshold=115 - Math.round(s*0.80f); // 115 .. 35
        int threshold2=threshold*threshold;
        int minX=bw,minY=bh,maxX=-1,maxY=-1,fg=0,total=0;
        int halo=Math.max(step,Math.min(bw,bh)/250);
        for(int y=halo;y<bh-halo;y+=step){
            for(int x=halo;x<bw-halo;x+=step){
                int c=bmp.getPixel(x,y); total++;
                int dr=Color.red(c)-br,dg=Color.green(c)-bg,db=Color.blue(c)-bb;
                if(dr*dr+dg*dg+db*db > threshold2){
                    fg++; if(x<minX)minX=x;if(x>maxX)maxX=x;if(y<minY)minY=y;if(y>maxY)maxY=y;
                }
            }
        }
        if(fg<12||maxX<=minX||maxY<=minY) return new Result(null,0f,false);
        float boxArea=((maxX-minX+step)*(float)(maxY-minY+step))/(bw*(float)bh);
        float fgRatio=fg/(float)Math.max(1,total);
        float conf=clampf(.28f+(1f-boxArea)*.48f+Math.min(.24f,fgRatio*.38f),0f,1f);
        if(boxArea>.985f||fgRatio>.94f) conf=.22f;
        Rect r=pad(new Rect(minX-step,minY-step,maxX+step+1,maxY+step+1),bw,bh,paddingPct);
        if(r.width()<bw*.08f||r.height()<bh*.08f) return new Result(null,.20f,false);
        return new Result(r,conf,false);
    }

    /**
     * Fallback detector: creates a sampled foreground mask, groups connected components,
     * then prefers a large component near the image center while penalizing edge-touching noise.
     */
    private static Result componentDetector(Bitmap bmp, int sensitivity, float paddingPct) {
        final int w=bmp.getWidth(), h=bmp.getHeight();
        int step=Math.max(2,Math.max(w,h)/260);
        int gw=Math.max(1,(w+step-1)/step), gh=Math.max(1,(h+step-1)/step);
        if((long)gw*gh>120000) return new Result(null,0f,true);

        // Robust background estimate from many border samples using median RGB.
        int cap=2*(gw+gh)+16, n=0;
        int[] rr=new int[cap],gg=new int[cap],bb=new int[cap];
        for(int gx=0;gx<gw;gx++){
            int x=Math.min(w-1,gx*step);
            int c1=bmp.getPixel(x,0), c2=bmp.getPixel(x,h-1);
            rr[n]=Color.red(c1);gg[n]=Color.green(c1);bb[n++]=Color.blue(c1);
            rr[n]=Color.red(c2);gg[n]=Color.green(c2);bb[n++]=Color.blue(c2);
        }
        for(int gy=1;gy<gh-1;gy++){
            int y=Math.min(h-1,gy*step);
            int c1=bmp.getPixel(0,y),c2=bmp.getPixel(w-1,y);
            rr[n]=Color.red(c1);gg[n]=Color.green(c1);bb[n++]=Color.blue(c1);
            rr[n]=Color.red(c2);gg[n]=Color.green(c2);bb[n++]=Color.blue(c2);
        }
        rr=Arrays.copyOf(rr,n);gg=Arrays.copyOf(gg,n);bb=Arrays.copyOf(bb,n);
        Arrays.sort(rr);Arrays.sort(gg);Arrays.sort(bb);
        int br=rr[n/2], bg=gg[n/2], bl=bb[n/2];

        int s=clamp(sensitivity,0,100);
        int threshold=105-Math.round(s*.70f); // 105..35
        int t2=threshold*threshold;
        boolean[] mask=new boolean[gw*gh];
        for(int gy=0;gy<gh;gy++){
            int y=Math.min(h-1,gy*step);
            for(int gx=0;gx<gw;gx++){
                int x=Math.min(w-1,gx*step), c=bmp.getPixel(x,y);
                int dr=Color.red(c)-br,dg=Color.green(c)-bg,db=Color.blue(c)-bl;
                int lum=(Color.red(c)+Color.green(c)+Color.blue(c))/3;
                int bgl=(br+bg+bl)/3;
                boolean diff=(dr*dr+dg*dg+db*db)>t2 || Math.abs(lum-bgl)>threshold;
                mask[gy*gw+gx]=diff;
            }
        }

        boolean[] seen=new boolean[mask.length];
        int[] qx={-1,0,1,-1,1,-1,0,1};
        int[] qy={-1,-1,-1,0,0,1,1,1};
        Rect best=null; float bestScore=-1,bestConf=0;
        float cx=(gw-1)/2f, cy=(gh-1)/2f;

        for(int i=0;i<mask.length;i++){
            if(!mask[i]||seen[i]) continue;
            ArrayDeque<Integer> q=new ArrayDeque<>(); q.add(i); seen[i]=true;
            int count=0,minX=gw,minY=gh,maxX=-1,maxY=-1,edge=0;
            double sx=0,sy=0;
            while(!q.isEmpty()){
                int v=q.removeFirst(),x=v%gw,y=v/gw;count++;sx+=x;sy+=y;
                if(x<minX)minX=x;if(x>maxX)maxX=x;if(y<minY)minY=y;if(y>maxY)maxY=y;
                if(x==0||y==0||x==gw-1||y==gh-1) edge++;
                for(int k=0;k<8;k++){
                    int nx=x+qx[k],ny=y+qy[k];
                    if(nx<0||ny<0||nx>=gw||ny>=gh) continue;
                    int nv=ny*gw+nx;
                    if(mask[nv]&&!seen[nv]){seen[nv]=true;q.add(nv);}
                }
            }
            if(count<8) continue;
            float area=count/(float)(gw*gh);
            if(area<.004f) continue;
            float ccx=(float)(sx/count),ccy=(float)(sy/count);
            float dx=(ccx-cx)/Math.max(1f,cx),dy=(ccy-cy)/Math.max(1f,cy);
            float center=1f-Math.min(1f,(float)Math.sqrt(dx*dx+dy*dy));
            float edgeRatio=edge/(float)count;
            float boxArea=((maxX-minX+1)*(float)(maxY-minY+1))/(gw*(float)gh);
            float fill=count/(float)Math.max(1,(maxX-minX+1)*(maxY-minY+1));
            float score=area*2.1f + center*.55f + fill*.30f - edgeRatio*.85f - (boxArea>.92f?.6f:0f);
            if(score>bestScore){
                bestScore=score;
                int l=minX*step,t=minY*step,r=Math.min(w,(maxX+1)*step),b=Math.min(h,(maxY+1)*step);
                best=new Rect(l,t,r,b);
                bestConf=clampf(.38f+Math.min(.34f,area*1.8f)+center*.18f+fill*.12f-edgeRatio*.22f,0f,.92f);
            }
        }
        if(best==null) return new Result(null,0f,true);
        best=pad(best,w,h,paddingPct);
        if(best.width()<w*.07f||best.height()<h*.07f) return new Result(null,.22f,true);
        return new Result(best,bestConf,true);
    }

    private static Rect pad(Rect r,int w,int h,float pct){
        int px=Math.round(Math.max(0f,pct)*r.width());
        int py=Math.round(Math.max(0f,pct)*r.height());
        return new Rect(clamp(r.left-px,0,w-1),clamp(r.top-py,0,h-1),clamp(r.right+px,1,w),clamp(r.bottom+py,1,h));
    }
    private static int clamp(int v,int a,int b){return Math.max(a,Math.min(b,v));}
    private static float clampf(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
