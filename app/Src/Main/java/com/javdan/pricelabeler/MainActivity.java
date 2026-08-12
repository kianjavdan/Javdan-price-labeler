package com.javdan.pricelabeler;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.provider.*;
import android.view.*;
import android.widget.*;

import org.json.*;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class MainActivity extends Activity {

    static final int PICK_EXCEL = 10;
    static final int PICK_IMAGE = 11;
    static final int PICK_FOLDER = 12;

    LinearLayout body, manualRows, excelInfo, fieldEditorContainer;
    Button tabData, tabDesigner, tabOutput;

    RadioButton modeExcel, modeManual;

    Uri excelUri, imageUri, folderUri;
    Bitmap currentBitmap;

    LabelDesignerView designer;

    ArrayList<LabelField> fields = new ArrayList<>();
    ArrayList<LinkedHashMap<String, String>> excelRows = new ArrayList<>();
    ArrayList<String> headers = new ArrayList<>();

    Spinner codeSpinner;

    TextView status, previewStatus;

    EditText labelWidth;

    CheckBox rialToToman, appendMode;

    int selectedField = -1;

    ArrayList<EditText> nameEdits = new ArrayList<>();
    ArrayList<EditText> valueEdits = new ArrayList<>();


    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().setStatusBarColor(Color.WHITE);

        loadTemplate();
        buildUi();
        showData();
    }


    private TextView tv(String s, int sp, boolean bold) {
        TextView t = new TextView(this);

        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(0xFF222222);

        if (bold) {
            t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }

        t.setPadding(8, 8, 8, 8);

        return t;
    }


    private Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        return b;
    }


    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }


    private void buildUi() {
        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 12);

        root.addView(tv("Javdan Price Labeler", 24, true));

        root.addView(
                tv(
                        "نسخه اندروید — Excel + ورود دستی قیمت + طراح Drag & Drop",
                        14,
                        false
                )
        );

        LinearLayout tabs = row();

        tabData = btn("Excel / دستی");
        tabDesigner = btn("طراح برچسب");
        tabOutput = btn("خروجی");

        tabs.addView(
                tabData,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        tabs.addView(
                tabDesigner,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        tabs.addView(
                tabOutput,
                new LinearLayout.LayoutParams(0, -2, 1)
        );

        root.addView(tabs);

        ScrollView sv = new ScrollView(this);

        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(4, 12, 4, 80);

        sv.addView(body);

        root.addView(
                sv,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        status = tv(
                "آفلاین • بدون آپلود • عکس اصلی تغییر نمی‌کند",
                12,
                false
        );

        root.addView(status);

        setContentView(root);

        tabData.setOnClickListener(v -> showData());
        tabDesigner.setOnClickListener(v -> showDesigner());
        tabOutput.setOnClickListener(v -> showOutput());
    }


    private void clear() {
        body.removeAllViews();
    }


    private void showData() {
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


    private void renderDataContent(boolean excel) {
        while (body.getChildCount() > 1) {
            body.removeViewAt(1);
        }

        if (excel) {

            Button pe = btn("انتخاب فایل Excel (.xlsx)");
            body.addView(pe);

            pe.setOnClickListener(
                    v -> pickFile(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            PICK_EXCEL
                    )
            );

            Button pf = btn("انتخاب پوشه عکس محصولات");
            body.addView(pf);

            pf.setOnClickListener(v -> pickFolder());

            body.addView(tv("ستون کد محصول:", 14, true));

            codeSpinner = new Spinner(this);
            body.addView(codeSpinner);

            rialToToman = new CheckBox(this);

            rialToToman.setText(
                    "تبدیل ریال به تومان (÷۱۰)"
            );

            rialToToman.setChecked(true);

            body.addView(rialToToman);

            excelInfo = new LinearLayout(this);
            excelInfo.setOrientation(LinearLayout.VERTICAL);

            body.addView(excelInfo);

            if (!headers.isEmpty()) {
                refreshExcelUi();
            }

            Button validate =
                    btn("بررسی تطبیق Excel و تصاویر");

            body.addView(validate);

            validate.setOnClickListener(
                    v -> validateBatch()
            );

        } else {

            body.addView(
                    tv("حالت دستی", 18, true)
            );

            Button pi =
                    btn("انتخاب / دراپ عکس از گالری یا Files");

            body.addView(pi);

            pi.setOnClickListener(
                    v -> pickFile(
                            "image/*",
                            PICK_IMAGE
                    )
            );

            rialToToman =
                    new CheckBox(this);

            rialToToman.setText(
                    "قیمت‌های واردشده ریال هستند؛ تبدیل به تومان ÷۱۰"
            );

            body.addView(rialToToman);

            body.addView(
                    tv(
                            "نام و قیمت هر ردیف را خودت می‌توانی تغییر بدهی:",
                            14,
                            false
                    )
            );

            manualRows = new LinearLayout(this);
            manualRows.setOrientation(
                    LinearLayout.VERTICAL
            );

            body.addView(manualRows);

            if (fields.isEmpty()) {
                makeDefaults();
            }

            rebuildManualRows();

            Button add =
                    btn("+ افزودن قیمت جدید");

            body.addView(add);

            add.setOnClickListener(v -> {
                syncManualRows();

                fields.add(
                        new LabelField(
                                "قیمت جدید",
                                ""
                        )
                );

                relayoutFields();
                rebuildManualRows();
            });

            Button go =
                    btn("ذخیره قیمت‌ها و رفتن به طراح");

            body.addView(go);

            go.setOnClickListener(v -> {
                syncManualRows();
                showDesigner();
            });
        }
    }


    private void makeDefaults() {
        fields.add(
                new LabelField(
                        "قیمت مصرف کننده",
                        ""
                )
        );

        fields.add(
                new LabelField(
                        "قیمت پایه",
                        ""
                )
        );

        fields.add(
                new LabelField(
                        "قیمت حجم متوسط",
                        ""
                )
        );

        fields.add(
                new LabelField(
                        "قیمت حجم بالا",
                        ""
                )
        );

        relayoutFields();

        fields.get(0).strike = true;
    }


    private void rebuildManualRows() {
        manualRows.removeAllViews();

        nameEdits.clear();
        valueEdits.clear();

        for (int i = 0; i < fields.size(); i++) {

            LabelField f = fields.get(i);

            LinearLayout r = row();

            EditText n = new EditText(this);
            n.setText(f.name);
            n.setHint("نام قیمت");

            EditText v = new EditText(this);
            v.setText(f.value);
            v.setHint("قیمت");
            v.setInputType(2);

            Button del = btn("×");

            final int idx = i;

            del.setOnClickListener(x -> {
                syncManualRows();

                if (fields.size() > 1) {
                    fields.remove(idx);
                }

                relayoutFields();
                rebuildManualRows();
            });

            r.addView(
                    n,
                    new LinearLayout.LayoutParams(
                            0,
                            -2,
                            1.4f
                    )
            );

            r.addView(
                    v,
                    new LinearLayout.LayoutParams(
                            0,
                            -2,
                            1
                    )
            );

            r.addView(
                    del,
                    new LinearLayout.LayoutParams(
                            -2,
                            -2
                    )
            );

            manualRows.addView(r);

            nameEdits.add(n);
            valueEdits.add(v);
        }
    }


    private void syncManualRows() {
        for (
                int i = 0;
                i < Math.min(
                        fields.size(),
                        nameEdits.size()
                );
                i++
        ) {

            fields.get(i).name =
                    nameEdits
                            .get(i)
                            .getText()
                            .toString()
                            .trim();

            String raw =
                    valueEdits
                            .get(i)
                            .getText()
                            .toString()
                            .replace(",", "")
                            .trim();

            fields.get(i).value =
                    formatPrice(
                            raw,
                            rialToToman != null
                                    && rialToToman.isChecked()
                    );
        }

        saveTemplate();
    }


    private String formatPrice(
            String raw,
            boolean rial
    ) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }

        try {
            String clean =
                    raw.replace(",", "")
                            .replace("٬", "")
                            .trim();

            double d =
                    Double.parseDouble(clean);

            if (rial) {
                d /= 10.0;
            }

            long n =
                    Math.round(d);

            return new DecimalFormat(
                    "#,###"
            ).format(n);

        } catch (Exception e) {
            return raw;
        }
    }


    private void relayoutFields() {
        int n =
                Math.max(
                        1,
                        fields.size()
                );

        float top = 0.07f;
        float bottom = 0.93f;

        float available =
                bottom - top;

        float gap = 0.018f;

        float h =
                (available - gap * (n - 1))
                        / n;

        h = Math.min(
                0.21f,
                h
        );

        for (int i = 0; i < fields.size(); i++) {

            LabelField f =
                    fields.get(i);

            f.x = 0.055f;
            f.w = 0.89f;
            f.h = h;

            f.y =
                    top + i * (h + gap);

            if (i == 0) {
                f.strike = true;
            }

            /*
             * اندازه‌های جمع‌وجورتر برای جلوگیری از تداخل
             */
            f.titleSize = 0.22f;
            f.priceSize = 0.34f;
        }
    }


    private void showDesigner() {
        if (
                manualRows != null
                        && manualRows.getParent() != null
        ) {
            syncManualRows();
        }

        clear();

        body.addView(
                tv(
                        "طراح Drag & Drop",
                        18,
                        true
                )
        );

        body.addView(
                tv(
                        "هر کادر قیمت را با انگشت بگیر و جابه‌جا کن.",
                        13,
                        false
                )
        );

        designer =
                new LabelDesignerView(this);

        designer.setFields(fields);
        designer.setProductBitmap(currentBitmap);

        body.addView(
                designer,
                new LinearLayout.LayoutParams(
                        -1,
                        900
                )
        );

        designer.setListener(
                new LabelDesignerView.Listener() {

                    public void onFieldSelected(int i) {
                        selectedField = i;
                        showFieldEditor();
                    }

                    public void onChanged() {
                        saveTemplate();
                    }
                }
        );

        LinearLayout opts = row();

        appendMode =
                new CheckBox(this);

        appendMode.setText(
                "کادر بیرون عکس"
        );

        appendMode.setChecked(true);

        opts.addView(appendMode);

        labelWidth =
                new EditText(this);

        labelWidth.setHint(
                "عرض %"
        );

        labelWidth.setText("36");

        labelWidth.setInputType(2);

        opts.addView(labelWidth);

        body.addView(opts);

        Button add =
                btn("+ افزودن فیلد قیمت");

        body.addView(add);

        add.setOnClickListener(v -> {
            fields.add(
                    new LabelField(
                            "قیمت جدید",
                            ""
                    )
            );

            relayoutFields();

            designer.setFields(fields);

            saveTemplate();
        });

        Button save =
                btn("ذخیره قالب");

        body.addView(save);

        save.setOnClickListener(v -> {
            saveTemplate();

            Toast.makeText(
                    this,
                    "قالب ذخیره شد",
                    Toast.LENGTH_SHORT
            ).show();
        });

        Button sample =
                btn("انتخاب عکس نمونه");

        body.addView(sample);

        sample.setOnClickListener(
                v -> pickFile(
                        "image/*",
                        PICK_IMAGE
                )
        );

        body.addView(
                tv(
                        "تنظیمات فیلد انتخابی:",
                        14,
                        true
                )
        );

        fieldEditorContainer =
                new LinearLayout(this);

        fieldEditorContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        body.addView(fieldEditorContainer);
    }


    private void showFieldEditor() {
        if (
                fieldEditorContainer == null
                        || selectedField < 0
                        || selectedField >= fields.size()
        ) {
            return;
        }

        fieldEditorContainer.removeAllViews();

        LabelField f =
                fields.get(selectedField);

        EditText name =
                new EditText(this);

        name.setText(f.name);

        fieldEditorContainer.addView(name);

        EditText val =
                new EditText(this);

        val.setText(f.value);
        val.setHint("قیمت / مقدار");

        fieldEditorContainer.addView(val);

        CheckBox strike =
                new CheckBox(this);

        strike.setText(
                "خط‌خورده کردن قیمت"
        );

        strike.setChecked(
                f.strike
        );

        fieldEditorContainer.addView(
                strike
        );

        Button apply =
                btn("اعمال");

        fieldEditorContainer.addView(
                apply
        );

        apply.setOnClickListener(v -> {

            f.name =
                    name.getText()
                            .toString();

            f.value =
                    val.getText()
                            .toString();

            f.strike =
                    strike.isChecked();

            designer.invalidate();

            saveTemplate();
        });

        Button del =
                btn("حذف این فیلد");

        fieldEditorContainer.addView(del);

        del.setOnClickListener(v -> {

            fields.remove(selectedField);

            selectedField = -1;

            relayoutFields();

            designer.setFields(fields);

            fieldEditorContainer.removeAllViews();

            saveTemplate();
        });
    }


    private void showOutput() {
        if (
                manualRows != null
                        && manualRows.getParent() != null
        ) {
            syncManualRows();
        }

        clear();

        body.addView(
                tv(
                        "خروجی",
                        18,
                        true
                )
        );

        Button preview =
                btn(
                        "پیش‌نمایش خروجی نهایی"
                );

        body.addView(preview);

        preview.setOnClickListener(
                v -> makePreview()
        );

        previewStatus =
                tv(
                        "برای خروجی دستی ابتدا عکس را انتخاب کن.",
                        13,
                        false
                );

        body.addView(previewStatus);

        Button save =
                btn(
                        "ذخیره عکس فعلی در گالری"
                );

        body.addView(save);

        save.setOnClickListener(
                v -> saveCurrent()
        );

        Button batch =
                btn(
                        "ساخت گروهی از Excel + پوشه عکس‌ها"
                );

        body.addView(batch);

        batch.setOnClickListener(
                v -> runBatch()
        );
    }


    private void makePreview() {
        if (currentBitmap == null) {
            Toast.makeText(
                    this,
                    "عکس انتخاب نشده",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        LabelDesignerView renderer =
                new LabelDesignerView(this);

        renderer.setFields(fields);

        boolean append =
                appendMode == null
                        || appendMode.isChecked();

        float width = 0.36f;

        try {
            width =
                    Float.parseFloat(
                            labelWidth == null
                                    ? "36"
                                    : labelWidth
                                    .getText()
                                    .toString()
                    ) / 100f;
        } catch (Exception ignored) {
        }

        Bitmap out =
                renderer.renderFinal(
                        currentBitmap,
                        Color.WHITE,
                        0xFFD8D8D8,
                        append,
                        width
                );

        ImageView iv =
                new ImageView(this);

        iv.setAdjustViewBounds(true);
        iv.setImageBitmap(out);

        body.addView(
                iv,
                Math.min(
                        body.getWidth() > 0
                                ? body.getWidth()
                                : 1000,
                        1000
                ),
                -2
        );

        previewStatus.setText(
                "پیش‌نمایش ساخته شد."
        );
    }


    private Bitmap render(
            Bitmap src,
            ArrayList<LabelField> useFields
    ) {
        LabelDesignerView r =
                new LabelDesignerView(this);

        r.setFields(useFields);

        boolean append =
                appendMode == null
                        || appendMode.isChecked();

        float width = 0.36f;

        try {
            width =
                    Float.parseFloat(
                            labelWidth == null
                                    ? "36"
                                    : labelWidth
                                    .getText()
                                    .toString()
                    ) / 100f;
        } catch (Exception ignored) {
        }

        return r.renderFinal(
                src,
                Color.WHITE,
                0xFFD8D8D8,
                append,
                width
        );
    }


    private void saveCurrent() {
        if (currentBitmap == null) {
            Toast.makeText(
                    this,
                    "عکس انتخاب نشده",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Bitmap out =
                render(
                        currentBitmap,
                        fields
                );

        String name =
                "Javdan_"
                        + System.currentTimeMillis()
                        + ".jpg";

        try {
            saveToGallery(
                    out,
                    name
            );

            Toast.makeText(
                    this,
                    "در Pictures/JavdanPriceLabeler ذخیره شد",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    private void saveToGallery(
            Bitmap bmp,
            String name
    ) throws Exception {

        ContentValues v =
                new ContentValues();

        v.put(
                MediaStore.Images.Media.DISPLAY_NAME,
                name
        );

        v.put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
        );

        v.put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "Pictures/JavdanPriceLabeler"
        );

        Uri u =
                getContentResolver()
                        .insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                v
                        );

        if (u == null) {
            throw new IOException(
                    "ساخت فایل خروجی ناموفق بود"
            );
        }

        try (
                OutputStream o =
                        getContentResolver()
                                .openOutputStream(u)
        ) {

            if (o == null) {
                throw new IOException(
                        "فایل خروجی باز نشد"
                );
            }

            bmp.compress(
                    Bitmap.CompressFormat.JPEG,
                    94,
                    o
            );
        }
    }


    private void pickFile(
            String type,
            int request
    ) {
        Intent i =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        i.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        i.setType(type);

        i.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        startActivityForResult(
                i,
                request
        );
    }


    private void pickFolder() {
        Intent i =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT_TREE
                );

        i.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                i,
                PICK_FOLDER
        );
    }


    @Override
    protected void onActivityResult(
            int req,
            int result,
            Intent data
    ) {
        super.onActivityResult(
                req,
                result,
                data
        );

        if (
                result != RESULT_OK
                        || data == null
        ) {
            return;
        }

        Uri u =
                data.getData();

        if (u == null) {
            return;
        }

        try {
            getContentResolver()
                    .takePersistableUriPermission(
                            u,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
        } catch (Exception ignored) {
        }

        try {

            if (req == PICK_IMAGE) {

                imageUri = u;

                try (
                        InputStream in =
                                getContentResolver()
                                        .openInputStream(u)
                ) {

                    currentBitmap =
                            BitmapFactory.decodeStream(in);
                }

                status.setText(
                        "عکس انتخاب شد"
                );

                if (designer != null) {
                    designer.setProductBitmap(
                            currentBitmap
                    );
                }

            } else if (req == PICK_EXCEL) {

                excelUri = u;

                status.setText(
                        "در حال خواندن Excel..."
                );

                excelRows =
                        new XlsxReader(this)
                                .readFirstSheet(u);

                headers.clear();

                if (!excelRows.isEmpty()) {
                    headers.addAll(
                            excelRows.get(0)
                                    .keySet()
                    );
                }

                refreshExcelUi();

                status.setText(
                        excelRows.size()
                                + " ردیف Excel خوانده شد"
                );

            } else if (req == PICK_FOLDER) {

                folderUri = u;

                status.setText(
                        "پوشه تصاویر انتخاب شد"
                );

                Toast.makeText(
                        this,
                        "پوشه عکس محصولات انتخاب شد",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا: "
                            + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    private void refreshExcelUi() {
        if (codeSpinner != null) {

            ArrayAdapter<String> a =
                    new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            headers
                    );

            codeSpinner.setAdapter(a);
        }

        if (excelInfo != null) {

            excelInfo.removeAllViews();

            excelInfo.addView(
                    tv(
                            "ردیف‌ها: "
                                    + excelRows.size()
                                    + " | ستون‌ها: "
                                    + headers.size(),
                            13,
                            false
                    )
            );
        }
    }


    private void validateBatch() {
        if (
                excelUri == null
                        || folderUri == null
                        || excelRows.isEmpty()
        ) {
            Toast.makeText(
                    this,
                    "Excel و پوشه عکس‌ها را انتخاب کن",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (headers.isEmpty()) {
            Toast.makeText(
                    this,
                    "ستون‌های Excel شناسایی نشده‌اند",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        final int selectedPos =
                codeSpinner != null
                        ? codeSpinner.getSelectedItemPosition()
                        : 0;

        if (
                selectedPos < 0
                        || selectedPos >= headers.size()
        ) {
            Toast.makeText(
                    this,
                    "ستون کد محصول را انتخاب کن",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        final String codeHeader =
                headers.get(selectedPos);

        status.setText(
                "در حال بررسی تطبیق Excel و تصاویر..."
        );

        new Thread(() -> {

            try {

                HashMap<String, Uri> images =
                        listTreeImages(folderUri);

                int excelCodeCount = 0;
                int matched = 0;
                int missingImages = 0;

                ArrayList<String> missingCodes =
                        new ArrayList<>();

                HashSet<String> excelCodes =
                        new HashSet<>();

                for (
                        LinkedHashMap<String, String> row
                                : excelRows
                ) {

                    String code =
                            normalizeCode(
                                    row.get(codeHeader)
                            );

                    if (code.isEmpty()) {
                        continue;
                    }

                    excelCodeCount++;
                    excelCodes.add(code);

                    if (images.containsKey(code)) {
                        matched++;
                    } else {
                        missingImages++;

                        if (missingCodes.size() < 20) {
                            missingCodes.add(code);
                        }
                    }
                }

                int extraImages = 0;

                ArrayList<String> extraImageCodes =
                        new ArrayList<>();

                for (String imageCode : images.keySet()) {

                    if (!excelCodes.contains(imageCode)) {

                        extraImages++;

                        if (
                                extraImageCodes.size() < 20
                        ) {
                            extraImageCodes.add(
                                    imageCode
                            );
                        }
                    }
                }

                final int fExcelCodeCount =
                        excelCodeCount;

                final int fMatched =
                        matched;

                final int fMissingImages =
                        missingImages;

                final int fExtraImages =
                        extraImages;

                final int totalImages =
                        images.size();

                final String missingText =
                        missingCodes.isEmpty()
                                ? "-"
                                : android.text.TextUtils.join(
                                        "، ",
                                        missingCodes
                                );

                final String extraText =
                        extraImageCodes.isEmpty()
                                ? "-"
                                : android.text.TextUtils.join(
                                        "، ",
                                        extraImageCodes
                                );

                runOnUiThread(() -> {

                    status.setText(
                            "تطبیق انجام شد — موفق: "
                                    + fMatched
                                    + " | بدون عکس: "
                                    + fMissingImages
                    );

                    if (excelInfo != null) {

                        excelInfo.removeAllViews();

                        excelInfo.addView(
                                tv(
                                        "ردیف‌های Excel: "
                                                + excelRows.size()
                                                + " | ستون‌ها: "
                                                + headers.size(),
                                        13,
                                        false
                                )
                        );

                        excelInfo.addView(
                                tv(
                                        "کدهای محصول: "
                                                + fExcelCodeCount,
                                        14,
                                        true
                                )
                        );

                        excelInfo.addView(
                                tv(
                                        "تعداد عکس‌های پوشه: "
                                                + totalImages,
                                        14,
                                        false
                                )
                        );

                        TextView okView =
                                tv(
                                        "✓ تطبیق موفق: "
                                                + fMatched,
                                        16,
                                        true
                                );

                        okView.setTextColor(
                                0xFF168A3B
                        );

                        excelInfo.addView(
                                okView
                        );

                        TextView missingView =
                                tv(
                                        "✕ کدهای بدون عکس: "
                                                + fMissingImages,
                                        15,
                                        true
                                );

                        missingView.setTextColor(
                                0xFFC62828
                        );

                        excelInfo.addView(
                                missingView
                        );

                        if (fMissingImages > 0) {

                            excelInfo.addView(
                                    tv(
                                            "نمونه کدهای بدون عکس:\n"
                                                    + missingText,
                                            12,
                                            false
                                    )
                            );
                        }

                        TextView extraView =
                                tv(
                                        "عکس‌های بدون کد در Excel: "
                                                + fExtraImages,
                                        14,
                                        true
                                );

                        extraView.setTextColor(
                                0xFFE28A00
                        );

                        excelInfo.addView(
                                extraView
                        );

                        if (fExtraImages > 0) {

                            excelInfo.addView(
                                    tv(
                                            "نمونه عکس‌های اضافی:\n"
                                                    + extraText,
                                            12,
                                            false
                                    )
                            );
                        }

                        if (fMatched > 0) {

                            Button goOutput =
                                    btn(
                                            "ادامه و ساخت خروجی گروهی"
                                    );

                            excelInfo.addView(
                                    goOutput
                            );

                            goOutput.setOnClickListener(
                                    v -> showOutput()
                            );
                        }
                    }

                    Toast.makeText(
                            this,
                            "بررسی تمام شد — تطبیق: "
                                    + fMatched
                                    + " | بدون عکس: "
                                    + fMissingImages
                                    + " | عکس اضافی: "
                                    + fExtraImages,
                            Toast.LENGTH_LONG
                    ).show();
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    status.setText(
                            "خطا در بررسی تطبیق"
                    );

                    Toast.makeText(
                            this,
                            "خطا: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
            }

        }).start();
    }


    /*
     * فقط ستون‌هایی که عنوانشان شامل «قیمت» است
     * وارد لیبل می‌شوند.
     */
    private boolean isPriceColumn(
            String header
    ) {
        if (header == null) {
            return false;
        }

        String h =
                header.trim()
                        .replace("ي", "ی")
                        .replace("ك", "ک")
                        .toLowerCase(Locale.ROOT);

        return h.contains("قیمت");
    }


    private void runBatch() {
        if (
                excelRows.isEmpty()
                        || folderUri == null
        ) {
            Toast.makeText(
                    this,
                    "Excel و پوشه عکس‌ها را انتخاب کن",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (headers.isEmpty()) {
            Toast.makeText(
                    this,
                    "ستون‌های Excel پیدا نشد",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Toast.makeText(
                this,
                "ساخت خروجی گروهی شروع شد...",
                Toast.LENGTH_SHORT
        ).show();

        status.setText(
                "در حال ساخت خروجی گروهی..."
        );

        final boolean convert =
                rialToToman == null
                        || rialToToman.isChecked();

        new Thread(() -> {

            int ok = 0;
            int missing = 0;
            int errors = 0;

            try {

                int selectedPos =
                        codeSpinner != null
                                ? codeSpinner.getSelectedItemPosition()
                                : 0;

                if (
                        selectedPos < 0
                                || selectedPos >= headers.size()
                ) {
                    selectedPos = 0;
                }

                String codeHeader =
                        headers.get(selectedPos);

                ArrayList<String> priceHeaders =
                        new ArrayList<>();

                for (String h : headers) {

                    if (
                            !h.equals(codeHeader)
                                    && isPriceColumn(h)
                    ) {
                        priceHeaders.add(h);
                    }
                }

                if (priceHeaders.isEmpty()) {

                    throw new IOException(
                            "هیچ ستون قیمتی در Excel پیدا نشد."
                    );
                }

                HashMap<String, Uri> images =
                        listTreeImages(folderUri);

                for (
                        LinkedHashMap<String, String> row
                                : excelRows
                ) {

                    String code =
                            normalizeCode(
                                    row.get(codeHeader)
                            );

                    if (code.isEmpty()) {
                        continue;
                    }

                    Uri img =
                            images.get(code);

                    if (img == null) {
                        missing++;
                        continue;
                    }

                    try {

                        Bitmap src;

                        try (
                                InputStream in =
                                        getContentResolver()
                                                .openInputStream(img)
                        ) {
                            src =
                                    BitmapFactory.decodeStream(in);
                        }

                        if (src == null) {
                            errors++;
                            continue;
                        }

                        ArrayList<LabelField> fs =
                                new ArrayList<>();

                        for (
                                int i = 0;
                                i < priceHeaders.size();
                                i++
                        ) {

                            String h =
                                    priceHeaders.get(i);

                            String raw =
                                    row.get(h);

                            if (
                                    raw == null
                                            || raw.trim().isEmpty()
                            ) {
                                continue;
                            }

                            LabelField f =
                                    new LabelField(
                                            h.trim(),
                                            formatPrice(
                                                    raw,
                                                    convert
                                            )
                                    );

                            if (
                                    i < fields.size()
                            ) {

                                LabelField t =
                                        fields.get(i);

                                f.titleColor =
                                        t.titleColor;

                                f.priceColor =
                                        t.priceColor;

                                f.titleSize =
                                        t.titleSize;

                                f.priceSize =
                                        t.priceSize;
                            }

                            f.strike =
                                    i == 0;

                            fs.add(f);
                        }

                        /*
                         * مهم:
                         * برای تعداد واقعی قیمت‌های همین محصول
                         * چیدمان مرتب دوباره ساخته می‌شود.
                         */
                        layoutBatchFields(fs);

                        Bitmap out =
                                render(src, fs);

                        saveToGallery(
                                out,
                                code + ".jpg"
                        );

                        ok++;

                        src.recycle();

                        if (
                                out != src
                                        && !out.isRecycled()
                        ) {
                            out.recycle();
                        }

                    } catch (Exception e) {
                        errors++;
                    }
                }

            } catch (Exception e) {
                errors++;
            }

            int fok = ok;
            int fm = missing;
            int fe = errors;

            runOnUiThread(() -> {

                status.setText(
                        "خروجی گروهی تمام شد — موفق: "
                                + fok
                                + " | بدون عکس: "
                                + fm
                                + " | خطا: "
                                + fe
                );

                Toast.makeText(
                        this,
                        "تمام شد — موفق: "
                                + fok
                                + " | بدون عکس: "
                                + fm
                                + " | خطا: "
                                + fe,
                        Toast.LENGTH_LONG
                ).show();
            });

        }).start();
    }


    /*
     * چیدمان مرتب برای خروجی گروهی
     */
    private void layoutBatchFields(
            ArrayList<LabelField> fs
    ) {
        int n =
                Math.max(
                        1,
                        fs.size()
                );

        float top = 0.08f;
        float bottom = 0.92f;

        float available =
                bottom - top;

        float gap = 0.020f;

        float h =
                (available - gap * (n - 1))
                        / n;

        h = Math.min(
                0.20f,
                h
        );

        for (
                int i = 0;
                i < fs.size();
                i++
        ) {

            LabelField f =
                    fs.get(i);

            f.x = 0.055f;
            f.w = 0.89f;

            f.h = h;

            f.y =
                    top + i * (h + gap);

            f.strike =
                    i == 0;

            /*
             * اندازه متن برای هر ردیف
             */
            if (n <= 4) {
                f.titleSize = 0.22f;
                f.priceSize = 0.35f;
            } else if (n <= 6) {
                f.titleSize = 0.19f;
                f.priceSize = 0.29f;
            } else {
                f.titleSize = 0.16f;
                f.priceSize = 0.24f;
            }
        }
    }


    private HashMap<String, Uri> listTreeImages(
            Uri tree
    ) throws Exception {

        HashMap<String, Uri> map =
                new HashMap<>();

        String docId =
                DocumentsContract
                        .getTreeDocumentId(tree);

        Uri children =
                DocumentsContract
                        .buildChildDocumentsUriUsingTree(
                                tree,
                                docId
                        );

        String[] cols = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };

        try (
                Cursor c =
                        getContentResolver()
                                .query(
                                        children,
                                        cols,
                                        null,
                                        null,
                                        null
                                )
        ) {

            if (c != null) {

                while (c.moveToNext()) {

                    String id =
                            c.getString(0);

                    String name =
                            c.getString(1);

                    String mime =
                            c.getString(2);

                    if (
                            mime != null
                                    && mime.startsWith("image/")
                    ) {

                        String stem =
                                name == null
                                        ? ""
                                        : name;

                        int dot =
                                stem.lastIndexOf('.');

                        if (dot > 0) {

                            stem =
                                    stem.substring(
                                            0,
                                            dot
                                    );
                        }

                        Uri child =
                                DocumentsContract
                                        .buildDocumentUriUsingTree(
                                                tree,
                                                id
                                        );

                        map.put(
                                normalizeCode(stem),
                                child
                        );
                    }
                }
            }
        }

        return map;
    }


    private String normalizeCode(
            String s
    ) {
        if (s == null) {
            return "";
        }

        s = s.trim();

        if (s.endsWith(".0")) {
            s =
                    s.substring(
                            0,
                            s.length() - 2
                    );
        }

        return s;
    }


    private void saveTemplate() {
        try {

            JSONArray a =
                    new JSONArray();

            for (LabelField f : fields) {
                a.put(f.toJson());
            }

            getSharedPreferences(
                    "javdan",
                    MODE_PRIVATE
            )
                    .edit()
                    .putString(
                            "fields",
                            a.toString()
                    )
                    .apply();

        } catch (Exception ignored) {
        }
    }


    private void loadTemplate() {
        fields.clear();

        String s =
                getSharedPreferences(
                        "javdan",
                        MODE_PRIVATE
                )
                        .getString(
                                "fields",
                                ""
                        );

        if (!s.isEmpty()) {

            try {

                JSONArray a =
                        new JSONArray(s);

                for (
                        int i = 0;
                        i < a.length();
                        i++
                ) {

                    fields.add(
                            LabelField.fromJson(
                                    a.getJSONObject(i)
                            )
                    );
                }

            } catch (Exception ignored) {
            }
        }

        if (fields.isEmpty()) {
            makeDefaults();
        }
    }
}
