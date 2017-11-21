package edu.wright.ceg4110.fooddroid.web;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 *
 * Created by ian on 11/8/17.
 */
public class Image {
    private static final DateFormat TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
    private static final String TYPE = "jpg";
    private Bitmap bitmap;
    private String name;

    public Image(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.name = TIMESTAMP.format(new Date()) + ".jpg";
    }

    public Image(String data) throws JSONException {
        JSONObject jsonObject = new JSONObject(data);
        String binary = jsonObject.getString("data");
        byte[] imageData = Base64.decode(binary, Base64.DEFAULT);
        this.bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        this.name = jsonObject.getString("name");
    }

    public JSONObject json() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        this.bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        jsonObject.put("data", Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT));
        jsonObject.put("name", name);
        jsonObject.put("type", TYPE);
        return jsonObject;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setName(String imageName) {
        imageName = imageName
                .replace(" ", "")
                .replace("/", "")
                .replace(".", "")
                .replace("!", "")
                .replace("?", "");
        this.name = imageName + "." + TYPE;
    }
}
