package com.danielcarpio.tucosecha.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.google.android.material.chip.Chip;

import java.util.List;

public class CultivoAdapter extends RecyclerView.Adapter<CultivoAdapter.CultivoViewHolder> {

    public interface OnCultivoClickListener {
        void onCultivoClick(Cultivo cultivo);
    }

    private final Context context;
    private final List<Cultivo> cultivoList;
    private final OnCultivoClickListener listener;

    public CultivoAdapter(Context context, List<Cultivo> cultivoList, OnCultivoClickListener listener) {
        this.context = context;
        this.cultivoList = cultivoList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CultivoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cultivo, parent, false);
        return new CultivoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CultivoViewHolder holder, int position) {
        Cultivo cultivo = cultivoList.get(position);

        holder.tvNombre.setText(cultivo.getNombre());
        holder.progressBar.setProgress(cultivo.getProgreso());
        holder.tvProgreso.setText(cultivo.getProgreso() + "%");

        String fase = cultivo.getFase() != null ? cultivo.getFase() : "";
        holder.chipFase.setText(capitalize(fase));

        if (fase.equalsIgnoreCase("listo")) {
            holder.chipFase.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#3E8E2F")));
            holder.chipFase.setTextColor(Color.WHITE);
        } else if (fase.equalsIgnoreCase("creciendo")) {
            holder.chipFase.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFB300")));
            holder.chipFase.setTextColor(Color.BLACK);
        } else if (fase.equalsIgnoreCase("sembrado")) {
            holder.chipFase.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#8B5A2B")));
            holder.chipFase.setTextColor(Color.WHITE);
        } else {
            holder.chipFase.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            holder.chipFase.setTextColor(Color.BLACK);
        }

        int resId = context.getResources().getIdentifier(cultivo.getImagen(), "drawable", context.getPackageName());
        if (resId == 0) {
            resId = R.drawable.logo_tucosecha;
        }
        holder.imgCultivo.setImageResource(resId);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCultivoClick(cultivo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cultivoList.size();
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    public static class CultivoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCultivo;
        TextView tvNombre, tvProgreso;
        Chip chipFase;
        ProgressBar progressBar;

        public CultivoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCultivo = itemView.findViewById(R.id.imgCultivo);
            tvNombre = itemView.findViewById(R.id.tvNombreCultivo);
            tvProgreso = itemView.findViewById(R.id.tvProgresoPorcentaje);
            chipFase = itemView.findViewById(R.id.chipFase);
            progressBar = itemView.findViewById(R.id.progressBarCultivo);
        }
    }
}
