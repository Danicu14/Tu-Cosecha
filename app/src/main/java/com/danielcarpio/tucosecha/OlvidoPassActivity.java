package com.danielcarpio.tucosecha;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;

import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class OlvidoPassActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnRecover;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_olvido_pass);

        mAuth = FirebaseAuth.getInstance();
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnRecover = findViewById(R.id.btnRecover);

        etEmail.addTextChangedListener(new SimpleTextWatcher(() -> tilEmail.setError(null)));

        btnRecover.setOnClickListener(v -> recoverPassword());
    }

    private void recoverPassword() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("El correo electrónico es obligatorio");
            etEmail.requestFocus();
            return;
        }

        setLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Mensajes.mostrarExito(btnRecover, getString(R.string.recover_sent_success));
                        btnRecover.postDelayed(this::finish, 1500);
                    } else {
                        Mensajes.mostrarError(btnRecover, "Error al enviar el correo de recuperación.");
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        btnRecover.setEnabled(!isLoading);
        btnRecover.setText(isLoading ? "Enviando..." : getString(R.string.recover_button));
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
