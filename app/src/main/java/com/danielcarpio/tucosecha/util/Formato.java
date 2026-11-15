package com.danielcarpio.tucosecha.util;

import java.util.Locale;

public class Formato {
    public static String cantidad(double valor, String unidad) {
        String numStr;
        if (valor == (int) valor) {
            numStr = String.valueOf((int) valor);
        } else {
            numStr = String.format(Locale.getDefault(), "%.1f", valor).replace('.', ',');
        }

        if (unidad == null) unidad = "ud";
        String u = unidad.trim().toLowerCase();

        if (valor > 1) {
            if (u.equals("ud") || u.equals("unidad")) u = "uds";
            else if (u.equals("kg")) u = "kg";
            else if (u.equals("manojo")) u = "manojos";
            else if (u.equals("ud.")) u = "uds.";
            else if (!u.endsWith("s")) u = u + "s";
        }

        return numStr + " " + u;
    }
}
