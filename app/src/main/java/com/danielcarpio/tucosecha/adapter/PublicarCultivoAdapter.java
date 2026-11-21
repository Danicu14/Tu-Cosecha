package com.danielcarpio.tucosecha.adapter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.danielcarpio.tucosecha.model.ProductoCosecha;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PublicarCultivoAdapter extends RecyclerView.Adapter<PublicarCultivoAdapter.CultivoPublicarViewHolder> {

    public static class CultivoConfig {
        public Cultivo cultivo;
        public boolean incluido = true;
        public String unidad = "kg";
        public double cantBasico = 1.0;
        public double cantFamiliar = 2.0;
        public double cantPremium = 3.0;
        public boolean escaso = false;

        public CultivoConfig(Cultivo cultivo) {
            this.cultivo = cultivo;
            if (cultivo.getUnidadVenta() != null && !cultivo.getUnidadVenta().isEmpty()) {
                String uv = cultivo.getUnidadVenta().toLowerCase();
                if (uv.contains("manojo")) {
                    this.unidad = "manojo";
                } else if (uv.contains("ud")) {
                    this.unidad = "ud";
                } else {
                    this.unidad = "kg";
                }
            }
        }
    }

    public interface OnConfigChangeListener {
        void onConfigChanged();
    }

    private final List<CultivoConfig> items;
    private final OnConfigChangeListener changeListener;
    private final List<String> opcionesUnidades = Arrays.asList("kg", "ud", "manojo");

    public PublicarCultivoAdapter(List<CultivoConfig> items, OnConfigChangeListener changeListener) {
        this.items = items;
        this.changeListener = changeListener;
    }

    @NonNull
    @Override
    public CultivoPublicarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cultivo_publicar_cosecha, parent, false);
        return new CultivoPublicarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CultivoPublicarViewHolder holder, int position) {
        CultivoConfig config = items.get(position);
        Context context = holder.itemView.getContext();

        holder.tvNombre.setText(config.cultivo.getNombre() != null ? config.cultivo.getNombre() : "");

        if (config.cultivo.getImagen() != null && !config.cultivo.getImagen().isEmpty()) {
            int resId = context.getResources().getIdentifier(config.cultivo.getImagen(), "drawable", context.getPackageName());
            if (resId == 0) resId = R.drawable.logo_tucosecha;
            holder.imgFoto.setImageResource(resId);
        } else {
            holder.imgFoto.setImageResource(R.drawable.logo_tucosecha);
        }

        // Setup dropdown unidad
        ArrayAdapter<String> unidadAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, opcionesUnidades);
        holder.actvUnidad.setAdapter(unidadAdapter);
        holder.actvUnidad.setText(config.unidad, false);
        holder.actvUnidad.setOnItemClickListener((parent, view, pos, id) -> {
            config.unidad = opcionesUnidades.get(pos);
            if (changeListener != null) changeListener.onConfigChanged();
        });

        // Setup checkbox incluir
        holder.cbIncluir.setOnCheckedChangeListener(null);
        holder.cbIncluir.setChecked(config.incluido);
        holder.layoutOpciones.setVisibility(config.incluido ? View.VISIBLE : View.GONE);
        holder.cbIncluir.setOnCheckedChangeListener((buttonView, isChecked) -> {
            config.incluido = isChecked;
            holder.layoutOpciones.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (changeListener != null) changeListener.onConfigChanged();
        });

        // Setup checkbox escaso
        holder.cbEscaso.setOnCheckedChangeListener(null);
        holder.cbEscaso.setChecked(config.escaso);
        holder.cbEscaso.setOnCheckedChangeListener((buttonView, isChecked) -> {
            config.escaso = isChecked;
            if (isChecked) {
                config.cantBasico = 0;
                config.cantFamiliar = 0;
                holder.etBasico.setText("0");
                holder.etFamiliar.setText("0");
            }
            if (changeListener != null) changeListener.onConfigChanged();
        });

        // Set input texts
        holder.etBasico.setText(formatearNumero(config.cantBasico));
        holder.etFamiliar.setText(formatearNumero(config.cantFamiliar));
        holder.etPremium.setText(formatearNumero(config.cantPremium));

        // Watchers for text changes
        holder.etBasico.addTextChangedListener(new SimpleWatcher(s -> {
            config.cantBasico = parseDouble(s);
            if (config.cantBasico > 0 && config.escaso) {
                config.escaso = false;
                holder.cbEscaso.setChecked(false);
            }
            if (changeListener != null) changeListener.onConfigChanged();
        }));

        holder.etFamiliar.addTextChangedListener(new SimpleWatcher(s -> {
            config.cantFamiliar = parseDouble(s);
            if (config.cantFamiliar > 0 && config.escaso) {
                config.escaso = false;
                holder.cbEscaso.setChecked(false);
            }
            if (changeListener != null) changeListener.onConfigChanged();
        }));

        holder.etPremium.addTextChangedListener(new SimpleWatcher(s -> {
            config.cantPremium = parseDouble(s);
            if (changeListener != null) changeListener.onConfigChanged();
        }));
    }

    private String formatearNumero(double d) {
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            return String.valueOf(d).replace(".", ",");
        }
    }

    private double parseDouble(String str) {
        if (str == null || str.trim().isEmpty()) return 0;
        try {
            return Double.parseDouble(str.trim().replace(",", "."));
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public List<ProductoCosecha> getProductosSeleccionados() {
        List<ProductoCosecha> result = new ArrayList<>();
        for (CultivoConfig cfg : items) {
            if (cfg.incluido) {
                String img = cfg.cultivo.getImagen() != null ? cfg.cultivo.getImagen() : "logo_tucosecha";
                result.add(new ProductoCosecha(
                        cfg.cultivo.getNombre(),
                        img,
                        cfg.unidad,
                        cfg.cantBasico,
                        cfg.cantFamiliar,
                        cfg.cantPremium,
                        cfg.escaso
                ));
            }
        }
        return result;
    }

    static class CultivoPublicarViewHolder extends RecyclerView.ViewHolder {
        final MaterialCheckBox cbIncluir;
        final ShapeableImageView imgFoto;
        final TextView tvNombre;
        final AutoCompleteTextView actvUnidad;
        final LinearLayout layoutOpciones;
        final MaterialCheckBox cbEscaso;
        final TextInputEditText etBasico, etFamiliar, etPremium;

        public CultivoPublicarViewHolder(@NonNull View itemView) {
            super(itemView);
            cbIncluir = itemView.findViewById(R.id.cbIncluirCultivo);
            imgFoto = itemView.findViewById(R.id.imgPublicarCultivo);
            tvNombre = itemView.findViewById(R.id.tvNombrePublicarCultivo);
            actvUnidad = itemView.findViewById(R.id.actvUnidadCultivo);
            layoutOpciones = itemView.findViewById(R.id.layoutOpcionesCantidades);
            cbEscaso = itemView.findViewById(R.id.cbEscasoPremium);
            etBasico = itemView.findViewById(R.id.etCantidadBasico);
            etFamiliar = itemView.findViewById(R.id.etCantidadFamiliar);
            etPremium = itemView.findViewById(R.id.etCantidadPremium);
        }
    }

    private static class SimpleWatcher implements TextWatcher {
        interface OnChanged { void call(String s); }
        private final OnChanged callback;

        SimpleWatcher(OnChanged callback) {
            this.callback = callback;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            callback.call(s != null ? s.toString() : "");
        }
        @Override public void afterTextChanged(Editable s) {}
    }
}
