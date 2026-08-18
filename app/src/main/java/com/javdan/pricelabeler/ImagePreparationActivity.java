package com.javdan.pricelabeler;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.view.*;import android.widget.*;import java.io.*;

public class ImagePreparationActivity extends Activity {
 public static final String EXTRA_SOURCE_PATH="source_path", EXTRA_PREPARED_PATH="prepared_path";
 Bitmap original,working; CropView view; SeekBar padding,sensitivity; TextView info;
 int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 @Override public void onCreate(Bundle b){super.onCreate(b); String p=getIntent().getStringExtra(EXTRA_SOURCE_PATH); original=BitmapFactory.decodeFile(p); if(original==null){finish();return;} working=original; build();}
 void build(){ LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(12),dp(12),dp(12));
  TextView title=new TextView(this);title.setText("آماده‌سازی تصویر محصول");title.setTextSize(22);title.setGravity(Gravity.CENTER);title.setPadding(0,dp(8),0,dp(8));root.addView(title);
  view=new CropView(this);view.setBitmap(working);root.addView(view,new LinearLayout.LayoutParams(-1,0,1));
  info=new TextView(this);info.setText("گوشه‌های کادر را بکشید؛ سپس Crop یا Snap را اعمال کنید.");info.setGravity(Gravity.CENTER);root.addView(info);
  LinearLayout r1=row(); addBtn(r1,"↺ چپ",v->rotate(-90));addBtn(r1,"راست ↻",v->rotate(90));addBtn(r1,"Reset",v->reset());root.addView(r1);
  LinearLayout r2=row();addBtn(r2,"✂ Crop",v->crop());addBtn(r2,"Smart Snap",v->smartSnap());addBtn(r2,"اصل / ویرایش",v->toggle());root.addView(r2);
  TextView pl=new TextView(this);pl.setText("Padding دور محصول: 4%");root.addView(pl);padding=new SeekBar(this);padding.setMax(30);padding.setProgress(4);padding.setOnSeekBarChangeListener(listener(pl,"Padding دور محصول: ","%"));root.addView(padding);
  TextView sl=new TextView(this);sl.setText("حساسیت تشخیص: 62");root.addView(sl);sensitivity=new SeekBar(this);sensitivity.setMax(100);sensitivity.setProgress(62);sensitivity.setOnSeekBarChangeListener(listener(sl,"حساسیت تشخیص: ",""));root.addView(sensitivity);
  LinearLayout bottom=row();addBtn(bottom,"انصراف",v->finish());addBtn(bottom,"بازنشانی",v->reset());addBtn(bottom,"✓ استفاده از این تصویر",v->accept());root.addView(bottom);
  setContentView(root);
 }
 SeekBar.OnSeekBarChangeListener listener(TextView t,String a,String z){return new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){t.setText(a+p+z);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};}
 LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER);return r;}
 void addBtn(LinearLayout r,String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(l);r.addView(b,new LinearLayout.LayoutParams(0,dp(58),1));}
 void rotate(float d){Matrix m=new Matrix();m.postRotate(d);working=Bitmap.createBitmap(working,0,0,working.getWidth(),working.getHeight(),m,true);view.setBitmap(working);}
 void reset(){working=original;view.setBitmap(working);}
 void toggle(){view.setBitmap(view.bmp==original?working:original);}
 void crop(){RectF n=view.cropNormalized();int l=(int)(n.left*working.getWidth()),t=(int)(n.top*working.getHeight()),r=(int)(n.right*working.getWidth()),b=(int)(n.bottom*working.getHeight());l=Math.max(0,l);t=Math.max(0,t);r=Math.min(working.getWidth(),r);b=Math.min(working.getHeight(),b);if(r-l>20&&b-t>20){working=Bitmap.createBitmap(working,l,t,r-l,b-t);view.setBitmap(working);}}
 void smartSnap(){int w=working.getWidth(),h=working.getHeight(),step=Math.max(1,Math.min(w,h)/350);int bg=avgCorners(working);int threshold=20+(100-sensitivity.getProgress());int minX=w,minY=h,maxX=0,maxY=0;for(int y=0;y<h;y+=step)for(int x=0;x<w;x+=step){if(dist(working.getPixel(x,y),bg)>threshold){minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}}if(maxX<=minX||maxY<=minY){Toast.makeText(this,"محصول تشخیص داده نشد؛ Crop دستی را استفاده کنید",Toast.LENGTH_LONG).show();return;}float p=padding.getProgress()/100f;int px=(int)((maxX-minX)*p),py=(int)((maxY-minY)*p);minX=Math.max(0,minX-px);maxX=Math.min(w,maxX+px);minY=Math.max(0,minY-py);maxY=Math.min(h,maxY+py);view.setCropNormalized(new RectF(minX/(float)w,minY/(float)h,maxX/(float)w,maxY/(float)h));info.setText("محدوده Smart Snap آماده است؛ برای اعمال، Crop را بزنید.");}
 int avgCorners(Bitmap b){int w=b.getWidth(),h=b.getHeight(),s=Math.max(2,Math.min(w,h)/30);long rr=0,gg=0,bb=0,n=0;int[][] q={{0,0},{w-s,0},{0,h-s},{w-s,h-s}};for(int[] a:q)for(int y=a[1];y<Math.min(h,a[1]+s);y+=2)for(int x=a[0];x<Math.min(w,a[0]+s);x+=2){int c=b.getPixel(x,y);rr+=Color.red(c);gg+=Color.green(c);bb+=Color.blue(c);n++;}return Color.rgb((int)(rr/n),(int)(gg/n),(int)(bb/n));}
 int dist(int a,int b){return (Math.abs(Color.red(a)-Color.red(b))+Math.abs(Color.green(a)-Color.green(b))+Math.abs(Color.blue(a)-Color.blue(b)))/3;}
 void accept(){
  try{
    File pictures=getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
    if(pictures==null) throw new IOException("حافظه تصاویر برنامه در دسترس نیست");

    File d=new File(pictures,"camera_capture");
    if(!d.exists() && !d.mkdirs()) throw new IOException("پوشه خروجی تصویر ساخته نشد");

    File f=new File(d,"prepared_"+System.currentTimeMillis()+".jpg");
    try(FileOutputStream o=new FileOutputStream(f)){
      if(!working.compress(Bitmap.CompressFormat.JPEG,96,o))
        throw new IOException("فشرده‌سازی JPEG ناموفق بود");
      o.flush();
    }

    if(!f.exists() || f.length()<=0)
      throw new IOException("فایل تصویر آماده‌شده ساخته نشد");

    Intent i=new Intent();
    i.putExtra(EXTRA_PREPARED_PATH,f.getAbsolutePath());
    setResult(RESULT_OK,i);
    finish();
  }catch(Exception e){
    Toast.makeText(this,"ذخیره تصویر ناموفق بود: "+e.getMessage(),Toast.LENGTH_LONG).show();
  }
}
 static class CropView extends View {Bitmap bmp;Paint p=new Paint(3);RectF crop=new RectF(.05f,.05f,.95f,.95f),dst=new RectF();float sx,sy;int handle=-1;CropView(android.content.Context c){super(c);p.setStrokeWidth(5);}
  void setBitmap(Bitmap b){bmp=b;crop.set(.05f,.05f,.95f,.95f);invalidate();}void setCropNormalized(RectF r){crop.set(r);invalidate();}RectF cropNormalized(){return new RectF(crop);}
  protected void onDraw(Canvas c){super.onDraw(c);if(bmp==null)return;float sc=Math.min(getWidth()/(float)bmp.getWidth(),getHeight()/(float)bmp.getHeight());float dw=bmp.getWidth()*sc,dh=bmp.getHeight()*sc;dst.set((getWidth()-dw)/2,(getHeight()-dh)/2,(getWidth()+dw)/2,(getHeight()+dh)/2);c.drawBitmap(bmp,null,dst,p);p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(0,180,160));RectF q=screenCrop();c.drawRect(q,p);p.setStyle(Paint.Style.FILL);for(float[] z:new float[][]{{q.left,q.top},{q.right,q.top},{q.left,q.bottom},{q.right,q.bottom}})c.drawCircle(z[0],z[1],16,p);}
  RectF screenCrop(){return new RectF(dst.left+crop.left*dst.width(),dst.top+crop.top*dst.height(),dst.left+crop.right*dst.width(),dst.top+crop.bottom*dst.height());}
  public boolean onTouchEvent(android.view.MotionEvent e){RectF q=screenCrop();if(e.getAction()==0){float[][] a={{q.left,q.top},{q.right,q.top},{q.left,q.bottom},{q.right,q.bottom}};float best=80;for(int i=0;i<4;i++){float d=(float)Math.hypot(e.getX()-a[i][0],e.getY()-a[i][1]);if(d<best){best=d;handle=i;}}sx=e.getX();sy=e.getY();return true;}if(e.getAction()==2&&handle>=0){float nx=Math.max(0,Math.min(1,(e.getX()-dst.left)/dst.width())),ny=Math.max(0,Math.min(1,(e.getY()-dst.top)/dst.height()));if(handle==0){crop.left=nx;crop.top=ny;}if(handle==1){crop.right=nx;crop.top=ny;}if(handle==2){crop.left=nx;crop.bottom=ny;}if(handle==3){crop.right=nx;crop.bottom=ny;}if(crop.right<crop.left+.03f)crop.right=crop.left+.03f;if(crop.bottom<crop.top+.03f)crop.bottom=crop.top+.03f;invalidate();return true;}if(e.getAction()==1){handle=-1;return true;}return true;}
 }
}
