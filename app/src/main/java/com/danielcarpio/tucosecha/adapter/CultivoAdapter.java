package com.danielcarpio.tucosecha.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class CultivoAdapter extends RecyclerView.Adapter<CultivoAdapter.CultivoViewHolder> {

    public interface OnCultivoClickListener {
        void onCultivoClick(Cultivo cultivo);
    }

    public interface OnCultivoSelectListener {
        void onCultivoSelect(Cultivo cultivo, boolean isSelected);
    }

    private final Context context;
    private final List<Cultivo> cultivoList;
    private final OnCultivoClickListener clickListener;
    private final OnCultivoSelectListener selectListener;
    private final boolean selectionMode;
    private final List<Cultivo> selectedCultivos;
    private final String userPlan;

    public CultivoAdapter(Context context, List<Cultivo> cultivoList, boolean selectionMode, List<Cultivo> selectedCultivos, String userPlan, OnCultivoClickListener clickListener, OnCultivoSelectListener selectListener) {
        this.context = context;
        this.cultivoList = cultivoList;
        this.selectionMode = selectionMode;
        this.selectedCultivos = selectedCultivos;
        this.userPlan = userPlan != null ? userPlan.toLowerCase() : "ninguno";
        this.clickListener = clickListener;
        this.selectListener = selectListener;
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
        double precio = cultivo.getPrecio() > 0 ? cultivo.getPrecio() : 2.50;
        holder.tvPrecio.setText(String.format(Locale.getDefault(), "%.2f €", precio));

        // Badge matching reference image precisely
        String fase = cultivo.getFase();
        if (fase != null && fase.equalsIgnoreCase("listo")) {
            holder.chipFase.setText(cultivo.getUnidadVenta() != null ? cultivo.getUnidadVenta() : "1 kg");
            holder.chipFase.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3E8E2F")));
            holder.chipFase.setTextColor(Color.WHITE);
        } else if (cultivo.getEtiquetas() != null && cultivo.getEtiquetas().contains("Ecológico")) {
            holder.chipFase.setText("Ecológico");
            holder.chipFase.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#8B5A2B")));
            holder.chipFase.setTextColor(Color.WHITE);
        } else {
            holder.chipFase.setText("Temporada");
            holder.chipFase.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFB300")));
            holder.chipFase.setTextColor(Color.BLACK);
        }

        // Progress bar for Products tab
        if (selectionMode) {
            holder.layoutProgreso.setVisibility(View.GONE);
        } else {
            holder.layoutProgreso.setVisibility(View.VISIBLE);
            holder.progressBar.setProgress(cultivo.getProgreso());
        }

        // Selection mode (+ button) for Crea tu Cesta
        boolean isScarce = "Ajo".equalsIgnoreCase(cultivo.getNombre()) || "Cebolla morada".equalsIgnoreCase(cultivo.getNombre());
        boolean isPremium = userPlan.contains("premium");

        if (selectionMode) {
            if (isScarce && !isPremium) {
                holder.tvLockBadge.setVisibility(View.VISIBLE);
                holder.btnAddCesta.setVisibility(View.GONE);
            } else if (fase != null && fase.equalsIgnoreCase("listo")) {
                holder.tvLockBadge.setVisibility(View.GONE);
                holder.btnAddCesta.setVisibility(View.VISIBLE);

                boolean isSelected = selectedCultivos != null && selectedCultivos.contains(cultivo);
                if (isSelected) {
                    holder.btnAddCesta.setIconResource(android.R.drawable.checkbox_on_background);
                    holder.btnAddCesta.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1F5A2A")));
                } else {
                    holder.btnAddCesta.setIconResource(android.R.drawable.ic_input_add);
                    holder.btnAddCesta.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3E8E2F")));
                }

                holder.btnAddCesta.setOnClickListener(v -> {
                    boolean currentlySelected = selectedCultivos != null && selectedCultivos.contains(cultivo);
                    if (selectListener != null) {
                        selectListener.onCultivoSelect(cultivo, !currentlySelected);
                    }
                });
            } else {
                holder.tvLockBadge.setVisibility(View.GONE);
                holder.btnAddCesta.setVisibility(View.GONE);
            }
        } else {
            holder.tvLockBadge.setVisibility(View.GONE);
            holder.btnAddCesta.setVisibility(View.GONE);
        }

        // Image
        int resId = context.getResources().getIdentifier(cultivo.getImagen(), "drawable", context.getPackageName());
        if (resId == 0) {
            resId = R.drawable.logo_tucosecha;
        }
        holder.imgCultivo.setImageResource(resId);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCultivoClick(cultivo);
        });
    }

    @Override
    public int getItemCount() {
        return cultivoList.size();
    }

    public static class CultivoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCultivo;
        TextView tvNombre, tvPrecio, tvLockBadge;
        MaterialButton chipFase, btnAddCesta;
        ProgressBar progressBar;
        LinearLayout layoutProgreso;

        public CultivoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCultivo = itemView.findViewById(R.id.imgCultivo);
            tvNombre = itemView.findViewById(R.id.tvNombreCultivo);
            tvPrecio = itemView.findViewById(R.id.tvPrecioCultivo);
            tvLockBadge = itemView.findViewById(R.id.tvLockBadge);
            chipFase = itemView.findViewById(R.id.chipFase);
            btnAddCesta = itemView.findViewById(R.id.btnAddCesta);
            progressBar = itemView.findViewById(R.id.progressBarCultivo);
            layoutProgreso = itemView.findViewById(R.id.layoutProgresoCultivo);
        }
    }
}
