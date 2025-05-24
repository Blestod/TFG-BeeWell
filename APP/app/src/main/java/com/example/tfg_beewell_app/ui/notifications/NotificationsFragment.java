package com.example.tfg_beewell_app.ui.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.tfg_beewell_app.R;
import com.example.tfg_beewell_app.local.GlucoseDB;
import com.example.tfg_beewell_app.local.LocalGlucoseHistoryDao;
import com.example.tfg_beewell_app.local.LocalGlucoseHistoryEntry;
import com.example.tfg_beewell_app.utils.GlucoseMonthAdapter;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationsFragment extends Fragment {
    private static final int COLLAPSED_DP = 80, EXPANDED_DP = 300;
    private ExecutorService bg;
    private boolean expanded;
    private ViewPager2 pager;
    private List<YearMonth> months;
    private TextView interpretationText;

    @Override @NonNull
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        bg = Executors.newSingleThreadExecutor();
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        // ACHIEVEMENTS TOGGLE SETUP
        CardView card = root.findViewById(R.id.achievementsCard);
        ImageView toggle = root.findViewById(R.id.achievementsToggleBtn);
        RecyclerView rvAchievements = root.findViewById(R.id.rvAchievements);

        // Initial collapsed state
        setViewHeight(rvAchievements, COLLAPSED_DP);

        toggle.setOnClickListener(v -> {
            expanded = !expanded;
            setViewHeight(rvAchievements, expanded ? EXPANDED_DP : COLLAPSED_DP);
            toggle.setImageResource(expanded ? R.drawable.fold : R.drawable.unfold);
        });

        // CHART SETUP
        pager = root.findViewById(R.id.chartViewPager);
        interpretationText = root.findViewById(R.id.interpretationText);
        loadMonthRangeAndAdapter();

        return root;
    }

    private void loadMonthRangeAndAdapter() {
        bg.execute(() -> {
            LocalGlucoseHistoryDao dao =
                    GlucoseDB.getInstance(requireContext()).historyDao();
            long minSec = dao.getMinTimestamp();  // seconds
            long maxSec = dao.getMaxTimestamp();
            if (minSec == 0 && maxSec == 0) return;

            ZoneId zone = ZoneId.systemDefault();
            YearMonth start = YearMonth.from(Instant.ofEpochSecond(minSec).atZone(zone));
            YearMonth end   = YearMonth.from(Instant.ofEpochSecond(maxSec).atZone(zone));

            List<YearMonth> list = new ArrayList<>();
            YearMonth cur = start;
            while (!cur.isAfter(end)) {
                list.add(cur);
                cur = cur.plusMonths(1);
            }
            months = list;

            requireActivity().runOnUiThread(() -> {
                GlucoseMonthAdapter adapter = new GlucoseMonthAdapter(this, months);
                pager.setAdapter(adapter);
                pager.setOffscreenPageLimit(2);

                pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        updateMonthlySummary(position);
                    }
                });

                pager.setCurrentItem(months.size() - 1, false);
                updateMonthlySummary(months.size() - 1);
            });
        });
    }

    private void updateMonthlySummary(int position) {
        if (months == null || position >= months.size()) return;
        YearMonth ym = months.get(position);
        String key = "summary_" + ym.atDay(1).toString(); // e.g., 2025-05-01
        SharedPreferences prefs = requireContext().getSharedPreferences("monthly_summary", Context.MODE_PRIVATE);
        String summary = prefs.getString(key, "No summary available for this month.");
        interpretationText.setText(summary);
    }

    private void setViewHeight(View view, int dp) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
        view.setLayoutParams(params);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        bg.shutdown();
    }
}
