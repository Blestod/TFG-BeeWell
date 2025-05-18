package com.example.tfg_beewell_app.utils;

import androidx.annotation.NonNull;
import java.util.Objects;

public class ChatMessage {

    private final String  message;
    private final boolean isUser;
    private       boolean loading;

    /* ------------ constructores ------------- */
    private ChatMessage(String msg, boolean user, boolean loading){
        this.message = msg;
        this.isUser  = user;
        this.loading = loading;
    }
    /** mensaje normal  */
    public static ChatMessage of(String msg, boolean user){
        return new ChatMessage(msg, user, /*loading*/ false);
    }
    /** placeholder “estoy pensando …”  */
    public static ChatMessage loading(){
        return new ChatMessage("", /*user*/false, /*loading*/true);
    }

    /* ------------- getters ------------------ */
    public String  getMessage() { return message; }
    public boolean isUser()     { return isUser;   }
    public boolean isLoading()  { return loading;  }

    /* ------------- equals / hashCode -------- */
    @Override public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof ChatMessage)) return false;
        ChatMessage m = (ChatMessage) o;
        return isUser == m.isUser &&
                loading == m.loading &&
                Objects.equals(message, m.message);
    }
    @Override public int hashCode(){
        return Objects.hash(message, isUser, loading);
    }
    @NonNull @Override public String toString(){
        return (isLoading() ? "[loading]" : (isUser?"USR:":"AI:")) + message;
    }
}
