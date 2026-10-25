package com.danielcarpio.tucosecha.model;

public class Cultivo {
    private String id;
    private String nombre;
    private String imagen;
    private String fase;
    private int progreso;
    private String categoria; // "hortaliza" o "fruta"
    private String fechaSiembra;
    private String fechaCosechaEstimada;
    private String descripcion;

    public Cultivo() {
        // Empty constructor required for Firestore
    }

    public Cultivo(String id, String nombre, String imagen, String fase, int progreso, String categoria, String fechaSiembra, String fechaCosechaEstimada, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.imagen = imagen;
        this.fase = fase;
        this.progreso = progreso;
        this.categoria = categoria;
        this.fechaSiembra = fechaSiembra;
        this.fechaCosechaEstimada = fechaCosechaEstimada;
        this.descripcion = descripcion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public String getFase() {
        return fase;
    }

    public void setFase(String fase) {
        this.fase = fase;
    }

    public int getProgreso() {
        return progreso;
    }

    public void setProgreso(int progreso) {
        this.progreso = progreso;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getFechaSiembra() {
        return fechaSiembra;
    }

    public void setFechaSiembra(String fechaSiembra) {
        this.fechaSiembra = fechaSiembra;
    }

    public String getFechaCosechaEstimada() {
        return fechaCosechaEstimada;
    }

    public void setFechaCosechaEstimada(String fechaCosechaEstimada) {
        this.fechaCosechaEstimada = fechaCosechaEstimada;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
