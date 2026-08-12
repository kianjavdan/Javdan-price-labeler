package com.javdan.pricelabeler;

import org.json.JSONObject;

public class LabelField {
    public String name;
    public String value;
    public float x = 0.08f, y = 0.08f, w = 0.84f, h = 0.16f;
    public int titleSize = 24;
    public int priceSize = 34;
    public int titleColor = 0xFF333333;
    public int priceColor = 0xFFC62828;
    public boolean strike = false;

    public LabelField(String n, String v) {
        name = n;
        value = v;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("name", name);
            o.put("value", value);
            o.put("x", x); o.put("y", y); o.put("w", w); o.put("h", h);
            o.put("titleSize", titleSize); o.put("priceSize", priceSize);
            o.put("titleColor", titleColor); o.put("priceColor", priceColor);
            o.put("strike", strike);
        } catch (Exception ignored) {}
        return o;
    }

    public static LabelField fromJson(JSONObject o) {
        LabelField f = new LabelField(o.optString("name","قیمت"), o.optString("value",""));
        f.x = (float)o.optDouble("x",.08); f.y = (float)o.optDouble("y",.08);
        f.w = (float)o.optDouble("w",.84); f.h = (float)o.optDouble("h",.16);
        f.titleSize = o.optInt("titleSize",24); f.priceSize = o.optInt("priceSize",34);
        f.titleColor = o.optInt("titleColor",0xFF333333);
        f.priceColor = o.optInt("priceColor",0xFFC62828);
        f.strike = o.optBoolean("strike",false);
        return f;
    }
}
