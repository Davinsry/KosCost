package com.example.koscost;

import android.content.Intent;
import android.database.Cursor; // PENTING
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.koscost.activities.LoginActivity;
import com.example.koscost.activities.TambahKamarActivity;
import com.example.koscost.activities.LaporanActivity;
import com.example.koscost.activities.ProfileActivity;
import com.example.koscost.adapter.KamarAdapter;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.database.DatabaseHelper;
import com.example.koscost.model.Kamar;
import com.example.koscost.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import okhttp3.ResponseBody; // PENTING
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvKamar;
    private KamarAdapter adapter;
    private SessionManager sessionManager;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        rvKamar = findViewById(R.id.rv_kamar);
        rvKamar.setLayoutManager(new GridLayoutManager(this, 2));

        // Tombol Logout
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

        // FAB Tambah Kamar
        FloatingActionButton fab = findViewById(R.id.fab_tambah);
        if (fab != null) {
            fab.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TambahKamarActivity.class)));
        }

        // Tombol Laporan
        ImageButton btnLaporan = findViewById(R.id.btn_laporan);
        if (btnLaporan != null) {
            btnLaporan.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LaporanActivity.class)));
        }

        // Tombol Profile
        ImageButton btnProfile = findViewById(R.id.btn_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 1. Coba Sinkronisasi Data Pending dulu
        syncDataOffline();

        // 2. Load Data (Kalau ada internet ambil baru, kalau tidak ambil lokal)
        loadDataKamar();
    }

    // --- LOGIKA SINKRONISASI DATA PENDING ---
    // --- LOGIKA SINKRONISASI DATA PENDING (DIPERBAIKI) ---
    // --- LOGIKA SINKRONISASI DATA PENDING (VERSI FIX) ---
    private void syncDataOffline() {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        // A. Kirim Kamar Pending
        Cursor cKamar = dbHelper.getPendingKamar();
        if (cKamar != null && cKamar.moveToFirst()) {
            Toast.makeText(this, "Mengirim data tertunda...", Toast.LENGTH_SHORT).show();
            do {
                // Ambil ID (Penting untuk penghapusan)
                int id = cKamar.getInt(cKamar.getColumnIndexOrThrow("id"));

                String email = cKamar.getString(cKamar.getColumnIndexOrThrow("email"));
                String no = cKamar.getString(cKamar.getColumnIndexOrThrow("no_kamar"));
                String fas = cKamar.getString(cKamar.getColumnIndexOrThrow("fasilitas"));
                double h = cKamar.getDouble(cKamar.getColumnIndexOrThrow("harian"));
                double m = cKamar.getDouble(cKamar.getColumnIndexOrThrow("mingguan"));
                double b = cKamar.getDouble(cKamar.getColumnIndexOrThrow("bulanan"));

                api.tambahKamar(email, no, fas, h, m, b).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            // SUKSES -> BARU HAPUS DARI HP
                            dbHelper.deletePendingKamar(id);
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        // GAGAL/OFFLINE -> BIARKAN SAJA (JANGAN DIHAPUS)
                        // Nanti akan dicoba lagi saat onResume berikutnya
                    }
                });
            } while (cKamar.moveToNext());
            // HAPUS BARIS INI: dbHelper.clearPendingKamar(); <--- JANGAN PAKAI INI LAGI
        }
        if (cKamar != null) cKamar.close();

        // B. Kirim Sewa Pending
        Cursor cSewa = dbHelper.getPendingSewa();
        if (cSewa != null && cSewa.moveToFirst()) {
            do {
                int id = cSewa.getInt(cSewa.getColumnIndexOrThrow("id"));

                String email = cSewa.getString(cSewa.getColumnIndexOrThrow("email"));
                String no = cSewa.getString(cSewa.getColumnIndexOrThrow("no_kamar"));
                String nama = cSewa.getString(cSewa.getColumnIndexOrThrow("nama"));
                String wa = cSewa.getString(cSewa.getColumnIndexOrThrow("wa"));
                String kerja = cSewa.getString(cSewa.getColumnIndexOrThrow("kerja"));
                String durasi = cSewa.getString(cSewa.getColumnIndexOrThrow("durasi"));
                double total = cSewa.getDouble(cSewa.getColumnIndexOrThrow("total"));
                String status = cSewa.getString(cSewa.getColumnIndexOrThrow("status"));

                api.simpanSewa(email, no, nama, wa, kerja, durasi, total, status).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            // SUKSES -> HAPUS DARI HP
                            dbHelper.deletePendingSewa(id);
                            // Refresh tampilan Dashboard biar status "TERISI" muncul
                            loadDataKamar();
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        // GAGAL -> BIARKAN
                    }
                });
            } while (cSewa.moveToNext());
            // HAPUS BARIS INI: dbHelper.clearPendingSewa();
        }
        if (cSewa != null) cSewa.close();
    }

    private void loadDataKamar() {
        String emailUser = sessionManager.getEmail();
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        apiService.getDaftarKamar(emailUser).enqueue(new Callback<List<Kamar>>() {
            @Override
            public void onResponse(Call<List<Kamar>> call, Response<List<Kamar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Kamar> list = response.body();

                    // 1. Tampilkan Data
                    adapter = new KamarAdapter(MainActivity.this, list);
                    rvKamar.setAdapter(adapter);

                    // 2. PENTING: SIMPAN KE LOKAL SEKARANG JUGA!
                    // Ini yang bikin data lama tetap ada pas offline nanti
                    dbHelper.simpanKamarOffline(list);

                    // (Opsional) Toast untuk debug
                    // Toast.makeText(MainActivity.this, "Data Disimpan ke Lokal", Toast.LENGTH_SHORT).show();
                }
            }
            // ... (onFailure tetap ambil dari lokal) ...
        });
    }

            @Override
            public void onFailure(Call<List<Kamar>> call, Throwable t) {
                // Jika Gagal/Offline -> Ambil dari lokal
                Toast.makeText(MainActivity.this, "Mode Offline", Toast.LENGTH_SHORT).show();
                List<Kamar> offlineList = dbHelper.getKamarOffline();
                adapter = new KamarAdapter(MainActivity.this, offlineList);
                rvKamar.setAdapter(adapter);
            }
        });
    }
}