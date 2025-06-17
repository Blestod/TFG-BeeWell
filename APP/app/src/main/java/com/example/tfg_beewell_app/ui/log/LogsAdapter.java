package com.example.tfg_beewell_app.ui.log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tfg_beewell_app.R;

public class LogsAdapter extends ListAdapter<Object, RecyclerView.ViewHolder> {

    /* ---------- diff util ---------- */
    private static final DiffUtil.ItemCallback<Object> DIFF =
            new DiffUtil.ItemCallback<Object>() {
                @Override public boolean areItemsTheSame(@NonNull Object a,@NonNull Object b) {
                    return a.hashCode() == b.hashCode();
                }
                @Override public boolean areContentsTheSame(@NonNull Object a,@NonNull Object b) {
                    return a.equals(b);
                }
            };

    /* ---------- constructor ---------- */
    public interface DeleteCb { void del(Object item); }

    private final int     type;   // 0 insulin | 1 meal | 2 activity
    private final DeleteCb delete;

    public LogsAdapter(int type, DeleteCb cb){
        super(DIFF);
        this.type   = type;
        this.delete = cb;
    }

    @Override public int getItemViewType(int position){ return type; }

    /* ---------- create holders ---------- */
    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup g,int vt){
        LayoutInflater in = LayoutInflater.from(g.getContext());
        if (vt == 0) return new InsVH (in.inflate(R.layout.item_insulin_card,   g,false));
        if (vt == 1) return new MealVH(in.inflate(R.layout.item_meal_card,     g,false));
        return               new ActVH (in.inflate(R.layout.item_exercise_card,g,false));
    }

    /* ---------- bind ---------- */
    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h,int p){
        Object o = getItem(p);
        if (h instanceof InsVH)  ((InsVH) h).bind((Net.InsulinLog) o);
        if (h instanceof MealVH) ((MealVH) h).bind((Net.MealLog)    o);
        if (h instanceof ActVH)  ((ActVH) h).bind((Net.ActLog)     o);
    }

    /*──────────────────────── 3 view-holders ───────────────────────*/

    /* ── insulin ── */
    class InsVH extends RecyclerView.ViewHolder{
        TextView t1,t2;
        InsVH(View v){
            super(v);
            t1 = v.findViewById(R.id.txtInsulin);
            t2 = v.findViewById(R.id.txtInsulinInfo);
            v.findViewById(R.id.btnDeleteInsulin)
                    .setOnClickListener(x -> delete.del(getItem(getAdapterPosition())));
        }
        void bind(Net.InsulinLog l){
            t1.setText(l.insulin_type + "  " + l.in_units + " U");
            t2.setText(Utils.fmt(l.in_time) + "  •  " + l.in_spot);
        }
    }

    /* ── meal ── */
    class MealVH extends RecyclerView.ViewHolder{
        TextView t1,t2;
        MealVH(View v){
            super(v);
            t1 = v.findViewById(R.id.txtMealFood);
            t2 = v.findViewById(R.id.txtMealInfo);
            v.findViewById(R.id.btnDeleteMeal)
                    .setOnClickListener(x -> delete.del(getItem(getAdapterPosition())));
        }
        void bind(Net.MealLog l){
            String title = l.food_name != null ? l.food_name : ("Food #" + l.food_id);
            t1.setText(title);
            t2.setText(Utils.fmt(l.meal_time) + "  •  " + l.grams + " g");
        }
    }

    /* ── activity ── */
    class ActVH extends RecyclerView.ViewHolder{
        TextView t1,t2;
        ActVH(View v){
            super(v);
            t1 = v.findViewById(R.id.txtExercise);
            t2 = v.findViewById(R.id.txtExerciseInfo);
            v.findViewById(R.id.btnDeleteExercise)
                    .setOnClickListener(x -> delete.del(getItem(getAdapterPosition())));
        }
        void bind(Net.ActLog l){
            String title = l.act_name != null ? l.act_name : "Exercise";
            t1.setText(title);
            t2.setText(Utils.fmt(l.act_time) + "  •  " +
                    l.duration_min + " min  •  " + l.intensity);
        }
    }
}
