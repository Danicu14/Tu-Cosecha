package com.danielcarpio.tucosecha.ui.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.danielcarpio.tucosecha.LoginActivity;
import com.danielcarpio.tucosecha.MiPlanActivity;
import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.RegistroActivity;
import com.danielcarpio.tucosecha.model.Usuario;
import com.danielcarpio.tucosecha.util.DatosEjemplo;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.danielcarpio.tucosecha.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CuentaFragment extends Fragment {

    private LinearLayout layoutGuestAccount, layoutLoggedAccount;
    private TextView tvUserName, tvUserEmail, tvUserInitials;
    private Chip chipPlan;
    private MaterialButton btnLogout, btnCargarEjemplo, btnCargarNoticiasEjemplo, btnCargarPlanesEjemplo, btnCargarCosechaEjemplo, btnLoginFromAccount, btnRegisterFromAccount;
    private View btnMyPlan, btnMisDatos, btnMisPedidos, btnOptionPedidosAdmin;
    private MaterialCardView cardAdminTools, cardPanelHortelano, cardOptionPedidosAdmin;
    private TextView tvBadgeCancelaciones;
    private com.google.firebase.firestore.ListenerRegistration cancelacionesListener;
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
        tvUserInitials = view.findViewById(R.id.tvUserInitials);
        chipPlan = view.findViewById(R.id.chipPlan);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnMyPlan = view.findViewById(R.id.btnMyPlan);
        btnMisDatos = view.findViewById(R.id.btnMisDatos);
        btnMisPedidos = view.findViewById(R.id.btnMisPedidos);
        btnCargarEjemplo = view.findViewById(R.id.btnCargarEjemplo);
        btnCargarNoticiasEjemplo = view.findViewById(R.id.btnCargarNoticiasEjemplo);
        btnCargarPlanesEjemplo = view.findViewById(R.id.btnCargarPlanesEjemplo);
        btnCargarCosechaEjemplo = view.findViewById(R.id.btnCargarCosechaEjemplo);
        cardAdminTools = view.findViewById(R.id.cardAdminTools);
        cardPanelHortelano = view.findViewById(R.id.cardPanelHortelano);
        cardOptionPedidosAdmin = view.findViewById(R.id.cardOptionPedidosAdmin);
        btnOptionPedidosAdmin = view.findViewById(R.id.btnOptionPedidosAdmin);
        tvBadgeCancelaciones = view.findViewById(R.id.tvBadgeCancelaciones);
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
                startActivity(new Intent(getActivity(), MiPlanActivity.class));
            }
        });

        btnMisDatos.setOnClickListener(v -> {
            if (SessionManager.isEsInvitado()) {
                showGuestActionDialog();
            } else {
                startActivity(new Intent(getActivity(), com.danielcarpio.tucosecha.MisDatosActivity.class));
            }
        });

        btnMisPedidos.setOnClickListener(v -> {
            if (SessionManager.isEsInvitado()) {
                showGuestActionDialog();
            } else {
                startActivity(new Intent(getActivity(), com.danielcarpio.tucosecha.MisPedidosActivity.class));
            }
        });

        View cardOptionMiPlan = view.findViewById(R.id.cardOptionMiPlan);
        if (cardOptionMiPlan != null) cardOptionMiPlan.setOnClickListener(v -> btnMyPlan.performClick());

        View cardOptionMisDatos = view.findViewById(R.id.cardOptionMisDatos);
        if (cardOptionMisDatos != null) cardOptionMisDatos.setOnClickListener(v -> btnMisDatos.performClick());

        View cardOptionMisPedidos = view.findViewById(R.id.cardOptionMisPedidos);
        if (cardOptionMisPedidos != null) cardOptionMisPedidos.setOnClickListener(v -> btnMisPedidos.performClick());

        if (btnOptionPedidosAdmin != null) {
            btnOptionPedidosAdmin.setOnClickListener(v -> startActivity(new Intent(getActivity(), com.danielcarpio.tucosecha.PedidosAdminActivity.class)));
        }
        if (cardOptionPedidosAdmin != null) {
            cardOptionPedidosAdmin.setOnClickListener(v -> {
                if (btnOptionPedidosAdmin != null) btnOptionPedidosAdmin.performClick();
            });
        }

        btnCargarEjemplo.setOnClickListener(v -> {
            btnCargarEjemplo.setEnabled(false);
            btnCargarEjemplo.setText("Cargando datos...");
            DatosEjemplo.cargarDatosEjemplo(new DatosEjemplo.OnDataLoadedListener() {
                @Override
                public void onSuccess(int count) {
                    if (isAdded()) {
                        btnCargarEjemplo.setEnabled(true);
                        btnCargarEjemplo.setText("Cargar datos de ejemplo");
                        Mensajes.mostrarExito(btnCargarEjemplo, count + " cultivos cargados");
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (isAdded()) {
                        btnCargarEjemplo.setEnabled(true);
                        btnCargarEjemplo.setText("Cargar datos de ejemplo");
                        Log.e("CARGA_DATOS", "Error al cargar datos: " + e.getMessage(), e);
                        Mensajes.mostrarError(btnCargarEjemplo, e.getMessage());
                    }
                }
            });
        });

        btnCargarNoticiasEjemplo.setOnClickListener(v -> {
            btnCargarNoticiasEjemplo.setEnabled(false);
            btnCargarNoticiasEjemplo.setText("Cargando noticias...");
            DatosEjemplo.cargarNoticiasEjemplo(new DatosEjemplo.OnNoticiasLoadedListener() {
                @Override
                public void onSuccess(int count) {
                    if (isAdded()) {
                        btnCargarNoticiasEjemplo.setEnabled(true);
                        btnCargarNoticiasEjemplo.setText("Cargar noticias de ejemplo");
                        if (count > 0) {
                            Mensajes.mostrarExito(btnCargarNoticiasEjemplo, count + " noticias cargadas");
                        } else {
                            Mensajes.mostrarInfo(btnCargarNoticiasEjemplo, "La colección de noticias ya tiene datos.");
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (isAdded()) {
                        btnCargarNoticiasEjemplo.setEnabled(true);
                        btnCargarNoticiasEjemplo.setText("Cargar noticias de ejemplo");
                        Log.e("CARGA_DATOS", "Error al cargar noticias: " + e.getMessage(), e);
                        Mensajes.mostrarError(btnCargarNoticiasEjemplo, e.getMessage());
                    }
                }
            });
        });

        btnCargarPlanesEjemplo.setOnClickListener(v -> {
            btnCargarPlanesEjemplo.setEnabled(false);
            btnCargarPlanesEjemplo.setText("Cargando planes...");
            DatosEjemplo.cargarPlanesEjemplo(new DatosEjemplo.OnPlanesLoadedListener() {
                @Override
                public void onSuccess(int count) {
                    if (isAdded()) {
                        btnCargarPlanesEjemplo.setEnabled(true);
                        btnCargarPlanesEjemplo.setText("Cargar planes de ejemplo");
                        if (count > 0) {
                            Mensajes.mostrarExito(btnCargarPlanesEjemplo, count + " planes cargados");
                        } else {
                            Mensajes.mostrarInfo(btnCargarPlanesEjemplo, "La colección de planes ya tiene datos.");
                        }
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (isAdded()) {
                        btnCargarPlanesEjemplo.setEnabled(true);
                        btnCargarPlanesEjemplo.setText("Cargar planes de ejemplo");
                        Log.e("CARGA_DATOS", "Error al cargar planes: " + e.getMessage(), e);
                        Mensajes.mostrarError(btnCargarPlanesEjemplo, e.getMessage());
                    }
                }
            });
        });

        btnCargarCosechaEjemplo.setOnClickListener(v -> {
            btnCargarCosechaEjemplo.setEnabled(false);
            btnCargarCosechaEjemplo.setText("Cargando cosecha...");
            DatosEjemplo.cargarCosechaEjemplo(new DatosEjemplo.OnCosechaLoadedListener() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        btnCargarCosechaEjemplo.setEnabled(true);
                        btnCargarCosechaEjemplo.setText("Cargar cosecha de ejemplo");
                        Mensajes.mostrarExito(btnCargarCosechaEjemplo, "Cosecha de ejemplo creada con éxito");
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (isAdded()) {
                        btnCargarCosechaEjemplo.setEnabled(true);
                        btnCargarCosechaEjemplo.setText("Cargar cosecha de ejemplo");
                        Log.e("CARGA_DATOS", "Error al cargar cosecha: " + e.getMessage(), e);
                        Mensajes.mostrarError(btnCargarCosechaEjemplo, e.getMessage());
                    }
                }
            });
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            SessionManager.isEsInvitado();
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
        new MaterialAlertDialogBuilder(requireContext())
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

                                // Check if role is administrador (evaluated after reading user document)
                                if ("administrador".equalsIgnoreCase(usuario.getRol())) {
                                    cardAdminTools.setVisibility(View.VISIBLE);
                                    if (cardPanelHortelano != null) cardPanelHortelano.setVisibility(View.VISIBLE);
                                    listenCancellationRequests();
                                } else {
                                    cardAdminTools.setVisibility(View.GONE);
                                    if (cardPanelHortelano != null) cardPanelHortelano.setVisibility(View.GONE);
                                    stopCancellationListener();
                                }

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
                        Log.e("CARGA_DATOS", "Error al cargar datos de usuario: " + e.getMessage(), e);
                        Mensajes.mostrarError(tvUserName, "Error al cargar datos: " + e.getMessage());
                    });
        }
    }

    private void listenCancellationRequests() {
        if (cancelacionesListener != null) cancelacionesListener.remove();
        cancelacionesListener = db.collection("pedidos")
                .whereEqualTo("estado", com.danielcarpio.tucosecha.util.EstadoPedido.CANCELACION_SOLICITADA)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;
                    if (value != null && tvBadgeCancelaciones != null) {
                        int count = value.size();
                        if (count > 0) {
                            tvBadgeCancelaciones.setText(String.valueOf(count));
                            tvBadgeCancelaciones.setVisibility(View.VISIBLE);
                        } else {
                            tvBadgeCancelaciones.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void stopCancellationListener() {
        if (cancelacionesListener != null) {
            cancelacionesListener.remove();
            cancelacionesListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopCancellationListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!SessionManager.isEsInvitado()) {
            loadUserData();
        }
    }
}
