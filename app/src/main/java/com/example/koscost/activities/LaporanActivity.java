package com.example.koscost.activities;

import android.os.Bundle;
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
import com.example.koscost.utils.SessionManager; // <--- JANGAN LUPA IMPORT INI
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
    SessionManager sessionManager; // 1. Tambah variabel ini

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_laporan);

        sessionManager = new SessionManager(this); // 2. Inisialisasi Session

        tvBulan = findViewById(R.id.tv_bulan_laporan);
        tvOmzet = findViewById(R.id.tv_omzet_total);
        rvRiwayat = findViewById(R.id.rv_riwayat);
        rvRiwayat.setLayoutManager(new LinearLayoutManager(this));

        loadData();
    }

    private void loadData() {
        // 3. Ambil Email dari Sesi Login
        String emailUser = sessionManager.getEmail();

        ApiService api = RetrofitClient.getClient().create(ApiService.class);

        // 4. Masukkan email ke dalam kurung (Solusi Error tadi)
        api.getLaporanKeuangan(emailUser).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);

                        if (json.has("status") && json.getString("status").equals("gagal")) {
                            Toast.makeText(LaporanActivity.this, json.getString("message"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Set Header
                        String bulan = json.getString("bulan");
                        double omzet = json.getDouble("omzet");

                        tvBulan.setText("Pendapatan " + bulan);
                        NumberFormat rp = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
                        tvOmzet.setText(rp.format(omzet));

                        // Set List
                        String jsonArray = json.getJSONArray("riwayat").toString();
                        Type listType = new TypeToken<List<Transaksi>>(){}.getType();
                        List<Transaksi> dataList = new Gson().fromJson(jsonArray, listType);

                        rvRiwayat.setAdapter(new RiwayatAdapter(dataList));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(LaporanActivity.this, "Error Parsing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LaporanActivity.this, "Gagal muat laporan", Toast.LENGTH_SHORT).show();
            }
        });
    }
}