package com.example.tfg_beewell_app.ui.home;

import android.content.*;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.tfg_beewell_app.*;
import com.example.tfg_beewell_app.databinding.FragmentHomeBinding;
import com.example.tfg_beewell_app.local.*;
import com.example.tfg_beewell_app.ui.VitalData;
import com.example.tfg_beewell_app.utils.*;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.flexbox.FlexboxLayout;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import kotlin.Unit;
import kotlinx.coroutines.Job;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HealthConnectPermissionHelper permissionHelper;
    private Job vitalsJob;
    private String userEmail;
    private List<Entry> currentPredictions = new ArrayList<>();
    private VitalData currentVitals;
    private HomeViewModel viewModel;

    private final BroadcastReceiver permsGrantedReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            Toast.makeText(c, "Permisos de salud concedidos 🎉", Toast.LENGTH_SHORT).show();
            readAndShowVitals();
            drawChartAsync();
            startVitalsListener();
        }
    };

    private final BroadcastReceiver logUpdatedReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            drawChartAsync();
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(requireActivity(), new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())).get(HomeViewModel.class);
        viewModel.getInsights().observe(getViewLifecycleOwner(), ins -> {
            binding.textInfo.setText(ins.getVitalsRecommendation());
            binding.predictText.setText(ins.getPredictionRecommendation());
        });

        SharedPreferences sp = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userEmail = sp.getString("user_email", null);

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(permsGrantedReceiver, new IntentFilter("HC_PERMS_GRANTED"));

        permissionHelper = new HealthConnectPermissionHelper(requireContext());
        if (permissionHelper.hasAllHcPermissionsSync()) readAndShowVitals();

        return binding.getRoot();
    }

    @Override public void onStart() {
        super.onStart();
        binding.tipText.setText(TipManager.getDailyTip(requireContext()));
        if (permissionHelper.hasAllHcPermissionsSync()) startVitalsListener();
        drawChartAsync();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(requireContext());
        lbm.registerReceiver(logUpdatedReceiver, new IntentFilter("LOG_UPDATED"));
        lbm.registerReceiver(vitalsUpdatedReceiver, new IntentFilter("VITALS_UPDATED"));
    }

    @Override public void onStop() {
        super.onStop();
        if (vitalsJob != null) vitalsJob.cancel(null);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(requireContext());
        lbm.unregisterReceiver(logUpdatedReceiver);
        lbm.unregisterReceiver(vitalsUpdatedReceiver);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(permsGrantedReceiver);
        binding = null;
    }

    private void drawChartAsync() {
        new Thread(() -> {
            List<Entry> preds = new ArrayList<>();
            List<lecho.lib.hellocharts.model.PointValue> pts = PredictionManager.getPredictionForNextHour(requireContext());
            if (pts != null) {
                for (lecho.lib.hellocharts.model.PointValue p : pts) {
                    float x = (p.getX() * Forecast.FUZZER) / 1000f;
                    preds.add(new Entry(x, p.getY()));
                }
            }
            currentPredictions = preds;

            long now = System.currentTimeMillis();
            long oneHrAgo = now - TimeUnit.HOURS.toMillis(1);
            List<LocalGlucoseEntry> realRows = GlucoseDB.getInstance(requireContext()).glucoseDao().getLast8Hours(oneHrAgo);
            List<Entry> real = new ArrayList<>();
            for (LocalGlucoseEntry e : realRows) real.add(new Entry(e.timestamp / 1000f, (float) e.glucoseValue));

            long from = oneHrAgo / 1000;
            long to = (now + TimeUnit.HOURS.toMillis(1)) / 1000;
            LogbookDao dao = GlucoseDB.getInstance(requireContext()).logbookDao();

            List<Entry> mealDots = new ArrayList<>();
            for (LocalMealEntry m : dao.mealsBetween(from, to))
                mealDots.add(new Entry((float)m.timestampSec, getClosestYValue(real, m.timestampSec)));
            List<Entry> insulinDots = new ArrayList<>();
            for (LocalInsulinEntry i : dao.insulinBetween(from, to))
                insulinDots.add(new Entry((float)i.timestampSec, getClosestYValue(real, i.timestampSec)));
            List<Entry> actDots = new ArrayList<>();
            for (LocalActivityEntry a : dao.actsBetween(from, to))
                actDots.add(new Entry((float)a.timestampSec, getClosestYValue(real, a.timestampSec)));

            requireActivity().runOnUiThread(() -> renderChart(real, preds, mealDots, insulinDots, actDots));
        }).start();
    }

    private void renderChart(List<Entry> real, List<Entry> preds, List<Entry> mealDots, List<Entry> insulinDots, List<Entry> actDots) {
        LineChart chart = binding.glucoseChart;
        chart.clear();
        if (real.isEmpty() && preds.isEmpty()) {
            chart.setNoDataText("Aún no hay datos de glucosa");
            chart.invalidate();
            return;
        }

        LineData data = new LineData();
        if (!real.isEmpty()) data.addDataSet(makeLine(real, "Historial", android.R.color.holo_blue_dark));
        if (!preds.isEmpty()) {
            LineDataSet ds = makeLine(preds, "Predicción", android.R.color.holo_red_dark);
            ds.enableDashedLine(10,10,0);
            data.addDataSet(ds);
        }
        data.addDataSet(makeDots(mealDots, "Meals", android.R.color.holo_green_dark));
        data.addDataSet(makeDots(insulinDots, "Insulin", android.R.color.darker_gray));
        data.addDataSet(makeDots(actDots, "Exercise", android.R.color.holo_blue_light));

        chart.setData(data);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            @Override public String getFormattedValue(float v) {
                return sdf.format(new Date((long)(v * 1000)));
            }
        });

        chart.invalidate();
    }

    private LineDataSet makeLine(List<Entry> entries, String label, int colorRes) {
        LineDataSet ds = new LineDataSet(entries, label);
        ds.setColor(ContextCompat.getColor(requireContext(), colorRes));
        ds.setLineWidth(2f);
        ds.setDrawCircles(false);
        ds.setDrawValues(false);
        return ds;
    }

    private LineDataSet makeDots(List<Entry> dots, String label, int colorRes) {
        LineDataSet ds = new LineDataSet(dots, label);
        int color = ContextCompat.getColor(requireContext(), colorRes);
        ds.setColor(color);
        ds.setCircleColor(color);
        ds.setCircleRadius(4f);
        ds.setLineWidth(0f);
        ds.setDrawValues(false);
        return ds;
    }

    private float getClosestYValue(List<Entry> entries, long timestampSec) {
        float targetX = (float) timestampSec;
        Entry closest = null;
        float minDiff = Float.MAX_VALUE;
        for (Entry e : entries) {
            float diff = Math.abs(e.getX() - targetX);
            if (diff < minDiff) {
                minDiff = diff;
                closest = e;
            }
        }
        return (closest != null) ? closest.getY() : 100f;
    }

    private void readAndShowVitals() {
        new Thread(() -> {
            VitalData latest = null;
            int retries = 3;
            while (retries-- > 0) {
                latest = HealthReader.getLatestVitalsBlocking(requireContext(), userEmail);
                if (latest != null && latest.getGlucoseValue() != null && latest.getGlucoseValue() > 0) break;
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }
            final VitalData finalData = latest;
            if (finalData != null && finalData.getGlucoseValue() != null && finalData.getGlucoseValue() > 0) {
                requireActivity().runOnUiThread(() -> {
                    showVitals(finalData);
                    currentVitals = finalData;
                    if (!currentPredictions.isEmpty()) viewModel.fetchInsightsIfNeeded(currentVitals, currentPredictions);
                });
            }
        }).start();
    }

    private void startVitalsListener() {
        LifecycleCoroutineScope scope = LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner());
        vitalsJob = scope.launchWhenStarted((co, ct) -> VitalsChangesListener.INSTANCE.listen(
                requireContext(),
                cont -> {
                    VitalData latest = HealthReader.getLatestVitalsBlocking(requireContext(), userEmail);
                    if (latest != null && latest.getGlucoseValue() != null && latest.getGlucoseValue() > 0) {
                        requireActivity().runOnUiThread(() -> showVitals(latest));
                    }
                    return Unit.INSTANCE;
                },
                ct
        ));
    }


    private final BroadcastReceiver vitalsUpdatedReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            readAndShowVitals();
            drawChartAsync();
        }
    };

    private void showVitals(VitalData v) {
        currentVitals = v;
        if (binding == null) return;

        FlexboxLayout card = binding.greenCard;
        card.removeAllViews();

        // Arrow logic based on prediction trend
        String glucoseDirection = "";
        if (!currentPredictions.isEmpty()) {
            float last = currentPredictions.get(currentPredictions.size() - 1).getY();
            float now = currentPredictions.get(0).getY();
            glucoseDirection = last > now ? "↑" : last < now ? "↓" : "→";
        }

        addTxt(card, v.getGlucoseValue(), "mg/dL " + glucoseDirection);
        addTxt(card, v.getHeartRate() != null ? v.getHeartRate().intValue() : null, "bpm");
        addTxt(card, v.getTemperature(), "ºC");
        addTxt(card, v.getOxygenSaturation(), "% SpO₂");
    }

    private void addTxt(FlexboxLayout box, Number val, String unit) {
        if (val == null) return;
        Context ctx = requireContext();
        int beeBlack = ContextCompat.getColor(ctx, R.color.beeBlack);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(16, 0, 16, 0);

        TextView vTxt = new TextView(ctx);
        vTxt.setText(String.valueOf(val));
        vTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        vTxt.setTextColor(beeBlack);
        vTxt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        TextView uTxt = new TextView(ctx);
        uTxt.setText(unit);
        uTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        uTxt.setTextColor(beeBlack);
        uTxt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        col.addView(vTxt);
        col.addView(uTxt);

        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(16, 0, 16, 4);
        col.setLayoutParams(lp);
        box.addView(col);
    }
}
