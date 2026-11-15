package com.danielcarpio.tucosecha;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.model.Cosecha;
import com.danielcarpio.tucosecha.model.Pedido;
import com.danielcarpio.tucosecha.model.Usuario;
import com.danielcarpio.tucosecha.util.EstadoPedido;
import com.danielcarpio.tucosecha.util.Formato;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PedidosAdminActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ProgressBar progressAdminLoading;
    private TextView tvSemanaCosechaAdmin, tvTotalPrepararResumen;
    private ChipGroup chipGroupFiltros;
    private Chip chipFiltroPorPreparar, chipFiltroCancelaciones, chipFiltroEnReparto, chipFiltroEntregados, chipFiltroCancelados;
    private LinearLayout layoutEmptyAdmin, layoutErrorAdmin;
    private TextView tvEmptyAdminMessage;
    private MaterialButton btnReintentarAdmin;
    private RecyclerView rvPedidosAdmin;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration harvestListener;
    private ListenerRegistration pedidosListener;

    private String activeHarvestId = null;
    private String activeHarvestSemana = null;
    private final List<Pedido> allPedidos = new ArrayList<>();
    private final List<Pedido> filteredPedidos = new ArrayList<>();
    private PedidosAdminAdapter adapter;

    private enum Filtro {
        POR_PREPARAR, CANCELACIONES, EN_REPARTO, ENTREGADOS, CANCELADOS
    }

    private Filtro filtroActual = Filtro.POR_PREPARAR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedidos_admin);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbarPedidosAdmin);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressAdminLoading = findViewById(R.id.progressAdminLoading);
        tvSemanaCosechaAdmin = findViewById(R.id.tvSemanaCosechaAdmin);
        tvTotalPrepararResumen = findViewById(R.id.tvTotalPrepararResumen);

        chipGroupFiltros = findViewById(R.id.chipGroupFiltros);
        chipFiltroPorPreparar = findViewById(R.id.chipFiltroPorPreparar);
        chipFiltroCancelaciones = findViewById(R.id.chipFiltroCancelaciones);
        chipFiltroEnReparto = findViewById(R.id.chipFiltroEnReparto);
        chipFiltroEntregados = findViewById(R.id.chipFiltroEntregados);
        chipFiltroCancelados = findViewById(R.id.chipFiltroCancelados);

        layoutEmptyAdmin = findViewById(R.id.layoutEmptyAdmin);
        tvEmptyAdminMessage = findViewById(R.id.tvEmptyAdminMessage);
        layoutErrorAdmin = findViewById(R.id.layoutErrorAdmin);
        btnReintentarAdmin = findViewById(R.id.btnReintentarAdmin);
        rvPedidosAdmin = findViewById(R.id.rvPedidosAdmin);

        rvPedidosAdmin.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PedidosAdminAdapter(filteredPedidos, new PedidosAdminAdapter.OnPedidoActionCallback() {
            @Override
            public void onAvanzarEstado(Pedido pedido, String nuevoEstado, String mensajeExito) {
                avanzarEstadoPedido(pedido, nuevoEstado, mensajeExito);
            }

            @Override
            public void onAceptarCancelacion(Pedido pedido) {
                confirmarAceptarCancelacion(pedido);
            }

            @Override
            public void onRechazarCancelacion(Pedido pedido) {
                confirmarRechazarCancelacion(pedido);
            }
        });
        rvPedidosAdmin.setAdapter(adapter);

        setupFiltroChips();
        btnReintentarAdmin.setOnClickListener(v -> checkAdminRoleAndLoad());

        checkAdminRoleAndLoad();
    }

    private void checkAdminRoleAndLoad() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        progressAdminLoading.setVisibility(View.VISIBLE);
        layoutErrorAdmin.setVisibility(View.GONE);

        db.collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        finish();
                        return;
                    }

                    Usuario usuario = documentSnapshot.toObject(Usuario.class);
                    if (usuario == null || !"administrador".equalsIgnoreCase(usuario.getRol())) {
                        finish();
                        return;
                    }

                    loadActiveHarvestAndPedidos();
                })
                .addOnFailureListener(e -> {
                    progressAdminLoading.setVisibility(View.GONE);
                    layoutErrorAdmin.setVisibility(View.VISIBLE);
                    Mensajes.mostrarError(toolbar, "Error de verificación: " + e.getMessage());
                });
    }

    private void loadActiveHarvestAndPedidos() {
        if (harvestListener != null) harvestListener.remove();

        harvestListener = db.collection("cosechas")
                .whereEqualTo("activa", true)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("PEDIDOS_ADMIN", "Error al cargar cosecha: " + e.getMessage(), e);
                        progressAdminLoading.setVisibility(View.GONE);
                        layoutErrorAdmin.setVisibility(View.VISIBLE);
                        Mensajes.mostrarError(toolbar, "Error al cargar cosecha activa");
                        return;
                    }

                    activeHarvestId = null;
                    activeHarvestSemana = null;

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Cosecha cosecha = doc.toObject(Cosecha.class);
                            activeHarvestId = doc.getId();
                            activeHarvestSemana = cosecha.getSemana();
                            break;
                        }
                    }

                    if (activeHarvestId == null) {
                        progressAdminLoading.setVisibility(View.GONE);
                        tvSemanaCosechaAdmin.setText("No hay cosecha activa");
                        tvTotalPrepararResumen.setText("Sin pedidos pendientes");
                        allPedidos.clear();
                        aplicarFiltroYActualizarUI();
                        return;
                    }

                    tvSemanaCosechaAdmin.setText(activeHarvestSemana != null ? activeHarvestSemana : "Cosecha activa");
                    listenPedidosRealtime(activeHarvestId);
                });
    }

    private void listenPedidosRealtime(String harvestId) {
        if (pedidosListener != null) pedidosListener.remove();

        pedidosListener = db.collection("pedidos")
                .whereEqualTo("idCosecha", harvestId)
                .addSnapshotListener((snapshots, e) -> {
                    progressAdminLoading.setVisibility(View.GONE);

                    if (e != null) {
                        Log.e("PEDIDOS_ADMIN", "Error al cargar pedidos: " + e.getMessage(), e);
                        layoutErrorAdmin.setVisibility(View.VISIBLE);
                        Mensajes.mostrarError(toolbar, "Error al cargar pedidos: " + e.getMessage());
                        return;
                    }

                    allPedidos.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Pedido pedido = doc.toObject(Pedido.class);
                            if (pedido.getId() == null || pedido.getId().isEmpty()) {
                                pedido.setId(doc.getId());
                            }
                            allPedidos.add(pedido);
                        }
                    }

                    // Client-side sort by fechaPedido descending
                    Collections.sort(allPedidos, (p1, p2) -> {
                        Object f1 = p1.getFechaPedido();
                        Object f2 = p2.getFechaPedido();
                        if (f1 instanceof Timestamp && f2 instanceof Timestamp) {
                            return ((Timestamp) f2).compareTo((Timestamp) f1);
                        }
                        return 0;
                    });

                    actualizarResumenTotalPreparar();
                    actualizarContadorCancelaciones();
                    aplicarFiltroYActualizarUI();
                });
    }

    private void actualizarResumenTotalPreparar() {
        Map<String, Double> productSums = new LinkedHashMap<>();
        Map<String, String> productUnits = new HashMap<>();

        for (Pedido p : allPedidos) {
            if (EstadoPedido.CANCELADO.equalsIgnoreCase(p.getEstado())) {
                continue; // Except cancelados
            }

            if (p.getProductos() != null) {
                for (Map<String, Object> prod : p.getProductos()) {
                    String nombre = prod.get("nombre") != null ? String.valueOf(prod.get("nombre")) : "Producto";
                    double cant = 0;
                    if (prod.get("cantidad") instanceof Number) {
                        cant = ((Number) prod.get("cantidad")).doubleValue();
                    }
                    String unidad = prod.get("unidad") != null ? String.valueOf(prod.get("unidad")) : "ud";

                    productSums.put(nombre, productSums.getOrDefault(nombre, 0.0) + cant);
                    productUnits.put(nombre, unidad);
                }
            }
        }

        if (productSums.isEmpty()) {
            tvTotalPrepararResumen.setText("Sin pedidos activos todavía para esta cosecha");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Double> entry : productSums.entrySet()) {
                String nombre = entry.getKey();
                double totalCant = entry.getValue();
                String unidad = productUnits.get(nombre);
                sb.append("• ").append(nombre).append(": ").append(Formato.cantidad(totalCant, unidad)).append("\n");
            }
            tvTotalPrepararResumen.setText(sb.toString().trim());
        }
    }

    private void actualizarContadorCancelaciones() {
        int count = 0;
        for (Pedido p : allPedidos) {
            if (EstadoPedido.CANCELACION_SOLICITADA.equalsIgnoreCase(p.getEstado())) {
                count++;
            }
        }

        if (count > 0) {
            chipFiltroCancelaciones.setText("Cancelaciones (" + count + ")");
        } else {
            chipFiltroCancelaciones.setText("Cancelaciones");
        }
    }

    private void setupFiltroChips() {
        chipGroupFiltros.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipFiltroPorPreparar) {
                filtroActual = Filtro.POR_PREPARAR;
            } else if (id == R.id.chipFiltroCancelaciones) {
                filtroActual = Filtro.CANCELACIONES;
            } else if (id == R.id.chipFiltroEnReparto) {
                filtroActual = Filtro.EN_REPARTO;
            } else if (id == R.id.chipFiltroEntregados) {
                filtroActual = Filtro.ENTREGADOS;
            } else if (id == R.id.chipFiltroCancelados) {
                filtroActual = Filtro.CANCELADOS;
            }
            aplicarFiltroYActualizarUI();
        });
    }

    private void aplicarFiltroYActualizarUI() {
        filteredPedidos.clear();
        String emptyMessage = "No hay pedidos en esta sección";

        for (Pedido p : allPedidos) {
            String est = p.getEstado() != null ? p.getEstado().toLowerCase() : EstadoPedido.CONFIRMADO;
            switch (filtroActual) {
                case POR_PREPARAR:
                    if (est.equals(EstadoPedido.CONFIRMADO) || est.equals(EstadoPedido.PREPARANDO)) {
                        filteredPedidos.add(p);
                    }
                    emptyMessage = "No hay pedidos por preparar";
                    break;
                case CANCELACIONES:
                    if (est.equals(EstadoPedido.CANCELACION_SOLICITADA)) {
                        filteredPedidos.add(p);
                    }
                    emptyMessage = "No hay solicitudes de cancelación pendientes";
                    break;
                case EN_REPARTO:
                    if (est.equals(EstadoPedido.EN_CAMINO) || est.equals(EstadoPedido.LISTO_PARA_RECOGER)) {
                        filteredPedidos.add(p);
                    }
                    emptyMessage = "No hay pedidos en reparto";
                    break;
                case ENTREGADOS:
                    if (est.equals(EstadoPedido.ENTREGADO)) {
                        filteredPedidos.add(p);
                    }
                    emptyMessage = "No hay pedidos entregados";
                    break;
                case CANCELADOS:
                    if (est.equals(EstadoPedido.CANCELADO)) {
                        filteredPedidos.add(p);
                    }
                    emptyMessage = "No hay pedidos cancelados";
                    break;
            }
        }

        adapter.notifyDataSetChanged();

        if (filteredPedidos.isEmpty()) {
            tvEmptyAdminMessage.setText(emptyMessage);
            layoutEmptyAdmin.setVisibility(View.VISIBLE);
            rvPedidosAdmin.setVisibility(View.GONE);
        } else {
            layoutEmptyAdmin.setVisibility(View.GONE);
            rvPedidosAdmin.setVisibility(View.VISIBLE);
        }
    }

    private void avanzarEstadoPedido(Pedido pedido, String nuevoEstado, String mensajeExito) {
        if (pedido == null || pedido.getId() == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("estado", nuevoEstado);

        db.collection("pedidos").document(pedido.getId()).update(updates)
                .addOnSuccessListener(aVoid -> Mensajes.mostrarExito(toolbar, mensajeExito))
                .addOnFailureListener(e -> Mensajes.mostrarError(toolbar, "Error al actualizar estado: " + e.getMessage()));
    }

    private void confirmarAceptarCancelacion(Pedido pedido) {
        String nombre = pedido.getNombreUsuario() != null ? pedido.getNombreUsuario() : "este cliente";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Aceptar cancelación")
                .setMessage("¿Deseas aceptar la cancelación del pedido de " + nombre + "?")
                .setPositiveButton("Sí, aceptar", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("estado", EstadoPedido.CANCELADO);
                    updates.put("cancelacion.respuesta", "aceptada");

                    db.collection("pedidos").document(pedido.getId()).update(updates)
                            .addOnSuccessListener(aVoid -> Mensajes.mostrarExito(toolbar, "Cancelación aceptada"))
                            .addOnFailureListener(e -> Mensajes.mostrarError(toolbar, "Error al cancelar: " + e.getMessage()));
                })
                .setNegativeButton("Volver", null)
                .show();
    }

    private void confirmarRechazarCancelacion(Pedido pedido) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Rechazar cancelación")
                .setMessage("¿Deseas rechazar la solicitud de cancelación y volver al estado anterior?")
                .setPositiveButton("Sí, rechazar", (dialog, which) -> {
                    String estadoVolver = (pedido.getEstadoPrevio() != null && !pedido.getEstadoPrevio().isEmpty())
                            ? pedido.getEstadoPrevio()
                            : EstadoPedido.CONFIRMADO;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("estado", estadoVolver);
                    updates.put("cancelacion.respuesta", "rechazada");

                    db.collection("pedidos").document(pedido.getId()).update(updates)
                            .addOnSuccessListener(aVoid -> Mensajes.mostrarExito(toolbar, "Solicitud de cancelación rechazada"))
                            .addOnFailureListener(e -> Mensajes.mostrarError(toolbar, "Error al rechazar: " + e.getMessage()));
                })
                .setNegativeButton("Volver", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (harvestListener != null) {
            harvestListener.remove();
            harvestListener = null;
        }
        if (pedidosListener != null) {
            pedidosListener.remove();
            pedidosListener = null;
        }
    }

    // ADAPTER FOR ADMIN ORDERS
    private static class PedidosAdminAdapter extends RecyclerView.Adapter<PedidosAdminAdapter.ViewHolder> {

        public interface OnPedidoActionCallback {
            void onAvanzarEstado(Pedido pedido, String nuevoEstado, String mensajeExito);
            void onAceptarCancelacion(Pedido pedido);
            void onRechazarCancelacion(Pedido pedido);
        }

        private final List<Pedido> pedidos;
        private final OnPedidoActionCallback callback;

        public PedidosAdminAdapter(List<Pedido> pedidos, OnPedidoActionCallback callback) {
            this.pedidos = pedidos;
            this.callback = callback;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pedido_admin, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Pedido pedido = pedidos.get(position);

            // Cliente
            holder.tvCliente.setText(pedido.getNombreUsuario() != null ? pedido.getNombreUsuario() : "Cliente");

            // Plan
            String planStr = pedido.getPlan() != null ? pedido.getPlan() : "Básico";
            holder.tvPlan.setText("Plan " + planStr.substring(0, 1).toUpperCase() + planStr.substring(1));

            // Chip Estado
            String estado = pedido.getEstado() != null ? pedido.getEstado().toLowerCase() : EstadoPedido.CONFIRMADO;
            setupStatusChip(holder.chipEstado, estado);

            // Metodo de entrega, direccion y telefono
            boolean esDomicilio = "domicilio".equalsIgnoreCase(pedido.getMetodoEntrega());
            if (esDomicilio && pedido.getDireccion() != null) {
                Map<String, Object> dir = pedido.getDireccion();
                String calle = dir.get("calle") != null ? String.valueOf(dir.get("calle")).trim() : "";
                String piso = (dir.get("piso") != null && !dir.get("piso").toString().trim().isEmpty()) ? " " + dir.get("piso").toString().trim() : "";
                String loc = dir.get("localidad") != null ? String.valueOf(dir.get("localidad")).trim() : "Talavera de la Reina";
                String cp = (dir.get("codigoPostal") != null && !dir.get("codigoPostal").toString().trim().isEmpty()) ? " (" + dir.get("codigoPostal").toString().trim() + ")" : "";
                String tel = dir.get("telefono") != null ? String.valueOf(dir.get("telefono")).trim() : "";

                StringBuilder sbDir = new StringBuilder();
                sbDir.append("Envío a domicilio: ").append(calle).append(piso).append(", ").append(loc).append(cp);
                if (!tel.isEmpty()) {
                    sbDir.append("\nTeléfono: ").append(tel);
                }
                holder.tvMetodoYDireccion.setText(sbDir.toString());

                // Indicaciones
                String ind = dir.get("indicaciones") != null ? String.valueOf(dir.get("indicaciones")).trim() : "";
                if (!ind.isEmpty()) {
                    holder.tvIndicaciones.setText("Indicaciones: " + ind);
                    holder.tvIndicaciones.setVisibility(View.VISIBLE);
                } else {
                    holder.tvIndicaciones.setVisibility(View.GONE);
                }
            } else {
                holder.tvMetodoYDireccion.setText("Recoge en el huerto" + (pedido.getLugarRecogida() != null ? ": " + pedido.getLugarRecogida() : ""));
                holder.tvIndicaciones.setVisibility(View.GONE);
            }

            // Lista de productos con cantidades
            if (pedido.getProductos() != null && !pedido.getProductos().isEmpty()) {
                StringBuilder sbProd = new StringBuilder();
                for (Map<String, Object> prod : pedido.getProductos()) {
                    String nombre = prod.get("nombre") != null ? String.valueOf(prod.get("nombre")) : "Producto";
                    double cant = 1.0;
                    if (prod.get("cantidad") instanceof Number) {
                        cant = ((Number) prod.get("cantidad")).doubleValue();
                    }
                    String unidad = prod.get("unidad") != null ? String.valueOf(prod.get("unidad")) : "ud";
                    sbProd.append("• ").append(Formato.cantidad(cant, unidad)).append(" ").append(nombre).append("\n");
                }
                holder.tvProductos.setText(sbProd.toString().trim());
            } else {
                holder.tvProductos.setText("Sin productos registrados");
            }

            // Actions logic
            if (EstadoPedido.CANCELACION_SOLICITADA.equalsIgnoreCase(estado)) {
                holder.layoutCancelacion.setVisibility(View.VISIBLE);
                holder.btnAccionEstado.setVisibility(View.GONE);

                String motivo = "No especificado";
                if (pedido.getCancelacion() != null && pedido.getCancelacion().get("motivo") != null) {
                    String m = String.valueOf(pedido.getCancelacion().get("motivo")).trim();
                    if (!m.isEmpty()) motivo = m;
                }
                holder.tvMotivoCancelacion.setText("Motivo: " + motivo);

                holder.btnAceptarCancelacion.setOnClickListener(v -> {
                    if (callback != null) callback.onAceptarCancelacion(pedido);
                });

                holder.btnRechazarCancelacion.setOnClickListener(v -> {
                    if (callback != null) callback.onRechazarCancelacion(pedido);
                });
            } else {
                holder.layoutCancelacion.setVisibility(View.GONE);

                if (EstadoPedido.CONFIRMADO.equalsIgnoreCase(estado)) {
                    holder.btnAccionEstado.setVisibility(View.VISIBLE);
                    holder.btnAccionEstado.setText("Empezar a preparar");
                    holder.btnAccionEstado.setOnClickListener(v -> {
                        if (callback != null) {
                            callback.onAvanzarEstado(pedido, EstadoPedido.PREPARANDO, "Pedido marcado en preparación");
                        }
                    });
                } else if (EstadoPedido.PREPARANDO.equalsIgnoreCase(estado)) {
                    holder.btnAccionEstado.setVisibility(View.VISIBLE);
                    if (esDomicilio) {
                        holder.btnAccionEstado.setText("Marcar en camino");
                        holder.btnAccionEstado.setOnClickListener(v -> {
                            if (callback != null) {
                                callback.onAvanzarEstado(pedido, EstadoPedido.EN_CAMINO, "Pedido marcado en camino");
                            }
                        });
                    } else {
                        holder.btnAccionEstado.setText("Marcar listo para recoger");
                        holder.btnAccionEstado.setOnClickListener(v -> {
                            if (callback != null) {
                                callback.onAvanzarEstado(pedido, EstadoPedido.LISTO_PARA_RECOGER, "Pedido listo para recoger");
                            }
                        });
                    }
                } else if (EstadoPedido.EN_CAMINO.equalsIgnoreCase(estado) || EstadoPedido.LISTO_PARA_RECOGER.equalsIgnoreCase(estado)) {
                    holder.btnAccionEstado.setVisibility(View.VISIBLE);
                    holder.btnAccionEstado.setText("Marcar entregado");
                    holder.btnAccionEstado.setOnClickListener(v -> {
                        if (callback != null) {
                            callback.onAvanzarEstado(pedido, EstadoPedido.ENTREGADO, "Pedido marcado como entregado");
                        }
                    });
                } else {
                    // ENTREGADO or CANCELADO
                    holder.btnAccionEstado.setVisibility(View.GONE);
                }
            }
        }

        private void setupStatusChip(Chip chip, String estado) {
            String text;
            int bgColor;
            int textColor;

            switch (estado) {
                case EstadoPedido.PREPARANDO:
                    text = "Preparando";
                    bgColor = Color.parseColor("#FFF8E1");
                    textColor = Color.parseColor("#FF8F00");
                    break;
                case EstadoPedido.EN_CAMINO:
                    text = "En camino";
                    bgColor = Color.parseColor("#E3F2FD");
                    textColor = Color.parseColor("#1565C0");
                    break;
                case EstadoPedido.LISTO_PARA_RECOGER:
                    text = "Listo para recoger";
                    bgColor = Color.parseColor("#E3F2FD");
                    textColor = Color.parseColor("#1565C0");
                    break;
                case EstadoPedido.ENTREGADO:
                    text = "Entregado";
                    bgColor = Color.parseColor("#CFD8DC");
                    textColor = Color.parseColor("#2E3E34");
                    break;
                case EstadoPedido.CANCELACION_SOLICITADA:
                    text = "Cancelación solicitada";
                    bgColor = Color.parseColor("#FFE0B2");
                    textColor = Color.parseColor("#E65100");
                    break;
                case EstadoPedido.CANCELADO:
                    text = "Cancelado";
                    bgColor = Color.parseColor("#FFCDD2");
                    textColor = Color.parseColor("#C62828");
                    break;
                case EstadoPedido.CONFIRMADO:
                default:
                    text = "Confirmado";
                    bgColor = Color.parseColor("#C8E6C9");
                    textColor = Color.parseColor("#1F5A2A");
                    break;
            }

            chip.setText(text);
            chip.setChipBackgroundColor(ColorStateList.valueOf(bgColor));
            chip.setTextColor(textColor);
        }

        @Override
        public int getItemCount() {
            return pedidos.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCliente, tvPlan, tvMetodoYDireccion, tvIndicaciones, tvProductos, tvMotivoCancelacion;
            Chip chipEstado;
            View layoutCancelacion;
            MaterialButton btnAceptarCancelacion, btnRechazarCancelacion, btnAccionEstado;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCliente = itemView.findViewById(R.id.tvAdminPedidoCliente);
                tvPlan = itemView.findViewById(R.id.tvAdminPedidoPlan);
                chipEstado = itemView.findViewById(R.id.chipAdminPedidoEstado);
                tvMetodoYDireccion = itemView.findViewById(R.id.tvAdminPedidoMetodoYDireccion);
                tvIndicaciones = itemView.findViewById(R.id.tvAdminPedidoIndicaciones);
                tvProductos = itemView.findViewById(R.id.tvAdminPedidoProductos);
                layoutCancelacion = itemView.findViewById(R.id.layoutAdminCancelacion);
                tvMotivoCancelacion = itemView.findViewById(R.id.tvAdminMotivoCancelacion);
                btnAceptarCancelacion = itemView.findViewById(R.id.btnAdminAceptarCancelacion);
                btnRechazarCancelacion = itemView.findViewById(R.id.btnAdminRechazarCancelacion);
                btnAccionEstado = itemView.findViewById(R.id.btnAdminAccionEstado);
            }
        }
    }
}
