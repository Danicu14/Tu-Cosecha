package com.danielcarpio.tucosecha;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.danielcarpio.tucosecha.model.Usuario;
import com.danielcarpio.tucosecha.util.GestorSesion;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegistroActivity extends AppCompatActivity {

    private TextInputLayout tilNombre, tilApellidos, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etNombre, etApellidos, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tilNombre = findViewById(R.id.tilName);
        tilApellidos = findViewById(R.id.tilSurname);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etNombre = findViewById(R.id.etName);
        etApellidos = findViewById(R.id.etSurname);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        // Limpiar errores al escribir
        etNombre.addTextChangedListener(new SimpleTextWatcher(() -> tilNombre.setError(null)));
        etApellidos.addTextChangedListener(new SimpleTextWatcher(() -> tilApellidos.setError(null)));
        etEmail.addTextChangedListener(new SimpleTextWatcher(() -> tilEmail.setError(null)));
        etPassword.addTextChangedListener(new SimpleTextWatcher(() -> tilPassword.setError(null)));
        etConfirmPassword.addTextChangedListener(new SimpleTextWatcher(() -> tilConfirmPassword.setError(null)));

        btnRegister.setOnClickListener(v -> registrarUsuario());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String apellidos = etApellidos.getText() != null ? etApellidos.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmarPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        View vistaEnfoque = null;
        boolean tieneError = false;

        if (TextUtils.isEmpty(nombre)) {
            tilNombre.setError("El nombre es obligatorio");
            vistaEnfoque = etNombre;
            tieneError = true;
        }

        if (TextUtils.isEmpty(apellidos)) {
            tilApellidos.setError("Los apellidos son obligatorios");
            if (!tieneError) vistaEnfoque = etApellidos;
            tieneError = true;
        }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("El correo es obligatorio");
            if (!tieneError) vistaEnfoque = etEmail;
            tieneError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Formato de correo inválido");
            if (!tieneError) vistaEnfoque = etEmail;
            tieneError = true;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("La contraseña es obligatoria");
            if (!tieneError) vistaEnfoque = etPassword;
            tieneError = true;
        } else if (password.length() < 6) {
            tilPassword.setError("Mínimo 6 caracteres");
            if (!tieneError) vistaEnfoque = etPassword;
            tieneError = true;
        }

        if (TextUtils.isEmpty(confirmarPassword)) {
            tilConfirmPassword.setError("Confirma la contraseña");
            if (!tieneError) vistaEnfoque = etConfirmPassword;
            tieneError = true;
        } else if (!password.equals(confirmarPassword)) {
            tilConfirmPassword.setError("Las contraseñas no coinciden");
            if (!tieneError) vistaEnfoque = etConfirmPassword;
            tieneError = true;
        }

        if (tieneError) {
            if (vistaEnfoque != null) vistaEnfoque.requestFocus();
            return;
        }

        establecerCargando(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, tarea -> {
                    if (tarea.isSuccessful() && tarea.getResult().getUser() != null) {
                        String uid = tarea.getResult().getUser().getUid();
                        String fechaAlta = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

                        Usuario usuario = new Usuario(uid, nombre, apellidos, email, "suscriptor", "ninguno", fechaAlta);

                        db.collection("usuarios").document(uid)
                                .set(usuario)
                                .addOnSuccessListener(aVoid -> {
                                    establecerCargando(false);
                                    GestorSesion.setEsInvitado(false);
                                    Mensajes.mostrarExito(btnRegister, "¡Cuenta creada con éxito!");
                                    Intent intent = new Intent(RegistroActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    establecerCargando(false);
                                    Mensajes.mostrarError(btnRegister, "Error al guardar datos: " + e.getMessage());
                                });
                    } else {
                        establecerCargando(false);
                        String mensajeError = "Error al registrar usuario.";
                        if (tarea.getException() != null) {
                            String exMensaje = tarea.getException().getMessage();
                            if (exMensaje != null && exMensaje.contains("email-already-in-use")) {
                                mensajeError = "El correo ya está registrado.";
                            } else if (exMensaje != null && exMensaje.contains("network")) {
                                mensajeError = "Sin conexión a internet.";
                            }
                        }
                        Mensajes.mostrarError(btnRegister, mensajeError);
                    }
                });
    }

    private void establecerCargando(boolean cargando) {
        btnRegister.setEnabled(!cargando);
        btnRegister.setText(cargando ? "Registrando..." : getString(R.string.register_button));
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable alCambiarTexto;

        public SimpleTextWatcher(Runnable alCambiarTexto) {
            this.alCambiarTexto = alCambiarTexto;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            alCambiarTexto.run();
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}
