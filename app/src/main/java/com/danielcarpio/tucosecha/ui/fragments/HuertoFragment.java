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

import java.util.ArrayList;
import java.util.List;

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
        adapter = new CultivoAdapter(getContext(), filteredCultivos, cultivo -> {
            DetalleCultivoBottomSheet bottomSheet = DetalleCultivoBottomSheet.newInstance(cultivo);
            bottomSheet.show(getParentFragmentManager(), "DetalleCultivoBottomSheet");
        });
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
            filterCultivos();
        });

        chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipCategoryAll) {
                selectedCategory = "todo";
            } else if (checkedId == R.id.chipCategoryHortaliza) {
                selectedCategory = "hortaliza";
            } else if (checkedId == R.id.chipCategoryFruta) {
                selectedCategory = "fruta";
            }
            filterCultivos();
        });

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

                filterCultivos();
            }
        });
    }

    private void filterCultivos() {
        filteredCultivos.clear();
        for (Cultivo c : allCultivos) {
            boolean matchesPhase = selectedPhase.equals("todo") || (c.getFase() != null && c.getFase().equalsIgnoreCase(selectedPhase));
            boolean matchesCategory = selectedCategory.equals("todo") || (c.getCategoria() != null && c.getCategoria().equalsIgnoreCase(selectedCategory));

            if (matchesPhase && matchesCategory) {
                filteredCultivos.add(c);
            }
        }
        adapter.notifyDataSetChanged();

        if (filteredCultivos.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvCultivos.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvCultivos.setVisibility(View.VISIBLE);
        }
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
