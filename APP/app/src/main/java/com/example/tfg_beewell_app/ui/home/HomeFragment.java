package com.example.tfg_beewell_app.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import androidx.lifecycle.LifecycleCoroutineScope;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.lifecycle.ViewModelProvider;

import com.example.tfg_beewell_app.databinding.FragmentHomeBinding;
import com.example.tfg_beewell_app.ui.VitalData;
import com.example.tfg_beewell_app.utils.HealthConnectPermissionHelper;
import com.example.tfg_beewell_app.utils.HealthReader;
import com.example.tfg_beewell_app.utils.Prefs;
import com.example.tfg_beewell_app.utils.VitalsChangesListener;
import com.google.android.flexbox.FlexboxLayout;
import androidx.health.connect.client.contracts.HealthPermissionsRequestContract;


import java.util.HashSet;
import java.util.Set;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Job;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;



public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ActivityResultLauncher<Set<? extends String>> permissionLauncher;
    private HealthConnectPermissionHelper permissionHelper;

    private Job vitalsJob;          // ⬅­­ guarda la corrutina


    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(requireActivity(),
                        new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()))
                        .get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        SharedPreferences prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("user_email", null);

        permissionHelper = new HealthConnectPermissionHelper(requireContext());

        permissionLauncher = registerForActivityResult(
                new HealthPermissionsRequestContract(),
                granted -> {
                    Set<String> requested = permissionHelper.getRequiredPermissions();
                    Set<String> missing = new HashSet<>(requested);
                    missing.removeAll(granted);

                    if (missing.isEmpty()) {
                        Prefs.markShown(requireContext());
                        Toast.makeText(getContext(), "✅ All permissions granted", Toast.LENGTH_SHORT).show();
                    } else {
                        StringBuilder sb = new StringBuilder("❌ Missing permissions:\n\n");
                        for (String perm : missing) sb.append("• ").append(perm).append("\n");

                        new AlertDialog.Builder(requireContext())
                                .setTitle("Permisos denegados")
                                .setMessage(sb.toString())
                                .setPositiveButton("OK", null)
                                .show();
                    }
                }
        );
        if (!Prefs.wasShown(requireContext())) {          // ← solo la 1ª vez
            permissionLauncher.launch(
                    permissionHelper.getRequiredPermissions());
        }

        // Texto superior (info del ViewModel)
        final TextView textView = binding.textInfo;
        homeViewModel.getText().observe(getViewLifecycleOwner(), text -> {
            if (text != null && !text.isEmpty()) {
                textView.setText(text);
            }
        });

        // print health connect reader

        new Thread(() -> {
            VitalData data = HealthReader.getLatestVitalsBlocking(requireContext(), userEmail);

            if (data != null) {
                requireActivity().runOnUiThread(() -> {
                    //Muestra los datos
                    showVitals(data);

                });
            }
        }).start();

        return root;
    }


    private void showVitals(VitalData vital) {
        FlexboxLayout greenCard = binding.greenCard;
        greenCard.removeAllViews();

        if (vital.getGlucoseValue() != null) {
            greenCard.addView(createVitalTextView(String.valueOf(vital.getGlucoseValue()), "mg/dL"));
        }

        if (vital.getHeartRate() != null) {
            greenCard.addView(createVitalTextView(String.valueOf(vital.getHeartRate().intValue()), "bpm"));
        }

        if (vital.getTemperature() != null) {
            greenCard.addView(createVitalTextView(String.valueOf(vital.getTemperature()), "ºC"));
        }

        if (vital.getOxygenSaturation() != null) {
            greenCard.addView(createVitalTextView(String.valueOf(vital.getOxygenSaturation()), "% SpO₂"));
        }

    }


    private LinearLayout createVitalTextView(String value, String unit) {
        Context context = getContext();

        // Container to hold value and unit vertically
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(16, 0, 16, 0);

        // Large value (e.g., 88, 74, 36.5)
        TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        valueView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        // Smaller unit label (e.g., mg/dL, bpm)
        TextView unitView = new TextView(context);
        unitView.setText(unit);
        unitView.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        unitView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        unitView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        // Add both to container
        container.addView(valueView);
        container.addView(unitView);

        // Set layout params
        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(16, 0, 16, 4);
        container.setLayoutParams(lp);

        return container;
    }

    @Override
    public void onStart() {
        super.onStart();

        LifecycleCoroutineScope scope =
                LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner());

        vitalsJob = scope.launchWhenStarted(
                /* --------------- λ de launchWhenStarted --------------- */
                (Function2<CoroutineScope, Continuation<? super Unit>, Object>)
                        (coroutineScope, cont) ->

                                /* --------------- llamada suspend --------------- */
                                VitalsChangesListener.INSTANCE.listen(
                                        requireContext(),

                                        /* ---- λ suspend onUpsert ---- */
                                        (Function1<Continuation<? super Unit>, Object>)
                                                (Continuation<? super Unit> cont2) -> {
                                                    Long bpm =
                                                            HealthReader.getLastHeartRateBpmBlocking(requireContext());

                                                    requireActivity().runOnUiThread(() -> {
                                                        String txt = bpm != null ? bpm + " bpm" : "—";
                                                        binding.textInfo.setText(txt);
                                                    });

                                                    // finalizar la λ suspend
                                                    return Unit.INSTANCE;
                                                },

                                        /* ---- continuation que nos pasa launchWhenStarted ---- */
                                        cont
                                )
        );
    }


    @Override
    public void onStop() {
        super.onStop();
        if (vitalsJob != null) vitalsJob.cancel(null);   // ok en Java
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
