package com.example.tfg_beewell_app.ui.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.tfg_beewell_app.R;
import com.example.tfg_beewell_app.local.GlucoseDB;
import com.example.tfg_beewell_app.local.LocalGlucoseHistoryDao;
import com.example.tfg_beewell_app.utils.GlucoseMonthAdapter;
import com.example.tfg_beewell_app.utils.MonthlyInsightWorker;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationsFragment extends Fragment {
    private ExecutorService bg;
    private ViewPager2 pager;
    private List<YearMonth> months;
    private TextView interpretationText;
    private String userEmail;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        bg = Executors.newSingleThreadExecutor();
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);

        pager = root.findViewById(R.id.chartViewPager);
        interpretationText = root.findViewById(R.id.interpretationText);

        loadMonthRangeAndAdapter(root);
        return root;
    }

    private void loadMonthRangeAndAdapter(View root) {
        bg.execute(() -> {
            SharedPreferences session = requireContext()
                    .getSharedPreferences("user_session", Context.MODE_PRIVATE);
            userEmail = session.getString("user_email", null);
            if (userEmail == null) return;

            LocalGlucoseHistoryDao dao = GlucoseDB
                    .getInstance(requireContext())
                    .historyDao();
            long minTs = dao.getMinTimestamp();
            long maxTs = dao.getMaxTimestamp();
            if (minTs == 0 && maxTs == 0) return;

            ZoneId zone = ZoneId.systemDefault();
            YearMonth start = YearMonth.from(Instant.ofEpochSecond(minTs).atZone(zone));
            YearMonth end = YearMonth.from(Instant.ofEpochSecond(maxTs).atZone(zone));

            List<YearMonth> list = new ArrayList<>();
            for (YearMonth cur = start; !cur.isAfter(end); cur = cur.plusMonths(1)) {
                list.add(cur);
            }

            YearMonth now = YearMonth.now(zone);
            list.removeIf(ym -> ym.equals(now));

            months = list;

            requireActivity().runOnUiThread(() -> {
                if (months.isEmpty()) return;
                int lastPos = months.size() - 1;
                GlucoseMonthAdapter adapter = new GlucoseMonthAdapter(this, months);
                pager.setAdapter(adapter);
                pager.setOffscreenPageLimit(2);

                TabLayout dots = root.findViewById(R.id.observationDots);
                new TabLayoutMediator(dots, pager,
                        (tab, position) -> tab.setIcon(R.drawable.dot_selector)
                ).attach();

                pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        int summaryPos = position == 0 ? 0 : position - 1;
                        updateMonthlySummary(summaryPos);
                    }
                });

                pager.setCurrentItem(lastPos, false);
                updateMonthlySummary(lastPos == 0 ? 0 : lastPos - 1);

                YearMonth toSummarize = months.get(lastPos == 0 ? 0 : lastPos - 1);
                String summaryKey = "summary_" + toSummarize.atDay(1);
                SharedPreferences summaryPrefs = requireContext()
                        .getSharedPreferences("monthly_summary_" + userEmail, Context.MODE_PRIVATE);
                if (!summaryPrefs.contains(summaryKey)) {
                    OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(MonthlyInsightWorker.class)
                            .setInputData(new Data.Builder()
                                    .putString("email", userEmail)
                                    .putString("month", toSummarize.toString())
                                    .build())
                            .build();
                    WorkManager.getInstance(requireContext()).enqueue(req);
                }
            });
        });
    }

    private void updateMonthlySummary(int position) {
        if (months == null || position >= months.size()) return;
        YearMonth ym = months.get(position);
        String key = "summary_" + ym.atDay(1).toString();

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("monthly_summary_" + userEmail, Context.MODE_PRIVATE);
        String summary = prefs.getString(key, "No summary available for this month.");
        interpretationText.setText(summary);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bg.shutdown();
    }
}
