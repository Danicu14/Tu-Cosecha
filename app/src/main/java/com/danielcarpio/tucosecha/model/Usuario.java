package com.danielcarpio.tucosecha.model;

import java.util.Map;

public class Usuario {
    private String id;
    private String nombre;
    private String apellidos;
    private String email;
    private String rol;
    private String plan;
    private String estadoSuscripcion;
    private String fechaInicioPlan;
    private Map<String, Object> direccion;

    public Usuario() {}

    public Usuario(String id, String nombre, String apellidos, String email, String rol, String plan, String estadoSuscripcion, String fechaInicioPlan, Map<String, Object> direccion) {
        this.id = id;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.email = email;
        this.rol = rol;
        this.plan = plan;
        this.estadoSuscripcion = estadoSuscripcion;
        this.fechaInicioPlan = fechaInicioPlan;
        this.direccion = direccion;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public String getEstadoSuscripcion() { return estadoSuscripcion; }
    public void setEstadoSuscripcion(String estadoSuscripcion) { this.estadoSuscripcion = estadoSuscripcion; }
    public String getFechaInicioPlan() { return fechaInicioPlan; }
    public void setFechaInicioPlan(String fechaInicioPlan) { this.fechaInicioPlan = fechaInicioPlan; }
    public Map<String, Object> getDireccion() { return direccion; }
    public void setDireccion(Map<String, Object> direccion) { this.direccion = direccion; }
}
