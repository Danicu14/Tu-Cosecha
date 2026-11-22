package com.danielcarpio.tucosecha.util;

public class GestorSesion {
    private static boolean esInvitado = false;

    public static boolean esInvitado() {
        return esInvitado;
    }

    public static void setEsInvitado(boolean invitado) {
        esInvitado = invitado;
    }
}
