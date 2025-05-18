package com.example.tfg_beewell_app.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tfg_beewell_app.R;

public class ChatAdapter
        extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER    = 0;
    private static final int VIEW_TYPE_AI      = 1;
    private static final int VIEW_TYPE_LOADING = 2;      // <- nuevo

    public ChatAdapter(){ super(new ChatDiff()); }

    /* --------- diff util ---------- */
    private static class ChatDiff extends DiffUtil.ItemCallback<ChatMessage>{
        @Override public boolean areItemsTheSame(@NonNull ChatMessage a,@NonNull ChatMessage b){
            return a.hashCode()==b.hashCode();
        }
        @Override public boolean areContentsTheSame(@NonNull ChatMessage a,@NonNull ChatMessage b){
            return a.equals(b);
        }
    }

    /* ---------- ListAdapter -------- */
    @Override public int getItemViewType(int pos){
        ChatMessage m = getItem(pos);
        if(m.isLoading())          return VIEW_TYPE_LOADING;
        return m.isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p,int type){
        LayoutInflater inf = LayoutInflater.from(p.getContext());
        if(type==VIEW_TYPE_USER)   return new UserVH   (inf.inflate(R.layout.item_chat_user   ,p,false));
        if(type==VIEW_TYPE_AI)     return new AiVH     (inf.inflate(R.layout.item_chat_ai     ,p,false));
        /* loading */
        return new LoadingVH(inf.inflate(R.layout.item_chat_loading,p,false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder vh,int pos){
        ((BaseVH)vh).bind(getItem(pos));
    }

    /* ------------- VHs -------------- */
    private abstract static class BaseVH extends RecyclerView.ViewHolder{
        BaseVH(View v){ super(v); }
        void bind(ChatMessage m){}
    }
    private static class UserVH extends BaseVH{
        final TextView txt;
        UserVH(View v){ super(v); txt=v.findViewById(R.id.txtUser); }
        @Override void bind(ChatMessage m){ txt.setText(m.getMessage()); }
    }
    private static class AiVH extends BaseVH{
        final TextView txt;
        AiVH(View v){ super(v); txt=v.findViewById(R.id.txtAI); }
        @Override void bind(ChatMessage m){ txt.setText(m.getMessage()); }
    }
    private static class LoadingVH extends BaseVH{
        final ImageView img;
        LoadingVH(View v){ super(v); img=v.findViewById(R.id.imgLoading); }
        @Override void bind(ChatMessage m){
            img.startAnimation(AnimationUtils.loadAnimation(
                    itemView.getContext(), R.anim.spin));
        }
    }
}
