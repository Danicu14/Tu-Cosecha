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
import com.danielcarpio.tucosecha.util.Mensajes;
import com.danielcarpio.tucosecha.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegistroActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilSurname, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etName, etSurname, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tilName = findViewById(R.id.tilName);
        tilSurname = findViewById(R.id.tilSurname);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etName = findViewById(R.id.etName);
        etSurname = findViewById(R.id.etSurname);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        // Clear errors on typing
        etName.addTextChangedListener(new SimpleTextWatcher(() -> tilName.setError(null)));
        etSurname.addTextChangedListener(new SimpleTextWatcher(() -> tilSurname.setError(null)));
        etEmail.addTextChangedListener(new SimpleTextWatcher(() -> tilEmail.setError(null)));
        etPassword.addTextChangedListener(new SimpleTextWatcher(() -> tilPassword.setError(null)));
        etConfirmPassword.addTextChangedListener(new SimpleTextWatcher(() -> tilConfirmPassword.setError(null)));

        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String surname = etSurname.getText() != null ? etSurname.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        View focusView = null;
        boolean hasError = false;

        if (TextUtils.isEmpty(name)) {
            tilName.setError("El nombre es obligatorio");
            focusView = etName;
            hasError = true;
        }

        if (TextUtils.isEmpty(surname)) {
            tilSurname.setError("Los apellidos son obligatorios");
            if (!hasError) focusView = etSurname;
            hasError = true;
        }

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("El correo es obligatorio");
            if (!hasError) focusView = etEmail;
            hasError = true;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Formato de correo inválido");
            if (!hasError) focusView = etEmail;
            hasError = true;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("La contraseña es obligatoria");
            if (!hasError) focusView = etPassword;
            hasError = true;
        } else if (password.length() < 6) {
            tilPassword.setError("Mínimo 6 caracteres");
            if (!hasError) focusView = etPassword;
            hasError = true;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Confirma la contraseña");
            if (!hasError) focusView = etConfirmPassword;
            hasError = true;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Las contraseñas no coinciden");
            if (!hasError) focusView = etConfirmPassword;
            hasError = true;
        }

        if (hasError) {
            if (focusView != null) focusView.requestFocus();
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        String fechaAlta = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

                        Usuario usuario = new Usuario(uid, name, surname, email, "suscriptor", "ninguno", fechaAlta);

                        db.collection("usuarios").document(uid)
                                .set(usuario)
                                .addOnSuccessListener(aVoid -> {
                                    setLoading(false);
                                    SessionManager.setEsInvitado(false);
                                    Mensajes.mostrarExito(btnRegister, "¡Cuenta creada con éxito!");
                                    Intent intent = new Intent(RegistroActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    Mensajes.mostrarError(btnRegister, "Error al guardar datos: " + e.getMessage());
                                });
                    } else {
                        setLoading(false);
                        String errorMsg = "Error al registrar usuario.";
                        if (task.getException() != null) {
                            String exMsg = task.getException().getMessage();
                            if (exMsg != null && exMsg.contains("email-already-in-use")) {
                                errorMsg = "El correo ya está registrado.";
                            } else if (exMsg != null && exMsg.contains("network")) {
                                errorMsg = "Sin conexión a internet.";
                            }
                        }
                        Mensajes.mostrarError(btnRegister, errorMsg);
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        btnRegister.setEnabled(!isLoading);
        btnRegister.setText(isLoading ? "Registrando..." : getString(R.string.register_button));
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
