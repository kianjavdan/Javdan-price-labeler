package com.javdan.pricelabeler;

import android.graphics.Typeface;
import org.json.JSONObject;

public class LabelField {
    public String name;
    public String value;

    public int backgroundColor = 0xFFFFFFFF;
    public int borderColor = 0xFF222222;
    public int titleColor = 0xFF111111;
    public int priceColor = 0xFF111111;
    public int tomanColor = 0xFF111111;

    public int borderWidth = 2;
    public int cornerRadius = 18;
    public int paddingHorizontal = 12;
    public int paddingVertical = 10;
    public int titlePriceGap = 4;

    public int titleSize = 22;
    public int priceSize = 42;
    public int tomanSize = 17;

    public boolean visible = true;
    public boolean showTitle = true;
    public boolean showPrice = true;
    public boolean showToman = true;
    public boolean strike = false;

    public boolean titleBold = false;
    public boolean titleItalic = false;
    public boolean priceBold = true;
    public boolean priceItalic = false;

    // 0 = left, 1 = center, 2 = right
    public int textAlign = 2;

    public String titleFont = "default";
    public String priceFont = "default";

    // Legacy normalized geometry retained for template compatibility.
    public float x = 0f;
    public float y = 0f;
    public float w = 1f;
    public float h = 0.2f;

    public LabelField(String name, String value) {
        this.name = name == null ? "" : name;
        this.value = value == null ? "" : value;
    }

    public Typeface getTitleTypeface() {
        return makeTypeface(titleFont, titleBold, titleItalic);
    }

    public Typeface getPriceTypeface() {
        return makeTypeface(priceFont, priceBold, priceItalic);
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

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("name", name);
            o.put("value", value);
            o.put("backgroundColor", backgroundColor);
            o.put("borderColor", borderColor);
            o.put("titleColor", titleColor);
            o.put("priceColor", priceColor);
            o.put("tomanColor", tomanColor);
            o.put("borderWidth", borderWidth);
            o.put("cornerRadius", cornerRadius);
            o.put("paddingHorizontal", paddingHorizontal);
            o.put("paddingVertical", paddingVertical);
            o.put("titlePriceGap", titlePriceGap);
            o.put("titleSize", titleSize);
            o.put("priceSize", priceSize);
            o.put("tomanSize", tomanSize);
            o.put("visible", visible);
            o.put("showTitle", showTitle);
            o.put("showPrice", showPrice);
            o.put("showToman", showToman);
            o.put("strike", strike);
            o.put("titleBold", titleBold);
            o.put("titleItalic", titleItalic);
            o.put("priceBold", priceBold);
            o.put("priceItalic", priceItalic);
            o.put("textAlign", textAlign);
            o.put("titleFont", titleFont);
            o.put("priceFont", priceFont);
            o.put("x", x);
            o.put("y", y);
            o.put("w", w);
            o.put("h", h);
        } catch (Exception ignored) {}
        return o;
    }

    public static LabelField fromJson(JSONObject o) {
        LabelField f = new LabelField(
                o == null ? "" : o.optString("name", ""),
                o == null ? "" : o.optString("value", "")
        );
        if (o == null) return f;

        f.backgroundColor = o.optInt("backgroundColor", f.backgroundColor);
        f.borderColor = o.optInt("borderColor", f.borderColor);
        f.titleColor = o.optInt("titleColor", f.titleColor);
        f.priceColor = o.optInt("priceColor", f.priceColor);
        f.tomanColor = o.optInt("tomanColor", f.tomanColor);

        f.borderWidth = o.optInt("borderWidth", f.borderWidth);
        f.cornerRadius = o.optInt("cornerRadius", f.cornerRadius);
        f.paddingHorizontal = o.optInt("paddingHorizontal", f.paddingHorizontal);
        f.paddingVertical = o.optInt("paddingVertical", f.paddingVertical);
        f.titlePriceGap = o.optInt("titlePriceGap", f.titlePriceGap);

        f.titleSize = o.optInt("titleSize", f.titleSize);
        f.priceSize = o.optInt("priceSize", f.priceSize);
        f.tomanSize = o.optInt("tomanSize", f.tomanSize);

        f.visible = o.optBoolean("visible", f.visible);
        f.showTitle = o.optBoolean("showTitle", f.showTitle);
        f.showPrice = o.optBoolean("showPrice", f.showPrice);
        f.showToman = o.optBoolean("showToman", f.showToman);
        f.strike = o.optBoolean("strike", f.strike);

        f.titleBold = o.optBoolean("titleBold", f.titleBold);
        f.titleItalic = o.optBoolean("titleItalic", f.titleItalic);
        f.priceBold = o.optBoolean("priceBold", f.priceBold);
        f.priceItalic = o.optBoolean("priceItalic", f.priceItalic);

        f.textAlign = o.optInt("textAlign", f.textAlign);
        f.titleFont = o.optString("titleFont", f.titleFont);
        f.priceFont = o.optString("priceFont", f.priceFont);

        f.x = (float)o.optDouble("x", f.x);
        f.y = (float)o.optDouble("y", f.y);
        f.w = (float)o.optDouble("w", f.w);
        f.h = (float)o.optDouble("h", f.h);
        return f;
    }
}
