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

    static final int PICK_EXCEL = 10, PICK_IMAGE = 11, PICK_FOLDER = 12;

    LinearLayout body, manualRows, excelInfo, fieldEditorContainer;
    Button tabData, tabDesigner, tabOutput;
    RadioButton modeExcel, modeManual;

    Uri excelUri, imageUri, folderUri;
    Bitmap currentBitmap;
    LabelDesignerView designer;

    ArrayList<LabelField> fields = new ArrayList<>();
    ArrayList<LinkedHashMap<String,String>> excelRows = new ArrayList<>();
    ArrayList<String> headers = new ArrayList<>();
    ArrayList<EditText> nameEdits = new ArrayList<>(), valueEdits = new ArrayList<>();

    Spinner codeSpinner;
    TextView status, previewStatus;
    CheckBox rialToToman, appendMode, groupCropCheck;

    int selectedField = -1;

    // تنظیمات خروجی / طراح
    float settingLabelWidth = 0.28f;
    float settingFieldGap = 0.014f;
    int settingOuterRadius = 22;
    int settingOuterBorderWidth = 3;
    int settingOuterColor = 0xFF181818;
    int settingOuterBorderColor = 0xFF3A3A3A;
    boolean settingAutoHeight = true;

    float settingProductX = 0.02f;
    float settingProductY = 0.08f;
    float settingProductW = 0.62f;
    float settingProductH = 0.84f;

    boolean settingCropEnabled = false;
    float settingCropLeft = 0f;
    float settingCropTop = 0f;
    float settingCropRight = 1f;
    float settingCropBottom = 1f;

    final String[] FONT_VALUES = {"DEFAULT","SANS_SERIF","SERIF","MONOSPACE"};
    final String[] FONT_LABELS = {"پیش‌فرض","Sans Serif","Serif","Monospace"};
    final String[] ALIGN_LABELS = {"راست","وسط","چپ"};

    @Override
    public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.WHITE);
        loadTemplate();
        loadDesignerSettings();
        buildUi();
        showData();
    }

    private TextView tv(String s, int sp, boolean bold){
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(0xFF222222);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(8,8,8,8);
        return t;
    }

    private TextView section(String s){
        TextView t = tv(s, 17, true);
        t.setPadding(8,22,8,12);
        return t;
    }

    private Button btn(String s){
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }

    private LinearLayout row(){
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private EditText numberEdit(String value, String hint){
        EditText e = new EditText(this);
        e.setText(value);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    private EditText textEdit(String value, String hint){
        EditText e = new EditText(this);
        e.setText(value);
        e.setHint(hint);
        e.setSingleLine(true);
        return e;
    }

    private void addLabeledEdit(LinearLayout parent, String label, EditText edit){
        LinearLayout r = row();
        r.addView(tv(label,13,false), new LinearLayout.LayoutParams(0,-2,1));
        r.addView(edit, new LinearLayout.LayoutParams(0,-2,1.35f));
        parent.addView(r);
    }

    private Spinner makeSpinner(String[] labels, int selected){
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        if (selected >= 0 && selected < labels.length) s.setSelection(selected);
        return s;
    }

    private void addLabeledSpinner(LinearLayout parent, String label, Spinner spinner){
        LinearLayout r = row();
        r.addView(tv(label,13,false), new LinearLayout.LayoutParams(0,-2,1));
        r.addView(spinner, new LinearLayout.LayoutParams(0,-2,1.35f));
        parent.addView(r);
    }

    private void buildUi(){
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(14,14,14,10);

        root.addView(tv("Javdan Price Labeler",22,true));
        root.addView(tv("Excel + ورود دستی + طراح حرفه‌ای لیبل",13,false));

        LinearLayout tabs = row();
        tabData = btn("Excel / دستی");
        tabDesigner = btn("طراح برچسب");
        tabOutput = btn("خروجی");

        tabs.addView(tabData,new LinearLayout.LayoutParams(0,-2,1));
        tabs.addView(tabDesigner,new LinearLayout.LayoutParams(0,-2,1));
        tabs.addView(tabOutput,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(tabs);

        ScrollView sv = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(4,10,4,90);
        sv.addView(body);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        status = tv("آفلاین • عکس اصلی تغییر نمی‌کند",12,false);
        root.addView(status);

        setContentView(root);

        tabData.setOnClickListener(v -> showData());
        tabDesigner.setOnClickListener(v -> showDesigner());
        tabOutput.setOnClickListener(v -> showOutput());
    }

    private void clear(){
        body.removeAllViews();
    }

    private void showData(){
        clear();

        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.HORIZONTAL);

        modeExcel = new RadioButton(this);
        modeExcel.setText("Excel");

        modeManual = new RadioButton(this);
        modeManual.setText("ورود دستی");

        rg.addView(modeExcel);
        rg.addView(modeManual);
        body.addView(rg);

        modeManual.setChecked(excelUri == null);
        modeExcel.setChecked(excelUri != null);

        modeExcel.setOnClickListener(v -> renderDataContent(true));
        modeManual.setOnClickListener(v -> renderDataContent(false));

        renderDataContent(modeExcel.isChecked());
    }

    private void renderDataContent(boolean excel){
        while (body.getChildCount() > 1) body.removeViewAt(1);

        if (excel){
            Button pe = btn("انتخاب فایل Excel (.xlsx)");
            body.addView(pe);
            pe.setOnClickListener(v -> pickFile(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    PICK_EXCEL
            ));

            Button pf = btn("انتخاب پوشه عکس محصولات");
            body.addView(pf);
            pf.setOnClickListener(v -> pickFolder());

            body.addView(tv("ستون کد محصول:",14,true));

            codeSpinner = new Spinner(this);
            body.addView(codeSpinner);

            rialToToman = new CheckBox(this);
            rialToToman.setText("تبدیل ریال به تومان (÷۱۰)");
            rialToToman.setChecked(true);
            body.addView(rialToToman);

            excelInfo = new LinearLayout(this);
            excelInfo.setOrientation(LinearLayout.VERTICAL);
            body.addView(excelInfo);

            if (!headers.isEmpty()) refreshExcelUi();

            Button validate = btn("بررسی تطبیق Excel و تصاویر");
            body.addView(validate);
            validate.setOnClickListener(v -> validateBatch());

        } else {
            body.addView(section("حالت دستی"));

            Button pi = btn("انتخاب عکس از گالری یا Files");
            body.addView(pi);
            pi.setOnClickListener(v -> pickFile("image/*", PICK_IMAGE));

            rialToToman = new CheckBox(this);
            rialToToman.setText("قیمت‌های واردشده ریال هستند؛ تبدیل به تومان ÷۱۰");
            body.addView(rialToToman);

            manualRows = new LinearLayout(this);
            manualRows.setOrientation(LinearLayout.VERTICAL);
            body.addView(manualRows);

            if (fields.isEmpty()) makeDefaults();
            rebuildManualRows();

            Button add = btn("+ افزودن قیمت جدید");
            body.addView(add);
            add.setOnClickListener(v -> {
                syncManualRows();
                fields.add(new LabelField("قیمت جدید",""));
                relayoutFields();
                rebuildManualRows();
            });

            Button go = btn("ذخیره قیمت‌ها و رفتن به طراح");
            body.addView(go);
            go.setOnClickListener(v -> {
                syncManualRows();
                showDesigner();
            });
        }
    }

    private void makeDefaults(){
        fields.clear();

        LabelField f1 = new LabelField("قیمت مصرف کننده","");
        f1.strike = false;
        f1.backgroundColor = 0xFFFFD600;
        f1.borderColor = 0xFFE0A800;
        f1.priceColor = 0xFF111111;
        f1.tomanColor = 0xFF111111;
        f1.titleColor = 0xFF111111;
        f1.titleBold = true;
        f1.priceBold = true;

        LabelField f2 = new LabelField("قیمت پایه","");
        f2.backgroundColor = 0xFFE91319;
        f2.borderColor = 0xFFB70E13;
        f2.priceColor = 0xFFFFFFFF;
        f2.tomanColor = 0xFFFFFFFF;
        f2.titleColor = 0xFFFFFFFF;
        f2.titleBold = true;
        f2.priceBold = true;

        LabelField f3 = new LabelField("قیمت حجم متوسط","");
        f3.backgroundColor = 0xFF111111;
        f3.borderColor = 0xFFFFFFFF;
        f3.priceColor = 0xFFFFFFFF;
        f3.tomanColor = 0xFFFFFFFF;
        f3.titleColor = 0xFFFFFFFF;
        f3.titleBold = true;
        f3.priceBold = true;

        LabelField f4 = new LabelField("قیمت حجم بالا","");
        f4.backgroundColor = 0xFF63BF67;
        f4.borderColor = 0xFF2F8C3A;
        f4.priceColor = 0xFF111111;
        f4.tomanColor = 0xFF111111;
        f4.titleColor = 0xFF111111;
        f4.titleBold = true;
        f4.priceBold = true;

        fields.add(f1);
        fields.add(f2);
        fields.add(f3);
        fields.add(f4);

        relayoutFields();
    }

    private void rebuildManualRows(){
        manualRows.removeAllViews();
        nameEdits.clear();
        valueEdits.clear();

        for (int i=0;i<fields.size();i++){
            LabelField f = fields.get(i);
            LinearLayout r = row();

            EditText n = new EditText(this);
            n.setText(f.name);
            n.setHint("نام قیمت");

            EditText v = new EditText(this);
            v.setText(f.value);
            v.setHint("قیمت");
            v.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

            Button del = btn("×");
            final int idx = i;
            del.setOnClickListener(x -> {
                syncManualRows();
                if (fields.size() > 1) fields.remove(idx);
                relayoutFields();
                rebuildManualRows();
            });

            r.addView(n,new LinearLayout.LayoutParams(0,-2,1.4f));
            r.addView(v,new LinearLayout.LayoutParams(0,-2,1));
            r.addView(del,new LinearLayout.LayoutParams(-2,-2));

            manualRows.addView(r);
            nameEdits.add(n);
            valueEdits.add(v);
        }
    }

    private void syncManualRows(){
        for (int i=0;i<Math.min(fields.size(),nameEdits.size());i++){
            fields.get(i).name = nameEdits.get(i).getText().toString().trim();
            String raw = valueEdits.get(i).getText().toString().replace(",","").trim();
            fields.get(i).value = formatPrice(raw, rialToToman != null && rialToToman.isChecked());
        }
        saveTemplate();
    }

    private String formatPrice(String raw, boolean rial){
        if (raw == null || raw.isEmpty()) return "";
        try {
            String clean = raw.replace(",","").replace("٬","").trim();
            double d = Double.parseDouble(clean);
            if (rial) d /= 10.0;
            return new DecimalFormat("#,###").format(Math.round(d));
        } catch (Exception e){
            return raw;
        }
    }

    private void relayoutFields(){
        int n = Math.max(1,fields.size());
        float top = .02f;
        float gap = .012f;
        float h = Math.min(.22f, (.96f-gap*(n-1))/n);

        for (int i=0;i<fields.size();i++){
            LabelField f = fields.get(i);
            f.x = .02f;
            f.w = .96f;
            f.h = h;
            f.y = top + i*(h+gap);
        }
    }

    private void showDesigner(){
        if (manualRows != null && manualRows.getParent() != null) syncManualRows();
        clear();

        body.addView(section("طراح حرفه‌ای برچسب"));
        body.addView(tv("استایل نمونه: قاب اصلی جمع‌وجور + Auto Height + کادرهای فشرده. روی عنوان یا قیمت بزن تا سایز فونت را سریع تغییر بدهی.",13,false));

        designer = new LabelDesignerView(this);
        applyDesignerSettingsToView(designer);
        designer.setFields(fields);
        designer.setProductBitmap(currentBitmap);

        body.addView(designer, new LinearLayout.LayoutParams(-1, 1020));

        designer.setListener(new LabelDesignerView.Listener() {
            @Override
            public void onFieldSelected(int i) {
                selectedField = i;
                showFieldEditor();
            }

            @Override
            public void onChanged() {
                syncDesignerSettingsFromView();
                saveTemplate();
                saveDesignerSettings();
            }

            @Override
            public void onTextClicked(int fieldIndex, int textType) {
                selectedField = fieldIndex;
                showQuickTextSizeDialog(fieldIndex, textType);
            }
        });

        body.addView(section("تنظیمات لیبل"));

        EditText labelWidth = numberEdit(String.valueOf(Math.round(settingLabelWidth*100f)), "28");
        EditText gapPx = numberEdit(String.valueOf(Math.round(settingFieldGap*1000f)), "14");
        EditText radius = numberEdit(String.valueOf(settingOuterRadius), "22");
        EditText borderWidth = numberEdit(String.valueOf(settingOuterBorderWidth), "3");

        addLabeledEdit(body,"عرض لیبل از کل تصویر %",labelWidth);
        addLabeledEdit(body,"فاصله بین کادرها (کمتر = جمع‌وجورتر)",gapPx);
        addLabeledEdit(body,"گردی گوشه قاب اصلی",radius);
        addLabeledEdit(body,"ضخامت قاب اصلی",borderWidth);

        Button outerColorBtn = colorButton("رنگ قاب اصلی", settingOuterColor);
        body.addView(outerColorBtn);
        outerColorBtn.setOnClickListener(v -> showColorPalette("رنگ قاب اصلی", settingOuterColor, color -> {
            settingOuterColor = color;
            if (designer != null) {
                designer.outerTagColor = color;
                designer.invalidate();
            }
            refreshColorButton(outerColorBtn,"رنگ قاب اصلی",color);
            saveDesignerSettings();
        }));

        CheckBox autoHeight = check("ارتفاع لیبل خودکار (Auto Height)", settingAutoHeight);
        body.addView(autoHeight);

        Button applyLabel = btn("اعمال تنظیمات لیبل");
        body.addView(applyLabel);
        applyLabel.setOnClickListener(v -> {
            settingLabelWidth = clamp(parseFloat(labelWidth, settingLabelWidth*100f)/100f, .20f, .45f);
            settingFieldGap = clamp(parseFloat(gapPx, settingFieldGap*1000f)/1000f, .004f, .05f);
            settingOuterRadius = parseIntSafe(radius, settingOuterRadius, 0, 80);
            settingOuterBorderWidth = parseIntSafe(borderWidth, settingOuterBorderWidth, 0, 20);
            settingAutoHeight = autoHeight.isChecked();

            applyDesignerSettingsToView(designer);
            designer.invalidate();
            saveDesignerSettings();

            Toast.makeText(this,"تنظیمات لیبل اعمال شد",Toast.LENGTH_SHORT).show();
        });

        body.addView(section("رنگ کادرهای قیمت"));

        for (int i=0;i<fields.size();i++){
            final int idx = i;
            LabelField f = fields.get(i);
            Button cb = colorButton("کادر "+(i+1)+" — "+f.name, f.backgroundColor);
            body.addView(cb);
            cb.setOnClickListener(v -> showColorPalette("رنگ "+f.name, f.backgroundColor, color -> {
                fields.get(idx).backgroundColor = color;
                designer.setFields(fields);
                refreshColorButton(cb,"کادر "+(idx+1)+" — "+fields.get(idx).name,color);
                saveTemplate();
            }));
        }

        body.addView(section("تصویر محصول — Resize + Crop"));

        EditText productX = numberEdit(pct(settingProductX),"2");
        EditText productY = numberEdit(pct(settingProductY),"8");
        EditText productW = numberEdit(pct(settingProductW),"62");
        EditText productH = numberEdit(pct(settingProductH),"84");

        addLabeledEdit(body,"موقعیت افقی تصویر %",productX);
        addLabeledEdit(body,"موقعیت عمودی تصویر %",productY);
        addLabeledEdit(body,"عرض تصویر %",productW);
        addLabeledEdit(body,"ارتفاع تصویر %",productH);

        Button applyResize = btn("اعمال Resize تصویر");
        body.addView(applyResize);
        applyResize.setOnClickListener(v -> {
            settingProductX = clamp(parsePercent(productX, settingProductX),0f,.95f);
            settingProductY = clamp(parsePercent(productY, settingProductY),0f,.95f);
            settingProductW = clamp(parsePercent(productW, settingProductW),.05f,1f-settingProductX);
            settingProductH = clamp(parsePercent(productH, settingProductH),.05f,1f-settingProductY);

            applyDesignerSettingsToView(designer);
            designer.invalidate();
            saveDesignerSettings();
            Toast.makeText(this,"Resize تصویر اعمال شد",Toast.LENGTH_SHORT).show();
        });

        LinearLayout cropButtons = row();

        Button cropBtn = btn("✂ Crop تصویر");
        Button resetCrop = btn("بازنشانی Crop");

        cropButtons.addView(cropBtn,new LinearLayout.LayoutParams(0,-2,1));
        cropButtons.addView(resetCrop,new LinearLayout.LayoutParams(0,-2,1));
        body.addView(cropButtons);

        cropBtn.setOnClickListener(v -> showCropDialog());
        resetCrop.setOnClickListener(v -> {
            settingCropEnabled = false;
            settingCropLeft = 0f;
            settingCropTop = 0f;
            settingCropRight = 1f;
            settingCropBottom = 1f;

            if (designer != null) designer.resetCrop();
            saveDesignerSettings();
        });

        groupCropCheck = check("همین Crop روی همه تصاویر خروجی گروهی اعمال شود", false);
        body.addView(groupCropCheck);

        Button sample = btn("انتخاب / تغییر عکس نمونه");
        body.addView(sample);
        sample.setOnClickListener(v -> pickFile("image/*", PICK_IMAGE));

        Button add = btn("+ افزودن کادر قیمت");
        body.addView(add);
        add.setOnClickListener(v -> {
            fields.add(new LabelField("قیمت جدید",""));
            relayoutFields();
            designer.setFields(fields);
            selectedField = fields.size()-1;
            designer.select(selectedField);
            showFieldEditor();
            saveTemplate();
        });

        Button auto = btn("چیدمان مرتب خودکار کادرها");
        body.addView(auto);
        auto.setOnClickListener(v -> {
            relayoutFields();
            designer.setFields(fields);
            saveTemplate();
        });

        Button save = btn("ذخیره قالب");
        body.addView(save);
        save.setOnClickListener(v -> {
            syncDesignerSettingsFromView();
            saveTemplate();
            saveDesignerSettings();
            Toast.makeText(this,"قالب ذخیره شد",Toast.LENGTH_SHORT).show();
        });

        body.addView(section("تنظیمات کادر انتخاب‌شده"));

        fieldEditorContainer = new LinearLayout(this);
        fieldEditorContainer.setOrientation(LinearLayout.VERTICAL);
        body.addView(fieldEditorContainer);

        if (!fields.isEmpty()){
            if (selectedField < 0 || selectedField >= fields.size()) selectedField = 0;
            designer.select(selectedField);
            showFieldEditor();
        }
    }

    private void showFieldEditor(){
        if (fieldEditorContainer == null || selectedField < 0 || selectedField >= fields.size()) return;

        fieldEditorContainer.removeAllViews();

        LabelField f = fields.get(selectedField);

        TextView selectedLabel = tv("در حال ویرایش: "+f.name,14,true);
        fieldEditorContainer.addView(selectedLabel);

        EditText name = textEdit(f.name,"نام کادر");
        EditText val = textEdit(f.value,"قیمت");

        addLabeledEdit(fieldEditorContainer,"عنوان",name);
        addLabeledEdit(fieldEditorContainer,"قیمت",val);

        Spinner titleFont = makeSpinner(FONT_LABELS,fontIndex(f.titleFont));
        Spinner priceFont = makeSpinner(FONT_LABELS,fontIndex(f.priceFont));
        Spinner align = makeSpinner(ALIGN_LABELS,clampInt(f.textAlign,0,2));

        addLabeledSpinner(fieldEditorContainer,"فونت عنوان",titleFont);
        addLabeledSpinner(fieldEditorContainer,"فونت قیمت",priceFont);
        addLabeledSpinner(fieldEditorContainer,"تراز متن",align);

        Button titleColorBtn = colorButton("رنگ عنوان",f.titleColor);
        Button priceColorBtn = colorButton("رنگ قیمت",f.priceColor);
        Button tomanColorBtn = colorButton("رنگ تومان",f.tomanColor);
        Button bgColorBtn = colorButton("رنگ پس‌زمینه کادر",f.backgroundColor);
        Button borderColorBtn = colorButton("رنگ حاشیه کادر",f.borderColor);

        fieldEditorContainer.addView(titleColorBtn);
        fieldEditorContainer.addView(priceColorBtn);
        fieldEditorContainer.addView(tomanColorBtn);
        fieldEditorContainer.addView(bgColorBtn);
        fieldEditorContainer.addView(borderColorBtn);

        final int[] titleColor = {f.titleColor};
        final int[] priceColor = {f.priceColor};
        final int[] tomanColor = {f.tomanColor};
        final int[] bgColor = {f.backgroundColor};
        final int[] borderColor = {f.borderColor};

        titleColorBtn.setOnClickListener(v -> showColorPalette("رنگ عنوان",titleColor[0],c -> {
            titleColor[0]=c;
            refreshColorButton(titleColorBtn,"رنگ عنوان",c);
        }));

        priceColorBtn.setOnClickListener(v -> showColorPalette("رنگ قیمت",priceColor[0],c -> {
            priceColor[0]=c;
            refreshColorButton(priceColorBtn,"رنگ قیمت",c);
        }));

        tomanColorBtn.setOnClickListener(v -> showColorPalette("رنگ تومان",tomanColor[0],c -> {
            tomanColor[0]=c;
            refreshColorButton(tomanColorBtn,"رنگ تومان",c);
        }));

        bgColorBtn.setOnClickListener(v -> showColorPalette("رنگ پس‌زمینه کادر",bgColor[0],c -> {
            bgColor[0]=c;
            refreshColorButton(bgColorBtn,"رنگ پس‌زمینه کادر",c);
        }));

        borderColorBtn.setOnClickListener(v -> showColorPalette("رنگ حاشیه کادر",borderColor[0],c -> {
            borderColor[0]=c;
            refreshColorButton(borderColorBtn,"رنگ حاشیه کادر",c);
        }));

        EditText borderWidth = numberEdit(String.valueOf(f.borderWidth),"2");
        EditText radius = numberEdit(String.valueOf(f.cornerRadius),"18");
        EditText padH = numberEdit(String.valueOf(f.paddingHorizontal),"14");
        EditText padV = numberEdit(String.valueOf(f.paddingVertical),"8");
        EditText titleGap = numberEdit(String.valueOf(f.titlePriceGap),"3");

        addLabeledEdit(fieldEditorContainer,"ضخامت حاشیه",borderWidth);
        addLabeledEdit(fieldEditorContainer,"گردی گوشه‌ها",radius);
        addLabeledEdit(fieldEditorContainer,"فاصله داخلی افقی",padH);
        addLabeledEdit(fieldEditorContainer,"فاصله داخلی عمودی",padV);
        addLabeledEdit(fieldEditorContainer,"فاصله عنوان تا قیمت",titleGap);

        CheckBox strike = check("خط‌خورده کردن قیمت",f.strike);
        CheckBox showToman = check("نمایش تومان",f.showToman);
        CheckBox visible = check("نمایش کادر",f.visible);
        CheckBox showTitle = check("نمایش عنوان",f.showTitle);
        CheckBox showPrice = check("نمایش قیمت",f.showPrice);
        CheckBox titleBold = check("عنوان Bold",f.titleBold);
        CheckBox titleItalic = check("عنوان Italic",f.titleItalic);
        CheckBox priceBold = check("قیمت Bold",f.priceBold);
        CheckBox priceItalic = check("قیمت Italic",f.priceItalic);

        fieldEditorContainer.addView(strike);
        fieldEditorContainer.addView(showToman);
        fieldEditorContainer.addView(visible);
        fieldEditorContainer.addView(showTitle);
        fieldEditorContainer.addView(showPrice);
        fieldEditorContainer.addView(titleBold);
        fieldEditorContainer.addView(titleItalic);
        fieldEditorContainer.addView(priceBold);
        fieldEditorContainer.addView(priceItalic);

        Button apply = btn("اعمال تنظیمات این کادر");
        fieldEditorContainer.addView(apply);

        apply.setOnClickListener(v -> {
            f.name = name.getText().toString().trim();
            f.value = val.getText().toString().trim();

            f.titleFont = FONT_VALUES[titleFont.getSelectedItemPosition()];
            f.priceFont = FONT_VALUES[priceFont.getSelectedItemPosition()];
            f.textAlign = align.getSelectedItemPosition();

            f.titleColor = titleColor[0];
            f.priceColor = priceColor[0];
            f.tomanColor = tomanColor[0];
            f.backgroundColor = bgColor[0];
            f.borderColor = borderColor[0];

            f.borderWidth = parseIntSafe(borderWidth,f.borderWidth,0,30);
            f.cornerRadius = parseIntSafe(radius,f.cornerRadius,0,100);
            f.paddingHorizontal = parseIntSafe(padH,f.paddingHorizontal,0,80);
            f.paddingVertical = parseIntSafe(padV,f.paddingVertical,0,80);
            f.titlePriceGap = parseIntSafe(titleGap,f.titlePriceGap,0,50);

            f.strike = strike.isChecked();
            f.showToman = showToman.isChecked();
            f.visible = visible.isChecked();
            f.showTitle = showTitle.isChecked();
            f.showPrice = showPrice.isChecked();
            f.titleBold = titleBold.isChecked();
            f.titleItalic = titleItalic.isChecked();
            f.priceBold = priceBold.isChecked();
            f.priceItalic = priceItalic.isChecked();

            designer.setFields(fields);
            designer.select(selectedField);
            selectedLabel.setText("در حال ویرایش: "+f.name);

            saveTemplate();
            Toast.makeText(this,"تنظیمات کادر اعمال شد",Toast.LENGTH_SHORT).show();
        });

        Button copyStyle = btn("اعمال استایل این کادر برای همه کادرها");
        fieldEditorContainer.addView(copyStyle);

        copyStyle.setOnClickListener(v -> {
            for (int i=0;i<fields.size();i++){
                if (i != selectedField) copyStyleOnly(f, fields.get(i));
            }
            designer.setFields(fields);
            saveTemplate();
            Toast.makeText(this,"استایل برای همه کادرها اعمال شد",Toast.LENGTH_SHORT).show();
        });

        Button del = btn("حذف این کادر");
        fieldEditorContainer.addView(del);

        del.setOnClickListener(v -> {
            if (fields.size() <= 1){
                Toast.makeText(this,"حداقل یک کادر باید باقی بماند",Toast.LENGTH_SHORT).show();
                return;
            }

            fields.remove(selectedField);
            relayoutFields();
            selectedField = Math.min(selectedField,fields.size()-1);

            designer.setFields(fields);
            designer.select(selectedField);
            showFieldEditor();
            saveTemplate();
        });
    }

    private void showQuickTextSizeDialog(int fieldIndex, int textType){
        if (fieldIndex < 0 || fieldIndex >= fields.size()) return;

        LabelField f = fields.get(fieldIndex);

        String title;
        int current;
        int min;
        int max;

        if (textType == LabelDesignerView.TEXT_TITLE){
            title = "اندازه فونت عنوان";
            current = f.titleSize;
            min = 8;
            max = 72;
        } else {
            title = "اندازه فونت قیمت";
            current = f.priceSize;
            min = 10;
            max = 96;
        }

        final int[] value = {current};

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28,24,28,20);

        TextView caption = tv(title,18,true);
        caption.setGravity(Gravity.CENTER);
        root.addView(caption);

        LinearLayout controls = row();

        Button down = btn("▼");
        EditText size = numberEdit(String.valueOf(current),"");
        size.setGravity(Gravity.CENTER);
        Button up = btn("▲");

        controls.addView(down,new LinearLayout.LayoutParams(0,-2,1));
        controls.addView(size,new LinearLayout.LayoutParams(0,-2,1.4f));
        controls.addView(up,new LinearLayout.LayoutParams(0,-2,1));

        root.addView(controls);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(root)
                .setPositiveButton("اعمال",null)
                .setNegativeButton("انصراف",null)
                .create();

        down.setOnClickListener(v -> {
            value[0] = Math.max(min, value[0]-1);
            size.setText(String.valueOf(value[0]));
        });

        up.setOnClickListener(v -> {
            value[0] = Math.min(max, value[0]+1);
            size.setText(String.valueOf(value[0]));
        });

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int n = parseIntSafe(size,value[0],min,max);

            if (textType == LabelDesignerView.TEXT_TITLE) f.titleSize = n;
            else f.priceSize = n;

            if (designer != null) designer.invalidate();
            saveTemplate();
            dialog.dismiss();
        }));

        dialog.show();
    }

    private void showCropDialog(){
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24,18,24,18);

        root.addView(tv("Crop مستقل تصویر",18,true));

        EditText left = numberEdit(String.valueOf(Math.round(settingCropLeft*100f)),"0");
        EditText top = numberEdit(String.valueOf(Math.round(settingCropTop*100f)),"0");
        EditText right = numberEdit(String.valueOf(Math.round((1f-settingCropRight)*100f)),"0");
        EditText bottom = numberEdit(String.valueOf(Math.round((1f-settingCropBottom)*100f)),"0");

        addLabeledEdit(root,"برش از چپ %",left);
        addLabeledEdit(root,"برش از بالا %",top);
        addLabeledEdit(root,"برش از راست %",right);
        addLabeledEdit(root,"برش از پایین %",bottom);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(root)
                .setPositiveButton("اعمال Crop",null)
                .setNegativeButton("انصراف",null)
                .create();

        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            float l = clamp(parseFloat(left,0f)/100f,0f,.80f);
            float t = clamp(parseFloat(top,0f)/100f,0f,.80f);
            float rCut = clamp(parseFloat(right,0f)/100f,0f,.80f);
            float bCut = clamp(parseFloat(bottom,0f)/100f,0f,.80f);

            float r = 1f-rCut;
            float b = 1f-bCut;

            if (r-l < .10f || b-t < .10f){
                Toast.makeText(this,"Crop بیش از حد است",Toast.LENGTH_SHORT).show();
                return;
            }

            settingCropLeft = l;
            settingCropTop = t;
            settingCropRight = r;
            settingCropBottom = b;
            settingCropEnabled = true;

            if (designer != null){
                designer.setCrop(l,t,r,b);
            }

            saveDesignerSettings();
            dialog.dismiss();
        }));

        dialog.show();
    }

    private CheckBox check(String text, boolean checked){
        CheckBox c = new CheckBox(this);
        c.setText(text);
        c.setChecked(checked);
        return c;
    }

    private Button colorButton(String title, int color){
        Button b = btn(title+"   "+colorToHex(color));
        refreshColorButton(b,title,color);
        return b;
    }

    private void refreshColorButton(Button b, String title, int color){
        b.setText(title+"   "+colorToHex(color));
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(16);
        gd.setStroke(2,0xFFBDBDBD);
        b.setBackground(gd);
        b.setTextColor(isDark(color) ? Color.WHITE : Color.BLACK);
    }

    private boolean isDark(int color){
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        double y = (0.299*r + 0.587*g + 0.114*b);
        return y < 145;
    }

    private interface ColorSelectedListener {
        void onColorSelected(int color);
    }

    private void showColorPalette(String title, int currentColor, final ColorSelectedListener listener){
        final Dialog dialog = new Dialog(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24,20,24,20);

        TextView titleView = tv(title,18,true);
        titleView.setGravity(Gravity.CENTER);
        root.addView(titleView);

        final int[] colors = {
                0xFFFFFFFF,0xFFF5F5F5,0xFFE0E0E0,0xFF9E9E9E,0xFF616161,0xFF212121,
                0xFFFFCDD2,0xFFEF5350,0xFFC62828,0xFF8E0000,
                0xFFFFE0B2,0xFFFF9800,0xFFEF6C00,
                0xFFFFF9C4,0xFFFFEB3B,0xFFF9A825,0xFFFFD600,
                0xFFC8E6C9,0xFF66BB6A,0xFF2E7D32,0xFF1B5E20,
                0xFFB2DFDB,0xFF26A69A,0xFF00796B,
                0xFFBBDEFB,0xFF42A5F5,0xFF1976D2,0xFF0D47A1,
                0xFFD1C4E9,0xFF7E57C2,0xFF512DA8,
                0xFFF8BBD0,0xFFEC407A,0xFFC2185B,
                0xFFFFF3E0,0xFFE3F2FD,0xFFE8F5E9,0xFFF3E5F5,
                0xFFE91319,0xFF111111,0xFF63BF67
        };

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(5);

        int size = (int)(48*getResources().getDisplayMetrics().density);
        int margin = (int)(4*getResources().getDisplayMetrics().density);

        for (final int color : colors){
            TextView box = new TextView(this);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(color);
            bg.setCornerRadius(12);
            bg.setStroke(color == currentColor ? 5 : 2, color == currentColor ? 0xFF1976D2 : 0xFFBDBDBD);
            box.setBackground(bg);

            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = size;
            p.height = size;
            p.setMargins(margin,margin,margin,margin);
            box.setLayoutParams(p);

            box.setOnClickListener(v -> {
                listener.onColorSelected(color);
                dialog.dismiss();
            });

            grid.addView(box);
        }

        root.addView(grid);

        Button cancel = btn("انصراف");
        root.addView(cancel);
        cancel.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(root);
        dialog.show();

        Window w = dialog.getWindow();
        if (w != null) w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void showOutput(){
        if (manualRows != null && manualRows.getParent() != null) syncManualRows();
        clear();

        body.addView(section("خروجی"));

        appendMode = check("لیبل بیرون تصویر قرار بگیرد",true);
        body.addView(appendMode);

        Button preview = btn("پیش‌نمایش خروجی نهایی");
        body.addView(preview);
        preview.setOnClickListener(v -> makePreview());

        previewStatus = tv("برای خروجی دستی ابتدا عکس را انتخاب کن.",13,false);
        body.addView(previewStatus);

        Button save = btn("ذخیره عکس فعلی در گالری");
        body.addView(save);
        save.setOnClickListener(v -> saveCurrent());

        Button batch = btn("ساخت گروهی از Excel + پوشه عکس‌ها");
        body.addView(batch);
        batch.setOnClickListener(v -> runBatch());
    }

    private void makePreview(){
        if (currentBitmap == null){
            Toast.makeText(this,"عکس انتخاب نشده",Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap out = render(currentBitmap,fields,true);

        ImageView iv = new ImageView(this);
        iv.setAdjustViewBounds(true);
        iv.setImageBitmap(out);

        body.addView(iv,Math.min(body.getWidth()>0?body.getWidth():1000,1000),-2);
        previewStatus.setText("پیش‌نمایش ساخته شد.");
    }

    private Bitmap render(Bitmap src, ArrayList<LabelField> useFields, boolean applyCrop){
        LabelDesignerView r = new LabelDesignerView(this);
        applyDesignerSettingsToView(r);
        r.setFields(useFields);

        if (!applyCrop){
            r.cropEnabled = false;
            r.cropLeft = 0f;
            r.cropTop = 0f;
            r.cropRight = 1f;
            r.cropBottom = 1f;
        }

        boolean append = appendMode == null || appendMode.isChecked();
        return r.renderFinal(src,Color.WHITE,0xFFD8D8D8,append,settingLabelWidth);
    }

    private void saveCurrent(){
        if (currentBitmap == null){
            Toast.makeText(this,"عکس انتخاب نشده",Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Bitmap out = render(currentBitmap,fields,true);
            saveToGallery(out,"Javdan_"+System.currentTimeMillis()+".jpg");
            Toast.makeText(this,"در Pictures/JavdanPriceLabeler ذخیره شد",Toast.LENGTH_LONG).show();
        } catch (Exception e){
            Toast.makeText(this,safeMessage(e),Toast.LENGTH_LONG).show();
        }
    }

    private void saveToGallery(Bitmap bmp, String name) throws Exception{
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME,name);
        v.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/JavdanPriceLabeler");

        Uri u = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);
        if (u == null) throw new IOException("ساخت فایل خروجی ناموفق بود");

        try (OutputStream o = getContentResolver().openOutputStream(u)){
            if (o == null) throw new IOException("فایل خروجی باز نشد");
            bmp.compress(Bitmap.CompressFormat.JPEG,94,o);
        }
    }

    private void pickFile(String type, int request){
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(type);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i,request);
    }

    private void pickFolder(){
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i,PICK_FOLDER);
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data){
        super.onActivityResult(req,result,data);
        if (result != RESULT_OK || data == null) return;

        Uri u = data.getData();
        if (u == null) return;

        try {
            getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored){}

        try {
            if (req == PICK_IMAGE){
                imageUri = u;

                try (InputStream in = getContentResolver().openInputStream(u)){
                    currentBitmap = BitmapFactory.decodeStream(in);
                }

                status.setText("عکس انتخاب شد");
                if (designer != null) designer.setProductBitmap(currentBitmap);

            } else if (req == PICK_EXCEL){
                excelUri = u;
                status.setText("در حال خواندن Excel...");

                excelRows = new XlsxReader(this).readFirstSheet(u);

                headers.clear();
                if (!excelRows.isEmpty()) headers.addAll(excelRows.get(0).keySet());

                refreshExcelUi();
                status.setText(excelRows.size()+" ردیف Excel خوانده شد");

            } else if (req == PICK_FOLDER){
                folderUri = u;
                status.setText("پوشه تصاویر انتخاب شد");
                Toast.makeText(this,"پوشه عکس محصولات انتخاب شد",Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e){
            Toast.makeText(this,"خطا: "+safeMessage(e),Toast.LENGTH_LONG).show();
        }
    }

    private void refreshExcelUi(){
        if (codeSpinner != null){
            codeSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,headers));
        }

        if (excelInfo != null){
            excelInfo.removeAllViews();
            excelInfo.addView(tv("ردیف‌ها: "+excelRows.size()+" | ستون‌ها: "+headers.size(),13,false));
        }
    }

    private void validateBatch(){
        if (excelUri == null || folderUri == null || excelRows.isEmpty()){
            Toast.makeText(this,"Excel و پوشه عکس‌ها را انتخاب کن",Toast.LENGTH_LONG).show();
            return;
        }

        if (headers.isEmpty()){
            Toast.makeText(this,"ستون‌های Excel شناسایی نشده‌اند",Toast.LENGTH_LONG).show();
            return;
        }

        final int selectedPos = codeSpinner != null ? codeSpinner.getSelectedItemPosition() : 0;

        if (selectedPos < 0 || selectedPos >= headers.size()){
            Toast.makeText(this,"ستون کد محصول را انتخاب کن",Toast.LENGTH_LONG).show();
            return;
        }

        final String codeHeader = headers.get(selectedPos);
        status.setText("در حال بررسی تطبیق Excel و تصاویر...");

        new Thread(() -> {
            try {
                HashMap<String,Uri> images = listTreeImages(folderUri);

                int excelCodeCount = 0;
                int matched = 0;
                int missingImages = 0;

                HashSet<String> excelCodes = new HashSet<>();

                for (LinkedHashMap<String,String> row : excelRows){
                    String code = normalizeCode(row.get(codeHeader));
                    if (code.isEmpty()) continue;

                    excelCodeCount++;
                    excelCodes.add(code);

                    if (images.containsKey(code)) matched++;
                    else missingImages++;
                }

                int extraImages = 0;
                for (String imageCode : images.keySet()){
                    if (!excelCodes.contains(imageCode)) extraImages++;
                }

                final int fExcelCodeCount = excelCodeCount;
                final int fMatched = matched;
                final int fMissingImages = missingImages;
                final int fExtraImages = extraImages;
                final int totalImages = images.size();

                runOnUiThread(() -> {
                    status.setText("تطبیق انجام شد — موفق: "+fMatched+" | بدون عکس: "+fMissingImages);

                    if (excelInfo != null){
                        excelInfo.removeAllViews();
                        excelInfo.addView(tv("کدهای محصول: "+fExcelCodeCount,14,true));
                        excelInfo.addView(tv("تعداد عکس‌های پوشه: "+totalImages,14,false));

                        TextView ok = tv("✓ تطبیق موفق: "+fMatched,16,true);
                        ok.setTextColor(0xFF168A3B);
                        excelInfo.addView(ok);

                        TextView miss = tv("✕ کدهای بدون عکس: "+fMissingImages,15,true);
                        miss.setTextColor(0xFFC62828);
                        excelInfo.addView(miss);

                        TextView extra = tv("عکس‌های بدون کد در Excel: "+fExtraImages,14,true);
                        extra.setTextColor(0xFFE28A00);
                        excelInfo.addView(extra);

                        if (fMatched > 0){
                            Button go = btn("ادامه و ساخت خروجی گروهی");
                            excelInfo.addView(go);
                            go.setOnClickListener(v -> showOutput());
                        }
                    }
                });

            } catch (Exception e){
                runOnUiThread(() -> Toast.makeText(this,"خطا: "+safeMessage(e),Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private boolean isPriceColumn(String header){
        if (header == null) return false;

        String h = header.trim()
                .replace("ي","ی")
                .replace("ك","ک")
                .toLowerCase(Locale.ROOT);

        return h.contains("قیمت");
    }

    private void runBatch(){
        if (excelRows.isEmpty() || folderUri == null){
            Toast.makeText(this,"Excel و پوشه عکس‌ها را انتخاب کن",Toast.LENGTH_LONG).show();
            return;
        }

        if (headers.isEmpty()){
            Toast.makeText(this,"ستون‌های Excel پیدا نشد",Toast.LENGTH_LONG).show();
            return;
        }

        status.setText("در حال ساخت خروجی گروهی...");

        final boolean convert = rialToToman == null || rialToToman.isChecked();
        final boolean cropAll = groupCropCheck != null && groupCropCheck.isChecked();

        new Thread(() -> {
            int ok=0, missing=0, errors=0;

            try {
                int selectedPos = codeSpinner != null ? codeSpinner.getSelectedItemPosition() : 0;
                if (selectedPos < 0 || selectedPos >= headers.size()) selectedPos = 0;

                String codeHeader = headers.get(selectedPos);
                ArrayList<String> priceHeaders = new ArrayList<>();

                for (String h : headers){
                    if (!h.equals(codeHeader) && isPriceColumn(h)) priceHeaders.add(h);
                }

                if (priceHeaders.isEmpty()) throw new IOException("هیچ ستون قیمتی در Excel پیدا نشد.");

                HashMap<String,Uri> images = listTreeImages(folderUri);

                for (LinkedHashMap<String,String> row : excelRows){
                    String code = normalizeCode(row.get(codeHeader));
                    if (code.isEmpty()) continue;

                    Uri img = images.get(code);
                    if (img == null){
                        missing++;
                        continue;
                    }

                    try {
                        Bitmap src;

                        try (InputStream in = getContentResolver().openInputStream(img)){
                            src = BitmapFactory.decodeStream(in);
                        }

                        if (src == null){
                            errors++;
                            continue;
                        }

                        ArrayList<LabelField> fs = new ArrayList<>();

                        for (int i=0;i<priceHeaders.size();i++){
                            String h = priceHeaders.get(i);
                            String raw = row.get(h);

                            if (raw == null || raw.trim().isEmpty()) continue;

                            LabelField f = new LabelField(h.trim(), formatPrice(raw,convert));

                            if (i < fields.size()) copyStyleOnly(fields.get(i),f);
                            else if (!fields.isEmpty()) copyStyleOnly(fields.get(fields.size()-1),f);

                            fs.add(f);
                        }

                        layoutBatchFields(fs);

                        Bitmap out = render(src,fs,cropAll);
                        saveToGallery(out,code+".jpg");
                        ok++;

                        if (!src.isRecycled()) src.recycle();
                        if (out != null && !out.isRecycled()) out.recycle();

                    } catch (Exception e){
                        errors++;
                    }
                }

            } catch (Exception e){
                errors++;
            }

            final int fok=ok, fm=missing, fe=errors;

            runOnUiThread(() -> {
                status.setText("خروجی گروهی تمام شد — موفق: "+fok+" | بدون عکس: "+fm+" | خطا: "+fe);
                Toast.makeText(this,"تمام شد — موفق: "+fok+" | بدون عکس: "+fm+" | خطا: "+fe,Toast.LENGTH_LONG).show();
            });

        }).start();
    }

    private void layoutBatchFields(ArrayList<LabelField> fs){
        int n = Math.max(1,fs.size());
        float top = .02f;
        float gap = .012f;
        float h = Math.min(.22f, (.96f-gap*(n-1))/n);

        for (int i=0;i<fs.size();i++){
            LabelField f = fs.get(i);
            f.x = .02f;
            f.w = .96f;
            f.h = h;
            f.y = top + i*(h+gap);
        }
    }

    private void copyStyleOnly(LabelField from, LabelField to){
        to.titleSize = from.titleSize;
        to.priceSize = from.priceSize;
        to.tomanSize = from.tomanSize;

        to.titleColor = from.titleColor;
        to.priceColor = from.priceColor;
        to.tomanColor = from.tomanColor;

        to.backgroundColor = from.backgroundColor;
        to.borderColor = from.borderColor;
        to.borderWidth = from.borderWidth;
        to.cornerRadius = from.cornerRadius;

        to.paddingHorizontal = from.paddingHorizontal;
        to.paddingVertical = from.paddingVertical;
        to.titlePriceGap = from.titlePriceGap;

        to.strike = from.strike;
        to.showToman = from.showToman;
        to.visible = from.visible;

        to.titleBold = from.titleBold;
        to.titleItalic = from.titleItalic;
        to.priceBold = from.priceBold;
        to.priceItalic = from.priceItalic;

        to.textAlign = from.textAlign;
        to.titleFont = from.titleFont;
        to.priceFont = from.priceFont;

        to.showTitle = from.showTitle;
        to.showPrice = from.showPrice;
    }

    private HashMap<String,Uri> listTreeImages(Uri tree) throws Exception{
        HashMap<String,Uri> map = new HashMap<>();

        String docId = DocumentsContract.getTreeDocumentId(tree);
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree,docId);

        String[] cols = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };

        try (Cursor c = getContentResolver().query(children,cols,null,null,null)){
            if (c != null){
                while (c.moveToNext()){
                    String id = c.getString(0);
                    String name = c.getString(1);
                    String mime = c.getString(2);

                    if (mime != null && mime.startsWith("image/")){
                        String stem = name == null ? "" : name;
                        int dot = stem.lastIndexOf('.');
                        if (dot > 0) stem = stem.substring(0,dot);

                        Uri child = DocumentsContract.buildDocumentUriUsingTree(tree,id);
                        map.put(normalizeCode(stem),child);
                    }
                }
            }
        }

        return map;
    }

    private String normalizeCode(String s){
        if (s == null) return "";
        s = s.trim();
        if (s.endsWith(".0")) s = s.substring(0,s.length()-2);
        return s;
    }

    private void applyDesignerSettingsToView(LabelDesignerView d){
        if (d == null) return;

        d.labelWidthPct = settingLabelWidth;
        d.fieldGapPct = settingFieldGap;
        d.outerTagRadius = settingOuterRadius;
        d.outerTagBorderWidth = settingOuterBorderWidth;
        d.outerTagColor = settingOuterColor;
        d.outerTagBorderColor = settingOuterBorderColor;
        d.autoHeight = settingAutoHeight;

        d.productX = settingProductX;
        d.productY = settingProductY;
        d.productW = settingProductW;
        d.productH = settingProductH;

        d.cropEnabled = settingCropEnabled;
        d.cropLeft = settingCropLeft;
        d.cropTop = settingCropTop;
        d.cropRight = settingCropRight;
        d.cropBottom = settingCropBottom;
    }

    private void syncDesignerSettingsFromView(){
        if (designer == null) return;

        settingLabelWidth = designer.labelWidthPct;
        settingFieldGap = designer.fieldGapPct;
        settingOuterRadius = designer.outerTagRadius;
        settingOuterBorderWidth = designer.outerTagBorderWidth;
        settingOuterColor = designer.outerTagColor;
        settingOuterBorderColor = designer.outerTagBorderColor;
        settingAutoHeight = designer.autoHeight;

        settingProductX = designer.productX;
        settingProductY = designer.productY;
        settingProductW = designer.productW;
        settingProductH = designer.productH;

        settingCropEnabled = designer.cropEnabled;
        settingCropLeft = designer.cropLeft;
        settingCropTop = designer.cropTop;
        settingCropRight = designer.cropRight;
        settingCropBottom = designer.cropBottom;
    }

    private void saveDesignerSettings(){
        getSharedPreferences("javdan",MODE_PRIVATE).edit()
                .putFloat("labelWidth",settingLabelWidth)
                .putFloat("fieldGap",settingFieldGap)
                .putInt("outerRadius",settingOuterRadius)
                .putInt("outerBorderWidth",settingOuterBorderWidth)
                .putInt("outerColor",settingOuterColor)
                .putInt("outerBorderColor",settingOuterBorderColor)
                .putBoolean("autoHeight",settingAutoHeight)

                .putFloat("productX",settingProductX)
                .putFloat("productY",settingProductY)
                .putFloat("productW",settingProductW)
                .putFloat("productH",settingProductH)

                .putBoolean("cropEnabled",settingCropEnabled)
                .putFloat("cropLeft",settingCropLeft)
                .putFloat("cropTop",settingCropTop)
                .putFloat("cropRight",settingCropRight)
                .putFloat("cropBottom",settingCropBottom)
                .apply();
    }

    private void loadDesignerSettings(){
        android.content.SharedPreferences p = getSharedPreferences("javdan",MODE_PRIVATE);

        settingLabelWidth = p.getFloat("labelWidth",0.28f);
        settingFieldGap = p.getFloat("fieldGap",0.014f);
        settingOuterRadius = p.getInt("outerRadius",22);
        settingOuterBorderWidth = p.getInt("outerBorderWidth",3);
        settingOuterColor = p.getInt("outerColor",0xFF181818);
        settingOuterBorderColor = p.getInt("outerBorderColor",0xFF3A3A3A);
        settingAutoHeight = p.getBoolean("autoHeight",true);

        settingProductX = p.getFloat("productX",0.02f);
        settingProductY = p.getFloat("productY",0.08f);
        settingProductW = p.getFloat("productW",0.62f);
        settingProductH = p.getFloat("productH",0.84f);

        settingCropEnabled = p.getBoolean("cropEnabled",false);
        settingCropLeft = p.getFloat("cropLeft",0f);
        settingCropTop = p.getFloat("cropTop",0f);
        settingCropRight = p.getFloat("cropRight",1f);
        settingCropBottom = p.getFloat("cropBottom",1f);
    }

    private void saveTemplate(){
        try {
            JSONArray a = new JSONArray();
            for (LabelField f : fields) a.put(f.toJson());

            getSharedPreferences("javdan",MODE_PRIVATE)
                    .edit()
                    .putString("fields",a.toString())
                    .apply();

        } catch (Exception ignored){}
    }

    private void loadTemplate(){
        fields.clear();

        String s = getSharedPreferences("javdan",MODE_PRIVATE).getString("fields","");

        if (!s.isEmpty()){
            try {
                JSONArray a = new JSONArray(s);
                for (int i=0;i<a.length();i++){
                    fields.add(LabelField.fromJson(a.getJSONObject(i)));
                }
            } catch (Exception ignored){}
        }

        if (fields.isEmpty()) makeDefaults();
    }

    private int fontIndex(String font){
        if (font == null) return 0;

        for (int i=0;i<FONT_VALUES.length;i++){
            if (FONT_VALUES[i].equals(font)) return i;
        }
        return 0;
    }

    private String pct(float value){
        return String.valueOf(Math.round(value*100f));
    }

    private float parsePercent(EditText e, float fallback){
        try {
            String s = e.getText().toString().trim();
            if (s.isEmpty()) return fallback;
            return Float.parseFloat(s)/100f;
        } catch (Exception ex){
            return fallback;
        }
    }

    private float parseFloat(EditText e, float fallback){
        try {
            return Float.parseFloat(e.getText().toString().trim());
        } catch (Exception ex){
            return fallback;
        }
    }

    private int parseIntSafe(EditText e, int fallback, int min, int max){
        try {
            int v = Integer.parseInt(e.getText().toString().trim());
            return clampInt(v,min,max);
        } catch (Exception ex){
            return fallback;
        }
    }

    private String colorToHex(int color){
        return String.format(Locale.US,"#%08X",color);
    }

    private float clamp(float v, float min, float max){
        return Math.max(min,Math.min(max,v));
    }

    private int clampInt(int v, int min, int max){
        return Math.max(min,Math.min(max,v));
    }

    private String safeMessage(Exception e){
        if (e == null) return "خطای نامشخص";
        String m = e.getMessage();
        return (m == null || m.trim().isEmpty()) ? e.getClass().getSimpleName() : m;
    }
}
