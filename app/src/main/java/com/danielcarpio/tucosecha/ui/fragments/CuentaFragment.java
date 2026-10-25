package com.danielcarpio.tucosecha.ui.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.danielcarpio.tucosecha.LoginActivity;
import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.RegistroActivity;
import com.danielcarpio.tucosecha.model.Usuario;
import com.danielcarpio.tucosecha.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CuentaFragment extends Fragment {

    private LinearLayout layoutGuestAccount, layoutLoggedAccount;
    private TextView tvUserName, tvUserEmail, tvUserRole, tvUserInitials;
    private Chip chipPlan;
    private MaterialButton btnLogout, btnMyPlan, btnLoginFromAccount, btnRegisterFromAccount;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cuenta, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        layoutGuestAccount = view.findViewById(R.id.layoutGuestAccount);
        layoutLoggedAccount = view.findViewById(R.id.layoutLoggedAccount);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserRole = view.findViewById(R.id.tvUserRole);
        tvUserInitials = view.findViewById(R.id.tvUserInitials);
        chipPlan = view.findViewById(R.id.chipPlan);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnMyPlan = view.findViewById(R.id.btnMyPlan);
        btnLoginFromAccount = view.findViewById(R.id.btnLoginFromAccount);
        btnRegisterFromAccount = view.findViewById(R.id.btnRegisterFromAccount);

        if (SessionManager.isEsInvitado()) {
            layoutGuestAccount.setVisibility(View.VISIBLE);
            layoutLoggedAccount.setVisibility(View.GONE);

            btnLoginFromAccount.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), LoginActivity.class));
                if (getActivity() != null) getActivity().finish();
            });

            btnRegisterFromAccount.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), RegistroActivity.class));
            });
        } else {
            layoutGuestAccount.setVisibility(View.GONE);
            layoutLoggedAccount.setVisibility(View.VISIBLE);
            loadUserData();
        }

        btnMyPlan.setOnClickListener(v -> {
            if (SessionManager.isEsInvitado()) {
                showGuestActionDialog();
            } else {
                Toast.makeText(getContext(), "Sección 'Mi plan' próximamente", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            SessionManager.setEsInvitado(false);
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }

    private void showGuestActionDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Acceso requerido")
                .setMessage("Para acceder a esta función debes iniciar sesión o crear una cuenta.")
                .setPositiveButton("Iniciar sesión", (dialog, which) -> {
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                    if (getActivity() != null) getActivity().finish();
                })
                .setNegativeButton("Registrarse", (dialog, which) -> {
                    startActivity(new Intent(getActivity(), RegistroActivity.class));
                })
                .setNeutralButton("Cancelar", null)
                .show();
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
                                String name = usuario.getNombre() != null ? usuario.getNombre() : "";
                                String surname = usuario.getApellidos() != null ? usuario.getApellidos() : "";

                                tvUserName.setText(name + " " + surname);
                                tvUserEmail.setText(usuario.getEmail());
                                tvUserRole.setText("Rol: " + usuario.getRol());

                                String plan = usuario.getPlan() != null ? usuario.getPlan() : "ninguno";
                                chipPlan.setText("Plan: " + plan);

                                if (plan.equalsIgnoreCase("ninguno")) {
                                    chipPlan.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                                    chipPlan.setTextColor(Color.parseColor("#424242"));
                                } else {
                                    chipPlan.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#C8E6C9")));
                                    chipPlan.setTextColor(Color.parseColor("#1F5A2A"));
                                }

                                String initials = "";
                                if (!name.isEmpty()) {
                                    initials += name.substring(0, 1).toUpperCase();
                                }
                                if (!surname.isEmpty()) {
                                    initials += surname.substring(0, 1).toUpperCase();
                                }
                                if (initials.isEmpty()) {
                                    initials = "TC";
                                }
                                tvUserInitials.setText(initials);
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
