package com.danielcarpio.tucosecha.ui.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Plan;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DetallePlanBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PLAN_ID = "id";
    private static final String ARG_PLAN_NOMBRE = "nombre";
    private static final String ARG_PLAN_DESC = "desc";
    private static final String ARG_PLAN_PRECIO = "precio";
    private static final String ARG_PLAN_PERSONAS = "personas";
    private static final String ARG_PLAN_PRODUCTOS = "productos";

    public static DetallePlanBottomSheet newInstance(Plan plan) {
        DetallePlanBottomSheet fragment = new DetallePlanBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PLAN_ID, plan.getId());
        args.putString(ARG_PLAN_NOMBRE, plan.getNombre());
        args.putString(ARG_PLAN_DESC, plan.getDescripcion());
        args.putDouble(ARG_PLAN_PRECIO, plan.getPrecioMensual());
        args.putString(ARG_PLAN_PERSONAS, plan.getPersonas());
        args.putInt(ARG_PLAN_PRODUCTOS, plan.getProductosPorCesta());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_detalle_plan, container, false);

        TextView tvNombre = view.findViewById(R.id.tvPlanNombre);
        TextView tvPrecio = view.findViewById(R.id.tvPlanPrecio);
        TextView tvPersonas = view.findViewById(R.id.tvPlanPersonas);
        TextView tvProductos = view.findViewById(R.id.tvPlanProductos);
        TextView tvAvisoSimulacion = view.findViewById(R.id.tvAvisoSimulacion);
        MaterialButton btnConfirmar = view.findViewById(R.id.btnConfirmarSuscripcion);

        if (getArguments() != null) {
            String planId = getArguments().getString(ARG_PLAN_ID);
            String nombre = getArguments().getString(ARG_PLAN_NOMBRE);
            double precio = getArguments().getDouble(ARG_PLAN_PRECIO);
            String personas = getArguments().getString(ARG_PLAN_PERSONAS);
            int productos = getArguments().getInt(ARG_PLAN_PRODUCTOS);

            tvNombre.setText(nombre);
            tvPrecio.setText(precio + " € / mes");
            tvPersonas.setText("Para " + personas);
            tvProductos.setText(productos + " productos por cesta semanal");
            tvAvisoSimulacion.setText("⚠️ Aviso: Este es un pago simulado para la demo de la aplicación.");

            btnConfirmar.setOnClickListener(v -> {
                btnConfirmar.setEnabled(false);
                btnConfirmar.setText("Procesando pago...");

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        String uid = user.getUid();
                        String fechaActual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("plan", planId);
                        updates.put("estadoSuscripcion", "activa");
                        updates.put("fechaInicioPlan", fechaActual);

                        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Mensajes.mostrarExito(btnConfirmar, "¡Suscripción al " + nombre + " completada con éxito!");
                                    dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    btnConfirmar.setEnabled(true);
                                    btnConfirmar.setText("Confirmar suscripción");
                                    Mensajes.mostrarError(btnConfirmar, "Error al procesar suscripción: " + e.getMessage());
                                });
                    }
                }, 2000);
            });
        }

        return view;
    }
}
