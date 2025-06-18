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
import com.example.tfg_beewell_app.utils.ChatMessage;
import com.example.tfg_beewell_app.utils.ChatViewModel;
import com.example.tfg_beewell_app.utils.PredictionPoster;

import java.util.List;

/**
 * Fragmento de chat (Usuario ↔ IA)
 * UI only; persists last AI reply directly here using ChatMessage.
 */
public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private ChatViewModel       viewModel;
    private ChatAdapter         adapter;

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

        // 1. ViewModel
        viewModel = new ViewModelProvider(requireActivity())
                .get(ChatViewModel.class);

        // 2. RecyclerView setup
        binding.chatRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        adapter = new ChatAdapter();
        binding.chatRecyclerView.setAdapter(adapter);
        binding.chatRecyclerView.setHasFixedSize(true);

        // 3. Observe chat list and persist last AI answer
        viewModel.getChat().observe(getViewLifecycleOwner(), msgs -> {
            adapter.submitList(msgs);
            scrollToBottom();

            // Persist only the last AI reply
            if (msgs != null && !msgs.isEmpty()) {
                ChatMessage last = msgs.get(msgs.size() - 1);
                if (!last.isUser()) {
                    PredictionPoster.post(
                            requireContext(),
                            last.getMessage(),
                            "chat",
                            null,
                            null
                    );
                }
            }
        });

        // 4. Loading spinner & button state
        viewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.sendButton.setEnabled(!isLoading);
            binding.messageInput.setEnabled(!isLoading);

            int tint = requireContext().getColor(
                    isLoading ? R.color.beeGrey : R.color.beeBlue);
            binding.sendButton.setBackgroundTintList(
                    ColorStateList.valueOf(tint));
        });

        // 5. Send message
        binding.sendButton.setOnClickListener(v -> {
            String txt = binding.messageInput.getText().toString().trim();
            if (!TextUtils.isEmpty(txt)) {
                viewModel.sendMessage(txt);
                binding.messageInput.setText("");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;  // avoid memory leaks
    }

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) {
            binding.chatRecyclerView.scrollToPosition(count - 1);
        }
    }
}
