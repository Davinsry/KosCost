package com.example.koscost.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.koscost.R;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailKamarActivity extends AppCompatActivity {

    TextView tvJudul, tvNama, tvTgl, tvDurasi;
    Button btnCheckout;
    String noKamar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_kamar);

        tvJudul = findViewById(R.id.tv_judul_kamar);
        tvNama = findViewById(R.id.tv_nama_penghuni);
        tvTgl = findViewById(R.id.tv_tgl_masuk);
        tvDurasi = findViewById(R.id.tv_durasi);
        btnCheckout = findViewById(R.id.btn_checkout);

        // Ambil No Kamar dari Intent
        noKamar = getIntent().getStringExtra("NO_KAMAR");
        tvJudul.setText("Detail Kamar " + noKamar);

        // Load Data Penghuni
        loadDataPenghuni();

        // Klik Tombol Checkout
        btnCheckout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Check-Out")
                    .setMessage("Apakah yakin ingin mengosongkan kamar ini? Status akan berubah menjadi Kosong.")
                    .setPositiveButton("Ya, Check-Out", (dialog, which) -> prosesCheckout())
                    .setNegativeButton("Batal", null)
                    .show();
        });
    }

    private void loadDataPenghuni() {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.getDetailSewa(noKamar).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);

                        // Set Text
                        tvNama.setText(json.optString("nama_penghuni", "Tidak ada data"));
                        tvTgl.setText("Check In: " + json.optString("tgl_checkin"));
                        tvDurasi.setText("Durasi: " + json.optString("durasi_sewa"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(DetailKamarActivity.this, "Gagal ambil data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void prosesCheckout() {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.prosesCheckout(noKamar).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Toast.makeText(DetailKamarActivity.this, "Check-Out Berhasil!", Toast.LENGTH_SHORT).show();
                finish(); // Kembali ke Dashboard
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(DetailKamarActivity.this, "Gagal koneksi", Toast.LENGTH_SHORT).show();
            }
        });
    }
}