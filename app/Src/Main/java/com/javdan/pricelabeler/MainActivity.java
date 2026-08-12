package com.javdan.pricelabeler;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
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
    CheckBox rialToToman, appendMode;
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
        t.setPadding(8,8,8,8);
        return t;
    }

    private TextView section(String s){
        TextView t=tv(s,16,true);
        t.setPadding(8,20,8,10);
        return t;
    }

    private Button btn(String s){
        Button b=new Button(this);
        b.setText(s); b.setAllCaps(false);
        return b;
    }

    private LinearLayout row(){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private EditText numberEdit(String value,String hint){
        EditText e=new EditText(this);
        e.setText(value); e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    private EditText textEdit(String value,String hint){
        EditText e=new EditText(this);
        e.setText(value); e.setHint(hint);
        return e;
    }

    private void addLabeledEdit(LinearLayout parent,String label,EditText edit){
        LinearLayout r=row();
        r.addView(tv(label,13,false),new LinearLayout.LayoutParams(0,-2,1));
        r.addView(edit,new LinearLayout.LayoutParams(0,-2,1.5f));
        parent.addView(r);
    }

    private Spinner makeSpinner(String[] labels,int selected){
        Spinner s=new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        if(selected>=0&&selected<labels.length)s.setSelection(selected);
        return s;
    }

    private void addLabeledSpinner(LinearLayout parent,String label,Spinner spinner){
        LinearLayout r=row();
        r.addView(tv(label,13,false),new LinearLayout.LayoutParams(0,-2,1));
        r.addView(spinner,new LinearLayout.LayoutParams(0,-2,1.5f));
        parent.addView(r);
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16,16,16,12);

        root.addView(tv("Javdan Price Labeler",24,true));
        root.addView(tv("Excel + ورود دستی + طراح حرفه‌ای لیبل",14,false));

        LinearLayout tabs=row();
        tabData=btn("Excel / دستی");
        tabDesigner=btn("طراح برچسب");
        tabOutput=btn("خروجی");
        tabs.addView(tabData,new LinearLayout.LayoutParams(0,-2,1));
        tabs.addView(tabDesigner,new LinearLayout.LayoutParams(0,-2,1));
        tabs.addView(tabOutput,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(tabs);

        ScrollView sv=new ScrollView(this);
        body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(4,12,4,80);
        sv.addView(body);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        status=tv("آفلاین • عکس اصلی تغییر نمی‌کند",12,false);
        root.addView(status);
        setContentView(root);

        tabData.setOnClickListener(v->showData());
        tabDesigner.setOnClickListener(v->showDesigner());
        tabOutput.setOnClickListener(v->showOutput());
    }

    private void clear(){ body.removeAllViews(); }

    private void showData(){
        clear();
        RadioGroup rg=new RadioGroup(this);
        rg.setOrientation(RadioGroup.HORIZONTAL);
        modeExcel=new RadioButton(this); modeExcel.setText("Excel");
        modeManual=new RadioButton(this); modeManual.setText("ورود دستی");
        rg.addView(modeExcel); rg.addView(modeManual); body.addView(rg);

        modeManual.setChecked(excelUri==null);
        modeExcel.setChecked(excelUri!=null);
        modeExcel.setOnClickListener(v->renderDataContent(true));
        modeManual.setOnClickListener(v->renderDataContent(false));
        renderDataContent(modeExcel.isChecked());
    }

    private void renderDataContent(boolean excel){
        while(body.getChildCount()>1)body.removeViewAt(1);

        if(excel){
            Button pe=btn("انتخاب فایل Excel (.xlsx)");
            body.addView(pe);
            pe.setOnClickListener(v->pickFile("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",PICK_EXCEL));

            Button pf=btn("انتخاب پوشه عکس محصولات");
            body.addView(pf); pf.setOnClickListener(v->pickFolder());

            body.addView(tv("ستون کد محصول:",14,true));
            codeSpinner=new Spinner(this); body.addView(codeSpinner);

            rialToToman=new CheckBox(this);
            rialToToman.setText("تبدیل ریال به تومان (÷۱۰)");
            rialToToman.setChecked(true);
            body.addView(rialToToman);

            excelInfo=new LinearLayout(this);
            excelInfo.setOrientation(LinearLayout.VERTICAL);
            body.addView(excelInfo);
            if(!headers.isEmpty())refreshExcelUi();

            Button validate=btn("بررسی تطبیق Excel و تصاویر");
            body.addView(validate);
            validate.setOnClickListener(v->validateBatch());

        }else{
            body.addView(section("حالت دستی"));

            Button pi=btn("انتخاب عکس از گالری یا Files");
            body.addView(pi); pi.setOnClickListener(v->pickFile("image/*",PICK_IMAGE));

            rialToToman=new CheckBox(this);
            rialToToman.setText("قیمت‌های واردشده ریال هستند؛ تبدیل به تومان ÷۱۰");
            body.addView(rialToToman);

            manualRows=new LinearLayout(this);
            manualRows.setOrientation(LinearLayout.VERTICAL);
            body.addView(manualRows);

            if(fields.isEmpty())makeDefaults();
            rebuildManualRows();

            Button add=btn("+ افزودن قیمت جدید");
            body.addView(add);
            add.setOnClickListener(v->{
                syncManualRows();
                fields.add(new LabelField("قیمت جدید",""));
                relayoutFields();
                rebuildManualRows();
            });

            Button go=btn("ذخیره قیمت‌ها و رفتن به طراح");
            body.addView(go);
            go.setOnClickListener(v->{syncManualRows();showDesigner();});
        }
    }

    private void makeDefaults(){
        fields.clear();

        LabelField f1=new LabelField("قیمت مصرف کننده","");
        f1.strike=true;
        f1.backgroundColor=0xFFFFF3F3;
        f1.borderColor=0xFFE7BABA;
        f1.priceColor=0xFFC62828;
        f1.tomanColor=0xFFC62828;

        LabelField f2=new LabelField("قیمت پایه","");
        f2.backgroundColor=0xFFF2F7FF;
        f2.borderColor=0xFFB8C9E6;
        f2.priceColor=0xFF1557A5;
        f2.tomanColor=0xFF1557A5;

        LabelField f3=new LabelField("قیمت حجم متوسط","");
        f3.backgroundColor=0xFFF4FAF3;
        f3.borderColor=0xFFB7D8B2;
        f3.priceColor=0xFF287A35;
        f3.tomanColor=0xFF287A35;

        LabelField f4=new LabelField("قیمت حجم بالا","");
        f4.backgroundColor=0xFFFFF9E8;
        f4.borderColor=0xFFE1CD8B;
        f4.priceColor=0xFF9A6A00;
        f4.tomanColor=0xFF9A6A00;

        fields.add(f1); fields.add(f2); fields.add(f3); fields.add(f4);
        relayoutFields();
    }

    private void rebuildManualRows(){
        manualRows.removeAllViews();
        nameEdits.clear(); valueEdits.clear();

        for(int i=0;i<fields.size();i++){
            LabelField f=fields.get(i);
            LinearLayout r=row();

            EditText n=new EditText(this);
            n.setText(f.name); n.setHint("نام قیمت");

            EditText v=new EditText(this);
            v.setText(f.value); v.setHint("قیمت");
            v.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);

            Button del=btn("×");
            final int idx=i;
            del.setOnClickListener(x->{
                syncManualRows();
                if(fields.size()>1)fields.remove(idx);
                relayoutFields(); rebuildManualRows();
            });

            r.addView(n,new LinearLayout.LayoutParams(0,-2,1.4f));
            r.addView(v,new LinearLayout.LayoutParams(0,-2,1));
            r.addView(del,new LinearLayout.LayoutParams(-2,-2));
            manualRows.addView(r);

            nameEdits.add(n); valueEdits.add(v);
        }
    }

    private void syncManualRows(){
        for(int i=0;i<Math.min(fields.size(),nameEdits.size());i++){
            fields.get(i).name=nameEdits.get(i).getText().toString().trim();
            String raw=valueEdits.get(i).getText().toString().replace(",","").trim();
            fields.get(i).value=formatPrice(raw,rialToToman!=null&&rialToToman.isChecked());
        }
        saveTemplate();
    }

    private String formatPrice(String raw,boolean rial){
        if(raw==null||raw.isEmpty())return "";
        try{
            String clean=raw.replace(",","").replace("٬","").trim();
            double d=Double.parseDouble(clean);
            if(rial)d/=10.0;
            return new DecimalFormat("#,###").format(Math.round(d));
        }catch(Exception e){return raw;}
    }

    private void relayoutFields(){
        int n=Math.max(1,fields.size());
        float top=.08f, gap=.025f;
        float h=Math.min(.18f,(.82f-gap*(n-1))/n);

        for(int i=0;i<fields.size();i++){
            LabelField f=fields.get(i);
            f.x=.04f; f.w=.92f; f.h=h;
            f.y=top+i*(h+gap);
        }
    }

    private void showDesigner(){
        if(manualRows!=null&&manualRows.getParent()!=null)syncManualRows();
        clear();

        body.addView(section("طراح حرفه‌ای برچسب"));
        body.addView(tv("روی هر کادر قیمت بزن و تنظیمات همان کادر را پایین تغییر بده.",13,false));

        designer=new LabelDesignerView(this);
        designer.setFields(fields);
        designer.setProductBitmap(currentBitmap);
        body.addView(designer,new LinearLayout.LayoutParams(-1,900));

        designer.setListener(new LabelDesignerView.Listener(){
            @Override
            public void onFieldSelected(int i){
                selectedField=i;
                showFieldEditor();
            }

            @Override
            public void onChanged(){
                saveTemplate();
            }

            @Override
            public void onTextClicked(int fieldIndex,int part){
                selectedField=fieldIndex;
                showFontSizeDialog(fieldIndex,part);
            }
        });

        body.addView(section("تنظیمات کلی خروجی"));

        LinearLayout opts=row();
        appendMode=new CheckBox(this);
        appendMode.setText("لیبل بیرون تصویر");
        appendMode.setChecked(true);

        labelWidth=numberEdit("36","عرض لیبل %");
        opts.addView(appendMode,new LinearLayout.LayoutParams(0,-2,1.2f));
        opts.addView(labelWidth,new LinearLayout.LayoutParams(0,-2,1));
        body.addView(opts);

        body.addView(section("تصویر محصول"));

        EditText productX=numberEdit(pct(designer.productX),"X %");
        EditText productY=numberEdit(pct(designer.productY),"Y %");
        EditText productW=numberEdit(pct(designer.productW),"عرض %");
        EditText productH=numberEdit(pct(designer.productH),"ارتفاع %");
        EditText canvasColor=textEdit(colorToHex(designer.canvasBackground),"#FFF2F2F2");

        addLabeledEdit(body,"موقعیت افقی تصویر %",productX);
        addLabeledEdit(body,"موقعیت عمودی تصویر %",productY);
        addLabeledEdit(body,"عرض تصویر %",productW);
        addLabeledEdit(body,"ارتفاع تصویر %",productH);
        addLabeledEdit(body,"رنگ پس‌زمینه طراح",canvasColor);

        Button applyProduct=btn("اعمال تنظیمات تصویر");
        body.addView(applyProduct);
        applyProduct.setOnClickListener(v->{
            designer.productX=clamp(parsePercent(productX,designer.productX),0f,.95f);
            designer.productY=clamp(parsePercent(productY,designer.productY),0f,.95f);
            designer.productW=clamp(parsePercent(productW,designer.productW),.05f,1f-designer.productX);
            designer.productH=clamp(parsePercent(productH,designer.productH),.05f,1f-designer.productY);
            designer.canvasBackground=parseColorSafe(canvasColor.getText().toString(),designer.canvasBackground);
            designer.invalidate();
            Toast.makeText(this,"تنظیمات تصویر اعمال شد",Toast.LENGTH_SHORT).show();
        });

        Button sample=btn("انتخاب / تغییر عکس نمونه");
        body.addView(sample);
        sample.setOnClickListener(v->pickFile("image/*",PICK_IMAGE));

        Button add=btn("+ افزودن کادر قیمت");
        body.addView(add);
        add.setOnClickListener(v->{
            LabelField f=new LabelField("قیمت جدید","");
            fields.add(f);
            relayoutFields();
            designer.setFields(fields);
            selectedField=fields.size()-1;
            designer.select(selectedField);
            showFieldEditor();
            saveTemplate();
        });

        Button auto=btn("چیدمان مرتب خودکار کادرها");
        body.addView(auto);
        auto.setOnClickListener(v->{
            relayoutFields();
            designer.setFields(fields);
            saveTemplate();
        });

        Button save=btn("ذخیره قالب");
        body.addView(save);
        save.setOnClickListener(v->{
            saveTemplate();
            Toast.makeText(this,"قالب ذخیره شد",Toast.LENGTH_SHORT).show();
        });

        body.addView(section("تنظیمات کادر انتخاب‌شده"));

        fieldEditorContainer=new LinearLayout(this);
        fieldEditorContainer.setOrientation(LinearLayout.VERTICAL);
        body.addView(fieldEditorContainer);

        if(!fields.isEmpty()){
            if(selectedField<0||selectedField>=fields.size())selectedField=0;
            designer.select(selectedField);
            showFieldEditor();
        }
    }

    private void showFontSizeDialog(int fieldIndex,int part){
        if(fieldIndex<0||fieldIndex>=fields.size())return;

        LabelField f=fields.get(fieldIndex);
        final Dialog dialog=new Dialog(this);

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30,25,30,25);

        String title;
        int current;

        if(part==0){
            title="اندازه فونت عنوان";
            current=f.titleSize;
        }else if(part==2){
            title="اندازه فونت تومان";
            current=f.tomanSize;
        }else{
            title="اندازه فونت قیمت";
            current=f.priceSize;
        }

        TextView titleView=tv(title,18,true);
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView);

        LinearLayout sizeRow=row();

        Button minus=btn("−");
        Button plus=btn("+");
        EditText value=numberEdit(String.valueOf(current),"سایز");
        value.setGravity(Gravity.CENTER);

        sizeRow.addView(minus,new LinearLayout.LayoutParams(0,-2,1));
        sizeRow.addView(value,new LinearLayout.LayoutParams(0,-2,1.5f));
        sizeRow.addView(plus,new LinearLayout.LayoutParams(0,-2,1));

        root.addView(sizeRow);

        final int fallback=current;

        Runnable applySize=()->{
            int newSize=parseIntText(value,fallback);
            newSize=clampInt(newSize,8,140);
            value.setText(String.valueOf(newSize));

            if(part==0){
                f.titleSize=newSize;
            }else if(part==2){
                f.tomanSize=newSize;
            }else{
                f.priceSize=newSize;
            }

            if(designer!=null)designer.invalidate();
            saveTemplate();
        };

        minus.setOnClickListener(v->{
            int n=parseIntText(value,fallback);
            value.setText(String.valueOf(Math.max(8,n-1)));
            applySize.run();
        });

        plus.setOnClickListener(v->{
            int n=parseIntText(value,fallback);
            value.setText(String.valueOf(Math.min(140,n+1)));
            applySize.run();
        });

        Button apply=btn("اعمال");
        root.addView(apply);
        apply.setOnClickListener(v->{
            applySize.run();
            dialog.dismiss();
            if(fieldEditorContainer!=null)showFieldEditor();
        });

        Button cancel=btn("انصراف");
        root.addView(cancel);
        cancel.setOnClickListener(v->dialog.dismiss());

        dialog.setContentView(root);
        dialog.show();

        Window w=dialog.getWindow();
        if(w!=null){
            w.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private int parseIntText(EditText edit,int fallback){
        try{
            return Integer.parseInt(edit.getText().toString().trim());
        }catch(Exception e){
            return fallback;
        }
    }

    private void showFieldEditor(){
        if(fieldEditorContainer==null||selectedField<0||selectedField>=fields.size())return;
        fieldEditorContainer.removeAllViews();

        LabelField f=fields.get(selectedField);
        TextView selectedLabel=tv("در حال ویرایش: "+f.name,14,true);
        fieldEditorContainer.addView(selectedLabel);

        EditText name=textEdit(f.name,"نام کادر");
        EditText val=textEdit(f.value,"قیمت");
        addLabeledEdit(fieldEditorContainer,"عنوان",name);
        addLabeledEdit(fieldEditorContainer,"قیمت",val);

        EditText xPct=numberEdit(pct(f.x),"X %");
        EditText yPct=numberEdit(pct(f.y),"Y %");
        EditText wPct=numberEdit(pct(f.w),"عرض %");
        EditText hPct=numberEdit(pct(f.h),"ارتفاع %");
        addLabeledEdit(fieldEditorContainer,"موقعیت افقی %",xPct);
        addLabeledEdit(fieldEditorContainer,"موقعیت عمودی %",yPct);
        addLabeledEdit(fieldEditorContainer,"عرض کادر %",wPct);
        addLabeledEdit(fieldEditorContainer,"ارتفاع کادر %",hPct);

        EditText titleSize=numberEdit(String.valueOf(f.titleSize),"24");
        EditText priceSize=numberEdit(String.valueOf(f.priceSize),"34");
        EditText tomanSize=numberEdit(String.valueOf(f.tomanSize),"15");
        addLabeledEdit(fieldEditorContainer,"اندازه فونت عنوان",titleSize);
        addLabeledEdit(fieldEditorContainer,"اندازه فونت قیمت",priceSize);
        addLabeledEdit(fieldEditorContainer,"اندازه تومان",tomanSize);

        Spinner titleFont=makeSpinner(FONT_LABELS,fontIndex(f.titleFont));
        Spinner priceFont=makeSpinner(FONT_LABELS,fontIndex(f.priceFont));
        Spinner align=makeSpinner(ALIGN_LABELS,clampInt(f.textAlign,0,2));
        addLabeledSpinner(fieldEditorContainer,"فونت عنوان",titleFont);
        addLabeledSpinner(fieldEditorContainer,"فونت قیمت",priceFont);
        addLabeledSpinner(fieldEditorContainer,"تراز متن",align);

        EditText titleColor=textEdit(colorToHex(f.titleColor),"#FF333333");
        EditText priceColor=textEdit(colorToHex(f.priceColor),"#FFC62828");
        EditText tomanColor=textEdit(colorToHex(f.tomanColor),"#FFC62828");
        EditText backgroundColor=textEdit(colorToHex(f.backgroundColor),"#FFFFFFFF");
        EditText borderColor=textEdit(colorToHex(f.borderColor),"#FFD8D8D8");
        addLabeledEdit(fieldEditorContainer,"رنگ عنوان",titleColor);
        addLabeledEdit(fieldEditorContainer,"رنگ قیمت",priceColor);
        addLabeledEdit(fieldEditorContainer,"رنگ تومان",tomanColor);
        addLabeledEdit(fieldEditorContainer,"رنگ پس‌زمینه کادر",backgroundColor);
        addLabeledEdit(fieldEditorContainer,"رنگ حاشیه کادر",borderColor);

        EditText borderWidth=numberEdit(String.valueOf(f.borderWidth),"2");
        EditText radius=numberEdit(String.valueOf(f.cornerRadius),"18");
        EditText padH=numberEdit(String.valueOf(f.paddingHorizontal),"18");
        EditText padV=numberEdit(String.valueOf(f.paddingVertical),"12");
        EditText titleGap=numberEdit(String.valueOf(f.titlePriceGap),"6");
        addLabeledEdit(fieldEditorContainer,"ضخامت حاشیه",borderWidth);
        addLabeledEdit(fieldEditorContainer,"گردی گوشه‌ها",radius);
        addLabeledEdit(fieldEditorContainer,"فاصله داخلی افقی",padH);
        addLabeledEdit(fieldEditorContainer,"فاصله داخلی عمودی",padV);
        addLabeledEdit(fieldEditorContainer,"فاصله عنوان تا قیمت",titleGap);

        CheckBox strike=check("خط‌خورده کردن قیمت",f.strike);
        CheckBox showToman=check("نمایش تومان",f.showToman);
        CheckBox visible=check("نمایش کادر",f.visible);
        CheckBox showTitle=check("نمایش عنوان",f.showTitle);
        CheckBox showPrice=check("نمایش قیمت",f.showPrice);
        CheckBox titleBold=check("عنوان Bold",f.titleBold);
        CheckBox titleItalic=check("عنوان Italic",f.titleItalic);
        CheckBox priceBold=check("قیمت Bold",f.priceBold);
        CheckBox priceItalic=check("قیمت Italic",f.priceItalic);

        fieldEditorContainer.addView(strike);
        fieldEditorContainer.addView(showToman);
        fieldEditorContainer.addView(visible);
        fieldEditorContainer.addView(showTitle);
        fieldEditorContainer.addView(showPrice);
        fieldEditorContainer.addView(titleBold);
        fieldEditorContainer.addView(titleItalic);
        fieldEditorContainer.addView(priceBold);
        fieldEditorContainer.addView(priceItalic);

        Button apply=btn("اعمال تنظیمات این کادر");
        fieldEditorContainer.addView(apply);

        apply.setOnClickListener(v->{
            f.name=name.getText().toString().trim();
            f.value=val.getText().toString().trim();

            f.x=clamp(parsePercent(xPct,f.x),0f,.98f);
            f.y=clamp(parsePercent(yPct,f.y),0f,.98f);
            f.w=clamp(parsePercent(wPct,f.w),.05f,1f-f.x);
            f.h=clamp(parsePercent(hPct,f.h),.04f,1f-f.y);

            f.titleSize=parseIntSafe(titleSize,f.titleSize,8,100);
            f.priceSize=parseIntSafe(priceSize,f.priceSize,10,140);
            f.tomanSize=parseIntSafe(tomanSize,f.tomanSize,8,60);

            f.titleFont=FONT_VALUES[titleFont.getSelectedItemPosition()];
            f.priceFont=FONT_VALUES[priceFont.getSelectedItemPosition()];
            f.textAlign=align.getSelectedItemPosition();

            f.titleColor=parseColorSafe(titleColor.getText().toString(),f.titleColor);
            f.priceColor=parseColorSafe(priceColor.getText().toString(),f.priceColor);
            f.tomanColor=parseColorSafe(tomanColor.getText().toString(),f.tomanColor);
            f.backgroundColor=parseColorSafe(backgroundColor.getText().toString(),f.backgroundColor);
            f.borderColor=parseColorSafe(borderColor.getText().toString(),f.borderColor);

            f.borderWidth=parseIntSafe(borderWidth,f.borderWidth,0,30);
            f.cornerRadius=parseIntSafe(radius,f.cornerRadius,0,100);
            f.paddingHorizontal=parseIntSafe(padH,f.paddingHorizontal,0,100);
            f.paddingVertical=parseIntSafe(padV,f.paddingVertical,0,100);
            f.titlePriceGap=parseIntSafe(titleGap,f.titlePriceGap,0,100);

            f.strike=strike.isChecked();
            f.showToman=showToman.isChecked();
            f.visible=visible.isChecked();
            f.showTitle=showTitle.isChecked();
            f.showPrice=showPrice.isChecked();
            f.titleBold=titleBold.isChecked();
            f.titleItalic=titleItalic.isChecked();
            f.priceBold=priceBold.isChecked();
            f.priceItalic=priceItalic.isChecked();

            designer.setFields(fields);
            designer.select(selectedField);
            selectedLabel.setText("در حال ویرایش: "+f.name);
            saveTemplate();
            Toast.makeText(this,"تنظیمات کادر اعمال شد",Toast.LENGTH_SHORT).show();
        });

        Button copyStyle=btn("کپی استایل این کادر برای همه");
        fieldEditorContainer.addView(copyStyle);
        copyStyle.setOnClickListener(v->{
            for(int i=0;i<fields.size();i++){
                if(i!=selectedField)copyStyleOnly(f,fields.get(i));
            }
            designer.setFields(fields);
            saveTemplate();
            Toast.makeText(this,"استایل برای همه کادرها کپی شد",Toast.LENGTH_SHORT).show();
        });

        Button del=btn("حذف این کادر");
        fieldEditorContainer.addView(del);
        del.setOnClickListener(v->{
            if(fields.size()<=1){
                Toast.makeText(this,"حداقل یک کادر باید باقی بماند",Toast.LENGTH_SHORT).show();
                return;
            }
            fields.remove(selectedField);
            relayoutFields();
            selectedField=Math.min(selectedField,fields.size()-1);
            designer.setFields(fields);
            designer.select(selectedField);
            showFieldEditor();
            saveTemplate();
        });
    }

    private CheckBox check(String text,boolean checked){
        CheckBox c=new CheckBox(this);
        c.setText(text); c.setChecked(checked);
        return c;
    }

    private void showOutput(){
        if(manualRows!=null&&manualRows.getParent()!=null)syncManualRows();
        clear();

        body.addView(section("خروجی"));

        Button preview=btn("پیش‌نمایش خروجی نهایی");
        body.addView(preview); preview.setOnClickListener(v->makePreview());

        previewStatus=tv("برای خروجی دستی ابتدا عکس را انتخاب کن.",13,false);
        body.addView(previewStatus);

        Button save=btn("ذخیره عکس فعلی در گالری");
        body.addView(save); save.setOnClickListener(v->saveCurrent());

        Button batch=btn("ساخت گروهی از Excel + پوشه عکس‌ها");
        body.addView(batch); batch.setOnClickListener(v->runBatch());
    }

    private void makePreview(){
        if(currentBitmap==null){
            Toast.makeText(this,"عکس انتخاب نشده",Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap out=render(currentBitmap,fields);
        ImageView iv=new ImageView(this);
        iv.setAdjustViewBounds(true); iv.setImageBitmap(out);
        body.addView(iv,Math.min(body.getWidth()>0?body.getWidth():1000,1000),-2);
        previewStatus.setText("پیش‌نمایش ساخته شد.");
    }

    private Bitmap render(Bitmap src,ArrayList<LabelField> useFields){
        LabelDesignerView r=new LabelDesignerView(this);
        r.setFields(useFields);

        boolean append=appendMode==null||appendMode.isChecked();
        float width=.36f;
        try{
            width=Float.parseFloat(labelWidth==null?"36":labelWidth.getText().toString())/100f;
        }catch(Exception ignored){}

        return r.renderFinal(src,Color.WHITE,0xFFD8D8D8,append,width);
    }

    private void saveCurrent(){
        if(currentBitmap==null){
            Toast.makeText(this,"عکس انتخاب نشده",Toast.LENGTH_SHORT).show();
            return;
        }

        try{
            Bitmap out=render(currentBitmap,fields);
            saveToGallery(out,"Javdan_"+System.currentTimeMillis()+".jpg");
            Toast.makeText(this,"در Pictures/JavdanPriceLabeler ذخیره شد",Toast.LENGTH_LONG).show();
        }catch(Exception e){
            Toast.makeText(this,safeMessage(e),Toast.LENGTH_LONG).show();
        }
    }

    private void saveToGallery(Bitmap bmp,String name)throws Exception{
        ContentValues v=new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME,name);
        v.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/JavdanPriceLabeler");

        Uri u=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);
        if(u==null)throw new IOException("ساخت فایل خروجی ناموفق بود");

        try(OutputStream o=getContentResolver().openOutputStream(u)){
            if(o==null)throw new IOException("فایل خروجی باز نشد");
            bmp.compress(Bitmap.CompressFormat.JPEG,94,o);
        }
    }

    private void pickFile(String type,int request){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(type);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i,request);
    }

    private void pickFolder(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i,PICK_FOLDER);
    }

    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(result!=RESULT_OK||data==null)return;

        Uri u=data.getData();
        if(u==null)return;

        try{
            getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }catch(Exception ignored){}

        try{
            if(req==PICK_IMAGE){
                imageUri=u;
                try(InputStream in=getContentResolver().openInputStream(u)){
                    currentBitmap=BitmapFactory.decodeStream(in);
                }
                status.setText("عکس انتخاب شد");
                if(designer!=null)designer.setProductBitmap(currentBitmap);

            }else if(req==PICK_EXCEL){
                excelUri=u;
                status.setText("در حال خواندن Excel...");
                excelRows=new XlsxReader(this).readFirstSheet(u);
                headers.clear();
                if(!excelRows.isEmpty())headers.addAll(excelRows.get(0).keySet());
                refreshExcelUi();
                status.setText(excelRows.size()+" ردیف Excel خوانده شد");

            }else if(req==PICK_FOLDER){
                folderUri=u;
                status.setText("پوشه تصاویر انتخاب شد");
                Toast.makeText(this,"پوشه عکس محصولات انتخاب شد",Toast.LENGTH_SHORT).show();
            }

        }catch(Exception e){
            Toast.makeText(this,"خطا: "+safeMessage(e),Toast.LENGTH_LONG).show();
        }
    }

    private void refreshExcelUi(){
        if(codeSpinner!=null){
            codeSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,headers));
        }

        if(excelInfo!=null){
            excelInfo.removeAllViews();
            excelInfo.addView(tv("ردیف‌ها: "+excelRows.size()+" | ستون‌ها: "+headers.size(),13,false));
        }
    }

    private void validateBatch(){
        if(excelUri==null||folderUri==null||excelRows.isEmpty()){
            Toast.makeText(this,"Excel و پوشه عکس‌ها را انتخاب کن",Toast.LENGTH_LONG).show();
            return;
        }

        if(headers.isEmpty()){
            Toast.makeText(this,"ستون‌های Excel شناسایی نشده‌اند",Toast.LENGTH_LONG).show();
            return;
        }

        final int selectedPos=codeSpinner!=null?codeSpinner.getSelectedItemPosition():0;
        if(selectedPos<0||selectedPos>=headers.size()){
            Toast.makeText(this,"ستون کد محصول را انتخاب کن",Toast.LENGTH_LONG).show();
            return;
        }

        final String codeHeader=headers.get(selectedPos);
        status.setText("در حال بررسی تطبیق Excel و تصاویر...");

        new Thread(()->{
            try{
                HashMap<String,Uri> images=listTreeImages(folderUri);
                int excelCodeCount=0, matched=0, missingImages=0;
                ArrayList<String> missingCodes=new ArrayList<>();
                HashSet<String> excelCodes=new HashSet<>();

                for(LinkedHashMap<String,String> row:excelRows){
                    String code=normalizeCode(row.get(codeHeader));
                    if(code.isEmpty())continue;

                    excelCodeCount++;
                    excelCodes.add(code);

                    if(images.containsKey(code))matched++;
                    else{
                        missingImages++;
                        if(missingCodes.size()<20)missingCodes.add(code);
                    }
                }

                int extraImages=0;
                ArrayList<String> extraImageCodes=new ArrayList<>();

                for(String imageCode:images.keySet()){
                    if(!excelCodes.contains(imageCode)){
                        extraImages++;
                        if(extraImageCodes.size()<20)extraImageCodes.add(imageCode);
                    }
                }

                final int fExcelCodeCount=excelCodeCount;
                final int fMatched=matched;
                final int fMissingImages=missingImages;
                final int fExtraImages=extraImages;
                final int totalImages=images.size();

                final String missingText=missingCodes.isEmpty()?"-":android.text.TextUtils.join("، ",missingCodes);
                final String extraText=extraImageCodes.isEmpty()?"-":android.text.TextUtils.join("، ",extraImageCodes);

                runOnUiThread(()->{
                    status.setText("تطبیق انجام شد — موفق: "+fMatched+" | بدون عکس: "+fMissingImages);

                    if(excelInfo!=null){
                        excelInfo.removeAllViews();
                        excelInfo.addView(tv("ردیف‌های Excel: "+excelRows.size()+" | ستون‌ها: "+headers.size(),13,false));
                        excelInfo.addView(tv("کدهای محصول: "+fExcelCodeCount,14,true));
                        excelInfo.addView(tv("تعداد عکس‌های پوشه: "+totalImages,14,false));

                        TextView okView=tv("✓ تطبیق موفق: "+fMatched,16,true);
                        okView.setTextColor(0xFF168A3B);
                        excelInfo.addView(okView);

                        TextView missingView=tv("✕ کدهای بدون عکس: "+fMissingImages,15,true);
                        missingView.setTextColor(0xFFC62828);
                        excelInfo.addView(missingView);

                        if(fMissingImages>0)excelInfo.addView(tv("نمونه کدهای بدون عکس:\n"+missingText,12,false));

                        TextView extraView=tv("عکس‌های بدون کد در Excel: "+fExtraImages,14,true);
                        extraView.setTextColor(0xFFE28A00);
                        excelInfo.addView(extraView);

                        if(fExtraImages>0)excelInfo.addView(tv("نمونه عکس‌های اضافی:\n"+extraText,12,false));

                        if(fMatched>0){
                            Button goOutput=btn("ادامه و ساخت خروجی گروهی");
                            excelInfo.addView(goOutput);
                            goOutput.setOnClickListener(v->showOutput());
                        }
                    }

                    Toast.makeText(this,"بررسی تمام شد — تطبیق: "+fMatched+" | بدون عکس: "+fMissingImages+" | عکس اضافی: "+fExtraImages,Toast.LENGTH_LONG).show();
                });

            }catch(Exception e){
                runOnUiThread(()->{
                    status.setText("خطا در بررسی تطبیق");
                    Toast.makeText(this,"خطا: "+safeMessage(e),Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private boolean isPriceColumn(String header){
        if(header==null)return false;

        String h=header.trim()
                .replace("ي","ی")
                .replace("ك","ک")
                .toLowerCase(Locale.ROOT);

        return h.contains("قیمت");
    }

    private void runBatch(){
        if(excelRows.isEmpty()||folderUri==null){
            Toast.makeText(this,"Excel و پوشه عکس‌ها را انتخاب کن",Toast.LENGTH_LONG).show();
            return;
        }

        if(headers.isEmpty()){
            Toast.makeText(this,"ستون‌های Excel پیدا نشد",Toast.LENGTH_LONG).show();
            return;
        }

        status.setText("در حال ساخت خروجی گروهی...");
        Toast.makeText(this,"ساخت خروجی گروهی شروع شد...",Toast.LENGTH_SHORT).show();

        final boolean convert=rialToToman==null||rialToToman.isChecked();

        new Thread(()->{
            int ok=0,missing=0,errors=0;

            try{
                int selectedPos=codeSpinner!=null?codeSpinner.getSelectedItemPosition():0;
                if(selectedPos<0||selectedPos>=headers.size())selectedPos=0;

                String codeHeader=headers.get(selectedPos);
                ArrayList<String> priceHeaders=new ArrayList<>();

                for(String h:headers){
                    if(!h.equals(codeHeader)&&isPriceColumn(h))priceHeaders.add(h);
                }

                if(priceHeaders.isEmpty())throw new IOException("هیچ ستون قیمتی در Excel پیدا نشد.");

                HashMap<String,Uri> images=listTreeImages(folderUri);

                for(LinkedHashMap<String,String> row:excelRows){
                    String code=normalizeCode(row.get(codeHeader));
                    if(code.isEmpty())continue;

                    Uri img=images.get(code);
                    if(img==null){missing++;continue;}

                    try{
                        Bitmap src;
                        try(InputStream in=getContentResolver().openInputStream(img)){
                            src=BitmapFactory.decodeStream(in);
                        }

                        if(src==null){errors++;continue;}

                        ArrayList<LabelField> fs=new ArrayList<>();

                        for(int i=0;i<priceHeaders.size();i++){
                            String h=priceHeaders.get(i);
                            String raw=row.get(h);

                            if(raw==null||raw.trim().isEmpty())continue;

                            LabelField f=new LabelField(h.trim(),formatPrice(raw,convert));

                            if(i<fields.size())copyStyleOnly(fields.get(i),f);
                            else if(!fields.isEmpty())copyStyleOnly(fields.get(fields.size()-1),f);

                            fs.add(f);
                        }

                        layoutBatchFields(fs);

                        Bitmap out=render(src,fs);
                        saveToGallery(out,code+".jpg");
                        ok++;

                        src.recycle();
                        if(out!=src&&!out.isRecycled())out.recycle();

                    }catch(Exception e){errors++;}
                }

            }catch(Exception e){errors++;}

            int fok=ok,fm=missing,fe=errors;

            runOnUiThread(()->{
                status.setText("خروجی گروهی تمام شد — موفق: "+fok+" | بدون عکس: "+fm+" | خطا: "+fe);
                Toast.makeText(this,"تمام شد — موفق: "+fok+" | بدون عکس: "+fm+" | خطا: "+fe,Toast.LENGTH_LONG).show();
            });

        }).start();
    }

    private void layoutBatchFields(ArrayList<LabelField> fs){
        int n=Math.max(1,fs.size());
        float top=.08f,gap=.025f;
        float h=Math.min(.18f,(.82f-gap*(n-1))/n);

        for(int i=0;i<fs.size();i++){
            LabelField f=fs.get(i);
            f.x=.04f; f.w=.92f; f.h=h;
            f.y=top+i*(h+gap);
        }
    }

    private void copyStyleOnly(LabelField from,LabelField to){
        to.titleSize=from.titleSize;
        to.priceSize=from.priceSize;
        to.tomanSize=from.tomanSize;

        to.titleColor=from.titleColor;
        to.priceColor=from.priceColor;
        to.tomanColor=from.tomanColor;

        to.backgroundColor=from.backgroundColor;
        to.borderColor=from.borderColor;
        to.borderWidth=from.borderWidth;
        to.cornerRadius=from.cornerRadius;

        to.paddingHorizontal=from.paddingHorizontal;
        to.paddingVertical=from.paddingVertical;
        to.titlePriceGap=from.titlePriceGap;

        to.strike=from.strike;
        to.showToman=from.showToman;
        to.visible=from.visible;

        to.titleBold=from.titleBold;
        to.titleItalic=from.titleItalic;
        to.priceBold=from.priceBold;
        to.priceItalic=from.priceItalic;

        to.textAlign=from.textAlign;
        to.titleFont=from.titleFont;
        to.priceFont=from.priceFont;

        to.showTitle=from.showTitle;
        to.showPrice=from.showPrice;
    }

    private HashMap<String,Uri> listTreeImages(Uri tree)throws Exception{
        HashMap<String,Uri> map=new HashMap<>();

        String docId=DocumentsContract.getTreeDocumentId(tree);
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,docId);

        String[] cols={
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };

        try(Cursor c=getContentResolver().query(children,cols,null,null,null)){
            if(c!=null){
                while(c.moveToNext()){
                    String id=c.getString(0);
                    String name=c.getString(1);
                    String mime=c.getString(2);

                    if(mime!=null&&mime.startsWith("image/")){
                        String stem=name==null?"":name;
                        int dot=stem.lastIndexOf('.');
                        if(dot>0)stem=stem.substring(0,dot);

                        Uri child=DocumentsContract.buildDocumentUriUsingTree(tree,id);
                        map.put(normalizeCode(stem),child);
                    }
                }
            }
        }

        return map;
    }

    private String normalizeCode(String s){
        if(s==null)return "";
        s=s.trim();
        if(s.endsWith(".0"))s=s.substring(0,s.length()-2);
        return s;
    }

    private void saveTemplate(){
        try{
            JSONArray a=new JSONArray();
            for(LabelField f:fields)a.put(f.toJson());

            getSharedPreferences("javdan",MODE_PRIVATE)
                    .edit()
                    .putString("fields",a.toString())
                    .apply();

        }catch(Exception ignored){}
    }

    private void loadTemplate(){
        fields.clear();

        String s=getSharedPreferences("javdan",MODE_PRIVATE)
                .getString("fields","");

        if(!s.isEmpty()){
            try{
                JSONArray a=new JSONArray(s);
                for(int i=0;i<a.length();i++){
                    fields.add(LabelField.fromJson(a.getJSONObject(i)));
                }
            }catch(Exception ignored){}
        }

        if(fields.isEmpty())makeDefaults();
    }

    private int fontIndex(String font){
        if(font==null)return 0;

        for(int i=0;i<FONT_VALUES.length;i++){
            if(FONT_VALUES[i].equals(font))return i;
        }

        return 0;
    }

    private String pct(float value){
        return String.valueOf(Math.round(value*100f));
    }

    private float parsePercent(EditText e,float fallback){
        try{
            String s=e.getText().toString().trim();
            if(s.isEmpty())return fallback;
            return Float.parseFloat(s)/100f;
        }catch(Exception ex){return fallback;}
    }

    private int parseIntSafe(EditText e,int fallback,int min,int max){
        try{
            int v=Integer.parseInt(e.getText().toString().trim());
            return clampInt(v,min,max);
        }catch(Exception ex){return fallback;}
    }

    private int parseColorSafe(String raw,int fallback){
        try{
            String s=raw==null?"":raw.trim();
            if(s.isEmpty())return fallback;
            if(!s.startsWith("#"))s="#"+s;
            return Color.parseColor(s);
        }catch(Exception e){return fallback;}
    }

    private String colorToHex(int color){
        return String.format(Locale.US,"#%08X",color);
    }

    private float clamp(float v,float min,float max){
        return Math.max(min,Math.min(max,v));
    }

    private int clampInt(int v,int min,int max){
        return Math.max(min,Math.min(max,v));
    }

    private String safeMessage(Exception e){
        if(e==null)return "خطای نامشخص";
        String m=e.getMessage();
        return (m==null||m.trim().isEmpty())?e.getClass().getSimpleName():m;
    }
}
