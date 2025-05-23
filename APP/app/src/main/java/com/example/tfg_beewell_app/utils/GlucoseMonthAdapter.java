// GlucoseMonthAdapter.java
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

            // Capitalized month name
            String month = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH);
            month = month.substring(0, 1).toUpperCase() + month.substring(1);  // Ensure capitalized
            title.setText(month + " " + ym.getYear());

            chart.getDescription().setEnabled(false);
            chart.getAxisRight().setEnabled(false);
            chart.getLegend().setEnabled(false);

            XAxis x = chart.getXAxis();
            x.setPosition(XAxis.XAxisPosition.BOTTOM);
            x.setDrawGridLines(false);
            x.setGranularity(1f);
            x.setAxisMinimum(1f);
            x.setAxisMaximum(ym.lengthOfMonth());
            x.setLabelCount(ym.lengthOfMonth(), true);
            x.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float v) {
                    return String.valueOf((int) v);
                }
            });

            loadChartData();
            return view;
        }

        private void loadChartData() {
            Executors.newSingleThreadExecutor().execute(() -> {
                ZoneId zone = ZoneId.systemDefault();
                long fromSec = ym.atDay(1).atStartOfDay(zone).toEpochSecond();
                long toSec = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toEpochSecond();

                LocalGlucoseHistoryDao dao = GlucoseDB.getInstance(requireContext()).historyDao();
                List<LocalGlucoseHistoryEntry> rows = dao.range(fromSec, toSec);

                List<Entry> pts = new ArrayList<>();
                for (LocalGlucoseHistoryEntry e : rows) {
                    int day = Instant.ofEpochSecond(e.timestamp).atZone(zone).getDayOfMonth();
                    pts.add(new Entry(day, e.glucoseValue));
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
