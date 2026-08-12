package com.javdan.pricelabeler;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class MainActivity extends Activity {

    static final int PICK_EXCEL=10, PICK_IMAGE=11, PICK_FOLDER=12;

    LinearLayout body, manualRows, excelInfo, fieldEditorContainer;
    Button tabData, tabDesigner, tabOutput;
    RadioButton modeExcel, modeManual;
    Uri excelUri, imageUri, folderUri;
    Bitmap currentBitmap;
    LabelDesignerView designer;

    ArrayList<LabelField> fields=new ArrayList<>();
    ArrayList<LinkedHashMap<String,String>> excelRows=new ArrayList<>();
    ArrayList<String> headers=new ArrayList<>();
    ArrayList<EditText> nameEdits=new ArrayList<>(), valueEdits=new ArrayList<>();

    Spinner codeSpinner;
    TextView status, previewStatus;
    EditText labelWidth;
    CheckBox rialToToman, appendMode, batchCrop;
    int selectedField=-1;

    final String[] FONT_VALUES={"DEFAULT","SANS_SERIF","SERIF","MONOSPACE"};
    final String[] FONT_LABELS={"پیش‌فرض","Sans Serif","Serif","Monospace"};
    final String[] ALIGN_LABELS={"راست","وسط","چپ"};

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.WHITE);
        loadTemplate();
        buildUi();
        showData();
    }

    private TextView tv(String s,int sp,boolean bold){
        TextView t=new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(0xFF222222);
        if(bold)t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(8,8,8,8); return t;
    }

    private TextView section(String s){ TextView t=tv(s,16,true); t.setPadding(8,20,8,10); return t; }
    private Button btn(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }
    private LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }

    private EditText numberEdit(String value,String hint){
        EditText e=new EditText(this); e.setText(value); e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); return e;
    }

    private EditText textEdit(String value,String hint){ EditText e=new EditText(this); e.setText(value); e.setHint(hint); return e; }

    private void addLabeledEdit(LinearLayout parent,String label,EditText edit){
        LinearLayout r=row();
        r.addView(tv(label,13,false),new LinearLayout.LayoutParams(0,-2,1));
        r.addView(edit,new LinearLayout.LayoutParams(0,-2,1.5f)); parent.addView(r);
    }

    private Spinner makeSpinner(String[] labels,int selected){
        Spinner s=new Spinner(this); s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        if(selected>=0&&selected<labels.length)s.setSelection(selected); return s;
    }

    private void addLabeledSpinner(LinearLayout parent,String label,Spinner spinner){
        LinearLayout r=row();
        r.addView(tv(label,13,false),new LinearLayout.LayoutParams(0,-2,1));
        r.addView(spinner,new LinearLayout.LayoutParams(0,-2,1.5f)); parent.addView(r);
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(16,16,16,12);
        root.addView(tv("Javdan Price Labeler",24,true));
        root.addView(tv("Excel + ورود دستی + طراح حرفه‌ای لیبل",14,false));

        LinearLayout tabs=row();
        tabData=btn("Excel / دستی"); tabDesigner=btn("طراح برچسب"); tabOutput=btn("خروجی");
        tabs.addView(tabData,new LinearLayout.LayoutParams(0,-2,1));
        tabs.addView(tabDesigner,new LinearLayout.LayoutParams(0,-2,1));
        tabs.addView(tabOutput,new LinearLayout.LayoutParams(0,-2,1)); root.addView(tabs);

        ScrollView sv=new ScrollView(this); body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(4,12,4,90);
        sv.addView(body); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        status=tv("آفلاین • عکس اصلی تغییر نمی‌کند",12,false); root.addView(status); setContentView(root);

        tabData.setOnClickListener(v->showData()); tabDesigner.setOnClickListener(v->showDesigner()); tabOutput.setOnClickListener(v->showOutput());
    }

    private void clear(){ body.removeAllViews(); }

    private void showData(){
        clear();
        RadioGroup rg=new RadioGroup(this); rg.setOrientation(RadioGroup.HORIZONTAL);
        modeExcel=new RadioButton(this); modeExcel.setText("Excel"); modeManual=new RadioButton(this); modeManual.setText("ورود دستی");
        rg.addView(modeExcel); rg.addView(modeManual); body.addView(rg);
        modeManual.setChecked(excelUri==null); modeExcel.setChecked(excelUri!=null);
        modeExcel.setOnClickListener(v->renderDataContent(true)); modeManual.setOnClickListener(v->renderDataContent(false));
        renderDataContent(modeExcel.isChecked());
    }

    private void renderDataContent(boolean excel){
        while(body.getChildCount()>1)body.removeViewAt(1);
        if(excel){
            Button pe=btn("انتخاب فایل Excel (.xlsx)"); body.addView(pe); pe.setOnClickListener(v->pickFile("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",PICK_EXCEL));
            Button pf=btn("انتخاب پوشه عکس محصولات"); body.addView(pf); pf.setOnClickListener(v->pickFolder());
            body.addView(tv("ستون کد محصول:",14,true)); codeSpinner=new Spinner(this); body.addView(codeSpinner);
            rialToToman=new CheckBox(this); rialToToman.setText("تبدیل ریال به تومان (÷۱۰)"); rialToToman.setChecked(true); body.addView(rialToToman);
            excelInfo=new LinearLayout(this); excelInfo.setOrientation(LinearLayout.VERTICAL); body.addView(excelInfo); if(!headers.isEmpty())refreshExcelUi();
            Button validate=btn("بررسی تطبیق Excel و تصاویر"); body.addView(validate); validate.setOnClickListener(v->validateBatch());
        }else{
            body.addView(section("حالت دستی"));
            Button pi=btn("انتخاب عکس از گالری یا Files"); body.addView(pi); pi.setOnClickListener(v->pickFile("image/*",PICK_IMAGE));
            rialToToman=new CheckBox(this); rialToToman.setText("قیمت‌های واردشده ریال هستند؛ تبدیل به تومان ÷۱۰"); body.addView(rialToToman);
            manualRows=new LinearLayout(this); manualRows.setOrientation(LinearLayout.VERTICAL); body.addView(manualRows);
            if(fields.isEmpty())makeDefaults(); rebuildManualRows();
            Button add=btn("+ افزودن قیمت جدید"); body.addView(add); add.setOnClickListener(v->{syncManualRows(); fields.add(new LabelField("قیمت جدید","")); relayoutFields(); rebuildManualRows();});
            Button go=btn("ذخیره قیمت‌ها و رفتن به طراح"); body.addView(go); go.setOnClickListener(v->{syncManualRows();showDesigner();});
        }
    }

    private void makeDefaults(){
        fields.clear();
        LabelField f1=new LabelField("قیمت مصرف کننده",""); f1.strike=true; style(f1,0xFFFFEB3B,0xFFBFA000,0xFF111111);
        LabelField f2=new LabelField("قیمت پایه",""); style(f2,0xFFE31B23,0xFFB00000,Color.WHITE); f2.titleColor=Color.WHITE;
        LabelField f3=new LabelField("قیمت حجم متوسط",""); style(f3,0xFF111111,0xFFEEEEEE,Color.WHITE); f3.titleColor=Color.WHITE;
        LabelField f4=new LabelField("قیمت حجم بالا",""); style(f4,0xFFFFD91A,0xFFBFA000,0xFF111111);
        fields.add(f1);fields.add(f2);fields.add(f3);fields.add(f4); relayoutFields();
    }

    private void style(LabelField f,int bg,int border,int price){
        f.backgroundColor=bg; f.borderColor=border; f.priceColor=price; f.tomanColor=price;
        f.titleSize=16; f.priceSize=28; f.tomanSize=14; f.cornerRadius=20; f.borderWidth=2;
        f.paddingHorizontal=14; f.paddingVertical=8; f.titlePriceGap=3; f.textAlign=1;
    }

    private void rebuildManualRows(){
        manualRows.removeAllViews(); nameEdits.clear(); valueEdits.clear();
        for(int i=0;i<fields.size();i++){
            LabelField f=fields.get(i); LinearLayout r=row();
            EditText n=new EditText(this); n.setText(f.name); n.setHint("نام قیمت");
            EditText v=new EditText(this); v.setText(f.value); v.setHint("قیمت"); v.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
            Button del=btn("×"); final int idx=i; del.setOnClickListener(x->{syncManualRows(); if(fields.size()>1)fields.remove(idx); relayoutFields(); rebuildManualRows();});
            r.addView(n,new LinearLayout.LayoutParams(0,-2,1.4f)); r.addView(v,new LinearLayout.LayoutParams(0,-2,1)); r.addView(del,new LinearLayout.LayoutParams(-2,-2)); manualRows.addView(r);
            nameEdits.add(n); valueEdits.add(v);
        }
    }

    private void syncManualRows(){
        for(int i=0;i<Math.min(fields.size(),nameEdits.size());i++){
            fields.get(i).name=nameEdits.get(i).getText().toString().trim();
            String raw=valueEdits.get(i).getText().toString().replace(",","").trim();
            fields.get(i).value=formatPrice(raw,rialToToman!=null&&rialToToman.isChecked());
        } saveTemplate();
    }

    private String formatPrice(String raw,boolean rial){
        if(raw==null||raw.isEmpty())return "";
        try{String clean=raw.replace(",","").replace("٬","").trim(); double d=Double.parseDouble(clean); if(rial)d/=10.0; return new DecimalFormat("#,###").format(Math.round(d));}
        catch(Exception e){return raw;}
    }

    private void relayoutFields(){
        int n=Math.max(1,fields.size()); float gap=.018f; float h=(1f-gap*(n-1))/n;
        for(int i=0;i<fields.size();i++){ LabelField f=fields.get(i); f.x=0f; f.w=1f; f.h=h; f.y=i*(h+gap); }
    }

    private void showDesigner(){
        if(manualRows!=null&&manualRows.getParent()!=null)syncManualRows(); clear();
        body.addView(section("طراح حرفه‌ای برچسب"));
        body.addView(tv("استایل نمونه: قاب اصلی جمع‌وجور + Auto height. روی عنوان یا قیمت بزن تا اندازه فونت را سریع تغییر بدهی.",13,false));

        designer=new LabelDesignerView(this); designer.setFields(fields); designer.setProductBitmap(currentBitmap);
        body.addView(designer,new LinearLayout.LayoutParams(-1,1050));
        designer.setListener(new LabelDesignerView.Listener(){
            public void onFieldSelected(int i){ selectedField=i; showFieldEditor(); }
            public void onChanged(){ saveTemplate(); }
            public void onTextClicked(int fieldIndex,int textType){ selectedField=fieldIndex; showTextSizeDialog(fieldIndex,textType); }
        });

        body.addView(section("تصویر محصول — Resize + Crop"));
        EditText productX=numberEdit(pct(designer.productX),"X %"), productY=numberEdit(pct(designer.productY),"Y %"), productW=numberEdit(pct(designer.productW),"عرض %"), productH=numberEdit(pct(designer.productH),"ارتفاع %");
        addLabeledEdit(body,"موقعیت افقی تصویر %",productX); addLabeledEdit(body,"موقعیت عمودی تصویر %",productY); addLabeledEdit(body,"عرض تصویر %",productW); addLabeledEdit(body,"ارتفاع تصویر %",productH);
        Button applyProduct=btn("اعمال Resize تصویر"); body.addView(applyProduct);
        applyProduct.setOnClickListener(v->{ designer.productX=clamp(parsePercent(productX,designer.productX),0,.95f); designer.productY=clamp(parsePercent(productY,designer.productY),0,.95f); designer.productW=clamp(parsePercent(productW,designer.productW),.05f,1-designer.productX); designer.productH=clamp(parsePercent(productH,designer.productH),.05f,1-designer.productY); designer.invalidate(); });

        LinearLayout cropButtons=row(); Button crop=btn("✂ کراپ تصویر"); Button resetCrop=btn("بازنشانی کراپ"); cropButtons.addView(crop,new LinearLayout.LayoutParams(0,-2,1)); cropButtons.addView(resetCrop,new LinearLayout.LayoutParams(0,-2,1)); body.addView(cropButtons);
        crop.setOnClickListener(v->showCropDialog()); resetCrop.setOnClickListener(v->{designer.resetCrop(); Toast.makeText(this,"کراپ بازنشانی شد",Toast.LENGTH_SHORT).show();});
        batchCrop=new CheckBox(this); batchCrop.setText("همین کراپ روی همه تصاویر خروجی گروهی اعمال شود"); batchCrop.setChecked(false); body.addView(batchCrop);

        Button sample=btn("انتخاب / تغییر عکس نمونه"); body.addView(sample); sample.setOnClickListener(v->pickFile("image/*",PICK_IMAGE));

        body.addView(section("تنظیمات کلی لیبل"));
        LinearLayout opts=row(); appendMode=new CheckBox(this); appendMode.setText("لیبل بیرون تصویر"); appendMode.setChecked(true); labelWidth=numberEdit("30","عرض لیبل %");
        opts.addView(appendMode,new LinearLayout.LayoutParams(0,-2,1.2f)); opts.addView(labelWidth,new LinearLayout.LayoutParams(0,-2,1)); body.addView(opts);

        Button add=btn("+ افزودن کادر قیمت"); body.addView(add); add.setOnClickListener(v->{LabelField f=new LabelField("قیمت جدید",""); fields.add(f); relayoutFields(); designer.setFields(fields); selectedField=fields.size()-1; designer.select(selectedField); showFieldEditor(); saveTemplate();});
        Button auto=btn("چیدمان فشرده خودکار کادرها"); body.addView(auto); auto.setOnClickListener(v->{relayoutFields(); designer.setFields(fields); saveTemplate();});
        Button save=btn("ذخیره قالب"); body.addView(save); save.setOnClickListener(v->{saveTemplate();Toast.makeText(this,"قالب ذخیره شد",Toast.LENGTH_SHORT).show();});

        body.addView(section("تنظیمات کادر انتخاب‌شده")); fieldEditorContainer=new LinearLayout(this); fieldEditorContainer.setOrientation(LinearLayout.VERTICAL); body.addView(fieldEditorContainer);
        if(!fields.isEmpty()){ if(selectedField<0||selectedField>=fields.size())selectedField=0; designer.select(selectedField); showFieldEditor(); }
    }

    private void showTextSizeDialog(int fieldIndex,int textType){
        if(fieldIndex<0||fieldIndex>=fields.size())return; LabelField f=fields.get(fieldIndex);
        final int[] size={textType==LabelDesignerView.TEXT_TITLE?f.titleSize:(textType==LabelDesignerView.TEXT_TOMAN?f.tomanSize:f.priceSize)};
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(30,25,30,25);
        TextView title=tv(textType==LabelDesignerView.TEXT_TITLE?"اندازه فونت عنوان":"اندازه فونت قیمت",18,true); root.addView(title);
        LinearLayout controls=row(); Button down=btn("▼"); EditText value=numberEdit(String.valueOf(size[0]),"سایز"); Button up=btn("▲");
        controls.addView(down,new LinearLayout.LayoutParams(0,-2,.7f)); controls.addView(value,new LinearLayout.LayoutParams(0,-2,1)); controls.addView(up,new LinearLayout.LayoutParams(0,-2,.7f)); root.addView(controls);
        down.setOnClickListener(v->{size[0]=Math.max(8,size[0]-1);value.setText(String.valueOf(size[0]));}); up.setOnClickListener(v->{size[0]=Math.min(140,size[0]+1);value.setText(String.valueOf(size[0]));});
        new AlertDialog.Builder(this).setView(root).setPositiveButton("اعمال",(d,w)->{int s=parseIntSafe(value,size[0],8,140); if(textType==LabelDesignerView.TEXT_TITLE)f.titleSize=s; else if(textType==LabelDesignerView.TEXT_TOMAN)f.tomanSize=s; else f.priceSize=s; designer.invalidate(); saveTemplate();}).setNegativeButton("انصراف",null).show();
    }

    private void showCropDialog(){
        if(designer==null)return;
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(25,20,25,20);
        EditText l=numberEdit(pct(designer.cropLeft),"چپ %"), t=numberEdit(pct(designer.cropTop),"بالا %"), r=numberEdit(pct(1-designer.cropRight),"راست %"), b=numberEdit(pct(1-designer.cropBottom),"پایین %");
        addLabeledEdit(root,"حذف از چپ %",l); addLabeledEdit(root,"حذف از بالا %",t); addLabeledEdit(root,"حذف از راست %",r); addLabeledEdit(root,"حذف از پایین %",b);
        new AlertDialog.Builder(this).setTitle("کراپ مستقل تصویر").setView(root).setPositiveButton("اعمال",(d,w)->{float L=parsePercent(l,0),T=parsePercent(t,0),R=parsePercent(r,0),B=parsePercent(b,0); designer.setCrop(L,T,1-R,1-B);}).setNeutralButton("بازنشانی",(d,w)->designer.resetCrop()).setNegativeButton("انصراف",null).show();
    }

    private void showFieldEditor(){
        if(fieldEditorContainer==null||selectedField<0||selectedField>=fields.size())return; fieldEditorContainer.removeAllViews();
        LabelField f=fields.get(selectedField); fieldEditorContainer.addView(tv("در حال ویرایش: "+f.name,14,true));
        EditText name=textEdit(f.name,"نام کادر"), val=textEdit(f.value,"قیمت"); addLabeledEdit(fieldEditorContainer,"عنوان",name); addLabeledEdit(fieldEditorContainer,"قیمت",val);

        Spinner titleFont=makeSpinner(FONT_LABELS,fontIndex(f.titleFont)), priceFont=makeSpinner(FONT_LABELS,fontIndex(f.priceFont)), align=makeSpinner(ALIGN_LABELS,clampInt(f.textAlign,0,2));
        addLabeledSpinner(fieldEditorContainer,"فونت عنوان",titleFont); addLabeledSpinner(fieldEditorContainer,"فونت قیمت",priceFont); addLabeledSpinner(fieldEditorContainer,"تراز متن",align);

        fieldEditorContainer.addView(section("رنگ‌بندی کادر"));
        addColorButton(fieldEditorContainer,"رنگ عنوان",f.titleColor,c->{f.titleColor=c;designer.invalidate();saveTemplate();});
        addColorButton(fieldEditorContainer,"رنگ قیمت",f.priceColor,c->{f.priceColor=c;designer.invalidate();saveTemplate();});
        addColorButton(fieldEditorContainer,"رنگ تومان",f.tomanColor,c->{f.tomanColor=c;designer.invalidate();saveTemplate();});
        addColorButton(fieldEditorContainer,"رنگ پس‌زمینه کادر",f.backgroundColor,c->{f.backgroundColor=c;designer.invalidate();saveTemplate();});
        addColorButton(fieldEditorContainer,"رنگ حاشیه کادر",f.borderColor,c->{f.borderColor=c;designer.invalidate();saveTemplate();});

        EditText borderWidth=numberEdit(String.valueOf(f.borderWidth),"2"), radius=numberEdit(String.valueOf(f.cornerRadius),"18"), padH=numberEdit(String.valueOf(f.paddingHorizontal),"14"), padV=numberEdit(String.valueOf(f.paddingVertical),"8"), gap=numberEdit(String.valueOf(f.titlePriceGap),"3");
        addLabeledEdit(fieldEditorContainer,"ضخامت حاشیه",borderWidth); addLabeledEdit(fieldEditorContainer,"گردی گوشه‌ها",radius); addLabeledEdit(fieldEditorContainer,"فاصله داخلی افقی",padH); addLabeledEdit(fieldEditorContainer,"فاصله داخلی عمودی",padV); addLabeledEdit(fieldEditorContainer,"فاصله عنوان تا قیمت",gap);

        CheckBox strike=check("خط‌خورده کردن قیمت",f.strike), showToman=check("نمایش تومان",f.showToman), visible=check("نمایش کادر",f.visible), showTitle=check("نمایش عنوان",f.showTitle), showPrice=check("نمایش قیمت",f.showPrice), titleBold=check("عنوان Bold",f.titleBold), titleItalic=check("عنوان Italic",f.titleItalic), priceBold=check("قیمت Bold",f.priceBold), priceItalic=check("قیمت Italic",f.priceItalic);
        fieldEditorContainer.addView(strike);fieldEditorContainer.addView(showToman);fieldEditorContainer.addView(visible);fieldEditorContainer.addView(showTitle);fieldEditorContainer.addView(showPrice);fieldEditorContainer.addView(titleBold);fieldEditorContainer.addView(titleItalic);fieldEditorContainer.addView(priceBold);fieldEditorContainer.addView(priceItalic);

        Button apply=btn("اعمال تنظیمات این کادر"); fieldEditorContainer.addView(apply);
        apply.setOnClickListener(v->{f.name=name.getText().toString().trim();f.value=val.getText().toString().trim();f.titleFont=FONT_VALUES[titleFont.getSelectedItemPosition()];f.priceFont=FONT_VALUES[priceFont.getSelectedItemPosition()];f.textAlign=align.getSelectedItemPosition();f.borderWidth=parseIntSafe(borderWidth,f.borderWidth,0,30);f.cornerRadius=parseIntSafe(radius,f.cornerRadius,0,100);f.paddingHorizontal=parseIntSafe(padH,f.paddingHorizontal,0,100);f.paddingVertical=parseIntSafe(padV,f.paddingVertical,0,100);f.titlePriceGap=parseIntSafe(gap,f.titlePriceGap,0,50);f.strike=strike.isChecked();f.showToman=showToman.isChecked();f.visible=visible.isChecked();f.showTitle=showTitle.isChecked();f.showPrice=showPrice.isChecked();f.titleBold=titleBold.isChecked();f.titleItalic=titleItalic.isChecked();f.priceBold=priceBold.isChecked();f.priceItalic=priceItalic.isChecked();designer.setFields(fields);saveTemplate();});

        Button copy=btn("اعمال استایل این کادر برای همه کادرها"); fieldEditorContainer.addView(copy); copy.setOnClickListener(v->{for(int i=0;i<fields.size();i++)if(i!=selectedField)copyStyleOnly(f,fields.get(i));designer.setFields(fields);saveTemplate();Toast.makeText(this,"استایل برای همه کادرها اعمال شد",Toast.LENGTH_SHORT).show();});
        Button del=btn("حذف این کادر"); fieldEditorContainer.addView(del); del.setOnClickListener(v->{if(fields.size()<=1)return;fields.remove(selectedField);relayoutFields();selectedField=Math.min(selectedField,fields.size()-1);designer.setFields(fields);designer.select(selectedField);showFieldEditor();saveTemplate();});
    }

    interface ColorSelectedListener{void onColorSelected(int color);}
    private void addColorButton(LinearLayout parent,String label,int current,ColorSelectedListener listener){
        Button b=btn(label+"   "+colorToHex(current)); GradientDrawable g=new GradientDrawable(); g.setColor(current); g.setCornerRadius(16); g.setStroke(2,0xFFBDBDBD); b.setBackground(g); b.setTextColor(isDark(current)?Color.WHITE:Color.BLACK); parent.addView(b); b.setOnClickListener(v->showColorPalette(label,current,c->{listener.onColorSelected(c);showFieldEditor();}));
    }

    private void showColorPalette(String title,int currentColor,ColorSelectedListener listener){
        final Dialog dialog=new Dialog(this); LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(25,20,25,20);
        TextView t=tv(title,18,true); t.setGravity(Gravity.CENTER); root.addView(t);
        final int[] colors={0xFFFFFFFF,0xFFF5F5F5,0xFFE0E0E0,0xFF9E9E9E,0xFF616161,0xFF212121,0xFFFFCDD2,0xFFEF5350,0xFFC62828,0xFF8E0000,0xFFFFE0B2,0xFFFF9800,0xFFEF6C00,0xFFFFF9C4,0xFFFFEB3B,0xFFF9A825,0xFFC8E6C9,0xFF66BB6A,0xFF2E7D32,0xFF1B5E20,0xFFB2DFDB,0xFF26A69A,0xFF00796B,0xFFBBDEFB,0xFF42A5F5,0xFF1976D2,0xFF0D47A1,0xFFD1C4E9,0xFF7E57C2,0xFF512DA8,0xFFF8BBD0,0xFFEC407A,0xFFC2185B,0xFFFFF3E0,0xFFE3F2FD,0xFFE8F5E9,0xFFF3E5F5};
        GridLayout grid=new GridLayout(this); grid.setColumnCount(5); int size=(int)(52*getResources().getDisplayMetrics().density), m=(int)(5*getResources().getDisplayMetrics().density);
        for(final int c:colors){TextView box=new TextView(this);GradientDrawable bg=new GradientDrawable();bg.setColor(c);bg.setCornerRadius(14);bg.setStroke(c==currentColor?5:2,c==currentColor?0xFF1976D2:0xFFBDBDBD);box.setBackground(bg);GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=size;p.height=size;p.setMargins(m,m,m,m);box.setLayoutParams(p);box.setOnClickListener(v->{listener.onColorSelected(c);dialog.dismiss();});grid.addView(box);} root.addView(grid); Button cancel=btn("انصراف");root.addView(cancel);cancel.setOnClickListener(v->dialog.dismiss());dialog.setContentView(root);dialog.show(); if(dialog.getWindow()!=null)dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private boolean isDark(int color){return (0.299*Color.red(color)+0.587*Color.green(color)+0.114*Color.blue(color))<150;}
    private CheckBox check(String text,boolean checked){CheckBox c=new CheckBox(this);c.setText(text);c.setChecked(checked);return c;}

    private void showOutput(){
        if(manualRows!=null&&manualRows.getParent()!=null)syncManualRows(); clear(); body.addView(section("خروجی"));
        Button preview=btn("پیش‌نمایش خروجی نهایی"); body.addView(preview); preview.setOnClickListener(v->makePreview());
        previewStatus=tv("برای خروجی دستی ابتدا عکس را انتخاب کن.",13,false); body.addView(previewStatus);
        Button save=btn("ذخیره عکس فعلی در گالری"); body.addView(save); save.setOnClickListener(v->saveCurrent());
        Button batch=btn("ساخت گروهی از Excel + پوشه عکس‌ها"); body.addView(batch); batch.setOnClickListener(v->runBatch());
    }

    private void makePreview(){if(currentBitmap==null){Toast.makeText(this,"عکس انتخاب نشده",Toast.LENGTH_SHORT).show();return;}Bitmap out=render(currentBitmap,fields,true);ImageView iv=new ImageView(this);iv.setAdjustViewBounds(true);iv.setImageBitmap(out);body.addView(iv,Math.min(body.getWidth()>0?body.getWidth():1000,1000),-2);previewStatus.setText("پیش‌نمایش ساخته شد.");}

    private Bitmap render(Bitmap src,ArrayList<LabelField> useFields,boolean manual){
        LabelDesignerView r=new LabelDesignerView(this); r.setFields(useFields);
        boolean append=appendMode==null||appendMode.isChecked(); float width=.30f; try{width=Float.parseFloat(labelWidth==null?"30":labelWidth.getText().toString())/100f;}catch(Exception ignored){}
        if(manual && designer!=null){r.productX=designer.productX;r.productY=designer.productY;r.productW=designer.productW;r.productH=designer.productH;r.canvasBackground=designer.canvasBackground;r.cropEnabled=designer.cropEnabled;r.cropLeft=designer.cropLeft;r.cropTop=designer.cropTop;r.cropRight=designer.cropRight;r.cropBottom=designer.cropBottom;}
        if(!manual && batchCrop!=null && batchCrop.isChecked() && designer!=null){r.cropEnabled=designer.cropEnabled;r.cropLeft=designer.cropLeft;r.cropTop=designer.cropTop;r.cropRight=designer.cropRight;r.cropBottom=designer.cropBottom;}
        return r.renderFinal(src,Color.WHITE,0xFFD8D8D8,append,width);
    }

    private void saveCurrent(){if(currentBitmap==null){Toast.makeText(this,"عکس انتخاب نشده",Toast.LENGTH_SHORT).show();return;}try{Bitmap out=render(currentBitmap,fields,true);saveToGallery(out,"Javdan_"+System.currentTimeMillis()+".jpg");Toast.makeText(this,"ذخیره شد",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,safeMessage(e),Toast.LENGTH_LONG).show();}}

    private void saveToGallery(Bitmap bmp,String name)throws Exception{ContentValues v=new ContentValues();v.put(MediaStore.Images.Media.DISPLAY_NAME,name);v.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");v.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/JavdanPriceLabeler");Uri u=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);if(u==null)throw new IOException("ساخت فایل خروجی ناموفق بود");try(OutputStream o=getContentResolver().openOutputStream(u)){if(o==null)throw new IOException("فایل خروجی باز نشد");bmp.compress(Bitmap.CompressFormat.JPEG,94,o);}}

    private void pickFile(String type,int request){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(type);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,request);}
    private void pickFolder(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK_FOLDER);}

    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;Uri u=data.getData();if(u==null)return;try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        try{if(req==PICK_IMAGE){imageUri=u;try(InputStream in=getContentResolver().openInputStream(u)){currentBitmap=BitmapFactory.decodeStream(in);}status.setText("عکس انتخاب شد");if(designer!=null){designer.setProductBitmap(currentBitmap);designer.resetCrop();}}
        else if(req==PICK_EXCEL){excelUri=u;status.setText("در حال خواندن Excel...");excelRows=new XlsxReader(this).readFirstSheet(u);headers.clear();if(!excelRows.isEmpty())headers.addAll(excelRows.get(0).keySet());refreshExcelUi();status.setText(excelRows.size()+" ردیف Excel خوانده شد");}
        else if(req==PICK_FOLDER){folderUri=u;status.setText("پوشه تصاویر انتخاب شد");}}catch(Exception e){Toast.makeText(this,"خطا: "+safeMessage(e),Toast.LENGTH_LONG).show();}
    }

    private void refreshExcelUi(){if(codeSpinner!=null)codeSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,headers));if(excelInfo!=null){excelInfo.removeAllViews();excelInfo.addView(tv("ردیف‌ها: "+excelRows.size()+" | ستون‌ها: "+headers.size(),13,false));}}

    private void validateBatch(){if(excelUri==null||folderUri==null||excelRows.isEmpty()){Toast.makeText(this,"Excel و پوشه عکس‌ها را انتخاب کن",Toast.LENGTH_LONG).show();return;}if(headers.isEmpty())return;final int pos=codeSpinner!=null?codeSpinner.getSelectedItemPosition():0;final String codeHeader=headers.get(Math.max(0,Math.min(pos,headers.size()-1)));new Thread(()->{try{HashMap<String,Uri> images=listTreeImages(folderUri);int matched=0,missing=0;for(LinkedHashMap<String,String> r:excelRows){String c=normalizeCode(r.get(codeHeader));if(c.isEmpty())continue;if(images.containsKey(c))matched++;else missing++;}int fm=matched,fn=missing;runOnUiThread(()->Toast.makeText(this,"تطبیق: "+fm+" | بدون عکس: "+fn,Toast.LENGTH_LONG).show());}catch(Exception e){runOnUiThread(()->Toast.makeText(this,safeMessage(e),Toast.LENGTH_LONG).show());}}).start();}

    private boolean isPriceColumn(String header){if(header==null)return false;String h=header.trim().replace("ي","ی").replace("ك","ک").toLowerCase(Locale.ROOT);return h.contains("قیمت");}

    private void runBatch(){
        if(excelRows.isEmpty()||folderUri==null||headers.isEmpty()){Toast.makeText(this,"Excel و پوشه عکس‌ها را انتخاب کن",Toast.LENGTH_LONG).show();return;}
        final boolean convert=rialToToman==null||rialToToman.isChecked(); status.setText("در حال ساخت خروجی گروهی...");
        new Thread(()->{int ok=0,missing=0,errors=0;try{int selectedPos=codeSpinner!=null?codeSpinner.getSelectedItemPosition():0;if(selectedPos<0||selectedPos>=headers.size())selectedPos=0;String codeHeader=headers.get(selectedPos);ArrayList<String> priceHeaders=new ArrayList<>();for(String h:headers)if(!h.equals(codeHeader)&&isPriceColumn(h))priceHeaders.add(h);HashMap<String,Uri> images=listTreeImages(folderUri);for(LinkedHashMap<String,String> row:excelRows){String code=normalizeCode(row.get(codeHeader));if(code.isEmpty())continue;Uri img=images.get(code);if(img==null){missing++;continue;}try{Bitmap src;try(InputStream in=getContentResolver().openInputStream(img)){src=BitmapFactory.decodeStream(in);}if(src==null){errors++;continue;}ArrayList<LabelField> fs=new ArrayList<>();for(int i=0;i<priceHeaders.size();i++){String h=priceHeaders.get(i),raw=row.get(h);if(raw==null||raw.trim().isEmpty())continue;LabelField f=new LabelField(h.trim(),formatPrice(raw,convert));if(i<fields.size())copyStyleOnly(fields.get(i),f);else if(!fields.isEmpty())copyStyleOnly(fields.get(fields.size()-1),f);fs.add(f);}layoutBatchFields(fs);Bitmap out=render(src,fs,false);saveToGallery(out,code+".jpg");ok++;src.recycle();if(out!=src&&!out.isRecycled())out.recycle();}catch(Exception e){errors++;}}}catch(Exception e){errors++;}int fok=ok,fm=missing,fe=errors;runOnUiThread(()->{status.setText("تمام شد — موفق: "+fok+" | بدون عکس: "+fm+" | خطا: "+fe);Toast.makeText(this,status.getText(),Toast.LENGTH_LONG).show();});}).start();
    }

    private void layoutBatchFields(ArrayList<LabelField> fs){int n=Math.max(1,fs.size());float gap=.018f,h=(1f-gap*(n-1))/n;for(int i=0;i<fs.size();i++){LabelField f=fs.get(i);f.x=0;f.w=1;f.h=h;f.y=i*(h+gap);}}

    private void copyStyleOnly(LabelField from,LabelField to){to.titleSize=from.titleSize;to.priceSize=from.priceSize;to.tomanSize=from.tomanSize;to.titleColor=from.titleColor;to.priceColor=from.priceColor;to.tomanColor=from.tomanColor;to.backgroundColor=from.backgroundColor;to.borderColor=from.borderColor;to.borderWidth=from.borderWidth;to.cornerRadius=from.cornerRadius;to.paddingHorizontal=from.paddingHorizontal;to.paddingVertical=from.paddingVertical;to.titlePriceGap=from.titlePriceGap;to.strike=from.strike;to.showToman=from.showToman;to.visible=from.visible;to.titleBold=from.titleBold;to.titleItalic=from.titleItalic;to.priceBold=from.priceBold;to.priceItalic=from.priceItalic;to.textAlign=from.textAlign;to.titleFont=from.titleFont;to.priceFont=from.priceFont;to.showTitle=from.showTitle;to.showPrice=from.showPrice;}

    private HashMap<String,Uri> listTreeImages(Uri tree)throws Exception{HashMap<String,Uri> map=new HashMap<>();String docId=DocumentsContract.getTreeDocumentId(tree);Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,docId);String[] cols={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE};try(Cursor c=getContentResolver().query(children,cols,null,null,null)){if(c!=null)while(c.moveToNext()){String id=c.getString(0),name=c.getString(1),mime=c.getString(2);if(mime!=null&&mime.startsWith("image/")){String stem=name==null?"":name;int dot=stem.lastIndexOf('.');if(dot>0)stem=stem.substring(0,dot);map.put(normalizeCode(stem),DocumentsContract.buildDocumentUriUsingTree(tree,id));}}}return map;}

    private String normalizeCode(String s){if(s==null)return "";s=s.trim();if(s.endsWith(".0"))s=s.substring(0,s.length()-2);return s;}

    private void saveTemplate(){try{JSONArray a=new JSONArray();for(LabelField f:fields)a.put(f.toJson());getSharedPreferences("javdan",MODE_PRIVATE).edit().putString("fields",a.toString()).apply();}catch(Exception ignored){}}
    private void loadTemplate(){fields.clear();String s=getSharedPreferences("javdan",MODE_PRIVATE).getString("fields","");if(!s.isEmpty())try{JSONArray a=new JSONArray(s);for(int i=0;i<a.length();i++)fields.add(LabelField.fromJson(a.getJSONObject(i)));}catch(Exception ignored){}if(fields.isEmpty())makeDefaults();}

    private int fontIndex(String font){if(font==null)return 0;for(int i=0;i<FONT_VALUES.length;i++)if(FONT_VALUES[i].equals(font))return i;return 0;}
    private String pct(float value){return String.valueOf(Math.round(value*100f));}
    private float parsePercent(EditText e,float fallback){try{String s=e.getText().toString().trim();if(s.isEmpty())return fallback;return Float.parseFloat(s)/100f;}catch(Exception ex){return fallback;}}
    private int parseIntSafe(EditText e,int fallback,int min,int max){try{return clampInt(Integer.parseInt(e.getText().toString().trim()),min,max);}catch(Exception ex){return fallback;}}
    private String colorToHex(int color){return String.format(Locale.US,"#%08X",color);}
    private float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
    private int clampInt(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private String safeMessage(Exception e){if(e==null)return "خطای نامشخص";String m=e.getMessage();return(m==null||m.trim().isEmpty())?e.getClass().getSimpleName():m;}
}
