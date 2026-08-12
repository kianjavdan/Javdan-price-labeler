package com.javdan.pricelabeler;

import android.graphics.Typeface;

import org.json.JSONObject;

public class LabelField {

    // متن
    public String name;
    public String value;

    // موقعیت و اندازه کادر به صورت درصدی
    public float x = 0.08f;
    public float y = 0.08f;
    public float w = 0.84f;
    public float h = 0.16f;

    // اندازه فونت
    public int titleSize = 24;
    public int priceSize = 34;
    public int tomanSize = 15;

    // رنگ نوشته‌ها
    public int titleColor = 0xFF333333;
    public int priceColor = 0xFFC62828;
    public int tomanColor = 0xFFC62828;

    // ظاهر کادر
    public int backgroundColor = 0xFFFFFFFF;
    public int borderColor = 0xFFD8D8D8;

    public int borderWidth = 2;
    public int cornerRadius = 18;

    // فاصله داخلی کادر
    public int paddingHorizontal = 18;
    public int paddingVertical = 12;

    // فاصله عنوان تا قیمت
    public int titlePriceGap = 6;

    // وضعیت‌ها
    public boolean strike = false;
    public boolean showToman = true;
    public boolean visible = true;

    // استایل عنوان
    public boolean titleBold = false;
    public boolean titleItalic = false;

    // استایل قیمت
    public boolean priceBold = true;
    public boolean priceItalic = false;

    // تراز متن
    // 0 = راست
    // 1 = وسط
    // 2 = چپ
    public int textAlign = 0;

    // فونت
    // فعلاً نام منطقی ذخیره می‌کنیم.
    // بعداً در LabelDesignerView به Typeface تبدیل می‌شود.
    public String titleFont = "DEFAULT";
    public String priceFont = "DEFAULT";

    // نمایش عنوان و قیمت
    public boolean showTitle = true;
    public boolean showPrice = true;


    public LabelField(String n, String v) {
        name = n;
        value = v;
    }


    public JSONObject toJson() {

        JSONObject o = new JSONObject();

        try {

            o.put("name", name);
            o.put("value", value);

            o.put("x", x);
            o.put("y", y);
            o.put("w", w);
            o.put("h", h);

            o.put("titleSize", titleSize);
            o.put("priceSize", priceSize);
            o.put("tomanSize", tomanSize);

            o.put("titleColor", titleColor);
            o.put("priceColor", priceColor);
            o.put("tomanColor", tomanColor);

            o.put("backgroundColor", backgroundColor);
            o.put("borderColor", borderColor);

            o.put("borderWidth", borderWidth);
            o.put("cornerRadius", cornerRadius);

            o.put("paddingHorizontal", paddingHorizontal);
            o.put("paddingVertical", paddingVertical);

            o.put("titlePriceGap", titlePriceGap);

            o.put("strike", strike);
            o.put("showToman", showToman);
            o.put("visible", visible);

            o.put("titleBold", titleBold);
            o.put("titleItalic", titleItalic);

            o.put("priceBold", priceBold);
            o.put("priceItalic", priceItalic);

            o.put("textAlign", textAlign);

            o.put("titleFont", titleFont);
            o.put("priceFont", priceFont);

            o.put("showTitle", showTitle);
            o.put("showPrice", showPrice);

        } catch (Exception ignored) {
        }

        return o;
    }


    public static LabelField fromJson(JSONObject o) {

        LabelField f =
                new LabelField(
                        o.optString("name", "قیمت"),
                        o.optString("value", "")
                );

        f.x =
                (float) o.optDouble(
                        "x",
                        0.08
                );

        f.y =
                (float) o.optDouble(
                        "y",
                        0.08
                );

        f.w =
                (float) o.optDouble(
                        "w",
                        0.84
                );

        f.h =
                (float) o.optDouble(
                        "h",
                        0.16
                );


        f.titleSize =
                o.optInt(
                        "titleSize",
                        24
                );

        f.priceSize =
                o.optInt(
                        "priceSize",
                        34
                );

        f.tomanSize =
                o.optInt(
                        "tomanSize",
                        15
                );


        f.titleColor =
                o.optInt(
                        "titleColor",
                        0xFF333333
                );

        f.priceColor =
                o.optInt(
                        "priceColor",
                        0xFFC62828
                );

        f.tomanColor =
                o.optInt(
                        "tomanColor",
                        f.priceColor
                );


        f.backgroundColor =
                o.optInt(
                        "backgroundColor",
                        0xFFFFFFFF
                );

        f.borderColor =
                o.optInt(
                        "borderColor",
                        0xFFD8D8D8
                );


        f.borderWidth =
                o.optInt(
                        "borderWidth",
                        2
                );

        f.cornerRadius =
                o.optInt(
                        "cornerRadius",
                        18
                );


        f.paddingHorizontal =
                o.optInt(
                        "paddingHorizontal",
                        18
                );

        f.paddingVertical =
                o.optInt(
                        "paddingVertical",
                        12
                );


        f.titlePriceGap =
                o.optInt(
                        "titlePriceGap",
                        6
                );


        f.strike =
                o.optBoolean(
                        "strike",
                        false
                );

        f.showToman =
                o.optBoolean(
                        "showToman",
                        true
                );

        f.visible =
                o.optBoolean(
                        "visible",
                        true
                );


        f.titleBold =
                o.optBoolean(
                        "titleBold",
                        false
                );

        f.titleItalic =
                o.optBoolean(
                        "titleItalic",
                        false
                );


        f.priceBold =
                o.optBoolean(
                        "priceBold",
                        true
                );

        f.priceItalic =
                o.optBoolean(
                        "priceItalic",
                        false
                );


        f.textAlign =
                o.optInt(
                        "textAlign",
                        0
                );


        f.titleFont =
                o.optString(
                        "titleFont",
                        "DEFAULT"
                );

        f.priceFont =
                o.optString(
                        "priceFont",
                        "DEFAULT"
                );


        f.showTitle =
                o.optBoolean(
                        "showTitle",
                        true
                );

        f.showPrice =
                o.optBoolean(
                        "showPrice",
                        true
                );


        return f;
    }


    public Typeface getTitleTypeface() {

        int style =
                Typeface.NORMAL;

        if (titleBold && titleItalic) {
            style = Typeface.BOLD_ITALIC;
        } else if (titleBold) {
            style = Typeface.BOLD;
        } else if (titleItalic) {
            style = Typeface.ITALIC;
        }

        return makeTypeface(
                titleFont,
                style
        );
    }


    public Typeface getPriceTypeface() {

        int style =
                Typeface.NORMAL;

        if (priceBold && priceItalic) {
            style = Typeface.BOLD_ITALIC;
        } else if (priceBold) {
            style = Typeface.BOLD;
        } else if (priceItalic) {
            style = Typeface.ITALIC;
        }

        return makeTypeface(
                priceFont,
                style
        );
    }


    private Typeface makeTypeface(
            String font,
            int style
    ) {

        if (font == null) {
            return Typeface.create(
                    Typeface.DEFAULT,
                    style
            );
        }

        switch (font) {

            case "SERIF":
                return Typeface.create(
                        Typeface.SERIF,
                        style
                );

            case "MONOSPACE":
                return Typeface.create(
                        Typeface.MONOSPACE,
                        style
                );

            case "SANS_SERIF":
                return Typeface.create(
                        Typeface.SANS_SERIF,
                        style
                );

            case "DEFAULT":

            default:
                return Typeface.create(
                        Typeface.DEFAULT,
                        style
                );
        }
    }
}
