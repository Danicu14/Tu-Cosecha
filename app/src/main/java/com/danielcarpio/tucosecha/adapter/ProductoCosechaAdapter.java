package com.danielcarpio.tucosecha.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.ProductoCosecha;
import com.google.android.material.chip.Chip;

import java.util.List;

public class ProductoCosechaAdapter extends RecyclerView.Adapter<ProductoCosechaAdapter.ProductoViewHolder> {

    private final Context context;
    private final List<ProductoCosecha> productoList;
    private final String userPlan; // "basico", "familiar", "premium", or "ninguno"/"invitado"

    public ProductoCosechaAdapter(Context context, List<ProductoCosecha> productoList, String userPlan) {
        this.context = context;
        this.productoList = productoList;
        this.userPlan = userPlan != null ? userPlan.toLowerCase() : "ninguno";
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_producto_cosecha, parent, false);
        return new ProductoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        ProductoCosecha producto = productoList.get(position);

        holder.tvNombre.setText(producto.getNombre());

        double cantidad = 0;
        if (userPlan.contains("basico")) {
            cantidad = producto.getCantidadBasico();
        } else if (userPlan.contains("familiar")) {
            cantidad = producto.getCantidadFamiliar();
        } else if (userPlan.contains("premium")) {
            cantidad = producto.getCantidadPremium();
        } else {
            cantidad = Math.max(producto.getCantidadBasico(), Math.max(producto.getCantidadFamiliar(), producto.getCantidadPremium()));
        }

        if (userPlan.equals("ninguno") || userPlan.equals("invitado")) {
            holder.tvCantidad.setText(producto.getUnidad());
        } else {
            holder.tvCantidad.setText(cantidad + " " + producto.getUnidad());
        }

        if (producto.isEscaso()) {
            holder.chipEscaso.setVisibility(View.VISIBLE);
        } else {
            holder.chipEscaso.setVisibility(View.GONE);
        }

        int resId = context.getResources().getIdentifier(producto.getImagen(), "drawable", context.getPackageName());
        if (resId == 0) {
            resId = R.drawable.logo_tucosecha;
        }
        holder.imgProducto.setImageResource(resId);
    }

    @Override
    public int getItemCount() {
        return productoList.size();
    }

    public static class ProductoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProducto;
        TextView tvNombre, tvCantidad;
        Chip chipEscaso;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProducto = itemView.findViewById(R.id.imgProductoCosecha);
            tvNombre = itemView.findViewById(R.id.tvNombreProductoCosecha);
            tvCantidad = itemView.findViewById(R.id.tvCantidadProductoCosecha);
            chipEscaso = itemView.findViewById(R.id.chipEscaso);
        }
    }
}
