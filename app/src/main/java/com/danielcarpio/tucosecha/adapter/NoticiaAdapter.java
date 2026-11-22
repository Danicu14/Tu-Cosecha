package com.danielcarpio.tucosecha.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.model.Noticia;
import com.danielcarpio.tucosecha.util.CacheBitmap;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NoticiaAdapter extends RecyclerView.Adapter<NoticiaAdapter.NoticiaViewHolder> {

    public interface ListenerNoticia {
        void onNoticiaClick(Noticia noticia);
        void onNoticiaLongClick(Noticia noticia);
    }

    private final Context contexto;
    private final List<Noticia> listaNoticias;
    private final ListenerNoticia listener;
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd MMM yyyy", new Locale("es", "ES"));

    public NoticiaAdapter(Context contexto, List<Noticia> listaNoticias, ListenerNoticia listener) {
        this.contexto = contexto;
        this.listaNoticias = listaNoticias;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoticiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(contexto).inflate(R.layout.item_noticia, parent, false);
        return new NoticiaViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticiaViewHolder soporte, int posicion) {
        Noticia noticia = listaNoticias.get(posicion);

        soporte.tvTitulo.setText(noticia.getTitulo());
        soporte.tvResumen.setText(noticia.getResumen());

        if (noticia.getFecha() != null) {
            soporte.tvFecha.setText(formatoFecha.format(noticia.getFecha().toDate()));
        } else {
            soporte.tvFecha.setText("");
        }

        String categoria = noticia.getCategoria() != null ? noticia.getCategoria() : "huerto";
        soporte.chipCategoria.setText(capitalizar(categoria));

        switch (categoria.toLowerCase()) {
            case "cosecha":
                soporte.chipCategoria.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F57C1B")));
                soporte.chipCategoria.setTextColor(Color.WHITE);
                break;
            case "consejos":
                soporte.chipCategoria.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1E88E5")));
                soporte.chipCategoria.setTextColor(Color.WHITE);
                break;
            case "aviso":
                soporte.chipCategoria.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E8401C")));
                soporte.chipCategoria.setTextColor(Color.WHITE);
                break;
            case "huerto":
            default:
                soporte.chipCategoria.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#3E8E2F")));
                soporte.chipCategoria.setTextColor(Color.WHITE);
                break;
        }

        if (noticia.getFotoBase64() != null && !noticia.getFotoBase64().isEmpty()) {
            soporte.imgNoticiaCard.setVisibility(View.VISIBLE);
            String claveFoto = noticia.getId();
            Bitmap bitmapEnCache = CacheBitmap.obtenerBitmap(claveFoto);
            if (bitmapEnCache != null) {
                soporte.imgNoticiaCard.setImageBitmap(bitmapEnCache);
            } else {
                soporte.imgNoticiaCard.setImageDrawable(null);
                AsyncTask.execute(() -> {
                    try {
                        byte[] bytesDecodificados = Base64.decode(noticia.getFotoBase64(), Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytesDecodificados, 0, bytesDecodificados.length);
                        if (bmp != null) {
                            CacheBitmap.guardarBitmap(claveFoto, bmp);
                            soporte.itemView.post(() -> {
                                if (soporte.getAdapterPosition() != RecyclerView.NO_POSITION) {
                                    Noticia actual = listaNoticias.get(soporte.getAdapterPosition());
                                    if (actual.getId().equals(noticia.getId())) {
                                        soporte.imgNoticiaCard.setImageBitmap(bmp);
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } else {
            soporte.imgNoticiaCard.setVisibility(View.GONE);
            soporte.imgNoticiaCard.setImageDrawable(null);
        }

        soporte.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNoticiaClick(noticia);
        });

        soporte.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onNoticiaLongClick(noticia);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return listaNoticias.size();
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return "";
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    public static class NoticiaViewHolder extends RecyclerView.ViewHolder {
        ImageView imgNoticiaCard;
        TextView tvTitulo, tvResumen, tvFecha;
        Chip chipCategoria;

        public NoticiaViewHolder(@NonNull View itemView) {
            super(itemView);
            imgNoticiaCard = itemView.findViewById(R.id.imgNoticiaCard);
            tvTitulo = itemView.findViewById(R.id.tvTituloNoticia);
            tvResumen = itemView.findViewById(R.id.tvResumenNoticia);
            tvFecha = itemView.findViewById(R.id.tvFechaNoticia);
            chipCategoria = itemView.findViewById(R.id.chipCategoriaNoticia);
        }
    }
}
