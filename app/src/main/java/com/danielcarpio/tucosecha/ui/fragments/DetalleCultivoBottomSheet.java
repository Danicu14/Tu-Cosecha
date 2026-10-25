package com.danielcarpio.tucosecha.ui.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

public class DetalleCultivoBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_CULTIVO_NOMBRE = "nombre";
    private static final String ARG_CULTIVO_IMAGEN = "imagen";
    private static final String ARG_CULTIVO_FASE = "fase";
    private static final String ARG_CULTIVO_PROGRESO = "progreso";
    private static final String ARG_CULTIVO_SIEMBRA = "siembra";
    private static final String ARG_CULTIVO_COSECHA = "cosecha";
    private static final String ARG_CULTIVO_DESCRIPCION = "descripcion";

    public static DetalleCultivoBottomSheet newInstance(Cultivo cultivo) {
        DetalleCultivoBottomSheet fragment = new DetalleCultivoBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_CULTIVO_NOMBRE, cultivo.getNombre());
        args.putString(ARG_CULTIVO_IMAGEN, cultivo.getImagen());
        args.putString(ARG_CULTIVO_FASE, cultivo.getFase());
        args.putInt(ARG_CULTIVO_PROGRESO, cultivo.getProgreso());
        args.putString(ARG_CULTIVO_SIEMBRA, cultivo.getFechaSiembra());
        args.putString(ARG_CULTIVO_COSECHA, cultivo.getFechaCosechaEstimada());
        args.putString(ARG_CULTIVO_DESCRIPCION, cultivo.getDescripcion());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_detalle_cultivo, container, false);

        ImageView img = view.findViewById(R.id.imgBottomSheetCultivo);
        TextView tvNombre = view.findViewById(R.id.tvBottomSheetNombre);
        TextView tvDesc = view.findViewById(R.id.tvBottomSheetDescripcion);
        TextView tvProgresoText = view.findViewById(R.id.tvBottomSheetProgreso);
        TextView tvSiembra = view.findViewById(R.id.tvBottomSheetSiembra);
        TextView tvCosecha = view.findViewById(R.id.tvBottomSheetCosecha);
        Chip chipFase = view.findViewById(R.id.chipBottomSheetFase);
        ProgressBar progressBar = view.findViewById(R.id.progressBottomSheet);

        if (getArguments() != null) {
            String nombre = getArguments().getString(ARG_CULTIVO_NOMBRE);
            String imagen = getArguments().getString(ARG_CULTIVO_IMAGEN);
            String fase = getArguments().getString(ARG_CULTIVO_FASE);
            int progreso = getArguments().getInt(ARG_CULTIVO_PROGRESO, 0);
            String siembra = getArguments().getString(ARG_CULTIVO_SIEMBRA);
            String cosecha = getArguments().getString(ARG_CULTIVO_COSECHA);
            String descripcion = getArguments().getString(ARG_CULTIVO_DESCRIPCION);

            tvNombre.setText(nombre);
            tvDesc.setText(descripcion);
            tvProgresoText.setText(progreso + "%");
            progressBar.setProgress(progreso);
            tvSiembra.setText("Fecha de siembra: " + (siembra != null ? siembra : "-"));
            tvCosecha.setText("Fecha estimada de cosecha: " + (cosecha != null ? cosecha : "-"));

            if (fase != null) {
                chipFase.setText(capitalize(fase));
                if (fase.equalsIgnoreCase("listo")) {
                    chipFase.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#3E8E2F")));
                    chipFase.setTextColor(Color.WHITE);
                } else if (fase.equalsIgnoreCase("creciendo")) {
                    chipFase.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#FFB300")));
                    chipFase.setTextColor(Color.BLACK);
                } else if (fase.equalsIgnoreCase("sembrado")) {
                    chipFase.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#8B5A2B")));
                    chipFase.setTextColor(Color.WHITE);
                }
            }

            if (getContext() != null && imagen != null) {
                int resId = getContext().getResources().getIdentifier(imagen, "drawable", getContext().getPackageName());
                if (resId == 0) {
                    resId = R.drawable.logo_tucosecha;
                }
                img.setImageResource(resId);
            }
        }

        return view;
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
