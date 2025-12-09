package com.example.koscost;

import android.content.Intent;
import android.database.Cursor;
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
import okhttp3.ResponseBody;
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

        // 1. Tombol Logout
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

        // 2. FAB Tambah Kamar
        FloatingActionButton fab = findViewById(R.id.fab_tambah);
        if (fab != null) {
            fab.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, TambahKamarActivity.class)));
        }

        // 3. Tombol Laporan
        ImageButton btnLaporan = findViewById(R.id.btn_laporan);
        if (btnLaporan != null) {
            btnLaporan.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, LaporanActivity.class)));
        }

        // 4. Tombol Profile
        ImageButton btnProfile = findViewById(R.id.btn_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Coba sinkronisasi data pending dulu
        syncDataOffline();
        // Load data (Offline/Online otomatis dihandle)
        loadDataKamar();
    }

    // --- LOGIKA SINKRONISASI DATA PENDING ---
    private void syncDataOffline() {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        // A. Kirim Kamar Pending (Tambah Kamar)
        Cursor cKamar = dbHelper.getPendingKamar();
        if (cKamar != null && cKamar.moveToFirst()) {
            Toast.makeText(this, "Sinkronisasi Tambah Kamar...", Toast.LENGTH_SHORT).show();
            do {
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
                            dbHelper.deletePendingKamar(id);
                        }
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
            } while (cKamar.moveToNext());
        }
        if (cKamar != null) cKamar.close();

        // B. Kirim Sewa Pending (Input Sewa Baru)
        Cursor cSewa = dbHelper.getPendingSewa();
        if (cSewa != null && cSewa.moveToFirst()) {
            Toast.makeText(this, "Sinkronisasi Sewa Baru...", Toast.LENGTH_SHORT).show();
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
                            dbHelper.deletePendingSewa(id);
                            loadDataKamar();
                        }
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
            } while (cSewa.moveToNext());
        }
        if (cSewa != null) cSewa.close();

        // C. Kirim Update Sewa Pending (Edit Penghuni)
        Cursor cUpdSewa = dbHelper.getPendingUpdateSewa();
        if (cUpdSewa != null && cUpdSewa.moveToFirst()) {
            Toast.makeText(this, "Sinkronisasi Edit Penghuni...", Toast.LENGTH_SHORT).show();
            do {
                int id = cUpdSewa.getInt(cUpdSewa.getColumnIndexOrThrow("id"));
                String idSewa = cUpdSewa.getString(cUpdSewa.getColumnIndexOrThrow("id_sewa"));
                String nama = cUpdSewa.getString(cUpdSewa.getColumnIndexOrThrow("nama"));
                String wa = cUpdSewa.getString(cUpdSewa.getColumnIndexOrThrow("wa"));
                String kerja = cUpdSewa.getString(cUpdSewa.getColumnIndexOrThrow("kerja"));
                String tglIn = cUpdSewa.getString(cUpdSewa.getColumnIndexOrThrow("tgl_in"));
                String tglOut = cUpdSewa.getString(cUpdSewa.getColumnIndexOrThrow("tgl_out"));
                String durasi = cUpdSewa.getString(cUpdSewa.getColumnIndexOrThrow("durasi"));
                double total = cUpdSewa.getDouble(cUpdSewa.getColumnIndexOrThrow("total"));
                double bayar = cUpdSewa.getDouble(cUpdSewa.getColumnIndexOrThrow("bayar"));
                String status = cUpdSewa.getString(cUpdSewa.getColumnIndexOrThrow("status"));

                api.updateSewa(idSewa, nama, wa, kerja, tglIn, tglOut, durasi, total, bayar, status)
                        .enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                if(response.isSuccessful()) dbHelper.deletePendingUpdateSewa(id);
                            }
                            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                        });
            } while (cUpdSewa.moveToNext());
        }
        if (cUpdSewa != null) cUpdSewa.close();

        // D. Kirim Checkout Pending
        Cursor cOut = dbHelper.getPendingCheckout();
        if (cOut != null && cOut.moveToFirst()) {
            Toast.makeText(this, "Sinkronisasi Checkout...", Toast.LENGTH_SHORT).show();
            do {
                int id = cOut.getInt(cOut.getColumnIndexOrThrow("id"));
                String noKamar = cOut.getString(cOut.getColumnIndexOrThrow("no_kamar"));

                api.prosesCheckout(noKamar).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if(response.isSuccessful()) {
                            dbHelper.deletePendingCheckout(id);
                            loadDataKamar(); // Refresh agar kamar jadi hijau
                        }
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
            } while (cOut.moveToNext());
        }
        if (cOut != null) cOut.close();

        // E. Kirim Edit Kamar Pending
        Cursor cEditKam = dbHelper.getPendingEditKamar();
        if (cEditKam != null && cEditKam.moveToFirst()) {
            Toast.makeText(this, "Sinkronisasi Edit Kamar...", Toast.LENGTH_SHORT).show();
            do {
                int id = cEditKam.getInt(cEditKam.getColumnIndexOrThrow("id"));
                String idKamar = cEditKam.getString(cEditKam.getColumnIndexOrThrow("id_kamar"));
                String no = cEditKam.getString(cEditKam.getColumnIndexOrThrow("no_kamar"));
                String fas = cEditKam.getString(cEditKam.getColumnIndexOrThrow("fasilitas"));
                double h = cEditKam.getDouble(cEditKam.getColumnIndexOrThrow("harian"));
                double m = cEditKam.getDouble(cEditKam.getColumnIndexOrThrow("mingguan"));
                double b = cEditKam.getDouble(cEditKam.getColumnIndexOrThrow("bulanan"));

                api.editKamar(idKamar, no, fas, h, m, b).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if(response.isSuccessful()) {
                            dbHelper.deletePendingEditKamar(id);
                            loadDataKamar();
                        }
                    }
                    @Override public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
            } while (cEditKam.moveToNext());
        }
        if (cEditKam != null) cEditKam.close();
    }

    // --- LOAD DATA ---
    private void loadDataKamar() {
        String emailUser = sessionManager.getEmail();
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);

        apiService.getDaftarKamar(emailUser).enqueue(new Callback<List<Kamar>>() {
            @Override
            public void onResponse(Call<List<Kamar>> call, Response<List<Kamar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Kamar> list = response.body();
                    adapter = new KamarAdapter(MainActivity.this, list);
                    rvKamar.setAdapter(adapter);
                    dbHelper.simpanKamarOffline(list);
                }
            }

            @Override
            public void onFailure(Call<List<Kamar>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Mode Offline", Toast.LENGTH_SHORT).show();
                List<Kamar> offlineList = dbHelper.getKamarOffline();
                adapter = new KamarAdapter(MainActivity.this, offlineList);
                rvKamar.setAdapter(adapter);
            }
        });
    }
}