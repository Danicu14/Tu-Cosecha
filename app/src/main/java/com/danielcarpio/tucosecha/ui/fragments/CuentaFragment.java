package com.danielcarpio.tucosecha.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.danielcarpio.tucosecha.LoginActivity;
import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Usuario;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CuentaFragment extends Fragment {

    private TextView tvUserName, tvUserEmail, tvUserRole, tvUserPlan;
    private MaterialButton btnLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cuenta, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserRole = view.findViewById(R.id.tvUserRole);
        tvUserPlan = view.findViewById(R.id.tvUserPlan);
        btnLogout = view.findViewById(R.id.btnLogout);

        loadUserData();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("usuarios").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Usuario usuario = documentSnapshot.toObject(Usuario.class);
                            if (usuario != null) {
                                tvUserName.setText("Nombre: " + usuario.getNombre() + " " + usuario.getApellidos());
                                tvUserEmail.setText("Correo: " + usuario.getEmail());
                                tvUserRole.setText("Rol: " + usuario.getRol());
                                tvUserPlan.setText("Plan: " + usuario.getPlan());
                            }
                        } else {
                            tvUserName.setText("No se encontró información del usuario.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al cargar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
