package com.danielcarpio.tucosecha.model;

import java.util.List;

public class Cultivo {
    private String id;
    private String nombre;
    private String imagen;
    private String fase;
    private int progreso;
    private String categoria;
    private String fechaSiembra;
    private String fechaCosecha;
    private String descripcion;
    private double precio;
    private String unidadVenta;
    private List<String> etiquetas;
    private String tipoCultivo;

    public Cultivo() {}

    public Cultivo(String id, String nombre, String imagen, String fase, int progreso, String categoria, String fechaSiembra, String fechaCosecha, String descripcion, double precio, String unidadVenta, List<String> etiquetas, String tipoCultivo) {
        this.id = id;
        this.nombre = nombre;
        this.imagen = imagen;
        this.fase = fase;
        this.progreso = progreso;
        this.categoria = categoria;
        this.fechaSiembra = fechaSiembra;
        this.fechaCosecha = fechaCosecha;
        this.descripcion = descripcion;
        this.precio = precio;
        this.unidadVenta = unidadVenta;
        this.etiquetas = etiquetas;
        this.tipoCultivo = tipoCultivo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }
    public int getProgreso() { return progreso; }
    public void setProgreso(int progreso) { this.progreso = progreso; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getFechaSiembra() { return fechaSiembra; }
    public void setFechaSiembra(String fechaSiembra) { this.fechaSiembra = fechaSiembra; }
    public String getFechaCosecha() { return fechaCosecha; }
    public void setFechaCosecha(String fechaCosecha) { this.fechaCosecha = fechaCosecha; }
    public String getFechaCosechaEstimada() { return fechaCosecha; }
    public void setFechaCosechaEstimada(String fechaCosechaEstimada) { this.fechaCosecha = fechaCosechaEstimada; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public String getUnidadVenta() { return unidadVenta; }
    public void setUnidadVenta(String unidadVenta) { this.unidadVenta = unidadVenta; }
    public List<String> getEtiquetas() { return etiquetas; }
    public void setEtiquetas(List<String> etiquetas) { this.etiquetas = etiquetas; }
    public String getTipoCultivo() { return tipoCultivo; }
    public void setTipoCultivo(String tipoCultivo) { this.tipoCultivo = tipoCultivo; }
}
