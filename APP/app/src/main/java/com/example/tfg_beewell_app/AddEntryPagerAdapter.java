package com.example.tfg_beewell_app;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AddEntryPagerAdapter extends FragmentStateAdapter {
    public AddEntryPagerAdapter(FragmentActivity fa) {
        super(fa);
    }

    @Override
    public Fragment createFragment(int position) {
        return position == 0 ? new InsulinFragment() : new MealFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
