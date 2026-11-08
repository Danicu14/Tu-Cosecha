package com.danielcarpio.tucosecha.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.PlanesActivity;
import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.adapter.CultivoAdapter;
import com.danielcarpio.tucosecha.model.Cosecha;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.danielcarpio.tucosecha.model.ProductoCosecha;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.danielcarpio.tucosecha.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CestaFragment extends Fragment {

    private RecyclerView rvCestaCultivos;
    private ProgressBar progressCestaLoading;
    private TextView tvCestaEmpty, tvCestaResumenVariedades, tvCestaResumenPlan;
    private MaterialButton btnCestaAction;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration cultivosListener;
    private ListenerRegistration harvestListener;
    private ListenerRegistration userListener;

    private List<Cultivo> listoCultivos;
    private List<Cultivo> selectedCultivos;
    private List<ProductoCosecha> activeHarvestProducts;
    private CultivoAdapter adapter;

    private String userPlan = "ninguno";
    private int maxVariedades = 4;
    private String planNombre = "Ninguno";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cesta, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvCestaCultivos = view.findViewById(R.id.rvCestaCultivos);
        progressCestaLoading = view.findViewById(R.id.progressCestaLoading);
        tvCestaEmpty = view.findViewById(R.id.tvCestaEmpty);
        tvCestaResumenVariedades = view.findViewById(R.id.tvCestaResumenVariedades);
        tvCestaResumenPlan = view.findViewById(R.id.tvCestaResumenPlan);
        btnCestaAction = view.findViewById(R.id.btnCestaAction);

        rvCestaCultivos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        listoCultivos = new ArrayList<>();
        selectedCultivos = new ArrayList<>();
        activeHarvestProducts = new ArrayList<>();

        if (SessionManager.isEsInvitado()) {
            userPlan = "invitado";
            maxVariedades = 0;
            planNombre = "Invitado";
            setupBottomBar();
            loadDataRealtime();
        } else {
            checkUserSubscriptionAndLoadData();
        }

        return view;
    }

    private void setupBottomBar() {
        if (userPlan.equalsIgnoreCase("ninguno") || userPlan.equalsIgnoreCase("invitado")) {
            tvCestaResumenVariedades.setText("Tu cesta: 0 variedades");
            tvCestaResumenPlan.setText("Elige un plan para crear tu cesta");
            btnCestaAction.setText("Ver planes");
            btnCestaAction.setOnClickListener(v -> startActivity(new Intent(getActivity(), PlanesActivity.class)));
        } else {
            updateResumenBar();
            tvCestaResumenPlan.setText("Incluido en tu plan " + planNombre);
            btnCestaAction.setText("Confirmar cesta");
            btnCestaAction.setOnClickListener(v -> {
                if (selectedCultivos.isEmpty()) {
                    Mensajes.mostrarInfo(btnCestaAction, "Selecciona al menos un producto para tu cesta.");
                } else {
                    Mensajes.mostrarExito(btnCestaAction, "¡Cesta confirmada con éxito! Recibirás tus productos frescos.");
                }
            });
        }
    }

    private void updateResumenBar() {
        tvCestaResumenVariedades.setText("Tu cesta: " + selectedCultivos.size() + " de " + maxVariedades + " variedades");
    }

    private void checkUserSubscriptionAndLoadData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (userListener != null) userListener.remove();

            userListener = db.collection("usuarios").document(user.getUid())
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (!isAdded()) return;

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            userPlan = documentSnapshot.getString("plan");
                            if (userPlan == null) userPlan = "ninguno";

                            if (userPlan.equalsIgnoreCase("basico")) {
                                maxVariedades = 4;
                                planNombre = "Básico";
                            } else if (userPlan.equalsIgnoreCase("familiar")) {
                                maxVariedades = 6;
                                planNombre = "Familiar";
                            } else if (userPlan.equalsIgnoreCase("premium")) {
                                maxVariedades = 8;
                                planNombre = "Premium";
                            } else {
                                maxVariedades = 0;
                                planNombre = "Ninguno";
                            }

                            setupBottomBar();
                            loadDataRealtime();
                        }
                    });
        } else {
            userPlan = "ninguno";
            maxVariedades = 0;
            planNombre = "Ninguno";
            setupBottomBar();
            loadDataRealtime();
        }
    }

    private void loadDataRealtime() {
        progressCestaLoading.setVisibility(View.VISIBLE);
        tvCestaEmpty.setVisibility(View.GONE);
        rvCestaCultivos.setVisibility(View.GONE);

        if (harvestListener != null) harvestListener.remove();
        if (cultivosListener != null) cultivosListener.remove();

        harvestListener = db.collection("cosechas")
                .whereEqualTo("activa", true)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;

                    activeHarvestProducts.clear();
                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot doc : value) {
                            Cosecha cosecha = doc.toObject(Cosecha.class);
                            if (cosecha.getProductos() != null) {
                                activeHarvestProducts.addAll(cosecha.getProductos());
                            }
                            break;
                        }
                    }

                    loadCultivos();
                });
    }

    private void loadCultivos() {
        boolean selectionMode = !userPlan.equalsIgnoreCase("ninguno") && !userPlan.equalsIgnoreCase("invitado");

        adapter = new CultivoAdapter(getContext(), listoCultivos, selectionMode, selectedCultivos, userPlan, activeHarvestProducts, cultivo -> {
            DetalleCultivoBottomSheet bottomSheet = DetalleCultivoBottomSheet.newInstance(cultivo);
            bottomSheet.show(getParentFragmentManager(), "DetalleCultivoBottomSheet");
        }, (cultivo, isSelected) -> {
            if (isSelected) {
                if (selectedCultivos.size() >= maxVariedades) {
                    Mensajes.mostrarInfo(rvCestaCultivos, "Has alcanzado el límite de " + maxVariedades + " variedades de tu plan.");
                    return;
                }
                selectedCultivos.add(cultivo);
            } else {
                selectedCultivos.remove(cultivo);
            }
            adapter.notifyDataSetChanged();
            updateResumenBar();
        });

        rvCestaCultivos.setAdapter(adapter);

        cultivosListener = db.collection("cultivos")
                .whereEqualTo("fase", "listo")
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;

                    progressCestaLoading.setVisibility(View.GONE);

                    if (value != null) {
                        listoCultivos.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Cultivo cultivo = doc.toObject(Cultivo.class);
                            if (cultivo.getId() == null || cultivo.getId().isEmpty()) {
                                cultivo.setId(doc.getId());
                            }

                            if (selectionMode) {
                                boolean hasQuantity = false;
                                for (ProductoCosecha p : activeHarvestProducts) {
                                    if (p.getNombre().equalsIgnoreCase(cultivo.getNombre())) {
                                        double cant = 0;
                                        if (userPlan.contains("basico")) cant = p.getCantidadBasico();
                                        else if (userPlan.contains("familiar")) cant = p.getCantidadFamiliar();
                                        else if (userPlan.contains("premium")) cant = p.getCantidadPremium();

                                        if (cant > 0) hasQuantity = true;
                                        break;
                                    }
                                }
                                if (hasQuantity) {
                                    listoCultivos.add(cultivo);
                                }
                            } else {
                                listoCultivos.add(cultivo);
                            }
                        }

                        adapter.notifyDataSetChanged();

                        if (listoCultivos.isEmpty()) {
                            tvCestaEmpty.setVisibility(View.VISIBLE);
                            rvCestaCultivos.setVisibility(View.GONE);
                        } else {
                            tvCestaEmpty.setVisibility(View.GONE);
                            rvCestaCultivos.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cultivosListener != null) {
            cultivosListener.remove();
            cultivosListener = null;
        }
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
