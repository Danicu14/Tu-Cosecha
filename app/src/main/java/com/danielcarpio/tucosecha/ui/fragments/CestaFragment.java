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
import com.danielcarpio.tucosecha.util.Mensajes;
import com.danielcarpio.tucosecha.util.SessionManager;
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

    private RecyclerView rvCestaCultivos, rvConfirmedProducts, rvThumbnails;
    private ProgressBar progressCestaLoading;
    private TextView tvCestaEmpty, tvCestaResumenVariedades, tvCestaResumenPlan, tvSubTitle;
    private MaterialButton btnCestaAction;

    // Confirmed order UI views
    private LinearLayout layoutDraftState;
    private ScrollView layoutConfirmedState;
    private TextView tvConfirmedHeader, tvConfirmedDetails, tvConfirmedDays;
    private MaterialButton btnModificarPedido, btnCancelarPedido;
    private TextView tvClosedOrderTag;

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
        layoutConfirmedState = view.findViewById(R.id.layoutConfirmedState);
        tvConfirmedHeader = view.findViewById(R.id.tvConfirmedHeader);
        tvConfirmedDetails = view.findViewById(R.id.tvConfirmedDetails);
        tvConfirmedDays = view.findViewById(R.id.tvConfirmedDays);
        btnModificarPedido = view.findViewById(R.id.btnModificarPedido);
        btnCancelarPedido = view.findViewById(R.id.btnCancelarPedido);
        tvClosedOrderTag = view.findViewById(R.id.tvClosedOrderTag);
        rvConfirmedProducts = view.findViewById(R.id.rvConfirmedProducts);
        rvThumbnails = view.findViewById(R.id.rvThumbnails);

        rvCestaCultivos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        allListoCultivos = new ArrayList<>();
        selectedCultivos = new ArrayList<>();
        activeHarvestProducts = new ArrayList<>();

        if (tvSubTitle != null) {
            tvSubTitle.setText("Toca un producto para añadirlo a tu lista");
        }

        if (SessionManager.isEsInvitado()) {
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
                            showConfirmedStateUI();
                        } else {
                            confirmedPedido = null;
                            showDraftStateUI();
                            loadDraftFromPreferences();
                            loadCultivosCatalog();
                        }
                    });
        } else {
            showDraftStateUI();
            loadDraftFromPreferences();
            loadCultivosCatalog();
        }
    }

    private void showDraftStateUI() {
        if (layoutDraftState != null) layoutDraftState.setVisibility(View.VISIBLE);
        if (layoutConfirmedState != null) layoutConfirmedState.setVisibility(View.GONE);
    }

    private void showConfirmedStateUI() {
        if (layoutDraftState != null) layoutDraftState.setVisibility(View.GONE);
        if (layoutConfirmedState != null) layoutConfirmedState.setVisibility(View.VISIBLE);
        progressCestaLoading.setVisibility(View.GONE);

        if (confirmedPedido == null) return;

        tvConfirmedHeader.setText("Tu cesta está confirmada");
        if ("domicilio".equalsIgnoreCase(confirmedPedido.getMetodoEntrega()) && confirmedPedido.getDireccion() != null) {
            Map<String, Object> dir = confirmedPedido.getDireccion();
            String calle = dir.get("calle") != null ? String.valueOf(dir.get("calle")) : "";
            String loc = dir.get("localidad") != null ? String.valueOf(dir.get("localidad")) : "Talavera de la Reina";
            tvConfirmedDetails.setText("Te la mandamos a: " + calle + ", " + loc + "\nFecha: " + confirmedPedido.getFechaEntrega() + " (" + confirmedPedido.getHoraEntrega() + ")");
        } else {
            tvConfirmedDetails.setText("Recógela en: " + (confirmedPedido.getLugarRecogida() != null ? confirmedPedido.getLugarRecogida() : "Huerto Tú Cosecha") + "\nFecha: " + confirmedPedido.getFechaEntrega() + " (" + confirmedPedido.getHoraEntrega() + ")");
        }

        // Calculate days remaining
        int daysRemaining = calculateDaysRemaining(confirmedPedido.getFechaEntrega());
        if (daysRemaining >= 0) {
            tvConfirmedDays.setText("Faltan " + daysRemaining + " días");
        } else {
            tvConfirmedDays.setText("Fecha de entrega alcanzada");
        }

        if (daysRemaining > 2) {
            btnModificarPedido.setVisibility(View.VISIBLE);
            btnCancelarPedido.setVisibility(View.VISIBLE);
            tvClosedOrderTag.setVisibility(View.GONE);

            btnModificarPedido.setOnClickListener(v -> modificarPedido());
            btnCancelarPedido.setOnClickListener(v -> cancelarPedidoDialog());
        } else {
            btnModificarPedido.setVisibility(View.GONE);
            btnCancelarPedido.setVisibility(View.GONE);
            tvClosedOrderTag.setVisibility(View.VISIBLE);
        }

        // Products list in confirmed order
        if (confirmedPedido.getProductos() != null) {
            ConfirmedProductsAdapter pAdapter = new ConfirmedProductsAdapter(confirmedPedido.getProductos());
            rvConfirmedProducts.setLayoutManager(new LinearLayoutManager(getContext()));
            rvConfirmedProducts.setAdapter(pAdapter);
        }
    }

    private int calculateDaysRemaining(String fechaStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date deliveryDate = sdf.parse(fechaStr);
            if (deliveryDate != null) {
                long diffMillis = deliveryDate.getTime() - System.currentTimeMillis();
                long days = diffMillis / (1000 * 60 * 60 * 24);
                return (int) Math.max(0, days);
            }
        } catch (Exception e) {
            // ignore
        }
        return 3; // default > 2
    }

    private void modificarPedido() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || activeHarvestId == null) return;
        String orderId = activeHarvestId + "_" + user.getUid();

        // Restore selected cultivos from confirmed order products
        selectedCultivos.clear();
        if (confirmedPedido.getProductos() != null) {
            for (Map<String, Object> pMap : confirmedPedido.getProductos()) {
                Cultivo c = new Cultivo();
                c.setNombre(String.valueOf(pMap.get("nombre")));
                c.setImagen(pMap.get("imagen") != null ? String.valueOf(pMap.get("imagen")) : "logo_tucosecha");
                c.setUnidadVenta(pMap.get("unidad") != null ? String.valueOf(pMap.get("unidad")) : "kg");
                selectedCultivos.add(c);
            }
        }
        saveDraftToPreferences();

        // Delete order in Firestore so user returns to draft state
        db.collection("pedidos").document(orderId).delete()
                .addOnSuccessListener(aVoid -> {
                    Mensajes.mostrarInfo(btnModificarPedido, "Pedido abierto para modificación.");
                    loadConfirmedOrder();
                });
    }

    private void cancelarPedidoDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancelar pedido")
                .setMessage("¿Estás seguro de que deseas cancelar este pedido?")
                .setPositiveButton("Sí, cancelar", (dialog, which) -> ejecutarCancelacionPedido())
                .setNegativeButton("No", null)
                .show();
    }

    private void ejecutarCancelacionPedido() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || activeHarvestId == null) return;
        String orderId = activeHarvestId + "_" + user.getUid();

        db.collection("pedidos").document(orderId).delete()
                .addOnSuccessListener(aVoid -> {
                    clearDraftInPreferences();
                    selectedCultivos.clear();
                    Mensajes.mostrarExito(btnCancelarPedido, "Pedido cancelado correctamente.");
                    loadConfirmedOrder();
                })
                .addOnFailureListener(e -> Mensajes.mostrarError(btnCancelarPedido, "Error al cancelar: " + e.getMessage()));
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

    private static class ConfirmedProductsAdapter extends RecyclerView.Adapter<ConfirmedProductsAdapter.ViewHolder> {
        private final List<Map<String, Object>> productos;

        public ConfirmedProductsAdapter(List<Map<String, Object>> productos) {
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
            Object cant = p.get("cantidad");
            Object unidad = p.get("unidad");
            holder.tvQuantity.setText((cant != null ? cant.toString() : "1") + " " + (unidad != null ? unidad.toString() : "kg"));

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
