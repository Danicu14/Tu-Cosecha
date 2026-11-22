package com.danielcarpio.tucosecha.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import com.danielcarpio.tucosecha.NuevaNoticiaActivity;
import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.adapter.NoticiaAdapter;
import com.danielcarpio.tucosecha.model.Noticia;
import com.danielcarpio.tucosecha.util.GestorSesion;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NoticiasFragment extends Fragment {

    private RecyclerView rvNoticias;
    private ProgressBar progressBarNoticiasLoading;
    private TextView tvNoticiasEmptyState;
    private LinearLayout layoutNoticiasErrorState;
    private MaterialButton btnNoticiasRetry;
    private FloatingActionButton fabNuevaNoticia;

    private FirebaseFirestore db;
    private List<Noticia> listaNoticias;
    private NoticiaAdapter adapter;
    private ListenerRegistration listenerRegistro;
    private boolean esAdmin = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_noticias, container, false);

        db = FirebaseFirestore.getInstance();

        rvNoticias = view.findViewById(R.id.rvNoticias);
        progressBarNoticiasLoading = view.findViewById(R.id.progressBarNoticiasLoading);
        tvNoticiasEmptyState = view.findViewById(R.id.tvNoticiasEmptyState);
        layoutNoticiasErrorState = view.findViewById(R.id.layoutNoticiasErrorState);
        btnNoticiasRetry = view.findViewById(R.id.btnNoticiasRetry);
        fabNuevaNoticia = view.findViewById(R.id.fabNuevaNoticia);

        rvNoticias.setLayoutManager(new LinearLayoutManager(getContext()));
        listaNoticias = new ArrayList<>();
        adapter = new NoticiaAdapter(getContext(), listaNoticias, new NoticiaAdapter.ListenerNoticia() {
            @Override
            public void onNoticiaClick(Noticia noticia) {
                DetalleNoticiaBottomSheet bottomSheet = DetalleNoticiaBottomSheet.newInstance(noticia);
                bottomSheet.show(getParentFragmentManager(), "DetalleNoticiaBottomSheet");
            }

            @Override
            public void onNoticiaLongClick(Noticia noticia) {
                if (esAdmin) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Eliminar noticia")
                            .setMessage("¿Estás seguro de que deseas eliminar esta noticia?")
                            .setPositiveButton("Eliminar", (dialog, which) -> {
                                db.collection("noticias").document(noticia.getId()).delete()
                                        .addOnSuccessListener(aVoid -> Mensajes.mostrarExito(rvNoticias, "Noticia eliminada"))
                                        .addOnFailureListener(e -> {
                                            Log.e("CARGA_DATOS", "Error al eliminar noticia: " + e.getMessage(), e);
                                            Mensajes.mostrarError(rvNoticias, "Error al eliminar: " + e.getMessage());
                                        });
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                }
            }
        });
        rvNoticias.setAdapter(adapter);

        verificarRolAdminYCargar();

        btnNoticiasRetry.setOnClickListener(v -> cargarNoticiasEnTiempoReal());

        fabNuevaNoticia.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), NuevaNoticiaActivity.class));
        });

        return view;
    }

    private void verificarRolAdminYCargar() {
        if (GestorSesion.esInvitado()) {
            esAdmin = false;
            fabNuevaNoticia.setVisibility(View.GONE);
            cargarNoticiasEnTiempoReal();
            return;
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            db.collection("usuarios").document(usuario.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String rol = documentSnapshot.getString("rol");
                            esAdmin = "administrador".equalsIgnoreCase(rol);
                            if (esAdmin) {
                                fabNuevaNoticia.setVisibility(View.VISIBLE);
                            } else {
                                fabNuevaNoticia.setVisibility(View.GONE);
                            }
                        }
                        cargarNoticiasEnTiempoReal();
                    })
                    .addOnFailureListener(e -> cargarNoticiasEnTiempoReal());
        } else {
            cargarNoticiasEnTiempoReal();
        }
    }

    private void cargarNoticiasEnTiempoReal() {
        progressBarNoticiasLoading.setVisibility(View.VISIBLE);
        tvNoticiasEmptyState.setVisibility(View.GONE);
        layoutNoticiasErrorState.setVisibility(View.GONE);
        rvNoticias.setVisibility(View.GONE);

        if (listenerRegistro != null) {
            listenerRegistro.remove();
        }

        listenerRegistro = db.collection("noticias")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener((valor, error) -> {
                    if (!isAdded()) return;

                    progressBarNoticiasLoading.setVisibility(View.GONE);

                    if (error != null) {
                        layoutNoticiasErrorState.setVisibility(View.VISIBLE);
                        rvNoticias.setVisibility(View.GONE);
                        tvNoticiasEmptyState.setVisibility(View.GONE);
                        Log.e("CARGA_DATOS", "Error al cargar noticias: " + error.getMessage(), error);
                        Mensajes.mostrarError(rvNoticias, "Error al cargar noticias: " + error.getMessage());
                        return;
                    }

                    if (valor != null) {
                        listaNoticias.clear();
                        for (QueryDocumentSnapshot doc : valor) {
                            Noticia noticia = doc.toObject(Noticia.class);
                            if (noticia.getId() == null || noticia.getId().isEmpty()) {
                                noticia.setId(doc.getId());
                            }
                            listaNoticias.add(noticia);
                        }

                        adapter.notifyDataSetChanged();

                        if (listaNoticias.isEmpty()) {
                            tvNoticiasEmptyState.setVisibility(View.VISIBLE);
                            rvNoticias.setVisibility(View.GONE);
                        } else {
                            tvNoticiasEmptyState.setVisibility(View.GONE);
                            rvNoticias.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistro != null) {
            listenerRegistro.remove();
            listenerRegistro = null;
        }
    }
}
