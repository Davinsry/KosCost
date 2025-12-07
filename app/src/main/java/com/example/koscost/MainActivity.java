package com.example.koscost;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.koscost.activities.LoginActivity;
import com.example.koscost.activities.TambahKamarActivity; // Import Activity Tambah Kamar
import com.example.koscost.adapter.KamarAdapter;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.model.Kamar;
import com.example.koscost.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Import FAB

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvKamar;
    private KamarAdapter adapter;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        // Setup RecyclerView
        rvKamar = findViewById(R.id.rv_kamar);
        rvKamar.setLayoutManager(new GridLayoutManager(this, 2));

        // --- 1. Logika Tombol Logout ---
        ImageButton btnLogout = findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                sessionManager.logoutUser();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // --- 2. Logika Floating Action Button (Tambah Kamar) ---
        FloatingActionButton fab = findViewById(R.id.fab_tambah);
        if (fab != null) { // Cek null safety
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, TambahKamarActivity.class);
                startActivity(intent);
            });
        }
        ImageButton btnLaporan = findViewById(R.id.btn_laporan);
        btnLaporan.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, com.example.koscost.activities.LaporanActivity.class));
        });
    }

    // --- 3. Refresh Data saat kembali ke halaman ini ---
    @Override
    protected void onResume() {
        super.onResume();
        loadDataKamar();
    }

    private void loadDataKamar() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<Kamar>> call = apiService.getDaftarKamar();

        call.enqueue(new Callback<List<Kamar>>() {
            @Override
            public void onResponse(Call<List<Kamar>> call, Response<List<Kamar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Kamar> listKamar = response.body();
                    adapter = new KamarAdapter(MainActivity.this, listKamar);
                    rvKamar.setAdapter(adapter);
                } else {
                    Toast.makeText(MainActivity.this, "Gagal memuat data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Kamar>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}