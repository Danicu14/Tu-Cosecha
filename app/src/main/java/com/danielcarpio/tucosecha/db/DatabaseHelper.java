package com.danielcarpio.tucosecha.db;

import android.util.Log;

import com.danielcarpio.tucosecha.model.Cultivo;
import com.danielcarpio.tucosecha.model.Plan;
import com.danielcarpio.tucosecha.model.ProductoCosecha;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseHelper {

    public interface OnDataLoadedListener {
        void onSuccess(int count);
        void onError(Exception e);
    }

    public interface OnNoticiasLoadedListener {
        void onSuccess(int count);
        void onError(Exception e);
    }

    public interface OnPlanesLoadedListener {
        void onSuccess(int count);
        void onError(Exception e);
    }

    public interface OnCosechaLoadedListener {
        void onSuccess();
        void onError(Exception e);
    }

    public static void cargarDatosEjemplo(OnDataLoadedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<Cultivo> sampleCultivos = new ArrayList<>();
        // Hortalizas (Ajo and Cebolla morada updated to listo, 100)
        sampleCultivos.add(new Cultivo("", "Acelga", "acelga", "listo", 100, "hortaliza", "01/11/2026", "10/02/2027", "Hojas verdes y carnosas de excelente calidad, ideales para sopas y revueltos frescos.", 2.50, "1 manojo", Arrays.asList("Temporada", "Ecológico"), "hoja"));
        sampleCultivos.add(new Cultivo("", "Lechuga", "lechuga", "listo", 100, "hortaliza", "15/11/2026", "12/02/2027", "Crujiente y refrescante, cultivada con mimo en nuestra huerta de Talavera.", 1.50, "1 ud", Arrays.asList("Temporada", "Km 0"), "hoja"));
        sampleCultivos.add(new Cultivo("", "Zanahoria", "zanahoria", "listo", 100, "hortaliza", "01/10/2026", "15/02/2027", "Raíz dulce y crujiente, rica en betacarotenos y sabor natural inigualable.", 1.80, "1 kg", Arrays.asList("Temporada", "Ecológico", "Km 0"), "raíz"));
        sampleCultivos.add(new Cultivo("", "Puerro", "puerro", "listo", 100, "hortaliza", "20/09/2026", "18/02/2027", "Sabor suave y aromático, perfecto para caldos y cremas tradicionales.", 2.20, "1 manojo", Arrays.asList("Temporada", "Km 0"), "bulbo"));
        sampleCultivos.add(new Cultivo("", "Coliflor", "coliflor", "listo", 100, "hortaliza", "10/09/2026", "20/02/2027", "Pellas compactas y blancas, recién recolectadas para tu mesa cada semana.", 2.80, "1 ud", Arrays.asList("Temporada"), "inflorescencia"));
        sampleCultivos.add(new Cultivo("", "Brócoli", "brocoli", "listo", 100, "hortaliza", "15/09/2026", "22/02/2027", "Rico en nutrientes y antioxidantes, cosechado en su punto óptimo de madurez.", 3.20, "1 ud", Arrays.asList("Temporada", "Ecológico"), "inflorescencia"));
        sampleCultivos.add(new Cultivo("", "Ajo", "ajo", "listo", 100, "hortaliza", "01/12/2026", "25/02/2027", "Cabezas robustas listas para recolectar bajo el sol manchego.", 4.00, "1 kg", Arrays.asList("Ecológico", "Km 0"), "bulbo"));
        sampleCultivos.add(new Cultivo("", "Cebolla morada", "cebolla_morada", "listo", 100, "hortaliza", "20/11/2026", "28/02/2027", "Dulce y crujiente, punto óptimo para ensaladas frescas y guisos selectos.", 2.00, "1 kg", Arrays.asList("Temporada", "Km 0"), "bulbo"));
        sampleCultivos.add(new Cultivo("", "Tomate", "tomate", "sembrado", 10, "hortaliza", "15/01/2027", "15/06/2027", "La estrella de nuestra huerta, creciendo con el auténtico sabor tradicional.", 3.50, "1 kg", Arrays.asList("Temporada", "Ecológico"), "fruto"));
        sampleCultivos.add(new Cultivo("", "Pimiento", "pimiento", "sembrado", 8, "hortaliza", "20/01/2027", "20/06/2027", "Plantones jóvenes desarrollándose con fuerza para dar frutos coloridos y jugosos.", 3.00, "1 kg", Arrays.asList("Temporada"), "fruto"));
        sampleCultivos.add(new Cultivo("", "Berenjena", "berenjena", "sembrado", 6, "hortaliza", "22/01/2027", "25/06/2027", "Cultivo de verano en pleno proceso de crecimiento bajo invernadero ecológico.", 2.80, "1 kg", Arrays.asList("Temporada", "Km 0"), "fruto"));
        sampleCultivos.add(new Cultivo("", "Calabacín", "calabacin", "sembrado", 5, "hortaliza", "25/01/2027", "10/06/2027", "Tierno y versátil, ideal para cualquier receta saludable de temporada.", 2.20, "1 kg", Arrays.asList("Temporada", "Km 0"), "fruto"));
        sampleCultivos.add(new Cultivo("", "Pepino", "pepino", "sembrado", 5, "hortaliza", "25/01/2027", "12/06/2027", "Refrescante y natural, creciendo vigoroso en suelos ricos en nutrientes.", 1.90, "1 kg", Arrays.asList("Temporada"), "fruto"));
        sampleCultivos.add(new Cultivo("", "Judía verde", "judia_verde", "sembrado", 4, "hortaliza", "28/01/2027", "18/06/2027", "Vainas tiernas cultivadas mediante técnicas de agricultura sostenible.", 3.80, "1 kg", Arrays.asList("Temporada", "Ecológico"), "fruto"));
        sampleCultivos.add(new Cultivo("", "Patata", "patata", "sembrado", 12, "hortaliza", "10/01/2027", "30/05/2027", "Tubérculos sembrados en tierra fértil para garantizar una cosecha abundante.", 1.40, "1 kg", Arrays.asList("Km 0"), "raíz"));
        sampleCultivos.add(new Cultivo("", "Calabaza", "calabaza", "sembrado", 3, "hortaliza", "01/02/2027", "15/09/2027", "Grandes frutos en formación que llenarán de sabor tus creaciones culinarias.", 2.00, "1 kg", Arrays.asList("Temporada"), "fruto"));

        // Frutas
        sampleCultivos.add(new Cultivo("", "Pera", "pera", "creciendo", 40, "fruta", "10/03/2026", "10/08/2027", "Árbol frutal en desarrollo, ofreciendo jugosas peras de piel fina y dulce.", 3.20, "1 kg", Arrays.asList("Temporada"), "frutal"));
        sampleCultivos.add(new Cultivo("", "Albaricoque", "albaricoques", "creciendo", 30, "fruta", "15/02/2026", "20/06/2027", "Ramas florecientes que prometen frutos dulces y aromáticos para el verano.", 4.50, "1 kg", Arrays.asList("Temporada", "Ecológico"), "frutal"));
        sampleCultivos.add(new Cultivo("", "Melocotón", "melocoton", "creciendo", 35, "fruta", "20/02/2026", "05/07/2027", "Melocotoneros fuertes y sanos desarrollándose bajo el clima de Talavera.", 3.80, "1 kg", Arrays.asList("Temporada"), "frutal"));
        sampleCultivos.add(new Cultivo("", "Higo", "higo", "creciendo", 25, "fruta", "01/03/2026", "25/08/2027", "Higuera tradicional que crece frondosa aportando higos exquisitos y melosos.", 4.20, "1 kg", Arrays.asList("Temporada", "Ecológico", "Km 0"), "frutal"));
        sampleCultivos.add(new Cultivo("", "Melón", "melon", "sembrado", 2, "fruta", "05/02/2027", "10/08/2027", "Cultivo rastrero recién sembrado que nos regalará los mejores melones estivales.", 2.50, "1 ud", Arrays.asList("Temporada"), "frutal"));
        sampleCultivos.add(new Cultivo("", "Sandía", "sandia", "sembrado", 2, "fruta", "05/02/2027", "15/08/2027", "Semillas germinando con fuerza para asegurar sandías gigantes y muy dulces.", 2.00, "1 ud", Arrays.asList("Temporada"), "frutal"));

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

    public static void cargarNoticiasEjemplo(OnNoticiasLoadedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("noticias").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                listener.onSuccess(0);
                return;
            }

            long currentTime = System.currentTimeMillis();
            long dayMillis = 24 * 60 * 60 * 1000L;

            List<Map<String, Object>> sampleNoticias = new ArrayList<>();

            Map<String, Object> n1 = new HashMap<>();
            n1.put("titulo", "Cosecha récord de acelgas y lechugas frescas");
            n1.put("resumen", "Esta semana hemos recolectado las mejores acelgas y lechugas crujientes de nuestra huerta talaverana.");
            n1.put("contenido", "Gracias a las recientes lluvias y al cuidado ecológico, nuestras acelgas y lechugas han alcanzado un tamaño y sabor extraordinarios. Ya están disponibles en las cestas de nuestros suscriptores para disfrutar en casa.");
            n1.put("categoria", "cosecha");
            n1.put("autor", "Administrador Tu Cosecha");
            n1.put("fecha", new com.google.firebase.Timestamp(new Date(currentTime - dayMillis)));
            sampleNoticias.add(n1);

            Map<String, Object> n2 = new HashMap<>();
            n2.put("titulo", "Sembrando los primeros tomates de temporada");
            n2.put("resumen", "Comenzamos la siembra de variedades tradicionales de tomate en nuestros invernaderos.");
            n2.put("contenido", "Ya hemos iniciado el trasplante de los semilleros de tomate. Este año apostamos por variedades autóctonas que garantizan un sabor inigualable para los meses de verano.");
            n2.put("categoria", "huerto");
            n2.put("autor", "Administrador Tu Cosecha");
            n2.put("fecha", new com.google.firebase.Timestamp(new Date(currentTime - 2 * dayMillis)));
            sampleNoticias.add(n2);

            Map<String, Object> n3 = new HashMap<>();
            n3.put("titulo", "Consejos clave para el riego eficiente en invierno");
            n3.put("resumen", "Cómo optimizar el agua de riego aprovechando la humedad ambiental de la vega del Tajo.");
            n3.put("contenido", "Durante los meses más fríos, las plantas requieren menor frecuencia de riego pero mayor profundidad. Te contamos las mejores prácticas para mantener tus cultivos sanos sin desperdiciar recursos hídricos.");
            n3.put("categoria", "consejos");
            n3.put("autor", "Administrador Tu Cosecha");
            n3.put("fecha", new com.google.firebase.Timestamp(new Date(currentTime - 3 * dayMillis)));
            sampleNoticias.add(n3);

            Map<String, Object> n4 = new HashMap<>();
            n4.put("titulo", "Aviso importante sobre horarios de recogida en el huerto");
            n4.put("resumen", "Actualización de los horarios de apertura y recogida de cestas para socios y suscriptores.");
            n4.put("contenido", "Recordamos a todos nuestros suscriptores que el horario de recogida en el punto de Talavera de la Reina es de lunes a viernes de 9:00 a 14:00 y de 17:00 a 19:00 horas. ¡Os esperamos!");
            n4.put("categoria", "aviso");
            n4.put("autor", "Administrador Tu Cosecha");
            n4.put("fecha", new com.google.firebase.Timestamp(new Date(currentTime - 4 * dayMillis)));
            sampleNoticias.add(n4);

            Map<String, Object> n5 = new HashMap<>();
            n5.put("titulo", "Nuevas variedades de frutos rojos en camino");
            n5.put("resumen", "Expandimos nuestra zona de frutales con frambuesas, moras y arándanos ecológicos.");
            n5.put("contenido", "Hemos preparado una nueva parcela dedicada exclusivamente a frutos rojos. Las primeras plantas ya están asentadas y esperamos una primera cata piloto para la próxima primavera.");
            n5.put("categoria", "cosecha");
            n5.put("autor", "Administrador Tu Cosecha");
            n5.put("fecha", new com.google.firebase.Timestamp(new Date(currentTime - 5 * dayMillis)));
            sampleNoticias.add(n5);

            Map<String, Object> n6 = new HashMap<>();
            n6.put("titulo", "Preparando el compostaje orgánico sostenible");
            n6.put("resumen", "Transformamos los restos de poda y hortalizas en abono natural de primera calidad.");
            n6.put("contenido", "El ciclo del huerto es circular. Todos los restos orgánicos de cosechas anteriores son procesados en nuestra zona de compostaje para devolver los nutrientes esenciales directamente a la tierra.");
            n6.put("categoria", "consejos");
            n6.put("autor", "Administrador Tu Cosecha");
            n6.put("fecha", new com.google.firebase.Timestamp(new Date(currentTime - 6 * dayMillis)));
            sampleNoticias.add(n6);

            WriteBatch batch = db.batch();
            for (Map<String, Object> n : sampleNoticias) {
                com.google.firebase.firestore.DocumentReference docRef = db.collection("noticias").document();
                batch.set(docRef, n);
            }

            batch.commit()
                    .addOnSuccessListener(aVoid -> listener.onSuccess(sampleNoticias.size()))
                    .addOnFailureListener(e -> {
                        Log.e("CARGA_DATOS", "Error al cargar noticias: " + e.getMessage(), e);
                        listener.onError(e);
                    });

        }).addOnFailureListener(e -> {
            Log.e("CARGA_DATOS", "Error al verificar noticias: " + e.getMessage(), e);
            listener.onError(e);
        });
    }

    public static void cargarPlanesEjemplo(OnPlanesLoadedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("planes").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                listener.onSuccess(0);
                return;
            }

            WriteBatch batch = db.batch();

            Plan basico = new Plan("basico", "Plan Básico", "Ideal para 1-2 personas. Cesta semanal de hortalizas de temporada.", 39.0, "1 persona", 4, false,
                    Arrays.asList("Hortalizas frescas de temporada", "Recogida semanal en punto asignado", "Recetario exclusivo digital"));
            batch.set(db.collection("planes").document("basico"), basico);

            Plan familiar = new Plan("familiar", "Plan Familiar", "Perfecto para familias. Gran variedad de verduras y frutas frescas.", 69.0, "3-4 personas", 6, false,
                    Arrays.asList("Verduras y frutas de temporada", "Doble ración de productos estrella", "Prioridad en elección de entrega"));
            batch.set(db.collection("planes").document("familiar"), familiar);

            Plan premium = new Plan("premium", "Plan Premium", "Máxima variedad, productos ecológicos extra y visitas guiadas al huerto.", 99.0, "3-4 personas", 8, true,
                    Arrays.asList("Máxima variedad y productos ecológicos extra", "Prioridad absoluta en productos escasos", "Visitas guiadas y talleres en el huerto"));
            batch.set(db.collection("planes").document("premium"), premium);

            batch.commit()
                    .addOnSuccessListener(aVoid -> listener.onSuccess(3))
                    .addOnFailureListener(e -> {
                        Log.e("CARGA_DATOS", "Error al cargar planes: " + e.getMessage(), e);
                        listener.onError(e);
                    });

        }).addOnFailureListener(e -> {
            Log.e("CARGA_DATOS", "Error al verificar planes: " + e.getMessage(), e);
            listener.onError(e);
        });
    }

    public static void cargarCosechaEjemplo(OnCosechaLoadedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Calendar calendar = Calendar.getInstance();
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        String fechaRecogida = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime());

        List<ProductoCosecha> productos = new ArrayList<>();
        productos.add(new ProductoCosecha("Zanahoria", "zanahoria", "kg", 1, 2, 3, false));
        productos.add(new ProductoCosecha("Lechuga", "lechuga", "uds", 1, 2, 2, false));
        productos.add(new ProductoCosecha("Acelga", "acelga", "manojo", 1, 1, 2, false));
        productos.add(new ProductoCosecha("Puerro", "puerro", "manojo", 1, 1, 2, false));
        productos.add(new ProductoCosecha("Coliflor", "coliflor", "ud", 0, 1, 1, false));
        productos.add(new ProductoCosecha("Brócoli", "brocoli", "ud", 0, 1, 1, false));
        productos.add(new ProductoCosecha("Ajo", "ajo", "kg", 0, 0, 0.5, true));
        productos.add(new ProductoCosecha("Cebolla morada", "cebolla_morada", "kg", 0, 0, 1, true));

        Map<String, Object> cosechaMap = new HashMap<>();
        cosechaMap.put("semana", "Semana actual en curso");
        cosechaMap.put("fechaRecogida", fechaRecogida);
        cosechaMap.put("horaRecogida", "10:00 a 13:00");
        cosechaMap.put("lugarRecogida", "Huerto Tú Cosecha, vega del Tajo, Talavera de la Reina");
        cosechaMap.put("activa", true);
        cosechaMap.put("productos", productos);

        db.collection("cosechas").add(cosechaMap)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e("CARGA_DATOS", "Error al cargar cosecha: " + e.getMessage(), e);
                    listener.onError(e);
                });
    }
}
