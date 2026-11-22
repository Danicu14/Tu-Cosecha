package com.danielcarpio.tucosecha.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Formato {
    public static String cantidad(double valor, String unidad) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#.##", symbols);
        String numStr = df.format(valor);

        if (unidad == null) unidad = "ud";
        String u = unidad.trim().toLowerCase();
        u = u.replaceAll("^[0-9.,\\s]+", "").trim();
        if (u.isEmpty()) u = "ud";

        if (valor > 1) {
            if (u.equals("ud") || u.equals("unidad") || u.equals("uds") || u.equals("unidades")) {
                u = "uds";
            } else if (u.equals("kg") || u.equals("kilo") || u.equals("kilos")) {
                u = "kg";
            } else if (u.equals("manojo") || u.equals("manojos")) {
                u = "manojos";
            } else if (u.equals("ud.") || u.equals("uds.")) {
                u = "uds.";
            } else if (!u.endsWith("s")) {
                u = u + "s";
            }
        } else {
            if (u.equals("uds") || u.equals("unidades")) {
                u = "ud";
            } else if (u.equals("manojos")) {
                u = "manojo";
            } else if (u.equals("kilos")) {
                u = "kg";
            } else if (u.equals("uds.")) {
                u = "ud.";
            }
        }

        return numStr + " " + u;
    }
}
