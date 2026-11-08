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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.MiPlanActivity;
import com.danielcarpio.tucosecha.R;
import com.danielcarpio.tucosecha.RegistroActivity;
import com.danielcarpio.tucosecha.adapter.PlanAdapter;
import com.danielcarpio.tucosecha.model.Plan;
import com.danielcarpio.tucosecha.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CestaFragment extends Fragment {

    private LinearLayout layoutGuestView, layoutLoggedNoPlan, layoutLoggedActivePlan;
    private MaterialButton btnRegisterFromCesta, btnVerMiPlan;
    private RecyclerView rvPlanes;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Plan> planList;
    private PlanAdapter planAdapter;
    private ListenerRegistration planListener;
    private ListenerRegistration userListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cesta, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        layoutGuestView = view.findViewById(R.id.layoutGuestView);
        layoutLoggedNoPlan = view.findViewById(R.id.layoutLoggedNoPlan);
        layoutLoggedActivePlan = view.findViewById(R.id.layoutLoggedActivePlan);
        btnRegisterFromCesta = view.findViewById(R.id.btnRegisterFromCesta);
        btnVerMiPlan = view.findViewById(R.id.btnVerMiPlan);
        rvPlanes = view.findViewById(R.id.rvPlanes);

        rvPlanes.setLayoutManager(new LinearLayoutManager(getContext()));
        planList = new ArrayList<>();
        planAdapter = new PlanAdapter(getContext(), planList, plan -> {
            DetallePlanBottomSheet bottomSheet = DetallePlanBottomSheet.newInstance(plan);
            bottomSheet.show(getParentFragmentManager(), "DetallePlanBottomSheet");
        });
        rvPlanes.setAdapter(planAdapter);

        if (SessionManager.isEsInvitado()) {
            layoutGuestView.setVisibility(View.VISIBLE);
            layoutLoggedNoPlan.setVisibility(View.GONE);
            layoutLoggedActivePlan.setVisibility(View.GONE);

            btnRegisterFromCesta.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), RegistroActivity.class));
            });
        } else {
            layoutGuestView.setVisibility(View.GONE);
            checkUserSubscriptionAndLoadPlans();
        }

        btnVerMiPlan.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), MiPlanActivity.class));
        });

        return view;
    }

    private void checkUserSubscriptionAndLoadPlans() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (userListener != null) userListener.remove();

            userListener = db.collection("usuarios").document(user.getUid())
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (!isAdded()) return;

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String plan = documentSnapshot.getString("plan");
                            if (plan != null && !plan.equalsIgnoreCase("ninguno")) {
                                layoutLoggedActivePlan.setVisibility(View.VISIBLE);
                                layoutLoggedNoPlan.setVisibility(View.GONE);
                            } else {
                                layoutLoggedActivePlan.setVisibility(View.GONE);
                                layoutLoggedNoPlan.setVisibility(View.VISIBLE);
                                loadPlansRealtime();
                            }
                        }
                    });
        }
    }

    private void loadPlansRealtime() {
        if (planListener != null) planListener.remove();

        planListener = db.collection("planes")
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;

                    if (value != null) {
                        planList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Plan plan = doc.toObject(Plan.class);
                            if (plan.getId() == null || plan.getId().isEmpty()) {
                                plan.setId(doc.getId());
                            }
                            planList.add(plan);
                        }
                        planAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (planListener != null) {
            planListener.remove();
            planListener = null;
        }
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
    }
}
