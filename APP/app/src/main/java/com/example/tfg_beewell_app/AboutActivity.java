// ==============================  AboutActivity.java  ==============================
// Single‑file drop‑in: paste this class into your ui package. Create the XML shown
// below in res/layout/activity_about.xml. Add a menu/toolbar entry that opens
// AboutActivity with a simple Intent.
// -------------------------------------------------------------------------------
package com.example.tfg_beewell_app;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Simple "About" screen listing authors, acknowledgements and app version.
 * The ✕ button (top‑right) always returns the user to the Home fragment.
 */
public class AboutActivity extends AppCompatActivity {

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // ---- X button → jump straight to MainActivity + Home fragment ---- //
        ImageView closeBtn = findViewById(R.id.backButton2);
        closeBtn.setOnClickListener(v -> {
            Intent home = new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |  // reuse existing task
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
            home.putExtra("navigate_home", true);              // let MainActivity decide
            startActivity(home);
            // optional fade‑out animation (comment if undesired)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // ---- Show app version ---- //
        TextView versionTv = findViewById(R.id.appVersionText);
        versionTv.setText(fetchVersion());
    }

    // Fallback for toolbar up‑arrow (if you add one later)
    @Override public boolean onSupportNavigateUp() { finish(); return true; }

    private String fetchVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return "v" + pInfo.versionName + " (" + pInfo.versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) { return "-"; }
    }
}
