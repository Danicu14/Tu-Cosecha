package com.danielcarpio.tucosecha.model;

import com.google.firebase.Timestamp;

public class Noticia {
    private String id;
    private String titulo;
    private String resumen;
    private String contenido;
    private Timestamp fecha;
    private String categoria; // "huerto", "cosecha", "consejos", "aviso"
    private String autor;
    private String fotoBase64;

    public Noticia() {
        // Empty constructor required for Firestore
    }

    public Noticia(String id, String titulo, String resumen, String contenido, Timestamp fecha, String categoria, String autor, String fotoBase64) {
        this.id = id;
        this.titulo = titulo;
        this.resumen = resumen;
        this.contenido = contenido;
        this.fecha = fecha;
        this.categoria = categoria;
        this.autor = autor;
        this.fotoBase64 = fotoBase64;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Timestamp getFecha() {
        return fecha;
    }

    public void setFecha(Timestamp fecha) {
        this.fecha = fecha;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getFotoBase64() {
        return fotoBase64;
    }

    public void setFotoBase64(String fotoBase64) {
        this.fotoBase64 = fotoBase64;
    }
}
