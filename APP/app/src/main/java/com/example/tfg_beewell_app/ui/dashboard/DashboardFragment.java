package com.example.tfg_beewell_app.ui.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.tfg_beewell_app.R;
import com.example.tfg_beewell_app.utils.PredictionPoster;
import com.example.tfg_beewell_app.utils.RecoWorker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dashboard with diet & exercise recommendation cards.
 * Receives updates that RecoWorker broadcasts and reloads the lists.
 */
public class DashboardFragment extends Fragment {

    /* --------------------------------------------------  UI config  */
    private static final int NORMAL_HEIGHT_DP    = 320;
    private static final int EXPANDED_HEIGHT_DP  = 560;
    private static final int COLLAPSED_HEIGHT_DP =  80;

    /* --------------------------------------------------  view refs  */
    private RecyclerView dietRv, exerRv;
    private boolean isDietExpanded    = false;
    private boolean isExerciseExpanded = false;

    /* --------------------------------------------------  lifecycle  */
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            loadFromPrefsAndFill();
            persistCurrentRecommendations();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // listen for broadcasts that RecoWorker sends after finishing
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(updateReceiver,
                        new IntentFilter(RecoWorker.BROADCAST));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        /* ----------  get handles to widgets present in your layout  */
        CardView dietCard        = root.findViewById(R.id.dietCard);
        CardView exerciseCard    = root.findViewById(R.id.exerciseCard);
        ImageView dietToggleBtn  = root.findViewById(R.id.dietToggleBtn);
        ImageView exerciseToggleBtn = root.findViewById(R.id.exerciseToggleBtn);

        dietRv = root.findViewById(R.id.dietRecyclerView);
        exerRv = root.findViewById(R.id.exerciseRecyclerView);

        dietRv.setLayoutManager(new LinearLayoutManager(getContext()));
        exerRv.setLayoutManager(new LinearLayoutManager(getContext()));

        /* ----------  original toggle-height logic  */
        setCardHeight(dietCard, NORMAL_HEIGHT_DP);
        setCardHeight(exerciseCard, NORMAL_HEIGHT_DP);

        dietToggleBtn.setOnClickListener(v -> {
            if (!isDietExpanded) {
                setCardHeight(dietCard,     EXPANDED_HEIGHT_DP);
                setCardHeight(exerciseCard, COLLAPSED_HEIGHT_DP);
                dietToggleBtn.setImageResource(R.drawable.fold);
                exerciseToggleBtn.setImageResource(R.drawable.unfold);
                isDietExpanded = true;
                isExerciseExpanded = false;
            } else {
                resetCardHeights(dietCard, exerciseCard,
                        dietToggleBtn, exerciseToggleBtn);
            }
        });

        exerciseToggleBtn.setOnClickListener(v -> {
            if (!isExerciseExpanded) {
                setCardHeight(exerciseCard, EXPANDED_HEIGHT_DP);
                setCardHeight(dietCard,     COLLAPSED_HEIGHT_DP);
                exerciseToggleBtn.setImageResource(R.drawable.fold);
                dietToggleBtn.setImageResource(R.drawable.unfold);
                isExerciseExpanded = true;
                isDietExpanded = false;
            } else {
                resetCardHeights(dietCard, exerciseCard,
                        dietToggleBtn, exerciseToggleBtn);
            }
        });

        /* ----------  first paint + trigger if empty  */
        loadFromPrefsAndFill();
        ensureNotEmpty();

