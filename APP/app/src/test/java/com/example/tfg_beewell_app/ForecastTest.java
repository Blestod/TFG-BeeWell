package com.example.tfg_beewell_app;

import android.util.Log;

import com.google.common.truth.Truth;

import org.junit.Test;
import com.example.tfg_beewell_app.Forecast;
import com.example.tfg_beewell_app.models.BgReading;
import com.example.tfg_beewell_app.Forecast.GlucoseForecast;
import lecho.lib.hellocharts.model.PointValue;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
/**
 * Tests for the forecast code.
 * <p>
 * Many of these tests also work as examples of usage of the Forecast function.
 * <p>
 * Created by Asbjorn Aarrestad on 7th January 2018.
 */
public class ForecastTest extends RobolectricTestNoConfig {
    private final static String TAG = "Forecast test";
    public static final int FUZZER = (int) (30 * 1000);
    @Test
    public void polyTrendLine_SimpleForecast() {
        // :: Setup
        Forecast.PolyTrendLine trendLine = new Forecast.PolyTrendLine(1);
        trendLine.setValues(new double[]{1, 2, 3, 4}, new double[]{1, 2, 3, 4});

        // :: Act
        double errorVarience = trendLine.errorVarience();

        double prediction = trendLine.predict(10);

        // :: Verify
        Truth.assertThat(prediction)
                .isWithin(0.01)
                .of(10);

        Truth.assertThat(errorVarience)
                .isWithin(0.01)
                .of(0);
    }

    @Test
    public void polyTrendLine_SimpleForecast_SecondDegree() {
        // :: Setup
        Forecast.PolyTrendLine trendLine = new Forecast.PolyTrendLine(2);
        trendLine.setValues(new double[]{1, 2, 3, 4}, new double[]{1, 2, 3, 4});

        // :: Act
        double errorVarience = trendLine.errorVarience();
        double prediction = trendLine.predict(10);
        System.out.println("Prediction: " + prediction);
        System.out.println("Error Variance: " + errorVarience);
        // :: Verify
        Truth.assertThat(prediction)
                .isWithin(0.01)
                .of(10);

        Truth.assertThat(errorVarience)
                .isWithin(0.01)
                .of(0);
    }

