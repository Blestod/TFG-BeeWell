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
        if (position == 0) return new InsulinFragment();
        else if (position == 1) return new MealFragment();
        else return new ExerciseFragment();
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
