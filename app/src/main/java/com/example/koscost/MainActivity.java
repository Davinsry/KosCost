package com.example.koscost;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.koscost.activities.LoginActivity;
import com.example.koscost.activities.TambahKamarActivity;
import com.example.koscost.activities.LaporanActivity;
import com.example.koscost.activities.ProfileActivity; // Import ProfileActivity (jika ada)
import com.example.koscost.adapter.KamarAdapter;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.database.DatabaseHelper; // JANGAN LUPA INI
import com.example.koscost.model.Kamar;
import com.example.koscost.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvKamar;
    private KamarAdapter adapter;
    private SessionManager sessionManager;
    private DatabaseHelper dbHelper; // Ditaruh di dalam class

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inisialisasi Helper
        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        // Setup RecyclerView
        rvKamar = findViewById(R.id.rv_kamar);
        rvKamar.setLayoutManager(new GridLayoutManager(this, 2));

        // --- 1. Tombol Logout ---
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

        // --- 2. Tombol Tambah Kamar (FAB) ---
        FloatingActionButton fab = findViewById(R.id.fab_tambah);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, TambahKamarActivity.class);
                startActivity(intent);
            });
        }

        /// --- 3. Tombol Laporan ---
        ImageButton btnLaporan = findViewById(R.id.btn_laporan);
        if (btnLaporan != null) {
            btnLaporan.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, LaporanActivity.class));
            });
        }

        // --- 4. Tombol Profile (BARU - JANGAN DI-KOMEN LAGI) ---
        ImageButton btnProfile = findViewById(R.id.btn_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                // Pindah ke Halaman Profile
                startActivity(new Intent(MainActivity.this, com.example.koscost.activities.ProfileActivity.class));
            });
        }

    }

    // --- Refresh Data saat kembali ke halaman ini ---
    @Override
    protected void onResume() {
        super.onResume();
        loadDataKamar();
    }

    private void loadDataKamar() {
        String emailUser = sessionManager.getEmail();
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        // Panggil API
        apiService.getDaftarKamar(emailUser).enqueue(new Callback<List<Kamar>>() {
            @Override
            public void onResponse(Call<List<Kamar>> call, Response<List<Kamar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Kamar> list = response.body();

                    // 1. Tampilkan Data dari Server
                    adapter = new KamarAdapter(MainActivity.this, list);
                    rvKamar.setAdapter(adapter);

                    // 2. SIMPAN KE LOKAL (UNTUK CADANGAN OFFLINE)
                    dbHelper.simpanKamarOffline(list);
                }
            }

            @Override
            public void onFailure(Call<List<Kamar>> call, Throwable t) {
                // 3. JIKA INTERNET MATI / ERROR -> AMBIL DARI LOKAL
                Toast.makeText(MainActivity.this, "Mode Offline: Menampilkan data terakhir", Toast.LENGTH_LONG).show();

                List<Kamar> offlineList = dbHelper.getKamarOffline();
                adapter = new KamarAdapter(MainActivity.this, offlineList);
                rvKamar.setAdapter(adapter);
            }
        });
    }
}