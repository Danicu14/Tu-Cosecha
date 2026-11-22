package com.danielcarpio.tucosecha.model;

import java.util.List;
import java.util.Map;

public class Pedido {
    private String id;
    private String uid;
    private String nombreUsuario;
    private String idCosecha;
    private String semana;
    private String plan;
    private List<Map<String, Object>> productos;
    private String metodoEntrega;
    private Map<String, Object> direccion;
    private String lugarRecogida;
    private String fechaEntrega;
    private String horaEntrega;
    private String estado;
    private String estadoPrevio;
    private Map<String, Object> cancelacion;
    private Object fechaPedido;

    public Pedido() {}

    public Pedido(String id, String uid, String nombreUsuario, String idCosecha, String semana, String plan, List<Map<String, Object>> productos, String metodoEntrega, Map<String, Object> direccion, String lugarRecogida, String fechaEntrega, String horaEntrega, String estado, String estadoPrevio, Map<String, Object> cancelacion, Object fechaPedido) {
        this.id = id;
        this.uid = uid;
        this.nombreUsuario = nombreUsuario;
        this.idCosecha = idCosecha;
        this.semana = semana;
        this.plan = plan;
        this.productos = productos;
        this.metodoEntrega = metodoEntrega;
        this.direccion = direccion;
        this.lugarRecogida = lugarRecogida;
        this.fechaEntrega = fechaEntrega;
        this.horaEntrega = horaEntrega;
        this.estado = estado;
        this.estadoPrevio = estadoPrevio;
        this.cancelacion = cancelacion;
        this.fechaPedido = fechaPedido;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
    public String getIdCosecha() { return idCosecha; }
    public void setIdCosecha(String idCosecha) { this.idCosecha = idCosecha; }
    public String getSemana() { return semana; }
    public void setSemana(String semana) { this.semana = semana; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public List<Map<String, Object>> getProductos() { return productos; }
    public void setProductos(List<Map<String, Object>> productos) { this.productos = productos; }
    public String getMetodoEntrega() { return metodoEntrega; }
    public void setMetodoEntrega(String metodoEntrega) { this.metodoEntrega = metodoEntrega; }
    public Map<String, Object> getDireccion() { return direccion; }
    public void setDireccion(Map<String, Object> direccion) { this.direccion = direccion; }
    public String getLugarRecogida() { return lugarRecogida; }
    public void setLugarRecogida(String lugarRecogida) { this.lugarRecogida = lugarRecogida; }
    public String getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(String fechaEntrega) { this.fechaEntrega = fechaEntrega; }
    public String getHoraEntrega() { return horaEntrega; }
    public void setHoraEntrega(String horaEntrega) { this.horaEntrega = horaEntrega; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getEstadoPrevio() { return estadoPrevio; }
    public void setEstadoPrevio(String estadoPrevio) { this.estadoPrevio = estadoPrevio; }
    public Map<String, Object> getCancelacion() { return cancelacion; }
    public void setCancelacion(Map<String, Object> cancelacion) { this.cancelacion = cancelacion; }
    public Object getFechaPedido() { return fechaPedido; }
    public void setFechaPedido(Object fechaPedido) { this.fechaPedido = fechaPedido; }
}
