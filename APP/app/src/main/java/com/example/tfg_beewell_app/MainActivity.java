package com.example.tfg_beewell_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;



import com.example.tfg_beewell_app.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {

    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ImageView menuButton = findViewById(R.id.menuButton);
        FrameLayout menuOverlay = findViewById(R.id.menuOverlay);

        // Menu overlay show
        menuButton.setOnClickListener(v -> {
            menuOverlay.setVisibility(View.VISIBLE);
            menuOverlay.setTranslationY(-1000f);
            menuOverlay.animate().translationY(0f).setDuration(300).start();
            hideSystemUI();
        });

        // Menu close handlers
        findViewById(R.id.closeMenuBtn).setOnClickListener(v -> menuOverlay.setVisibility(View.GONE));
        menuOverlay.setOnClickListener(v -> menuOverlay.setVisibility(View.GONE));

        // Menu options
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

        // Navigation
        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        AppBarConfiguration appBarConfig = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_chat
        ).build();
        NavigationUI.setupWithNavController(navView, navController);

        // Inset padding for gesture nav
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets barInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(0, 0, 0, barInsets.bottom);
            return insets;
        });

        // FAB to open AddEntryBottomSheet
        CardView fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> {
            AddEntryDialog dialog = new AddEntryDialog();
            dialog.show(getSupportFragmentManager(), "AddEntryDialog");
        });

        // Hide FAB on chat fragment
        navController.addOnDestinationChangedListener((controller, destination, args) -> {
            if (destination.getId() == R.id.navigation_chat) {
                fabAdd.setVisibility(View.GONE);
            } else {
                fabAdd.setVisibility(View.VISIBLE);
            }
        });

        hideSystemUI();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
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
