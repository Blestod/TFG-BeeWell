/* HomeFragment.java */
package com.example.tfg_beewell_app.ui.home;

import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;

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

import kotlinx.coroutines.Job;
import kotlin.Unit;
import lecho.lib.hellocharts.model.PointValue;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    private HealthConnectPermissionHelper permissionHelper;
    private Job vitalsJob;
    private String userEmail;

    /* cache */
    private VitalData  currentVitals;
    private List<Entry> currentPredictions = new ArrayList<>();

    private HomeViewModel viewModel;

    /* ─────────────────────────── LIFECYCLE ─────────────────────────── */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        /* ViewModel */
        viewModel = new ViewModelProvider(
                requireActivity(),
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication())
        ).get(HomeViewModel.class);

        viewModel.getInsights().observe(getViewLifecycleOwner(), ins -> {
            binding.textInfo.setText(ins.getVitalsRecommendation());
            binding.predictText.setText(ins.getPredictionRecommendation());
        });

        /* e-mail */
        SharedPreferences sp = requireContext()
                .getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userEmail = sp.getString("user_email", null);

        /* HC perms */
        permissionHelper = new HealthConnectPermissionHelper(requireContext());
        IntentFilter f = new IntentFilter("HC_PERMS_GRANTED");
        requireContext().registerReceiver(permsGrantedReceiver, f,
                Context.RECEIVER_NOT_EXPORTED);

        if (permissionHelper.hasAllHcPermissionsSync()) {
            readAndShowVitals();
        }
        return root;
    }

    @Override public void onStart() {
        super.onStart();
        binding.tipText.setText(TipManager.getDailyTip(requireContext()));
        if (permissionHelper.hasAllHcPermissionsSync()) startVitalsListener();
        drawChartAsync();               // ← build chart & markers
    }

    @Override public void onStop() {
        super.onStop();
        if (vitalsJob!=null) vitalsJob.cancel(null);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        try { requireContext().unregisterReceiver(permsGrantedReceiver); }
        catch (IllegalArgumentException ignore){}
        binding=null;
    }

    /* ─────────────────────────── CHART ─────────────────────────── */
    private void drawChartAsync(){
        new Thread(() -> {

            /* 1️⃣  prediction curve ---------------------------------- */
            List<PointValue> predPts = PredictionManager
                    .getPredictionForNextHour(requireContext());

            List<Entry> preds = new ArrayList<>();
            if (predPts!=null){
                for(PointValue p:predPts)
                    preds.add(new Entry((p.getX()*Forecast.FUZZER)/1000f, p.getY()));
            }
            currentPredictions = preds;

            /* 2️⃣  real glucose (last hour) -------------------------- */
            long now = System.currentTimeMillis();
            long oneHrAgo = now - TimeUnit.HOURS.toMillis(1);
            List<LocalGlucoseEntry> realRows = GlucoseDB.getInstance(requireContext())
                    .glucoseDao().getLast8Hours(oneHrAgo);

            List<Entry> real = new ArrayList<>();
            for(LocalGlucoseEntry e: realRows)
                real.add(new Entry(e.timestamp/1000f, (float)e.glucoseValue));

            /* 3️⃣  markers: meals / insulin / activity --------------- */
            long fromSec = oneHrAgo/1000;
            long toSec   = (now + TimeUnit.HOURS.toMillis(1))/1000;   // include forecast span

            LogbookDao dao = GlucoseDB.getInstance(requireContext()).logbookDao();
            List<Entry> mealDots     = new ArrayList<>();
            List<Entry> insulinDots  = new ArrayList<>();
            List<Entry> actDots      = new ArrayList<>();

            double minY = Double.MAX_VALUE;
            double maxY = Double.MIN_VALUE;
            for(Entry e:real){ minY=Math.min(minY,e.getY()); maxY=Math.max(maxY,e.getY()); }
            for(Entry e:preds){ minY=Math.min(minY,e.getY()); maxY=Math.max(maxY,e.getY()); }
            if(minY==Double.MAX_VALUE){ minY=80; maxY=200; }   // fallback

            float dotY = (float)(minY + (maxY-minY)*0.1);      // 10 % up from bottom

            for(LocalMealEntry m : dao.mealsBetween(fromSec,toSec))
                mealDots.add(new Entry(m.timestampSec, dotY));

            for(LocalInsulinEntry ins : dao.insulinBetween(fromSec,toSec))
                insulinDots.add(new Entry(ins.timestampSec, dotY));

            for(LocalActivityEntry a : dao.actsBetween(fromSec,toSec))
                actDots.add(new Entry(a.timestampSec, dotY));

            /* 4️⃣  UI ------------------------------------------------- */
            requireActivity().runOnUiThread(() -> renderChart(real,preds,
                    mealDots,insulinDots,actDots));
        }).start();
    }

    private void renderChart(List<Entry> real,
                             List<Entry> preds,
                             List<Entry> mealDots,
                             List<Entry> insulinDots,
                             List<Entry> actDots){

        LineChart chart = binding.glucoseChart;
        chart.clear();

        if(real.isEmpty() && preds.isEmpty()){
            chart.setNoDataText("Aún no hay datos de glucosa");
            chart.invalidate();
            return;
        }

        LineData data = new LineData();

        if(!real.isEmpty()){
            LineDataSet ds = new LineDataSet(real,"Historial");
            ds.setColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));
            ds.setLineWidth(2f);
            ds.setDrawCircles(false);
            ds.setDrawValues(false);
            data.addDataSet(ds);
        }

        if(!preds.isEmpty()){
            LineDataSet ds = new LineDataSet(preds,"Predicción");
            ds.setColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            ds.setLineWidth(2f);
            ds.setDrawCircles(false);
            ds.setDrawValues(false);
            ds.enableDashedLine(10,10,0);
            data.addDataSet(ds);
        }

        /* dots – meals (green) */
        if(!mealDots.isEmpty()){
            LineDataSet ds = new LineDataSet(mealDots,"Comidas");
            ds.setColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
            ds.setCircleColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
            ds.setCircleRadius(4f);
            ds.setLineWidth(0f);
            ds.setDrawValues(false);
            data.addDataSet(ds);
        }

        /* dots – insulin (grey) */
        if(!insulinDots.isEmpty()){
            LineDataSet ds = new LineDataSet(insulinDots,"Insulina");
            ds.setColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            ds.setCircleColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
            ds.setCircleRadius(4f);
            ds.setLineWidth(0f);
            ds.setDrawValues(false);
            data.addDataSet(ds);
        }

        /* dots – activity (blue) */
        if(!actDots.isEmpty()){
            LineDataSet ds = new LineDataSet(actDots,"Ejercicio");
            ds.setColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light));
            ds.setCircleColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light));
            ds.setCircleRadius(4f);
            ds.setLineWidth(0f);
            ds.setDrawValues(false);
            data.addDataSet(ds);
        }

        chart.setData(data);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setValueFormatter(new ValueFormatter(){
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            @Override public String getFormattedValue(float v){
                return sdf.format(new Date((long)v*1000));
            }
        });

        chart.invalidate();
    }

    /* ─────────────────────────── VITALS ─────────────────────────── */
    private void readAndShowVitals(){
        new Thread(() -> {
            VitalData d = HealthReader.getLatestVitalsBlocking(requireContext(), userEmail);
            if(d!=null) requireActivity().runOnUiThread(() -> showVitals(d));
        }).start();
    }

    private void startVitalsListener(){
        LifecycleCoroutineScope scope =
                LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner());
        vitalsJob = scope.launchWhenStarted((co,ct)->
                VitalsChangesListener.INSTANCE.listen(
                        requireContext(),
                        cont -> { Long bpm=HealthReader.getLastHeartRateBpmBlocking(requireContext());
                            requireActivity().runOnUiThread(() ->
                                    binding.textInfo.setText(bpm!=null?bpm+" bpm":"—"));
                            return Unit.INSTANCE; }, ct));
    }

    private final BroadcastReceiver permsGrantedReceiver = new BroadcastReceiver(){
        @Override public void onReceive(Context c, Intent i){
            Toast.makeText(c,"Permisos de salud concedidos 🎉",Toast.LENGTH_SHORT).show();
            readAndShowVitals(); startVitalsListener();
        }
    };

    /* ─────────────────────────── UI helpers ─────────────────────────── */
    private void showVitals(VitalData v){
        currentVitals = v;
        FlexboxLayout card = binding.greenCard;
        card.removeAllViews();
        addTxt(card, v.getGlucoseValue(),"mg/dL");
        addTxt(card, v.getHeartRate(),"bpm");
        addTxt(card, v.getTemperature(),"ºC");
        addTxt(card, v.getOxygenSaturation(),"% SpO₂");

        if(!currentPredictions.isEmpty())
            viewModel.fetchInsightsIfNeeded(currentVitals,currentPredictions);
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

        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(16, 0, 16, 4);
        col.setLayoutParams(lp);

        box.addView(col);
    }

}
