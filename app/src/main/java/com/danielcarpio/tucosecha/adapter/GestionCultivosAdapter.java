package com.danielcarpio.tucosecha.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.google.android.material.chip.Chip;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class GestionCultivosAdapter extends RecyclerView.Adapter<GestionCultivosAdapter.CultivoViewHolder> {

    public interface OnCultivoClickListener {
        void onCultivoClick(Cultivo cultivo);
    }

    private final List<Cultivo> cultivos;
    private final OnCultivoClickListener listener;

    public GestionCultivosAdapter(List<Cultivo> cultivos, OnCultivoClickListener listener) {
        this.cultivos = cultivos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CultivoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gestion_cultivo, parent, false);
        return new CultivoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CultivoViewHolder holder, int position) {
        Cultivo cultivo = cultivos.get(position);
        Context context = holder.itemView.getContext();

        holder.tvNombre.setText(cultivo.getNombre() != null ? cultivo.getNombre() : "");
        holder.tvProgreso.setText(cultivo.getProgreso() + "%");
        holder.progressBar.setProgress(cultivo.getProgreso());

        String fase = cultivo.getFase() != null ? cultivo.getFase() : "";
        holder.chipFase.setText(capitalize(fase));
        if ("listo".equalsIgnoreCase(fase)) {
            holder.chipFase.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#3E8E2F")));
            holder.chipFase.setTextColor(Color.WHITE);
        } else if ("creciendo".equalsIgnoreCase(fase)) {
            holder.chipFase.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFB300")));
            holder.chipFase.setTextColor(Color.BLACK);
        } else {
            holder.chipFase.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#8B5A2B")));
            holder.chipFase.setTextColor(Color.WHITE);
        }

        if (cultivo.getImagen() != null && !cultivo.getImagen().isEmpty()) {
            int resId = context.getResources().getIdentifier(cultivo.getImagen(), "drawable", context.getPackageName());
            if (resId == 0) {
                resId = R.drawable.logo_tucosecha;
            }
            holder.imgFoto.setImageResource(resId);
        } else {
            holder.imgFoto.setImageResource(R.drawable.logo_tucosecha);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCultivoClick(cultivo);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cultivos.size();
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    static class CultivoViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView imgFoto;
        final TextView tvNombre;
        final Chip chipFase;
        final ProgressBar progressBar;
        final TextView tvProgreso;

        public CultivoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFoto = itemView.findViewById(R.id.imgItemCultivo);
            tvNombre = itemView.findViewById(R.id.tvItemCultivoNombre);
            chipFase = itemView.findViewById(R.id.chipItemCultivoFase);
            progressBar = itemView.findViewById(R.id.progressItemCultivo);
            tvProgreso = itemView.findViewById(R.id.tvItemCultivoProgreso);
        }
    }
}
