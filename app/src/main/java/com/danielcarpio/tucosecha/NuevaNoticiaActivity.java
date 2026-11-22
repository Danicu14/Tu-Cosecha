package com.danielcarpio.tucosecha;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.danielcarpio.tucosecha.util.Mensajes;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NuevaNoticiaActivity extends AppCompatActivity {

    private TextInputLayout tilTitulo, tilResumen, tilContenido;
    private TextInputEditText etTitulo, etResumen, etContenido;
    private AutoCompleteTextView autoCompleteCategoria;
    private MaterialButton btnPublicarNoticia, btnGaleria, btnCamara, btnRemovePhoto;
    private FrameLayout layoutPhotoPreview;
    private ImageView imgPhotoPreview;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Uri photoUri;
    private Bitmap selectedBitmap;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private ActivityResultLauncher<Uri> takePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nueva_noticia);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbarNuevaNoticia);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilTitulo = findViewById(R.id.tilTitulo);
        tilResumen = findViewById(R.id.tilResumen);
        tilContenido = findViewById(R.id.tilContenido);
        etTitulo = findViewById(R.id.etTitulo);
        etResumen = findViewById(R.id.etResumen);
        etContenido = findViewById(R.id.etContenido);
        autoCompleteCategoria = findViewById(R.id.autoCompleteCategoria);
        btnPublicarNoticia = findViewById(R.id.btnPublicarNoticia);
        btnGaleria = findViewById(R.id.btnGaleria);
        btnCamara = findViewById(R.id.btnCamara);
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto);
        layoutPhotoPreview = findViewById(R.id.layoutPhotoPreview);
        imgPhotoPreview = findViewById(R.id.imgPhotoPreview);

        // Categories with capitalized first letter in dropdown
        String[] categoriasDisplay = new String[]{"Huerto", "Cosecha", "Consejos", "Aviso"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categoriasDisplay);
        autoCompleteCategoria.setAdapter(adapter);
        autoCompleteCategoria.setText("Huerto", false);

        // Register Activity Results
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    selectedBitmap = BitmapFactory.decodeStream(is);
                    if (is != null) is.close();
                    if (selectedBitmap != null) {
                        imgPhotoPreview.setImageBitmap(selectedBitmap);
                        layoutPhotoPreview.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        File photoFile = new File(getFilesDir(), "noticia_temp.jpg");
        photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);

        takePicture = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                try {
                    InputStream is = getContentResolver().openInputStream(photoUri);
                    selectedBitmap = BitmapFactory.decodeStream(is);
                    if (is != null) is.close();
                    if (selectedBitmap != null) {
                        selectedBitmap = rotateBitmapIfNeeded(photoFile.getAbsolutePath(), selectedBitmap);
                        imgPhotoPreview.setImageBitmap(selectedBitmap);
                        layoutPhotoPreview.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        btnGaleria.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnCamara.setOnClickListener(v -> {
            takePicture.launch(photoUri);
        });

        btnRemovePhoto.setOnClickListener(v -> {
            selectedBitmap = null;
            imgPhotoPreview.setImageDrawable(null);
            layoutPhotoPreview.setVisibility(View.GONE);
        });

        btnPublicarNoticia.setOnClickListener(v -> publicarNoticia());
    }

    private Bitmap rotateBitmapIfNeeded(String photoPath, Bitmap bitmap) {
        try {
            android.media.ExifInterface exif = new android.media.ExifInterface(photoPath);
            int orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                default:
                    return bitmap;
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            return bitmap;
        }
    }

    private void publicarNoticia() {
        String titulo = etTitulo.getText() != null ? etTitulo.getText().toString().trim() : "";
        String resumen = etResumen.getText() != null ? etResumen.getText().toString().trim() : "";
        String contenido = etContenido.getText() != null ? etContenido.getText().toString().trim() : "";
        String categoriaDisplay = autoCompleteCategoria.getText() != null ? autoCompleteCategoria.getText().toString().trim() : "Huerto";
        String categoria = categoriaDisplay.toLowerCase();

        boolean hasError = false;

        if (TextUtils.isEmpty(titulo)) {
            tilTitulo.setError("El título es obligatorio");
            hasError = true;
        } else {
            tilTitulo.setError(null);
        }

        if (TextUtils.isEmpty(resumen)) {
            tilResumen.setError("El resumen es obligatorio");
            hasError = true;
        } else {
            tilResumen.setError(null);
        }

        if (TextUtils.isEmpty(contenido)) {
            tilContenido.setError("El contenido es obligatorio");
            hasError = true;
        } else {
            tilContenido.setError(null);
        }

        if (hasError) return;

        btnPublicarNoticia.setEnabled(false);
        btnPublicarNoticia.setText("Publicando...");

        executorService.execute(() -> {
            String fotoBase64 = null;
            if (selectedBitmap != null) {
                int width = selectedBitmap.getWidth();
                int height = selectedBitmap.getHeight();
                int maxDim = Math.max(width, height);
                Bitmap resized = selectedBitmap;
                if (maxDim > 1024) {
                    float scale = 1024f / maxDim;
                    int newW = Math.round(width * scale);
                    int newH = Math.round(height * scale);
                    resized = Bitmap.createScaledBitmap(selectedBitmap, newW, newH, true);
                }

                int quality = 75;
                byte[] bytes = null;
                while (quality >= 10) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    resized.compress(Bitmap.CompressFormat.JPEG, quality, stream);
                    bytes = stream.toByteArray();
                    if (bytes.length <= 700 * 1024) {
                        break;
                    }
                    quality -= 10;
                }

                if (bytes != null) {
                    fotoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                }
            }

            String finalFotoBase64 = fotoBase64;

            runOnUiThread(() -> {
                if (mAuth.getCurrentUser() != null) {
                    String uid = mAuth.getCurrentUser().getUid();
                    db.collection("usuarios").document(uid).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                String autor = "Administrador";
                                if (documentSnapshot.exists()) {
                                    String nombre = documentSnapshot.getString("nombre");
                                    String apellidos = documentSnapshot.getString("apellidos");
                                    if (nombre != null) {
                                        autor = nombre + (apellidos != null ? " " + apellidos : "");
                                    }
                                }

                                Map<String, Object> noticiaMap = new HashMap<>();
                                noticiaMap.put("titulo", titulo);
                                noticiaMap.put("resumen", resumen);
                                noticiaMap.put("contenido", contenido);
                                noticiaMap.put("categoria", categoria);
                                noticiaMap.put("autor", autor);
                                noticiaMap.put("fecha", FieldValue.serverTimestamp());
                                if (finalFotoBase64 != null) {
                                    noticiaMap.put("fotoBase64", finalFotoBase64);
                                }

                                db.collection("noticias").add(noticiaMap)
                                        .addOnSuccessListener(documentReference -> {
                                            Mensajes.mostrarExito(btnPublicarNoticia, "Noticia publicada con éxito");
                                            btnPublicarNoticia.postDelayed(this::finish, 1200);
                                        })
                                        .addOnFailureListener(e -> {
                                            btnPublicarNoticia.setEnabled(true);
                                            btnPublicarNoticia.setText("Publicar noticia");
                                            Mensajes.mostrarError(btnPublicarNoticia, "Error al publicar: " + e.getMessage());
                                        });
                            })
                            .addOnFailureListener(e -> {
                                btnPublicarNoticia.setEnabled(true);
                                btnPublicarNoticia.setText("Publicar noticia");
                                Mensajes.mostrarError(btnPublicarNoticia, "Error al obtener usuario: " + e.getMessage());
                            });
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
