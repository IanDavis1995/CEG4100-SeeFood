package edu.wright.ceg4110.fooddroid;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TimingLogger;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraUtils;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.wright.ceg4110.fooddroid.web.HTTPHandler;
import edu.wright.ceg4110.fooddroid.web.Image;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "CAMERA_ACTIVITY";
    private final int SELECT_IMAGE = 1;
    private CameraView cameraView;
    private EditText imageNameEdit;
    private TextView imageNameLabel;
    private Button takePictureButton;
    private Button confirmUploadButton;
    private Button cancelUploadButton;
    private Button pastUploadsButton;
    private Button uploadExistingButton;
    private Intent analysisResultsIntent;
    public static Image currentImage;
    private TimingLogger timing = new TimingLogger(TAG, "cameraActivityTiming");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        analysisResultsIntent = new Intent(this, AnalysisResultsActivity.class);

        imageNameLabel = this.findViewById(R.id.image_name_label);

        pastUploadsButton = this.findViewById(R.id.past_uploads_button);
        pastUploadsButton.setOnClickListener(this);

        uploadExistingButton = this.findViewById(R.id.upload_existing_button);
        uploadExistingButton.setOnClickListener(this);

        confirmUploadButton = this.findViewById(R.id.confirm_upload_button);
        confirmUploadButton.setOnClickListener(this);

        cancelUploadButton = this.findViewById(R.id.cancel_upload_button);
        cancelUploadButton.setOnClickListener(this);

        imageNameEdit = this.findViewById(R.id.image_name_edit);

        takePictureButton = this.findViewById(R.id.take_picture_button);
        takePictureButton.setOnClickListener(this);
        cameraView = this.findViewById(R.id.camera);

        cameraView.mapGesture(Gesture.PINCH, GestureAction.ZOOM); // Pinch to zoom!
        cameraView.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER); // Tap to focus!
        cameraView.mapGesture(Gesture.LONG_TAP, GestureAction.CAPTURE); // Long tap to shoot!
        cameraView.addCameraListener(pictureTakenListener);

        HTTPHandler.initialize(getApplicationContext());
        HTTPHandler.start();
    }

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

            AnalysisResultsActivity.setCurrentImage(currentImage);

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
                    currentImage = new Image(bitmap);
                    timing.addSplit("decoded the byte array, making the http request");
                    setConfirmUploadMode();
                }
            });
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        setTakePictureMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HTTPHandler.stop();
        cameraView.destroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.take_picture_button:
                timing.addSplit("About to take a picture");
                cameraView.capturePicture();
                break;
            case R.id.confirm_upload_button:
                String imageName = imageNameEdit.getText().toString();

                if (imageName.equals("")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("No image name given")
                            .setMessage("Please supply a name for the given image!")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .show();
                    return;
                }

                currentImage.setName(imageName);

                try {
                    HTTPHandler.analyze(currentImage, httpResponseListener, httpErrorListener);
                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                }
                break;
            case R.id.cancel_upload_button:
                setTakePictureMode();
                break;
            case R.id.past_uploads_button:
                Intent galleryIntent = new Intent(this, GalleryActivity.class);
                startActivity(galleryIntent);
                break;
            case R.id.upload_existing_button:
                // TODO: Implement launching the local gallery to select image(s).
                break;
        }
    }

    private void setConfirmUploadMode() {
        confirmUploadButton.setVisibility(View.VISIBLE);
        cancelUploadButton.setVisibility(View.VISIBLE);
        imageNameEdit.setVisibility(View.VISIBLE);
        imageNameLabel.setVisibility(View.VISIBLE);
        pastUploadsButton.setVisibility(View.INVISIBLE);
        uploadExistingButton.setVisibility(View.INVISIBLE);
        takePictureButton.setVisibility(View.INVISIBLE);
    }

    private void setTakePictureMode() {
        takePictureButton.setVisibility(View.VISIBLE);
        pastUploadsButton.setVisibility(View.VISIBLE);
        uploadExistingButton.setVisibility(View.VISIBLE);
        imageNameEdit.setVisibility(View.INVISIBLE);
        imageNameLabel.setVisibility(View.INVISIBLE);
        confirmUploadButton.setVisibility(View.INVISIBLE);
        cancelUploadButton.setVisibility(View.INVISIBLE);
        cameraView.start();
    }

    private void displayImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), SELECT_IMAGE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                currentImage = new Image(bitmap);
                setConfirmUploadMode();
            } catch (IOException e) {
                Log.e(TAG, "Unknown error", e);
            }
        }
    }
}
