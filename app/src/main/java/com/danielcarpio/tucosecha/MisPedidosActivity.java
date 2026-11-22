package com.danielcarpio.tucosecha;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.adapter.PedidoAdapter;
import com.danielcarpio.tucosecha.model.Pedido;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MisPedidosActivity extends AppCompatActivity {

    private RecyclerView rvMisPedidos;
    private LinearLayout layoutEmpty, layoutError;
    private MaterialButton btnCrearMiCesta, btnReintentar;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration pedidosListener;
    private PedidoAdapter adapter;
    private final List<Pedido> pedidoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mis_pedidos);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbarMisPedidos);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvMisPedidos = findViewById(R.id.rvMisPedidos);
        layoutEmpty = findViewById(R.id.layoutMisPedidosEmpty);
        layoutError = findViewById(R.id.layoutMisPedidosError);
        btnCrearMiCesta = findViewById(R.id.btnCrearMiCesta);
        btnReintentar = findViewById(R.id.btnReintentarPedidos);

        rvMisPedidos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PedidoAdapter(pedido -> {
            Intent intent = new Intent(MisPedidosActivity.this, PedidoDetalleActivity.class);
            intent.putExtra("pedido_id", pedido.getId());
            startActivity(intent);
        });
        rvMisPedidos.setAdapter(adapter);

        btnCrearMiCesta.setOnClickListener(v -> {
            finish(); // Returns to MainActivity / CestaFragment
        });

        btnReintentar.setOnClickListener(v -> loadMisPedidos());

        loadMisPedidos();
    }

    private void loadMisPedidos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvMisPedidos.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);
            return;
        }

        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        rvMisPedidos.setVisibility(View.VISIBLE);

        if (pedidosListener != null) pedidosListener.remove();

        pedidosListener = db.collection("pedidos")
                .whereEqualTo("uid", user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("MIS_PEDIDOS", "Error al cargar pedidos: " + error.getMessage(), error);
                        layoutError.setVisibility(View.VISIBLE);
                        rvMisPedidos.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.GONE);
                        Mensajes.mostrarError(rvMisPedidos, "Error al cargar pedidos: " + error.getMessage());
                        return;
                    }

                    pedidoList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Pedido pedido = doc.toObject(Pedido.class);
                            if (pedido.getId() == null || pedido.getId().isEmpty()) {
                                pedido.setId(doc.getId());
                            }
                            pedidoList.add(pedido);
                        }
                    }

                    // Sort client-side by fechaPedido descending if available
                    Collections.sort(pedidoList, (p1, p2) -> {
                        Object f1 = p1.getFechaPedido();
                        Object f2 = p2.getFechaPedido();
                        if (f1 instanceof com.google.firebase.Timestamp && f2 instanceof com.google.firebase.Timestamp) {
                            return ((com.google.firebase.Timestamp) f2).compareTo((com.google.firebase.Timestamp) f1);
                        }
                        return 0;
                    });

                    adapter.setPedidos(pedidoList);

                    if (pedidoList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvMisPedidos.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvMisPedidos.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pedidosListener != null) {
            pedidosListener.remove();
            pedidosListener = null;
        }
    }
}
