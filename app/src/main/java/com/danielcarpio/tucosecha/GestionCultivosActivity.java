package com.danielcarpio.tucosecha;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.adapter.GestionCultivosAdapter;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.danielcarpio.tucosecha.model.Usuario;
import com.danielcarpio.tucosecha.ui.fragments.EditarCultivoBottomSheet;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GestionCultivosActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration cultivosListener;

    private MaterialToolbar toolbar;
    private TextInputEditText etBuscarCultivo;
    private ChipGroup chipGroupFiltros;
    private Chip chipFiltroTodos, chipFiltroSembrado, chipFiltroCreciendo, chipFiltroListo;
    private ProgressBar progressBar;
    private LinearLayout layoutError, layoutEmpty;
    private TextView tvEmptyMessage;
    private MaterialButton btnReintentar;
    private RecyclerView rvCultivos;

    private final List<Cultivo> allCultivos = new ArrayList<>();
    private final List<Cultivo> filteredCultivos = new ArrayList<>();
    private GestionCultivosAdapter adapter;

    private String filtroFase = "todos";
    private String queryBusqueda = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_cultivos);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbarGestionCultivos);
        toolbar.setNavigationOnClickListener(v -> finish());

        etBuscarCultivo = findViewById(R.id.etBuscarCultivo);
        chipGroupFiltros = findViewById(R.id.chipGroupFiltrosCultivos);
        chipFiltroTodos = findViewById(R.id.chipFiltroCultivoTodos);
        chipFiltroSembrado = findViewById(R.id.chipFiltroCultivoSembrado);
        chipFiltroCreciendo = findViewById(R.id.chipFiltroCultivoCreciendo);
        chipFiltroListo = findViewById(R.id.chipFiltroCultivoListo);

        progressBar = findViewById(R.id.progressCultivosLoading);
        layoutError = findViewById(R.id.layoutErrorCultivos);
        layoutEmpty = findViewById(R.id.layoutEmptyCultivos);
        tvEmptyMessage = findViewById(R.id.tvEmptyCultivosMessage);
        btnReintentar = findViewById(R.id.btnReintentarCultivos);
        rvCultivos = findViewById(R.id.rvGestionCultivos);

        rvCultivos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GestionCultivosAdapter(filteredCultivos, this::abrirBottomSheetEditar);
        rvCultivos.setAdapter(adapter);

        setupFiltros();
        setupBusqueda();

        btnReintentar.setOnClickListener(v -> checkAdminRoleAndLoad());

        checkAdminRoleAndLoad();
    }

    private void checkAdminRoleAndLoad() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Mensajes.mostrarError(findViewById(android.R.id.content), "Acceso solo para el hortelano");
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        db.collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Mensajes.mostrarError(findViewById(android.R.id.content), "Acceso solo para el hortelano");
                        finish();
                        return;
                    }

                    Usuario usuario = documentSnapshot.toObject(Usuario.class);
                    if (usuario == null || !"administrador".equalsIgnoreCase(usuario.getRol())) {
                        Mensajes.mostrarError(findViewById(android.R.id.content), "Acceso solo para el hortelano");
                        finish();
                        return;
                    }

                    listenCultivosRealtime();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    layoutError.setVisibility(View.VISIBLE);
                    Mensajes.mostrarError(toolbar, "Error de verificación: " + e.getMessage());
                });
    }

    private void listenCultivosRealtime() {
        if (cultivosListener != null) cultivosListener.remove();

        cultivosListener = db.collection("cultivos")
                .addSnapshotListener((snapshots, e) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null) {
                        layoutError.setVisibility(View.VISIBLE);
                        Mensajes.mostrarError(toolbar, "Error al cargar cultivos: " + e.getMessage());
                        return;
                    }

                    allCultivos.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Cultivo c = doc.toObject(Cultivo.class);
                            if (c.getId() == null || c.getId().isEmpty()) {
                                c.setId(doc.getId());
                            }
                            allCultivos.add(c);
                        }
                    }

                    aplicarFiltros();
                });
    }

    private void setupFiltros() {
        chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipFiltroCultivoSembrado)) {
                filtroFase = "sembrado";
            } else if (checkedIds.contains(R.id.chipFiltroCultivoCreciendo)) {
                filtroFase = "creciendo";
            } else if (checkedIds.contains(R.id.chipFiltroCultivoListo)) {
                filtroFase = "listo";
            } else {
                filtroFase = "todos";
            }
            aplicarFiltros();
        });
    }

    private void setupBusqueda() {
        etBuscarCultivo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                queryBusqueda = s != null ? s.toString().trim().toLowerCase() : "";
                aplicarFiltros();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void aplicarFiltros() {
        filteredCultivos.clear();
        for (Cultivo c : allCultivos) {
            boolean coincideFase = "todos".equalsIgnoreCase(filtroFase) ||
                    (c.getFase() != null && c.getFase().equalsIgnoreCase(filtroFase));

            boolean coincideNombre = queryBusqueda.isEmpty() ||
                    (c.getNombre() != null && c.getNombre().toLowerCase().contains(queryBusqueda));

            if (coincideFase && coincideNombre) {
                filteredCultivos.add(c);
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredCultivos.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            if (!queryBusqueda.isEmpty()) {
                tvEmptyMessage.setText("No hay cultivos que coincidan con \"" + queryBusqueda + "\"");
            } else if (!"todos".equalsIgnoreCase(filtroFase)) {
                tvEmptyMessage.setText("No hay cultivos en fase " + filtroFase);
            } else {
                tvEmptyMessage.setText("No se encontraron cultivos");
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void abrirBottomSheetEditar(Cultivo cultivo) {
        EditarCultivoBottomSheet bottomSheet = EditarCultivoBottomSheet.newInstance(cultivo);
        bottomSheet.show(getSupportFragmentManager(), "EditarCultivoBottomSheet");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cultivosListener != null) {
            cultivosListener.remove();
        }
    }
}
