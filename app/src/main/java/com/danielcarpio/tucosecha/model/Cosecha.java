package com.danielcarpio.tucosecha.model;

import java.util.List;

public class Cosecha {
    private String id;
    private String semana;
    private String fechaRecogida;
    private String horaRecogida;
    private String lugarRecogida;
    private boolean activa;
    private List<ProductoCosecha> productos;

    public Cosecha() {}

    public Cosecha(String id, String semana, String fechaRecogida, String horaRecogida, String lugarRecogida, boolean activa, List<ProductoCosecha> productos) {
        this.id = id;
        this.semana = semana;
        this.fechaRecogida = fechaRecogida;
        this.horaRecogida = horaRecogida;
        this.lugarRecogida = lugarRecogida;
        this.activa = activa;
        this.productos = productos;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSemana() { return semana; }
    public void setSemana(String semana) { this.semana = semana; }
    public String getFechaRecogida() { return fechaRecogida; }
    public void setFechaRecogida(String fechaRecogida) { this.fechaRecogida = fechaRecogida; }
    public String getHoraRecogida() { return horaRecogida; }
    public void setHoraRecogida(String horaRecogida) { this.horaRecogida = horaRecogida; }
    public String getLugarRecogida() { return lugarRecogida; }
    public void setLugarRecogida(String lugarRecogida) { this.lugarRecogida = lugarRecogida; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
    public List<ProductoCosecha> getProductos() { return productos; }
    public void setProductos(List<ProductoCosecha> productos) { this.productos = productos; }
}
