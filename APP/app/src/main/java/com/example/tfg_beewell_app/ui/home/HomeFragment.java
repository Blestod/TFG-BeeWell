package com.example.tfg_beewell_app.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.tfg_beewell_app.R;
import com.example.tfg_beewell_app.databinding.FragmentHomeBinding;
import com.example.tfg_beewell_app.ui.VitalData;
import com.google.android.flexbox.FlexboxLayout;

import android.util.TypedValue;
import android.view.ViewGroup.LayoutParams;

import com.example.tfg_beewell_app.utils.SmartwatchReader;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        HomeViewModel homeViewModel =
                new ViewModelProvider(requireActivity(),
                        new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()))
                        .get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textInfo;

        homeViewModel.getText().observe(getViewLifecycleOwner(), text -> {
            if (text != null && !text.isEmpty()) {
                textView.setText(text);
            }
        });

        // 🔁 Crear ejemplo de datos simulados
        // 🔁 Obtener datos desde "dispositivo"
        VitalData v = SmartwatchReader.getCurrentVitals("paciente@ejemplo.com");
        showVitals(v);


        // 🔁 Mostrar los datos en greenCard
        showVitals(v);

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

        if (vital.getSystolic() != null && vital.getDiastolic() != null) {
            String pressure = vital.getSystolic().intValue() + "/" + vital.getDiastolic().intValue();
            greenCard.addView(createVitalTextView(pressure, "mmHg"));
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
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(16, 0, 16, 4);
        container.setLayoutParams(lp);

        return container;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
