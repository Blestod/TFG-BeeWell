package com.example.tfg_beewell_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.tfg_beewell_app.databinding.ActivityMainBinding;
import com.example.tfg_beewell_app.local.GlucoseDB;
import com.example.tfg_beewell_app.utils.FullSyncWorker;
import com.example.tfg_beewell_app.utils.HealthConnectPermissionHelper;
import com.example.tfg_beewell_app.utils.VitalsWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.health.connect.client.contracts.HealthPermissionsRequestContract;
import kotlin.Unit;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private HealthConnectPermissionHelper permHelper;
    private ActivityResultLauncher<Set<? extends String>> hcPermLauncher;
    private ActivityResultLauncher<String> bgPermLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── full‐screen setup ──
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupMenu();
        setupNavigation();
        setupFab();
        hideSystemUI();
        logNextWorkerExecution();

        // ── Health Connect permissions ──
        permHelper = new HealthConnectPermissionHelper(this);
        hcPermLauncher = registerForActivityResult(
                new HealthPermissionsRequestContract(),
                granted -> {
                    if (granted.containsAll(permHelper.getHcPermissions())) {
                        Log.d("HC-PERM", "✔️ permisos HC concedidos");
                        if (!permHelper.bgPermissionNeeded()) {
                            notifyPermsGranted();
                        }
                        maybeRequestBgPermission();
                    } else {
                        Log.w("HC-PERM", "❌ faltan permisos HC");
                    }
                }
        );
        bgPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    Log.d("HC-PERM-BG",
                            granted ? "✔️ 2º plano concedido" : "❌ 2º plano denegado");
                    if (granted) notifyPermsGranted();
                }
        );

        // ── 1) If local history empty, do one‐off full sync ──
        Executors.newSingleThreadExecutor().execute(() -> {
            long count = GlucoseDB
                    .getInstance(getApplicationContext())
                    .historyDao()
                    .count();
            if (count == 0) {
                OneTimeWorkRequest fullReq =
                        new OneTimeWorkRequest.Builder(FullSyncWorker.class)
                                .build();
                WorkManager.getInstance(this)
                        .enqueue(fullReq);
            }
        });

        // ── 2) Schedule periodic vitals upload every 15′ ──
        PeriodicWorkRequest vitalsReq =
                new PeriodicWorkRequest.Builder(
                        VitalsWorker.class,
                        15, TimeUnit.MINUTES
                ).build();
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "vitals_upload",
                        ExistingPeriodicWorkPolicy.KEEP,
                        vitalsReq
                );

        // ── 3) (Optional) monthly full‐history refresh ──
        scheduleMonthlyFullSync();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAndRequestPermissions();
        maybeRequestNotificationPermission();
    }

    private void checkAndRequestPermissions() {
        permHelper.hasAllHcPermissions(allGranted -> {
            if (!allGranted) {
                hcPermLauncher.launch(permHelper.getHcPermissions());
            } else {
                maybeRequestBgPermission();
            }
            return Unit.INSTANCE;
        });
    }

    private void maybeRequestBgPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && permHelper.bgPermissionNeeded()
                && checkSelfPermission(permHelper.getBgPermission())
                != PackageManager.PERMISSION_GRANTED
        ) {
            bgPermLauncher.launch(permHelper.getBgPermission());
        } else {
            notifyPermsGranted();
        }
    }

    private void notifyPermsGranted() {
        sendBroadcast(new Intent("HC_PERMS_GRANTED"));
        Intent svc = new Intent(this,
                com.example.tfg_beewell_app.utils.HealthDataService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
    }

    private void setupMenu() {
        ImageView menuButton = findViewById(R.id.menuButton);
        FrameLayout overlay  = findViewById(R.id.menuOverlay);
        menuButton.setOnClickListener(v -> {
            overlay.setVisibility(View.VISIBLE);
            overlay.setTranslationY(-1000f);
            overlay.animate().translationY(0f).setDuration(300).start();
            hideSystemUI();
        });
        findViewById(R.id.closeMenuBtn).setOnClickListener(v -> overlay.setVisibility(View.GONE));
        overlay.setOnClickListener(v -> overlay.setVisibility(View.GONE));
        findViewById(R.id.menu_profile).setOnClickListener(v -> {
            overlay.setVisibility(View.GONE);
            startActivity(new Intent(this, ProfileActivity.class));
        });
        findViewById(R.id.menu_terms).setOnClickListener(v -> {
            overlay.setVisibility(View.GONE);
            startActivity(new Intent(this, TermsReadActivity.class));
        });
        findViewById(R.id.menu_aboutus).setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
            prefs.edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setupNavigation() {
        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController nav = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        AppBarConfiguration cfg = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard,
                R.id.navigation_notifications, R.id.navigation_chat
        ).build();
        NavigationUI.setupWithNavController(navView, nav);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container),
                (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars() |
                                    WindowInsetsCompat.Type.ime()
                    );
                    v.setPadding(0,0,0, bars.bottom);
                    return insets;
                }
        );
    }

    private void setupFab() {
        CardView fab = findViewById(R.id.fab_add);
        NavController nav = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        fab.setOnClickListener(v ->
                new AddEntryDialog().show(getSupportFragmentManager(), "AddEntryDialog")
        );
        nav.addOnDestinationChangedListener((c,d,a)->
                fab.setVisibility(d.getId()==R.id.navigation_chat?View.GONE:View.VISIBLE)
        );
    }

    private void logNextWorkerExecution() {
        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData("vitals_upload")
                .observe(this, workInfos -> {
                    if (workInfos == null || workInfos.isEmpty()) return;
                    for (androidx.work.WorkInfo info : workInfos) {
                        if (info.getState() == androidx.work.WorkInfo.State.ENQUEUED) {
                            long nextMs = info.getNextScheduleTimeMillis();
                            if (nextMs > 0) {
                                Log.d("VitalsWorker",
                                        "⏰ Próxima ejecución: " + new java.util.Date(nextMs));
                            }
                        }
                    }
                });
    }


    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    1002
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==1002) {
            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                Log.d("NOTIF","✔️ Notification granted");
            } else {
                Log.w("NOTIF","❌ Notification denied");
            }
            checkAndRequestPermissions();
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setStatusBarColor     (android.graphics.Color.TRANSPARENT);
    }

    private void scheduleMonthlyFullSync() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next1st = now.withDayOfMonth(1)
                .plusMonths(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        long delayMs = Duration.between(now, next1st).toMillis();

        PeriodicWorkRequest monthReq =
                new PeriodicWorkRequest.Builder(FullSyncWorker.class, 30, TimeUnit.DAYS)
                        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                        .build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "monthly_full_sync",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        monthReq
                );

        // 📅 Log para confirmar la fecha y hora programada
        Log.d("MonthlySync", "✅ Sync mensual programado para: " + next1st.toString());
    }


    @Override protected void onResume()   { super.onResume();   hideSystemUI(); }
    @Override public    void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }
}
