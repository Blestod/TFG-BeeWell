package com.example.tfg_beewell_app;

import android.util.Log;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import com.example.tfg_beewell_app.models.BgReading;
import lecho.lib.hellocharts.model.PointValue;

/**
 * Created by jamorham on 08/02/2016.
 */
public class Forecast {

    // from stackoverflow.com/questions/17592139/trend-lines-regression-curve-fitting-java-library
    public static final int FUZZER = (int) (30 * 1000);
    private static final String TAG = "jamorham forecast";
    // singletons to avoid repeated allocation
    private static DecimalFormatSymbols dfs;
    private static DecimalFormat df;
    public static long tsl() {
        return System.currentTimeMillis();
    }
    public static String qs(double x) {
        return qs(x, 2);
    }
    public static String dateTimeText(long timestamp) {
        return android.text.format.DateFormat.format("yyyy-MM-dd kk:mm:ss", timestamp).toString();
    }
    public static String qs(double x, int digits) {

        if (digits == -1) {
            digits = 0;
            if (((int) x != x)) {
                digits++;
                if ((((int) x * 10) / 10 != x)) {
                    digits++;
                    if ((((int) x * 100) / 100 != x)) digits++;
                }
            }
        }

        if (dfs == null) {
            final DecimalFormatSymbols local_dfs = new DecimalFormatSymbols();
            local_dfs.setDecimalSeparator('.');
            dfs = local_dfs; // avoid race condition
        }

        final DecimalFormat this_df;
        // use singleton if on ui thread otherwise allocate new as DecimalFormat is not thread safe
        if (Thread.currentThread().getId() == 1) {
            if (df == null) {
                final DecimalFormat local_df = new DecimalFormat("#", dfs);
                local_df.setMinimumIntegerDigits(1);
                df = local_df; // avoid race condition
            }
            this_df = df;
        } else {
            this_df = new DecimalFormat("#", dfs);
        }

        this_df.setMaximumFractionDigits(digits);
        return this_df.format(x);
    }
    public interface TrendLine {
        void setValues(double[] y, double[] x); // y ~ f(x)

        double predict(double x); // get a predicted y for a given x

        double errorVarience();
    }

    public abstract static class OLSTrendLine implements TrendLine {

        RealMatrix coef = null; // will hold prediction coefs once we get values
        Double last_error_rate = null;

        public static double[] toPrimitive(Double[] array) {
            if (array == null) {
                return null;
            } else if (array.length == 0) {
                return new double[0];
            }
            final double[] result = new double[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i];
            }
            return result;
        }

        public static double[] toPrimitiveFromList(Collection<Double> array) {
            if (array == null) {
                return null;
            }

            return toPrimitive(array.toArray(new Double[array.size()]));
        }

        protected abstract double[] xVector(double x); // create vector of values from x

        protected abstract boolean logY(); // set true to predict log of y (note: y must be positive)

        @Override
        public void setValues(double[] y, double[] x) {
            if (x.length != y.length) {
                throw new IllegalArgumentException(String.format("The numbers of y and x values must be equal (%d != %d)", y.length, x.length));
            }
            double[][] xData = new double[x.length][];
            for (int i = 0; i < x.length; i++) {
                // the implementation determines how to produce a vector of predictors from a single x
                xData[i] = xVector(x[i]);
            }
            if (logY()) { // in some models we are predicting ln y, so we replace each y with ln y
                y = Arrays.copyOf(y, y.length); // user might not be finished with the array we were given
                for (int i = 0; i < x.length; i++) {
                    y[i] = Math.log(y[i]);
                }
            }
            final OLSMultipleLinearRegression ols = new OLSMultipleLinearRegression();
            ols.setNoIntercept(true); // let the implementation include a constant in xVector if desired
            ols.newSampleData(y, xData); // provide the data to the model
            coef = MatrixUtils.createColumnRealMatrix(ols.estimateRegressionParameters()); // get our coefs
            last_error_rate = ols.estimateErrorVariance();
            Log.d(TAG, getClass().getSimpleName() + " Forecast Error rate: errorvar:"
                    + qs(last_error_rate, 4)
                    + " regssionvar:" + qs(ols.estimateRegressandVariance(), 4)
                    + "  stderror:" + qs(ols.estimateRegressionStandardError(), 4));
        }

        @Override
        public double predict(double x) {
            double yhat = coef.preMultiply(xVector(x))[0]; // apply coefs to xVector
            if (logY()) yhat = (Math.exp(yhat)); // if we predicted ln y, we still need to get y
            return yhat;
        }

