package com.danielcarpio.tucosecha.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Pedido;
import com.danielcarpio.tucosecha.util.EstadoPedido;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class PedidoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<Object> displayList = new ArrayList<>();
    private final OnPedidoClickListener listener;

    public interface OnPedidoClickListener {
        void onPedidoClick(Pedido pedido);
    }

    public PedidoAdapter(OnPedidoClickListener listener) {
        this.listener = listener;
    }

    public void setPedidos(List<Pedido> pedidos) {
        displayList.clear();
        List<Pedido> enCurso = new ArrayList<>();
        List<Pedido> anteriores = new ArrayList<>();

        for (Pedido p : pedidos) {
            String est = p.getEstado() != null ? p.getEstado().toLowerCase() : EstadoPedido.CONFIRMADO;
            if (est.equals(EstadoPedido.ENTREGADO) || est.equals(EstadoPedido.CANCELADO)) {
                anteriores.add(p);
            } else {
                enCurso.add(p);
            }
        }

        if (!enCurso.isEmpty()) {
            displayList.add("En curso");
            displayList.addAll(enCurso);
        }

        if (!anteriores.isEmpty()) {
            displayList.add("Anteriores");
            displayList.addAll(anteriores);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (displayList.get(position) instanceof String) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_pedido, parent, false);
            return new PedidoViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            String title = (String) displayList.get(position);
            ((HeaderViewHolder) holder).tvHeader.setText(title);
            ((HeaderViewHolder) holder).tvHeader.setTextSize(18f);
            ((HeaderViewHolder) holder).tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            ((HeaderViewHolder) holder).tvHeader.setTextColor(Color.parseColor("#1F5A2A"));
            ((HeaderViewHolder) holder).tvHeader.setPadding(0, position == 0 ? 8 : 24, 0, 12);
        } else if (holder instanceof PedidoViewHolder) {
            Pedido pedido = (Pedido) displayList.get(position);
            PedidoViewHolder vh = (PedidoViewHolder) holder;

            vh.tvSemana.setText(pedido.getSemana() != null ? pedido.getSemana() : "Semana de Cosecha");
            vh.tvFecha.setText("Entrega: " + (pedido.getFechaEntrega() != null ? pedido.getFechaEntrega() : "-") + " (" + (pedido.getHoraEntrega() != null ? pedido.getHoraEntrega() : "") + ")");
            
            String metodo = "domicilio".equalsIgnoreCase(pedido.getMetodoEntrega()) ? "Envío a domicilio" : "Recogida en huerto";
            vh.tvMetodo.setText("Método: " + metodo);

            int count = pedido.getProductos() != null ? pedido.getProductos().size() : 0;
            vh.tvProductosCount.setText(count + (count == 1 ? " producto" : " productos"));

            // Status chip and colors
            String estado = pedido.getEstado() != null ? pedido.getEstado().toLowerCase() : EstadoPedido.CONFIRMADO;
            setupStatusChip(vh.chipEstado, estado);

            vh.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onPedidoClick(pedido);
            });
        }
    }

    private void setupStatusChip(Chip chip, String estado) {
        String text;
        int bgColor;
        int textColor;

        switch (estado) {
            case EstadoPedido.PREPARANDO:
                text = "Preparando";
                bgColor = Color.parseColor("#FFF8E1"); // ámbar
                textColor = Color.parseColor("#FF8F00");
                break;
            case EstadoPedido.EN_CAMINO:
                text = "En camino";
                bgColor = Color.parseColor("#E3F2FD"); // azul
                textColor = Color.parseColor("#1565C0");
                break;
            case EstadoPedido.LISTO_PARA_RECOGER:
                text = "Listo para recoger";
                bgColor = Color.parseColor("#E3F2FD"); // azul
                textColor = Color.parseColor("#1565C0");
                break;
            case EstadoPedido.ENTREGADO:
                text = "Entregado";
                bgColor = Color.parseColor("#CFD8DC"); // gris verdoso
                textColor = Color.parseColor("#2E3E34");
                break;
            case EstadoPedido.CANCELACION_SOLICITADA:
                text = "Cancelación solicitada";
                bgColor = Color.parseColor("#FFE0B2"); // naranja
                textColor = Color.parseColor("#E65100");
                break;
            case EstadoPedido.CANCELADO:
                text = "Cancelado";
                bgColor = Color.parseColor("#FFCDD2"); // rojo
                textColor = Color.parseColor("#C62828");
                break;
            case EstadoPedido.CONFIRMADO:
            default:
                text = "Confirmado";
                bgColor = Color.parseColor("#C8E6C9"); // verde
                textColor = Color.parseColor("#1F5A2A");
                break;
        }

        chip.setText(text);
        chip.setChipBackgroundColor(ColorStateList.valueOf(bgColor));
        chip.setTextColor(textColor);
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = (TextView) itemView;
        }
    }

    static class PedidoViewHolder extends RecyclerView.ViewHolder {
        TextView tvSemana, tvFecha, tvMetodo, tvProductosCount;
        Chip chipEstado;

        public PedidoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSemana = itemView.findViewById(R.id.tvItemPedidoSemana);
            tvFecha = itemView.findViewById(R.id.tvItemPedidoFecha);
            tvMetodo = itemView.findViewById(R.id.tvItemPedidoMetodo);
            tvProductosCount = itemView.findViewById(R.id.tvItemPedidoProductosCount);
            chipEstado = itemView.findViewById(R.id.chipItemPedidoEstado);
        }
    }
}
