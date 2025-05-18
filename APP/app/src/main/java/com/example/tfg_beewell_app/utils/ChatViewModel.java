package com.example.tfg_beewell_app.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatViewModel extends ViewModel {

    /* ---------- loading flag (para des/habilitar el botón) --------- */
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading() { return loading; }

    /* ---------- historial del chat ---------- */
    private final MutableLiveData<List<ChatMessage>> chat =
            new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<ChatMessage>> getChat() { return chat; }

    /* -------------------------------------------------------------------- */
    public void sendMessage(String userMsg){

        if(userMsg==null || userMsg.trim().isEmpty()) return;

        /* añade mensaje usuario */
        List<ChatMessage> list = new ArrayList<>(Objects.requireNonNull(chat.getValue()));
        list.add(ChatMessage.of(userMsg,true));

        /* placeholder loading */
        list.add(ChatMessage.loading());
        chat.setValue(list);
        loading.setValue(true);

        ChatService.getInstance().askBot(userMsg, resp -> {
            List<ChatMessage> l = new ArrayList<>(Objects.requireNonNull(chat.getValue()));
            /* quita el placeholder (último) */
            if(!l.isEmpty() && l.get(l.size()-1).isLoading()) l.remove(l.size()-1);
            /* añade respuesta */
            l.add(ChatMessage.of(resp,false));
            chat.postValue(l);
            loading.postValue(false);
        });
    }

}
