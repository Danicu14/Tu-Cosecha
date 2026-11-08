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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.PlanesActivity;
import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.adapter.CultivoAdapter;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.danielcarpio.tucosecha.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CestaFragment extends Fragment {

    private RecyclerView rvCestaCultivos;
    private ProgressBar progressCestaLoading;
    private TextView tvCestaEmpty, tvCestaResumenVariedades, tvCestaResumenPrecio;
    private MaterialButton btnCestaAction;
    private Chip chipCestaAll, chipCestaTemporada, chipCestaAgricultura, chipCestaTipoCultivo, chipCestaCategoria, chipCestaPrecio;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration cultivosListener;
    private ListenerRegistration userListener;

    private List<Cultivo> allCultivos;
    private List<Cultivo> filteredCultivos;
    private List<Cultivo> selectedCultivos;
    private CultivoAdapter adapter;

    private String userPlan = "ninguno";
    private int maxVariedades = 4; // default basic or none

    private boolean filterTemporada = false;
    private boolean filterEcolologico = false;
    private String filterTipoCultivo = "todos";
    private String filterCategoria = "todas";
    private String sortPrecio = "ninguno"; // "asc", "desc"

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
        tvCestaResumenPrecio = view.findViewById(R.id.tvCestaResumenPrecio);
        btnCestaAction = view.findViewById(R.id.btnCestaAction);

        chipCestaAll = view.findViewById(R.id.chipCestaAll);
        chipCestaTemporada = view.findViewById(R.id.chipCestaTemporada);
        chipCestaAgricultura = view.findViewById(R.id.chipCestaAgricultura);
        chipCestaTipoCultivo = view.findViewById(R.id.chipCestaTipoCultivo);
        chipCestaCategoria = view.findViewById(R.id.chipCestaCategoria);
        chipCestaPrecio = view.findViewById(R.id.chipCestaPrecio);

        rvCestaCultivos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        allCultivos = new ArrayList<>();
        filteredCultivos = new ArrayList<>();
        selectedCultivos = new ArrayList<>();

        setupFilters();

        if (SessionManager.isEsInvitado()) {
            userPlan = "invitado";
            maxVariedades = 0;
            setupBottomBarForGuestOrNoPlan();
            loadCultivosRealtime();
        } else {
            checkUserSubscriptionAndLoadCultivos();
        }

        return view;
    }

    private void setupBottomBarForGuestOrNoPlan() {
        if (userPlan.equalsIgnoreCase("ninguno") || userPlan.equalsIgnoreCase("invitado")) {
            tvCestaResumenVariedades.setText("Explora los productos de la huerta");
            tvCestaResumenPrecio.setText("Suscríbete para crear tu cesta");
            btnCestaAction.setText("Ver planes");
            btnCestaAction.setOnClickListener(v -> startActivity(new Intent(getActivity(), PlanesActivity.class)));
        } else {
            updateResumenBar();
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
        double total = 0;
        for (Cultivo c : selectedCultivos) {
            total += c.getPrecio();
        }
        tvCestaResumenVariedades.setText("Tu cesta: " + selectedCultivos.size() + " de " + maxVariedades + " variedades");
        tvCestaResumenPrecio.setText(String.format(Locale.getDefault(), "Total estimado: %.2f €", total));
    }

    private void checkUserSubscriptionAndLoadCultivos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (userListener != null) userListener.remove();

            userListener = db.collection("usuarios").document(user.getUid())
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (!isAdded()) return;

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            userPlan = documentSnapshot.getString("plan");
                            if (userPlan == null) userPlan = "ninguno";

                            if (userPlan.equalsIgnoreCase("basico")) maxVariedades = 4;
                            else if (userPlan.equalsIgnoreCase("familiar")) maxVariedades = 6;
                            else if (userPlan.equalsIgnoreCase("premium")) maxVariedades = 8;
                            else maxVariedades = 0;

                            setupBottomBarForGuestOrNoPlan();
                            loadCultivosRealtime();
                        }
                    });
        } else {
            userPlan = "ninguno";
            maxVariedades = 0;
            setupBottomBarForGuestOrNoPlan();
            loadCultivosRealtime();
        }
    }

    private void loadCultivosRealtime() {
        progressCestaLoading.setVisibility(View.VISIBLE);
        tvCestaEmpty.setVisibility(View.GONE);
        rvCestaCultivos.setVisibility(View.GONE);

        if (cultivosListener != null) cultivosListener.remove();

        boolean selectionMode = !userPlan.equalsIgnoreCase("ninguno") && !userPlan.equalsIgnoreCase("invitado");

        adapter = new CultivoAdapter(getContext(), filteredCultivos, selectionMode, selectedCultivos, userPlan, cultivo -> {
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

        cultivosListener = db.collection("cultivos").addSnapshotListener((value, error) -> {
            if (!isAdded()) return;

            progressCestaLoading.setVisibility(View.GONE);

            if (value != null) {
                allCultivos.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Cultivo cultivo = doc.toObject(Cultivo.class);
                    if (cultivo.getId() == null || cultivo.getId().isEmpty()) {
                        cultivo.setId(doc.getId());
                    }
                    allCultivos.add(cultivo);
                }
                filterAndSortCultivos();
            }
        });
    }

    private void setupFilters() {
        chipCestaAll.setOnClickListener(v -> {
            filterTemporada = false;
            filterEcolologico = false;
            filterTipoCultivo = "todos";
            filterCategoria = "todas";
            sortPrecio = "ninguno";
            chipCestaTemporada.setChecked(false);
            chipCestaAgricultura.setChecked(false);
            chipCestaTipoCultivo.setText("Tipo de cultivo ▼");
            chipCestaCategoria.setText("Categoría ▼");
            chipCestaPrecio.setText("Precio ▼");
            filterAndSortCultivos();
        });

        chipCestaTemporada.setOnClickListener(v -> {
            filterTemporada = chipCestaTemporada.isChecked();
            filterAndSortCultivos();
        });

        chipCestaAgricultura.setOnClickListener(v -> {
            filterEcolologico = chipCestaAgricultura.isChecked();
            filterAndSortCultivos();
        });

        chipCestaTipoCultivo.setOnClickListener(v -> {
            String[] tipos = {"Todos", "Hoja", "Raíz", "Fruto", "Bulbo", "Inflorescencia", "Frutal"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("Filtrar por tipo de cultivo")
                    .setItems(tipos, (dialog, which) -> {
                        filterTipoCultivo = tipos[which].toLowerCase();
                        if (filterTipoCultivo.equals("todos")) filterTipoCultivo = "todos";
                        chipCestaTipoCultivo.setText(tipos[which] + " ▼");
                        filterAndSortCultivos();
                    })
                    .show();
        });

        chipCestaCategoria.setOnClickListener(v -> {
            String[] categorias = {"Todas", "Hortaliza", "Fruta"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("Filtrar por categoría")
                    .setItems(categorias, (dialog, which) -> {
                        filterCategoria = categorias[which].toLowerCase();
                        if (filterCategoria.equals("todas")) filterCategoria = "todas";
                        chipCestaCategoria.setText(categorias[which] + " ▼");
                        filterAndSortCultivos();
                    })
                    .show();
        });

        chipCestaPrecio.setOnClickListener(v -> {
            String[] orden = {"Por defecto", "Menor a mayor precio", "Mayor a menor precio"};
            new AlertDialog.Builder(requireContext())
                    .setTitle("Ordenar por precio")
                    .setItems(orden, (dialog, which) -> {
                        if (which == 0) sortPrecio = "ninguno";
                        else if (which == 1) sortPrecio = "asc";
                        else if (which == 2) sortPrecio = "desc";
                        chipCestaPrecio.setText(orden[which] + " ▼");
                        filterAndSortCultivos();
                    })
                    .show();
        });
    }

    private void filterAndSortCultivos() {
        filteredCultivos.clear();
        for (Cultivo c : allCultivos) {
            boolean matchesTemporada = !filterTemporada || (c.getEtiquetas() != null && c.getEtiquetas().contains("Temporada"));
            boolean matchesEcolologico = !filterEcolologico || (c.getEtiquetas() != null && c.getEtiquetas().contains("Ecológico"));
            boolean matchesTipo = filterTipoCultivo.equals("todos") || (c.getTipoCultivo() != null && c.getTipoCultivo().equalsIgnoreCase(filterTipoCultivo));
            boolean matchesCat = filterCategoria.equals("todas") || (c.getCategoria() != null && c.getCategoria().equalsIgnoreCase(filterCategoria));

            if (matchesTemporada && matchesEcolologico && matchesTipo && matchesCat) {
                filteredCultivos.add(c);
            }
        }

        if (sortPrecio.equals("asc")) {
            Collections.sort(filteredCultivos, (o1, o2) -> Double.compare(o1.getPrecio(), o2.getPrecio()));
        } else if (sortPrecio.equals("desc")) {
            Collections.sort(filteredCultivos, (o1, o2) -> Double.compare(o2.getPrecio(), o1.getPrecio()));
        }

        if (adapter != null) adapter.notifyDataSetChanged();

        if (filteredCultivos.isEmpty()) {
            tvCestaEmpty.setVisibility(View.VISIBLE);
            rvCestaCultivos.setVisibility(View.GONE);
        } else {
            tvCestaEmpty.setVisibility(View.GONE);
            rvCestaCultivos.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cultivosListener != null) {
            cultivosListener.remove();
            cultivosListener = null;
        }
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }
}
