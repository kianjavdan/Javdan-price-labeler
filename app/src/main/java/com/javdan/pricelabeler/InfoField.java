package com.javdan.pricelabeler;

import android.graphics.Typeface;
import org.json.JSONObject;

/**
 * One card inside the Product Information panel.
 * Data can be typed manually or mapped to an exact Excel header.
 */
public class InfoField {
    public String title = "";
    public String value = "";
    public String sourceHeader = "";
    public String prefix = "";
    public String suffix = "";

    public int backgroundColor = 0xFFF8FFF8;
    public int borderColor = 0xFF64B56A;
    public int titleColor = 0xFF333333;
    public int valueColor = 0xFF111111;

    public int borderWidth = 2;
    public int cornerRadius = 14;
    public int paddingHorizontal = 10;
    public int paddingVertical = 8;
    public int titleValueGap = 4;

    public int titleSize = 14;
    public int valueSize = 19;

    public boolean visible = true;
    public boolean showTitle = true;
    public boolean showValue = true;
    public boolean fullRow = false;

    public boolean titleBold = false;
    public boolean titleItalic = false;
    public boolean valueBold = true;
    public boolean valueItalic = false;

    // 0 = right, 1 = center, 2 = left — same convention as LabelField.
    public int textAlign = 1;

    public String titleFont = "DEFAULT";
    public String valueFont = "DEFAULT";

    public InfoField() {}

    public InfoField(String title, String value) {
        this.title = title == null ? "" : title;
        this.value = value == null ? "" : value;
    }

    public Typeface getTitleTypeface() {
        return makeTypeface(titleFont, titleBold, titleItalic);
    }

    public Typeface getValueTypeface() {
        return makeTypeface(valueFont, valueBold, valueItalic);
    }

    private static Typeface makeTypeface(String fontName, boolean bold, boolean italic) {
        int style;
        if (bold && italic) style = Typeface.BOLD_ITALIC;
        else if (bold) style = Typeface.BOLD;
        else if (italic) style = Typeface.ITALIC;
        else style = Typeface.NORMAL;

        String family = "sans-serif";
        if (fontName != null) {
            String s = fontName.trim().toLowerCase();
            if (s.contains("serif") && !s.contains("sans")) family = "serif";
            else if (s.contains("mono")) family = "monospace";
            else if (s.contains("sans")) family = "sans-serif";
        }
        return Typeface.create(family, style);
    }

    public String formattedValue() {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) return "";
        String p = prefix == null ? "" : prefix.trim();
        String s = suffix == null ? "" : suffix.trim();
        StringBuilder b = new StringBuilder();
        if (!p.isEmpty()) b.append(p).append(' ');
        b.append(v);
        if (!s.isEmpty()) b.append(' ').append(s);
        return b.toString();
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("title", title);
            o.put("value", value);
            o.put("sourceHeader", sourceHeader);
            o.put("prefix", prefix);
            o.put("suffix", suffix);

            o.put("backgroundColor", backgroundColor);
            o.put("borderColor", borderColor);
            o.put("titleColor", titleColor);
            o.put("valueColor", valueColor);

            o.put("borderWidth", borderWidth);
            o.put("cornerRadius", cornerRadius);
            o.put("paddingHorizontal", paddingHorizontal);
            o.put("paddingVertical", paddingVertical);
            o.put("titleValueGap", titleValueGap);

            o.put("titleSize", titleSize);
            o.put("valueSize", valueSize);

            o.put("visible", visible);
            o.put("showTitle", showTitle);
            o.put("showValue", showValue);
            o.put("fullRow", fullRow);

            o.put("titleBold", titleBold);
            o.put("titleItalic", titleItalic);
            o.put("valueBold", valueBold);
            o.put("valueItalic", valueItalic);

            o.put("textAlign", textAlign);
            o.put("titleFont", titleFont);
            o.put("valueFont", valueFont);
        } catch (Exception ignored) {}
        return o;
    }

    public static InfoField fromJson(JSONObject o) {
        InfoField f = new InfoField();
        if (o == null) return f;

        f.title = o.optString("title", f.title);
        f.value = o.optString("value", f.value);
        f.sourceHeader = o.optString("sourceHeader", f.sourceHeader);
        f.prefix = o.optString("prefix", f.prefix);
        f.suffix = o.optString("suffix", f.suffix);

        f.backgroundColor = o.optInt("backgroundColor", f.backgroundColor);
        f.borderColor = o.optInt("borderColor", f.borderColor);
        f.titleColor = o.optInt("titleColor", f.titleColor);
        f.valueColor = o.optInt("valueColor", f.valueColor);

        f.borderWidth = o.optInt("borderWidth", f.borderWidth);
        f.cornerRadius = o.optInt("cornerRadius", f.cornerRadius);
        f.paddingHorizontal = o.optInt("paddingHorizontal", f.paddingHorizontal);
        f.paddingVertical = o.optInt("paddingVertical", f.paddingVertical);
        f.titleValueGap = o.optInt("titleValueGap", f.titleValueGap);

        f.titleSize = o.optInt("titleSize", f.titleSize);
        f.valueSize = o.optInt("valueSize", f.valueSize);

        f.visible = o.optBoolean("visible", f.visible);
        f.showTitle = o.optBoolean("showTitle", f.showTitle);
        f.showValue = o.optBoolean("showValue", f.showValue);
        f.fullRow = o.optBoolean("fullRow", f.fullRow);

        f.titleBold = o.optBoolean("titleBold", f.titleBold);
        f.titleItalic = o.optBoolean("titleItalic", f.titleItalic);
        f.valueBold = o.optBoolean("valueBold", f.valueBold);
        f.valueItalic = o.optBoolean("valueItalic", f.valueItalic);

        f.textAlign = o.optInt("textAlign", f.textAlign);
        f.titleFont = o.optString("titleFont", f.titleFont);
        f.valueFont = o.optString("valueFont", f.valueFont);
        return f;
    }
}
