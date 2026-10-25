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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.NuevaNoticiaActivity;
import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.adapter.NoticiaAdapter;
import com.danielcarpio.tucosecha.model.Noticia;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.danielcarpio.tucosecha.util.SessionManager;
import com.google.android.material.button.MaterialButton;
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
    private List<Noticia> noticiaList;
    private NoticiaAdapter adapter;
    private ListenerRegistration listenerRegistration;
    private boolean isAdmin = false;

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
        noticiaList = new ArrayList<>();
        adapter = new NoticiaAdapter(getContext(), noticiaList, new NoticiaAdapter.OnNoticiaClickListener() {
            @Override
            public void onNoticiaClick(Noticia noticia) {
                DetalleNoticiaBottomSheet bottomSheet = DetalleNoticiaBottomSheet.newInstance(noticia);
                bottomSheet.show(getParentFragmentManager(), "DetalleNoticiaBottomSheet");
            }

            @Override
            public void onNoticiaLongClick(Noticia noticia) {
                if (isAdmin) {
                    new AlertDialog.Builder(requireContext())
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

        checkAdminRoleAndLoad();

        btnNoticiasRetry.setOnClickListener(v -> loadNoticiasRealtime());

        fabNuevaNoticia.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), NuevaNoticiaActivity.class));
        });

        return view;
    }

    private void checkAdminRoleAndLoad() {
        if (SessionManager.isEsInvitado()) {
            isAdmin = false;
            fabNuevaNoticia.setVisibility(View.GONE);
            loadNoticiasRealtime();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("usuarios").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String rol = documentSnapshot.getString("rol");
                            isAdmin = "administrador".equalsIgnoreCase(rol);
                            if (isAdmin) {
                                fabNuevaNoticia.setVisibility(View.VISIBLE);
                            } else {
                                fabNuevaNoticia.setVisibility(View.GONE);
                            }
                        }
                        loadNoticiasRealtime();
                    })
                    .addOnFailureListener(e -> loadNoticiasRealtime());
        } else {
            loadNoticiasRealtime();
        }
    }

    private void loadNoticiasRealtime() {
        progressBarNoticiasLoading.setVisibility(View.VISIBLE);
        tvNoticiasEmptyState.setVisibility(View.GONE);
        layoutNoticiasErrorState.setVisibility(View.GONE);
        rvNoticias.setVisibility(View.GONE);

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        listenerRegistration = db.collection("noticias")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
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

                    if (value != null) {
                        noticiaList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Noticia noticia = doc.toObject(Noticia.class);
                            if (noticia.getId() == null || noticia.getId().isEmpty()) {
                                noticia.setId(doc.getId());
                            }
                            noticiaList.add(noticia);
                        }

                        adapter.notifyDataSetChanged();

                        if (noticiaList.isEmpty()) {
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
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}
