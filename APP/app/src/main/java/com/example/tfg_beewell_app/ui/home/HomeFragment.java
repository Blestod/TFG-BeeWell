package com.example.tfg_beewell_app.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleCoroutineScope;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.lifecycle.ViewModelProvider;

import com.example.tfg_beewell_app.Forecast;
import com.example.tfg_beewell_app.TipManager;
import com.example.tfg_beewell_app.databinding.FragmentHomeBinding;
import com.example.tfg_beewell_app.ui.VitalData;
import com.example.tfg_beewell_app.utils.HealthConnectPermissionHelper;
import com.example.tfg_beewell_app.utils.HealthReader;
import com.example.tfg_beewell_app.utils.PredictionManager;
import com.example.tfg_beewell_app.utils.VitalsChangesListener;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.flexbox.FlexboxLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import kotlinx.coroutines.Job;
import kotlin.Unit;
import lecho.lib.hellocharts.model.PointValue;

import com.example.tfg_beewell_app.local.GlucoseDB;
import com.example.tfg_beewell_app.local.LocalGlucoseEntry;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    private HealthConnectPermissionHelper permissionHelper;
    private Job vitalsJob;
    private String userEmail;

    // Para no pedir más de 1 vez por hora
    private long lastInsightsFetch = 0L;

    // Últimos datos
    private VitalData currentVitals;
    private List<Entry> currentPredictions = new ArrayList<>();

    // ViewModel
    private HomeViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ➊ Instancia el ViewModel
        viewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())
        ).get(HomeViewModel.class);

        // ➌ Observa las recomendaciones
        viewModel.getInsights().observe(getViewLifecycleOwner(), insights -> {
            Log.d(TAG, "🔔 got insights: " + insights);
            binding.textInfo.setText(insights.getVitalsRecommendation());
            binding.predictText.setText(insights.getPredictionRecommendation());
        });

        // Lee email de las prefs
        SharedPreferences sp = requireContext()
                .getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userEmail = sp.getString("user_email", null);

        permissionHelper = new HealthConnectPermissionHelper(requireContext());

        // Registra receptor de permisos
        IntentFilter filter = new IntentFilter("HC_PERMS_GRANTED");
        requireContext().registerReceiver(permsGrantedReceiver,
                filter, Context.RECEIVER_NOT_EXPORTED);

        if (permissionHelper.hasAllHcPermissionsSync()) {
            readAndShowVitals();
        }

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        binding.tipText.setText(TipManager.getDailyTip(requireContext()));

        if (permissionHelper.hasAllHcPermissionsSync()) {
            startVitalsListener();
        }

        // Dibuja gráfica y luego pide insights
        new Thread(() -> {
            // 1) calcula predicciones
            List<PointValue> predsPts;
            List<Entry> preds = new ArrayList<>();
            try {
                predsPts = PredictionManager.getPredictionForNextHour(requireContext());
                if (predsPts != null) {
                    for (PointValue p : predsPts) {
                        float x = (p.getX() * Forecast.FUZZER) / 1000f;
                        preds.add(new Entry(x, p.getY()));
                    }
                }
            } catch (Exception ex) {
                predsPts = List.of();
            }

            currentPredictions = preds;

            // 2) obtiene reales
            List<LocalGlucoseEntry> reales = GlucoseDB.getInstance(requireContext())
                    .glucoseDao()
                    .getLast8Hours(System.currentTimeMillis()
                            - TimeUnit.HOURS.toMillis(1));

            List<Entry> realEntries = new ArrayList<>();
            if (reales != null) {
                for (LocalGlucoseEntry e : reales) {
                    realEntries.add(new Entry(e.timestamp / 1000f,
                            (float) e.glucoseValue));
                }
            }

            // 3) actualiza UI
            requireActivity().runOnUiThread(() -> {
                LineChart chart = binding.glucoseChart;
                if (realEntries.isEmpty() && preds.isEmpty()) {
                    chart.clear();
                    chart.setNoDataText("Aún no hay datos de glucosa");
                    chart.setNoDataTextColor(
                            ContextCompat.getColor(requireContext(),
                                    android.R.color.darker_gray));
                    chart.invalidate();
                } else {
                    LineDataSet setReal = new LineDataSet(realEntries, "Historial");
                    setReal.setColor(ContextCompat.getColor(requireContext(),
                            android.R.color.holo_blue_dark));
                    setReal.setLineWidth(2f);
                    setReal.setDrawCircles(false);

                    LineDataSet setPred = new LineDataSet(preds, "Predicción");
                    setPred.setColor(ContextCompat.getColor(requireContext(),
                            android.R.color.holo_red_dark));
                    setPred.setLineWidth(2f);
                    setPred.setDrawCircles(false);
                    setPred.enableDashedLine(10, 10, 0);

                    LineData lineData = new LineData();
                    if (!realEntries.isEmpty()) lineData.addDataSet(setReal);
                    if (!preds.isEmpty()) lineData.addDataSet(setPred);

                    chart.setData(lineData);
                    chart.getDescription().setEnabled(false);
                    chart.setTouchEnabled(true);
                    chart.setPinchZoom(true);

                    XAxis xAxis = chart.getXAxis();
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setValueFormatter(new ValueFormatter() {
                        private final SimpleDateFormat sdf =
                                new SimpleDateFormat("HH:mm", Locale.getDefault());
                        @Override public String getFormattedValue(float value) {
                            return sdf.format(new Date((long) value * 1000));
                        }
                    });

                    chart.invalidate();
                }

                // ➍ Pregunta al ViewModel
                if (currentVitals != null && !currentPredictions.isEmpty()) {
                    Log.d(TAG, "📤 asking ViewModel for insights");
                    viewModel.fetchInsightsIfNeeded(currentVitals,
                            currentPredictions);
                }
            });
        }).start();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (vitalsJob != null) vitalsJob.cancel(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            requireContext().unregisterReceiver(permsGrantedReceiver);
        } catch (IllegalArgumentException ignored) { }
        binding = null;
    }

    private void readAndShowVitals() {
        new Thread(() -> {
            VitalData data =
                    HealthReader.getLatestVitalsBlocking(requireContext(),
                            userEmail);
            if (data != null) {
                requireActivity().runOnUiThread(() -> showVitals(data));
            }
        }).start();
    }

    private void startVitalsListener() {
        LifecycleCoroutineScope scope =
                LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner());

        vitalsJob = scope.launchWhenStarted((coScope, cont) ->
                VitalsChangesListener.INSTANCE.listen(
                        requireContext(),
                        cont2 -> {
                            Long bpm =
                                    HealthReader.getLastHeartRateBpmBlocking(requireContext());
                            requireActivity().runOnUiThread(() ->
                                    binding.textInfo.setText(
                                            bpm != null ? bpm + " bpm" : "—"));
                            return Unit.INSTANCE;
                        }, cont));
    }

    private final BroadcastReceiver permsGrantedReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            Toast.makeText(c, "Permisos de salud concedidos 🎉",
                    Toast.LENGTH_SHORT).show();
            readAndShowVitals();
            startVitalsListener();
        }
    };

    private void showVitals(VitalData vital) {
        currentVitals = vital;
        Log.d(TAG, "🔥 showVitals: nuevos vitals=" + vital);
        FlexboxLayout card = binding.greenCard;
        card.removeAllViews();
        if (vital.getGlucoseValue() != null)
            card.addView(createVitalTextView(
                    String.valueOf(vital.getGlucoseValue()), "mg/dL"));
        if (vital.getHeartRate() != null)
            card.addView(createVitalTextView(
                    String.valueOf(vital.getHeartRate().intValue()), "bpm"));
        if (vital.getTemperature() != null)
            card.addView(createVitalTextView(
                    String.valueOf(vital.getTemperature()), "ºC"));
        if (vital.getOxygenSaturation() != null)
            card.addView(createVitalTextView(
                    String.valueOf(vital.getOxygenSaturation()), "% SpO₂"));

        // Dispara la petición al ViewModel tan pronto tengas datos
        if (!currentPredictions.isEmpty()) {
            Log.d(TAG, "📤 showVitals → asking ViewModel");
            viewModel.fetchInsightsIfNeeded(currentVitals, currentPredictions);
        }
    }

    private LinearLayout createVitalTextView(String value, String unit) {
        Context ctx = requireContext();
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16, 0, 16, 0);

        TextView vTxt = new TextView(ctx);
        vTxt.setText(value);
        vTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        vTxt.setTextColor(
                ContextCompat.getColor(ctx, android.R.color.black));
        vTxt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        TextView uTxt = new TextView(ctx);
        uTxt.setText(unit);
        uTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        uTxt.setTextColor(
                ContextCompat.getColor(ctx, android.R.color.darker_gray));
        uTxt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        box.addView(vTxt);
        box.addView(uTxt);

        FlexboxLayout.LayoutParams lp =
                new FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(16, 0, 16, 4);
        box.setLayoutParams(lp);
        return box;
    }
}
