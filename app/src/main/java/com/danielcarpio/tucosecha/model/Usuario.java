package com.danielcarpio.tucosecha.model;

public class Usuario {
    private String uid;
    private String nombre;
    private String apellidos;
    private String email;
    private String rol;
    private String plan;
    private String fechaAlta;

    public Usuario() {
        // Empty constructor required for Firestore
    }

    public Usuario(String uid, String nombre, String apellidos, String email, String rol, String plan, String fechaAlta) {
        this.uid = uid;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.email = email;
        this.rol = rol;
        this.plan = plan;
        this.fechaAlta = fechaAlta;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(String fechaAlta) {
        this.fechaAlta = fechaAlta;
    }
}
