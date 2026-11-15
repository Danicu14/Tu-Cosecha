package com.danielcarpio.tucosecha.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.danielcarpio.tucosecha.model.Pedido;
import com.danielcarpio.tucosecha.model.ProductoCosecha;
import com.danielcarpio.tucosecha.model.Usuario;
import com.danielcarpio.tucosecha.util.Formato;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TuListaBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_HARVEST_ID = "harvest_id";
    private static final String ARG_SEMANA = "semana";
    private static final String ARG_FECHA = "fecha";
    private static final String ARG_HORA = "hora";
    private static final String ARG_LUGAR = "lugar";
    private static final String ARG_PLAN = "plan";
    private static final String ARG_MAX_VAR = "max_var";

    private String harvestId, semana, fechaRecogida, horaRecogida, lugarRecogida, plan;
    private int maxVariedades;
    private static List<Cultivo> selectedCultivos;
    private static List<ProductoCosecha> activeHarvestProducts;
    private static Runnable onOrderConfirmed;
    private static Consumer<List<Cultivo>> onListUpdated;

    private LinearLayout layoutStep1, layoutStep2, layoutWarning, layoutDomicilio, layoutRecogida;
    private TextView tvStep1Counter;
    private RecyclerView rvTuLista;
    private MaterialButton btnStep1Continuar, btnStep2Volver, btnConfirmarPedido, btnSeguirEligiendo;
    private MaterialButtonToggleGroup toggleGroupEntrega;

    private TextView tvDomicilioDireccion, tvDomicilioFechaHora;
    private MaterialButton btnCambiarDireccion;
    private TextView tvRecogidaLugar, tvRecogidaFechaHora;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private DraftAdapter draftAdapter;
    private Map<String, Object> userDireccion;
    private String userName = "Cliente Tu Cosecha";

    public static TuListaBottomSheet newInstance(List<Cultivo> seleccionados, List<ProductoCosecha> harvestProducts, String plan, int maxVariedades, String harvestId, String semana, String lugarRecogida, String fechaRecogida, String horaRecogida, Runnable confirmCallback, Consumer<List<Cultivo>> updateCallback) {
        TuListaBottomSheet fragment = new TuListaBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_HARVEST_ID, harvestId);
        args.putString(ARG_SEMANA, semana);
        args.putString(ARG_FECHA, fechaRecogida);
        args.putString(ARG_HORA, horaRecogida);
        args.putString(ARG_LUGAR, lugarRecogida);
        args.putString(ARG_PLAN, plan);
        args.putInt(ARG_MAX_VAR, maxVariedades);
        fragment.setArguments(args);
        selectedCultivos = seleccionados;
        activeHarvestProducts = harvestProducts;
        onOrderConfirmed = confirmCallback;
        onListUpdated = updateCallback;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_tu_lista, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            harvestId = getArguments().getString(ARG_HARVEST_ID);
            semana = getArguments().getString(ARG_SEMANA);
            fechaRecogida = getArguments().getString(ARG_FECHA);
            horaRecogida = getArguments().getString(ARG_HORA);
            lugarRecogida = getArguments().getString(ARG_LUGAR);
            plan = getArguments().getString(ARG_PLAN);
            maxVariedades = getArguments().getInt(ARG_MAX_VAR);
        }

        layoutStep1 = view.findViewById(R.id.layoutStep1TuLista);
        layoutStep2 = view.findViewById(R.id.layoutStep2Metodo);
        layoutWarning = view.findViewById(R.id.layoutVariedadesWarning);
        layoutDomicilio = view.findViewById(R.id.layoutDomicilioBlock);
        layoutRecogida = view.findViewById(R.id.layoutRecogidaBlock);

        tvStep1Counter = view.findViewById(R.id.tvStep1Counter);
        rvTuLista = view.findViewById(R.id.rvTuLista);
        btnStep1Continuar = view.findViewById(R.id.btnStep1Continuar);
        btnStep2Volver = view.findViewById(R.id.btnStep2Volver);
        btnConfirmarPedido = view.findViewById(R.id.btnConfirmarPedido);
        btnSeguirEligiendo = view.findViewById(R.id.btnSeguirEligiendo);
        toggleGroupEntrega = view.findViewById(R.id.toggleGroupEntrega);

        tvDomicilioDireccion = view.findViewById(R.id.tvDomicilioDireccion);
        tvDomicilioFechaHora = view.findViewById(R.id.tvDomicilioFechaHora);
        btnCambiarDireccion = view.findViewById(R.id.btnCambiarDireccion);

        tvRecogidaLugar = view.findViewById(R.id.tvRecogidaLugar);
        tvRecogidaFechaHora = view.findViewById(R.id.tvRecogidaFechaHora);

        rvTuLista.setLayoutManager(new LinearLayoutManager(getContext()));
        setupDraftAdapter();
        updateStep1UI();

        btnStep1Continuar.setOnClickListener(v -> {
            int missing = maxVariedades - selectedCultivos.size();
            if (missing > 0) {
                String msg = missing == 1 ? "Te falta 1 variedad por elegir" : "Te faltan " + missing + " variedades por elegir";
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Variedades incompletas")
                        .setMessage(msg)
                        .setPositiveButton("Confirmar así", (dialog, which) -> {
                            layoutStep1.setVisibility(View.GONE);
                            layoutStep2.setVisibility(View.VISIBLE);
                            loadUserDataAndAddress();
                        })
                        .setNegativeButton("Seguir eligiendo", null)
                        .show();
            } else {
                layoutStep1.setVisibility(View.GONE);
                layoutStep2.setVisibility(View.VISIBLE);
                loadUserDataAndAddress();
            }
        });

        if (btnSeguirEligiendo != null) {
            btnSeguirEligiendo.setOnClickListener(v -> dismiss());
        }

        btnStep2Volver.setOnClickListener(v -> {
            layoutStep2.setVisibility(View.GONE);
            layoutStep1.setVisibility(View.VISIBLE);
        });

        toggleGroupEntrega.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnToggleDomicilio) {
                    layoutDomicilio.setVisibility(View.VISIBLE);
                    layoutRecogida.setVisibility(View.GONE);
                } else if (checkedId == R.id.btnToggleRecogida) {
                    layoutDomicilio.setVisibility(View.GONE);
                    layoutRecogida.setVisibility(View.VISIBLE);
                }
            }
        });
        toggleGroupEntrega.check(R.id.btnToggleDomicilio);

        btnCambiarDireccion.setOnClickListener(v -> {
            DireccionBottomSheet sheet = DireccionBottomSheet.newInstance(userDireccion, direccion -> {
                userDireccion = direccion;
                updateDomicilioUI();
            });
            sheet.show(getParentFragmentManager(), "DireccionBottomSheet");
        });

        btnConfirmarPedido.setOnClickListener(v -> confirmarPedido());

        return view;
    }

    private void setupDraftAdapter() {
        draftAdapter = new DraftAdapter();
        rvTuLista.setAdapter(draftAdapter);
    }

    private void updateStep1UI() {
        if (selectedCultivos == null) selectedCultivos = new ArrayList<>();
        tvStep1Counter.setText("Tu lista: " + selectedCultivos.size() + " de " + maxVariedades + " variedades");

        if (selectedCultivos.size() < maxVariedades) {
            layoutWarning.setVisibility(View.VISIBLE);
        } else {
            layoutWarning.setVisibility(View.GONE);
        }

        if (draftAdapter != null) {
            draftAdapter.notifyDataSetChanged();
        }
    }

    private void loadUserDataAndAddress() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("usuarios").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Usuario u = documentSnapshot.toObject(Usuario.class);
                            if (u != null) {
                                userName = (u.getNombre() != null ? u.getNombre() : "") + " " + (u.getApellidos() != null ? u.getApellidos() : "");
                                userDireccion = u.getDireccion();
                                updateDomicilioUI();
                            }
                        }
                    });
        }
    }

    private void updateDomicilioUI() {
        if (userDireccion != null && userDireccion.get("calle") != null && !String.valueOf(userDireccion.get("calle")).isEmpty()) {
            String calle = String.valueOf(userDireccion.get("calle"));
            String piso = userDireccion.get("piso") != null && !String.valueOf(userDireccion.get("piso")).isEmpty() ? ", " + userDireccion.get("piso") : "";
            String cp = userDireccion.get("codigoPostal") != null ? " (" + userDireccion.get("codigoPostal") + ")" : "";
            String tel = userDireccion.get("telefono") != null ? " - Tel: " + userDireccion.get("telefono") : "";
            tvDomicilioDireccion.setText("Dirección: " + calle + piso + ", Talavera de la Reina" + cp + tel);
            btnCambiarDireccion.setText("Cambiar");
        } else {
            tvDomicilioDireccion.setText("Añade tu dirección de entrega");
            btnCambiarDireccion.setText("Añadir dirección");
        }
        tvDomicilioFechaHora.setText("Entrega: " + (fechaRecogida != null ? fechaRecogida : "-") + " (" + (horaRecogida != null ? horaRecogida : "-") + ")");

        tvRecogidaLugar.setText("Lugar de recogida: " + (lugarRecogida != null ? lugarRecogida : "Huerto Tu Cosecha, Talavera de la Reina"));
        tvRecogidaFechaHora.setText("Fecha y hora: " + (fechaRecogida != null ? fechaRecogida : "-") + " (" + (horaRecogida != null ? horaRecogida : "-") + ")");
    }

    private void confirmarPedido() {
        int checkedId = toggleGroupEntrega.getCheckedButtonId();
        boolean esDomicilio = (checkedId == R.id.btnToggleDomicilio);

        if (esDomicilio && (userDireccion == null || userDireccion.get("calle") == null || String.valueOf(userDireccion.get("calle")).isEmpty())) {
            Mensajes.mostrarInfo(btnConfirmarPedido, "Por favor, añade tu dirección de entrega.");
            DireccionBottomSheet sheet = DireccionBottomSheet.newInstance(userDireccion, direccion -> {
                userDireccion = direccion;
                updateDomicilioUI();
            });
            sheet.show(getParentFragmentManager(), "DireccionBottomSheet");
            return;
        }

        ejecutarGuardadoPedido(esDomicilio);
    }

    private void ejecutarGuardadoPedido(boolean esDomicilio) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        List<Map<String, Object>> productosMapList = new ArrayList<>();
        for (Cultivo c : selectedCultivos) {
            Map<String, Object> pMap = new HashMap<>();
            pMap.put("nombre", c.getNombre());
            pMap.put("imagen", c.getImagen());
            double cant = 1.0;
            if (activeHarvestProducts != null) {
                for (ProductoCosecha ap : activeHarvestProducts) {
                    if (ap.getNombre().equalsIgnoreCase(c.getNombre())) {
                        if (plan.contains("basico")) cant = ap.getCantidadBasico();
                        else if (plan.contains("familiar")) cant = ap.getCantidadFamiliar();
                        else if (plan.contains("premium")) cant = ap.getCantidadPremium();
                        break;
                    }
                }
            }
            pMap.put("cantidad", cant);
            pMap.put("unidad", c.getUnidadVenta() != null ? c.getUnidadVenta() : "ud");
            productosMapList.add(pMap);
        }

        Pedido pedido = new Pedido();
        pedido.setUid(user.getUid());
        pedido.setNombreUsuario(userName);
        pedido.setIdCosecha(harvestId);
        pedido.setSemana(semana);
        pedido.setPlan(plan);
        pedido.setProductos(productosMapList);
        pedido.setMetodoEntrega(esDomicilio ? "domicilio" : "recogida");
        if (esDomicilio) {
            pedido.setDireccion(userDireccion);
            pedido.setLugarRecogida(null);
        } else {
            pedido.setDireccion(null);
            pedido.setLugarRecogida(lugarRecogida);
        }
        pedido.setFechaEntrega(fechaRecogida);
        pedido.setHoraEntrega(horaRecogida);
        pedido.setEstado("confirmado");
        pedido.setFechaPedido(FieldValue.serverTimestamp());

        String pedidoDocId = (harvestId != null ? harvestId : "cosecha") + "_" + user.getUid();

        db.collection("pedidos").document(pedidoDocId)
                .set(pedido)
                .addOnSuccessListener(aVoid -> {
                    Mensajes.mostrarExito(btnConfirmarPedido, "¡Pedido confirmado!");
                    if (onOrderConfirmed != null) {
                        onOrderConfirmed.run();
                    }
                    dismiss();
                })
                .addOnFailureListener(e -> Mensajes.mostrarError(btnConfirmarPedido, "Error al confirmar pedido: " + e.getMessage()));
    }

    private class DraftAdapter extends RecyclerView.Adapter<DraftAdapter.DraftViewHolder> {

        @NonNull
        @Override
        public DraftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cesta_draft, parent, false);
            return new DraftViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DraftViewHolder holder, int position) {
            Cultivo cultivo = selectedCultivos.get(position);
            holder.tvName.setText(cultivo.getNombre());

            double cant = 1.0;
            if (activeHarvestProducts != null) {
                for (ProductoCosecha ap : activeHarvestProducts) {
                    if (ap.getNombre().equalsIgnoreCase(cultivo.getNombre())) {
                        if (plan.contains("basico")) cant = ap.getCantidadBasico();
                        else if (plan.contains("familiar")) cant = ap.getCantidadFamiliar();
                        else if (plan.contains("premium")) cant = ap.getCantidadPremium();
                        break;
                    }
                }
            }
            String qtyStr = Formato.cantidad(cant, cultivo.getUnidadVenta());
            holder.tvQuantity.setText(qtyStr);

            int resId = holder.itemView.getContext().getResources().getIdentifier(cultivo.getImagen(), "drawable", holder.itemView.getContext().getPackageName());
            if (resId == 0) resId = R.drawable.logo_tucosecha;
            holder.imgPhoto.setImageResource(resId);

            holder.btnRemove.setOnClickListener(v -> {
                Cultivo removed = selectedCultivos.get(position);
                selectedCultivos.remove(removed);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, selectedCultivos.size());
                updateStep1UI();
                if (onListUpdated != null) {
                    onListUpdated.accept(selectedCultivos);
                }
            });
        }

        @Override
        public int getItemCount() {
            return selectedCultivos != null ? selectedCultivos.size() : 0;
        }

        public class DraftViewHolder extends RecyclerView.ViewHolder {
            ImageView imgPhoto;
            TextView tvName, tvQuantity;
            MaterialButton btnRemove;

            public DraftViewHolder(@NonNull View itemView) {
                super(itemView);
                imgPhoto = itemView.findViewById(R.id.imgDraftPhoto);
                tvName = itemView.findViewById(R.id.tvDraftName);
                tvQuantity = itemView.findViewById(R.id.tvDraftQuantity);
                btnRemove = itemView.findViewById(R.id.btnRemoveDraftItem);
            }
        }
    }
}
