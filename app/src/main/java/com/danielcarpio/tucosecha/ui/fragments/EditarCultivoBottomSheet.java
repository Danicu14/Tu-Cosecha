package com.danielcarpio.tucosecha.ui.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Cultivo;
import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditarCultivoBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_CULTIVO_ID = "id";
    private static final String ARG_CULTIVO_NOMBRE = "nombre";
    private static final String ARG_CULTIVO_FASE = "fase";
    private static final String ARG_CULTIVO_PROGRESO = "progreso";
    private static final String ARG_CULTIVO_COSECHA = "cosecha";
    private static final String ARG_CULTIVO_DESCRIPCION = "descripcion";

    public interface OnCultivoUpdatedListener {
        void onCultivoUpdated(String cultivoId, String nuevaFase, int nuevoProgreso, String nuevaFechaCosecha, String nuevaDesc);
    }

    private OnCultivoUpdatedListener updateListener;

    public void setOnCultivoUpdatedListener(OnCultivoUpdatedListener listener) {
        this.updateListener = listener;
    }

    public static EditarCultivoBottomSheet newInstance(Cultivo cultivo) {
        EditarCultivoBottomSheet sheet = new EditarCultivoBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_CULTIVO_ID, cultivo.getId() != null ? cultivo.getId() : cultivo.getNombre());
        args.putString(ARG_CULTIVO_NOMBRE, cultivo.getNombre());
        args.putString(ARG_CULTIVO_FASE, cultivo.getFase());
        args.putInt(ARG_CULTIVO_PROGRESO, cultivo.getProgreso());
        args.putString(ARG_CULTIVO_COSECHA, cultivo.getFechaCosechaEstimada());
        args.putString(ARG_CULTIVO_DESCRIPCION, cultivo.getDescripcion());
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_editar_cultivo, container, false);

        TextView tvTitulo = view.findViewById(R.id.tvEditarCultivoTitulo);
        ChipGroup chipGroupFase = view.findViewById(R.id.chipGroupFaseEdit);
        Chip chipSembrado = view.findViewById(R.id.chipFaseSembrado);
        Chip chipCreciendo = view.findViewById(R.id.chipFaseCreciendo);
        Chip chipListo = view.findViewById(R.id.chipFaseListo);
        TextView tvProgresoValue = view.findViewById(R.id.tvProgresoEditValue);
        Slider sliderProgreso = view.findViewById(R.id.sliderProgresoEdit);
        TextInputLayout tilFechaCosecha = view.findViewById(R.id.tilFechaCosechaEdit);
        TextInputEditText etFechaCosecha = view.findViewById(R.id.etFechaCosechaEdit);
        TextInputEditText etDescripcion = view.findViewById(R.id.etDescripcionEdit);
        MaterialButton btnGuardar = view.findViewById(R.id.btnGuardarCultivoEdit);

        Bundle args = getArguments();
        final String cultivoId = args != null ? args.getString(ARG_CULTIVO_ID) : null;
        final String cultivoNombre = args != null ? args.getString(ARG_CULTIVO_NOMBRE) : "";
        String faseActual = args != null ? args.getString(ARG_CULTIVO_FASE, "sembrado") : "sembrado";
        int progresoActual = args != null ? args.getInt(ARG_CULTIVO_PROGRESO, 0) : 0;
        String fechaCosechaActual = args != null ? args.getString(ARG_CULTIVO_COSECHA, "") : "";
        String descActual = args != null ? args.getString(ARG_CULTIVO_DESCRIPCION, "") : "";

        tvTitulo.setText("Editar " + cultivoNombre);
        etFechaCosecha.setText(fechaCosechaActual);
        etDescripcion.setText(descActual);
        sliderProgreso.setValue(Math.max(0, Math.min(100, progresoActual)));
        tvProgresoValue.setText((int) sliderProgreso.getValue() + "%");

        if ("listo".equalsIgnoreCase(faseActual)) {
            chipListo.setChecked(true);
        } else if ("creciendo".equalsIgnoreCase(faseActual)) {
            chipCreciendo.setChecked(true);
        } else {
            chipSembrado.setChecked(true);
        }

        // Si elige "listo", progreso pasa a 100 automáticamente
        chipGroupFase.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipFaseListo)) {
                sliderProgreso.setValue(100f);
                tvProgresoValue.setText("100%");
            }
        });

        // Si el progreso llega a 100, proponer la fase "listo"
        sliderProgreso.addOnChangeListener((slider, value, fromUser) -> {
            int intVal = (int) value;
            tvProgresoValue.setText(intVal + "%");
            if (fromUser && intVal == 100) {
                chipListo.setChecked(true);
            }
        });

        // DatePicker para fecha de cosecha estimada (formato dd/MM/yyyy)
        View.OnClickListener datePickerListener = v -> {
            Calendar cal = Calendar.getInstance();
            try {
                if (etFechaCosecha.getText() != null && !etFechaCosecha.getText().toString().isEmpty()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    cal.setTime(sdf.parse(etFechaCosecha.getText().toString().trim()));
                }
            } catch (Exception ignored) {}

            DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view1, year, month, dayOfMonth) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(year, month, dayOfMonth);
                String formatted = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(sel.getTime());
                etFechaCosecha.setText(formatted);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        };

        etFechaCosecha.setOnClickListener(datePickerListener);
        tilFechaCosecha.setEndIconOnClickListener(datePickerListener);

        // Guardar cambios: update() solo de fase, progreso, fechaCosechaEstimada y descripcion
        btnGuardar.setOnClickListener(v -> {
            if (cultivoId == null || cultivoId.isEmpty()) {
                dismiss();
                return;
            }

            String seleccionFase = "sembrado";
            if (chipListo.isChecked()) {
                seleccionFase = "listo";
            } else if (chipCreciendo.isChecked()) {
                seleccionFase = "creciendo";
            }

            int finalProgreso = (int) sliderProgreso.getValue();
            String finalFechaCosecha = etFechaCosecha.getText() != null ? etFechaCosecha.getText().toString().trim() : "";
            String finalDesc = etDescripcion.getText() != null ? etDescripcion.getText().toString().trim() : "";

            btnGuardar.setEnabled(false);
            btnGuardar.setText("Guardando...");

            Map<String, Object> updates = new HashMap<>();
            updates.put("fase", seleccionFase);
            updates.put("progreso", finalProgreso);
            updates.put("fechaCosecha", finalFechaCosecha);
            updates.put("descripcion", finalDesc);

            final String fFase = seleccionFase;
            FirebaseFirestore.getInstance().collection("cultivos").document(cultivoId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            Mensajes.mostrarExito(requireActivity().findViewById(android.R.id.content), "Cultivo actualizado");
                            if (updateListener != null) {
                                updateListener.onCultivoUpdated(cultivoId, fFase, finalProgreso, finalFechaCosecha, finalDesc);
                            }
                            dismiss();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            btnGuardar.setEnabled(true);
                            btnGuardar.setText("Guardar cambios");
                            Mensajes.mostrarError(btnGuardar, "Error al actualizar: " + e.getMessage());
                        }
                    });
        });

        return view;
    }
}
