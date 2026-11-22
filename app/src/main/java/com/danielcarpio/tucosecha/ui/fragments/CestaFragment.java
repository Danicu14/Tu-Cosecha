package com.danielcarpio.tucosecha.ui.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.PlanesActivity;
import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.adapter.CultivoListAdapter;
import com.danielcarpio.tucosecha.model.Cosecha;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.danielcarpio.tucosecha.model.Pedido;
import com.danielcarpio.tucosecha.model.ProductoCosecha;
import com.danielcarpio.tucosecha.util.GestorSesion;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CestaFragment extends Fragment {

    private RecyclerView rvCestaCultivos, rvThumbnails;
    private ProgressBar progressCestaLoading;
    private TextView tvCestaEmpty, tvCestaResumenVariedades, tvCestaResumenPlan, tvSubTitle;
    private MaterialButton btnCestaAction;

    // State views
    private View layoutDraftState;
    private View layoutPreparingState;
    private View layoutOrderDoneState;
    private View layoutClosedOrdersState;
    private View layoutBottomCestaBar;
    private TextView tvOrderDoneFechaFranja;
    private MaterialButton btnVerMisPedidos;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration cultivosListener;
    private ListenerRegistration harvestListener;
    private ListenerRegistration userListener;
    private ListenerRegistration pedidoListener;

    private List<Cultivo> allListoCultivos;
    private List<Cultivo> selectedCultivos;
    private List<ProductoCosecha> activeHarvestProducts;
    private CultivoListAdapter adapter;

    private String userPlan = "ninguno";
    private int maxVariedades = 4;
    private String planNombre = "Ninguno";
    private String activeHarvestId = null;
    private String activeHarvestSemana = null;
    private String activeHarvestLugar = null;
    private String activeHarvestFecha = null;
    private String activeHarvestHora = null;

    private Pedido confirmedPedido = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cesta, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvCestaCultivos = view.findViewById(R.id.rvCestaCultivos);
        progressCestaLoading = view.findViewById(R.id.progressCestaLoading);
        tvCestaEmpty = view.findViewById(R.id.tvCestaEmpty);
        tvCestaResumenVariedades = view.findViewById(R.id.tvCestaResumenVariedades);
        tvCestaResumenPlan = view.findViewById(R.id.tvCestaResumenPlan);
        btnCestaAction = view.findViewById(R.id.btnCestaAction);
        tvSubTitle = view.findViewById(R.id.tvCestaSubTitle);

        layoutDraftState = view.findViewById(R.id.layoutDraftState);
        layoutPreparingState = view.findViewById(R.id.layoutPreparingState);
        layoutOrderDoneState = view.findViewById(R.id.layoutOrderDoneState);
        layoutClosedOrdersState = view.findViewById(R.id.layoutClosedOrdersState);
        layoutBottomCestaBar = view.findViewById(R.id.layoutBottomCestaBar);
        tvOrderDoneFechaFranja = view.findViewById(R.id.tvOrderDoneFechaFranja);
        btnVerMisPedidos = view.findViewById(R.id.btnVerMisPedidos);
        rvThumbnails = view.findViewById(R.id.rvThumbnails);

        rvCestaCultivos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        allListoCultivos = new ArrayList<>();
        selectedCultivos = new ArrayList<>();
        activeHarvestProducts = new ArrayList<>();

        if (tvSubTitle != null) {
            tvSubTitle.setText("Toca un producto para añadirlo a tu lista");
        }

        if (GestorSesion.esInvitado()) {
            userPlan = "invitado";
            maxVariedades = 0;
            planNombre = "Invitado";
            setupBottomBar();
            loadHarvestAndData();
        } else {
            checkUserSubscriptionAndLoadData();
        }

        return view;
    }

    private void setupBottomBar() {
        if (userPlan.equalsIgnoreCase("ninguno") || userPlan.equalsIgnoreCase("invitado")) {
            tvCestaResumenVariedades.setText("Tu cesta: 0 variedades");
            tvCestaResumenPlan.setText("Elige un plan para crear tu cesta");
            btnCestaAction.setText("Ver planes");
            btnCestaAction.setOnClickListener(v -> startActivity(new Intent(getActivity(), PlanesActivity.class)));
        } else {
            updateResumenBar();
            tvCestaResumenPlan.setText("Incluido en tu plan " + planNombre);
            btnCestaAction.setText("Confirmar cesta");
            btnCestaAction.setOnClickListener(v -> {
                if (selectedCultivos.isEmpty()) {
                    Mensajes.mostrarInfo(btnCestaAction, "Selecciona al menos un producto para tu lista.");
                } else {
                    openTuListaBottomSheet();
                }
            });
        }
    }

    private void updateResumenBar() {
        tvCestaResumenVariedades.setText("Tu lista: " + selectedCultivos.size() + " de " + maxVariedades + " variedades");
    }

    private void openTuListaBottomSheet() {
        TuListaBottomSheet sheet = TuListaBottomSheet.newInstance(
                selectedCultivos,
                activeHarvestProducts,
                userPlan,
                maxVariedades,
                activeHarvestId,
                activeHarvestSemana,
                activeHarvestLugar,
                activeHarvestFecha,
                activeHarvestHora,
                () -> {
                    // Pedido confirmado listener
                    loadConfirmedOrder();
                },
                updatedSelected -> {
                    selectedCultivos = new ArrayList<>(updatedSelected);
                    saveDraftToPreferences();
                    updateCatalogAndThumbnails();
                }
        );
        sheet.show(getParentFragmentManager(), "TuListaBottomSheet");
    }

    private void checkUserSubscriptionAndLoadData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (userListener != null) userListener.remove();

            userListener = db.collection("usuarios").document(user.getUid())
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (!isAdded()) return;

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            userPlan = documentSnapshot.getString("plan");
                            if (userPlan == null) userPlan = "ninguno";

                            if (userPlan.equalsIgnoreCase("basico")) {
                                maxVariedades = 4;
                                planNombre = "Básico";
                            } else if (userPlan.equalsIgnoreCase("familiar")) {
                                maxVariedades = 6;
                                planNombre = "Familiar";
                            } else if (userPlan.equalsIgnoreCase("premium")) {
                                maxVariedades = 8;
                                planNombre = "Premium";
                            } else {
                                maxVariedades = 0;
                                planNombre = "Ninguno";
                            }

                            setupBottomBar();
                            loadHarvestAndData();
                        }
                    });
        } else {
            userPlan = "ninguno";
            maxVariedades = 0;
            planNombre = "Ninguno";
            setupBottomBar();
            loadHarvestAndData();
        }
    }

    private void loadHarvestAndData() {
        progressCestaLoading.setVisibility(View.VISIBLE);
        tvCestaEmpty.setVisibility(View.GONE);
        rvCestaCultivos.setVisibility(View.GONE);

        if (harvestListener != null) harvestListener.remove();

        harvestListener = db.collection("cosechas")
                .whereEqualTo("activa", true)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;

                    activeHarvestProducts.clear();
                    activeHarvestId = null;
                    activeHarvestSemana = null;
                    activeHarvestLugar = null;
                    activeHarvestFecha = null;
                    activeHarvestHora = null;

                    if (value != null && !value.isEmpty()) {
                        for (QueryDocumentSnapshot doc : value) {
                            Cosecha cosecha = doc.toObject(Cosecha.class);
                            cosecha.setId(doc.getId());
                            activeHarvestId = cosecha.getId();
                            activeHarvestSemana = cosecha.getSemana();
                            activeHarvestLugar = cosecha.getLugarRecogida();
                            activeHarvestFecha = cosecha.getFechaRecogida();
                            activeHarvestHora = cosecha.getHoraRecogida();

                            if (cosecha.getProductos() != null) {
                                activeHarvestProducts.addAll(cosecha.getProductos());
                            }
                            break;
                        }
                    }

                    if (getContext() != null) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String uid = user != null ? user.getUid() : "guest";
                        SharedPreferences prefs = getContext().getSharedPreferences("tucosecha_prefs", Context.MODE_PRIVATE);
                        String savedHarvestId = prefs.getString("last_harvest_id_" + uid, null);

                        if (activeHarvestId != null) {
                            if (savedHarvestId != null && !savedHarvestId.equals(activeHarvestId)) {
                                // Harvest changed! Discard old draft and empty list
                                prefs.edit().remove("draft_" + uid + "_" + savedHarvestId).apply();
                                selectedCultivos.clear();
                            }
                            prefs.edit().putString("last_harvest_id_" + uid, activeHarvestId).apply();
                        }
                    }

                    loadConfirmedOrder();
                });
    }

    private void loadConfirmedOrder() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && activeHarvestId != null) {
            String orderId = activeHarvestId + "_" + user.getUid();
            if (pedidoListener != null) pedidoListener.remove();

            pedidoListener = db.collection("pedidos").document(orderId)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (!isAdded()) return;

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            confirmedPedido = documentSnapshot.toObject(Pedido.class);
                        } else {
                            confirmedPedido = null;
                        }
                        updateCestaUIState();
                    });
        } else {
            confirmedPedido = null;
            updateCestaUIState();
        }
    }

    private void updateCestaUIState() {
        if (!isAdded()) return;
        progressCestaLoading.setVisibility(View.GONE);

        boolean isSubscriber = !userPlan.equalsIgnoreCase("ninguno") && !userPlan.equalsIgnoreCase("invitado");

        if (!isSubscriber) {
            layoutDraftState.setVisibility(View.VISIBLE);
            layoutPreparingState.setVisibility(View.GONE);
            layoutOrderDoneState.setVisibility(View.GONE);
            layoutClosedOrdersState.setVisibility(View.GONE);
            layoutBottomCestaBar.setVisibility(View.VISIBLE);
            setupBottomBar();
            loadCultivosCatalog();
            return;
        }

        int days = calculateDaysRemaining(activeHarvestFecha);
        boolean orderExists = (confirmedPedido != null);
        boolean isCancelled = orderExists && com.danielcarpio.tucosecha.util.EstadoPedido.CANCELADO.equalsIgnoreCase(confirmedPedido.getEstado());
        boolean hasActiveOrder = orderExists && !isCancelled;

        // 1. Sin cosecha activa, o con la fecha de entrega pasada y sin pedido:
        // "Estamos preparando la próxima cesta".
        if (activeHarvestId == null || (days < 0 && !hasActiveOrder)) {
            layoutPreparingState.setVisibility(View.VISIBLE);
            layoutOrderDoneState.setVisibility(View.GONE);
            layoutDraftState.setVisibility(View.GONE);
            layoutClosedOrdersState.setVisibility(View.GONE);
            layoutBottomCestaBar.setVisibility(View.GONE);
            return;
        }

        // 2. Con pedido de la cosecha activa cuyo estado NO sea "cancelado":
        // tarjeta corta con degradado verde "¡Pedido realizado!", la fecha y franja de entrega y el botón primario "Ver mis pedidos" (abre Mis pedidos). Debajo, un bloque "Tu próxima cesta": "Podrás crear otra cesta cuando el huerto publique productos nuevos. Te avisaremos." NO muestres aquí la lista de productos del pedido ni la barra "Tu lista".
        if (hasActiveOrder) {
            layoutPreparingState.setVisibility(View.GONE);
            layoutOrderDoneState.setVisibility(View.VISIBLE);
            layoutDraftState.setVisibility(View.GONE);
            layoutClosedOrdersState.setVisibility(View.GONE);
            layoutBottomCestaBar.setVisibility(View.GONE);

            String fechaStr = confirmedPedido.getFechaEntrega() != null ? confirmedPedido.getFechaEntrega().trim() : (activeHarvestFecha != null ? activeHarvestFecha.trim() : "-");
            String horaStr = (confirmedPedido.getHoraEntrega() != null && !confirmedPedido.getHoraEntrega().trim().isEmpty()) ? " (" + confirmedPedido.getHoraEntrega().trim() + ")" : "";
            tvOrderDoneFechaFranja.setText("Entrega: " + fechaStr + horaStr);

            btnVerMisPedidos.setOnClickListener(v -> startActivity(new Intent(getActivity(), com.danielcarpio.tucosecha.MisPedidosActivity.class)));
            return;
        }

        // 3. Sin pedido (o con pedido "cancelado") y faltando más de 2 días:
        // modo selección (catálogo, lista, hoja de confirmación) como ahora. Al confirmar tras una cancelación, se reutiliza el mismo documento {idCosecha}_{uid} con set().
        if (days > 2) {
            layoutPreparingState.setVisibility(View.GONE);
            layoutOrderDoneState.setVisibility(View.GONE);
            layoutDraftState.setVisibility(View.VISIBLE);
            layoutClosedOrdersState.setVisibility(View.GONE);
            layoutBottomCestaBar.setVisibility(View.VISIBLE);

            setupBottomBar();
            loadDraftFromPreferences();
            loadCultivosCatalog();
            return;
        }

        // 4. Sin pedido y faltando 2 días o menos:
        // "Los pedidos de esta semana están cerrados. La próxima cesta se abrirá cuando el huerto publique la nueva cosecha."
        layoutPreparingState.setVisibility(View.GONE);
        layoutOrderDoneState.setVisibility(View.GONE);
        layoutDraftState.setVisibility(View.GONE);
        layoutClosedOrdersState.setVisibility(View.VISIBLE);
        layoutBottomCestaBar.setVisibility(View.GONE);
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

    private void loadCultivosCatalog() {
        boolean selectionMode = !userPlan.equalsIgnoreCase("ninguno") && !userPlan.equalsIgnoreCase("invitado");

        adapter = new CultivoListAdapter(getContext(), userPlan, selectedCultivos.size() >= maxVariedades, cultivo -> {
            if (!selectionMode) {
                DetalleCultivoBottomSheet bottomSheet = DetalleCultivoBottomSheet.newInstance(cultivo);
                bottomSheet.show(getParentFragmentManager(), "DetalleCultivoBottomSheet");
                return;
            }

            boolean isScarce = "Ajo".equalsIgnoreCase(cultivo.getNombre()) || "Cebolla morada".equalsIgnoreCase(cultivo.getNombre());
            boolean isPremium = userPlan.contains("premium");

            if (isScarce && !isPremium) {
                Mensajes.mostrarInfo(rvCestaCultivos, "Este producto escaso está reservado para el plan Premium.");
                return;
            }

            if (selectedCultivos.size() >= maxVariedades) {
                Mensajes.mostrarInfo(rvCestaCultivos, "Ya tienes tus " + maxVariedades + " variedades. Quita alguna para cambiarla.");
                return;
            }

            selectedCultivos.add(cultivo);
            saveDraftToPreferences();
            updateCatalogAndThumbnails();
        });

        rvCestaCultivos.setAdapter(adapter);

        if (cultivosListener != null) cultivosListener.remove();

        cultivosListener = db.collection("cultivos")
                .whereEqualTo("fase", "listo")
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;

                    progressCestaLoading.setVisibility(View.GONE);

                    if (value != null) {
                        allListoCultivos.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Cultivo cultivo = doc.toObject(Cultivo.class);
                            if (cultivo.getId() == null || cultivo.getId().isEmpty()) {
                                cultivo.setId(doc.getId());
                            }

                            if (selectionMode) {
                                boolean hasQuantity = false;
                                for (ProductoCosecha p : activeHarvestProducts) {
                                    if (p.getNombre().equalsIgnoreCase(cultivo.getNombre())) {
                                        double cant = 0;
                                        if (userPlan.contains("basico")) cant = p.getCantidadBasico();
                                        else if (userPlan.contains("familiar")) cant = p.getCantidadFamiliar();
                                        else if (userPlan.contains("premium")) cant = p.getCantidadPremium();

                                        if (cant > 0) hasQuantity = true;
                                        break;
                                    }
                                }
                                if (hasQuantity) {
                                    allListoCultivos.add(cultivo);
                                }
                            } else {
                                allListoCultivos.add(cultivo);
                            }
                        }

                        updateCatalogAndThumbnails();
                    }
                });
    }

    private void updateCatalogAndThumbnails() {
        List<Cultivo> availableList = new ArrayList<>();
        List<String> selectedNames = new ArrayList<>();
        for (Cultivo c : selectedCultivos) {
            selectedNames.add(c.getNombre().toLowerCase());
        }

        for (Cultivo c : allListoCultivos) {
            if (!selectedNames.contains(c.getNombre().toLowerCase())) {
                availableList.add(c);
            }
        }

        boolean maxReached = selectedCultivos.size() >= maxVariedades;
        adapter = new CultivoListAdapter(getContext(), userPlan, maxReached, cultivo -> {
            boolean isScarce = "Ajo".equalsIgnoreCase(cultivo.getNombre()) || "Cebolla morada".equalsIgnoreCase(cultivo.getNombre());
            boolean isPremium = userPlan.contains("premium");

            if (isScarce && !isPremium) {
                Mensajes.mostrarInfo(rvCestaCultivos, "Este producto escaso está reservado para el plan Premium.");
                return;
            }

            if (selectedCultivos.size() >= maxVariedades) {
                Mensajes.mostrarInfo(rvCestaCultivos, "Ya tienes tus " + maxVariedades + " variedades. Quita alguna para cambiarla.");
                return;
            }

            selectedCultivos.add(cultivo);
            saveDraftToPreferences();
            updateCatalogAndThumbnails();
        });

        rvCestaCultivos.setAdapter(adapter);
        adapter.submitList(availableList);

        updateResumenBar();

        if (availableList.isEmpty() && selectedCultivos.isEmpty() && !allListoCultivos.isEmpty()) {
            tvCestaEmpty.setText("¡Has elegido todo lo disponible esta semana!");
            tvCestaEmpty.setVisibility(View.VISIBLE);
            rvCestaCultivos.setVisibility(View.GONE);
        } else if (allListoCultivos.isEmpty()) {
            tvCestaEmpty.setText("No hay productos listos para cosechar esta semana");
            tvCestaEmpty.setVisibility(View.VISIBLE);
            rvCestaCultivos.setVisibility(View.GONE);
        } else {
            tvCestaEmpty.setVisibility(View.GONE);
            rvCestaCultivos.setVisibility(View.VISIBLE);
        }

        updateThumbnailsBar();
    }

    private void updateThumbnailsBar() {
        if (rvThumbnails != null) {
            if (selectedCultivos.isEmpty()) {
                rvThumbnails.setVisibility(View.GONE);
            } else {
                rvThumbnails.setVisibility(View.VISIBLE);
                List<Cultivo> thumbList = selectedCultivos.size() > 4 ? selectedCultivos.subList(0, 4) : selectedCultivos;
                ThumbnailsAdapter thumbAdapter = new ThumbnailsAdapter(thumbList);
                rvThumbnails.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                rvThumbnails.setAdapter(thumbAdapter);
            }
        }
    }

    private String getPreferencesKey() {
        FirebaseUser user = mAuth.getCurrentUser();
        String uid = user != null ? user.getUid() : "guest";
        return "draft_" + uid + "_" + (activeHarvestId != null ? activeHarvestId : "harvest");
    }

    private void saveDraftToPreferences() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("tucosecha_prefs", Context.MODE_PRIVATE);
        JSONArray jsonArray = new JSONArray();
        for (Cultivo c : selectedCultivos) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", c.getId());
                obj.put("nombre", c.getNombre());
                obj.put("imagen", c.getImagen());
                obj.put("unidadVenta", c.getUnidadVenta());
                jsonArray.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putString(getPreferencesKey(), jsonArray.toString()).apply();
    }

    private void loadDraftFromPreferences() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("tucosecha_prefs", Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(getPreferencesKey(), null);
        selectedCultivos.clear();
        if (jsonStr != null) {
            try {
                JSONArray jsonArray = new JSONArray(jsonStr);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    Cultivo c = new Cultivo();
                    c.setId(obj.optString("id"));
                    c.setNombre(obj.optString("nombre"));
                    c.setImagen(obj.optString("imagen"));
                    c.setUnidadVenta(obj.optString("unidadVenta"));
                    selectedCultivos.add(c);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void clearDraftInPreferences() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("tucosecha_prefs", Context.MODE_PRIVATE);
        prefs.edit().remove(getPreferencesKey()).apply();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cultivosListener != null) {
            cultivosListener.remove();
            cultivosListener = null;
        }
        if (harvestListener != null) {
            harvestListener.remove();
            harvestListener = null;
        }
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        if (pedidoListener != null) {
            pedidoListener.remove();
            pedidoListener = null;
        }
    }



    private static class ThumbnailsAdapter extends RecyclerView.Adapter<ThumbnailsAdapter.ViewHolder> {
        private final List<Cultivo> thumbnails;

        public ThumbnailsAdapter(List<Cultivo> thumbnails) {
            this.thumbnails = thumbnails;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView img = new ImageView(parent.getContext());
            int size = (int) (40 * parent.getResources().getDisplayMetrics().density);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
            params.setMarginEnd(8 * (int) parent.getResources().getDisplayMetrics().density);
            img.setLayoutParams(params);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ViewHolder(img);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Cultivo c = thumbnails.get(position);
            int resId = holder.itemView.getContext().getResources().getIdentifier(c.getImagen(), "drawable", holder.itemView.getContext().getPackageName());
            if (resId == 0) resId = R.drawable.logo_tucosecha;
            ((ImageView) holder.itemView).setImageResource(resId);
        }

        @Override
        public int getItemCount() {
            return thumbnails.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
