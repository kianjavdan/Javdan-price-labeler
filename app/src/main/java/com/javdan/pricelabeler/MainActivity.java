package com.javdan.pricelabeler;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.content.res.AssetFileDescriptor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.*;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import org.json.*;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class MainActivity extends Activity {

    static final int PICK_EXCEL = 10, PICK_IMAGE = 11, PICK_FOLDER = 12, PICK_BACKGROUND = 13;

    LinearLayout body, manualRows, excelInfo, fieldEditorContainer, infoEditorContainer;
    Button tabData, tabDesigner, tabOutput;
    RadioButton modeExcel, modeManual;

    Uri excelUri, imageUri, folderUri;
    Bitmap currentBitmap;
    LabelDesignerView designer;

    ArrayList<LabelField> fields = new ArrayList<>();
    ArrayList<InfoField> infoFields = new ArrayList<>();
    ArrayList<LinkedHashMap<String,String>> excelRows = new ArrayList<>();
    ArrayList<String> headers = new ArrayList<>();
    ArrayList<EditText> nameEdits = new ArrayList<>(), valueEdits = new ArrayList<>();

    Spinner codeSpinner, infoFieldSpinner;
    TextView status, previewStatus;
    CheckBox rialToToman, appendMode, groupCropCheck;

    int selectedField = -1;
    int selectedInfoField = -1;

    // تنظیمات خروجی / طراح
    float settingLabelWidth = 0.28f;
    float settingFieldGap = 0.014f;
    int settingOuterRadius = 22;
    int settingOuterBorderWidth = 3;
    int settingOuterColor = 0xFF181818;
    int settingOuterBorderColor = 0xFF3A3A3A;
    boolean settingAutoHeight = true;
    float settingManualCardHeightPx = 130f;

    float settingProductX = 0.02f;
    float settingProductY = 0.08f;
    float settingProductW = 0.62f;
    float settingProductH = 0.84f;
    float settingProductZoom = 1.0f;

    // WYSIWYG background + panel placement
    int settingBackgroundMode = LabelDesignerView.BG_SOLID;
    int settingBackgroundColor = 0xFFF2F2F2;
    int settingGradientColor1 = 0xFFFFFFFF;
    int settingGradientColor2 = 0xFFE8EEF8;
    float settingGradientAngle = 0f;
    int settingBackgroundAlpha = 255;
    int settingPatternIndex = 0;
    String settingCustomBackgroundUri = "";
    Bitmap customBackgroundBitmap;
    float settingLabelX = 0.70f;
    float settingLabelY = 0.12f;
    float settingFieldGapPx = 6f;
    float settingPanelPaddingPx = 10f;

    // Product information panel
    boolean settingInfoEnabled = true;
    // 0 below product, 1 above product, 2 custom
    int settingInfoPositionMode = 0;
    // 0 vertical, 1 horizontal, 2 wrap/grid
    int settingInfoLayoutMode = 2;
    int settingInfoColumns = 2;
    float settingInfoWidth = 0.48f;
    float settingInfoX = 0.10f;
    float settingInfoY = 0.76f;
    float settingInfoDistancePx = 12f;
    float settingInfoGapPx = 8f;
    float settingInfoPaddingPx = 10f;
    int settingInfoOuterColor = 0x00FFFFFF;
    int settingInfoOuterBorderColor = 0xFF6AAE72;
    int settingInfoOuterBorderWidth = 0;
    int settingInfoOuterRadius = 18;

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
        t.setTextColor(0xFF20302E);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(8,8,8,8);
        return t;
    }

    private TextView section(String s){
        TextView t = tv(s, 17, true);
        t.setTextColor(0xFF0B6E64);
        t.setPadding(8,22,8,10);
        return t;
    }

    /** Rounded, elevated "card" button — the app's default button look. */
    private Button btn(String s){
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(0xFF20302E);
        b.setPadding((int)dp(14),(int)dp(11),(int)dp(14),(int)dp(11));
        b.setStateListAnimator(null);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(0xFFFFFFFF);
        b.setBackground(bg);
        b.setElevation(dp(2.5f));
        return b;
    }

    private float dp(float value){
        return value * getResources().getDisplayMetrics().density;
    }

    private LinearLayout row(){
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    interface FloatChanged { void onChanged(float value); }

    private LinearLayout slider(String label, float min, float max, float value, float step, String suffix, FloatChanged cb){
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(4,6,4,6);
        LinearLayout head = row();
        TextView name = tv(label,13,false);
        TextView val = tv("",13,true);
        val.setGravity(Gravity.END);
        head.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        head.addView(val,new LinearLayout.LayoutParams(-2,-2));
        box.addView(head);
        SeekBar seek = new SeekBar(this);
        int steps = Math.max(1,Math.round((max-min)/step));
        seek.setMax(steps);
        int initial = clampInt(Math.round((value-min)/step),0,steps);
        seek.setProgress(initial);
        box.addView(seek);
        float initialValue = min + initial*step;
        val.setText((step < 1f ? String.format(Locale.US,"%.2f",initialValue) : String.valueOf(Math.round(initialValue))) + (suffix==null?"":suffix));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int progress,boolean fromUser){
                float v=min+progress*step;
                val.setText((step < 1f ? String.format(Locale.US,"%.2f",v) : String.valueOf(Math.round(v))) + (suffix==null?"":suffix));
                if(cb!=null)cb.onChanged(v);
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        return box;
    }

    private Button transformProductBtn, transformPanelBtn, transformInfoBtn;

    /**
     * Toolbar that lets the user move/resize the product image, the main label panel and the
     * info panel directly with their finger on the canvas, instead of the old position/size/
     * zoom SeekBars: tap a button to select a target, then drag = move, pinch = resize.
     */
    private LinearLayout buildTransformToolbar(){
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0,10,0,14);

        TextView hint = tv("لمسی: یکی از دکمه‌های زیر را بزن، بعد روی تصویر Preview بکش (جابجایی) یا با دو انگشت پینچ کن (تغییر اندازه)",12,false);
        hint.setTextColor(0xFF00838F);
        wrap.addView(hint);

        LinearLayout row = row();
        transformProductBtn = btn("🖼  عکس محصول");
        transformPanelBtn = btn("🏷  قاب لیبل");
        transformInfoBtn = btn("ℹ️  قاب اطلاعات");
        row.addView(transformProductBtn,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(transformPanelBtn,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(transformInfoBtn,new LinearLayout.LayoutParams(0,-2,1));
        wrap.addView(row);

        transformProductBtn.setOnClickListener(v -> toggleTransformTarget(LabelDesignerView.TARGET_PRODUCT));
        transformPanelBtn.setOnClickListener(v -> toggleTransformTarget(LabelDesignerView.TARGET_PANEL));
        transformInfoBtn.setOnClickListener(v -> toggleTransformTarget(LabelDesignerView.TARGET_INFO));

        return wrap;
    }

    private void toggleTransformTarget(int target){
        if (designer == null) return;
        boolean turningOn = designer.getTransformTarget() != target;
        designer.setTransformTarget(turningOn ? target : LabelDesignerView.TARGET_NONE);
        refreshTransformButtons();
    }

    private void refreshTransformButtons(){
        if (designer == null) return;
        int active = designer.getTransformTarget();
        styleTransformButton(transformProductBtn, active == LabelDesignerView.TARGET_PRODUCT);
        styleTransformButton(transformPanelBtn, active == LabelDesignerView.TARGET_PANEL);
        styleTransformButton(transformInfoBtn, active == LabelDesignerView.TARGET_INFO);
    }

    private void styleTransformButton(Button b, boolean active){
        if (b == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        if (active) {
            bg.setColor(0xFF00BCD4);
            b.setTextColor(Color.WHITE);
            b.setElevation(dp(4));
        } else {
            bg.setColor(0xFFEFF3F5);
            b.setTextColor(0xFF37474F);
            b.setElevation(0f);
        }
        b.setBackground(bg);
    }

    private LinearLayout accordion(String title, boolean expanded){
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        Button header = btn((expanded?"▼  ":"▶  ")+title);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(8,2,8,8);
        content.setVisibility(expanded?View.VISIBLE:View.GONE);
        header.setOnClickListener(v->{
            boolean open=content.getVisibility()==View.VISIBLE;
            content.setVisibility(open?View.GONE:View.VISIBLE);
            header.setText((open?"▶  ":"▼  ")+title);
        });
        wrap.addView(header); wrap.addView(content);
        wrap.setTag(content);
        return wrap;
    }

    private LinearLayout accordionContent(LinearLayout accordion){ return (LinearLayout)accordion.getTag(); }

    private void designerChanged(){
        if(designer!=null) designer.invalidate();
        syncDesignerSettingsFromView();
        saveDesignerSettings();
        saveTemplate();
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
        GradientDrawable rootBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFEFF6F6, 0xFFDCEAEA});
        root.setBackground(rootBg);

        // Header: teal gradient card with soft shadow — replaces the old plain text banner.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding((int)dp(18),(int)dp(22),(int)dp(18),(int)dp(20));
        GradientDrawable headerBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0B6E64, 0xFF0FA093});
        header.setBackground(headerBg);
        header.setElevation(dp(8));

        TextView title = tv("Javdan Price Labeler",23,true);
        title.setTextColor(Color.WHITE);
        TextView subtitle = tv("Excel + ورود دستی + طراح حرفه‌ای لیبل",13,false);
        subtitle.setTextColor(0xFFD7F2EE);
        header.addView(title);
        header.addView(subtitle);
        root.addView(header);

        LinearLayout tabs = row();
        tabs.setPadding((int)dp(12),(int)dp(12),(int)dp(12),(int)dp(4));
        tabData = btn("Excel / دستی");
        tabDesigner = btn("طراح برچسب");
        tabOutput = btn("خروجی");

        LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(0,-2,1);
        tabLp.setMargins((int)dp(4),0,(int)dp(4),0);
        tabs.addView(tabData,tabLp);
        tabs.addView(tabDesigner,new LinearLayout.LayoutParams(tabLp));
        tabs.addView(tabOutput,new LinearLayout.LayoutParams(tabLp));
        root.addView(tabs);

        ScrollView sv = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding((int)dp(8),(int)dp(10),(int)dp(8),(int)dp(90));
        sv.addView(body);
        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        status = tv("آفلاین • عکس اصلی تغییر نمی‌کند",12,false);
        status.setTextColor(0xFF3E5C58);
        status.setPadding((int)dp(14),(int)dp(6),(int)dp(14),(int)dp(10));
        root.addView(status);

        setContentView(root);

        tabData.setOnClickListener(v -> showData());
        tabDesigner.setOnClickListener(v -> showDesigner());
        tabOutput.setOnClickListener(v -> showOutput());
        highlightTab(tabData);
    }

    /** Highlights the active tab with a filled accent pill; the others stay flat white cards. */
    private void highlightTab(Button active){
        for (Button b : new Button[]{tabData, tabDesigner, tabOutput}){
            if (b == null) continue;
            boolean isActive = b == active;
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            if (isActive) { bg.setColor(0xFF00BCD4); b.setTextColor(Color.WHITE); b.setElevation(dp(5)); }
            else { bg.setColor(0xFFFFFFFF); b.setTextColor(0xFF1F3A37); b.setElevation(dp(1)); }
            b.setBackground(bg);
        }
    }

    private void clear(){
        body.removeAllViews();
    }

    private void showData(){
        highlightTab(tabData);
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

            Button pi = btn("افزودن عکس محصول");
            body.addView(pi);
            pi.setOnClickListener(v -> showImageSourceChooser());

            TextView imageHint = tv("عکس اصلی محصول را از Gallery / Files انتخاب کنید؛ تصویر بدون پردازش واسط وارد طراح می‌شود.",12,false);
            imageHint.setTextColor(0xFF666666);
            body.addView(imageHint);

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

    private void makeDefaultInfoFields(){
        infoFields.clear();

        InfoField name = new InfoField("نام محصول","شامپو سیب");
        name.showTitle = false;
        name.fullRow = true;
        name.valueSize = 28;
        name.valueBold = true;
        name.backgroundColor = 0xFFF8FFF8;
        name.borderColor = 0xFF64B56A;

        InfoField weight = new InfoField("وزن","300");
        weight.showTitle = false;
        weight.suffix = "گرم";
        weight.valueSize = 18;
        weight.valueBold = true;
        weight.backgroundColor = 0xFFF8FFF8;
        weight.borderColor = 0xFF64B56A;

        InfoField carton = new InfoField("تعداد در کارتن","24");
        carton.showTitle = true;
        carton.valueSize = 18;
        carton.valueBold = true;
        carton.backgroundColor = 0xFFF8FFF8;
        carton.borderColor = 0xFF64B56A;

        infoFields.add(name);
        infoFields.add(weight);
        infoFields.add(carton);
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
        highlightTab(tabDesigner);
        if (manualRows != null && manualRows.getParent() != null) syncManualRows();
        clear();

        body.addView(section("طراح WYSIWYG برچسب"));
        body.addView(tv("Preview و JPG/PNG از یک Render Engine مشترک ساخته می‌شوند؛ تمام Sliderها Live هستند.",13,false));

        designer = new LabelDesignerView(this);
        applyDesignerSettingsToView(designer);
        designer.setFields(fields);
        designer.setInfoFields(infoFields);
        designer.setProductBitmap(currentBitmap);
        designer.setCustomBackgroundBitmap(customBackgroundBitmap);

        // Floating white "card" behind the canvas — gives the WYSIWYG preview real depth
        // instead of sitting flat against the page background.
        LinearLayout canvasCard = new LinearLayout(this);
        canvasCard.setOrientation(LinearLayout.VERTICAL);
        canvasCard.setPadding((int)dp(6),(int)dp(6),(int)dp(6),(int)dp(6));
        GradientDrawable canvasCardBg = new GradientDrawable();
        canvasCardBg.setCornerRadius(dp(18));
        canvasCardBg.setColor(0xFFFFFFFF);
        canvasCard.setBackground(canvasCardBg);
        canvasCard.setElevation(dp(10));
        LinearLayout.LayoutParams canvasCardLp = new LinearLayout.LayoutParams(-1,-2);
        canvasCardLp.setMargins(0,(int)dp(4),0,(int)dp(10));
        canvasCard.setLayoutParams(canvasCardLp);
        // TRUE WYSIWYG: LabelDesignerView measures itself with the canonical design aspect.
        // Do not force an arbitrary height here; that was the root cause of Preview/Export drift.
        canvasCard.addView(designer, new LinearLayout.LayoutParams(-1, -2));
        body.addView(canvasCard);

        designer.setListener(new LabelDesignerView.Listener() {
            @Override public void onFieldSelected(int i) { selectedField=i; showFieldEditor(); }
            @Override public void onChanged() { designerChanged(); }
            @Override public void onTextClicked(int fieldIndex, int textType) { selectedField=fieldIndex; showFieldEditor(); }
        });

        body.addView(buildTransformToolbar());
        refreshTransformButtons();

        // BACKGROUND
        LinearLayout bgAcc=accordion("Background",true); body.addView(bgAcc); LinearLayout bg=accordionContent(bgAcc);
        RadioGroup bgModes=new RadioGroup(this); bgModes.setOrientation(RadioGroup.HORIZONTAL);
        String[] bgNames={"رنگ ساده","Gradient","Pattern","تصویر"};
        for(int i=0;i<bgNames.length;i++){ final int mode=i; RadioButton rb=new RadioButton(this); rb.setText(bgNames[i]); rb.setChecked(settingBackgroundMode==i); rb.setOnClickListener(v->{settingBackgroundMode=mode; designer.backgroundMode=mode; designerChanged();}); bgModes.addView(rb); }
        bg.addView(bgModes);

        Button bgColor=colorButton("رنگ Background",settingBackgroundColor); bg.addView(bgColor);
        bgColor.setOnClickListener(v->showColorPalette("رنگ Background",settingBackgroundColor,c->{settingBackgroundColor=c;designer.canvasBackground=c;refreshColorButton(bgColor,"رنگ Background",c);designerChanged();}));
        Button g1=colorButton("Gradient رنگ اول",settingGradientColor1), g2=colorButton("Gradient رنگ دوم",settingGradientColor2);
        bg.addView(g1);bg.addView(g2);
        g1.setOnClickListener(v->showColorPalette("Gradient رنگ اول",settingGradientColor1,c->{settingGradientColor1=c;designer.gradientColor1=c;refreshColorButton(g1,"Gradient رنگ اول",c);settingBackgroundMode=LabelDesignerView.BG_GRADIENT;designer.backgroundMode=settingBackgroundMode;designerChanged();}));
        g2.setOnClickListener(v->showColorPalette("Gradient رنگ دوم",settingGradientColor2,c->{settingGradientColor2=c;designer.gradientColor2=c;refreshColorButton(g2,"Gradient رنگ دوم",c);settingBackgroundMode=LabelDesignerView.BG_GRADIENT;designer.backgroundMode=settingBackgroundMode;designerChanged();}));
        bg.addView(slider("زاویه Gradient",0,360,settingGradientAngle,5,"°",v->{settingGradientAngle=v;designer.gradientAngle=v;designerChanged();}));
        bg.addView(slider("شفافیت Background",0,255,settingBackgroundAlpha,1,"",v->{settingBackgroundAlpha=Math.round(v);designer.backgroundAlpha=settingBackgroundAlpha;designerChanged();}));

        bg.addView(tv("Patternهای آماده — لمس = نمایش فوری",13,true));
        String[] patterns={"موج آبی","موج طلایی","موج بنفش","خطوط مورب","خطوط ظریف","نقطه‌ای","هندسی","مشکی لوکس","ترمه","طرح ایرانی","سفید مینیمال"};
        GridLayout pg=new GridLayout(this);pg.setColumnCount(3);
        for(int i=0;i<patterns.length;i++){ final int ix=i; Button b=btn(patterns[i]); b.setOnClickListener(v->{settingPatternIndex=ix;settingBackgroundMode=LabelDesignerView.BG_PATTERN;designer.patternIndex=ix;designer.backgroundMode=settingBackgroundMode;designerChanged();}); pg.addView(b); }
        bg.addView(pg);
        Button customBg=btn("افزودن طرح دلخواه JPG / PNG"); bg.addView(customBg);
        customBg.setOnClickListener(v->pickFile("image/*",PICK_BACKGROUND));

        // PRODUCT / CROP
        LinearLayout prodAcc=accordion("Resize + Crop محصول",false);body.addView(prodAcc);LinearLayout prod=accordionContent(prodAcc);
        TextView prodTouchHint = tv("موقعیت، اندازه و Zoom عکس محصول از این پس با انگشت روی Preview تنظیم می‌شود: بالا دکمه «🖼 عکس محصول» را بزن، بعد بکش یا پینچ کن.",12,false);
        prodTouchHint.setTextColor(0xFF00838F);
        prod.addView(prodTouchHint);
        Button resetProductTransform = btn("↺ بازنشانی موقعیت و اندازه عکس");
        prod.addView(resetProductTransform);
        resetProductTransform.setOnClickListener(v->{
            designer.productX=0.02f; designer.productY=0.08f; designer.productW=0.62f; designer.productH=0.84f; designer.productZoom=1f;
            designerChanged();
        });
        LinearLayout cr=row();Button crop=btn("✂ Crop تصویر");Button reset=btn("Reset Crop");Button apply=btn("Apply Crop");cr.addView(crop,new LinearLayout.LayoutParams(0,-2,1));cr.addView(reset,new LinearLayout.LayoutParams(0,-2,1));cr.addView(apply,new LinearLayout.LayoutParams(0,-2,1));prod.addView(cr);
        crop.setOnClickListener(v->{designer.setCropMode(true);refreshTransformButtons();Toast.makeText(this,"چهار گوشه Crop را روی Preview بکش",Toast.LENGTH_SHORT).show();});
        reset.setOnClickListener(v->{designer.resetCrop();designer.setCropMode(true);refreshTransformButtons();designerChanged();});
        apply.setOnClickListener(v->{designer.setCropMode(false);designerChanged();});
        groupCropCheck=check("همین Crop روی همه تصاویر خروجی گروهی اعمال شود",true);prod.addView(groupCropCheck);

        Button sample=btn("انتخاب / تغییر عکس نمونه");prod.addView(sample);sample.setOnClickListener(v->showImageSourceChooser());

        // MAIN PANEL
        LinearLayout panelAcc=accordion("قاب اصلی لیبل",false);body.addView(panelAcc);LinearLayout panel=accordionContent(panelAcc);
        TextView panelTouchHint = tv("موقعیت و عرض قاب لیبل از این پس با انگشت تنظیم می‌شود: بالا دکمه «🏷 قاب لیبل» را بزن، بعد بکش یا پینچ کن.",12,false);
        panelTouchHint.setTextColor(0xFF00838F);
        panel.addView(panelTouchHint);
        Button resetPanelTransform = btn("↺ بازنشانی موقعیت و عرض قاب لیبل");
        panel.addView(resetPanelTransform);
        resetPanelTransform.setOnClickListener(v->{
            designer.labelWidthPct=0.28f; designer.labelX=0.70f; designer.labelY=0.12f;
            designerChanged();
        });
        panel.addView(slider("فاصله بین کادرها",0,40,settingFieldGapPx,1,"px",v->{settingFieldGapPx=v;designer.fieldGapPx=v;designerChanged();}));
        panel.addView(slider("فاصله داخلی قاب اصلی",0,40,settingPanelPaddingPx,1,"px",v->{settingPanelPaddingPx=v;designer.panelPaddingPx=v;designerChanged();}));
        panel.addView(slider("گردی گوشه قاب",0,100,settingOuterRadius,1,"px",v->{settingOuterRadius=Math.round(v);designer.outerTagRadius=settingOuterRadius;designerChanged();}));
        panel.addView(slider("ضخامت حاشیه قاب",0,30,settingOuterBorderWidth,1,"px",v->{settingOuterBorderWidth=Math.round(v);designer.outerTagBorderWidth=settingOuterBorderWidth;designerChanged();}));
        Button outer=colorButton("رنگ قاب اصلی",settingOuterColor);panel.addView(outer);outer.setOnClickListener(v->showColorPalette("رنگ قاب اصلی",settingOuterColor,c->{settingOuterColor=c;designer.outerTagColor=c;refreshColorButton(outer,"رنگ قاب اصلی",c);designerChanged();}));
        Button outerBorder=colorButton("رنگ Border قاب",settingOuterBorderColor);panel.addView(outerBorder);outerBorder.setOnClickListener(v->showColorPalette("رنگ Border قاب",settingOuterBorderColor,c->{settingOuterBorderColor=c;designer.outerTagBorderColor=c;refreshColorButton(outerBorder,"رنگ Border قاب",c);designerChanged();}));
        CheckBox autoHeight=check("ارتفاع لیبل خودکار (Auto Height)",settingAutoHeight); panel.addView(autoHeight);
        LinearLayout manualHeightSlider = slider("ارتفاع دستی کادرها",35,300,settingManualCardHeightPx,1,"px",v->{
            settingManualCardHeightPx=v;
            designer.manualCardHeightPx=v;
            designerChanged();
        });
        panel.addView(manualHeightSlider);
        manualHeightSlider.setAlpha(settingAutoHeight ? 0.45f : 1f);
        autoHeight.setOnCheckedChangeListener((b,c)->{
            settingAutoHeight=c;
            designer.autoHeight=c;
            manualHeightSlider.setAlpha(c ? 0.45f : 1f);
            designerChanged();
        });

        // PRODUCT INFORMATION PANEL
        if(infoFields.isEmpty()) makeDefaultInfoFields();
        LinearLayout infoAcc=accordion("قاب اطلاعات محصول",false);body.addView(infoAcc);LinearLayout infoPanel=accordionContent(infoAcc);

        CheckBox infoEnabled=check("نمایش قاب اطلاعات محصول",settingInfoEnabled);infoPanel.addView(infoEnabled);
        infoEnabled.setOnCheckedChangeListener((b,c)->{settingInfoEnabled=c;designer.infoPanelEnabled=c;designerChanged();});

        String[] infoPositionLabels={"پایین محصول","بالای محصول","موقعیت آزاد"};
        Spinner infoPosition=makeSpinner(infoPositionLabels,clampInt(settingInfoPositionMode,0,2));
        addLabeledSpinner(infoPanel,"موقعیت قاب",infoPosition);
        infoPosition.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){settingInfoPositionMode=pos;designer.infoPositionMode=pos;designerChanged();}});

        String[] infoLayoutLabels={"زیر هم (عمودی)","کنار هم (افقی)","چندردیفی / Wrap"};
        Spinner infoLayout=makeSpinner(infoLayoutLabels,clampInt(settingInfoLayoutMode,0,2));
        addLabeledSpinner(infoPanel,"چیدمان کادرها",infoLayout);
        infoLayout.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){settingInfoLayoutMode=pos;designer.infoLayoutMode=pos;designerChanged();}});

        TextView infoTouchHint = tv("عرض و موقعیت قاب اطلاعات از این پس با انگشت تنظیم می‌شود: بالا دکمه «ℹ️ قاب اطلاعات» را بزن، بعد بکش یا پینچ کن (موقعیت عمودی فقط در حالت «موقعیت آزاد» اثر دارد).",12,false);
        infoTouchHint.setTextColor(0xFF00838F);
        infoPanel.addView(infoTouchHint);
        Button resetInfoTransform = btn("↺ بازنشانی موقعیت و عرض قاب اطلاعات");
        infoPanel.addView(resetInfoTransform);
        resetInfoTransform.setOnClickListener(v->{
            designer.infoWidthPct=0.48f; designer.infoX=0.10f; designer.infoY=0.76f;
            designerChanged();
        });
        infoPanel.addView(slider("فاصله از محصول",0,100,settingInfoDistancePx,1,"px",v->{settingInfoDistancePx=v;designer.infoDistancePx=v;designerChanged();}));
        infoPanel.addView(slider("فاصله بین کادرها",0,40,settingInfoGapPx,1,"px",v->{settingInfoGapPx=v;designer.infoGapPx=v;designerChanged();}));
        infoPanel.addView(slider("Padding قاب اطلاعات",0,40,settingInfoPaddingPx,1,"px",v->{settingInfoPaddingPx=v;designer.infoPaddingPx=v;designerChanged();}));
        infoPanel.addView(slider("تعداد ستون در حالت Wrap",1,4,settingInfoColumns,1,"",v->{settingInfoColumns=Math.round(v);designer.infoColumns=settingInfoColumns;designerChanged();}));
        infoPanel.addView(slider("گردی گوشه قاب اطلاعات",0,100,settingInfoOuterRadius,1,"px",v->{settingInfoOuterRadius=Math.round(v);designer.infoOuterRadius=settingInfoOuterRadius;designerChanged();}));
        infoPanel.addView(slider("ضخامت Border قاب اطلاعات",0,20,settingInfoOuterBorderWidth,1,"px",v->{settingInfoOuterBorderWidth=Math.round(v);designer.infoOuterBorderWidth=settingInfoOuterBorderWidth;designerChanged();}));

        Button infoOuter=colorButton("رنگ Background قاب اطلاعات",settingInfoOuterColor);infoPanel.addView(infoOuter);
        infoOuter.setOnClickListener(v->showColorPalette("رنگ Background قاب اطلاعات",settingInfoOuterColor,c->{settingInfoOuterColor=c;designer.infoOuterColor=c;refreshColorButton(infoOuter,"رنگ Background قاب اطلاعات",c);designerChanged();}));
        Button infoBorder=colorButton("رنگ Border قاب اطلاعات",settingInfoOuterBorderColor);infoPanel.addView(infoBorder);
        infoBorder.setOnClickListener(v->showColorPalette("رنگ Border قاب اطلاعات",settingInfoOuterBorderColor,c->{settingInfoOuterBorderColor=c;designer.infoOuterBorderColor=c;refreshColorButton(infoBorder,"رنگ Border قاب اطلاعات",c);designerChanged();}));

        String[] infoNames=new String[infoFields.size()];
        for(int i=0;i<infoFields.size();i++)infoNames[i]=safeInfoTitle(infoFields.get(i),i);
        if(selectedInfoField<0||selectedInfoField>=infoFields.size())selectedInfoField=0;
        infoFieldSpinner=makeSpinner(infoNames,selectedInfoField);
        addLabeledSpinner(infoPanel,"کادر اطلاعات انتخاب‌شده",infoFieldSpinner);

        LinearLayout infoEditAcc=accordion("تنظیم کادر اطلاعات انتخاب‌شده",true);infoPanel.addView(infoEditAcc);
        infoEditorContainer=accordionContent(infoEditAcc);
        infoFieldSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){selectedInfoField=pos;showInfoFieldEditor();}});
        showInfoFieldEditor();

        LinearLayout infoActions=row();
        Button addInfo=btn("+ افزودن کادر اطلاعات");
        Button saveInfo=btn("ذخیره تنظیمات");
        infoActions.addView(addInfo,new LinearLayout.LayoutParams(0,-2,1));
        infoActions.addView(saveInfo,new LinearLayout.LayoutParams(0,-2,1));
        infoPanel.addView(infoActions);

        addInfo.setOnClickListener(v->{
            InfoField f=new InfoField("مشخصات جدید","");
            f.fullRow=false;
            infoFields.add(f);
            selectedInfoField=infoFields.size()-1;
            designer.setInfoFields(infoFields);
            designerChanged();
            showDesigner();
        });
        saveInfo.setOnClickListener(v->{designerChanged();Toast.makeText(this,"قاب اطلاعات محصول ذخیره شد",Toast.LENGTH_SHORT).show();});

        // FIELD EDITOR
        LinearLayout fieldAcc=accordion("تنظیم کادر قیمت انتخاب‌شده",true);body.addView(fieldAcc);
        fieldEditorContainer=accordionContent(fieldAcc);
        if(!fields.isEmpty()){if(selectedField<0||selectedField>=fields.size())selectedField=0;designer.select(selectedField);showFieldEditor();}

        LinearLayout actions=row(); Button add=btn("+ افزودن کادر");Button save=btn("ذخیره Template");actions.addView(add,new LinearLayout.LayoutParams(0,-2,1));actions.addView(save,new LinearLayout.LayoutParams(0,-2,1));body.addView(actions);
        add.setOnClickListener(v->{fields.add(new LabelField("قیمت جدید",""));selectedField=fields.size()-1;designer.setFields(fields);designer.select(selectedField);showFieldEditor();designerChanged();});
        save.setOnClickListener(v->{designerChanged();Toast.makeText(this,"Template ذخیره شد",Toast.LENGTH_SHORT).show();});
    }


    private void showInfoFieldEditor(){
        if(infoEditorContainer==null||selectedInfoField<0||selectedInfoField>=infoFields.size())return;
        infoEditorContainer.removeAllViews();
        final InfoField f=infoFields.get(selectedInfoField);

        TextView selectedLabel=tv("در حال ویرایش: "+safeInfoTitle(f,selectedInfoField),14,true);
        infoEditorContainer.addView(selectedLabel);

        EditText title=textEdit(f.title,"عنوان");
        EditText value=textEdit(f.value,"مقدار نمونه");
        EditText prefix=textEdit(f.prefix,"Prefix");
        EditText suffix=textEdit(f.suffix,"Suffix مثل گرم");
        addLabeledEdit(infoEditorContainer,"عنوان",title);
        addLabeledEdit(infoEditorContainer,"مقدار نمونه",value);
        addLabeledEdit(infoEditorContainer,"متن قبل مقدار",prefix);
        addLabeledEdit(infoEditorContainer,"متن بعد مقدار",suffix);

        title.addTextChangedListener(new SimpleWatcher(){public void afterTextChanged(Editable e){
            f.title=e.toString();
            selectedLabel.setText("در حال ویرایش: "+safeInfoTitle(f,selectedInfoField));
            designerChanged();
        }});
        value.addTextChangedListener(new SimpleWatcher(){public void afterTextChanged(Editable e){f.value=e.toString();designerChanged();}});
        prefix.addTextChangedListener(new SimpleWatcher(){public void afterTextChanged(Editable e){f.prefix=e.toString();designerChanged();}});
        suffix.addTextChangedListener(new SimpleWatcher(){public void afterTextChanged(Editable e){f.suffix=e.toString();designerChanged();}});

        String[] sourceOptions=excelHeaderOptions();
        Spinner source=makeSpinner(sourceOptions,headerSelectionIndex(f.sourceHeader));
        addLabeledSpinner(infoEditorContainer,"منبع داده از Excel",source);
        source.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                f.sourceHeader=pos<=0?"":headers.get(pos-1);
                updateInfoPreviewValue(f);
                designerChanged();
            }
        });

        Spinner titleFont=makeSpinner(FONT_LABELS,fontIndex(f.titleFont));
        Spinner valueFont=makeSpinner(FONT_LABELS,fontIndex(f.valueFont));
        Spinner align=makeSpinner(ALIGN_LABELS,clampInt(f.textAlign,0,2));
        addLabeledSpinner(infoEditorContainer,"فونت عنوان",titleFont);
        addLabeledSpinner(infoEditorContainer,"فونت مقدار",valueFont);
        addLabeledSpinner(infoEditorContainer,"Alignment",align);

        AdapterView.OnItemSelectedListener formatListener=new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                f.titleFont=FONT_VALUES[titleFont.getSelectedItemPosition()];
                f.valueFont=FONT_VALUES[valueFont.getSelectedItemPosition()];
                f.textAlign=align.getSelectedItemPosition();
                designerChanged();
            }
        };
        titleFont.setOnItemSelectedListener(formatListener);
        valueFont.setOnItemSelectedListener(formatListener);
        align.setOnItemSelectedListener(formatListener);

        infoEditorContainer.addView(slider("اندازه فونت عنوان",8,72,f.titleSize,1,"sp",v->{f.titleSize=Math.round(v);designerChanged();}));
        infoEditorContainer.addView(slider("اندازه فونت مقدار",8,80,f.valueSize,1,"sp",v->{f.valueSize=Math.round(v);designerChanged();}));
        infoEditorContainer.addView(slider("فاصله عنوان تا مقدار",0,40,f.titleValueGap,1,"px",v->{f.titleValueGap=Math.round(v);designerChanged();}));
        infoEditorContainer.addView(slider("Padding افقی",0,60,f.paddingHorizontal,1,"px",v->{f.paddingHorizontal=Math.round(v);designerChanged();}));
        infoEditorContainer.addView(slider("Padding عمودی",0,60,f.paddingVertical,1,"px",v->{f.paddingVertical=Math.round(v);designerChanged();}));
        infoEditorContainer.addView(slider("گردی گوشه کادر",0,100,f.cornerRadius,1,"px",v->{f.cornerRadius=Math.round(v);designerChanged();}));
        infoEditorContainer.addView(slider("ضخامت Border",0,20,f.borderWidth,1,"px",v->{f.borderWidth=Math.round(v);designerChanged();}));

        Button titleColor=colorButton("رنگ عنوان",f.titleColor);
        Button valueColor=colorButton("رنگ مقدار",f.valueColor);
        Button bgColor=colorButton("Background کادر",f.backgroundColor);
        Button borderColor=colorButton("Border کادر",f.borderColor);
        infoEditorContainer.addView(titleColor);
        infoEditorContainer.addView(valueColor);
        infoEditorContainer.addView(bgColor);
        infoEditorContainer.addView(borderColor);

        titleColor.setOnClickListener(v->showColorPalette("رنگ عنوان",f.titleColor,c->{f.titleColor=c;refreshColorButton(titleColor,"رنگ عنوان",c);designerChanged();}));
        valueColor.setOnClickListener(v->showColorPalette("رنگ مقدار",f.valueColor,c->{f.valueColor=c;refreshColorButton(valueColor,"رنگ مقدار",c);designerChanged();}));
        bgColor.setOnClickListener(v->showColorPalette("Background کادر",f.backgroundColor,c->{f.backgroundColor=c;refreshColorButton(bgColor,"Background کادر",c);designerChanged();}));
        borderColor.setOnClickListener(v->showColorPalette("Border کادر",f.borderColor,c->{f.borderColor=c;refreshColorButton(borderColor,"Border کادر",c);designerChanged();}));

        CheckBox visible=check("نمایش این کادر",f.visible);
        CheckBox showTitle=check("نمایش عنوان",f.showTitle);
        CheckBox showValue=check("نمایش مقدار",f.showValue);
        CheckBox fullRow=check("این کادر یک ردیف کامل باشد",f.fullRow);
        CheckBox titleBold=check("Bold عنوان",f.titleBold);
        CheckBox titleItalic=check("Italic عنوان",f.titleItalic);
        CheckBox valueBold=check("Bold مقدار",f.valueBold);
        CheckBox valueItalic=check("Italic مقدار",f.valueItalic);

        CheckBox[] infoBoxes={visible,showTitle,showValue,fullRow,titleBold,titleItalic,valueBold,valueItalic};
        for(CheckBox b:infoBoxes)infoEditorContainer.addView(b);

        visible.setOnCheckedChangeListener((b,c)->{f.visible=c;designerChanged();});
        showTitle.setOnCheckedChangeListener((b,c)->{f.showTitle=c;designerChanged();});
        showValue.setOnCheckedChangeListener((b,c)->{f.showValue=c;designerChanged();});
        fullRow.setOnCheckedChangeListener((b,c)->{f.fullRow=c;designerChanged();});
        titleBold.setOnCheckedChangeListener((b,c)->{f.titleBold=c;designerChanged();});
        titleItalic.setOnCheckedChangeListener((b,c)->{f.titleItalic=c;designerChanged();});
        valueBold.setOnCheckedChangeListener((b,c)->{f.valueBold=c;designerChanged();});
        valueItalic.setOnCheckedChangeListener((b,c)->{f.valueItalic=c;designerChanged();});

        Button del=btn("حذف این کادر اطلاعات");
        infoEditorContainer.addView(del);
        del.setOnClickListener(v->{
            if(infoFields.size()<=1){
                Toast.makeText(this,"حداقل یک کادر اطلاعات باید باقی بماند",Toast.LENGTH_SHORT).show();
                return;
            }
            infoFields.remove(selectedInfoField);
            selectedInfoField=Math.min(selectedInfoField,infoFields.size()-1);
            if(designer!=null)designer.setInfoFields(infoFields);
            designerChanged();
            showDesigner();
        });
    }

    private void showFieldEditor(){
        if(fieldEditorContainer==null||selectedField<0||selectedField>=fields.size())return;
        fieldEditorContainer.removeAllViews();
        final LabelField f=fields.get(selectedField);
        TextView selectedLabel=tv("در حال ویرایش: "+f.name,14,true);fieldEditorContainer.addView(selectedLabel);

        EditText name=textEdit(f.name,"عنوان"); EditText value=textEdit(f.value,"قیمت");
        addLabeledEdit(fieldEditorContainer,"عنوان",name);addLabeledEdit(fieldEditorContainer,"قیمت",value);
        name.addTextChangedListener(new SimpleWatcher(){public void afterTextChanged(Editable e){f.name=e.toString();selectedLabel.setText("در حال ویرایش: "+f.name);designerChanged();}});
        value.addTextChangedListener(new SimpleWatcher(){public void afterTextChanged(Editable e){f.value=e.toString();designerChanged();}});

        Spinner titleFont=makeSpinner(FONT_LABELS,fontIndex(f.titleFont));Spinner priceFont=makeSpinner(FONT_LABELS,fontIndex(f.priceFont));Spinner align=makeSpinner(ALIGN_LABELS,clampInt(f.textAlign,0,2));
        addLabeledSpinner(fieldEditorContainer,"فونت عنوان",titleFont);addLabeledSpinner(fieldEditorContainer,"فونت قیمت",priceFont);addLabeledSpinner(fieldEditorContainer,"Alignment",align);
        AdapterView.OnItemSelectedListener fontListener=new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,View v,int pos,long id){f.titleFont=FONT_VALUES[titleFont.getSelectedItemPosition()];f.priceFont=FONT_VALUES[priceFont.getSelectedItemPosition()];f.textAlign=align.getSelectedItemPosition();designerChanged();}};
        titleFont.setOnItemSelectedListener(fontListener);priceFont.setOnItemSelectedListener(fontListener);align.setOnItemSelectedListener(fontListener);

        fieldEditorContainer.addView(slider("اندازه فونت عنوان",8,72,f.titleSize,1,"sp",v->{f.titleSize=Math.round(v);designerChanged();}));
        fieldEditorContainer.addView(slider("اندازه فونت قیمت",10,100,f.priceSize,1,"sp",v->{f.priceSize=Math.round(v);designerChanged();}));
        fieldEditorContainer.addView(slider("اندازه فونت تومان",7,60,f.tomanSize,1,"sp",v->{f.tomanSize=Math.round(v);designerChanged();}));
        fieldEditorContainer.addView(slider("فاصله عنوان تا قیمت",0,50,f.titlePriceGap,1,"px",v->{f.titlePriceGap=Math.round(v);designerChanged();}));
        fieldEditorContainer.addView(slider("Padding افقی",0,80,f.paddingHorizontal,1,"px",v->{f.paddingHorizontal=Math.round(v);designerChanged();}));
        fieldEditorContainer.addView(slider("Padding عمودی",0,80,f.paddingVertical,1,"px",v->{f.paddingVertical=Math.round(v);designerChanged();}));
        fieldEditorContainer.addView(slider("گردی گوشه‌ها",0,100,f.cornerRadius,1,"px",v->{f.cornerRadius=Math.round(v);designerChanged();}));
        fieldEditorContainer.addView(slider("ضخامت Border",0,30,f.borderWidth,1,"px",v->{f.borderWidth=Math.round(v);designerChanged();}));

        Button titleColor=colorButton("رنگ عنوان",f.titleColor), priceColor=colorButton("رنگ قیمت",f.priceColor), tomanColor=colorButton("رنگ تومان",f.tomanColor), bgColor=colorButton("Background کادر",f.backgroundColor), borderColor=colorButton("Border کادر",f.borderColor);
        fieldEditorContainer.addView(titleColor);fieldEditorContainer.addView(priceColor);fieldEditorContainer.addView(tomanColor);fieldEditorContainer.addView(bgColor);fieldEditorContainer.addView(borderColor);
        titleColor.setOnClickListener(v->showColorPalette("رنگ عنوان",f.titleColor,c->{f.titleColor=c;refreshColorButton(titleColor,"رنگ عنوان",c);designerChanged();}));
        priceColor.setOnClickListener(v->showColorPalette("رنگ قیمت",f.priceColor,c->{f.priceColor=c;refreshColorButton(priceColor,"رنگ قیمت",c);designerChanged();}));
        tomanColor.setOnClickListener(v->showColorPalette("رنگ تومان",f.tomanColor,c->{f.tomanColor=c;refreshColorButton(tomanColor,"رنگ تومان",c);designerChanged();}));
        bgColor.setOnClickListener(v->showColorPalette("Background کادر",f.backgroundColor,c->{f.backgroundColor=c;refreshColorButton(bgColor,"Background کادر",c);designerChanged();}));
        borderColor.setOnClickListener(v->showColorPalette("Border کادر",f.borderColor,c->{f.borderColor=c;refreshColorButton(borderColor,"Border کادر",c);designerChanged();}));

        CheckBox showTitle=check("نمایش عنوان",f.showTitle),showPrice=check("نمایش قیمت",f.showPrice),showToman=check("نمایش تومان",f.showToman),visible=check("نمایش کادر",f.visible),strike=check("Strike-through قیمت",f.strike),titleBold=check("عنوان Bold",f.titleBold),titleItalic=check("عنوان Italic",f.titleItalic),priceBold=check("قیمت Bold",f.priceBold),priceItalic=check("قیمت Italic",f.priceItalic);
        CheckBox[] boxes={showTitle,showPrice,showToman,visible,strike,titleBold,titleItalic,priceBold,priceItalic};for(CheckBox b:boxes)fieldEditorContainer.addView(b);
        CompoundButton.OnCheckedChangeListener checks=(button,checked)->{f.showTitle=showTitle.isChecked();f.showPrice=showPrice.isChecked();f.showToman=showToman.isChecked();f.visible=visible.isChecked();f.strike=strike.isChecked();f.titleBold=titleBold.isChecked();f.titleItalic=titleItalic.isChecked();f.priceBold=priceBold.isChecked();f.priceItalic=priceItalic.isChecked();designerChanged();};for(CheckBox b:boxes)b.setOnCheckedChangeListener(checks);

        Button copy=btn("اعمال استایل این کادر برای همه");fieldEditorContainer.addView(copy);copy.setOnClickListener(v->{for(int i=0;i<fields.size();i++)if(i!=selectedField)copyStyleOnly(f,fields.get(i));designer.setFields(fields);designerChanged();});
        Button del=btn("حذف این کادر");fieldEditorContainer.addView(del);del.setOnClickListener(v->{if(fields.size()<=1){Toast.makeText(this,"حداقل یک کادر باید باقی بماند",Toast.LENGTH_SHORT).show();return;}fields.remove(selectedField);selectedField=Math.min(selectedField,fields.size()-1);designer.setFields(fields);designer.select(selectedField);showFieldEditor();designerChanged();});
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        public void beforeTextChanged(CharSequence s,int start,int count,int after){}
        public void onTextChanged(CharSequence s,int start,int before,int count){}
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
        final Dialog dialog=new Dialog(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,18,24,18);
        TextView cap=tv(title,18,true);cap.setGravity(Gravity.CENTER);root.addView(cap);

        final float[] hsv=new float[3];Color.colorToHSV(currentColor,hsv);final int[] chosen={currentColor};
        TextView preview=new TextView(this);preview.setText("Preview   "+colorToHex(currentColor));preview.setGravity(Gravity.CENTER);preview.setTextSize(16);preview.setPadding(8,18,8,18);root.addView(preview);
        ColorSvView sv=new ColorSvView(this,hsv[0],hsv[1],hsv[2]);root.addView(sv,new LinearLayout.LayoutParams(-1,(int)(180*getResources().getDisplayMetrics().density)));
        SeekBar hue=new SeekBar(this);hue.setMax(360);hue.setProgress(Math.round(hsv[0]));root.addView(tv("Hue",13,true));root.addView(hue);
        EditText hex=textEdit(colorToHex(currentColor),"#FFFFFFFF");addLabeledEdit(root,"HEX",hex);

        Runnable refresh=()->{chosen[0]=Color.HSVToColor(Color.alpha(currentColor),hsv);preview.setText("Preview   "+colorToHex(chosen[0]));GradientDrawable g=new GradientDrawable();g.setColor(chosen[0]);g.setCornerRadius(12);preview.setBackground(g);hex.setText(colorToHex(chosen[0]));};
        sv.setListener((sat,val)->{hsv[1]=sat;hsv[2]=val;refresh.run();});
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean f){hsv[0]=p;sv.setHue(p);refresh.run();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});

        root.addView(tv("رنگ‌های آماده",13,true));GridLayout grid=new GridLayout(this);grid.setColumnCount(6);
        int[] colors={0xFFFFFFFF,0xFFF5F5F5,0xFF9E9E9E,0xFF212121,0xFFFFD600,0xFFE91319,0xFF63BF67,0xFF1976D2,0xFF0D47A1,0xFF7E57C2,0xFFC2185B,0xFFFF9800,0xFF2E7D32,0xFF00796B,0xFF000000,0xFFB78D38,0xFF7041A6,0xFF2779BD};
        int size=(int)(42*getResources().getDisplayMetrics().density),margin=(int)(3*getResources().getDisplayMetrics().density);
        for(int c:colors){TextView box=new TextView(this);GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(9);g.setStroke(1,0xFFBDBDBD);box.setBackground(g);GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=size;lp.height=size;lp.setMargins(margin,margin,margin,margin);box.setLayoutParams(lp);box.setOnClickListener(v->{Color.colorToHSV(c,hsv);hue.setProgress(Math.round(hsv[0]));sv.setHSV(hsv[0],hsv[1],hsv[2]);chosen[0]=c;refresh.run();});grid.addView(box);}root.addView(grid);

        LinearLayout buttons=row();Button cancel=btn("انصراف"),ok=btn("اعمال رنگ");buttons.addView(cancel,new LinearLayout.LayoutParams(0,-2,1));buttons.addView(ok,new LinearLayout.LayoutParams(0,-2,1));root.addView(buttons);
        cancel.setOnClickListener(v->dialog.dismiss());ok.setOnClickListener(v->{try{String hs=hex.getText().toString().trim();if(hs.startsWith("#"))hs=hs.substring(1);long raw=Long.parseLong(hs,16);if(hs.length()==6)raw|=0xFF000000L;chosen[0]=(int)raw;}catch(Exception ignored){}listener.onColorSelected(chosen[0]);dialog.dismiss();});
        refresh.run();dialog.setContentView(root);dialog.show();Window w=dialog.getWindow();if(w!=null)w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static class ColorSvView extends View {
        interface Listener{void onChanged(float saturation,float value);} private float hue,sat,val;private Listener listener;private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        ColorSvView(Context c,float h,float s,float v){super(c);hue=h;sat=s;val=v;}
        void setListener(Listener l){listener=l;} void setHue(float h){hue=h;invalidate();} void setHSV(float h,float s,float v){hue=h;sat=s;val=v;invalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);int hc=Color.HSVToColor(new float[]{hue,1,1});Paint p=new Paint();p.setShader(new LinearGradient(0,0,getWidth(),0,Color.WHITE,hc,Shader.TileMode.CLAMP));c.drawRect(0,0,getWidth(),getHeight(),p);Paint shade=new Paint();shade.setShader(new LinearGradient(0,0,0,getHeight(),Color.TRANSPARENT,Color.BLACK,Shader.TileMode.CLAMP));c.drawRect(0,0,getWidth(),getHeight(),shade);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);paint.setColor(Color.WHITE);c.drawCircle(sat*getWidth(),(1-val)*getHeight(),12,paint);paint.setColor(Color.BLACK);paint.setStrokeWidth(2);c.drawCircle(sat*getWidth(),(1-val)*getHeight(),14,paint);}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){sat=Math.max(0,Math.min(1,e.getX()/Math.max(1f,getWidth())));val=1-Math.max(0,Math.min(1,e.getY()/Math.max(1f,getHeight())));invalidate();if(listener!=null)listener.onChanged(sat,val);return true;}return true;}
    }

    private void showOutput(){
        highlightTab(tabOutput);
        if (manualRows != null && manualRows.getParent() != null) syncManualRows();
        clear();

        body.addView(section("خروجی"));

        appendMode = check("حالت قدیمی: لیبل بیرون تصویر قرار بگیرد",false);
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
        return render(src,useFields,infoFields,applyCrop);
    }

    private Bitmap render(Bitmap src, ArrayList<LabelField> useFields, ArrayList<InfoField> useInfoFields, boolean applyCrop){
        LabelDesignerView r = new LabelDesignerView(this);
        applyDesignerSettingsToView(r);
        r.setFields(useFields);
        r.setInfoFields(useInfoFields);

        if (!applyCrop){
            r.cropEnabled = false;
            r.cropLeft = 0f;
            r.cropTop = 0f;
            r.cropRight = 1f;
            r.cropBottom = 1f;
        }

        boolean append = appendMode != null && appendMode.isChecked();
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

    private void showImageSourceChooser(){
        // No camera and no preparation/crop stage:
        // the original image is selected directly and loaded into the Designer.
        pickFile("image/*", PICK_IMAGE);
    }

    private Bitmap loadBitmapFromUri(Uri u) throws Exception {
        Bitmap bmp;
        try (InputStream in = getContentResolver().openInputStream(u)){
            bmp = BitmapFactory.decodeStream(in);
        }
        if (bmp == null) throw new IOException("تصویر قابل خواندن نیست");

        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try (InputStream exifIn = getContentResolver().openInputStream(u)){
            if (exifIn != null){
                ExifInterface exif = new ExifInterface(exifIn);
                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
            }
        } catch (Exception ignored){}

        Matrix m = new Matrix();
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) m.postRotate(90);
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) m.postRotate(180);
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) m.postRotate(270);
        else if (orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL) m.postScale(-1,1);
        else if (orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL) m.postScale(1,-1);
        else if (orientation == ExifInterface.ORIENTATION_TRANSPOSE){ m.postRotate(90); m.postScale(-1,1); }
        else if (orientation == ExifInterface.ORIENTATION_TRANSVERSE){ m.postRotate(270); m.postScale(-1,1); }

        if (!m.isIdentity()){
            Bitmap rotated = Bitmap.createBitmap(bmp,0,0,bmp.getWidth(),bmp.getHeight(),m,true);
            if (rotated != bmp) bmp.recycle();
            bmp = rotated;
        }
        return bmp;
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
            getContentResolver().takePersistableUriPermission(
                    u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored){}

        try {
            if (req == PICK_IMAGE){
                // Load the user's original file directly.
                // No camera or preparation Activity; the original image is loaded directly.
                Bitmap bmp = loadBitmapFromUri(u);
                if (bmp == null) throw new IOException("تصویر قابل خواندن نیست");

                currentBitmap = bmp;
                imageUri = u;

                if (status != null)
                    status.setText("عکس اصلی محصول انتخاب شد");
                if (previewStatus != null)
                    previewStatus.setText("عکس اصلی بدون پردازش وارد طراح شد.");

                if (designer != null){
                    designer.setProductBitmap(currentBitmap);
                    designer.requestLayout();
                    designer.invalidate();
                }

                Toast.makeText(this,
                        "عکس اصلی بدون تغییر وارد شد",
                        Toast.LENGTH_SHORT).show();
                return;

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

            } else if (req == PICK_BACKGROUND){
                settingCustomBackgroundUri = u.toString();
                try (InputStream in = getContentResolver().openInputStream(u)){
                    customBackgroundBitmap = BitmapFactory.decodeStream(in);
                }
                settingBackgroundMode = LabelDesignerView.BG_IMAGE;
                if (designer != null){
                    designer.setCustomBackgroundBitmap(customBackgroundBitmap);
                    designer.backgroundMode = settingBackgroundMode;
                    designer.invalidate();
                }
                saveDesignerSettings();
                Toast.makeText(this,"Background تصویر انتخاب شد",Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e){
            Toast.makeText(this, safeMessage(e), Toast.LENGTH_LONG).show();
        }
    }

    private void refreshExcelUi(){
        if (codeSpinner != null){
            codeSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,headers));
        }

        if (excelInfo != null){
            excelInfo.removeAllViews();
            excelInfo.addView(tv("ردیف‌ها: "+excelRows.size()+" | ستون‌ها: "+headers.size(),13,false));

            if (infoFields.isEmpty()) makeDefaultInfoFields();

            excelInfo.addView(tv("Mapping اطلاعات محصول از Excel",14,true));
            String[] options = excelHeaderOptions();
            for (int i=0;i<infoFields.size();i++){
                final InfoField info = infoFields.get(i);
                String label = safeInfoTitle(info,i);
                Spinner sp = makeSpinner(options, headerSelectionIndex(info.sourceHeader));
                addLabeledSpinner(excelInfo,label,sp);
                sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                    public void onNothingSelected(AdapterView<?> p){}
                    public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                        info.sourceHeader = pos <= 0 ? "" : headers.get(pos-1);
                        updateInfoPreviewValue(info);
                        saveTemplate();
                        if(designer!=null){designer.setInfoFields(infoFields);designer.invalidate();}
                    }
                });
            }
            TextView hint=tv("وزن فقط از ستون انتخاب‌شده خوانده می‌شود و «گرم» به‌صورت خودکار در خروجی اضافه می‌شود.",12,false);
            hint.setTextColor(0xFF666666);
            excelInfo.addView(hint);
        }
    }

    private String[] excelHeaderOptions(){
        String[] options = new String[headers.size()+1];
        options[0] = "— انتخاب نشده —";
        for(int i=0;i<headers.size();i++) options[i+1]=headers.get(i);
        return options;
    }

    private int headerSelectionIndex(String header){
        if(header==null||header.trim().isEmpty())return 0;
        for(int i=0;i<headers.size();i++)if(header.equals(headers.get(i)))return i+1;
        return 0;
    }

    private void updateInfoPreviewValue(InfoField info){
        if(info==null||info.sourceHeader==null||info.sourceHeader.isEmpty()||excelRows.isEmpty())return;
        for(LinkedHashMap<String,String> row:excelRows){
            String raw=row.get(info.sourceHeader);
            if(raw!=null&&!raw.trim().isEmpty()){
                info.value=cleanInfoValue(raw);
                return;
            }
        }
    }

    private String cleanInfoValue(String raw){
        if(raw==null)return "";
        String s=raw.trim();
        if(s.endsWith(".0")){
            try{s=String.valueOf((long)Double.parseDouble(s));}catch(Exception ignored){}
        }
        return s;
    }

    private String safeInfoTitle(InfoField info,int index){
        String s=info==null?"":info.title;
        if(s==null||s.trim().isEmpty())return "کادر اطلاعات "+(index+1);
        return s;
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

    private String normalizeHeaderText(String header){
        if (header == null) return "";
        return header.trim()
                .replace("ي","ی")
                .replace("ك","ک")
                .replace("ۀ","ه")
                .replace("ة","ه")
                .replace("‌"," ")
                .replaceAll("\\s+"," ")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Batch price-column detection must not depend only on the literal word «قیمت».
     * Real customer sheets sometimes use headers such as «مصرف کننده»، «پایه»،
     * «حجم متوسط» or «حجم بالا». Missing one of those headers previously caused
     * the corresponding designer card to be REMOVED from batch output.
     */
    private boolean isPriceColumn(String header){
        String h = normalizeHeaderText(header);
        if (h.isEmpty()) return false;

        if (h.contains("کد") || h.contains("بارکد") || h.contains("نام کالا") || h.contains("شرح کالا"))
            return false;

        return h.contains("قیمت")
                || h.contains("مصرف کننده") || h.contains("مصرف‌کننده")
                || h.equals("پایه") || h.contains("قیمت پایه")
                || h.contains("حجم متوسط") || h.contains("متوسط")
                || h.contains("حجم بالا") || h.contains("خرید حجم بالا") || h.contains("عمده")
                || h.contains("حجم کم") || h.contains("پلن") || h.contains("تخفیف")
                || h.contains("فروش") || h.contains("همکار");
    }

    private int priceHeaderScore(String fieldTitle, String header){
        String f = normalizeHeaderText(fieldTitle);
        String h = normalizeHeaderText(header);
        int score = 0;
        if (f.isEmpty() || h.isEmpty()) return score;

        if ((f.contains("مصرف") && h.contains("مصرف"))) score += 100;
        if ((f.contains("پایه") && h.contains("پایه"))) score += 100;
        if ((f.contains("متوسط") && h.contains("متوسط"))) score += 100;
        if ((f.contains("بالا") && h.contains("بالا"))) score += 100;
        if ((f.contains("کم") && h.contains("کم"))) score += 100;
        if (f.equals(h)) score += 200;
        if (h.contains(f) || f.contains(h)) score += 40;
        return score;
    }

    /** Resolve an Excel price column for a designer card without changing card count/order. */
    private String resolvePriceHeader(LabelField templateField, ArrayList<String> priceHeaders,
                                      HashSet<String> alreadyUsed, int fallbackIndex){
        String best = null;
        int bestScore = 0;
        for (String h : priceHeaders){
            if (alreadyUsed.contains(h)) continue;
            int sc = priceHeaderScore(templateField != null ? templateField.name : "", h);
            if (sc > bestScore){ bestScore = sc; best = h; }
        }
        if (best != null && bestScore > 0) return best;

        // No semantic match: preserve the Excel column order. Prefer the column at the
        // same designer-card index, then fall back to the next unused detected price column.
        if (fallbackIndex >= 0 && fallbackIndex < priceHeaders.size()) {
            String ordered = priceHeaders.get(fallbackIndex);
            if (!alreadyUsed.contains(ordered)) return ordered;
        }
        for (String h : priceHeaders) if (!alreadyUsed.contains(h)) return h;
        return null;
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
                        HashSet<String> usedPriceHeaders = new HashSet<>();

                        /*
                         * IMPORTANT: the Designer template owns the number/order of cards.
                         * Do NOT "continue" and delete a card merely because an Excel cell is blank
                         * or a header spelling differs. This is what made card #4 disappear in Batch.
                         */
                        if (!fields.isEmpty()){
                            for (int i=0;i<fields.size();i++){
                                LabelField template = fields.get(i);
                                String h = resolvePriceHeader(template,priceHeaders,usedPriceHeaders,i);
                                String raw = h != null ? row.get(h) : null;
                                if (h != null) usedPriceHeaders.add(h);

                                String value = (raw == null || raw.trim().isEmpty())
                                        ? ""
                                        : formatPrice(raw,convert);

                                LabelField f = new LabelField(template.name,value);
                                copyStyleOnly(template,f);
                                fs.add(f);
                            }
                        } else {
                            // Compatibility fallback for an old/empty template.
                            for (String h : priceHeaders){
                                String raw = row.get(h);
                                LabelField f = new LabelField(h.trim(),
                                        (raw == null || raw.trim().isEmpty()) ? "" : formatPrice(raw,convert));
                                fs.add(f);
                            }
                        }

                        layoutBatchFields(fs);
                        ArrayList<InfoField> batchInfo = buildInfoFieldsForRow(row);

                        Bitmap out = render(src,fs,batchInfo,cropAll);
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

    private ArrayList<InfoField> buildInfoFieldsForRow(LinkedHashMap<String,String> row){
        ArrayList<InfoField> result=new ArrayList<>();
        for(InfoField template:infoFields){
            InfoField f=copyInfoField(template);
            if(f.sourceHeader!=null&&!f.sourceHeader.trim().isEmpty()){
                String raw=row.get(f.sourceHeader);
                f.value=cleanInfoValue(raw);
                if(f.value.trim().isEmpty()) f.visible=false;
            } else {
                // Batch must never guess or export sample placeholder data.
                f.value="";
                f.visible=false;
            }
            result.add(f);
        }
        return result;
    }

    private InfoField copyInfoField(InfoField from){
        InfoField to=new InfoField(from.title,from.value);
        to.sourceHeader=from.sourceHeader;
        to.prefix=from.prefix;
        to.suffix=from.suffix;
        to.backgroundColor=from.backgroundColor;
        to.borderColor=from.borderColor;
        to.titleColor=from.titleColor;
        to.valueColor=from.valueColor;
        to.borderWidth=from.borderWidth;
        to.cornerRadius=from.cornerRadius;
        to.paddingHorizontal=from.paddingHorizontal;
        to.paddingVertical=from.paddingVertical;
        to.titleValueGap=from.titleValueGap;
        to.titleSize=from.titleSize;
        to.valueSize=from.valueSize;
        to.visible=from.visible;
        to.showTitle=from.showTitle;
        to.showValue=from.showValue;
        to.fullRow=from.fullRow;
        to.titleBold=from.titleBold;
        to.titleItalic=from.titleItalic;
        to.valueBold=from.valueBold;
        to.valueItalic=from.valueItalic;
        to.textAlign=from.textAlign;
        to.titleFont=from.titleFont;
        to.valueFont=from.valueFont;
        return to;
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
        if(d==null)return;
        d.labelWidthPct=settingLabelWidth; d.labelX=settingLabelX; d.labelY=settingLabelY;
        d.fieldGapPct=settingFieldGap; d.fieldGapPx=settingFieldGapPx; d.panelPaddingPx=settingPanelPaddingPx;
        d.outerTagRadius=settingOuterRadius; d.outerTagBorderWidth=settingOuterBorderWidth; d.outerTagColor=settingOuterColor; d.outerTagBorderColor=settingOuterBorderColor; d.autoHeight=settingAutoHeight; d.manualCardHeightPx=settingManualCardHeightPx;
        d.productX=settingProductX; d.productY=settingProductY; d.productW=settingProductW; d.productH=settingProductH; d.productZoom=settingProductZoom;
        d.cropEnabled=settingCropEnabled; d.cropLeft=settingCropLeft; d.cropTop=settingCropTop; d.cropRight=settingCropRight; d.cropBottom=settingCropBottom;
        d.backgroundMode=settingBackgroundMode; d.canvasBackground=settingBackgroundColor; d.gradientColor1=settingGradientColor1; d.gradientColor2=settingGradientColor2; d.gradientAngle=settingGradientAngle; d.backgroundAlpha=settingBackgroundAlpha; d.patternIndex=settingPatternIndex; d.setCustomBackgroundBitmap(customBackgroundBitmap);
        d.infoPanelEnabled=settingInfoEnabled; d.infoPositionMode=settingInfoPositionMode; d.infoLayoutMode=settingInfoLayoutMode; d.infoColumns=settingInfoColumns; d.infoWidthPct=settingInfoWidth; d.infoX=settingInfoX; d.infoY=settingInfoY; d.infoDistancePx=settingInfoDistancePx; d.infoGapPx=settingInfoGapPx; d.infoPaddingPx=settingInfoPaddingPx; d.infoOuterColor=settingInfoOuterColor; d.infoOuterBorderColor=settingInfoOuterBorderColor; d.infoOuterBorderWidth=settingInfoOuterBorderWidth; d.infoOuterRadius=settingInfoOuterRadius;
        d.setInfoFields(infoFields);
    }

    private void syncDesignerSettingsFromView(){
        if(designer==null)return;
        settingLabelWidth=designer.labelWidthPct; settingLabelX=designer.labelX; settingLabelY=designer.labelY; settingFieldGap=designer.fieldGapPct; settingFieldGapPx=designer.fieldGapPx; settingPanelPaddingPx=designer.panelPaddingPx;
        settingOuterRadius=designer.outerTagRadius; settingOuterBorderWidth=designer.outerTagBorderWidth; settingOuterColor=designer.outerTagColor; settingOuterBorderColor=designer.outerTagBorderColor; settingAutoHeight=designer.autoHeight; settingManualCardHeightPx=designer.manualCardHeightPx;
        settingProductX=designer.productX; settingProductY=designer.productY; settingProductW=designer.productW; settingProductH=designer.productH; settingProductZoom=designer.productZoom;
        settingCropEnabled=designer.cropEnabled; settingCropLeft=designer.cropLeft; settingCropTop=designer.cropTop; settingCropRight=designer.cropRight; settingCropBottom=designer.cropBottom;
        settingBackgroundMode=designer.backgroundMode; settingBackgroundColor=designer.canvasBackground; settingGradientColor1=designer.gradientColor1; settingGradientColor2=designer.gradientColor2; settingGradientAngle=designer.gradientAngle; settingBackgroundAlpha=designer.backgroundAlpha; settingPatternIndex=designer.patternIndex;
        settingInfoEnabled=designer.infoPanelEnabled; settingInfoPositionMode=designer.infoPositionMode; settingInfoLayoutMode=designer.infoLayoutMode; settingInfoColumns=designer.infoColumns; settingInfoWidth=designer.infoWidthPct; settingInfoX=designer.infoX; settingInfoY=designer.infoY; settingInfoDistancePx=designer.infoDistancePx; settingInfoGapPx=designer.infoGapPx; settingInfoPaddingPx=designer.infoPaddingPx; settingInfoOuterColor=designer.infoOuterColor; settingInfoOuterBorderColor=designer.infoOuterBorderColor; settingInfoOuterBorderWidth=designer.infoOuterBorderWidth; settingInfoOuterRadius=designer.infoOuterRadius;
    }

    private void saveDesignerSettings(){
        getSharedPreferences("javdan",MODE_PRIVATE).edit()
                .putFloat("labelWidth",settingLabelWidth).putFloat("labelX",settingLabelX).putFloat("labelY",settingLabelY)
                .putFloat("fieldGap",settingFieldGap).putFloat("fieldGapPx",settingFieldGapPx).putFloat("panelPaddingPx",settingPanelPaddingPx)
                .putInt("outerRadius",settingOuterRadius).putInt("outerBorderWidth",settingOuterBorderWidth).putInt("outerColor",settingOuterColor).putInt("outerBorderColor",settingOuterBorderColor).putBoolean("autoHeight",settingAutoHeight).putFloat("manualCardHeightPx",settingManualCardHeightPx)
                .putFloat("productX",settingProductX).putFloat("productY",settingProductY).putFloat("productW",settingProductW).putFloat("productH",settingProductH).putFloat("productZoom",settingProductZoom)
                .putBoolean("cropEnabled",settingCropEnabled).putFloat("cropLeft",settingCropLeft).putFloat("cropTop",settingCropTop).putFloat("cropRight",settingCropRight).putFloat("cropBottom",settingCropBottom)
                .putInt("backgroundMode",settingBackgroundMode).putInt("backgroundColor",settingBackgroundColor).putInt("gradientColor1",settingGradientColor1).putInt("gradientColor2",settingGradientColor2).putFloat("gradientAngle",settingGradientAngle).putInt("backgroundAlpha",settingBackgroundAlpha).putInt("patternIndex",settingPatternIndex).putString("customBackgroundUri",settingCustomBackgroundUri)
                .putBoolean("infoEnabled",settingInfoEnabled).putInt("infoPositionMode",settingInfoPositionMode).putInt("infoLayoutMode",settingInfoLayoutMode).putInt("infoColumns",settingInfoColumns).putFloat("infoWidth",settingInfoWidth).putFloat("infoX",settingInfoX).putFloat("infoY",settingInfoY).putFloat("infoDistancePx",settingInfoDistancePx).putFloat("infoGapPx",settingInfoGapPx).putFloat("infoPaddingPx",settingInfoPaddingPx).putInt("infoOuterColor",settingInfoOuterColor).putInt("infoOuterBorderColor",settingInfoOuterBorderColor).putInt("infoOuterBorderWidth",settingInfoOuterBorderWidth).putInt("infoOuterRadius",settingInfoOuterRadius)
                .apply();
    }

    private void loadDesignerSettings(){
        android.content.SharedPreferences p=getSharedPreferences("javdan",MODE_PRIVATE);
        settingLabelWidth=p.getFloat("labelWidth",0.28f); settingLabelX=p.getFloat("labelX",0.70f); settingLabelY=p.getFloat("labelY",0.12f);
        settingFieldGap=p.getFloat("fieldGap",0.006f); settingFieldGapPx=p.getFloat("fieldGapPx",6f); settingPanelPaddingPx=p.getFloat("panelPaddingPx",10f);
        settingOuterRadius=p.getInt("outerRadius",22); settingOuterBorderWidth=p.getInt("outerBorderWidth",3); settingOuterColor=p.getInt("outerColor",0xFF181818); settingOuterBorderColor=p.getInt("outerBorderColor",0xFF3A3A3A); settingAutoHeight=p.getBoolean("autoHeight",true); settingManualCardHeightPx=p.getFloat("manualCardHeightPx",130f);
        settingProductX=p.getFloat("productX",0.02f); settingProductY=p.getFloat("productY",0.08f); settingProductW=p.getFloat("productW",0.62f); settingProductH=p.getFloat("productH",0.84f); settingProductZoom=p.getFloat("productZoom",1f);
        settingCropEnabled=p.getBoolean("cropEnabled",false); settingCropLeft=p.getFloat("cropLeft",0f); settingCropTop=p.getFloat("cropTop",0f); settingCropRight=p.getFloat("cropRight",1f); settingCropBottom=p.getFloat("cropBottom",1f);
        settingBackgroundMode=p.getInt("backgroundMode",LabelDesignerView.BG_SOLID); settingBackgroundColor=p.getInt("backgroundColor",0xFFF2F2F2); settingGradientColor1=p.getInt("gradientColor1",0xFFFFFFFF); settingGradientColor2=p.getInt("gradientColor2",0xFFE8EEF8); settingGradientAngle=p.getFloat("gradientAngle",0f); settingBackgroundAlpha=p.getInt("backgroundAlpha",255); settingPatternIndex=p.getInt("patternIndex",0); settingCustomBackgroundUri=p.getString("customBackgroundUri","");
        settingInfoEnabled=p.getBoolean("infoEnabled",true); settingInfoPositionMode=p.getInt("infoPositionMode",0); settingInfoLayoutMode=p.getInt("infoLayoutMode",2); settingInfoColumns=p.getInt("infoColumns",2); settingInfoWidth=p.getFloat("infoWidth",0.48f); settingInfoX=p.getFloat("infoX",0.10f); settingInfoY=p.getFloat("infoY",0.76f); settingInfoDistance
