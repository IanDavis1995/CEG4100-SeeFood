package edu.wright.ceg4110.fooddroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import edu.wright.ceg4110.fooddroid.web.HTTPHandler;
import edu.wright.ceg4110.fooddroid.web.Image;

public class GalleryActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "GALLERY_ACTIVITY";
    private LinearLayout scrollLinearLayout;
    private ProgressBar galleryProgressBar;
    private HashMap<Integer, String> imagesToIDs;
    private Intent analysisResultsIntent;
    private boolean imageClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        galleryProgressBar = this.findViewById(R.id.galleryLoadingProgress);
        galleryProgressBar.setIndeterminate(true);
        scrollLinearLayout = this.findViewById(R.id.scroll_view_layout);
        analysisResultsIntent = new Intent(this, AnalysisResultsActivity.class);
    }

    private Response.Listener<JSONObject> getAnalysisResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            try {
                Log.d(TAG, response.toString(4));
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            if (!response.has("file0")) {
                Log.e(TAG, "No results found for image!");
                return;
            }

            JSONObject imageData;

            try {
                imageData = response.getJSONObject("file0");
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
                return;
            }

            Image image;

            try {
                image = new Image(imageData.toString());
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
                return;
            }

            AnalysisResultsActivity.setCurrentImage(image);
            imageData.remove("data");

            analysisResultsIntent.putExtra("analysis_results", imageData.toString());
            startActivity(analysisResultsIntent);
        }
    };

    private Response.Listener<JSONObject> getGalleryResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            imageClicked = false;

            try {
                Log.d(TAG, response.toString(4));
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            handleGalleryResponse(response);
        }
    };

    private Response.ErrorListener httpErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            imageClicked = false;
            Log.e(TAG, "Unexpected error occurred communicating with server: " + error.getMessage());
        }
    };

    private void handleGalleryResponse(JSONObject response) {
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

            if (currentColumn >= columnsPerRow) {
                allRows.add(makeImageRow(currentRow));
                currentRow = new ArrayList<>();
                currentColumn = 1;
            } else {
                currentColumn++;
            }
        }

        reloadGallery();

        for (LinearLayout layout: allRows) {
            scrollLinearLayout.addView(layout, 0);
        }

        if (!currentRow.isEmpty()) {
            scrollLinearLayout.addView(makeImageRow(currentRow));
        }

        galleryProgressBar.setVisibility(View.INVISIBLE);
    }

    private void reloadGallery() {
        scrollLinearLayout.removeAllViews();
        scrollLinearLayout.addView(galleryProgressBar);
    }

    private LinearLayout makeImageRow(ArrayList<Image> imageRow) {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(25, 25, 25, 25);
        linearLayout.setLayoutParams(layoutParams);

        for (Image currentImage: imageRow) {
            ImageView imageView = new ImageView(this);
            String imageName = currentImage.getName();
            Integer imageID = imageName.hashCode();
            imageView.setId(imageID);
            imageView.setImageBitmap(currentImage.getBitmap());
            imageView.setLayoutParams(params);
            linearLayout.addView(imageView);
            imagesToIDs.put(imageID, imageName);
            imageView.setOnClickListener(this);
        }

        return linearLayout;
    }

    @Override
    protected void onResume() {
        super.onResume();

        imagesToIDs = new HashMap<>();
        imageClicked = false;

        reloadGallery();

        try {
            galleryProgressBar.setVisibility(View.VISIBLE);
            HTTPHandler.getAllImages(getGalleryResponseListener, httpErrorListener);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        if (imageClicked) {
            return;
        } else {
            imageClicked = true;
        }

        String imageName = imagesToIDs.get(view.getId());

        if (imageName == null) {
            imageClicked = false;
            return;
        }

        Log.d(TAG, "Image with name: " + imageName + " was clicked, loading results...");

        try {
            HTTPHandler.lookupImage(imageName, getAnalysisResponseListener, httpErrorListener);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
