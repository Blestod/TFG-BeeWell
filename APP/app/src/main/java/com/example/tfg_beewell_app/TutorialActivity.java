/*  app/src/main/java/com/example/tfg_beewell_app/TutorialActivity.java  */
package com.example.tfg_beewell_app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.tfg_beewell_app.utils.TutorialAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TutorialActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* edge-to-edge */
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_tutorial);

        ViewPager2 pager = findViewById(R.id.pager);
        TabLayout  dots  = findViewById(R.id.dots);

        /* pages */
        pager.setAdapter(new TutorialAdapter());

        /* one dot per page (filled ⇄ hollow via selector) */
        new TabLayoutMediator(dots, pager,
                (tab, pos) -> tab.setIcon(R.drawable.dot_selector)
        ).attach();

        /* lift the dots above the gesture area */
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root),
                (v, insets) -> {
                    int bottomInset = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()).bottom;
                    int minPad = getResources()
                            .getDimensionPixelSize(R.dimen.dots_min_bottom);
                    dots.setPadding(0, 0, 0, Math.max(bottomInset, minPad));
                    return insets;
                });

        /* close */
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }
}