        return root;
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(updateReceiver);
        super.onDestroy();
    }

    /* --------------------------------------------------  helper  */
    private void setCardHeight(CardView card, int heightDp) {
        ViewGroup.LayoutParams params = card.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                heightDp,
                getResources().getDisplayMetrics());
        card.setLayoutParams(params);
    }

    private void resetCardHeights(CardView dietCard, CardView exerciseCard,
                                  ImageView dietBtn, ImageView exerciseBtn) {
        setCardHeight(dietCard,     NORMAL_HEIGHT_DP);
        setCardHeight(exerciseCard, NORMAL_HEIGHT_DP);
        dietBtn.setImageResource(R.drawable.unfold);
        exerciseBtn.setImageResource(R.drawable.unfold);
        isDietExpanded    = false;
        isExerciseExpanded = false;
    }

    /** Reads the last recommendations from SharedPreferences and fills the lists. */
    private void loadFromPrefsAndFill() {
        SharedPreferences sp = requireContext()
                .getSharedPreferences(RecoWorker.PREFS, Context.MODE_PRIVATE);

        String diet = sp.getString(RecoWorker.KEY_DIET, "");
        String exer = sp.getString(RecoWorker.KEY_EXER, "");

        // 1) Split into lines…
        List<String> dietLines = splitLines(diet);
        List<String> exerLines = splitLines(exer);

        // 2) Set adapters as before
        dietRv.setAdapter(new SimpleStringAdapter(dietLines));
        exerRv.setAdapter(new SimpleStringAdapter(exerLines));


    }

    /**
     * Persist the full diet & exercise blocks once, when new data arrives.
     */
    private void persistCurrentRecommendations() {
        SharedPreferences sp = requireContext()
                .getSharedPreferences(RecoWorker.PREFS, Context.MODE_PRIVATE);

        String diet = sp.getString(RecoWorker.KEY_DIET, "");
        String exer = sp.getString(RecoWorker.KEY_EXER, "");

        if (!diet.isEmpty()) {
            PredictionPoster.post(requireContext(), diet, "diet", null, null);
        }
        if (!exer.isEmpty()) {
            PredictionPoster.post(requireContext(), exer, "exercise", null, null);
        }
    }


    /** Fire a one-shot work if nothing is stored yet (e.g. fresh install). */
    private void ensureNotEmpty() {
        SharedPreferences sp = requireContext()
                .getSharedPreferences(RecoWorker.PREFS, Context.MODE_PRIVATE);

        if (!sp.contains(RecoWorker.KEY_DIET) &&
                !sp.contains(RecoWorker.KEY_EXER)) {

            WorkManager.getInstance(requireContext()).enqueue(
                    new OneTimeWorkRequest.Builder(RecoWorker.class).build());
        }
    }

    /**
     * Splits a block of recommendations into individual sentences.
     * Each sentence becomes its own list item with a trailing period.
     */
    private static List<String> splitLines(String block) {
        if (block.isEmpty())
            return Collections.singletonList("(no data yet)");

        List<String> sentences = new ArrayList<>();
        for (String part : block.split("\\.")) {   // split on every period
            String sentence = part.trim();
            if (sentence.isEmpty()) continue;
            // ensure the period is kept
            if (!sentence.endsWith(".")) sentence += ".";
            sentences.add(sentence);
        }
        return sentences;
    }

    /* --------------------------------------------------  adapter with nicer row layout  */
    private static class SimpleStringAdapter
            extends RecyclerView.Adapter<SimpleStringAdapter.Holder> {

        private final List<String> items;
        SimpleStringAdapter(List<String> items) { this.items = items; }

        @NonNull @Override public Holder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reco, parent, false);
            return new Holder(row);
        }

        @Override public void onBindViewHolder(@NonNull Holder h, int pos) {
            String line = items.get(pos).trim();
            if (line.isEmpty()) {
                h.bullet.setVisibility(View.INVISIBLE);
                h.text.setText("");
            } else {
                h.bullet.setVisibility(View.VISIBLE);
                h.text.setText(line);
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView bullet;
            final TextView text;
            Holder(@NonNull View v) {
                super(v);
                bullet = v.findViewById(R.id.bullet);
                text   = v.findViewById(R.id.lineText);
                int beeBlack = ContextCompat.getColor(v.getContext(), R.color.beeBlack);
                bullet.setTextColor(beeBlack);
                text.setTextColor(beeBlack);
            }
        }
    }
}
