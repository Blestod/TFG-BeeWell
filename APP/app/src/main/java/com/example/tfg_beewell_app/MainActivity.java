package com.example.tfg_beewell_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.example.tfg_beewell_app.databinding.ActivityMainBinding;

import android.widget.Toast;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

import android.widget.PopupMenu;
import android.widget.FrameLayout;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "POST_VITAL";
    private static final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Enable fullscreen immersive mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ImageView menuButton = findViewById(R.id.menuButton);
        FrameLayout menuOverlay = findViewById(R.id.menuOverlay);

        menuButton.setOnClickListener(v -> {
            menuOverlay.setVisibility(View.VISIBLE);
            menuOverlay.setTranslationY(-1000f);
            menuOverlay.animate().translationY(0f).setDuration(300).start();
            hideSystemUI(); // keep immersive
        });

        TextView profile = findViewById(R.id.menu_profile);
        TextView terms = findViewById(R.id.menu_terms);
        TextView logout = findViewById(R.id.menu_aboutus);

        ImageView closeMenuBtn = findViewById(R.id.closeMenuBtn);

        closeMenuBtn.setOnClickListener(v -> {
            menuOverlay.setVisibility(View.GONE);
        });

// Optional: close when tapping outside the menu
        menuOverlay.setOnClickListener(v -> {
            menuOverlay.setVisibility(View.GONE);
        });


        profile.setOnClickListener(v -> {
            menuOverlay.setVisibility(View.GONE);
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        terms.setOnClickListener(v -> {
            menuOverlay.setVisibility(View.GONE);
            startActivity(new Intent(MainActivity.this, TermsReadActivity.class));
        });

        logout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
            prefs.edit().clear().apply();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        BottomNavigationView navView = findViewById(R.id.nav_view);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets barInsets = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
            );

            v.setPadding(0, 0, 0, barInsets.bottom);

            return insets;
        });


        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications,
                R.id.navigation_chat
        ).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(navView, navController);

        // ✅ Apply fullscreen UI hiding
        hideSystemUI();
    }


    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
        );

        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }
}
