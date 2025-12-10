package com.example.koscost.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.koscost.R;
import com.example.koscost.adapter.RiwayatAdapter;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.model.Transaksi;
import com.example.koscost.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;
import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LaporanActivity extends AppCompatActivity {

    TextView tvBulan, tvOmzet;
    RecyclerView rvRiwayat;
    ImageButton btnBack;
    SessionManager sessionManager;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_laporan);

        sessionManager = new SessionManager(this);
        // Setup Cache Sederhana
        sharedPreferences = getSharedPreferences("LaporanCache", Context.MODE_PRIVATE);

        tvBulan = findViewById(R.id.tv_bulan_laporan);
        tvOmzet = findViewById(R.id.tv_omzet_total);
        btnBack = findViewById(R.id.btn_back); // Pastikan ada tombol back di XML jika mau
        rvRiwayat = findViewById(R.id.rv_riwayat);
        rvRiwayat.setLayoutManager(new LinearLayoutManager(this));

        // Setup Back Button (Optional kalau ada di layout)
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        loadData();
    }

    private void loadData() {
        String emailUser = sessionManager.getEmail();
        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        api.getLaporanKeuangan(emailUser).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        // 1. Sukses -> Tampilkan & Simpan ke Cache
                        tampilkanLaporan(res);
                        simpanCacheLaporan(res);
                    } else {
                        // 2. Gagal Server -> Coba Offline
                        loadCacheLaporan();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    loadCacheLaporan();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 3. Gagal Koneksi -> Coba Offline
                Toast.makeText(LaporanActivity.this, "Mode Offline: Menampilkan data terakhir", Toast.LENGTH_SHORT).show();
                loadCacheLaporan();
            }
        });
    }

    private void tampilkanLaporan(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);

            // Set Header
            String bulan = json.optString("bulan", "Bulan Ini");
            double omzet = json.optDouble("omzet", 0);

            tvBulan.setText("Pendapatan " + bulan);
            NumberFormat rp = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            tvOmzet.setText(rp.format(omzet));

            // Set List
            if (json.has("riwayat")) {
                String jsonArray = json.getJSONArray("riwayat").toString();
                Type listType = new TypeToken<List<Transaksi>>(){}.getType();
                List<Transaksi> dataList = new Gson().fromJson(jsonArray, listType);
                rvRiwayat.setAdapter(new RiwayatAdapter(dataList));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Simpan JSON string ke SharedPreferences
    private void simpanCacheLaporan(String jsonString) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("last_laporan", jsonString);
        editor.apply();
    }

    // Ambil JSON string dari SharedPreferences
    private void loadCacheLaporan() {
        String cache = sharedPreferences.getString("last_laporan", null);
        if (cache != null) {
            tampilkanLaporan(cache);
        } else {
            Toast.makeText(this, "Data laporan belum tersedia offline", Toast.LENGTH_SHORT).show();
        }
    }
}