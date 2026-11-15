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
    private View layoutProgressSteps;
    private ImageView imgStep1, imgStep2, imgStep3, imgStep4;
    private TextView tvStep1Label, tvStep2Label, tvStep3Label, tvStep4Label;
    private View cardCancellationPending, cardCancellationRejected;
    private RecyclerView rvProductos;
    private MaterialButton btnSolicitarCancelacion;

    private FirebaseFirestore db;
    private ListenerRegistration pedidoListener;
    private String pedidoId;
    private Pedido currentPedido;
    private String previousEstado = null;
    private String previousRespuesta = null;

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
        imgStep1 = findViewById(R.id.imgStep1);
        imgStep2 = findViewById(R.id.imgStep2);
        imgStep3 = findViewById(R.id.imgStep3);
        imgStep4 = findViewById(R.id.imgStep4);
        tvStep1Label = findViewById(R.id.tvStep1Label);
        tvStep2Label = findViewById(R.id.tvStep2Label);
        tvStep3Label = findViewById(R.id.tvStep3Label);
        tvStep4Label = findViewById(R.id.tvStep4Label);
        cardCancellationPending = findViewById(R.id.cardCancellationPending);
        cardCancellationRejected = findViewById(R.id.cardCancellationRejected);
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
                            Map<String, Object> cancel = currentPedido.getCancelacion();
                            String newRespuesta = (cancel != null && cancel.get("respuesta") != null) ? String.valueOf(cancel.get("respuesta")) : null;

                            if (previousEstado != null && !previousEstado.equalsIgnoreCase(newEstado)) {
                                if (EstadoPedido.CANCELADO.equalsIgnoreCase(newEstado)) {
                                    if ("aceptada".equalsIgnoreCase(newRespuesta)) {
                                        Mensajes.mostrarInfo(btnSolicitarCancelacion, "Tu cancelación ha sido aceptada");
                                    } else {
                                        Mensajes.mostrarInfo(btnSolicitarCancelacion, "Pedido cancelado");
                                    }
                                } else if (EstadoPedido.EN_CAMINO.equalsIgnoreCase(newEstado)) {
                                    Mensajes.mostrarInfo(btnSolicitarCancelacion, "¡Tu pedido está en camino!");
                                } else if (EstadoPedido.LISTO_PARA_RECOGER.equalsIgnoreCase(newEstado)) {
                                    Mensajes.mostrarInfo(btnSolicitarCancelacion, "Tu cesta te espera en el huerto");
                                } else if (EstadoPedido.PREPARANDO.equalsIgnoreCase(newEstado)) {
                                    Mensajes.mostrarInfo(btnSolicitarCancelacion, "Estamos preparando tu cesta");
                                } else if (EstadoPedido.ENTREGADO.equalsIgnoreCase(newEstado)) {
                                    Mensajes.mostrarInfo(btnSolicitarCancelacion, "¡Cesta entregada! Que aproveche");
                                } else {
                                    Mensajes.mostrarInfo(btnSolicitarCancelacion, EstadoPedido.getMensajeEstado(newEstado));
                                }
                            } else if (previousRespuesta != null && !previousRespuesta.equalsIgnoreCase(newRespuesta)) {
                                if ("rechazada".equalsIgnoreCase(newRespuesta)) {
                                    Mensajes.mostrarInfo(btnSolicitarCancelacion, "El huerto no ha podido cancelar tu pedido");
                                } else if ("aceptada".equalsIgnoreCase(newRespuesta)) {
                                    Mensajes.mostrarInfo(btnSolicitarCancelacion, "Tu cancelación ha sido aceptada");
                                }
                            }

                            previousEstado = newEstado;
                            previousRespuesta = newRespuesta;

                            updateUI();
                        }
                    }
                });
    }

    private void updateUI() {
        if (currentPedido == null) return;

        String estado = currentPedido.getEstado() != null ? currentPedido.getEstado().toLowerCase() : EstadoPedido.CONFIRMADO;

        tvEstadoHeader.setText(EstadoPedido.getMensajeEstado(estado));

        String franja = (currentPedido.getHoraEntrega() != null && !currentPedido.getHoraEntrega().trim().isEmpty()) ? " (" + currentPedido.getHoraEntrega().trim() + ")" : "";
        String fecha = (currentPedido.getFechaEntrega() != null ? currentPedido.getFechaEntrega().trim() : "-");

        if ("domicilio".equalsIgnoreCase(currentPedido.getMetodoEntrega()) && currentPedido.getDireccion() != null) {
            Map<String, Object> dir = currentPedido.getDireccion();
            String calle = dir.get("calle") != null ? String.valueOf(dir.get("calle")).trim() : "";
            String localidad = dir.get("localidad") != null ? String.valueOf(dir.get("localidad")).trim() : "Talavera de la Reina";
            tvInfoEntrega.setText("Te la mandamos a: " + calle + ", " + localidad + "\nFecha: " + fecha + franja);
        } else {
            String lugar = currentPedido.getLugarRecogida() != null ? currentPedido.getLugarRecogida().trim() : "Huerto Tu Cosecha";
            tvInfoEntrega.setText("Recógela en: " + lugar + "\nFecha: " + fecha + franja);
        }

        // Countdown: "Faltan N días" solo si N > 0, "Hoy es el día de entrega" si es hoy, y nada si ya pasó.
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
            cardCancellationPending.setVisibility(View.VISIBLE);
            cardCancellationRejected.setVisibility(View.GONE);
            btnSolicitarCancelacion.setVisibility(View.GONE);
        } else if (estado.equals(EstadoPedido.CANCELADO)) {
            layoutProgressSteps.setVisibility(View.GONE);
            cardCancellationPending.setVisibility(View.GONE);
            cardCancellationRejected.setVisibility(View.GONE);
            btnSolicitarCancelacion.setVisibility(View.GONE);
        } else {
            layoutProgressSteps.setVisibility(View.VISIBLE);
            updateProgressSteps(estado);

            boolean isRejected = currentPedido.getCancelacion() != null && "rechazada".equalsIgnoreCase(String.valueOf(currentPedido.getCancelacion().get("respuesta")));
            if (isRejected) {
                cardCancellationRejected.setVisibility(View.VISIBLE);
            } else {
                cardCancellationRejected.setVisibility(View.GONE);
            }
            cardCancellationPending.setVisibility(View.GONE);

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
        int currentStepIndex;
        switch (estado) {
            case EstadoPedido.ENTREGADO:
                currentStepIndex = 4;
                break;
            case EstadoPedido.EN_CAMINO:
            case EstadoPedido.LISTO_PARA_RECOGER:
                currentStepIndex = 3;
                break;
            case EstadoPedido.PREPARANDO:
                currentStepIndex = 2;
                break;
            case EstadoPedido.CONFIRMADO:
            default:
                currentStepIndex = 1;
                break;
        }

        boolean isRecogida = "recogida".equalsIgnoreCase(currentPedido.getMetodoEntrega());
        tvStep3Label.setText(isRecogida ? "Listo" : "En camino");

        applyStepState(1, currentStepIndex, imgStep1, tvStep1Label, R.drawable.ic_step_confirmado);
        applyStepState(2, currentStepIndex, imgStep2, tvStep2Label, R.drawable.ic_step_preparando);
        applyStepState(3, currentStepIndex, imgStep3, tvStep3Label, isRecogida ? R.drawable.ic_step_listo : R.drawable.ic_step_camino);
        applyStepState(4, currentStepIndex, imgStep4, tvStep4Label, R.drawable.ic_step_entregado);
    }

    private void applyStepState(int stepIndex, int currentStepIndex, ImageView img, TextView tv, int defaultIconRes) {
        if (stepIndex < currentStepIndex) {
            // Anterior: marcado como hecho
            img.setBackgroundResource(R.drawable.bg_step_circle_done);
            img.setImageResource(R.drawable.ic_step_check);
            img.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
            tv.setTextColor(Color.parseColor("#1F5A2A"));
            tv.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else if (stepIndex == currentStepIndex) {
            // Actual: resaltado
            img.setBackgroundResource(R.drawable.bg_step_circle_active);
            img.setImageResource(defaultIconRes);
            img.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
            tv.setTextColor(Color.parseColor("#1F5A2A"));
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            // Futuro: inactivo
            img.setBackgroundResource(R.drawable.bg_step_circle_inactive);
            img.setImageResource(defaultIconRes);
            img.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            tv.setTextColor(Color.parseColor("#9E9E9E"));
            tv.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private int calculateDaysRemaining(String fechaStr) {
        if (fechaStr == null || fechaStr.trim().isEmpty()) return -1;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date deliveryDate = sdf.parse(fechaStr.trim());
            if (deliveryDate != null) {
                java.util.Calendar calToday = java.util.Calendar.getInstance();
                calToday.set(java.util.Calendar.HOUR_OF_DAY, 0);
                calToday.set(java.util.Calendar.MINUTE, 0);
                calToday.set(java.util.Calendar.SECOND, 0);
                calToday.set(java.util.Calendar.MILLISECOND, 0);

                java.util.Calendar calDelivery = java.util.Calendar.getInstance();
                calDelivery.setTime(deliveryDate);
                calDelivery.set(java.util.Calendar.HOUR_OF_DAY, 0);
                calDelivery.set(java.util.Calendar.MINUTE, 0);
                calDelivery.set(java.util.Calendar.SECOND, 0);
                calDelivery.set(java.util.Calendar.MILLISECOND, 0);

                long diffMillis = calDelivery.getTimeInMillis() - calToday.getTimeInMillis();
                return (int) (diffMillis / (1000 * 60 * 60 * 24));
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private void mostrarDialogoCancelacion() {
        final EditText inputMotivo = new EditText(this);
        inputMotivo.setHint("Motivo (opcional)");
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, (int) (8 * getResources().getDisplayMetrics().density), pad, 0);
        container.addView(inputMotivo);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Solicitar cancelación")
                .setMessage("¿Deseas solicitar la cancelación de este pedido al huerto?")
                .setView(container)
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
        cancelMap.put("solicitada", true);
        cancelMap.put("fecha", FieldValue.serverTimestamp());
        cancelMap.put("motivo", motivo != null ? motivo : "");
        cancelMap.put("respuesta", "pendiente");

        Map<String, Object> updates = new HashMap<>();
        updates.put("estado", EstadoPedido.CANCELACION_SOLICITADA);
        updates.put("estadoPrevio", currentPedido.getEstado());
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
