package com.danielcarpio.tucosecha;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class OlvidoPassActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnRecover;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_olvido_pass);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        btnRecover = findViewById(R.id.btnRecover);

        btnRecover.setOnClickListener(v -> recoverPassword());
    }

    private void recoverPassword() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(OlvidoPassActivity.this, R.string.recover_sent_success, Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(OlvidoPassActivity.this, "Error al enviar el correo de recuperación.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
