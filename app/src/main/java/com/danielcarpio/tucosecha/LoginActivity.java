package com.danielcarpio.tucosecha;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.danielcarpio.tucosecha.util.Mensajes;
import com.danielcarpio.tucosecha.util.SessionManager;
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

        // Clear errors on typing
        etEmail.addTextChangedListener(new SimpleTextWatcher(() -> tilEmail.setError(null)));
        etPassword.addTextChangedListener(new SimpleTextWatcher(() -> tilPassword.setError(null)));

        btnLogin.setOnClickListener(v -> loginUser());

        btnGuest.setOnClickListener(v -> {
            SessionManager.setEsInvitado(true);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, OlvidoPassActivity.class)));

        tvRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegistroActivity.class)));
    }

    private void loginUser() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        boolean hasError = false;

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("El correo electrónico es obligatorio");
            hasError = true;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("La contraseña es obligatoria");
            hasError = true;
        }

        if (hasError) {
            if (TextUtils.isEmpty(email)) {
                etEmail.requestFocus();
            } else if (TextUtils.isEmpty(password)) {
                etPassword.requestFocus();
            }
            return;
        }

        // Loading state
        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        SessionManager.setEsInvitado(false);
                        Mensajes.mostrarExito(btnLogin, "¡Bienvenido de nuevo!");
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMsg = "Error al iniciar sesión. Comprueba tus credenciales.";
                        if (task.getException() != null) {
                            String exMsg = task.getException().getMessage();
                            if (exMsg != null && exMsg.contains("network")) {
                                errorMsg = "Sin conexión a internet.";
                            } else if (exMsg != null && (exMsg.contains("password") || exMsg.contains("user") || exMsg.contains("no user"))) {
                                errorMsg = "Credenciales incorrectas o usuario no registrado.";
                            }
                        }
                        Mensajes.mostrarError(btnLogin, errorMsg);
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        btnGuest.setEnabled(!isLoading);
        btnLogin.setText(isLoading ? "Iniciando sesión..." : getString(R.string.login_button));
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable onTextChanged;

        public SimpleTextWatcher(Runnable onTextChanged) {
            this.onTextChanged = onTextChanged;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onTextChanged.run();
        }

        @Override
        public void afterTextChanged(Editable s) {}
    }
}
