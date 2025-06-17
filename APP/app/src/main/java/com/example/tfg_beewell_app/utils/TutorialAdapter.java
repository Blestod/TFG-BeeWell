package com.example.tfg_beewell_app.utils;

import android.view.*;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.tfg_beewell_app.R;

public class TutorialAdapter extends RecyclerView.Adapter<TutorialAdapter.VH> {

    private final int[] slides = {
            R.drawable.tut1, R.drawable.tut2, R.drawable.tut3,
            R.drawable.tut4, R.drawable.tut5, R.drawable.tut6
    };

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vType){
        ImageView iv = new ImageView(p.getContext());
        iv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return new VH(iv);
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos){
        h.img.setImageResource(slides[pos]);
    }
    @Override public int getItemCount(){ return slides.length; }

    static class VH extends RecyclerView.ViewHolder{
        ImageView img;
        VH(View v){ super(v); img=(ImageView)v; }
    }
}
