package com.danielcarpio.tucosecha.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.MiPlanActivity;
import com.danielcarpio.tucosecha.PlanesActivity;
import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.RegistroActivity;
import com.danielcarpio.tucosecha.adapter.ProductoCosechaAdapter;
import com.danielcarpio.tucosecha.model.Cosecha;
import com.danielcarpio.tucosecha.model.ProductoCosecha;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.danielcarpio.tucosecha.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CestaFragment extends Fragment {

    private LinearLayout layoutCestaActive, layoutCestaNoPlan, layoutCestaError;
    private ProgressBar progressCestaLoading;
    private TextView tvCestaEmpty, tvCestaRecogidaInfo, tvCestaDiasFaltan, tvPremiumPriorityNote;
    private Chip chipActivePlan;
    private MaterialButton btnVerMiPlanCesta, btnVerPlanesCesta, btnCrearCuentaCesta, btnCestaRetry;
    private RecyclerView rvProductosCesta, rvProductosCestaGuest;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration harvestListener;
    private ListenerRegistration userListener;

    private List<ProductoCosecha> productoListActive;
    private List<ProductoCosecha> productoListGuest;
    private ProductoCosechaAdapter activeAdapter;
    private ProductoCosechaAdapter guestAdapter;

    private String userPlan = "ninguno";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cesta, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        layoutCestaActive = view.findViewById(R.id.layoutCestaActive);
        layoutCestaNoPlan = view.findViewById(R.id.layoutCestaNoPlan);
        layoutCestaError = view.findViewById(R.id.layoutCestaError);
        progressCestaLoading = view.findViewById(R.id.progressCestaLoading);
        tvCestaEmpty = view.findViewById(R.id.tvCestaEmpty);
        tvCestaRecogidaInfo = view.findViewById(R.id.tvCestaRecogidaInfo);
        tvCestaDiasFaltan = view.findViewById(R.id.tvCestaDiasFaltan);
        tvPremiumPriorityNote = view.findViewById(R.id.tvPremiumPriorityNote);
        chipActivePlan = view.findViewById(R.id.chipActivePlan);
        btnVerMiPlanCesta = view.findViewById(R.id.btnVerMiPlanCesta);
        btnVerPlanesCesta = view.findViewById(R.id.btnVerPlanesCesta);
        btnCrearCuentaCesta = view.findViewById(R.id.btnCrearCuentaCesta);
        btnCestaRetry = view.findViewById(R.id.btnCestaRetry);
        rvProductosCesta = view.findViewById(R.id.rvProductosCesta);
        rvProductosCestaGuest = view.findViewById(R.id.rvProductosCestaGuest);

        rvProductosCesta.setLayoutManager(new LinearLayoutManager(getContext()));
        rvProductosCestaGuest.setLayoutManager(new LinearLayoutManager(getContext()));

        productoListActive = new ArrayList<>();
        productoListGuest = new ArrayList<>();

        activeAdapter = new ProductoCosechaAdapter(getContext(), productoListActive, "ninguno");
        guestAdapter = new ProductoCosechaAdapter(getContext(), productoListGuest, "ninguno");

        rvProductosCesta.setAdapter(activeAdapter);
        rvProductosCestaGuest.setAdapter(guestAdapter);

        if (SessionManager.isEsInvitado()) {
            userPlan = "invitado";
            btnCrearCuentaCesta.setVisibility(View.VISIBLE);
            btnCrearCuentaCesta.setOnClickListener(v -> startActivity(new Intent(getActivity(), RegistroActivity.class)));
            loadHarvestRealtime();
        } else {
            btnCrearCuentaCesta.setVisibility(View.GONE);
            checkUserSubscriptionAndLoadHarvest();
        }

        btnVerMiPlanCesta.setOnClickListener(v -> startActivity(new Intent(getActivity(), MiPlanActivity.class)));
        btnVerPlanesCesta.setOnClickListener(v -> startActivity(new Intent(getActivity(), PlanesActivity.class)));
        btnCestaRetry.setOnClickListener(v -> {
            if (SessionManager.isEsInvitado()) {
                loadHarvestRealtime();
            } else {
                checkUserSubscriptionAndLoadHarvest();
            }
        });

        return view;
    }

    private void checkUserSubscriptionAndLoadHarvest() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (userListener != null) userListener.remove();

            userListener = db.collection("usuarios").document(user.getUid())
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (!isAdded()) return;

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            userPlan = documentSnapshot.getString("plan");
                            if (userPlan == null) userPlan = "ninguno";

                            loadHarvestRealtime();
                        }
                    });
        } else {
            userPlan = "ninguno";
            loadHarvestRealtime();
        }
    }

    private void loadHarvestRealtime() {
        progressCestaLoading.setVisibility(View.VISIBLE);
        tvCestaEmpty.setVisibility(View.GONE);
        layoutCestaError.setVisibility(View.GONE);
        layoutCestaActive.setVisibility(View.GONE);
        layoutCestaNoPlan.setVisibility(View.GONE);

        if (harvestListener != null) harvestListener.remove();

        harvestListener = db.collection("cosechas")
                .whereEqualTo("activa", true)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;

                    progressCestaLoading.setVisibility(View.GONE);

                    if (error != null) {
                        layoutCestaError.setVisibility(View.VISIBLE);
                        Mensajes.mostrarError(rvProductosCesta, "Error al cargar la cesta: " + error.getMessage());
                        return;
                    }

                    if (value == null || value.isEmpty()) {
                        tvCestaEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    // Get first active harvest
                    Cosecha cosecha = null;
                    for (QueryDocumentSnapshot doc : value) {
                        cosecha = doc.toObject(Cosecha.class);
                        break;
                    }

                    if (cosecha == null || cosecha.getProductos() == null) {
                        tvCestaEmpty.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (userPlan.equalsIgnoreCase("ninguno") || userPlan.equalsIgnoreCase("invitado")) {
                        layoutCestaNoPlan.setVisibility(View.VISIBLE);
                        productoListGuest.clear();
                        productoListGuest.addAll(cosecha.getProductos());
                        guestAdapter = new ProductoCosechaAdapter(getContext(), productoListGuest, userPlan);
                        rvProductosCestaGuest.setAdapter(guestAdapter);
                    } else {
                        layoutCestaActive.setVisibility(View.VISIBLE);
                        chipActivePlan.setText("Plan " + capitalize(userPlan));

                        String info = "Recogida: " + cosecha.getFechaRecogida() + " (" + cosecha.getHoraRecogida() + ")\nLugar: " + cosecha.getLugarRecogida();
                        tvCestaRecogidaInfo.setText(info);

                        // Calculate days remaining
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            Date recogidaDate = sdf.parse(cosecha.getFechaRecogida());
                            if (recogidaDate != null) {
                                long diff = recogidaDate.getTime() - System.currentTimeMillis();
                                long days = diff / (1000 * 60 * 60 * 24);
                                if (days >= 0) {
                                    tvCestaDiasFaltan.setText("⏳ Faltan " + (days == 0 ? "hoy" : days + " días") + " para la recogida");
                                } else {
                                    tvCestaDiasFaltan.setText("⏳ Cesta disponible");
                                }
                            }
                        } catch (Exception e) {
                            tvCestaDiasFaltan.setText("");
                        }

                        if (userPlan.equalsIgnoreCase("premium")) {
                            tvPremiumPriorityNote.setVisibility(View.VISIBLE);
                        } else {
                            tvPremiumPriorityNote.setVisibility(View.GONE);
                        }

                        // Filter products with quantity > 0 for active user plan
                        productoListActive.clear();
                        for (ProductoCosecha p : cosecha.getProductos()) {
                            double cant = 0;
                            if (userPlan.contains("basico")) cant = p.getCantidadBasico();
                            else if (userPlan.contains("familiar")) cant = p.getCantidadFamiliar();
                            else if (userPlan.contains("premium")) cant = p.getCantidadPremium();

                            if (cant > 0) {
                                productoListActive.add(p);
                            }
                        }

                        activeAdapter = new ProductoCosechaAdapter(getContext(), productoListActive, userPlan);
                        rvProductosCesta.setAdapter(activeAdapter);
                    }
                });
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (harvestListener != null) {
            harvestListener.remove();
            harvestListener = null;
        }
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }
}
