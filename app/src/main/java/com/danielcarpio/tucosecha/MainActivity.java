package com.danielcarpio.tucosecha;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.danielcarpio.tucosecha.ui.fragments.CestaFragment;
import com.danielcarpio.tucosecha.ui.fragments.CuentaFragment;
import com.danielcarpio.tucosecha.ui.fragments.HuertoFragment;
import com.danielcarpio.tucosecha.ui.fragments.NoticiasFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Load default fragment on start
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.navHostFragment, new CestaFragment())
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_cesta) {
                selectedFragment = new CestaFragment();
            } else if (itemId == R.id.nav_huerto) {
                selectedFragment = new HuertoFragment();
            } else if (itemId == R.id.nav_noticias) {
                selectedFragment = new NoticiasFragment();
            } else if (itemId == R.id.nav_cuenta) {
                selectedFragment = new CuentaFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.navHostFragment, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }
}
