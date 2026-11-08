package com.danielcarpio.tucosecha;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.danielcarpio.tucosecha.ui.fragments.CestaFragment;
import com.danielcarpio.tucosecha.ui.fragments.CuentaFragment;
import com.danielcarpio.tucosecha.ui.fragments.HuertoFragment;
import com.danielcarpio.tucosecha.ui.fragments.NoticiasFragment;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_TAB = "selected_tab";
    private int currentPosition = 0;

    private View tabCesta, tabProductos, tabNoticias, tabZona;
    private ImageView imgCesta, imgProductos, imgNoticias, imgZona;
    private TextView tvCesta, tvProductos, tvNoticias, tvZona;
    private View lineCesta, lineProductos, lineNoticias, lineZona;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tabCesta = findViewById(R.id.navCesta);
        tabProductos = findViewById(R.id.navProductos);
        tabNoticias = findViewById(R.id.navNoticias);
        tabZona = findViewById(R.id.navZona);

        imgCesta = findViewById(R.id.imgCesta);
        imgProductos = findViewById(R.id.imgProductos);
        imgNoticias = findViewById(R.id.imgNoticias);
        imgZona = findViewById(R.id.imgZona);

        tvCesta = findViewById(R.id.tvCesta);
        tvProductos = findViewById(R.id.tvProductos);
        tvNoticias = findViewById(R.id.tvNoticias);
        tvZona = findViewById(R.id.tvZona);

        lineCesta = findViewById(R.id.lineCesta);
        lineProductos = findViewById(R.id.lineProductos);
        lineNoticias = findViewById(R.id.lineNoticias);
        lineZona = findViewById(R.id.lineZona);

        tabCesta.setOnClickListener(v -> seleccionar(0));
        tabProductos.setOnClickListener(v -> seleccionar(1));
        tabNoticias.setOnClickListener(v -> seleccionar(2));
        tabZona.setOnClickListener(v -> seleccionar(3));

        if (savedInstanceState != null) {
            currentPosition = savedInstanceState.getInt(KEY_SELECTED_TAB, 0);
            seleccionar(currentPosition);
        } else {
            seleccionar(0);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, currentPosition);
    }

    private void seleccionar(int posicion) {
        currentPosition = posicion;
        Fragment fragment;

        switch (posicion) {
            case 0:
                fragment = new CestaFragment();
                break;
            case 1:
                fragment = new HuertoFragment();
                break;
            case 2:
                fragment = new NoticiasFragment();
                break;
            case 3:
            default:
                fragment = new CuentaFragment();
                break;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.navHostFragment, fragment)
                .commit();

        updateNavUI(posicion);
    }

    private void updateNavUI(int activePos) {
        int activeTextColor = Color.parseColor("#1F5A2A");
        int inactiveTextColor = Color.parseColor("#6B7A5E");

        // Item 0: Cesta
        boolean isCesta = (activePos == 0);
        animateItem(imgCesta, isCesta);
        tvCesta.setTextColor(isCesta ? activeTextColor : inactiveTextColor);
        tvCesta.getPaint().setFakeBoldText(isCesta);
        lineCesta.setVisibility(isCesta ? View.VISIBLE : View.INVISIBLE);

        // Item 1: Productos
        boolean isProductos = (activePos == 1);
        animateItem(imgProductos, isProductos);
        tvProductos.setTextColor(isProductos ? activeTextColor : inactiveTextColor);
        tvProductos.getPaint().setFakeBoldText(isProductos);
        lineProductos.setVisibility(isProductos ? View.VISIBLE : View.INVISIBLE);

        // Item 2: Noticias
        boolean isNoticias = (activePos == 2);
        animateItem(imgNoticias, isNoticias);
        tvNoticias.setTextColor(isNoticias ? activeTextColor : inactiveTextColor);
        tvNoticias.getPaint().setFakeBoldText(isNoticias);
        lineNoticias.setVisibility(isNoticias ? View.VISIBLE : View.INVISIBLE);

        // Item 3: Mi Zona
        boolean isZona = (activePos == 3);
        animateItem(imgZona, isZona);
        tvZona.setTextColor(isZona ? activeTextColor : inactiveTextColor);
        tvZona.getPaint().setFakeBoldText(isZona);
        lineZona.setVisibility(isZona ? View.VISIBLE : View.INVISIBLE);
    }

    private void animateItem(View iconView, boolean active) {
        float scale = active ? 1.1f : 1.0f;
        float alpha = active ? 1.0f : 0.7f;

        iconView.animate()
                .scaleX(scale)
                .scaleY(scale)
                .alpha(alpha)
                .setDuration(150)
                .start();
    }
}
