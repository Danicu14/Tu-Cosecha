package com.danielcarpio.tucosecha.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Plan;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.List;

public class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.PlanViewHolder> {

    public interface OnPlanSelectListener {
        void onPlanSelect(Plan plan);
    }

    private final Context context;
    private final List<Plan> planList;
    private final OnPlanSelectListener listener;

    public PlanAdapter(Context context, List<Plan> planList, OnPlanSelectListener listener) {
        this.context = context;
        this.planList = planList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_plan, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        Plan plan = planList.get(position);

        holder.tvNombre.setText(plan.getNombre());
        holder.tvPrecio.setText(plan.getPrecioMensual() + " € / mes");
        holder.tvPersonas.setText("👥 " + plan.getPersonas());
        holder.tvProductos.setText("🧺 " + plan.getProductosPorCesta() + " productos por cesta");
        holder.tvDescripcion.setText(plan.getDescripcion());

        // Benefits
        holder.layoutBeneficios.removeAllViews();
        if (plan.getBeneficios() != null) {
            for (String beneficio : plan.getBeneficios()) {
                TextView tv = new TextView(context);
                tv.setText("✓ " + beneficio);
                tv.setTextSize(13f);
                tv.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                tv.setPadding(0, 2, 0, 2);
                holder.layoutBeneficios.addView(tv);
            }
        }

        // Popular badge for Familiar plan
        if ("familiar".equalsIgnoreCase(plan.getId())) {
            holder.chipPopular.setVisibility(View.VISIBLE);
        } else {
            holder.chipPopular.setVisibility(View.GONE);
        }

        holder.btnElegirPlan.setOnClickListener(v -> {
            if (listener != null) listener.onPlanSelect(plan);
        });
    }

    @Override
    public int getItemCount() {
        return planList.size();
    }

    public static class PlanViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvPrecio, tvPersonas, tvProductos, tvDescripcion;
        LinearLayout layoutBeneficios;
        Chip chipPopular;
        MaterialButton btnElegirPlan;
        MaterialCardView cardView;

        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            tvNombre = itemView.findViewById(R.id.tvPlanItemNombre);
            tvPrecio = itemView.findViewById(R.id.tvPlanItemPrecio);
            tvPersonas = itemView.findViewById(R.id.tvPlanItemPersonas);
            tvProductos = itemView.findViewById(R.id.tvPlanItemProductos);
            tvDescripcion = itemView.findViewById(R.id.tvPlanItemDescripcion);
            layoutBeneficios = itemView.findViewById(R.id.layoutPlanItemBeneficios);
            chipPopular = itemView.findViewById(R.id.chipPlanPopular);
            btnElegirPlan = itemView.findViewById(R.id.btnElegirPlan);
        }
    }
}
