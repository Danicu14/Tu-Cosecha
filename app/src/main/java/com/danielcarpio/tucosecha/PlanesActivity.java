package com.danielcarpio.tucosecha;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.danielcarpio.tucosecha.adapter.PlanAdapter;
import com.danielcarpio.tucosecha.model.Plan;
import com.danielcarpio.tucosecha.ui.fragments.DetallePlanBottomSheet;
import com.danielcarpio.tucosecha.util.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PlanesActivity extends AppCompatActivity {

    private RecyclerView rvPlanes;
    private PlanAdapter planAdapter;
    private List<Plan> planList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planes);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbarPlanes);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvPlanes = findViewById(R.id.rvPlanesActivity);
        rvPlanes.setLayoutManager(new LinearLayoutManager(this));
        planList = new ArrayList<>();
        planAdapter = new PlanAdapter(this, planList, plan -> {
            if (SessionManager.isEsInvitado()) {
                startActivity(new Intent(PlanesActivity.this, LoginActivity.class));
            } else {
                DetallePlanBottomSheet bottomSheet = DetallePlanBottomSheet.newInstance(plan);
                bottomSheet.show(getSupportFragmentManager(), "DetallePlanBottomSheet");
            }
        });
        rvPlanes.setAdapter(planAdapter);

        loadPlanesRealtime();
    }

    private void loadPlanesRealtime() {
        db.collection("planes").addSnapshotListener((value, error) -> {
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
}
