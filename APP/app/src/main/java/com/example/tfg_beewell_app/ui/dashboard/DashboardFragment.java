package com.example.tfg_beewell_app.ui.dashboard;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.tfg_beewell_app.R;

public class DashboardFragment extends Fragment {

    private boolean isDietExpanded = false;
    private boolean isExerciseExpanded = false;

    private final int NORMAL_HEIGHT_DP = 320;
    private final int EXPANDED_HEIGHT_DP = 580;
    private final int COLLAPSED_HEIGHT_DP = 80;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        CardView dietCard = root.findViewById(R.id.dietCard);
        CardView exerciseCard = root.findViewById(R.id.exerciseCard);
        ImageView dietToggleBtn = root.findViewById(R.id.dietToggleBtn);
        ImageView exerciseToggleBtn = root.findViewById(R.id.exerciseToggleBtn);

        // Initial fixed height
        setCardHeight(dietCard, NORMAL_HEIGHT_DP);
        setCardHeight(exerciseCard, NORMAL_HEIGHT_DP);

        dietToggleBtn.setOnClickListener(v -> {
            if (!isDietExpanded) {
                setCardHeight(dietCard, EXPANDED_HEIGHT_DP);
                setCardHeight(exerciseCard, COLLAPSED_HEIGHT_DP);
                dietToggleBtn.setImageResource(R.drawable.x); // close icon
                exerciseToggleBtn.setImageResource(R.drawable.agrandar);
                isDietExpanded = true;
                isExerciseExpanded = false;
            } else {
                resetCardHeights(dietCard, exerciseCard, dietToggleBtn, exerciseToggleBtn);
            }
        });

        exerciseToggleBtn.setOnClickListener(v -> {
            if (!isExerciseExpanded) {
                setCardHeight(exerciseCard, EXPANDED_HEIGHT_DP);
                setCardHeight(dietCard, COLLAPSED_HEIGHT_DP);
                exerciseToggleBtn.setImageResource(R.drawable.x);
                dietToggleBtn.setImageResource(R.drawable.agrandar);
                isExerciseExpanded = true;
                isDietExpanded = false;
            } else {
                resetCardHeights(dietCard, exerciseCard, dietToggleBtn, exerciseToggleBtn);
            }
        });

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

    private void resetCardHeights(CardView dietCard, CardView exerciseCard,
                                  ImageView dietBtn, ImageView exerciseBtn) {
        setCardHeight(dietCard, NORMAL_HEIGHT_DP);
        setCardHeight(exerciseCard, NORMAL_HEIGHT_DP);
        dietBtn.setImageResource(R.drawable.agrandar);
        exerciseBtn.setImageResource(R.drawable.agrandar);
        isDietExpanded = false;
        isExerciseExpanded = false;
    }
}
