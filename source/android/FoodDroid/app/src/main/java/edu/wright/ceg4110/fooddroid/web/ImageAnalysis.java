package edu.wright.ceg4110.fooddroid.web;

import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * Created by ian on 11/14/17.
 */

public class ImageAnalysis {
    private String name;
    private boolean containsFood;
    private double certainty;

    public ImageAnalysis(String name, boolean containsFood, double certainty) {
        this.name = name;
        this.containsFood = containsFood;
        this.certainty = certainty;
    }

    public ImageAnalysis(JSONObject analysis) throws JSONException {
        name = analysis.getString("name");
        containsFood = analysis.getBoolean("contains_food");
        certainty = analysis.getDouble("certainty");
    }

    public String getName() {
        return name;
    }

    public boolean containsFood() {
        return containsFood;
    }

    public double getCertainty() {
        return certainty;
    }
}
