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
    CheckBox rialToToman, appendMode, batchCropCheck;
    int selectedField=-1;

    final String[] FONT_VALUES={"DEFAULT","SANS_SERIF","SERIF","MONOSPACE"};
    final String[] FONT_LABELS={"پیش‌فرض","Sans Serif","Serif","Monospace"};
    final String[] ALIGN_LABELS={"راست","وسط","چپ"};

    // Crop is kept separately for each manually-selected image URI.
    HashMap<String,float[]> cropByImage=new HashMap<>();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.WHITE);
        loadTemplate();
        loadCropMap();
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
        float top=.045f, gap=.018f;
        float h=Math.min(.205f,(.91f-gap*(n-1))/n);

        for(int i=0;i<fields.size();i++){
            LabelField f=fields.get(i);
            f.x=.025f; f.w=.95f; f.h=h;
            f.y=top+i*(h+gap);
        }
    }

    private void showDesigner(){
        if(manualRows!=null&&manualRows.getParent()!=null)syncManualRows();
        clear();

        body.addView(section("طراح حرفه‌ای برچسب"));
        body.addView(tv("فضای لیبل بزرگ‌تر شده. روی عنوان/قیمت/تومان بزن؛ روی عکس بکش برای جابه‌جایی و از دایره بنفش گوشه عکس برای Resize استفاده کن.",13,false));

        designer=new LabelDesignerView(this);
        designer.setFields(fields);
        designer.setProductBitmap(currentBitmap);
        restoreCropForCurrentImage();
        body.addView(designer,new LinearLayout.LayoutParams(-1,1050));

        designer.setListener(new LabelDesignerView.Listener(){
            @Override public void onFieldSelected(int i){
                selectedField=i;
                showFieldEditor();
            }

            @Override public void onChanged(){
                saveCurrentImageCrop();
                saveTemplate();
            }

            @Override public void onTextClicked(int fieldIndex,int part){
                selectedField=fieldIndex;
                showFontSizeDialog(fieldIndex,part);
            }
        });

        body.addView(section("تصویر محصول"));

        LinearLayout imageTools=row();
        Button cropButton=btn("✂ کراپ تصویر");
        Button resetCrop=btn("بازنشانی کراپ");
        imageTools.addView(cropButton,new LinearLayout.LayoutParams(0,-2,1));
        imageTools.addView(resetCrop,new LinearLayout.LayoutParams(0,-2,1));
        body.addView(imageTools);

        cropButton.setOnClickListener(v->{
            boolean on=!designer.isCropMode();
            designer.setCropMode(on);
            cropButton.setText(on?"✓ پایان کراپ":"✂ کراپ تصویر");
            if(on){
                Toast.makeText(this,"لبه‌های آبی را بکش. دایره بنفش گوشه عکس همچنان Resize را انجام می‌دهد.",Toast.LENGTH_LONG).show();
            }else{
                saveCurrentImageCrop();
            }
        });

        resetCrop.setOnClickListener(v->{
            designer.resetCrop();
            saveCurrentImageCrop();
        });

        EditText productX=numberEdit(pct(designer.productX),"X %");
        EditText productY=numberEdit(pct(designer.productY),"Y %");
        EditText productW=numberEdit(pct(designer.productW),"عرض %");
        EditText productH=numberEdit(pct(designer.productH),"ارتفاع %");

        addLabeledEdit(body,"موقعیت افقی تصویر %",productX);
        addLabeledEdit(body,"موقعیت عمودی تصویر %",productY);
        addLabeledEdit(body,"عرض تصویر %",productW);
        addLabeledEdit(body,"ارتفاع تصویر %",productH);

        Button canvasColorButton=makeColorButton("رنگ پس‌زمینه طراح",designer.canvasBackground,color->{
            designer.canvasBackground=color;
            designer.invalidate();
        });
        body.addView(canvasColorButton);

        Button applyProduct=btn("اعمال تنظیمات عددی تصویر");
        body.addView(applyProduct);
        applyProduct.setOnClickListener(v->{
            designer.productX=clamp(parsePercent(productX,designer.productX),0f,.95f);
            designer.productY=clamp(parsePercent(productY,designer.productY),0f,.95f);
            designer.productW=clamp(parsePercent(productW,designer.productW),.05f,1f-designer.productX);
            designer.productH=clamp(parsePercent(productH,designer.productH),.05f,1f-designer.productY);
            designer.invalidate();
        });

        Button sample=btn("انتخاب / تغییر عکس نمونه");
        body.addView(sample);
        sample.setOnClickListener(v->pickFile("image/*",PICK_IMAGE));

        body.addView(section("تنظیمات کلی خروجی"));

        appendMode=new CheckBox(this);
        appendMode.setText("لیبل بیرون تصویر");
        appendMode.setChecked(true);
        body.addView(appendMode);

        labelWidth=numberEdit("44","عرض لیبل %");
        addLabeledEdit(body,"عرض لیبل %",labelWidth);

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
        Button minus=btn("▼");
        Button plus=btn("▲");
        EditText value=numberEdit(String.valueOf(current),"سایز");
        value.setGravity(Gravity.CENTER);

        sizeRow.addView(minus,new LinearLayout.LayoutParams(0,-2,1));
        sizeRow.addView(value,new LinearLayout.LayoutParams(0,-2,1.5f));
        sizeRow.addView(plus,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(sizeRow);

        final int fallback=current;

        Runnable applySize=()->{
            int newSize=clampInt(parseIntText(value,fallback),8,140);
            value.setText(String.valueOf(newSize));

            if(part==0)f.titleSize=newSize;
            else if(part==2)f.tomanSize=newSize;
            else f.priceSize=newSize;

            if(designer!=null)designer.invalidate();
            saveTemplate();
        };

        minus.setOnClickListener(v->{
            value.setText(String.valueOf(Math.max(8,parseIntText(value,fallback)-1)));
            applySize.run();
        });

        plus.setOnClickListener(v->{
            value.setText(String.valueOf(Math.min(140,parseIntText(value,fallback)+1)));
            applySize.run();
        });

        Button apply=btn("اعمال");
        root.addView(apply);
        apply.setOnClickListener(v->{ applySize.run(); dialog.dismiss(); });

        dialog.setContentView(root);
        dialog.show();

        Window w=dialog.getWindow();
        if(w!=null)w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int parseIntText(EditText edit,int fallback){
        try{return Integer.parseInt(edit.getText().toString().trim());}
        catch(Exception e){return fallback;}
    }

    private void showFieldEditor(){
        if(fieldEditorContainer==null||selectedField<0||selectedField>=fields.size())return;
        fieldEditorContainer.removeAllViews();

        LabelField f=fields.get(selectedField);
        fieldEditorContainer.addView(tv("در حال ویرایش: "+f.name,14,true));
        fieldEditorContainer.addView(tv("اندازه فونت را با لمس مستقیم متن داخل طراح تغییر بده.",12,false));

        EditText name=textEdit(f.name,"نام کادر");
        EditText val=textEdit(f.value,"قیمت");
        addLabeledEdit(fieldEditorContainer,"عنوان",name);
        addLabeledEdit(fieldEditorContainer,"قیمت",val);

        Spinner titleFont=makeSpinner(FONT_LABELS,fontIndex(f.titleFont));
        Spinner priceFont=makeSpinner(FONT_LABELS,fontIndex(f.priceFont));
        Spinner align=makeSpinner(ALIGN_LABELS,clampInt(f.textAlign,0,2));
        addLabeledSpinner(fieldEditorContainer,"فونت عنوان",titleFont);
        addLabeledSpinner(fieldEditorContainer,"فونت قیمت",priceFont);
        addLabeledSpinner(fieldEditorContainer,"تراز متن",align);

        fieldEditorContainer.addView(section("رنگ‌بندی لمسی"));

        Button titleColor=makeColorButton("رنگ عنوان",f.titleColor,color->f.titleColor=color);
        Button priceColor=makeColorButton("رنگ قیمت",f.priceColor,color->f.priceColor=color);
        Button tomanColor=makeColorButton("رنگ تومان",f.tomanColor,color->f.tomanColor=color);
        Button backgroundColor=makeColorButton("رنگ پس‌زمینه کادر",f.backgroundColor,color->f.backgroundColor=color);
        Button borderColor=makeColorButton("رنگ حاشیه کادر",f.borderColor,color->f.borderColor=color);

        fieldEditorContainer.addView(titleColor);
        fieldEditorContainer.addView(priceColor);
        fieldEditorContainer.addView(tomanColor);
        fieldEditorContainer.addView(backgroundColor);
        fieldEditorContainer.addView(borderColor);

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
            f.titleFont=FONT_VALUES[titleFont.getSelectedItemPosition()];
            f.priceFont=FONT_VALUES[priceFont.getSelectedItemPosition()];
            f.textAlign=align.getSelectedItemPosition();
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
            saveTemplate();
        });

        Button copyStyle=btn("اعمال همین استایل برای همه کادرها");
        fieldEditorContainer.addView(copyStyle);
        copyStyle.setOnClickListener(v->{
            for(int i=0;i<fields.size();i++){
                if(i!=selectedField)copyStyleOnly(f,fields.get(i));
            }
            designer.setFields(fields);
            saveTemplate();
            Toast.makeText(this,"استایل برای همه کادرها اعمال شد",Toast.LENGTH_SHORT).show();
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

        batchCropCheck=new CheckBox(this);
        batchCropCheck.setText("اعمال کراپ عکس نمونه روی همه تصاویر خروجی گروهی");
        batchCropCheck.setChecked(false);
        body.addView(batchCropCheck);

        body.addView(tv("خاموش = هر تصویر گروهی بدون کراپ عمومی ساخته می‌شود. روشن = کراپ فعلی نمونه روی همه اعمال می‌شود.",12,false));

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

        Bitmap out=render(currentBitmap,fields,true);
        ImageView iv=new ImageView(this);
        iv.setAdjustViewBounds(true); iv.setImageBitmap(out);
        body.addView(iv,Math.min(body.getWidth()>0?body.getWidth():1000,1000),-2);
        previewStatus.setText("پیش‌نمایش ساخته شد.");
    }

    private Bitmap render(Bitmap src,ArrayList<LabelField> useFields,boolean useCurrentCrop){
        LabelDesignerView r=new LabelDesignerView(this);
        r.setFields(useFields);

        if(useCurrentCrop && designer!=null){
            r.setCrop(designer.cropLeft,designer.cropTop,designer.cropRight,designer.cropBottom);
        }else if(useCurrentCrop){
            float[] c=getCurrentImageCrop();
            r.setCrop(c[0],c[1],c[2],c[3]);
        }else{
            r.setCrop(0f,0f,1f,1f);
        }

        boolean append=appendMode==null||appendMode.isChecked();
        float width=.44f;
        try{
            width=Float.parseFloat(labelWidth==null?"44":labelWidth.getText().toString())/100f;
        }catch(Exception ignored){}

        return r.renderFinal(src,Color.WHITE,0xFFD8D8D8,append,width);
    }

    private void saveCurrent(){
        if(currentBitmap==null){
            Toast.makeText(this,"عکس انتخاب نشده",Toast.LENGTH_SHORT).show();
            return;
        }

        try{
            Bitmap out=render(currentBitmap,fields,true);
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
                saveCurrentImageCrop();
                imageUri=u;

                try(InputStream in=getContentResolver().openInputStream(u)){
                    currentBitmap=BitmapFactory.decodeStream(in);
                }

                status.setText("عکس انتخاب شد");

                if(designer!=null){
                    designer.setProductBitmap(currentBitmap);
                    restoreCropForCurrentImage();
                }

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
                for(String imageCode:images.keySet()){
                    if(!excelCodes.contains(imageCode))extraImages++;
                }

                final int fMatched=matched;
                final int fMissingImages=missingImages;
                final int fExtraImages=extraImages;
                final int totalImages=images.size();

                runOnUiThread(()->{
                    status.setText("تطبیق انجام شد — موفق: "+fMatched+" | بدون عکس: "+fMissingImages);

                    if(excelInfo!=null){
                        excelInfo.removeAllViews();
                        excelInfo.addView(tv("ردیف‌های Excel: "+excelRows.size()+" | عکس‌ها: "+totalImages,13,false));

                        TextView okView=tv("✓ تطبیق موفق: "+fMatched,16,true);
                        okView.setTextColor(0xFF168A3B);
                        excelInfo.addView(okView);

                        TextView missingView=tv("✕ کدهای بدون عکس: "+fMissingImages,15,true);
                        missingView.setTextColor(0xFFC62828);
                        excelInfo.addView(missingView);

                        TextView extraView=tv("عکس‌های بدون کد در Excel: "+fExtraImages,14,true);
                        extraView.setTextColor(0xFFE28A00);
                        excelInfo.addView(extraView);

                        if(fMatched>0){
                            Button goOutput=btn("ادامه و ساخت خروجی گروهی");
                            excelInfo.addView(goOutput);
                            goOutput.setOnClickListener(v->showOutput());
                        }
                    }
                });

            }catch(Exception e){
                runOnUiThread(()->Toast.makeText(this,"خطا: "+safeMessage(e),Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private boolean isPriceColumn(String header){
        if(header==null)return false;
        String h=header.trim().replace("ي","ی").replace("ك","ک").toLowerCase(Locale.ROOT);
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
        final boolean convert=rialToToman==null||rialToToman.isChecked();
        final boolean applyGroupCrop=batchCropCheck!=null&&batchCropCheck.isChecked();

        final float[] groupCrop = designer!=null
                ? new float[]{designer.cropLeft,designer.cropTop,designer.cropRight,designer.cropBottom}
                : getCurrentImageCrop();

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
                        int styleIndex=0;

                        for(String h:priceHeaders){
                            String raw=row.get(h);
                            if(raw==null||raw.trim().isEmpty())continue;

                            LabelField f=new LabelField(h.trim(),formatPrice(raw,convert));

                            if(styleIndex<fields.size())copyStyleOnly(fields.get(styleIndex),f);
                            else if(!fields.isEmpty())copyStyleOnly(fields.get(fields.size()-1),f);

                            fs.add(f);
                            styleIndex++;
                        }

                        layoutBatchFields(fs);

                        LabelDesignerView renderer=new LabelDesignerView(this);
                        renderer.setFields(fs);

                        if(applyGroupCrop){
                            renderer.setCrop(groupCrop[0],groupCrop[1],groupCrop[2],groupCrop[3]);
                        }else{
                            renderer.setCrop(0f,0f,1f,1f);
                        }

                        boolean append=appendMode==null||appendMode.isChecked();
                        float width=.44f;
                        try{
                            width=Float.parseFloat(labelWidth==null?"44":labelWidth.getText().toString())/100f;
                        }catch(Exception ignored){}

                        Bitmap out=renderer.renderFinal(src,Color.WHITE,0xFFD8D8D8,append,width);
                        saveToGallery(out,code+".jpg");
                        ok++;

                        if(src!=null&&!src.isRecycled())src.recycle();
                        if(out!=null&&!out.isRecycled())out.recycle();

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
        float top=.045f,gap=.018f;
        float h=Math.min(.205f,(.91f-gap*(n-1))/n);

        for(int i=0;i<fs.size();i++){
            LabelField f=fs.get(i);
            f.x=.025f; f.w=.95f; f.h=h;
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

    private void saveCurrentImageCrop(){
        if(imageUri==null||designer==null)return;
        cropByImage.put(imageUri.toString(),new float[]{
                designer.cropLeft,designer.cropTop,designer.cropRight,designer.cropBottom
        });
        saveCropMap();
    }

    private void restoreCropForCurrentImage(){
        if(designer==null)return;
        float[] c=getCurrentImageCrop();
        designer.setCrop(c[0],c[1],c[2],c[3]);
    }

    private float[] getCurrentImageCrop(){
        if(imageUri==null)return new float[]{0f,0f,1f,1f};
        float[] c=cropByImage.get(imageUri.toString());
        return c==null?new float[]{0f,0f,1f,1f}:c;
    }

    private void saveCropMap(){
        try{
            JSONObject root=new JSONObject();
            for(Map.Entry<String,float[]> e:cropByImage.entrySet()){
                float[] c=e.getValue();
                JSONArray a=new JSONArray();
                a.put(c[0]);a.put(c[1]);a.put(c[2]);a.put(c[3]);
                root.put(e.getKey(),a);
            }
            getSharedPreferences("javdan",MODE_PRIVATE).edit()
                    .putString("cropByImage",root.toString()).apply();
        }catch(Exception ignored){}
    }

    private void loadCropMap(){
        cropByImage.clear();
        String raw=getSharedPreferences("javdan",MODE_PRIVATE).getString("cropByImage","");
        if(raw.isEmpty())return;

        try{
            JSONObject root=new JSONObject(raw);
            Iterator<String> keys=root.keys();
            while(keys.hasNext()){
                String k=keys.next();
                JSONArray a=root.optJSONArray(k);
                if(a!=null&&a.length()>=4){
                    cropByImage.put(k,new float[]{
                            (float)a.optDouble(0,0),
                            (float)a.optDouble(1,0),
                            (float)a.optDouble(2,1),
                            (float)a.optDouble(3,1)
                    });
                }
            }
        }catch(Exception ignored){}
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

        String s=getSharedPreferences("javdan",MODE_PRIVATE).getString("fields","");

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

    interface ColorSelectedListener{
        void onColorSelected(int color);
    }

    private Button makeColorButton(String title,int currentColor,ColorSelectedListener listener){
        Button b=btn(title);
        applyButtonColor(b,currentColor);

        b.setOnClickListener(v->showColorPalette(title,currentColor,color->{
            listener.onColorSelected(color);
            saveTemplate();
            if(designer!=null)designer.invalidate();
            if(fieldEditorContainer!=null&&selectedField>=0)showFieldEditor();
        }));

        return b;
    }

    private void applyButtonColor(Button button,int color){
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(18);
        bg.setStroke(2,0xFFBDBDBD);
        button.setBackground(bg);

        double darkness=1-(0.299*Color.red(color)+0.587*Color.green(color)+0.114*Color.blue(color))/255;
        button.setTextColor(darkness>=0.5?Color.WHITE:Color.BLACK);
    }

    private void showColorPalette(String title,int currentColor,final ColorSelectedListener listener){
        final Dialog dialog=new Dialog(this);

        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(30,25,30,25);

        TextView titleView=tv(title,20,true);
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView);

        final int[] colors={
                0xFFFFFFFF,0xFFF5F5F5,0xFFE0E0E0,0xFF9E9E9E,0xFF616161,
                0xFF212121,0xFFFFCDD2,0xFFEF5350,0xFFC62828,0xFF8E0000,
                0xFFFFE0B2,0xFFFF9800,0xFFEF6C00,0xFFFFF9C4,0xFFFFEB3B,
                0xFFF9A825,0xFFC8E6C9,0xFF66BB6A,0xFF2E7D32,0xFF1B5E20,
                0xFFB2DFDB,0xFF26A69A,0xFF00796B,0xFFBBDEFB,0xFF42A5F5,
                0xFF1976D2,0xFF0D47A1,0xFFD1C4E9,0xFF7E57C2,0xFF512DA8,
                0xFFF8BBD0,0xFFEC407A,0xFFC2185B,0xFFFFF3E0,0xFFE3F2FD,
                0xFFE8F5E9,0xFFF3E5F5
        };

        GridLayout grid=new GridLayout(this);
        grid.setColumnCount(5);

        int size=(int)(55*getResources().getDisplayMetrics().density);
        int margin=(int)(5*getResources().getDisplayMetrics().density);

        for(final int color:colors){
            TextView box=new TextView(this);

            GradientDrawable bg=new GradientDrawable();
            bg.setColor(color);
            bg.setCornerRadius(14);
            bg.setStroke(color==currentColor?5:2,color==currentColor?0xFF1976D2:0xFFBDBDBD);
            box.setBackground(bg);

            GridLayout.LayoutParams p=new GridLayout.LayoutParams();
            p.width=size; p.height=size;
            p.setMargins(margin,margin,margin,margin);
            box.setLayoutParams(p);

            box.setOnClickListener(v->{
                listener.onColorSelected(color);
                dialog.dismiss();
            });

            grid.addView(box);
        }

        root.addView(grid);

        Button cancel=btn("انصراف");
        root.addView(cancel);
        cancel.setOnClickListener(v->dialog.dismiss());

        dialog.setContentView(root);
        dialog.show();

        Window window=dialog.getWindow();
        if(window!=null)window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
