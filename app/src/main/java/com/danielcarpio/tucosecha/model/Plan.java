package com.danielcarpio.tucosecha.model;

import java.util.List;

public class Plan {
    private String id;
    private String nombre;
    private String descripcion;
    private double precioMensual;
    private String personas;
    private int productosPorCesta;
    private boolean prioridad;
    private List<String> beneficios;

    public Plan() {
        // Empty constructor required for Firestore
    }

    public Plan(String id, String nombre, String descripcion, double precioMensual, String personas, int productosPorCesta, boolean prioridad, List<String> beneficios) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioMensual = precioMensual;
        this.personas = personas;
        this.productosPorCesta = productosPorCesta;
        this.prioridad = prioridad;
        this.beneficios = beneficios;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecioMensual() {
        return precioMensual;
    }

    public void setPrecioMensual(double precioMensual) {
        this.precioMensual = precioMensual;
    }

    public String getPersonas() {
        return personas;
    }

    public void setPersonas(String personas) {
        this.personas = personas;
    }

    public int getProductosPorCesta() {
        return productosPorCesta;
    }

    public void setProductosPorCesta(int productosPorCesta) {
        this.productosPorCesta = productosPorCesta;
    }

    public boolean isPrioridad() {
        return prioridad;
    }

    public void setPrioridad(boolean prioridad) {
        this.prioridad = prioridad;
    }

    public List<String> getBeneficios() {
        return beneficios;
    }

    public void setBeneficios(List<String> beneficios) {
        this.beneficios = beneficios;
    }
}
