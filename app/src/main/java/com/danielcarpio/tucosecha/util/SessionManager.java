package com.danielcarpio.tucosecha.util;

public class SessionManager {
    private static boolean esInvitado = false;

    public static boolean isEsInvitado() {
        return esInvitado;
    }

    public static void setEsInvitado(boolean invitado) {
        esInvitado = invitado;
    }
}
