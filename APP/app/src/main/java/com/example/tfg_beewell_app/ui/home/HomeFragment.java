package com.example.tfg_beewell_app.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleCoroutineScope;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.lifecycle.ViewModelProvider;

import com.example.tfg_beewell_app.databinding.FragmentHomeBinding;
import com.example.tfg_beewell_app.ui.VitalData;
import com.example.tfg_beewell_app.utils.HealthConnectPermissionHelper;
import com.example.tfg_beewell_app.utils.HealthReader;
import com.example.tfg_beewell_app.utils.VitalsChangesListener;
import com.example.tfg_beewell_app.utils.VitalsWorker;
import com.google.android.flexbox.FlexboxLayout;

import java.util.concurrent.TimeUnit;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import kotlinx.coroutines.Job;
import kotlin.Unit;

public class HomeFragment extends Fragment {

    /* ──────────────────────── campos ─────────────────────── */
    private FragmentHomeBinding binding;
    private HealthConnectPermissionHelper permissionHelper;
    private Job vitalsJob;
    private boolean workerScheduled = false;
    private String userEmail;

    /* ──────────────────────── ciclo de vida ─────────────────────── */

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inf, container, false);
        View root = binding.getRoot();

        /* ViewModel de cabecera */
        HomeViewModel vm = new ViewModelProvider(
                requireActivity(),
                new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()))
                .get(HomeViewModel.class);

        vm.getText().observe(getViewLifecycleOwner(),
                t -> binding.textInfo.setText(t == null ? "" : t));

        /* email del usuario logeado */
        SharedPreferences sp = requireContext()
                .getSharedPreferences("user_session", Context.MODE_PRIVATE);
        userEmail = sp.getString("user_email", null);

        permissionHelper = new HealthConnectPermissionHelper(requireContext());

        /* Receiver para saber cuándo MainActivity concede permisos */
        IntentFilter filter = new IntentFilter("HC_PERMS_GRANTED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                    permsGrantedReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED      // ← solo visible en app
            );
        } else {
            requireContext().registerReceiver(permsGrantedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }

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

    @Override public void onStart() {
        super.onStart();
        ensureVitalsStarted();                // por si ya había permisos
    }

    @Override public void onStop() {
        super.onStop();
        if (vitalsJob != null) vitalsJob.cancel(null);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        try { requireContext().unregisterReceiver(permsGrantedReceiver); }
        catch (IllegalArgumentException ignored) {}
        binding = null;
    }

    /* ──────────────────────── lógica ─────────────────────── */

    /** Arranca listener y programa Worker sólo una vez */
    private void ensureVitalsStarted() {
        if (!permissionHelper.hasAllHcPermissionsSync()) return;

        if (!workerScheduled) {
            scheduleVitalsWorker();
            workerScheduled = true;
        }
        if (vitalsJob == null || vitalsJob.isCancelled()) startVitalsListener();
    }

    private void scheduleVitalsWorker() {
        /* uno inmediato */
        WorkManager.getInstance(requireContext())
                .enqueue(new OneTimeWorkRequest.Builder(VitalsWorker.class).build());

        /* y periódico cada 15 min */
        Constraints c = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest periodic = new PeriodicWorkRequest.Builder(
                VitalsWorker.class,
                15, TimeUnit.MINUTES)
                .setConstraints(c)
                .build();

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "vitals_upload",
                ExistingPeriodicWorkPolicy.KEEP,
                periodic);
    }

    private void startVitalsListener() {
        LifecycleCoroutineScope scope =
                LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner());

        vitalsJob = scope.launchWhenStarted((coScope, cont) ->
                VitalsChangesListener.INSTANCE.listen(
                        requireContext(),
                        cont2 -> {
                            Long bpm =
                                    HealthReader.getLastHeartRateBpmBlocking(requireContext());

                            requireActivity().runOnUiThread(() ->
                                    binding.textInfo.setText(
                                            bpm != null ? bpm + " bpm" : "—"));
                            return Unit.INSTANCE;
                        }, cont));
    }

    /* ──────────────────────── Broadcast ─────────────────────── */

    private final BroadcastReceiver permsGrantedReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            Toast.makeText(c,
                    "Permisos de salud concedidos 🎉", Toast.LENGTH_SHORT).show();
            ensureVitalsStarted();
        }
    };

    /* ──────────────────────── UI helpers ─────────────────────── */
    private void showVitals(VitalData vital) {
        FlexboxLayout card = binding.greenCard;
        card.removeAllViews();

        if (vital.getGlucoseValue() != null)
            card.addView(createVitalTextView(
                    String.valueOf(vital.getGlucoseValue()), "mg/dL"));

        if (vital.getHeartRate() != null)
            card.addView(createVitalTextView(
                    String.valueOf(vital.getHeartRate().intValue()), "bpm"));

        if (vital.getTemperature() != null)
            card.addView(createVitalTextView(
                    String.valueOf(vital.getTemperature()), "ºC"));

        if (vital.getOxygenSaturation() != null)
            card.addView(createVitalTextView(
                    String.valueOf(vital.getOxygenSaturation()), "% SpO₂"));
    }

    private LinearLayout createVitalTextView(String value, String unit) {
        Context ctx = requireContext();

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16, 0, 16, 0);

        TextView vTxt = new TextView(ctx);
        vTxt.setText(value);
        vTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        vTxt.setTextColor(ContextCompat.getColor(ctx, android.R.color.black));
        vTxt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        TextView uTxt = new TextView(ctx);
        uTxt.setText(unit);
        uTxt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        uTxt.setTextColor(ContextCompat.getColor(ctx, android.R.color.darker_gray));
        uTxt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        box.addView(vTxt);
        box.addView(uTxt);

        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(16, 0, 16, 4);
        box.setLayoutParams(lp);
        return box;
    }
}