    @Test
    public void polyTrendLine_AdvancedForecast_SecondDegree() {
        // :: Setup
        Forecast.PolyTrendLine trendLine = new Forecast.PolyTrendLine(2);
        // Data set as y=x^2
        trendLine.setValues(new double[]{1, 4, 9, 16}, new double[]{1, 2, 3, 4});

        // :: Act
        double errorVarience = trendLine.errorVarience();

        double prediction = trendLine.predict(5);

        // :: Verify
        Truth.assertThat(prediction)
                .isWithin(0.01)
                .of(25);

        Truth.assertThat(errorVarience)
                .isWithin(0.01)
                .of(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setValuesWithDifferentLengthXandY() {
        // :: Setup
        Forecast.PolyTrendLine trendLine = new Forecast.PolyTrendLine(2);

        // :: Act
        trendLine.setValues(new double[]{1, 2, 3}, new double[]{1, 2});
    }

    @Test
    public void toPrimitiveFromList_nullInput() {
        // :: Act
        double[] result = Forecast.OLSTrendLine.toPrimitiveFromList(null);

        // :: Verify
        Truth.assertThat(result).isNull();
    }

    @Test
    public void toPrimitiveFromList_emptyInput() {
        // :: Act
        double[] result = Forecast.OLSTrendLine.toPrimitiveFromList(Collections.emptyList());

        // :: Verify
        Truth.assertThat(result).isEmpty();
    }

    @Test
    public void toPrimitiveFromList_ListWithElements() {
        // :: Setup
        List<Double> values = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            values.add((double) i);
        }

        // :: Act
        double[] result = Forecast.OLSTrendLine.toPrimitiveFromList(values);

        // :: Verify
        Truth.assertThat(result).hasLength(5);
        Truth.assertThat(result)
                .usingTolerance(0.001)
                .containsExactly(new double[]{1, 2, 3, 4, 5})
                .inOrder();
    }

    @Test
    public void expTrendLine_simpleTest() {
        // :: Setup
        Forecast.ExpTrendLine trendLine = new Forecast.ExpTrendLine();
        // Data set as y=2^x
        trendLine.setValues(new double[]{2, 4, 8, 16}, new double[]{1, 2, 3, 4});

        // :: Act
        double errorVarience = trendLine.errorVarience();
        double prediction = trendLine.predict(5);

        // :: Verify
        Truth.assertThat(prediction)
                .isWithin(0.01)
                .of(32);

        Truth.assertThat(errorVarience)
                .isWithin(0.01)
                .of(0);
    }

    @Test
    public void powerTrendLine_simpleTest() {
        // :: Setup
        Forecast.PowerTrendLine trendLine = new Forecast.PowerTrendLine();
        // Data set as y=2x
        trendLine.setValues(new double[]{2, 4, 6, 8}, new double[]{1, 2, 3, 4});

        // :: Act
        double errorVarience = trendLine.errorVarience();
        double prediction = trendLine.predict(5);

        // :: Verify
        Truth.assertThat(prediction)
                .isWithin(0.01)
                .of(10);

        Truth.assertThat(errorVarience)
                .isWithin(0.01)
                .of(0);
    }


    @Test
    public void logTrendLine_simpleTest() {
        // :: Setup
        Forecast.LogTrendLine trendLine = new Forecast.LogTrendLine();
        // Data set first large increase, then less and less increasing values.
        trendLine.setValues(new double[]{1, 16, 26, 31}, new double[]{1, 2, 3, 4});

        // :: Act
        double errorVarience = trendLine.errorVarience();
        double prediction = trendLine.predict(5);

        // :: Verify
        Truth.assertThat(prediction)
                .isWithin(0.01)
                .of(36.41);

        Truth.assertThat(errorVarience)
                .isWithin(0.01)
                .of(0.5);
    }

    @Test
    public void polyTrendLine_24HourForecast() {
        Forecast.PolyTrendLine trendLine = new Forecast.PolyTrendLine(2);
        double[] yValues = new double[24];
        double[] xValues = new double[24];
        for (int i = 0; i < 24; i++) {
            yValues[i] = 100 + 10 * Math.sin(i * Math.PI / 12);
            xValues[i] = i;
        }
        trendLine.setValues(yValues, xValues);

        double prediction = trendLine.predict(24);
        double errorVarience = trendLine.errorVarience();

        Truth.assertThat(prediction)
                .isWithin(50)
                .of(100);

        Truth.assertThat(errorVarience)
                .isLessThan(50);
    }

    /**
     * Test the predictNextHour method of the GlucoseForecast class.
     *
     * This test simulates a scenario where we have glucose readings for the past 6 hours
     * and predict the glucose levels for the next hour.
     */
    @Test
    public void testPredictNextHour() {
        // Initialize the GlucoseForecast object
        GlucoseForecast forecast = new GlucoseForecast();

        // Create a list to store simulated glucose readings
        List<BgReading> bgReadings = new ArrayList<>();

        // Set the start time to 6 hours ago
        long startTime = System.currentTimeMillis() - (6 * 60 * 60 * 1000);

        // Generate 72 simulated readings (one every 5 minutes for 6 hours)
        for (int i = 0; i < 72; i++) {
            BgReading reading = new BgReading();
            reading.timestamp = startTime + i * 5 * 60 * 1000;
            // Simulate glucose values using a sine wave for variation
            reading.calculated_value = 100 + 10 * Math.sin(i * Math.PI / 36);
            bgReadings.add(reading);
        }

        // Predict glucose levels for the next hour
        List<PointValue> predictions = forecast.predictNextHour(bgReadings);

        // Verify that predictions are not null and contain the expected number of points
        assertNotNull(predictions);
        assertEquals(13, predictions.size());  // 12 5-minute intervals plus the current time

        // Calculate the current time and one hour from now for validation
        long currentTime = System.currentTimeMillis();
        long oneHourLater = currentTime + (60 * 60 * 1000);

        // Print the predictions for manual inspection
        System.out.println("Predictions for the next hour:");
        for (PointValue point : predictions) {
            long timestamp = (long) (point.getX() * FUZZER);
            double glucoseValue = point.getY();
            System.out.printf("Time: %s, Predicted Glucose: %.2f mg/dL%n",
                    Forecast.dateTimeText(timestamp), glucoseValue);
        }

        // Verify that the predictions fall within the expected time range
        assertTrue(predictions.get(0).getX() * FUZZER >= currentTime);
        assertTrue(predictions.get(predictions.size() - 1).getX() * FUZZER <= oneHourLater + 5 * 60 * 1000);

        // Verify that all predicted glucose values are positive
        for (PointValue point : predictions) {
            assertTrue(point.getY() > 0);  // Assuming glucose values are always positive
        }
    }
}

