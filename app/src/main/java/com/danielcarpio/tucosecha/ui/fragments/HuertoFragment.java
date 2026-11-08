package com.danielcarpio.tucosecha.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.adapter.CultivoAdapter;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HuertoFragment extends Fragment {

    private RecyclerView rvCultivos;
    private TextView tvSummary;
    private ProgressBar progressBarLoading;
    private TextView tvEmptyState;
    private LinearLayout layoutErrorState;
    private MaterialButton btnRetry;
    private ChipGroup chipGroupPhase, chipGroupCategory;

    private FirebaseFirestore db;
    private List<Cultivo> allCultivos;
    private List<Cultivo> filteredCultivos;
    private CultivoAdapter adapter;
    private ListenerRegistration listenerRegistration;

    private String selectedPhase = "todo";
    private String selectedCategory = "todo";
    private String sortOrder = "listos"; // "listos", "az", "za"

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_huerto, container, false);

        db = FirebaseFirestore.getInstance();

        rvCultivos = view.findViewById(R.id.rvCultivos);
        tvSummary = view.findViewById(R.id.tvSummary);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        layoutErrorState = view.findViewById(R.id.layoutErrorState);
        btnRetry = view.findViewById(R.id.btnRetry);
        chipGroupPhase = view.findViewById(R.id.chipGroupPhase);
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);

        rvCultivos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        allCultivos = new ArrayList<>();
        filteredCultivos = new ArrayList<>();
        adapter = new CultivoAdapter(getContext(), filteredCultivos, false, null, "ninguno", null, cultivo -> {
            DetalleCultivoBottomSheet bottomSheet = DetalleCultivoBottomSheet.newInstance(cultivo);
            bottomSheet.show(getParentFragmentManager(), "DetalleCultivoBottomSheet");
        }, null);
        rvCultivos.setAdapter(adapter);

        loadCultivosRealtime();

        btnRetry.setOnClickListener(v -> loadCultivosRealtime());

        chipGroupPhase.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipPhaseAll) {
                selectedPhase = "todo";
            } else if (checkedId == R.id.chipPhaseListo) {
                selectedPhase = "listo";
            } else if (checkedId == R.id.chipPhaseCreciendo) {
                selectedPhase = "creciendo";
            } else if (checkedId == R.id.chipPhaseSembrado) {
                selectedPhase = "sembrado";
            }
            filterAndSortCultivos();
        });

        chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipCategoryAll) {
                selectedCategory = "todo";
            } else if (checkedId == R.id.chipCategoryHortaliza) {
                selectedCategory = "hortaliza";
            } else if (checkedId == R.id.chipCategoryFruta) {
                selectedCategory = "fruta";
            }
            filterAndSortCultivos();
        });

        // Setup sort chip if present
        View chipOrdenar = view.findViewById(R.id.chipOrdenar);
        if (chipOrdenar != null) {
            chipOrdenar.setOnClickListener(v -> {
                String[] opciones = {"Más listos primero", "A-Z", "Z-A"};
                new AlertDialog.Builder(requireContext())
                        .setTitle("Ordenar productos")
                        .setItems(opciones, (dialog, which) -> {
                            if (which == 0) sortOrder = "listos";
                            else if (which == 1) sortOrder = "az";
                            else if (which == 2) sortOrder = "za";
                            filterAndSortCultivos();
                        })
                        .show();
            });
        }

        return view;
    }

    private void loadCultivosRealtime() {
        progressBarLoading.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        layoutErrorState.setVisibility(View.GONE);
        rvCultivos.setVisibility(View.GONE);

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        listenerRegistration = db.collection("cultivos").addSnapshotListener((value, error) -> {
            if (!isAdded()) return;

            progressBarLoading.setVisibility(View.GONE);

            if (error != null) {
                layoutErrorState.setVisibility(View.VISIBLE);
                rvCultivos.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.GONE);
                Mensajes.mostrarError(rvCultivos, "Error al sincronizar cultivos: " + error.getMessage());
                return;
            }

            if (value != null) {
                allCultivos.clear();
                int listosCount = 0;
                for (QueryDocumentSnapshot doc : value) {
                    Cultivo cultivo = doc.toObject(Cultivo.class);
                    if (cultivo.getId() == null || cultivo.getId().isEmpty()) {
                        cultivo.setId(doc.getId());
                    }
                    allCultivos.add(cultivo);
                    if ("listo".equalsIgnoreCase(cultivo.getFase())) {
                        listosCount++;
                    }
                }

                tvSummary.setText(listosCount + " cultivos listos para cosechar · " + allCultivos.size() + " en total");

                filterAndSortCultivos();
            }
        });
    }

    private void filterAndSortCultivos() {
        filteredCultivos.clear();
        for (Cultivo c : allCultivos) {
            boolean matchesPhase = selectedPhase.equals("todo") || (c.getFase() != null && c.getFase().equalsIgnoreCase(selectedPhase));
            boolean matchesCategory = selectedCategory.equals("todo") || (c.getCategoria() != null && c.getCategoria().equalsIgnoreCase(selectedCategory));

            if (matchesPhase && matchesCategory) {
                filteredCultivos.add(c);
            }
        }

        // Sorting: "Más listos primero" (listo, creciendo, sembrado, alphabetically by name)
        Collator collator = Collator.getInstance(new Locale("es", "ES"));
        collator.setStrength(Collator.PRIMARY);

        Collections.sort(filteredCultivos, (o1, o2) -> {
            if (sortOrder.equals("az")) {
                return collator.compare(o1.getNombre(), o2.getNombre());
            } else if (sortOrder.equals("za")) {
                return collator.compare(o2.getNombre(), o1.getNombre());
            } else {
                // "listos" default order: listo -> creciendo -> sembrado, then alphabetical
                int p1 = getPhasePriority(o1.getFase());
                int p2 = getPhasePriority(o2.getFase());
                if (p1 != p2) {
                    return Integer.compare(p1, p2);
                }
                return collator.compare(o1.getNombre(), o2.getNombre());
            }
        });

        adapter.notifyDataSetChanged();

        if (filteredCultivos.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvCultivos.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvCultivos.setVisibility(View.VISIBLE);
        }
    }

    private int getPhasePriority(String fase) {
        if (fase == null) return 3;
        if (fase.equalsIgnoreCase("listo")) return 1;
        if (fase.equalsIgnoreCase("creciendo")) return 2;
        return 3; // sembrado
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}
