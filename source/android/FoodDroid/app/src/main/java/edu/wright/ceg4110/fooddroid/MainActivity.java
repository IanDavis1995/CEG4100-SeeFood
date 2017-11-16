package edu.wright.ceg4110.fooddroid;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TimingLogger;
import android.view.Surface;
import android.view.View;
import android.widget.Button;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;

import org.json.JSONException;
import org.json.JSONObject;

import edu.wright.ceg4110.fooddroid.web.HTTPHandler;
import edu.wright.ceg4110.fooddroid.web.Image;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "CAMERA_ACTIVITY";
    private CameraView cameraView;
    private Button takePictureButton;
    private Intent analysisResultsIntent;
    public static Image currentImage;
    private TimingLogger timing = new TimingLogger(TAG, "cameraActivityTiming");

    private Response.Listener<JSONObject> httpResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            timing.addSplit("got the http response back, starting the results view");
            timing.dumpToLog();

            try {
                Log.d(TAG, response.toString(4));
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }

            analysisResultsIntent.putExtra("analysis_results", response.toString());
            startActivity(analysisResultsIntent);
        }
    };

    private Response.ErrorListener httpErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e(TAG, error.getMessage());
        }
    };

    private CameraListener pictureTakenListener = new CameraListener() {
        @Override
        public void onPictureTaken(byte[] picture) {
            timing.addSplit("got the byte array for the picture, decoding");
            cameraView.stop();
            CameraUtils.decodeBitmap(picture, new CameraUtils.BitmapCallback() {
                @Override
                public void onBitmapReady(Bitmap bitmap) {
                    try {
                        currentImage = new Image(bitmap);
                        timing.addSplit("decoded the byte array, making the http request");
                        HTTPHandler.analyze(currentImage, httpResponseListener, httpErrorListener);
                    } catch (JSONException e) {
                        Log.e(TAG, Log.getStackTraceString(e));
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        analysisResultsIntent = new Intent(this, AnalysisResultsActivity.class);

        takePictureButton = this.findViewById(R.id.take_picture_button);
        takePictureButton.setOnClickListener(this);

        cameraView = this.findViewById(R.id.camera);
        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM); // Pinch to zoom!
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER); // Tap to focus!
        cameraView.mapGesture(Gesture.LONG_TAP, GestureAction.CAPTURE); // Long tap to shoot!
        cameraView.addCameraListener(pictureTakenListener);

        HTTPHandler.initialize(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
        HTTPHandler.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
        HTTPHandler.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.destroy();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == takePictureButton.getId()) {
            timing.addSplit("About to take a picture");
            cameraView.capturePicture();
        }
    }
}
