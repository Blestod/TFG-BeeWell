package com.example.tfg_beewell_app.utils;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.tfg_beewell_app.R;
import com.example.tfg_beewell_app.local.GlucoseDB;
import com.example.tfg_beewell_app.local.LocalGlucoseHistoryDao;
import com.example.tfg_beewell_app.local.LocalGlucoseHistoryEntry;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class GlucoseMonthAdapter extends FragmentStateAdapter {
    private static final String ARG_MONTH = "arg_month";
    private final List<YearMonth> months;

    public GlucoseMonthAdapter(@NonNull Fragment host, List<YearMonth> months) {
        super(host);
        this.months = months;
    }

    @Override public int getItemCount() { return months.size(); }

    @NonNull @Override
    public Fragment createFragment(int position) {
        YearMonth ym = months.get(position);
        ChartHolderFragment frag = new ChartHolderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MONTH, ym.toString());
        frag.setArguments(args);
        return frag;
    }

    public static class ChartHolderFragment extends Fragment {
        private LineChart chart;
        private YearMonth ym;
        private long fromSec;
        private long toSec;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ym = YearMonth.parse(requireArguments().getString(ARG_MONTH));
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inf, ViewGroup container, Bundle savedInstanceState) {
            View view = inf.inflate(R.layout.fragment_chart_holder, container, false);
            chart = view.findViewById(R.id.chart);
            TextView title = view.findViewById(R.id.monthTitle);

            // Título en inglés (May 2025)
            String month = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
            month = month.substring(0, 1).toUpperCase() + month.substring(1);
            title.setText(month + " " + ym.getYear());

            chart.getDescription().setEnabled(false);
            chart.getAxisRight().setEnabled(false);
            chart.getLegend().setEnabled(false);

            // Intervalo de tiempo
            ZoneId zone = ZoneId.systemDefault();
            fromSec = ym.atDay(1).atStartOfDay(zone).toEpochSecond();
            toSec   = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toEpochSecond();

            XAxis x = chart.getXAxis();
            x.setPosition(XAxis.XAxisPosition.BOTTOM);
            x.setDrawGridLines(false);
            x.setGranularity(3600f); // 1 hora en segundos
            x.setAxisMinimum(fromSec);
            x.setAxisMaximum(toSec);
            x.setValueFormatter(new AdaptiveDateFormatter(chart));

            loadChartData();
            return view;
        }

        public class AdaptiveDateFormatter extends ValueFormatter {
            private final LineChart chart;
            private final ZoneId zone = ZoneId.systemDefault();

            private final DateTimeFormatter fullFmt = DateTimeFormatter.ofPattern("dd HH'h'");
            private final DateTimeFormatter dayOnlyFmt = DateTimeFormatter.ofPattern("dd");

            public AdaptiveDateFormatter(LineChart chart) {
                this.chart = chart;
            }

            @Override
            public String getFormattedValue(float value) {
                float range = chart.getVisibleXRange(); // en segundos
                Instant instant = Instant.ofEpochSecond((long) value);

                if (range < 3 * 24 * 3600f) { // si estás viendo menos de 3 días → mostrar hora
                    return fullFmt.format(instant.atZone(zone));
                } else {
                    return dayOnlyFmt.format(instant.atZone(zone));
                }
            }
        }


        private void loadChartData() {
            Executors.newSingleThreadExecutor().execute(() -> {
                LocalGlucoseHistoryDao dao = GlucoseDB.getInstance(requireContext()).historyDao();
                List<LocalGlucoseHistoryEntry> rows = dao.range(fromSec, toSec);

                List<Entry> pts = new ArrayList<>();
                for (LocalGlucoseHistoryEntry e : rows) {
                    pts.add(new Entry(e.timestamp, e.glucoseValue)); // eje X = timestamp (segundos)
                }

                requireActivity().runOnUiThread(() -> {
                    LineDataSet ds = new LineDataSet(pts, "");
                    ds.setColor(Color.parseColor("#03A9F4"));
                    ds.setLineWidth(2f);
                    ds.setDrawCircles(false);
                    ds.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
                    chart.setData(new LineData(ds));
                    chart.invalidate();
                });
            });
        }
    }
}
