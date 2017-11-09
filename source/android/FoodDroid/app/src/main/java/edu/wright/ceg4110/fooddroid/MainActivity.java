package edu.wright.ceg4110.fooddroid;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.otaliastudios.cameraview.CameraListener;
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

    private Response.Listener<JSONObject> httpResponseListener = new Response.Listener<JSONObject>() {
        @Override
        public void onResponse(JSONObject response) {
            try {
                Log.d(TAG, response.toString(4));
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    };

    private Response.ErrorListener httpErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.e(TAG, Log.getStackTraceString(error));
        }
    };

    private CameraListener pictureTakenListener = new CameraListener() {
        @Override
        public void onPictureTaken(byte[] picture) {
//            cameraView.stop();

            try {
                HTTPHandler.analyze(new Image(picture), httpResponseListener, httpErrorListener);
            } catch (JSONException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            cameraView.capturePicture();
        }
    }
}
