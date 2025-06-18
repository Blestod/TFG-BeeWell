package com.example.tfg_beewell_app.ui.dashboard;

import android.content.*;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.*;
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

import java.util.*;

/**
 * • One card collapses to 80 dp (content height).
 * • The other card gets the remainder of the LinearLayout _after subtracting both cards’ margins_.
 * • Second tap on the same button returns to an even split.
 */
public class DashboardFragment extends Fragment {

    // ────────── views & state ──────────
    private CardView dietCard, exerciseCard;
    private ImageView dietBtn, exerBtn;
    private RecyclerView dietRv, exerRv;
    private boolean dietOpen = false, exerOpen = false;

    // ────────── broadcast from RecoWorker ──────────
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            loadFromPrefsAndFill();
            persistCurrentRecommendations();
        }
    };

    // ────────── lifecycle ──────────
    @Override public void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(updateReceiver,
                        new IntentFilter(RecoWorker.BROADCAST));
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             ViewGroup parent,
                             Bundle s) {

        View root = inf.inflate(R.layout.fragment_dashboard, parent, false);

        dietCard  = root.findViewById(R.id.dietCard);
        exerciseCard = root.findViewById(R.id.exerciseCard);
        dietBtn   = root.findViewById(R.id.dietToggleBtn);
        exerBtn   = root.findViewById(R.id.exerciseToggleBtn);
        dietRv    = root.findViewById(R.id.dietRecyclerView);
        exerRv    = root.findViewById(R.id.exerciseRecyclerView);

        dietRv.setLayoutManager(new LinearLayoutManager(getContext()));
        exerRv.setLayoutManager(new LinearLayoutManager(getContext()));

        // equal split after first layout pass
        root.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override public void onGlobalLayout() {
                        root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        expandBothNormal();
                    }
                });

        dietBtn.setOnClickListener(v -> toggleDiet());
        exerBtn.setOnClickListener(v -> toggleExercise());

        loadFromPrefsAndFill();
        ensureNotEmpty();
        return root;
    }

    @Override public void onDestroy() {
        LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(updateReceiver);
        super.onDestroy();
    }

    // ────────── expand / collapse logic ──────────
    private void toggleDiet() {
        if (dietOpen) {
            expandBothNormal();
            dietOpen = exerOpen = false;
        } else {
            expandOneCollapseOther(dietCard, exerciseCard);
            dietOpen = true;  exerOpen = false;
        }
        updateIcons();
    }

    private void toggleExercise() {
        if (exerOpen) {
            expandBothNormal();
            dietOpen = exerOpen = false;
        } else {
            expandOneCollapseOther(exerciseCard, dietCard);
            exerOpen = true;  dietOpen = false;
        }
        updateIcons();
    }

    /**
     * big card = remaining height, small card = 80 dp (content),
     * everything margin-aware.
     */
    private void expandOneCollapseOther(CardView big, CardView small) {
        View parent = (View) big.getParent();
        int parentH = parent.getHeight();

        // 1) small card visible height
        int collapsedContent = dpToPx(80);
        ViewGroup.MarginLayoutParams smlLp = (ViewGroup.MarginLayoutParams) small.getLayoutParams();
        int smallVisible = collapsedContent + smlLp.topMargin + smlLp.bottomMargin;

        // 2) big card margins
        ViewGroup.MarginLayoutParams bigLp = (ViewGroup.MarginLayoutParams) big.getLayoutParams();
        int bigMargins = bigLp.topMargin + bigLp.bottomMargin;

        // 3) expanded content height
        int expandedContent = parentH - smallVisible - bigMargins;
        expandedContent = Math.max(expandedContent, dpToPx(120)); // safety min

        setExactHeight(small, collapsedContent);
        setExactHeight(big,   expandedContent);
    }

    /** Even split of (parentHeight − both cards' total margins) */
    private void expandBothNormal() {
        View parent = (View) dietCard.getParent();
        int parentH = parent.getHeight();

        ViewGroup.MarginLayoutParams lpDiet = (ViewGroup.MarginLayoutParams) dietCard.getLayoutParams();
        ViewGroup.MarginLayoutParams lpExer = (ViewGroup.MarginLayoutParams) exerciseCard.getLayoutParams();
        int totalMargins = lpDiet.topMargin + lpDiet.bottomMargin +
                lpExer.topMargin + lpExer.bottomMargin;

        int remaining = parentH - totalMargins;
        int half = remaining / 2;

        setExactHeight(dietCard,     half);
        setExactHeight(exerciseCard, half);
    }

    // ────────── height helpers ──────────
    private static void setExactHeight(View v, int px) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        lp.height = px;
        v.setLayoutParams(lp);
    }
    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics()));
    }
    private void updateIcons() {
        dietBtn.setImageResource(dietOpen ? R.drawable.fold : R.drawable.unfold);
        exerBtn.setImageResource(exerOpen ? R.drawable.fold : R.drawable.unfold);
    }

    // ────────── data / persistence helpers (unchanged) ──────────
    private void loadFromPrefsAndFill() {
        SharedPreferences sp = requireContext()
                .getSharedPreferences(RecoWorker.PREFS, Context.MODE_PRIVATE);
        dietRv.setAdapter(new SimpleStringAdapter(
                splitLines(sp.getString(RecoWorker.KEY_DIET, ""))));
        exerRv.setAdapter(new SimpleStringAdapter(
                splitLines(sp.getString(RecoWorker.KEY_EXER, ""))));
    }

    private void persistCurrentRecommendations() {
        SharedPreferences sp = requireContext()
                .getSharedPreferences(RecoWorker.PREFS, Context.MODE_PRIVATE);
        String diet = sp.getString(RecoWorker.KEY_DIET, "");
        String exer = sp.getString(RecoWorker.KEY_EXER, "");
        if (!diet.isEmpty())
            PredictionPoster.post(requireContext(), diet, "diet", null, null);
        if (!exer.isEmpty())
            PredictionPoster.post(requireContext(), exer, "exercise", null, null);
    }

    private void ensureNotEmpty() {
        SharedPreferences sp = requireContext()
                .getSharedPreferences(RecoWorker.PREFS, Context.MODE_PRIVATE);
        if (!sp.contains(RecoWorker.KEY_DIET) &&
                !sp.contains(RecoWorker.KEY_EXER)) {
            WorkManager.getInstance(requireContext())
                    .enqueue(new OneTimeWorkRequest.Builder(RecoWorker.class).build());
        }
    }

    private static List<String> splitLines(String block) {
        if (block.isEmpty()) return Collections.singletonList("(no data yet)");
        List<String> out = new ArrayList<>();
        for (String p : block.split("\\.")) {
            String s = p.trim();
            if (!s.isEmpty())
                out.add(s.endsWith(".") ? s : s + ".");
        }
        return out;
    }

    // ────────── simple bullet adapter ──────────
    private static class SimpleStringAdapter
            extends RecyclerView.Adapter<SimpleStringAdapter.Holder> {

        private final List<String> items;
        SimpleStringAdapter(List<String> items) { this.items = items; }

        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View row = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_reco, p, false);
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
            final TextView bullet, text;
            Holder(@NonNull View v) {
                super(v);
                bullet = v.findViewById(R.id.bullet);
                text   = v.findViewById(R.id.lineText);
                int c = ContextCompat.getColor(v.getContext(), R.color.beeBlack);
                bullet.setTextColor(c);
                text.setTextColor(c);
            }
        }
    }
}
