package com.example.tfg_beewell_app.ui.notifications;

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
import com.example.tfg_beewell_app.databinding.FragmentNotificationsBinding;

import java.util.Arrays;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private boolean isAchievementsExpanded = false;

    private final int COLLAPSED_HEIGHT_DP = 80;
    private final int EXPANDED_HEIGHT_DP = 300;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        CardView achievementsCard = root.findViewById(R.id.gotCard);
        ImageView toggleBtn = root.findViewById(R.id.achievementsToggleBtn);

        // Set initial height
        setCardHeight(achievementsCard, COLLAPSED_HEIGHT_DP);

        toggleBtn.setOnClickListener(v -> {
            if (!isAchievementsExpanded) {
                setCardHeight(achievementsCard, EXPANDED_HEIGHT_DP);
                toggleBtn.setImageResource(R.drawable.fold);
            } else {
                setCardHeight(achievementsCard, COLLAPSED_HEIGHT_DP);
                toggleBtn.setImageResource(R.drawable.unfold);
            }
            isAchievementsExpanded = !isAchievementsExpanded;
        });

        // === Chart ViewPager2 Setup ===
        ViewPager2 chartPager = root.findViewById(R.id.chartViewPager);
        TextView noChartsText = root.findViewById(R.id.noChartsText);

        List<Integer> charts = Arrays.asList(); // Empty for now

        if (charts.isEmpty()) {
            chartPager.setVisibility(View.GONE);
            noChartsText.setVisibility(View.VISIBLE);
        } else {
            chartPager.setVisibility(View.VISIBLE);
            noChartsText.setVisibility(View.GONE);
            chartPager.setAdapter(new ChartPagerAdapter(charts));
        }

        return root;
    }

    private void setCardHeight(CardView card, int heightDp) {
        ViewGroup.LayoutParams params = card.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                heightDp,
                getResources().getDisplayMetrics()
        );
        card.setLayoutParams(params);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // === ViewPager2 Adapter ===
    public static class ChartPagerAdapter extends RecyclerView.Adapter<ChartPagerAdapter.ChartViewHolder> {
        private final List<Integer> chartImages;

        public ChartPagerAdapter(List<Integer> chartImages) {
            this.chartImages = chartImages;
        }

        @NonNull
        @Override
        public ChartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            return new ChartViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ChartViewHolder holder, int position) {
            ((ImageView) holder.itemView).setImageResource(chartImages.get(position));
        }

        @Override
        public int getItemCount() {
            return chartImages.size();
        }

        static class ChartViewHolder extends RecyclerView.ViewHolder {
            public ChartViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
