package edu.wright.ceg4110.fooddroid;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import edu.wright.ceg4110.fooddroid.web.HTTPHandler;
import edu.wright.ceg4110.fooddroid.web.Image;

public class GalleryActivity extends AppCompatActivity {

    private final String TAG = "GALLERY_ACTIVITY";
    private LinearLayout scrollLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        scrollLinearLayout = this.findViewById(R.id.scroll_view_layout);

        try {
            HTTPHandler.getAllImages(httpResponseListener, httpErrorListener);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private Response.Listener<JSONObject> httpResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            try {
                Log.d(TAG, response.toString(4));
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            int columnsPerRow = 3;
            int currentColumn = 1;
            ArrayList<LinearLayout> allRows = new ArrayList<>();
            ArrayList<Image> currentRow = new ArrayList<>();
            Iterator<String> keyIterator = response.keys();
            String currentKey;

            while (keyIterator.hasNext()) {
                currentKey = keyIterator.next();
                Image imageObject;

                try {
                    imageObject = new Image(response.getString(currentKey));
                } catch (JSONException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                    return;
                }

                currentRow.add(imageObject);
                currentColumn++;

                if (currentColumn > columnsPerRow) {
                    allRows.add(makeImageRow(currentRow));
                    currentRow = new ArrayList<>();
                }
            }

            for (LinearLayout layout: allRows) {
                scrollLinearLayout.addView(layout, 0);
            }
        }
    };

    private Response.ErrorListener httpErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e(TAG, error.getMessage());
        }
    };

    private LinearLayout makeImageRow(ArrayList<Image> imageRow) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        for (Image currentImage: imageRow) {
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(currentImage.getBitmap());
            linearLayout.addView(imageView);
        }

        return linearLayout;
    }
}
