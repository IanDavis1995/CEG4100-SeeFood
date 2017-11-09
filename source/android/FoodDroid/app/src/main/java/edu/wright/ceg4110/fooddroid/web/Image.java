package edu.wright.ceg4110.fooddroid.web;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represent
 * Created by ian on 11/8/17.
 */

public class Image {
    private static final DateFormat TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
    private static final String TYPE = "jpg";
    private byte[] data;
    private String name;

    public Image(byte[] data) {
        this.data = data;
        this.name = TIMESTAMP.format(new Date()) + ".jpg";
    }

    public JSONObject json() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", Base64.encodeToString(data, Base64.DEFAULT));
        jsonObject.put("name", name);
        jsonObject.put("type", TYPE);
        return jsonObject;
    }
}
