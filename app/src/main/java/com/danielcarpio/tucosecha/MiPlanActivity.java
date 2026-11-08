package com.danielcarpio.tucosecha;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.danielcarpio.tucosecha.model.Plan;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MiPlanActivity extends AppCompatActivity {

    private TextView tvNombre, tvPrecio, tvProductos, tvFecha, tvEstado;
    private MaterialButton btnCambiarPlan, btnCancelarSuscripcion;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mi_plan);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbarMiPlan);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvNombre = findViewById(R.id.tvMiPlanNombre);
        tvPrecio = findViewById(R.id.tvMiPlanPrecio);
        tvProductos = findViewById(R.id.tvMiPlanProductos);
        tvFecha = findViewById(R.id.tvMiPlanFecha);
        tvEstado = findViewById(R.id.tvMiPlanEstado);
        btnCambiarPlan = findViewById(R.id.btnCambiarPlan);
        btnCancelarSuscripcion = findViewById(R.id.btnCancelarSuscripcion);

        loadCurrentPlan();

        btnCambiarPlan.setOnClickListener(v -> finish());

        btnCancelarSuscripcion.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Cancelar suscripción")
                    .setMessage("¿Estás seguro de que deseas cancelar tu suscripción actual? Perderás los beneficios del plan.")
                    .setPositiveButton("Sí, cancelar", (dialog, which) -> cancelarSuscripcion())
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void loadCurrentPlan() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("usuarios").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String planId = documentSnapshot.getString("plan");
                            String estado = documentSnapshot.getString("estadoSuscripcion");
                            String fecha = documentSnapshot.getString("fechaInicioPlan");

                            if (planId != null && !planId.equalsIgnoreCase("ninguno")) {
                                db.collection("planes").document(planId).get()
                                        .addOnSuccessListener(planDoc -> {
                                            if (planDoc.exists()) {
                                                Plan plan = planDoc.toObject(Plan.class);
                                                if (plan != null) {
                                                    tvNombre.setText(plan.getNombre());
                                                    tvPrecio.setText(plan.getPrecioMensual() + " € / mes");
                                                    tvProductos.setText(plan.getProductosPorCesta() + " productos por cesta semanal");
                                                    tvFecha.setText("Fecha de inicio: " + (fecha != null ? fecha : "-"));
                                                    tvEstado.setText("Estado: " + (estado != null ? estado : "activa"));
                                                }
                                            }
                                        });
                            } else {
                                tvNombre.setText("Sin plan activo");
                                tvPrecio.setText("-");
                                tvProductos.setText("-");
                                tvFecha.setText("-");
                                tvEstado.setText("Estado: inactivo");
                            }
                        }
                    });
        }
    }

    private void cancelarSuscripcion() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("plan", "ninguno");
            updates.put("estadoSuscripcion", "cancelada");

            db.collection("usuarios").document(user.getUid()).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Mensajes.mostrarExito(btnCancelarSuscripcion, "Suscripción cancelada correctamente");
                        loadCurrentPlan();
                    })
                    .addOnFailureListener(e -> {
                        Mensajes.mostrarError(btnCancelarSuscripcion, "Error al cancelar: " + e.getMessage());
                    });
        }
    }
}
