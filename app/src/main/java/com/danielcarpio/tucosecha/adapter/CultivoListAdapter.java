package com.danielcarpio.tucosecha.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Cultivo;

public class CultivoListAdapter extends ListAdapter<Cultivo, CultivoListAdapter.CultivoViewHolder> {

    public interface OnCultivoItemClickListener {
        void onCultivoClick(Cultivo cultivo);
    }

    private final Context context;
    private final String userPlan;
    private final boolean maxReached;
    private final OnCultivoItemClickListener clickListener;

    public static final DiffUtil.ItemCallback<Cultivo> DIFF_CALLBACK = new DiffUtil.ItemCallback<Cultivo>() {
        @Override
        public boolean areItemsTheSame(@NonNull Cultivo oldItem, @NonNull Cultivo newItem) {
            return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Cultivo oldItem, @NonNull Cultivo newItem) {
            return oldItem.getNombre().equals(newItem.getNombre()) &&
                    oldItem.getFase().equals(newItem.getFase()) &&
                    oldItem.getProgreso() == newItem.getProgreso();
        }
    };

    public CultivoListAdapter(Context context, String userPlan, boolean maxReached, OnCultivoItemClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.userPlan = userPlan != null ? userPlan.toLowerCase() : "ninguno";
        this.maxReached = maxReached;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public CultivoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cultivo, parent, false);
        return new CultivoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CultivoViewHolder holder, int position) {
        Cultivo cultivo = getItem(position);

        holder.tvNombre.setText(cultivo.getNombre());

        String fase = cultivo.getFase();
        String displayTag;
        int tagBgColor;
        int tagTextColor;

        if (fase != null && fase.equalsIgnoreCase("listo")) {
            displayTag = cultivo.getUnidadVenta() != null ? cultivo.getUnidadVenta() : "1 kg";
            tagBgColor = Color.parseColor("#3E8E2F");
            tagTextColor = Color.WHITE;
        } else {
            displayTag = "Temporada";
            tagBgColor = Color.parseColor("#FFB300");
            tagTextColor = Color.BLACK;
        }

        holder.tvTag.setText(displayTag);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius(12 * context.getResources().getDisplayMetrics().density);
        shape.setColor(tagBgColor);
        holder.tvTag.setBackground(shape);
        holder.tvTag.setTextColor(tagTextColor);

        boolean isScarce = "Ajo".equalsIgnoreCase(cultivo.getNombre()) || "Cebolla morada".equalsIgnoreCase(cultivo.getNombre());
        boolean isPremium = userPlan.contains("premium");

        if (isScarce && !isPremium) {
            holder.tvLockBadge.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(0.5f);
        } else if (maxReached) {
            holder.tvLockBadge.setVisibility(View.GONE);
            holder.itemView.setAlpha(0.5f);
        } else {
            holder.tvLockBadge.setVisibility(View.GONE);
            holder.itemView.setAlpha(1.0f);
        }

        holder.tvPrecio.setText(cultivo.getUnidadVenta() != null ? cultivo.getUnidadVenta() : "Listo");
        holder.tvPrecio.setTextColor(Color.parseColor("#1F5A2A"));

        int resId = context.getResources().getIdentifier(cultivo.getImagen(), "drawable", context.getPackageName());
        if (resId == 0) {
            resId = R.drawable.logo_tucosecha;
        }
        holder.imgCultivo.setImageResource(resId);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCultivoClick(cultivo);
            }
        });
    }

    public static class CultivoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCultivo;
        TextView tvNombre, tvPrecio, tvLockBadge, tvTag;

        public CultivoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCultivo = itemView.findViewById(R.id.imgCultivo);
            tvNombre = itemView.findViewById(R.id.tvNombreCultivo);
            tvPrecio = itemView.findViewById(R.id.tvPrecioCultivo);
            tvLockBadge = itemView.findViewById(R.id.tvLockBadge);
            tvTag = itemView.findViewById(R.id.tvTagCultivo);
            // hide progress bar / add button in this adapter since entire card is clickable
            View progressLayout = itemView.findViewById(R.id.layoutProgresoCultivo);
            if (progressLayout != null) progressLayout.setVisibility(View.GONE);
            View addBtn = itemView.findViewById(R.id.btnAddCesta);
            if (addBtn != null) addBtn.setVisibility(View.GONE);
        }
    }
}
