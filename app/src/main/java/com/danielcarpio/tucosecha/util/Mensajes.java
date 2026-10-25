package com.danielcarpio.tucosecha.util;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.core.view.ViewCompat;

import com.google.android.material.snackbar.Snackbar;

public class Mensajes {

    public static void mostrarError(View view, String mensaje) {
        mostrarSnackbar(view, mensaje, Color.parseColor("#E8401C"));
    }

    public static void mostrarExito(View view, String mensaje) {
        mostrarSnackbar(view, mensaje, Color.parseColor("#3E8E2F"));
    }

    public static void mostrarInfo(View view, String mensaje) {
        mostrarSnackbar(view, mensaje, Color.parseColor("#1F5A2A"));
    }

    private static void mostrarSnackbar(View view, String mensaje, int backgroundColor) {
        if (view == null) return;
        Snackbar snackbar = Snackbar.make(view, mensaje, Snackbar.LENGTH_LONG);
        View snackBarView = snackbar.getView();
        ViewCompat.setBackgroundTintList(snackBarView, ColorStateList.valueOf(backgroundColor));
        
        TextView tv = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (tv != null) {
            tv.setTextColor(Color.parseColor("#FFFDF7"));
            tv.setTextSize(14f);
        }
        snackbar.show();
    }
}
