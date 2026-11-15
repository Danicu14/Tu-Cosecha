package com.danielcarpio.tucosecha;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.model.Pedido;
import com.danielcarpio.tucosecha.util.EstadoPedido;
import com.danielcarpio.tucosecha.util.Formato;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PedidoDetalleActivity extends AppCompatActivity {

    private TextView tvEstadoHeader, tvInfoEntrega, tvCountdown;
    private LinearLayout layoutProgressSteps;
    private View indicatorStep1, indicatorStep2, indicatorStep3, indicatorStep4;
    private TextView tvStep3Label;
    private TextView tvCancellationPendingWarning, tvCancellationRejectedWarning;
    private RecyclerView rvProductos;
    private MaterialButton btnSolicitarCancelacion;

    private FirebaseFirestore db;
    private ListenerRegistration pedidoListener;
    private String pedidoId;
    private Pedido currentPedido;
    private String previousEstado = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedido_detalle);

        db = FirebaseFirestore.getInstance();
        pedidoId = getIntent().getStringExtra("pedido_id");

        MaterialToolbar toolbar = findViewById(R.id.toolbarPedidoDetalle);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvEstadoHeader = findViewById(R.id.tvDetalleEstadoHeader);
        tvInfoEntrega = findViewById(R.id.tvDetalleInfoEntrega);
        tvCountdown = findViewById(R.id.tvDetalleCountdown);
        layoutProgressSteps = findViewById(R.id.layoutProgressSteps);
        indicatorStep1 = findViewById(R.id.indicatorStep1);
        indicatorStep2 = findViewById(R.id.indicatorStep2);
        indicatorStep3 = findViewById(R.id.indicatorStep3);
        indicatorStep4 = findViewById(R.id.indicatorStep4);
        tvStep3Label = findViewById(R.id.tvStep3Label);
        tvCancellationPendingWarning = findViewById(R.id.tvCancellationPendingWarning);
        tvCancellationRejectedWarning = findViewById(R.id.tvCancellationRejectedWarning);
        rvProductos = findViewById(R.id.rvPedidoDetalleProductos);
        btnSolicitarCancelacion = findViewById(R.id.btnSolicitarCancelacion);

        rvProductos.setLayoutManager(new LinearLayoutManager(this));

        btnSolicitarCancelacion.setOnClickListener(v -> mostrarDialogoCancelacion());

        loadPedidoRealtime();
    }

    private void loadPedidoRealtime() {
        if (pedidoId == null) return;

        if (pedidoListener != null) pedidoListener.remove();

        pedidoListener = db.collection("pedidos").document(pedidoId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e("PEDIDO_DETALLE", "Error al cargar detalle: " + error.getMessage(), error);
                        Mensajes.mostrarError(btnSolicitarCancelacion, "Error al cargar pedido");
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        currentPedido = documentSnapshot.toObject(Pedido.class);
                        if (currentPedido != null) {
                            if (currentPedido.getId() == null) currentPedido.setId(documentSnapshot.getId());

                            String newEstado = currentPedido.getEstado() != null ? currentPedido.getEstado().toLowerCase() : EstadoPedido.CONFIRMADO;
                            if (previousEstado != null && !previousEstado.equalsIgnoreCase(newEstado)) {
                                Mensajes.mostrarInfo(btnSolicitarCancelacion, EstadoPedido.getMensajeEstado(newEstado));
                            }
                            previousEstado = newEstado;

                            updateUI();
                        }
                    }
                });
    }

    private void updateUI() {
        if (currentPedido == null) return;

        String estado = currentPedido.getEstado() != null ? currentPedido.getEstado().toLowerCase() : EstadoPedido.CONFIRMADO;

        tvEstadoHeader.setText(EstadoPedido.getMensajeEstado(estado));

        if ("domicilio".equalsIgnoreCase(currentPedido.getMetodoEntrega()) && currentPedido.getDireccion() != null) {
            Map<String, Object> dir = currentPedido.getDireccion();
            String calle = dir.get("calle") != null ? String.valueOf(dir.get("calle")) : "";
            tvInfoEntrega.setText("Envío a domicilio: " + calle + "\nFecha: " + currentPedido.getFechaEntrega() + " (" + currentPedido.getHoraEntrega() + ")");
        } else {
            tvInfoEntrega.setText("Recogida en huerto: " + (currentPedido.getLugarRecogida() != null ? currentPedido.getLugarRecogida() : "Talavera de la Reina") + "\nFecha: " + currentPedido.getFechaEntrega() + " (" + currentPedido.getHoraEntrega() + ")");
        }

        // Countdown
        int days = calculateDaysRemaining(currentPedido.getFechaEntrega());
        if (days > 0) {
            tvCountdown.setText("Faltan " + days + (days == 1 ? " día" : " días"));
            tvCountdown.setVisibility(View.VISIBLE);
        } else if (days == 0) {
            tvCountdown.setText("Hoy es el día de entrega");
            tvCountdown.setVisibility(View.VISIBLE);
        } else {
            tvCountdown.setVisibility(View.GONE);
        }

        // Progress bar steps & warnings
        if (estado.equals(EstadoPedido.CANCELACION_SOLICITADA)) {
            layoutProgressSteps.setVisibility(View.GONE);
            tvCancellationPendingWarning.setVisibility(View.VISIBLE);
            tvCancellationRejectedWarning.setVisibility(View.GONE);
            btnSolicitarCancelacion.setVisibility(View.GONE);
        } else if (estado.equals(EstadoPedido.CANCELADO)) {
            layoutProgressSteps.setVisibility(View.GONE);
            tvCancellationPendingWarning.setVisibility(View.GONE);
            tvCancellationRejectedWarning.setVisibility(View.GONE);
            btnSolicitarCancelacion.setVisibility(View.GONE);
        } else {
            layoutProgressSteps.setVisibility(View.VISIBLE);
            updateProgressSteps(estado);

            // Check if rejected
            boolean isRejected = currentPedido.getCancelacion() != null && "rechazada".equalsIgnoreCase(String.valueOf(currentPedido.getCancelacion().get("respuesta")));
            if (isRejected) {
                tvCancellationRejectedWarning.setVisibility(View.VISIBLE);
            } else {
                tvCancellationRejectedWarning.setVisibility(View.GONE);
            }
            tvCancellationPendingWarning.setVisibility(View.GONE);

            // Show cancellation button only if confirmado or preparando
            if (estado.equals(EstadoPedido.CONFIRMADO) || estado.equals(EstadoPedido.PREPARANDO)) {
                btnSolicitarCancelacion.setVisibility(View.VISIBLE);
            } else {
                btnSolicitarCancelacion.setVisibility(View.GONE);
            }
        }

        // Products list
        if (currentPedido.getProductos() != null) {
            ProductosPedidoAdapter adapter = new ProductosPedidoAdapter(currentPedido.getProductos());
            rvProductos.setAdapter(adapter);
        }
    }

    private void updateProgressSteps(String estado) {
        int activeColor = Color.parseColor("#3E8E2F");
        int inactiveColor = Color.parseColor("#E0E0E0");

        indicatorStep1.setBackgroundColor(inactiveColor);
        indicatorStep2.setBackgroundColor(inactiveColor);
        indicatorStep3.setBackgroundColor(inactiveColor);
        indicatorStep4.setBackgroundColor(inactiveColor);

        boolean isRecogida = "recogida".equalsIgnoreCase(currentPedido.getMetodoEntrega());
        tvStep3Label.setText(isRecogida ? "Listo" : "En camino");

        switch (estado) {
            case EstadoPedido.ENTREGADO:
                indicatorStep4.setBackgroundColor(activeColor);
            case EstadoPedido.EN_CAMINO:
            case EstadoPedido.LISTO_PARA_RECOGER:
                indicatorStep3.setBackgroundColor(activeColor);
            case EstadoPedido.PREPARANDO:
                indicatorStep2.setBackgroundColor(activeColor);
            case EstadoPedido.CONFIRMADO:
            default:
                indicatorStep1.setBackgroundColor(activeColor);
                break;
        }
    }

    private int calculateDaysRemaining(String fechaStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date deliveryDate = sdf.parse(fechaStr);
            if (deliveryDate != null) {
                long diffMillis = deliveryDate.getTime() - System.currentTimeMillis();
                long days = diffMillis / (1000 * 60 * 60 * 24);
                return (int) days;
            }
        } catch (Exception e) {
            // ignore
        }
        return 1;
    }

    private void mostrarDialogoCancelacion() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_direccion, null); // or simple view with EditText
        // Let's build a clean MaterialAlertDialogBuilder with EditText for reason
        final EditText inputMotivo = new EditText(this);
        inputMotivo.setHint("Motivo de cancelación (opcional)");
        inputMotivo.setPadding(48, 32, 48, 32);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Solicitar cancelación")
                .setMessage("¿Deseas solicitar la cancelación de este pedido al huerto?")
                .setView(inputMotivo)
                .setPositiveButton("Enviar solicitud", (dialog, which) -> {
                    String motivo = inputMotivo.getText().toString().trim();
                    ejecutarSolicitudCancelacion(motivo);
                })
                .setNegativeButton("Volver", null)
                .show();
    }

    private void ejecutarSolicitudCancelacion(String motivo) {
        if (currentPedido == null || pedidoId == null) return;

        Map<String, Object> cancelMap = new HashMap<>();
        cancelMap.put("motivo", motivo);
        cancelMap.put("fecha", FieldValue.serverTimestamp());
        cancelMap.put("estadoAnterior", currentPedido.getEstado());

        Map<String, Object> updates = new HashMap<>();
        updates.put("estadoPrevio", currentPedido.getEstado());
        updates.put("estado", EstadoPedido.CANCELACION_SOLICITADA);
        updates.put("cancelacion", cancelMap);

        db.collection("pedidos").document(pedidoId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Mensajes.mostrarExito(btnSolicitarCancelacion, "Solicitud enviada al huerto");
                })
                .addOnFailureListener(e -> {
                    Mensajes.mostrarError(btnSolicitarCancelacion, "Error al enviar solicitud: " + e.getMessage());
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pedidoListener != null) {
            pedidoListener.remove();
            pedidoListener = null;
        }
    }

    private static class ProductosPedidoAdapter extends RecyclerView.Adapter<ProductosPedidoAdapter.ViewHolder> {
        private final List<Map<String, Object>> productos;

        public ProductosPedidoAdapter(List<Map<String, Object>> productos) {
            this.productos = productos;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cesta_draft, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> p = productos.get(position);
            holder.tvName.setText(String.valueOf(p.get("nombre")));
            
            double cantidad = 1.0;
            try {
                if (p.get("cantidad") instanceof Number) {
                    cantidad = ((Number) p.get("cantidad")).doubleValue();
                }
            } catch (Exception ignored) {}

            String unidad = p.get("unidad") != null ? String.valueOf(p.get("unidad")) : "ud";
            holder.tvQuantity.setText(Formato.cantidad(cantidad, unidad));

            int resId = holder.itemView.getContext().getResources().getIdentifier(p.get("imagen") != null ? String.valueOf(p.get("imagen")) : "logo_tucosecha", "drawable", holder.itemView.getContext().getPackageName());
            if (resId == 0) resId = R.drawable.logo_tucosecha;
            holder.imgPhoto.setImageResource(resId);
            holder.btnRemove.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return productos.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgPhoto;
            TextView tvName, tvQuantity;
            MaterialButton btnRemove;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgPhoto = itemView.findViewById(R.id.imgDraftPhoto);
                tvName = itemView.findViewById(R.id.tvDraftName);
                tvQuantity = itemView.findViewById(R.id.tvDraftQuantity);
                btnRemove = itemView.findViewById(R.id.btnRemoveDraftItem);
            }
        }
    }
}
