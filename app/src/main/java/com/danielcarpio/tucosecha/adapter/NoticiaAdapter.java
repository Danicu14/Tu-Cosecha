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
import com.danielcarpio.tucosecha.util.BitmapCache;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NoticiaAdapter extends RecyclerView.Adapter<NoticiaAdapter.NoticiaViewHolder> {

    public interface OnNoticiaClickListener {
        void onNoticiaClick(Noticia noticia);
        void onNoticiaLongClick(Noticia noticia);
    }

    private final Context context;
    private final List<Noticia> noticiaList;
    private final OnNoticiaClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("es", "ES"));

    public NoticiaAdapter(Context context, List<Noticia> noticiaList, OnNoticiaClickListener listener) {
        this.context = context;
        this.noticiaList = noticiaList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoticiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_noticia, parent, false);
        return new NoticiaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticiaViewHolder holder, int position) {
        Noticia noticia = noticiaList.get(position);

        holder.tvTitulo.setText(noticia.getTitulo());
        holder.tvResumen.setText(noticia.getResumen());

        if (noticia.getFecha() != null) {
            holder.tvFecha.setText(dateFormat.format(noticia.getFecha().toDate()));
        } else {
            holder.tvFecha.setText("");
        }

        String categoria = noticia.getCategoria() != null ? noticia.getCategoria() : "huerto";
        holder.chipCategoria.setText(capitalize(categoria));

        switch (categoria.toLowerCase()) {
            case "cosecha":
                holder.chipCategoria.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F57C1B")));
                holder.chipCategoria.setTextColor(Color.WHITE);
                break;
            case "consejos":
                holder.chipCategoria.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1E88E5")));
                holder.chipCategoria.setTextColor(Color.WHITE);
                break;
            case "aviso":
                holder.chipCategoria.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#E8401C")));
                holder.chipCategoria.setTextColor(Color.WHITE);
                break;
            case "huerto":
            default:
                holder.chipCategoria.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor("#3E8E2F")));
                holder.chipCategoria.setTextColor(Color.WHITE);
                break;
        }

        if (noticia.getFotoBase64() != null && !noticia.getFotoBase64().isEmpty()) {
            holder.imgNoticiaCard.setVisibility(View.VISIBLE);
            String photoKey = noticia.getId();
            Bitmap cached = BitmapCache.getBitmap(photoKey);
            if (cached != null) {
                holder.imgNoticiaCard.setImageBitmap(cached);
            } else {
                holder.imgNoticiaCard.setImageDrawable(null);
                AsyncTask.execute(() -> {
                    try {
                        byte[] decodedBytes = Base64.decode(noticia.getFotoBase64(), Base64.DEFAULT);
                        Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        if (bmp != null) {
                            BitmapCache.putBitmap(photoKey, bmp);
                            holder.itemView.post(() -> {
                                if (holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                                    Noticia current = noticiaList.get(holder.getAdapterPosition());
                                    if (current.getId().equals(noticia.getId())) {
                                        holder.imgNoticiaCard.setImageBitmap(bmp);
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
            holder.imgNoticiaCard.setVisibility(View.GONE);
            holder.imgNoticiaCard.setImageDrawable(null);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNoticiaClick(noticia);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onNoticiaLongClick(noticia);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return noticiaList.size();
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
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
