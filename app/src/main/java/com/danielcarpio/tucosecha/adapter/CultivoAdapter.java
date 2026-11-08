package com.danielcarpio.tucosecha.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import com.danielcarpio.tucosecha.model.ProductoCosecha;
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
    private final List<ProductoCosecha> harvestProducts;

    public CultivoAdapter(Context context, List<Cultivo> cultivoList, boolean selectionMode, List<Cultivo> selectedCultivos, String userPlan, List<ProductoCosecha> harvestProducts, OnCultivoClickListener clickListener, OnCultivoSelectListener selectListener) {
        this.context = context;
        this.cultivoList = cultivoList;
        this.selectionMode = selectionMode;
        this.selectedCultivos = selectedCultivos;
        this.userPlan = userPlan != null ? userPlan.toLowerCase() : "ninguno";
        this.harvestProducts = harvestProducts;
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

        // 1. Etiqueta de la tarjeta
        String fase = cultivo.getFase();
        String displayTag;
        int tagBgColor;
        int tagTextColor;

        if (fase != null && fase.equalsIgnoreCase("listo")) {
            displayTag = cultivo.getUnidadVenta() != null ? cultivo.getUnidadVenta() : "1 kg";
            tagBgColor = Color.parseColor("#3E8E2F");
            tagTextColor = Color.WHITE;
        } else if (cultivo.getEtiquetas() != null && !cultivo.getEtiquetas().isEmpty()) {
            String etiqueta = cultivo.getEtiquetas().get(0);
            displayTag = etiqueta;
            if (etiqueta.equalsIgnoreCase("ecológico")) {
                tagBgColor = Color.parseColor("#8B5A2B");
                tagTextColor = Color.WHITE;
            } else {
                tagBgColor = Color.parseColor("#FFB300");
                tagTextColor = Color.BLACK;
            }
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

        // Opacidad y badge Pronto
        if (fase != null && fase.equalsIgnoreCase("listo")) {
            holder.viewOpacityOverlay.setVisibility(View.GONE);
            holder.tvProntoBadge.setVisibility(View.GONE);
        } else {
            holder.viewOpacityOverlay.setVisibility(View.VISIBLE);
            holder.tvProntoBadge.setVisibility(View.VISIBLE);
        }

        // 3. Precio / Cantidad / Estado
        if (selectionMode) {
            holder.layoutProgreso.setVisibility(View.GONE);
            String qtyText = "";
            if (userPlan.equalsIgnoreCase("ninguno") || userPlan.equalsIgnoreCase("invitado")) {
                qtyText = "● Listo";
                holder.tvPrecio.setTextColor(Color.parseColor("#3E8E2F"));
            } else {
                double qty = 0;
                if (harvestProducts != null) {
                    for (ProductoCosecha p : harvestProducts) {
                        if (p.getNombre().equalsIgnoreCase(cultivo.getNombre())) {
                            if (userPlan.contains("basico")) qty = p.getCantidadBasico();
                            else if (userPlan.contains("familiar")) qty = p.getCantidadFamiliar();
                            else if (userPlan.contains("premium")) qty = p.getCantidadPremium();
                            break;
                        }
                    }
                }
                if (qty > 0) {
                    qtyText = (qty == (int) qty ? String.valueOf((int) qty) : String.valueOf(qty)) + " " + (cultivo.getUnidadVenta() != null ? cultivo.getUnidadVenta() : "ud");
                }
                holder.tvPrecio.setTextColor(Color.parseColor("#1F5A2A"));
            }
            holder.tvPrecio.setText(qtyText);
        } else {
            holder.layoutProgreso.setVisibility(View.VISIBLE);
            holder.progressBar.setProgress(cultivo.getProgreso());
            if (fase != null && fase.equalsIgnoreCase("listo")) {
                holder.tvPrecio.setText("● Listo");
                holder.tvPrecio.setTextColor(Color.parseColor("#3E8E2F"));
            } else {
                holder.tvPrecio.setText(cultivo.getProgreso() + "%");
                holder.tvPrecio.setTextColor(Color.parseColor("#6B7A5E"));
            }
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
        View viewOpacityOverlay;
        TextView tvNombre, tvPrecio, tvLockBadge, tvTag, tvProntoBadge;
        MaterialButton btnAddCesta;
        ProgressBar progressBar;
        LinearLayout layoutProgreso;

        public CultivoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCultivo = itemView.findViewById(R.id.imgCultivo);
            viewOpacityOverlay = itemView.findViewById(R.id.viewOpacityOverlay);
            tvNombre = itemView.findViewById(R.id.tvNombreCultivo);
            tvPrecio = itemView.findViewById(R.id.tvPrecioCultivo);
            tvLockBadge = itemView.findViewById(R.id.tvLockBadge);
            tvTag = itemView.findViewById(R.id.tvTagCultivo);
            tvProntoBadge = itemView.findViewById(R.id.tvProntoBadge);
            btnAddCesta = itemView.findViewById(R.id.btnAddCesta);
            progressBar = itemView.findViewById(R.id.progressBarCultivo);
            layoutProgreso = itemView.findViewById(R.id.layoutProgresoCultivo);
        }
    }
}
