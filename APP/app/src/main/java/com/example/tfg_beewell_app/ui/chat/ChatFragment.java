package com.example.tfg_beewell_app.ui.chat;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tfg_beewell_app.R;
import com.example.tfg_beewell_app.databinding.FragmentChatBinding;
import com.example.tfg_beewell_app.utils.ChatAdapter;
import com.example.tfg_beewell_app.utils.ChatViewModel;

import java.util.Collections;
import java.util.Objects;

/**
 * Fragmento de chat (Usuario  ↔  IA)
 */
public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatViewModel      viewModel;
    private ChatAdapter        adapter;

    /* --------------------------------------------------------------------- */
    /* CICLO DE VIDA                                                         */
    /* --------------------------------------------------------------------- */

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {

        /* 1. ViewModel ---------------------------------------------- */
        viewModel = new ViewModelProvider(requireActivity())
                .get(ChatViewModel.class);

        /* 2. RecyclerView ------------------------------------------- */
        // 👉  PONEMOS EL LAYOUT-MANAGER **con** su parámetro
        binding.chatRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext()));

        // 👉  El adapter **sin** argumentos
        adapter = new ChatAdapter();
        binding.chatRecyclerView.setAdapter(adapter);
        binding.chatRecyclerView.setHasFixedSize(true);

        /* 3. Observa la lista para refrescar ------------------------ */
        viewModel.getChat().observe(getViewLifecycleOwner(), msgs -> {
            adapter.submitList(msgs);                       // DiffUtil animado
            if (msgs != null && !msgs.isEmpty()) {
                binding.chatRecyclerView.scrollToPosition(msgs.size() - 1);
            }
        });
        // dentro de onViewCreated(), después del observer de chat:

        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.sendButton.setEnabled(!isLoading);
            binding.messageInput.setEnabled(!isLoading);

            int color = isLoading ? getResources().getColor(R.color.beeGrey)
                    : getResources().getColor(R.color.beeBlue);   // tu azul
            binding.sendButton.setBackgroundTintList(
                    ColorStateList.valueOf(color));
        });


        /* 4. Enviar mensaje ---------------------------------------- */
        binding.sendButton.setOnClickListener(v -> {
            String txt = binding.messageInput.getText().toString().trim();
            if (!txt.isEmpty()) {
                viewModel.sendMessage(txt);
                binding.messageInput.setText("");
            }
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;                         // evita fugas de memoria
    }
}
