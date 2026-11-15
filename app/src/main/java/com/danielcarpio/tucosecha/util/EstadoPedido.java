package com.danielcarpio.tucosecha.util;

public class EstadoPedido {
    public static final String CONFIRMADO = "confirmado";
    public static final String PREPARANDO = "preparando";
    public static final String EN_CAMINO = "en_camino";
    public static final String LISTO_PARA_RECOGER = "listo_para_recoger";
    public static final String ENTREGADO = "entregado";
    public static final String CANCELACION_SOLICITADA = "cancelacion_solicitada";
    public static final String CANCELADO = "cancelado";

    public static String getMensajeEstado(String estado) {
        if (estado == null) return "Tu cesta está confirmada";
        switch (estado.toLowerCase()) {
            case PREPARANDO:
                return "Estamos preparando tu cesta";
            case EN_CAMINO:
                return "¡Tu cesta está en camino!";
            case LISTO_PARA_RECOGER:
                return "Tu cesta te espera en el huerto";
            case ENTREGADO:
                return "¡Cesta entregada! Que aproveche";
            case CANCELACION_SOLICITADA:
                return "Solicitud de cancelación en revisión";
            case CANCELADO:
                return "Pedido cancelado";
            case CONFIRMADO:
            default:
                return "Tu cesta está confirmada";
        }
    }
}
