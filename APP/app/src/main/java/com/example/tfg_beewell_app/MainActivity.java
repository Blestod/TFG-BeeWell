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
import androidx.work.WorkManager;

import com.example.tfg_beewell_app.databinding.ActivityMainBinding;
import com.example.tfg_beewell_app.utils.HealthConnectPermissionHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Set;

import androidx.health.connect.client.contracts.HealthPermissionsRequestContract;
import kotlin.Unit;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private HealthConnectPermissionHelper permHelper;
    private ActivityResultLauncher<Set<? extends String>> hcPermLauncher;
    private ActivityResultLauncher<String> bgPermLauncher;

    /* ─────────────────────────────── */
    /*  Ciclo de vida                  */
    /* ─────────────────────────────── */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupMenu();
        setupNavigation();
        setupFab();
        hideSystemUI();
        logNextWorkerExecution();

        /* ─── Permisos Health Connect ─── */
        permHelper = new HealthConnectPermissionHelper(this);

        hcPermLauncher = registerForActivityResult(
                new HealthPermissionsRequestContract(),
                granted -> {
                    if (granted.containsAll(permHelper.getHcPermissions())) {
                        Log.d("HC‑PERM", "✔️ permisos HC concedidos");
                        // si no hace falta permiso en 2º plano ► todo listo
                        if (!permHelper.bgPermissionNeeded()) {
                            notifyPermsGranted();
                        }
                        // si hace falta, lo pedimos
                        maybeRequestBgPermission();
                    } else {
                        Log.w("HC‑PERM", "❌ faltan permisos HC");
                    }
                });

        bgPermLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    Log.d("HC‑PERM‑BG",
                            granted ? "✔️ 2º plano concedido" : "❌ 2º plano denegado");
                    if (granted) notifyPermsGranted();
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAndRequestPermissions();             // ⬅️ lanzamos chequeo
        maybeRequestNotificationPermission();
    }

    /* ─────────────────────────────── */
    /*  Permisos                       */
    /* ─────────────────────────────── */

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

    /**  Pide permiso de 2.º plano si procede; si ya está concedido, avisa  */
    private void maybeRequestBgPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                permHelper.bgPermissionNeeded()) {
            if (checkSelfPermission(permHelper.getBgPermission())
                    != PackageManager.PERMISSION_GRANTED) {
                bgPermLauncher.launch(permHelper.getBgPermission());
                return;             // esperamos resultado
            }
        }
        // aquí llegamos si NO hace falta pedirlo o ya estaba concedido
        notifyPermsGranted();
    }

    /*  ───────────── NUEVO ─────────────
        Avisa al resto de la app de que ya
        tenemos todos los permisos       */
    private void notifyPermsGranted() {
        sendBroadcast(new Intent("HC_PERMS_GRANTED"));

        // Start the HealthDataService
        Intent serviceIntent = new Intent(this, com.example.tfg_beewell_app.utils.HealthDataService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    /* ─────────────────────────────── */
    /*  UI / Navegación                */
    /* ─────────────────────────────── */

    private void setupMenu() {
        ImageView menuButton = findViewById(R.id.menuButton);
        FrameLayout menuOverlay = findViewById(R.id.menuOverlay);

        menuButton.setOnClickListener(v -> {
            menuOverlay.setVisibility(View.VISIBLE);
            menuOverlay.setTranslationY(-1000f);
            menuOverlay.animate().translationY(0f).setDuration(300).start();
            hideSystemUI();
        });

        findViewById(R.id.closeMenuBtn).setOnClickListener(v -> menuOverlay.setVisibility(View.GONE));
        menuOverlay.setOnClickListener(v -> menuOverlay.setVisibility(View.GONE));

        findViewById(R.id.menu_profile).setOnClickListener(v -> {
            menuOverlay.setVisibility(View.GONE);
            startActivity(new Intent(this, ProfileActivity.class));
        });

        findViewById(R.id.menu_terms).setOnClickListener(v -> {
            menuOverlay.setVisibility(View.GONE);
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
        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        AppBarConfiguration config = new AppBarConfiguration.Builder(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications,
                R.id.navigation_chat
        ).build();
        NavigationUI.setupWithNavController(navView, navController);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.ime());
                    v.setPadding(0, 0, 0, bars.bottom);
                    return insets;
                });
    }

    private void setupFab() {
        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        CardView fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v ->
                new AddEntryDialog().show(getSupportFragmentManager(), "AddEntryDialog"));

        navController.addOnDestinationChangedListener(
                (controller, destination, args) ->
                        fabAdd.setVisibility(destination.getId() == R.id.navigation_chat
                                ? View.GONE : View.VISIBLE));
    }

    /* ─────────────────────────────── */
    /*  Utils / misc                   */
    /* ─────────────────────────────── */

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1002);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1002) { // POST_NOTIFICATIONS
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("NOTIF", "✔️ Notification permission granted");
            } else {
                Log.w("NOTIF", "❌ Notification permission denied");
            }

            // 👉 Continue with Health Connect permissions after notif permission response
            checkAndRequestPermissions();
        }
    }


    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }
}
