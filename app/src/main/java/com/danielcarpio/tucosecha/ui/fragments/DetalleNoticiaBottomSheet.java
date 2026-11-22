package com.danielcarpio.tucosecha.ui.fragments;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Noticia;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DetalleNoticiaBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_NOTICIA_TITULO = "titulo";
    private static final String ARG_NOTICIA_RESUMEN = "resumen";
    private static final String ARG_NOTICIA_CONTENIDO = "contenido";
    private static final String ARG_NOTICIA_FECHA = "fecha";
    private static final String ARG_NOTICIA_CATEGORIA = "categoria";
    private static final String ARG_NOTICIA_AUTOR = "autor";
    private static final String ARG_NOTICIA_FOTO = "foto";

    public static DetalleNoticiaBottomSheet newInstance(Noticia noticia) {
        DetalleNoticiaBottomSheet fragment = new DetalleNoticiaBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_NOTICIA_TITULO, noticia.getTitulo());
        args.putString(ARG_NOTICIA_RESUMEN, noticia.getResumen());
        args.putString(ARG_NOTICIA_CONTENIDO, noticia.getContenido());
        if (noticia.getFecha() != null) {
            args.putLong(ARG_NOTICIA_FECHA, noticia.getFecha().toDate().getTime());
        }
        args.putString(ARG_NOTICIA_CATEGORIA, noticia.getCategoria());
        args.putString(ARG_NOTICIA_AUTOR, noticia.getAutor());
        args.putString(ARG_NOTICIA_FOTO, noticia.getFotoBase64());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_detalle_noticia, container, false);

        ImageView img = view.findViewById(R.id.imgBottomSheetNoticia);
        TextView tvTitulo = view.findViewById(R.id.tvBottomSheetTitulo);
        TextView tvContenido = view.findViewById(R.id.tvBottomSheetContenido);
        TextView tvFecha = view.findViewById(R.id.tvBottomSheetFecha);
        TextView tvAutor = view.findViewById(R.id.tvBottomSheetAutor);
        Chip chipCategoria = view.findViewById(R.id.chipBottomSheetCategoria);

        if (getArguments() != null) {
            tvTitulo.setText(getArguments().getString(ARG_NOTICIA_TITULO));
            tvContenido.setText(getArguments().getString(ARG_NOTICIA_CONTENIDO));
            String autor = getArguments().getString(ARG_NOTICIA_AUTOR);
            tvAutor.setText(autor != null && !autor.isEmpty() ? "Por " + autor : "");

            long time = getArguments().getLong(ARG_NOTICIA_FECHA, 0);
            if (time > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                tvFecha.setText(sdf.format(time));
            }

            String fotoBase64 = getArguments().getString(ARG_NOTICIA_FOTO);
            if (fotoBase64 != null && !fotoBase64.isEmpty()) {
                img.setVisibility(View.VISIBLE);
                AsyncTask.execute(() -> {
                    try {
                        byte[] decodedBytes = Base64.decode(fotoBase64, Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        if (bmp != null) {
                            img.post(() -> img.setImageBitmap(bmp));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                img.setVisibility(View.GONE);
            }

            String categoria = getArguments().getString(ARG_NOTICIA_CATEGORIA);
            if (categoria != null) {
                chipCategoria.setText(capitalize(categoria));
                switch (categoria.toLowerCase()) {
                    case "cosecha":
                        chipCategoria.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F57C1B")));
                        chipCategoria.setTextColor(Color.WHITE);
                        break;
                    case "consejos":
                        chipCategoria.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1E88E5")));
                        chipCategoria.setTextColor(Color.WHITE);
                        break;
                    case "aviso":
                        chipCategoria.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E8401C")));
                        chipCategoria.setTextColor(Color.WHITE);
                        break;
                    case "huerto":
                    default:
                        chipCategoria.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#3E8E2F")));
                        chipCategoria.setTextColor(Color.WHITE);
                        break;
                }
            }
        }

        return view;
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
