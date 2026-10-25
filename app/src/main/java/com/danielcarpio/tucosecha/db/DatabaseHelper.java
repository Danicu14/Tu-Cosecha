package com.danielcarpio.tucosecha.db;

import android.util.Log;

import com.danielcarpio.tucosecha.model.Cultivo;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {

    public interface OnDataLoadedListener {
        void onSuccess(int count);
        void onError(Exception e);
    }

    public static void cargarDatosEjemplo(OnDataLoadedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<Cultivo> sampleCultivos = new ArrayList<>();
        // Hortalizas
        sampleCultivos.add(new Cultivo("", "Acelga", "acelga", "listo", 100, "hortaliza", "01/11/2026", "10/02/2027", "Hojas verdes y carnosas de excelente calidad, ideales para sopas y revueltos frescos."));
        sampleCultivos.add(new Cultivo("", "Lechuga", "lechuga", "listo", 100, "hortaliza", "15/11/2026", "12/02/2027", "Crujiente y refrescante, cultivada con mimo en nuestra huerta de Talavera."));
        sampleCultivos.add(new Cultivo("", "Zanahoria", "zanahoria", "listo", 100, "hortaliza", "01/10/2026", "15/02/2027", "Raíz dulce y crujiente, rica en betacarotenos y sabor natural inigualable."));
        sampleCultivos.add(new Cultivo("", "Puerro", "puerro", "listo", 100, "hortaliza", "20/09/2026", "18/02/2027", "Sabor suave y aromático, perfecto para caldos y cremas tradicionales."));
        sampleCultivos.add(new Cultivo("", "Coliflor", "coliflor", "listo", 100, "hortaliza", "10/09/2026", "20/02/2027", "Pellas compactas y blancas, recién recolectadas para tu mesa cada semana."));
        sampleCultivos.add(new Cultivo("", "Brócoli", "brocoli", "listo", 100, "hortaliza", "15/09/2026", "22/02/2027", "Rico en nutrientes y antioxidantes, cosechado en su punto óptimo de madurez."));
        sampleCultivos.add(new Cultivo("", "Ajo", "ajo", "creciendo", 60, "hortaliza", "01/12/2026", "25/05/2027", "Cabezas robustas desarrollándose lentamente bajo el sol manchego."));
        sampleCultivos.add(new Cultivo("", "Cebolla morada", "cebolla_morada", "creciendo", 70, "hortaliza", "20/11/2026", "30/03/2027", "Dulce y crujiente, ideal para ensaladas frescas y guisos selectos."));
        sampleCultivos.add(new Cultivo("", "Tomate", "tomate", "sembrado", 10, "hortaliza", "15/01/2027", "15/06/2027", "La estrella de nuestra huerta, creciendo con el auténtico sabor tradicional."));
        sampleCultivos.add(new Cultivo("", "Pimiento", "pimiento", "sembrado", 8, "hortaliza", "20/01/2027", "20/06/2027", "Plantones jóvenes desarrollándose con fuerza para dar frutos coloridos y jugosos."));
        sampleCultivos.add(new Cultivo("", "Berenjena", "berenjena", "sembrado", 6, "hortaliza", "22/01/2027", "25/06/2027", "Cultivo de verano en pleno proceso de crecimiento bajo invernadero ecológico."));
        sampleCultivos.add(new Cultivo("", "Calabacín", "calabacin", "sembrado", 5, "hortaliza", "25/01/2027", "10/06/2027", "Tierno y versátil, ideal para cualquier receta saludable de temporada."));
        sampleCultivos.add(new Cultivo("", "Pepino", "pepino", "sembrado", 5, "hortaliza", "25/01/2027", "12/06/2027", "Refrescante y natural, creciendo vigoroso en suelos ricos en nutrientes."));
        sampleCultivos.add(new Cultivo("", "Judía verde", "judia_verde", "sembrado", 4, "hortaliza", "28/01/2027", "18/06/2027", "Vainas tiernas cultivadas mediante técnicas de agricultura sostenible."));
        sampleCultivos.add(new Cultivo("", "Patata", "patata", "sembrado", 12, "hortaliza", "10/01/2027", "30/05/2027", "Tubérculos sembrados en tierra fértil para garantizar una cosecha abundante."));
        sampleCultivos.add(new Cultivo("", "Calabaza", "calabaza", "sembrado", 3, "hortaliza", "01/02/2027", "15/09/2027", "Grandes frutos en formación que llenarán de sabor tus creaciones culinarias."));

        // Frutas
        sampleCultivos.add(new Cultivo("", "Pera", "pera", "creciendo", 40, "fruta", "10/03/2026", "10/08/2027", "Árbol frutal en desarrollo, ofreciendo jugosas peras de piel fina y dulce."));
        sampleCultivos.add(new Cultivo("", "Albaricoque", "albaricoques", "creciendo", 30, "fruta", "15/02/2026", "20/06/2027", "Ramas florecientes que prometen frutos dulces y aromáticos para el verano."));
        sampleCultivos.add(new Cultivo("", "Melocotón", "melocoton", "creciendo", 35, "fruta", "20/02/2026", "05/07/2027", "Melocotoneros fuertes y sanos desarrollándose bajo el clima de Talavera."));
        sampleCultivos.add(new Cultivo("", "Higo", "higo", "creciendo", 25, "fruta", "01/03/2026", "25/08/2027", "Higuera tradicional que crece frondosa aportando higos exquisitos y melosos."));
        sampleCultivos.add(new Cultivo("", "Melón", "melon", "sembrado", 2, "fruta", "05/02/2027", "10/08/2027", "Cultivo rastrero recién sembrado que nos regalará los mejores melones estivales."));
        sampleCultivos.add(new Cultivo("", "Sandía", "sandia", "sembrado", 2, "fruta", "05/02/2027", "15/08/2027", "Semillas germinando con fuerza para asegurar sandías gigantes y muy dulces."));

        WriteBatch batch = db.batch();
        for (Cultivo c : sampleCultivos) {
            c.setId(c.getNombre());
            batch.set(db.collection("cultivos").document(c.getNombre()), c);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> listener.onSuccess(sampleCultivos.size()))
                .addOnFailureListener(e -> {
                    Log.e("CARGA_DATOS", "Error al cargar cultivos: " + e.getMessage(), e);
                    listener.onError(e);
                });
    }
}