        public double errorVarience() {
            return last_error_rate;
        }

    }

    public static class PolyTrendLine extends OLSTrendLine {
        final int degree;

        public PolyTrendLine(int degree) {
            if (degree < 0)
                throw new IllegalArgumentException("The degree of the polynomial must not be negative");
            this.degree = degree;
        }

        protected double[] xVector(double x) { // {1, x, x*x, x*x*x, ...}
            double[] poly = new double[degree + 1];
            double xi = 1;
            for (int i = 0; i <= degree; i++) {
                poly[i] = xi;
                xi *= x;
            }
            return poly;
        }

        @Override
        protected boolean logY() {
            return false;
        }
    }

    public static class ExpTrendLine extends OLSTrendLine {
        @Override
        protected double[] xVector(double x) {
            return new double[]{1, x};
        }

        @Override
        protected boolean logY() {
            return true;
        }
    }

    public static class PowerTrendLine extends OLSTrendLine {
        @Override
        protected double[] xVector(double x) {
            return new double[]{1, Math.log(x)};
        }

        @Override
        protected boolean logY() {
            return true;
        }
    }

    public static class LogTrendLine extends OLSTrendLine {
        @Override
        protected double[] xVector(double x) {
            return new double[]{1, Math.log(x)};
        }

        @Override
        protected boolean logY() {
            return false;
        }
    }

    public static class GlucoseForecast {
        private static final int PREDICTION_MINUTES = 60;
        private static final int INTERVAL_MINUTES = 5;

        public static List<PointValue> predictNextHour(List<BgReading> bgReadings) {
            if (bgReadings == null || bgReadings.isEmpty()) {
                return new ArrayList<>();
            }
            final long now = Forecast.tsl();
            final long timeshift = 500_000;
            long highest_bgreading_timestamp = -1; // most recent bgreading timestamp we have
            long trend_start_working = now - (1000 * 60 * 12); // 10 minutes // TODO MAKE PREFERENCE?
            if (bgReadings.size() > 0) {
                highest_bgreading_timestamp = bgReadings.get(0).timestamp;
                final long ms_since_last_reading = now - highest_bgreading_timestamp;
                if (ms_since_last_reading < 500000) {
                    trend_start_working -= ms_since_last_reading; // push back start of trend calc window
                    System.out.println("Pushed back trend start by: " + Forecast.qs(ms_since_last_reading / 1000) + " secs - last reading: " + Forecast.dateTimeText(highest_bgreading_timestamp));
                }
            }

            final long trendstart = trend_start_working;
            final long noise_trendstart = now - (1000 * 60 * 20); // 20 minutes // TODO MAKE PREFERENCE
            final long momentum_illustration_start = now - (1000 * 60 * 60 * 2); // 8 hours
            long oldest_noise_timestamp = now;
            long newest_noise_timestamp = 0;
            Forecast.TrendLine[] polys = new Forecast.TrendLine[5];

            polys[0] = new Forecast.PolyTrendLine(1);
            polys[1] = new Forecast.LogTrendLine();
            polys[2] = new Forecast.ExpTrendLine();
            polys[3] = new Forecast.PowerTrendLine();
            Forecast.TrendLine poly = null;

            final List<Double> polyxList = new ArrayList<>();
            final List<Double> polyyList = new ArrayList<>();
            long avg1start = now - (1000 * 60 * 30); // 1/2 hour
            double avg1value = 0;
            double avg2value = 0;
            int avg1counter = 0;
            int avg2counter = 0;
            for (final BgReading bgReading : bgReadings) {
                avg2counter++;
                avg2value += bgReading.calculated_value;
                if (bgReading.timestamp > avg1start) {
                    avg1counter++;
                    avg1value += bgReading.calculated_value;
                    polyxList.add((double) bgReading.timestamp);
                    polyyList.add(bgReading.calculated_value);
                    System.out.printf("Poly added -> Time: %s, Glucose: %.2f mg/dL%n", Forecast.dateTimeText(bgReading.timestamp), bgReading.calculated_value);
                }
            }
            // momentum
            try {
                System.out.println("moment Poly list size: " + polyxList.size());
                if (polyxList.size() > 1) {
                    final double[] polyys = Forecast.PolyTrendLine.toPrimitiveFromList(polyyList);
                    final double[] polyxs = Forecast.PolyTrendLine.toPrimitiveFromList(polyxList);

                    // set and evaluate poly curve models and select first best
                    double min_errors = 9999999;
                    for (Forecast.TrendLine this_poly : polys) {
                        if (this_poly != null) {
                            if (poly == null) poly = this_poly;
                            this_poly.setValues(polyys, polyxs);
                            if (this_poly.errorVarience() < min_errors) {
                                min_errors = this_poly.errorVarience();
                                poly = this_poly;
                                System.out.println("set forecast best model to: " + poly.getClass().getSimpleName() + " with varience of: " + Forecast.qs(poly.errorVarience(),14));
                            }
                        }
                    }
                    System.out.println("set forecast best model to: " + poly.getClass().getSimpleName() + " with varience of: " + Forecast.qs(poly.errorVarience(), 4));
                } else {
                    System.out.println("Not enough data for forecast model");
                }

            } catch (Exception e) {
                System.out.println(" Error with poly trend: " + e.toString());
            }

            try {
                // Create prediction for the next hour
                if (poly != null) {
                    double currentTime = System.currentTimeMillis();
                    double oneHourLater = currentTime + (60 * 60 * 1000); // 1 hour in milliseconds

                    List<PointValue> predictions = new ArrayList<>();

                    for (double timestamp = currentTime; timestamp <= oneHourLater; timestamp += 5 * 60 * 1000) { // 5-minute intervals
                        double predictedValue = poly.predict(timestamp);
                        System.out.println("Prediction for " + Forecast.dateTimeText((long) timestamp) + ": " + Forecast.qs(predictedValue));

                        PointValue point = new PointValue((float) (timestamp / FUZZER), (float) predictedValue);
                        predictions.add(point);
                    }

                    // Here you can use the 'predictions' list for further processing or visualization
                    System.out.println("Total predictions for next hour: " + predictions.size());
                    return predictions;
                } else {
                    System.out.println("No polynomial model available for prediction");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating prediction for next hour: " + e.toString());
            }
            return null;
        }
    }
}

