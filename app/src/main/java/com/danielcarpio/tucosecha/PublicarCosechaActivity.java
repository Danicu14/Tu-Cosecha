package com.danielcarpio.tucosecha;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.adapter.PublicarCultivoAdapter;
import com.danielcarpio.tucosecha.model.Cosecha;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.danielcarpio.tucosecha.model.Plan;
import com.danielcarpio.tucosecha.model.ProductoCosecha;
import com.danielcarpio.tucosecha.model.Usuario;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PublicarCosechaActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    private LinearLayout layoutError, layoutEmpty;
    private MaterialButton btnReintentar;
    private View scrollContent;

    private TextInputLayout tilFechaEntrega;
    private TextInputEditText etFechaEntrega;
    private TextInputLayout tilSemanaCalculada;
    private TextInputEditText etSemanaCalculada;
    private TextInputLayout tilFranjaHoraria;
    private AutoCompleteTextView actvFranjaHoraria;
    private TextInputLayout tilLugarRecogida;
    private TextInputEditText etLugarRecogida;

    private MaterialButton btnCopiarAnterior;
    private RecyclerView rvCultivos;
    private MaterialCardView cardAvisos;
    private TextView tvAvisos;
    private MaterialButton btnPublicar;

    private final List<PublicarCultivoAdapter.CultivoConfig> configItems = new ArrayList<>();
    private PublicarCultivoAdapter adapter;

    private final Map<String, Integer> cuposPlanes = new HashMap<>(); // basico -> 3, familiar -> 4, premium -> 6
    private Date fechaEntregaSeleccionada = null;

    private final List<String> franjasHorarias = Arrays.asList("10:00 a 13:00", "17:00 a 20:00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publicar_cosecha);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.toolbarPublicarCosecha);
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progressPublicarLoading);
        layoutError = findViewById(R.id.layoutErrorPublicar);
        layoutEmpty = findViewById(R.id.layoutEmptyCultivosListos);
        btnReintentar = findViewById(R.id.btnReintentarPublicar);
        scrollContent = findViewById(R.id.scrollPublicarCosecha);

        tilFechaEntrega = findViewById(R.id.tilFechaEntrega);
        etFechaEntrega = findViewById(R.id.etFechaEntrega);
        tilSemanaCalculada = findViewById(R.id.tilSemanaCalculada);
        etSemanaCalculada = findViewById(R.id.etSemanaCalculada);
        tilFranjaHoraria = findViewById(R.id.tilFranjaHoraria);
        actvFranjaHoraria = findViewById(R.id.actvFranjaHoraria);
        tilLugarRecogida = findViewById(R.id.tilLugarRecogida);
        etLugarRecogida = findViewById(R.id.etLugarRecogida);

        btnCopiarAnterior = findViewById(R.id.btnCopiarAnterior);
        rvCultivos = findViewById(R.id.rvCultivosPublicar);
        cardAvisos = findViewById(R.id.cardAvisosVariedades);
        tvAvisos = findViewById(R.id.tvAvisosVariedades);
        btnPublicar = findViewById(R.id.btnPublicarCosechaConfirmar);

        rvCultivos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PublicarCultivoAdapter(configItems, this::validarAvisosCupos);
        rvCultivos.setAdapter(adapter);

        setupFranjaHorariaDropdown();
        setupDatePickerFechaEntrega();
        setupDefectoFechaEntrega();

        btnCopiarAnterior.setOnClickListener(v -> copiarDeCosechaAnterior());
        btnPublicar.setOnClickListener(v -> intentarPublicarCosecha());
        btnReintentar.setOnClickListener(v -> checkAdminRoleAndLoad());

        checkAdminRoleAndLoad();
    }

    private void setupFranjaHorariaDropdown() {
        ArrayAdapter<String> franjaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, franjasHorarias);
        actvFranjaHoraria.setAdapter(franjaAdapter);
        actvFranjaHoraria.setText(franjasHorarias.get(0), false);
    }

    private void setupDefectoFechaEntrega() {
        // Por defecto, sugerir fecha válida: al menos 3 días desde hoy (ej. próximo sábado que cumpla)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 4);
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        setFechaEntrega(cal.getTime());
    }

    private void setupDatePickerFechaEntrega() {
        View.OnClickListener clickListener = v -> {
            Calendar cal = Calendar.getInstance();
            if (fechaEntregaSeleccionada != null) {
                cal.setTime(fechaEntregaSeleccionada);
            } else {
                cal.add(Calendar.DAY_OF_YEAR, 4);
            }

            DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(Calendar.YEAR, year);
                sel.set(Calendar.MONTH, month);
                sel.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                sel.set(Calendar.HOUR_OF_DAY, 0);
                sel.set(Calendar.MINUTE, 0);
                sel.set(Calendar.SECOND, 0);
                sel.set(Calendar.MILLISECOND, 0);

                // Comprobar mínimo 3 días desde hoy
                Calendar hoyMin3 = Calendar.getInstance();
                hoyMin3.set(Calendar.HOUR_OF_DAY, 0);
                hoyMin3.set(Calendar.MINUTE, 0);
                hoyMin3.set(Calendar.SECOND, 0);
                hoyMin3.set(Calendar.MILLISECOND, 0);
                hoyMin3.add(Calendar.DAY_OF_YEAR, 3);

                if (sel.before(hoyMin3)) {
                    tilFechaEntrega.setError("La fecha debe ser al menos 3 días posterior a hoy");
                    fechaEntregaSeleccionada = null;
                    etFechaEntrega.setText("");
                    etSemanaCalculada.setText("");
                } else {
                    tilFechaEntrega.setError(null);
                    setFechaEntrega(sel.getTime());
                }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        };

        etFechaEntrega.setOnClickListener(clickListener);
        tilFechaEntrega.setEndIconOnClickListener(clickListener);
    }

    private void setFechaEntrega(Date date) {
        fechaEntregaSeleccionada = date;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etFechaEntrega.setText(sdf.format(date));

        // Calcular texto de semana: ej. "Semana del 15 al 21 de febrero"
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        Calendar calInicio = (Calendar) cal.clone();
        calInicio.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        if (calInicio.after(cal)) {
            calInicio.add(Calendar.DAY_OF_YEAR, -7);
        }

        Calendar calFin = (Calendar) calInicio.clone();
        calFin.add(Calendar.DAY_OF_YEAR, 6);

        Locale esLocale = Locale.forLanguageTag("es-ES");
        SimpleDateFormat sdfDia = new SimpleDateFormat("d", esLocale);
        SimpleDateFormat sdfMes = new SimpleDateFormat("MMMM", esLocale);

        String textoSemana;
        if (calInicio.get(Calendar.MONTH) == calFin.get(Calendar.MONTH)) {
            textoSemana = "Semana del " + sdfDia.format(calInicio.getTime()) + " al " + sdfDia.format(calFin.getTime()) + " de " + sdfMes.format(calFin.getTime());
        } else {
            textoSemana = "Semana del " + sdfDia.format(calInicio.getTime()) + " de " + sdfMes.format(calInicio.getTime()) + " al " + sdfDia.format(calFin.getTime()) + " de " + sdfMes.format(calFin.getTime());
        }

        etSemanaCalculada.setText(textoSemana);
    }

    private void checkAdminRoleAndLoad() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Mensajes.mostrarError(findViewById(android.R.id.content), "Acceso solo para el hortelano");
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        db.collection("usuarios").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Mensajes.mostrarError(findViewById(android.R.id.content), "Acceso solo para el hortelano");
                        finish();
                        return;
                    }

                    Usuario usuario = documentSnapshot.toObject(Usuario.class);
                    if (usuario == null || !"administrador".equalsIgnoreCase(usuario.getRol())) {
                        Mensajes.mostrarError(findViewById(android.R.id.content), "Acceso solo para el hortelano");
                        finish();
                        return;
                    }

                    cargarPlanesYCupos();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    layoutError.setVisibility(View.VISIBLE);
                    Mensajes.mostrarError(toolbar, "Error de verificación: " + e.getMessage());
                });
    }

    private void cargarPlanesYCupos() {
        // Defaults
        cuposPlanes.put("basico", 3);
        cuposPlanes.put("familiar", 4);
        cuposPlanes.put("premium", 6);

        db.collection("planes").get().addOnSuccessListener(snapshots -> {
            if (snapshots != null) {
                for (QueryDocumentSnapshot doc : snapshots) {
                    Plan plan = doc.toObject(Plan.class);
                    if (plan.getId() != null && plan.getProductosPorCesta() > 0) {
                        cuposPlanes.put(plan.getId().toLowerCase(), plan.getProductosPorCesta());
                    }
                }
            }
            cargarCultivosListos();
        }).addOnFailureListener(e -> {
            // Continúa con cupos por defecto si falla lectura de planes
            cargarCultivosListos();
        });
    }

    private void cargarCultivosListos() {
        db.collection("cultivos")
                .whereEqualTo("fase", "listo")
                .get()
                .addOnSuccessListener(snapshots -> {
                    progressBar.setVisibility(View.GONE);
                    configItems.clear();

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Cultivo c = doc.toObject(Cultivo.class);
                            if (c.getId() == null || c.getId().isEmpty()) {
                                c.setId(doc.getId());
                            }
                            configItems.add(new PublicarCultivoAdapter.CultivoConfig(c));
                        }
                    }

                    if (configItems.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        btnPublicar.setEnabled(false);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        btnPublicar.setEnabled(true);
                    }

                    adapter.notifyDataSetChanged();
                    validarAvisosCupos();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    layoutError.setVisibility(View.VISIBLE);
                    Mensajes.mostrarError(toolbar, "Error al cargar cultivos: " + e.getMessage());
                });
    }

    private void validarAvisosCupos() {
        int countBasico = 0;
        int countFamiliar = 0;
        int countPremium = 0;

        for (PublicarCultivoAdapter.CultivoConfig cfg : configItems) {
            if (cfg.incluido) {
                if (cfg.cantBasico > 0) countBasico++;
                if (cfg.cantFamiliar > 0) countFamiliar++;
                if (cfg.cantPremium > 0) countPremium++;
            }
        }

        int cupoBasico = cuposPlanes.containsKey("basico") ? cuposPlanes.get("basico") : 3;
        int cupoFamiliar = cuposPlanes.containsKey("familiar") ? cuposPlanes.get("familiar") : 4;
        int cupoPremium = cuposPlanes.containsKey("premium") ? cuposPlanes.get("premium") : 6;

        List<String> avisos = new ArrayList<>();
        if (countBasico < cupoBasico) {
            avisos.add("• El plan Básico solo podrá elegir " + countBasico + " variedades (su cupo es " + cupoBasico + ").");
        }
        if (countFamiliar < cupoFamiliar) {
            avisos.add("• El plan Familiar solo podrá elegir " + countFamiliar + " variedades (su cupo es " + cupoFamiliar + ").");
        }
        if (countPremium < cupoPremium) {
            avisos.add("• El plan Premium solo podrá elegir " + countPremium + " variedades (su cupo es " + cupoPremium + ").");
        }

        if (avisos.isEmpty()) {
            cardAvisos.setVisibility(View.GONE);
        } else {
            cardAvisos.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder("Avisos de variedades por plan:\n");
            for (String a : avisos) {
                sb.append(a).append("\n");
            }
            tvAvisos.setText(sb.toString().trim());
        }
    }

    private void copiarDeCosechaAnterior() {
        btnCopiarAnterior.setEnabled(false);
        db.collection("cosechas")
                .get()
                .addOnSuccessListener(snapshots -> {
                    btnCopiarAnterior.setEnabled(true);
                    if (snapshots == null || snapshots.isEmpty()) {
                        Mensajes.mostrarInfo(toolbar, "No hay cosechas anteriores registradas");
                        return;
                    }

                    // Encontrar la última cosecha (activa o la más reciente)
                    Cosecha ultimaCosecha = null;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Cosecha c = doc.toObject(Cosecha.class);
                        if (c.isActiva()) {
                            ultimaCosecha = c;
                            break;
                        }
                    }

                    if (ultimaCosecha == null) {
                        // Tomar la primera disponible
                        ultimaCosecha = snapshots.getDocuments().get(0).toObject(Cosecha.class);
                    }

                    if (ultimaCosecha == null || ultimaCosecha.getProductos() == null || ultimaCosecha.getProductos().isEmpty()) {
                        Mensajes.mostrarInfo(toolbar, "La cosecha anterior no contenía productos");
                        return;
                    }

                    Map<String, ProductoCosecha> prodMap = new HashMap<>();
                    for (ProductoCosecha pc : ultimaCosecha.getProductos()) {
                        if (pc.getNombre() != null) {
                            prodMap.put(pc.getNombre().toLowerCase(), pc);
                        }
                    }

                    for (PublicarCultivoAdapter.CultivoConfig cfg : configItems) {
                        String k = cfg.cultivo.getNombre() != null ? cfg.cultivo.getNombre().toLowerCase() : "";
                        if (prodMap.containsKey(k)) {
                            ProductoCosecha ant = prodMap.get(k);
                            if (ant != null) {
                                cfg.incluido = true;
                                cfg.unidad = ant.getUnidad() != null ? ant.getUnidad() : cfg.unidad;
                                cfg.cantBasico = ant.getCantidadBasico();
                                cfg.cantFamiliar = ant.getCantidadFamiliar();
                                cfg.cantPremium = ant.getCantidadPremium();
                                cfg.escaso = ant.isEscaso();
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                    validarAvisosCupos();
                    Mensajes.mostrarExito(toolbar, "Cantidades copiadas de la cosecha anterior");
                })
                .addOnFailureListener(e -> {
                    btnCopiarAnterior.setEnabled(true);
                    Mensajes.mostrarError(toolbar, "Error al consultar cosecha anterior: " + e.getMessage());
                });
    }

    private void intentarPublicarCosecha() {
        if (fechaEntregaSeleccionada == null) {
            tilFechaEntrega.setError("Selecciona una fecha de entrega válida");
            return;
        }

        Calendar hoyMin3 = Calendar.getInstance();
        hoyMin3.set(Calendar.HOUR_OF_DAY, 0);
        hoyMin3.set(Calendar.MINUTE, 0);
        hoyMin3.set(Calendar.SECOND, 0);
        hoyMin3.set(Calendar.MILLISECOND, 0);
        hoyMin3.add(Calendar.DAY_OF_YEAR, 3);

        Calendar sel = Calendar.getInstance();
        sel.setTime(fechaEntregaSeleccionada);
        if (sel.before(hoyMin3)) {
            tilFechaEntrega.setError("La fecha debe ser al menos 3 días posterior a hoy");
            return;
        }

        List<ProductoCosecha> productos = adapter.getProductosSeleccionados();
        if (productos.isEmpty()) {
            Mensajes.mostrarError(btnPublicar, "Debes incluir al menos un producto");
            return;
        }

        btnPublicar.setEnabled(false);

        // Comprobar si hay pedidos de la cosecha activa que no estén entregados ni cancelados
        db.collection("cosechas")
                .whereEqualTo("activa", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        String idCosechaActiva = snapshots.getDocuments().get(0).getId();
                        db.collection("pedidos")
                                .whereEqualTo("idCosecha", idCosechaActiva)
                                .get()
                                .addOnSuccessListener(pedidosSnaps -> {
                                    int pedidosSinEntregar = 0;
                                    if (pedidosSnaps != null) {
                                        for (QueryDocumentSnapshot pDoc : pedidosSnaps) {
                                            String est = pDoc.getString("estado");
                                            if (est != null && !est.equalsIgnoreCase("entregado") && !est.equalsIgnoreCase("cancelado")) {
                                                pedidosSinEntregar++;
                                            }
                                        }
                                    }

                                    if (pedidosSinEntregar > 0) {
                                        btnPublicar.setEnabled(true);
                                        mostrarDialogoPedidosPendientes(pedidosSinEntregar, productos);
                                    } else {
                                        ejecutarPublicacionEnFirestore(productos);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // Si falla comprobar pedidos, preguntar o proceder
                                    ejecutarPublicacionEnFirestore(productos);
                                });
                    } else {
                        ejecutarPublicacionEnFirestore(productos);
                    }
                })
                .addOnFailureListener(e -> {
                    ejecutarPublicacionEnFirestore(productos);
                });
    }

    private void mostrarDialogoPedidosPendientes(int numPedidos, List<ProductoCosecha> productos) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Pedidos pendientes")
                .setMessage("Hay " + numPedidos + " pedidos sin entregar de la cosecha actual. ¿Publicar igualmente?")
                .setPositiveButton("Publicar", (dialog, which) -> {
                    ejecutarPublicacionEnFirestore(productos);
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    btnPublicar.setEnabled(true);
                })
                .setCancelable(false)
                .show();
    }

    private void ejecutarPublicacionEnFirestore(List<ProductoCosecha> productos) {
        btnPublicar.setEnabled(false);
        btnPublicar.setText("Publicando cosecha...");

        String semanaTexto = etSemanaCalculada.getText() != null ? etSemanaCalculada.getText().toString().trim() : "";
        String fechaRecogidaTexto = etFechaEntrega.getText() != null ? etFechaEntrega.getText().toString().trim() : "";
        String horaRecogidaTexto = actvFranjaHoraria.getText() != null ? actvFranjaHoraria.getText().toString().trim() : "10:00 a 13:00";
        String lugarRecogidaTexto = etLugarRecogida.getText() != null ? etLugarRecogida.getText().toString().trim() : "Huerto Tu Cosecha, vega del Tajo, Talavera de la Reina";

        Map<String, Object> nuevaCosecha = new HashMap<>();
        nuevaCosecha.put("activa", true);
        nuevaCosecha.put("semana", semanaTexto);
        nuevaCosecha.put("fechaRecogida", fechaRecogidaTexto);
        nuevaCosecha.put("horaRecogida", horaRecogidaTexto);
        nuevaCosecha.put("lugarRecogida", lugarRecogidaTexto);
        nuevaCosecha.put("productos", productos);
        nuevaCosecha.put("fechaPublicacion", FieldValue.serverTimestamp());

        db.collection("cosechas").get()
                .addOnSuccessListener(snapshots -> {
                    WriteBatch batch = db.batch();

                    // Desactivar todas las cosechas existentes
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            batch.update(doc.getReference(), "activa", false);
                        }
                    }

                    // Crear la nueva cosecha activa
                    DocumentReference newDocRef = db.collection("cosechas").document();
                    batch.set(newDocRef, nuevaCosecha);

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Mensajes.mostrarExito(toolbar, "¡Cosecha publicada con éxito!");
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnPublicar.setEnabled(true);
                                btnPublicar.setText("Publicar cosecha");
                                Mensajes.mostrarError(btnPublicar, "Error al publicar cosecha: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    btnPublicar.setEnabled(true);
                    btnPublicar.setText("Publicar cosecha");
                    Mensajes.mostrarError(btnPublicar, "Error al consultar cosechas: " + e.getMessage());
                });
    }
}
