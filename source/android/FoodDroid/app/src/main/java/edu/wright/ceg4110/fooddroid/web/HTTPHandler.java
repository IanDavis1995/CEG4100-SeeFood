package edu.wright.ceg4110.fooddroid.web;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handler for the HTTP requests to send to the server.
 * Created by ian on 11/2/17.
 */
public class HTTPHandler {
    private static final String TAG = "HTTP_HANDLER";
    private static final String url = "http://34.234.120.169/";
    private static RequestQueue requestQueue;
    private static boolean initialized = false;

    public static void initialize(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        initialized = true;
    }

    public static void start() {
        assertInitialized();
        Log.d(TAG, "Starting requestQueue");
        requestQueue.start();
    }

    public static void stop() {
        assertInitialized();
        Log.d(TAG, "Stopping requestQueue");
        requestQueue.stop();
    }

    public static void sendRequest(JsonObjectRequest request) {
        request.setRetryPolicy(new DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    public static void analyze(Image image,
                               Response.Listener<JSONObject> responseListener,
                               Response.ErrorListener errorListener) throws JSONException {
        assertInitialized();
        Log.d(TAG, "Sending analyze HTTP Request");
        String requestUrl = url + "analyze_json";
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, requestUrl, image.json(), responseListener, errorListener);
        sendRequest(jsObjRequest);
    }

    public static void lookupImage(String imageName, Response.Listener<JSONObject> responseListener,
                                   Response.ErrorListener errorListener) throws JSONException {
        assertInitialized();
        Log.d(TAG, "Sending lookupImage HTTP Request");
        String requestUrl = url + "search";
        JSONObject criteria = makeCriteria(imageName, "", "");
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, requestUrl, criteria, responseListener, errorListener);
        sendRequest(jsonObjectRequest);
    }

    public static void getAllImages(Response.Listener<JSONObject> responseListener,
                                    Response.ErrorListener errorListener) throws JSONException {
        assertInitialized();
        Log.d(TAG, "Sending getAllImages HTTP Request");
        String requestUrl = url + "search";
        JSONObject emptyCriteria = makeEmptyCriteria();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, requestUrl, emptyCriteria, responseListener, errorListener);
        sendRequest(jsonObjectRequest);
    }

    private static JSONObject makeCriteria(String imageName, String decision, String time) throws JSONException {
        JSONObject object = new JSONObject();
        object.put("name", imageName);
        object.put("food", decision);
        object.put("time", time);
        return object;
    }

    private static JSONObject makeEmptyCriteria() throws JSONException {
        return makeCriteria("", "", "");
    }

    private static void assertInitialized() throws IllegalStateException {
        if (!initialized) {
            throw new IllegalStateException("HTTPHandler was used before it was initialized!");
        }
    }
}
