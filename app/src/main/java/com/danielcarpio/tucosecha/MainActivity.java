package com.danielcarpio.tucosecha;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.danielcarpio.tucosecha.ui.fragments.CestaFragment;
import com.danielcarpio.tucosecha.ui.fragments.CuentaFragment;
import com.danielcarpio.tucosecha.ui.fragments.HuertoFragment;
import com.danielcarpio.tucosecha.ui.fragments.NoticiasFragment;

public class MainActivity extends AppCompatActivity {

    private View pillCesta, pillProductos, pillNoticias, pillMiZona;
    private View lineCesta, lineProductos, lineNoticias, lineMiZona;
    private TextView tvCesta, tvProductos, tvNoticias, tvMiZona;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View tabCesta = findViewById(R.id.navTabCesta);
        View tabProductos = findViewById(R.id.navTabProductos);
        View tabNoticias = findViewById(R.id.navTabNoticias);
        View tabMiZona = findViewById(R.id.navTabMiZona);

        pillCesta = findViewById(R.id.indicatorPillCesta);
        pillProductos = findViewById(R.id.indicatorPillProductos);
        pillNoticias = findViewById(R.id.indicatorPillNoticias);
        pillMiZona = findViewById(R.id.indicatorPillMiZona);

        lineCesta = findViewById(R.id.indicatorLineCesta);
        lineProductos = findViewById(R.id.indicatorLineProductos);
        lineNoticias = findViewById(R.id.indicatorLineNoticias);
        lineMiZona = findViewById(R.id.indicatorLineMiZona);

        tvCesta = findViewById(R.id.tvLabelCesta);
        tvProductos = findViewById(R.id.tvLabelProductos);
        tvNoticias = findViewById(R.id.tvLabelNoticias);
        tvMiZona = findViewById(R.id.tvLabelMiZona);

        // Load default fragment on start
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navHostFragment, new CestaFragment())
                    .commit();
            updateTabSelection(0);
        }

        tabCesta.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navHostFragment, new CestaFragment())
                    .commit();
            updateTabSelection(0);
        });

        tabProductos.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navHostFragment, new HuertoFragment())
                    .commit();
            updateTabSelection(1);
        });

        tabNoticias.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navHostFragment, new NoticiasFragment())
                    .commit();
            updateTabSelection(2);
        });

        tabMiZona.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navHostFragment, new CuentaFragment())
                    .commit();
            updateTabSelection(3);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    private void updateTabSelection(int index) {
        pillCesta.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        lineCesta.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        tvCesta.setTextColor(index == 0 ? Color.parseColor("#1F5A2A") : Color.parseColor("#6B7A5E"));
        tvCesta.getPaint().setFakeBoldText(index == 0);

        pillProductos.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        lineProductos.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        tvProductos.setTextColor(index == 1 ? Color.parseColor("#1F5A2A") : Color.parseColor("#6B7A5E"));
        tvProductos.getPaint().setFakeBoldText(index == 1);

        pillNoticias.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        lineNoticias.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        tvNoticias.setTextColor(index == 2 ? Color.parseColor("#1F5A2A") : Color.parseColor("#6B7A5E"));
        tvNoticias.getPaint().setFakeBoldText(index == 2);

        pillMiZona.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        lineMiZona.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        tvMiZona.setTextColor(index == 3 ? Color.parseColor("#1F5A2A") : Color.parseColor("#6B7A5E"));
        tvMiZona.getPaint().setFakeBoldText(index == 3);
    }
}
