package com.example.tfg_beewell_app.ui.log;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.tfg_beewell_app.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogFragment extends Fragment {

    public LogFragment() {
        super(R.layout.fragment_log);
    }

    private String userEmail;

    @Override
    public void onViewCreated(@NonNull View v, Bundle s) {
        /* 1 ───────── load email from prefs ───────── */
        SharedPreferences sp = requireContext()
                .getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userEmail = sp.getString("user_email", null);
        if (userEmail == null) {
            Toast.makeText(requireContext(), "No user e-mail saved", Toast.LENGTH_SHORT).show();
            return;
        }

        v.findViewById(R.id.btnCloseLogs)
                .setOnClickListener(view -> requireActivity().finish());


        /* 2 ───────── set up tabs + pages ───────── */
        ViewPager2 vp = v.findViewById(R.id.viewPagerLogs);
        TabLayout  tb = v.findViewById(R.id.tabLayoutLogs);

        vp.setAdapter(new FragmentStateAdapter(this) {
            @NonNull @Override
            public Fragment createFragment(int pos) {
                return Page.newInstance(pos, userEmail);     // pass email to each page
            }
            @Override public int getItemCount() { return 3; }
        });

        new TabLayoutMediator(tb, vp, (tab, pos) -> tab.setText(
                pos == 0 ? "INSULIN" : pos == 1 ? "MEALS" : "EXERCISE"
        )).attach();
    }

    /*────────────────────────────── inner page fragment ──────────────────────────────*/
    public static class Page extends Fragment {

        private static final String ARG_KIND  = "kind";   // 0,1,2
        private static final String ARG_EMAIL = "email";

        /** factory */
        static Page newInstance(int kind, String email) {
            Bundle b = new Bundle();
            b.putInt(ARG_KIND,  kind);
            b.putString(ARG_EMAIL, email);
            Page f = new Page();
            f.setArguments(b);
            return f;
        }

        /* constructor binds fragment_logs_list.xml */
        public Page() { super(R.layout.fragment_logs_list); }

        private int          kind;      // 0 insulin | 1 meal | 2 act
        private String       email;
        private LogsAdapter  adapter;

        @Override
        public void onViewCreated(@NonNull View v, Bundle s) {
            kind  = getArguments().getInt(ARG_KIND);
            email = getArguments().getString(ARG_EMAIL);

            adapter = new LogsAdapter(kind, this::deleteItem);


            RecyclerView rv = v.findViewById(R.id.recyclerLogs);
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            rv.setAdapter(adapter);

            load();          // fetch from server
        }

        /* -------- networking: LIST -------- */
        private void load() {
            /* pick the right endpoint */
            Call call = (kind == 0) ? Net.api.ins(email)
                    : (kind == 1) ? Net.api.meals(email)
                    : Net.api.acts(email);

            String tag = (kind == 0) ? "INSULIN" : (kind == 1) ? "MEAL" : "ACT";
            Log.d("LOGS-REQ", "▶ " + tag + " request for " + email);

            /* enqueue with a raw Callback so the compiler is happy */
            call.enqueue(new Callback() {
                @Override public void onResponse(Call c, Response r) {
                    Log.d("LOGS-REQ", "✔ " + tag + " HTTP " + r.code());
                    if (r.body() != null) {
                        List<?> list = (List<?>) r.body();
                        Log.d("LOGS-REQ", "   items received: " + list.size());
                        adapter.submitList(new ArrayList<>(list));
                    } else {
                        Log.w("LOGS-REQ", "✖ body is null");
                    }
                }
                @Override public void onFailure(Call c, Throwable t) {
                    Log.e("LOGS-REQ", "☠ " + tag + " failed → " + t);
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        }


        /* -------- networking: DELETE -------- */
        private void deleteItem(Object item) {
            Call<Void> c =
                    item instanceof Net.InsulinLog ? Net.api.delIns(((Net.InsulinLog) item).injected_id) :
                            item instanceof Net.MealLog    ? Net.api.delMeal(((Net.MealLog) item).meal_id) :
                                    Net.api.delAct(((Net.ActLog) item).activity_id);

            c.enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> r) { load(); }
                @Override public void onFailure(Call<Void> call, Throwable t) { }
            });
        }
    }
}
