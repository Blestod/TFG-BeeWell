package com.example.tfg_beewell_app.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
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
import com.example.tfg_beewell_app.utils.VitalsChangesListener;
import com.google.android.flexbox.FlexboxLayout;

import java.util.List;
import java.util.concurrent.TimeUnit;

import kotlinx.coroutines.Job;
import kotlin.Unit;

import com.example.tfg_beewell_app.utils.PredictionManager;
import lecho.lib.hellocharts.model.PointValue;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import com.example.tfg_beewell_app.local.GlucoseDB;
import com.example.tfg_beewell_app.local.LocalGlucoseEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HealthConnectPermissionHelper permissionHelper;
    private Job vitalsJob;
    private String userEmail;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inf, container, false);
        View root = binding.getRoot();

        HomeViewModel vm = new ViewModelProvider(
                requireActivity(),
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()))
                .get(HomeViewModel.class);

        vm.getText().observe(getViewLifecycleOwner(),
                t -> binding.textInfo.setText(t == null ? "" : t));

        SharedPreferences sp = requireContext()
                .getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userEmail = sp.getString("user_email", null);

        permissionHelper = new HealthConnectPermissionHelper(requireContext());

        // Register for HC_PERMS_GRANTED broadcast
        IntentFilter filter = new IntentFilter("HC_PERMS_GRANTED");
        requireContext().registerReceiver(permsGrantedReceiver,
                filter, Context.RECEIVER_NOT_EXPORTED);

        // If user already granted, read and listen now
        if (permissionHelper.hasAllHcPermissionsSync()) {
            readAndShowVitals();
        }

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        binding.tipText.setText(TipManager.getDailyTip(requireContext()));

        // only start listener if perms granted
        if (permissionHelper.hasAllHcPermissionsSync()) {
            startVitalsListener();
        }

        // prediction + chart logic unchanged
        new Thread(() -> {
            List<PointValue> predicciones;
            List<Entry> predPoints = new ArrayList<>();
            try {
                predicciones = PredictionManager.getPredictionForNextHour(requireContext());
                if (predicciones != null) {
                    for (PointValue p : predicciones) {
                        float x = (p.getX() * Forecast.FUZZER) / 1000f;
                        predPoints.add(new Entry(x, p.getY()));
                    }
                }
            } catch (Exception ex) {
                predicciones = List.of();
            }

            List<LocalGlucoseEntry> reales = GlucoseDB.getInstance(requireContext())
                    .glucoseDao()
                    .getLast8Hours(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));

            List<Entry> realPoints = new ArrayList<>();
            if (reales != null) {
                for (LocalGlucoseEntry e : reales) {
                    realPoints.add(new Entry(e.timestamp / 1000f, (float) e.glucoseValue));
                }
            }

            requireActivity().runOnUiThread(() -> {
                LineChart chart = binding.glucoseChart;

                if (realPoints.isEmpty() && predPoints.isEmpty()) {
                    chart.clear();
                    chart.setNoDataText("Aún no hay datos de glucosa");
                    chart.setNoDataTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
                    chart.invalidate();
                    return;
                }

                LineDataSet setReal = new LineDataSet(realPoints, "Historial");
                setReal.setColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));
                setReal.setLineWidth(2f);
                setReal.setDrawCircles(false);

                LineDataSet setPred = new LineDataSet(predPoints, "Predicción");
                setPred.setColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                setPred.setLineWidth(2f);
                setPred.setDrawCircles(false);
                setPred.enableDashedLine(10, 10, 0);

                LineData lineData = new LineData();
                if (!realPoints.isEmpty()) lineData.addDataSet(setReal);
                if (!predPoints.isEmpty()) lineData.addDataSet(setPred);

                chart.setData(lineData);
                chart.getDescription().setEnabled(false);
                chart.setTouchEnabled(true);
                chart.setPinchZoom(true);

                XAxis xAxis = chart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setValueFormatter(new ValueFormatter() {
                    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    @Override
                    public String getFormattedValue(float value) {
                        return sdf.format(new Date((long) value * 1000));
                    }
                });

                chart.invalidate();
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
        } catch (IllegalArgumentException ignored) {}
        binding = null;
    }

    private void readAndShowVitals() {
        new Thread(() -> {
            VitalData data =
                    HealthReader.getLatestVitalsBlocking(requireContext(), userEmail);
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
                            Long bpm = HealthReader
                                    .getLastHeartRateBpmBlocking(requireContext());
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

        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(16, 0, 16, 4);
        box.setLayoutParams(lp);
        return box;
    }
}
