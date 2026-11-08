package com.danielcarpio.tucosecha.model;

public class ProductoCosecha {
    private String nombre;
    private String imagen;
    private String unidad;
    private double cantidadBasico;
    private double cantidadFamiliar;
    private double cantidadPremium;
    private boolean escaso;

    public ProductoCosecha() {}

    public ProductoCosecha(String nombre, String imagen, String unidad, double cantidadBasico, double cantidadFamiliar, double cantidadPremium, boolean escaso) {
        this.nombre = nombre;
        this.imagen = imagen;
        this.unidad = unidad;
        this.cantidadBasico = cantidadBasico;
        this.cantidadFamiliar = cantidadFamiliar;
        this.cantidadPremium = cantidadPremium;
        this.escaso = escaso;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public double getCantidadBasico() { return cantidadBasico; }
    public void setCantidadBasico(double cantidadBasico) { this.cantidadBasico = cantidadBasico; }
    public double getCantidadFamiliar() { return cantidadFamiliar; }
    public void setCantidadFamiliar(double cantidadFamiliar) { this.cantidadFamiliar = cantidadFamiliar; }
    public double getCantidadPremium() { return cantidadPremium; }
    public void setCantidadPremium(double cantidadPremium) { this.cantidadPremium = cantidadPremium; }
    public boolean isEscaso() { return escaso; }
    public void setEscaso(boolean escaso) { this.escaso = escaso; }
}
