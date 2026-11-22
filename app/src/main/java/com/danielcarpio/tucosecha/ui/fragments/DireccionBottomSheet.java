package com.danielcarpio.tucosecha.ui.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DireccionBottomSheet extends BottomSheetDialogFragment {

    public interface OnDireccionSavedListener {
        void onDireccionSaved(Map<String, Object> direccion);
    }

    private TextInputLayout tilCalle, tilPiso, tilCodigoPostal, tilTelefono, tilIndicaciones;
    private TextInputEditText etCalle, etPiso, etCodigoPostal, etTelefono, etIndicaciones;
    private MaterialButton btnGuardarDireccion;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private OnDireccionSavedListener listener;
    private Map<String, Object> existingDireccion;

    public static DireccionBottomSheet newInstance(Map<String, Object> direccion, OnDireccionSavedListener listener) {
        DireccionBottomSheet fragment = new DireccionBottomSheet();
        fragment.listener = listener;
        fragment.existingDireccion = direccion;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_direccion, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tilCalle = view.findViewById(R.id.tilCalle);
        tilPiso = view.findViewById(R.id.tilPiso);
        tilCodigoPostal = view.findViewById(R.id.tilCodigoPostal);
        tilTelefono = view.findViewById(R.id.tilTelefono);
        tilIndicaciones = view.findViewById(R.id.tilIndicaciones);

        etCalle = view.findViewById(R.id.etCalle);
        etPiso = view.findViewById(R.id.etPiso);
        etCodigoPostal = view.findViewById(R.id.etCodigoPostal);
        etTelefono = view.findViewById(R.id.etTelefono);
        etIndicaciones = view.findViewById(R.id.etIndicaciones);
        btnGuardarDireccion = view.findViewById(R.id.btnGuardarDireccion);

        if (existingDireccion != null) {
            if (existingDireccion.get("calle") != null) etCalle.setText(String.valueOf(existingDireccion.get("calle")));
            if (existingDireccion.get("piso") != null) etPiso.setText(String.valueOf(existingDireccion.get("piso")));
            if (existingDireccion.get("codigoPostal") != null) etCodigoPostal.setText(String.valueOf(existingDireccion.get("codigoPostal")));
            if (existingDireccion.get("telefono") != null) etTelefono.setText(String.valueOf(existingDireccion.get("telefono")));
            if (existingDireccion.get("indicaciones") != null) etIndicaciones.setText(String.valueOf(existingDireccion.get("indicaciones")));
        }

        btnGuardarDireccion.setOnClickListener(v -> guardarDireccion());

        return view;
    }

    private void guardarDireccion() {
        String calle = etCalle.getText() != null ? etCalle.getText().toString().trim() : "";
        String piso = etPiso.getText() != null ? etPiso.getText().toString().trim() : "";
        String codigoPostal = etCodigoPostal.getText() != null ? etCodigoPostal.getText().toString().trim() : "";
        String telefono = etTelefono.getText() != null ? etTelefono.getText().toString().trim() : "";
        String indicaciones = etIndicaciones.getText() != null ? etIndicaciones.getText().toString().trim() : "";

        boolean hasError = false;
        if (TextUtils.isEmpty(calle)) {
            tilCalle.setError("La calle es obligatoria");
            hasError = true;
        } else {
            tilCalle.setError(null);
        }

        if (TextUtils.isEmpty(codigoPostal)) {
            tilCodigoPostal.setError("El código postal es obligatorio");
            hasError = true;
        } else if (!codigoPostal.matches("\\d{5}")) {
            tilCodigoPostal.setError("Debe tener 5 dígitos");
            hasError = true;
        } else {
            tilCodigoPostal.setError(null);
        }

        if (TextUtils.isEmpty(telefono)) {
            tilTelefono.setError("El teléfono es obligatorio");
            hasError = true;
        } else if (!telefono.matches("\\d{9}")) {
            tilTelefono.setError("Debe tener 9 dígitos");
            hasError = true;
        } else {
            tilTelefono.setError(null);
        }

        if (hasError) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Mensajes.mostrarError(btnGuardarDireccion, "Usuario no autenticado");
            return;
        }

        Map<String, Object> direccionMap = new HashMap<>();
        direccionMap.put("calle", calle);
        direccionMap.put("piso", piso);
        direccionMap.put("codigoPostal", codigoPostal);
        direccionMap.put("telefono", telefono);
        direccionMap.put("localidad", "Talavera de la Reina");
        direccionMap.put("indicaciones", indicaciones);

        btnGuardarDireccion.setEnabled(false);
        btnGuardarDireccion.setText("Guardando...");

        db.collection("usuarios").document(user.getUid())
                .update("direccion", direccionMap)
                .addOnSuccessListener(aVoid -> {
                    Mensajes.mostrarExito(btnGuardarDireccion, "Dirección guardada correctamente");
                    if (listener != null) {
                        listener.onDireccionSaved(direccionMap);
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    btnGuardarDireccion.setEnabled(true);
                    btnGuardarDireccion.setText("Guardar dirección");
                    Mensajes.mostrarError(btnGuardarDireccion, "Error al guardar: " + e.getMessage());
                });
    }
}
