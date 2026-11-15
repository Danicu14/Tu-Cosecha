package com.danielcarpio.tucosecha;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.danielcarpio.tucosecha.ui.fragments.CestaFragment;
import com.danielcarpio.tucosecha.ui.fragments.CuentaFragment;
import com.danielcarpio.tucosecha.ui.fragments.HuertoFragment;
import com.danielcarpio.tucosecha.ui.fragments.NoticiasFragment;
import com.danielcarpio.tucosecha.util.EstadoPedido;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.danielcarpio.tucosecha.util.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_TAB = "selected_tab";
    private int currentPosition = 0;

    private View tabCesta, tabProductos, tabNoticias, tabZona;
    private ImageView imgCesta, imgProductos, imgNoticias, imgZona;
    private TextView tvCesta, tvProductos, tvNoticias, tvZona;
    private View lineCesta, lineProductos, lineNoticias, lineZona;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabCesta = findViewById(R.id.navCesta);
        tabProductos = findViewById(R.id.navProductos);
        tabNoticias = findViewById(R.id.navNoticias);
        tabZona = findViewById(R.id.navZona);

        imgCesta = findViewById(R.id.imgCesta);
        imgProductos = findViewById(R.id.imgProductos);
        imgNoticias = findViewById(R.id.imgNoticias);
        imgZona = findViewById(R.id.imgZona);

        tvCesta = findViewById(R.id.tvCesta);
        tvProductos = findViewById(R.id.tvProductos);
        tvNoticias = findViewById(R.id.tvNoticias);
        tvZona = findViewById(R.id.tvZona);

        lineCesta = findViewById(R.id.lineCesta);
        lineProductos = findViewById(R.id.lineProductos);
        lineNoticias = findViewById(R.id.lineNoticias);
        lineZona = findViewById(R.id.lineZona);

        tabCesta.setOnClickListener(v -> seleccionar(0));
        tabProductos.setOnClickListener(v -> seleccionar(1));
        tabNoticias.setOnClickListener(v -> seleccionar(2));
        tabZona.setOnClickListener(v -> seleccionar(3));

        if (savedInstanceState != null) {
            currentPosition = savedInstanceState.getInt(KEY_SELECTED_TAB, 0);
            seleccionar(currentPosition);
        } else {
            seleccionar(0);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private ListenerRegistration ordersNotificationListener;
    private final Map<String, String> lastOrderStates = new HashMap<>();
    private final Map<String, String> lastOrderCancelRespuesta = new HashMap<>();
    private boolean isOrdersFirstLoad = true;

    @Override
    protected void onStart() {
        super.onStart();
        startOrdersNotificationListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (ordersNotificationListener != null) {
            ordersNotificationListener.remove();
            ordersNotificationListener = null;
        }
    }

    private void startOrdersNotificationListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || SessionManager.isEsInvitado()) return;

        if (ordersNotificationListener != null) ordersNotificationListener.remove();
        isOrdersFirstLoad = true;
        lastOrderStates.clear();
        lastOrderCancelRespuesta.clear();

        ordersNotificationListener = FirebaseFirestore.getInstance()
                .collection("pedidos")
                .whereEqualTo("uid", user.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    if (isOrdersFirstLoad) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            lastOrderStates.put(doc.getId(), doc.getString("estado"));
                            Map<String, Object> cancel = (Map<String, Object>) doc.get("cancelacion");
                            if (cancel != null && cancel.get("respuesta") != null) {
                                lastOrderCancelRespuesta.put(doc.getId(), String.valueOf(cancel.get("respuesta")));
                            }
                        }
                        isOrdersFirstLoad = false;
                        return;
                    }

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            DocumentSnapshot doc = dc.getDocument();
                            String docId = doc.getId();
                            String newEstado = doc.getString("estado");
                            String oldEstado = lastOrderStates.get(docId);

                            Map<String, Object> cancel = (Map<String, Object>) doc.get("cancelacion");
                            String newRespuesta = (cancel != null && cancel.get("respuesta") != null) ? String.valueOf(cancel.get("respuesta")) : null;
                            String oldRespuesta = lastOrderCancelRespuesta.get(docId);

                            View anchor = findViewById(R.id.main);
                            if (anchor != null) {
                                if (newEstado != null && !newEstado.equalsIgnoreCase(oldEstado)) {
                                    if (EstadoPedido.CANCELADO.equalsIgnoreCase(newEstado)) {
                                        if ("aceptada".equalsIgnoreCase(newRespuesta)) {
                                            Mensajes.mostrarInfo(anchor, "Tu cancelación ha sido aceptada");
                                        } else {
                                            Mensajes.mostrarInfo(anchor, "Pedido cancelado");
                                        }
                                    } else if (EstadoPedido.EN_CAMINO.equalsIgnoreCase(newEstado)) {
                                        Mensajes.mostrarInfo(anchor, "¡Tu pedido está en camino!");
                                    } else if (EstadoPedido.LISTO_PARA_RECOGER.equalsIgnoreCase(newEstado)) {
                                        Mensajes.mostrarInfo(anchor, "Tu cesta te espera en el huerto");
                                    } else if (EstadoPedido.PREPARANDO.equalsIgnoreCase(newEstado)) {
                                        Mensajes.mostrarInfo(anchor, "Estamos preparando tu cesta");
                                    } else if (EstadoPedido.ENTREGADO.equalsIgnoreCase(newEstado)) {
                                        Mensajes.mostrarInfo(anchor, "¡Cesta entregada! Que aproveche");
                                    } else if (EstadoPedido.CANCELACION_SOLICITADA.equalsIgnoreCase(newEstado)) {
                                        Mensajes.mostrarInfo(anchor, "Cancelación solicitada");
                                    } else {
                                        Mensajes.mostrarInfo(anchor, EstadoPedido.getMensajeEstado(newEstado));
                                    }
                                } else if (newRespuesta != null && !newRespuesta.equalsIgnoreCase(oldRespuesta)) {
                                    if ("rechazada".equalsIgnoreCase(newRespuesta)) {
                                        Mensajes.mostrarInfo(anchor, "El huerto no ha podido cancelar tu pedido");
                                    } else if ("aceptada".equalsIgnoreCase(newRespuesta)) {
                                        Mensajes.mostrarInfo(anchor, "Tu cancelación ha sido aceptada");
                                    }
                                }
                            }

                            lastOrderStates.put(docId, newEstado);
                            if (newRespuesta != null) {
                                lastOrderCancelRespuesta.put(docId, newRespuesta);
                            }
                        }
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, currentPosition);
    }

    private void seleccionar(int posicion) {
        currentPosition = posicion;
        Fragment fragment;

        switch (posicion) {
            case 0:
                fragment = new CestaFragment();
                break;
            case 1:
                fragment = new HuertoFragment();
                break;
            case 2:
                fragment = new NoticiasFragment();
                break;
            case 3:
            default:
                fragment = new CuentaFragment();
                break;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.navHostFragment, fragment)
                .commit();

        updateNavUI(posicion);
    }

    private void updateNavUI(int activePos) {
        int activeTextColor = Color.parseColor("#1F5A2A");
        int inactiveTextColor = Color.parseColor("#6B7A5E");

        // Item 0: Cesta
        boolean isCesta = (activePos == 0);
        animateItem(imgCesta, isCesta);
        tvCesta.setTextColor(isCesta ? activeTextColor : inactiveTextColor);
        tvCesta.getPaint().setFakeBoldText(isCesta);
        lineCesta.setVisibility(isCesta ? View.VISIBLE : View.INVISIBLE);

        // Item 1: Productos
        boolean isProductos = (activePos == 1);
        animateItem(imgProductos, isProductos);
        tvProductos.setTextColor(isProductos ? activeTextColor : inactiveTextColor);
        tvProductos.getPaint().setFakeBoldText(isProductos);
        lineProductos.setVisibility(isProductos ? View.VISIBLE : View.INVISIBLE);

        // Item 2: Noticias
        boolean isNoticias = (activePos == 2);
        animateItem(imgNoticias, isNoticias);
        tvNoticias.setTextColor(isNoticias ? activeTextColor : inactiveTextColor);
        tvNoticias.getPaint().setFakeBoldText(isNoticias);
        lineNoticias.setVisibility(isNoticias ? View.VISIBLE : View.INVISIBLE);

        // Item 3: Mi Zona
        boolean isZona = (activePos == 3);
        animateItem(imgZona, isZona);
        tvZona.setTextColor(isZona ? activeTextColor : inactiveTextColor);
        tvZona.getPaint().setFakeBoldText(isZona);
        lineZona.setVisibility(isZona ? View.VISIBLE : View.INVISIBLE);
    }

    private void animateItem(View iconView, boolean active) {
        float scale = active ? 1.1f : 1.0f;
        float alpha = active ? 1.0f : 0.7f;

        iconView.animate()
                .scaleX(scale)
                .scaleY(scale)
                .alpha(alpha)
                .setDuration(150)
                .start();
    }
}
