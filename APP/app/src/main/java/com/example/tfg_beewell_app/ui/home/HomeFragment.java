package com.example.tfg_beewell_app.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.tfg_beewell_app.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // ✅ Use ViewModelProvider.Factory for AndroidViewModel
        HomeViewModel homeViewModel =
                new ViewModelProvider(requireActivity(),
                        new ViewModelProvider.AndroidViewModelFactory(requireActivity().getApplication()))
                        .get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // ✅ Ensure textInfo is correctly referenced in the layout
        final TextView textView = binding.textInfo;

        // ✅ Observe ViewModel's LiveData and update UI
        homeViewModel.getText().observe(getViewLifecycleOwner(), text -> {
            if (text != null && !text.isEmpty()) {
                textView.setText(text);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
