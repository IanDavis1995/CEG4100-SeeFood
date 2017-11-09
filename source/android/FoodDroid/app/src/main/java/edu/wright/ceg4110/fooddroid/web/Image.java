package edu.wright.ceg4110.fooddroid.web;

import android.util.Base64;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.request.MultiPartRequest;
import com.android.volley.request.SimpleMultiPartRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    public MultiPartRequest<String> multiPartRequest(String requestUrl,
                                                     Response.Listener<String> responseListener,
                                                     Response.ErrorListener errorListener) throws IOException {
        SimpleMultiPartRequest request = new SimpleMultiPartRequest(Request.Method.POST,
                requestUrl,
                responseListener,
                errorListener);
        request.setRetryPolicy(new DefaultRetryPolicy(5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        addMultipartEntity(request);
        return request;
    }

    public void addMultipartEntity(MultiPartRequest<String> request) throws IOException {
        request.addFile("file", multipartFilepath());
        request.addStringParam("name", name);
        request.addStringParam("type", TYPE);
    }

    public String multipartFilepath() throws IOException {
        File temporaryImageFile = File.createTempFile("seefood_upload_", "");
        FileOutputStream fos = new FileOutputStream(temporaryImageFile);
        fos.write(data);
        fos.flush();
        fos.close();
        return temporaryImageFile.getAbsolutePath();
    }
}
