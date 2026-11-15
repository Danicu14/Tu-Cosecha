package com.danielcarpio.tucosecha;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.danielcarpio.tucosecha.model.Usuario;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MisDatosActivity extends AppCompatActivity {

    private TextInputEditText etNombre, etApellidos, etEmail, etCalle, etPiso, etCodigoPostal, etTelefono, etIndicaciones;
    private MaterialButton btnGuardar, btnCambiarPassword;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_datos);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbarMisDatos);
        toolbar.setNavigationOnClickListener(v -> finish());

        etNombre = findViewById(R.id.etMisDatosNombre);
        etApellidos = findViewById(R.id.etMisDatosApellidos);
        etEmail = findViewById(R.id.etMisDatosEmail);
        etCalle = findViewById(R.id.etMisDatosCalle);
        etPiso = findViewById(R.id.etMisDatosPiso);
        etCodigoPostal = findViewById(R.id.etMisDatosCodigoPostal);
        etTelefono = findViewById(R.id.etMisDatosTelefono);
        etIndicaciones = findViewById(R.id.etMisDatosIndicaciones);

        btnGuardar = findViewById(R.id.btnGuardarMisDatos);
        btnCambiarPassword = findViewById(R.id.btnCambiarPassword);

        loadUserData();

        btnGuardar.setOnClickListener(v -> guardarCambios());

        btnCambiarPassword.setOnClickListener(v -> cambiarPassword());
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("usuarios").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Usuario usuario = documentSnapshot.toObject(Usuario.class);
                            if (usuario != null) {
                                etNombre.setText(usuario.getNombre());
                                etApellidos.setText(usuario.getApellidos());
                                etEmail.setText(usuario.getEmail());

                                Map<String, Object> dir = usuario.getDireccion();
                                if (dir != null) {
                                    if (dir.get("calle") != null) etCalle.setText(String.valueOf(dir.get("calle")));
                                    if (dir.get("piso") != null) etPiso.setText(String.valueOf(dir.get("piso")));
                                    if (dir.get("codigoPostal") != null) etCodigoPostal.setText(String.valueOf(dir.get("codigoPostal")));
                                    if (dir.get("telefono") != null) etTelefono.setText(String.valueOf(dir.get("telefono")));
                                    if (dir.get("indicaciones") != null) etIndicaciones.setText(String.valueOf(dir.get("indicaciones")));
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> Mensajes.mostrarError(btnGuardar, "Error al cargar datos: " + e.getMessage()));
        }
    }

    private void guardarCambios() {
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String apellidos = etApellidos.getText() != null ? etApellidos.getText().toString().trim() : "";
        String calle = etCalle.getText() != null ? etCalle.getText().toString().trim() : "";
        String piso = etPiso.getText() != null ? etPiso.getText().toString().trim() : "";
        String codigoPostal = etCodigoPostal.getText() != null ? etCodigoPostal.getText().toString().trim() : "";
        String telefono = etTelefono.getText() != null ? etTelefono.getText().toString().trim() : "";
        String indicaciones = etIndicaciones.getText() != null ? etIndicaciones.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("El nombre es obligatorio");
            etNombre.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(apellidos)) {
            etApellidos.setError("Los apellidos son obligatorios");
            etApellidos.requestFocus();
            return;
        }

        if (!TextUtils.isEmpty(codigoPostal) && !codigoPostal.matches("\\d{5}")) {
            etCodigoPostal.setError("Debe tener 5 dígitos");
            etCodigoPostal.requestFocus();
            return;
        }

        if (!TextUtils.isEmpty(telefono) && !telefono.matches("\\d{9}")) {
            etTelefono.setError("Debe tener 9 dígitos");
            etTelefono.requestFocus();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Mensajes.mostrarError(btnGuardar, "Usuario no autenticado");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("nombre", nombre);
        updates.put("apellidos", apellidos);

        Map<String, Object> dirMap = new HashMap<>();
        dirMap.put("calle", calle);
        dirMap.put("piso", piso);
        dirMap.put("codigoPostal", codigoPostal);
        dirMap.put("telefono", telefono);
        dirMap.put("localidad", "Talavera de la Reina");
        dirMap.put("indicaciones", indicaciones);
        updates.put("direccion", dirMap);

        btnGuardar.setEnabled(false);
        btnGuardar.setText("Guardando...");

        db.collection("usuarios").document(user.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("Guardar cambios");
                    Mensajes.mostrarExito(btnGuardar, "Datos actualizados correctamente");
                })
                .addOnFailureListener(e -> {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("Guardar cambios");
                    Mensajes.mostrarError(btnGuardar, "Error al actualizar: " + e.getMessage());
                });
    }

    private void cambiarPassword() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            mAuth.sendPasswordResetEmail(user.getEmail())
                    .addOnSuccessListener(aVoid -> Mensajes.mostrarExito(btnCambiarPassword, getString(R.string.recover_sent_success)))
                    .addOnFailureListener(e -> Mensajes.mostrarError(btnCambiarPassword, "Error: " + e.getMessage()));
        } else {
            Mensajes.mostrarError(btnCambiarPassword, "No se pudo obtener el correo del usuario");
        }
    }
}
