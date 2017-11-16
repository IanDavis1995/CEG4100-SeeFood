package edu.wright.ceg4110.fooddroid;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import edu.wright.ceg4110.fooddroid.web.ImageAnalysis;


public class AnalysisResultsActivity extends AppCompatActivity {

    private TextView imageNameDisplay;
    private TextView containsFoodDisplay;
    private TextView certaintyDisplay;
    private ImageView imagePreview;
    private ImageAnalysis imageAnalysis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis_results);

        imagePreview = findViewById(R.id.image_preview);
        imageNameDisplay = findViewById(R.id.image_name_result);
        containsFoodDisplay = findViewById(R.id.contains_food_result);
        certaintyDisplay = findViewById(R.id.certainty_result);

        JSONObject analysis;

        try {
            analysis = new JSONObject(getIntent().getStringExtra("analysis_results"));
        } catch (JSONException e) {
            Log.wtf("FoodDroid", e);
            return;
        }

//        String filename = getIntent().getStringExtra("image_name");
//        StringBuilder fileData = new StringBuilder();
//        Image uploadedImage;
//        String line;
//
//        try {
//            BufferedReader reader = new BufferedReader(new FileReader(filename));
//
//            while ((line = reader.readLine()) != null) {
//                    fileData.append(line).append("\n");
//            }
//
//            reader.close();
//            uploadedImage = new Image(fileData.toString());
//            imagePreview.setImageBitmap(uploadedImage.getBitmap());
//        } catch (IOException | JSONException e) {
//            e.printStackTrace();
//        }

        imagePreview.setImageBitmap(MainActivity.currentImage.getBitmap());

        try {
            imageAnalysis = new ImageAnalysis(analysis);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        if (imageAnalysis.containsFood()) {
            containsFoodDisplay.setText(R.string.has_food);
        } else {
            containsFoodDisplay.setText(R.string.no_food);
        }

        imageNameDisplay.setText(imageAnalysis.getName());
        certaintyDisplay.setText(imageAnalysis.getCertainty() + "%");
    }
}
