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
import com.danielcarpio.tucosecha.util.GestorSesion;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String CLAVE_PESTANA_SELECCIONADA = "pestana_seleccionada";
    private int posicionActual = 0;

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
            posicionActual = savedInstanceState.getInt(CLAVE_PESTANA_SELECCIONADA, 0);
            seleccionar(posicionActual);
        } else {
            seleccionar(0);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private ListenerRegistration listenerNotificacionesPedidos;
    private final Map<String, String> ultimosEstadosPedidos = new HashMap<>();
    private final Map<String, String> ultimaRespuestaCancelacionPedidos = new HashMap<>();
    private boolean esPrimeraCargaPedidos = true;

    @Override
    protected void onStart() {
        super.onStart();
        iniciarEscuchaNotificacionesPedidos();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerNotificacionesPedidos != null) {
            listenerNotificacionesPedidos.remove();
            listenerNotificacionesPedidos = null;
        }
    }

    private void iniciarEscuchaNotificacionesPedidos() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null || GestorSesion.esInvitado()) return;

        if (listenerNotificacionesPedidos != null) listenerNotificacionesPedidos.remove();
        esPrimeraCargaPedidos = true;
        ultimosEstadosPedidos.clear();
        ultimaRespuestaCancelacionPedidos.clear();

        listenerNotificacionesPedidos = FirebaseFirestore.getInstance()
                .collection("pedidos")
                .whereEqualTo("uid", usuario.getUid())
                .addSnapshotListener((instantaneas, e) -> {
                    if (e != null || instantaneas == null) return;

                    if (esPrimeraCargaPedidos) {
                        for (DocumentSnapshot doc : instantaneas.getDocuments()) {
                            ultimosEstadosPedidos.put(doc.getId(), doc.getString("estado"));
                            Map<String, Object> cancelacion = (Map<String, Object>) doc.get("cancelacion");
                            if (cancelacion != null && cancelacion.get("respuesta") != null) {
                                ultimaRespuestaCancelacionPedidos.put(doc.getId(), String.valueOf(cancelacion.get("respuesta")));
                            }
                        }
                        esPrimeraCargaPedidos = false;
                        return;
                    }

                    for (DocumentChange cambioDoc : instantaneas.getDocumentChanges()) {
                        if (cambioDoc.getType() == DocumentChange.Type.MODIFIED) {
                            DocumentSnapshot doc = cambioDoc.getDocument();
                            String docId = doc.getId();
                            String nuevoEstado = doc.getString("estado");
                            String viejoEstado = ultimosEstadosPedidos.get(docId);

                            Map<String, Object> cancelacion = (Map<String, Object>) doc.get("cancelacion");
                            String nuevaRespuesta = (cancelacion != null && cancelacion.get("respuesta") != null) ? String.valueOf(cancelacion.get("respuesta")) : null;
                            String viejaRespuesta = ultimaRespuestaCancelacionPedidos.get(docId);

                            View anclaje = findViewById(R.id.main);
                            if (anclaje != null) {
                                if (nuevoEstado != null && !nuevoEstado.equalsIgnoreCase(viejoEstado)) {
                                    if (EstadoPedido.CANCELADO.equalsIgnoreCase(nuevoEstado)) {
                                        if ("aceptada".equalsIgnoreCase(nuevaRespuesta)) {
                                            Mensajes.mostrarInfo(anclaje, "Tu cancelación ha sido aceptada");
                                        } else {
                                            Mensajes.mostrarInfo(anclaje, "Pedido cancelado");
                                        }
                                    } else if (EstadoPedido.EN_CAMINO.equalsIgnoreCase(nuevoEstado)) {
                                        Mensajes.mostrarInfo(anclaje, "¡Tu pedido está en camino!");
                                    } else if (EstadoPedido.LISTO_PARA_RECOGER.equalsIgnoreCase(nuevoEstado)) {
                                        Mensajes.mostrarInfo(anclaje, "Tu cesta te espera en el huerto");
                                    } else if (EstadoPedido.PREPARANDO.equalsIgnoreCase(nuevoEstado)) {
                                        Mensajes.mostrarInfo(anclaje, "Estamos preparando tu cesta");
                                    } else if (EstadoPedido.ENTREGADO.equalsIgnoreCase(nuevoEstado)) {
                                        Mensajes.mostrarInfo(anclaje, "¡Cesta entregada! Que aproveche");
                                    } else if (EstadoPedido.CANCELACION_SOLICITADA.equalsIgnoreCase(nuevoEstado)) {
                                        Mensajes.mostrarInfo(anclaje, "Cancelación solicitada");
                                    } else {
                                        Mensajes.mostrarInfo(anclaje, EstadoPedido.getMensajeEstado(nuevoEstado));
                                    }
                                } else if (nuevaRespuesta != null && !nuevaRespuesta.equalsIgnoreCase(viejaRespuesta)) {
                                    if ("rechazada".equalsIgnoreCase(nuevaRespuesta)) {
                                        Mensajes.mostrarInfo(anclaje, "El huerto no ha podido cancelar tu pedido");
                                    } else if ("aceptada".equalsIgnoreCase(nuevaRespuesta)) {
                                        Mensajes.mostrarInfo(anclaje, "Tu cancelación ha sido aceptada");
                                    }
                                }
                            }

                            ultimosEstadosPedidos.put(docId, nuevoEstado);
                            if (nuevaRespuesta != null) {
                                ultimaRespuestaCancelacionPedidos.put(docId, nuevaRespuesta);
                            }
                        }
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle estadoSalida) {
        super.onSaveInstanceState(estadoSalida);
        estadoSalida.putInt(CLAVE_PESTANA_SELECCIONADA, posicionActual);
    }

    private void seleccionar(int posicion) {
        posicionActual = posicion;
        Fragment fragmento;

        switch (posicion) {
            case 0:
                fragmento = new CestaFragment();
                break;
            case 1:
                fragmento = new HuertoFragment();
                break;
            case 2:
                fragmento = new NoticiasFragment();
                break;
            case 3:
            default:
                fragmento = new CuentaFragment();
                break;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.navHostFragment, fragmento)
                .commit();

        actualizarInterfazNavegacion(posicion);
    }

    private void actualizarInterfazNavegacion(int posActiva) {
        int colorTextoActivo = Color.parseColor("#1F5A2A");
        int colorTextoInactivo = Color.parseColor("#6B7A5E");

        // Elemento 0: Cesta
        boolean esCesta = (posActiva == 0);
        animarElemento(imgCesta, esCesta);
        tvCesta.setTextColor(esCesta ? colorTextoActivo : colorTextoInactivo);
        tvCesta.getPaint().setFakeBoldText(esCesta);
        lineCesta.setVisibility(esCesta ? View.VISIBLE : View.INVISIBLE);

        // Elemento 1: Productos
        boolean esProductos = (posActiva == 1);
        animarElemento(imgProductos, esProductos);
        tvProductos.setTextColor(esProductos ? colorTextoActivo : colorTextoInactivo);
        tvProductos.getPaint().setFakeBoldText(esProductos);
        lineProductos.setVisibility(esProductos ? View.VISIBLE : View.INVISIBLE);

        // Elemento 2: Noticias
        boolean esNoticias = (posActiva == 2);
        animarElemento(imgNoticias, esNoticias);
        tvNoticias.setTextColor(esNoticias ? colorTextoActivo : colorTextoInactivo);
        tvNoticias.getPaint().setFakeBoldText(esNoticias);
        lineNoticias.setVisibility(esNoticias ? View.VISIBLE : View.INVISIBLE);

        // Elemento 3: Mi Zona
        boolean esZona = (posActiva == 3);
        animarElemento(imgZona, esZona);
        tvZona.setTextColor(esZona ? colorTextoActivo : colorTextoInactivo);
        tvZona.getPaint().setFakeBoldText(esZona);
        lineZona.setVisibility(esZona ? View.VISIBLE : View.INVISIBLE);
    }

    private void animarElemento(View vistaIcono, boolean activo) {
        float escala = activo ? 1.1f : 1.0f;
        float alfa = activo ? 1.0f : 0.7f;

        vistaIcono.animate()
                .scaleX(escala)
                .scaleY(escala)
                .alpha(alfa)
                .setDuration(150)
                .start();
    }
}
