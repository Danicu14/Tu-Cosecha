package com.danielcarpio.tucosecha.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.RegistroActivity;
import com.danielcarpio.tucosecha.util.SessionManager;
import com.google.android.material.button.MaterialButton;

public class CestaFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cesta, container, false);

        LinearLayout layoutGuestView = view.findViewById(R.id.layoutGuestView);
        LinearLayout layoutLoggedView = view.findViewById(R.id.layoutLoggedView);
        MaterialButton btnRegisterFromCesta = view.findViewById(R.id.btnRegisterFromCesta);

        if (SessionManager.isEsInvitado()) {
            layoutGuestView.setVisibility(View.VISIBLE);
            layoutLoggedView.setVisibility(View.GONE);

            btnRegisterFromCesta.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), RegistroActivity.class));
            });
        } else {
            layoutGuestView.setVisibility(View.GONE);
            layoutLoggedView.setVisibility(View.VISIBLE);
        }

        return view;
    }
}
