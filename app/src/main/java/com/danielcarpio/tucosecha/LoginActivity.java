package com.danielcarpio.tucosecha;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.danielcarpio.tucosecha.util.GestorSesion;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGuest;
    private TextView tvForgotPassword, tvRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGuest = findViewById(R.id.btnGuest);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);

        // Limpiar errores al escribir
        etEmail.addTextChangedListener(new SimpleTextWatcher(() -> tilEmail.setError(null)));
        etPassword.addTextChangedListener(new SimpleTextWatcher(() -> tilPassword.setError(null)));

        btnLogin.setOnClickListener(v -> iniciarSesionUsuario());

        btnGuest.setOnClickListener(v -> {
            GestorSesion.setEsInvitado(true);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, OlvidoPassActivity.class)));

        tvRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegistroActivity.class)));
    }

    private void iniciarSesionUsuario() {
        String correo = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String contrasena = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        boolean tieneError = false;

        if (TextUtils.isEmpty(correo)) {
            tilEmail.setError("El correo electrónico es obligatorio");
            tieneError = true;
        }

        if (TextUtils.isEmpty(contrasena)) {
            tilPassword.setError("La contraseña es obligatoria");
            tieneError = true;
        }

        if (tieneError) {
            if (TextUtils.isEmpty(correo)) {
                etEmail.requestFocus();
            } else if (TextUtils.isEmpty(contrasena)) {
                etPassword.requestFocus();
            }
            return;
        }

        // Estado de carga
        establecerCargando(true);

        mAuth.signInWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(this, tarea -> {
                    establecerCargando(false);
                    if (tarea.isSuccessful()) {
                        GestorSesion.setEsInvitado(false);
                        Mensajes.mostrarExito(btnLogin, "¡Bienvenido de nuevo!");
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String mensajeError = "Error al iniciar sesión. Comprueba tus credenciales.";
                        if (tarea.getException() != null) {
                            String exMensaje = tarea.getException().getMessage();
                            if (exMensaje != null && exMensaje.contains("network")) {
                                mensajeError = "Sin conexión a internet.";
                            } else if (exMensaje != null && (exMensaje.contains("password") || exMensaje.contains("user") || exMensaje.contains("no user"))) {
                                mensajeError = "Credenciales incorrectas o usuario no registrado.";
                            }
                        }
                        Mensajes.mostrarError(btnLogin, mensajeError);
                    }
                });
    }

    private void establecerCargando(boolean cargando) {
        btnLogin.setEnabled(!cargando);
        btnGuest.setEnabled(!cargando);
        btnLogin.setText(cargando ? "Iniciando sesión..." : getString(R.string.login_button));
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
