package edu.wright.ceg4110.fooddroid.web;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Handler for the HTTP requests to send to the server.
 * Created by ian on 11/2/17.
 */
public class HTTPHandler {
    private static final String url = "http://34.234.120.169/";
    private static RequestQueue requestQueue;
    private static Context context;

    public static void initialize(Context ctx) {
        context = ctx;
        createRequestQueue();
    }

    public static void start() {
        requestQueue.start();
    }

    public static void stop() {
        requestQueue.stop();
    }

    private static void createRequestQueue() {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static void analyze(Image image,
                               Response.Listener<JSONObject> responseListener,
                               Response.ErrorListener errorListener) throws JSONException {
        String requestUrl = url + "analyze_json";
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, requestUrl, image.json(), responseListener, errorListener);
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(5000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(jsObjRequest);
    }
}
